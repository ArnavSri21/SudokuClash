package com.example.logic

import kotlin.random.Random

enum class Difficulty(
    val displayName: String,
    val clues: Int,
    val maxMistakes: Int,
    val stars: Int,
    val description: String
) {
    EASY(
        displayName = "Easy",
        clues = 40,
        maxMistakes = 5,
        stars = 1,
        description = "Relaxed flow with plenty of clues"
    ),
    MEDIUM(
        displayName = "Medium",
        clues = 32,
        maxMistakes = 3,
        stars = 2,
        description = "Balanced logic test for regular solvers"
    ),
    HARD(
        displayName = "Hard",
        clues = 28,
        maxMistakes = 3,
        stars = 3,
        description = "Deep deductions and candidate filtering"
    ),
    EXPERT(
        displayName = "Expert",
        clues = 24,
        maxMistakes = 3,
        stars = 4,
        description = "Pencil marks required, pure mastery"
    ),
    DAILY(
        displayName = "Daily Challenge",
        clues = 30,
        maxMistakes = 3,
        stars = 3,
        description = "Specially calibrated puzzle of the day"
    );

    companion object {
        fun fromName(name: String): Difficulty {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MEDIUM
        }
    }
}

data class GeneratedPuzzle(
    val initialBoard: Array<IntArray>,
    val solution: Array<IntArray>,
    val difficulty: Difficulty
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeneratedPuzzle) return false
        return difficulty == other.difficulty &&
                initialBoard.contentDeepEquals(other.initialBoard) &&
                solution.contentDeepEquals(other.solution)
    }

    override fun hashCode(): Int {
        var result = initialBoard.contentDeepHashCode()
        result = 31 * result + solution.contentDeepHashCode()
        result = 31 * result + difficulty.hashCode()
        return result
    }
}

data class MoveValidationResult(
    val isValid: Boolean,
    val isCorrectAgainstSolution: Boolean,
    val conflictReason: String? = null,
    val conflictingCoordinates: List<Pair<Int, Int>> = emptyList()
)

data class WinConditionResult(
    val isWon: Boolean,
    val isComplete: Boolean,
    val hasConflicts: Boolean,
    val emptyCellsCount: Int,
    val message: String
)

data class HintResult(
    val row: Int,
    val col: Int,
    val value: Int
)

/**
 * Pure Kotlin Sudoku Game Engine handling:
 * 1. Board generation with guaranteed unique solutions across 5 difficulty levels.
 * 2. Real-time move validation checking row, column, and 3x3 box constraints and solution alignment.
 * 3. Complete win condition verification ensuring all 81 cells are non-zero, compliant with Sudoku rules,
 *    and matching the solution.
 * 4. Helper candidate calculation and hints.
 */
object SudokuEngine {

    const val GRID_SIZE = 9
    const val BOX_SIZE = 3

    // -------------------------------------------------------------
    // 1. BOARD GENERATION
    // -------------------------------------------------------------

