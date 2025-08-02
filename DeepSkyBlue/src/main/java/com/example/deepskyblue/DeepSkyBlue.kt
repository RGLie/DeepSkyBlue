package com.example.deepskyblue

import android.net.Uri
import com.google.mlkit.vision.text.Text

interface DeepSkyBlue {
    /**
     * public functions and interfaces that DeepSkyBlue provide
     */

    fun recognizeText(
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean
    )

    fun recognizeTextDetailed(
        uri: Uri,
        onSuccess: (Text) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean
    )

    fun recognizeTextBlocks(
        uri: Uri,
        onSuccess: (List<TextOcrBlock>) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean
    )
}
