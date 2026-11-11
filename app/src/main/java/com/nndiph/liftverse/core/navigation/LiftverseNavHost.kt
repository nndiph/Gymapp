package com.nndiph.liftverse.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nndiph.liftverse.feature.dashboard.DashboardScreen
import com.nndiph.liftverse.feature.logger.LoggerScreen
import com.nndiph.liftverse.feature.profile.ProfileScreen

/** Navigation graph. Feature screens are placeholders until their milestones (M3–M5). */
@Composable
fun LiftverseNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.START.route,
        modifier = modifier
    ) {
        composable(TopLevelDestination.DASHBOARD.route) { DashboardScreen() }
        composable(TopLevelDestination.LOGGER.route) { LoggerScreen() }
        composable(TopLevelDestination.PROFILE.route) { ProfileScreen() }
    }
}
