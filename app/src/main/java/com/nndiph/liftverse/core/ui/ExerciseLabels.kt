package com.nndiph.liftverse.core.ui

import androidx.annotation.StringRes
import com.nndiph.liftverse.R
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.TrackingType

// Labels of the design's filter chips and tracking types, shared by the picker, the forms and the session.

@get:StringRes
val MuscleGroup.labelRes: Int
    get() = when (this) {
        MuscleGroup.CHEST -> R.string.muscle_chest
        MuscleGroup.BACK -> R.string.muscle_back
        MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
        MuscleGroup.LEGS -> R.string.muscle_legs
        MuscleGroup.ARMS -> R.string.muscle_arms
        MuscleGroup.CORE -> R.string.muscle_core
        MuscleGroup.CARDIO -> R.string.muscle_cardio
    }

@get:StringRes
val Equipment.labelRes: Int
    get() = when (this) {
        Equipment.BARBELL -> R.string.equipment_barbell
        Equipment.DUMBBELL -> R.string.equipment_dumbbell
        Equipment.MACHINE -> R.string.equipment_machine
        Equipment.CABLE -> R.string.equipment_cable
        Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
        Equipment.OTHER -> R.string.equipment_other
    }

/** "Beban + reps", "Bodyweight reps", "Durasi", "Jarak + durasi". */
@get:StringRes
val TrackingType.labelRes: Int
    get() = when (this) {
        TrackingType.WEIGHT_REPS -> R.string.tracking_weight_reps
        TrackingType.BODYWEIGHT_REPS -> R.string.tracking_bodyweight_reps
        TrackingType.DURATION -> R.string.tracking_duration
        TrackingType.DISTANCE_DURATION -> R.string.tracking_distance_duration
    }
