package com.example.verestscannerapp

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

object OcrManager {

    fun prepareTessData(context: Context) {
        val tessDir = File(context.filesDir, "tessdata")
        if (!tessDir.exists()) {
            tessDir.mkdirs()
        }

        val trainedData = File(tessDir, "ukr.traineddata")
        if (!trainedData.exists()) {
            try {
                context.assets.open("tessdata/ukr.traineddata").use { input ->
                    FileOutputStream(trainedData).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun extractTextFromBitmap(context: Context, bitmap: Bitmap): String {
        val tessBaseAPI = TessBaseAPI()

        val dataPath = context.filesDir.absolutePath

        if (!tessBaseAPI.init(dataPath, "ukr")) {
            return "Помилка ініціалізації OCR"
        }

        tessBaseAPI.setImage(bitmap)

        val extractedText = tessBaseAPI.utF8Text

        tessBaseAPI.recycle()

        return extractedText ?: ""
    }
}