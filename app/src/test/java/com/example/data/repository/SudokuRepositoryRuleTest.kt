package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class SudokuRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveActiveSession_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val repository = SudokuRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveActiveSession(
                difficulty = "MEDIUM",
                timerSeconds = 42L,
                mistakes = 1,
                isPaused = false,
                isDaily = false,
                boardData = "1:1:0,2:0:0",
                solutionData = "1,2",
                notesData = ";"
            )
        }

        val session = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeActiveSession().first { it != null && it.timerSeconds == 42L }
        }
        assertNotNull(session)
        assertEquals("MEDIUM", session?.difficulty)
        assertEquals(1, session?.mistakes)

        // Update existing session to verify immutable createdAt & affectedKeys rule
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveActiveSession(
                difficulty = "MEDIUM",
                timerSeconds = 55L,
                mistakes = 2,
                isPaused = true,
                isDaily = false,
                boardData = "1:1:0,2:0:0",
                solutionData = "1,2",
                notesData = ";"
            )
        }

        val updated = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getActiveSessionOnce()
        }
        assertEquals(55L, updated?.timerSeconds)
        assertEquals(true, updated?.isPaused)
    }

    @Test
    fun recordGameFinishedAndDailyChallenge_authenticatedOwner_updatesStatsAndStickers() = runBlocking {
        signInTestUser("solver_${UUID.randomUUID()}@test.com")
        val repository = SudokuRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.initDefaultStickers()
            repository.recordGameFinished(
                difficulty = "EXPERT",
                timeSeconds = 210L,
                mistakes = 0,
                isWon = true
            )
            repository.completeDailyChallenge(
                dateString = "2026-10-17",
                difficulty = "DAILY",
                timeSeconds = 210L,
                mistakes = 0,
                stars = 3
            )
        }

        val stats = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAllStats().first { it.isNotEmpty() }
        }
        val expertStats = stats.firstOrNull { it.difficulty == "EXPERT" }
        assertNotNull(expertStats)
        assertEquals(1, expertStats?.gamesWon)
        assertEquals(1, expertStats?.perfectGames)

        val stickers = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeStickers().first { list -> list.any { it.id == "first_win" && it.unlocked } }
        }
        assertTrue(stickers.any { it.id == "first_win" && it.unlocked })
        assertTrue(stickers.any { it.id == "flawless" && it.unlocked })
        assertTrue(stickers.any { it.id == "expert_solver" && it.unlocked })

        val challenges = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDailyChallenges().first { it.isNotEmpty() }
        }
        assertEquals("2026-10-17", challenges.first().dateString)
        assertTrue(challenges.first().completed)
    }

    @Test
    fun getActiveSession_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_cross_${UUID.randomUUID()}@test.com")
        val aliceRepo = SudokuRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveActiveSession(
                difficulty = "EASY",
                timerSeconds = 15L,
                mistakes = 0,
                isPaused = false,
                isDaily = false,
                boardData = "1:1:0",
                solutionData = "1",
                notesData = ""
            )
        }

        signInTestUser("bob_cross_${UUID.randomUUID()}@test.com")
        val bobRepo = SudokuRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getActiveSessionOnce(targetUserId = aliceUid)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeStickers_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = SudokuRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeStickers().first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for unauthenticated caller")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
