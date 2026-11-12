package com.nndiph.liftverse.feature.logger.session

import com.nndiph.liftverse.domain.formula.DISTANCE_STEP_M
import com.nndiph.liftverse.domain.formula.REPS_STEP
import com.nndiph.liftverse.domain.formula.durationStepSec
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType
import kotlin.math.roundToInt

/** A number of a set that one stepper edits. */
enum class SetField { WEIGHT, REPS, DURATION, DISTANCE }

/** Steppers per tracking type, in the prototype's order (bodyweight: reps first, then the added load). */
fun fieldsFor(trackingType: TrackingType): List<SetField> = when (trackingType) {
    TrackingType.WEIGHT_REPS -> listOf(SetField.WEIGHT, SetField.REPS)
    TrackingType.BODYWEIGHT_REPS -> listOf(SetField.REPS, SetField.WEIGHT)
    TrackingType.DURATION -> listOf(SetField.DURATION)
    TrackingType.DISTANCE_DURATION -> listOf(SetField.DISTANCE, SetField.DURATION)
}

/** The numpad edits load, reps and distance; time is changed with the steppers only (prototype). */
val SetField.hasNumpad: Boolean get() = this != SetField.DURATION

val SetField.allowsDecimal: Boolean get() = this == SetField.WEIGHT || this == SetField.DISTANCE

/** The value in the unit the user sees: kg, reps, seconds or km. */
fun SetValues.valueOf(field: SetField): Double = when (field) {
    SetField.WEIGHT -> weightKg ?: 0.0
    SetField.REPS -> (reps ?: 0).toDouble()
    SetField.DURATION -> (durationSec ?: 0).toDouble()
    SetField.DISTANCE -> (distanceM ?: 0) / 1000.0
}

/** Writes [value] (unit as in [valueOf]) back, never below zero. */
fun SetValues.withValue(field: SetField, value: Double): SetValues {
    val safe = value.coerceAtLeast(0.0)
    return when (field) {
        SetField.WEIGHT -> copy(weightKg = (safe * 100).roundToInt() / 100.0)
        SetField.REPS -> copy(reps = safe.roundToInt())
        SetField.DURATION -> copy(durationSec = safe.roundToInt())
        SetField.DISTANCE -> copy(distanceM = (safe * 1000).roundToInt())
    }
}

/** One stepper tap, in the unit of [valueOf]. */
fun stepFor(field: SetField, trackingType: TrackingType, weightStepKg: Double): Double = when (field) {
    SetField.WEIGHT -> weightStepKg
    SetField.REPS -> REPS_STEP.toDouble()
    SetField.DURATION -> durationStepSec(trackingType).toDouble()
    SetField.DISTANCE -> DISTANCE_STEP_M / 1000.0
}
