package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GameStatsEntity
import com.example.data.repository.SudokuRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val title: String,
    val score: Int,
    val bestTime: String,
    val isCurrentUser: Boolean = false
)

data class SocialUiState(
    val statsList: List<GameStatsEntity> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val totalGamesWon: Int = 0,
    val totalTimeFormatted: String = "00:00",
    val errorMessage: String? = null
)

class SocialViewModel(
    repository: SudokuRepository
) : ViewModel() {

    val uiState: StateFlow<SocialUiState> = repository.allStats
        .map { stats ->
            val totalWon = stats.sumOf { it.gamesWon }
            val totalSecs = stats.sumOf { it.totalTimeSeconds }
            val hours = totalSecs / 3600
            val mins = (totalSecs % 3600) / 60
            val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m ${totalSecs % 60}s"

            val userScore = totalWon * 150 + stats.sumOf { it.perfectGames } * 200
            val entries = listOf(
                LeaderboardEntry(1, "Aetherius", "Grandmaster", 3420, "02:14"),
                LeaderboardEntry(2, "Vespera", "Void Sovereign", 2890, "02:45"),
                LeaderboardEntry(3, "You", "Obsidian Solver", userScore, if (totalWon > 0) "Active" else "--:--", isCurrentUser = true),
                LeaderboardEntry(4, "Kaelen", "Cipher Adept", 2150, "03:12"),
                LeaderboardEntry(5, "Nyx_99", "Grid Sentinel", 1840, "03:55"),
                LeaderboardEntry(6, "Solaris", "Pencil Artisan", 1430, "04:10"),
                LeaderboardEntry(7, "Zephyr", "Rift Seeker", 980, "05:02")
            ).sortedByDescending { it.score }
                .mapIndexed { idx, entry -> entry.copy(rank = idx + 1) }

            SocialUiState(
                statsList = stats,
                leaderboard = entries,
                totalGamesWon = totalWon,
                totalTimeFormatted = timeStr
            )
        }
        .catch { error ->
            Log.w("SocialViewModel", "Error observing game stats", error)
            emit(SocialUiState(errorMessage = error.localizedMessage))
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SocialUiState()
        )
}
