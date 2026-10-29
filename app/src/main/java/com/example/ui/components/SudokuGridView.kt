package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.Cell
import com.example.ui.theme.CellErrorText
import com.example.ui.theme.CellGivenText
import com.example.ui.theme.CellHintHighlight
import com.example.ui.theme.CellNoteText
import com.example.ui.theme.CellRelated
import com.example.ui.theme.CellSameNumber
import com.example.ui.theme.CellSelected
import com.example.ui.theme.CellUserText
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderStrong
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianErrorBg
import com.example.ui.theme.ObsidianPrimary
import com.example.ui.theme.ObsidianSurface

@Composable
fun SudokuGridView(
    board: List<List<Cell>>,
    selectedRow: Int?,
    selectedCol: Int?,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (board.size != 9 || board.any { it.size != 9 }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(ObsidianCard, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Preparing Board...", color = Color.White)
        }
        return
    }

    val selectedValue = if (selectedRow != null && selectedCol != null) {
        board[selectedRow][selectedCol].value
    } else 0

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(2.dp, ObsidianBorderStrong, RoundedCornerShape(12.dp))
            .padding(2.dp)
            .testTag("sudoku_grid")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (r in 0 until 9) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (c in 0 until 9) {
                        val cell = board[r][c]
                        val isSelected = (r == selectedRow && c == selectedCol)
                        val isRelated = (r == selectedRow || c == selectedCol ||
                                (selectedRow != null && selectedCol != null &&
                                        (r / 3 == selectedRow / 3) && (c / 3 == selectedCol / 3)))
                        val isSameNum = (selectedValue != 0 && cell.value == selectedValue)

                        SudokuCellView(
                            cell = cell,
                            isSelected = isSelected,
                            isRelated = isRelated,
                            isSameNumber = isSameNum,
                            onClick = { onCellClick(r, c) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SudokuCellView(
    cell: Cell,
    isSelected: Boolean,
    isRelated: Boolean,
    isSameNumber: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = com.example.ui.theme.LocalThemePalette.current
    val targetBgColor = when {
        cell.isError -> ObsidianErrorBg
        isSelected -> palette.cellSelected
        isSameNumber -> palette.cellSameNumber
        cell.isHint -> palette.secondary.copy(alpha = 0.35f)
        isRelated -> palette.cellRelated
        else -> Color.Transparent
    }

    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(150),
        label = "cellBg"
    )

    // Border thickness for 3x3 blocks
    val isBottomThick = (cell.row % 3 == 2 && cell.row != 8)
    val isRightThick = (cell.col % 3 == 2 && cell.col != 8)

    Box(
        modifier = modifier
            .background(animatedBgColor)
            .border(
                width = 0.5.dp,
                color = palette.border
            )
            .clickable { onClick() }
            .testTag("cell_${cell.row}_${cell.col}"),
        contentAlignment = Alignment.Center
    ) {
        // Block separator borders
        if (isRightThick) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .size(width = 2.dp, height = 0.dp)
                    .background(palette.borderStrong)
            )
        }
        if (isBottomThick) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .size(width = 0.dp, height = 2.dp)
                    .background(palette.borderStrong)
            )
        }

        if (cell.value != 0) {
            val textColor = when {
                cell.isError -> CellErrorText
                cell.isGiven -> palette.cellGivenText
                cell.isHint -> palette.primary
                else -> palette.cellUserText
            }

            Text(
                text = cell.value.toString(),
                color = textColor,
                fontSize = 20.sp,
                fontWeight = if (cell.isGiven) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        } else if (cell.notes.isNotEmpty()) {
            // Notes 3x3 display
            NotesGridView(notes = cell.notes)
        }
    }
}

@Composable
fun NotesGridView(notes: Set<Int>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(1.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0 until 3) {
                    val digit = row * 3 + col + 1
                    Text(
                        text = if (notes.contains(digit)) digit.toString() else " ",
                        color = CellNoteText,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
