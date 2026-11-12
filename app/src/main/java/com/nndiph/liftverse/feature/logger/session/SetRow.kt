package com.nndiph.liftverse.feature.logger.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.format.formatClock
import com.nndiph.liftverse.core.format.formatDecimal
import com.nndiph.liftverse.core.format.formatKilometres
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.core.ui.LiftversePreview
import com.nndiph.liftverse.core.ui.ThemePreviews
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType

/**
 * A set that is not being edited (README "Baris set", compact row): 56 dp tall, number badge, values, tag,
 * and the tick. Tapping the row opens it as the active set; the tick completes or reopens it in one tap.
 */
@Composable
fun SetRow(
    set: SetUi,
    trackingType: TrackingType,
    onOpen: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val openLabel = set.number?.let { stringResource(R.string.action_open_set, it) }
        ?: stringResource(R.string.action_open_warmup_set)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClickLabel = openLabel, onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SetBadge(number = set.number)
        Spacer(Modifier.width(12.dp))
        SetValuesText(
            trackingType = trackingType,
            values = set.values,
            emphasized = set.isCompleted,
            modifier = Modifier.weight(1f)
        )
        if (set.isWarmup) {
            Text(
                text = stringResource(R.string.set_warmup_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        SetTagChip(tag = set.tag, modifier = Modifier.padding(end = 4.dp))
        SetCheckButton(done = set.isCompleted, onToggle = onToggleDone)
    }
}

/**
 * Values of a compact row in Archivo with small units: "80 kg  8 reps", "12 reps  +5 kg", "1:00", "2 km  15:00".
 * Done sets use onSurface, planned ones onSurfaceVariant. The first value has a fixed width so columns line up.
 */
@Composable
private fun SetValuesText(trackingType: TrackingType, values: SetValues, emphasized: Boolean, modifier: Modifier) {
    val color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val kg = stringResource(R.string.unit_kg)
    val reps = stringResource(R.string.unit_reps)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        when (trackingType) {
            TrackingType.WEIGHT_REPS -> {
                ValueWithUnit(formatDecimal(values.weightKg ?: 0.0), kg, color, Modifier.width(FIRST_VALUE_WIDTH))
                ValueWithUnit((values.reps ?: 0).toString(), reps, color)
            }
            TrackingType.BODYWEIGHT_REPS -> {
                ValueWithUnit((values.reps ?: 0).toString(), reps, color, Modifier.width(FIRST_VALUE_WIDTH))
                val added = values.weightKg ?: 0.0
                if (added > 0.0) {
                    ValueWithUnit(stringResource(R.string.value_added_weight, formatDecimal(added)), kg, color)
                } else {
                    ValueWithUnit(stringResource(R.string.value_bodyweight), unit = null, color = color)
                }
            }
            TrackingType.DURATION ->
                ValueWithUnit(formatClock((values.durationSec ?: 0).toLong()), unit = null, color = color)
            TrackingType.DISTANCE_DURATION -> {
                ValueWithUnit(
                    formatKilometres(values.distanceM ?: 0),
                    stringResource(R.string.unit_km),
                    color,
                    Modifier.width(FIRST_VALUE_WIDTH)
                )
                ValueWithUnit(formatClock((values.durationSec ?: 0).toLong()), unit = null, color = color)
            }
        }
    }
}

@Composable
private fun ValueWithUnit(value: String, unit: String?, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            style = LiftverseTheme.numbers.setRow,
            color = color,
            maxLines = 1,
            modifier = Modifier.alignByBaseline()
        )
        if (unit != null) {
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(start = 3.dp).alignByBaseline()
            )
        }
    }
}

private val FIRST_VALUE_WIDTH = 62.dp

@ThemePreviews
@Composable
private fun SetRowPreview() {
    LiftversePreview {
        Column(Modifier.padding(vertical = 8.dp)) {
            listOf(
                SessionPreviewData.warmupDone,
                SessionPreviewData.firstImproved,
                SessionPreviewData.prSet,
                SessionPreviewData.pendingSet
            ).forEach { set ->
                SetRow(set = set, trackingType = TrackingType.WEIGHT_REPS, onOpen = {}, onToggleDone = {})
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SetRowOtherTypesPreview() {
    LiftversePreview {
        Column(Modifier.padding(vertical = 8.dp)) {
            SetRow(
                set = SessionPreviewData.firstDone.copy(values = SetValues(weightKg = 0.0, reps = 12)),
                trackingType = TrackingType.BODYWEIGHT_REPS,
                onOpen = {},
                onToggleDone = {}
            )
            SetRow(
                set = SessionPreviewData.pendingSet.copy(values = SetValues(durationSec = 60)),
                trackingType = TrackingType.DURATION,
                onOpen = {},
                onToggleDone = {}
            )
            SetRow(
                set = SessionPreviewData.pendingSet.copy(values = SetValues(distanceM = 2_000, durationSec = 900)),
                trackingType = TrackingType.DISTANCE_DURATION,
                onOpen = {},
                onToggleDone = {}
            )
        }
    }
}
