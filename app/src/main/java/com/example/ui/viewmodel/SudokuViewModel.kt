package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GameSessionEntity
import com.example.data.repository.SudokuRepository
import com.example.logic.Difficulty
import com.example.logic.SudokuEngine
import com.example.ui.model.Cell
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoardSnapshot(
    val board: List<List<Cell>>,
    val mistakes: Int
)

data class SudokuUiState(
    val board: List<List<Cell>> = createEmptyBoard(),
    val solution: Array<IntArray> = Array(9) { IntArray(9) },
    val selectedRow: Int? = null,
    val selectedCol: Int? = null,
    val difficulty: Difficulty = Difficulty.EASY,
    val isDaily: Boolean = false,
    val timerSeconds: Long = 0L,
    val isPaused: Boolean = false,
    val mistakes: Int = 0,
    val maxMistakes: Int = 5,
    val isGameOver: Boolean = false,
    val isGameWon: Boolean = false,
    val isNotesMode: Boolean = false,
    val remainingCounts: Map<Int, Int> = (1..9).associateWith { 9 },
    val feedbackMessage: String? = null
)

fun createEmptyBoard(): List<List<Cell>> {
    return List(9) { r ->
        List(9) { c ->
            Cell(row = r, col = c)
        }
    }
}

class SudokuViewModel(
    private val repository: SudokuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SudokuUiState())
    val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<BoardSnapshot>()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                repository.initDefaultStickers()
                val session = repository.getActiveSessionOnce()
                if (session != null && session.boardData.isNotEmpty()) {
                    restoreSession(session)
                } else {
                    startNewGame(Difficulty.EASY, isDaily = false)
                }
            } catch (_: Exception) {
                startNewGame(Difficulty.EASY, isDaily = false)
            }
        }
        startTimerLoop()
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var saveCounter = 0
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (!state.isPaused && !state.isGameOver && !state.isGameWon) {
                    _uiState.update { it.copy(timerSeconds = it.timerSeconds + 1) }
                    saveCounter++
                    if (saveCounter >= 5) {
                        saveCounter = 0
                        persistCurrentSession()
                    }
                }
            }
        }
    }

    private fun persistCurrentSession() {
        val s = _uiState.value
        if (s.isGameOver || s.isGameWon) return
        viewModelScope.launch {
            try {
                val boardStr = serializeBoard(s.board)
                val solutionStr = serializeIntMatrix(s.solution)
                val notesStr = serializeNotes(s.board)
                repository.saveActiveSession(
                    difficulty = s.difficulty.name,
                    timerSeconds = s.timerSeconds,
                    mistakes = s.mistakes,
                    isPaused = s.isPaused,
                    isDaily = s.isDaily,
                    boardData = boardStr,
                    solutionData = solutionStr,
                    notesData = notesStr
                )
            } catch (_: Exception) {
                // Handled by structured FirestoreErrorHandler in repository
            }
        }
    }

    fun startNewGame(difficulty: Difficulty, isDaily: Boolean = false) {
        viewModelScope.launch {
            try {
                repository.clearActiveSession()
            } catch (_: Exception) {
                // Ignore if no active session exists
            }
            val seed = if (isDaily) repository.getTodayDateString().hashCode().toLong() else System.currentTimeMillis()
            val generated = SudokuEngine.generatePuzzle(difficulty, seed)

            val newBoard = List(9) { r ->
                List(9) { c ->
                    val v = generated.initialBoard[r][c]
                    Cell(
                        row = r,
                        col = c,
                        value = v,
                        isGiven = (v != 0),
                        notes = emptySet()
                    )
                }
            }

            undoStack.clear()
            _uiState.update {
                it.copy(
                    board = newBoard,
                    solution = generated.solution,
                    selectedRow = null,
                    selectedCol = null,
                    difficulty = difficulty,
                    isDaily = isDaily,
                    timerSeconds = 0L,
                    isPaused = false,
                    mistakes = 0,
                    maxMistakes = difficulty.maxMistakes,
                    isGameOver = false,
                    isGameWon = false,
                    isNotesMode = false,
                    remainingCounts = calculateRemainingCounts(newBoard),
                    feedbackMessage = if (isDaily) "Daily Challenge Initialized!" else "${difficulty.displayName} Game Started"
                )
            }
            persistCurrentSession()
        }
    }

    fun onCellClick(row: Int, col: Int) {
        if (_uiState.value.isPaused || _uiState.value.isGameOver || _uiState.value.isGameWon) return
        _uiState.update {
            it.copy(
                selectedRow = if (it.selectedRow == row && it.selectedCol == col) null else row,
                selectedCol = if (it.selectedRow == row && it.selectedCol == col) null else col,
                feedbackMessage = null
            )
        }
    }

    fun onNumberInput(digit: Int) {
        val state = _uiState.value
        val r = state.selectedRow ?: return
        val c = state.selectedCol ?: return
        if (state.isPaused || state.isGameOver || state.isGameWon) return

        val currentCell = state.board[r][c]
        if (currentCell.isGiven) return

        if (state.isNotesMode) {
            // Toggle note
            val newNotes = if (currentCell.notes.contains(digit)) {
                currentCell.notes - digit
            } else {
                currentCell.notes + digit
            }
            updateCell(r, c) { it.copy(notes = newNotes, value = 0, isError = false) }
            return
        }

        // Normal number input
        saveUndoSnapshot()

        // Move validation against current board & solution
        val currentMatrix = Array(9) { rowIdx -> IntArray(9) { colIdx -> state.board[rowIdx][colIdx].value } }
        val valResult = SudokuEngine.validateMove(currentMatrix, state.solution, r, c, digit)

        if (!valResult.isCorrectAgainstSolution) {
            // Mistake
            val newMistakes = state.mistakes + 1
            val isOver = newMistakes >= state.maxMistakes
            updateCell(r, c) { it.copy(value = digit, isError = true, notes = emptySet()) }
            _uiState.update {
                it.copy(
                    mistakes = newMistakes,
                    isGameOver = isOver,
                    feedbackMessage = if (isOver) "Game Over! Too many mistakes." else "Incorrect placement! ($newMistakes/${state.maxMistakes})"
                )
            }
            if (isOver) {
                viewModelScope.launch {
                    try {
                        repository.recordGameFinished(state.difficulty.name, state.timerSeconds, newMistakes, isWon = false)
                        repository.clearActiveSession()
                    } catch (_: Exception) {
                        // Logged in repository
                    }
                }
            }
            return
        }

        // Correct digit
        val updatedBoard = updateCellAndGetBoard(r, c) {
            it.copy(value = digit, isError = false, notes = emptySet())
        }
        // Remove this digit from notes in same row, column, and 3x3 box
        val cleanedBoard = cleanNotes(updatedBoard, r, c, digit)
        val remaining = calculateRemainingCounts(cleanedBoard)

        // Check if game won
        val isWon = isBoardFullySolved(cleanedBoard, state.solution)
        _uiState.update {
            it.copy(
                board = cleanedBoard,
                remainingCounts = remaining,
                isGameWon = isWon,
                feedbackMessage = if (isWon) "Victory! Puzzle Solved!" else null
            )
        }

        if (isWon) {
            handleVictory()
        } else {
            persistCurrentSession()
        }
    }

    private fun handleVictory() {
        val s = _uiState.value
        viewModelScope.launch {
            try {
                repository.recordGameFinished(s.difficulty.name, s.timerSeconds, s.mistakes, isWon = true)
                if (s.isDaily) {
                    repository.completeDailyChallenge(
                        dateString = repository.getTodayDateString(),
                        difficulty = s.difficulty.name,
                        timeSeconds = s.timerSeconds,
                        mistakes = s.mistakes,
                        stars = s.difficulty.stars
                    )
                }
                repository.clearActiveSession()
            } catch (_: Exception) {
                // Logged in repository
            }
        }
    }

    fun onErase() {
        val state = _uiState.value
        val r = state.selectedRow ?: return
        val c = state.selectedCol ?: return
        if (state.isPaused || state.isGameOver || state.isGameWon) return

        val cell = state.board[r][c]
        if (cell.isGiven) return

        saveUndoSnapshot()
        val updated = updateCellAndGetBoard(r, c) {
            it.copy(value = 0, isError = false, notes = emptySet())
        }
        _uiState.update {
            it.copy(
                board = updated,
                remainingCounts = calculateRemainingCounts(updated)
            )
        }
        persistCurrentSession()
    }

    fun onNotesToggle() {
        _uiState.update { it.copy(isNotesMode = !it.isNotesMode) }
    }

    fun onHint() {
        val state = _uiState.value
        if (state.isPaused || state.isGameOver || state.isGameWon) return

        val currentMatrix = Array(9) { r -> IntArray(9) { c -> state.board[r][c].value } }
        val hint = SudokuEngine.findHint(currentMatrix, state.solution) ?: return

        saveUndoSnapshot()
        val updatedBoard = updateCellAndGetBoard(hint.row, hint.col) {
            it.copy(
                value = hint.value,
                isHint = true,
                isError = false,
                notes = emptySet()
            )
        }
        val cleaned = cleanNotes(updatedBoard, hint.row, hint.col, hint.value)
        val remaining = calculateRemainingCounts(cleaned)
        val isWon = isBoardFullySolved(cleaned, state.solution)

        _uiState.update {
            it.copy(
                board = cleaned,
                selectedRow = hint.row,
                selectedCol = hint.col,
                remainingCounts = remaining,
                isGameWon = isWon,
                feedbackMessage = "Hint placed at (${hint.row + 1}, ${hint.col + 1})"
            )
        }

        if (isWon) {
            handleVictory()
        } else {
            persistCurrentSession()
        }
    }

    fun onUndo() {
        if (undoStack.isEmpty()) return
        val snapshot = undoStack.removeAt(undoStack.lastIndex)
        _uiState.update {
            it.copy(
                board = snapshot.board,
                mistakes = snapshot.mistakes,
                remainingCounts = calculateRemainingCounts(snapshot.board),
                feedbackMessage = "Move undone"
            )
        }
        persistCurrentSession()
    }

    fun onTogglePause() {
        val newPaused = !_uiState.value.isPaused
        _uiState.update { it.copy(isPaused = newPaused) }
        persistCurrentSession()
    }

    fun resumeGame() {
        _uiState.update { it.copy(isPaused = false) }
        persistCurrentSession()
    }

    fun resetBoard() {
        val s = _uiState.value
        val resetBoard = s.board.map { row ->
            row.map { cell ->
                if (cell.isGiven) cell
                else cell.copy(value = 0, isError = false, isHint = false, notes = emptySet())
            }
        }
        undoStack.clear()
        _uiState.update {
            it.copy(
                board = resetBoard,
                mistakes = 0,
                isGameOver = false,
                isGameWon = false,
                remainingCounts = calculateRemainingCounts(resetBoard),
                feedbackMessage = "Board reset to initial clues"
            )
        }
        persistCurrentSession()
    }

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    private fun saveUndoSnapshot() {
        val s = _uiState.value
        undoStack.add(BoardSnapshot(board = s.board, mistakes = s.mistakes))
        if (undoStack.size > 20) {
            undoStack.removeAt(0)
        }
    }

    private fun updateCell(r: Int, c: Int, transform: (Cell) -> Cell) {
        val newBoard = updateCellAndGetBoard(r, c, transform)
        _uiState.update { it.copy(board = newBoard) }
    }

    private fun updateCellAndGetBoard(r: Int, c: Int, transform: (Cell) -> Cell): List<List<Cell>> {
        return _uiState.value.board.mapIndexed { rowIdx, row ->
            if (rowIdx == r) {
                row.mapIndexed { colIdx, cell ->
                    if (colIdx == c) transform(cell) else cell
                }
            } else row
        }
    }

    private fun cleanNotes(board: List<List<Cell>>, row: Int, col: Int, digit: Int): List<List<Cell>> {
        val boxR = (row / 3) * 3
        val boxC = (col / 3) * 3
        return board.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, cell ->
                if (r == row || c == col || (r in boxR until boxR + 3 && c in boxC until boxC + 3)) {
                    if (cell.notes.contains(digit)) {
                        cell.copy(notes = cell.notes - digit)
                    } else cell
                } else cell
            }
        }
    }

    private fun calculateRemainingCounts(board: List<List<Cell>>): Map<Int, Int> {
        val counts = mutableMapOf<Int, Int>()
        for (i in 1..9) counts[i] = 9
        for (row in board) {
            for (cell in row) {
                if (cell.value in 1..9 && !cell.isError) {
                    val cur = counts[cell.value] ?: 9
                    counts[cell.value] = (cur - 1).coerceAtLeast(0)
                }
            }
        }
        return counts
    }

    private fun isBoardFullySolved(board: List<List<Cell>>, solution: Array<IntArray>): Boolean {
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val cell = board[r][c]
                if (cell.value == 0 || cell.isError || cell.value != solution[r][c]) {
                    return false
                }
            }
        }
        return true
    }

    private fun serializeBoard(board: List<List<Cell>>): String {
        return board.flatten().joinToString(",") { "${it.value}:${if (it.isGiven) 1 else 0}:${if (it.isError) 1 else 0}" }
    }

    private fun serializeIntMatrix(matrix: Array<IntArray>): String {
        return matrix.flatMap { it.asIterable() }.joinToString(",")
    }

    private fun serializeNotes(board: List<List<Cell>>): String {
        return board.flatten().joinToString(";") { it.notes.joinToString(",") }
    }

    private fun restoreSession(session: GameSessionEntity) {
        try {
            val cellItems = session.boardData.split(",")
            val solutionDigits = session.solutionData.split(",").map { it.toInt() }
            val noteItems = session.notesData.split(";")

            val solMatrix = Array(9) { r ->
                IntArray(9) { c ->
                    solutionDigits[r * 9 + c]
                }
            }

            val restoredBoard = List(9) { r ->
                List(9) { c ->
                    val idx = r * 9 + c
                    val parts = cellItems[idx].split(":")
                    val v = parts[0].toInt()
                    val isGiven = parts.getOrNull(1) == "1"
                    val isError = parts.getOrNull(2) == "1"
                    val notesSet = noteItems.getOrNull(idx)
                        ?.split(",")
                        ?.filter { it.isNotEmpty() }
                        ?.mapNotNull { it.toIntOrNull() }
                        ?.toSet() ?: emptySet()

                    Cell(
                        row = r,
                        col = c,
                        value = v,
                        isGiven = isGiven,
                        isError = isError,
                        notes = notesSet
                    )
                }
            }

            val diff = Difficulty.fromName(session.difficulty)
            _uiState.update {
                it.copy(
                    board = restoredBoard,
                    solution = solMatrix,
                    difficulty = diff,
                    isDaily = session.isDaily,
                    timerSeconds = session.timerSeconds,
                    mistakes = session.mistakes,
                    maxMistakes = diff.maxMistakes,
                    isPaused = session.isPaused,
                    remainingCounts = calculateRemainingCounts(restoredBoard),
                    feedbackMessage = "Game resumed"
                )
            }
        } catch (_: Exception) {
            startNewGame(Difficulty.EASY, isDaily = false)
        }
    }
}
