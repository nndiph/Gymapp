package com.nndiph.liftverse.feature.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.format.formatClock
import com.nndiph.liftverse.core.format.formatDayDate
import com.nndiph.liftverse.core.format.formatDecimal
import com.nndiph.liftverse.core.format.formatTime
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.core.ui.LiftversePreview
import com.nndiph.liftverse.core.ui.PrimaryFillButton
import com.nndiph.liftverse.core.ui.ScreenPreviews
import com.nndiph.liftverse.core.ui.VoltSurface
import com.nndiph.liftverse.core.ui.accentEdgeColor
import com.nndiph.liftverse.feature.logger.session.improvementText
import com.nndiph.liftverse.feature.logger.session.setSummary

private val CardShape = RoundedCornerShape(16.dp)
private const val MIN_ACTIVE_FRACTION = 0.04f

/** "Selesai sesi" (README Screens `selesai`, screenshots light/dark 10). */
@Composable
fun SessionSummaryRoute(
    onOpenProfile: () -> Unit,
    onOpenDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionSummaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SessionSummaryScreen(state, onOpenProfile, onOpenDashboard, modifier)
}

@Composable
fun SessionSummaryScreen(
    state: SessionSummaryUiState,
    onOpenProfile: () -> Unit,
    onOpenDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(onClick = onOpenDashboard) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.action_close))
            }
        }
        if (state is SessionSummaryUiState.Ready) {
            SummaryContent(state.summary, Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PrimaryFillButton(
                text = stringResource(R.string.summary_open_profile),
                onClick = onOpenProfile,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
            TextButton(
                onClick = onOpenDashboard,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(R.string.summary_back_dashboard),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SummaryContent(summary: SessionSummaryUi, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryHero(summary)
        SummaryStats(summary)
        SessionTimeCard(summary)
        if (summary.improvedExercises.isNotEmpty()) ImprovedSection(summary)
        if (summary.records.isNotEmpty()) {
            RecordSection(summary.records)
        } else {
            Text(
                text = stringResource(R.string.summary_no_records),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HeatmapCard(summary)
    }
}

@Composable
private fun SummaryHero(summary: SessionSummaryUi) {
    VoltSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(
                    R.string.summary_date_range,
                    formatDayDate(summary.day),
                    formatTime(summary.startTime),
                    formatTime(summary.endTime)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.summary_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.summary_saved, summary.name),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryStats(summary: SessionSummaryUi) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, line, CardShape)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            StatCell(formatClock(summary.durationSec), stringResource(R.string.summary_duration), Modifier.weight(1f))
            VerticalDivider(color = line)
            StatCell(summary.setCount.toString(), stringResource(R.string.summary_sets), Modifier.weight(1f))
        }
        HorizontalDivider(color = line)
        Row(Modifier.height(IntrinsicSize.Min)) {
            StatCell(
                formatDecimal(summary.volumeKg, maxFractionDigits = 0),
                stringResource(R.string.summary_volume),
                Modifier.weight(1f)
            )
            VerticalDivider(color = line)
            StatCell(summary.exerciseCount.toString(), stringResource(R.string.summary_exercises), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp)) {
        Text(text = value, style = MaterialTheme.typography.headlineLarge, maxLines = 1)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** "Waktu sesi": active vs rest bar, both totals and the average rest per set (SPEC "Timer istirahat"). */
@Composable
private fun SessionTimeCard(summary: SessionSummaryUi) {
    val edge = accentEdgeColor()
    val activeFraction = if (summary.durationSec > 0) {
        (summary.activeSec.toFloat() / summary.durationSec).coerceIn(MIN_ACTIVE_FRACTION, 1f)
    } else {
        0.5f
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.summary_time_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.summary_average_rest, formatClock(summary.averageRestSec)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false).alignByBaseline()
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(Modifier.weight(activeFraction).fillMaxHeight().background(LiftverseTheme.colors.primaryFill))
            if (activeFraction < 1f) {
                Box(
                    Modifier
                        .weight(1f - activeFraction)
                        .fillMaxHeight()
                        .background(LiftverseTheme.colors.accent)
                        .border(1.dp, edge, RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeLegend(
                fill = LiftverseTheme.colors.primaryFill,
                edge = Color.Transparent,
                label = stringResource(R.string.summary_active),
                value = formatClock(summary.activeSec),
                modifier = Modifier.weight(1f)
            )
            TimeLegend(
                fill = LiftverseTheme.colors.accent,
                edge = edge,
                label = stringResource(R.string.summary_rest),
                value = formatClock(summary.restSec),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimeLegend(fill: Color, edge: Color, label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val swatch = RoundedCornerShape(2.dp)
            Box(Modifier.size(10.dp).clip(swatch).background(fill).border(1.dp, edge, swatch))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(text = value, style = LiftverseTheme.numbers.title.copy(fontWeight = FontWeight.ExtraBold))
    }
}

/** "6 set naik dari sesi lalu" with one row per exercise. */
@Composable
private fun ImprovedSection(summary: SessionSummaryUi) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(LiftverseTheme.colors.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_upward),
                    contentDescription = null,
                    tint = LiftverseTheme.colors.onAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = stringResource(R.string.summary_improved, summary.improvedSetCount),
                style = sectionTitleStyle(),
                modifier = Modifier.semantics { heading() }
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
        ) {
            summary.improvedExercises.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.summary_improved_sets, item.setCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    UpTag(improvementText(item.firstImprovement))
                }
            }
        }
    }
}

/** Volt "↑ +2,5 kg" tag (22 dp, radius 6). */
@Composable
private fun UpTag(text: String) {
    Row(
        modifier = Modifier
            .height(22.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(LiftverseTheme.colors.accent)
            .padding(start = 4.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_upward),
            contentDescription = null,
            tint = LiftverseTheme.colors.onAccent,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = LiftverseTheme.colors.onAccent
        )
    }
}

/** "PR baru": one tertiary card per exercise with a record. */
@Composable
private fun RecordSection(records: List<SummaryRecordUi>) {
    val ink = MaterialTheme.colorScheme.onTertiaryContainer
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_trophy_filled),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.summary_records),
                style = sectionTitleStyle(),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.semantics { heading() }
            )
        }
        records.forEach { record ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = record.name, style = sectionTitleStyle(), color = ink)
                    Text(
                        text = recordDetail(record),
                        style = MaterialTheme.typography.bodySmall,
                        color = ink,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    text = setSummary(record.trackingType, record.values),
                    style = LiftverseTheme.numbers.setRow.copy(fontWeight = FontWeight.ExtraBold),
                    color = ink,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun recordDetail(record: SummaryRecordUi): String {
    val estimate = formatDecimal(record.estimatedKg, maxFractionDigits = 1)
    return record.previousKg?.let {
        stringResource(R.string.summary_record_detail, estimate, formatDecimal(it, maxFractionDigits = 1))
    } ?: stringResource(R.string.summary_record_estimate, estimate)
}

/** Five weeks of the heatmap with the session's day outlined, and how the session counts towards the streak. */
@Composable
private fun HeatmapCard(summary: SessionSummaryUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val description = stringResource(R.string.summary_heatmap_description)
        MiniHeatmap(summary.heatmap, Modifier.clearAndSetSemantics { contentDescription = description })
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(R.string.summary_heatmap_title), style = sectionTitleStyle())
            Text(
                text = heatmapText(summary),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun heatmapText(summary: SessionSummaryUi): String {
    val sessions = stringResource(R.string.summary_heatmap_sessions, summary.weekSessions, summary.monthSessions)
    val streak = when (val streak = summary.streak) {
        is SummaryStreak.Days -> stringResource(R.string.summary_streak_days, streak.count)
        is SummaryStreak.TargetReached -> stringResource(R.string.summary_target_reached, streak.target, streak.weeks)
        is SummaryStreak.TargetLeft -> stringResource(R.string.summary_target_left, streak.remaining)
    }
    return "$sessions $streak"
}

/** 18 dp cells, 3 dp apart, one column per week; the session's day gets a 2 dp primary outline 1 dp outside. */
@Composable
private fun MiniHeatmap(weeks: List<List<HeatCellUi>>, modifier: Modifier = Modifier) {
    val levels = LiftverseTheme.colors.heatmap
    val outline = MaterialTheme.colorScheme.primary
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { cell ->
                    Box(
                        Modifier
                            .size(18.dp)
                            .then(if (cell.isSessionDay) Modifier.dayOutline(outline) else Modifier)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (cell.isFuture) Color.Transparent else levels[cell.level])
                    )
                }
            }
        }
    }
}

private fun Modifier.dayOutline(color: Color): Modifier = drawBehind {
    val stroke = 2.dp.toPx()
    val reach = 1.dp.toPx() + stroke / 2
    drawRoundRect(
        color = color,
        topLeft = Offset(-reach, -reach),
        size = Size(size.width + 2 * reach, size.height + 2 * reach),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(stroke)
    )
}

@Composable
private fun sectionTitleStyle() = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp)

@ScreenPreviews
@Composable
private fun SessionSummaryScreenPreview() {
    LiftversePreview {
        SessionSummaryScreen(
            state = SessionSummaryUiState.Ready(SummaryPreviewData.summary),
            onOpenProfile = {},
            onOpenDashboard = {}
        )
    }
}

@ScreenPreviews
@Composable
private fun SessionSummaryNoRecordPreview() {
    LiftversePreview {
        SessionSummaryScreen(
            state = SessionSummaryUiState.Ready(SummaryPreviewData.summary.copy(records = emptyList())),
            onOpenProfile = {},
            onOpenDashboard = {}
        )
    }
}
