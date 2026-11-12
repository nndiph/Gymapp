package com.nndiph.liftverse.feature.logger.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.format.formatClock
import com.nndiph.liftverse.core.format.formatDecimal
import com.nndiph.liftverse.core.format.formatKilometres
import com.nndiph.liftverse.domain.formula.Improvement
import com.nndiph.liftverse.domain.formula.durationStepSec
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType

// Text of the session screen, built from string resources (prototype `fmtVal`, `liveAch`, card summaries).

@Composable
internal fun trackingTypeLabel(trackingType: TrackingType): String = stringResource(
    when (trackingType) {
        TrackingType.WEIGHT_REPS -> R.string.tracking_weight_reps
        TrackingType.BODYWEIGHT_REPS -> R.string.tracking_bodyweight_reps
        TrackingType.DURATION -> R.string.tracking_duration
        TrackingType.DISTANCE_DURATION -> R.string.tracking_distance_duration
    }
)

/** One set as text: "80 kg × 8", "BW × 12", "+5 kg × 12", "1:00", "2 km · 15:00"; [short] gives "80×8". */
@Composable
internal fun setSummary(trackingType: TrackingType, values: SetValues, short: Boolean = false): String {
    val reps = values.reps ?: 0
    val weight = values.weightKg ?: 0.0
    return when (trackingType) {
        TrackingType.WEIGHT_REPS -> stringResource(
            if (short) R.string.set_summary_weight_reps_short else R.string.set_summary_weight_reps,
            formatDecimal(weight),
            reps
        )
        TrackingType.BODYWEIGHT_REPS -> if (weight > 0.0) {
            stringResource(
                if (short) R.string.set_summary_added_weight_short else R.string.set_summary_added_weight,
                formatDecimal(weight),
                reps
            )
        } else {
            stringResource(R.string.set_summary_bodyweight, reps)
        }
        TrackingType.DURATION -> formatClock((values.durationSec ?: 0).toLong())
        TrackingType.DISTANCE_DURATION -> stringResource(
            R.string.set_summary_distance,
            formatKilometres(values.distanceM ?: 0),
            formatClock((values.durationSec ?: 0).toLong())
        )
    }
}

/** "Sesi lalu 80 kg × 8", or "Sesi lalu belum ada" for a set the last session did not have. */
@Composable
internal fun previousLabel(trackingType: TrackingType, previous: SetValues?): String = if (previous != null) {
    stringResource(R.string.set_previous, setSummary(trackingType, previous))
} else {
    stringResource(R.string.set_previous_none)
}

@Composable
internal fun setTitle(set: SetUi): String =
    set.number?.let { stringResource(R.string.set_title, it) } ?: stringResource(R.string.set_title_warmup)

/** "+2,5 kg", "+2 reps", "+0:15", "+0,1 km". */
@Composable
internal fun improvementText(improvement: Improvement): String = when (improvement) {
    is Improvement.Weight -> stringResource(R.string.improvement_weight, formatDecimal(improvement.kg))
    is Improvement.Reps -> stringResource(R.string.improvement_reps, improvement.reps)
    is Improvement.Duration -> stringResource(R.string.improvement_duration, formatClock(improvement.seconds.toLong()))
    is Improvement.Distance -> stringResource(R.string.improvement_distance, formatKilometres(improvement.meters))
}

/** Spoken form of an improvement chip: "Naik 2,5 kg dari sesi lalu". */
@Composable
internal fun improvementDescription(improvement: Improvement): String = when (improvement) {
    is Improvement.Weight -> stringResource(R.string.achievement_up_weight_description, formatDecimal(improvement.kg))
    is Improvement.Reps -> stringResource(R.string.achievement_up_reps_description, improvement.reps)
    is Improvement.Duration ->
        stringResource(R.string.achievement_up_duration_description, formatClock(improvement.seconds.toLong()))
    is Improvement.Distance ->
        stringResource(R.string.achievement_up_distance_description, formatKilometres(improvement.meters))
}

/**
 * Card subtitle. Open: "Beban + reps · langkah 2,5 kg". Closed: "4 set selesai · 2.440 kg",
 * "2 dari 4 set selesai", or "3 set · sesi lalu 40×8, 40×8, 40×7".
 */
