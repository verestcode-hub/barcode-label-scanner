package com.example.verestscannerapp

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

object MLKitHelper {

    // Для фотографій з Галереї
    fun analyzeBarcodeUri(
        context: Context,
        imageUri: Uri,
        onResult: (barcode: String?) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            processImage(image, onResult)
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(null)
        }
    }

    // Для швидких фотографій прямо з Камери (не зберігаючи на диск!)
    fun analyzeBarcodeBitmap(
        bitmap: Bitmap,
        onResult: (barcode: String?) -> Unit
    ) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            processImage(image, onResult)
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(null)
        }
    }

    // Спільна логіка сканування
    private fun processImage(image: InputImage, onResult: (barcode: String?) -> Unit) {
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                // Беремо перший знайдений штрих-код
                val foundBarcode = barcodes.firstOrNull()?.rawValue
                onResult(foundBarcode)
            }
            .addOnFailureListener {
                onResult(null)
            }
            .addOnCompleteListener {
                scanner.close() // Звільняємо пам'ять
            }
    }
}