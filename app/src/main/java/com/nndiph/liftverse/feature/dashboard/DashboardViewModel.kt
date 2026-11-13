package com.nndiph.liftverse.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nndiph.liftverse.domain.formula.STREAK_LOOKBACK_WEEKS
import com.nndiph.liftverse.domain.model.DayStats
import com.nndiph.liftverse.domain.model.Exercise
import com.nndiph.liftverse.domain.model.Routine
import com.nndiph.liftverse.domain.model.SessionSummary
import com.nndiph.liftverse.domain.model.StreakSchedule
import com.nndiph.liftverse.domain.repository.ExerciseRepository
import com.nndiph.liftverse.domain.repository.RoutineRepository
import com.nndiph.liftverse.domain.repository.SessionRepository
import com.nndiph.liftverse.domain.repository.StreakScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Dashboard: everything comes from the device, so it works offline and never waits for the network. */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessions: SessionRepository,
    routines: RoutineRepository,
    schedules: StreakScheduleRepository,
    private val exercises: ExerciseRepository,
    private val clock: Clock
) : ViewModel() {
    private val today: LocalDate = LocalDate.now(clock)
    private val startedChannel = Channel<Unit>(Channel.BUFFERED)

    /** A session was started from here; the Catat tab should open. */
    val sessionStarted: Flow<Unit> = startedChannel.receiveAsFlow()

    private val activity = combine(
        sessions.observeDayStats(today.minusWeeks(STREAK_LOOKBACK_WEEKS), today),
        sessions.observeRunningSession().map { it != null }.distinctUntilChanged(),
        sessions.observeHistory(limit = 1).map { it.firstOrNull() },
        sessions.observeFinishedSessionCount()
    ) { stats, running, latest, count -> Activity(stats, running, latest, count) }

    private val library = exercises.observeExercises().map { list -> list.size to list.count { it.isCustom } }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        activity,
        routines.observeRoutines(),
        schedules.observeSchedule(),
        library
    ) { activity, routines, schedule, library -> Inputs(activity, routines, schedule, library) }
        .mapLatest { inputs -> DashboardUiState.Ready(build(inputs)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DashboardUiState.Loading)

    fun startRoutine(routineId: String) {
        viewModelScope.launch {
            sessions.startRoutineSession(routineId)
            startedChannel.send(Unit)
        }
    }

    fun startEmptySession(name: String) {
        viewModelScope.launch {
            sessions.startEmptySession(name)
            startedChannel.send(Unit)
        }
    }

    private suspend fun build(inputs: Inputs): DashboardUi {
        val routine = todayRoutine(inputs.routines, today)
        val routineExercises = routine?.exercises.orEmpty()
        val resolved = routineExercises.map { it.exerciseKey }.distinct()
            .mapNotNull { key -> exercises.getExercise(key)?.let { key to it } }
            .toMap<String, Exercise>()
        val history = routineExercises.take(DASHBOARD_EXERCISE_ROWS).map { it.exerciseKey }.distinct()
            .associateWith { key ->
                ExerciseHistory(
                    previousSets = sessions.previousSets(key, clock.instant()),
                    volumes = sessions.exerciseVolumes(key, VOLUME_HISTORY_SESSIONS)
                )
            }
        return buildDashboardUi(
            today = today,
            schedule = inputs.schedule,
            dayStats = inputs.activity.dayStats,
            routines = inputs.routines,
            isSessionRunning = inputs.activity.isSessionRunning,
            latestSession = inputs.activity.latestSession,
            historyCount = inputs.activity.finishedCount,
            exerciseCount = inputs.library.first,
            customExerciseCount = inputs.library.second,
            exercises = resolved,
            history = history,
            lastRoutineDay = routine?.let { sessions.lastSessionDay(it.id) }
        )
    }

    private data class Activity(
        val dayStats: List<DayStats>,
        val isSessionRunning: Boolean,
        val latestSession: SessionSummary?,
        val finishedCount: Int
    )

    private data class Inputs(
        val activity: Activity,
        val routines: List<Routine>,
        val schedule: StreakSchedule,
        val library: Pair<Int, Int>
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        /** Enough sessions of one exercise to cover this month and last month. */
        const val VOLUME_HISTORY_SESSIONS = 62
    }
}
