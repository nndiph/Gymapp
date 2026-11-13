package com.nndiph.liftverse.feature.logger.session

import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.data.repository.OfflineFirstExerciseRepository
import com.nndiph.liftverse.data.repository.OfflineFirstSessionRepository
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.testing.BENCH
import com.nndiph.liftverse.testing.MutableClock
import com.nndiph.liftverse.testing.PLANK
import com.nndiph.liftverse.testing.inMemoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
class ActiveSessionViewModelTest {
    private val clock = MutableClock()
    private lateinit var database: LiftverseDatabase
    private lateinit var sessions: OfflineFirstSessionRepository
    private lateinit var viewModel: ActiveSessionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = inMemoryDatabase()
        sessions = OfflineFirstSessionRepository(database, clock)
        val exercises = OfflineFirstExerciseRepository(database, clock)
        viewModel = ActiveSessionViewModel(sessions, exercises)
        runTest { exercises.upsertSeedExercises(listOf(BENCH, PLANK)) }
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `a load step also moves the later open sets that had the same load`() = runTest {
        val sessionId = startBenchSession()
        val first = awaitSession { it.exercises.isNotEmpty() }.exercises.single().sets.first()

        viewModel.onStep(first.id, SetField.WEIGHT, direction = 1)

        // Three default sets of 20 kg: all of them follow the first one
        val shown = awaitSession { it.exercises.single().sets.first().values.weightKg == 22.5 }
        assertEquals(listOf(22.5, 22.5, 22.5), shown.exercises.single().sets.map { it.values.weightKg })
        val stored = sessions.observeSession(sessionId)
            .first { session -> session!!.exercises.single().sets.all { it.values.weightKg == 22.5 } }
        assertEquals(3, stored!!.exercises.single().sets.size)
    }

    @Test
    fun `ticking a barbell set starts a 2·30 rest and moves on to the next set`() = runTest {
        startBenchSession()
        val loaded = awaitSession { it.exercises.singleOrNull()?.equipment == Equipment.BARBELL }
        val (first, second) = loaded.exercises.single().sets

        viewModel.onToggleDone(first.id)

        val after = awaitSession { it.rest != null }
        assertEquals(150, after.rest!!.targetSec)
        assertEquals(second.id, after.activeSetId)
    }

    @Test
    fun `deleting a set offers undo, and undo puts it back`() = runTest {
        startBenchSession()
        val second = awaitSession { it.exercises.isNotEmpty() }.exercises.single().sets[1]

        viewModel.onDeleteSet(second.id)
        val message = viewModel.messages.first() as SessionMessage.SetRemoved
        assertEquals(2, message.setNumber)
        awaitSession { it.exercises.single().sets.size == 2 }

        viewModel.undo(message)
        assertEquals(second.id, awaitSession { it.exercises.single().sets.size == 3 }.exercises.single().sets[1].id)
    }

    @Test
    fun `an exercise picked in Tambah gerakan is added and opened at its first set`() = runTest {
        startBenchSession()
        awaitSession { it.exercises.isNotEmpty() }

        viewModel.addExercise(PLANK.key)

        val shown = awaitSession { it.exercises.size == 2 && it.activeSetId == it.exercises[1].sets.first().id }
        assertEquals(listOf(false, true), shown.exercises.map { it.isExpanded })
        assertEquals("Plank", shown.exercises[1].name)
    }

    private suspend fun startBenchSession(): String {
        val sessionId = sessions.startEmptySession("Sesi bebas")
        sessions.addExercise(sessionId, BENCH.key)
        return sessionId
    }

    private suspend fun awaitSession(predicate: (ActiveSessionUi) -> Boolean): ActiveSessionUi = viewModel.uiState
        .filterIsInstance<ActiveSessionUiState.Running>()
        .map { it.session }
        .first(predicate)
}
