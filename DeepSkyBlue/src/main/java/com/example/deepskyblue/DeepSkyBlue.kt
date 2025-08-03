package com.example.deepskyblue

import android.graphics.Bitmap
import android.net.Uri
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
}
