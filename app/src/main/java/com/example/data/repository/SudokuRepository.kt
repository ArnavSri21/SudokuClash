package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.SudokuDao
import com.example.data.model.DailyChallengeEntity
import com.example.data.model.FirestoreDailyChallenge
import com.example.data.model.FirestoreGameSession
import com.example.data.model.FirestoreGameStats
import com.example.data.model.FirestoreSticker
import com.example.data.model.GameSessionEntity
import com.example.data.model.GameStatsEntity
import com.example.data.model.StickerEntity
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SudokuRepository(
    private val db: FirebaseFirestore,
    private val dao: SudokuDao? = null
) {
    constructor(context: Context, dao: SudokuDao? = null) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        dao
    )

    private val auth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun currentOrTargetUserId(targetUserId: String?): String {
        return targetUserId ?: auth.currentUser?.uid ?: "unauthenticated"
    }

    fun observeActiveSession(targetUserId: String? = null): Flow<GameSessionEntity?> = callbackFlow {
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

    fun observeDailyChallenges(targetUserId: String? = null): Flow<List<DailyChallengeEntity>> = callbackFlow {
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

    fun observeStickers(targetUserId: String? = null): Flow<List<StickerEntity>> = callbackFlow {
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

    fun observeAllStats(targetUserId: String? = null): Flow<List<GameStatsEntity>> = callbackFlow {
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
        val uid = requireUserId()
        val defaultList = defaultStickerCatalog()
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
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("sessions").document("active")
        val safeBoard = boardData.ifBlank { "0:0:0" }
        val safeSolution = solutionData.ifBlank { "1" }
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

    suspend fun recordGameFinished(
        difficulty: String,
        timeSeconds: Long,
        mistakes: Int,
        isWon: Boolean
    ) {
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
            val newStreak = if (isWon) existing.winStreak + 1 else 0
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
                if (timeSeconds < 300) {
                    unlockSticker("speed_demon")
                }
                if (mistakes == 0) {
                    unlockSticker("flawless")
                }
                if (difficulty == "EXPERT") {
                    unlockSticker("expert_solver")
                }
                if (newWon >= 10) {
                    unlockSticker("ten_games")
                }
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
