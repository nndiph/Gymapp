package com.nndiph.liftverse.feature.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nndiph.liftverse.R
import com.nndiph.liftverse.core.format.formatDayDate
import com.nndiph.liftverse.core.format.formatWeekday
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.core.ui.LiftversePreview
import com.nndiph.liftverse.core.ui.ScreenPreviews
import com.nndiph.liftverse.core.ui.SyncChip
import com.nndiph.liftverse.core.ui.VoltSurface
import java.time.LocalDate
import kotlinx.coroutines.launch

internal val DashboardCardShape = RoundedCornerShape(16.dp)

/** What the dashboard can ask for. Screens that are not built yet go to [onUnavailable]. */
class DashboardCallbacks(
    val onStartRoutine: (routineId: String) -> Unit,
    val onStartEmpty: () -> Unit,
    /** "Lanjutkan sesi" and "Pilih routine": the Catat tab. */
    val onOpenLogger: () -> Unit,
    val onOpenProfile: () -> Unit,
    /** The Gerakan shortcut: the exercise library. */
    val onOpenExercises: () -> Unit,
    val onUnavailable: () -> Unit
)

@Composable
fun DashboardRoute(
    onOpenLogger: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenExercises: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unavailable = stringResource(R.string.screen_unavailable)
    val sessionName = stringResource(R.string.session_default_name)
    val openLogger by rememberUpdatedState(onOpenLogger)
    val openProfile by rememberUpdatedState(onOpenProfile)
    val openExercises by rememberUpdatedState(onOpenExercises)

    LaunchedEffect(viewModel) { viewModel.sessionStarted.collect { openLogger() } }

    val callbacks = remember(viewModel, sessionName, unavailable) {
        DashboardCallbacks(
            onStartRoutine = viewModel::startRoutine,
            onStartEmpty = { viewModel.startEmptySession(sessionName) },
            onOpenLogger = { openLogger() },
            onOpenProfile = { openProfile() },
            onOpenExercises = { openExercises() },
            onUnavailable = {
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(unavailable)
                }
            }
        )
    }
    DashboardScreen(state, callbacks, snackbarHostState, modifier)
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    callbacks: DashboardCallbacks,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (state is DashboardUiState.Ready) DashboardContent(state.dashboard, callbacks)
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun DashboardContent(dashboard: DashboardUi, callbacks: DashboardCallbacks) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            DashboardTopBar()
            Spacer(Modifier.height(8.dp))
            Greeting(dashboard)
        }
        StreakCard(dashboard, onOpenSchedule = callbacks.onUnavailable)
        TodayCard(dashboard.todayCard, dashboard.isSessionRunning, callbacks)
        dashboard.weekComparison?.let { WeekComparisonCard(it, onOpenProgress = callbacks.onUnavailable) }
        Shortcuts(dashboard, onOpenHistory = callbacks.onUnavailable, onOpenExercises = callbacks.onOpenExercises)
    }
}

/** Wordmark (volt mark + "Liftverse") and the sync chip; 64 dp. */
@Composable
private fun DashboardTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_liftverse_mark),
                contentDescription = null,
                tint = LiftverseTheme.colors.accentText,
                modifier = Modifier.size(width = 36.dp, height = 22.dp)
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
        }
        SyncChip()
    }
}

@Composable
private fun Greeting(dashboard: DashboardUi) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = formatDayDate(dashboard.today),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.dashboard_greeting),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, lineHeight = 32.sp)
        )
    }
}

