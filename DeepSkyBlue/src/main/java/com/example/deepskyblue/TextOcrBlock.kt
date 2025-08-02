package com.example.deepskyblue

import android.graphics.Point

data class TextOcrBlock(
    val text: String,
    val cornerPoints: List<Point>
)
