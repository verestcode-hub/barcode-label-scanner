package com.example.verestscannerapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.verestscannerapp.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val VerestColorScheme = lightColorScheme(
    primary = VerestBurgundy,
    onPrimary = VerestGold,
    primaryContainer = VerestBurgundy,
    onPrimaryContainer = VerestGold,
    secondary = VerestGold,
    onSecondary = VerestBurgundy,
    secondaryContainer = VerestSurface,
    onSecondaryContainer = VerestBurgundy,
    background = VerestCreamBg,
    onBackground = VerestBurgundy,
    surface = VerestCreamBg,
    onSurface = VerestBurgundy,
    surfaceVariant = VerestSurface,
    onSurfaceVariant = VerestBurgundy,
    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun VerestScannerAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        else -> false
    }

    val colorScheme = when {
        themeMode == ThemeMode.VEREST -> VerestColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}