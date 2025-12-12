package com.example.netraguide.views
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetectionAnalyzer(
    private val context: Context,
    private val onObjectDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Detektor dari TensorFlow Lite Task Library
    private var detector: ObjectDetector? = null

    init {
        setupDetector()
    }

    private fun setupDetector() {
        try {
            // Konfigurasi Detektor
            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(1)          // Hanya ambil 1 benda terjelas
                .setScoreThreshold(0.3f)   // Yakin minimal 30%
                .build()

            // Muat model dari file assets (pastikan nama file benar: model.tflite)
            detector = ObjectDetector.createFromFileAndOptions(
                context,
                "model.tflite",
                options
            )
            Log.d("NetraGuide", "Detektor Berhasil Dimuat!")
        } catch (e: Exception) {
            Log.e("NetraGuide", "Gagal memuat detektor: ${e.message}")
        }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (detector == null) {
            imageProxy.close()
            return
        }

        try {
            // 1. Ubah Gambar Kamera menjadi Bitmap (Format yang disukai TFLite)
            val bitmapBuffer = imageProxy.toBitmap()

            // 2. Masukkan ke TensorImage
            val image = TensorImage.fromBitmap(bitmapBuffer)

            // 3. Deteksi!
            val results = detector?.detect(image)

            // 4. Proses Hasil
            if (!results.isNullOrEmpty()) {
                val firstObj = results[0]
                val label = firstObj.categories.firstOrNull()?.label ?: "Benda Asing"
                val score = firstObj.categories.firstOrNull()?.score ?: 0f

                Log.d("NetraGuide", "Ketemu: $label (${(score * 100).toInt()}%)")
                onObjectDetected(label)
            }

        } catch (e: Exception) {
            Log.e("NetraGuide", "Error saat analisa: ${e.message}")
        } finally {
            imageProxy.close() // Wajib tutup agar tidak macet
        }
    }
}