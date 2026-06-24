package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ColorPrimary,
    secondary = ColorAccent,
    tertiary = Color(0xFFB3261E),
    background = BgMain,
    surface = ColorWhite,
    onPrimary = ColorWhite,
    onSecondary = ColorOnAccent,
    onTertiary = ColorWhite,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = BgInput,
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = ColorPrimary,
    secondary = ColorAccent,
    tertiary = Color(0xFFB3261E),
    background = BgMain,
    surface = ColorWhite,
    onPrimary = ColorWhite,
    onSecondary = ColorOnAccent,
    onTertiary = ColorWhite,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = BgInput,
    onSurfaceVariant = TextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark mode as standard for media player context
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
