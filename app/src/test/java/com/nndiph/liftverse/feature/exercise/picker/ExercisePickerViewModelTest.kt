package com.nndiph.liftverse.feature.exercise.picker

import androidx.lifecycle.SavedStateHandle
import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.data.repository.OfflineFirstExerciseRepository
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.ExerciseDraft
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.TrackingType
import com.nndiph.liftverse.testing.BENCH
import com.nndiph.liftverse.testing.DIPS
import com.nndiph.liftverse.testing.MutableClock
import com.nndiph.liftverse.testing.PLANK
import com.nndiph.liftverse.testing.inMemoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExercisePickerViewModelTest {
    private lateinit var database: LiftverseDatabase
    private lateinit var viewModel: ExercisePickerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = inMemoryDatabase()
        val exercises = OfflineFirstExerciseRepository(database, MutableClock())
        runTest {
            exercises.upsertSeedExercises(listOf(BENCH, PLANK, DIPS))
            exercises.createCustomExercise(
                ExerciseDraft(
                    "Bulgarian Split Squat (DB)",
                    MuscleGroup.LEGS,
                    Equipment.DUMBBELL,
                    TrackingType.WEIGHT_REPS
                )
            )
        }
        viewModel = ExercisePickerViewModel(SavedStateHandle(mapOf(PICKER_MODE_ARG to "ADD")), exercises)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun awaitItems(predicate: (List<PickerItemUi>) -> Boolean): ExercisePickerUi =
        viewModel.uiState.first { it.items != null && predicate(it.items!!) }

    @Test
    fun `built-in and custom exercises share one list sorted by name`() = runTest {
        val state = awaitItems { it.size == 4 }
        assertEquals(PickerMode.ADD, state.mode)
        assertEquals(
            listOf("Barbell Bench Press", "Bulgarian Split Squat (DB)", "Dips", "Plank"),
            state.items!!.map { it.name }
        )
        assertEquals(1, state.customCount)
    }

    @Test
    fun `chips filter one per row and a second tap clears them`() = runTest {
        viewModel.onMuscleGroup(MuscleGroup.CHEST)
        assertEquals(listOf("Barbell Bench Press", "Dips"), awaitItems { it.size == 2 }.items!!.map { it.name })

        viewModel.onEquipment(Equipment.BODYWEIGHT)
        assertEquals(listOf("Dips"), awaitItems { it.size == 1 }.items!!.map { it.name })

        viewModel.onMuscleGroup(MuscleGroup.CHEST)
        val cleared = awaitItems { it.size == 2 }
        assertEquals(null, cleared.filter.muscleGroup)
        assertEquals(listOf("Dips", "Plank"), cleared.items!!.map { it.name })
    }

    @Test
    fun `no results keep the search text for the create button`() = runTest {
        viewModel.onQueryChange("zercher squat")
        val state = awaitItems { it.isEmpty() }
        assertEquals("zercher squat", state.filter.query)
        assertEquals("Zercher squat", createLabelName(state.filter.query))
    }
}
