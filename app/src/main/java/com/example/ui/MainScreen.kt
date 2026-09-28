package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Difficulty
import com.example.ui.screens.DailyScreen
import com.example.ui.screens.PlayScreen
import com.example.ui.screens.SocialScreen
import com.example.ui.screens.StickersScreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DailyViewModel
import com.example.ui.viewmodel.SocialViewModel
import com.example.ui.viewmodel.StickersViewModel
import com.example.ui.viewmodel.SudokuViewModel

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    PLAY("Play", Icons.Filled.GridOn, Icons.Outlined.GridOn, "nav_tab_play"),
    DAILY("Daily", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_tab_daily"),
    STICKERS("Stickers", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_tab_stickers"),
    SOCIAL("Friends", Icons.Filled.Group, Icons.Outlined.Group, "nav_tab_friends")
}

@Composable
fun MainScreen(
    sudokuViewModel: SudokuViewModel,
    dailyViewModel: DailyViewModel,
    stickersViewModel: StickersViewModel,
    socialViewModel: SocialViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AppTab.PLAY) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .background(ObsidianSurface)
                    .border(0.5.dp, ObsidianBorder)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = ObsidianSurface,
                contentColor = TextSecondary
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ObsidianBg,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = selectedTab,
                label = "TabCrossfade"
            ) { tab ->
                when (tab) {
                    AppTab.PLAY -> PlayScreen(
                        viewModel = sudokuViewModel,
                        onNavigateToStickers = { selectedTab = AppTab.STICKERS }
                    )
                    AppTab.DAILY -> DailyScreen(
                        viewModel = dailyViewModel,
                        onPlayDailyChallenge = {
                            sudokuViewModel.startNewGame(Difficulty.DAILY, isDaily = true)
                            selectedTab = AppTab.PLAY
                        }
                    )
                    AppTab.STICKERS -> StickersScreen(
                        viewModel = stickersViewModel
                    )
                    AppTab.SOCIAL -> SocialScreen(
                        viewModel = socialViewModel,
                        onChallengeFriend = {
                            sudokuViewModel.startNewGame(Difficulty.DAILY, isDaily = true)
                            selectedTab = AppTab.PLAY
                        }
                    )
                }
            }
        }
    }
}
