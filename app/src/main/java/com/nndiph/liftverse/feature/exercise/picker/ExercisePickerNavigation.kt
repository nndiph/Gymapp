package com.nndiph.liftverse.feature.exercise.picker

// Route of "Pemilih gerakan". In ADD mode the picked exercise key goes back to the screen that opened the
// picker through its SavedStateHandle under PICKED_EXERCISE_KEY.

const val PICKER_MODE_ARG = "mode"
private const val EXERCISE_PICKER_BASE = "exercise_picker"
const val EXERCISE_PICKER_ROUTE = "$EXERCISE_PICKER_BASE/{$PICKER_MODE_ARG}"
const val PICKED_EXERCISE_KEY = "picked_exercise_key"

fun exercisePickerRoute(mode: PickerMode): String = "$EXERCISE_PICKER_BASE/${mode.name}"
