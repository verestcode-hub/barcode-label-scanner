package com.example.verestscannerapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(viewModel: ScannerViewModel, onBackClick: () -> Unit) {
    val products by viewModel.allProducts.collectAsState()

    // Стан для пошуку
    var searchQuery by remember { mutableStateOf("") }

    var selectedProducts by remember { mutableStateOf(setOf<Product>()) }
    val isSelectionMode = selectedProducts.isNotEmpty()

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
                        Text("Інвентар (${filteredProducts.size} шт.)")
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedProducts = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Скасувати виділення")
                        }
                    } else {
                        IconButton(onClick = { onBackClick() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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

            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("База даних порожня", color = Color.Gray)
                }
            } else if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("За вашим запитом нічого не знайдено", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts) { product ->
                        val isSelected = selectedProducts.contains(product)

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
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(text = "Код: ${product.barcode}", style = MaterialTheme.typography.bodyMedium)
                                    if (product.plu.isNotEmpty()) {
                                        Text(text = "PLU: ${product.plu}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }

                                if (isSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            selectedProducts = setOf(product)
                                            showDeleteDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Видалити",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
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