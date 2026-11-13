package com.nndiph.liftverse.core.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nndiph.liftverse.R
import com.nndiph.liftverse.domain.model.ExerciseKeys
import com.nndiph.liftverse.feature.dashboard.DashboardRoute
import com.nndiph.liftverse.feature.exercise.form.EXERCISE_FORM_ROUTE
import com.nndiph.liftverse.feature.exercise.form.EXERCISE_KEY_ARG
import com.nndiph.liftverse.feature.exercise.form.ExerciseFormRoute
import com.nndiph.liftverse.feature.exercise.form.SUGGESTED_NAME_ARG
import com.nndiph.liftverse.feature.exercise.form.editExerciseFormRoute
import com.nndiph.liftverse.feature.exercise.form.newExerciseFormRoute
import com.nndiph.liftverse.feature.exercise.picker.EXERCISE_PICKER_ROUTE
import com.nndiph.liftverse.feature.exercise.picker.ExercisePickerRoute
import com.nndiph.liftverse.feature.exercise.picker.PICKED_EXERCISE_KEY
import com.nndiph.liftverse.feature.exercise.picker.PICKER_MODE_ARG
import com.nndiph.liftverse.feature.exercise.picker.PickerCallbacks
import com.nndiph.liftverse.feature.exercise.picker.PickerMode
import com.nndiph.liftverse.feature.exercise.picker.exercisePickerRoute
import com.nndiph.liftverse.feature.logger.LoggerScreen
import com.nndiph.liftverse.feature.profile.ProfileScreen
import com.nndiph.liftverse.feature.routine.form.ROUTINE_FORM_ROUTE
import com.nndiph.liftverse.feature.routine.form.ROUTINE_ID_ARG
import com.nndiph.liftverse.feature.routine.form.RoutineFormRoute
import com.nndiph.liftverse.feature.routine.form.editRoutineFormRoute
import com.nndiph.liftverse.feature.routine.form.newRoutineFormRoute
import com.nndiph.liftverse.feature.summary.SESSION_ID_ARG
import com.nndiph.liftverse.feature.summary.SESSION_SUMMARY_ROUTE
import com.nndiph.liftverse.feature.summary.SessionSummaryRoute
import com.nndiph.liftverse.feature.summary.sessionSummaryRoute
import kotlinx.coroutines.launch

/**
 * Navigation graph: the three tabs plus full screens without the bottom bar ("Selesai sesi", the exercise picker,
 * the custom exercise and routine forms). Screens that pick an exercise read it from their own SavedStateHandle
 * under [PICKED_EXERCISE_KEY].
 */
@Composable
fun LiftverseNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.START.route,
        modifier = modifier
    ) {
        composable(TopLevelDestination.DASHBOARD.route) {
            DashboardRoute(
                onOpenLogger = { navController.navigateTopLevel(TopLevelDestination.LOGGER) },
                onOpenProfile = { navController.navigateTopLevel(TopLevelDestination.PROFILE) },
                onOpenExercises = { navController.navigate(exercisePickerRoute(PickerMode.BROWSE)) },
                onCreateRoutine = { navController.navigate(newRoutineFormRoute()) }
            )
        }
        composable(TopLevelDestination.LOGGER.route) { entry ->
            val picked by entry.pickedExercise()
            LoggerScreen(
                onAddExercise = { navController.navigate(exercisePickerRoute(PickerMode.ADD)) },
                pickedExerciseKey = picked,
                onPickedExerciseHandled = { entry.clearPickedExercise() },
                onCreateRoutine = { navController.navigate(newRoutineFormRoute()) },
                onEditRoutine = { navController.navigate(editRoutineFormRoute(it)) },
                onSessionFinished = { sessionId ->
                    // Replace the Catat tab, so Back from the summary goes to the dashboard, not an empty Catat.
                    navController.navigate(sessionSummaryRoute(sessionId)) {
                        popUpTo(TopLevelDestination.LOGGER.route) { inclusive = true }
                    }
                }
            )
        }
        composable(TopLevelDestination.PROFILE.route) { ProfileScreen() }
        exercisePicker(navController)
        exerciseForm(navController)
        routineForm(navController)
        composable(
            route = SESSION_SUMMARY_ROUTE,
            arguments = listOf(navArgument(SESSION_ID_ARG) { type = NavType.StringType })
        ) {
            SessionSummaryRoute(
                onOpenProfile = { navController.leaveSummaryTo(TopLevelDestination.PROFILE) },
                onOpenDashboard = { navController.leaveSummaryTo(TopLevelDestination.DASHBOARD) }
            )
        }
    }
}

