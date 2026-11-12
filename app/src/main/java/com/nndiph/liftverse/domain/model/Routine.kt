package com.nndiph.liftverse.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

/** A saved workout plan, also shown as a "playlist" on the profile. */
data class Routine(
    val id: String,
    val name: String,
    val weekdays: Set<DayOfWeek>,
    val reminderEnabled: Boolean,
    /** Local device time of the reminder. Kept while the reminder is switched off. */
    val reminderTime: LocalTime?,
    val exercises: List<RoutineExercise>
)

data class RoutineExercise(
    val exerciseKey: String,
    /** Exercise name when the routine was saved, so other users can read a routine without our custom exercises. */
    val nameSnapshot: String,
    /** Number of sets including warm-ups, as in the routine form stepper. */
    val targetSets: Int,
    /** Reps from the routine form stepper; null for DURATION and DISTANCE_DURATION exercises. */
    val targetReps: Int?
)

/** The fields of the routine form. */
data class RoutineDraft(
    val name: String,
    val weekdays: Set<DayOfWeek>,
    val reminderEnabled: Boolean,
    val reminderTime: LocalTime?,
    val exercises: List<RoutineExercise>
)
