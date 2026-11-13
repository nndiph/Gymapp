package com.nndiph.liftverse.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.format.formatDecimal
import com.nndiph.liftverse.core.format.formatMonth
import com.nndiph.liftverse.core.format.formatSigned
import com.nndiph.liftverse.core.format.formatSignedPercent
import com.nndiph.liftverse.core.format.formatWeekday
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.core.ui.DeltaLabel
import com.nndiph.liftverse.core.ui.Trend
import kotlin.math.roundToInt

private val BarAreaHeight = 72.dp
private const val BAR_MAX_HEIGHT_DP = 64
private const val BAR_MIN_HEIGHT_DP = 4
private const val TODAY_PLACEHOLDER_FRACTION = 0.3f
private val BarShape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)

/** "Minggu ini vs minggu lalu": volume and sets with change, paired day bars, and this month's row. */
@Composable
internal fun WeekComparisonCard(week: WeekComparisonUi, onOpenProgress: () -> Unit) {
    // The 48 dp "Progress" button reaches into the card padding (prototype: margin −10/−8/−6 around the header).
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DashboardCardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DashboardCardShape)
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.dashboard_week_title),
                style = cardLabelStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .offset(x = 8.dp)
                    .height(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button, onClick = onOpenProgress)
                    .padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_progress),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeekStat(
                value = formatDecimal(week.volumeKg, maxFractionDigits = 0),
                label = stringResource(R.string.dashboard_stat_volume),
                change = week.volumeChangePercent?.let { percent ->
                    weekChangeText(percent, formatSignedPercent(percent)) to trendOf(percent)
                },
                modifier = Modifier.weight(1f)
            )
            WeekStat(
                value = week.setCount.toString(),
                label = stringResource(R.string.dashboard_stat_sets),
                change = weekChangeText(
                    week.setChange,
                    stringResource(R.string.dashboard_delta_sets, formatSigned(week.setChange))
                ) to trendOf(week.setChange),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(16.dp))
        DayBars(week)
        Spacer(Modifier.height(12.dp))
        Legend()
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.dashboard_month_title,
                        formatMonth(week.month),
                        week.monthSessions,
                        formatDecimal(week.monthVolumeKg, maxFractionDigits = 0)
                    ),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(
                        R.string.dashboard_month_compare,
                        week.previousMonthUntil.dayOfMonth,
                        formatMonth(week.previousMonthUntil)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            week.monthChangePercent?.let { percent ->
                DeltaLabel(
                    text = if (percent ==
                        0
                    ) {
                        stringResource(R.string.dashboard_delta_same)
                    } else {
                        formatSignedPercent(percent)
                    },
                    trend = trendOf(percent),
                    style = LiftverseTheme.numbers.small,
                    iconSize = 16.dp
                )
            }
        }
    }
}

/** "+23% dari minggu lalu", or "Sama dengan minggu lalu" without change. */
@Composable
private fun weekChangeText(change: Int, signed: String): String = if (change == 0) {
    stringResource(R.string.dashboard_delta_week_same)
} else {
    stringResource(R.string.dashboard_delta_week, signed)
}

@Composable
private fun WeekStat(value: String, label: String, change: Pair<String, Trend>?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(text = value, style = LiftverseTheme.numbers.stat, maxLines = 1)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        change?.let { (text, trend) ->
            DeltaLabel(
                text = text,
                trend = trend,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** Seven day pairs: last week (outlined) and this week (filled); today without a session shows a dashed bar. */
@Composable
private fun DayBars(week: WeekComparisonUi) {
    val shortNames = stringArrayResource(R.array.weekday_short)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        week.bars.forEach { bar ->
            val description = stringResource(
                R.string.dashboard_bar_description,
                formatWeekday(bar.date.dayOfWeek),
                formatDecimal(bar.currentKg ?: 0.0, maxFractionDigits = 0),
                formatDecimal(bar.previousKg ?: 0.0, maxFractionDigits = 0)
            )
            Column(
                modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.height(BarAreaHeight),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        Modifier
                            .width(9.dp)
                            .height(barHeight(bar.previousKg, week.maxDayVolumeKg))
                            .clip(BarShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .border(1.dp, MaterialTheme.colorScheme.outline, BarShape)
                    )
                    val current = bar.currentKg
                    when {
                        current != null -> Box(
                            Modifier
                                .width(10.dp)
                                .height(barHeight(current, week.maxDayVolumeKg))
                                .clip(BarShape)
                                .background(LiftverseTheme.colors.primaryFill)
                        )
                        bar.isToday -> Box(
                            Modifier
                                .width(10.dp)
                                .height((BAR_MAX_HEIGHT_DP * TODAY_PLACEHOLDER_FRACTION).roundToInt().dp)
                                .dashedOutline(MaterialTheme.colorScheme.primary, 1.5.dp)
                        )
                        else -> Spacer(Modifier.width(10.dp))
                    }
                }
                Text(
                    text = dayLabel(bar.date, bar.isToday, shortNames),
                    style = dayLabelStyle(bar.isToday),
                    color = dayLabelColor(bar.isToday),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

private fun barHeight(kg: Double?, maxKg: Double): Dp = if (kg == null || kg <= 0.0) {
    0.dp
} else {
    (kg / maxKg * BAR_MAX_HEIGHT_DP).roundToInt().coerceAtLeast(BAR_MIN_HEIGHT_DP).dp
}

@Composable
private fun Legend() {
    val swatchShape = RoundedCornerShape(2.dp)
    val swatch = Modifier.size(10.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(stringResource(R.string.dashboard_legend_this_week)) {
            Box(swatch.clip(swatchShape).background(LiftverseTheme.colors.primaryFill))
        }
        LegendItem(stringResource(R.string.dashboard_legend_last_week)) {
            Box(
                swatch
                    .clip(swatchShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(1.dp, MaterialTheme.colorScheme.outline, swatchShape)
            )
        }
        LegendItem(stringResource(R.string.dashboard_legend_planned)) {
            Box(swatch.dashedOutline(MaterialTheme.colorScheme.primary, 1.5.dp))
        }
    }
}

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        swatch()
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Modifier.dashedOutline(color: Color, width: Dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())))
    )
}
