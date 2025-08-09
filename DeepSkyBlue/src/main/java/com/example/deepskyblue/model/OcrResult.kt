package com.example.deepskyblue.model

data class OcrResult(
    val blocks: List<TextOcrBlock>,
    val imageWidth: Int,
    val imageHeight: Int
)