package com.nndiph.liftverse.feature.exercise.form

import androidx.lifecycle.SavedStateHandle
import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.data.repository.OfflineFirstExerciseRepository
import com.nndiph.liftverse.data.repository.OfflineFirstSessionRepository
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.ExerciseDraft
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.TrackingType
import com.nndiph.liftverse.testing.BENCH
import com.nndiph.liftverse.testing.MutableClock
import com.nndiph.liftverse.testing.inMemoryDatabase
import com.nndiph.liftverse.testing.pollUntil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExerciseFormViewModelTest {
    private val clock = MutableClock()
    private lateinit var database: LiftverseDatabase
    private lateinit var exercises: OfflineFirstExerciseRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = inMemoryDatabase()
        exercises = OfflineFirstExerciseRepository(database, clock)
        runTest { exercises.upsertSeedExercises(listOf(BENCH)) }
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(vararg args: Pair<String, String>) =
        ExerciseFormViewModel(SavedStateHandle(mapOf(*args)), exercises)

    @Test
    fun `a new exercise from search starts with the search text and needs muscle and equipment`() = runTest {
        val viewModel = viewModel(SUGGESTED_NAME_ARG to "Zercher squat")
        assertEquals("Zercher squat", viewModel.form.name)
        assertFalse(viewModel.form.canSave)

        viewModel.onMuscleGroup(MuscleGroup.LEGS)
        viewModel.onEquipment(Equipment.BARBELL)
        viewModel.onParentPicked(BENCH.key)
        pollUntil({ viewModel.form }) { it.parentKey == BENCH.key }
        viewModel.onNotes("  Bar di siku  ")
        assertTrue(viewModel.form.canSave)
        viewModel.save()
        viewModel.closed.first()

        val saved = exercises.observeExercises(query = "zercher").first().single()
        assertTrue(saved.isCustom)
        assertEquals(MuscleGroup.LEGS, saved.muscleGroup)
        assertEquals(TrackingType.WEIGHT_REPS, saved.trackingType)
        assertEquals(BENCH.key, saved.basedOn)
        assertEquals("Bar di siku", saved.notes)
    }

    @Test
    fun `editing loads the exercise and locks the tracking type once sessions use it`() = runTest {
        val key = exercises.createCustomExercise(
            ExerciseDraft("Incline DB Press", MuscleGroup.CHEST, Equipment.DUMBBELL, TrackingType.WEIGHT_REPS)
        )
        val sessions = OfflineFirstSessionRepository(database, clock)
        sessions.addExercise(sessions.startEmptySession("Sesi bebas"), key)

        val viewModel = viewModel(EXERCISE_KEY_ARG to key)
        pollUntil({ viewModel.form }) { it.isLoaded }
        assertTrue(viewModel.form.isEdit)
        assertEquals("Incline DB Press", viewModel.form.name)
        assertTrue(viewModel.form.isTrackingLocked)
        viewModel.onTrackingType(TrackingType.DURATION)
        assertEquals(TrackingType.WEIGHT_REPS, viewModel.form.trackingType)

        // An exercise cannot be a variation of itself.
        viewModel.onParentPicked(key)
        assertNull(viewModel.form.parentKey)

        viewModel.onName("Incline DB Press (grip netral)")
        viewModel.save()
        viewModel.closed.first()
        assertEquals("Incline DB Press (grip netral)", exercises.getExercise(key)!!.name)
    }

    @Test
    fun `archiving hides the exercise from the picker`() = runTest {
        val key = exercises.createCustomExercise(
            ExerciseDraft("Zercher squat", MuscleGroup.LEGS, Equipment.BARBELL, TrackingType.WEIGHT_REPS)
        )
        val viewModel = viewModel(EXERCISE_KEY_ARG to key)
        viewModel.archive()
        viewModel.closed.first()
        assertEquals(listOf(BENCH.key), exercises.observeExercises().first().map { it.key })
        assertTrue(exercises.getExercise(key)!!.archived)
    }
}
