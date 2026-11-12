package com.nndiph.liftverse.domain.formula

import java.time.Duration
import java.time.Instant

// Training formulas from the design handoff (README "Rumus", SPEC "Rumus" and "Timer istirahat").

/**
 * Volume of one set = kg × reps. For bodyweight sets the weight is the added load, so plain bodyweight reps add 0.
 * Warm-up sets count towards volume (they do not count towards records).
 * The data layer applies the same rule in SQL (`SET_VOLUME_SQL`); change both together.
 */
fun setVolumeKg(weightKg: Double?, reps: Int?): Double = (weightKg ?: 0.0) * (reps ?: 0)

/** Estimated one-rep max (Epley): weight × (1 + reps / 30). The UI always labels it "estimasi". */
fun estimatedOneRepMaxKg(weightKg: Double, reps: Int): Double = weightKg * (1 + reps / 30.0)

/** Seconds assumed per rep when the time between two ticks is split into set time and rest. */
const val ESTIMATED_SECONDS_PER_REP = 4

/** A new estimate must beat the record by more than this, so equal lifts never count through rounding noise. */
const val RECORD_TOLERANCE_KG = 0.01

/**
 * Rest logged for the previous set: the time since the rest timer started, minus the estimated duration of the
 * set that ends it (reps × 4 s). Pass `null` reps when the rest is ended by "Lewati". Never negative.
 */
fun loggedRestSec(restStartedAt: Instant, endedAt: Instant, nextSetReps: Int?): Int {
    val elapsed = Duration.between(restStartedAt, endedAt).seconds
    return (elapsed - (nextSetReps ?: 0) * ESTIMATED_SECONDS_PER_REP).coerceAtLeast(0).toInt()
}

/** A completed set of one exercise, as input to [personalRecordSetIds]. */
data class RecordCandidate(
    val setId: String,
    val sessionId: String,
    val weightKg: Double?,
    val reps: Int?,
    val isWarmup: Boolean
)

/**
 * The sets that were personal records when they were done. [chronologicalSets] are all completed sets of one
 * exercise, oldest first. A record is a working set whose estimated 1RM beats every earlier working set.
 * Warm-ups never count. The first session with the exercise sets the baseline and has no records, because there
 * is no earlier session to compare with (README: "gerakan tanpa sesi lalu tidak dihitung").
 */
fun personalRecordSetIds(chronologicalSets: List<RecordCandidate>): Set<String> {
    var baselineSessionId: String? = null
    var best = 0.0
    val records = mutableSetOf<String>()
    for (set in chronologicalSets) {
        val oneRepMax = set.workingOneRepMaxKg() ?: continue
        if (baselineSessionId == null) baselineSessionId = set.sessionId
        if (set.sessionId != baselineSessionId && oneRepMax > best + RECORD_TOLERANCE_KG) records += set.setId
        best = maxOf(best, oneRepMax)
    }
    return records
}

/** The best estimated 1RM among the working sets in [sets], or null when there is no working set. */
fun bestOneRepMaxKg(sets: List<RecordCandidate>): Double? = sets.mapNotNull { it.workingOneRepMaxKg() }.maxOrNull()

private fun RecordCandidate.workingOneRepMaxKg(): Double? {
    val weight = weightKg
    val repCount = reps
    if (isWarmup || weight == null || repCount == null || weight <= 0.0 || repCount <= 0) return null
    return estimatedOneRepMaxKg(weight, repCount)
}
