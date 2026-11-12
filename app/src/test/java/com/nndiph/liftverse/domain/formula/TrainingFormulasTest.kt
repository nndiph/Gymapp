package com.nndiph.liftverse.domain.formula

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingFormulasTest {
    @Test
    fun `Epley estimate matches the design example`() {
        // README "Rumus": 100 × (1 + 5/30) = 116,7 kg
        assertEquals(116.67, estimatedOneRepMaxKg(100.0, 5), 0.01)
    }

    @Test
    fun `set volume is kg times reps and plain bodyweight adds nothing`() {
        assertEquals(640.0, setVolumeKg(80.0, 8), 0.0)
        assertEquals(0.0, setVolumeKg(0.0, 12), 0.0)
        assertEquals(0.0, setVolumeKg(null, null), 0.0)
    }

    @Test
    fun `logged rest subtracts four seconds per rep and never goes negative`() {
        val start = Instant.parse("2026-11-09T11:00:00Z")
        assertEquals(118, loggedRestSec(start, start.plusSeconds(150), nextSetReps = 8))
        assertEquals(150, loggedRestSec(start, start.plusSeconds(150), nextSetReps = null))
        assertEquals(0, loggedRestSec(start, start.plusSeconds(20), nextSetReps = 10))
    }

    @Test
    fun `the first session with working sets only sets the baseline`() {
        val sets = listOf(
            candidate("w", "s1", 60.0, 10, isWarmup = true),
            candidate("a", "s2", 80.0, 8),
            candidate("b", "s2", 85.0, 8)
        )
        assertEquals(emptySet<String>(), personalRecordSetIds(sets))
    }

    @Test
    fun `a later working set that beats every earlier set is a record`() {
        val sets = listOf(
            // Baseline: 116.7
            candidate("a", "s1", 100.0, 5),
            // Warm-ups never count, however heavy
            candidate("w", "s2", 120.0, 5, isWarmup = true),
            // Equal to the record: not a record
            candidate("b", "s2", 100.0, 5),
            // 119.6
            candidate("c", "s2", 102.5, 5),
            // 122.5, beats c from the same session
            candidate("d", "s2", 105.0, 5),
            // Bodyweight without added load has no estimate
            candidate("e", "s3", 0.0, 12),
            // 126.7
            candidate("f", "s3", 100.0, 8)
        )
        assertEquals(setOf("c", "d", "f"), personalRecordSetIds(sets))
    }

    private fun candidate(id: String, session: String, kg: Double, reps: Int, isWarmup: Boolean = false) =
        RecordCandidate(setId = id, sessionId = session, weightKg = kg, reps = reps, isWarmup = isWarmup)
}
