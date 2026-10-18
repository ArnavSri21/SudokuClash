package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey val id: Int = 1,
    val difficulty: String,
    val timerSeconds: Long,
    val mistakes: Int,
    val isPaused: Boolean,
    val isDaily: Boolean,
    val boardData: String,
    val solutionData: String,
    val notesData: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_challenges")
data class DailyChallengeEntity(
    @PrimaryKey val dateString: String,
    val difficulty: String,
    val completed: Boolean,
    val timeSpentSeconds: Long,
    val mistakes: Int,
    val starsAwarded: Int,
    val completedAt: Long? = null
)

@Entity(tableName = "stickers")
data class StickerEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val category: String,
    val unlocked: Boolean,
    val unlockedAt: Long? = null
)

@Entity(tableName = "game_stats")
data class GameStatsEntity(
    @PrimaryKey val difficulty: String,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val bestTimeSeconds: Long = 0L,
    val totalTimeSeconds: Long = 0L,
    val winStreak: Int = 0,
    val perfectGames: Int = 0
)
