package com.example.data.model

enum class Difficulty(val displayName: String, val clueCount: Int, val stars: Int) {
    EASY("Easy", 40, 1),
    MEDIUM("Medium", 32, 2),
    HARD("Hard", 28, 3),
    EXPERT("Expert", 24, 4),
    DAILY("Daily Challenge", 30, 3)
}

data class Cell(
    val row: Int,
    val col: Int,
    val value: Int = 0,
    val isGiven: Boolean = false,
    val notes: Set<Int> = emptySet(),
    val isError: Boolean = false,
    val isHinted: Boolean = false
)

data class GameMove(
    val row: Int,
    val col: Int,
    val oldValue: Int,
    val newValue: Int,
    val oldNotes: Set<Int>,
    val newNotes: Set<Int>
)

data class HintResult(
    val row: Int,
    val col: Int,
    val targetValue: Int,
    val title: String,
    val explanation: String,
    val relatedCells: List<Pair<Int, Int>> = emptyList()
)

data class SessionTimerState(
    val elapsedSeconds: Long = 0,
    val isRunning: Boolean = true,
    val isPaused: Boolean = false
)
