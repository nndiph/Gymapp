package com.nndiph.liftverse.domain.formula

import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.TrackingType
import kotlin.math.roundToInt

/** One routine exercise as needed for [estimatedRoutineMinutes]. */
data class PlannedWork(val trackingType: TrackingType, val equipment: Equipment?, val sets: Int, val reps: Int?)

/**
 * "sekitar 55 menit" on the dashboard. The design gives no formula, so each set counts its work (reps × 4 s, a
 * minute for timed sets, 15 minutes for distance sets) plus the default rest after it. Rounded to 5 minutes.
 */
fun estimatedRoutineMinutes(work: List<PlannedWork>): Int {
    val seconds = work.sumOf { item ->
        val setSeconds = when (item.trackingType) {
            TrackingType.WEIGHT_REPS, TrackingType.BODYWEIGHT_REPS -> (item.reps ?: 10) * ESTIMATED_SECONDS_PER_REP
            TrackingType.DURATION -> 60
            TrackingType.DISTANCE_DURATION -> 900
        }
        item.sets * (setSeconds + defaultRestSec(item.trackingType, item.equipment))
    }
    return ((seconds / 60.0 / 5).roundToInt() * 5).coerceAtLeast(5)
}
