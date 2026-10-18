package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StickerEntity
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderStrong
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianPrimary
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianViolet
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.StickersViewModel

@Composable
fun StickersScreen(
    viewModel: StickersViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Unlocked Progress Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stickers_progress_banner"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianViolet.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OBSIDIAN RELICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.unlockedCount} / ${uiState.totalCount} Unlocked",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Earn badges by mastering speed, precision, and streaks",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(ObsidianViolet.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = ObsidianViolet,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // Stickers 2-Column Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("stickers_grid")
        ) {
            items(uiState.stickers, key = { it.id }) { sticker ->
                StickerCard(sticker = sticker)
            }
        }
    }
}

@Composable
fun StickerCard(sticker: StickerEntity) {
    val isUnlocked = sticker.unlocked
    val iconVector = getIconForSticker(sticker.iconName)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) ObsidianCard else ObsidianSurface.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isUnlocked) ObsidianBorderStrong else ObsidianBorder.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sticker_card_${sticker.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) ObsidianPrimary.copy(alpha = 0.2f)
                        else Color.DarkGray.copy(alpha = 0.3f)
                    )
                    .border(
                        1.dp,
                        if (isUnlocked) NeonCyan else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = sticker.title,
                        tint = if (sticker.category == "Streaks") WarmAmber else ObsidianViolet,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = sticker.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isUnlocked) Color.White else Color.Gray,
                textAlign = TextAlign.Center
            )

            Text(
                text = sticker.description,
                fontSize = 11.sp,
                color = if (isUnlocked) Color.LightGray else Color.DarkGray,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isUnlocked) EmeraldGreen.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isUnlocked) "UNLOCKED" else "LOCKED",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) EmeraldGreen else Color.Gray,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

fun getIconForSticker(iconName: String): ImageVector {
    return when (iconName) {
        "sparkles" -> Icons.Default.AutoAwesome
        "bolt" -> Icons.Default.Bolt
        "diamond" -> Icons.Default.Diamond
        "military_tech" -> Icons.Default.MilitaryTech
        "local_fire_department" -> Icons.Default.LocalFireDepartment
        "brightness_auto" -> Icons.Default.BrightnessAuto
        "edit_note" -> Icons.Default.EditNote
        "auto_awesome" -> Icons.Default.AutoAwesome
        else -> Icons.Default.Diamond
    }
}