@Composable
internal fun exerciseSubtitle(exercise: ExerciseUi): String {
    if (exercise.isExpanded) {
        val type = trackingTypeLabel(exercise.trackingType)
        val hasLoad = exercise.trackingType == TrackingType.WEIGHT_REPS ||
            exercise.trackingType == TrackingType.BODYWEIGHT_REPS
        return if (hasLoad) {
            stringResource(R.string.exercise_subtitle_step, type, formatDecimal(exercise.weightStep))
        } else {
            type
        }
    }
    val total = exercise.sets.size
    return when {
        exercise.isDone && exercise.completedVolumeKg > 0.0 -> stringResource(
            R.string.exercise_summary_done_volume,
            total,
            formatDecimal(exercise.completedVolumeKg)
        )
        exercise.isDone -> stringResource(R.string.exercise_summary_done, total)
        exercise.completedSets > 0 -> stringResource(R.string.exercise_summary_partial, exercise.completedSets, total)
        exercise.previousSets.isNotEmpty() -> stringResource(
            R.string.exercise_summary_planned_previous,
            total,
            exercise.previousSets.take(3).map { setSummary(exercise.trackingType, it, short = true) }.joinToString(", ")
        )
        else -> stringResource(R.string.exercise_summary_planned, total)
    }
}

/** Lower-case field name for "Kurangi beban" and similar. */
@Composable
internal fun fieldName(field: SetField, trackingType: TrackingType): String = stringResource(
    when (field) {
        SetField.WEIGHT ->
            if (trackingType == TrackingType.BODYWEIGHT_REPS) R.string.field_added_weight else R.string.field_weight
        SetField.REPS -> R.string.field_reps
        SetField.DURATION -> R.string.field_duration
        SetField.DISTANCE -> R.string.field_distance
    }
)

/** Stepper value: "82,5", "8", "BW" or "+5" for bodyweight load, "1:00", "2,5". */
@Composable
internal fun fieldValueText(field: SetField, trackingType: TrackingType, values: SetValues): String = when (field) {
    SetField.WEIGHT -> {
        val weight = values.weightKg ?: 0.0
        when {
            trackingType != TrackingType.BODYWEIGHT_REPS -> formatDecimal(weight)
            weight > 0.0 -> stringResource(R.string.value_added_weight, formatDecimal(weight))
            else -> stringResource(R.string.value_bodyweight)
        }
    }
    SetField.REPS -> (values.reps ?: 0).toString()
    SetField.DURATION -> formatClock((values.durationSec ?: 0).toLong())
    SetField.DISTANCE -> formatKilometres(values.distanceM ?: 0)
}

/** Stepper caption: "kg · ±2,5", "reps", "kg tambahan", "menit:detik · ±15 dtk", "km · ±0,1". */
@Composable
internal fun fieldCaption(field: SetField, trackingType: TrackingType, values: SetValues, weightStepKg: Double) =
    when (field) {
        SetField.WEIGHT -> when {
            trackingType != TrackingType.BODYWEIGHT_REPS ->
                stringResource(R.string.caption_weight, formatDecimal(weightStepKg))
            (values.weightKg ?: 0.0) > 0.0 -> stringResource(R.string.caption_added_weight)
            else -> stringResource(R.string.caption_no_added_weight)
        }
        SetField.REPS -> stringResource(R.string.caption_reps)
        SetField.DURATION -> stringResource(
            if (trackingType == TrackingType.DISTANCE_DURATION) R.string.caption_cardio_duration else R.string.caption_duration,
            durationStepSec(trackingType)
        )
        SetField.DISTANCE -> stringResource(
            R.string.caption_distance,
            formatDecimal(stepFor(field, trackingType, weightStepKg))
        )
    }

/** Numpad title field: "Beban", "Beban tambahan", "Reps", "Jarak". */
@Composable
internal fun numpadFieldTitle(field: SetField, trackingType: TrackingType): String = stringResource(
    when (field) {
        SetField.WEIGHT -> if (trackingType == TrackingType.BODYWEIGHT_REPS) {
            R.string.numpad_field_added_weight
        } else {
            R.string.numpad_field_weight
        }
        SetField.REPS -> R.string.numpad_field_reps
        SetField.DISTANCE -> R.string.numpad_field_distance
        SetField.DURATION -> R.string.field_duration
    }
)

/** Unit beside the numpad value. */
@Composable
internal fun numpadUnit(field: SetField): String = stringResource(
    when (field) {
        SetField.WEIGHT -> R.string.unit_kg
        SetField.REPS -> R.string.unit_reps
        SetField.DISTANCE -> R.string.unit_km
        SetField.DURATION -> R.string.field_duration
    }
)
