package com.example.netraguide

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.netraguide.views.ObjectDetectionAnalyzer
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun CameraPreviewScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State untuk teks di layar
    var detectedObjectText by remember { mutableStateOf("Mencari benda...") }

    // --- 1. Persiapan Text-to-Speech (TTS) ---
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    // Variabel "Rem" agar tidak spam suara
    var lastSpokenText by remember { mutableStateOf("") }
    var lastSpokenTime by remember { mutableLongStateOf(0L) }

    // Inisialisasi TTS saat layar dibuka
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set Bahasa ke Indonesia (atau Inggris jika gagal)
                val result = tts?.setLanguage(Locale("id", "ID"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("NetraGuide", "Bahasa Indonesia tidak didukung, pakai Inggris")
                    tts?.language = Locale.US
                }
            }
        }
        // Matikan TTS saat keluar aplikasi agar hemat memori
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Fungsi Pembantu untuk Bicara
    fun speakObject(label: String) {
        val currentTime = System.currentTimeMillis()
        // Syarat bicara: Benda beda ATAU sudah lebih dari 3 detik
        if (label != lastSpokenText || (currentTime - lastSpokenTime) > 3000) {
            tts?.speak(label, TextToSpeech.QUEUE_FLUSH, null, null)
            lastSpokenText = label
            lastSpokenTime = currentTime
        }
    }

    // --- 2. Tampilan UI ---
    Box(modifier = Modifier.fillMaxSize()) {
        // Kamera
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_START
                }
            },
            update = { previewView ->
                startCamera(context, lifecycleOwner, previewView) { label ->
                    // Callback saat benda ditemukan:
                    detectedObjectText = label // 1. Update Teks Layar
                    speakObject(label)         // 2. Keluarkan Suara
                }
            }
        )

        // Overlay Teks (Agar mudah dibaca developer/pengguna awas)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .background(Color.Black.copy(alpha = 0.6f)) // Background semi-transparan
                .padding(16.dp)
        ) {
            Text(
                text = detectedObjectText,
                color = Color.White,
                fontSize = 24.sp
            )
        }
    }
}

// --- 3. Logika Kamera & Analyzer ---
private fun startCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    onObjectFound: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(
                    Executors.newSingleThreadExecutor(),
                    // UBAH BARIS INI (Tambahkan context):
                    ObjectDetectionAnalyzer(context) { label ->
                        onObjectFound(label)
                    }
                )
            }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e("NetraGuide", "Gagal bind kamera", exc)
        }

    }, ContextCompat.getMainExecutor(context))
}