package com.nndiph.liftverse.feature.exercise.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.core.ui.CustomTag
import com.nndiph.liftverse.core.ui.LiftversePreview
import com.nndiph.liftverse.core.ui.ScreenPreviews
import com.nndiph.liftverse.core.ui.SelectChip
import com.nndiph.liftverse.core.ui.labelRes
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.TrackingType

/** What the picker can ask for. */
class PickerCallbacks(
    val onBack: () -> Unit,
    /** ADD mode: the exercise to add. BROWSE mode: the exercise to open. */
    val onPick: (exerciseKey: String) -> Unit,
    /** "Buat gerakan custom", with the search text as the suggested name ("" when there is none). */
    val onCreate: (suggestedName: String) -> Unit
)

@Composable
fun ExercisePickerRoute(
    callbacks: PickerCallbacks,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: ExercisePickerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisePickerScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onMuscleGroup = viewModel::onMuscleGroup,
        onEquipment = viewModel::onEquipment,
        callbacks = callbacks,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

/** "Pemilih gerakan" (README Screens `pemilih`, screenshots light/23–24). */
@Composable
fun ExercisePickerScreen(
    state: ExercisePickerUi,
    onQueryChange: (String) -> Unit,
    onMuscleGroup: (MuscleGroup) -> Unit,
    onEquipment: (Equipment) -> Unit,
    callbacks: PickerCallbacks,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val items = state.items
    val noResults = items != null && items.isEmpty()
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // The create bar stays above the keyboard while searching.
        Column(Modifier.fillMaxSize().imePadding()) {
            PickerHeader(state, onQueryChange, onMuscleGroup, onEquipment, callbacks.onBack)
            Box(Modifier.weight(1f)) {
                when {
                    items == null -> Unit
                    noResults -> EmptyResults(state.filter.query)
                    else -> ResultList(state, items, callbacks.onPick)
                }
            }
            if (state.mode != PickerMode.SELECT) {
                CreateBar(
                    query = state.filter.query,
                    prominent = noResults,
                    onCreate = { callbacks.onCreate(createLabelName(state.filter.query)) }
                )
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }
}

@Composable
private fun PickerHeader(
    state: ExercisePickerUi,
    onQueryChange: (String) -> Unit,
    onMuscleGroup: (MuscleGroup) -> Unit,
    onEquipment: (Equipment) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back)
                )
            }
            Text(
                text = stringResource(
                    when (state.mode) {
                        PickerMode.ADD -> R.string.action_add_exercise
                        PickerMode.SELECT -> R.string.picker_title_parent
                        PickerMode.BROWSE -> R.string.picker_title_browse
                    }
                ),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                modifier = Modifier.semantics { heading() }
            )
        }
        SearchField(state.filter.query, onQueryChange)
        Spacer(Modifier.height(4.dp))
        ChipRow(
            label = stringResource(R.string.picker_filter_muscle),
            options = MuscleGroup.entries,
            selected = state.filter.muscleGroup,
            labelOf = { it.labelRes },
            onToggle = onMuscleGroup
        )
        ChipRow(
            label = stringResource(R.string.picker_filter_equipment),
            options = Equipment.entries,
            selected = state.filter.equipment,
            labelOf = { it.labelRes },
            onToggle = onEquipment
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** 52 dp pill with a search icon and a clear button once something is typed. */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val hint = stringResource(R.string.picker_search_hint)
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Search
            ),
            modifier = Modifier.weight(1f).semantics { contentDescription = hint },
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    field()
                }
            }
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.picker_clear_search),
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Spacer(Modifier.width(44.dp).height(44.dp))
        }
    }
}

/** "Otot" / "Alat" row of single-choice chips; tapping the selected chip clears it. */
@Composable
private fun <T> ChipRow(label: String, options: List<T>, selected: T?, labelOf: (T) -> Int, onToggle: (T) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp)
        )
        options.forEach { option ->
            SelectChip(stringResource(labelOf(option)), option == selected) { onToggle(option) }
        }
    }
}

