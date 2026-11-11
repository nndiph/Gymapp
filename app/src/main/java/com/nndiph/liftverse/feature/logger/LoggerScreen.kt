package com.nndiph.liftverse.feature.logger

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.ui.PlaceholderScreen

/** M3 turns this into the workout logger (start session, pick exercise, log sets). */
@Composable
fun LoggerScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.nav_logger),
        description = stringResource(R.string.placeholder_logger),
        modifier = modifier
    )
}
