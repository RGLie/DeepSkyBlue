package com.example.deepskyblue.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.deepskyblue.DeepSkyBlueProvider
import com.google.android.material.progressindicator.CircularProgressIndicator

@Composable
fun DeepSkyBluePreview(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    useKorean: Boolean = false,
    resultCallback: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val dsp      = remember { DeepSkyBlueProvider.with(context).getDeepSkyBlue() }
    var loading  by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (bitmap != null) {
                    loading = true
                    dsp.recognizeTextBlocks(
                        bitmap = bitmap,
                        useKorean = useKorean,
                        onSuccess = { blocks ->
                            loading = false
                            resultCallback(
                                blocks.joinToString("\n\n") { block ->
                                    val corners = block.cornerPoints.joinToString(", ") { p -> "(${p.x},${p.y})" }
                                    "Text: ${block.text}\nCorners: $corners"
                                }
                            )

                        },
                        onFailure = { e ->
                            loading = false
                            resultCallback("OCR 실패: ${e.message}")
                        }
                    )
                }
            },
            enabled = bitmap != null && !loading
        ) { Text("OCR 실행") }

        if (loading) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator()
        }
    }
}
