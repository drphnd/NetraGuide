package com.example.netraguide.views

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.Locale
import java.util.concurrent.Executors

// Deklarasi Variabel TTS di luar fungsi agar aman
private var lastSpokenText = ""
private var lastSpokenTime = 0L

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State Koordinat
    var detectedObjects by remember { mutableStateOf<List<DetectionResult>>(emptyList()) }

    // Setup TTS
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts?.language = Locale("id", "ID")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. LAYAR KAMERA (ANTI-LOOPING)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // Factory hanya jalan 1 KALI saat aplikasi dibuka
                val previewView = PreviewView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER // Ubah ke FILL_CENTER agar rasio pas
                }

                // Start Kamera di sini (DIJAMIN SEKALI)
                startCamera(ctx, lifecycleOwner, previewView) { results ->
                    // Update UI (Garis Merah)
                    detectedObjects = results

                    // Update Suara
                    if (results.isNotEmpty()) {
                        speakObject(tts, results[0].text)
                    }
                }

                previewView
            },
            // Update dikosongkan agar Compose tidak merestart kamera saat garis merah berubah
            update = { }
        )

        // 2. LAYAR GARIS MERAH
        DetectionOverlay(detectedObjects)
    }
}

@Composable
fun DetectionOverlay(objects: List<DetectionResult>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 50f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(10f, 0f, 0f, android.graphics.Color.BLACK)
        }

        objects.forEach { obj ->
            // Scale dari 0-1 ke Ukuran Layar HP
            val left = obj.boundingBox.left * w
            val top = obj.boundingBox.top * h
            val right = obj.boundingBox.right * w
            val bottom = obj.boundingBox.bottom * h

            // Gambar Kotak
            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 8f)
            )

            // Gambar Teks
            drawContext.canvas.nativeCanvas.drawText(
                obj.text,
                left,
                top - 20f,
                paint
            )
        }
    }
}

// Fungsi Helper untuk start kamera
private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onResult: (List<DetectionResult>) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(),
                ObjectDetectionAnalyzer(context, onResult))

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
            Log.d("NetraGuide", "Kamera Start Sukses!")
        } catch (e: Exception) {
            Log.e("NetraGuide", "Kamera Error: ${e.message}")
        }
    }, ContextCompat.getMainExecutor(context))
}

// Fungsi Helper TTS
fun speakObject(tts: TextToSpeech?, text: String) {
    val currentTime = System.currentTimeMillis()
    val labelOnly = text.split(" ").firstOrNull() ?: text

    // Jeda 3 detik agar tidak berisik
    if (labelOnly != lastSpokenText || (currentTime - lastSpokenTime) > 3000) {
        tts?.speak(labelOnly, TextToSpeech.QUEUE_FLUSH, null, null)
        lastSpokenText = labelOnly
        lastSpokenTime = currentTime
    }
}