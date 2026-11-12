package com.nndiph.liftverse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nndiph.liftverse.data.local.entity.CompletedSetRow
import com.nndiph.liftverse.data.local.entity.DayStatsRow
import com.nndiph.liftverse.data.local.entity.SessionEntity
import com.nndiph.liftverse.data.local.entity.SessionExerciseEntity
import com.nndiph.liftverse.data.local.entity.SessionSummaryRow
import com.nndiph.liftverse.data.local.entity.SessionWithExercises
import com.nndiph.liftverse.data.local.entity.SetEntity
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Volume of one set in SQL, for `set_entries` aliased as `st`. Same rule as `setVolumeKg()` in
 * domain/formula: kg × reps, bodyweight sets use the added load, warm-ups included. Change both together.
 */
internal const val SET_VOLUME_SQL = "COALESCE(st.weightKg, 0) * COALESCE(st.reps, 0)"

@Dao
interface SessionDao {
    @Transaction
    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeRunning(): Flow<SessionWithExercises?>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observe(id: String): Flow<SessionWithExercises?>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getRunning(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: String): SessionEntity?

    @Insert
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("SELECT * FROM session_exercises WHERE id = :id")
    suspend fun getExercise(id: String): SessionExerciseEntity?

    @Query("SELECT DISTINCT exerciseKey FROM session_exercises WHERE sessionId = :sessionId")
    suspend fun exerciseKeys(sessionId: String): List<String>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM session_exercises WHERE sessionId = :sessionId")
    suspend fun nextExercisePosition(sessionId: String): Int

    @Insert
    suspend fun insertExercise(exercise: SessionExerciseEntity)

    @Query("DELETE FROM session_exercises WHERE id = :id")
    suspend fun deleteExercise(id: String)

    @Query("SELECT * FROM set_entries WHERE id = :id")
    suspend fun getSet(id: String): SetEntity?

    @Query("SELECT * FROM set_entries WHERE sessionExerciseId = :sessionExerciseId ORDER BY position")
    suspend fun setsOf(sessionExerciseId: String): List<SetEntity>

    @Insert
    suspend fun insertSets(sets: List<SetEntity>)

    @Update
    suspend fun updateSet(set: SetEntity)

    @Query("UPDATE set_entries SET restAfterSec = :restSec WHERE id = :id")
    suspend fun setRestAfter(id: String, restSec: Int)

    @Query("UPDATE set_entries SET isPr = :isPr WHERE id = :id")
    suspend fun setPr(id: String, isPr: Boolean)

    @Query("DELETE FROM set_entries WHERE id = :id")
    suspend fun deleteSet(id: String)

    @Query(
        """
        DELETE FROM set_entries WHERE completedAt IS NULL
          AND sessionExerciseId IN (SELECT id FROM session_exercises WHERE sessionId = :sessionId)
        """
    )
    suspend fun deleteUncompletedSets(sessionId: String)

    @Query(
        """
        DELETE FROM session_exercises WHERE sessionId = :sessionId
          AND id NOT IN (SELECT sessionExerciseId FROM set_entries)
        """
    )
    suspend fun deleteExercisesWithoutSets(sessionId: String)

    /** Sets of the newest finished session before [before] that included [exerciseKey]. */
    @Query(
        """
        SELECT * FROM set_entries WHERE sessionExerciseId = (
            SELECT e.id FROM session_exercises e JOIN sessions s ON s.id = e.sessionId
            WHERE e.exerciseKey = :exerciseKey AND s.endedAt IS NOT NULL AND s.startedAt < :before
            ORDER BY s.startedAt DESC, e.position DESC
            LIMIT 1
        )
        ORDER BY position
        """
    )
    suspend fun lastSetsOf(exerciseKey: String, before: Instant): List<SetEntity>

    /** Completed sets of [exerciseKey] in the order they were done, running session included. */
    @Query(
        """
        SELECT st.id AS setId, e.sessionId AS sessionId, s.startedAt AS sessionStartedAt,
               s.endedAt IS NOT NULL AS sessionFinished, e.trackingType AS trackingType,
               st.weightKg AS weightKg, st.reps AS reps, st.isWarmup AS isWarmup, st.isPr AS isPr
        FROM set_entries st
        JOIN session_exercises e ON e.id = st.sessionExerciseId
        JOIN sessions s ON s.id = e.sessionId
        WHERE e.exerciseKey = :exerciseKey AND st.completedAt IS NOT NULL
        ORDER BY s.startedAt, st.completedAt, st.position
        """
    )
    suspend fun completedSetsOf(exerciseKey: String): List<CompletedSetRow>

    @Query(
        """
        SELECT s.id AS id, s.name AS name, s.day AS day, s.startedAt AS startedAt, s.endedAt AS endedAt,
               COUNT(DISTINCT e.id) AS exerciseCount,
               COUNT(st.id) AS setCount,
               COALESCE(SUM($SET_VOLUME_SQL), 0) AS volumeKg,
               COALESCE(SUM(st.restAfterSec), 0) AS restSec,
               COALESCE(SUM(st.isPr), 0) AS prCount
        FROM sessions s
        LEFT JOIN session_exercises e ON e.sessionId = s.id
        LEFT JOIN set_entries st ON st.sessionExerciseId = e.id AND st.completedAt IS NOT NULL
        WHERE s.endedAt IS NOT NULL
        GROUP BY s.id
        ORDER BY s.startedAt DESC
        LIMIT :limit
        """
    )
    fun observeHistory(limit: Int): Flow<List<SessionSummaryRow>>

    // Totals per session first, then per day, so session durations are not multiplied by the set join.
    @Query(
        """
        SELECT day, COUNT(*) AS sessionCount, SUM(setCount) AS setCount, SUM(volumeKg) AS volumeKg,
               SUM(durationSec) AS durationSec
        FROM (
            SELECT s.day AS day, (s.endedAt - s.startedAt) / 1000 AS durationSec,
                   COUNT(st.id) AS setCount, COALESCE(SUM($SET_VOLUME_SQL), 0) AS volumeKg
            FROM sessions s
            LEFT JOIN session_exercises e ON e.sessionId = s.id
            LEFT JOIN set_entries st ON st.sessionExerciseId = e.id AND st.completedAt IS NOT NULL
            WHERE s.endedAt IS NOT NULL AND s.day BETWEEN :from AND :to
            GROUP BY s.id
        )
        GROUP BY day
        ORDER BY day
        """
    )
    fun observeDayStats(from: LocalDate, to: LocalDate): Flow<List<DayStatsRow>>
}
