package com.example.sampledevproject

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.deepskyblue.DeepSkyBlueProvider
import com.example.sampledevproject.ui.theme.SampleDevProjectTheme

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

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var ocrResult by remember { mutableStateOf("") }
    val context = LocalContext.current
    val dsp = remember { DeepSkyBlueProvider.with(context).getDeepSkyBlue() }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        item {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(onClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }) { Text("사진 1장 선택") }
        }

        item {
            Button(onClick = {
                val uri = selectedImageUri
                if (uri != null) {
                    // TODO: uri null exception in DeepSkyBlue
                    dsp.recognizeTextBlocks(
                        uri = uri,
                        useKorean = true,
                        onSuccess = { blocks ->
                            ocrResult = blocks.joinToString("\n\n") { block ->
                                val corners = block.cornerPoints.joinToString(", ") { p -> "(${p.x},${p.y})" }
                                "Text: ${block.text}\nCorners: $corners"
                            }
                        },
                        onFailure = { e ->
                            ocrResult = "OCR Error: ${e.message}"
                        }
                    )
                } else {
                    ocrResult = "이미지를 먼저 선택하세요."
                }
            }) { Text("텍스트 인식") }
        }

        item { Spacer(Modifier.height(15.dp)) }
        item { Text(text = ocrResult) }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SampleDevProjectTheme {
        MainScreen(modifier = Modifier.padding(16.dp))
    }
}
