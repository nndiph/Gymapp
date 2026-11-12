package com.nndiph.liftverse.feature.logger.session

import com.nndiph.liftverse.domain.formula.Improvement
import com.nndiph.liftverse.domain.formula.LiveAchievement
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.RestTimer
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.Instant

/** Sample data for previews, modelled on the "Push Day" of the design screenshots (light/04–07). */
internal object SessionPreviewData {
    val now: Instant = Instant.parse("2026-11-12T11:42:00Z")

    private fun set(
        id: String,
        number: Int?,
        kg: Double,
        reps: Int,
        done: Boolean,
        previous: SetValues? = SetValues(kg, reps),
        tag: SetTag = SetTag.None,
        achievement: LiveAchievement = LiveAchievement.None
    ) = SetUi(
        id = id,
        number = number,
        values = SetValues(weightKg = kg, reps = reps),
        isWarmup = number == null,
        isCompleted = done,
        previous = previous,
        tag = tag,
        achievement = achievement
    )

    val warmupDone = set("w", null, 60.0, 10, done = true)
    val firstDone = set("s1", 1, 80.0, 8, done = true)
    val firstImproved = set(
        "s1",
        1,
        82.5,
        8,
        done = true,
        previous = SetValues(80.0, 8),
        tag = SetTag.Improved(Improvement.Weight(2.5))
    )
    val activeSet = set("s2", 2, 80.0, 8, done = false)
    val activeImproved = set(
        "s2",
        2,
        85.0,
        8,
        done = false,
        previous = SetValues(80.0, 8),
        achievement = LiveAchievement.Improved(Improvement.Weight(5.0))
    )
    val activePrCandidate = set(
        "i1",
        1,
        26.0,
        9,
        done = false,
        previous = SetValues(24.0, 10),
        achievement = LiveAchievement.PrCandidate(estimatedKg = 33.8, recordKg = 32.0)
    )
    val pendingSet = set("s3", 3, 80.0, 7, done = false)
    val prSet = set("s4", 3, 85.0, 6, done = true, tag = SetTag.Pr)

    val bench = ExerciseUi(
        id = "bench",
        name = "Barbell Bench Press",
        isCustom = false,
        trackingType = TrackingType.WEIGHT_REPS,
        equipment = Equipment.BARBELL,
        isExpanded = true,
        sets = listOf(warmupDone, firstDone, activeSet, pendingSet),
        previousSets = listOf(SetValues(60.0, 10), SetValues(80.0, 8), SetValues(80.0, 8), SetValues(80.0, 7))
    )

    val overheadPress = ExerciseUi(
        id = "ohp",
        name = "Overhead Press",
        isCustom = false,
        trackingType = TrackingType.WEIGHT_REPS,
        equipment = Equipment.BARBELL,
        isExpanded = false,
        sets = List(3) { index -> set("o$index", index + 1, 40.0, if (index == 2) 7 else 8, done = false) },
        previousSets = listOf(SetValues(40.0, 8), SetValues(40.0, 8), SetValues(40.0, 7))
    )

    val pushDay = ActiveSessionUi(
        id = "session",
        name = "Push Day",
        startedAt = now.minusSeconds(1_491),
        rest = RestTimer(afterSetId = "s1", startedAt = now.minusSeconds(78), targetSec = 150),
        loggedRestSec = 548,
        exercises = listOf(bench, overheadPress),
        activeSetId = activeSet.id
    )

    val emptySession = ActiveSessionUi(
        id = "empty",
        name = "Sesi bebas",
        startedAt = now.minusSeconds(132),
        rest = null,
        loggedRestSec = 0,
        exercises = emptyList(),
        activeSetId = null
    )

    val restRunning = RestTimer(afterSetId = "s1", startedAt = now.minusSeconds(78), targetSec = 150)
    val restOver = RestTimer(afterSetId = "s1", startedAt = now.minusSeconds(162), targetSec = 150)
}
