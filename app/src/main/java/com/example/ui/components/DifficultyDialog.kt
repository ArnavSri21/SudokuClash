package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.Difficulty
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderStrong
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianPrimary
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultyDialog(
    currentDifficulty: Difficulty,
    onSelectDifficulty: (Difficulty) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorderStrong, RoundedCornerShape(20.dp))
            .padding(20.dp)
            .testTag("difficulty_dialog")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Select Difficulty",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Choose your level of challenge",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            val difficulties = listOf(
                Difficulty.EASY,
                Difficulty.MEDIUM,
                Difficulty.HARD,
                Difficulty.EXPERT
            )

            for (diff in difficulties) {
                val isSelected = (diff == currentDifficulty)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ObsidianPrimary.copy(alpha = 0.2f) else ObsidianCard)
                        .border(
                            1.dp,
                            if (isSelected) ObsidianPrimary else ObsidianBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            onSelectDifficulty(diff)
                            onDismiss()
                        }
                        .padding(14.dp)
                        .testTag("difficulty_${diff.name.lowercase()}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = diff.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = diff.description,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (i in 1..diff.stars) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = ObsidianWarning,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
