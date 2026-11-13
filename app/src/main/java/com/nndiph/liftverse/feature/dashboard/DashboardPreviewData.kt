package com.nndiph.liftverse.feature.dashboard

import com.nndiph.liftverse.domain.formula.weekStart
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Sample dashboards as in screenshots light/01 (Thursday, Push Day planned) and light/02 (Friday, rest day). */
internal object DashboardPreviewData {
    private val thursday: LocalDate = LocalDate.of(2026, 11, 12)
    private val friday: LocalDate = thursday.plusDays(1)

    private fun week(today: LocalDate, done: Int, todayPlanned: Boolean) = (0L..6L).map { offset ->
        val date = today.weekStart().plusDays(offset)
        val mark = when {
            offset < done -> DayMark.DONE
            date == today -> if (todayPlanned) DayMark.TODAY else DayMark.TODAY_REST
            else -> DayMark.REST
        }
        WeekDayUi(date, date == today, mark)
    }

    private fun exercise(
        name: String,
        sets: Int,
        reps: Int,
        kg: Double,
        lastReps: List<Int>,
        volumes: List<Double>,
        change: Int,
        custom: Boolean = false
    ) = PlannedExerciseUi(
        name = name,
        isCustom = custom,
        trackingType = TrackingType.WEIGHT_REPS,
        targetSets = sets,
        targetReps = reps,
        previousSets = lastReps.map { SetValues(weightKg = kg, reps = it) },
        recentVolumesKg = volumes,
        monthChangePercent = change
    )

    private fun comparison(today: LocalDate, current: List<Double?>, previous: List<Double?>) = WeekComparisonUi(
        volumeKg = current.filterNotNull().sum(),
        volumeChangePercent = 6,
        setCount = 62,
        setChange = 3,
        bars = (0L..6L).map { offset ->
            val date = today.weekStart().plusDays(offset)
            DayBarUi(date, date == today, current[offset.toInt()], previous[offset.toInt()])
        },
        maxDayVolumeKg = (current + previous).filterNotNull().max(),
        month = today,
        monthSessions = 9,
        monthVolumeKg = 41_200.0,
        previousMonthUntil = today.minusMonths(1).minusDays(1),
        monthChangePercent = 12
    )

    private val lastWeek = listOf(4_800.0, 5_900.0, 5_600.0, 6_000.0, 4_200.0, null, null)

    val planned = DashboardUi(
        today = thursday,
        streak = 35,
        streakLabel = StreakLabel.TRAINING_DAYS,
        weekSessions = 3,
        weeklyTarget = null,
        week = week(thursday, done = 3, todayPlanned = true),
        todayCard = TodayCardUi.Planned(
            routineId = "push",
            name = "Push Day",
            weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            exerciseCount = 6,
            setCount = 19,
            estimatedMinutes = 55,
            exercises = listOf(
                exercise(
                    "Barbell Bench Press",
                    4,
                    8,
                    80.0,
                    listOf(8, 8, 7),
                    listOf(1_880.0, 2_000.0, 2_120.0, 2_200.0, 1_980.0, 2_440.0),
                    23
                ),
                exercise(
                    "Overhead Press",
                    3,
                    8,
                    40.0,
                    listOf(8, 8, 7),
                    listOf(880.0, 900.0, 920.0, 940.0, 960.0, 920.0),
                    6
                ),
                exercise(
                    "Incline DB Press (grip netral)",
                    3,
                    10,
                    24.0,
                    listOf(10, 10, 9),
                    listOf(560.0, 600.0, 620.0, 660.0, 680.0, 696.0),
                    12,
                    custom = true
                )
            ),
            moreExercises = 3,
            lastDone = LocalDate.of(2026, 11, 9)
        ),
        isSessionRunning = false,
        weekComparison = comparison(thursday, listOf(5_420.0, 6_100.0, 5_800.0, null, null, null, null), lastWeek),
        historyCount = 128,
        exerciseCount = 21,
        customExerciseCount = 3
    )

    val restDay = planned.copy(
        today = friday,
        weekSessions = 4,
        week = week(friday, done = 4, todayPlanned = false),
        todayCard = TodayCardUi.Rest(NextWorkoutUi("Push Day", LocalDate.of(2026, 11, 16), LocalTime.of(18, 30))),
        weekComparison = comparison(friday, listOf(5_420.0, 6_100.0, 5_800.0, 6_217.0, null, null, null), lastWeek)
    )

    val newUser = planned.copy(
        streak = 0,
        streakLabel = StreakLabel.NOT_STARTED,
        weekSessions = 0,
        week = week(thursday, done = 0, todayPlanned = false),
        todayCard = TodayCardUi.NoRoutine,
        weekComparison = null,
        historyCount = 0,
        customExerciseCount = 0
    )

    val done = planned.copy(
        weekSessions = 4,
        week = week(thursday, done = 4, todayPlanned = true),
        todayCard = TodayCardUi.Done(name = "Push Day", setCount = 19, volumeKg = 5_277.0, minutes = 54)
    )
}
