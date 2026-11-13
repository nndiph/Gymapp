package com.nndiph.liftverse.core.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nndiph.liftverse.R
import com.nndiph.liftverse.feature.dashboard.DashboardRoute
import com.nndiph.liftverse.feature.exercise.picker.EXERCISE_PICKER_ROUTE
import com.nndiph.liftverse.feature.exercise.picker.ExercisePickerRoute
import com.nndiph.liftverse.feature.exercise.picker.PICKED_EXERCISE_KEY
import com.nndiph.liftverse.feature.exercise.picker.PICKER_MODE_ARG
import com.nndiph.liftverse.feature.exercise.picker.PickerCallbacks
import com.nndiph.liftverse.feature.exercise.picker.PickerMode
import com.nndiph.liftverse.feature.exercise.picker.exercisePickerRoute
import com.nndiph.liftverse.feature.logger.LoggerScreen
import com.nndiph.liftverse.feature.profile.ProfileScreen
import com.nndiph.liftverse.feature.summary.SESSION_ID_ARG
import com.nndiph.liftverse.feature.summary.SESSION_SUMMARY_ROUTE
import com.nndiph.liftverse.feature.summary.SessionSummaryRoute
import com.nndiph.liftverse.feature.summary.sessionSummaryRoute
import kotlinx.coroutines.launch

/** Navigation graph: the three tabs plus full screens without the bottom bar ("Selesai sesi", the exercise picker). */
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
                onOpenExercises = { navController.navigate(exercisePickerRoute(PickerMode.BROWSE)) }
            )
        }
        composable(TopLevelDestination.LOGGER.route) { entry ->
            val picked by entry.savedStateHandle.getStateFlow<String?>(PICKED_EXERCISE_KEY, null)
                .collectAsStateWithLifecycle()
            LoggerScreen(
                onAddExercise = { navController.navigate(exercisePickerRoute(PickerMode.ADD)) },
                pickedExerciseKey = picked,
                onPickedExerciseHandled = { entry.savedStateHandle[PICKED_EXERCISE_KEY] = null },
                onSessionFinished = { sessionId ->
                    // Replace the Catat tab, so Back from the summary goes to the dashboard, not an empty Catat.
                    navController.navigate(sessionSummaryRoute(sessionId)) {
                        popUpTo(TopLevelDestination.LOGGER.route) { inclusive = true }
                    }
                }
            )
        }
        composable(TopLevelDestination.PROFILE.route) { ProfileScreen() }
        composable(
            route = EXERCISE_PICKER_ROUTE,
            arguments = listOf(navArgument(PICKER_MODE_ARG) { type = NavType.StringType })
        ) { entry ->
            val mode = entry.arguments?.getString(PICKER_MODE_ARG)?.let(PickerMode::valueOf) ?: PickerMode.ADD
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val unavailable = stringResource(R.string.screen_unavailable)
            val showUnavailable: () -> Unit = { scope.launch { snackbarHostState.showSnackbar(unavailable) } }
            ExercisePickerRoute(
                callbacks = PickerCallbacks(
                    onBack = { navController.popBackStack() },
                    onPick = { key ->
                        if (mode == PickerMode.ADD) {
                            navController.previousBackStackEntry?.savedStateHandle?.set(PICKED_EXERCISE_KEY, key)
                            navController.popBackStack()
                        } else {
                            // Per-exercise progress is a later screen.
                            showUnavailable()
                        }
                    },
                    // The custom exercise form is the next pull request.
                    onCreate = { showUnavailable() }
                ),
                snackbarHostState = snackbarHostState
            )
        }
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

/** True for routes that show the bottom navigation bar (the tabs). */
fun showsBottomBar(route: String?): Boolean = route != SESSION_SUMMARY_ROUTE && route != EXERCISE_PICKER_ROUTE
