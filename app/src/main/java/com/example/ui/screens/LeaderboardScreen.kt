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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalThemePalette
import com.example.ui.viewmodel.LeaderboardEntry
import com.example.ui.viewmodel.SocialViewModel

@Composable
fun LeaderboardScreen(
    viewModel: SocialViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val palette = LocalThemePalette.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Player stats overview card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stats_overview_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderStrong)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${uiState.totalGamesWon}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.cellGivenText
                        )
                    }
                    Text(
                        text = "Victories",
                        fontSize = 12.sp,
                        color = palette.cellNoteText
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 36.dp)
                        .background(palette.border)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = palette.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${uiState.currentStreak}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.cellGivenText
                        )
                    }
                    Text(
                        text = "Active Streak",
                        fontSize = 12.sp,
                        color = palette.cellNoteText
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 36.dp)
                        .background(palette.border)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = palette.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.totalTimeFormatted,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.cellGivenText
                        )
                    }
                    Text(
                        text = "Time Played",
                        fontSize = 12.sp,
                        color = palette.cellNoteText
                    )
                }
            }
        }

        // Leaderboard List Header with Realtime Sync Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Global Leaderboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.cellGivenText
                )
                Text(
                    text = "Playing as @${uiState.publicUsername}",
                    fontSize = 12.sp,
                    color = palette.cellNoteText
                )
            }

            // Realtime Sync chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.secondary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = palette.secondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Realtime",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.secondary
                )
            }
        }

        // Leaderboard List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("leaderboard_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.leaderboard, key = { it.rank.toString() + it.name }) { entry ->
                LeaderboardRow(entry = entry)
            }
        }
    }
}

@Composable
fun LeaderboardRow(entry: LeaderboardEntry) {
    val palette = LocalThemePalette.current
    val isUser = entry.isCurrentUser
    val medalColor = when (entry.rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> palette.cellNoteText
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isUser) palette.primary.copy(alpha = 0.18f) else palette.card)
            .border(
                1.dp,
                if (isUser) palette.primary else palette.border,
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(if (isUser) "current_user_rank_row" else "leaderboard_rank_${entry.rank}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (entry.rank <= 3) medalColor.copy(alpha = 0.2f)
                        else palette.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${entry.rank}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.rank <= 3) medalColor else palette.cellNoteText
                )
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.name,
                        fontWeight = if (isUser) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isUser) palette.secondary else palette.cellGivenText,
                        fontSize = 15.sp
                    )
                    if (isUser) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.primary.copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "YOU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                        }
                    }
                }
                Text(
                    text = entry.title,
                    fontSize = 12.sp,
                    color = palette.cellNoteText
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${entry.score} pts",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isUser) palette.secondary else palette.cellGivenText
            )
            Text(
                text = "Best: ${entry.bestTime}",
                fontSize = 11.sp,
                color = palette.cellNoteText
            )
        }
    }
}
