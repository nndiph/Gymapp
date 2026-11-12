package com.nndiph.liftverse.feature.logger.session

import com.nndiph.liftverse.domain.formula.Improvement
import com.nndiph.liftverse.domain.formula.LiveAchievement
import com.nndiph.liftverse.domain.model.Session
import com.nndiph.liftverse.domain.model.SessionExercise
import com.nndiph.liftverse.domain.model.SetEntry
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveSessionUiStateTest {
    private val start = Instant.parse("2026-11-12T11:00:00Z")

    @Test
    fun `the first open set is active, warm-ups are not numbered, only its exercise is open`() {
        val bench = exercise(
            "bench",
            entry("w", 60.0, 10, done = true, warmup = true, restSec = 90),
            entry("s1", 80.0, 8, done = true, restSec = 120),
            entry("s2", 80.0, 8)
        )
        val ui = buildActiveSessionUi(session(bench, exercise("ohp", entry("o1", 40.0, 8))), emptyMap(), null, emptyMap())

        assertEquals("s2", ui.activeSetId)
        assertEquals(listOf(null, 1, 2), ui.exercises[0].sets.map { it.number })
        assertEquals(listOf(true, false), ui.exercises.map { it.isExpanded })
        assertEquals(2, ui.completedSets)
        assertEquals(4, ui.totalSets)
        assertEquals(210, ui.loggedRestSec)
    }

    @Test
    fun `a picked set stays active and other cards open on request`() {
        val bench = exercise("bench", entry("s1", 80.0, 8), entry("s2", 80.0, 8))
        val ohp = exercise("ohp", entry("o1", 40.0, 8))
        val ui = buildActiveSessionUi(session(bench, ohp), emptyMap(), "o1", mapOf("bench" to true))

        assertEquals("o1", ui.activeSetId)
        assertEquals(listOf(true, true), ui.exercises.map { it.isExpanded })
    }

    @Test
    fun `sets pair with the previous session by position for tags and the live chip`() {
        val bench = exercise("bench", entry("s1", 82.5, 8, done = true), entry("s2", 85.0, 8))
        val context = ExerciseContext(
            exercise = null,
            previousSets = listOf(entry("p1", 80.0, 8, done = true), entry("p2", 80.0, 8, done = true)),
            recordKg = 200.0
        )
        val sets = buildActiveSessionUi(session(bench), mapOf("seed:bench" to context), null, emptyMap())
            .exercises.single().sets

        assertEquals(SetTag.Improved(Improvement.Weight(2.5)), sets[0].tag)
        assertEquals(LiveAchievement.Improved(Improvement.Weight(5.0)), sets[1].achievement)
        assertEquals(SetValues(80.0, 8), sets[1].previous)
    }

    @Test
    fun `sets done today raise the record that a candidate has to beat`() {
        val context = ExerciseContext(
            exercise = null,
            previousSets = listOf(entry("p1", 90.0, 5, done = true), entry("p2", 90.0, 5, done = true)),
            recordKg = 100.0
        )
        // Done today: 95 × 5 → 110,8. Active: 97,5 × 5 → 113,8 beats it; 92,5 × 5 → 107,9 does not.
        fun active(kg: Double) = buildActiveSessionUi(
            session(exercise("bench", entry("s1", 95.0, 5, done = true), entry("s2", kg, 5))),
            mapOf("seed:bench" to context),
            null,
            emptyMap()
        ).exercises.single().sets[1].achievement

        assertTrue(active(97.5) is LiveAchievement.PrCandidate)
        assertEquals(LiveAchievement.Improved(Improvement.Weight(2.5)), active(92.5))
    }

    @Test
    fun `stored records and an empty history show no candidate`() {
        val bench = exercise("bench", entry("s1", 120.0, 5, done = true, pr = true), entry("s2", 130.0, 5))
        val context = ExerciseContext(exercise = null, previousSets = emptyList(), recordKg = null)
        val sets = buildActiveSessionUi(session(bench), mapOf("seed:bench" to context), null, emptyMap())
            .exercises.single().sets

        assertEquals(SetTag.Pr, sets[0].tag)
        assertEquals(LiveAchievement.None, sets[1].achievement)
    }

    private fun entry(
        id: String,
        kg: Double,
        reps: Int,
        done: Boolean = false,
        warmup: Boolean = false,
        pr: Boolean = false,
        restSec: Int? = null
    ) = SetEntry(id, SetValues(kg, reps), warmup, if (done) start else null, restSec, pr)

    private fun exercise(id: String, vararg sets: SetEntry) =
        SessionExercise(id, "seed:$id", id, TrackingType.WEIGHT_REPS, sets.toList())

    private fun session(vararg exercises: SessionExercise) = Session(
        id = "s",
        name = "Push Day",
        routineId = null,
        startedAt = start,
        endedAt = null,
        day = LocalDate.of(2026, 11, 12),
        routineSnapshot = emptyList(),
        rest = null,
        exercises = exercises.toList()
    )
}
