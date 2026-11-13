package com.nndiph.liftverse.domain.formula

import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.Exercise
import com.nndiph.liftverse.domain.model.SetEntry
import com.nndiph.liftverse.domain.model.TrackingType

// Stepper and rest-timer rules from README "Baris set" and "Timer istirahat".

/** Load step: dumbbell 2 kg; barbell, cable and everything else 2,5 kg. */
fun weightStepKg(equipment: Equipment?): Double = if (equipment == Equipment.DUMBBELL) 2.0 else 2.5

/** The exercise's own load step (kettlebells: 4 kg), else the default for its equipment. */
fun weightStepOf(exercise: Exercise?): Double = exercise?.weightStepKg ?: weightStepKg(exercise?.equipment)

const val REPS_STEP = 1

/** 15 s for timed exercises; 30 s for the time of distance exercises ("durasi kardio" in SPEC). */
fun durationStepSec(trackingType: TrackingType): Int = if (trackingType == TrackingType.DISTANCE_DURATION) 30 else 15

/** 0,1 km. */
const val DISTANCE_STEP_M = 100

/**
 * Rest timer default: 1:00 for timed and distance exercises, 2:30 for barbell lifts, 1:30 for the rest.
 * The design lists bench, squat, deadlift, OHP and row by name; until the seed dataset gives exercises stable
 * keys, "barbell" stands in for that list.
 */
fun defaultRestSec(trackingType: TrackingType, equipment: Equipment?): Int = when {
    trackingType == TrackingType.DURATION || trackingType == TrackingType.DISTANCE_DURATION -> 60
    equipment == Equipment.BARBELL -> 150
    else -> 90
}

/**
 * "Mengubah beban set aktif ikut mengubah set berikutnya yang belum selesai dengan nilai sama": the ids of the
 * later sets in [sets] (one exercise, in order) that are not done and still carry [oldWeightKg].
 */
fun setsFollowingWeightChange(sets: List<SetEntry>, changedSetId: String, oldWeightKg: Double?): List<String> {
    val index = sets.indexOfFirst { it.id == changedSetId }
    if (index < 0) return emptyList()
    return sets.drop(index + 1).filter { !it.isCompleted && it.values.weightKg == oldWeightKg }.map { it.id }
}
