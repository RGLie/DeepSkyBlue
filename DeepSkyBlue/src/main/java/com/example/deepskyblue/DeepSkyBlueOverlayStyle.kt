package com.example.deepskyblue

import androidx.compose.ui.graphics.Color

/**
 * Visual style configuration for the OCR overlay.
 *
 * All alpha values are clamped within [0f, 1f] by the renderer.
 *
 * @param maskColor Color of the dimmed background mask.
 * @param maskAlpha Opacity of the background mask (0..1).
 * @param strokeColor Color of block outlines.
 * @param strokeAlpha Opacity of block outlines (0..1).
 * @param strokeWidthPx Optional stroke width; if null, auto-scaled.
 * @since 0.1.0
 */
data class DeepSkyBlueOverlayStyle(
    val maskColor: Color = Color.Black,
    val maskAlpha: Float = 0.5f,
    val strokeColor: Color = Color.White,
    val strokeAlpha: Float = 1f,
    val strokeWidthPx: Float? = null
)
