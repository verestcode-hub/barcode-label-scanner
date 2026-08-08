@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.verestscannerapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(viewModel: ScannerViewModel, onClose: () -> Unit) {
    var currentSubScreen by remember { mutableStateOf<String?>(null) }

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = {
            if (targetState != null && initialState == null) {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300))
            } else {

                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
            }
        },
        label = "SettingsTransition"
    ) { targetScreen ->
        when (targetScreen) {
            "Interface" -> InterfaceSettings(viewModel) { currentSubScreen = null }
            "Networks" -> NetworksSettings(viewModel) { currentSubScreen = null }
            "Prefixes" -> PrefixesSettings(viewModel) { currentSubScreen = null }
            else -> {
                SettingsBaseScaffold("Налаштування", onClose) {
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItemCard(
                        title = "Інтерфейс",
                        desc = "Кнопки головного екрана та карток",
                        icon = Icons.Default.DisplaySettings,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTintColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) { currentSubScreen = "Interface" }

                    SettingsItemCard(
                        title = "Клієнти",
                        desc = "Список доступних клієнтів",
                        icon = Icons.Default.Lan,
                        iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconTintColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) { currentSubScreen = "Networks" }

                    SettingsItemCard(
                        title = "Префікси штрих-кодів",
                        desc = "Внутрішні коди клієнтів",
                        icon = Icons.Default.Dialpad,
                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTintColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) { currentSubScreen = "Prefixes" }
                }
            }
        }
    }
}

@Composable
fun SettingsBaseScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            content = content
        )
    }
}

@Composable
fun SettingsItemCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
            supportingContent = { Text(desc) },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(iconBgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTintColor)
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun SettingsSwitchCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
            trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun InterfaceSettings(viewModel: ScannerViewModel, onBack: () -> Unit) {
    val showScanButton by viewModel.showScanButton.collectAsState()
    val showMagicButton by viewModel.showMagicButton.collectAsState()
    val showCreateNewBtn by viewModel.showCreateNewBtn.collectAsState()
    val showScanAnotherBtn by viewModel.showScanAnotherBtn.collectAsState()

    BackHandler {
        onBack()
    }

    SettingsBaseScaffold("Інтерфейс", onBack) {
        Text("Головний екран", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 16.dp))

        SettingsSwitchCard("Кнопка 'Знайти товар'", showScanButton) { viewModel.setShowScanButton(it) }
        SettingsSwitchCard("Кнопка 'Створити з фото'", showMagicButton) { viewModel.setShowMagicButton(it) }

        Text("Картка товару", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 16.dp))

        SettingsSwitchCard("Кнопка 'Створити новий'", showCreateNewBtn) { viewModel.setShowCreateNewBtn(it) }
        SettingsSwitchCard("Кнопка 'Сканувати інший'", showScanAnotherBtn) { viewModel.setShowScanAnotherBtn(it) }
    }
}

@Composable
fun NetworksSettings(viewModel: ScannerViewModel, onBack: () -> Unit) {
    val networks by viewModel.customNetworks.collectAsState()
    var newNet by remember { mutableStateOf("") }
    var delNet by remember { mutableStateOf<String?>(null) }
    var confirmText by remember { mutableStateOf("") }

    BackHandler {
        onBack()
    }

    SettingsBaseScaffold("Керування клієнтами", onBack) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newNet,
                onValueChange = { newNet = it },
                modifier = Modifier.weight(1f),
                label = { Text("Назва клієнта") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { viewModel.addNetwork(newNet); newNet = "" }) { Text("Додати") }
        }

        if (networks.isNotEmpty()) {
            Text("Список мереж", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))

            networks.toList().sorted().forEach { net ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    ListItem(
                        headlineContent = { Text(net, fontWeight = FontWeight.Medium) },
                        trailingContent = {
                            IconButton(onClick = { delNet = net }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    if (delNet != null) {
        AlertDialog(
            onDismissRequest = { delNet = null; confirmText = "" },
            title = { Text("Видалити мережу?") },
            text = {
                Column {
                    Text("Введіть 'DELETE' для підтвердження видалення $delNet:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = confirmText, onValueChange = { confirmText = it }, shape = RoundedCornerShape(16.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.removeNetwork(delNet!!); delNet = null; confirmText = "" },
                    enabled = confirmText == "DELETE",
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Видалити") }
            },
            dismissButton = { TextButton(onClick = { delNet = null; confirmText = "" }) { Text("Скасувати") } }
        )
    }
}

@Composable
fun PrefixesSettings(viewModel: ScannerViewModel, onBack: () -> Unit) {
    val prefixes by viewModel.customPrefixes.collectAsState()
    var newPrefix by remember { mutableStateOf("") }

    BackHandler {
        onBack()
    }

    SettingsBaseScaffold("Префікси штрих-кодів", onBack) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newPrefix,
                onValueChange = { newPrefix = it },
                modifier = Modifier.weight(1f),
                label = { Text("Новий префікс (напр. 1324)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newPrefix.isNotBlank()) {
                    viewModel.addPrefix(newPrefix)
                    newPrefix = ""
                }
            }) { Text("Додати") }
        }

        if (prefixes.isNotEmpty()) {
            Text("Список активних префіксів", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))

            prefixes.toList().sorted().forEach { prefix ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    ListItem(
                        headlineContent = { Text(prefix, fontWeight = FontWeight.Medium) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removePrefix(prefix) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}