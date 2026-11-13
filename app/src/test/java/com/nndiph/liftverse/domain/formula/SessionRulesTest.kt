package com.nndiph.liftverse.domain.formula

import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.Exercise
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.SetEntry
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionRulesTest {
    @Test
    fun `steps follow the README`() {
        assertEquals(2.5, weightStepKg(Equipment.BARBELL), 0.0)
        assertEquals(2.0, weightStepKg(Equipment.DUMBBELL), 0.0)
        assertEquals(2.5, weightStepKg(Equipment.CABLE), 0.0)
        val kettlebell =
            Exercise(
                "seed:k",
                "Kettlebell Swing",
                MuscleGroup.LEGS,
                Equipment.DUMBBELL,
                TrackingType.WEIGHT_REPS,
                weightStepKg = 4.0
            )
        assertEquals(4.0, weightStepOf(kettlebell), 0.0)
        assertEquals(2.0, weightStepOf(kettlebell.copy(weightStepKg = null)), 0.0)
        assertEquals(2.5, weightStepOf(null), 0.0)
        assertEquals(15, durationStepSec(TrackingType.DURATION))
        assertEquals(30, durationStepSec(TrackingType.DISTANCE_DURATION))
    }

    @Test
    fun `rest defaults are 2·30 for barbell, 1·30 otherwise, 1·00 for time and distance`() {
        assertEquals(150, defaultRestSec(TrackingType.WEIGHT_REPS, Equipment.BARBELL))
        assertEquals(90, defaultRestSec(TrackingType.WEIGHT_REPS, Equipment.DUMBBELL))
        assertEquals(90, defaultRestSec(TrackingType.BODYWEIGHT_REPS, null))
        assertEquals(60, defaultRestSec(TrackingType.DURATION, Equipment.BODYWEIGHT))
        assertEquals(60, defaultRestSec(TrackingType.DISTANCE_DURATION, Equipment.MACHINE))
    }

    @Test
    fun `a load change carries over to later open sets that had the same load`() {
        val sets = listOf(
            entry("a", 80.0, done = true),
            entry("b", 80.0, done = false),
            entry("c", 80.0, done = false),
            entry("d", 75.0, done = false),
            entry("e", 80.0, done = true)
        )
        assertEquals(listOf("c"), setsFollowingWeightChange(sets, changedSetId = "b", oldWeightKg = 80.0))
        assertEquals(emptyList<String>(), setsFollowingWeightChange(sets, changedSetId = "x", oldWeightKg = 80.0))
    }

    private fun entry(id: String, kg: Double, done: Boolean) = SetEntry(
        id = id,
        values = SetValues(kg, 8),
        isWarmup = false,
        completedAt = if (done) Instant.EPOCH else null,
        restAfterSec = null,
        isPr = false
    )
}
