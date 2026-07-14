package com.example.verestscannerapp

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OcrAnalyzer(
    private val context: Context,
    private val onTextRecognized: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var isProcessing = false
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close() // Пропускаємо кадр
            return
        }
        isProcessing = true

        val bitmap = imageProxy.toBitmap()

        scope.launch {
            val text = OcrManager.extractTextFromBitmap(context, bitmap)

            if (text.isNotBlank()) {
                withContext(Dispatchers.Main) {
                    onTextRecognized(text.trim())
                }
            }

            isProcessing = false
            imageProxy.close()
        }
    }
}