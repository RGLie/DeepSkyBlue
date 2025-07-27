package com.example.sampledevproject

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.core.net.toUri
import com.example.deepskyblue.DeepSkyBlueTextRecognitionProcessor
import com.example.deepskyblue.TextOcrBlock
import com.example.sampledevproject.ui.theme.SampleDevProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SampleDevProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var ocrResult by remember { mutableStateOf("Processing...") }
    val context = LocalContext.current

    // 리소스 URI를 내부에서 생성
    val imageUri = "android.resource://${context.packageName}/${R.drawable.test_image_2}".toUri()

    // 이미지 URI가 변경될 때마다 한 번만 OCR 실행
    LaunchedEffect(imageUri) {
        DeepSkyBlueTextRecognitionProcessor(useKorean = true)
            .processImageAsList(
                context = context,
                uri = imageUri,
                onSuccess = { blocks: List<TextOcrBlock> ->
                    ocrResult = blocks.joinToString("\n\n") { block ->
                        val corners = block.cornerPoints
                            .joinToString(", ") { p -> "(${p.x},${p.y})" }

                        "Text: ${block.text}\nCorners: $corners\n"
                    }
                },
                onFailure = { error ->
                    ocrResult = "OCR Error: \${error.message}"
                }
            )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = ocrResult)
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SampleDevProjectTheme {
        MainScreen(modifier = Modifier.padding(16.dp))
    }
}
