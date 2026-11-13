package com.nndiph.liftverse.feature.routine.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.format.formatTime
import com.nndiph.liftverse.core.format.formatWeekday
import com.nndiph.liftverse.core.theme.Archivo
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.core.ui.CustomTag
import com.nndiph.liftverse.core.ui.FormSaveBar
import com.nndiph.liftverse.core.ui.FormSectionTitle
import com.nndiph.liftverse.core.ui.FormTopBar
import com.nndiph.liftverse.core.ui.LiftversePreview
import com.nndiph.liftverse.core.ui.OutlinedValueField
import com.nndiph.liftverse.core.ui.ScreenPreviews
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.flow.collectLatest

/** What the routine form can ask for. */
class RoutineFormActions(
    val onName: (String) -> Unit,
    val onAddExercise: () -> Unit,
    val onStepSets: (index: Int, delta: Int) -> Unit,
    val onStepReps: (index: Int, delta: Int) -> Unit,
    val onRemove: (index: Int) -> Unit,
    val onMove: (from: Int, to: Int) -> Unit,
    val onToggleDay: (DayOfWeek) -> Unit,
    val onToggleReminder: () -> Unit,
    val onReminderTime: (LocalTime) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onClose: () -> Unit
)

@Composable
fun RoutineFormRoute(
    onClose: () -> Unit,
    onAddExercise: () -> Unit,
    /** The exercise picked in "Tambah gerakan", until [onPickedExerciseHandled] clears it. */
    pickedExerciseKey: String?,
    onPickedExerciseHandled: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutineFormViewModel = hiltViewModel()
) {
    val close by rememberUpdatedState(onClose)
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) { viewModel.closed.collect { close() } }
    LaunchedEffect(viewModel) {
        viewModel.removed.collectLatest { removed ->
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.routine_form_removed, removed.exercise.name),
                actionLabel = resources.getString(R.string.action_undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove(removed)
        }
    }
    LaunchedEffect(pickedExerciseKey) {
        if (pickedExerciseKey != null) {
            viewModel.addExercise(pickedExerciseKey)
            onPickedExerciseHandled()
        }
    }
    RoutineFormScreen(
        form = viewModel.form,
        actions = RoutineFormActions(
            onName = viewModel::onName,
            onAddExercise = onAddExercise,
            onStepSets = viewModel::stepSets,
            onStepReps = viewModel::stepReps,
            onRemove = viewModel::removeExercise,
            onMove = viewModel::moveExercise,
            onToggleDay = viewModel::toggleDay,
            onToggleReminder = viewModel::toggleReminder,
            onReminderTime = viewModel::setReminderTime,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onClose = onClose
        ),
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

/** "Routine baru" / "Ubah routine" (README Screens `routine-form`, screenshot light/27). */
@Composable
fun RoutineFormScreen(
    form: RoutineFormUi,
    actions: RoutineFormActions,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var pickingTime by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            FormTopBar(
                title = stringResource(
                    if (form.isEdit) R.string.routine_form_title_edit else R.string.routine_form_title_new
                ),
                onClose = actions.onClose
            )
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    OutlinedTextField(
                        value = form.name,
                        onValueChange = actions.onName,
                        label = { Text(stringResource(R.string.routine_form_name)) },
                        placeholder = { Text(stringResource(R.string.routine_form_name_hint)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExercisesSection(form, actions)
                    DaysSection(form.weekdays, actions.onToggleDay)
                    ReminderSection(form, actions.onToggleReminder) { pickingTime = true }
                }
                if (form.isEdit) DeleteSection { confirmingDelete = true }
            }
            FormSaveBar(
                text = stringResource(
                    if (form.isEdit) R.string.action_save_changes else R.string.routine_form_save_new
                ),
                enabled = form.canSave,
                hint = stringResource(R.string.routine_form_incomplete),
                onSave = actions.onSave
            )
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }
    if (pickingTime) {
        ReminderTimeDialog(
            initial = form.reminderTime,
            onConfirm = {
                actions.onReminderTime(it)
                pickingTime = false
            },
            onDismiss = { pickingTime = false }
        )
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.routine_form_delete_title, form.name.trim())) },
            text = { Text(stringResource(R.string.routine_form_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    actions.onDelete()
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun ExercisesSection(form: RoutineFormUi, actions: RoutineFormActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FormSectionTitle(stringResource(R.string.routine_form_exercises), Modifier.alignByBaseline())
            Text(
                text = if (form.exercises.isEmpty()) {
                    stringResource(R.string.routine_form_meta_empty)
                } else {
                    stringResource(R.string.routine_form_meta, form.exercises.size, form.totalSets)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline()
            )
        }
        if (form.exercises.isNotEmpty()) ReorderableExercises(form.exercises, actions)
        OutlinedButton(
            onClick = actions.onAddExercise,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.action_add_exercise), style = MaterialTheme.typography.titleSmall)
        }
    }
}

