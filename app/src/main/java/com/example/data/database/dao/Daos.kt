package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.DailyLeaderboardEntity
import com.example.data.database.entity.FriendEntity
import com.example.data.database.entity.GameRecordEntity
import com.example.data.database.entity.StickerEntity
import com.example.data.database.entity.StreakRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE isCompleted = 0 ORDER BY id DESC LIMIT 1")
    fun getActiveGame(): Flow<GameRecordEntity?>

    @Query("SELECT * FROM games WHERE isCompleted = 0 ORDER BY id DESC LIMIT 1")
    suspend fun getActiveGameSync(): GameRecordEntity?

    @Query("SELECT * FROM games WHERE isDaily = 1 AND dailyDate = :date LIMIT 1")
    suspend fun getDailyGame(date: String): GameRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameRecordEntity): Long

    @Update
    suspend fun updateGame(game: GameRecordEntity)

    @Query("UPDATE games SET elapsedSeconds = :seconds, isPaused = :isPaused WHERE isCompleted = 0")
    suspend fun updateTimerState(seconds: Long, isPaused: Boolean)

    @Query("DELETE FROM games WHERE isCompleted = 0")
    suspend fun clearActiveGames()
}

@Dao
interface StreakDao {
    @Query("SELECT * FROM streaks WHERE id = 1")
    fun getStreak(): Flow<StreakRecordEntity?>

    @Query("SELECT * FROM streaks WHERE id = 1")
    suspend fun getStreakSync(): StreakRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: StreakRecordEntity)
}

@Dao
interface StickerDao {
    @Query("SELECT * FROM stickers ORDER BY requirementThreshold ASC")
    fun getAllStickers(): Flow<List<StickerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickers(stickers: List<StickerEntity>)

    @Query("UPDATE stickers SET isFeatured = 0")
    suspend fun clearFeatured()

    @Query("UPDATE stickers SET isFeatured = 1 WHERE id = :stickerId")
    suspend fun setFeatured(stickerId: String)

    @Query("UPDATE stickers SET isUnlocked = 1, unlockedDate = :date WHERE id = :stickerId AND isUnlocked = 0")
    suspend fun unlockSticker(stickerId: String, date: String): Int
}

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY currentStreak DESC")
    fun getAllFriends(): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<FriendEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Update
    suspend fun updateFriend(friend: FriendEntity)
}

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM daily_leaderboard WHERE dateString = :date ORDER BY rank ASC")
    fun getDailyLeaderboard(date: String): Flow<List<DailyLeaderboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<DailyLeaderboardEntity>)
}
