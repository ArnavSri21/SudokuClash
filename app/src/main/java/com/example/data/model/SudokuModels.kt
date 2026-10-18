package com.example.data.model

enum class Difficulty(
    val displayName: String,
    val clueCount: Int,
    val stars: Int,
    val description: String = "",
    val maxMistakes: Int = 3
) {
    EASY("Easy", 40, 1, "Ideal for beginners with plenty of starting clues.", 5),
    MEDIUM("Medium", 32, 2, "Balanced challenge requiring standard logic deductions.", 3),
    HARD("Hard", 28, 3, "Demanding puzzle requiring hidden singles & pairs.", 3),
    EXPERT("Expert", 24, 4, "Master-level puzzle requiring advanced logic techniques.", 3),
    DAILY("Daily Challenge", 30, 3, "Daily competitive puzzle with streaks & rankings.", 3)
}

data class MoveValidationResult(
    val isValidRule: Boolean,
    val isCorrectSolution: Boolean,
    val conflictingCells: List<Pair<Int, Int>> = emptyList(),
    val conflictReason: String? = null
)

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
    val explanation: String
)
