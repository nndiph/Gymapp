package com.nndiph.liftverse.feature.exercise.picker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.Exercise
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.TrackingType
import com.nndiph.liftverse.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** How the picker was opened: to add an exercise (session, routine) or to browse the library. */
enum class PickerMode { ADD, BROWSE }

/** The filter state the user controls. */
data class PickerFilter(val query: String = "", val muscleGroup: MuscleGroup? = null, val equipment: Equipment? = null)

data class ExercisePickerUi(
    val mode: PickerMode,
    val filter: PickerFilter,
    /** Null until the library has loaded once. */
    val items: List<PickerItemUi>?
) {
    val customCount: Int get() = items.orEmpty().count { it.isCustom }
}

data class PickerItemUi(
    val key: String,
    val name: String,
    val isCustom: Boolean,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
    val trackingType: TrackingType
)

/**
 * "Pemilih gerakan": one list of built-in and custom exercises, filtered by search text and the Otot and Alat
 * chips (one chip per row; tapping it again clears it). Reads the device library only.
 */
@HiltViewModel
class ExercisePickerViewModel @Inject constructor(savedStateHandle: SavedStateHandle, exercises: ExerciseRepository) :
    ViewModel() {
    private val mode = savedStateHandle.get<String>(PICKER_MODE_ARG)?.let(PickerMode::valueOf) ?: PickerMode.ADD
    private val filter = MutableStateFlow(PickerFilter())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val items = filter.flatMapLatest { f ->
        exercises.observeExercises(f.query, f.muscleGroup, f.equipment).map { list -> list.map { it.toItem() } }
    }

    val uiState: StateFlow<ExercisePickerUi> = combine(filter, items) { f, list -> ExercisePickerUi(mode, f, list) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            ExercisePickerUi(mode, PickerFilter(), items = null)
        )

    fun onQueryChange(query: String) = filter.update { it.copy(query = query) }

    fun onMuscleGroup(group: MuscleGroup) =
        filter.update { it.copy(muscleGroup = group.takeUnless { selected -> selected == it.muscleGroup }) }

    fun onEquipment(equipment: Equipment) =
        filter.update { it.copy(equipment = equipment.takeUnless { selected -> selected == it.equipment }) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private fun Exercise.toItem() = PickerItemUi(key, name, isCustom, muscleGroup, equipment, trackingType)

/** "Buat “Zercher squat”": the search text with its first letter capitalised. */
fun createLabelName(query: String): String = query.trim().replaceFirstChar { it.uppercaseChar() }
