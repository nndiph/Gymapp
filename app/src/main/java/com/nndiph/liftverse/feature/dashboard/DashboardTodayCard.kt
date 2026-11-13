package com.nndiph.liftverse.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.format.formatClock
import com.nndiph.liftverse.core.format.formatDayDate
import com.nndiph.liftverse.core.format.formatDayDatePlain
import com.nndiph.liftverse.core.format.formatDecimal
import com.nndiph.liftverse.core.format.formatSignedPercent
import com.nndiph.liftverse.core.format.formatTime
import com.nndiph.liftverse.core.format.formatWeekday
import com.nndiph.liftverse.core.theme.Archivo
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.core.ui.CustomTag
import com.nndiph.liftverse.core.ui.DeltaLabel
import com.nndiph.liftverse.core.ui.PrimaryFillButton
import com.nndiph.liftverse.core.ui.Sparkline
import com.nndiph.liftverse.core.ui.Trend
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType
import com.nndiph.liftverse.feature.logger.session.setSummary

/** "Latihan hari ini" in its four forms: today's routine, rest day, no routine yet, and done. */
@Composable
internal fun TodayCard(card: TodayCardUi, isSessionRunning: Boolean, callbacks: DashboardCallbacks) {
    when (card) {
        is TodayCardUi.Planned -> PlannedCard(card, isSessionRunning, callbacks)
        is TodayCardUi.Rest -> StartCard(
            title = stringResource(R.string.dashboard_rest_title),
            body = stringResource(R.string.dashboard_rest_body),
            secondaryLabel = stringResource(R.string.dashboard_choose_routine),
            onSecondary = callbacks.onOpenLogger,
            isSessionRunning = isSessionRunning,
            callbacks = callbacks
        ) { card.next?.let { NextWorkoutRow(it) } }
        TodayCardUi.NoRoutine -> StartCard(
            title = stringResource(R.string.dashboard_new_title),
            body = stringResource(R.string.dashboard_new_body),
            secondaryLabel = stringResource(R.string.dashboard_create_routine),
            onSecondary = callbacks.onUnavailable,
            isSessionRunning = isSessionRunning,
            callbacks = callbacks
        )
        is TodayCardUi.Done -> DoneCard(card, callbacks.onOpenProfile)
    }
}

