package com.nndiph.liftverse.feature.exercise.form

import android.net.Uri

// Route of the custom exercise form. Without a key it creates a new exercise, optionally with a suggested name
// (the picker's search text); with a key it edits that custom exercise.

const val EXERCISE_KEY_ARG = "key"
const val SUGGESTED_NAME_ARG = "name"
private const val EXERCISE_FORM_BASE = "exercise_form"
const val EXERCISE_FORM_ROUTE =
    "$EXERCISE_FORM_BASE?$EXERCISE_KEY_ARG={$EXERCISE_KEY_ARG}&$SUGGESTED_NAME_ARG={$SUGGESTED_NAME_ARG}"

fun newExerciseFormRoute(suggestedName: String): String =
    "$EXERCISE_FORM_BASE?$SUGGESTED_NAME_ARG=${Uri.encode(suggestedName)}"

fun editExerciseFormRoute(exerciseKey: String): String =
    "$EXERCISE_FORM_BASE?$EXERCISE_KEY_ARG=${Uri.encode(exerciseKey)}"