/** Volt card: streak, sessions this week, and the seven days of the week. */
@Composable
private fun StreakCard(dashboard: DashboardUi, onOpenSchedule: () -> Unit) {
    val number = MaterialTheme.typography.displaySmall.copy(lineHeight = 36.sp)
    VoltSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(
                            onClickLabel = stringResource(R.string.dashboard_streak_action),
                            role = Role.Button,
                            onClick = onOpenSchedule
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_fire_filled),
                            contentDescription = null,
                            tint = LiftverseTheme.colors.accentText,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = dashboard.streak.toString(),
                            style = number,
                            color = LiftverseTheme.colors.accentText
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(
                                when (dashboard.streakLabel) {
                                    StreakLabel.TRAINING_DAYS -> R.string.dashboard_streak_days
                                    StreakLabel.WEEKS -> R.string.dashboard_streak_weeks
                                    StreakLabel.NOT_STARTED -> R.string.dashboard_streak_new
                                }
                            ),
                            style = cardCaptionStyle(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(text = dashboard.weekSessions.toString(), style = number)
                    Text(
                        text = dashboard.weeklyTarget?.let { stringResource(R.string.dashboard_week_target, it) }
                            ?: stringResource(R.string.dashboard_week_sessions),
                        style = cardCaptionStyle(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val shortNames = stringArrayResource(R.array.weekday_short)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                dashboard.week.forEach { day ->
                    WeekDay(day, shortNames, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WeekDay(day: WeekDayUi, shortNames: Array<String>, modifier: Modifier = Modifier) {
    val weekday = formatWeekday(day.date.dayOfWeek)
    val description = when (day.mark) {
        DayMark.DONE -> stringResource(R.string.dashboard_day_done, weekday)
        DayMark.TODAY, DayMark.PLANNED -> stringResource(R.string.dashboard_day_planned, weekday)
        DayMark.REST, DayMark.TODAY_REST -> weekday
    }
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DayCircle(day.mark)
        Text(
            text = dayLabel(day.date, day.isToday, shortNames),
            style = dayLabelStyle(day.isToday),
            color = dayLabelColor(day.isToday),
            textAlign = TextAlign.Center
        )
    }
}

/** 32 dp day circle (SPEC: done = #1C2400 with a volt check, today = #1C2400 ring). */
@Composable
private fun DayCircle(mark: DayMark) {
    val colors = MaterialTheme.colorScheme
    val shape = Modifier.size(32.dp).clip(CircleShape)
    Box(
        modifier = when (mark) {
            DayMark.DONE -> shape.background(LiftverseTheme.colors.primaryFill)
            DayMark.REST -> shape.background(colors.surfaceContainer)
            DayMark.TODAY -> shape.border(2.dp, colors.primary, CircleShape)
            DayMark.TODAY_REST -> shape.background(colors.surfaceContainer).border(2.dp, colors.outline, CircleShape)
            DayMark.PLANNED -> shape.dashedCircle(colors.outline, 1.5.dp)
        },
        contentAlignment = Alignment.Center
    ) {
        if (mark == DayMark.DONE) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = LiftverseTheme.colors.onPrimaryFill,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun Modifier.dashedCircle(color: Color, width: Dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawCircle(
        color = color,
        radius = size.minDimension / 2 - stroke / 2,
        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
    )
}

/** Riwayat and Gerakan shortcuts (64 dp tiles). */
@Composable
private fun Shortcuts(dashboard: DashboardUi, onOpenHistory: () -> Unit, onOpenExercises: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ShortcutTile(
            icon = R.drawable.ic_history,
            title = stringResource(R.string.dashboard_history),
            subtitle = if (dashboard.historyCount > 0) {
                stringResource(R.string.dashboard_history_count, dashboard.historyCount)
            } else {
                stringResource(R.string.dashboard_history_none)
            },
            onClick = onOpenHistory,
            modifier = Modifier.weight(1f)
        )
        ShortcutTile(
            icon = R.drawable.ic_list_alt,
            title = stringResource(R.string.dashboard_exercises),
            subtitle = if (dashboard.customExerciseCount > 0) {
                stringResource(
                    R.string.dashboard_exercises_count_custom,
                    dashboard.exerciseCount,
                    dashboard.customExerciseCount
                )
            } else {
                stringResource(R.string.dashboard_exercises_count, dashboard.exerciseCount)
            },
            onClick = onOpenExercises,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ShortcutTile(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = 64.dp)
            .clip(DashboardCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Outlined surface card used by the dashboard (radius 16, 1 dp outlineVariant). */
@Composable
internal fun DashboardCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp),
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(DashboardCardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DashboardCardShape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

/** "Latihan hari ini", "Minggu ini vs minggu lalu": 13 sp semibold. */
@Composable
internal fun cardLabelStyle(): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

@Composable
internal fun cardCaptionStyle(): TextStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)

/** "Hari ini" for today, otherwise the short weekday from [shortNames] (Sen … Min). */
@Composable
internal fun dayLabel(date: LocalDate, isToday: Boolean, shortNames: Array<String>): String =
    if (isToday) stringResource(R.string.dashboard_today) else shortNames[date.dayOfWeek.value - 1]

@Composable
internal fun dayLabelColor(isToday: Boolean): Color =
    if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

/** Weekday labels under circles and bars: 11 sp, bold for today. */
@Composable
internal fun dayLabelStyle(isToday: Boolean): TextStyle = MaterialTheme.typography.labelSmall.copy(
    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
)

private val previewCallbacks = DashboardCallbacks({}, {}, {}, {}, {}, {})

@ScreenPreviews
@Composable
private fun DashboardPlannedPreview() {
    LiftversePreview {
        DashboardScreen(
            DashboardUiState.Ready(DashboardPreviewData.planned),
            previewCallbacks,
            remember { SnackbarHostState() }
        )
    }
}

@ScreenPreviews
@Composable
private fun DashboardRestDayPreview() {
    LiftversePreview {
        DashboardScreen(
            DashboardUiState.Ready(DashboardPreviewData.restDay),
            previewCallbacks,
            remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "Baru", widthDp = 412, heightDp = 915)
@Composable
private fun DashboardNewUserPreview() {
    LiftversePreview {
        DashboardScreen(
            DashboardUiState.Ready(DashboardPreviewData.newUser),
            previewCallbacks,
            remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "Selesai", widthDp = 412, heightDp = 915)
@Composable
private fun DashboardDonePreview() {
    LiftversePreview {
        DashboardScreen(
            DashboardUiState.Ready(DashboardPreviewData.done),
            previewCallbacks,
            remember { SnackbarHostState() }
        )
    }
}
