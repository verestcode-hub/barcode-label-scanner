package com.example.verestscannerapp

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_settings")

class SettingsManager(private val context: Context) {
    companion object {
        val SHOW_SCAN_BTN = booleanPreferencesKey("show_scan_btn")
        val SHOW_MAGIC_BTN = booleanPreferencesKey("show_magic_btn")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val THEME_MODE = stringPreferencesKey("theme_mode") // Зберігаємо як String (назву enum)
    }

    // Потоки даних (Flow)
    val settingsFlow = context.dataStore.data.map { prefs ->
        UserSettings(
            showScanBtn = prefs[SHOW_SCAN_BTN] ?: true,
            showMagicBtn = prefs[SHOW_MAGIC_BTN] ?: true,
            gridColumns = prefs[GRID_COLUMNS] ?: 2,
            themeMode = prefs[THEME_MODE] ?: "SYSTEM"
        )
    }

    suspend fun updateScanBtn(value: Boolean) = context.dataStore.edit { it[SHOW_SCAN_BTN] = value }
    suspend fun updateMagicBtn(value: Boolean) = context.dataStore.edit { it[SHOW_MAGIC_BTN] = value }
    suspend fun updateGridColumns(value: Int) = context.dataStore.edit { it[GRID_COLUMNS] = value }
    suspend fun updateTheme(value: String) = context.dataStore.edit { it[THEME_MODE] = value }
}

data class UserSettings(val showScanBtn: Boolean, val showMagicBtn: Boolean, val gridColumns: Int, val themeMode: String)