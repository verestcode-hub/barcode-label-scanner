package com.example.verestscannerapp

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

enum class ThemeMode { SYSTEM, LIGHT, DARK, VEREST }

class ScannerViewModel(application: Application, private val dao: ProductDao) : AndroidViewModel(application) {

    private val THEME_KEY = stringPreferencesKey("theme_mode")

    private val SHOW_SCAN_BTN_KEY = booleanPreferencesKey("show_scan_btn")
    private val SHOW_MAGIC_BTN_KEY = booleanPreferencesKey("show_magic_btn")

    private val SHOW_CREATE_NEW_BTN_KEY = booleanPreferencesKey("show_create_new_btn")
    private val SHOW_SCAN_ANOTHER_BTN_KEY = booleanPreferencesKey("show_scan_another_btn")

    private val IS_GRID_VIEW_KEY = booleanPreferencesKey("is_grid_view")
    private val GRID_COLUMNS_KEY = intPreferencesKey("grid_columns")
    private val REMEMBER_GRID_CHOICE_KEY = booleanPreferencesKey("remember_grid_choice")
    private val FONT_SIZE_SCALE_KEY = floatPreferencesKey("font_size_scale")
    private val SORT_TYPE_KEY = stringPreferencesKey("sort_type")
    private val IS_SORT_ASCENDING_KEY = booleanPreferencesKey("is_sort_ascending")

    private val NETWORKS_KEY = stringSetPreferencesKey("custom_networks")


