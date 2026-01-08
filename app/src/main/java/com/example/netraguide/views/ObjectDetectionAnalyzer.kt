package com.example.netraguide.views

import android.content.Context
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

data class DetectionResult(
    val boundingBox: RectF,
    val text: String
)

class ObjectDetectionAnalyzer(
    private val context: Context,
    private val onObjectDetected: (List<DetectionResult>) -> Unit
) : ImageAnalysis.Analyzer {

    private var interpreter: Interpreter? = null
    private var labels = listOf<String>()

    
    private val inputSize = 640
    private val confThreshold = 0.40f

    private var lastTimeStamp = 0L
    private val updateInterval = 100L

    init {
        setupInterpreter()
    }

    private fun setupInterpreter() {
        try {
            labels = getCocoLabels()
            val options = Interpreter.Options()

            try {
                val compatList = CompatibilityList()
                if (compatList.isDelegateSupportedOnThisDevice) {
                    val delegateOptions = compatList.bestOptionsForThisDevice
                    options.addDelegate(GpuDelegate(delegateOptions))
                } else {
                    options.setNumThreads(4)
                }
            } catch (e: Exception) {
                options.setNumThreads(4)
            }

            val modelFile = FileUtil.loadMappedFile(context, "model.tflite")
            interpreter = Interpreter(modelFile, options)
        } catch (e: Exception) {
            Log.e("NetraGuide", "Error setup model: ${e.message}")
        }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (interpreter == null) {
            imageProxy.close()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTimeStamp < updateInterval) {
            imageProxy.close()
            return
        }
        lastTimeStamp = currentTime

        try {
            // 1. Ambil Rotasi Gambar dari Kamera (PENTING!)
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toBitmap()

            // 2. Preprocess Image (Putar -> Resize -> Normalisasi)
            val imageProcessor = ImageProcessor.Builder()
                // Langkah A: Putar gambar agar TEGAK lurus sesuai cara kita pegang HP
                .add(Rot90Op(-rotationDegrees / 90))

                // Langkah B: Resize ke 640x640 (Model input)
                .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))

                // Langkah C: Normalisasi (0-255 -> 0-1 Float)
                .add(NormalizeOp(0f, 255f))
                .build()

            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // 3. Output Buffer
            val outputShape = interpreter!!.getOutputTensor(0).shape() // [1, 84, 8400]
            val outputBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)

            // 4. Inference
            interpreter?.run(tensorImage.buffer, outputBuffer.buffer.rewind())

            // 5. Post Processing
            val outputArray = outputBuffer.floatArray
            val numRows = outputShape[1] // 84
            val numCols = outputShape[2] // 8400

            val detectedObjects = ArrayList<DetectionResult>()

            for (col in 0 until numCols) {
                var maxClassScore = 0f
                var maxClassIndex = -1

                for (row in 4 until numRows) {
                    val score = outputArray[row * numCols + col]
                    if (score > maxClassScore) {
                        maxClassScore = score
                        maxClassIndex = row - 4
                    }
                }

                if (maxClassScore > confThreshold) {
                    var cx = outputArray[0 * numCols + col]
                    var cy = outputArray[1 * numCols + col]
                    var w = outputArray[2 * numCols + col]
                    var h = outputArray[3 * numCols + col]

                    // Normalisasi Koordinat (Jika pixel, bagi 640)
                    if (cx > 1.0f) cx /= inputSize
                    if (cy > 1.0f) cy /= inputSize
                    if (w > 1.0f)  w /= inputSize
                    if (h > 1.0f)  h /= inputSize

                    val left = (cx - w / 2).coerceIn(0f, 1f)
                    val top = (cy - h / 2).coerceIn(0f, 1f)
                    val right = (cx + w / 2).coerceIn(0f, 1f)
                    val bottom = (cy + h / 2).coerceIn(0f, 1f)

                    val rect = RectF(left, top, right, bottom)

                    val labelName = if (maxClassIndex in labels.indices) labels[maxClassIndex] else "Unknown"
                    val percentage = (maxClassScore * 100).toInt()

                    detectedObjects.add(DetectionResult(rect, "$labelName $percentage%"))
                }
            }

            // Urutkan berdasarkan akurasi tertinggi, ambil maksimal 3
            val finalResults = detectedObjects.sortedByDescending {
                // Hack simple: ambil angka persen dari string "Label 90%"
                it.text.split(" ").last().replace("%","").toIntOrNull() ?: 0
            }.take(3)

            onObjectDetected(finalResults)

        } catch (e: Exception) {
            Log.e("NetraGuide", "Error analyze: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun getCocoLabels(): List<String> {
        // Daftar 80 Benda COCO (Pastikan urutan ini benar sesuai model yolov8n default)
        return listOf(
            "Orang", "Sepeda", "Mobil", "Motor", "Pesawat", "Bus", "Kereta", "Truk", "Perahu", "Lampu Merah",
            "Pemadam Api", "Tanda Stop", "Meteran Parkir", "Bangku", "Burung", "Kucing", "Anjing", "Kuda", "Domba", "Sapi",
            "Gajah", "Beruang", "Zebra", "Jerapah", "Ransel", "Payung", "Tas Tangan", "Dasi", "Koper", "Frisbee",
            "Ski", "Snowboard", "Bola", "Layangan", "Pemukul Bisbol", "Sarung Bisbol", "Skateboard", "Papan Surfing", "Raket Tenis", "Botol",
            "Gelas", "Cangkir", "Garpu", "Pisau", "Sendok", "Mangkuk", "Pisang", "Apel", "Sandwich", "Jeruk",
            "Brokoli", "Wortel", "Hot Dog", "Pizza", "Donat", "Kue", "Kursi", "Sofa", "Tanaman Pot", "Tempat Tidur",
            "Meja Makan", "Toilet", "TV", "Laptop", "Mouse", "Remote", "Keyboard", "HP", "Microwave", "Oven",
            "Toaster", "Wastafel", "Kulkas", "Buku", "Jam", "Vas Bunga", "Gunting", "Boneka", "Sikat Gigi"
        )
    }
}
