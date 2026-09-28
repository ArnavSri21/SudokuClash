package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.DailyLeaderboardEntity
import com.example.data.database.entity.StreakRecordEntity
import com.example.data.repository.SudokuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class WeekDayStatus(
    val dayName: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val isCompleted: Boolean
)

data class DailyUiState(
    val todayDateFormatted: String = "",
    val todayDateIso: String = "",
    val isTodayCompleted: Boolean = false,
    val weekDays: List<WeekDayStatus> = emptyList(),
    val leaderboardFilter: LeaderboardFilter = LeaderboardFilter.GLOBAL
)

enum class LeaderboardFilter {
    GLOBAL, FRIENDS
}

class DailyViewModel(
    private val repository: SudokuRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val todayIso = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    val streak: StateFlow<StreakRecordEntity?> = repository.streak
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val leaderboard: StateFlow<List<DailyLeaderboardEntity>> = repository.getDailyLeaderboard(todayIso)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(DailyUiState())
    val uiState: StateFlow<DailyUiState> = _uiState.asStateFlow()

    init {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
        val formattedDate = today.format(formatter)

        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val days = (0..6).map { i ->
            val d = startOfWeek.plusDays(i.toLong())
            WeekDayStatus(
                dayName = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                dayOfMonth = d.dayOfMonth,
                isToday = d == today,
                isCompleted = false
            )
        }

        _uiState.update {
            it.copy(
                todayDateFormatted = formattedDate,
                todayDateIso = todayIso,
                weekDays = days
            )
        }

        viewModelScope.launch {
            repository.streak.collect { streakRec ->
                val completedDates = streakRec?.completedDailyDates?.split(",")?.toSet() ?: emptySet()
                val updatedDays = days.map { day ->
                    val isDone = completedDates.any { it.endsWith("-${String.format("%02d", day.dayOfMonth)}") }
                    day.copy(isCompleted = isDone)
                }
                _uiState.update {
                    it.copy(
                        isTodayCompleted = completedDates.contains(todayIso),
                        weekDays = updatedDays
                    )
                }
            }
        }
    }

    fun setFilter(filter: LeaderboardFilter) {
        _uiState.update { it.copy(leaderboardFilter = filter) }
    }
}
