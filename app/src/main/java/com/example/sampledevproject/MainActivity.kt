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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import com.example.deepskyblue.model.OcrResult
import com.example.deepskyblue.ui.DeepSkyBlueImageView
import com.example.deepskyblue.ui.DeepSkyBluePreview
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


        item { Spacer(Modifier.height(12.dp)) }

        item {
            val bmp = bitmap
            if (bmp != null) {
                val aspect = if (bmp.height == 0) 1f else bmp.width.toFloat() / bmp.height
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspect)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                deepSkyBlue.handleTouch(offset.x, offset.y)
                            }
                        }
                ) {
                    val a = ocrResult // to add dependency

                    val img = bmp.asImageBitmap()
                    val dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    drawImage(
                        image = img,
                        srcSize = IntSize(img.width, img.height),
                        dstSize = dstSize
                    )
                    deepSkyBlue.drawOverlay(this, Size(size.width, size.height))
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            val text = ocrResult?.blocks?.joinToString("\n\n") { b ->
                val c = b.cornerPoints.joinToString(", ") { p -> "(${p.x},${p.y})" }
                "Text: ${b.text}\nCorners: $c"
            } ?: ""
            Text(text)
        }

        item {
            Text("blocks: ${ocrResult?.blocks?.size ?: 0}")
        }
//        item {
//            DeepSkyBlueImageView(
//                bitmap = bitmap,
//                useKorean = true,
//                resultCallback = { result -> ocrResult = result },
//                modifier = Modifier.fillMaxWidth()
//            )
//        }
//
//        item { Spacer(Modifier.height(15.dp)) }
//        item { Text(text = ocrResult) }


    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SampleDevProjectTheme {
        MainScreen(modifier = Modifier.padding(16.dp))
    }
}
