package com.nndiph.liftverse.domain.formula

import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineEstimateTest {
    @Test
    fun `each set counts its reps and the default rest, rounded to five minutes`() {
        val work = listOf(
            // 4 × (8 × 4 s + 150 s) = 728 s
            PlannedWork(TrackingType.WEIGHT_REPS, Equipment.BARBELL, sets = 4, reps = 8),
            // 3 × (10 × 4 s + 90 s) = 390 s
            PlannedWork(TrackingType.WEIGHT_REPS, Equipment.DUMBBELL, sets = 3, reps = 10)
        )
        // 1118 s ≈ 18,6 min → 20
        assertEquals(20, estimatedRoutineMinutes(work))
    }

    @Test
    fun `timed sets count a minute, missing reps count ten`() {
        // 3 × (60 s + 60 s) + 3 × (10 × 4 s + 90 s) = 360 + 390 = 750 s = 12,5 min → 15 (half rounds up)
        val work = listOf(
            PlannedWork(TrackingType.DURATION, Equipment.BODYWEIGHT, sets = 3, reps = null),
            PlannedWork(TrackingType.WEIGHT_REPS, Equipment.MACHINE, sets = 3, reps = null)
        )
        assertEquals(15, estimatedRoutineMinutes(work))
    }

    @Test
    fun `a short or empty routine still shows five minutes`() {
        assertEquals(5, estimatedRoutineMinutes(emptyList()))
        assertEquals(
            5,
            estimatedRoutineMinutes(listOf(PlannedWork(TrackingType.DURATION, null, sets = 1, reps = null)))
        )
    }
}
