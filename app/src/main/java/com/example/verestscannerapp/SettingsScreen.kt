@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.verestscannerapp

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            "AutoBackup" -> AutoBackupSettings(viewModel) { currentSubScreen = null }
            "About" -> AboutDeveloperScreen { currentSubScreen = null }

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

                    SettingsItemCard(
                        title = "Автобекап",
                        desc = "Налаштування резервного копіювання",
                        icon = Icons.Default.CloudSync,
                        iconBgColor = MaterialTheme.colorScheme.errorContainer,
                        iconTintColor = MaterialTheme.colorScheme.onErrorContainer
                    ) { currentSubScreen = "AutoBackup" }

                    SettingsItemCard(
                        title = "Про додаток та Підтримка",
                        desc = "Версія, зв'язок з розробником",
                        icon = Icons.Default.Info,
                        iconBgColor = MaterialTheme.colorScheme.primary,
                        iconTintColor = MaterialTheme.colorScheme.onPrimary
                    ) { currentSubScreen = "About" }
                }
            }
        }
    }
}

@Composable
fun AboutDeveloperScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var clickCount by remember { mutableStateOf(0) }

    BackHandler {
        onBack()
    }

    SettingsBaseScaffold("Про додаток", onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Verest Scanner",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Версія 1.0.0",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Розробка та підтримка:", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Green Crane",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            clickCount++
                            if (clickCount == 7) {
                                android.widget.Toast.makeText(
                                    context,
                                    "🎉 Вітаю! Ви знайшли пасхалку! З любов'ю, Розробник 😉",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                clickCount = 0
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://t.me/nazarzajcev3")
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC))
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Написати в Telegram")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:nazarzajcev3@gmail.com")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Підтримка: Verest Scanner")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Поштовий додаток не знайдено", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Написати на Email")
                    }
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

@Composable
fun AutoBackupSettings(viewModel: ScannerViewModel, onBack: () -> Unit) {
    val context = LocalContext.current

    val backupFreq by viewModel.autoBackupFrequency.collectAsState()
    val backupUri by viewModel.autoBackupUri.collectAsState()
    var expandedFreq by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setAutoBackupUri(it.toString())
        }
    }

    BackHandler {
        onBack()
    }

    SettingsBaseScaffold("Резервне копіювання", onBack) {
        Text("Налаштування автобекапу", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 16.dp))

        // ВИБІР ЧАСТОТИ (Додано "Щомісяця")
        ExposedDropdownMenuBox(
            expanded = expandedFreq,
            onExpandedChange = { expandedFreq = it }
        ) {
            OutlinedTextField(
                value = backupFreq,
                onValueChange = {},
                readOnly = true,
                label = { Text("Частота бекапу") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFreq) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(24.dp)
            )
            ExposedDropdownMenu(expanded = expandedFreq, onDismissRequest = { expandedFreq = false }) {
                listOf("Ніколи", "Щодня", "Щотижня", "Щомісяця").forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            viewModel.setAutoBackupFrequency(option)
                            expandedFreq = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ВИБІР ПАПКИ
        if (backupFreq != "Ніколи") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Місце збереження:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (backupUri.isEmpty()) "За замовчуванням (внутрішня пам'ять)" else "Власна папка (або Google Диск) обрана",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 Підказка: Виберіть папку вашого Google Диску у системному вікні, щоб бекапи одразу летіли в хмару.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Змінити папку")
                    }

                    if (backupUri.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.setAutoBackupUri("") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Скинути за замовчуванням", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}