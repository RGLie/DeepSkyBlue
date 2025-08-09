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
import kotlinx.coroutines.suspendCancellableCoroutine
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

    override fun drawOverlay(drawScope: DrawScope, canvasSize: Size) {
        val result = lastResult ?: return
        val sx = if (result.imageWidth == 0) 1f else canvasSize.width / result.imageWidth
        val sy = if (result.imageHeight == 0) 1f else canvasSize.height / result.imageHeight
        val stroke = Stroke(width = max(canvasSize.minDimension * 0.003f, 2f))

        val layerRect = androidx.compose.ui.geometry.Rect(0f, 0f, canvasSize.width, canvasSize.height)
        val overlayPaint = androidx.compose.ui.graphics.Paint().apply { color = Color.Black.copy(alpha = 0.5f) }
        val clearPaint = androidx.compose.ui.graphics.Paint().apply { blendMode = androidx.compose.ui.graphics.BlendMode.Clear }

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

        result.blocks.forEachIndexed { idx, b ->
            if (b.cornerPoints.size >= 4) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(b.cornerPoints[0].x * sx, b.cornerPoints[0].y * sy)
                    for (i in 1 until b.cornerPoints.size) lineTo(b.cornerPoints[i].x * sx, b.cornerPoints[i].y * sy)
                    close()
                }
                val sel = selectedIndex == idx
                val color = if (sel) Color(0xFF00C853) else Color.White.copy(alpha = 1f)
                drawScope.drawPath(path = path, color = color, style = stroke)
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

    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { cont ->
        task.addOnSuccessListener(OnSuccessListener { if (!cont.isCompleted) cont.resume(it) })
            .addOnFailureListener(OnFailureListener { e -> if (!cont.isCompleted) cont.cancel(e) })
            .addOnCanceledListener(OnCanceledListener { if (!cont.isCompleted) cont.cancel() })
    }
}
