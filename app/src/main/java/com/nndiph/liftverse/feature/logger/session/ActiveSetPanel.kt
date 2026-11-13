package com.nndiph.liftverse.feature.logger.session

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.core.ui.LiftversePreview
import com.nndiph.liftverse.core.ui.PrimaryFillButton
import com.nndiph.liftverse.core.ui.ThemePreviews
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType

/**
 * The one editable set of a session (README "Baris set"): header with the previous session and the live
 * achievement chip, steppers (tap the number for the numpad), then delete, warm-up and "Selesai set".
 */
@Composable
fun ActiveSetPanel(
    set: SetUi,
    trackingType: TrackingType,
    weightStepKg: Double,
    onStep: (SetField, Int) -> Unit,
    onEdit: (SetField) -> Unit,
    onToggleWarmup: () -> Unit,
    onDelete: () -> Unit,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = setTitle(set),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = previousLabel(trackingType, set.previous),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AchievementChip(achievement = set.achievement)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fieldsFor(trackingType).forEach { field ->
                SetStepper(
                    field = field,
                    trackingType = trackingType,
                    values = set.values,
                    weightStepKg = weightStepKg,
                    onStep = { direction -> onStep(field, direction) },
                    onEdit = { onEdit(field) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DeleteSetButton(set = set, onDelete = onDelete)
            WarmupToggle(isWarmup = set.isWarmup, onToggle = onToggleWarmup)
            PrimaryFillButton(
                text = stringResource(
                    if (set.isCompleted) R.string.action_save_changes else R.string.action_complete_set
                ),
                onClick = onPrimaryAction,
                icon = R.drawable.ic_check,
                modifier = Modifier.weight(1f).height(56.dp)
            )
        }
    }
}

/** 76 dp stepper on surface: − value + . Tapping the value opens the numpad (load, reps, distance). */
@Composable
private fun SetStepper(
    field: SetField,
    trackingType: TrackingType,
    values: SetValues,
    weightStepKg: Double,
    onStep: (Int) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = fieldName(field, trackingType)
    val editLabel = stringResource(R.string.action_edit_value, name)
    Row(
        modifier = modifier
            .height(76.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton(R.drawable.ic_remove, stringResource(R.string.action_decrease, name)) { onStep(-1) }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (field.hasNumpad) Modifier.clickable(onClickLabel = editLabel, onClick = onEdit) else Modifier
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = fieldValueText(field, trackingType, values),
                style = LiftverseTheme.numbers.stepper,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = fieldCaption(field, trackingType, values, weightStepKg),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        StepperButton(R.drawable.ic_add, stringResource(R.string.action_increase, name)) { onStep(1) }
    }
}

@Composable
private fun StepperButton(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(48.dp)
            .fillMaxHeight()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DeleteSetButton(set: SetUi, onDelete: () -> Unit) {
    val description = set.number?.let { stringResource(R.string.action_delete_set, it) }
        ?: stringResource(R.string.action_delete_warmup_set)
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onDelete)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(R.drawable.ic_delete), contentDescription = null, tint = MaterialTheme.colorScheme.error)
    }
}

/** "+ Warm-up" outlined; when on, primaryContainer with a check (prototype `wBg`). */
@Composable
private fun WarmupToggle(isWarmup: Boolean, onToggle: () -> Unit) {
    val container = if (isWarmup) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val content = if (isWarmup) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .height(56.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .then(
                if (isWarmup) {
                    Modifier
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                }
            )
            .toggleable(value = isWarmup, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(if (isWarmup) R.drawable.ic_check else R.drawable.ic_add),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.action_warmup),
            style = MaterialTheme.typography.titleSmall,
            color = content
        )
    }
}

@ThemePreviews
@Composable
private fun ActiveSetPanelPreview() {
    LiftversePreview {
        Column(Modifier.padding(vertical = 8.dp)) {
            PanelPreview(SessionPreviewData.activeSet)
            PanelPreview(SessionPreviewData.activeImproved)
            PanelPreview(SessionPreviewData.activePrCandidate, weightStepKg = 2.0)
        }
    }
}

@ThemePreviews
@Composable
private fun ActiveSetPanelOtherTypesPreview() {
    LiftversePreview {
        Column(Modifier.padding(vertical = 8.dp)) {
            PanelPreview(
                SessionPreviewData.activeSet.copy(values = SetValues(weightKg = 0.0, reps = 12), isCompleted = true),
                trackingType = TrackingType.BODYWEIGHT_REPS
            )
            PanelPreview(
                SessionPreviewData.activeSet.copy(values = SetValues(durationSec = 60), previous = null),
                trackingType = TrackingType.DURATION
            )
            PanelPreview(
                SessionPreviewData.activeSet.copy(
                    values = SetValues(distanceM = 2_000, durationSec = 900),
                    isWarmup = true,
                    number = null
                ),
                trackingType = TrackingType.DISTANCE_DURATION
            )
        }
    }
}

@Composable
private fun PanelPreview(
    set: SetUi,
    trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    weightStepKg: Double = 2.5
) {
    ActiveSetPanel(
        set = set,
        trackingType = trackingType,
        weightStepKg = weightStepKg,
        onStep = { _, _ -> },
        onEdit = {},
        onToggleWarmup = {},
        onDelete = {},
        onPrimaryAction = {},
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}
