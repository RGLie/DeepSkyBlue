package com.example.deepskyblue.model

/**
 * Structured OCR result including all blocks and the original image size.
 *
 * @param blocks List of recognized text blocks.
 * @param imageWidth Width of the source image in pixels.
 * @param imageHeight Height of the source image in pixels.
 * @since 0.1.0
 */
data class OcrResult(
    val blocks: List<TextOcrBlock>,
    val imageWidth: Int,
    val imageHeight: Int
)
