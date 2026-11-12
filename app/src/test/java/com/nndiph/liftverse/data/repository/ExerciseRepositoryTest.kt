package com.nndiph.liftverse.data.repository

import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.data.local.entity.SyncEntityType
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.ExerciseDraft
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.TrackingType
import com.nndiph.liftverse.testing.BENCH
import com.nndiph.liftverse.testing.MutableClock
import com.nndiph.liftverse.testing.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseRepositoryTest {
    private val clock = MutableClock()
    private lateinit var database: LiftverseDatabase
    private lateinit var exercises: OfflineFirstExerciseRepository

    private val inclinePress = ExerciseDraft(
        name = "Incline DB Press (grip netral)",
        muscleGroup = MuscleGroup.CHEST,
        equipment = Equipment.DUMBBELL,
        trackingType = TrackingType.WEIGHT_REPS,
        basedOn = "seed:incline-dumbbell-press",
        notes = "Siku 45°"
    )

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        exercises = OfflineFirstExerciseRepository(database, clock)
        exercises.upsertSeedExercises(listOf(BENCH))
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `custom exercises sit next to seed ones and are found by name, alias and chips`() = runTest {
        val key = exercises.createCustomExercise(inclinePress)
        assertTrue(key.startsWith("custom:"))

        assertEquals(
            listOf("Barbell Bench Press", "Incline DB Press (grip netral)"),
            exercises.observeExercises().first().map { it.name }
        )
        // Alias match, case-insensitive
        assertEquals(listOf(BENCH.key), exercises.observeExercises(query = " FLAT ").first().map { it.key })
        assertEquals(listOf(key), exercises.observeExercises(query = "netral").first().map { it.key })
        assertEquals(listOf(key), exercises.observeExercises(equipment = Equipment.DUMBBELL).first().map { it.key })
        assertEquals(
            listOf(BENCH.key),
            exercises.observeExercises(muscleGroup = MuscleGroup.CHEST, equipment = Equipment.BARBELL).first()
                .map { it.key }
        )
        // "%" is a literal character, not a wildcard
        assertTrue(exercises.observeExercises(query = "%").first().isEmpty())
    }

    @Test
    fun `archived custom exercises leave the picker but still resolve for history`() = runTest {
        val key = exercises.createCustomExercise(inclinePress)

        exercises.archiveCustomExercise(key)

        assertEquals(listOf(BENCH.key), exercises.observeExercises().first().map { it.key })
        assertTrue(exercises.getExercise(key)!!.archived)
        assertEquals(
            listOf(SyncEntityType.CUSTOM_EXERCISE to key),
            database.pendingSyncDao().getAll().map { it.entityType to it.entityId }
        )
    }

    @Test
    fun `tracking type locks once a session uses the exercise`() = runTest {
        val key = exercises.createCustomExercise(inclinePress)
        assertFalse(exercises.isTrackingTypeLocked(key))
        // No history yet, so the type may still change
        exercises.updateCustomExercise(key, inclinePress.copy(trackingType = TrackingType.BODYWEIGHT_REPS))

        val sessions = OfflineFirstSessionRepository(database, clock)
        sessions.addExercise(sessions.startEmptySession("Sesi bebas"), key)

        assertTrue(exercises.isTrackingTypeLocked(key))
        val error = runCatching { exercises.updateCustomExercise(key, inclinePress) }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        // Other fields stay editable
        exercises.updateCustomExercise(
            key,
            inclinePress.copy(name = "Incline DB", trackingType = TrackingType.BODYWEIGHT_REPS)
        )
        assertEquals("Incline DB", exercises.getExercise(key)!!.name)
    }
}
