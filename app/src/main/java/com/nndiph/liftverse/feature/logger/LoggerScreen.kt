package com.nndiph.liftverse.feature.logger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.format.formatWeekday
import com.nndiph.liftverse.core.ui.PlaceholderScreen
import com.nndiph.liftverse.core.ui.PrimaryFillButton
import com.nndiph.liftverse.domain.model.Routine
import com.nndiph.liftverse.feature.logger.session.ActiveSessionRoute
import com.nndiph.liftverse.feature.logger.session.ActiveSessionUiState
import com.nndiph.liftverse.feature.logger.session.ActiveSessionViewModel

/**
 * Catat tab. With a running session it opens the session straight away (handoff README: "Bila ada sesi aktif,
 * tab langsung membuka Sesi"). Without one it shows a placeholder with "Mulai sesi kosong" until the Catat screen
 * (routines, start buttons) is built, plus "Buat routine" and the saved routines to edit. A saved session opens its
 * summary through [onSessionFinished].
 */
@Composable
fun LoggerScreen(
    onSessionFinished: (sessionId: String) -> Unit,
    onAddExercise: () -> Unit,
    /** The exercise picked in "Tambah gerakan", until [onPickedExerciseHandled] clears it. */
    pickedExerciseKey: String?,
    onPickedExerciseHandled: () -> Unit,
    onCreateRoutine: () -> Unit,
    onEditRoutine: (routineId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveSessionViewModel = hiltViewModel(),
    homeViewModel: LoggerHomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val routines by homeViewModel.routines.collectAsStateWithLifecycle()
    val sessionFinished by rememberUpdatedState(onSessionFinished)
    LaunchedEffect(viewModel) { viewModel.finishedSessions.collect { sessionFinished(it) } }
    LaunchedEffect(pickedExerciseKey) {
        if (pickedExerciseKey != null) {
            viewModel.addExercise(pickedExerciseKey)
            onPickedExerciseHandled()
        }
    }
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
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCreateRoutine,
                    modifier = Modifier.height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text(stringResource(R.string.logger_create_routine), style = MaterialTheme.typography.titleSmall)
                }
                if (routines.isNotEmpty()) RoutineList(routines, onEditRoutine)
            }
        }
        is ActiveSessionUiState.Running -> ActiveSessionRoute(current.session, viewModel, onAddExercise, modifier)
    }
}

/** Saved routines with their days; tapping one opens the routine form. Temporary until the Catat screen. */
@Composable
private fun RoutineList(routines: List<Routine>, onEditRoutine: (String) -> Unit) {
    val editLabel = stringResource(R.string.logger_edit_routine)
    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.logger_routines),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    ) {
        routines.forEachIndexed { index, routine ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clickable(onClickLabel = editLabel, role = Role.Button) { onEditRoutine(routine.id) }
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = routine.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (routine.weekdays.isEmpty()) {
                            stringResource(R.string.logger_routine_no_days)
                        } else {
                            routine.weekdays.sorted().joinToString(" · ") { formatWeekday(it) }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
