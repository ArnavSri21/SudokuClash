package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.SudokuDao
import com.example.data.model.DailyChallengeEntity
import com.example.data.model.FirestoreDailyChallenge
import com.example.data.model.FirestoreGameSession
import com.example.data.model.FirestoreGameStats
import com.example.data.model.FirestoreLeaderboardEntry
import com.example.data.model.FirestoreSticker
import com.example.data.model.GameSessionEntity
import com.example.data.model.GameStatsEntity
import com.example.data.model.StickerEntity
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.ui.viewmodel.LeaderboardEntry
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SudokuRepository(
    private val db: FirebaseFirestore,
    private val dao: SudokuDao? = null,
    private val context: Context? = null
) {
    constructor(context: Context, dao: SudokuDao? = null) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        dao,
        context.applicationContext
    )

    private val auth
        get() = Firebase.auth

    private fun isLocalOnlySession(targetUserId: String? = null): Boolean {
        return targetUserId == null && auth.currentUser == null && dao != null
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun currentOrTargetUserId(targetUserId: String?): String {
        return targetUserId ?: auth.currentUser?.uid ?: "unauthenticated"
    }

    fun getPublicUsername(): String {
        val prefs = context?.getSharedPreferences("obsidian_sudoku_settings", Context.MODE_PRIVATE)
        val saved = prefs?.getString("key_public_username", null)
        if (!saved.isNullOrBlank()) return saved

        val currentGoogleName = auth.currentUser?.displayName
        if (!currentGoogleName.isNullOrBlank()) {
            val sanitized = currentGoogleName.replace(Regex("[^a-zA-Z0-9_]"), "").take(18)
            if (sanitized.isNotBlank()) return sanitized
        }
        val uidPart = auth.currentUser?.uid?.take(4) ?: "User"
        return "Solver_$uidPart"
    }

    suspend fun setPublicUsername(newUsername: String) {
        val trimmed = newUsername.trim().take(24)
        if (trimmed.isBlank()) return
        context?.getSharedPreferences("obsidian_sudoku_settings", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("key_public_username", trimmed)
            ?.apply()

        syncLeaderboardEntry(overrideUsername = trimmed)
    }

    fun getOverallStreak(): Int {
        val prefs = context?.getSharedPreferences("obsidian_sudoku_settings", Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        val lastWonDate = prefs?.getString("key_last_won_date", "") ?: ""
        val currentStreak = prefs?.getInt("key_overall_streak", 0) ?: 0

        // If last won date is earlier than yesterday, streak is broken
        return when {
            lastWonDate == today || lastWonDate == yesterday -> currentStreak.coerceAtLeast(1)
            lastWonDate.isNotBlank() -> 0
            else -> currentStreak
        }
    }

    private fun updateOverallStreakOnWin(): Int {
        val prefs = context?.getSharedPreferences("obsidian_sudoku_settings", Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        val lastWonDate = prefs?.getString("key_last_won_date", "") ?: ""
        val currentStreak = prefs?.getInt("key_overall_streak", 0) ?: 0

        val newStreak = when {
            lastWonDate == yesterday -> currentStreak + 1
            lastWonDate == today -> if (currentStreak > 0) currentStreak else 1
            else -> 1 // Started today
        }

        prefs?.edit()
            ?.putInt("key_overall_streak", newStreak)
            ?.putString("key_last_won_date", today)
            ?.apply()

        return newStreak
    }

    // -------------------------------------------------------------
    // REALTIME LEADERBOARD
    // -------------------------------------------------------------

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = callbackFlow {
        val colRef = db.collection("leaderboard")
        val currentUid = auth.currentUser?.uid

        fun fallbackList(): List<LeaderboardEntry> {
            val myName = getPublicUsername()
            val myStreak = getOverallStreak()
            val userScore = (myStreak * 150 + 450).coerceAtLeast(300)
            return listOf(
                LeaderboardEntry(1, "Aetherius", "Grandmaster", 3850, "02:14"),
                LeaderboardEntry(2, "Vespera", "Void Sovereign", 3120, "02:45"),
                LeaderboardEntry(3, myName, "Obsidian Solver", userScore, if (myStreak > 0) "02:50" else "--:--", isCurrentUser = true),
                LeaderboardEntry(4, "Kaelen", "Cipher Adept", 2250, "03:12"),
                LeaderboardEntry(5, "Nyx_99", "Grid Sentinel", 1840, "03:55"),
                LeaderboardEntry(6, "Solaris", "Pencil Artisan", 1430, "04:10"),
                LeaderboardEntry(7, "Zephyr", "Rift Seeker", 980, "05:02")
            ).sortedByDescending { it.score }.mapIndexed { idx, item -> item.copy(rank = idx + 1) }
        }

        if (auth.currentUser == null) {
            trySend(fallbackList())
            awaitClose { }
            return@callbackFlow
        }

        val registration = colRef
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(fallbackList())
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) {
                    trySend(fallbackList())
                    return@addSnapshotListener
                }

                var hasCurrentUserInList = false
                val remoteList = snapshot.documents.mapIndexed { index, doc ->
                    val entryUid = doc.getString("userId") ?: doc.id
                    val username = doc.getString("username") ?: "Solver_${entryUid.take(4)}"
                    val score = (doc.getLong("score") ?: 0L).toInt()
                    val bestTime = doc.getString("bestTime") ?: "--:--"
                    val title = doc.getString("title") ?: "Obsidian Solver"
                    val isCurrent = (currentUid != null && (entryUid == currentUid || doc.id == currentUid))
                    if (isCurrent) hasCurrentUserInList = true

                    LeaderboardEntry(
                        rank = index + 1,
                        name = username,
                        title = title,
                        score = score,
                        bestTime = bestTime,
                        isCurrentUser = isCurrent
                    )
                }.toMutableList()

                // If current user is not in top 50, append local user entry
                if (!hasCurrentUserInList && currentUid != null) {
                    val myName = getPublicUsername()
                    val myStreak = getOverallStreak()
                    remoteList.add(
                        LeaderboardEntry(
                            rank = remoteList.size + 1,
                            name = myName,
                            title = "Obsidian Solver",
                            score = myStreak * 150 + 100,
                            bestTime = "--:--",
                            isCurrentUser = true
                        )
                    )
                }

                trySend(remoteList)
            }
        awaitClose { registration.remove() }
    }

    suspend fun syncLeaderboardEntry(overrideUsername: String? = null) {
        val uid = auth.currentUser?.uid ?: return
        val username = overrideUsername ?: getPublicUsername()
        val docRef = db.collection("leaderboard").document(uid)

        val allStatsList = try {
            val snap = db.collection("users").document(uid).collection("game_stats").get().await()
            snap.documents.mapNotNull { it.toObject(FirestoreGameStats::class.java)?.toEntity() }
        } catch (_: Exception) {
            emptyList()
        }

        val totalWon = allStatsList.sumOf { it.gamesWon }
        val totalPerfect = allStatsList.sumOf { it.perfectGames }
        val streak = getOverallStreak()
        val bestTimeSecs = allStatsList.filter { it.bestTimeSeconds > 0 }.minOfOrNull { it.bestTimeSeconds } ?: 0L
        val bestTimeStr = if (bestTimeSecs > 0) {
            val mins = bestTimeSecs / 60
            val secs = bestTimeSecs % 60
            String.format(Locale.US, "%02d:%02d", mins, secs)
        } else "--:--"

        val score = (totalWon * 150 + totalPerfect * 200 + streak * 50).coerceAtLeast(100)
        val title = when {
            score >= 5000 -> "Grandmaster"
            score >= 3000 -> "Void Sovereign"
            score >= 1500 -> "Grid Sentinel"
            score >= 500 -> "Cipher Adept"
            else -> "Obsidian Initiate"
        }

        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "username" to username,
                        "score" to score,
                        "gamesWon" to totalWon,
                        "winStreak" to streak,
                        "bestTimeSeconds" to bestTimeSecs,
                        "bestTime" to bestTimeStr,
                        "title" to title,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "username" to username,
                        "score" to score,
                        "gamesWon" to totalWon,
                        "winStreak" to streak,
                        "bestTimeSeconds" to bestTimeSecs,
                        "bestTime" to bestTimeStr,
                        "title" to title,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (_: Exception) {
            // Firestore write ignored if transient
        }
    }

    // -------------------------------------------------------------
    // GAME SESSIONS
    // -------------------------------------------------------------

    fun observeActiveSession(targetUserId: String? = null): Flow<GameSessionEntity?> {
        if (isLocalOnlySession(targetUserId)) {
            return dao!!.getActiveSession()
        }
        return callbackFlow {
            val uid = currentOrTargetUserId(targetUserId)
            val docRef = db.collection("users").document(uid).collection("sessions").document("active")
            val registration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, docRef.path)
                    close(error)
                    return@addSnapshotListener
                }
                val entity = if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject(
                        FirestoreGameSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )?.toEntity()
                } else {
                    null
                }
                trySend(entity)
            }
            awaitClose { registration.remove() }
        }
    }

    fun observeDailyChallenges(targetUserId: String? = null): Flow<List<DailyChallengeEntity>> {
        if (isLocalOnlySession(targetUserId)) {
            return dao!!.getAllDailyChallenges()
        }
        return callbackFlow {
            val uid = currentOrTargetUserId(targetUserId)
            val colRef = db.collection("users").document(uid).collection("daily_challenges")
            val registration = colRef.whereEqualTo("userId", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, colRef.path)
                        close(error)
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(
                            FirestoreDailyChallenge::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )?.toEntity()
                    }?.sortedByDescending { it.dateString } ?: emptyList()
                    trySend(list)
                }
            awaitClose { registration.remove() }
        }
    }

    fun observeStickers(targetUserId: String? = null): Flow<List<StickerEntity>> {
        if (isLocalOnlySession(targetUserId)) {
            return dao!!.getAllStickers()
        }
        return callbackFlow {
            val uid = currentOrTargetUserId(targetUserId)
            val colRef = db.collection("users").document(uid).collection("stickers")
            val registration = colRef.whereEqualTo("userId", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, colRef.path)
                        close(error)
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(
                            FirestoreSticker::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )?.toEntity()
                    } ?: emptyList()
                    trySend(list)
                }
            awaitClose { registration.remove() }
        }
    }

    fun observeAllStats(targetUserId: String? = null): Flow<List<GameStatsEntity>> {
        if (isLocalOnlySession(targetUserId)) {
            return dao!!.getAllStats()
        }
        return callbackFlow {
            val uid = currentOrTargetUserId(targetUserId)
            val colRef = db.collection("users").document(uid).collection("game_stats")
            val registration = colRef.whereEqualTo("userId", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, colRef.path)
                        close(error)
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(
                            FirestoreGameStats::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )?.toEntity()
                    } ?: emptyList()
                    trySend(list)
                }
            awaitClose { registration.remove() }
        }
    }

    val activeSession: Flow<GameSessionEntity?>
        get() = observeActiveSession()

    val allDailyChallenges: Flow<List<DailyChallengeEntity>>
        get() = observeDailyChallenges()

    val stickers: Flow<List<StickerEntity>>
        get() = observeStickers()

    val unlockedStickersCount: Flow<Int>
        get() = observeStickers().map { list -> list.count { it.unlocked } }

    val allStats: Flow<List<GameStatsEntity>>
        get() = observeAllStats()

    suspend fun getActiveSessionOnce(targetUserId: String? = null): GameSessionEntity? {
        if (isLocalOnlySession(targetUserId)) {
            return dao!!.getActiveSession().firstOrNull()
        }
        val uid = currentOrTargetUserId(targetUserId)
        val docRef = db.collection("users").document(uid).collection("sessions").document("active")
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(
                    FirestoreGameSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.toEntity()
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun initDefaultStickers() {
        val defaultList = defaultStickerCatalog()
        if (isLocalOnlySession()) {
            dao?.insertStickers(defaultList)
            return
        }
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("stickers")
        try {
            val existingSnap = colRef.whereEqualTo("userId", uid).get().await()
            val existingIds = existingSnap.documents.map { it.id }.toSet()
            for (sticker in defaultList) {
                if (!existingIds.contains(sticker.id)) {
                    val docRef = colRef.document(sticker.id)
                    val payload = mapOf(
                        "userId" to uid,
                        "title" to sticker.title,
                        "description" to sticker.description,
                        "iconName" to sticker.iconName,
                        "category" to sticker.category,
                        "unlocked" to sticker.unlocked,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                }
            }
            dao?.insertStickers(defaultList)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colRef.path)
            throw e
        }
    }

    suspend fun saveActiveSession(
        difficulty: String,
        timerSeconds: Long,
        mistakes: Int,
        isPaused: Boolean,
        isDaily: Boolean,
        boardData: String,
        solutionData: String,
        notesData: String
    ) {
        val safeBoard = boardData.ifBlank { "0:0:0" }
        val safeSolution = solutionData.ifBlank { "1" }
        if (isLocalOnlySession()) {
            dao?.saveActiveSession(
                GameSessionEntity(
                    id = 1,
                    difficulty = difficulty,
                    timerSeconds = timerSeconds,
                    mistakes = mistakes,
                    isPaused = isPaused,
                    isDaily = isDaily,
                    boardData = safeBoard,
                    solutionData = safeSolution,
                    notesData = notesData,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            return
        }
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("sessions").document("active")
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updatePayload = mapOf(
                    "difficulty" to difficulty,
                    "timerSeconds" to timerSeconds,
                    "mistakes" to mistakes,
                    "isPaused" to isPaused,
                    "isDaily" to isDaily,
                    "boardData" to safeBoard,
                    "solutionData" to safeSolution,
                    "notesData" to notesData,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "difficulty" to difficulty,
                    "timerSeconds" to timerSeconds,
                    "mistakes" to mistakes,
                    "isPaused" to isPaused,
                    "isDaily" to isDaily,
                    "boardData" to safeBoard,
                    "solutionData" to safeSolution,
                    "notesData" to notesData,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            dao?.saveActiveSession(
                GameSessionEntity(
                    id = 1,
                    difficulty = difficulty,
                    timerSeconds = timerSeconds,
                    mistakes = mistakes,
                    isPaused = isPaused,
                    isDaily = isDaily,
                    boardData = safeBoard,
                    solutionData = safeSolution,
                    notesData = notesData,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun clearActiveSession() {
        if (isLocalOnlySession()) {
            dao?.clearActiveSession()
            return
        }
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("sessions").document("active")
        try {
            docRef.delete().await()
            dao?.clearActiveSession()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    /**
     * Records a finished Sudoku game.
     * When won, increments the overall active streak for ANY puzzle difficulty (not just Daily)!
     */
    suspend fun recordGameFinished(
        difficulty: String,
        timeSeconds: Long,
        mistakes: Int,
        isWon: Boolean
    ) {
        val newStreak = if (isWon) {
            updateOverallStreakOnWin()
        } else {
            0
        }

        if (isLocalOnlySession()) {
            val localDao = dao ?: return
            val existing = localDao.getStats(difficulty) ?: GameStatsEntity(difficulty = difficulty)
            val newPlayed = existing.gamesPlayed + 1
            val newWon = if (isWon) existing.gamesWon + 1 else existing.gamesWon
            val newPerfect = if (isWon && mistakes == 0) existing.perfectGames + 1 else existing.perfectGames
            val newBestTime = if (isWon) {
                if (existing.bestTimeSeconds == 0L || timeSeconds < existing.bestTimeSeconds) timeSeconds else existing.bestTimeSeconds
            } else existing.bestTimeSeconds
            val newTotalTime = existing.totalTimeSeconds + timeSeconds

            localDao.saveStats(
                existing.copy(
                    gamesPlayed = newPlayed,
                    gamesWon = newWon,
                    bestTimeSeconds = newBestTime,
                    totalTimeSeconds = newTotalTime,
                    winStreak = newStreak,
                    perfectGames = newPerfect
                )
            )
            if (isWon) {
                localDao.unlockSticker("first_win")
                if (timeSeconds < 300) localDao.unlockSticker("speed_demon")
                if (mistakes == 0) localDao.unlockSticker("flawless")
                if (difficulty == "EXPERT") localDao.unlockSticker("expert_solver")
                if (newStreak >= 3) localDao.unlockSticker("daily_streak_3")
                if (newStreak >= 7) localDao.unlockSticker("daily_streak_7")
                if (newWon >= 10) localDao.unlockSticker("ten_games")
            }
            return
        }

        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("game_stats").document(difficulty)
        try {
            val snap = docRef.get().await()
            val existing = if (snap.exists()) {
                snap.toObject(
                    FirestoreGameStats::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.toEntity() ?: GameStatsEntity(difficulty = difficulty)
            } else {
                GameStatsEntity(difficulty = difficulty)
            }

            val newPlayed = existing.gamesPlayed + 1
            val newWon = if (isWon) existing.gamesWon + 1 else existing.gamesWon
            val newPerfect = if (isWon && mistakes == 0) existing.perfectGames + 1 else existing.perfectGames
            val newBestTime = if (isWon) {
                if (existing.bestTimeSeconds == 0L || timeSeconds < existing.bestTimeSeconds) {
                    timeSeconds
                } else {
                    existing.bestTimeSeconds
                }
            } else {
                existing.bestTimeSeconds
            }
            val newTotalTime = existing.totalTimeSeconds + timeSeconds

            if (snap.exists()) {
                val updatePayload = mapOf(
                    "gamesPlayed" to newPlayed,
                    "gamesWon" to newWon,
                    "bestTimeSeconds" to newBestTime,
                    "totalTimeSeconds" to newTotalTime,
                    "winStreak" to newStreak,
                    "perfectGames" to newPerfect,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "difficulty" to difficulty,
                    "gamesPlayed" to newPlayed,
                    "gamesWon" to newWon,
                    "bestTimeSeconds" to newBestTime,
                    "totalTimeSeconds" to newTotalTime,
                    "winStreak" to newStreak,
                    "perfectGames" to newPerfect,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }

            dao?.saveStats(
                existing.copy(
                    gamesPlayed = newPlayed,
                    gamesWon = newWon,
                    bestTimeSeconds = newBestTime,
                    totalTimeSeconds = newTotalTime,
                    winStreak = newStreak,
                    perfectGames = newPerfect
                )
            )

            if (isWon) {
                unlockSticker("first_win")
                if (timeSeconds < 300) unlockSticker("speed_demon")
                if (mistakes == 0) unlockSticker("flawless")
                if (difficulty == "EXPERT") unlockSticker("expert_solver")
                if (newStreak >= 3) unlockSticker("daily_streak_3")
                if (newStreak >= 7) unlockSticker("daily_streak_7")
                if (newWon >= 10) unlockSticker("ten_games")

                // Update leaderboard in realtime!
                syncLeaderboardEntry()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun completeDailyChallenge(
        dateString: String,
        difficulty: String,
        timeSeconds: Long,
        mistakes: Int,
        stars: Int
    ) {
        if (isLocalOnlySession()) {
            dao?.saveDailyChallenge(
                DailyChallengeEntity(
                    dateString = dateString,
                    difficulty = difficulty,
                    completed = true,
                    timeSpentSeconds = timeSeconds,
                    mistakes = mistakes,
                    starsAwarded = stars,
                    completedAt = System.currentTimeMillis()
                )
            )
            return
        }
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("daily_challenges").document(dateString)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val updatePayload = mapOf(
                    "difficulty" to difficulty,
                    "completed" to true,
                    "timeSpentSeconds" to timeSeconds,
                    "mistakes" to mistakes,
                    "starsAwarded" to stars,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "dateString" to dateString,
                    "difficulty" to difficulty,
                    "completed" to true,
                    "timeSpentSeconds" to timeSeconds,
                    "mistakes" to mistakes,
                    "starsAwarded" to stars,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }

            dao?.saveDailyChallenge(
                DailyChallengeEntity(
                    dateString = dateString,
                    difficulty = difficulty,
                    completed = true,
                    timeSpentSeconds = timeSeconds,
                    mistakes = mistakes,
                    starsAwarded = stars,
                    completedAt = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun unlockSticker(id: String) {
        if (isLocalOnlySession()) {
            dao?.unlockSticker(id)
            return
        }
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("stickers").document(id)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "unlocked" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val template = defaultStickerCatalog().firstOrNull { it.id == id }
                    ?: StickerEntity(
                        id = id,
                        title = id,
                        description = "Achievement unlocked",
                        iconName = "sparkles",
                        category = "Milestone",
                        unlocked = true
                    )
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to template.title,
                        "description" to template.description,
                        "iconName" to template.iconName,
                        "category" to template.category,
                        "unlocked" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            dao?.unlockSticker(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun calculateStreak(challenges: List<DailyChallengeEntity>): Int {
        val overall = getOverallStreak()
        if (overall > 0) return overall

        if (challenges.isEmpty()) return 0
        val completedDates = challenges.filter { it.completed }.map { it.dateString }.toSet()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()

        var streak = 0
        val todayStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        val checkCal = Calendar.getInstance()
        if (completedDates.contains(todayStr)) {
            streak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else if (completedDates.contains(yesterdayStr)) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            return 0
        }

        while (true) {
            val dateStr = sdf.format(checkCal.time)
            if (completedDates.contains(dateStr)) {
                if (dateStr != todayStr) streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    companion object {
        fun defaultStickerCatalog(): List<StickerEntity> = listOf(
            StickerEntity(
                id = "first_win",
                title = "Obsidian Initiate",
                description = "Complete your first Sudoku puzzle",
                iconName = "sparkles",
                category = "Milestone",
                unlocked = false
            ),
            StickerEntity(
                id = "speed_demon",
                title = "Void Runner",
                description = "Solve any puzzle in under 5 minutes",
                iconName = "bolt",
                category = "Speed",
                unlocked = false
            ),
            StickerEntity(
                id = "flawless",
                title = "Obsidian Crystal",
                description = "Complete a puzzle with zero mistakes",
                iconName = "diamond",
                category = "Precision",
                unlocked = false
            ),
            StickerEntity(
                id = "expert_solver",
                title = "Grandmaster Shard",
                description = "Master an Expert difficulty puzzle",
                iconName = "military_tech",
                category = "Difficulty",
                unlocked = false
            ),
            StickerEntity(
                id = "daily_streak_3",
                title = "Starlight Continuum",
                description = "Reach a 3-day daily challenge streak",
                iconName = "local_fire_department",
                category = "Streaks",
                unlocked = false
            ),
            StickerEntity(
                id = "daily_streak_7",
                title = "Eternal Eclipse",
                description = "Reach a 7-day daily challenge streak",
                iconName = "brightness_auto",
                category = "Streaks",
                unlocked = false
            ),
            StickerEntity(
                id = "note_master",
                title = "Cipher Architect",
                description = "Win a puzzle while utilizing smart pencil notes",
                iconName = "edit_note",
                category = "Mastery",
                unlocked = false
            ),
            StickerEntity(
                id = "ten_games",
                title = "Dark Matter Collector",
                description = "Complete 10 total Sudoku puzzles",
                iconName = "auto_awesome",
                category = "Milestone",
                unlocked = false
            )
        )
    }
}
