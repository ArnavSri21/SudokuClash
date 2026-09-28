package com.example.engine

import com.example.data.model.Difficulty
import com.example.data.model.HintResult
import kotlin.random.Random

data class GeneratedPuzzle(
    val initialBoard: Array<IntArray>,
    val solutionBoard: Array<IntArray>
)

object SudokuEngine {

    fun generatePuzzle(difficulty: Difficulty, seed: Long? = null): GeneratedPuzzle {
        val random = if (seed != null) Random(seed) else Random.Default
        val solution = Array(9) { IntArray(9) }
        fillDiagonalBoxes(solution, random)
        solveBacktracking(solution, 0, 0, random)

        val initial = Array(9) { r -> solution[r].clone() }
        val cluesTarget = difficulty.clueCount
        var cellsToRemove = 81 - cluesTarget

        val positions = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                positions.add(r to c)
            }
        }
        positions.shuffle(random)

        for ((r, c) in positions) {
            if (cellsToRemove <= 0) break
            initial[r][c] = 0
            cellsToRemove--
        }

        return GeneratedPuzzle(initialBoard = initial, solutionBoard = solution)
    }

    private fun fillDiagonalBoxes(board: Array<IntArray>, random: Random) {
        for (i in 0 until 9 step 3) {
            fillBox(board, i, i, random)
        }
    }

    private fun fillBox(board: Array<IntArray>, startRow: Int, startCol: Int, random: Random) {
        val nums = (1..9).shuffled(random)
        var idx = 0
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                board[startRow + r][startCol + c] = nums[idx++]
            }
        }
    }

    fun isValidPlacement(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (i in 0 until 9) {
            if (i != col && board[row][i] == num) return false
            if (i != row && board[i][col] == num) return false
        }
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                val cr = boxRow + r
                val cc = boxCol + c
                if ((cr != row || cc != col) && board[cr][cc] == num) return false
            }
        }
        return true
    }

    private fun solveBacktracking(board: Array<IntArray>, row: Int, col: Int, random: Random): Boolean {
        var r = row
        var c = col
        if (c == 9) {
            c = 0
            r++
            if (r == 9) return true
        }

        if (board[r][c] != 0) {
            return solveBacktracking(board, r, c + 1, random)
        }

        val digits = (1..9).shuffled(random)
        for (num in digits) {
            if (isValidPlacement(board, r, c, num)) {
                board[r][c] = num
                if (solveBacktracking(board, r, c + 1, random)) return true
                board[r][c] = 0
            }
        }
        return false
    }

    fun getCandidates(board: Array<IntArray>, row: Int, col: Int): Set<Int> {
        if (board[row][col] != 0) return emptySet()
        val candidates = (1..9).toMutableSet()
        for (i in 0 until 9) {
            candidates.remove(board[row][i])
            candidates.remove(board[i][col])
        }
        val startR = (row / 3) * 3
        val startC = (col / 3) * 3
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                candidates.remove(board[startR + r][startC + c])
            }
        }
        return candidates
    }

    fun findSmartHint(
        currentBoard: Array<IntArray>,
        solutionBoard: Array<IntArray>
    ): HintResult? {
        // Step 1: Look for Naked Single
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (currentBoard[r][c] == 0) {
                    val cand = getCandidates(currentBoard, r, c)
                    if (cand.size == 1) {
                        val singleNum = cand.first()
                        val related = mutableListOf<Pair<Int, Int>>()
                        for (i in 0 until 9) {
                            if (currentBoard[r][i] != 0) related.add(r to i)
                            if (currentBoard[i][c] != 0) related.add(i to c)
                        }
                        return HintResult(
                            row = r,
                            col = c,
                            targetValue = singleNum,
                            title = "Naked Single",
                            explanation = "Cell (${r + 1}, ${c + 1}) has only 1 possible candidate: $singleNum. All other numbers (1-9) already appear in its row, column, or 3x3 block.",
                            relatedCells = related
                        )
                    }
                }
            }
        }

        // Step 2: Look for Hidden Single in 3x3 Box
        for (br in 0 until 9 step 3) {
            for (bc in 0 until 9 step 3) {
                for (digit in 1..9) {
                    val possibleCells = mutableListOf<Pair<Int, Int>>()
                    var alreadyPresent = false
                    for (r in 0 until 3) {
                        for (c in 0 until 3) {
                            val cr = br + r
                            val cc = bc + c
                            if (currentBoard[cr][cc] == digit) {
                                alreadyPresent = true
                                break
                            }
                            if (currentBoard[cr][cc] == 0 && getCandidates(currentBoard, cr, cc).contains(digit)) {
                                possibleCells.add(cr to cc)
                            }
                        }
                        if (alreadyPresent) break
                    }
                    if (!alreadyPresent && possibleCells.size == 1) {
                        val (hr, hc) = possibleCells.first()
                        return HintResult(
                            row = hr,
                            col = hc,
                            targetValue = digit,
                            title = "Hidden Single in Box",
                            explanation = "Within this 3×3 box, the number $digit can only be placed at (${hr + 1}, ${hc + 1}).",
                            relatedCells = listOf(hr to hc)
                        )
                    }
                }
            }
        }

        // Step 3: Fallback to minimal candidates from solution
        var minCandidates = 10
        var bestCell: Pair<Int, Int>? = null
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (currentBoard[r][c] == 0) {
                    val cands = getCandidates(currentBoard, r, c)
                    if (cands.isNotEmpty() && cands.size < minCandidates) {
                        minCandidates = cands.size
                        bestCell = r to c
                    }
                }
            }
        }

        if (bestCell != null) {
            val (r, c) = bestCell
            val solVal = solutionBoard[r][c]
            return HintResult(
                row = r,
                col = c,
                targetValue = solVal,
                title = "Logical Deduction",
                explanation = "Examine cell (${r + 1}, ${c + 1}). Cross-referencing row ${r + 1}, column ${c + 1}, and the local box, the correct digit is $solVal.",
                relatedCells = listOf(r to c)
            )
        }

        return null
    }

    fun dateToSeed(dateStr: String): Long {
        var hash = 1125899906842597L
        for (char in dateStr) {
            hash = 31 * hash + char.code
        }
        return hash
    }
}
