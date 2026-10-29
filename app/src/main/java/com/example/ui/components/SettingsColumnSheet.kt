package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import com.example.ui.theme.LocalActiveTheme
import com.example.ui.theme.LocalThemePalette
import com.example.ui.theme.ThemeStateHolder
import com.example.ui.viewmodel.SocialViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

const val PAPERFLOW_APK_URL = "https://github.com/aistudio/obsidian-sudoku/releases/latest/download/obsidian-sudoku-debug.apk"
const val PAPERFLOW_WEBSITE_URL = "https://paperflow.dev/apps/obsidian-sudoku"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsColumnSheet(
    socialViewModel: SocialViewModel,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = LocalThemePalette.current
    val activeTheme = LocalActiveTheme.current
    val socialUiState by socialViewModel.uiState.collectAsState()

    var usernameInput by remember(socialUiState.publicUsername) {
        mutableStateOf(socialUiState.publicUsername)
    }
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    var usernameSavedSuccess by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        contentColor = palette.cellGivenText,
        modifier = Modifier.testTag("settings_column_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .testTag("secluded_settings_column"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Settings & Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.cellGivenText
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.testTag("close_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Settings",
                        tint = palette.cellNoteText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Profile Card
            val user = Firebase.auth.currentUser
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_profile_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = palette.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceElevated)
                            .border(1.5.dp, palette.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (user != null) (user.displayName ?: "Google Solver") else "Offline Player",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = palette.cellGivenText
                        )
                        Text(
                            text = user?.email ?: "Playing with Local Persistence",
                            fontSize = 12.sp,
                            color = palette.cellNoteText
                        )
                    }

                    // Cloud synced badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (user != null) palette.secondary.copy(alpha = 0.2f) else palette.surfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (user != null) "Cloud Sync" else "Local",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (user != null) palette.secondary else palette.cellNoteText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Public Username Setting
            SectionHeader(
                icon = Icons.Default.Public,
                title = "Public Username",
                palette = palette
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Choose your public handle shown to all players on the realtime leaderboard.",
                fontSize = 13.sp,
                color = palette.cellNoteText,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = {
                        if (it.length <= 20) {
                            usernameInput = it
                            usernameSavedSuccess = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("public_username_input"),
                    singleLine = true,
                    placeholder = { Text("Enter username...", color = palette.cellNoteText) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primary,
                        unfocusedBorderColor = palette.border,
                        focusedTextColor = palette.cellGivenText,
                        unfocusedTextColor = palette.cellGivenText
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        socialViewModel.updatePublicUsername(usernameInput)
                        usernameSavedSuccess = true
                        Toast.makeText(context, "Username updated to $usernameInput", Toast.LENGTH_SHORT).show()
                    },
                    enabled = usernameInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("save_username_button")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }

            if (usernameSavedSuccess) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = palette.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Username synced with realtime leaderboard!",
                        fontSize = 12.sp,
                        color = palette.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Color Themes Setting
            SectionHeader(
                icon = Icons.Default.Palette,
                title = "Color Themes",
                palette = palette
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Select a custom palette to dynamically style boards, cells, and obsidian lighting.",
                fontSize = 13.sp,
                color = palette.cellNoteText,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppTheme.entries.forEach { theme ->
                    val isSelected = (theme == activeTheme)
                    ThemeSelectionCard(
                        theme = theme,
                        isSelected = isSelected,
                        onClick = {
                            ThemeStateHolder.setTheme(context, theme)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Streak Progression Info
            SectionHeader(
                icon = Icons.Default.LocalFireDepartment,
                title = "Streak Multiplier",
                palette = palette
            )
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("streak_info_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = palette.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "🔥 ${socialUiState.currentStreak}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.secondary
                    )
                    Column {
                        Text(
                            text = "Daily Puzzle Streak",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = palette.cellGivenText
                        )
                        Text(
                            text = "Winning ANY puzzle (Easy, Medium, Hard, Expert, or Daily) increases your daily streak and unlocks achievement stickers!",
                            fontSize = 12.sp,
                            color = palette.cellNoteText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Paperflow & Game APK Distribution
            SectionHeader(
                icon = Icons.Default.Download,
                title = "Paperflow Game APK Link",
                palette = palette
            )
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("paperflow_apk_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = palette.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Official Release Artifact on Paperflow",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = palette.cellGivenText
                    )
                    Text(
                        text = "Download the latest compiled release APK directly or visit the Paperflow developer release hub for updates.",
                        fontSize = 12.sp,
                        color = palette.cellNoteText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Game APK Link", PAPERFLOW_APK_URL)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "APK link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_apk_link_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy APK Link", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PAPERFLOW_WEBSITE_URL))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(PAPERFLOW_APK_URL))
                                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(fallback)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_paperflow_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paperflow Web", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
            HorizontalDivider(color = palette.border)
            Spacer(modifier = Modifier.height(20.dp))

            // 5. Secluded Sign Out Button in Settings Column
            OutlinedButton(
                onClick = { showSignOutConfirmation = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = palette.cellErrorText
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cellErrorText.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = "Sign Out",
                    tint = palette.cellErrorText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out of Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = palette.cellErrorText
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmation = false },
            title = {
                Text(
                    text = "Sign Out?",
                    fontWeight = FontWeight.Bold,
                    color = palette.cellGivenText
                )
            },
            text = {
                Text(
                    text = "Your active Sudoku sessions, leaderboard ranking, and unlocked stickers will remain safely synced in the cloud. You can sign back in anytime.",
                    color = palette.cellNoteText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirmation = false
                        onDismiss()
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.cellErrorText),
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutConfirmation = false }
                ) {
                    Text("Cancel", color = palette.cellNoteText)
                }
            },
            containerColor = palette.surfaceElevated
        )
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    palette: com.example.ui.theme.ThemePalette
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.cellGivenText
        )
    }
}

@Composable
private fun ThemeSelectionCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalThemePalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) palette.primary.copy(alpha = 0.15f) else palette.card)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) palette.primary else palette.border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("theme_option_${theme.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Theme preview color swatches
            Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(theme.primaryPreviewColor)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(theme.accentPreviewColor)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                )
            }

            Column {
                Text(
                    text = theme.displayName,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isSelected) palette.cellGivenText else palette.cellGivenText.copy(alpha = 0.85f)
                )
                Text(
                    text = theme.description,
                    fontSize = 11.sp,
                    color = palette.cellNoteText
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(palette.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
