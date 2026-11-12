package com.nndiph.liftverse.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** A workout session. It is running while [endedAt] is null; at most one session runs at a time. */
data class Session(
    val id: String,
    val name: String,
    val routineId: String?,
    val startedAt: Instant,
    val endedAt: Instant?,
    /** Local calendar day the session started on (`dayKey` in docs/DATA_MODEL.md §1). */
    val day: LocalDate,
    /** Routine structure at the start, used to offer "Perbarui routine" when the session ends up different. */
    val routineSnapshot: List<PlannedExercise>,
    /** The running rest timer, or null when no rest is being timed. */
    val rest: RestTimer?,
    val exercises: List<SessionExercise>
) {
    val isRunning: Boolean get() = endedAt == null
}

/** One exercise of a routine at session start: which exercise and how many sets. */
data class PlannedExercise(val exerciseKey: String, val setCount: Int)

/** Rest timer kept as timestamps so it stays right after the app is closed (SPEC "Timer istirahat"). */
data class RestTimer(val afterSetId: String, val startedAt: Instant, val targetSec: Int)

data class SessionExercise(
    val id: String,
    val exerciseKey: String,
    val nameSnapshot: String,
    val trackingType: TrackingType,
    val sets: List<SetEntry>
)

/** The numbers of one set. Which fields apply depends on the exercise's [TrackingType]. */
data class SetValues(
    /** WEIGHT_REPS: the load. BODYWEIGHT_REPS: the added load, 0 = bodyweight only. */
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val distanceM: Int? = null
)

/** A set to create when an exercise is added to a session, before it has an id. */
data class PlannedSet(val values: SetValues, val isWarmup: Boolean = false)

data class SetEntry(
    val id: String,
    val values: SetValues,
    val isWarmup: Boolean,
    /** When the set was ticked off; null while it is still planned. */
    val completedAt: Instant?,
    /** Rest taken after this set (SPEC `SetLog.restAfterSec`). */
    val restAfterSec: Int?,
    val isPr: Boolean
) {
    val isCompleted: Boolean get() = completedAt != null
}

/** One row of the history list. Only finished sessions have one. */
data class SessionSummary(
    val id: String,
    val name: String,
    val day: LocalDate,
    val startedAt: Instant,
    val endedAt: Instant,
    val exerciseCount: Int,
    val setCount: Int,
    val volumeKg: Double,
    val restSec: Int,
    val prCount: Int
) {
    val durationSec: Long get() = Duration.between(startedAt, endedAt).seconds

    /** Active time = duration minus logged rest (SPEC "Timer istirahat"). */
    val activeSec: Long get() = (durationSec - restSec).coerceAtLeast(0)
}

/** Totals of the finished sessions on one calendar day, for the heatmap and progress screens. */
data class DayStats(
    val day: LocalDate,
    val sessionCount: Int,
    val setCount: Int,
    val volumeKg: Double,
    val durationSec: Long
)

/** A set removed from a session. The UI keeps it so "Urungkan" can put it back. */
data class RemovedSet(val sessionExerciseId: String, val position: Int, val set: SetEntry)

/** An exercise removed from a session. The UI keeps it so "Urungkan" can put it back. */
data class RemovedExercise(val sessionId: String, val position: Int, val exercise: SessionExercise)
