package com.example.deepskyblue

import androidx.compose.ui.graphics.Color

data class DeepSkyBlueOverlayStyle(
    val maskColor: Color = Color.Black,
    val maskAlpha: Float = 0.5f,
    val strokeColor: Color = Color.White,
    val strokeAlpha: Float = 1f,
    val strokeWidthPx: Float? = null
)
