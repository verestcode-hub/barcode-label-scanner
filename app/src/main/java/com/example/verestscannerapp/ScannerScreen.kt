package com.example.verestscannerapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import androidx.compose.foundation.horizontalScroll


fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String {

    val filename = "IMG_${UUID.randomUUID()}.png"
    val file = File(context.filesDir, filename)
    return try {
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

fun saveImageTransformLocally(context: Context, uri: String, scale: Float, offsetX: Float, offsetY: Float) {
    val prefs = context.getSharedPreferences("img_transforms", Context.MODE_PRIVATE)
    prefs.edit().putString(uri, "$scale,$offsetX,$offsetY").apply()
}

fun getImageTransformLocally(context: Context, uri: String): Triple<Float, Float, Float> {
    val prefs = context.getSharedPreferences("img_transforms", Context.MODE_PRIVATE)
    val str = prefs.getString(uri, "1.0,0.0,0.0") ?: "1.0,0.0,0.0"
    val parts = str.split(",")
    if (parts.size == 3) {
        return Triple(
            parts[0].toFloatOrNull() ?: 1f,
            parts[1].toFloatOrNull() ?: 0f,
            parts[2].toFloatOrNull() ?: 0f
        )
    }
    return Triple(1f, 0f, 0f)
}

fun generateBarcodeBitmap(barcodeValue: String): androidx.compose.ui.graphics.ImageBitmap? {
    if (barcodeValue.isEmpty()) return null
    return try {
        val bitMatrix = MultiFormatWriter().encode(
            barcodeValue,
            BarcodeFormat.CODE_128,
            600, 150
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(viewModel: ScannerViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentMainScreen by rememberSaveable { mutableStateOf("SCANNER") }
    var isViewingFromInventory by rememberSaveable { mutableStateOf(false) }

    val isGridView by viewModel.isGridView.collectAsState()
    val savedGridColumns by viewModel.savedGridColumns.collectAsState()
    val rememberGridChoice by viewModel.rememberGridChoice.collectAsState()
    val fontSizeScale by viewModel.fontSizeScale.collectAsState()

    val sortTypeString by viewModel.sortType.collectAsState()
    val sortType = try { SortType.valueOf(sortTypeString) } catch (e: Exception) { SortType.DATE_ADDED }
    val isSortAscending by viewModel.isSortAscending.collectAsState()

    val showScanButton by viewModel.showScanButton.collectAsState()
    val showMagicButton by viewModel.showMagicButton.collectAsState()
    val showCreateNewBtn by viewModel.showCreateNewBtn.collectAsState()
    val showScanAnotherBtn by viewModel.showScanAnotherBtn.collectAsState()

    val currentTheme by viewModel.themeMode.collectAsState()
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }

    val gridState = rememberLazyGridState()

    val scannedProduct by viewModel.scannedProduct.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val lastScannedBarcode by viewModel.lastScannedBarcode.collectAsState()

    val scanInput by viewModel.scanInput.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()
    val isManualInputMode by viewModel.isManualInputMode.collectAsState()
    val newNameInput by viewModel.newNameInput.collectAsState()
    val activePrefixes by viewModel.customPrefixes.collectAsState()

    val labelNameSize by viewModel.labelNameSize.collectAsState()
    val labelBarcodeSize by viewModel.labelBarcodeSize.collectAsState()
    val isLabelNameCaps by viewModel.isLabelNameCaps.collectAsState()
    val labelLineHeight by viewModel.labelLineHeight.collectAsState()

    var showLayoutSettings by remember { mutableStateOf(false) }
    var isCardEditMode by rememberSaveable { mutableStateOf(false) }

    val rootFocusRequester = remember { FocusRequester() }
    val manualFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var isEditing by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var isMagicScanning by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scanner = remember { GmsBarcodeScanning.getClient(context) }

    var smartOcrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var smartOcrFile by remember { mutableStateOf<File?>(null) }

    val excelHeaders by viewModel.excelHeaders.collectAsState()
    val excelPreviewData by viewModel.excelPreviewData.collectAsState()

    if (excelHeaders.isNotEmpty()) {
        var barcodeIdx by remember { mutableStateOf(excelHeaders.indexOfFirst { it.contains("штрих", true) || it.contains("код", true) || it.contains("barcode", true) }) }
        var nameIdx by remember { mutableStateOf(excelHeaders.indexOfFirst { it.contains("назв", true) || it.contains("найменування", true) || it.contains("товар", true) }) }
        var pluIdx by remember { mutableStateOf(excelHeaders.indexOfFirst { it.contains("plu", true) || it.contains("плу", true) }) }
        var groupIdx by remember { mutableStateOf(excelHeaders.indexOfFirst { it.contains("клієнт", true) || it.contains("постачальник", true) || it.contains("група", true) }) }
        var dateIdx by remember { mutableStateOf(excelHeaders.indexOfFirst { it.contains("термін", true) || it.contains("дата", true) || it.contains("придат", true) }) }

        AlertDialog(
            onDismissRequest = { viewModel.clearPendingExcel() },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.80f)
                .widthIn(max = 800.dp),
            title = { Text("Налаштування імпорту") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Вкажіть, які колонки з файлу відповідають даним у системі.",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        ColumnMapper("Штрих-код", excelHeaders, barcodeIdx) { barcodeIdx = it }
                        ColumnMapper("Назва товару", excelHeaders, nameIdx) { nameIdx = it }
                        ColumnMapper("PLU-код", excelHeaders, pluIdx) { pluIdx = it }
                        ColumnMapper("Клієнт (Група)", excelHeaders, groupIdx) { groupIdx = it }
                        ColumnMapper("Термін придатності", excelHeaders, dateIdx) { dateIdx = it }

                        if (excelPreviewData.isNotEmpty()) {
                            ExcelPreviewTable(headers = excelHeaders, previewData = excelPreviewData)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importExcelWithMapping(context, barcodeIdx, nameIdx, pluIdx, groupIdx, dateIdx)
                }) {
                    Text("Імпортувати")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearPendingExcel() }) {
                    Text("Скасувати")
                }
            }
        )
    }

    val magicCropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val croppedUri = result.uriContent
            val originalUri = result.originalUri
            val cropRect = result.cropRect

            if (croppedUri != null && originalUri != null) {
                isMagicScanning = true
                scope.launch(Dispatchers.IO) {
                    try {
                        val origStream = context.contentResolver.openInputStream(originalUri)
                        val origBitmap = android.graphics.BitmapFactory.decodeStream(origStream)
                        origStream?.close()

                        val cropStream = context.contentResolver.openInputStream(croppedUri)
                        val cropBitmap = android.graphics.BitmapFactory.decodeStream(cropStream)
                        cropStream?.close()

                        withContext(Dispatchers.Main) {
                            if (origBitmap != null && cropBitmap != null) {
                                viewModel.performTextRecognition(context, cropBitmap) { text ->
                                    var cleanName = text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
                                    cleanName = cleanName.replace(Regex("^[^А-Яа-яЄєІіЇїҐґA-Za-z]+"), "")

                                    val correctedName = viewModel.getCorrectedOcrText(cleanName)
                                    viewModel.updateNewNameInput(correctedName)

                                    viewModel.scanBarcodeFromBitmap(origBitmap, cropRect) { barcode ->
                                        isMagicScanning = false
                                        if (barcode != null) {
                                            viewModel.setIsCreating(true)
                                            viewModel.processBarcode(barcode)
                                        } else {
                                            Toast.makeText(context, "Штрих-код не знайдено", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } else {
                                isMagicScanning = false
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) { isMagicScanning = false }
                    }
                }
            }
        } else {
            isMagicScanning = false
        }
    }

    val smartOcrCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            smartOcrFile?.let { file ->
                isMagicScanning = true
                scope.launch(Dispatchers.IO) {
                    try {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        withContext(Dispatchers.Main) {
                            smartOcrBitmap = bitmap
                            isMagicScanning = false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) { isMagicScanning = false }
                    }
                }
            }
        } else {
            isMagicScanning = false
        }
    }

    val imageCropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { croppedUri ->
                scope.launch(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(croppedUri)
                        val filename = "IMG_${UUID.randomUUID()}.png"
                        val file = File(context.filesDir, filename)
                        val outputStream = FileOutputStream(file)
                        inputStream?.copyTo(outputStream)
                        inputStream?.close()
                        outputStream.close()

                        withContext(Dispatchers.Main) {
                            viewModel.updateProductImage(Uri.fromFile(file).toString())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val options = CropImageContractOptions(it, CropImageOptions(
                fixAspectRatio = false,
                imageSourceIncludeCamera = false,
                outputCompressFormat = Bitmap.CompressFormat.PNG,

                borderLineColor = android.graphics.Color.BLACK,
                borderCornerColor = android.graphics.Color.BLACK,
                guidelinesColor = android.graphics.Color.BLACK
            ))
            imageCropLauncher.launch(options)
        }
    }

    var tempImageFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageFile?.let { file ->
                val uri = Uri.fromFile(file)
                val options = CropImageContractOptions(uri, CropImageOptions(
                    fixAspectRatio = false,
                    imageSourceIncludeCamera = false,
                    outputCompressFormat = Bitmap.CompressFormat.PNG,

                    borderLineColor = android.graphics.Color.BLACK,
                    borderCornerColor = android.graphics.Color.BLACK,
                    guidelinesColor = android.graphics.Color.BLACK
                ))
                imageCropLauncher.launch(options)
            }
        }
    }

    val backupPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                viewModel.importBackup(context, it)
            }
        }
    )

    val excelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.readExcelHeaders(context, it) }
        }
    )

    LaunchedEffect(isManualInputMode) {
        delay(100)
        if (!isManualInputMode) {
            rootFocusRequester.requestFocus()
            keyboardController?.hide()
        } else {
            manualFocusRequester.requestFocus()
        }
    }

    AnimatedContent(
        targetState = currentMainScreen,
        transitionSpec = {
            if (targetState != "SCANNER" && initialState == "SCANNER") {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300))
            } else {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
            }
        },
        label = "AppScreenTransition"
    ) { screen ->
        when (screen) {
            "SETTINGS" -> {
                SettingsScreen(viewModel = viewModel, onClose = { currentMainScreen = "SCANNER" })
            }
            "INVENTORY" -> {
                InventoryScreen(
                    viewModel = viewModel,
                    onClose = { currentMainScreen = "SCANNER"; isViewingFromInventory = false },
                    onProductSelected = { barcode ->
                        isViewingFromInventory = true
                        currentMainScreen = "SCANNER"
                        viewModel.processBarcode(barcode)
                    },
                    gridState = gridState,
                    isGridView = isGridView,
                    onGridViewChange = { viewModel.setIsGridView(it) },
                    savedGridColumns = savedGridColumns,
                    onColumnsChange = { viewModel.setSavedGridColumns(it) },
                    rememberGridChoice = rememberGridChoice,
                    onRememberChange = { viewModel.setRememberGridChoice(it) },
                    fontSizeScale = fontSizeScale,
                    onFontSizeChange = { viewModel.setFontSizeScale(it) },
                    sortType = sortType,
                    onSortTypeChange = { viewModel.setSortType(it.name) },
                    isSortAscending = isSortAscending,
                    onSortOrderChange = { viewModel.setIsSortAscending(it) }
                )
            }
            "SCANNER" -> {
                if (showBottomSheet) {
                    ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                            Text(
                                "Як додати фото етикетки?",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    showBottomSheet = false
                                    val photoFile = File(context.filesDir, "IMG_${UUID.randomUUID()}.jpg")
                                    tempImageFile = photoFile
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
                                    cameraLauncher.launch(uri)
                                }.padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Зробити фото", style = MaterialTheme.typography.bodyLarge)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    showBottomSheet = false
                                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }.padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Вибрати з галереї", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                if (showLayoutSettings) {
                    ModalBottomSheet(onDismissRequest = { showLayoutSettings = false }) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Налаштування візуалу",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        viewModel.updateLabelSettings(22f, 18f, true, 28f)
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Скинути")
                                }
                            }

                            val rawName = scannedProduct?.name?.takeIf { it.isNotBlank() } ?: "САЛЯМІ ДОМАШНЯ\nВИЩОГО СОРТУ"
                            val previewName = if (isLabelNameCaps) rawName.uppercase() else rawName

                            Text(
                                text = previewName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = labelNameSize.sp,
                                    lineHeight = labelLineHeight.sp // Застосовується відступ!
                                ),
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                                    .padding(16.dp) // Трохи більше внутрішнього відступу для краси
                            )

                            Text("Розмір шрифту назви: ${labelNameSize.toInt()}", fontWeight = FontWeight.Bold)
                            Slider(
                                value = labelNameSize,
                                onValueChange = { viewModel.updateLabelSettings(it, labelBarcodeSize, isLabelNameCaps, labelLineHeight) },
                                valueRange = 12f..36f
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Відступ між рядками назви: ${labelLineHeight.toInt()}", fontWeight = FontWeight.Bold)
                            Slider(
                                value = labelLineHeight,
                                onValueChange = { viewModel.updateLabelSettings(labelNameSize, labelBarcodeSize, isLabelNameCaps, it) },
                                valueRange = 16f..48f
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Розмір шрифту штрих-коду: ${labelBarcodeSize.toInt()}", fontWeight = FontWeight.Bold)

                            val previewBarcode = scannedProduct?.barcode ?: "4823079700543"
                            val displayPreview = if (previewBarcode.length <= 8 && activePrefixes.isNotEmpty()) {
                                "${activePrefixes.first()}$previewBarcode" + "000000000"
                            } else {
                                previewBarcode
                            }

                            Text(
                                text = displayPreview,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    fontSize = labelBarcodeSize.sp
                                ),
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            )

                            Slider(
                                value = labelBarcodeSize,
                                onValueChange = { viewModel.updateLabelSettings(labelNameSize, it, isLabelNameCaps, labelLineHeight) },
                                valueRange = 12f..32f
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Всі літери великі (CAPS)", fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = isLabelNameCaps,
                                    onCheckedChange = { viewModel.updateLabelSettings(labelNameSize, labelBarcodeSize, it, labelLineHeight) }
                                )
                            }
                        }
                    }
                }

                if (showThemeDialog) {
                    AlertDialog(
                        onDismissRequest = { showThemeDialog = false },
                        title = { Text("Тема") },
                        text = {
                            Column {
                                ThemeMode.entries.forEach { mode ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.setThemeMode(mode)
                                                showThemeDialog = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = currentTheme == mode, onClick = null)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(when (mode) {
                                            ThemeMode.SYSTEM -> "Системна"
                                            ThemeMode.VEREST -> "Верест"
                                            ThemeMode.LIGHT -> "Світла"
                                            ThemeMode.DARK -> "Темна"
                                        })
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemeDialog = false }) {
                                Text("Скасувати")
                            }
                        }
                    )
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.width(300.dp),
                            drawerContainerColor = MaterialTheme.colorScheme.background
                        ) {
                            val drawerItemColors = NavigationDrawerItemDefaults.colors(
                                unselectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedContainerColor = Color.Transparent
                            )

                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Меню",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.List, null) },
                                label = { Text("Показати весь інвентар", fontWeight = FontWeight.Bold) },
                                selected = false,
                                onClick = { scope.launch { drawerState.close() }; currentMainScreen = "INVENTORY" },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                colors = drawerItemColors
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Settings, null) },
                                label = { Text("Налаштування", fontWeight = FontWeight.Bold) },
                                selected = false,
                                onClick = { scope.launch { drawerState.close() }; currentMainScreen = "SETTINGS" },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                colors = drawerItemColors
                            )
                            ListItem(
                                headlineContent = { Text("Тема") },
                                supportingContent = {
                                    Text(when(currentTheme) {
                                        ThemeMode.SYSTEM -> "Системна";
                                        ThemeMode.VEREST -> "Верест";
                                        ThemeMode.LIGHT -> "Світла";
                                        ThemeMode.DARK -> "Темна"
                                    })
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent,
                                    headlineColor = MaterialTheme.colorScheme.primary,
                                    supportingColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.clickable { showThemeDialog = true }
                            )

                            ListItem(
                                headlineContent = { Text("Режим редагування", fontWeight = FontWeight.Bold) },
                                supportingContent = { Text("Кнопки макета та фотографій") },
                                trailingContent = {
                                    Switch(
                                        checked = isCardEditMode,
                                        onCheckedChange = { isCardEditMode = it }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = MaterialTheme.colorScheme.primary, supportingColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                                modifier = Modifier.clickable { isCardEditMode = !isCardEditMode }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                "Дані",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.ArrowDownward, null) },
                                label = { Text("Імпорт JSON") },
                                selected = false,
                                onClick = { scope.launch { drawerState.close() }; viewModel.importDataFromJson(context) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                colors = drawerItemColors
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.ArrowDownward, null) },
                                label = { Text("Імпорт з Excel (.xlsx)") },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    excelPickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                colors = drawerItemColors
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Share, null) },
                                label = { Text("Зробити бекап") },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    viewModel.exportBackup(context)
                                    scope.launch { delay(1000); viewModel.shareBackup(context) }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                colors = drawerItemColors
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Refresh, null) },
                                label = { Text("Відновити з бекапу") },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    backupPickerLauncher.launch(arrayOf("application/zip"))
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                colors = drawerItemColors
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Складський облік Verest") },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, "Відкрити меню")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    titleContentColor = MaterialTheme.colorScheme.primary,
                                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                                    actionIconContentColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    ) { paddingValues ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .imePadding()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!isManualInputMode) {
                                        rootFocusRequester.requestFocus()
                                        keyboardController?.hide()
                                    }
                                }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (!isManualInputMode && keyEvent.type == KeyEventType.KeyUp) {
                                        val key = keyEvent.key
                                        if (key == Key.Enter || key == Key.NumPadEnter) {
                                            if (scanInput.isNotBlank()) {
                                                viewModel.processBarcode(scanInput.trim())
                                                viewModel.updateScanInput("")
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                        val char = keyEvent.utf16CodePoint.toChar()
                                        if (char in ' '..'~') {
                                            viewModel.updateScanInput(scanInput + char)
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                    false
                                }
                                .focusRequester(rootFocusRequester)
                                .focusable(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            if (isManualInputMode) {
                                OutlinedTextField(
                                    value = scanInput,
                                    onValueChange = { viewModel.updateScanInput(it) },
                                    modifier = Modifier.fillMaxWidth().focusRequester(manualFocusRequester),
                                    label = { Text("Введіть штрих-код вручну") },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (scanInput.isNotBlank()) {
                                                viewModel.processBarcode(scanInput.trim())
                                                viewModel.updateScanInput("")
                                            }
                                            viewModel.setIsManualInputMode(false)
                                        }
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { viewModel.setIsManualInputMode(false) }) {
                                            Icon(Icons.Default.Close, "Закрити")
                                        }
                                    }
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = scanInput,
                                        onValueChange = {},
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Очікується ввід штрих-коду...") },
                                        readOnly = true
                                    )
                                    Box(modifier = Modifier.matchParentSize().clickable { viewModel.setIsManualInputMode(true) })
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isMagicScanning) {
                                Spacer(modifier = Modifier.height(16.dp))
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Розпізнавання етикетки...", style = MaterialTheme.typography.bodyLarge)
                            } else if (scannedProduct == null && lastScannedBarcode == null) {
                                if (showScanButton) {
                                    Button(
                                        onClick = {
                                            viewModel.setIsCreating(false)
                                            scanner.startScan().addOnSuccessListener { barcode ->
                                                barcode.rawValue?.let { viewModel.processBarcode(it) }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(Icons.Default.Search, null, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Знайти товар", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (showScanButton && showMagicButton) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                if (showMagicButton) {
                                    Button(
                                        onClick = {
                                            val file = File(context.filesDir, "smart_ocr_${UUID.randomUUID()}.jpg")
                                            smartOcrFile = file
                                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                            smartOcrCameraLauncher.launch(uri)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        )
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Створити з фото етикетки", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (showScanButton || showMagicButton) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                            saveStatus?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            scannedProduct?.let { product ->
                                OutlinedButton(
                                    onClick = {
                                        viewModel.clearCurrentProduct()
                                        if (isViewingFromInventory) {
                                            currentMainScreen = "INVENTORY"
                                        } else {
                                            rootFocusRequester.requestFocus()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isViewingFromInventory) "Повернутися в інвентар" else "Закрити картку товару")
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        if (isEditing) {
                                            var nameInput by remember(product.barcode) { mutableStateOf(product.name) }
                                            var pluInput by remember(product.barcode) { mutableStateOf(product.plu) }
                                            var dateInput by remember(product.barcode) { mutableStateOf(product.useByDate) }
                                            var groupInput by remember(product.barcode) { mutableStateOf(product.groupPlu) }

                                            val networks by viewModel.customNetworks.collectAsState()
                                            var expandedNetwork by remember { mutableStateOf(false) }

                                            OutlinedTextField(
                                                value = nameInput,
                                                onValueChange = { nameInput = it },
                                                label = { Text("Назва товару") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = pluInput,
                                                onValueChange = { pluInput = it },
                                                label = { Text("Код PLU") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = dateInput,
                                                onValueChange = { dateInput = it },
                                                label = { Text("Термін придатності") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            ExposedDropdownMenuBox(
                                                expanded = expandedNetwork,
                                                onExpandedChange = {
                                                    expandedNetwork = it
                                                    if (it) keyboardController?.hide()
                                                }
                                            ) {
                                                OutlinedTextField(
                                                    value = groupInput,
                                                    onValueChange = { groupInput = it },
                                                    label = { Text("Клієнт") },
                                                    readOnly = expandedNetwork,
                                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNetwork) },
                                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = expandedNetwork,
                                                    onDismissRequest = { expandedNetwork = false }
                                                ) {
                                                    networks.forEach { selectionOption ->
                                                        DropdownMenuItem(
                                                            text = { Text(selectionOption) },
                                                            onClick = {
                                                                groupInput = selectionOption
                                                                expandedNetwork = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.updateProductDetails(nameInput, pluInput, dateInput, groupInput)
                                                    isEditing = false
                                                    rootFocusRequester.requestFocus()
                                                    keyboardController?.hide()
                                                }
                                            ) {
                                                Text("Зберегти зміни")
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "Системні дані:", fontWeight = FontWeight.Bold)
                                                Button(
                                                    onClick = { isEditing = true },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondary,
                                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                                    )
                                                ) {
                                                    Text("Редагувати")
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = "Клієнт: ${product.groupPlu.ifEmpty { "Не вказано" }}")
                                            Text(text = "Назва: ${product.name.ifEmpty { "Без назви" }}")
                                            Text(text = "Код: ${product.barcode}")
                                            Text(text = "PLU: ${product.plu.ifEmpty { "Не вказано" }}")
                                            Text(text = "Дата придатності: ${product.useByDate.ifEmpty { "Не вказано" }}")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(2.dp, Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {

                                        // 1. ХОВАЄМО ШЕСТІРНЮ В УМОВУ
                                        if (isCardEditMode) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                IconButton(
                                                    onClick = { showLayoutSettings = true },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Settings, "Налаштувати макет", tint = Color.Gray)
                                                }
                                            }
                                        }

                                        val displayName = if (isLabelNameCaps) {
                                            product.name.ifEmpty { "БЕЗ НАЗВИ" }.uppercase()
                                        } else {
                                            product.name.ifEmpty { "Без назви" }
                                        }

                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = labelNameSize.sp,
                                                lineHeight = labelLineHeight.sp
                                            ),
                                            color = Color.Black,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        if (product.imageHistory.isNotEmpty()) {
                                            val initialPage = product.imageHistory.indexOf(product.imagePath).coerceAtLeast(0)
                                            val pagerState = rememberPagerState(
                                                initialPage = initialPage,
                                                pageCount = { product.imageHistory.size }
                                            )

                                            LaunchedEffect(product.imagePath, product.imageHistory.size) {
                                                val targetPage = product.imageHistory.indexOf(product.imagePath).coerceAtLeast(0)
                                                if (pagerState.currentPage != targetPage) {
                                                    pagerState.animateScrollToPage(targetPage)
                                                }
                                            }

                                            HorizontalPager(
                                                state = pagerState,
                                                modifier = Modifier.fillMaxWidth().height(220.dp),
                                                userScrollEnabled = true // Завжди ввімкнено для гортання!
                                            ) { page ->
                                                val photoUri = product.imageHistory[page]

                                                val savedTransform = remember(photoUri) { getImageTransformLocally(context, photoUri) }
                                                var scale by remember(photoUri) { mutableFloatStateOf(savedTransform.first) }
                                                var offsetX by remember(photoUri) { mutableFloatStateOf(savedTransform.second) }
                                                var offsetY by remember(photoUri) { mutableFloatStateOf(savedTransform.third) }

                                                var isZoomMode by remember { mutableStateOf(false) }

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.White) // БІЛИЙ ФОН ДЛЯ ПРОЗОРИХ PNG
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .pointerInput(isZoomMode) {
                                                            if (isZoomMode) {
                                                                detectTransformGestures { _, pan, zoom, _ ->
                                                                    // ЗМІНЕНО: Тепер мінімальний ліміт 0.3f (можна віддалити в 3 рази!)
                                                                    scale = (scale * zoom).coerceIn(0.3f, 5f)

                                                                    // Універсальна математика: не дає фотографії вилетіти за краї
                                                                    // ні при сильному наближенні, ні при сильному віддаленні
                                                                    val maxX = (size.width * kotlin.math.abs(scale - 1)) / 2
                                                                    val maxY = (size.height * kotlin.math.abs(scale - 1)) / 2

                                                                    offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                                                    offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                                                                }
                                                            } else {
                                                                detectTapGestures(onLongPress = { isZoomMode = true })
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AsyncImage(
                                                        model = photoUri,
                                                        contentDescription = "Фото варіант $page",
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .graphicsLayer(
                                                                scaleX = scale,
                                                                scaleY = scale,
                                                                translationX = offsetX,
                                                                translationY = offsetY
                                                            ),
                                                        contentScale = ContentScale.Fit
                                                    )

                                                    if (isZoomMode) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color.Black.copy(alpha = 0.5f))
                                                        )
                                                        Text(
                                                            "Режим налаштування (Зумуйте двома пальцями)",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier
                                                                .align(Alignment.TopCenter)
                                                                .padding(top = 16.dp)
                                                        )

                                                        IconButton(
                                                            onClick = {
                                                                saveImageTransformLocally(context, photoUri, scale, offsetX, offsetY)
                                                                isZoomMode = false
                                                            },
                                                            modifier = Modifier
                                                                .align(Alignment.BottomEnd)
                                                                .padding(16.dp)
                                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                                .size(48.dp)
                                                        ) {
                                                            Icon(Icons.Default.Check, "Зберегти", tint = Color.White)
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                val orig = getImageTransformLocally(context, photoUri)
                                                                scale = orig.first
                                                                offsetX = orig.second
                                                                offsetY = orig.third
                                                                isZoomMode = false
                                                            },
                                                            modifier = Modifier
                                                                .align(Alignment.BottomStart)
                                                                .padding(16.dp)
                                                                .background(Color.Gray, CircleShape)
                                                                .size(48.dp)
                                                        ) {
                                                            Icon(Icons.Default.Close, "Скасувати", tint = Color.White)
                                                        }
                                                    }

                                                    if (!isZoomMode) {
                                                        if (isCardEditMode) {
                                                            IconButton(
                                                                onClick = {
                                                                    val options = CropImageContractOptions(Uri.parse(photoUri), CropImageOptions(
                                                                        fixAspectRatio = false,
                                                                        imageSourceIncludeCamera = false,
                                                                        outputCompressFormat = Bitmap.CompressFormat.PNG,

                                                                        borderLineColor = android.graphics.Color.BLACK,
                                                                        borderCornerColor = android.graphics.Color.BLACK,
                                                                        guidelinesColor = android.graphics.Color.BLACK
                                                                    ))
                                                                    imageCropLauncher.launch(options)
                                                                },
                                                                modifier = Modifier
                                                                    .align(Alignment.TopStart)
                                                                    .padding(8.dp)
                                                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                                    .size(36.dp)
                                                            ) {
                                                                Icon(Icons.Default.Edit, contentDescription = "Кадрувати", tint = Color.White, modifier = Modifier.size(20.dp))
                                                            }
                                                        }

                                                        // А от ярлик "Основне" залишаємо завжди
                                                        if (photoUri == product.imagePath) {
                                                            Surface(
                                                                color = Color.Black.copy(alpha = 0.6f),
                                                                shape = RoundedCornerShape(4.dp),
                                                                modifier = Modifier.padding(8.dp).align(Alignment.TopEnd)
                                                            ) {
                                                                Text("Основне", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // 3. ХОВАЄМО КРАПКИ І НИЖНІ КНОПКИ В УМОВУ
                                            if (isCardEditMode) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.wrapContentHeight().fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    repeat(product.imageHistory.size) { iteration ->
                                                        val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                                                        Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(8.dp))
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))
                                                val currentPagerPhoto = product.imageHistory[pagerState.currentPage]
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (currentPagerPhoto != product.imagePath) {
                                                        Button(
                                                            onClick = { viewModel.setPrimaryImage(currentPagerPhoto) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                        ) {
                                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("Зберегти як основну")
                                                        }
                                                    }
                                                    TextButton(onClick = { showBottomSheet = true }) {
                                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Додати варіант")
                                                    }
                                                }
                                            }

                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(150.dp)
                                                    .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                                                    .clickable { showBottomSheet = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Немає зображення", color = Color.Gray)
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Button(onClick = { showBottomSheet = true }) { Text("Додати фото") }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = Color.Black, thickness = 1.dp)
                                        Spacer(modifier = Modifier.height(16.dp))

                                        val displayBarcode = remember(product.barcode, activePrefixes) {
                                            val code = product.barcode
                                            if (code.length <= 8 && activePrefixes.isNotEmpty()) {
                                                val prefix = activePrefixes.first()
                                                "$prefix$code" + "000000000"
                                            } else { code }
                                        }

                                        val barcodeBitmap = remember(displayBarcode) { generateBarcodeBitmap(displayBarcode) }

                                        if (barcodeBitmap != null) {
                                            Image(
                                                bitmap = barcodeBitmap,
                                                contentDescription = "Штрих-код",
                                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = displayBarcode,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp,
                                                fontSize = labelBarcodeSize.sp
                                            ),
                                            color = Color.Black,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                            } ?: run {
                                if (lastScannedBarcode != null && !isMagicScanning) {
                                    if (isCreating) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    text = "Новий товар (Код: $lastScannedBarcode)",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))

                                                var isRecognizingText by remember { mutableStateOf(false) }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    OutlinedTextField(
                                                        value = newNameInput,
                                                        onValueChange = { viewModel.updateNewNameInput(it) },
                                                        label = { Text("Введіть назву для створення") },
                                                        modifier = Modifier.weight(1f),
                                                        maxLines = 3
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = {
                                                            val file = File(context.filesDir, "smart_ocr_${UUID.randomUUID()}.jpg")
                                                            smartOcrFile = file
                                                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                                            smartOcrCameraLauncher.launch(uri)
                                                        },
                                                        modifier = Modifier
                                                            .size(56.dp)
                                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                                    ) {
                                                        if (isMagicScanning) {
                                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                                        } else {
                                                            Icon(Icons.Default.Edit, "Розпізнати текст", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    TextButton(onClick = { viewModel.clearCurrentProduct() }) {
                                                        Text("Скасувати")
                                                    }
                                                    Button(
                                                        onClick = {
                                                            viewModel.createNewProduct(lastScannedBarcode!!, newNameInput)
                                                            rootFocusRequester.requestFocus()
                                                            keyboardController?.hide()
                                                        }
                                                    ) {
                                                        Text("Створити і зберегти")
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Товар не знайдено",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(text = "Код: $lastScannedBarcode", color = Color.Gray)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            if (showCreateNewBtn) {
                                                Button(
                                                    onClick = { viewModel.setIsCreating(true) },
                                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Створити новий товар")
                                                }
                                                if (showScanAnotherBtn) {
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                }
                                            }
                                            if (showScanAnotherBtn) {
                                                OutlinedButton(
                                                    onClick = {
                                                        scanner.startScan().addOnSuccessListener { barcode ->
                                                            barcode.rawValue?.let { viewModel.processBarcode(it) }
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                                ) {
                                                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Сканувати інший")
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            TextButton(onClick = { viewModel.clearCurrentProduct() }) {
                                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Очистити і закрити")
                                            }
                                        }
                                    }
                                } else if (!isMagicScanning) {
                                    Text("Візьміть сканер і відскануйте етикетку", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        smartOcrBitmap?.let { bitmap ->
                            Dialog(
                                onDismissRequest = { smartOcrBitmap = null },
                                properties = DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                SmartOcrScreen(
                                    bitmap = bitmap,
                                    viewModel = viewModel,
                                    onDismiss = { smartOcrBitmap = null },
                                    onBitmapCropped = { croppedBitmap ->
                                        smartOcrBitmap = null
                                        viewModel.performTextRecognition(context, croppedBitmap) { text ->
                                            var cleanName = text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
                                            cleanName = cleanName.replace(Regex("^[^А-Яа-яЄєІіЇїҐґA-Za-z]+"), "")
                                            val correctedName = viewModel.getCorrectedOcrText(cleanName)
                                            viewModel.updateNewNameInput(correctedName)
                                        }
                                        viewModel.scanBarcodeFromBitmap(bitmap, null) { barcode ->
                                            if (barcode != null) {
                                                viewModel.setIsCreating(true)
                                                viewModel.processBarcode(barcode)
                                            } else {
                                                Toast.makeText(context, "Штрих-код не знайдено на фотографії", Toast.LENGTH_LONG).show()
                                                viewModel.setIsCreating(true)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnMapper(label: String, headers: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        val displayValue = if (selectedIndex in headers.indices) headers[selectedIndex] else "Не вибрано"
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Не вибрано (Пропустити)") }, onClick = { onSelect(-1); expanded = false })
            headers.forEachIndexed { index, title ->
                DropdownMenuItem(text = { Text(title) }, onClick = { onSelect(index); expanded = false })
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun ExcelPreviewTable(headers: List<String>, previewData: List<List<String>>) {
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    val tableContent = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .horizontalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(1.dp)
        ) {
            headers.forEachIndexed { colIndex, headerText ->
                Column(
                    modifier = Modifier
                        .widthIn(min = 120.dp, max = 200.dp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = headerText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    previewData.forEach { row ->
                        Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            val cellText = if (colIndex < row.size) row[colIndex] else ""
                            Text(
                                text = cellText.ifEmpty { "-" },
                                maxLines = 1,
                                color = if (cellText.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    }
                }
                if (colIndex < headers.size - 1) {
                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )
                }
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Попередній перегляд файлу:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { isFullscreen = true }) {
                Icon(Icons.Default.Fullscreen, "На весь екран")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        tableContent()
    }
    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Попередній перегляд",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { isFullscreen = false }) {
                            Icon(Icons.Default.FullscreenExit, "Згорнути")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    tableContent()
                }
            }
        }
    }
}