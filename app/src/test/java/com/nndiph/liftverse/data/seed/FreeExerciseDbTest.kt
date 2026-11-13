package com.nndiph.liftverse.data.seed

import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.TrackingType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for org.json, which is only a stub in plain JVM tests.
@RunWith(RobolectricTestRunner::class)
class FreeExerciseDbTest {
    private fun exercise(
        name: String = "Barbell Curl",
        category: String = "strength",
        equipment: String? = "barbell",
        force: String? = "pull",
        muscles: List<String> = listOf("biceps")
    ) = FreeExercise("Some_Id", name, category, equipment, force, muscles)

    @Test
    fun `muscles map to the design filters`() {
        val cases = mapOf(
            "chest" to MuscleGroup.CHEST,
            "lats" to MuscleGroup.BACK,
            "middle back" to MuscleGroup.BACK,
            "lower back" to MuscleGroup.BACK,
            "traps" to MuscleGroup.BACK,
            "shoulders" to MuscleGroup.SHOULDERS,
            "neck" to MuscleGroup.SHOULDERS,
            "quadriceps" to MuscleGroup.LEGS,
            "hamstrings" to MuscleGroup.LEGS,
            "glutes" to MuscleGroup.LEGS,
            "calves" to MuscleGroup.LEGS,
            "adductors" to MuscleGroup.LEGS,
            "abductors" to MuscleGroup.LEGS,
            "biceps" to MuscleGroup.ARMS,
            "triceps" to MuscleGroup.ARMS,
            "forearms" to MuscleGroup.ARMS,
            "abdominals" to MuscleGroup.CORE
        )
        cases.forEach { (muscle, group) ->
            assertEquals(muscle, group, exercise(muscles = listOf(muscle)).toSeedExercise()!!.muscleGroup)
        }
        val cardio = exercise(category = "cardio", equipment = "machine", muscles = listOf("quadriceps"))
        assertEquals(MuscleGroup.CARDIO, cardio.toSeedExercise()!!.muscleGroup)
    }

    @Test
    fun `equipment maps to the six chips with the owner's load steps`() {
        val cases = listOf(
            Triple("barbell", Equipment.BARBELL, 2.5),
            Triple("e-z curl bar", Equipment.BARBELL, 2.5),
            Triple("dumbbell", Equipment.DUMBBELL, 2.0),
            Triple("kettlebells", Equipment.DUMBBELL, 4.0),
            Triple("machine", Equipment.MACHINE, 2.5),
            Triple("cable", Equipment.CABLE, 2.5),
            Triple("bands", Equipment.CABLE, 2.5)
        )
        cases.forEach { (raw, chip, step) ->
            val seeded = exercise(equipment = raw).toSeedExercise()!!
            assertEquals(raw, chip, seeded.equipment)
            assertEquals(raw, step, seeded.weightStepKg!!, 0.0)
        }
        assertEquals(Equipment.BODYWEIGHT, seedEquipment("body only"))
        assertEquals(Equipment.BODYWEIGHT, seedEquipment(null))
        listOf("medicine ball", "exercise ball", "foam roll", "other").forEach {
            assertEquals(it, Equipment.OTHER, seedEquipment(it))
            assertNull(it, seedWeightStepKg(it))
        }
    }

    @Test
    fun `tracking type follows category, equipment and static holds`() {
        assertEquals(TrackingType.WEIGHT_REPS, seedTrackingType(exercise()))
        assertEquals(TrackingType.DISTANCE_DURATION, seedTrackingType(exercise(category = "cardio")))
        assertEquals(TrackingType.BODYWEIGHT_REPS, seedTrackingType(exercise(equipment = "body only")))
        assertEquals(TrackingType.BODYWEIGHT_REPS, seedTrackingType(exercise(equipment = null)))
        assertEquals(
            TrackingType.DURATION,
            seedTrackingType(exercise(name = "Plank", equipment = "body only", force = "static"))
        )
        assertEquals(
            TrackingType.DURATION,
            seedTrackingType(exercise(name = "Wall Sit", equipment = null, force = null))
        )
        // A pushing movement that ends in a plank is still done for reps.
        assertEquals(
            TrackingType.BODYWEIGHT_REPS,
            seedTrackingType(exercise(name = "Push Up to Side Plank", equipment = "body only", force = "push"))
        )
    }

    @Test
    fun `stretching and strongman are left out`() {
        assertNull(exercise(category = "stretching").toSeedExercise())
        assertNull(exercise(category = "strongman").toSeedExercise())
        assertEquals("seed:Some_Id", exercise(category = "plyometrics").toSeedExercise()!!.key)
    }

    @Test
    fun `the bundled file imports completely`() {
        val json = File("src/main/assets/seed/exercises.json").readText()
        val raw = parseFreeExercises(json)
        assertEquals(876, raw.size)
        val seeded = seedExercisesFrom(json)
        // 876 minus 123 stretching and 21 strongman; every other entry maps.
        assertEquals(732, seeded.size)
        assertEquals(seeded.size, seeded.map { it.key }.toSet().size)
        assertTrue(seeded.all { it.key.startsWith("seed:") && !it.isCustom })
        assertEquals(MuscleGroup.entries.toSet(), seeded.map { it.muscleGroup }.toSet())
        assertEquals(Equipment.entries.toSet(), seeded.map { it.equipment }.toSet())

        val byName = seeded.associateBy { it.name }
        assertEquals(TrackingType.DURATION, byName.getValue("Plank").trackingType)
        assertEquals(TrackingType.BODYWEIGHT_REPS, byName.getValue("Push Up to Side Plank").trackingType)
        assertEquals(TrackingType.DISTANCE_DURATION, byName.getValue("Running, Treadmill").trackingType)
        assertEquals(TrackingType.WEIGHT_REPS, byName.getValue("Barbell Bench Press - Medium Grip").trackingType)
        assertEquals(14, seeded.count { it.trackingType == TrackingType.DISTANCE_DURATION })
        assertTrue(seeded.filter { it.weightStepKg == 4.0 }.all { it.equipment == Equipment.DUMBBELL })
    }
}
