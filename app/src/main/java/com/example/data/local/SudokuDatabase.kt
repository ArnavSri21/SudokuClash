package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DailyChallengeEntity
import com.example.data.model.GameSessionEntity
import com.example.data.model.GameStatsEntity
import com.example.data.model.StickerEntity

@Database(
    entities = [
        GameSessionEntity::class,
        DailyChallengeEntity::class,
        StickerEntity::class,
        GameStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SudokuDatabase : RoomDatabase() {
    abstract fun sudokuDao(): SudokuDao

    companion object {
        @Volatile
        private var INSTANCE: SudokuDatabase? = null

        fun getDatabase(context: Context): SudokuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SudokuDatabase::class.java,
                    "obsidian_sudoku.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
