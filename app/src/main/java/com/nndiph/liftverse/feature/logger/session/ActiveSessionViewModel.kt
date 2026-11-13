package com.nndiph.liftverse.feature.logger.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nndiph.liftverse.domain.formula.defaultRestSec
import com.nndiph.liftverse.domain.formula.setsFollowingWeightChange
import com.nndiph.liftverse.domain.formula.weightStepOf
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.Session
import com.nndiph.liftverse.domain.model.SessionExercise
import com.nndiph.liftverse.domain.model.SetEntry
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.repository.ExerciseRepository
import com.nndiph.liftverse.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * State and actions of the running session (Catat tab). Every write goes to the local database and returns at
 * once; nothing here waits for the network. Value edits show immediately ([pendingValues]) and are written in
 * the order they were made, so quick repeated taps on a stepper are never lost.
 */
@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val exercises: ExerciseRepository
) : ViewModel(),
    ActiveSessionActions {
    private val selectedSetId = MutableStateFlow<String?>(null)
    private val expandedOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val contexts = MutableStateFlow<Map<String, ExerciseContext>>(emptyMap())
    private val pendingValues = MutableStateFlow<Map<String, SetValues>>(emptyMap())
    private val requestedContexts = mutableSetOf<String>()
    private var contextSessionId: String? = null
    private val writeLock = Mutex()
    private val messageChannel = Channel<SessionMessage>(Channel.BUFFERED)

    /** "Set 2 dihapus" and similar, each with what [undo] needs. */
    val messages: Flow<SessionMessage> = messageChannel.receiveAsFlow()

    private val finishedChannel = Channel<String>(Channel.BUFFERED)

    /** Ids of sessions just saved by "Selesaikan sesi", to open their summary. Discarded sessions send nothing. */
    val finishedSessions: Flow<String> = finishedChannel.receiveAsFlow()

    private val runningSession: StateFlow<SessionLoad> = sessions.observeRunningSession()
        .onEach { session ->
            if (session != null) loadContexts(session)
            dropSettledEdits(session)
        }
        .map<Session?, SessionLoad> { SessionLoad.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SessionLoad.Loading)

    val uiState: StateFlow<ActiveSessionUiState> = combine(
        runningSession,
        contexts,
        selectedSetId,
        expandedOverrides,
        pendingValues
    ) { load, contexts, selected, expanded, pending ->
        when (load) {
            SessionLoad.Loading -> ActiveSessionUiState.Loading
            is SessionLoad.Loaded ->
                load.session
                    ?.let {
                        ActiveSessionUiState.Running(
                            buildActiveSessionUi(it.withValues(pending), contexts, selected, expanded)
                        )
                    }
                    ?: ActiveSessionUiState.NoSession
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ActiveSessionUiState.Loading)

    fun startEmptySession(name: String) {
        write { sessions.startEmptySession(name) }
    }

    fun undo(message: SessionMessage) {
        write {
            when (message) {
                is SessionMessage.SetRemoved -> sessions.restoreSet(message.removed)
                is SessionMessage.ExerciseRemoved -> sessions.restoreExercise(message.removed)
            }
        }
    }

    override fun onOpenSet(setId: String) {
        selectedSetId.value = setId
    }

    override fun onToggleExpanded(exerciseId: String) {
        val exercise = currentUi()?.exercises?.firstOrNull { it.id == exerciseId } ?: return
        expandedOverrides.update { it + (exerciseId to !exercise.isExpanded) }
    }

    override fun onStep(setId: String, field: SetField, direction: Int) {
        val (exercise, set) = findSet(setId) ?: return
        val step = stepFor(field, exercise.trackingType, weightStepOf(contexts.value[exercise.exerciseKey]?.exercise))
        updateValues(exercise, set, set.values.withValue(field, set.values.valueOf(field) + direction * step))
    }

    override fun onSetValue(setId: String, field: SetField, value: Double) {
        val (exercise, set) = findSet(setId) ?: return
        updateValues(exercise, set, set.values.withValue(field, value))
    }

    override fun onToggleWarmup(setId: String) {
        val (_, set) = findSet(setId) ?: return
        write { sessions.updateSet(setId, set.values, isWarmup = !set.isWarmup) }
    }

    override fun onPrimaryAction(setId: String) {
        val (_, set) = findSet(setId) ?: return
        if (set.isCompleted) selectedSetId.value = null else complete(setId)
    }

    override fun onToggleDone(setId: String) {
        val (_, set) = findSet(setId) ?: return
        if (set.isCompleted) write { sessions.uncompleteSet(setId) } else complete(setId)
    }

    override fun onDeleteSet(setId: String) {
        val number = currentUi()?.exercises?.flatMap { it.sets }?.firstOrNull { it.id == setId }?.number
        if (selectedSetId.value == setId) selectedSetId.value = null
        write { sessions.removeSet(setId)?.let { messageChannel.send(SessionMessage.SetRemoved(number, it)) } }
    }

    override fun onAddSet(exerciseId: String) {
        write { sessions.addSet(exerciseId) }
    }

    override fun onDeleteExercise(exerciseId: String) {
        val name = currentUi()?.exercises?.firstOrNull { it.id == exerciseId }?.name ?: return
        write {
            sessions.removeExercise(exerciseId)?.let { messageChannel.send(SessionMessage.ExerciseRemoved(name, it)) }
        }
    }

    override fun onAdjustRest(deltaSec: Int) {
        val sessionId = currentSession()?.id ?: return
        write { sessions.adjustRestTarget(sessionId, deltaSec) }
    }

    override fun onSkipRest() {
        val sessionId = currentSession()?.id ?: return
        write { sessions.skipRest(sessionId) }
    }

    override fun onFinish() {
        val sessionId = currentSession()?.id ?: return
        selectedSetId.value = null
        expandedOverrides.value = emptyMap()
        write { if (sessions.finishSession(sessionId)) finishedChannel.send(sessionId) }
    }

    private fun complete(setId: String) {
        val (exercise, _) = findSet(setId) ?: return
        selectedSetId.value = null
        val restSec = defaultRestSec(exercise.trackingType, equipmentOf(exercise))
        write { sessions.completeSet(setId, restSec) }
    }

    /** Applies [newValues] and, for a load change, the same load to later unfinished sets that had the old one. */
    private fun updateValues(exercise: SessionExercise, set: SetEntry, newValues: SetValues) {
        if (newValues == set.values) return
        val followers = if (newValues.weightKg != set.values.weightKg) {
            setsFollowingWeightChange(exercise.sets, set.id, set.values.weightKg).toSet()
        } else {
            emptySet()
        }
        val updates = buildMap {
            put(set.id, newValues to set.isWarmup)
            exercise.sets.filter { it.id in followers }
                .forEach { put(it.id, it.values.copy(weightKg = newValues.weightKg) to it.isWarmup) }
        }
        pendingValues.update { pending -> pending + updates.mapValues { it.value.first } }
        write { updates.forEach { (id, update) -> sessions.updateSet(id, update.first, update.second) } }
    }

    private fun loadContexts(session: Session) {
        if (session.id != contextSessionId) {
            contextSessionId = session.id
            requestedContexts.clear()
            contexts.value = emptyMap()
        }
        session.exercises.map { it.exerciseKey }.distinct().filter { requestedContexts.add(it) }.forEach { key ->
            viewModelScope.launch {
                val context = ExerciseContext(
                    exercise = exercises.getExercise(key),
                    previousSets = sessions.previousSets(key, session.startedAt),
                    recordKg = sessions.recordBefore(key, session.startedAt)
                )
                if (contextSessionId == session.id) contexts.update { it + (key to context) }
            }
        }
    }

    /** Forgets optimistic values once the database shows them (or the set is gone). */
    private fun dropSettledEdits(session: Session?) {
        val stored = session?.exercises?.flatMap { it.sets }?.associate { it.id to it.values }.orEmpty()
        pendingValues.update { pending -> pending.filter { (id, values) -> id in stored && stored[id] != values } }
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { writeLock.withLock { block() } }
    }

    private fun currentSession(): Session? =
        (runningSession.value as? SessionLoad.Loaded)?.session?.withValues(pendingValues.value)

    private fun currentUi(): ActiveSessionUi? = (uiState.value as? ActiveSessionUiState.Running)?.session

    private fun findSet(setId: String): Pair<SessionExercise, SetEntry>? = currentSession()?.exercises
        ?.firstNotNullOfOrNull { exercise -> exercise.sets.firstOrNull { it.id == setId }?.let { exercise to it } }

    private fun equipmentOf(exercise: SessionExercise): Equipment? =
        contexts.value[exercise.exerciseKey]?.exercise?.equipment

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private sealed interface SessionLoad {
    data object Loading : SessionLoad

    data class Loaded(val session: Session?) : SessionLoad
}

private fun Session.withValues(pending: Map<String, SetValues>): Session = if (pending.isEmpty()) {
    this
} else {
    copy(
        exercises = exercises.map { exercise ->
            exercise.copy(sets = exercise.sets.map { set -> pending[set.id]?.let { set.copy(values = it) } ?: set })
        }
    )
}
