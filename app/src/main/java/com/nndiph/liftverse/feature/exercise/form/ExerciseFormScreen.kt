package com.nndiph.liftverse.feature.exercise.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.ui.FormSaveBar
import com.nndiph.liftverse.core.ui.FormSectionTitle
import com.nndiph.liftverse.core.ui.FormTopBar
import com.nndiph.liftverse.core.ui.LiftversePreview
import com.nndiph.liftverse.core.ui.OutlinedValueField
import com.nndiph.liftverse.core.ui.ScreenPreviews
import com.nndiph.liftverse.core.ui.SelectChip
import com.nndiph.liftverse.core.ui.labelRes
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.TrackingType

/** What the custom exercise form can ask for. */
class ExerciseFormActions(
    val onName: (String) -> Unit,
    val onMuscleGroup: (MuscleGroup) -> Unit,
    val onEquipment: (Equipment) -> Unit,
    val onTrackingType: (TrackingType) -> Unit,
    val onPickParent: () -> Unit,
    val onClearParent: () -> Unit,
    val onNotes: (String) -> Unit,
    val onSave: () -> Unit,
    val onArchive: () -> Unit,
    val onClose: () -> Unit
)

@Composable
fun ExerciseFormRoute(
    onClose: () -> Unit,
    onPickParent: () -> Unit,
    /** The exercise picked in "Variasi dari", until [onPickedParentHandled] clears it. */
    pickedParentKey: String?,
    onPickedParentHandled: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseFormViewModel = hiltViewModel()
) {
    val close by rememberUpdatedState(onClose)
    LaunchedEffect(viewModel) { viewModel.closed.collect { close() } }
    LaunchedEffect(pickedParentKey) {
        if (pickedParentKey != null) {
            viewModel.onParentPicked(pickedParentKey)
            onPickedParentHandled()
        }
    }
    ExerciseFormScreen(
        form = viewModel.form,
        actions = ExerciseFormActions(
            onName = viewModel::onName,
            onMuscleGroup = viewModel::onMuscleGroup,
            onEquipment = viewModel::onEquipment,
            onTrackingType = viewModel::onTrackingType,
            onPickParent = onPickParent,
            onClearParent = viewModel::onClearParent,
            onNotes = viewModel::onNotes,
            onSave = viewModel::save,
            onArchive = viewModel::archive,
            onClose = onClose
        ),
        modifier = modifier
    )
}

/** "Gerakan custom baru" / "Ubah gerakan" (README Screens `gerakan-form`, screenshots light/25–26). */
@Composable
fun ExerciseFormScreen(form: ExerciseFormUi, actions: ExerciseFormActions, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        FormTopBar(
            title = stringResource(
                if (form.isEdit) R.string.exercise_form_title_edit else R.string.exercise_form_title_new
            ),
            onClose = actions.onClose
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = actions.onName,
                label = { Text(stringResource(R.string.exercise_form_name)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            ChipSection(
                title = stringResource(R.string.exercise_form_muscle),
                options = MuscleGroup.entries,
                selected = form.muscleGroup,
                labelOf = { it.labelRes },
                onSelect = actions.onMuscleGroup
            )
            ChipSection(
                title = stringResource(R.string.picker_filter_equipment),
                options = Equipment.entries,
                selected = form.equipment,
                labelOf = { it.labelRes },
                onSelect = actions.onEquipment
            )
            TrackingTypeSection(form, actions.onTrackingType)
            ParentField(form, actions)
            OutlinedTextField(
                value = form.notes,
                onValueChange = actions.onNotes,
                label = { Text(stringResource(R.string.exercise_form_notes)) },
                placeholder = { Text(stringResource(R.string.exercise_form_notes_hint)) },
                minLines = 3,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)
            )
            if (form.isEdit) ArchiveSection(actions.onArchive)
        }
        FormSaveBar(
            text = stringResource(if (form.isEdit) R.string.action_save_changes else R.string.exercise_form_save_new),
            enabled = form.canSave,
            hint = stringResource(R.string.exercise_form_incomplete),
            onSave = actions.onSave
        )
    }
}

@Composable
private fun <T> ChipSection(title: String, options: List<T>, selected: T?, labelOf: (T) -> Int, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        FormSectionTitle(title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                SelectChip(stringResource(labelOf(option)), option == selected) { onSelect(option) }
            }
        }
    }
}

/** Four radio cards; the selected one has a 2 dp primary border. Locked once the exercise has history. */
@Composable
private fun TrackingTypeSection(form: ExerciseFormUi, onSelect: (TrackingType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormSectionTitle(stringResource(R.string.exercise_form_tracking))
        if (form.isTrackingLocked) {
            Text(
                text = stringResource(R.string.exercise_form_tracking_locked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TrackingType.entries.forEach { type ->
                TrackingCard(
                    type = type,
                    selected = form.trackingType == type,
                    enabled = !form.isTrackingLocked,
                    onClick = { onSelect(type) }
                )
            }
        }
    }
}

@Composable
private fun TrackingCard(type: TrackingType, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .border(border, shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .alpha(if (enabled || selected) 1f else DISABLED_ALPHA)
            .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled || selected,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(12.dp)
        )
        Column {
            Text(
                text = stringResource(type.labelRes),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp)
            )
            Text(
                text = stringResource(type.exampleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val TrackingType.exampleRes: Int
    get() = when (this) {
        TrackingType.WEIGHT_REPS -> R.string.tracking_example_weight_reps
        TrackingType.BODYWEIGHT_REPS -> R.string.tracking_example_bodyweight_reps
        TrackingType.DURATION -> R.string.tracking_example_duration
        TrackingType.DISTANCE_DURATION -> R.string.tracking_example_distance_duration
    }

/** "Variasi dari (opsional)": opens the picker; the clear button removes the parent. */
@Composable
private fun ParentField(form: ExerciseFormUi, actions: ExerciseFormActions) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedValueField(label = stringResource(R.string.exercise_form_parent), onClick = actions.onPickParent) {
            Text(
                text = form.parentName.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (form.parentName != null) {
                IconButton(onClick = actions.onClearParent) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.exercise_form_parent_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }
        Text(
            text = stringResource(R.string.exercise_form_parent_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun ArchiveSection(onArchive: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button, onClick = onArchive),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(painterResource(R.drawable.ic_archive), contentDescription = null)
            Text(
                text = stringResource(R.string.exercise_form_archive),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
            )
        }
        Text(
            text = stringResource(R.string.exercise_form_archive_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private const val DISABLED_ALPHA = 0.5f

private val previewActions = ExerciseFormActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {})

@ScreenPreviews
@Composable
private fun ExerciseFormPreview() {
    LiftversePreview {
        ExerciseFormScreen(
            form = ExerciseFormUi(
                name = "Incline DB Press (grip netral)",
                muscleGroup = MuscleGroup.CHEST,
                equipment = Equipment.DUMBBELL,
                parentKey = "seed:Incline_Dumbbell_Press",
                parentName = "Incline Dumbbell Press"
            ),
            actions = previewActions
        )
    }
}

@ScreenPreviews
@Composable
private fun ExerciseFormEditPreview() {
    LiftversePreview {
        ExerciseFormScreen(
            form = ExerciseFormUi(
                isEdit = true,
                name = "Incline DB Press (grip netral)",
                muscleGroup = MuscleGroup.CHEST,
                equipment = Equipment.DUMBBELL,
                isTrackingLocked = true,
                notes = "Bangku 30°, telapak tangan saling berhadapan. Siku lebih rapat ke badan."
            ),
            actions = previewActions
        )
    }
}
