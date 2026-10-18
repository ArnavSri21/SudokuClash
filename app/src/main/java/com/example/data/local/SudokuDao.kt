package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyChallengeEntity
import com.example.data.model.GameSessionEntity
import com.example.data.model.GameStatsEntity
import com.example.data.model.StickerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SudokuDao {

    // Active session
    @Query("SELECT * FROM game_sessions WHERE id = 1 LIMIT 1")
    fun getActiveSession(): Flow<GameSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveSession(session: GameSessionEntity)

    @Query("DELETE FROM game_sessions WHERE id = 1")
    suspend fun clearActiveSession()

    // Daily challenges
    @Query("SELECT * FROM daily_challenges ORDER BY dateString DESC")
    fun getAllDailyChallenges(): Flow<List<DailyChallengeEntity>>

    @Query("SELECT * FROM daily_challenges WHERE dateString = :date LIMIT 1")
    suspend fun getDailyChallenge(date: String): DailyChallengeEntity?

    @Query("SELECT * FROM daily_challenges WHERE dateString = :date LIMIT 1")
    fun observeDailyChallenge(date: String): Flow<DailyChallengeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDailyChallenge(challenge: DailyChallengeEntity)

    // Stickers
    @Query("SELECT * FROM stickers ORDER BY unlocked DESC, id ASC")
    fun getAllStickers(): Flow<List<StickerEntity>>

    @Query("SELECT COUNT(*) FROM stickers WHERE unlocked = 1")
    fun getUnlockedStickersCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStickers(stickers: List<StickerEntity>)

    @Update
    suspend fun updateSticker(sticker: StickerEntity)

    @Query("UPDATE stickers SET unlocked = 1, unlockedAt = :timestamp WHERE id = :id AND unlocked = 0")
    suspend fun unlockSticker(id: String, timestamp: Long = System.currentTimeMillis()): Int

    // Stats
    @Query("SELECT * FROM game_stats WHERE difficulty = :difficulty LIMIT 1")
    suspend fun getStats(difficulty: String): GameStatsEntity?

    @Query("SELECT * FROM game_stats")
    fun getAllStats(): Flow<List<GameStatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStats(stats: GameStatsEntity)
}
