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
interface GameRecordDao {
    @Query("SELECT * FROM active_games WHERE id = 1 LIMIT 1")
    fun getActiveGame(): Flow<GameRecordEntity?>

    @Query("SELECT * FROM active_games WHERE id = 1 LIMIT 1")
    suspend fun getActiveGameSync(): GameRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGame(game: GameRecordEntity)

    @Query("DELETE FROM active_games WHERE id = 1")
    suspend fun clearActiveGame()
}

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak_record WHERE id = 1 LIMIT 1")
    fun getStreakRecord(): Flow<StreakRecordEntity?>

    @Query("SELECT * FROM streak_record WHERE id = 1 LIMIT 1")
    suspend fun getStreakRecordSync(): StreakRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(record: StreakRecordEntity)
}

@Dao
interface StickerDao {
    @Query("SELECT * FROM stickers ORDER BY rarity DESC, id ASC")
    fun getAllStickers(): Flow<List<StickerEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStickers(stickers: List<StickerEntity>)

    @Query("UPDATE stickers SET isUnlocked = 1, unlockedAt = :timestamp WHERE id = :stickerId")
    suspend fun unlockSticker(stickerId: String, timestamp: Long)

    @Query("SELECT * FROM stickers WHERE isUnlocked = 1")
    suspend fun getUnlockedStickersSync(): List<StickerEntity>
}

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY currentStreak DESC, name ASC")
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
    @Query("SELECT * FROM daily_leaderboard WHERE dateIso = :date ORDER BY elapsedSeconds ASC, mistakes ASC")
    fun getLeaderboardForDate(date: String): Flow<List<DailyLeaderboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntries(entries: List<DailyLeaderboardEntity>)
}
