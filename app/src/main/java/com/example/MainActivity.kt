package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.SudokuRepository
import com.example.ui.MainScreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSudokuTheme
import com.example.ui.viewmodel.DailyViewModel
import com.example.ui.viewmodel.SocialViewModel
import com.example.ui.viewmodel.StickersViewModel
import com.example.ui.viewmodel.SudokuViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SudokuRepository(
            gameDao = database.gameDao(),
            streakDao = database.streakDao(),
            stickerDao = database.stickerDao(),
            friendDao = database.friendDao(),
            leaderboardDao = database.leaderboardDao()
        )

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(SudokuViewModel::class.java) ->
                        SudokuViewModel(repository) as T
                    modelClass.isAssignableFrom(DailyViewModel::class.java) ->
                        DailyViewModel(repository) as T
                    modelClass.isAssignableFrom(StickersViewModel::class.java) ->
                        StickersViewModel(repository) as T
                    modelClass.isAssignableFrom(SocialViewModel::class.java) ->
                        SocialViewModel(repository) as T
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }

        val sudokuViewModel = ViewModelProvider(this, factory)[SudokuViewModel::class.java]
        val dailyViewModel = ViewModelProvider(this, factory)[DailyViewModel::class.java]
        val stickersViewModel = ViewModelProvider(this, factory)[StickersViewModel::class.java]
        val socialViewModel = ViewModelProvider(this, factory)[SocialViewModel::class.java]

        setContent {
            ObsidianSudokuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ObsidianBg
                ) {
                    MainScreen(
                        sudokuViewModel = sudokuViewModel,
                        dailyViewModel = dailyViewModel,
                        stickersViewModel = stickersViewModel,
                        socialViewModel = socialViewModel
                    )
                }
            }
        }
    }
}
