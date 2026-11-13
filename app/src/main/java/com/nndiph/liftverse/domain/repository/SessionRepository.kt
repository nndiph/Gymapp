package com.nndiph.liftverse.domain.repository

import com.nndiph.liftverse.domain.model.DayStats
import com.nndiph.liftverse.domain.model.ExerciseVolume
import com.nndiph.liftverse.domain.model.RemovedExercise
import com.nndiph.liftverse.domain.model.RemovedSet
import com.nndiph.liftverse.domain.model.Session
import com.nndiph.liftverse.domain.model.SessionSummary
import com.nndiph.liftverse.domain.model.SetEntry
import com.nndiph.liftverse.domain.model.SetValues
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Workout sessions, stored on the device first. Every write returns as soon as the local database has it;
 * syncing to the cloud happens later and never blocks the caller.
 */
interface SessionRepository {
    fun observeRunningSession(): Flow<Session?>

    fun observeSession(id: String): Flow<Session?>

    /** The newest [limit] finished sessions. Raise [limit] to page further back. */
    fun observeHistory(limit: Int): Flow<List<SessionSummary>>

    /** Per-day totals of finished sessions between [from] and [to] inclusive (heatmap, progress). */
    fun observeDayStats(from: LocalDate, to: LocalDate): Flow<List<DayStats>>

    /** Sets of the last finished session before [before] that included [exerciseKey] ("Sesi lalu 80 kg × 8"). */
    suspend fun previousSets(exerciseKey: String, before: Instant): List<SetEntry>

    /**
     * Best estimated 1RM of [exerciseKey] in finished sessions that started before [before], or null when there is
     * none yet. The first session of an exercise only sets this record, so it cannot hold a "Calon PR".
     */
    suspend fun recordBefore(exerciseKey: String, before: Instant): Double?

    /** Volume of [exerciseKey] in its newest [limit] finished sessions, newest first. */
    suspend fun exerciseVolumes(exerciseKey: String, limit: Int): List<ExerciseVolume>

    /** Day of the newest finished session started from [routineId] ("terakhir Senin, 9 November"). */
    suspend fun lastSessionDay(routineId: String): LocalDate?

    /** Number of finished sessions, for the history shortcut. */
    fun observeFinishedSessionCount(): Flow<Int>

    /** Starts a session without a routine. Returns the running session instead if there is one. */
    suspend fun startEmptySession(name: String): String

    /**
     * Starts a session from a routine with sets prefilled from the last session. Returns the running session instead
     * if there is one, or null when the routine no longer exists.
     */
    suspend fun startRoutineSession(routineId: String): String?

    /** Returns the new session-exercise id, or null when the session or exercise is unknown. */
    suspend fun addExercise(sessionId: String, exerciseKey: String): String?

    suspend fun removeExercise(sessionExerciseId: String): RemovedExercise?

    suspend fun restoreExercise(removed: RemovedExercise)

    /** Adds a set copying the last set of the exercise. Returns the new set id. */
    suspend fun addSet(sessionExerciseId: String): String?

    suspend fun updateSet(setId: String, values: SetValues, isWarmup: Boolean)

    suspend fun removeSet(setId: String): RemovedSet?

    suspend fun restoreSet(removed: RemovedSet)

    /** Ticks the set off: logs the rest that it ends, starts a new rest timer, and refreshes record flags. */
    suspend fun completeSet(setId: String, restTargetSec: Int)

    suspend fun uncompleteSet(setId: String)

    /** "Lewati": logs the rest so far and stops the timer. */
    suspend fun skipRest(sessionId: String)

    /** "−15" / "+15" on the rest timer. */
    suspend fun adjustRestTarget(sessionId: String, deltaSec: Int)

    /**
     * "Selesaikan sesi": sets that were not ticked off are dropped, then the session is saved.
     * Returns false when nothing was ticked off; the session is then discarded.
     */
    suspend fun finishSession(sessionId: String): Boolean

    suspend fun deleteSession(sessionId: String)
}
