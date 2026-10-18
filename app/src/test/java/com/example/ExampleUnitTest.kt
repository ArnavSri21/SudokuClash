package com.example

import com.example.logic.Difficulty
import com.example.logic.SudokuEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExampleUnitTest {

    @Test
    fun testGenerateCompletedBoard() {
        val board = SudokuEngine.generateCompletedBoard(Random(42))
        assertEquals(9, board.size)
        for (r in 0 until 9) {
            assertEquals(9, board[r].size)
            for (c in 0 until 9) {
                assertTrue("Cell value must be 1..9", board[r][c] in 1..9)
            }
        }
        assertTrue("Generated board must be valid", SudokuEngine.isBoardCompleteAndValid(board))
    }

    @Test
    fun testGeneratePuzzleUniqueness() {
        val puzzle = SudokuEngine.generatePuzzle(Difficulty.EASY, seed = 12345L)
        assertNotNull(puzzle.initialBoard)
        assertNotNull(puzzle.solution)
        assertTrue("Solution must be valid", SudokuEngine.isBoardCompleteAndValid(puzzle.solution))

        val copy = Array(9) { r -> puzzle.initialBoard[r].clone() }
        val solutionsCount = SudokuEngine.countSolutions(copy, limit = 2)
        assertEquals("Generated puzzle must have exactly one unique solution", 1, solutionsCount)
    }

    @Test
    fun testDifficultySettings() {
        assertEquals(40, Difficulty.EASY.clues)
        assertEquals(5, Difficulty.EASY.maxMistakes)
        assertEquals(32, Difficulty.MEDIUM.clues)
        assertEquals(3, Difficulty.MEDIUM.maxMistakes)
        assertEquals(28, Difficulty.HARD.clues)
        assertEquals(3, Difficulty.HARD.maxMistakes)
        assertEquals(24, Difficulty.EXPERT.clues)
        assertEquals(3, Difficulty.EXPERT.maxMistakes)
    }

    @Test
    fun testMoveValidation() {
        val solution = SudokuEngine.generateCompletedBoard(Random(99))
        val currentBoard = Array(9) { IntArray(9) }

        val r = 0
        val c = 0
        val correctDigit = solution[r][c]
        val validResult = SudokuEngine.validateMove(currentBoard, solution, r, c, correctDigit)
        assertTrue("Move should be valid", validResult.isValid)
        assertTrue("Should match solution", validResult.isCorrectAgainstSolution)

        currentBoard[0][1] = 5
        val conflictResult = SudokuEngine.validateMove(currentBoard, solution, 0, 8, 5)
        assertFalse("Move should conflict with existing row digit", conflictResult.isValid)
        assertNotNull(conflictResult.conflictReason)
    }

    @Test
    fun testHintSystem() {
        val puzzle = SudokuEngine.generatePuzzle(Difficulty.MEDIUM, seed = 777L)
        val currentBoard = Array(9) { r -> puzzle.initialBoard[r].clone() }

        val hint = SudokuEngine.findHint(currentBoard, puzzle.solution)
        assertNotNull("Hint must not be null for incomplete board", hint)
        assertEquals("Hint value must match solution", puzzle.solution[hint!!.row][hint.col], hint.value)
    }

    @Test
    fun testBoardCompleteAndValid() {
        val validBoard = SudokuEngine.generateCompletedBoard(Random(1))
        assertTrue(SudokuEngine.isBoardCompleteAndValid(validBoard))

        val invalidBoard = Array(9) { r -> validBoard[r].clone() }
        invalidBoard[0][0] = invalidBoard[0][1]
        assertFalse(SudokuEngine.isBoardCompleteAndValid(invalidBoard))
    }
}
