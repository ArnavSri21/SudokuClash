package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ObsidianPrimary,
    onPrimary = ObsidianBg,
    primaryContainer = ObsidianSurfaceElevated,
    onPrimaryContainer = ObsidianPrimaryLight,
    secondary = ObsidianSecondary,
    onSecondary = ObsidianBg,
    secondaryContainer = ObsidianSurfaceElevated,
    onSecondaryContainer = ObsidianSecondaryLight,
    tertiary = ObsidianTertiary,
    background = ObsidianBg,
    onBackground = CellGivenText,
    surface = ObsidianSurface,
    onSurface = CellGivenText,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = CellNoteText,
    outline = ObsidianBorder,
    error = ObsidianError,
    onError = CellGivenText
)

@Composable
fun ObsidianSudokuTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ObsidianBg.toArgb()
                window.navigationBarColor = ObsidianBg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
