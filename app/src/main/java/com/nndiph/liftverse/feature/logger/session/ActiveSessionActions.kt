package com.nndiph.liftverse.feature.logger.session

import androidx.compose.runtime.Stable
import com.nndiph.liftverse.domain.model.RemovedExercise
import com.nndiph.liftverse.domain.model.RemovedSet

/** Everything the active session screen can ask for. Defaults do nothing, which previews rely on. */
@Stable
interface ActiveSessionActions {
    /** Tapping a compact row makes it the active (editable) set. */
    fun onOpenSet(setId: String) {}

    /** The tick on a compact row: one tap completes or reopens the set. */
    fun onToggleDone(setId: String) {}

    fun onStep(setId: String, field: SetField, direction: Int) {}

    fun onSetValue(setId: String, field: SetField, value: Double) {}

    fun onToggleWarmup(setId: String) {}

    fun onDeleteSet(setId: String) {}

    /** "Selesai set", or "Simpan perubahan" on a set that is already done. */
    fun onPrimaryAction(setId: String) {}

    fun onAddSet(exerciseId: String) {}

    fun onDeleteExercise(exerciseId: String) {}

    fun onToggleExpanded(exerciseId: String) {}

    fun onAdjustRest(deltaSec: Int) {}

    fun onSkipRest() {}

    fun onFinish() {}
}

/** One-off messages for a snackbar with "Urungkan". */
sealed interface SessionMessage {
    /** [setNumber] is null for a warm-up set. */
    data class SetRemoved(val setNumber: Int?, val removed: RemovedSet) : SessionMessage

    data class ExerciseRemoved(val exerciseName: String, val removed: RemovedExercise) : SessionMessage
}

/** The −15 / +15 buttons of the rest timer. */
const val REST_ADJUST_SEC = 15
