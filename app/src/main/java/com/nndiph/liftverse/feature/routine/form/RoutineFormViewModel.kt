package com.nndiph.liftverse.feature.routine.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nndiph.liftverse.domain.model.RoutineDraft
import com.nndiph.liftverse.domain.model.RoutineExercise
import com.nndiph.liftverse.domain.model.TrackingType
import com.nndiph.liftverse.domain.repository.ExerciseRepository
import com.nndiph.liftverse.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** One exercise of the routine being edited. [reps] is null for timed and distance exercises. */
data class RoutineExerciseDraftUi(
    val key: String,
    val name: String,
    val isCustom: Boolean,
    val trackingType: TrackingType,
    val sets: Int,
    val reps: Int?
)

data class RoutineFormUi(
    val isEdit: Boolean = false,
    val isLoaded: Boolean = true,
    val name: String = "",
    val exercises: List<RoutineExerciseDraftUi> = emptyList(),
    val weekdays: Set<DayOfWeek> = emptySet(),
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = DEFAULT_REMINDER_TIME
) {
    val totalSets: Int get() = exercises.sumOf { it.sets }
    val canSave: Boolean get() = isLoaded && name.isNotBlank() && exercises.isNotEmpty()
}

/** An exercise removed from the draft, kept for "Urungkan". */
data class RemovedDraftExercise(val index: Int, val exercise: RoutineExerciseDraftUi)

/**
 * Routine form (README Screens `routine-form`). Edits a draft in memory; "Simpan" writes it to the device
 * database. Reminder fields are stored now; the notification itself comes with the reminder work (M5).
 */
@HiltViewModel
class RoutineFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routines: RoutineRepository,
    private val exercises: ExerciseRepository
) : ViewModel() {
    private val routineId: String? = savedStateHandle.get<String>(ROUTINE_ID_ARG)?.takeIf { it.isNotEmpty() }
    private val closeChannel = Channel<Unit>(Channel.BUFFERED)
    private val removedChannel = Channel<RemovedDraftExercise>(Channel.BUFFERED)

    val closed: Flow<Unit> = closeChannel.receiveAsFlow()

    /** "Lateral Raise dihapus dari routine", with what [undoRemove] needs. */
    val removed: Flow<RemovedDraftExercise> = removedChannel.receiveAsFlow()

    var form by mutableStateOf(RoutineFormUi(isEdit = routineId != null, isLoaded = routineId == null))
        private set

    init {
        if (routineId != null) load(routineId)
    }

    fun onName(name: String) {
        form = form.copy(name = name)
    }

    /** "Tambah gerakan": appended with 3 × 10, or one set for timed and distance exercises. */
    fun addExercise(key: String) {
        viewModelScope.launch {
            val exercise = exercises.getExercise(key) ?: return@launch
            val counted = exercise.trackingType.hasReps
            form = form.copy(
                exercises = form.exercises + RoutineExerciseDraftUi(
                    key = exercise.key,
                    name = exercise.name,
                    isCustom = exercise.isCustom,
                    trackingType = exercise.trackingType,
                    sets = if (counted) DEFAULT_SETS else 1,
                    reps = if (counted) DEFAULT_REPS else null
                )
            )
        }
    }

    fun stepSets(index: Int, delta: Int) = updateAt(index) { it.copy(sets = (it.sets + delta).coerceIn(1, MAX_SETS)) }

    fun stepReps(index: Int, delta: Int) =
        updateAt(index) { item -> item.copy(reps = item.reps?.let { (it + delta).coerceIn(1, MAX_REPS) }) }

    fun removeExercise(index: Int) {
        val item = form.exercises.getOrNull(index) ?: return
        form = form.copy(exercises = form.exercises.filterIndexed { i, _ -> i != index })
        viewModelScope.launch { removedChannel.send(RemovedDraftExercise(index, item)) }
    }

    fun undoRemove(removed: RemovedDraftExercise) {
        val list = form.exercises.toMutableList()
        list.add(removed.index.coerceAtMost(list.size), removed.exercise)
        form = form.copy(exercises = list)
    }

    fun moveExercise(from: Int, to: Int) {
        val list = form.exercises.toMutableList()
        if (from !in list.indices || to !in list.indices || from == to) return
        list.add(to, list.removeAt(from))
        form = form.copy(exercises = list)
    }

    fun toggleDay(day: DayOfWeek) {
        val days = form.weekdays
        form = form.copy(weekdays = if (day in days) days - day else days + day)
    }

    fun toggleReminder() {
        form = form.copy(reminderEnabled = !form.reminderEnabled)
    }

    fun setReminderTime(time: LocalTime) {
        form = form.copy(reminderTime = time)
    }

    fun save() {
        val current = form
        if (!current.canSave) return
        val draft = RoutineDraft(
            name = current.name.trim(),
            weekdays = current.weekdays,
            reminderEnabled = current.reminderEnabled,
            reminderTime = current.reminderTime,
            exercises = current.exercises.map { RoutineExercise(it.key, it.name, it.sets, it.reps) }
        )
        viewModelScope.launch {
            if (routineId == null) routines.createRoutine(draft) else routines.updateRoutine(routineId, draft)
            closeChannel.send(Unit)
        }
    }

    /** "Hapus routine": sessions started from it keep their history. */
    fun delete() {
        val id = routineId ?: return
        viewModelScope.launch {
            routines.deleteRoutine(id)
            closeChannel.send(Unit)
        }
    }

    private fun updateAt(index: Int, change: (RoutineExerciseDraftUi) -> RoutineExerciseDraftUi) {
        if (index !in form.exercises.indices) return
        form = form.copy(exercises = form.exercises.mapIndexed { i, item -> if (i == index) change(item) else item })
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val routine = routines.getRoutine(id) ?: return@launch
            val items = routine.exercises.map { item ->
                val exercise = exercises.getExercise(item.exerciseKey)
                RoutineExerciseDraftUi(
                    key = item.exerciseKey,
                    name = exercise?.name ?: item.nameSnapshot,
                    isCustom = exercise?.isCustom ?: false,
                    trackingType = exercise?.trackingType ?: TrackingType.WEIGHT_REPS,
                    sets = item.targetSets,
                    reps = item.targetReps
                )
            }
            form = form.copy(
                isLoaded = true,
                name = routine.name,
                exercises = items,
                weekdays = routine.weekdays,
                reminderEnabled = routine.reminderEnabled,
                reminderTime = routine.reminderTime ?: DEFAULT_REMINDER_TIME
            )
        }
    }

    private companion object {
        const val DEFAULT_SETS = 3
        const val DEFAULT_REPS = 10
        const val MAX_SETS = 20
        const val MAX_REPS = 100
    }
}

/** 18.00, the prototype's default for a new routine. */
val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(18, 0)

private val TrackingType.hasReps: Boolean
    get() = this == TrackingType.WEIGHT_REPS || this == TrackingType.BODYWEIGHT_REPS
