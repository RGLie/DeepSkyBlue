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
import com.example.deepskyblue.DeepSkyBlueOverlayStyle
import kotlinx.coroutines.launch



/**
 * Represents an action shown in the context menu for a selected OCR block.
 *
 * @param id Stable identifier for the action.
 * @param label UI label to display.
 * @param run Callback invoked with the [OcrMenuHit] when selected.
 * @since 0.1.0
 */
data class DeepSkyBlueAction(
    val id: String,
    val label: String,
    val run: (hit: OcrMenuHit) -> Unit
)

/**
 * Context of a menu invocation, including text, block index,
 * and both canvas and image coordinates at the click point.
 *
 * Coordinates:
 * - canvasX/canvasY: as drawn on the UI canvas (pixels)
 * - imageX/imageY: in original image coordinates (pixels)
 *
 * @param text Text content of the selected block.
 * @param blockIndex Index of the selected block within [OcrResult.blocks].
 * @param canvasX X on canvas at menu invocation.
 * @param canvasY Y on canvas at menu invocation.
 * @param imageX X in original image pixels at menu invocation.
 * @param imageY Y in original image pixels at menu invocation.
 * @since 0.1.0
 */
data class OcrMenuHit(
    val text: String,
    val blockIndex: Int,
    val canvasX: Float,
    val canvasY: Float,
    val imageX: Float,
    val imageY: Float
)

/**
 * Composable overlay that draws OCR masks/outlines on top of your content,
 * handles taps to select blocks, and shows a contextual menu with built‑in
 * actions (copy, summarize, translate) plus optional [extraActions].
 *
 * Typical usage:
 * ```
 * DeepSkyBlueOverlayBox(
 *   engine = engine,
 *   result = ocrResult,
 *   modifier = Modifier.fillMaxWidth()
 * ) {
 *   Image(bitmap = imageBitmap, contentDescription = null)
 * }
 * ```
 *
 * The composable calls [engine.onTouchCanvas] to update selection,
 * and then renders the overlay via [engine.drawOverlay].
 *
 * @param engine OCR/overlay engine implementing [DeepSkyBlue].
 * @param result Current OCR result; when changed, overlay is redrawn.
 * @param copyEnabled Whether the "Copy" default action is shown.
 * @param extraActions Additional actions appended to the menu.
 * @param overlayStyle Visual style for overlay rendering.
 * @param modifier Compose modifier for the overlay container.
 * @param content Underlay content (usually your image).
 * @since 0.1.0
 */
@Composable
fun DeepSkyBlueOverlayBox(
    engine: DeepSkyBlue,
    result: OcrResult?,
    copyEnabled: Boolean = true,
    extraActions: List<DeepSkyBlueAction> = emptyList(),
    overlayStyle: DeepSkyBlueOverlayStyle = DeepSkyBlueOverlayStyle(),
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
    var dialogTitle by remember { mutableStateOf("결과") }   // 추가: 작업별 제목 상태
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
                engine.drawOverlay(this, Size(size.width, size.height), overlayStyle)
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
                        dialogTitle = "요약"
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
                        dialogTitle = "번역"
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
            title = { Text(dialogTitle) },
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

