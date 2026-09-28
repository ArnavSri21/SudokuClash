package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextMuted

@Composable
fun StickerGlyph(
    iconType: String,
    accentColorHex: String,
    isUnlocked: Boolean,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(accentColorHex))
    } catch (e: Exception) {
        NeonCyan
    }

    val iconVector = when (iconType) {
        "SPARK" -> Icons.Default.Flare
        "TRIANGLE" -> Icons.Default.Category
        "WAVE" -> Icons.Default.Waves
        "FLAME" -> Icons.Default.LocalFireDepartment
        "SHIELD" -> Icons.Default.Shield
        "NEURAL" -> Icons.Default.Psychology
        "MOON" -> Icons.Default.DarkMode
        "PRISM" -> Icons.Default.AutoAwesome
        "CROWN" -> Icons.Default.Star
        "DIAMOND" -> Icons.Default.Diamond
        "LIGHTNING" -> Icons.Default.Bolt
        "CUBE" -> Icons.Default.Category
        "TROPHY" -> Icons.Default.EmojiEvents
        else -> Icons.Default.AutoAwesome
    }

    val bgBrush = if (isUnlocked) {
        Brush.radialGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.35f),
                ObsidianCard,
                Color(0xFF0F1522)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF141924),
                Color(0xFF0F131C)
            )
        )
    }

    val borderColor = if (isUnlocked) accentColor.copy(alpha = 0.8f) else ObsidianBorder

    Box(
        modifier = modifier
            .size(size)
            .shadow(if (isUnlocked) 8.dp else 2.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(bgBrush)
            .border(
                width = if (isUnlocked) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isUnlocked) {
            Icon(
                imageVector = iconVector,
                contentDescription = iconType,
                tint = accentColor,
                modifier = Modifier.size(size * 0.55f)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked Sticker",
                tint = TextMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}
