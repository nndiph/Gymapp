package com.nndiph.liftverse.feature.logger.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.format.formatClock
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.core.ui.LiftversePreview
import com.nndiph.liftverse.core.ui.ScreenPreviews
import com.nndiph.liftverse.domain.model.RestTimer
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Stateful entry of the running session: ticking clock, undo snackbars, view model actions. */
@Composable
fun ActiveSessionRoute(session: ActiveSessionUi, viewModel: ActiveSessionViewModel, modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        // A newer deletion replaces the snackbar of an older one, as in the prototype.
        viewModel.messages.collectLatest { message ->
            val text = when (message) {
                is SessionMessage.SetRemoved ->
                    message.setNumber
                        ?.let { resources.getString(R.string.deleted_set, it) }
                        ?: resources.getString(R.string.deleted_warmup_set)
                is SessionMessage.ExerciseRemoved -> resources.getString(
                    R.string.deleted_exercise,
                    message.exerciseName
                )
            }
            val result = withTimeoutOrNull(UNDO_WINDOW_MS) {
                snackbarHostState.showSnackbar(
                    message = text,
                    actionLabel = resources.getString(R.string.action_undo),
                    duration = SnackbarDuration.Indefinite
                )
            }
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(message)
        }
    }
    ActiveSessionScreen(
        session = session,
        now = rememberTickingNow(),
        actions = viewModel,
        onAddExercise = {
            // The exercise picker is the next screen in the handoff order; until then say so.
            scope.launch {
                snackbarHostState.showSnackbar(resources.getString(R.string.session_add_exercise_unavailable))
            }
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

/**
 * Active session (handoff screen `sesi`): header with the running time, one card per exercise where only the
 * running one is open, and a bottom bar with the rest timer above "Tambah gerakan" and "Selesai".
 */
@Composable
fun ActiveSessionScreen(
    session: ActiveSessionUi,
    now: Instant,
    actions: ActiveSessionActions,
    onAddExercise: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var numpad by rememberSaveable(stateSaver = NumpadRequestSaver) { mutableStateOf<NumpadRequest?>(null) }
    var finishing by rememberSaveable { mutableStateOf(false) }
    var bottomBarHeight by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            SessionHeader(session = session, now = now)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (session.exercises.isEmpty()) {
                EmptySessionState(Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(session.exercises, key = { it.id }) { exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            activeSetId = session.activeSetId,
                            actions = actions,
                            onEditField = { setId, field -> numpad = NumpadRequest(setId, field) }
                        )
                    }
                }
            }
            SessionBottomBar(
                rest = session.rest,
                now = now,
                actions = actions,
                onAddExercise = onAddExercise,
                onFinish = { finishing = true },
                modifier = Modifier.onSizeChanged { bottomBarHeight = it.height }
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = with(LocalDensity.current) { bottomBarHeight.toDp() })
        )
    }

    numpad?.let { request ->
        SessionNumpad(
            session = session,
            request = request,
            onSave = { value -> actions.onSetValue(request.setId, request.field, value) },
            onDismiss = { numpad = null }
        )
    }
    if (finishing) {
        FinishSessionSheet(session = session, onConfirm = actions::onFinish, onDismiss = { finishing = false })
    }
}

