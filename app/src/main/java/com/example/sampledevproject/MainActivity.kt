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

}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SampleDevProjectTheme {
        MainScreen(modifier = Modifier.padding(16.dp))
    }
}
