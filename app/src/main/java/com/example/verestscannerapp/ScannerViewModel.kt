package com.example.verestscannerapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File


class ScannerViewModel(private val dao: ProductDao) : ViewModel() {
    private val _scannedProduct = MutableStateFlow<Product?>(null)
    val scannedProduct: StateFlow<Product?> = _scannedProduct

    private val _saveStatus = MutableStateFlow<String?>(null)
    val saveStatus: StateFlow<String?> = _saveStatus

    private val _lastScannedBarcode = MutableStateFlow<String?>(null)
    val lastScannedBarcode: StateFlow<String?> = _lastScannedBarcode

    val allProducts: StateFlow<List<Product>> = dao.getAllProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun deleteInternalFile(path: String) {
        if (path.startsWith("file://")) {
            try {
                val uri = android.net.Uri.parse(path)
                val file = uri.path?.let { File(it) }
                if (file != null && file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun processBarcode(barcode: String) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isEmpty()) {
            _scannedProduct.value = null
            _lastScannedBarcode.value = null
            return
        }

        viewModelScope.launch {
            val existing = dao.getProductByBarcode(cleanBarcode)
            _scannedProduct.value = existing
            _lastScannedBarcode.value = if (existing == null) cleanBarcode else null
        }
    }

    fun createNewProduct(barcode: String, name: String) {
        viewModelScope.launch {
            val newProduct = Product(
                barcode = barcode,
                name = name.trim().ifEmpty { "Без назви" },
                plu = "",
                imagePath = "",
                imageHistory = emptyList()
            )
            dao.insertProduct(newProduct)
            _scannedProduct.value = newProduct
            _lastScannedBarcode.value = null
            _saveStatus.value = "Товар створено!"
            delay(2000)
            _saveStatus.value = null
        }
    }

    fun updateProductDetails(name: String, plu: String) {
        val currentProduct = _scannedProduct.value ?: return
        val updatedProduct = currentProduct.copy(name = name.trim(), plu = plu.trim())

        viewModelScope.launch {
            dao.insertProduct(updatedProduct)
            _scannedProduct.value = updatedProduct
            _saveStatus.value = "Збережено!"
            delay(2000)
            _saveStatus.value = null
        }
    }

    fun updateProductImage(imageUri: String) {
        val currentProduct = _scannedProduct.value ?: return

        val updatedHistory = currentProduct.imageHistory.toMutableList()
        updatedHistory.add(imageUri)

        if (updatedHistory.size > 5) {
            val oldestPhoto = updatedHistory.removeAt(0)
            if (oldestPhoto != imageUri && oldestPhoto != currentProduct.imagePath) {
                deleteInternalFile(oldestPhoto)
            }
        }

        val updatedProduct = currentProduct.copy(
            imagePath = imageUri,
            imageHistory = updatedHistory
        )

        viewModelScope.launch {
            dao.insertProduct(updatedProduct)
            _scannedProduct.value = updatedProduct
            _saveStatus.value = "Фото додано до галереї!"
            delay(2000)
            _saveStatus.value = null
        }
    }

    fun setPrimaryImage(imageUri: String) {
        val currentProduct = _scannedProduct.value ?: return
        val updatedProduct = currentProduct.copy(imagePath = imageUri)

        viewModelScope.launch {
            dao.insertProduct(updatedProduct)
            _scannedProduct.value = updatedProduct
            _saveStatus.value = "Основне фото змінено!"
            delay(2000)
            _saveStatus.value = null
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            product.imageHistory.forEach { path ->
                deleteInternalFile(path)
            }

            dao.deleteProduct(product)

            if (_scannedProduct.value?.barcode == product.barcode) {
                _scannedProduct.value = null
                _lastScannedBarcode.value = null
            }
        }
    }

    fun clearCurrentProduct() {
        _scannedProduct.value = null
        _lastScannedBarcode.value = null
    }
}

class ScannerViewModelFactory(private val dao: ProductDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScannerViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}