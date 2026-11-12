package com.nndiph.liftverse.feature.logger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.ui.PlaceholderScreen
import com.nndiph.liftverse.feature.logger.session.ActiveSessionRoute
import com.nndiph.liftverse.feature.logger.session.ActiveSessionUiState
import com.nndiph.liftverse.feature.logger.session.ActiveSessionViewModel
import com.nndiph.liftverse.feature.logger.session.PrimaryFillButton

/**
 * Catat tab. With a running session it opens the session straight away (handoff README: "Bila ada sesi aktif,
 * tab langsung membuka Sesi"). Without one it shows a placeholder with "Mulai sesi kosong" until the Catat screen
 * (routines, start buttons) is built.
 */
@Composable
fun LoggerScreen(modifier: Modifier = Modifier, viewModel: ActiveSessionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        ActiveSessionUiState.Loading -> Box(modifier.fillMaxSize())
        ActiveSessionUiState.NoSession -> {
            val sessionName = stringResource(R.string.session_default_name)
            PlaceholderScreen(
                title = stringResource(R.string.nav_logger),
                description = stringResource(R.string.placeholder_logger),
                modifier = modifier
            ) {
                Spacer(Modifier.height(24.dp))
                PrimaryFillButton(
                    text = stringResource(R.string.logger_start_empty),
                    onClick = { viewModel.startEmptySession(sessionName) },
                    modifier = Modifier.height(48.dp)
                )
            }
        }
        is ActiveSessionUiState.Running -> ActiveSessionRoute(current.session, viewModel, modifier)
    }
}
