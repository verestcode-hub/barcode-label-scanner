package com.example.verestscannerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.verestscannerapp.ui.theme.VerestScannerAppTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch(Dispatchers.IO) {
            OcrManager.prepareTessData(this@MainActivity)
        }

        val database = AppDatabase.getDatabase(this)
        val dao = database.productDao()

        // 🎯 ВИПРАВЛЕННЯ ТУТ: передаємо application першим параметром
        val factory = ScannerViewModelFactory(application, dao)
        val viewModel = ViewModelProvider(this, factory)[ScannerViewModel::class.java]

        setContent {
            // Підписуємось на потік теми з ViewModel
            val themeMode by viewModel.themeMode.collectAsState()

            // Передаємо themeMode у вашу функцію теми
            VerestScannerAppTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScannerScreen(viewModel = viewModel)
                }
            }
        }
    }
}