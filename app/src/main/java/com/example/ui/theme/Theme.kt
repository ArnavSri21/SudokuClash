package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun ObsidianSudokuTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ThemeStateHolder.initialize(context)
    }

    val activeTheme by ThemeStateHolder.currentTheme.collectAsState()
    val palette = getPaletteForTheme(activeTheme)

    val colorScheme = darkColorScheme(
        primary = palette.primary,
        onPrimary = palette.background,
        primaryContainer = palette.surfaceElevated,
        onPrimaryContainer = palette.primaryLight,
        secondary = palette.secondary,
        onSecondary = palette.background,
        secondaryContainer = palette.surfaceElevated,
        onSecondaryContainer = palette.secondaryLight,
        tertiary = palette.tertiary,
        background = palette.background,
        onBackground = palette.cellGivenText,
        surface = palette.surface,
        onSurface = palette.cellGivenText,
        surfaceVariant = palette.card,
        onSurfaceVariant = palette.cellNoteText,
        outline = palette.border,
        error = palette.cellErrorText,
        onError = palette.cellGivenText
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = palette.background.toArgb()
                window.navigationBarColor = palette.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalActiveTheme provides activeTheme,
        LocalThemePalette provides palette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
