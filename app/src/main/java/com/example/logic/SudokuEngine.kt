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
    val conflictReason: String? = null
)

data class HintResult(
    val row: Int,
    val col: Int,
    val value: Int
)

object SudokuEngine {

    const val GRID_SIZE = 9
    const val BOX_SIZE = 3

    fun generatePuzzle(
        difficulty: Difficulty,
        seed: Long = System.currentTimeMillis()
    ): GeneratedPuzzle {
        val random = Random(seed)
        val solution = generateCompletedBoard(random)
        val puzzleBoard = Array(GRID_SIZE) { r -> solution[r].clone() }

        // Remove numbers while keeping unique solution
        val cellsToRemove = (GRID_SIZE * GRID_SIZE) - difficulty.clues
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

            val copy = Array(GRID_SIZE) { rowIdx -> puzzleBoard[rowIdx].clone() }
            val solutions = countSolutions(copy, limit = 2)
            if (solutions != 1) {
                // Not unique, restore
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

    fun generateCompletedBoard(random: Random): Array<IntArray> {
        val board = Array(GRID_SIZE) { IntArray(GRID_SIZE) }
        // Fill the 3 independent 3x3 diagonal boxes first
        for (i in 0 until GRID_SIZE step BOX_SIZE) {
            fillDiagonalBox(board, i, i, random)
        }
        // Solve the rest
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

    fun countSolutions(board: Array<IntArray>, limit: Int = 2): Int {
        var emptyRow = -1
        var emptyCol = -1
        var minPossibilities = 10
        var bestCandidates = listOf<Int>()

        // MRV heuristic
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

    fun isSafe(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        // Row check
        for (c in 0 until GRID_SIZE) {
            if (board[row][c] == num) return false
        }
        // Column check
        for (r in 0 until GRID_SIZE) {
            if (board[r][col] == num) return false
        }
        // 3x3 Box check
        val boxRow = row - row % BOX_SIZE
        val boxCol = col - col % BOX_SIZE
        for (r in 0 until BOX_SIZE) {
            for (c in 0 until BOX_SIZE) {
                if (board[boxRow + r][boxCol + c] == num) return false
            }
        }
        return true
    }

    fun validateMove(
        currentBoard: Array<IntArray>,
        solution: Array<IntArray>,
        row: Int,
        col: Int,
        num: Int
    ): MoveValidationResult {
        if (num < 1 || num > 9) {
            return MoveValidationResult(false, false, "Invalid digit")
        }
        // Conflict in row
        for (c in 0 until GRID_SIZE) {
            if (c != col && currentBoard[row][c] == num) {
                return MoveValidationResult(false, false, "Row conflict")
            }
        }
        // Conflict in col
        for (r in 0 until GRID_SIZE) {
            if (r != row && currentBoard[r][col] == num) {
                return MoveValidationResult(false, false, "Column conflict")
            }
        }
        // Conflict in box
        val boxRow = row - row % BOX_SIZE
        val boxCol = col - col % BOX_SIZE
        for (r in 0 until BOX_SIZE) {
            for (c in 0 until BOX_SIZE) {
                val cr = boxRow + r
                val cc = boxCol + c
                if ((cr != row || cc != col) && currentBoard[cr][cc] == num) {
                    return MoveValidationResult(false, false, "Box conflict")
                }
            }
        }

        val matchesSolution = (solution[row][col] == num)
        return MoveValidationResult(
            isValid = true,
            isCorrectAgainstSolution = matchesSolution,
            conflictReason = null
        )
    }

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

    fun isBoardCompleteAndValid(board: Array<IntArray>): Boolean {
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                val v = board[r][c]
                if (v !in 1..9) return false
                // Check safety if we temporarily set to 0
                board[r][c] = 0
                val safe = isSafe(board, r, c, v)
                board[r][c] = v
                if (!safe) return false
            }
        }
        return true
    }
}
