package com.example

import com.example.data.model.Difficulty
import com.example.engine.SudokuEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun puzzleGeneration_hasValidCluesAndSolution() {
    val puzzle = SudokuEngine.generatePuzzle(Difficulty.EASY, seed = 12345L)
    assertEquals(9, puzzle.initialBoard.size)
    assertEquals(9, puzzle.solutionBoard.size)

    for (r in 0 until 9) {
      for (c in 0 until 9) {
        val solVal = puzzle.solutionBoard[r][c]
        assertTrue("Solution value should be between 1 and 9", solVal in 1..9)
      }
    }

    var emptyCount = 0
    for (r in 0 until 9) {
      for (c in 0 until 9) {
        if (puzzle.initialBoard[r][c] == 0) emptyCount++
      }
    }
    assertTrue("Should have empty cells to solve", emptyCount > 0)
  }

  @Test
  fun smartHint_returnsValidSuggestion() {
    val puzzle = SudokuEngine.generatePuzzle(Difficulty.EASY, seed = 12345L)
    val hint = SudokuEngine.findSmartHint(puzzle.initialBoard, puzzle.solutionBoard)
    assertNotNull(hint)
    assertTrue(hint!!.targetValue in 1..9)
    assertEquals(puzzle.solutionBoard[hint.row][hint.col], hint.targetValue)
  }

  @Test
  fun timerFormatting_test() {
    val seconds: Long = 3665 // 1 hour, 1 minute, 5 seconds
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    val formatted = String.format("%02d:%02d:%02d", hours, minutes, secs)
    assertEquals("01:01:05", formatted)
  }
}
