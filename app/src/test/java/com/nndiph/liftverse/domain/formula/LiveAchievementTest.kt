package com.nndiph.liftverse.domain.formula

import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveAchievementTest {
    private val previous = SetValues(weightKg = 80.0, reps = 8)

    @Test
    fun `more load counts while reps drop by at most one`() {
        assertEquals(Improvement.Weight(2.5), improvementOver(TrackingType.WEIGHT_REPS, SetValues(82.5, 7), previous))
        assertNull(improvementOver(TrackingType.WEIGHT_REPS, SetValues(82.5, 6), previous))
    }

    @Test
    fun `more reps count only at the same load`() {
        assertEquals(Improvement.Reps(2), improvementOver(TrackingType.WEIGHT_REPS, SetValues(80.0, 10), previous))
        assertNull(improvementOver(TrackingType.WEIGHT_REPS, SetValues(77.5, 12), previous))
    }

    @Test
    fun `timed and distance sets improve by lasting longer or going further`() {
        assertEquals(
            Improvement.Duration(15),
            improvementOver(TrackingType.DURATION, SetValues(durationSec = 75), SetValues(durationSec = 60))
        )
        assertEquals(
            Improvement.Distance(100),
            improvementOver(
                TrackingType.DISTANCE_DURATION,
                SetValues(distanceM = 2_100, durationSec = 900),
                SetValues(distanceM = 2_000, durationSec = 900)
            )
        )
        assertNull(improvementOver(TrackingType.DURATION, SetValues(durationSec = 60), SetValues(durationSec = 60)))
    }

    @Test
    fun `a record candidate replaces the improvement chip`() {
        val achievement =
            liveAchievement(TrackingType.WEIGHT_REPS, SetValues(26.0, 9), false, SetValues(24.0, 10), 32.0)
        // 26 × (1 + 9/30) = 33,8 beats 32
        assertEquals(LiveAchievement.PrCandidate(26.0 * (1 + 9 / 30.0), 32.0), achievement)
    }

    @Test
    fun `no chip for warm-ups, sets without a previous session, or without a record`() {
        val better = SetValues(85.0, 8)
        assertEquals(LiveAchievement.None, liveAchievement(TrackingType.WEIGHT_REPS, better, true, previous, 90.0))
        assertEquals(LiveAchievement.None, liveAchievement(TrackingType.WEIGHT_REPS, better, false, null, 90.0))
        assertEquals(
            LiveAchievement.Improved(Improvement.Weight(5.0)),
            liveAchievement(TrackingType.WEIGHT_REPS, better, false, previous, recordKg = null)
        )
    }

    @Test
    fun `bodyweight sets never become record candidates`() {
        val achievement = liveAchievement(
            TrackingType.BODYWEIGHT_REPS,
            SetValues(weightKg = 20.0, reps = 10),
            isWarmup = false,
            previous = SetValues(weightKg = 10.0, reps = 10),
            recordKg = 1.0
        )
        assertEquals(LiveAchievement.Improved(Improvement.Weight(10.0)), achievement)
    }
}
