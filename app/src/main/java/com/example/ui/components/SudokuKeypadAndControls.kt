package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SudokuActionBar(
    isNotesMode: Boolean,
    onToggleNotesMode: () -> Unit,
    onUndoClick: () -> Unit,
    onEraseClick: () -> Unit,
    onHintClick: () -> Unit,
    canUndo: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(
            icon = Icons.AutoMirrored.Filled.Undo,
            label = "Undo",
            isActive = false,
            isEnabled = canUndo,
            onClick = onUndoClick,
            testTag = "action_undo"
        )

        ActionButton(
            icon = Icons.Default.Backspace,
            label = "Erase",
            isActive = false,
            isEnabled = true,
            onClick = onEraseClick,
            testTag = "action_erase"
        )

        ActionButton(
            icon = Icons.Default.Edit,
            label = if (isNotesMode) "Notes ON" else "Notes",
            isActive = isNotesMode,
            activeColor = NeonCyan,
            isEnabled = true,
            onClick = onToggleNotesMode,
            testTag = "action_notes"
        )

        ActionButton(
            icon = Icons.Default.Lightbulb,
            label = "Hint",
            isActive = false,
            activeColor = NeonAmber,
            isEnabled = true,
            onClick = onHintClick,
            testTag = "action_hint"
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isEnabled: Boolean,
    activeColor: Color = NeonCyan,
    onClick: () -> Unit,
    testTag: String
) {
    val tintColor = when {
        !isEnabled -> TextMuted.copy(alpha = 0.4f)
        isActive -> activeColor
        else -> TextSecondary
    }

    val bgColor = if (isActive) activeColor.copy(alpha = 0.15f) else ObsidianCard

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                1.dp,
                if (isActive) activeColor.copy(alpha = 0.5f) else ObsidianBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(
                enabled = isEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = activeColor),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = tintColor
        )
    }
}

@Composable
fun SudokuKeypad(
    numberCounts: Map<Int, Int>,
    onNumberClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (digit in 1..9) {
            val remaining = numberCounts[digit] ?: 9
            val isCompleted = remaining <= 0

            KeypadDigitButton(
                digit = digit,
                remaining = remaining,
                isCompleted = isCompleted,
                onClick = { onNumberClick(digit) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KeypadDigitButton(
    digit: Int,
    remaining: Int,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isCompleted) Color(0xFF101520) else ObsidianCard
    val textColor = if (isCompleted) TextMuted.copy(alpha = 0.4f) else TextPrimary
    val borderColor = if (isCompleted) ObsidianBorder.copy(alpha = 0.3f) else ObsidianBorder

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(
                enabled = !isCompleted,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = NeonCyan),
                onClick = onClick
            )
            .padding(vertical = 10.dp)
            .testTag("keypad_digit_$digit"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = digit.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = if (isCompleted) "✓" else remaining.toString(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            color = if (isCompleted) NeonEmerald else TextMuted
        )
    }
}
