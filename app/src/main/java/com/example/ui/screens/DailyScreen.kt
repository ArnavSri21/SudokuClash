package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.entity.DailyLeaderboardEntity
import com.example.ui.components.StickerGlyph
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DailyViewModel
import com.example.ui.viewmodel.LeaderboardFilter
import com.example.ui.viewmodel.WeekDayStatus

@Composable
fun DailyScreen(
    viewModel: DailyViewModel,
    onPlayDailyChallenge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val streakRec by viewModel.streak.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()

    val currentStreak = streakRec?.currentStreak ?: 0
    val bestStreak = streakRec?.bestStreak ?: 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAILY CHALLENGE",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = uiState.todayDateFormatted,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonOrange.copy(alpha = 0.15f))
                        .border(1.dp, NeonOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = NeonOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$currentStreak Days",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StreakHeroCard(
                currentStreak = currentStreak,
                bestStreak = bestStreak,
                modifier = Modifier.widthIn(max = 540.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            WeeklyStreakCalendar(
                weekDays = uiState.weekDays,
                modifier = Modifier.widthIn(max = 540.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            DailyActionCard(
                isCompleted = uiState.isTodayCompleted,
                onPlayClick = onPlayDailyChallenge,
                modifier = Modifier.widthIn(max = 540.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S LEADERBOARD",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianCard)
                        .padding(2.dp)
                ) {
                    LeaderboardFilterTab(
                        title = "Global",
                        isSelected = uiState.leaderboardFilter == LeaderboardFilter.GLOBAL,
                        onClick = { viewModel.setFilter(LeaderboardFilter.GLOBAL) }
                    )
                    LeaderboardFilterTab(
                        title = "Friends",
                        isSelected = uiState.leaderboardFilter == LeaderboardFilter.FRIENDS,
                        onClick = { viewModel.setFilter(LeaderboardFilter.FRIENDS) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        items(leaderboard) { entry ->
            LeaderboardRow(
                entry = entry,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .padding(vertical = 4.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StreakHeroCard(
    currentStreak: Int,
    bestStreak: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF26180C),
                        Color(0xFF191316),
                        ObsidianSurface
                    )
                )
            )
            .border(1.5.dp, NeonOrange.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(18.dp)
            .testTag("streak_hero_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACTIVE STREAK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = NeonOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = currentStreak.toString(),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentStreak == 1) "day in a row" else "days in a row",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Personal Best: $bestStreak days 🔥",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NeonOrange.copy(alpha = 0.2f))
                    .border(2.dp, NeonOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = "Streak flame",
                    tint = NeonOrange,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun WeeklyStreakCalendar(
    weekDays: List<WeekDayStatus>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(ObsidianBorder, ObsidianBorder)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "THIS WEEK'S MOMENTUM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = day.dayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (day.isToday) NeonCyan else TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        day.isCompleted -> NeonEmerald.copy(alpha = 0.2f)
                                        day.isToday -> NeonCyan.copy(alpha = 0.15f)
                                        else -> ObsidianCard
                                    }
                                )
                                .border(
                                    width = if (day.isToday) 1.5.dp else 1.dp,
                                    color = when {
                                        day.isCompleted -> NeonEmerald
                                        day.isToday -> NeonCyan
                                        else -> ObsidianBorder
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (day.isToday) NeonCyan else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyActionCard(
    isCompleted: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                if (isCompleted) listOf(NeonEmerald, Color(0xFF007A3E)) else listOf(NeonCyan, Color(0xFF005F6B))
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (isCompleted) NeonEmerald else NeonAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCompleted) "Challenge Completed!" else "Today's Puzzle",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCompleted) "Your score is on today's leaderboard." else "Standard 9×9 Medium grid • Equal for all players",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) ObsidianSurface else NeonCyan,
                    contentColor = if (isCompleted) NeonEmerald else ObsidianBg
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("play_daily_challenge_button")
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isCompleted) "Play Again" else "Play Now",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LeaderboardFilterTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonCyan else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) ObsidianBg else TextSecondary
        )
    }
}

@Composable
private fun LeaderboardRow(
    entry: DailyLeaderboardEntity,
    modifier: Modifier = Modifier
) {
    val rankColor = when (entry.rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> TextMuted
    }

    val minutes = entry.timeSeconds / 60
    val seconds = entry.timeSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val bg = if (entry.isCurrentUser) Color(0xFF152A3F) else ObsidianCard
    val borderCol = if (entry.isCurrentUser) NeonCyan.copy(alpha = 0.6f) else ObsidianBorder

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("leaderboard_row_${entry.rank}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            when (entry.rank) {
                1 -> Text("🥇", fontSize = 18.sp)
                2 -> Text("🥈", fontSize = 18.sp)
                3 -> Text("🥉", fontSize = 18.sp)
                else -> Text(
                    text = "#${entry.rank}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = rankColor
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        StickerGlyph(
            iconType = "FLAME",
            accentColorHex = "#FFB300",
            isUnlocked = true,
            size = 32.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.playerName,
                    fontSize = 14.sp,
                    fontWeight = if (entry.isCurrentUser) FontWeight.Black else FontWeight.Bold,
                    color = if (entry.isCurrentUser) NeonCyan else TextPrimary
                )
                if (entry.isCurrentUser) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "YOU",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }
            }
            Text(
                text = "${entry.accuracyPercent}% Accuracy • ${entry.mistakes} mistakes",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        Text(
            text = timeFormatted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (entry.rank == 1) NeonAmber else TextPrimary
        )
    }
}
