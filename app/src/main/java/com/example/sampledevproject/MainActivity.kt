package com.example.sampledevproject

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.deepskyblue.DeepSkyBlueProvider
import com.example.sampledevproject.ui.theme.SampleDevProjectTheme
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntSize
import com.example.deepskyblue.DeepSkyBlueOverlayStyle
import com.example.deepskyblue.model.OcrResult
import com.example.deepskyblue.ui.DeepSkyBlueAction
import com.example.deepskyblue.ui.DeepSkyBlueOverlayBox
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SampleDevProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}


fun Uri.toBitmap(context: Context): Bitmap =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, this)
        ImageDecoder.decodeBitmap(source)
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, this)
    }


@Composable
fun MainScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val deepSkyBlue = remember { DeepSkyBlueProvider.with(context).getDeepSkyBlue() }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var ocrResult by remember { mutableStateOf<OcrResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var touchTick by remember { mutableStateOf(0) }
    var summaryText by remember { mutableStateOf("") }


    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        bitmap = uri?.toBitmap(context)
        ocrResult = null
        deepSkyBlue.setOcrResult(null)
    }

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    summaryText = ""
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("사진 1장 선택") }

                Button(
                    onClick = {
                        val bmp = bitmap ?: return@Button
                        scope.launch {
                            loading = true
                            try {
                                val res = deepSkyBlue.extractText(bmp, useKorean = true)
                                ocrResult = res
                                deepSkyBlue.setOcrResult(res)
                                val sum = deepSkyBlue.summarizeAll(langHint = "ko").orEmpty()
                                summaryText = if (sum.isBlank()) "요약 결과가 비어 있습니다." else "요약: " + sum
                            } catch (_: Exception) {
                                ocrResult = null
                                deepSkyBlue.setOcrResult(null)
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = bitmap != null && !loading
                ) { Text(if (loading) "처리 중..." else "OCR 실행") }
            }
        }

        // usage 1
//        item {
//            val bmp = bitmap
//            if (bmp != null) {
//                val aspect = if (bmp.height == 0) 1f else bmp.width.toFloat() / bmp.height
//                Canvas(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .aspectRatio(aspect)
//                ) {
//                    val a = ocrResult
//                    val img = bmp.asImageBitmap()
//                    val dstSize = IntSize(size.width.toInt(), size.height.toInt())
//                    drawImage(
//                        image = img,
//                        srcSize = IntSize(img.width, img.height),
//                        dstSize = dstSize
//                    )
//                    deepSkyBlue.drawOverlay(this, Size(size.width, size.height))
//                }
//            }
//        }

        // usage 2
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                DeepSkyBlueOverlayBox(
                    engine = deepSkyBlue,
                    result = ocrResult,
                    copyEnabled = true,
//                    overlayStyle = DeepSkyBlueOverlayStyle(
//                        maskAlpha = 0.2f,
//                        strokeColor = Color(0xFF00D8FF),
//                        strokeAlpha = 1f,
//                        strokeWidthPx = 3f
//                    ),
                    extraActions = listOf(
                        DeepSkyBlueAction("translate", "번역") { hit ->
                        }
                    )
                ) {
                    val bmp = bitmap
                    if (bmp != null) {
                        val aspect = if (bmp.height == 0) 1f else bmp.width.toFloat() / bmp.height
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspect)
                        ) {
                            val img = bmp.asImageBitmap()
                            val dstSize = IntSize(size.width.toInt(), size.height.toInt())
                            drawImage(
                                image = img,
                                srcSize = IntSize(img.width, img.height),
                                dstSize = dstSize
                            )
                        }
                    }
                }
            }

        }
        item { Spacer(Modifier.height(12.dp)) }

        item {
            Text(summaryText)
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            val text = ocrResult?.blocks?.joinToString("\n") { b ->
                "Text: ${b.text}\n"
            } ?: ""
            Text(text)
        }

        item {
            Text("blocks: ${ocrResult?.blocks?.size ?: 0}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SampleDevProjectTheme {
        MainScreen(modifier = Modifier.padding(16.dp))
    }
}