private fun NavGraphBuilder.exercisePicker(navController: NavHostController) {
    composable(
        route = EXERCISE_PICKER_ROUTE,
        arguments = listOf(navArgument(PICKER_MODE_ARG) { type = NavType.StringType })
    ) { entry ->
        val mode = entry.arguments?.getString(PICKER_MODE_ARG)?.let(PickerMode::valueOf) ?: PickerMode.ADD
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val unavailable = stringResource(R.string.screen_unavailable)
        ExercisePickerRoute(
            callbacks = PickerCallbacks(
                onBack = { navController.popBackStack() },
                onPick = { key ->
                    when {
                        mode != PickerMode.BROWSE -> {
                            navController.previousBackStackEntry?.savedStateHandle?.set(PICKED_EXERCISE_KEY, key)
                            navController.popBackStack()
                        }
                        key.startsWith(ExerciseKeys.CUSTOM_PREFIX) -> navController.navigate(editExerciseFormRoute(key))
                        // Per-exercise progress for built-in exercises is a later screen.
                        else -> scope.launch { snackbarHostState.showSnackbar(unavailable) }
                    }
                },
                onCreate = { name -> navController.navigate(newExerciseFormRoute(name)) }
            ),
            snackbarHostState = snackbarHostState
        )
    }
}

private fun NavGraphBuilder.exerciseForm(navController: NavHostController) {
    composable(
        route = EXERCISE_FORM_ROUTE,
        arguments = listOf(
            navArgument(EXERCISE_KEY_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(SUGGESTED_NAME_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { entry ->
        val picked by entry.pickedExercise()
        ExerciseFormRoute(
            onClose = { navController.popBackStack() },
            onPickParent = { navController.navigate(exercisePickerRoute(PickerMode.SELECT)) },
            pickedParentKey = picked,
            onPickedParentHandled = { entry.clearPickedExercise() }
        )
    }
}

private fun NavGraphBuilder.routineForm(navController: NavHostController) {
    composable(
        route = ROUTINE_FORM_ROUTE,
        arguments = listOf(
            navArgument(ROUTINE_ID_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { entry ->
        val picked by entry.pickedExercise()
        RoutineFormRoute(
            onClose = { navController.popBackStack() },
            onAddExercise = { navController.navigate(exercisePickerRoute(PickerMode.ADD)) },
            pickedExerciseKey = picked,
            onPickedExerciseHandled = { entry.clearPickedExercise() }
        )
    }
}

@Composable
private fun NavBackStackEntry.pickedExercise() =
    savedStateHandle.getStateFlow<String?>(PICKED_EXERCISE_KEY, null).collectAsStateWithLifecycle()

private fun NavBackStackEntry.clearPickedExercise() {
    savedStateHandle[PICKED_EXERCISE_KEY] = null
}

/** Standard bottom-nav behaviour: one copy of each tab, state saved and restored when switching. */
fun NavHostController.navigateTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Leaves "Selesai sesi" for a tab without saving the summary, so the tab never restores it later. */
private fun NavHostController.leaveSummaryTo(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
        restoreState = true
    }
}

private val FULL_SCREEN_ROUTES =
    setOf(SESSION_SUMMARY_ROUTE, EXERCISE_PICKER_ROUTE, EXERCISE_FORM_ROUTE, ROUTINE_FORM_ROUTE)

/** True for routes that show the bottom navigation bar (the tabs). */
fun showsBottomBar(route: String?): Boolean = route !in FULL_SCREEN_ROUTES
