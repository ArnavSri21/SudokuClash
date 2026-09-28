package com.example.data.repository

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
import com.example.data.model.Difficulty
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class SudokuRepository(
    private val gameDao: GameDao,
    private val streakDao: StreakDao,
    private val stickerDao: StickerDao,
    private val friendDao: FriendDao,
    private val leaderboardDao: LeaderboardDao
) {

    val activeGame: Flow<GameRecordEntity?> = gameDao.getActiveGame()
    val streak: Flow<StreakRecordEntity?> = streakDao.getStreak()
    val stickers: Flow<List<StickerEntity>> = stickerDao.getAllStickers()
    val friends: Flow<List<FriendEntity>> = friendDao.getAllFriends()

    fun getDailyLeaderboard(date: String): Flow<List<DailyLeaderboardEntity>> {
        return leaderboardDao.getDailyLeaderboard(date)
    }

    suspend fun getActiveGameSync(): GameRecordEntity? {
        return gameDao.getActiveGameSync()
    }

    suspend fun saveActiveGame(game: GameRecordEntity): Long {
        return gameDao.insertGame(game)
    }

    suspend fun updateTimerState(elapsedSeconds: Long, isPaused: Boolean) {
        gameDao.updateTimerState(elapsedSeconds, isPaused)
    }

    suspend fun clearActiveGame() {
        gameDao.clearActiveGames()
    }

    suspend fun initializeDefaultsIfNeeded(todayDate: String) {
        val currentStreak = streakDao.getStreakSync()
        if (currentStreak == null) {
            streakDao.insertOrUpdateStreak(
                StreakRecordEntity(
                    id = 1,
                    currentStreak = 0,
                    bestStreak = 0,
                    lastPlayedDate = "",
                    totalGamesWon = 0,
                    perfectGamesWon = 0,
                    dailyChallengesCompleted = 0,
                    completedDailyDates = ""
                )
            )
        }

        val initialStickers = listOf(
            StickerEntity(
                id = "spark_1",
                title = "First Spark",
                subtitle = "Day 1 Streak",
                lore = "The journey of a thousand grids begins with a single solved cell. Your discipline is ignited.",
                iconType = "SPARK",
                accentColorHex = "#00E5FF",
                requirementType = "STREAK",
                requirementThreshold = 1,
                isUnlocked = false,
                isFeatured = true
            ),
            StickerEntity(
                id = "nexus_3",
                title = "Tri-Force Nexus",
                subtitle = "Day 3 Streak",
                lore = "Three days of laser focus. The cognitive momentum has locked in.",
                iconType = "TRIANGLE",
                accentColorHex = "#76FF03",
                requirementType = "STREAK",
                requirementThreshold = 3,
                isUnlocked = false
            ),
            StickerEntity(
                id = "flow_5",
                title = "Flow State",
                subtitle = "Day 5 Streak",
                lore = "Calm under pressure. Digits arrange themselves before your mind even blinks.",
                iconType = "WAVE",
                accentColorHex = "#00E5FF",
                requirementType = "STREAK",
                requirementThreshold = 5,
                isUnlocked = false
            ),
            StickerEntity(
                id = "flare_7",
                title = "Seven Flare",
                subtitle = "Day 7 Streak",
                lore = "One entire week unbroken! A golden blaze of consistency and mental clarity.",
                iconType = "FLAME",
                accentColorHex = "#FFB300",
                requirementType = "STREAK",
                requirementThreshold = 7,
                isUnlocked = false
            ),
            StickerEntity(
                id = "shield_14",
                title = "Obsidian Shield",
                subtitle = "Day 14 Streak",
                lore = "Two solid weeks. Your focus is forged in pure obsidian titanium.",
                iconType = "SHIELD",
                accentColorHex = "#B388FF",
                requirementType = "STREAK",
                requirementThreshold = 14,
                isUnlocked = false
            ),
            StickerEntity(
                id = "habit_21",
                title = "Habit Forged",
                subtitle = "Day 21 Streak",
                lore = "Scientific consensus: 21 days form a permanent habit. Sudoku is now in your DNA.",
                iconType = "NEURAL",
                accentColorHex = "#00E676",
                requirementType = "STREAK",
                requirementThreshold = 21,
                isUnlocked = false
            ),
            StickerEntity(
                id = "lunar_30",
                title = "Lunar Master",
                subtitle = "Day 30 Streak",
                lore = "A full lunar revolution without missing a single day. A master among solvers.",
                iconType = "MOON",
                accentColorHex = "#E040FB",
                requirementType = "STREAK",
                requirementThreshold = 30,
                isUnlocked = false
            ),
            StickerEntity(
                id = "apex_50",
                title = "Apex Prism",
                subtitle = "Day 50 Streak",
                lore = "Half a hundred consecutive days. Shining radiant at the top of the pyramid.",
                iconType = "PRISM",
                accentColorHex = "#FF5252",
                requirementType = "STREAK",
                requirementThreshold = 50,
                isUnlocked = false
            ),
            StickerEntity(
                id = "legend_100",
                title = "Sudoku Legend",
                subtitle = "Day 100 Streak",
                lore = "The mythic century. A monument of timeless intellect and unmatched discipline.",
                iconType = "CROWN",
                accentColorHex = "#FFD700",
                requirementType = "STREAK",
                requirementThreshold = 100,
                isUnlocked = false
            ),
            StickerEntity(
                id = "flawless_win",
                title = "Flawless Mind",
                subtitle = "Zero Mistakes",
                lore = "Completed an entire grid without making a single error. Pure perfection.",
                iconType = "DIAMOND",
                accentColorHex = "#00E5FF",
                requirementType = "MISTAKE",
                requirementThreshold = 0,
                isUnlocked = false
            ),
            StickerEntity(
                id = "speed_demon",
                title = "Speed Demon",
                subtitle = "Under 4 Minutes",
                lore = "Lightning swift deduction. Solved in under 240 seconds.",
                iconType = "LIGHTNING",
                accentColorHex = "#FF6D00",
                requirementType = "TIME",
                requirementThreshold = 240,
                isUnlocked = false
            ),
            StickerEntity(
                id = "expert_architect",
                title = "Grand Architect",
                subtitle = "Conquer Expert",
                lore = "Defeated an Expert tier puzzle. You see through complex candidates like glass.",
                iconType = "CUBE",
                accentColorHex = "#B388FF",
                requirementType = "DIFFICULTY",
                requirementThreshold = 4,
                isUnlocked = false
            ),
            StickerEntity(
                id = "daily_champion",
                title = "Daily Contender",
                subtitle = "3 Daily Challenges",
                lore = "Completed 3 Daily Challenges and etched your name on the competitive leaderboards.",
                iconType = "TROPHY",
                accentColorHex = "#FFB300",
                requirementType = "DAILY",
                requirementThreshold = 3,
                isUnlocked = false
            )
        )
        stickerDao.insertStickers(initialStickers)

        val seedFriends = listOf(
            FriendEntity(
                id = "friend_1",
                name = "Elena Vance",
                friendCode = "SUDOKU-4192",
                avatarColorHex = "#00E5FF",
                currentStreak = 12,
                dailyTimeSeconds = 238,
                featuredStickerId = "flare_7",
                kudosReceived = 24,
                statusMessage = "Chasing that Day 14 Shield! 🛡️"
            ),
            FriendEntity(
                id = "friend_2",
                name = "Cipher Kai",
                friendCode = "SUDOKU-8831",
                avatarColorHex = "#FFB300",
                currentStreak = 6,
                dailyTimeSeconds = 312,
                featuredStickerId = "flow_5",
                kudosReceived = 15,
                statusMessage = "Notes mode is my superpower ⚡"
            ),
            FriendEntity(
                id = "friend_3",
                name = "Aria Hex",
                friendCode = "SUDOKU-1094",
                avatarColorHex = "#E040FB",
                currentStreak = 18,
                dailyTimeSeconds = 195,
                featuredStickerId = "shield_14",
                kudosReceived = 42,
                statusMessage = "Top 3 daily leaderboard today! 🔥"
            ),
            FriendEntity(
                id = "friend_4",
                name = "Zenith Neo",
                friendCode = "SUDOKU-6720",
                avatarColorHex = "#00E676",
                currentStreak = 29,
                dailyTimeSeconds = 265,
                featuredStickerId = "habit_21",
                kudosReceived = 68,
                statusMessage = "One day away from Lunar Master! 🌙"
            )
        )
        friendDao.insertFriends(seedFriends)

        val defaultLeaderboard = listOf(
            DailyLeaderboardEntity(
                dateString = todayDate,
                rank = 1,
                playerName = "Aria Hex",
                isCurrentUser = false,
                timeSeconds = 195,
                mistakes = 0,
                accuracyPercent = 100,
                featuredStickerId = "shield_14"
            ),
            DailyLeaderboardEntity(
                dateString = todayDate,
                rank = 2,
                playerName = "Elena Vance",
                isCurrentUser = false,
                timeSeconds = 238,
                mistakes = 1,
                accuracyPercent = 98,
                featuredStickerId = "flare_7"
            ),
            DailyLeaderboardEntity(
                dateString = todayDate,
                rank = 3,
                playerName = "Zenith Neo",
                isCurrentUser = false,
                timeSeconds = 265,
                mistakes = 0,
                accuracyPercent = 100,
                featuredStickerId = "habit_21"
            ),
            DailyLeaderboardEntity(
                dateString = todayDate,
                rank = 4,
                playerName = "Cipher Kai",
                isCurrentUser = false,
                timeSeconds = 312,
                mistakes = 2,
                accuracyPercent = 95,
                featuredStickerId = "flow_5"
            ),
            DailyLeaderboardEntity(
                dateString = todayDate,
                rank = 5,
                playerName = "PixelGrid_9",
                isCurrentUser = false,
                timeSeconds = 354,
                mistakes = 1,
                accuracyPercent = 97,
                featuredStickerId = "nexus_3"
            )
        )
        leaderboardDao.insertEntries(defaultLeaderboard)
    }

    suspend fun recordGameWin(
        difficulty: Difficulty,
        elapsedSeconds: Long,
        mistakes: Int,
        hintsUsed: Int,
        isDaily: Boolean,
        todayDateString: String,
        playerName: String
    ): List<StickerEntity> {
        val currentStreakRec = streakDao.getStreakSync() ?: StreakRecordEntity()
        val lastDateStr = currentStreakRec.lastPlayedDate

        var newStreak = currentStreakRec.currentStreak
        val today = try {
            LocalDate.parse(todayDateString, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            LocalDate.now()
        }

        if (lastDateStr.isEmpty()) {
            newStreak = 1
        } else {
            try {
                val lastDate = LocalDate.parse(lastDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                val diffDays = ChronoUnit.DAYS.between(lastDate, today)
                when {
                    diffDays == 0L -> { /* Already played today */ }
                    diffDays == 1L -> { newStreak += 1 }
                    else -> { newStreak = 1 }
                }
            } catch (e: Exception) {
                newStreak = 1
            }
        }

        val newBest = maxOf(newStreak, currentStreakRec.bestStreak)
        val newTotalWon = currentStreakRec.totalGamesWon + 1
        val newPerfectWon = if (mistakes == 0) currentStreakRec.perfectGamesWon + 1 else currentStreakRec.perfectGamesWon

        var newDailyCompleted = currentStreakRec.dailyChallengesCompleted
        var newDailyDates = currentStreakRec.completedDailyDates
        if (isDaily && !newDailyDates.contains(todayDateString)) {
            newDailyCompleted += 1
            newDailyDates = if (newDailyDates.isEmpty()) todayDateString else "$newDailyDates,$todayDateString"
        }

        val updatedStreakRec = currentStreakRec.copy(
            currentStreak = newStreak,
            bestStreak = newBest,
            lastPlayedDate = todayDateString,
            totalGamesWon = newTotalWon,
            perfectGamesWon = newPerfectWon,
            dailyChallengesCompleted = newDailyCompleted,
            completedDailyDates = newDailyDates
        )
        streakDao.insertOrUpdateStreak(updatedStreakRec)

        val newlyUnlocked = mutableListOf<StickerEntity>()
        val streakStickerIds = mapOf(
            1 to "spark_1",
            3 to "nexus_3",
            5 to "flow_5",
            7 to "flare_7",
            14 to "shield_14",
            21 to "habit_21",
            30 to "lunar_30",
            50 to "apex_50",
            100 to "legend_100"
        )
        for ((threshold, stickerId) in streakStickerIds) {
            if (newStreak >= threshold) {
                if (stickerDao.unlockSticker(stickerId, todayDateString) > 0) {
                    newlyUnlocked.add(StickerEntity(stickerId, "", "", "", "", "", "STREAK", threshold))
                }
            }
        }

        if (mistakes == 0 && stickerDao.unlockSticker("flawless_win", todayDateString) > 0) {
            newlyUnlocked.add(StickerEntity("flawless_win", "", "", "", "", "", "MISTAKE", 0))
        }

        if (elapsedSeconds <= 240 && stickerDao.unlockSticker("speed_demon", todayDateString) > 0) {
            newlyUnlocked.add(StickerEntity("speed_demon", "", "", "", "", "", "TIME", 240))
        }

        if (difficulty == Difficulty.EXPERT && stickerDao.unlockSticker("expert_architect", todayDateString) > 0) {
            newlyUnlocked.add(StickerEntity("expert_architect", "", "", "", "", "", "DIFFICULTY", 4))
        }

        if (newDailyCompleted >= 3 && stickerDao.unlockSticker("daily_champion", todayDateString) > 0) {
            newlyUnlocked.add(StickerEntity("daily_champion", "", "", "", "", "", "DAILY", 3))
        }

        if (isDaily) {
            val accuracy = maxOf(70, 100 - (mistakes * 5))
            val userEntry = DailyLeaderboardEntity(
                dateString = todayDateString,
                rank = 1,
                playerName = "$playerName (You)",
                isCurrentUser = true,
                timeSeconds = elapsedSeconds,
                mistakes = mistakes,
                accuracyPercent = accuracy,
                featuredStickerId = "spark_1"
            )
            leaderboardDao.insertEntries(listOf(userEntry))
        }

        return newlyUnlocked
    }

    suspend fun setFeaturedSticker(stickerId: String) {
        stickerDao.clearFeatured()
        stickerDao.setFeatured(stickerId)
    }

    suspend fun addFriend(name: String, code: String): Boolean {
        val newFriend = FriendEntity(
            id = "friend_${System.currentTimeMillis()}",
            name = name,
            friendCode = code,
            avatarColorHex = listOf("#00E5FF", "#FFB300", "#E040FB", "#00E676", "#FF5252").random(),
            currentStreak = (1..15).random(),
            dailyTimeSeconds = (180L..420L).random(),
            featuredStickerId = listOf("spark_1", "nexus_3", "flow_5", "flare_7").random(),
            kudosReceived = (5..30).random(),
            kudosSentToday = false,
            statusMessage = "Ready for daily Sudoku challenge! 🔥"
        )
        friendDao.insertFriend(newFriend)
        return true
    }

    suspend fun sendKudosToFriend(friend: FriendEntity) {
        val updated = friend.copy(
            kudosReceived = friend.kudosReceived + 1,
            kudosSentToday = true
        )
        friendDao.updateFriend(updated)
    }
}
