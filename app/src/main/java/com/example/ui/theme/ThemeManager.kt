package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme(
    val id: String,
    val displayName: String,
    val primaryPreviewColor: Color,
    val accentPreviewColor: Color,
    val description: String
) {
    OBSIDIAN_VIOLET(
        id = "obsidian_violet",
        displayName = "Obsidian Violet",
        primaryPreviewColor = Color(0xFF8B5CF6),
        accentPreviewColor = Color(0xFF06B6D4),
        description = "Royal purple and cyan glow on deep obsidian black"
    ),
    SOLAR_AMBER(
        id = "solar_amber",
        displayName = "Solar Amber",
        primaryPreviewColor = Color(0xFFF59E0B),
        accentPreviewColor = Color(0xFFFBBF24),
        description = "Warm golden amber with stellar radiance"
    ),
    CYBER_EMERALD(
        id = "cyber_emerald",
        displayName = "Cyber Emerald",
        primaryPreviewColor = Color(0xFF10B981),
        accentPreviewColor = Color(0xFF06D6A0),
        description = "Futuristic neon emerald and mint on slate"
    ),
    COSMIC_SAPPHIRE(
        id = "cosmic_sapphire",
        displayName = "Cosmic Sapphire",
        primaryPreviewColor = Color(0xFF3B82F6),
        accentPreviewColor = Color(0xFF38BDF8),
        description = "Deep celestial sapphire with ice blue clarity"
    ),
    CRIMSON_NEON(
        id = "crimson_neon",
        displayName = "Crimson Neon",
        primaryPreviewColor = Color(0xFFEF4444),
        accentPreviewColor = Color(0xFFFB7185),
        description = "Intense electric crimson with fiery edge"
    );

    companion object {
        fun fromId(id: String): AppTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OBSIDIAN_VIOLET
        }
    }
}

data class ThemePalette(
    val primary: Color,
    val primaryLight: Color,
    val secondary: Color,
    val secondaryLight: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val card: Color,
    val border: Color,
    val borderStrong: Color,
    val cellSelected: Color,
    val cellRelated: Color,
    val cellSameNumber: Color,
    val cellUserText: Color,
    val cellGivenText: Color = Color(0xFFF1F5F9),
    val cellErrorText: Color = Color(0xFFF87171),
    val cellNoteText: Color = Color(0xFF94A3B8)
)

val ObsidianVioletPalette = ThemePalette(
    primary = Color(0xFF8B5CF6),
    primaryLight = Color(0xFFA78BFA),
    secondary = Color(0xFF06B6D4),
    secondaryLight = Color(0xFF22D3EE),
    tertiary = Color(0xFFEC4899),
    background = Color(0xFF0B0B0E),
    surface = Color(0xFF14141A),
    surfaceElevated = Color(0xFF1C1C24),
    card = Color(0xFF181820),
    border = Color(0xFF2E2E3C),
    borderStrong = Color(0xFF4C4C64),
    cellSelected = Color(0xFF2C2448),
    cellRelated = Color(0xFF181828),
    cellSameNumber = Color(0xFF222838),
    cellUserText = Color(0xFF67E8F9)
)

val SolarAmberPalette = ThemePalette(
    primary = Color(0xFFF59E0B),
    primaryLight = Color(0xFFFBBF24),
    secondary = Color(0xFFF59E0B),
    secondaryLight = Color(0xFFFCD34D),
    tertiary = Color(0xFFFB923C),
    background = Color(0xFF0F0E0C),
    surface = Color(0xFF1C1A14),
    surfaceElevated = Color(0xFF27241C),
    card = Color(0xFF211E16),
    border = Color(0xFF3D3728),
    borderStrong = Color(0xFF66593D),
    cellSelected = Color(0xFF3D3012),
    cellRelated = Color(0xFF211D13),
    cellSameNumber = Color(0xFF2E2718),
    cellUserText = Color(0xFFFBBF24)
)

val CyberEmeraldPalette = ThemePalette(
    primary = Color(0xFF10B981),
    primaryLight = Color(0xFF34D399),
    secondary = Color(0xFF06D6A0),
    secondaryLight = Color(0xFF6EE7B7),
    tertiary = Color(0xFF2DD4BF),
    background = Color(0xFF080E0B),
    surface = Color(0xFF101B15),
    surfaceElevated = Color(0xFF16251D),
    card = Color(0xFF132019),
    border = Color(0xFF233B2E),
    borderStrong = Color(0xFF355E49),
    cellSelected = Color(0xFF143825),
    cellRelated = Color(0xFF0E2218),
    cellSameNumber = Color(0xFF182E22),
    cellUserText = Color(0xFF34D399)
)

val CosmicSapphirePalette = ThemePalette(
    primary = Color(0xFF3B82F6),
    primaryLight = Color(0xFF60A5FA),
    secondary = Color(0xFF38BDF8),
    secondaryLight = Color(0xFF7DD3FC),
    tertiary = Color(0xFF818CF8),
    background = Color(0xFF070B14),
    surface = Color(0xFF0E1626),
    surfaceElevated = Color(0xFF142036),
    card = Color(0xFF111B2E),
    border = Color(0xFF203254),
    borderStrong = Color(0xFF34528A),
    cellSelected = Color(0xFF172B4D),
    cellRelated = Color(0xFF0E1C33),
    cellSameNumber = Color(0xFF162642),
    cellUserText = Color(0xFF7DD3FC)
)

val CrimsonNeonPalette = ThemePalette(
    primary = Color(0xFFEF4444),
    primaryLight = Color(0xFFF87171),
    secondary = Color(0xFFF43F5E),
    secondaryLight = Color(0xFFFB7185),
    tertiary = Color(0xFFE11D48),
    background = Color(0xFF0E0809),
    surface = Color(0xFF1A1012),
    surfaceElevated = Color(0xFF261619),
    card = Color(0xFF201316),
    border = Color(0xFF3D2126),
    borderStrong = Color(0xFF61323B),
    cellSelected = Color(0xFF3E151A),
    cellRelated = Color(0xFF240E12),
    cellSameNumber = Color(0xFF331419),
    cellUserText = Color(0xFFFB7185)
)

fun getPaletteForTheme(theme: AppTheme): ThemePalette = when (theme) {
    AppTheme.OBSIDIAN_VIOLET -> ObsidianVioletPalette
    AppTheme.SOLAR_AMBER -> SolarAmberPalette
    AppTheme.CYBER_EMERALD -> CyberEmeraldPalette
    AppTheme.COSMIC_SAPPHIRE -> CosmicSapphirePalette
    AppTheme.CRIMSON_NEON -> CrimsonNeonPalette
}

val LocalThemePalette = staticCompositionLocalOf { ObsidianVioletPalette }
val LocalActiveTheme = staticCompositionLocalOf { AppTheme.OBSIDIAN_VIOLET }

object ThemeStateHolder {
    private val _currentTheme = MutableStateFlow(AppTheme.OBSIDIAN_VIOLET)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("obsidian_sudoku_settings", Context.MODE_PRIVATE)
        val savedId = prefs.getString("selected_theme_id", AppTheme.OBSIDIAN_VIOLET.id) ?: AppTheme.OBSIDIAN_VIOLET.id
        _currentTheme.value = AppTheme.fromId(savedId)
    }

    fun setTheme(context: Context, theme: AppTheme) {
        _currentTheme.value = theme
        val prefs = context.getSharedPreferences("obsidian_sudoku_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_theme_id", theme.id).apply()
    }
}
