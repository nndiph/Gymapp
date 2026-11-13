package com.nndiph.liftverse.feature.dashboard

import com.nndiph.liftverse.domain.formula.PlannedWork
import com.nndiph.liftverse.domain.formula.estimatedRoutineMinutes
import com.nndiph.liftverse.domain.formula.streakCount
import com.nndiph.liftverse.domain.formula.weekStart
import com.nndiph.liftverse.domain.model.DayStats
import com.nndiph.liftverse.domain.model.Exercise
import com.nndiph.liftverse.domain.model.ExerciseKeys
import com.nndiph.liftverse.domain.model.ExerciseVolume
import com.nndiph.liftverse.domain.model.Routine
import com.nndiph.liftverse.domain.model.SessionSummary
import com.nndiph.liftverse.domain.model.SetEntry
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.StreakSchedule
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    data class Ready(val dashboard: DashboardUi) : DashboardUiState
}

/** Dashboard (README Screens `dashboard`, screenshots light/01–02, dark/01). Numbers only; text is built in UI. */
data class DashboardUi(
    val today: LocalDate,
    val streak: Int,
    val streakLabel: StreakLabel,
    val weekSessions: Int,
    /** "dari target 3 sesi" under the week count; null shows "sesi minggu ini". */
    val weeklyTarget: Int?,
    /** Monday to Sunday of the current week. */
    val week: List<WeekDayUi>,
    val todayCard: TodayCardUi,
    /** A session is running: the start buttons become "Lanjutkan sesi". */
    val isSessionRunning: Boolean,
    /** "Minggu ini vs minggu lalu"; null before the first finished session. */
    val weekComparison: WeekComparisonUi?,
    val historyCount: Int,
    val exerciseCount: Int,
    val customExerciseCount: Int
)

enum class StreakLabel { TRAINING_DAYS, WEEKS, NOT_STARTED }

/** The circle of one weekday on the streak card. */
enum class DayMark {
    /** A finished session that day: filled with a check. */
    DONE,

    /** No training planned, or a past day without a session. */
    REST,

    /** Today, a training day without a session yet: ring. */
    TODAY,

    /** Today, a rest day without a session: ring on the rest fill. */
    TODAY_REST,

    /** A training day later this week: dashed ring. */
    PLANNED
}

data class WeekDayUi(val date: LocalDate, val isToday: Boolean, val mark: DayMark)

sealed interface TodayCardUi {
    /** "Latihan hari ini" with today's routine. */
    data class Planned(
        val routineId: String,
        val name: String,
        val weekdays: List<DayOfWeek>,
        val exerciseCount: Int,
        val setCount: Int,
        val estimatedMinutes: Int,
        /** The first [DASHBOARD_EXERCISE_ROWS] exercises. */
        val exercises: List<PlannedExerciseUi>,
        val moreExercises: Int,
        /** Day of the last finished session from this routine ("terakhir Senin, 9 November"). */
        val lastDone: LocalDate?
    ) : TodayCardUi

    /** "Tidak ada jadwal": routines exist but none is planned today. */
    data class Rest(val next: NextWorkoutUi?) : TodayCardUi

    /** "Belum ada routine". */
    data object NoRoutine : TodayCardUi

    /** Today's training is done ("Push Day selesai"). */
    data class Done(val name: String, val setCount: Int, val volumeKg: Double, val minutes: Int) : TodayCardUi
}

data class PlannedExerciseUi(
    val name: String,
    val isCustom: Boolean,
    val trackingType: TrackingType,
    val targetSets: Int,
    val targetReps: Int?,
    /** Working sets of the last session with this exercise; empty when it was never logged. */
    val previousSets: List<SetValues>,
    /** Volume of the last sessions, oldest first, for the sparkline; empty for timed exercises. */
    val recentVolumesKg: List<Double>,
    /** Volume this month vs the same days last month; null when there is nothing to compare with. */
    val monthChangePercent: Int?
)

/** "Berikutnya Push Day, Senin 16 November · 18.30". */
data class NextWorkoutUi(val routineName: String, val date: LocalDate, val time: LocalTime?)

data class WeekComparisonUi(
    val volumeKg: Double,
    val volumeChangePercent: Int?,
    val setCount: Int,
    val setChange: Int,
    /** Monday to Sunday: this week's and last week's volume per day. */
    val bars: List<DayBarUi>,
    /** The largest day volume of both weeks, the full height of a bar. */
    val maxDayVolumeKg: Double,
    val month: LocalDate,
    val monthSessions: Int,
    val monthVolumeKg: Double,
    /** Last day of last month's matching span ("Dibanding 1–12 Oktober"). */
    val previousMonthUntil: LocalDate,
    val monthChangePercent: Int?
)

data class DayBarUi(val date: LocalDate, val isToday: Boolean, val currentKg: Double?, val previousKg: Double?)

/** History of one exercise for the today card. */
data class ExerciseHistory(val previousSets: List<SetEntry>, val volumes: List<ExerciseVolume>)

const val DASHBOARD_EXERCISE_ROWS = 3
const val SPARKLINE_SESSIONS = 6

