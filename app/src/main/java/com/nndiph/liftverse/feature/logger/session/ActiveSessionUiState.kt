package com.nndiph.liftverse.feature.logger.session

import com.nndiph.liftverse.domain.formula.Improvement
import com.nndiph.liftverse.domain.formula.LiveAchievement
import com.nndiph.liftverse.domain.formula.RecordCandidate
import com.nndiph.liftverse.domain.formula.bestOneRepMaxKg
import com.nndiph.liftverse.domain.formula.improvementOver
import com.nndiph.liftverse.domain.formula.liveAchievement
import com.nndiph.liftverse.domain.formula.setVolumeKg
import com.nndiph.liftverse.domain.formula.weightStepOf
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.Exercise
import com.nndiph.liftverse.domain.model.RestTimer
import com.nndiph.liftverse.domain.model.Session
import com.nndiph.liftverse.domain.model.SetEntry
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.Instant

sealed interface ActiveSessionUiState {
    data object Loading : ActiveSessionUiState

    data object NoSession : ActiveSessionUiState

    data class Running(val session: ActiveSessionUi) : ActiveSessionUiState
}

data class ActiveSessionUi(
    val id: String,
    val name: String,
    val startedAt: Instant,
    val rest: RestTimer?,
    /** Rest already logged on sets; the running rest timer is added on screen. */
    val loggedRestSec: Int,
    val exercises: List<ExerciseUi>,
    /** The one set shown as the editable panel: picked by the user, otherwise the first set not done. */
    val activeSetId: String?
) {
    val completedSets: Int get() = exercises.sumOf { it.completedSets }
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
}

data class ExerciseUi(
    val id: String,
    val name: String,
    val isCustom: Boolean,
    val trackingType: TrackingType,
    val equipment: Equipment?,
    val isExpanded: Boolean,
    val sets: List<SetUi>,
    /** The sets of this exercise in the previous session, for the collapsed card summary. */
    val previousSets: List<SetValues>,
    /** Load step of the stepper. */
    val weightStep: Double
) {
    val completedSets: Int get() = sets.count { it.isCompleted }
    val isDone: Boolean get() = sets.isNotEmpty() && completedSets == sets.size
    val completedVolumeKg: Double
        get() = sets.filter { it.isCompleted }.sumOf { setVolumeKg(it.values.weightKg, it.values.reps) }
}

data class SetUi(
    val id: String,
    /** Working-set number ("Set 2"); null for warm-ups, which are not numbered. */
    val number: Int?,
    val values: SetValues,
    val isWarmup: Boolean,
    val isCompleted: Boolean,
    /** The same set in the previous session ("Sesi lalu 80 kg × 8"). */
    val previous: SetValues?,
    /** Tag on the compact row of a done set. */
    val tag: SetTag,
    /** Live chip; only the active set has one. */
    val achievement: LiveAchievement
)

sealed interface SetTag {
    data object None : SetTag

    data class Improved(val improvement: Improvement) : SetTag

    data object Pr : SetTag
}

/** What the screen needs about an exercise beyond the session: library entry, previous session, record. */
data class ExerciseContext(val exercise: Exercise?, val previousSets: List<SetEntry>, val recordKg: Double?)

/**
 * Builds the screen model. Sets are paired with the previous session by position. The record for "Calon PR" is
 * the record from earlier sessions raised by sets already done today; without an earlier record there is no
 * candidate, matching how records are stored.
 */
fun buildActiveSessionUi(
    session: Session,
    contexts: Map<String, ExerciseContext>,
    selectedSetId: String?,
    expandedOverrides: Map<String, Boolean>
): ActiveSessionUi {
    val allSets = session.exercises.flatMap { it.sets }
    val activeSetId = selectedSetId?.takeIf { id -> allSets.any { it.id == id } }
        ?: allSets.firstOrNull { !it.isCompleted }?.id
    val activeExerciseId = session.exercises.firstOrNull { exercise -> exercise.sets.any { it.id == activeSetId } }?.id
    val exercises = session.exercises.map { exercise ->
        val context = contexts[exercise.exerciseKey]
        val previous = context?.previousSets.orEmpty().map { it.values }
        val doneToday = exercise.sets.filter { it.isCompleted && it.id != activeSetId }
            .map { RecordCandidate(it.id, session.id, it.values.weightKg, it.values.reps, it.isWarmup) }
        val recordKg = context?.recordKg?.let { record -> maxOf(record, bestOneRepMaxKg(doneToday) ?: 0.0) }
        var workingNumber = 0
        val sets = exercise.sets.mapIndexed { index, set ->
            val previousValues = previous.getOrNull(index)
            val improvement = previousValues?.takeIf { !set.isWarmup }
                ?.let { improvementOver(exercise.trackingType, set.values, it) }
            SetUi(
                id = set.id,
                number = if (set.isWarmup) null else ++workingNumber,
                values = set.values,
                isWarmup = set.isWarmup,
                isCompleted = set.isCompleted,
                previous = previousValues,
                tag = when {
                    !set.isCompleted -> SetTag.None
                    set.isPr -> SetTag.Pr
                    improvement != null -> SetTag.Improved(improvement)
                    else -> SetTag.None
                },
                achievement = if (set.id == activeSetId) {
                    liveAchievement(exercise.trackingType, set.values, set.isWarmup, previousValues, recordKg)
                } else {
                    LiveAchievement.None
                }
            )
        }
        ExerciseUi(
            id = exercise.id,
            name = exercise.nameSnapshot,
            isCustom = context?.exercise?.isCustom ?: false,
            trackingType = exercise.trackingType,
            equipment = context?.exercise?.equipment,
            isExpanded = expandedOverrides[exercise.id] ?: (exercise.id == activeExerciseId),
            sets = sets,
            previousSets = previous,
            weightStep = weightStepOf(context?.exercise)
        )
    }
    return ActiveSessionUi(
        id = session.id,
        name = session.name,
        startedAt = session.startedAt,
        rest = session.rest,
        loggedRestSec = allSets.sumOf { it.restAfterSec ?: 0 },
        exercises = exercises,
        activeSetId = activeSetId
    )
}
