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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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

enum class SortType(val title: String) {
    NAME("За алфавітом (Назва)"),
    PLU("За PLU-кодом"),
    DATE_ADDED("За датою додавання"),
    DATE_MODIFIED("За датою зміни")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(
    viewModel: ScannerViewModel,
    onClose: () -> Unit,
    onProductSelected: (String) -> Unit,
    gridState: LazyGridState,
    isGridView: Boolean,
    onGridViewChange: (Boolean) -> Unit,
    savedGridColumns: Int,
    onColumnsChange: (Int) -> Unit,
    rememberGridChoice: Boolean,
    onRememberChange: (Boolean) -> Unit,
    fontSizeScale: Float,
    onFontSizeChange: (Float) -> Unit,
    sortType: SortType,
    onSortTypeChange: (SortType) -> Unit,
    isSortAscending: Boolean,
    onSortOrderChange: (Boolean) -> Unit
) {
    val products by viewModel.allProducts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedProducts by remember { mutableStateOf(setOf<Product>()) }
    val isSelectionMode = selectedProducts.isNotEmpty()

    var showGridSettings by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    val currentColumns = if (isGridView) savedGridColumns else 1

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmationText by remember { mutableStateOf("") }

    val sortedAndFilteredProducts = remember(products, searchQuery, sortType, isSortAscending) {
        val filtered = products.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.barcode.contains(searchQuery, ignoreCase = true) ||
                    it.plu.contains(searchQuery, ignoreCase = true)
        }

        val sorted = when (sortType) {
            SortType.NAME -> filtered.sortedBy {
                it.name.trim().replace("\"", "").replace("«", "").replace("»", "").lowercase()
            }
            SortType.PLU -> filtered.sortedBy { it.plu.toIntOrNull() ?: Int.MAX_VALUE }
            SortType.DATE_ADDED -> filtered.sortedBy { it.createdAt }
            SortType.DATE_MODIFIED -> filtered.sortedBy { it.updatedAt }
        }

        if (isSortAscending) sorted else sorted.reversed()
    }

    androidx.activity.compose.BackHandler {
        if (isSelectionMode) {
            selectedProducts = emptySet()
        } else {
            onClose()
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(onDismissRequest = { showSortSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Сортування",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                SortType.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSortTypeChange(option) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        RadioButton(
                            selected = sortType == option,
                            onClick = { onSortTypeChange(option) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(option.title, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSortOrderChange(!isSortAscending) }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isSortAscending) "За зростанням" else "За спаданням", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (isSortAscending) "(А-Я, 0-9, спочатку старі)" else "(Я-А, 9-0, спочатку нові)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isSortAscending,
                        onCheckedChange = { onSortOrderChange(it) }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("Вибрано: ${selectedProducts.size}")
                    } else {
                        Text("Інвентар (${sortedAndFilteredProducts.size})")
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedProducts = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Скасувати виділення")
                        }
                    } else {
                        IconButton(onClick = { onClose() }) {
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
                        IconButton(onClick = { showSortSheet = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Сортувати")
                        }

                        IconButton(onClick = {
                            if (!isGridView && !rememberGridChoice) {
                                onColumnsChange(2)
                            }
                            onGridViewChange(!isGridView)
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
                    onColumnsChange = { newCols ->
                        onColumnsChange(newCols)
                        onGridViewChange(true)
                    },
                    rememberChoice = rememberGridChoice,
                    onRememberChange = { newChoice -> onRememberChange(newChoice) },
                    fontSizeScale = fontSizeScale,
                    onFontSizeChange = { newScale -> onFontSizeChange(newScale) }
                )
            }

            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("База даних порожня", color = Color.Gray)
                }
            } else if (sortedAndFilteredProducts.isEmpty()) {
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
                        items(sortedAndFilteredProducts) { product ->
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
                                                onProductSelected(product.barcode)
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
                        totalItems = sortedAndFilteredProducts.size,
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