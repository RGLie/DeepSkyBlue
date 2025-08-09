package com.example.deepskyblue

import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.deepskyblue.model.OcrResult
import com.example.deepskyblue.model.TextOcrBlock
import com.google.mlkit.vision.text.Text

interface DeepSkyBlue {
    /**
     * public functions and interfaces that DeepSkyBlue provide
     */

    fun recognizeText(
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean = false
    )

    fun recognizeTextDetailed(
        bitmap: Bitmap,
        onSuccess: (Text) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean = false
    )

    fun recognizeTextBlocks(
        bitmap: Bitmap,
        onSuccess: (List<TextOcrBlock>) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean = false
    )

    suspend fun extractText(
        bitmap: Bitmap,
        useKorean: Boolean = false
    ): OcrResult

    fun setOcrResult(result: OcrResult?)

    fun drawOverlay(
        drawScope: DrawScope,
        canvasSize: Size
    )

    fun handleTouch(x: Float, y: Float): Boolean
}
