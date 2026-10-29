package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.local.SudokuDatabase
import com.example.data.repository.SudokuRepository
import com.example.logic.Difficulty
import com.example.ui.components.SettingsColumnSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DailyScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.PlayScreen
import com.example.ui.screens.StickersScreen
import com.example.ui.screens.signOut
import com.example.ui.theme.LocalThemePalette
import com.example.ui.viewmodel.DailyViewModel
import com.example.ui.viewmodel.SocialViewModel
import com.example.ui.viewmodel.StickersViewModel
import com.example.ui.viewmodel.SudokuViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class ScreenTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    PLAY("Play", Icons.Filled.GridOn, Icons.Outlined.GridOn, "nav_tab_play"),
    DAILY("Daily", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday, "nav_tab_daily"),
    STICKERS("Stickers", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_tab_stickers"),
    LEADERBOARD("Ranks", Icons.Filled.Leaderboard, Icons.Outlined.Leaderboard, "nav_tab_leaderboard")
}

@Composable
fun AppNavigation() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isOfflineGuest by rememberSaveable {
        mutableStateOf(Firebase.auth.currentUser != null)
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isOfflineGuest = true
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null && !isOfflineGuest) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                isOfflineGuest = true
            },
            onContinueOffline = {
                isOfflineGuest = true
            }
        )
    } else {
        AuthenticatedAppContent(
            currentUserId = user?.uid ?: "offline_local_player",
            onSignedOut = {
                currentUser = null
                isOfflineGuest = false
            }
        )
    }
}

@Composable
fun AuthenticatedAppContent(
    currentUserId: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val sudokuViewModel: SudokuViewModel = viewModel(
        key = "sudoku_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val dao = SudokuDatabase.getDatabase(app).sudokuDao()
                SudokuViewModel(SudokuRepository(db, dao, app))
            }
        }
    )

    val dailyViewModel: DailyViewModel = viewModel(
        key = "daily_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val dao = SudokuDatabase.getDatabase(app).sudokuDao()
                DailyViewModel(SudokuRepository(db, dao, app))
            }
        }
    )

    val stickersViewModel: StickersViewModel = viewModel(
        key = "stickers_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val dao = SudokuDatabase.getDatabase(app).sudokuDao()
                StickersViewModel(SudokuRepository(db, dao, app))
            }
        }
    )

    val socialViewModel: SocialViewModel = viewModel(
        key = "social_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val dao = SudokuDatabase.getDatabase(app).sudokuDao()
                SocialViewModel(SudokuRepository(db, dao, app))
            }
        }
    )

    MainScreen(
        sudokuViewModel = sudokuViewModel,
        dailyViewModel = dailyViewModel,
        stickersViewModel = stickersViewModel,
        socialViewModel = socialViewModel,
        onSignOut = {
            signOut(
                context = context,
                credentialManager = credentialManager,
                onSignOutComplete = onSignedOut,
                scope = scope
            )
        }
    )
}

@Composable
fun MainScreen(
    sudokuViewModel: SudokuViewModel,
    dailyViewModel: DailyViewModel,
    stickersViewModel: StickersViewModel,
    socialViewModel: SocialViewModel,
    onSignOut: () -> Unit = {}
) {
    var currentTab by remember { mutableStateOf(ScreenTab.PLAY) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val palette = LocalThemePalette.current
    val socialUiState by socialViewModel.uiState.collectAsState()

    BackHandler(enabled = currentTab != ScreenTab.PLAY) {
        currentTab = ScreenTab.PLAY
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = palette.surface,
                    contentColor = palette.cellGivenText,
                    modifier = Modifier.testTag("navigation_rail")
                ) {
                    ScreenTab.entries.forEach { tab ->
                        val isSelected = (currentTab == tab)
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = palette.primary,
                                selectedTextColor = palette.primary,
                                unselectedIconColor = palette.cellNoteText,
                                unselectedTextColor = palette.cellNoteText,
                                indicatorColor = palette.surfaceElevated
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = palette.cellNoteText
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                ) {
                    TopHeaderBar(
                        tab = currentTab,
                        streak = socialUiState.currentStreak,
                        onOpenSettings = { showSettingsSheet = true },
                        palette = palette
                    )
                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        TabContent(
                            tab = currentTab,
                            sudokuViewModel = sudokuViewModel,
                            dailyViewModel = dailyViewModel,
                            stickersViewModel = stickersViewModel,
                            socialViewModel = socialViewModel,
                            onNavigateToPlay = { currentTab = ScreenTab.PLAY }
                        )
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = palette.background,
                topBar = {
                    TopHeaderBar(
                        tab = currentTab,
                        streak = socialUiState.currentStreak,
                        onOpenSettings = { showSettingsSheet = true },
                        palette = palette,
                        modifier = Modifier.padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = palette.surface,
                        contentColor = palette.cellGivenText,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        ScreenTab.entries.forEach { tab ->
                            val isSelected = (currentTab == tab)
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = { Text(tab.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = palette.primary,
                                    selectedTextColor = palette.primary,
                                    unselectedIconColor = palette.cellNoteText,
                                    unselectedTextColor = palette.cellNoteText,
                                    indicatorColor = palette.surfaceElevated
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    TabContent(
                        tab = currentTab,
                        sudokuViewModel = sudokuViewModel,
                        dailyViewModel = dailyViewModel,
                        stickersViewModel = stickersViewModel,
                        socialViewModel = socialViewModel,
                        onNavigateToPlay = { currentTab = ScreenTab.PLAY }
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        SettingsColumnSheet(
            socialViewModel = socialViewModel,
            onDismiss = { showSettingsSheet = false },
            onSignOut = onSignOut
        )
    }
}

@Composable
fun TopHeaderBar(
    tab: ScreenTab,
    streak: Int,
    onOpenSettings: () -> Unit,
    palette: com.example.ui.theme.ThemePalette,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Obsidian Sudoku",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = palette.cellGivenText
            )

            // Live streak badge: shows streak for solving ANY puzzle!
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.secondary.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("header_streak_badge")
            ) {
                Text(
                    text = "🔥 $streak",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.secondary
                )
            }
        }

        // Secluded Settings in top corner
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.card)
                .testTag("settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = palette.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun TabContent(
    tab: ScreenTab,
    sudokuViewModel: SudokuViewModel,
    dailyViewModel: DailyViewModel,
    stickersViewModel: StickersViewModel,
    socialViewModel: SocialViewModel,
    onNavigateToPlay: () -> Unit
) {
    when (tab) {
        ScreenTab.PLAY -> PlayScreen(viewModel = sudokuViewModel)
        ScreenTab.DAILY -> DailyScreen(
            viewModel = dailyViewModel,
            onStartDailyGame = {
                sudokuViewModel.startNewGame(Difficulty.DAILY, isDaily = true)
                onNavigateToPlay()
            }
        )
        ScreenTab.STICKERS -> StickersScreen(viewModel = stickersViewModel)
        ScreenTab.LEADERBOARD -> LeaderboardScreen(viewModel = socialViewModel)
    }
}
