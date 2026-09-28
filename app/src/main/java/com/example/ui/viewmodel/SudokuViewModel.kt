package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.GameRecordEntity
import com.example.data.database.entity.StickerEntity
import com.example.data.model.Cell
import com.example.data.model.Difficulty
import com.example.data.model.GameMove
import com.example.data.model.HintResult
import com.example.data.repository.SudokuRepository
import com.example.engine.SudokuEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SudokuUiState(
    val board: List<List<Cell>> = emptyList(),
    val selectedCell: Pair<Int, Int>? = null,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val isDaily: Boolean = false,
    val dailyDate: String = "",
    val elapsedSeconds: Long = 0,
    val isPaused: Boolean = false,
    val mistakes: Int = 0,
    val maxMistakes: Int = 3,
    val isNotesMode: Boolean = false,
    val hintsUsed: Int = 0,
    val activeHint: HintResult? = null,
    val isGameWon: Boolean = false,
    val newlyUnlockedStickers: List<StickerEntity> = emptyList(),
    val numberCounts: Map<Int, Int> = emptyMap(),
    val canUndo: Boolean = false
)

class SudokuViewModel(
    private val repository: SudokuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SudokuUiState())
    val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

    private var solutionBoard: Array<IntArray> = Array(9) { IntArray(9) }
    private var initialBoard: Array<IntArray> = Array(9) { IntArray(9) }
    private val undoStack = mutableListOf<GameMove>()
    private var timerJob: Job? = null
    private var periodicSaveJob: Job? = null

    init {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded(todayStr)
            
            // Check for existing active game to restore session timer & board
            val existing = repository.getActiveGameSync()
            if (existing != null && existing.initialBoard.length == 81 && existing.solutionBoard.length == 81) {
                restoreExistingGame(existing)
            } else {
                startNewGame(Difficulty.MEDIUM)
            }
        }
    }

    private fun restoreExistingGame(game: GameRecordEntity) {
        val diff = Difficulty.values().firstOrNull { it.name == game.difficulty } ?: Difficulty.MEDIUM
        val init = Array(9) { r ->
            IntArray(9) { c ->
                game.initialBoard[r * 9 + c].digitToInt()
            }
        }
        val sol = Array(9) { r ->
            IntArray(9) { c ->
                game.solutionBoard[r * 9 + c].digitToInt()
            }
        }
        val curr = List(9) { r ->
            List(9) { c ->
                val v = game.currentBoard[r * 9 + c].digitToInt()
                val isG = game.initialBoard[r * 9 + c].digitToInt() != 0
                val isErr = v != 0 && v != sol[r][c]
                Cell(
                    row = r,
                    col = c,
                    value = v,
                    isGiven = isG,
                    notes = emptySet(),
                    isError = isErr,
                    isHinted = false
                )
            }
        }

        initialBoard = init
        solutionBoard = sol
        undoStack.clear()

        _uiState.update {
            it.copy(
                board = curr,
                selectedCell = null,
                difficulty = diff,
                isDaily = game.isDaily,
                dailyDate = game.dailyDate ?: "",
                elapsedSeconds = game.elapsedSeconds,
                isPaused = game.isPaused,
                mistakes = game.mistakes,
                hintsUsed = game.hintsUsed,
                isGameWon = false,
                canUndo = false,
                numberCounts = calculateNumberCounts(curr)
            )
        }

        startTimer()
    }

    fun startNewGame(difficulty: Difficulty, isDaily: Boolean = false) {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val seed = if (isDaily) SudokuEngine.dateToSeed(todayStr) else null
        val puzzle = SudokuEngine.generatePuzzle(difficulty, seed)

        solutionBoard = puzzle.solutionBoard
        initialBoard = puzzle.initialBoard
        undoStack.clear()

        val newBoard = List(9) { r ->
            List(9) { c ->
                val v = puzzle.initialBoard[r][c]
                Cell(
                    row = r,
                    col = c,
                    value = v,
                    isGiven = v != 0,
                    notes = emptySet(),
                    isError = false,
                    isHinted = false
                )
            }
        }

        _uiState.update {
            it.copy(
                board = newBoard,
                selectedCell = null,
                difficulty = difficulty,
                isDaily = isDaily,
                dailyDate = todayStr,
                elapsedSeconds = 0,
                isPaused = false,
                mistakes = 0,
                hintsUsed = 0,
                activeHint = null,
                isGameWon = false,
                newlyUnlockedStickers = emptyList(),
                canUndo = false,
                numberCounts = calculateNumberCounts(newBoard)
            )
        }

        startTimer()
        saveSessionToDatabase()
    }

    fun togglePauseTimer() {
        val nextPaused = !_uiState.value.isPaused
        _uiState.update { it.copy(isPaused = nextPaused) }
        saveSessionToDatabase()
    }

    fun resumeTimer() {
        if (_uiState.value.isPaused) {
            _uiState.update { it.copy(isPaused = false) }
            saveSessionToDatabase()
        }
    }

    fun selectCell(row: Int, col: Int) {
        if (_uiState.value.isPaused) return
        _uiState.update { current ->
            val updatedBoard = current.board.map { rList ->
                rList.map { cell -> if (cell.isHinted) cell.copy(isHinted = false) else cell }
            }
            current.copy(
                selectedCell = row to col,
                board = updatedBoard,
                activeHint = null
            )
        }
    }

    fun toggleNotesMode() {
        _uiState.update { it.copy(isNotesMode = !it.isNotesMode) }
    }

    fun onNumberInput(digit: Int) {
        val state = _uiState.value
        if (state.isPaused) return
        val selected = state.selectedCell ?: return
        val (row, col) = selected
        val currentCell = state.board[row][col]

        if (currentCell.isGiven || currentCell.value == digit) return

        if (state.isNotesMode) {
            val newNotes = if (currentCell.notes.contains(digit)) {
                currentCell.notes - digit
            } else {
                currentCell.notes + digit
            }

            val updatedBoard = state.board.mapIndexed { r, rList ->
                rList.mapIndexed { c, cell ->
                    if (r == row && c == col) cell.copy(notes = newNotes, value = 0) else cell
                }
            }

            undoStack.add(
                GameMove(
                    row = row,
                    col = col,
                    oldValue = currentCell.value,
                    newValue = 0,
                    oldNotes = currentCell.notes,
                    newNotes = newNotes
                )
            )

            _uiState.update {
                it.copy(board = updatedBoard, canUndo = true)
            }
        } else {
            val isCorrect = solutionBoard[row][col] == digit
            val newMistakes = if (!isCorrect) state.mistakes + 1 else state.mistakes

            val updatedBoard = state.board.mapIndexed { r, rList ->
                rList.mapIndexed { c, cell ->
                    if (r == row && c == col) {
                        cell.copy(
                            value = digit,
                            notes = emptySet(),
                            isError = !isCorrect,
                            isHinted = false
                        )
                    } else if (isCorrect && (r == row || c == col || ((r / 3 == row / 3) && (c / 3 == col / 3)))) {
                        cell.copy(notes = cell.notes - digit)
                    } else {
                        cell
                    }
                }
            }

            undoStack.add(
                GameMove(
                    row = row,
                    col = col,
                    oldValue = currentCell.value,
                    newValue = digit,
                    oldNotes = currentCell.notes,
                    newNotes = emptySet()
                )
            )

            val counts = calculateNumberCounts(updatedBoard)
            val isWon = isBoardComplete(updatedBoard)

            _uiState.update {
                it.copy(
                    board = updatedBoard,
                    mistakes = newMistakes,
                    isGameWon = isWon,
                    canUndo = true,
                    numberCounts = counts
                )
            }

            if (isWon) {
                stopTimer()
                handleVictory(newMistakes)
            }
        }
    }

    fun eraseSelectedCell() {
        val state = _uiState.value
        if (state.isPaused) return
        val (row, col) = state.selectedCell ?: return
        val currentCell = state.board[row][col]

        if (currentCell.isGiven || (currentCell.value == 0 && currentCell.notes.isEmpty())) return

        val updatedBoard = state.board.mapIndexed { r, rList ->
            rList.mapIndexed { c, cell ->
                if (r == row && c == col) {
                    cell.copy(value = 0, notes = emptySet(), isError = false, isHinted = false)
                } else cell
            }
        }

        undoStack.add(
            GameMove(
                row = row,
                col = col,
                oldValue = currentCell.value,
                newValue = 0,
                oldNotes = currentCell.notes,
                newNotes = emptySet()
            )
        )

        _uiState.update {
            it.copy(
                board = updatedBoard,
                canUndo = true,
                numberCounts = calculateNumberCounts(updatedBoard)
            )
        }
    }

    fun undo() {
        if (undoStack.isEmpty() || _uiState.value.isPaused) return
        val lastMove = undoStack.removeAt(undoStack.lastIndex)

        val updatedBoard = _uiState.value.board.mapIndexed { r, rList ->
            rList.mapIndexed { c, cell ->
                if (r == lastMove.row && c == lastMove.col) {
                    val isCorrect = lastMove.oldValue == 0 || solutionBoard[r][c] == lastMove.oldValue
                    cell.copy(
                        value = lastMove.oldValue,
                        notes = lastMove.oldNotes,
                        isError = !isCorrect,
                        isHinted = false
                    )
                } else cell
            }
        }

        _uiState.update {
            it.copy(
                board = updatedBoard,
                canUndo = undoStack.isNotEmpty(),
                numberCounts = calculateNumberCounts(updatedBoard)
            )
        }
    }

    fun requestHint() {
        val state = _uiState.value
        if (state.isPaused) return
        val currentBoardArray = Array(9) { r ->
            IntArray(9) { c -> state.board[r][c].value }
        }

        val hint = SudokuEngine.findSmartHint(currentBoardArray, solutionBoard)
        if (hint != null) {
            val updatedBoard = state.board.mapIndexed { r, rList ->
                rList.mapIndexed { c, cell ->
                    if (r == hint.row && c == hint.col) {
                        cell.copy(isHinted = true)
                    } else cell
                }
            }

            _uiState.update {
                it.copy(
                    board = updatedBoard,
                    selectedCell = hint.row to hint.col,
                    activeHint = hint,
                    hintsUsed = it.hintsUsed + 1
                )
            }
        }
    }

    fun applyHint(row: Int, col: Int, targetVal: Int) {
        val state = _uiState.value
        val updatedBoard = state.board.mapIndexed { r, rList ->
            rList.mapIndexed { c, cell ->
                if (r == row && c == col) {
                    cell.copy(
                        value = targetVal,
                        notes = emptySet(),
                        isError = false,
                        isHinted = false
                    )
                } else if (r == row || c == col || ((r / 3 == row / 3) && (c / 3 == col / 3))) {
                    cell.copy(notes = cell.notes - targetVal)
                } else {
                    cell
                }
            }
        }

        val counts = calculateNumberCounts(updatedBoard)
        val isWon = isBoardComplete(updatedBoard)

        _uiState.update {
            it.copy(
                board = updatedBoard,
                activeHint = null,
                isGameWon = isWon,
                numberCounts = counts
            )
        }

        if (isWon) {
            stopTimer()
            handleVictory(state.mistakes)
        }
    }

    fun dismissHint() {
        _uiState.update { it.copy(activeHint = null) }
    }

    fun dismissVictoryDialog() {
        _uiState.update { it.copy(isGameWon = false, newlyUnlockedStickers = emptyList()) }
    }

    private fun handleVictory(mistakes: Int) {
        val state = _uiState.value
        viewModelScope.launch {
            val newlyUnlocked = repository.recordGameWin(
                difficulty = state.difficulty,
                elapsedSeconds = state.elapsedSeconds,
                mistakes = mistakes,
                hintsUsed = state.hintsUsed,
                isDaily = state.isDaily,
                todayDateString = state.dailyDate,
                playerName = "Obsidian Solver"
            )

            repository.clearActiveGame()

            _uiState.update {
                it.copy(
                    isGameWon = true,
                    newlyUnlockedStickers = newlyUnlocked
                )
            }
        }
    }

    private fun isBoardComplete(board: List<List<Cell>>): Boolean {
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val cell = board[r][c]
                if (cell.value == 0 || cell.value != solutionBoard[r][c]) {
                    return false
                }
            }
        }
        return true
    }

    private fun calculateNumberCounts(board: List<List<Cell>>): Map<Int, Int> {
        val counts = mutableMapOf<Int, Int>()
        for (i in 1..9) counts[i] = 9
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val v = board[r][c].value
                if (v in 1..9) {
                    counts[v] = (counts[v] ?: 9) - 1
                }
            }
        }
        return counts
    }

    private fun saveSessionToDatabase() {
        val state = _uiState.value
        if (state.board.isEmpty() || state.isGameWon) return

        val initDigits = StringBuilder()
        val currDigits = StringBuilder()
        val solDigits = StringBuilder()

        for (r in 0 until 9) {
            for (c in 0 until 9) {
                initDigits.append(initialBoard[r][c])
                currDigits.append(state.board[r][c].value)
                solDigits.append(solutionBoard[r][c])
            }
        }

        viewModelScope.launch {
            repository.saveActiveGame(
                GameRecordEntity(
                    difficulty = state.difficulty.name,
                    isDaily = state.isDaily,
                    dailyDate = state.dailyDate,
                    initialBoard = initDigits.toString(),
                    currentBoard = currDigits.toString(),
                    solutionBoard = solDigits.toString(),
                    notesBoard = "",
                    elapsedSeconds = state.elapsedSeconds,
                    isPaused = state.isPaused,
                    mistakes = state.mistakes,
                    hintsUsed = state.hintsUsed,
                    isCompleted = false
                )
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        periodicSaveJob?.cancel()

        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (!_uiState.value.isPaused && !_uiState.value.isGameWon) {
                    _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }

        // Periodically persist timer & game session to database every 5 seconds
        periodicSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                if (!_uiState.value.isGameWon) {
                    saveSessionToDatabase()
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        periodicSaveJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        saveSessionToDatabase()
    }
}