/**
 * The exercise card. Each row drags by its handle; TalkBack users get "Pindah ke atas/bawah" actions instead.
 * A row swaps with its neighbour once it has moved past half of the neighbour's height.
 */
@Composable
private fun ReorderableExercises(items: List<RoutineExerciseDraftUi>, actions: RoutineFormActions) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<Int, Int>() }
    val shape = RoundedCornerShape(16.dp)
    val moveUp = stringResource(R.string.routine_form_move_up)
    val moveDown = stringResource(R.string.routine_form_move_down)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    ) {
        items.forEachIndexed { index, item ->
            val currentIndex by rememberUpdatedState(index)
            val dragging = draggingIndex == index
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                    .background(MaterialTheme.colorScheme.surface)
                    .onSizeChanged { heights[index] = it.height }
                    .semantics {
                        customActions = buildList {
                            if (index > 0) {
                                add(
                                    CustomAccessibilityAction(moveUp) {
                                        actions.onMove(index, index - 1)
                                        true
                                    }
                                )
                            }
                            if (index < items.lastIndex) {
                                add(
                                    CustomAccessibilityAction(moveDown) {
                                        actions.onMove(index, index + 1)
                                        true
                                    }
                                )
                            }
                        }
                    }
                    .padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 40.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    draggingIndex = currentIndex
                                    dragOffset = 0f
                                },
                                onDragEnd = { draggingIndex = null },
                                onDragCancel = { draggingIndex = null }
                            ) { change, amount ->
                                change.consume()
                                val at = draggingIndex ?: return@detectDragGestures
                                dragOffset += amount.y
                                val below = heights[at + 1]
                                val above = heights[at - 1]
                                if (below != null && dragOffset > below / 2f) {
                                    actions.onMove(at, at + 1)
                                    draggingIndex = at + 1
                                    dragOffset -= below
                                } else if (above != null && dragOffset < -above / 2f) {
                                    actions.onMove(at, at - 1)
                                    draggingIndex = at - 1
                                    dragOffset += above
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_drag_indicator),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                ExerciseRowContent(item, index, actions, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ExerciseRowContent(
    item: RoutineExerciseDraftUi,
    index: Int,
    actions: RoutineFormActions,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, lineHeight = 20.sp),
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.isCustom) CustomTag()
            }
            IconButton(onClick = { actions.onRemove(index) }, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.routine_form_remove, item.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CountStepper(
                value = item.sets,
                unit = stringResource(R.string.unit_set),
                decreaseLabel = stringResource(R.string.action_decrease_sets),
                increaseLabel = stringResource(R.string.action_increase_sets),
                onStep = { actions.onStepSets(index, it) },
                modifier = Modifier.weight(1f)
            )
            val reps = item.reps
            if (reps != null) {
                CountStepper(
                    value = reps,
                    unit = stringResource(R.string.unit_reps),
                    decreaseLabel = stringResource(R.string.action_decrease_reps),
                    increaseLabel = stringResource(R.string.action_increase_reps),
                    onStep = { actions.onStepReps(index, it) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** 48 dp stepper: −, "4 set" (Archivo 18), +. */
@Composable
private fun CountStepper(
    value: Int,
    unit: String,
    decreaseLabel: String,
    increaseLabel: String,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(R.drawable.ic_remove, decreaseLabel) { onStep(-1) }
        val ink = MaterialTheme.colorScheme.onSurface
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ink)
                ) {
                    append(value.toString())
                }
                append(" ")
                append(unit)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        StepButton(R.drawable.ic_add, increaseLabel) { onStep(1) }
    }
}

@Composable
private fun StepButton(icon: Int, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 48.dp)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

/** "Hari pelaksanaan": seven 48 dp day buttons and which days the routine shows on the dashboard. */
@Composable
private fun DaysSection(weekdays: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    val shortNames = stringArrayResource(R.array.weekday_short)
    val onSelected = LiftverseTheme.colors.onPrimaryFill
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FormSectionTitle(stringResource(R.string.routine_form_days))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DayOfWeek.entries.forEach { day ->
                val selected = day in weekdays
                val labelColor = if (selected) onSelected else MaterialTheme.colorScheme.onSurface
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (selected) {
                                Modifier.background(LiftverseTheme.colors.primaryFill)
                            } else {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            }
                        )
                        .clickable(role = Role.Checkbox, onClickLabel = formatWeekday(day)) { onToggle(day) }
                        .semantics {
                            this.contentDescription = formatWeekday(day)
                            this.selected = selected
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortNames[day.value - 1],
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = labelColor
                    )
                }
            }
        }
        Text(
            text = if (weekdays.isEmpty()) {
                stringResource(R.string.routine_form_days_none)
            } else {
                stringResource(R.string.routine_form_days_text, joinDays(weekdays.sorted()))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** "Senin dan Kamis", "Senin, Rabu dan Jumat". */
@Composable
private fun joinDays(days: List<DayOfWeek>): String {
    val names = days.map { formatWeekday(it) }
    return if (names.size < 2) {
        names.joinToString("")
    } else {
        stringResource(R.string.list_and, names.dropLast(1).joinToString(", "), names.last())
    }
}

@Composable
private fun ReminderSection(form: RoutineFormUi, onToggle: () -> Unit, onPickTime: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Switch, onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.routine_form_reminder),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp)
                )
                Text(
                    text = stringResource(R.string.routine_form_reminder_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = form.reminderEnabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = LiftverseTheme.colors.primaryFill,
                    checkedThumbColor = LiftverseTheme.colors.onPrimaryFill,
                    checkedBorderColor = LiftverseTheme.colors.primaryFill,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
        if (form.reminderEnabled) {
            OutlinedValueField(label = stringResource(R.string.routine_form_reminder_time), onClick = onPickTime) {
                Icon(
                    painter = painterResource(R.drawable.ic_schedule),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = formatTime(form.reminderTime),
                    style = LiftverseTheme.numbers.setRow,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(initial: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.routine_form_reminder_time)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.action_choose))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun DeleteSection(onDelete: () -> Unit) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button, onClick = onDelete),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.routine_form_delete),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.error
            )
        }
        Text(
            text = stringResource(R.string.routine_form_delete_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val previewActions =
    RoutineFormActions({}, {}, { _, _ -> }, { _, _ -> }, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})

private val previewForm = RoutineFormUi(
    isEdit = true,
    name = "Push Day",
    exercises = listOf(
        RoutineExerciseDraftUi("seed:a", "Barbell Bench Press", false, TrackingType.WEIGHT_REPS, 4, 8),
        RoutineExerciseDraftUi("seed:b", "Overhead Press", false, TrackingType.WEIGHT_REPS, 3, 8),
        RoutineExerciseDraftUi("custom:c", "Incline DB Press (grip netral)", true, TrackingType.WEIGHT_REPS, 3, 10),
        RoutineExerciseDraftUi("seed:d", "Dips", false, TrackingType.BODYWEIGHT_REPS, 3, 12),
        RoutineExerciseDraftUi("seed:e", "Plank", false, TrackingType.DURATION, 1, null)
    ),
    weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
    reminderEnabled = true,
    reminderTime = LocalTime.of(18, 30)
)

@ScreenPreviews
@Composable
private fun RoutineFormPreview() {
    LiftversePreview {
        RoutineFormScreen(previewForm, previewActions, remember { SnackbarHostState() })
    }
}

@ScreenPreviews
@Composable
private fun RoutineFormNewPreview() {
    LiftversePreview {
        RoutineFormScreen(RoutineFormUi(), previewActions, remember { SnackbarHostState() })
    }
}