/** Today's routine: the first routine (oldest first) planned on today's weekday. */
fun todayRoutine(routines: List<Routine>, today: LocalDate): Routine? =
    routines.firstOrNull { today.dayOfWeek in it.weekdays }

/**
 * Builds the dashboard. [dayStats] are finished sessions per day up to [today]. [exercises] resolves the routine's
 * exercises; [history] holds the first [DASHBOARD_EXERCISE_ROWS] of today's routine.
 */
fun buildDashboardUi(
    today: LocalDate,
    schedule: StreakSchedule,
    dayStats: List<DayStats>,
    routines: List<Routine>,
    isSessionRunning: Boolean,
    latestSession: SessionSummary?,
    historyCount: Int,
    exerciseCount: Int,
    customExerciseCount: Int,
    exercises: Map<String, Exercise>,
    history: Map<String, ExerciseHistory>,
    lastRoutineDay: LocalDate?
): DashboardUi {
    val statsByDay = dayStats.filter { it.day <= today }.associateBy { it.day }
    val weekStart = today.weekStart()
    val hasHistory = historyCount > 0 || statsByDay.isNotEmpty()
    val plannedDays = when (schedule) {
        is StreakSchedule.FixedDays -> schedule.days
        // Weekly targets have no fixed days; the routines' days show what is planned (inference, see README).
        is StreakSchedule.WeeklyTarget -> routines.flatMap { it.weekdays }.toSet()
    }
    val week = (0L..6L).map { offset ->
        val date = weekStart.plusDays(offset)
        val done = statsByDay.containsKey(date)
        val planned = date.dayOfWeek in plannedDays
        val mark = when {
            done -> DayMark.DONE
            date < today -> DayMark.REST
            date == today -> if (planned) DayMark.TODAY else DayMark.TODAY_REST
            planned -> DayMark.PLANNED
            else -> DayMark.REST
        }
        WeekDayUi(date, date == today, mark)
    }

    return DashboardUi(
        today = today,
        streak = streakCount(schedule, statsByDay.mapValues { it.value.sessionCount }, today),
        streakLabel = when {
            !hasHistory -> StreakLabel.NOT_STARTED
            schedule is StreakSchedule.FixedDays -> StreakLabel.TRAINING_DAYS
            else -> StreakLabel.WEEKS
        },
        weekSessions = statsByDay.values.filter { it.day >= weekStart }.sumOf { it.sessionCount },
        weeklyTarget = (schedule as? StreakSchedule.WeeklyTarget)?.target?.takeIf { hasHistory },
        week = week,
        todayCard = todayCard(
            today,
            statsByDay[today],
            routines,
            isSessionRunning,
            latestSession,
            exercises,
            history,
            lastRoutineDay
        ),
        isSessionRunning = isSessionRunning,
        weekComparison = if (hasHistory) weekComparison(today, statsByDay) else null,
        historyCount = historyCount,
        exerciseCount = exerciseCount,
        customExerciseCount = customExerciseCount
    )
}

private fun todayCard(
    today: LocalDate,
    todayStats: DayStats?,
    routines: List<Routine>,
    isSessionRunning: Boolean,
    latestSession: SessionSummary?,
    exercises: Map<String, Exercise>,
    history: Map<String, ExerciseHistory>,
    lastRoutineDay: LocalDate?
): TodayCardUi {
    if (todayStats != null && !isSessionRunning) {
        return TodayCardUi.Done(
            name = latestSession?.takeIf { it.day == today }?.name.orEmpty(),
            setCount = todayStats.setCount,
            volumeKg = todayStats.volumeKg,
            minutes = (todayStats.durationSec / 60.0).roundToInt()
        )
    }
    val routine = todayRoutine(routines, today)
        ?: return if (routines.isEmpty()) TodayCardUi.NoRoutine else TodayCardUi.Rest(nextWorkout(routines, today))
    val work = routine.exercises.map { item ->
        val exercise = exercises[item.exerciseKey]
        PlannedWork(
            trackingType = exercise?.trackingType ?: TrackingType.WEIGHT_REPS,
            equipment = exercise?.equipment,
            sets = item.targetSets,
            reps = item.targetReps
        )
    }
    return TodayCardUi.Planned(
        routineId = routine.id,
        name = routine.name,
        weekdays = routine.weekdays.sorted(),
        exerciseCount = routine.exercises.size,
        setCount = routine.exercises.sumOf { it.targetSets },
        estimatedMinutes = estimatedRoutineMinutes(work),
        exercises = routine.exercises.take(DASHBOARD_EXERCISE_ROWS).map { item ->
            val exercise = exercises[item.exerciseKey]
            val trackingType = exercise?.trackingType ?: TrackingType.WEIGHT_REPS
            val itemHistory = history[item.exerciseKey]
            val volumes = itemHistory?.volumes.orEmpty()
            val hasVolume = trackingType == TrackingType.WEIGHT_REPS || trackingType == TrackingType.BODYWEIGHT_REPS
            PlannedExerciseUi(
                name = exercise?.name ?: item.nameSnapshot,
                isCustom = exercise?.isCustom ?: item.exerciseKey.startsWith(ExerciseKeys.CUSTOM_PREFIX),
                trackingType = trackingType,
                targetSets = item.targetSets,
                targetReps = item.targetReps,
                previousSets = itemHistory?.previousSets.orEmpty().filterNot { it.isWarmup }.map { it.values },
                recentVolumesKg = if (hasVolume) {
                    volumes.take(SPARKLINE_SESSIONS).map {
                        it.volumeKg
                    }.reversed()
                } else {
                    emptyList()
                },
                monthChangePercent = if (hasVolume) monthChange(volumes, today) else null
            )
        },
        moreExercises = (routine.exercises.size - DASHBOARD_EXERCISE_ROWS).coerceAtLeast(0),
        lastDone = lastRoutineDay
    )
}