    val themeMode: StateFlow<ThemeMode> = application.dataStore.data.map { prefs ->
        try { ThemeMode.valueOf(prefs[THEME_KEY] ?: ThemeMode.SYSTEM.name) } catch (e: Exception) { ThemeMode.SYSTEM }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val showScanButton: StateFlow<Boolean> = application.dataStore.data.map { prefs ->
        prefs[SHOW_SCAN_BTN_KEY] ?: true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showMagicButton: StateFlow<Boolean> = application.dataStore.data.map { prefs ->
        prefs[SHOW_MAGIC_BTN_KEY] ?: true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showCreateNewBtn: StateFlow<Boolean> = application.dataStore.data.map { prefs ->
        prefs[SHOW_CREATE_NEW_BTN_KEY] ?: true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showScanAnotherBtn: StateFlow<Boolean> = application.dataStore.data.map { prefs ->
        prefs[SHOW_SCAN_ANOTHER_BTN_KEY] ?: true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isGridView: StateFlow<Boolean> = application.dataStore.data.map { prefs ->
        prefs[IS_GRID_VIEW_KEY] ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val savedGridColumns: StateFlow<Int> = application.dataStore.data.map { prefs ->
        prefs[GRID_COLUMNS_KEY] ?: 2
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val rememberGridChoice: StateFlow<Boolean> = application.dataStore.data.map { prefs ->
        prefs[REMEMBER_GRID_CHOICE_KEY] ?: true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val fontSizeScale: StateFlow<Float> = application.dataStore.data.map { prefs ->
        prefs[FONT_SIZE_SCALE_KEY] ?: 1.0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val sortType: StateFlow<String> = application.dataStore.data.map { prefs ->
        prefs[SORT_TYPE_KEY] ?: "DATE_ADDED"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DATE_ADDED")

    val isSortAscending: StateFlow<Boolean> = application.dataStore.data.map { prefs ->
        prefs[IS_SORT_ASCENDING_KEY] ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val customNetworks: StateFlow<Set<String>> = application.dataStore.data.map { prefs ->
        prefs[NETWORKS_KEY] ?: setOf("Verest", "АТБ", "Сільпо", "Novus", "Аврора")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("Verest", "АТБ", "Сільпо", "Novus", "Аврора"))


    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[THEME_KEY] = mode.name }
    }
    fun setShowScanButton(show: Boolean) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[SHOW_SCAN_BTN_KEY] = show }
    }
    fun setShowMagicButton(show: Boolean) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[SHOW_MAGIC_BTN_KEY] = show }
    }
    fun setShowCreateNewBtn(show: Boolean) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[SHOW_CREATE_NEW_BTN_KEY] = show }
    }
    fun setShowScanAnotherBtn(show: Boolean) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[SHOW_SCAN_ANOTHER_BTN_KEY] = show }
    }
    fun setIsGridView(isGrid: Boolean) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[IS_GRID_VIEW_KEY] = isGrid }
    }
    fun setSavedGridColumns(columns: Int) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[GRID_COLUMNS_KEY] = columns }
    }
    fun setRememberGridChoice(remember: Boolean) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[REMEMBER_GRID_CHOICE_KEY] = remember }
    }
    fun setFontSizeScale(scale: Float) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[FONT_SIZE_SCALE_KEY] = scale }
    }
    fun setSortType(type: String) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[SORT_TYPE_KEY] = type }
    }
    fun setIsSortAscending(isAscending: Boolean) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[IS_SORT_ASCENDING_KEY] = isAscending }
    }

    fun addNetwork(network: String) = viewModelScope.launch {
        val trimmed = network.trim()
        if (trimmed.isNotEmpty()) {
            getApplication<Application>().dataStore.edit { prefs ->
                val current = prefs[NETWORKS_KEY] ?: emptySet()
                prefs[NETWORKS_KEY] = current + trimmed
            }
        }
    }

    fun removeNetwork(network: String) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { prefs ->
            val current = prefs[NETWORKS_KEY] ?: emptySet()
            prefs[NETWORKS_KEY] = current - network
        }
    }

    private val _scannedProduct = MutableStateFlow<Product?>(null)
    val scannedProduct: StateFlow<Product?> = _scannedProduct

    private val _saveStatus = MutableStateFlow<String?>(null)
    val saveStatus: StateFlow<String?> = _saveStatus

    private val _lastScannedBarcode = MutableStateFlow<String?>(null)
    val lastScannedBarcode: StateFlow<String?> = _lastScannedBarcode

    private var statusJob: Job? = null

    val allProducts: StateFlow<List<Product>> = dao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun setStatus(message: String) {
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            _saveStatus.value = message
            delay(2000.milliseconds)
            _saveStatus.value = null
        }
    }

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

    fun scanBarcodeFromBitmap(bitmap: Bitmap, cropRect: Rect?, onResult: (String?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isEmpty()) {
                    onResult(null)
                    return@addOnSuccessListener
                }

                if (cropRect == null) {
                    val bestBarcode = barcodes.maxByOrNull { it.boundingBox?.width() ?: 0 }
                    onResult(bestBarcode?.rawValue)
                } else {
                    val cropCenterX = cropRect.exactCenterX()
                    val cropCenterY = cropRect.exactCenterY()

                    val closestBarcode = barcodes.minByOrNull { barcode ->
                        val bbox = barcode.boundingBox
                        if (bbox != null) {
                            val dx = bbox.exactCenterX() - cropCenterX
                            val dy = bbox.exactCenterY() - cropCenterY
                            sqrt(dx * dx + dy * dy)
                        } else {
                            Float.MAX_VALUE
                        }
                    }
                    onResult(closestBarcode?.rawValue)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun updateOrInsertProduct(newProductData: JsonProduct) {
        viewModelScope.launch {
            val isNameEmpty = newProductData.NAME.isNullOrBlank()
            val isEanEmpty = newProductData.EAN.isNullOrBlank()
            val isNumEmpty = newProductData.NUM.isNullOrBlank()

            if (isNameEmpty && isEanEmpty && isNumEmpty) {
                return@launch
            }
            val barcodeValue = newProductData.EAN?.takeIf { it.isNotBlank() }
                ?: newProductData.NUM?.takeIf { it.isNotBlank() }
                ?: java.util.UUID.randomUUID().toString()

            val existing = dao.getProductByBarcode(barcodeValue)

            if (existing != null) {
                val updated = existing.copy(
                    name = newProductData.NAME?.takeIf { it.isNotBlank() } ?: existing.name,
                    plu = newProductData.NUM?.takeIf { it.isNotBlank() } ?: existing.plu,
                    groupPlu = newProductData.F_GROUPPLU?.takeIf { it.isNotBlank() } ?: existing.groupPlu
                )
                dao.insertProduct(updated)
            } else {

                val product = Product(
                    barcode = barcodeValue,
                    name = newProductData.NAME?.takeIf { it.isNotBlank() } ?: "Без назви",
                    plu = newProductData.NUM ?: "",
                    groupPlu = newProductData.F_GROUPPLU ?: ""
                )
                dao.insertProduct(product)
            }
        }
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backupFile = File(context.getExternalFilesDir(null), "backup.zip")
                val zipOut = ZipOutputStream(backupFile.outputStream().buffered())

                val products = dao.getAllProductsList()
                val json = Gson().toJson(products)

                zipOut.putNextEntry(ZipEntry("products.json"))
                zipOut.write(json.toByteArray())
                zipOut.closeEntry()

                val files = context.filesDir.listFiles { _, name -> name.startsWith("IMG_") }
                files?.forEach { file ->
                    zipOut.putNextEntry(ZipEntry("images/${file.name}"))
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }

                zipOut.close()

                setStatus("Бекап створено: ${backupFile.absolutePath}")

            } catch (e: Exception) {
                e.printStackTrace()
                setStatus("Помилка експорту: ${e.message}")
            }
        }
    }

    fun shareBackup(context: Context) {
        val backupFile = File(context.getExternalFilesDir(null), "backup.zip")
        if (!backupFile.exists()) {
            setStatus("Файл бекапу не знайдено!")
            return
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            backupFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Надіслати бекап через..."))
    }

    fun importBackup(context: Context, zipUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(zipUri) ?: return@launch
                val zis = ZipInputStream(inputStream)

                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        if (entry.name == "products.json") {
                            val jsonString = zis.bufferedReader().readText()
                            val listType = object : TypeToken<List<Product>>() {}.type
                            val products: List<Product> = Gson().fromJson(jsonString, listType)
                            products.forEach { dao.insertProduct(it) }
                        } else if (entry.name.startsWith("images/")) {
                            val fileName = File(entry.name).name
                            if (fileName.isNotEmpty()) {
                                val outFile = File(context.filesDir, fileName)
                                FileOutputStream(outFile).use { fos ->
                                    val buffer = ByteArray(1024)
                                    var len: Int
                                    while (zis.read(buffer).also { len = it } > 0) {
                                        fos.write(buffer, 0, len)
                                    }
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                zis.close()
                inputStream.close()
                setStatus("Бекап успішно відновлено!")
            } catch (e: Exception) {
                e.printStackTrace()
                setStatus("Помилка: ${e.message}")
            }
        }
    }

    private fun calculateEan13Checksum(barcode12: String): Char {
        var sum = 0
        for (i in 0..11) {
            val digit = barcode12[i].digitToIntOrNull() ?: 0
            sum += if (i % 2 == 0) digit else digit * 3
        }
        val checksum = (10 - (sum % 10)) % 10
        return checksum.digitToChar()
    }

    fun processBarcode(barcode: String) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isEmpty()) {
            _scannedProduct.value = null
            _lastScannedBarcode.value = null
            return
        }

        viewModelScope.launch {
            var existing = dao.getProductByBarcode(cleanBarcode)

            if (existing == null && cleanBarcode.length >= 6) {

                for (baseLength in 8 downTo 5) {
                    if (baseLength > cleanBarcode.length) continue

                    val baseCode = cleanBarcode.substring(0, baseLength)

                    existing = dao.getProductByBarcode(baseCode)
                    if (existing != null) break

                    val paddedZeros = baseCode.padEnd(13, '0')
                    existing = dao.getProductByBarcode(paddedZeros)
                    if (existing != null) break

                    val paddedTo12 = baseCode.padEnd(12, '0')
                    val checksum = calculateEan13Checksum(paddedTo12)
                    val correctEan13 = paddedTo12 + checksum

                    existing = dao.getProductByBarcode(correctEan13)
                    if (existing != null) break
                }
            }

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
                useByDate = "",
                groupPlu = ""
            )
            dao.insertProduct(newProduct)
            _scannedProduct.value = newProduct
            _lastScannedBarcode.value = null
            setStatus("Товар створено!")
        }
    }

    fun updateProductDetails(name: String, plu: String, useByDate: String, groupPlu: String) {
        val currentProduct = _scannedProduct.value ?: return
        val updatedProduct = currentProduct.copy(
            name = name.trim(),
            plu = plu.trim(),
            useByDate = useByDate.trim(),
            groupPlu = groupPlu.trim(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            dao.insertProduct(updatedProduct)
            _scannedProduct.value = updatedProduct
            setStatus("Збережено!")
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

        val updatedProduct = currentProduct.copy(imagePath = imageUri, imageHistory = updatedHistory)

        viewModelScope.launch {
            dao.insertProduct(updatedProduct)
            _scannedProduct.value = updatedProduct
            setStatus("Фото додано!")
        }
    }

    fun setPrimaryImage(imageUri: String) {
        val currentProduct = _scannedProduct.value ?: return
        val updatedProduct = currentProduct.copy(imagePath = imageUri)

        viewModelScope.launch {
            dao.insertProduct(updatedProduct)
            _scannedProduct.value = updatedProduct
            setStatus("Фото змінено!")
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            product.imageHistory.forEach { deleteInternalFile(it) }
            dao.deleteProduct(product)
            if (_scannedProduct.value?.barcode == product.barcode) {
                clearCurrentProduct()
            }
        }
    }

    fun clearCurrentProduct() {
        _scannedProduct.value = null
        _lastScannedBarcode.value = null
    }

    fun performTextRecognition(context: Context, bitmap: Bitmap, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val recognizedName = OcrManager.extractTextFromBitmap(context, bitmap)
            launch(Dispatchers.Main) {
                onResult(recognizedName)
            }
        }
    }

    fun importDataFromJson(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("products.json").bufferedReader().use { it.readText() }
                val listType = object : TypeToken<List<JsonProduct>>() {}.type
                val jsonProducts: List<JsonProduct> = Gson().fromJson(jsonString, listType)

                jsonProducts.forEach { json ->
                    updateOrInsertProduct(json)
                }

                setStatus("Імпорт успішно завершено!")
            } catch (e: Exception) {
                e.printStackTrace()
                setStatus("Помилка імпорту")
            }
        }
    }

    fun importCustomJson(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val listType = object : TypeToken<List<JsonProduct>>() {}.type
                val jsonProducts: List<JsonProduct> = Gson().fromJson(jsonString, listType)

                jsonProducts.forEach { json ->
                    updateOrInsertProduct(json)
                }

                setStatus("Імпортовано ${jsonProducts.size} товарів!")
            } catch (e: Exception) {
                e.printStackTrace()
                setStatus("Помилка імпорту: ${e.message}")
            }
        }
    }
}

class ScannerViewModelFactory(
    private val application: Application,
    private val dao: ProductDao
) : ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScannerViewModel(application, dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}