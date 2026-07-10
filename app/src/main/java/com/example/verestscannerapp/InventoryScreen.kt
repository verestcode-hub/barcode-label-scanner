package com.example.verestscannerapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: ScannerViewModel, onBackClick: () -> Unit) {
    val products by viewModel.allProducts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Інвентар (${products.size} шт.)") },
                navigationIcon = {
                    Text(
                        text = " ⬅️ ",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .clickable { onBackClick() }
                            .padding(8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        if (products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("База даних порожня", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(products) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.processBarcode(product.barcode)
                                onBackClick() // Закриваємо інвентар
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                        .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp)),
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

                            Text(
                                text = "🗑️",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier
                                    .clickable { viewModel.deleteProduct(product) }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}