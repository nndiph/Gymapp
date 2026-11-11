package com.repset.app.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.repset.app.R
import com.repset.app.core.ui.PlaceholderScreen

/** M5 fills this with "today's workout", streak and weekly summary. */
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.nav_dashboard),
        description = stringResource(R.string.placeholder_dashboard),
        modifier = modifier
    )
}
