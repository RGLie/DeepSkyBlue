// com.example.deepskyblue.ui/DeepSkyBlueOverlayBox.kt
package com.example.deepskyblue.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.deepskyblue.DeepSkyBlue
import com.example.deepskyblue.model.OcrResult
import androidx.compose.runtime.LaunchedEffect


data class DeepSkyBlueAction(
    val id: String,
    val label: String,
    val run: (hit: OcrMenuHit) -> Unit
)

data class OcrMenuHit(
    val text: String,
    val blockIndex: Int,
    val canvasX: Float,
    val canvasY: Float,
    val imageX: Float,
    val imageY: Float
)


@Composable
fun DeepSkyBlueOverlayBox(
    engine: DeepSkyBlue,
    result: OcrResult?,
    copyEnabled: Boolean = true,
    extraActions: List<DeepSkyBlueAction> = emptyList(),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val clipboard = LocalClipboardManager.current
    var menuVisible by remember { mutableStateOf(false) }
    var anchorX by remember { mutableStateOf(0.dp) }
    var anchorY by remember { mutableStateOf(0.dp) }
    val spacing = 12.dp

    var redrawTick by remember { mutableStateOf(0) }
    LaunchedEffect(result) { redrawTick++ }

    Box(modifier = modifier) {
        Box {
            content()
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(result) {
                        detectTapGestures { o ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val ok = engine.onTouchCanvas(o.x, o.y, w, h)
                            if (!ok) { menuVisible = false; return@detectTapGestures }
                            val xDp = with(density) { o.x.toDp() }
                            val yDp = with(density) { o.y.toDp() }
                            val xAdj = if (o.x > w * 0.7f) xDp - spacing else xDp + spacing
                            val yAdj = if (o.y > h * 0.7f) yDp - spacing else yDp + spacing
                            anchorX = xAdj
                            anchorY = yAdj
                            menuVisible = true
                        }
                    }
            ) {
                val a = redrawTick
                engine.drawOverlay(this, Size(size.width, size.height))
            }
        }

        Box(Modifier.fillMaxSize().zIndex(1f)) {
            Box(Modifier.offset(anchorX, anchorY).size(1.dp)) {
                DropdownMenu(
                    expanded = menuVisible,
                    onDismissRequest = { menuVisible = false }
                ) {
                    if (copyEnabled) {
                        DropdownMenuItem(
                            text = { Text("복사") },
                            onClick = {
                                engine.getSelectedText()?.let { clipboard.setText(AnnotatedString(it)) }
                                menuVisible = false
                            }
                        )
                    }
                    extraActions.forEach { (_, label, run) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                val idx = engine.getSelectedIndex()
                                val txt = engine.getSelectedText()
                                if (idx != null && txt != null) run(OcrMenuHit(txt, idx, 0f, 0f, 0f, 0f))
                                menuVisible = false
                            }
                        )
                    }
                }
            }
        }
    }
}
