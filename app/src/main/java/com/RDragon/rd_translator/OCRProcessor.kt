package com.RDragon.rd_translator

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.nio.ByteBuffer

object OCRProcessor {
    private const val TAG = "OCRProcessor"
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Process a single Image (from ImageReader) and return detected words.
     * The image is closed automatically.
     */
    fun processImage(image: Image, callback: (List<String>) -> Unit) {
        try {
            val bitmap = convertImageToBitmap(image)
            // crop bottom half
            val bottom = Bitmap.createBitmap(
                bitmap,
                0,
                bitmap.height / 2,
                bitmap.width,
                bitmap.height / 2
            )
            bitmap.recycle()

            recognizer.process(InputImage.fromBitmap(bottom, 0))
                .addOnSuccessListener { visionText ->
                    val words = visionText.text.split(Regex("\\W+"))
                        .filter { it.length > 1 }
                        .map { it.lowercase() }
                    callback(words)
                    bottom.recycle()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error OCR", e)
                    callback(emptyList())
                    bottom.recycle()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception processing image", e)
            callback(emptyList())
        } finally {
            image.close()
        }
    }

    private fun convertImageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        return Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        ).apply {
            copyPixelsFromBuffer(buffer)
        }
    }
}