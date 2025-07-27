package com.example.deepskyblue

import android.content.Context
import android.graphics.Point
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

data class TextOcrBlock(
    val text: String,
    val cornerPoints: List<Point>,   // or Array<Point>, your call
)


class DeepSkyBlueClass {
    fun getMessage(): String = "Hello from DeepSkyBlue!!!!"
}

/**
 * ML Kit 기반 OCR 처리기
 * @param useKorean 한글 인식기를 사용할지 여부
 */
class DeepSkyBlueTextRecognitionProcessor(
    private val useKorean: Boolean = false
) {
    private val recognizer = if (useKorean) {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    } else {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * 이미지 Uri로부터 텍스트를 인식하고 콜백 반환
     * @param context Context
     * @param uri 이미지 파일 Uri
     * @param onSuccess 인식된 전체 텍스트
     * @param onFailure 예외 발생 시
     */
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
//                    // 블록 내 라인 단위
////                    for (line in block.lines) {
////                        results.add(line.text)
////                        // 라인 내 요소 단위
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
