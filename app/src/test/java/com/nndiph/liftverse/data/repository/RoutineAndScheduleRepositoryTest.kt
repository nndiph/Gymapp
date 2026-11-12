package com.nndiph.liftverse.data.repository

import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.data.local.entity.SyncEntityType
import com.nndiph.liftverse.domain.model.RoutineDraft
import com.nndiph.liftverse.domain.model.RoutineExercise
import com.nndiph.liftverse.domain.model.SplitPreset
import com.nndiph.liftverse.domain.model.StreakSchedule
import com.nndiph.liftverse.testing.MutableClock
import com.nndiph.liftverse.testing.inMemoryDatabase
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoutineAndScheduleRepositoryTest {
    private val clock = MutableClock()
    private lateinit var database: LiftverseDatabase
    private lateinit var routines: OfflineFirstRoutineRepository
    private lateinit var schedules: OfflineFirstStreakScheduleRepository

    private val bench =
        RoutineExercise("seed:barbell-bench-press", "Barbell Bench Press", targetSets = 4, targetReps = 8)
    private val press = RoutineExercise("seed:overhead-press", "Overhead Press", targetSets = 3, targetReps = 8)
    private val pushDay = RoutineDraft(
        name = "Push Day",
        weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
        reminderEnabled = true,
        reminderTime = LocalTime.of(18, 30),
        exercises = listOf(bench, press)
    )

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        routines = OfflineFirstRoutineRepository(database, clock)
        schedules = OfflineFirstStreakScheduleRepository(database, clock)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `updating a routine replaces its exercises in the new order`() = runTest {
        val id = routines.createRoutine(pushDay)

        routines.updateRoutine(
            id,
            pushDay.copy(
                name = "Push A",
                reminderEnabled = false,
                exercises = listOf(press.copy(targetReps = 10), bench)
            )
        )

        val routine = routines.getRoutine(id)!!
        assertEquals("Push A", routine.name)
        assertEquals(listOf(press.copy(targetReps = 10), bench), routine.exercises)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), routine.weekdays)
        // The reminder time survives switching the reminder off
        assertEquals(LocalTime.of(18, 30), routine.reminderTime)
    }

    @Test
    fun `deleting a routine hides it and queues one sync`() = runTest {
        val id = routines.createRoutine(pushDay)

        routines.deleteRoutine(id)

        assertNull(routines.getRoutine(id))
        assertTrue(routines.observeRoutines().first().isEmpty())
        assertEquals(
            listOf(SyncEntityType.ROUTINE to id),
            database.pendingSyncDao().getAll().map { it.entityType to it.entityId }
        )
    }

    @Test
    fun `schedule defaults to three sessions a week and round-trips both modes`() = runTest {
        assertEquals(StreakSchedule.WeeklyTarget(3), schedules.observeSchedule().first())

        val upperLower = StreakSchedule.FixedDays(
            days = SplitPreset.UPPER_LOWER.days!!,
            allowSwap = false,
            preset = SplitPreset.UPPER_LOWER
        )
        schedules.setSchedule(upperLower)
        assertEquals(upperLower, schedules.observeSchedule().first())

        schedules.setSchedule(StreakSchedule.WeeklyTarget(4))
        assertEquals(StreakSchedule.WeeklyTarget(4), schedules.observeSchedule().first())
        assertEquals(1, database.pendingSyncDao().getAll().size)
    }
}
