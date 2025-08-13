package com.example.deepskyblue.model

import android.graphics.Point

/**
 * A single OCR block with text and its polygon corner points.
 *
 * Corner points are provided in image coordinates (pixels).
 *
 * @param text Raw text content of the block.
 * @param cornerPoints Polygon points describing the block boundary.
 * @since 0.1.0
 */
data class TextOcrBlock(
    val text: String,
    val cornerPoints: List<Point>
)