@Composable
private fun SessionHeader(session: ActiveSessionUi, now: Instant) {
    val elapsed = Duration.between(session.startedAt, now).seconds.coerceAtLeast(0)
    val runningRest = session.rest?.let { Duration.between(it.startedAt, now).seconds.coerceAtLeast(0) } ?: 0
    val restTotal = session.loggedRestSec + runningRest
    val active = (elapsed - restTotal).coerceAtLeast(0)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(LiftverseTheme.colors.accent))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatClock(elapsed),
                    style = LiftverseTheme.numbers.small,
                    color = LiftverseTheme.colors.accentText
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.session_set_progress, session.completedSets, session.totalSets),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.session_active_rest, formatClock(active), formatClock(restTotal)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SyncChip(Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun ExerciseCard(
    exercise: ExerciseUi,
    activeSetId: String?,
    actions: ActiveSessionActions,
    onEditField: (String, SetField) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            ExerciseHeader(
                exercise = exercise,
                onDelete = { actions.onDeleteExercise(exercise.id) },
                onToggle = { actions.onToggleExpanded(exercise.id) }
            )
            if (exercise.isExpanded) {
                exercise.sets.forEach { set ->
                    if (set.id == activeSetId) {
                        ActiveSetPanel(
                            set = set,
                            trackingType = exercise.trackingType,
                            weightStepKg = exercise.weightStep,
                            onStep = { field, direction -> actions.onStep(set.id, field, direction) },
                            onEdit = { field -> onEditField(set.id, field) },
                            onToggleWarmup = { actions.onToggleWarmup(set.id) },
                            onDelete = { actions.onDeleteSet(set.id) },
                            onPrimaryAction = { actions.onPrimaryAction(set.id) }
                        )
                    } else {
                        SetRow(
                            set = set,
                            trackingType = exercise.trackingType,
                            onOpen = { actions.onOpenSet(set.id) },
                            onToggleDone = { actions.onToggleDone(set.id) }
                        )
                    }
                }
                TextButton(
                    onClick = { actions.onAddSet(exercise.id) },
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.action_add_set), style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ExerciseHeader(exercise: ExerciseUi, onDelete: () -> Unit, onToggle: () -> Unit) {
    Row(
        // 76 dp like the design cards; taller only when a "Custom" tag adds a line.
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (exercise.isCustom) {
                CustomTag(Modifier.padding(vertical = 4.dp))
            }
            Text(
                text = exerciseSubtitle(exercise),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!exercise.isExpanded && exercise.isDone) {
            val doneLabel = stringResource(R.string.exercise_done_description)
            DoneCircle(
                done = true,
                size = 24.dp,
                modifier = Modifier.padding(end = 8.dp).semantics { contentDescription = doneLabel }
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.action_delete_exercise, exercise.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                painter = painterResource(
                    if (exercise.isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                ),
                contentDescription = stringResource(
                    if (exercise.isExpanded) R.string.action_collapse_exercise else R.string.action_expand_exercise,
                    exercise.name
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Empty session (README "Kosong / error"): icon in a 64 dp box, an 18 bold title, one sentence. */
@Composable
private fun EmptySessionState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_fitness_center),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_session_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_session_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SessionBottomBar(
    rest: RestTimer?,
    now: Instant,
    actions: ActiveSessionActions,
    onAddExercise: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (rest != null) {
                RestTimerPanel(
                    rest = rest,
                    now = now,
                    onMinus = { actions.onAdjustRest(-REST_ADJUST_SEC) },
                    onPlus = { actions.onAdjustRest(REST_ADJUST_SEC) },
                    onSkip = actions::onSkipRest
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onAddExercise,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_add_exercise),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Button(
                    onClick = onFinish,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_flag),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.action_finish), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun SessionNumpad(
    session: ActiveSessionUi,
    request: NumpadRequest,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val match = session.exercises.firstNotNullOfOrNull { exercise ->
        exercise.sets.firstOrNull { it.id == request.setId }?.let { exercise to it }
    }
    if (match == null) {
        // The set was deleted while the numpad was open.
        LaunchedEffect(request) { onDismiss() }
        return
    }
    val (exercise, set) = match
    NumpadSheet(
        title = stringResource(
            R.string.numpad_title,
            numpadFieldTitle(request.field, exercise.trackingType),
            exercise.name
        ),
        previousLabel = previousLabel(exercise.trackingType, set.previous),
        unit = numpadUnit(request.field),
        initialValue = set.values.valueOf(request.field),
        step = stepFor(request.field, exercise.trackingType, exercise.weightStep),
        allowDecimal = request.field.allowsDecimal,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

/** "Selesaikan sesi?" (design light/08): says which sets are dropped, then finish or keep training. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinishSessionSheet(session: ActiveSessionUi, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = MaterialTheme.colorScheme.scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.finish_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = finishBody(session),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            PrimaryFillButton(
                text = stringResource(R.string.finish_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text(text = stringResource(R.string.finish_continue), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun finishBody(session: ActiveSessionUi): String {
    val unticked = session.totalSets - session.completedSets
    return when {
        session.completedSets == 0 -> stringResource(R.string.finish_body_nothing_done)
        unticked > 0 -> stringResource(R.string.finish_body_unticked, unticked, session.completedSets)
        else -> stringResource(R.string.finish_body_all_done)
    }
}

/** The current time, updated on every whole second. */
@Composable
private fun rememberTickingNow(): Instant {
    val now by produceState(initialValue = Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(TICK_MS - System.currentTimeMillis() % TICK_MS)
        }
    }
    return now
}

private val NumpadRequestSaver = listSaver<NumpadRequest?, String>(
    save = { request -> if (request == null) emptyList() else listOf(request.setId, request.field.name) },
    restore = { saved -> if (saved.size == 2) NumpadRequest(saved[0], SetField.valueOf(saved[1])) else null }
)

private const val UNDO_WINDOW_MS = 5_000L
private const val TICK_MS = 1_000L

@ScreenPreviews
@Composable
private fun ActiveSessionScreenPreview() {
    LiftversePreview {
        ActiveSessionScreen(
            session = SessionPreviewData.pushDay,
            now = SessionPreviewData.now,
            actions = object : ActiveSessionActions {},
            onAddExercise = {}
        )
    }
}

@ScreenPreviews
@Composable
private fun EmptySessionScreenPreview() {
    LiftversePreview {
        ActiveSessionScreen(
            session = SessionPreviewData.emptySession,
            now = SessionPreviewData.now,
            actions = object : ActiveSessionActions {},
            onAddExercise = {}
        )
    }
}
