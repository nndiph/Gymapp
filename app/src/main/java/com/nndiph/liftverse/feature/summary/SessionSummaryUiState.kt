package com.nndiph.liftverse.feature.summary

import com.nndiph.liftverse.core.theme.heatmapLevel
import com.nndiph.liftverse.domain.formula.Improvement
import com.nndiph.liftverse.domain.formula.estimatedOneRepMaxKg
import com.nndiph.liftverse.domain.formula.improvementOver
import com.nndiph.liftverse.domain.formula.setVolumeKg
import com.nndiph.liftverse.domain.formula.streakCount
import com.nndiph.liftverse.domain.formula.weekStart
import com.nndiph.liftverse.domain.model.DayStats
import com.nndiph.liftverse.domain.model.Session
import com.nndiph.liftverse.domain.model.SetEntry
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.StreakSchedule
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

sealed interface SessionSummaryUiState {
    data object Loading : SessionSummaryUiState

    /** The session is gone (deleted elsewhere) or never finished. */
    data object Missing : SessionSummaryUiState

    data class Ready(val summary: SessionSummaryUi) : SessionSummaryUiState
}

/** "Selesai sesi" (README Screens `selesai`). Numbers only; the screen turns them into text. */
data class SessionSummaryUi(
    val name: String,
    val day: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val durationSec: Long,
    val setCount: Int,
    val volumeKg: Double,
    val exerciseCount: Int,
    /** Duration minus logged rest (SPEC "Timer istirahat"). */
    val activeSec: Long,
    val restSec: Long,
    val averageRestSec: Long,
    val improvedExercises: List<ImprovedExerciseUi>,
    val records: List<SummaryRecordUi>,
    /** Five Monday–Sunday weeks ending with the session's week, oldest first, Monday first within a week. */
    val heatmap: List<List<HeatCellUi>>,
    val weekSessions: Int,
    val monthSessions: Int,
    val streak: SummaryStreak
) {
    val improvedSetCount: Int get() = improvedExercises.sumOf { it.setCount }
}

/** One row of "6 set naik dari sesi lalu": the exercise, how many sets went up, and the first such step. */
data class ImprovedExerciseUi(val name: String, val setCount: Int, val firstImprovement: Improvement)

/** One "PR baru" card: the best record set of an exercise in this session. */
data class SummaryRecordUi(
    val name: String,
    val trackingType: TrackingType,
    val values: SetValues,
    val estimatedKg: Double,
    /** The record before this session, or null when there was none to compare with. */
    val previousKg: Double?
)

data class HeatCellUi(val date: LocalDate, val level: Int, val isSessionDay: Boolean, val isFuture: Boolean)

/** The tail of the heatmap text, depending on the streak schedule. */
sealed interface SummaryStreak {
    /** "Streak naik jadi 35 hari latihan." */
    data class Days(val count: Int) : SummaryStreak

    /** "Target 3 sesi minggu ini tercapai, streak 6 minggu." */
    data class TargetReached(val target: Int, val weeks: Int) : SummaryStreak

    /** "Kurang 1 sesi lagi untuk target minggu ini." */
    data class TargetLeft(val remaining: Int) : SummaryStreak
}

/** What the summary needs about an exercise beyond the session: the previous session and the record before. */
data class SummaryExerciseContext(val previousSets: List<SetEntry>, val recordKg: Double?)

const val SUMMARY_HEATMAP_WEEKS = 5

/**
 * Builds the summary of a finished [session]. [dayStats] are the finished sessions per day up to the session's
 * day, already including this session. Sets are paired with the previous session by position, as on the session
 * screen. Times are shown in [zone].
 */
