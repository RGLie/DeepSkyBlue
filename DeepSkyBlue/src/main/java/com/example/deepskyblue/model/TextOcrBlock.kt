package com.example.deepskyblue.model

import android.graphics.Point

data class TextOcrBlock(
    val text: String,
    val cornerPoints: List<Point>
)
