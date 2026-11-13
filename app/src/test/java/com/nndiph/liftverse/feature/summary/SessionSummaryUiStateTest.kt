package com.nndiph.liftverse.feature.summary

import com.nndiph.liftverse.domain.formula.Improvement
import com.nndiph.liftverse.domain.model.DayStats
import com.nndiph.liftverse.domain.model.Session
import com.nndiph.liftverse.domain.model.SessionExercise
import com.nndiph.liftverse.domain.model.SetEntry
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.StreakSchedule
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSummaryUiStateTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val day = LocalDate.of(2026, 11, 12)
    private val start = Instant.parse("2026-11-12T11:42:00Z")
    private var nextId = 0

    private fun set(
        kg: Double?,
        reps: Int?,
        rest: Int?,
        warmup: Boolean = false,
        pr: Boolean = false,
        sec: Int? = null
    ) = SetEntry(
        id = "s${nextId++}",
        values = SetValues(weightKg = kg, reps = reps, durationSec = sec),
        isWarmup = warmup,
        completedAt = start,
        restAfterSec = rest,
        isPr = pr
    )

    private val session = Session(
        id = "push",
        name = "Push Day",
        routineId = "r1",
        startedAt = start,
        endedAt = start.plusSeconds(3_252),
        day = day,
        routineSnapshot = emptyList(),
        rest = null,
        exercises = listOf(
            SessionExercise(
                id = "e1",
                exerciseKey = "bench",
                nameSnapshot = "Barbell Bench Press",
                trackingType = TrackingType.WEIGHT_REPS,
                sets = listOf(
                    set(60.0, 10, rest = 90, warmup = true),
                    set(82.5, 8, rest = 150),
                    set(82.5, 8, rest = 150, pr = true),
                    set(80.0, 7, rest = null)
                )
            ),
            SessionExercise(
                id = "e2",
                exerciseKey = "plank",
                nameSnapshot = "Plank",
                trackingType = TrackingType.DURATION,
                sets = listOf(set(null, null, rest = 45, sec = 75))
            ),
            SessionExercise(
                id = "e3",
                exerciseKey = "dips",
                nameSnapshot = "Dips",
                trackingType = TrackingType.BODYWEIGHT_REPS,
                sets = listOf(set(0.0, 12, rest = null))
            ),
            SessionExercise("e4", "row", "Barbell Row", TrackingType.WEIGHT_REPS, sets = emptyList())
        )
    )

    private val contexts = mapOf(
        "bench" to SummaryExerciseContext(
            previousSets = listOf(
                set(60.0, 10, rest = 90, warmup = true),
                set(80.0, 8, rest = 150),
                set(80.0, 8, rest = 150),
                set(80.0, 7, rest = null)
            ),
            recordKg = 101.33
        ),
        "plank" to SummaryExerciseContext(listOf(set(null, null, rest = null, sec = 60)), recordKg = null),
        "dips" to SummaryExerciseContext(emptyList(), recordKg = null)
    )

    private fun stats(date: String, sessions: Int = 1, volume: Double = 4_000.0) =
        DayStats(LocalDate.parse(date), sessions, setCount = 15, volumeKg = volume, durationSec = 3_000)

    private val dayStats = listOf(
        stats("2026-10-30"),
        stats("2026-11-02"),
        stats("2026-11-04"),
        stats("2026-11-09"),
        stats("2026-11-11"),
        stats("2026-11-12", sessions = 2, volume = 5_277.0)
    )

    private fun build(schedule: StreakSchedule = StreakSchedule.WeeklyTarget(3)) =
        buildSessionSummaryUi(session, contexts, dayStats, schedule, zone)

    @Test
    fun `totals count ticked sets, warm-ups in the volume, and exercises with sets only`() {
        val summary = build()
        assertEquals(LocalTime.of(18, 42), summary.startTime)
        assertEquals(LocalTime.of(19, 36, 12), summary.endTime)
        assertEquals(3_252L, summary.durationSec)
        assertEquals(6, summary.setCount)
        assertEquals(600.0 + 2 * 660.0 + 560.0, summary.volumeKg, 0.001)
        assertEquals(3, summary.exerciseCount)
    }

    @Test
    fun `active time is duration minus logged rest, averaged over the logged rests`() {
        val summary = build()
        assertEquals(435L, summary.restSec)
        assertEquals(3_252L - 435L, summary.activeSec)
        assertEquals(108L, summary.averageRestSec)
    }

    @Test
    fun `sets that went up are paired by position and skip warm-ups`() {
        val summary = build()
        assertEquals(
            listOf(
                ImprovedExerciseUi("Barbell Bench Press", 2, Improvement.Weight(2.5)),
                ImprovedExerciseUi("Plank", 1, Improvement.Duration(15))
            ),
            summary.improvedExercises
        )
        assertEquals(3, summary.improvedSetCount)
    }

    @Test
    fun `the best record set per exercise is shown with the record before`() {
        val record = build().records.single()
        assertEquals("Barbell Bench Press", record.name)
        assertEquals(SetValues(weightKg = 82.5, reps = 8), record.values)
        assertEquals(104.5, record.estimatedKg, 0.001)
        assertEquals(101.33, record.previousKg!!, 0.001)
    }

    @Test
    fun `heatmap covers five weeks ending with the session week`() {
        val heatmap = build().heatmap
        assertEquals(5, heatmap.size)
        assertTrue(heatmap.all { it.size == 7 })
        assertEquals(LocalDate.of(2026, 10, 12), heatmap.first().first().date)
        val sessionDay = heatmap.last()[3]
        assertEquals(day, sessionDay.date)
        assertTrue(sessionDay.isSessionDay)
        assertEquals(3, sessionDay.level)
        assertTrue(heatmap.last()[4].isFuture)
        assertFalse(heatmap.last()[2].isFuture)
    }

    @Test
    fun `heatmap text follows the streak schedule`() {
        val reached = build(StreakSchedule.WeeklyTarget(3))
        assertEquals(4, reached.weekSessions)
        assertEquals(6, reached.monthSessions)
        // This week reached 4 of 3; last week had 2, so the streak is one week.
        assertEquals(SummaryStreak.TargetReached(target = 3, weeks = 1), reached.streak)
        assertEquals(SummaryStreak.TargetLeft(remaining = 1), build(StreakSchedule.WeeklyTarget(5)).streak)
        // Monday, Wednesday and Thursday this week; last week missed Thursday 5 November.
        assertEquals(
            SummaryStreak.Days(3),
            build(StreakSchedule.FixedDays(setOf(MONDAY, WEDNESDAY, THURSDAY))).streak
        )
    }
}
