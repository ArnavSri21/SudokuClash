package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CellNoteText
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianPrimary
import com.example.ui.theme.ObsidianSecondary
import com.example.ui.theme.ObsidianSurfaceElevated

@Composable
fun SudokuControls(
    isNotesMode: Boolean,
    remainingCounts: Map<Int, Int>,
    onNumberClick: (Int) -> Unit,
    onUndoClick: () -> Unit,
    onEraseClick: () -> Unit,
    onNotesToggle: () -> Unit,
    onHintClick: () -> Unit,
    canUndo: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                label = "Undo",
                enabled = canUndo,
                onClick = onUndoClick,
                testTag = "btn_undo"
            )
            ActionButton(
                icon = Icons.Default.Delete,
                label = "Erase",
                enabled = true,
                onClick = onEraseClick,
                testTag = "btn_erase"
            )
            ActionButton(
                icon = Icons.Default.Edit,
                label = if (isNotesMode) "Notes ON" else "Notes",
                isActive = isNotesMode,
                enabled = true,
                onClick = onNotesToggle,
                testTag = "btn_notes"
            )
            ActionButton(
                icon = Icons.Default.Lightbulb,
                label = "Hint",
                enabled = true,
                onClick = onHintClick,
                tint = ObsidianSecondary,
                testTag = "btn_hint"
            )
        }

        // Numpad Row 1..9
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (digit in 1..9) {
                val remaining = remainingCounts[digit] ?: 9
                val isCompleted = (remaining <= 0)

                NumpadButton(
                    digit = digit,
                    remaining = remaining,
                    isCompleted = isCompleted,
                    onClick = { onNumberClick(digit) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
    isActive: Boolean = false,
    tint: Color = ObsidianPrimary
) {
    val contentColor = when {
        !enabled -> Color.DarkGray
        isActive -> ObsidianSecondary
        else -> tint
    }

    val bgColor = if (isActive) ObsidianSecondary.copy(alpha = 0.2f) else ObsidianCard

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, if (isActive) ObsidianSecondary else ObsidianBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (enabled) Color.White else Color.DarkGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun NumpadButton(
    digit: Int,
    remaining: Int,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCompleted) ObsidianCard.copy(alpha = 0.4f) else ObsidianSurfaceElevated)
            .border(
                1.dp,
                if (isCompleted) Color.Transparent else ObsidianBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = !isCompleted) { onClick() }
            .testTag("numpad_$digit"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit.toString(),
                color = if (isCompleted) Color.DarkGray else Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isCompleted) "✓" else "$remaining",
                color = if (isCompleted) Color.DarkGray else CellNoteText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
