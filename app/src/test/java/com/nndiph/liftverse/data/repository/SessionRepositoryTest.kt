package com.nndiph.liftverse.data.repository

import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.data.local.entity.SyncEntityType
import com.nndiph.liftverse.domain.model.DayStats
import com.nndiph.liftverse.domain.model.PlannedExercise
import com.nndiph.liftverse.domain.model.RestTimer
import com.nndiph.liftverse.domain.model.RoutineDraft
import com.nndiph.liftverse.domain.model.RoutineExercise
import com.nndiph.liftverse.domain.model.SetEntry
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.testing.BENCH
import com.nndiph.liftverse.testing.DIPS
import com.nndiph.liftverse.testing.MutableClock
import com.nndiph.liftverse.testing.PLANK
import com.nndiph.liftverse.testing.inMemoryDatabase
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionRepositoryTest {
    private val clock = MutableClock()
    private lateinit var database: LiftverseDatabase
    private lateinit var sessions: OfflineFirstSessionRepository
    private lateinit var routines: OfflineFirstRoutineRepository

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        sessions = OfflineFirstSessionRepository(database, clock)
        routines = OfflineFirstRoutineRepository(database, clock)
        OfflineFirstExerciseRepository(database, clock).upsertSeedExercises(listOf(BENCH, PLANK, DIPS))
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `routine session starts from design defaults, then from the previous session`() = runTest {
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Push Day",
                weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                reminderEnabled = true,
                reminderTime = LocalTime.of(18, 30),
                exercises = listOf(RoutineExercise(BENCH.key, BENCH.name, targetSets = 3, targetReps = 8))
            )
        )
        val firstId = sessions.startRoutineSession(routineId)!!
        val first = sessions.observeSession(firstId).first()!!
        assertEquals("Push Day", first.name)
        assertEquals(listOf(PlannedExercise(BENCH.key, 3)), first.routineSnapshot)
        assertEquals(List(3) { SetValues(20.0, 8) }, setsOf(firstId).map { it.values })

        val logged = listOf(SetValues(60.0, 8), SetValues(80.0, 8), SetValues(80.0, 6))
        setsOf(firstId).zip(logged).forEachIndexed { index, (set, values) ->
            sessions.updateSet(set.id, values, isWarmup = index == 0)
            sessions.completeSet(set.id, restTargetSec = 150)
        }
        assertTrue(sessions.finishSession(firstId))

        clock.advance(Duration.ofDays(3))
        val secondId = sessions.startRoutineSession(routineId)!!
        assertEquals(logged, setsOf(secondId).map { it.values })
        assertEquals(listOf(true, false, false), setsOf(secondId).map { it.isWarmup })
    }

    @Test
    fun `ticking a set logs the rest before it minus the estimated set time`() = runTest {
        val sessionId = sessions.startEmptySession("Sesi bebas")
        sessions.addExercise(sessionId, BENCH.key)
        // Three default sets of 20 kg × 10
        val (first, second, third) = setsOf(sessionId)

        sessions.completeSet(first.id, restTargetSec = 150)
        clock.advanceSeconds(130)
        // 130 s minus 10 reps × 4 s
        sessions.completeSet(second.id, restTargetSec = 150)
        clock.advanceSeconds(45)
        // "Lewati" logs the whole 45 s and stops the timer
        sessions.skipRest(sessionId)
        sessions.completeSet(third.id, restTargetSec = 90)

        assertEquals(listOf(90, 45, null), setsOf(sessionId).map { it.restAfterSec })
        assertEquals(RestTimer(third.id, clock.instant(), 90), sessions.observeSession(sessionId).first()!!.rest)
    }

    @Test
    fun `records start with the second session and follow deletes in older sessions`() = runTest {
        val first = logBenchSession(SetValues(100.0, 5))
        val second = logBenchSession(SetValues(105.0, 5))
        val third = logBenchSession(SetValues(102.0, 5))
        assertEquals(listOf(false), setsOf(first).map { it.isPr })
        assertEquals(listOf(true), setsOf(second).map { it.isPr })
        assertEquals(listOf(false), setsOf(third).map { it.isPr })

        sessions.deleteSession(second)

        // 102 × 5 now beats the 100 × 5 baseline
        assertEquals(listOf(true), setsOf(third).map { it.isPr })
        val queue = database.pendingSyncDao().getAll().associateBy { it.entityId }
        assertTrue(queue.getValue(second).deleted)
        assertFalse(queue.getValue(third).deleted)
    }

    @Test
    fun `finishing keeps ticked sets only and totals include warm-ups`() = runTest {
        val sessionId = sessions.startEmptySession("Sesi bebas")
        val benchId = sessions.addExercise(sessionId, BENCH.key)!!
        // Never ticked, so it is dropped
        sessions.addExercise(sessionId, PLANK.key)
        val benchSets = sessions.observeSession(sessionId).first()!!.exercises.first { it.id == benchId }.sets
        sessions.updateSet(benchSets[0].id, SetValues(60.0, 10), isWarmup = true)
        sessions.completeSet(benchSets[0].id, restTargetSec = 150)
        clock.advanceSeconds(120)
        sessions.updateSet(benchSets[1].id, SetValues(80.0, 8), isWarmup = false)
        sessions.completeSet(benchSets[1].id, restTargetSec = 150)
        clock.advanceSeconds(600)

        assertTrue(sessions.finishSession(sessionId))

        val finished = sessions.observeSession(sessionId).first()!!
        assertEquals(listOf(BENCH.key), finished.exercises.map { it.exerciseKey })
        assertEquals(listOf(benchSets[0].id, benchSets[1].id), setsOf(sessionId).map { it.id })
        assertNull(finished.rest)

        val summary = sessions.observeHistory(limit = 10).first().single()
        assertEquals(2, summary.setCount)
        assertEquals(60.0 * 10 + 80.0 * 8, summary.volumeKg, 0.001)
        // Warm-up rest: 120 s minus 8 reps × 4 s. The rest after the last set is not counted.
        assertEquals(88, summary.restSec)
        assertEquals(720L, summary.durationSec)
        assertEquals(632L, summary.activeSec)

        val day = sessions.observeDayStats(finished.day, finished.day).first().single()
        assertEquals(DayStats(finished.day, sessionCount = 1, setCount = 2, volumeKg = 1240.0, durationSec = 720), day)
        assertEquals(
            listOf(SyncEntityType.SESSION to sessionId),
            database.pendingSyncDao().getAll().map { it.entityType to it.entityId }
        )
    }

    @Test
    fun `finishing without ticked sets discards the session`() = runTest {
        val sessionId = sessions.startEmptySession("Sesi bebas")
        sessions.addExercise(sessionId, BENCH.key)

        assertFalse(sessions.finishSession(sessionId))

        assertNull(sessions.observeSession(sessionId).first())
        assertNull(sessions.observeRunningSession().first())
        assertTrue(database.pendingSyncDao().getAll().isEmpty())
    }

    @Test
    fun `only one session runs at a time`() = runTest {
        val running = sessions.startEmptySession("Sesi bebas")
        assertEquals(running, sessions.startEmptySession("Sesi kedua"))
        assertEquals(running, sessions.observeRunningSession().first()?.id)
    }

    @Test
    fun `removed sets and exercises come back in place with undo`() = runTest {
        val sessionId = sessions.startEmptySession("Sesi bebas")
        sessions.addExercise(sessionId, BENCH.key)
        sessions.addExercise(sessionId, PLANK.key)
        val before = sessions.observeSession(sessionId).first()!!.exercises

        val removedSet = sessions.removeSet(before[0].sets[1].id)!!
        val removedExercise = sessions.removeExercise(before[1].id)!!
        assertEquals(listOf(2), sessions.observeSession(sessionId).first()!!.exercises.map { it.sets.size })

        sessions.restoreSet(removedSet)
        sessions.restoreExercise(removedExercise)
        assertEquals(before, sessions.observeSession(sessionId).first()!!.exercises)
    }

    private suspend fun setsOf(sessionId: String): List<SetEntry> =
        sessions.observeSession(sessionId).first()!!.exercises.single().sets

    /** Logs one finished bench session a day after the previous one, with exactly [values] ticked off. */
    @Test
    fun `bodyweight sets never become records, even with more added load`() = runTest {
        logSession(DIPS.key, SetValues(weightKg = 0.0, reps = 10))
        val heavier = logSession(DIPS.key, SetValues(weightKg = 10.0, reps = 10), SetValues(weightKg = 20.0, reps = 8))

        assertEquals(listOf(false, false), setsOf(heavier).map { it.isPr })
        assertNull(sessions.recordBefore(DIPS.key, clock.instant()))
    }

    @Test
    fun `the record for a live candidate comes from earlier finished sessions only`() = runTest {
        logBenchSession(SetValues(100.0, 5))
        logBenchSession(SetValues(90.0, 5))
        val runningId = sessions.startEmptySession("Sesi bebas")
        sessions.addExercise(runningId, BENCH.key)
        sessions.completeSet(setsOf(runningId).first().id, restTargetSec = 150)

        // 100 × 5 → 116,7; the running session and later sessions do not count
        assertEquals(116.67, sessions.recordBefore(BENCH.key, clock.instant())!!, 0.01)
        assertNull(sessions.recordBefore(BENCH.key, Instant.EPOCH))
    }

    private suspend fun logBenchSession(vararg values: SetValues): String = logSession(BENCH.key, *values)

    /** Logs one finished session a day after the previous one, with exactly [values] ticked off. */
    private suspend fun logSession(exerciseKey: String, vararg values: SetValues): String {
        clock.advance(Duration.ofDays(1))
        val sessionId = sessions.startEmptySession("Sesi bebas")
        val exerciseId = sessions.addExercise(sessionId, exerciseKey)!!
        repeat(values.size) { sessions.addSet(exerciseId) }
        setsOf(sessionId).zip(values).forEach { (set, value) ->
            sessions.updateSet(set.id, value, isWarmup = false)
            sessions.completeSet(set.id, restTargetSec = 150)
        }
        assertTrue(sessions.finishSession(sessionId))
        return sessionId
    }
}
