package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Cell
import com.example.ui.theme.CellBgError
import com.example.ui.theme.CellBgHighlighted
import com.example.ui.theme.CellBgNormal
import com.example.ui.theme.CellBgSameNumber
import com.example.ui.theme.CellBgSelected
import com.example.ui.theme.DigitError
import com.example.ui.theme.DigitGiven
import com.example.ui.theme.DigitNote
import com.example.ui.theme.DigitUser
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDivider

@Composable
fun SudokuGridView(
    board: List<List<Cell>>,
    selectedCell: Pair<Int, Int>?,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedVal = selectedCell?.let { (r, c) -> board.getOrNull(r)?.getOrNull(c)?.value } ?: 0

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0C1017))
            .border(2.dp, ObsidianDivider, RoundedCornerShape(12.dp))
            .testTag("sudoku_grid_view"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            for (row in 0 until 9) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 9) {
                        val cell = board[row][col]
                        val isSelected = selectedCell?.first == row && selectedCell.second == col
                        val isSameRowOrColOrBox = selectedCell != null && (
                            selectedCell.first == row ||
                                selectedCell.second == col ||
                                ((selectedCell.first / 3 == row / 3) && (selectedCell.second / 3 == col / 3))
                            )
                        val isSameNum = selectedVal > 0 && cell.value == selectedVal

                        SudokuCellView(
                            cell = cell,
                            isSelected = isSelected,
                            isHighlighted = isSameRowOrColOrBox,
                            isSameNumber = isSameNum,
                            onClick = { onCellClick(row, col) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SudokuCellView(
    cell: Cell,
    isSelected: Boolean,
    isHighlighted: Boolean,
    isSameNumber: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetBgColor = when {
        cell.isError -> CellBgError
        isSelected -> CellBgSelected
        isSameNumber -> CellBgSameNumber
        cell.isHinted -> Color(0x33FFB300)
        isHighlighted -> CellBgHighlighted
        else -> CellBgNormal
    }

    val animatedBg by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 150),
        label = "cellBg"
    )

    val isRightBorderThick = cell.col == 2 || cell.col == 5
    val isBottomBorderThick = cell.row == 2 || cell.row == 5

    val endPadding = if (isRightBorderThick) 1.5.dp else 0.5.dp
    val bottomPadding = if (isBottomBorderThick) 1.5.dp else 0.5.dp

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(end = endPadding, bottom = bottomPadding)
            .background(if (isRightBorderThick || isBottomBorderThick) ObsidianDivider else ObsidianBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedBg)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, NeonCyan)
                    } else if (cell.isHinted) {
                        Modifier.border(2.dp, NeonAmber)
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = NeonCyan),
                    onClick = onClick
                )
                .testTag("sudoku_cell_${cell.row}_${cell.col}"),
            contentAlignment = Alignment.Center
        ) {
            when {
                cell.value != 0 -> {
                    val digitColor = when {
                        cell.isError -> DigitError
                        cell.isGiven -> DigitGiven
                        else -> DigitUser
                    }

                    Text(
                        text = cell.value.toString(),
                        fontSize = 24.sp,
                        fontWeight = if (cell.isGiven) FontWeight.Bold else FontWeight.SemiBold,
                        color = digitColor,
                        textAlign = TextAlign.Center
                    )
                }
                cell.notes.isNotEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (noteRow in 0..2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (noteCol in 0..2) {
                                    val digit = noteRow * 3 + noteCol + 1
                                    val hasDigit = cell.notes.contains(digit)
                                    Text(
                                        text = if (hasDigit) digit.toString() else "",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DigitNote,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
