package com.nndiph.liftverse.domain.formula

import com.nndiph.liftverse.domain.model.PlannedSet
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Test

class SetPrefillTest {
    private val warmup = PlannedSet(SetValues(weightKg = 60.0, reps = 10), isWarmup = true)
    private val work = PlannedSet(SetValues(weightKg = 80.0, reps = 8))

    @Test
    fun `copies the previous session set by set`() {
        assertEquals(listOf(warmup, work), prefillSets(TrackingType.WEIGHT_REPS, listOf(warmup, work)))
    }

    @Test
    fun `a routine with more sets repeats the last one as a working set`() {
        assertEquals(
            listOf(warmup, work, work, work),
            prefillSets(TrackingType.WEIGHT_REPS, listOf(warmup, work), setCount = 4, targetReps = 6)
        )
    }

    @Test
    fun `a routine with fewer sets takes the first ones`() {
        assertEquals(listOf(warmup), prefillSets(TrackingType.WEIGHT_REPS, listOf(warmup, work), setCount = 1))
    }

    @Test
    fun `without history the design defaults apply`() {
        assertEquals(List(3) { PlannedSet(SetValues(20.0, 10)) }, prefillSets(TrackingType.WEIGHT_REPS, emptyList()))
        assertEquals(
            List(4) { PlannedSet(SetValues(20.0, 8)) },
            prefillSets(TrackingType.WEIGHT_REPS, emptyList(), setCount = 4, targetReps = 8)
        )
        assertEquals(List(3) { PlannedSet(SetValues(0.0, 10)) }, prefillSets(TrackingType.BODYWEIGHT_REPS, emptyList()))
        assertEquals(listOf(PlannedSet(SetValues(durationSec = 60))), prefillSets(TrackingType.DURATION, emptyList()))
        assertEquals(
            listOf(PlannedSet(SetValues(distanceM = 2_000, durationSec = 900))),
            prefillSets(TrackingType.DISTANCE_DURATION, emptyList())
        )
    }
}
