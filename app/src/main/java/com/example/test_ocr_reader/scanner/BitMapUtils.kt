package com.example.test_ocr_reader.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Color

object BitmapUtils {

    fun applyTopHat(grayBitmap: Bitmap, kernelWidth: Int = 9, kernelHeight: Int = 3): Bitmap {
        val topHatBitmap = Bitmap.createBitmap(grayBitmap.width, grayBitmap.height, Bitmap.Config.RGB_565)

        val halfKernelWidth = kernelWidth / 2
        val halfKernelHeight = kernelHeight / 2

        for (x in halfKernelWidth until grayBitmap.width - halfKernelWidth) {
            for (y in halfKernelHeight until grayBitmap.height - halfKernelHeight) {
                var maxPixelValue = 0

                // Perform max operation within the kernel
                for (i in -halfKernelWidth..halfKernelWidth) {
                    for (j in -halfKernelHeight..halfKernelHeight) {
                        val pixelValue = grayBitmap.getPixel(x + i, y + j) and 0xFF // Extract the grayscale value
                        maxPixelValue = maxOf(maxPixelValue, pixelValue)
                    }
                }

                // Compute top hat result
                val topHatValue = maxPixelValue - (grayBitmap.getPixel(x, y) and 0xFF) // Extract the grayscale value

                // Set the top hat result to the output bitmap
                topHatBitmap.setPixel(x, y, Color.rgb(topHatValue, topHatValue, topHatValue))
            }
        }

        return topHatBitmap
    }
    fun convertImageToGrayScale(bmpOriginal: Bitmap): Bitmap {
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
        return bmpGrayscale
    }

    fun optimizeBitImage(step: Int, bitmap: Bitmap): Bitmap {
        return when (step) {
            0 -> bitmap //no conversion
            1 -> convertImageToGrayScale(bitmap) // only grayscale
            2 -> applyTopHat(convertImageToGrayScale(bitmap))
            else -> bitmap
        }
    }
}
