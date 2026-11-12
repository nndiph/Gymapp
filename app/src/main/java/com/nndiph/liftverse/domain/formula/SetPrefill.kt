package com.nndiph.liftverse.domain.formula

import com.nndiph.liftverse.domain.model.PlannedSet
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType

/**
 * Initial sets for an exercise added to a session (SPEC "Baris set": "Isian awal = nilai set yang sama dari sesi
 * terakhir gerakan itu").
 *
 * With [previous] sets from the last session of the exercise, set N copies set N of that session. When [setCount]
 * asks for more sets than there were, the extra sets copy the last one as working sets. Without history the
 * design defaults apply. A routine passes [setCount] and [targetReps]; a manually added exercise passes neither.
 */
fun prefillSets(
    trackingType: TrackingType,
    previous: List<PlannedSet>,
    setCount: Int? = null,
    targetReps: Int? = null
): List<PlannedSet> {
    if (previous.isNotEmpty()) {
        val count = setCount ?: previous.size
        return List(count) { index -> previous.getOrNull(index) ?: previous.last().copy(isWarmup = false) }
    }
    val default = defaultSet(trackingType, targetReps)
    return List(setCount ?: defaultSetCount(trackingType)) { default }
}

// Defaults of the design prototype for an exercise without history: 3 × 20 kg × 10, 3 × bodyweight × 10,
// 1 × 1:00, 1 × 2 km in 15:00.
private fun defaultSet(trackingType: TrackingType, targetReps: Int?): PlannedSet = when (trackingType) {
    TrackingType.WEIGHT_REPS -> PlannedSet(SetValues(weightKg = 20.0, reps = targetReps ?: DEFAULT_REPS))
    TrackingType.BODYWEIGHT_REPS -> PlannedSet(SetValues(weightKg = 0.0, reps = targetReps ?: DEFAULT_REPS))
    TrackingType.DURATION -> PlannedSet(SetValues(durationSec = 60))
    TrackingType.DISTANCE_DURATION -> PlannedSet(SetValues(distanceM = 2_000, durationSec = 900))
}

private fun defaultSetCount(trackingType: TrackingType): Int = when (trackingType) {
    TrackingType.WEIGHT_REPS, TrackingType.BODYWEIGHT_REPS -> 3
    TrackingType.DURATION, TrackingType.DISTANCE_DURATION -> 1
}

private const val DEFAULT_REPS = 10