@Composable
private fun ResultList(state: ExercisePickerUi, items: List<PickerItemUi>, onPick: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp)) {
        item(key = "count") {
            Text(
                text = if (state.customCount > 0) {
                    stringResource(R.string.picker_count_custom, items.size, state.customCount)
                } else {
                    stringResource(R.string.picker_count, items.size)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            )
        }
        items(items, key = { it.key }) { item -> PickerRow(item, state.mode, onPick) }
    }
}

@Composable
private fun PickerRow(item: PickerItemUi, mode: PickerMode, onPick: (String) -> Unit) {
    val actionLabel = stringResource(
        when (mode) {
            PickerMode.ADD -> R.string.picker_add_label
            PickerMode.SELECT -> R.string.action_choose
            PickerMode.BROWSE -> R.string.picker_open_label
        }
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 64 dp content plus 8 dp padding above and below, as the design screenshot measures.
                .heightIn(min = 80.dp)
                .clickable(onClickLabel = actionLabel, role = Role.Button) { onPick(item.key) }
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Row(
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
                Text(
                    text = stringResource(
                        R.string.picker_item_meta,
                        stringResource(item.muscleGroup.labelRes),
                        stringResource(item.equipment.labelRes),
                        stringResource(item.trackingType.labelRes).lowercase()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(
                        if (mode == PickerMode.BROWSE) R.drawable.ic_chevron_right else R.drawable.ic_add
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun EmptyResults(query: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_search_off), contentDescription = null, modifier = Modifier.size(32.dp))
        }
        Text(
            text = stringResource(R.string.picker_empty_title, query.trim()),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.picker_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Sticky "Buat gerakan custom" (secondary) under the list. Without results it becomes the main button and carries
 * the search text: "Buat “Zercher squat”".
 */
@Composable
private fun CreateBar(query: String, prominent: Boolean, onCreate: () -> Unit) {
    val named = prominent && query.isNotBlank()
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Button(
            onClick = onCreate,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = if (prominent) {
                ButtonDefaults.buttonColors(
                    containerColor = LiftverseTheme.colors.primaryFill,
                    contentColor = LiftverseTheme.colors.onPrimaryFill
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            },
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (named) {
                    stringResource(R.string.picker_create_named, createLabelName(query))
                } else {
                    stringResource(R.string.picker_create)
                },
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

private val previewItems = listOf(
    PickerItemUi("seed:a", "Back Squat", false, MuscleGroup.LEGS, Equipment.BARBELL, TrackingType.WEIGHT_REPS),
    PickerItemUi(
        "seed:b",
        "Barbell Bench Press",
        false,
        MuscleGroup.CHEST,
        Equipment.BARBELL,
        TrackingType.WEIGHT_REPS
    ),
    PickerItemUi("seed:c", "Barbell Row", false, MuscleGroup.BACK, Equipment.BARBELL, TrackingType.WEIGHT_REPS),
    PickerItemUi(
        "custom:d",
        "Bulgarian Split Squat (DB)",
        true,
        MuscleGroup.LEGS,
        Equipment.DUMBBELL,
        TrackingType.WEIGHT_REPS
    ),
    PickerItemUi("seed:e", "Cable Fly", false, MuscleGroup.CHEST, Equipment.CABLE, TrackingType.WEIGHT_REPS),
    PickerItemUi("seed:f", "Plank", false, MuscleGroup.CORE, Equipment.BODYWEIGHT, TrackingType.DURATION)
)

@ScreenPreviews
@Composable
private fun ExercisePickerPreview() {
    LiftversePreview {
        ExercisePickerScreen(
            state = ExercisePickerUi(PickerMode.ADD, PickerFilter(), previewItems),
            onQueryChange = {},
            onMuscleGroup = {},
            onEquipment = {},
            callbacks = PickerCallbacks({}, {}, {}),
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@ScreenPreviews
@Composable
private fun ExercisePickerEmptyPreview() {
    LiftversePreview {
        ExercisePickerScreen(
            state = ExercisePickerUi(
                PickerMode.ADD,
                PickerFilter(query = "zercher squat", muscleGroup = MuscleGroup.LEGS),
                emptyList()
            ),
            onQueryChange = {},
            onMuscleGroup = {},
            onEquipment = {},
            callbacks = PickerCallbacks({}, {}, {}),
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
