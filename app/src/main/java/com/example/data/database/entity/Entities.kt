package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_games")
data class GameRecordEntity(
    @PrimaryKey val id: Int = 1,
    val difficulty: String,
    val isDaily: Boolean,
    val dailyDate: String,
    val initialBoard: String,
    val currentBoard: String,
    val solutionBoard: String,
    val notesBoard: String,
    val elapsedSeconds: Long,
    val isPaused: Boolean,
    val mistakes: Int,
    val hintsUsed: Int,
    val isCompleted: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "streak_record")
data class StreakRecordEntity(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val lastCompletedDate: String = "",
    val completedDailyDates: String = "",
    val totalGamesWon: Int = 0,
    val totalTimePlayedSeconds: Long = 0L,
    val featuredStickerId: String = "novice_spark"
)

@Entity(tableName = "stickers")
data class StickerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val rarity: String, // COMMON, RARE, EPIC, LEGENDARY
    val unlockCriteria: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarColor: Long,
    val currentStreak: Int,
    val friendCode: String,
    val lastActiveIso: String,
    val kudosCount: Int = 0
)

@Entity(tableName = "daily_leaderboard")
data class DailyLeaderboardEntity(
    @PrimaryKey val id: String,
    val dateIso: String,
    val playerName: String,
    val elapsedSeconds: Long,
    val mistakes: Int,
    val rank: Int,
    val isUser: Boolean = false
)
