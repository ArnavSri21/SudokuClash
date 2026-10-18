package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DailyChallengeEntity
import com.example.data.repository.SudokuRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DailyUiState(
    val todayDate: String,
    val streak: Int = 0,
    val isTodayCompleted: Boolean = false,
    val todayChallenge: DailyChallengeEntity? = null,
    val recentChallenges: List<DailyChallengeEntity> = emptyList(),
    val errorMessage: String? = null
)

class DailyViewModel(
    private val repository: SudokuRepository
) : ViewModel() {

    val todayDate: String = repository.getTodayDateString()

    val uiState: StateFlow<DailyUiState> = repository.allDailyChallenges
        .map { list ->
            val today = list.firstOrNull { it.dateString == todayDate }
            val streak = repository.calculateStreak(list)
            DailyUiState(
                todayDate = todayDate,
                streak = streak,
                isTodayCompleted = today?.completed == true,
                todayChallenge = today,
                recentChallenges = list.take(14)
            )
        }
        .catch { error ->
            Log.w("DailyViewModel", "Error observing daily challenges", error)
            emit(DailyUiState(todayDate = todayDate, errorMessage = error.localizedMessage ?: "Failed to load daily challenges"))
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DailyUiState(todayDate = todayDate)
        )
}
