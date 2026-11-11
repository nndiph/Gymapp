package com.repset.app.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.repset.app.R
import com.repset.app.core.ui.PlaceholderScreen

/** M4 fills this with the profile header, heatmap, progress and routine "playlist". */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.nav_profile),
        description = stringResource(R.string.placeholder_profile),
        modifier = modifier
    )
}
