package com.example.netraguide

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.netraguide.views.ObjectDetectionAnalyzer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // State untuk cek apakah izin kamera sudah diberikan
            var hasCameraPermission by remember { mutableStateOf(false) }

            // Launcher untuk meminta izin
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { granted ->
                    hasCameraPermission = granted
                }
            )

            // Meminta izin saat aplikasi pertama dibuka
            LaunchedEffect(Unit) {
                launcher.launch(Manifest.permission.CAMERA)
            }

            if (hasCameraPermission) {
                // JIKA DIIZINKAN: Tampilkan Kamera
                CameraPreviewScreen()


            } else {
                // JIKA DITOLAK: Tampilkan pesan
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aplikasi butuh izin kamera untuk bekerja")
                }
            }
        }
    }
}