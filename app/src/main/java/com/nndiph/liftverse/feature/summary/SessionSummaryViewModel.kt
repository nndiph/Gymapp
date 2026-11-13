package com.nndiph.liftverse.feature.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nndiph.liftverse.domain.formula.STREAK_LOOKBACK_WEEKS
import com.nndiph.liftverse.domain.model.Session
import com.nndiph.liftverse.domain.repository.SessionRepository
import com.nndiph.liftverse.domain.repository.StreakScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** "Selesai sesi": reads the saved session and its history from the device only. */
@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessions: SessionRepository,
    private val schedules: StreakScheduleRepository,
    private val clock: Clock
) : ViewModel() {
    private val sessionId: String = checkNotNull(savedStateHandle[SESSION_ID_ARG]) { "Missing $SESSION_ID_ARG" }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SessionSummaryUiState> = sessions.observeSession(sessionId)
        .flatMapLatest { session -> if (session == null || session.isRunning) missing() else summaryOf(session) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SessionSummaryUiState.Loading)

    private fun missing() = flow<SessionSummaryUiState> { emit(SessionSummaryUiState.Missing) }

    private fun summaryOf(session: Session) = flow {
        val contexts = session.exercises.map { it.exerciseKey }.distinct().associateWith { key ->
            SummaryExerciseContext(
                previousSets = sessions.previousSets(key, session.startedAt),
                recordKg = sessions.recordBefore(key, session.startedAt)
            )
        }
        val dayStats = sessions.observeDayStats(session.day.minusWeeks(STREAK_LOOKBACK_WEEKS), session.day)
        emitAll(
            combine(dayStats, schedules.observeSchedule()) { stats, schedule ->
                SessionSummaryUiState.Ready(buildSessionSummaryUi(session, contexts, stats, schedule, clock.zone))
            }
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
