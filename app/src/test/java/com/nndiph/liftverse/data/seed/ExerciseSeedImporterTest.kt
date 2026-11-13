package com.nndiph.liftverse.data.seed

import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.data.repository.OfflineFirstExerciseRepository
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.ExerciseDraft
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.TrackingType
import com.nndiph.liftverse.testing.MutableClock
import com.nndiph.liftverse.testing.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ExerciseSeedImporterTest {
    private lateinit var database: LiftverseDatabase
    private lateinit var exercises: OfflineFirstExerciseRepository
    private lateinit var importer: ExerciseSeedImporter

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        exercises = OfflineFirstExerciseRepository(database, MutableClock())
        importer = ExerciseSeedImporter(RuntimeEnvironment.getApplication(), exercises)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `first launch imports the library once, next to custom exercises`() = runTest {
        val customKey = exercises.createCustomExercise(
            ExerciseDraft(
                "Incline DB Press (grip netral)",
                MuscleGroup.CHEST,
                Equipment.DUMBBELL,
                TrackingType.WEIGHT_REPS
            )
        )
        importer.importIfNeeded()
        val library = exercises.observeExercises().first()
        assertEquals(733, library.size)
        assertEquals(1, library.count { it.isCustom })

        // The stored version skips a second import, so a renamed row stays as it is.
        database.exerciseDao().upsert(database.exerciseDao().get("seed:Plank")!!.copy(name = "Renamed"))
        importer.importIfNeeded()
        assertEquals("Renamed", exercises.getExercise("seed:Plank")!!.name)

        val kettlebell = library.first { it.weightStepKg == 4.0 }
        assertEquals(Equipment.DUMBBELL, kettlebell.equipment)
        // Custom exercises keep no own step and use their equipment's default.
        assertNull(exercises.getExercise(customKey)!!.weightStepKg)
    }

    @Test
    fun `search covers built-in and custom exercises with the design filters`() = runTest {
        importer.importIfNeeded()
        val bench = exercises.observeExercises(query = "bench press", equipment = Equipment.BARBELL).first()
        assertEquals(true, bench.any { it.name == "Barbell Bench Press - Medium Grip" })
        val cardio = exercises.observeExercises(muscleGroup = MuscleGroup.CARDIO).first()
        assertEquals(14, cardio.size)
    }
}
