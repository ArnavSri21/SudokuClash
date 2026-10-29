package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreGameSession(
    @DocumentId val id: String = "active",
    val userId: String = "",
    val difficulty: String = "EASY",
    val timerSeconds: Long = 0L,
    val mistakes: Int = 0,
    @get:PropertyName("isPaused") @set:PropertyName("isPaused")
    var isPaused: Boolean = false,
    @get:PropertyName("isDaily") @set:PropertyName("isDaily")
    var isDaily: Boolean = false,
    val boardData: String = "0:0:0",
    val solutionData: String = "1",
    val notesData: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): GameSessionEntity {
        return GameSessionEntity(
            id = 1,
            difficulty = difficulty,
            timerSeconds = timerSeconds,
            mistakes = mistakes,
            isPaused = isPaused,
            isDaily = isDaily,
            boardData = boardData,
            solutionData = solutionData,
            notesData = notesData,
            lastUpdated = (updatedAt ?: createdAt)?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    fun toCreateMap(uid: String): Map<String, Any> {
        return mapOf(
            "userId" to uid,
            "difficulty" to difficulty,
            "timerSeconds" to timerSeconds,
            "mistakes" to mistakes,
            "isPaused" to isPaused,
            "isDaily" to isDaily,
            "boardData" to boardData,
            "solutionData" to solutionData,
            "notesData" to notesData,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
    }
}

@IgnoreExtraProperties
data class FirestoreDailyChallenge(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateString: String = "",
    val difficulty: String = "DAILY",
    val completed: Boolean = false,
    val timeSpentSeconds: Long = 0L,
    val mistakes: Int = 0,
    val starsAwarded: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): DailyChallengeEntity {
        return DailyChallengeEntity(
            dateString = dateString.ifBlank { id },
            difficulty = difficulty,
            completed = completed,
            timeSpentSeconds = timeSpentSeconds,
            mistakes = mistakes,
            starsAwarded = starsAwarded,
            completedAt = (updatedAt ?: createdAt)?.toDate()?.time
        )
    }
}

@IgnoreExtraProperties
data class FirestoreSticker(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val iconName: String = "",
    val category: String = "",
    val unlocked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): StickerEntity {
        return StickerEntity(
            id = id,
            title = title,
            description = description,
            iconName = iconName,
            category = category,
            unlocked = unlocked,
            unlockedAt = if (unlocked) (updatedAt ?: createdAt)?.toDate()?.time else null
        )
    }
}

@IgnoreExtraProperties
data class FirestoreGameStats(
    @DocumentId val id: String = "",
    val userId: String = "",
    val difficulty: String = "EASY",
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val bestTimeSeconds: Long = 0L,
    val totalTimeSeconds: Long = 0L,
    val winStreak: Int = 0,
    val perfectGames: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): GameStatsEntity {
        return GameStatsEntity(
            difficulty = difficulty.ifBlank { id },
            gamesPlayed = gamesPlayed,
            gamesWon = gamesWon,
            bestTimeSeconds = bestTimeSeconds,
            totalTimeSeconds = totalTimeSeconds,
            winStreak = winStreak,
            perfectGames = perfectGames
        )
    }
}

@IgnoreExtraProperties
data class FirestoreLeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val score: Int = 0,
    val gamesWon: Int = 0,
    val winStreak: Int = 0,
    val bestTimeSeconds: Long = 0L,
    val bestTime: String = "--:--",
    val title: String = "Obsidian Solver",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

