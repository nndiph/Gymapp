package com.nndiph.liftverse.feature.routine.form

import androidx.lifecycle.SavedStateHandle
import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.data.repository.OfflineFirstExerciseRepository
import com.nndiph.liftverse.data.repository.OfflineFirstRoutineRepository
import com.nndiph.liftverse.domain.model.RoutineExercise
import com.nndiph.liftverse.testing.BENCH
import com.nndiph.liftverse.testing.DIPS
import com.nndiph.liftverse.testing.MutableClock
import com.nndiph.liftverse.testing.PLANK
import com.nndiph.liftverse.testing.inMemoryDatabase
import com.nndiph.liftverse.testing.pollUntil
import java.time.DayOfWeek
import java.time.LocalTime
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoutineFormViewModelTest {
    private val clock = MutableClock()
    private lateinit var database: LiftverseDatabase
    private lateinit var routines: OfflineFirstRoutineRepository
    private lateinit var exercises: OfflineFirstExerciseRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = inMemoryDatabase()
        routines = OfflineFirstRoutineRepository(database, clock)
        exercises = OfflineFirstExerciseRepository(database, clock)
        runTest { exercises.upsertSeedExercises(listOf(BENCH, PLANK, DIPS)) }
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun RoutineFormViewModel.awaitExercises(count: Int) = pollUntil({ form }) { it.exercises.size == count }

    private fun viewModel(routineId: String? = null) = RoutineFormViewModel(
        SavedStateHandle(routineId?.let { mapOf(ROUTINE_ID_ARG to it) } ?: emptyMap()),
        routines,
        exercises
    )

    @Test
    fun `a new routine with days is saved and shows up as a routine for those days`() = runTest {
        val viewModel = viewModel()
        assertFalse(viewModel.form.canSave)
        viewModel.onName(" Push Day ")
        viewModel.addExercise(BENCH.key)
        viewModel.awaitExercises(1)
        viewModel.addExercise(PLANK.key)
        viewModel.awaitExercises(2)
        assertEquals(listOf(3, 1), viewModel.form.exercises.map { it.sets })
        assertEquals(listOf(10, null), viewModel.form.exercises.map { it.reps })

        viewModel.stepSets(0, 1)
        viewModel.stepReps(0, -2)
        viewModel.stepReps(1, 1)
        viewModel.toggleDay(DayOfWeek.MONDAY)
        viewModel.toggleDay(DayOfWeek.THURSDAY)
        viewModel.toggleReminder()
        viewModel.setReminderTime(LocalTime.of(18, 30))
        viewModel.save()
        viewModel.closed.first()

        val saved = routines.observeRoutines().first().single()
        assertEquals("Push Day", saved.name)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), saved.weekdays)
        assertTrue(saved.reminderEnabled)
        assertEquals(LocalTime.of(18, 30), saved.reminderTime)
        assertEquals(
            listOf(
                RoutineExercise(BENCH.key, BENCH.name, targetSets = 4, targetReps = 8),
                RoutineExercise(PLANK.key, PLANK.name, targetSets = 1, targetReps = null)
            ),
            saved.exercises
        )
    }

    @Test
    fun `steppers stay at one or more, removing can be undone, rows reorder`() = runTest {
        val viewModel = viewModel()
        viewModel.addExercise(BENCH.key)
        viewModel.awaitExercises(1)
        viewModel.addExercise(DIPS.key)
        viewModel.awaitExercises(2)
        repeat(5) { viewModel.stepSets(0, -1) }
        assertEquals(1, viewModel.form.exercises[0].sets)

        viewModel.removeExercise(0)
        val removed = viewModel.removed.first()
        assertEquals(listOf(DIPS.key), viewModel.form.exercises.map { it.key })
        viewModel.undoRemove(removed)
        assertEquals(listOf(BENCH.key, DIPS.key), viewModel.form.exercises.map { it.key })

        viewModel.moveExercise(1, 0)
        assertEquals(listOf(DIPS.key, BENCH.key), viewModel.form.exercises.map { it.key })
    }

    @Test
    fun `editing loads the routine, saves changes and can delete it`() = runTest {
        val first = viewModel()
        first.onName("Leg Day")
        first.addExercise(BENCH.key)
        first.awaitExercises(1)
        first.toggleDay(DayOfWeek.WEDNESDAY)
        first.save()
        first.closed.first()
        val id = routines.observeRoutines().first().single().id

        val edit = viewModel(id)
        pollUntil({ edit.form }) { it.isLoaded }
        assertTrue(edit.form.isEdit)
        assertEquals("Leg Day", edit.form.name)
        assertEquals(setOf(DayOfWeek.WEDNESDAY), edit.form.weekdays)
        edit.toggleDay(DayOfWeek.SATURDAY)
        edit.save()
        edit.closed.first()
        assertEquals(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY), routines.getRoutine(id)!!.weekdays)

        val again = viewModel(id)
        again.delete()
        again.closed.first()
        assertEquals(emptyList<Any>(), routines.observeRoutines().first())
    }
}
