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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
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
import com.example.ui.theme.DigitGiven
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderStrong
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianPrimary
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianViolet
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.LeaderboardEntry
import com.example.ui.viewmodel.SocialViewModel

@Composable
fun LeaderboardScreen(
    viewModel: SocialViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Player stats overview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stats_overview_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorderStrong)
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
                            tint = WarmAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${uiState.totalGamesWon}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Victories",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 36.dp)
                        .background(ObsidianBorder)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.totalTimeFormatted,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Time Played",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Leaderboard List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Obsidian Leaderboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Global Rank",
                fontSize = 12.sp,
                color = NeonCyan,
                fontWeight = FontWeight.SemiBold
            )
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
    val isUser = entry.isCurrentUser
    val medalColor = when (entry.rank) {
        1 -> WarmAmber
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isUser) ObsidianPrimary.copy(alpha = 0.15f) else ObsidianCard)
            .border(
                1.dp,
                if (isUser) ObsidianViolet else ObsidianBorder,
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
                        else ObsidianSurface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${entry.rank}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.rank <= 3) medalColor else Color.Gray
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
                        color = if (isUser) NeonCyan else Color.White,
                        fontSize = 15.sp
                    )
                    if (isUser) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ObsidianViolet.copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "YOU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianViolet
                            )
                        }
                    }
                }
                Text(
                    text = entry.title,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${entry.score} pts",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isUser) NeonCyan else Color.White
            )
            Text(
                text = "Best: ${entry.bestTime}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}
