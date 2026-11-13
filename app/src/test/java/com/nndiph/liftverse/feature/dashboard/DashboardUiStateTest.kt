package com.nndiph.liftverse.feature.dashboard

import com.nndiph.liftverse.domain.model.DayStats
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.Exercise
import com.nndiph.liftverse.domain.model.ExerciseVolume
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.Routine
import com.nndiph.liftverse.domain.model.RoutineExercise
import com.nndiph.liftverse.domain.model.SessionSummary
import com.nndiph.liftverse.domain.model.SetEntry
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.StreakSchedule
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardUiStateTest {
    private val thursday = LocalDate.of(2026, 11, 12)

    private val bench = Exercise(
        "seed:bench",
        "Barbell Bench Press",
        MuscleGroup.CHEST,
        Equipment.BARBELL,
        TrackingType.WEIGHT_REPS
    )
    private val plank = Exercise("seed:plank", "Plank", MuscleGroup.CORE, Equipment.BODYWEIGHT, TrackingType.DURATION)
    private val dips =
        Exercise("seed:dips", "Dips", MuscleGroup.CHEST, Equipment.BODYWEIGHT, TrackingType.BODYWEIGHT_REPS)

    private val push = Routine(
        id = "push",
        name = "Push Day",
        weekdays = setOf(THURSDAY, MONDAY),
        reminderEnabled = true,
        reminderTime = LocalTime.of(18, 30),
        exercises = listOf(
            RoutineExercise(bench.key, bench.name, targetSets = 4, targetReps = 8),
            RoutineExercise(plank.key, plank.name, targetSets = 3, targetReps = null),
            RoutineExercise("custom:press", "Incline DB Press", targetSets = 3, targetReps = 10),
            RoutineExercise(dips.key, dips.name, targetSets = 3, targetReps = 12)
        )
    )
    private val legs = Routine("legs", "Leg Day", setOf(SATURDAY), true, LocalTime.of(7, 0), emptyList())

    private fun stats(date: String, sets: Int, volume: Double) =
        DayStats(LocalDate.parse(date), sessionCount = 1, setCount = sets, volumeKg = volume, durationSec = 3_000)

    private val history = listOf(
        stats("2026-11-02", 20, 4_500.0),
        stats("2026-11-03", 15, 3_500.0),
        stats("2026-11-04", 10, 2_000.0),
        stats("2026-11-09", 20, 5_000.0),
        stats("2026-11-10", 18, 4_000.0)
    )

    private fun previousSet(kg: Double, reps: Int, warmup: Boolean = false) =
        SetEntry("p$kg$reps$warmup", SetValues(kg, reps), warmup, Instant.EPOCH, null, isPr = false)

    private val benchHistory = ExerciseHistory(
        previousSets = listOf(
            previousSet(60.0, 10, warmup = true),
            previousSet(80.0, 8),
            previousSet(80.0, 8),
            previousSet(80.0, 7)
        ),
        volumes = listOf(
            ExerciseVolume(LocalDate.of(2026, 11, 9), 1_500.0),
            ExerciseVolume(LocalDate.of(2026, 11, 2), 1_200.0),
            ExerciseVolume(LocalDate.of(2026, 10, 26), 1_000.0),
            ExerciseVolume(LocalDate.of(2026, 10, 8), 900.0)
        )
    )

    private fun build(
        today: LocalDate = thursday,
        schedule: StreakSchedule = StreakSchedule.FixedDays(setOf(MONDAY, TUESDAY, THURSDAY, SATURDAY)),
        dayStats: List<DayStats> = history,
        routines: List<Routine> = listOf(push, legs),
        running: Boolean = false,
        latest: SessionSummary? = null
    ) = buildDashboardUi(
        today = today,
        schedule = schedule,
        dayStats = dayStats,
        routines = routines,
        isSessionRunning = running,
        latestSession = latest,
        historyCount = dayStats.size,
        exerciseCount = 21,
        customExerciseCount = 3,
        exercises = listOf(bench, plank, dips).associateBy { it.key },
        history = mapOf(bench.key to benchHistory, plank.key to ExerciseHistory(emptyList(), emptyList())),
        lastRoutineDay = LocalDate.of(2026, 11, 9)
    )

    @Test
    fun `week circles show done, rest, today and planned days`() {
        val dashboard = build()
        assertEquals(
            listOf(
                DayMark.DONE,
                DayMark.DONE,
                DayMark.REST,
                DayMark.TODAY,
                DayMark.REST,
                DayMark.PLANNED,
                DayMark.REST
            ),
            dashboard.week.map { it.mark }
        )
        assertEquals(thursday, dashboard.week.single { it.isToday }.date)
        assertEquals(2, dashboard.weekSessions)
        // Last week missed Thursday 5 and Saturday 7 with only three session days.
        assertEquals(2, dashboard.streak)
        assertEquals(StreakLabel.TRAINING_DAYS, dashboard.streakLabel)
        assertNull(dashboard.weeklyTarget)
    }

    @Test
    fun `today's routine lists the first three exercises with targets and history`() {
        val card = build().todayCard as TodayCardUi.Planned
        assertEquals("Push Day", card.name)
        assertEquals(listOf(MONDAY, THURSDAY), card.weekdays)
        assertEquals(4, card.exerciseCount)
        assertEquals(13, card.setCount)
        assertEquals(1, card.moreExercises)
        assertEquals(LocalDate.of(2026, 11, 9), card.lastDone)

        val (benchRow, plankRow, customRow) = card.exercises
        assertEquals(listOf(SetValues(80.0, 8), SetValues(80.0, 8), SetValues(80.0, 7)), benchRow.previousSets)
        assertEquals(listOf(900.0, 1_000.0, 1_200.0, 1_500.0), benchRow.recentVolumesKg)
        // 1–12 November 2 700 kg vs 1–12 October 900 kg.
        assertEquals(200, benchRow.monthChangePercent)
        assertEquals(emptyList<Double>(), plankRow.recentVolumesKg)
        assertNull(plankRow.monthChangePercent)
        assertEquals("Incline DB Press", customRow.name)
        assertEquals(true, customRow.isCustom)
        assertEquals(emptyList<SetValues>(), customRow.previousSets)
    }

    @Test
    fun `week comparison runs up to yesterday until today has a session`() {
        val week = build().weekComparison!!
        // Monday–Wednesday: 9 000 kg and 38 sets vs 10 000 kg and 45 sets.
        assertEquals(9_000.0, week.volumeKg, 0.001)
        assertEquals(-10, week.volumeChangePercent)
        assertEquals(38, week.setCount)
        assertEquals(-7, week.setChange)
        assertEquals(5_000.0, week.maxDayVolumeKg, 0.001)
        assertEquals(DayBarUi(LocalDate.of(2026, 11, 9), false, 5_000.0, 4_500.0), week.bars.first())
        assertEquals(DayBarUi(thursday, true, null, null), week.bars[3])
        assertEquals(5, week.monthSessions)
        assertEquals(19_000.0, week.monthVolumeKg, 0.001)
        assertEquals(LocalDate.of(2026, 10, 11), week.previousMonthUntil)
        // Nothing in October to compare with.
        assertNull(week.monthChangePercent)
    }

    @Test
    fun `a session today turns the card into done unless another session is running`() {
        val today = stats("2026-11-12", 19, 5_277.0).copy(durationSec = 3_252)
        val latest = SessionSummary(
            id = "s",
            name = "Push Day",
            day = thursday,
            startedAt = Instant.EPOCH,
            endedAt = Instant.EPOCH.plusSeconds(3_252),
            exerciseCount = 6,
            setCount = 19,
            volumeKg = 5_277.0,
            restSec = 0,
            prCount = 0
        )
        val done = build(dayStats = history + today, latest = latest)
        assertEquals(TodayCardUi.Done("Push Day", 19, 5_277.0, 54), done.todayCard)
        assertEquals(DayMark.DONE, done.week[3].mark)
        assertEquals(43, done.weekComparison!!.volumeChangePercent)

        val running = build(dayStats = history + today, latest = latest, running = true)
        assertEquals("Push Day", (running.todayCard as TodayCardUi.Planned).name)
    }

    @Test
    fun `a day without a routine shows the next one with its reminder time`() {
        val friday = thursday.plusDays(1)
        val dashboard = build(today = friday, schedule = StreakSchedule.WeeklyTarget(3))
        assertEquals(
            TodayCardUi.Rest(NextWorkoutUi("Leg Day", LocalDate.of(2026, 11, 14), LocalTime.of(7, 0))),
            dashboard.todayCard
        )
        // Weekly targets have no fixed days, so the routines' days show as planned.
        assertEquals(DayMark.TODAY_REST, dashboard.week[4].mark)
        assertEquals(DayMark.PLANNED, dashboard.week[5].mark)
        assertEquals(3, dashboard.weeklyTarget)
        assertEquals(StreakLabel.WEEKS, dashboard.streakLabel)

        val noReminder = build(today = friday, routines = listOf(push, legs.copy(reminderEnabled = false)))
        assertNull((noReminder.todayCard as TodayCardUi.Rest).next!!.time)
    }

    @Test
    fun `a new user sees no routine, no streak and no comparison`() {
        val dashboard = build(dayStats = emptyList(), routines = emptyList())
        assertEquals(TodayCardUi.NoRoutine, dashboard.todayCard)
        assertEquals(StreakLabel.NOT_STARTED, dashboard.streakLabel)
        assertEquals(0, dashboard.streak)
        assertNull(dashboard.weekComparison)
        // The schedule still marks Thursday as a training day.
        assertEquals(DayMark.TODAY, dashboard.week[3].mark)
    }

    @Test
    fun `percent change has no value without anything to compare with`() {
        assertEquals(10, percentChange(110.0, 100.0))
        assertEquals(-10, percentChange(90.0, 100.0))
        assertEquals(0, percentChange(0.0, 0.0))
        assertNull(percentChange(5.0, 0.0))
    }
}
