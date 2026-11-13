package com.nndiph.liftverse.feature.exercise.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.ExerciseDraft
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.TrackingType
import com.nndiph.liftverse.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The fields of "Gerakan custom baru" / "Ubah gerakan". */
data class ExerciseFormUi(
    val isEdit: Boolean = false,
    /** False while an existing exercise is being read. */
    val isLoaded: Boolean = true,
    val name: String = "",
    val muscleGroup: MuscleGroup? = null,
    val equipment: Equipment? = null,
    val trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    /** Sessions already use the exercise, so its tracking type cannot change (docs/DATA_MODEL.md §2.2). */
    val isTrackingLocked: Boolean = false,
    val parentKey: String? = null,
    val parentName: String? = null,
    val notes: String = ""
) {
    val canSave: Boolean get() = isLoaded && name.isNotBlank() && muscleGroup != null && equipment != null
}

/**
 * Custom exercise form (README Screens `gerakan-form`). Writes go to the device database only. The form state is
 * Compose state so the text fields never lag behind typing.
 */
@HiltViewModel
class ExerciseFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exercises: ExerciseRepository
) : ViewModel() {
    private val editKey: String? = savedStateHandle.get<String>(EXERCISE_KEY_ARG)?.takeIf { it.isNotEmpty() }
    private val closeChannel = Channel<Unit>(Channel.BUFFERED)

    /** Saved or archived: the form closes. */
    val closed: Flow<Unit> = closeChannel.receiveAsFlow()

    var form by mutableStateOf(
        ExerciseFormUi(
            isEdit = editKey != null,
            isLoaded = editKey == null,
            name = savedStateHandle.get<String>(SUGGESTED_NAME_ARG).orEmpty()
        )
    )
        private set

    init {
        if (editKey != null) load(editKey)
    }

    fun onName(name: String) {
        form = form.copy(name = name)
    }

    fun onMuscleGroup(group: MuscleGroup) {
        form = form.copy(muscleGroup = group)
    }

    fun onEquipment(equipment: Equipment) {
        form = form.copy(equipment = equipment)
    }

    fun onTrackingType(type: TrackingType) {
        if (!form.isTrackingLocked) form = form.copy(trackingType = type)
    }

    fun onNotes(notes: String) {
        form = form.copy(notes = notes)
    }

    /** "Variasi dari": the exercise picked as parent; an exercise cannot be a variation of itself. */
    fun onParentPicked(key: String) {
        if (key == editKey) return
        viewModelScope.launch {
            val parent = exercises.getExercise(key) ?: return@launch
            form = form.copy(parentKey = parent.key, parentName = parent.name)
        }
    }

    fun onClearParent() {
        form = form.copy(parentKey = null, parentName = null)
    }

    fun save() {
        val current = form
        val muscleGroup = current.muscleGroup ?: return
        val equipment = current.equipment ?: return
        if (!current.canSave) return
        val draft = ExerciseDraft(
            name = current.name.trim(),
            muscleGroup = muscleGroup,
            equipment = equipment,
            trackingType = current.trackingType,
            basedOn = current.parentKey,
            notes = current.notes.trim().ifEmpty { null }
        )
        viewModelScope.launch {
            if (editKey ==
                null
            ) {
                exercises.createCustomExercise(draft)
            } else {
                exercises.updateCustomExercise(editKey, draft)
            }
            closeChannel.send(Unit)
        }
    }

    fun archive() {
        val key = editKey ?: return
        viewModelScope.launch {
            exercises.archiveCustomExercise(key)
            closeChannel.send(Unit)
        }
    }

    private fun load(key: String) {
        viewModelScope.launch {
            val exercise = exercises.getExercise(key) ?: return@launch
            val parent = exercise.basedOn?.let { exercises.getExercise(it) }
            form = form.copy(
                isLoaded = true,
                name = exercise.name,
                muscleGroup = exercise.muscleGroup,
                equipment = exercise.equipment,
                trackingType = exercise.trackingType,
                isTrackingLocked = exercises.isTrackingTypeLocked(key),
                parentKey = parent?.key,
                parentName = parent?.name,
                notes = exercise.notes.orEmpty()
            )
        }
    }
}
