package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.Difficulty
import com.example.ui.components.DifficultyDialog
import com.example.ui.components.PersistentTimerComponent
import com.example.ui.components.SudokuControls
import com.example.ui.components.SudokuGridView
import com.example.ui.components.SudokuPauseOverlay
import com.example.ui.components.formatTimer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DigitGiven
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianPrimary
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianViolet
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.SudokuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    viewModel: SudokuViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDifficultyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: Difficulty picker & Reset board
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianCard)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                    .clickable { showDifficultyDialog = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("difficulty_selector"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.difficulty.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianViolet
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "▼",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Reset Board Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianCard)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                        .clickable { viewModel.resetBoard() }
                        .testTag("reset_board_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Board",
                        tint = DigitGiven,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // New Game Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianCard)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                        .clickable { showDifficultyDialog = true }
                        .testTag("new_game_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Game",
                        tint = ObsidianViolet,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Timer and Mistakes Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PersistentTimerComponent(
                seconds = uiState.timerSeconds,
                isPaused = uiState.isPaused,
                onTogglePause = { viewModel.onTogglePause() }
            )

            // Mistakes Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianCard)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("mistakes_display"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Mistakes",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${uiState.mistakes}/${uiState.maxMistakes}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.mistakes > 0) CrimsonError else DigitGiven
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sudoku Grid Container with Pause Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            SudokuGridView(
                board = uiState.board,
                selectedRow = uiState.selectedRow,
                selectedCol = uiState.selectedCol,
                onCellClick = { r, c -> viewModel.onCellClick(r, c) }
            )

            if (uiState.isPaused) {
                SudokuPauseOverlay(
                    onResume = { viewModel.resumeGame() }
                )
            }
        }

        // Feedback Banner
        AnimatedVisibility(
            visible = uiState.feedbackMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.feedbackMessage?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianCard)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Controls and Numpad
        SudokuControls(
            isNotesMode = uiState.isNotesMode,
            remainingCounts = uiState.remainingCounts,
            onNumberClick = { digit -> viewModel.onNumberInput(digit) },
            onUndoClick = { viewModel.onUndo() },
            onEraseClick = { viewModel.onErase() },
            onNotesToggle = { viewModel.onNotesToggle() },
            onHintClick = { viewModel.onHint() },
            canUndo = viewModel.canUndo
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Difficulty Dialog
    if (showDifficultyDialog) {
        DifficultyDialog(
            currentDifficulty = uiState.difficulty,
            onSelectDifficulty = { diff ->
                viewModel.startNewGame(diff, isDaily = false)
            },
            onDismiss = { showDifficultyDialog = false }
        )
    }

    // Victory Dialog
    if (uiState.isGameWon) {
        VictoryDialog(
            timeFormatted = formatTimer(uiState.timerSeconds),
            mistakes = uiState.mistakes,
            difficulty = uiState.difficulty.displayName,
            onPlayAgain = {
                viewModel.startNewGame(uiState.difficulty, isDaily = false)
            }
        )
    }

    // Game Over Dialog
    if (uiState.isGameOver) {
        GameOverDialog(
            mistakes = uiState.mistakes,
            onTryAgain = {
                viewModel.startNewGame(uiState.difficulty, isDaily = false)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VictoryDialog(
    timeFormatted: String,
    mistakes: Int,
    difficulty: String,
    onPlayAgain: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = {},
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianSurface)
            .border(1.dp, EmeraldGreen, RoundedCornerShape(20.dp))
            .padding(24.dp)
            .testTag("victory_dialog")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(EmeraldGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Puzzle Solved!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Time", fontSize = 12.sp, color = Color.Gray)
                    Text(timeFormatted, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mistakes", fontSize = 12.sp, color = Color.Gray)
                    Text("$mistakes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (mistakes == 0) EmeraldGreen else WarmAmber)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Difficulty", fontSize = 12.sp, color = Color.Gray)
                    Text(difficulty, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ObsidianViolet)
                }
            }

            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("victory_play_again_button")
            ) {
                Text("Play Again", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameOverDialog(
    mistakes: Int,
    onTryAgain: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = {},
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianSurface)
            .border(1.dp, CrimsonError, RoundedCornerShape(20.dp))
            .padding(24.dp)
            .testTag("game_over_dialog")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(CrimsonError.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = CrimsonError,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Game Over",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "You made $mistakes mistakes. Don't worry, every obsidian master hones their focus through practice.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontSize = 14.sp
            )

            Button(
                onClick = onTryAgain,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("try_again_button")
            ) {
                Text("Try Again", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