    /**
     * Generates a fully playable Sudoku puzzle with guaranteed unique solution.
     */
    fun generatePuzzle(
        difficulty: Difficulty,
        seed: Long = System.currentTimeMillis()
    ): GeneratedPuzzle {
        val random = Random(seed)
        val solution = generateCompletedBoard(random)
        val puzzleBoard = Array(GRID_SIZE) { r -> solution[r].clone() }

        val targetClues = difficulty.clues.coerceIn(17, 60)
        val cellsToRemove = (GRID_SIZE * GRID_SIZE) - targetClues

        val cellCoordinates = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                cellCoordinates.add(Pair(r, c))
            }
        }
        cellCoordinates.shuffle(random)

        var removedCount = 0
        for ((r, c) in cellCoordinates) {
            if (removedCount >= cellsToRemove) break

            val temp = puzzleBoard[r][c]
            puzzleBoard[r][c] = 0

            // Check if removing this cell preserves uniqueness
            val copy = Array(GRID_SIZE) { rowIdx -> puzzleBoard[rowIdx].clone() }
            val solutions = countSolutions(copy, limit = 2)
            if (solutions != 1) {
                puzzleBoard[r][c] = temp
            } else {
                removedCount++
            }
        }

        return GeneratedPuzzle(
            initialBoard = puzzleBoard,
            solution = solution,
            difficulty = difficulty
        )
    }

    /**
     * Generates a complete 9x9 Sudoku board with valid digits 1-9 in every row, column, and box.
     */
    fun generateCompletedBoard(random: Random = Random): Array<IntArray> {
        val board = Array(GRID_SIZE) { IntArray(GRID_SIZE) }
        // Fill the 3 independent 3x3 diagonal boxes first
        for (i in 0 until GRID_SIZE step BOX_SIZE) {
            fillDiagonalBox(board, i, i, random)
        }
        // Solve the remaining cells using randomized backtracking
        solveBoard(board, random)
        return board
    }

    private fun fillDiagonalBox(board: Array<IntArray>, startRow: Int, startCol: Int, random: Random) {
        val digits = (1..9).shuffled(random)
        var idx = 0
        for (r in 0 until BOX_SIZE) {
            for (c in 0 until BOX_SIZE) {
                board[startRow + r][startCol + c] = digits[idx++]
            }
        }
    }

    private fun solveBoard(board: Array<IntArray>, random: Random): Boolean {
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (board[r][c] == 0) {
                    val digits = (1..9).shuffled(random)
                    for (num in digits) {
                        if (isSafe(board, r, c, num)) {
                            board[r][c] = num
                            if (solveBoard(board, random)) return true
                            board[r][c] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    /**
     * Counts solutions up to [limit] using Minimum Remaining Values (MRV) heuristic.
     */
    fun countSolutions(board: Array<IntArray>, limit: Int = 2): Int {
        var emptyRow = -1
        var emptyCol = -1
        var minPossibilities = 10
        var bestCandidates = listOf<Int>()

        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (board[r][c] == 0) {
                    val candidates = (1..9).filter { isSafe(board, r, c, it) }
                    if (candidates.size < minPossibilities) {
                        minPossibilities = candidates.size
                        emptyRow = r
                        emptyCol = c
                        bestCandidates = candidates
                    }
                }
            }
        }

        if (emptyRow == -1) {
            return 1
        }

        var count = 0
        for (num in bestCandidates) {
            board[emptyRow][emptyCol] = num
            count += countSolutions(board, limit - count)
            board[emptyRow][emptyCol] = 0
            if (count >= limit) break
        }
        return count
    }

    /**
     * Checks if placing [num] at [row], [col] does not violate Sudoku constraints.
     */
    fun isSafe(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (c in 0 until GRID_SIZE) {
            if (board[row][c] == num) return false
        }
        for (r in 0 until GRID_SIZE) {
            if (board[r][col] == num) return false
        }
        val boxRow = row - row % BOX_SIZE
        val boxCol = col - col % BOX_SIZE
        for (r in 0 until BOX_SIZE) {
            for (c in 0 until BOX_SIZE) {
                if (board[boxRow + r][boxCol + c] == num) return false
            }
        }
        return true
    }

    // -------------------------------------------------------------
    // 2. MOVE VALIDATION
    // -------------------------------------------------------------

    /**
     * Validates a player move against current board constraints and unique solution.
     */
    fun validateMove(
        currentBoard: Array<IntArray>,
        solution: Array<IntArray>,
        row: Int,
        col: Int,
        num: Int
    ): MoveValidationResult {
        if (num !in 1..9) {
            return MoveValidationResult(
                isValid = false,
                isCorrectAgainstSolution = false,
                conflictReason = "Digit must be between 1 and 9"
            )
        }

        val conflictingCells = mutableListOf<Pair<Int, Int>>()

        // Row conflict check
        for (c in 0 until GRID_SIZE) {
            if (c != col && currentBoard[row][c] == num) {
                conflictingCells.add(Pair(row, c))
            }
        }

        // Column conflict check
        for (r in 0 until GRID_SIZE) {
            if (r != row && currentBoard[r][col] == num) {
                conflictingCells.add(Pair(r, col))
            }
        }

        // 3x3 Box conflict check
        val boxRow = row - row % BOX_SIZE
        val boxCol = col - col % BOX_SIZE
        for (r in 0 until BOX_SIZE) {
            for (c in 0 until BOX_SIZE) {
                val cr = boxRow + r
                val cc = boxCol + c
                if ((cr != row || cc != col) && currentBoard[cr][cc] == num) {
                    if (!conflictingCells.contains(Pair(cr, cc))) {
                        conflictingCells.add(Pair(cr, cc))
                    }
                }
            }
        }

        val hasBoardConflict = conflictingCells.isNotEmpty()
        val matchesSolution = (solution[row][col] == num)

        val reason = when {
            hasBoardConflict -> "Duplicate $num in row, column, or 3x3 subgrid"
            !matchesSolution -> "Does not match the unique solution"
            else -> null
        }

        return MoveValidationResult(
            isValid = !hasBoardConflict,
            isCorrectAgainstSolution = matchesSolution,
            conflictReason = reason,
            conflictingCoordinates = conflictingCells
        )
    }

    // -------------------------------------------------------------
    // 3. CHECKING FOR WIN CONDITIONS
    // -------------------------------------------------------------

    /**
     * Comprehensive check for game victory:
     * - Every cell is filled (non-zero).
     * - No duplicate digits in any row, column, or 3x3 box.
     * - Every cell matches the valid solution.
     */
    fun checkWinCondition(
        currentBoard: Array<IntArray>,
        solution: Array<IntArray>
    ): WinConditionResult {
        var emptyCount = 0
        var solutionMismatches = 0

        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                val v = currentBoard[r][c]
                if (v == 0) {
                    emptyCount++
                } else if (v != solution[r][c]) {
                    solutionMismatches++
                }
            }
        }

        if (emptyCount > 0) {
            return WinConditionResult(
                isWon = false,
                isComplete = false,
                hasConflicts = false,
                emptyCellsCount = emptyCount,
                message = "$emptyCount cells remain empty"
            )
        }

        val isStandardValid = isBoardCompleteAndValid(currentBoard)
        if (!isStandardValid || solutionMismatches > 0) {
            return WinConditionResult(
                isWon = false,
                isComplete = true,
                hasConflicts = true,
                emptyCellsCount = 0,
                message = "Board is full but contains invalid placements"
            )
        }

        return WinConditionResult(
            isWon = true,
            isComplete = true,
            hasConflicts = false,
            emptyCellsCount = 0,
            message = "Victory! Sudoku puzzle solved perfectly!"
        )
    }

    /**
     * Simple boolean check for whether the board matches the win condition.
     */
    fun isWin(currentBoard: Array<IntArray>, solution: Array<IntArray>): Boolean {
        return checkWinCondition(currentBoard, solution).isWon
    }

    fun isBoardCompleteAndValid(board: Array<IntArray>): Boolean {
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                val v = board[r][c]
                if (v !in 1..9) return false
                board[r][c] = 0
                val safe = isSafe(board, r, c, v)
                board[r][c] = v
                if (!safe) return false
            }
        }
        return true
    }

    // -------------------------------------------------------------
    // 4. HINTS & CANDIDATES
    // -------------------------------------------------------------

    fun findHint(currentBoard: Array<IntArray>, solution: Array<IntArray>): HintResult? {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (currentBoard[r][c] == 0) {
                    emptyCells.add(Pair(r, c))
                }
            }
        }
        if (emptyCells.isEmpty()) return null
        val (hr, hc) = emptyCells.random()
        return HintResult(hr, hc, solution[hr][hc])
    }

    fun getCandidates(board: Array<IntArray>, row: Int, col: Int): Set<Int> {
        if (board[row][col] != 0) return emptySet()
        return (1..9).filter { isSafe(board, row, col, it) }.toSet()
    }
}