@Composable
private fun PlannedCard(card: TodayCardUi.Planned, isSessionRunning: Boolean, callbacks: DashboardCallbacks) {
    DashboardCard(spacing = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardLabel()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_queue_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = card.weekdays.joinToString(" · ") { formatWeekday(it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Column {
            Text(
                text = card.name,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(
                    R.string.dashboard_routine_meta,
                    card.exerciseCount,
                    card.setCount,
                    card.estimatedMinutes
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ColumnLabel(stringResource(R.string.dashboard_column_target))
                ColumnLabel(stringResource(R.string.dashboard_column_volume))
            }
            card.exercises.forEach { exercise ->
                PlannedExerciseRow(exercise)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            val footer = listOfNotNull(
                card.moreExercises.takeIf { it > 0 }?.let { stringResource(R.string.dashboard_more_exercises, it) },
                card.lastDone?.let { stringResource(R.string.dashboard_last_done, formatDayDate(it)) }
            )
            if (footer.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = footer.joinToString(" · "),
                        style = cardCaptionStyle(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        PrimaryFillButton(
            text = stringResource(if (isSessionRunning) R.string.dashboard_continue else R.string.dashboard_start),
            onClick = { if (isSessionRunning) callbacks.onOpenLogger() else callbacks.onStartRoutine(card.routineId) },
            icon = R.drawable.ic_play_filled,
            iconSize = 24.dp,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
    }
}

@Composable
private fun ColumnLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** One exercise of today's routine: target and last session, volume sparkline, change vs last month. */
@Composable
private fun PlannedExerciseRow(exercise: PlannedExerciseUi) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (exercise.isCustom) CustomTag()
            }
            Text(
                text = targetLine(exercise),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box(Modifier.size(width = 56.dp, height = 24.dp)) {
            if (exercise.recentVolumesKg.isNotEmpty()) Sparkline(exercise.recentVolumesKg, Modifier.fillMaxSize())
        }
        Box(Modifier.widthIn(min = 50.dp), contentAlignment = Alignment.CenterEnd) {
            exercise.monthChangePercent?.let { percent ->
                DeltaLabel(
                    text = if (percent ==
                        0
                    ) {
                        stringResource(R.string.dashboard_delta_same)
                    } else {
                        formatSignedPercent(percent)
                    },
                    trend = trendOf(percent),
                    style = LiftverseTheme.numbers.small.copy(fontSize = 14.sp, lineHeight = 18.sp)
                )
            }
        }
    }
}

/** "**4 × 8** · lalu 80 kg × 8, 8, 7" (target in Archivo, the rest plain). */
@Composable
private fun targetLine(exercise: PlannedExerciseUi): AnnotatedString {
    val reps = exercise.targetReps
    val hasReps = exercise.trackingType == TrackingType.WEIGHT_REPS ||
        exercise.trackingType == TrackingType.BODYWEIGHT_REPS
    val target = if (hasReps && reps != null) {
        stringResource(R.string.dashboard_target_sets_reps, exercise.targetSets, reps)
    } else {
        stringResource(R.string.dashboard_target_sets, exercise.targetSets)
    }
    val last = if (exercise.previousSets.isEmpty()) {
        stringResource(R.string.dashboard_last_none)
    } else {
        stringResource(R.string.dashboard_last, previousSetsText(exercise.trackingType, exercise.previousSets))
    }
    val ink = MaterialTheme.colorScheme.onSurface
    return buildAnnotatedString {
        withStyle(SpanStyle(fontFamily = Archivo, fontWeight = FontWeight.Bold, color = ink)) { append(target) }
        append(" · ")
        append(last)
    }
}

/** "80 kg × 8, 8, 7" when the load stayed the same, otherwise "80×8, 82,5×6"; timed sets as "1:00, 0:45". */
@Composable
private fun previousSetsText(trackingType: TrackingType, sets: List<SetValues>): String {
    val reps = sets.joinToString(", ") { (it.reps ?: 0).toString() }
    val weights = sets.map { it.weightKg ?: 0.0 }.distinct()
    return when {
        trackingType == TrackingType.WEIGHT_REPS && weights.size == 1 ->
            stringResource(R.string.dashboard_last_weight, formatDecimal(weights.single()), reps)
        trackingType == TrackingType.BODYWEIGHT_REPS && weights.size == 1 -> if (weights.single() > 0.0) {
            stringResource(R.string.dashboard_last_added_weight, formatDecimal(weights.single()), reps)
        } else {
            stringResource(R.string.dashboard_last_bodyweight, reps)
        }
        trackingType == TrackingType.DURATION -> sets.joinToString(", ") { formatClock((it.durationSec ?: 0).toLong()) }
        else -> sets.map { setSummary(trackingType, it, short = true) }.joinToString(", ")
    }
}

/** Rest day ("Tidak ada jadwal") and no-routine ("Belum ada routine") cards: start empty + one other action. */
@Composable
private fun StartCard(
    title: String,
    body: String,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    isSessionRunning: Boolean,
    callbacks: DashboardCallbacks,
    extra: @Composable () -> Unit = {}
) {
    DashboardCard {
        CardLabel()
        Text(text = title, style = LiftverseTheme.numbers.stat, modifier = Modifier.semantics { heading() })
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        extra()
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isSessionRunning) {
                PrimaryFillButton(
                    text = stringResource(R.string.dashboard_continue),
                    onClick = callbacks.onOpenLogger,
                    icon = R.drawable.ic_play_filled,
                    iconSize = 24.dp,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )
            } else {
                PrimaryFillButton(
                    text = stringResource(R.string.logger_start_empty),
                    onClick = callbacks.onStartEmpty,
                    icon = R.drawable.ic_add,
                    iconSize = 24.dp,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )
            }
            SecondaryButton(secondaryLabel, onSecondary)
        }
    }
}

/** "Berikutnya **Push Day**, Senin 16 November · 18.30". */
@Composable
private fun NextWorkoutRow(next: NextWorkoutUi) {
    val sentence = stringResource(R.string.dashboard_next, next.routineName, formatDayDatePlain(next.date))
    val full = next.time?.let { stringResource(R.string.dashboard_next_time, sentence, formatTime(it)) } ?: sentence
    val nameStart = full.indexOf(next.routineName)
    val text = buildAnnotatedString {
        append(full)
        if (nameStart >= 0) {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), nameStart, nameStart + next.routineName.length)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_event_upcoming),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(text = text, style = cardCaptionStyle())
    }
}

@Composable
private fun DoneCard(card: TodayCardUi.Done, onOpenProfile: () -> Unit) {
    DashboardCard {
        CardLabel()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(LiftverseTheme.colors.primaryFill),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = LiftverseTheme.colors.onPrimaryFill,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = stringResource(R.string.dashboard_done_title, card.name),
                style = LiftverseTheme.numbers.stat,
                modifier = Modifier.semantics { heading() }
            )
        }
        Text(
            text = stringResource(
                R.string.dashboard_done_meta,
                card.setCount,
                formatDecimal(card.volumeKg, maxFractionDigits = 0),
                card.minutes
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SecondaryButton(stringResource(R.string.dashboard_done_profile), onOpenProfile)
    }
}

@Composable
private fun CardLabel() {
    Text(
        text = stringResource(R.string.dashboard_today_title),
        style = cardLabelStyle(),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** 48 dp outlined button (radius 12, 1 dp outline). */
@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall)
    }
}

internal fun trendOf(change: Int): Trend = when {
    change > 0 -> Trend.UP
    change < 0 -> Trend.DOWN
    else -> Trend.SAME
}
