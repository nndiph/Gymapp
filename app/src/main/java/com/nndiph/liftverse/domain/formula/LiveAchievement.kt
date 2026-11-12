package com.nndiph.liftverse.domain.formula

import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType
import kotlin.math.roundToInt

/** How a set beats the same set of the previous session (README "Pencapaian langsung"). */
sealed interface Improvement {
    data class Weight(val kg: Double) : Improvement

    data class Reps(val reps: Int) : Improvement

    data class Duration(val seconds: Int) : Improvement

    data class Distance(val meters: Int) : Improvement
}

/** The chip on the active set, recomputed every time one of its values changes. */
sealed interface LiveAchievement {
    data object None : LiveAchievement

    /** "Naik dari sesi lalu". */
    data class Improved(val improvement: Improvement) : LiveAchievement

    /** "Calon PR": the estimated 1RM beats the record. Shown instead of [Improved]. */
    data class PrCandidate(val estimatedKg: Double, val recordKg: Double) : LiveAchievement
}

/**
 * "Naik dari sesi lalu": more load with reps down by at most one, or more reps at the same load; for timed and
 * distance exercises, longer or further. Null when [current] is not better than [previous].
 */
fun improvementOver(trackingType: TrackingType, current: SetValues, previous: SetValues): Improvement? =
    when (trackingType) {
        TrackingType.WEIGHT_REPS, TrackingType.BODYWEIGHT_REPS -> {
            val deltaKg = ((current.weightKg ?: 0.0) - (previous.weightKg ?: 0.0)).roundedToHundredths()
            val deltaReps = (current.reps ?: 0) - (previous.reps ?: 0)
            when {
                deltaKg > 0.0 && deltaReps >= -1 -> Improvement.Weight(deltaKg)
                deltaKg == 0.0 && deltaReps > 0 -> Improvement.Reps(deltaReps)
                else -> null
            }
        }
        TrackingType.DURATION ->
            ((current.durationSec ?: 0) - (previous.durationSec ?: 0)).takeIf { it > 0 }?.let(Improvement::Duration)
        TrackingType.DISTANCE_DURATION ->
            ((current.distanceM ?: 0) - (previous.distanceM ?: 0)).takeIf { it > 0 }?.let(Improvement::Distance)
    }

/**
 * The live chip of the active set. Warm-ups and sets without a previous session get none. A record candidate
 * needs an existing record ([recordKg]) and, as in the design prototype, only load + reps sets can be one.
 */
fun liveAchievement(
    trackingType: TrackingType,
    current: SetValues,
    isWarmup: Boolean,
    previous: SetValues?,
    recordKg: Double?
): LiveAchievement {
    if (isWarmup || previous == null) return LiveAchievement.None
    val weight = current.weightKg
    val reps = current.reps
    if (trackingType == TrackingType.WEIGHT_REPS &&
        recordKg != null &&
        weight != null &&
        reps != null &&
        weight > 0.0 &&
        reps > 0
    ) {
        val estimate = estimatedOneRepMaxKg(weight, reps)
        if (estimate > recordKg + RECORD_TOLERANCE_KG) return LiveAchievement.PrCandidate(estimate, recordKg)
    }
    return improvementOver(trackingType, current, previous)?.let(LiveAchievement::Improved) ?: LiveAchievement.None
}

private fun Double.roundedToHundredths(): Double = (this * 100).roundToInt() / 100.0
