package com.example.verestscannerapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
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

fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String {
    val filename = "IMG_${UUID.randomUUID()}.jpg"
    val file = File(context.filesDir, filename)
    return try {
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        fos.flush()
        fos.close()
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
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

    // 🎯 Всі ці стани тепер зберігаються навіть при повороті екрана!
    var showInventory by rememberSaveable { mutableStateOf(false) }
    var isViewingFromInventory by rememberSaveable { mutableStateOf(false) }

    var isGridView by rememberSaveable { mutableStateOf(false) }
    var savedGridColumns by rememberSaveable { mutableIntStateOf(2) }
    var rememberGridChoice by rememberSaveable { mutableStateOf(true) }
    var fontSizeScale by rememberSaveable { mutableFloatStateOf(1.0f) }

    var sortType by rememberSaveable { mutableStateOf(SortType.DATE_ADDED) }
    var isSortAscending by rememberSaveable { mutableStateOf(false) }

    val gridState = rememberLazyGridState()

    if (showInventory) {
        InventoryScreen(
            viewModel = viewModel,
            onClose = {
                showInventory = false
                isViewingFromInventory = false
            },
            onProductSelected = { barcode ->
                isViewingFromInventory = true
                showInventory = false
                viewModel.processBarcode(barcode)
            },
            gridState = gridState,
            isGridView = isGridView,
            onGridViewChange = { isGridView = it },
            savedGridColumns = savedGridColumns,
            onColumnsChange = { savedGridColumns = it },
            rememberGridChoice = rememberGridChoice,
            onRememberChange = { rememberGridChoice = it },
            fontSizeScale = fontSizeScale,
            onFontSizeChange = { fontSizeScale = it },
            sortType = sortType,
            onSortTypeChange = { sortType = it },
            isSortAscending = isSortAscending,
            onSortOrderChange = { isSortAscending = it }
        )
        return
    }

    val scannedProduct by viewModel.scannedProduct.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val lastScannedBarcode by viewModel.lastScannedBarcode.collectAsState()

    var scanInput by remember { mutableStateOf("") }

    val rootFocusRequester = remember { FocusRequester() }
    val manualFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var isEditing by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var isManualInputMode by remember { mutableStateOf(false) }

    var showBottomSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val scanner = remember { GmsBarcodeScanning.getClient(context) }

    var isMagicScanning by remember { mutableStateOf(false) }
    var prefilledName by remember { mutableStateOf("") }

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
                                    prefilledName = text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()

                                    viewModel.scanBarcodeFromBitmap(origBitmap, cropRect) { barcode ->
                                        isMagicScanning = false
                                        if (barcode != null) {
                                            isCreating = true
                                            viewModel.processBarcode(barcode)
                                        } else {
                                            Toast.makeText(context, "Штрих-код не знайдено на фотографії", Toast.LENGTH_LONG).show()
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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                viewModel.updateProductImage(it.toString())
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            bitmap?.let {
                val savedUriString = saveBitmapToInternalStorage(context, it)
                if (savedUriString.isNotEmpty()) {
                    viewModel.updateProductImage(savedUriString)
                }
            }
        }
    )

    val backupPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                viewModel.importBackup(context, it)
            }
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

    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                Text(
                    text = "Як додати фото етикетки?",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        showBottomSheet = false
                        cameraLauncher.launch(null)
                    }.padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Вибрати з галереї", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Меню",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Інвентар") },
                    label = { Text("Показати весь інвентар") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showInventory = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Імпорт") },
                    label = { Text("Імпорт JSON") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.importDataFromJson(context)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(imageVector = Icons.Default.Share, contentDescription = "Бекап") },
                    label = { Text("Зробити і надіслати бекап") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.exportBackup(context)
                        scope.launch {
                            delay(1000)
                            viewModel.shareBackup(context)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(imageVector = Icons.Default.Refresh, contentDescription = "Відновити") },
                    label = { Text("Відновити з бекапу") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        backupPickerLauncher.launch(arrayOf("application/zip"))
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
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
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Відкрити меню")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                    scanInput = ""
                                }
                                return@onPreviewKeyEvent true
                            }
                            val char = keyEvent.utf16CodePoint.toChar()
                            if (char in ' '..'~') {
                                scanInput += char
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
                        onValueChange = { scanInput = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(manualFocusRequester),
                        label = { Text("Введіть штрих-код вручну") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (scanInput.isNotBlank()) {
                                    viewModel.processBarcode(scanInput.trim())
                                    scanInput = ""
                                }
                                isManualInputMode = false
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { isManualInputMode = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Закрити")
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
                        Box(modifier = Modifier.matchParentSize().clickable { isManualInputMode = true })
                    }
                }

                if (isMagicScanning) {
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Розпізнавання етикетки...", style = MaterialTheme.typography.bodyLarge)
                } else if (scannedProduct == null && lastScannedBarcode == null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isCreating = false
                            scanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    barcode.rawValue?.let { viewModel.processBarcode(it) }
                                }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Знайти товар", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            magicCropLauncher.launch(
                                CropImageContractOptions(
                                    uri = null,
                                    cropImageOptions = CropImageOptions().apply {
                                        imageSourceIncludeCamera = true
                                        imageSourceIncludeGallery = false
                                        guidelines = CropImageView.Guidelines.ON
                                        allowRotation = true
                                        allowCounterRotation = true
                                        multiTouchEnabled = true
                                    }
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Створити з фото етикетки", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                saveStatus?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                scannedProduct?.let { product ->
                    OutlinedButton(
                        onClick = {
                            viewModel.clearCurrentProduct()

                            if (isViewingFromInventory) {
                                showInventory = true
                            } else {
                                rootFocusRequester.requestFocus()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
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
                                    label = { Text("Дата придатності") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = groupInput,
                                    onValueChange = { groupInput = it },
                                    label = { Text("Мережа / Група") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = {
                                    viewModel.updateProductDetails(nameInput, pluInput, dateInput, groupInput)
                                    isEditing = false
                                    rootFocusRequester.requestFocus()
                                    keyboardController?.hide()
                                }) { Text("Зберегти зміни") }
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Системні дані:", fontWeight = FontWeight.Bold)
                                    Button(onClick = { isEditing = true }) { Text("Редагувати") }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Мережа: ${product.groupPlu.ifEmpty { "Не вказано" }}")
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
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = product.name.ifEmpty { "БЕЗ НАЗВИ" }.uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (product.imageHistory.isNotEmpty()) {
                                val pagerState = rememberPagerState(
                                    initialPage = product.imageHistory.indexOf(product.imagePath).coerceAtLeast(0),
                                    pageCount = { product.imageHistory.size }
                                )

                                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(220.dp)) { page ->
                                    val photoUri = product.imageHistory[page]
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                                        AsyncImage(
                                            model = photoUri,
                                            contentDescription = "Фото варіант $page",
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )

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

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(Modifier.wrapContentHeight().fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    repeat(product.imageHistory.size) { iteration ->
                                        val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                                        Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(8.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val currentPagerPhoto = product.imageHistory[pagerState.currentPage]

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                    if (currentPagerPhoto != product.imagePath) {
                                        Button(
                                            onClick = { viewModel.setPrimaryImage(currentPagerPhoto) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Зберегти як основну")
                                        }
                                    }

                                    TextButton(onClick = { showBottomSheet = true }) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Додати варіант")
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
                                        Button(onClick = { showBottomSheet = true }) {
                                            Text("Додати фото")
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color.Black, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            val barcodeBitmap = remember(product.barcode) { generateBarcodeBitmap(product.barcode) }

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
                                text = product.barcode,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = "Новий товар (Код: $lastScannedBarcode)", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    var newNameInput by remember(lastScannedBarcode) { mutableStateOf(if (prefilledName.isNotEmpty()) prefilledName else "") }
                                    var isRecognizingText by remember { mutableStateOf(false) }

                                    val manualTextCropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
                                        if (result.isSuccessful) {
                                            result.uriContent?.let { croppedUri ->
                                                isRecognizingText = true
                                                scope.launch(Dispatchers.IO) {
                                                    try {
                                                        val inputStream = context.contentResolver.openInputStream(croppedUri)
                                                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                                                        inputStream?.close()

                                                        if (bitmap != null) {
                                                            withContext(Dispatchers.Main) {
                                                                viewModel.performTextRecognition(context, bitmap) { text ->
                                                                    newNameInput = text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
                                                                    isRecognizingText = false
                                                                }
                                                            }
                                                        } else {
                                                            withContext(Dispatchers.Main) { isRecognizingText = false }
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        withContext(Dispatchers.Main) { isRecognizingText = false }
                                                    }
                                                }
                                            }
                                        } else {
                                            isRecognizingText = false
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = newNameInput,
                                            onValueChange = { newNameInput = it },
                                            label = { Text("Введіть назву для створення") },
                                            modifier = Modifier.weight(1f),
                                            maxLines = 3
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(
                                            onClick = {
                                                manualTextCropLauncher.launch(
                                                    CropImageContractOptions(
                                                        uri = null,
                                                        cropImageOptions = CropImageOptions().apply {
                                                            imageSourceIncludeGallery = true
                                                            imageSourceIncludeCamera = true
                                                            guidelines = CropImageView.Guidelines.ON
                                                            allowRotation = true
                                                            allowCounterRotation = true
                                                            multiTouchEnabled = true
                                                        }
                                                    )
                                                )
                                            },
                                            modifier = Modifier
                                                .size(56.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                        ) {
                                            if (isRecognizingText) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Розпізнати текст",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        TextButton(onClick = {
                                            isCreating = false
                                            prefilledName = ""
                                            viewModel.clearCurrentProduct()
                                        }) { Text("Скасувати") }

                                        Button(onClick = {
                                            viewModel.createNewProduct(lastScannedBarcode!!, newNameInput)
                                            isCreating = false
                                            prefilledName = ""
                                            rootFocusRequester.requestFocus()
                                            keyboardController?.hide()
                                        }) { Text("Створити і зберегти") }
                                    }
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("Товар не знайдено", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                                Text("Код: $lastScannedBarcode", color = Color.Gray)

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { isCreating = true },
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Створити новий товар")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        scanner.startScan()
                                            .addOnSuccessListener { barcode ->
                                                barcode.rawValue?.let {
                                                    viewModel.processBarcode(it)
                                                }
                                            }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Сканувати інший")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                TextButton(onClick = { viewModel.clearCurrentProduct() }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
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
        }
    }
}