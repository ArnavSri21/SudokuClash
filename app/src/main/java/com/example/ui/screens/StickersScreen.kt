package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.entity.StickerEntity
import com.example.ui.components.StickerGlyph
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.StickersViewModel

@Composable
fun StickersScreen(
    viewModel: StickersViewModel,
    modifier: Modifier = Modifier
) {
    val stickers by viewModel.stickers.collectAsStateWithLifecycle()
    val streakRec by viewModel.streak.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentStreak = streakRec?.currentStreak ?: 0
    val unlockedCount = stickers.count { it.isUnlocked }

    val nextStreakSticker = stickers
        .filter { it.requirementType == "STREAK" && !it.isUnlocked }
        .minByOrNull { it.requirementThreshold }

    val filteredStickers = when (uiState.filterType) {
        "STREAK" -> stickers.filter { it.requirementType == "STREAK" }
        "MILESTONE" -> stickers.filter { it.requirementType != "STREAK" }
        else -> stickers
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "STICKER VAULT",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Collect innovative badges by hitting streak targets",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$unlockedCount / ${stickers.size} Unlocked",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (nextStreakSticker != null) {
                        NextTargetCard(
                            targetSticker = nextStreakSticker,
                            currentStreak = currentStreak
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            title = "All Stickers",
                            isSelected = uiState.filterType == "ALL",
                            onClick = { viewModel.setFilter("ALL") }
                        )
                        FilterChip(
                            title = "Streak Targets",
                            isSelected = uiState.filterType == "STREAK",
                            onClick = { viewModel.setFilter("STREAK") }
                        )
                        FilterChip(
                            title = "Milestones",
                            isSelected = uiState.filterType == "MILESTONE",
                            onClick = { viewModel.setFilter("MILESTONE") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(filteredStickers) { sticker ->
                StickerCard(
                    sticker = sticker,
                    onClick = { viewModel.selectSticker(sticker) }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        if (uiState.showDetailModal && uiState.selectedSticker != null) {
            StickerDetailDialog(
                sticker = uiState.selectedSticker!!,
                onDismiss = { viewModel.dismissDetailModal() },
                onEquip = { viewModel.equipSticker(uiState.selectedSticker!!) }
            )
        }
    }
}

@Composable
private fun NextTargetCard(
    targetSticker: StickerEntity,
    currentStreak: Int
) {
    val progress = (currentStreak.toFloat() / targetSticker.requirementThreshold).coerceIn(0f, 1f)
    val daysLeft = maxOf(0, targetSticker.requirementThreshold - currentStreak)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(NeonOrange, NeonAmber))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StickerGlyph(
                        iconType = targetSticker.iconType,
                        accentColorHex = targetSticker.accentColorHex,
                        isUnlocked = false,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NEXT STREAK TARGET",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = targetSticker.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Text(
                    text = "$daysLeft ${if (daysLeft == 1) "day" else "days"} to go",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonOrange
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NeonAmber,
                trackColor = Color(0xFF161E2D)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current Streak: $currentStreak days",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = "Target: ${targetSticker.requirementThreshold} days",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else ObsidianCard)
            .border(
                1.dp,
                if (isSelected) NeonCyan else ObsidianBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) NeonCyan else TextSecondary
        )
    }
}

@Composable
private fun StickerCard(
    sticker: StickerEntity,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianCard)
            .border(
                1.dp,
                if (sticker.isFeatured) NeonAmber else if (sticker.isUnlocked) NeonCyan.copy(alpha = 0.5f) else ObsidianBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("sticker_card_${sticker.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (sticker.isFeatured) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonAmber.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Equipped",
                        tint = NeonAmber,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "EQUIPPED",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            StickerGlyph(
                iconType = sticker.iconType,
                accentColorHex = sticker.accentColorHex,
                isUnlocked = sticker.isUnlocked,
                size = 56.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = sticker.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (sticker.isUnlocked) TextPrimary else TextMuted,
                textAlign = TextAlign.Center
            )

            Text(
                text = sticker.subtitle,
                fontSize = 10.sp,
                color = if (sticker.isUnlocked) NeonCyan else TextMuted.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StickerDetailDialog(
    sticker: StickerEntity,
    onDismiss: () -> Unit,
    onEquip: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(ObsidianSurface)
                .border(
                    1.5.dp,
                    if (sticker.isUnlocked) NeonCyan.copy(alpha = 0.8f) else ObsidianBorder,
                    RoundedCornerShape(24.dp)
                )
                .padding(22.dp)
                .testTag("dialog_sticker_detail")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (sticker.isUnlocked) "UNLOCKED STICKER" else "LOCKED TARGET",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (sticker.isUnlocked) NeonCyan else TextMuted
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                StickerGlyph(
                    iconType = sticker.iconType,
                    accentColorHex = sticker.accentColorHex,
                    isUnlocked = sticker.isUnlocked,
                    size = 80.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = sticker.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )

                Text(
                    text = sticker.subtitle,
                    fontSize = 13.sp,
                    color = NeonAmber,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianCard)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "\"${sticker.lore}\"",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val conditionText = when (sticker.requirementType) {
                    "STREAK" -> "Requires a consecutive ${sticker.requirementThreshold}-day streak by solving puzzles daily."
                    "MISTAKE" -> "Requires solving any puzzle with 0 mistakes."
                    "TIME" -> "Requires solving a full puzzle in under 4 minutes."
                    "DIFFICULTY" -> "Requires defeating an Expert difficulty puzzle."
                    "DAILY" -> "Requires completing at least 3 competitive Daily Challenges."
                    else -> "Play daily to unlock."
                }

                Text(
                    text = conditionText,
                    fontSize = 11.5.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (sticker.isUnlocked) {
                    Button(
                        onClick = onEquip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("equip_sticker_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sticker.isFeatured) ObsidianCard else NeonCyan,
                            contentColor = if (sticker.isFeatured) NeonAmber else ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (sticker.isFeatured) "Currently Equipped" else "Equip as Profile Badge",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Keep Playing to Unlock")
                    }
                }
            }
        }
    }
}
