package com.example.test_ocr_reader.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log

object BitmapUtils {

    private const val TAG = "SCAN_OCR"

    private fun applyHistogramEqualization(grayscaleBitmap: Bitmap): Bitmap {
        Log.d(TAG, "Starting histogram")
        val pixels = IntArray(grayscaleBitmap.width * grayscaleBitmap.height)
        grayscaleBitmap.getPixels(
            pixels,
            0,
            grayscaleBitmap.width,
            0,
            0,
            grayscaleBitmap.width,
            grayscaleBitmap.height
        )

        // Compute histogram
        val histogram = IntArray(256)
        for (pixel in pixels) {
            val grayValue = pixel shr 16 and 0xff // Extracting red component assuming grayscale
            histogram[grayValue]++
        }

        // Compute cumulative histogram
        val cumulativeHistogram = IntArray(256)
        cumulativeHistogram[0] = histogram[0]
        for (i in 1..255) {
            cumulativeHistogram[i] = cumulativeHistogram[i - 1] + histogram[i]
        }

        // Normalize cumulative histogram
        val totalPixels = grayscaleBitmap.width * grayscaleBitmap.height
        val normalizedHistogram = IntArray(256)
        for (i in 0..255) {
            normalizedHistogram[i] =
                Math.round(cumulativeHistogram[i] * 255.0 / totalPixels).toInt()
        }

        // Apply histogram equalization
        for (i in pixels.indices) {
            val grayValue = pixels[i] shr 16 and 0xff
            val equalizedValue = normalizedHistogram[grayValue]
            pixels[i] =
                0xFF shl 24 or (equalizedValue shl 16) or (equalizedValue shl 8) or equalizedValue
        }

        // Create new bitmap with equalized histogram
        val equalizedBitmap = Bitmap.createBitmap(
            grayscaleBitmap.width,
            grayscaleBitmap.height,
            Bitmap.Config.RGB_565
        )
        equalizedBitmap.setPixels(
            pixels,
            0,
            grayscaleBitmap.width,
            0,
            0,
            grayscaleBitmap.width,
            grayscaleBitmap.height
        )

        Log.d(TAG, "finished histogram")
        return equalizedBitmap
    }

    private fun applyGrayScale(bmpOriginal: Bitmap): Bitmap {
        Log.d(TAG, "starting conversion gray scale")
        val width: Int  = bmpOriginal.width
        val height: Int =  bmpOriginal.height


        // Create a new bitmap with the same dimensions
        val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmpGrayscale)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f) // Set saturation to 0 for grayscale
        val f = ColorMatrixColorFilter(cm)
        paint.colorFilter = f
        c.drawBitmap(bmpOriginal, 0f, 0f, paint)
        Log.d(TAG, "finished gray scale conversion")
        return bmpGrayscale
    }

    fun optimizeBitImage(step: Int, bitmap: Bitmap): Bitmap {
        return when (step) {
            0 -> bitmap //no conversion
            1 -> applyGrayScale(bitmap) // only grayscale
            2 -> applyHistogramEqualization(applyGrayScale(bitmap))
            else -> bitmap
        }
    }
}
