package com.example.deepskyblue

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.deepskyblue.model.OcrResult
import com.example.deepskyblue.model.TextOcrBlock
import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

internal class DeepSkyBlueImpl(private val appContext: Context) : DeepSkyBlue {

    private val defaultRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val koreanRecognizer  = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    private var lastResult: OcrResult? = null
    private var selectedIndex: Int? = null

    private fun imageOf(bitmap: Bitmap, onFailure: (Exception) -> Unit): InputImage? =
        try { InputImage.fromBitmap(bitmap, 0) } catch (e: Exception) { onFailure(e); null }

    private fun recognizer(useKorean: Boolean) =
        if (useKorean) koreanRecognizer else defaultRecognizer

    override fun recognizeText(
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean
    ) {
        val image = imageOf(bitmap, onFailure) ?: return
        recognizer(useKorean).process(image)
            .addOnSuccessListener { onSuccess(it.text) }
            .addOnFailureListener(onFailure)
    }

    override fun recognizeTextDetailed(
        bitmap: Bitmap,
        onSuccess: (Text) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean
    ) {
        val image = imageOf(bitmap, onFailure) ?: return
        recognizer(useKorean).process(image)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
    }

    override fun recognizeTextBlocks(
        bitmap: Bitmap,
        onSuccess: (List<TextOcrBlock>) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean
    ) {
        val image = imageOf(bitmap, onFailure) ?: return
        recognizer(useKorean).process(image)
            .addOnSuccessListener { vt ->
                onSuccess(
                    vt.textBlocks.map { b ->
                        TextOcrBlock(b.text, b.cornerPoints?.toList() ?: emptyList())
                    }
                )
            }
            .addOnFailureListener(onFailure)
    }

    override suspend fun extractText(bitmap: Bitmap, useKorean: Boolean): OcrResult {
        val image = try { InputImage.fromBitmap(bitmap, 0) } catch (e: Exception) { throw e }
        val result = awaitTask(if (useKorean) koreanRecognizer.process(image) else defaultRecognizer.process(image))
        val blocks = result.textBlocks.map { b -> TextOcrBlock(b.text, b.cornerPoints?.toList() ?: emptyList()) }
        return OcrResult(blocks, bitmap.width, bitmap.height)
    }

    override fun setOcrResult(result: OcrResult?) {
        lastResult = result
        selectedIndex = null
    }

    override fun drawOverlay(
        drawScope: DrawScope,
        canvasSize: Size,
        style: DeepSkyBlueOverlayStyle
    ) {
        val result = lastResult ?: return
        val sx = if (result.imageWidth == 0) 1f else canvasSize.width / result.imageWidth
        val sy = if (result.imageHeight == 0) 1f else canvasSize.height / result.imageHeight
        val autoStroke = max(canvasSize.minDimension * 0.003f, 2f)
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = style.strokeWidthPx ?: autoStroke
        )

        val layerRect = androidx.compose.ui.geometry.Rect(0f, 0f, canvasSize.width, canvasSize.height)
        val overlayPaint = androidx.compose.ui.graphics.Paint().apply {
            color = style.maskColor.copy(alpha = style.maskAlpha)
        }
        val clearPaint = androidx.compose.ui.graphics.Paint().apply {
            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
        }

