// com.example.deepskyblue.ui/DeepSkyBlueOverlayBox.kt
package com.example.deepskyblue.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.deepskyblue.DeepSkyBlue
import com.example.deepskyblue.model.OcrResult
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch


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

    val scope = rememberCoroutineScope()
    var dialogVisible by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf("요약 중…") }
    val scroll = rememberScrollState()

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
                        DropdownMenuItem(text = { Text("복사") }, onClick = {
                            engine.getSelectedText()?.let { clipboard.setText(AnnotatedString(it)) }
                            menuVisible = false
                        })
                    }
                    DropdownMenuItem(text = { Text("요약") }, onClick = {
                        menuVisible = false
                        dialogVisible = true
                        dialogText = "요약 중…"
                        scope.launch {
                            try {
                                val sum = engine.summarizeSelected(langHint = "ko")
                                dialogText = when {
                                    sum.isNullOrBlank() -> "요약할 텍스트가 없습니다."
                                    else -> sum
                                }
                            } catch (e: Exception) {
                                dialogText = "요약 실패: ${e.message}"
                            }
                        }
                    })
                    DropdownMenuItem(text = { Text("번역") }, onClick = {
                        menuVisible = false
                        dialogVisible = true
                        dialogText = "번역 중…"
                        scope.launch {
                            try {
                                val sum = engine.translateSelected(langHint = "ko")
                                dialogText = when {
                                    sum.isNullOrBlank() -> "번역할 텍스트가 없습니다."
                                    else -> sum
                                }
                            } catch (e: Exception) {
                                dialogText = "번역 실패: ${e.message}"
                            }
                        }
                    })
                }
            }
        }
    }

    if (dialogVisible) {
        AlertDialog(
            onDismissRequest = { dialogVisible = false },
            title = { Text("요약") },
            text = {
                Box(Modifier.heightIn(max = 320.dp).verticalScroll(scroll)) {
                    Text(dialogText)
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboard.setText(AnnotatedString(dialogText))
                    dialogVisible = false
                }) { Text("복사") }
            },
            dismissButton = {
                Button(onClick = { dialogVisible = false }) { Text("닫기") }
            }
        )
    }
}

