package com.example.deepskyblue

import android.content.Context
import android.graphics.Bitmap
import com.example.deepskyblue.TextOcrBlock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

class DeepSkyBlueImpl(private val appContext: Context) : DeepSkyBlue {

    private val defaultRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val koreanRecognizer  = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

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
}
