package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.FriendDao
import com.example.data.database.dao.GameDao
import com.example.data.database.dao.LeaderboardDao
import com.example.data.database.dao.StickerDao
import com.example.data.database.dao.StreakDao
import com.example.data.database.entity.DailyLeaderboardEntity
import com.example.data.database.entity.FriendEntity
import com.example.data.database.entity.GameRecordEntity
import com.example.data.database.entity.StickerEntity
import com.example.data.database.entity.StreakRecordEntity

@Database(
    entities = [
        GameRecordEntity::class,
        StreakRecordEntity::class,
        StickerEntity::class,
        FriendEntity::class,
        DailyLeaderboardEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun streakDao(): StreakDao
    abstract fun stickerDao(): StickerDao
    abstract fun friendDao(): FriendDao
    abstract fun leaderboardDao(): LeaderboardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "obsidian_sudoku.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
