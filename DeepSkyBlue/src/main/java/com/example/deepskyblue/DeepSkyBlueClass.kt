package com.example.deepskyblue

import android.content.Context
import android.net.Uri
import com.example.deepskyblue.model.TextOcrBlock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

class DeepSkyBlueClass {
    fun getMessage(): String = "Hello from DeepSkyBlue!!!!"
}

class DeepSkyBlueTextRecognitionProcessor(
    private val useKorean: Boolean = false
) {
    private val recognizer = if (useKorean) {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    } else {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun processImage(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            onFailure(e)
            return
        }

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onSuccess(visionText.text)
            }
            .addOnFailureListener(onFailure)
    }

    fun processImageDetailed(
        context: Context,
        uri: Uri,
        onSuccess: (visionText: Text) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            onFailure(e)
            return
        }

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onSuccess(visionText)
            }
            .addOnFailureListener(onFailure)
    }
    fun processImageAsList(
        context: Context,
        uri: Uri,
        onSuccess: (List<TextOcrBlock>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            onFailure(e)
            return
        }

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val results = mutableListOf<TextOcrBlock>()

//                for (block in visionText.textBlocks) {
//                    results.add(block.text)
//                    val blockText = block.text
//                    val blockCornerPoints = block.cornerPoints
//                    val blockFrame = block.boundingBox
////                    for (line in block.lines) {
////                        results.add(line.text)
////                        for (element in line.elements) {
////                            results.add(element.text)
////                        }
////                    }
                for (block in visionText.textBlocks) {
                    // convert the cornerPoints array (Point[]) into a List<Point>
                    val corners = block.cornerPoints?.toList() ?: emptyList()

                    results += TextOcrBlock(
                        text = block.text,
                        cornerPoints = corners
                    )
                }

                onSuccess(results)
            }
            .addOnFailureListener(onFailure)
    }


}
