package com.nndiph.liftverse.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nndiph.liftverse.feature.dashboard.DashboardRoute
import com.nndiph.liftverse.feature.logger.LoggerScreen
import com.nndiph.liftverse.feature.profile.ProfileScreen
import com.nndiph.liftverse.feature.summary.SESSION_ID_ARG
import com.nndiph.liftverse.feature.summary.SESSION_SUMMARY_ROUTE
import com.nndiph.liftverse.feature.summary.SessionSummaryRoute
import com.nndiph.liftverse.feature.summary.sessionSummaryRoute

/** Navigation graph: the three tabs plus "Selesai sesi", which opens over them without the bottom bar. */
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
                onOpenProfile = { navController.navigateTopLevel(TopLevelDestination.PROFILE) }
            )
        }
        composable(TopLevelDestination.LOGGER.route) {
            LoggerScreen(
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
fun showsBottomBar(route: String?): Boolean = route != SESSION_SUMMARY_ROUTE