private fun nextWorkout(routines: List<Routine>, today: LocalDate): NextWorkoutUi? = (1L..7L).firstNotNullOfOrNull {
    val date = today.plusDays(it)
    routines.firstOrNull { routine -> date.dayOfWeek in routine.weekdays }?.let { routine ->
        NextWorkoutUi(routine.name, date, routine.reminderTime?.takeIf { routine.reminderEnabled })
    }
}

/** Volume of an exercise from the 1st to [today] vs the same days of last month; null without any history. */
private fun monthChange(volumes: List<ExerciseVolume>, today: LocalDate): Int? {
    if (volumes.isEmpty()) return null
    val span = PeriodSpan.monthToDate(today)
    val current = volumes.filter { it.day in span.current }.sumOf { it.volumeKg }
    val previous = volumes.filter { it.day in span.previous }.sumOf { it.volumeKg }
    return percentChange(current, previous)
}

private fun weekComparison(today: LocalDate, statsByDay: Map<LocalDate, DayStats>): WeekComparisonUi {
    // Compare up to the last day with a session: today once trained, otherwise yesterday (prototype `dashPw`).
    val end = if (statsByDay.containsKey(today)) today else today.minusDays(1)
    val weekStart = today.weekStart()
    val weekSpan = ChronoUnit.DAYS.between(weekStart, end)
    val current = statsByDay.totals(weekStart, end)
    val previous = statsByDay.totals(weekStart.minusWeeks(1), weekStart.minusWeeks(1).plusDays(weekSpan))
    val bars = (0L..6L).map { offset ->
        val date = weekStart.plusDays(offset)
        DayBarUi(
            date = date,
            isToday = date == today,
            currentKg = statsByDay[date]?.volumeKg,
            previousKg = statsByDay[date.minusWeeks(1)]?.volumeKg
        )
    }
    val maxDay = bars.flatMap { listOfNotNull(it.currentKg, it.previousKg) }.maxOrNull() ?: 0.0
    val month = PeriodSpan.monthToDate(end, today)
    val monthCurrent = statsByDay.totals(month.current.start, month.current.endInclusive)
    val monthPrevious = statsByDay.totals(month.previous.start, month.previous.endInclusive)
    return WeekComparisonUi(
        volumeKg = current.volumeKg,
        volumeChangePercent = percentChange(current.volumeKg, previous.volumeKg),
        setCount = current.setCount,
        setChange = current.setCount - previous.setCount,
        bars = bars,
        maxDayVolumeKg = maxOf(maxDay, 1.0),
        month = today,
        monthSessions = monthCurrent.sessionCount,
        monthVolumeKg = monthCurrent.volumeKg,
        previousMonthUntil = month.previous.endInclusive,
        monthChangePercent = percentChange(monthCurrent.volumeKg, monthPrevious.volumeKg)
    )
}

/** "+23%": rounded change, 0 when both are empty, null when there was nothing before to compare with. */
fun percentChange(current: Double, previous: Double): Int? = when {
    previous > 0.0 -> ((current - previous) / previous * 100).roundToInt()
    current > 0.0 -> null
    else -> 0
}

private data class Totals(val sessionCount: Int, val setCount: Int, val volumeKg: Double)

private fun Map<LocalDate, DayStats>.totals(from: LocalDate, to: LocalDate): Totals {
    val days = values.filter { it.day in from..to }
    return Totals(days.sumOf { it.sessionCount }, days.sumOf { it.setCount }, days.sumOf { it.volumeKg })
}

/** This month from the 1st up to a day, and the same days of last month (cut at its last day). */
private class PeriodSpan(val current: ClosedRange<LocalDate>, val previous: ClosedRange<LocalDate>) {
    companion object {
        fun monthToDate(end: LocalDate, today: LocalDate = end): PeriodSpan {
            val monthStart = today.withDayOfMonth(1)
            val days = ChronoUnit.DAYS.between(monthStart, end).coerceAtLeast(0)
            val previousStart = monthStart.minusMonths(1)
            val previousEnd = minOf(previousStart.plusDays(days), monthStart.minusDays(1))
            return PeriodSpan(monthStart..end, previousStart..previousEnd)
        }
    }
}
