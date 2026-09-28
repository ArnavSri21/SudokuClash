package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Difficulty
import com.example.ui.components.PersistentTimerComponent
import com.example.ui.components.SmartHintDialog
import com.example.ui.components.SudokuActionBar
import com.example.ui.components.SudokuGridView
import com.example.ui.components.SudokuKeypad
import com.example.ui.components.SudokuPauseOverlay
import com.example.ui.components.VictoryCelebrationDialog
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SudokuViewModel

@Composable
fun PlayScreen(
    viewModel: SudokuViewModel,
    onNavigateToStickers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDifficultyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App header bar with Difficulty Chip & Persistent Timer Component
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Difficulty Selector Chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianCard)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                    .clickable { showDifficultyDialog = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("difficulty_selector_chip"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isDaily) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Daily Challenge",
                        tint = NeonAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Daily Challenge",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber
                    )
                } else {
                    Text(
                        text = uiState.difficulty.displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Row {
                        for (i in 1..uiState.difficulty.stars) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Right side: Persistent Timer Component & New Game button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dedicated Persistent Timer Component with pause/resume toggle
                PersistentTimerComponent(
                    elapsedSeconds = uiState.elapsedSeconds,
                    isPaused = uiState.isPaused,
                    onTogglePause = { viewModel.togglePauseTimer() }
                )

                // Mistakes Counter Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (uiState.mistakes > 0) Color(0x33FF3D71) else ObsidianCard)
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "${uiState.mistakes}/${uiState.maxMistakes}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.mistakes > 0) NeonRose else TextSecondary
                    )
                }

                // New Game button
                IconButton(
                    onClick = { showDifficultyDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianCard)
                        .testTag("new_game_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "New Game",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sudoku 9x9 Grid View with Pause Overlay support
        Box(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SudokuGridView(
                board = uiState.board,
                selectedCell = uiState.selectedCell,
                onCellClick = { r, c -> viewModel.selectCell(r, c) }
            )

            // When game timer is paused, hide the grid with pause overlay
            if (uiState.isPaused) {
                SudokuPauseOverlay(
                    onResume = { viewModel.resumeTimer() },
                    elapsedSeconds = uiState.elapsedSeconds
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Controls (Undo, Erase, Notes, Hint)
        Box(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
        ) {
            SudokuActionBar(
                isNotesMode = uiState.isNotesMode,
                onToggleNotesMode = { viewModel.toggleNotesMode() },
                onUndoClick = { viewModel.undo() },
                onEraseClick = { viewModel.eraseSelectedCell() },
                onHintClick = { viewModel.requestHint() },
                canUndo = uiState.canUndo
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Keypad 1-9
        Box(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
        ) {
            SudokuKeypad(
                numberCounts = uiState.numberCounts,
                onNumberClick = { digit -> viewModel.onNumberInput(digit) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    // Smart Hint Dialog
    if (uiState.activeHint != null) {
        SmartHintDialog(
            hint = uiState.activeHint,
            onDismiss = { viewModel.dismissHint() },
            onApplyHint = { r, c, target -> viewModel.applyHint(r, c, target) }
        )
    }

    // Victory Dialog
    if (uiState.isGameWon) {
        VictoryCelebrationDialog(
            difficultyName = if (uiState.isDaily) "Daily Challenge" else uiState.difficulty.displayName,
            elapsedSeconds = uiState.elapsedSeconds,
            mistakes = uiState.mistakes,
            streakCount = 1,
            unlockedStickers = uiState.newlyUnlockedStickers,
            onPlayAgain = {
                viewModel.dismissVictoryDialog()
                viewModel.startNewGame(uiState.difficulty)
            },
            onGoToStickers = {
                viewModel.dismissVictoryDialog()
                onNavigateToStickers()
            },
            onDismiss = { viewModel.dismissVictoryDialog() }
        )
    }

    // Difficulty selection dialog
    if (showDifficultyDialog) {
        DifficultySelectionDialog(
            currentDifficulty = uiState.difficulty,
            onSelect = { diff ->
                showDifficultyDialog = false
                viewModel.startNewGame(diff)
            },
            onDismiss = { showDifficultyDialog = false }
        )
    }
}

@Composable
private fun DifficultySelectionDialog(
    currentDifficulty: Difficulty,
    onSelect: (Difficulty) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
                .testTag("dialog_difficulty_select"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT DIFFICULTY",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                listOf(
                    Difficulty.EASY,
                    Difficulty.MEDIUM,
                    Difficulty.HARD,
                    Difficulty.EXPERT
                ).forEach { diff ->
                    val isSelected = diff == currentDifficulty
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else ObsidianCard)
                            .border(
                                1.dp,
                                if (isSelected) NeonCyan else ObsidianBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(diff) }
                            .padding(14.dp)
                            .testTag("difficulty_option_${diff.name.lowercase()}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = diff.displayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeonCyan else TextPrimary
                            )
                            Text(
                                text = "${diff.clueCount} initial clues",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Row {
                            for (i in 1..diff.stars) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isSelected) NeonCyan else NeonAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    }
}
