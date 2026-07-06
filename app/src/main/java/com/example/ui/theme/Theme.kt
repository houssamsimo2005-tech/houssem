package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlue,
    onPrimary = Color.Black,
    primaryContainer = SkyBlueDark,
    onPrimaryContainer = Color.White,
    secondary = SkyBlueLight,
    onSecondary = Color.Black,
    tertiary = GoldenWarning,
    background = DeepBlack,
    onBackground = TextWhite,
    surface = Charcoal,
    onSurface = TextWhite,
    surfaceVariant = CharcoalLight,
    onSurfaceVariant = TextGray,
    outline = DarkGray
)

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    onPrimary = Color.White,
    primaryContainer = SkyBlueLight,
    onPrimaryContainer = Color.Black,
    secondary = SkyBlueDark,
    onSecondary = Color.White,
    tertiary = GoldenWarning,
    background = Color(0xFFF0F4F8),
    onBackground = Color(0xFF0F131F),
    surface = Color.White,
    onSurface = Color(0xFF0F131F),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF4A5568),
    outline = Color(0xFFCBD5E0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme by default as requested
    dynamicColor: Boolean = false, // Disable to preserve custom brand style
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
