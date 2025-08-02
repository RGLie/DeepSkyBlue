package com.example.deepskyblue

import android.content.Context
import android.net.Uri
import com.example.deepskyblue.TextOcrBlock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

class DeepSkyBlueImpl(private val appContext: Context) : DeepSkyBlue {

    private val defaultRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val koreanRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    private fun loadImage(uri: Uri, onFailure: (Exception) -> Unit): InputImage? =
        try { InputImage.fromFilePath(appContext, uri) } catch (e: Exception) { onFailure(e); null }

    private fun recognizer(useKorean: Boolean) =
        if (useKorean) koreanRecognizer else defaultRecognizer

    override fun recognizeText(
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean
    ) {
        val image = loadImage(uri, onFailure) ?: return
        recognizer(useKorean).process(image)
            .addOnSuccessListener { onSuccess(it.text) }
            .addOnFailureListener(onFailure)
    }

    override fun recognizeTextDetailed(
        uri: Uri,
        onSuccess: (Text) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean
    ) {
        val image = loadImage(uri, onFailure) ?: return
        recognizer(useKorean).process(image)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
    }

    override fun recognizeTextBlocks(
        uri: Uri,
        onSuccess: (List<TextOcrBlock>) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean
    ) {
        val image = loadImage(uri, onFailure) ?: return
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
