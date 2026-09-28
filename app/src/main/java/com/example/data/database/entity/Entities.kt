package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val difficulty: String,
    val isDaily: Boolean,
    val dailyDate: String?,
    val initialBoard: String, // 81 digits
    val currentBoard: String, // 81 digits
    val solutionBoard: String, // 81 digits
    val notesBoard: String, // Serialized cell notes
    val elapsedSeconds: Long, // Persistent session timer duration
    val isPaused: Boolean = false,
    val mistakes: Int,
    val hintsUsed: Int,
    val isCompleted: Boolean,
    val completedTimestamp: Long? = null
)

@Entity(tableName = "streaks")
data class StreakRecordEntity(
    @PrimaryKey
    val id: Int = 1,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastPlayedDate: String = "",
    val totalGamesWon: Int = 0,
    val perfectGamesWon: Int = 0,
    val dailyChallengesCompleted: Int = 0,
    val completedDailyDates: String = ""
)

@Entity(tableName = "stickers")
data class StickerEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val subtitle: String,
    val lore: String,
    val iconType: String,
    val accentColorHex: String,
    val requirementType: String,
    val requirementThreshold: Int,
    val isUnlocked: Boolean = false,
    val unlockedDate: String? = null,
    val isFeatured: Boolean = false
)

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val friendCode: String,
    val avatarColorHex: String,
    val currentStreak: Int,
    val dailyTimeSeconds: Long? = null,
    val featuredStickerId: String = "spark_1",
    val kudosReceived: Int = 0,
    val kudosSentToday: Boolean = false,
    val statusMessage: String = "Solving the daily grid!"
)

@Entity(tableName = "daily_leaderboard")
data class DailyLeaderboardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateString: String,
    val rank: Int,
    val playerName: String,
    val isCurrentUser: Boolean = false,
    val timeSeconds: Long,
    val mistakes: Int,
    val accuracyPercent: Int,
    val featuredStickerId: String
)
