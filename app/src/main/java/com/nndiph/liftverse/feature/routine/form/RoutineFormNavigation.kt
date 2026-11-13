package com.nndiph.liftverse.feature.routine.form

// Route of the routine form: without an id it creates a routine, with one it edits that routine.

const val ROUTINE_ID_ARG = "routineId"
private const val ROUTINE_FORM_BASE = "routine_form"
const val ROUTINE_FORM_ROUTE = "$ROUTINE_FORM_BASE?$ROUTINE_ID_ARG={$ROUTINE_ID_ARG}"

fun newRoutineFormRoute(): String = ROUTINE_FORM_BASE

fun editRoutineFormRoute(routineId: String): String = "$ROUTINE_FORM_BASE?$ROUTINE_ID_ARG=$routineId"
