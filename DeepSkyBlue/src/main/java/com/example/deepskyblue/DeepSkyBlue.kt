package com.example.deepskyblue

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.deepskyblue.model.OcrResult
import com.example.deepskyblue.model.TextOcrBlock
import com.google.mlkit.vision.text.Text

/**
 * Main interface for OCR, overlay rendering, selection handling,
 * and LLM-powered utilities (summarization/translation).
 *
 * This interface abstracts the core features so that different
 * implementations can be plugged in without changing UI code.
 *
 * Thread-safety: unless otherwise noted, methods are not thread-safe.
 *
 * @since 0.1.0
 */
interface DeepSkyBlue {

    /**
     * Performs OCR on a bitmap and returns the full recognized text.
     *
     * Runs ML Kit recognition internally. This is a non-suspending,
     * callback-based variant suitable for UI interactions.
     *
     * @param bitmap Source image to recognize.
     * @param onSuccess Callback invoked with the recognized full text.
     * @param onFailure Callback invoked with the thrown exception.
     * @param useKorean Whether to force Korean recognizer.
     */
    fun recognizeText(
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean = false
    )

    /**
     * Performs OCR and returns ML Kit's detailed [Text] object.
     *
     * @param bitmap Source image.
     * @param onSuccess Callback with ML Kit [Text] result.
     * @param onFailure Callback with an error.
     * @param useKorean Whether to force Korean recognizer.
     */
    fun recognizeTextDetailed(
        bitmap: Bitmap,
        onSuccess: (Text) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean = false
    )

    /**
     * Performs OCR and returns block-level results.
     *
     * Each block includes its text and corner points.
     *
     * @param bitmap Source image.
     * @param onSuccess Callback with list of [TextOcrBlock].
     * @param onFailure Callback with an error.
     * @param useKorean Whether to force Korean recognizer.
     */
    fun recognizeTextBlocks(
        bitmap: Bitmap,
        onSuccess: (List<TextOcrBlock>) -> Unit,
        onFailure: (Exception) -> Unit,
        useKorean: Boolean = false
    )

    /**
     * Suspends while extracting OCR result into a structured [OcrResult].
     *
     * @param bitmap Source image.
     * @param useKorean Whether to force Korean recognizer.
     * @return Structured OCR result with blocks and image size.
     */
    suspend fun extractText(
        bitmap: Bitmap,
        useKorean: Boolean = false
    ): OcrResult

    /**
     * Sets the current OCR result used by overlay and selection logic.
     *
     * Passing null clears the current state.
     *
     * @param result OCR result or null to clear.
     */
    fun setOcrResult(result: OcrResult?)

    /**
     * Draws the overlay (mask and block outlines) on the given [DrawScope].
     *
     * Scaling is automatically computed from [canvasSize] and image size.
     *
     * @param drawScope Compose draw scope.
     * @param canvasSize Canvas size in pixels.
     * @param style Visual style for overlay.
     */
    fun drawOverlay(
        drawScope: DrawScope,
        canvasSize: Size,
        style: DeepSkyBlueOverlayStyle = DeepSkyBlueOverlayStyle()
    )

    /**
     * Handles a hit test in image coordinates (pixels).
     *
     * Selects a block if the point falls within it.
     *
     * @param x X in image pixels.
     * @param y Y in image pixels.
     * @return true if a block was selected; false otherwise.
     */
    fun handleTouch(x: Float, y: Float): Boolean

    /**
     * Returns the currently selected block's text, if any.
     */
    fun getSelectedText(): String?

    /**
     * Returns the currently selected block index, if any.
     */
    fun getSelectedIndex(): Int?

    /**
     * Hit test with canvas coordinates (as displayed on screen).
     *
     * Converts the canvas touch to image coordinates internally and
     * delegates to [handleTouch].
     *
     * @param x X on canvas.
     * @param y Y on canvas.
     * @param canvasWidth Canvas width in pixels.
     * @param canvasHeight Canvas height in pixels.
     * @return true if a block was selected; false otherwise.
     */
    fun onTouchCanvas(x: Float, y: Float, canvasWidth: Float, canvasHeight: Float): Boolean

    /**
     * Summarizes arbitrary text via an LLM.
     *
     * @param text Input text to summarize.
     * @param model Optional model name; implementation may pick defaults.
     * @param langHint Optional language hint (e.g., "ko").
     * @return Summarized text.
     */
    suspend fun summarizeText(text: String, model: String? = null, langHint: String? = null): String

    /**
     * Summarizes the entire recognized text (all blocks), if available.
     *
     * @param model Optional model name.
     * @param langHint Optional language hint.
     * @return Summary or null if no OCR result exists.
     */
    suspend fun summarizeAll(model: String? = null, langHint: String? = null): String?

    /**
     * Summarizes the currently selected block's text, if any.
     *
     * @param model Optional model name.
     * @param langHint Optional language hint.
     * @return Summary or null if no selection exists.
     */
    suspend fun summarizeSelected(model: String? = null, langHint: String? = null): String?

    /**
     * Translates the currently selected block's text, if any.
     *
     * Direction is implementation-defined (e.g., EN<->KO).
     *
     * @param model Optional model name.
     * @param langHint Optional language hint.
     * @return Translation or null if no selection exists.
     */
    suspend fun translateSelected(model: String? = null, langHint: String? = null): String?
}
