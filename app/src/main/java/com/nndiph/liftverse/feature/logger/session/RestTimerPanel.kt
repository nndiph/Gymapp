package com.nndiph.liftverse.feature.logger.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.format.formatClock
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.core.ui.LiftversePreview
import com.nndiph.liftverse.core.ui.ThemePreviews
import com.nndiph.liftverse.domain.model.RestTimer
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

/**
 * Rest timer (README "Timer istirahat"): volt panel with the time left, −15 / +15 / Lewati, and a 5 dp bar of the
 * time already rested. Past the target it shows the overtime ("+0:12") and vibrates twice. Time comes from the
 * stored start, so it stays right after the app was closed.
 */
@Composable
fun RestTimerPanel(
    rest: RestTimer,
    now: Instant,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elapsed = Duration.between(rest.startedAt, now).seconds.coerceAtLeast(0)
    val remaining = rest.targetSec - elapsed
    val isOver = remaining < 0
    val progress = if (rest.targetSec <= 0) 1f else (elapsed.toFloat() / rest.targetSec).coerceIn(0f, 1f)
    VibrateWhenOver(rest, isOver)

    val content = LiftverseTheme.colors.onAccent
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(LiftverseTheme.colors.accent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(if (isOver) R.drawable.ic_notifications_active else R.drawable.ic_timer),
                contentDescription = null,
                tint = content
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOver) {
                        stringResource(R.string.rest_over)
                    } else {
                        stringResource(R.string.rest_label, formatClock(rest.targetSec.toLong()))
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = content,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                Text(
                    text = if (isOver) {
                        stringResource(R.string.rest_overtime, formatClock(-remaining))
                    } else {
                        formatClock(remaining)
                    },
                    style = LiftverseTheme.numbers.stepper,
                    color = content
                )
            }
            RestButton(
                label = stringResource(R.string.rest_minus, REST_ADJUST_SEC),
                description = stringResource(R.string.rest_minus_description, REST_ADJUST_SEC),
                color = content,
                onClick = onMinus
            )
            RestButton(
                label = stringResource(R.string.rest_plus, REST_ADJUST_SEC),
                description = stringResource(R.string.rest_plus_description, REST_ADJUST_SEC),
                color = content,
                onClick = onPlus
            )
            RestButton(
                label = stringResource(R.string.rest_skip),
                description = stringResource(R.string.rest_skip_description),
                color = content,
                onClick = onSkip
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(content.copy(alpha = TRACK_ALPHA))
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }
        ) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(content))
        }
    }
}

@Composable
private fun RestButton(label: String, description: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

/** Two short vibrations when the target passes while the screen is open, not when it opens already over. */
@Composable
private fun VibrateWhenOver(rest: RestTimer, isOver: Boolean) {
    val haptic = LocalHapticFeedback.current
    val overWhenShown = remember(rest.startedAt) { isOver }
    LaunchedEffect(rest.startedAt, isOver) {
        if (isOver && !overWhenShown) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(VIBRATION_GAP_MS)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

/** Track of the progress bar: onAccent at 18 % over volt, as measured on the design screenshots. */
private const val TRACK_ALPHA = 0.18f
private const val VIBRATION_GAP_MS = 180L

@ThemePreviews
@Composable
private fun RestTimerPanelPreview() {
    LiftversePreview {
        Column(Modifier.padding(16.dp)) {
            RestTimerPanel(
                rest = SessionPreviewData.restRunning,
                now = SessionPreviewData.now,
                onMinus = {},
                onPlus = {},
                onSkip = {}
            )
            Spacer(Modifier.size(12.dp))
            RestTimerPanel(
                rest = SessionPreviewData.restOver,
                now = SessionPreviewData.now,
                onMinus = {},
                onPlus = {},
                onSkip = {}
            )
        }
    }
}
