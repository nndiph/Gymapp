package com.nndiph.liftverse

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nndiph.liftverse.core.navigation.LiftverseNavHost
import com.nndiph.liftverse.core.navigation.TopLevelDestination
import com.nndiph.liftverse.core.navigation.navigateTopLevel
import com.nndiph.liftverse.core.navigation.showsBottomBar

/** Root of the UI: bottom navigation (Dashboard · Catat · Profil) hosting the nav graph; hidden on "Selesai sesi". */
@Composable
fun LiftverseApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = TopLevelDestination.fromRoute(backStackEntry?.destination?.route)

    Scaffold(
        bottomBar = {
            if (showsBottomBar(backStackEntry?.destination?.route)) {
                LiftverseBottomBar(
                    current = currentDestination,
                    onNavigate = { destination -> navController.navigateTopLevel(destination) }
                )
            }
        }
    ) { innerPadding ->
        LiftverseNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun LiftverseBottomBar(current: TopLevelDestination?, onNavigate: (TopLevelDestination) -> Unit) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            val selected = destination == current
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        painter = painterResource(
                            if (selected) destination.selectedIconRes else destination.unselectedIconRes
                        ),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(destination.labelRes)) }
            )
        }
    }
}
