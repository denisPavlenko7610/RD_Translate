package com.RDragon.rd_translator

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.nio.ByteBuffer

object OCRProcessor {
    private const val TAG = "OCRProcessor"
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    // Флаг для предотвращения повторных одновременных захватов
    private var isProcessing = false

    // Конвертирует Image в Bitmap. Обратите внимание — вычисление ширины может потребовать уточнений в зависимости от формата.
    private fun convertImageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        // Ширина битмапа увеличивается на количество пикселей, необходимое для компенсации отступа в строке.
        return Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        ).apply {
            copyPixelsFromBuffer(buffer)
        }
    }

    fun capture(
        projection: MediaProjection,
        callback: (List<String>) -> Unit
    ) {
        // Если захват уже выполняется, то повторный вызов пропускается.
        if (isProcessing) {
            Log.d(TAG, "Захват уже запущен. Пропускаем повторный вызов.")
            return
        }
        isProcessing = true

        val metrics: DisplayMetrics = Resources.getSystem().displayMetrics

        // Используем maxImages = 1 для получения только одного изображения
        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            1
        )

        virtualDisplay = projection.createVirtualDisplay(
            "OCR_${System.currentTimeMillis()}",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image == null) {
                isProcessing = false
                return@setOnImageAvailableListener
            }
            try {
                val bitmap = convertImageToBitmap(image)
                val bottomBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    bitmap.height / 2,
                    bitmap.width,
                    bitmap.height / 2
                )
                // Освобождаем исходный bitmap
                bitmap.recycle()

                recognizer.process(InputImage.fromBitmap(bottomBitmap, 0))
                    .addOnSuccessListener { visionText ->
                        val words = visionText.text.split(Regex("\\W+"))
                            .filter { it.length > 1 }
                            .map { it.lowercase() }
                        callback(words)
                        bottomBitmap.recycle()
                        // Очищаем ресурсы после успешного захвата
                        cleanup()
                        isProcessing = false
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Ошибка OCR", e)
                        callback(emptyList())
                        bottomBitmap.recycle()
                        cleanup()
                        isProcessing = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при обработке изображения", e)
                callback(emptyList())
                cleanup()
                isProcessing = false
            } finally {
                image.close()
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun cleanup() {
        // Убираем слушатель, чтобы предотвратить повторные срабатывания
        imageReader?.setOnImageAvailableListener(null, null)
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
    }
}
