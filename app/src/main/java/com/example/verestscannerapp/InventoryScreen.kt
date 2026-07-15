package com.example.verestscannerapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(viewModel: ScannerViewModel, onBackClick: () -> Unit) {
    val products by viewModel.allProducts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedProducts by remember { mutableStateOf(setOf<Product>()) }
    val isSelectionMode = selectedProducts.isNotEmpty()

    var isGridView by remember { mutableStateOf(false) }
    var savedGridColumns by remember { mutableStateOf(2) }
    var rememberGridChoice by remember { mutableStateOf(true) }
    var showGridSettings by remember { mutableStateOf(false) }
    var fontSizeScale by remember { mutableStateOf(1.0f) }

    val currentColumns = if (isGridView) savedGridColumns else 1
    val gridState = rememberLazyGridState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmationText by remember { mutableStateOf("") }

    val filteredProducts = products.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.barcode.contains(searchQuery, ignoreCase = true) ||
                it.plu.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("Вибрано: ${selectedProducts.size}")
                    } else {
                        Text("Інвентар (${filteredProducts.size})")
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedProducts = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Скасувати виділення")
                        }
                    } else {
                        IconButton(onClick = { onBackClick() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Видалити вибрані",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            if (!isGridView && !rememberGridChoice) {
                                savedGridColumns = 2
                            }
                            isGridView = !isGridView
                        }) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = "Змінити вигляд"
                            )
                        }
                        IconButton(onClick = { showGridSettings = !showGridSettings }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Налаштування сітки")
                        }
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
        ) {
            if (!isSelectionMode) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Пошук за назвою або кодом...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Пошук")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (showGridSettings && !isSelectionMode) {
                GridSettingsPanel(
                    savedColumns = savedGridColumns,
                    onColumnsChange = {
                        savedGridColumns = it
                        isGridView = true
                    },
                    rememberChoice = rememberGridChoice,
                    onRememberChange = { rememberGridChoice = it },
                    fontSizeScale = fontSizeScale,
                    onFontSizeChange = { fontSizeScale = it }
                )
            }

            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("База даних порожня", color = Color.Gray)
                }
            } else if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("За вашим запитом нічого не знайдено", color = Color.Gray)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(currentColumns),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProducts) { product ->
                            val isSelected = selectedProducts.contains(product)

                            val imageHeight = when (currentColumns) {
                                1 -> 60.dp
                                2 -> 110.dp
                                3 -> 90.dp
                                4 -> 75.dp
                                else -> 60.dp
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelectionMode) {
                                                selectedProducts = if (isSelected) {
                                                    selectedProducts - product
                                                } else {
                                                    selectedProducts + product
                                                }
                                            } else {
                                                viewModel.processBarcode(product.barcode)
                                                onBackClick()
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) {
                                                selectedProducts = selectedProducts + product
                                            }
                                        }
                                    ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                if (currentColumns > 1) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            if (product.imagePath.isNotEmpty()) {
                                                AsyncImage(
                                                    model = product.imagePath,
                                                    contentDescription = "Фото",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(imageHeight)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(imageHeight)
                                                        .background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Нема", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }

                                            if (isSelectionMode) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = null,
                                                    modifier = Modifier.align(Alignment.TopEnd)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = product.name.ifEmpty { "Без назви" },
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * fontSizeScale).sp),
                                            maxLines = if (currentColumns >= 4) 1 else 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (currentColumns <= 3) {
                                            Text(
                                                text = product.barcode,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * fontSizeScale).sp),
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (product.imagePath.isNotEmpty()) {
                                            AsyncImage(
                                                model = product.imagePath,
                                                contentDescription = "Фото",
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Нема", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = product.name.ifEmpty { "Без назви" },
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * fontSizeScale).sp),
                                            )
                                            Text(text = "Код: ${product.barcode}", style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * fontSizeScale).sp))
                                            if (product.plu.isNotEmpty()) {
                                                Text(text = "PLU: ${product.plu}", style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * fontSizeScale).sp), color = Color.Gray)
                                            }
                                        }

                                        if (isSelectionMode) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FastScroller(
                        gridState = gridState,
                        totalItems = filteredProducts.size,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    deleteConfirmationText = ""
                    if (!isSelectionMode) selectedProducts = emptySet()
                },
                title = { Text("Підтвердження видалення") },
                text = {
                    Column {
                        Text("Ви збираєтесь видалити ${selectedProducts.size} товар(ів). Цю дію неможливо буде скасувати.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Для підтвердження введіть слово DELETE:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = deleteConfirmationText,
                            onValueChange = { deleteConfirmationText = it },
                            singleLine = true,
                            isError = deleteConfirmationText.isNotEmpty() && deleteConfirmationText != "DELETE",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedProducts.forEach { viewModel.deleteProduct(it) }
                            showDeleteDialog = false
                            deleteConfirmationText = ""
                            selectedProducts = emptySet()
                        },
                        enabled = deleteConfirmationText == "DELETE",
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Видалити")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        deleteConfirmationText = ""
                        if (!isSelectionMode && selectedProducts.size == 1) {
                            selectedProducts = emptySet()
                        }
                    }) {
                        Text("Скасувати")
                    }
                }
            )
        }
    }
}

@Composable
fun GridSettingsPanel(
    savedColumns: Int,
    onColumnsChange: (Int) -> Unit,
    rememberChoice: Boolean,
    onRememberChange: (Boolean) -> Unit,
    fontSizeScale: Float,
    onFontSizeChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("Стовпців: $savedColumns", fontWeight = FontWeight.Bold)
        Slider(value = savedColumns.toFloat(), onValueChange = { onColumnsChange(it.toInt()) }, valueRange = 2f..6f, steps = 4)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Розмір шрифту: ${(fontSizeScale * 100).toInt()}%", fontWeight = FontWeight.Bold)
        Slider(value = fontSizeScale, onValueChange = { onFontSizeChange(it) }, valueRange = 0.8f..1.5f)

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onRememberChange(!rememberChoice) }) {
            Checkbox(checked = rememberChoice, onCheckedChange = { onRememberChange(it) })
            Text("Запам'ятати вибір")
        }
    }
}

@Composable
fun FastScroller(
    gridState: LazyGridState,
    totalItems: Int,
    modifier: Modifier = Modifier
) {
    if (totalItems <= 10) return

    val scope = rememberCoroutineScope()
    val firstVisible by remember { derivedStateOf { gridState.firstVisibleItemIndex } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(32.dp)
            .padding(end = 4.dp, top = 8.dp, bottom = 8.dp)
    ) {
        val thumbHeight = 48.dp
        val maxOffset = maxHeight - thumbHeight
        val proportion = (firstVisible.toFloat() / totalItems).coerceIn(0f, 1f)
        val thumbY = (proportion * maxOffset.value).dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(totalItems) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val y = change.position.y
                        val dragProportion = (y / size.height.toFloat()).coerceIn(0f, 1f)
                        val target = (dragProportion * totalItems).toInt().coerceIn(0, totalItems - 1)
                        scope.launch { gridState.scrollToItem(target) }
                    }
                }
                .pointerInput(totalItems) {
                    detectTapGestures { offset ->
                        val dragProportion = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                        val target = (dragProportion * totalItems).toInt().coerceIn(0, totalItems - 1)
                        scope.launch { gridState.scrollToItem(target) }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .offset(y = thumbY)
                    .height(thumbHeight)
                    .width(6.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            )
        }
    }
}