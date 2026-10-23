package com.aminul.multibook.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FacebookBlue,
    onPrimary = FbSurfaceLight,
    primaryContainer = FacebookDarkBlue,
    onPrimaryContainer = FbTextPrimaryDark,
    background = FbBackgroundDark,
    onBackground = FbTextPrimaryDark,
    surface = FbSurfaceDark,
    onSurface = FbTextPrimaryDark,
    surfaceVariant = FbSurfaceElevatedDark,
    onSurfaceVariant = FbTextSecondaryDark,
    outline = FbDividerDark
)

private val LightColorScheme = lightColorScheme(
    primary = FacebookBlue,
    onPrimary = FbSurfaceLight,
    primaryContainer = FacebookLightBlue,
    onPrimaryContainer = FacebookDarkBlue,
    background = FbBackgroundLight,
    onBackground = FbTextPrimaryLight,
    surface = FbSurfaceLight,
    onSurface = FbTextPrimaryLight,
    surfaceVariant = FbSurfaceLight,
    onSurfaceVariant = FbTextSecondaryLight,
    outline = FbDividerLight
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun MultiBookTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