        drawScope.drawIntoCanvas { c ->
            c.saveLayer(layerRect, androidx.compose.ui.graphics.Paint())
            c.drawRect(layerRect, overlayPaint)
            result.blocks.forEach { b ->
                val pts = b.cornerPoints
                if (pts.size >= 4) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(pts[0].x * sx, pts[0].y * sy)
                        for (i in 1 until pts.size) lineTo(pts[i].x * sx, pts[i].y * sy)
                        close()
                    }
                    c.drawPath(path, clearPaint)
                } else if (pts.isNotEmpty()) {
                    val xs = pts.map { it.x * sx }
                    val ys = pts.map { it.y * sy }
                    val r = androidx.compose.ui.geometry.Rect(xs.min(), ys.min(), xs.max(), ys.max())
                    c.drawRect(r, clearPaint)
                }
            }
            c.restore()
        }

        val strokeColor = style.strokeColor.copy(alpha = style.strokeAlpha)
        result.blocks.forEach { b ->
            if (b.cornerPoints.size >= 4) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(b.cornerPoints[0].x * sx, b.cornerPoints[0].y * sy)
                    for (i in 1 until b.cornerPoints.size) lineTo(b.cornerPoints[i].x * sx, b.cornerPoints[i].y * sy)
                    close()
                }
                drawScope.drawPath(path = path, color = strokeColor, style = stroke)
            }
        }
    }



    override fun handleTouch(x: Float, y: Float): Boolean {
        val result = lastResult ?: return false
        val cw = result.imageWidth.toFloat()
        val ch = result.imageHeight.toFloat()
        if (cw <= 0f || ch <= 0f) return false
        val hit = result.blocks.indexOfFirst { b ->
            if (b.cornerPoints.isEmpty()) return@indexOfFirst false
            val xs = b.cornerPoints.map { it.x / cw }
            val ys = b.cornerPoints.map { it.y / ch }
            val minX = xs.min()
            val maxX = xs.max()
            val minY = ys.min()
            val maxY = ys.max()
            val nx = x / cw
            val ny = y / ch
            nx in minX..maxX && ny in minY..maxY
        }
        selectedIndex = if (hit >= 0) hit else null
        return hit >= 0
    }

    override fun getSelectedText(): String? =
        lastResult?.let { res ->
            val i = selectedIndex ?: return null
            res.blocks.getOrNull(i)?.text
        }


    override fun onTouchCanvas(x: Float, y: Float, canvasWidth: Float, canvasHeight: Float): Boolean {
        val res = lastResult ?: return false
        if (res.imageWidth <= 0 || res.imageHeight <= 0 || canvasWidth <= 0f || canvasHeight <= 0f) return false
        val ix = x * res.imageWidth / canvasWidth
        val iy = y * res.imageHeight / canvasHeight
        return handleTouch(ix, iy)
    }

    override fun getSelectedIndex(): Int? = selectedIndex

    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun apiKey(): String = runCatching {
        appContext.getString(R.string.openai_api_key)
    }.getOrDefault("")

    private fun modelOrDefault(m: String?): String = runCatching {
        val fromRes = appContext.getString(R.string.openai_model)
        val chosen = m ?: fromRes
        if (chosen.isBlank()) "gpt-4o-mini" else chosen
    }.getOrDefault("gpt-4o-mini")

    private suspend fun requestOpenAIAPI(task: String, text: String, model: String?, langHint: String?): String =
        withContext(Dispatchers.IO) {
            val k = apiKey()
            if (k.isBlank()) throw IllegalStateException("OPENAI_API_KEY가 비어 있습니다")
            val sys = if (task == "summarize") {
                buildString {
                    append("You are a concise summarizer. Summarize the text in max 3 to 4 sentences.")
                    append("If the text is Korean or langHint is 'ko', respond in Korean.")
                }
            } else if (task == "translate") {
                buildString {
                    append("Translate the text.")
                    append("If the text is English, translate to Korean. If the text is Korean, translate to English.")
                }
            } else ""

            val payload = JSONObject().apply {
                put("model", modelOrDefault(model))
                put("temperature", 0.2)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role","system").put("content", sys))
                    if (!langHint.isNullOrBlank()) put(JSONObject().put("role","user").put("content","langHint=$langHint"))
                    put(JSONObject().put("role","user").put("content","$task\n$text"))
                })
            }
            val req = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $k")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            try {
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        throw RuntimeException("OpenAI 오류 ${resp.code}: $body")
                    }
                    val body = resp.body?.string() ?: throw RuntimeException("빈 응답")
                    val root = JSONObject(body)
                    val content = root.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                        ?.trim()
                        .orEmpty()
                    if (content.isBlank()) throw RuntimeException("요약 결과가 비어 있습니다")
                    content
                }
            } catch (e: Exception) {
                throw RuntimeException("요약 호출 실패: ${e.message}", e)
            }
        }


    override suspend fun summarizeText(text: String, model: String?, langHint: String?): String =
        requestOpenAIAPI("summarize", text, model, langHint)

    override suspend fun summarizeAll(model: String?, langHint: String?): String? {
        val r = lastResult ?: return null
        if (r.blocks.isEmpty()) return ""
        val full = r.blocks.joinToString("\n") { it.text }
        return requestOpenAIAPI("summarize", full, model, langHint)
    }

    override suspend fun summarizeSelected(model: String?, langHint: String?): String? {
        val t = getSelectedText() ?: return null
        return requestOpenAIAPI("summarize", t, model, langHint)
    }

    override suspend fun translateSelected(model: String?, langHint: String?): String? {
        val t = getSelectedText() ?: return null
        return requestOpenAIAPI("translate", t, model, langHint)
    }


    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { cont ->
        task.addOnSuccessListener(OnSuccessListener { if (!cont.isCompleted) cont.resume(it) })
            .addOnFailureListener(OnFailureListener { e -> if (!cont.isCompleted) cont.cancel(e) })
            .addOnCanceledListener(OnCanceledListener { if (!cont.isCompleted) cont.cancel() })
    }
}