fun buildSessionSummaryUi(
    session: Session,
    contexts: Map<String, SummaryExerciseContext>,
    dayStats: List<DayStats>,
    schedule: StreakSchedule,
    zone: ZoneId
): SessionSummaryUi {
    val endedAt = session.endedAt ?: session.startedAt
    val completed = session.exercises.map { exercise -> exercise to exercise.sets.filter { it.isCompleted } }
    val sets = completed.flatMap { it.second }
    val durationSec = Duration.between(session.startedAt, endedAt).seconds.coerceAtLeast(0)
    val restSec = sets.sumOf { (it.restAfterSec ?: 0).toLong() }
    val restCount = sets.count { it.restAfterSec != null }.takeIf { it > 0 } ?: (sets.size - 1).coerceAtLeast(1)

    val improved = session.exercises.mapNotNull { exercise ->
        val previous = contexts[exercise.exerciseKey]?.previousSets.orEmpty()
        val steps = exercise.sets.mapIndexedNotNull { index, set ->
            val before = previous.getOrNull(index)?.values
            if (!set.isCompleted || set.isWarmup || before == null) {
                null
            } else {
                improvementOver(exercise.trackingType, set.values, before)
            }
        }
        steps.firstOrNull()?.let { ImprovedExerciseUi(exercise.nameSnapshot, steps.size, it) }
    }

    val records = completed.mapNotNull { (exercise, done) ->
        done.filter { it.isPr }
            .mapNotNull { set -> set.oneRepMaxKg()?.let { set to it } }
            .maxByOrNull { it.second }
            ?.let { (set, estimate) ->
                SummaryRecordUi(
                    name = exercise.nameSnapshot,
                    trackingType = exercise.trackingType,
                    values = set.values,
                    estimatedKg = estimate,
                    previousKg = contexts[exercise.exerciseKey]?.recordKg
                )
            }
    }

    val day = session.day
    val sessionsByDay = dayStats.associate { it.day to it.sessionCount }
    val weekSessions = sessionsByDay.filterKeys { it >= day.weekStart() && it <= day }.values.sum()
    val monthSessions = sessionsByDay.filterKeys { it >= day.withDayOfMonth(1) && it <= day }.values.sum()
    val streak = streakCount(schedule, sessionsByDay, day)

    return SessionSummaryUi(
        name = session.name,
        day = day,
        startTime = session.startedAt.atZone(zone).toLocalTime(),
        endTime = endedAt.atZone(zone).toLocalTime(),
        durationSec = durationSec,
        setCount = sets.size,
        volumeKg = sets.sumOf { setVolumeKg(it.values.weightKg, it.values.reps) },
        exerciseCount = completed.count { it.second.isNotEmpty() },
        activeSec = (durationSec - restSec).coerceAtLeast(0),
        restSec = restSec,
        averageRestSec = restSec / restCount,
        improvedExercises = improved,
        records = records,
        heatmap = heatmapWeeks(day, dayStats.associate { it.day to it.volumeKg }),
        weekSessions = weekSessions,
        monthSessions = monthSessions,
        streak = when (schedule) {
            is StreakSchedule.FixedDays -> SummaryStreak.Days(streak)
            is StreakSchedule.WeeklyTarget -> if (weekSessions >= schedule.target) {
                SummaryStreak.TargetReached(schedule.target, streak)
            } else {
                SummaryStreak.TargetLeft(schedule.target - weekSessions)
            }
        }
    )
}

private fun heatmapWeeks(day: LocalDate, volumeByDay: Map<LocalDate, Double>): List<List<HeatCellUi>> {
    val firstWeek = day.weekStart().minusWeeks(SUMMARY_HEATMAP_WEEKS - 1L)
    return List(SUMMARY_HEATMAP_WEEKS) { week ->
        List(7) { weekday ->
            val date = firstWeek.plusWeeks(week.toLong()).plusDays(weekday.toLong())
            HeatCellUi(
                date = date,
                level = heatmapLevel(volumeByDay[date]),
                isSessionDay = date == day,
                isFuture = date > day
            )
        }
    }
}

private fun SetEntry.oneRepMaxKg(): Double? {
    val weight = values.weightKg ?: return null
    val reps = values.reps ?: return null
    return if (weight > 0.0 && reps > 0) estimatedOneRepMaxKg(weight, reps) else null
}
