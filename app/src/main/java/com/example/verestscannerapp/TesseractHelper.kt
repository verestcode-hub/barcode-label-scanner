package com.example.verestscannerapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object TesseractHelper {
    private const val DATA_PATH_DIR = "tesseract"
    private const val TESSDATA_DIR = "tessdata"
    private const val LANGUAGE = "ukr"

    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private suspend fun prepareTesseract(context: Context): String {
        return withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, DATA_PATH_DIR)
            if (!dir.exists()) dir.mkdirs()
            val tessdataDir = File(dir, TESSDATA_DIR)
            if (!tessdataDir.exists()) tessdataDir.mkdirs()
            val langFile = File(tessdataDir, "$LANGUAGE.traineddata")

            if (!langFile.exists()) {
                try {
                    val inputStream: InputStream = context.assets.open("$TESSDATA_DIR/$LANGUAGE.traineddata")
                    val outputStream = FileOutputStream(langFile)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                } catch (e: Exception) { e.printStackTrace() }
            }
            dir.absolutePath
        }
    }

    suspend fun recognizeText(context: Context, bitmap: Bitmap): String {
        return withContext(Dispatchers.Default) {
            val cleanBitmap = preprocessBitmap(bitmap)

            val roiHeight = (cleanBitmap.height * 0.35).toInt().coerceAtLeast(1)
            val roi = Bitmap.createBitmap(cleanBitmap, 0, 0, cleanBitmap.width, roiHeight)

            var rawText = ""
            try {
                val dataPath = prepareTesseract(context)
                val tess = TessBaseAPI()
                tess.init(dataPath, LANGUAGE)
                tess.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE
                tess.setImage(roi)
                rawText = tess.utF8Text ?: ""
                tess.recycle()
            } catch (e: Exception) { e.printStackTrace() }

            val cleaned = rawText
                .replace(Regex("[^А-Яа-яІіЄєЇї\\s]"), " ")
                .trim()
                .split(Regex("\\s+"))
                .filter { it.length > 2 }
                .take(8)
                .joinToString(" ")
                .uppercase()

            if (cleaned.isBlank()) "Назву не знайдено" else cleaned
        }
    }
}