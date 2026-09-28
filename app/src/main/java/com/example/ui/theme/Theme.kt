package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBg,
    primaryContainer = ObsidianCard,
    onPrimaryContainer = NeonCyan,
    secondary = NeonAmber,
    onSecondary = ObsidianBg,
    secondaryContainer = ObsidianSurfaceVariant,
    onSecondaryContainer = NeonAmber,
    tertiary = NeonPurple,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    outlineVariant = ObsidianDivider,
    error = NeonRose,
    onError = Color.White
)

@Composable
fun ObsidianSudokuTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ObsidianDarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Keep alias if needed
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    ObsidianSudokuTheme(content = content)
}
