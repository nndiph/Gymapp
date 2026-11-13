package com.nndiph.liftverse.feature.summary

import com.nndiph.liftverse.domain.formula.Improvement
import com.nndiph.liftverse.domain.formula.weekStart
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.LocalDate
import java.time.LocalTime

/** Sample "Push Day" summary as in screenshots light/10 and dark/10. */
internal object SummaryPreviewData {
    private val day: LocalDate = LocalDate.of(2026, 11, 12)

    // Heatmap levels per week, Monday first; days after the session day are not drawn.
    private val levels = listOf(
        listOf(2, 0, 3, 1, 0, 2, 0),
        listOf(3, 2, 0, 3, 0, 1, 0),
        listOf(2, 3, 2, 0, 4, 0, 0),
        listOf(4, 2, 3, 0, 3, 2, 0),
        listOf(3, 2, 4, 3, 0, 0, 0)
    )

    private val heatmap = levels.mapIndexed { week, days ->
        days.mapIndexed { weekday, level ->
            val date = day.weekStart().minusWeeks(SUMMARY_HEATMAP_WEEKS - 1L - week).plusDays(weekday.toLong())
            HeatCellUi(date = date, level = level, isSessionDay = date == day, isFuture = date > day)
        }
    }

    val summary = SessionSummaryUi(
        name = "Push Day",
        day = day,
        startTime = LocalTime.of(18, 42),
        endTime = LocalTime.of(19, 36),
        durationSec = 3_252,
        setCount = 19,
        volumeKg = 5_277.0,
        exerciseCount = 6,
        activeSec = 972,
        restSec = 2_280,
        averageRestSec = 127,
        improvedExercises = listOf(
            ImprovedExerciseUi("Barbell Bench Press", 3, Improvement.Weight(2.5)),
            ImprovedExerciseUi("Incline DB Press (grip netral)", 1, Improvement.Weight(2.0)),
            ImprovedExerciseUi("Lateral Raise", 2, Improvement.Reps(1))
        ),
        records = listOf(
            SummaryRecordUi(
                name = "Incline DB Press (grip netral)",
                trackingType = TrackingType.WEIGHT_REPS,
                values = SetValues(weightKg = 26.0, reps = 9),
                estimatedKg = 33.8,
                previousKg = 32.0
            )
        ),
        heatmap = heatmap,
        weekSessions = 4,
        monthSessions = 9,
        streak = SummaryStreak.Days(35)
    )
}
