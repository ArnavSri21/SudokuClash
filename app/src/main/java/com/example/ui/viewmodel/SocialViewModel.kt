package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GameStatsEntity
import com.example.data.repository.SudokuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val currentStreak: Int = 0,
    val publicUsername: String = "Player",
    val errorMessage: String? = null
)

class SocialViewModel(
    private val repository: SudokuRepository
) : ViewModel() {

    private val _usernameState = MutableStateFlow(repository.getPublicUsername())
    val usernameState: StateFlow<String> = _usernameState.asStateFlow()

    val uiState: StateFlow<SocialUiState> = combine(
        repository.allStats,
        repository.observeLeaderboard(),
        _usernameState
    ) { stats, leaderboard, currentUsername ->
        val totalWon = stats.sumOf { it.gamesWon }
        val totalSecs = stats.sumOf { it.totalTimeSeconds }
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m ${totalSecs % 60}s"
        val streak = repository.getOverallStreak()

        SocialUiState(
            statsList = stats,
            leaderboard = leaderboard,
            totalGamesWon = totalWon,
            totalTimeFormatted = timeStr,
            currentStreak = streak,
            publicUsername = currentUsername
        )
    }.catch { error ->
        Log.w("SocialViewModel", "Error observing realtime leaderboard or stats", error)
        emit(
            SocialUiState(
                publicUsername = repository.getPublicUsername(),
                errorMessage = error.localizedMessage
            )
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SocialUiState(
            publicUsername = repository.getPublicUsername(),
            currentStreak = repository.getOverallStreak()
        )
    )

    fun updatePublicUsername(newUsername: String) {
        val trimmed = newUsername.trim()
        if (trimmed.isEmpty()) return
        _usernameState.value = trimmed
        viewModelScope.launch {
            try {
                repository.setPublicUsername(trimmed)
            } catch (e: Exception) {
                Log.w("SocialViewModel", "Failed to update public username", e)
            }
        }
    }
}
