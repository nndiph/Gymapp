package com.nndiph.liftverse.data.repository

import androidx.room.withTransaction
import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.data.local.dao.enqueue
import com.nndiph.liftverse.data.local.entity.CompletedSetRow
import com.nndiph.liftverse.data.local.entity.ExerciseEntity
import com.nndiph.liftverse.data.local.entity.SessionEntity
import com.nndiph.liftverse.data.local.entity.SessionExerciseEntity
import com.nndiph.liftverse.data.local.entity.SetEntity
import com.nndiph.liftverse.data.local.entity.SyncEntityType
import com.nndiph.liftverse.data.local.toDomain
import com.nndiph.liftverse.data.local.toEntity
import com.nndiph.liftverse.data.local.toPlannedSet
import com.nndiph.liftverse.domain.formula.RecordCandidate
import com.nndiph.liftverse.domain.formula.bestOneRepMaxKg
import com.nndiph.liftverse.domain.formula.loggedRestSec
import com.nndiph.liftverse.domain.formula.personalRecordSetIds
import com.nndiph.liftverse.domain.formula.prefillSets
import com.nndiph.liftverse.domain.model.DayStats
import com.nndiph.liftverse.domain.model.PlannedExercise
import com.nndiph.liftverse.domain.model.PlannedSet
import com.nndiph.liftverse.domain.model.RemovedExercise
import com.nndiph.liftverse.domain.model.RemovedSet
import com.nndiph.liftverse.domain.model.Session
import com.nndiph.liftverse.domain.model.SessionExercise
import com.nndiph.liftverse.domain.model.SessionSummary
import com.nndiph.liftverse.domain.model.SetEntry
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType
import com.nndiph.liftverse.domain.repository.SessionRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstSessionRepository @Inject constructor(
    private val database: LiftverseDatabase,
    private val clock: Clock
) : SessionRepository {
    private val sessionDao = database.sessionDao()
    private val exerciseDao = database.exerciseDao()
    private val routineDao = database.routineDao()
    private val pendingSyncDao = database.pendingSyncDao()

    override fun observeRunningSession(): Flow<Session?> = sessionDao.observeRunning().map { it?.toDomain() }

    override fun observeSession(id: String): Flow<Session?> = sessionDao.observe(id).map { it?.toDomain() }

    override fun observeHistory(limit: Int): Flow<List<SessionSummary>> =
        sessionDao.observeHistory(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeDayStats(from: LocalDate, to: LocalDate): Flow<List<DayStats>> =
        sessionDao.observeDayStats(from, to).map { rows -> rows.map { it.toDomain() } }

    override suspend fun previousSets(exerciseKey: String, before: Instant): List<SetEntry> =
        sessionDao.lastSetsOf(exerciseKey, before).map { it.toDomain() }

    override suspend fun recordBefore(exerciseKey: String, before: Instant): Double? = bestOneRepMaxKg(
        sessionDao.completedSetsOf(exerciseKey)
            .filter { it.sessionFinished && it.sessionStartedAt < before }
            .recordCandidates()
    )

    override suspend fun startEmptySession(name: String): String = database.withTransaction {
        sessionDao.getRunning()?.id ?: insertSession(name, routineId = null, snapshot = emptyList()).id
    }

    override suspend fun startRoutineSession(routineId: String): String? = database.withTransaction {
        sessionDao.getRunning()?.let { return@withTransaction it.id }
        val routine = routineDao.get(routineId)?.takeUnless { it.routine.archived } ?: return@withTransaction null
        val items = routine.exercises.sortedBy { it.position }
        val session = insertSession(
            name = routine.routine.name,
            routineId = routineId,
            snapshot = items.map { PlannedExercise(it.exerciseKey, it.targetSets) }
        )
        // An exercise that left the library (e.g. a seed entry removed later) cannot be logged, so it is skipped.
        items.mapNotNull { item -> exerciseDao.get(item.exerciseKey)?.let { item to it } }
            .forEachIndexed { position, (item, exercise) ->
                val previous = sessionDao.lastSetsOf(item.exerciseKey, session.startedAt).map { it.toPlannedSet() }
                insertExercise(
                    sessionId = session.id,
                    position = position,
                    exercise = exercise,
                    sets = prefillSets(exercise.trackingType, previous, item.targetSets, item.targetReps)
                )
            }
        session.id
    }

    override suspend fun addExercise(sessionId: String, exerciseKey: String): String? = database.withTransaction {
        val session = sessionDao.getSession(sessionId) ?: return@withTransaction null
        val exercise = exerciseDao.get(exerciseKey) ?: return@withTransaction null
        val previous = sessionDao.lastSetsOf(exerciseKey, session.startedAt).map { it.toPlannedSet() }
        val id = insertExercise(
            sessionId = sessionId,
            position = sessionDao.nextExercisePosition(sessionId),
            exercise = exercise,
            sets = prefillSets(exercise.trackingType, previous)
        )
        markChanged(session)
        id
    }

    override suspend fun removeExercise(sessionExerciseId: String): RemovedExercise? = database.withTransaction {
        val exercise = sessionDao.getExercise(sessionExerciseId) ?: return@withTransaction null
        val session = sessionDao.getSession(exercise.sessionId) ?: return@withTransaction null
        val sets = sessionDao.setsOf(sessionExerciseId)
        sessionDao.deleteExercise(sessionExerciseId)
        if (sets.any { it.id == session.restSetId }) sessionDao.updateSession(session.withoutRest())
        refreshRecords(exercise.exerciseKey)
        markChanged(session)
        RemovedExercise(
            sessionId = session.id,
            position = exercise.position,
            exercise = SessionExercise(
                id = exercise.id,
                exerciseKey = exercise.exerciseKey,
                nameSnapshot = exercise.nameSnapshot,
                trackingType = exercise.trackingType,
                sets = sets.map { it.toDomain() }
            )
        )
    }

    override suspend fun restoreExercise(removed: RemovedExercise) {
        database.withTransaction {
            val session = sessionDao.getSession(removed.sessionId) ?: return@withTransaction
            val exercise = removed.exercise
            sessionDao.insertExercise(
                SessionExerciseEntity(
                    id = exercise.id,
                    sessionId = session.id,
                    position = removed.position,
                    exerciseKey = exercise.exerciseKey,
                    nameSnapshot = exercise.nameSnapshot,
                    trackingType = exercise.trackingType
                )
            )
            sessionDao.insertSets(exercise.sets.mapIndexed { index, set -> set.toEntity(exercise.id, index) })
            refreshRecords(exercise.exerciseKey)
            markChanged(session)
        }
    }

    override suspend fun addSet(sessionExerciseId: String): String? = database.withTransaction {
        val exercise = sessionDao.getExercise(sessionExerciseId) ?: return@withTransaction null
        val session = sessionDao.getSession(exercise.sessionId) ?: return@withTransaction null
        val sets = sessionDao.setsOf(sessionExerciseId)
        val template = sets.lastOrNull()?.toPlannedSet()?.copy(isWarmup = false)
            ?: prefillSets(exercise.trackingType, previous = emptyList()).first()
        val id = newId()
        val position = (sets.maxOfOrNull { it.position } ?: -1) + 1
        sessionDao.insertSets(listOf(template.toEntity(id, sessionExerciseId, position)))
        markChanged(session)
        id
    }

    override suspend fun updateSet(setId: String, values: SetValues, isWarmup: Boolean) {
        database.withTransaction {
            val set = sessionDao.getSet(setId) ?: return@withTransaction
            val (exercise, session) = parentsOf(set) ?: return@withTransaction
            sessionDao.updateSet(
                set.copy(
                    weightKg = values.weightKg,
                    reps = values.reps,
                    durationSec = values.durationSec,
                    distanceM = values.distanceM,
                    isWarmup = isWarmup
                )
            )
            if (set.completedAt != null) refreshRecords(exercise.exerciseKey)
            markChanged(session)
        }
    }

    override suspend fun removeSet(setId: String): RemovedSet? = database.withTransaction {
        val set = sessionDao.getSet(setId) ?: return@withTransaction null
        val (exercise, session) = parentsOf(set) ?: return@withTransaction null
        sessionDao.deleteSet(setId)
        if (session.restSetId == setId) sessionDao.updateSession(session.withoutRest())
        if (set.completedAt != null) refreshRecords(exercise.exerciseKey)
        markChanged(session)
        RemovedSet(set.sessionExerciseId, set.position, set.toDomain())
    }

    override suspend fun restoreSet(removed: RemovedSet) {
        database.withTransaction {
            val exercise = sessionDao.getExercise(removed.sessionExerciseId) ?: return@withTransaction
            val session = sessionDao.getSession(exercise.sessionId) ?: return@withTransaction
            sessionDao.insertSets(listOf(removed.set.toEntity(removed.sessionExerciseId, removed.position)))
            if (removed.set.isCompleted) refreshRecords(exercise.exerciseKey)
            markChanged(session)
        }
    }

    override suspend fun completeSet(setId: String, restTargetSec: Int) {
        database.withTransaction {
            val set = sessionDao.getSet(setId) ?: return@withTransaction
            if (set.completedAt != null) return@withTransaction
            val (exercise, session) = parentsOf(set) ?: return@withTransaction
            val now = clock.instant()
            sessionDao.updateSet(set.copy(completedAt = now))
            if (session.endedAt == null) {
                // The tick ends the rest of the previous set and starts the rest of this one.
                val restSetId = session.restSetId
                val restStartedAt = session.restStartedAt
                if (restSetId != null && restStartedAt != null) {
                    sessionDao.setRestAfter(restSetId, loggedRestSec(restStartedAt, now, set.reps))
                }
                sessionDao.updateSession(
                    session.copy(restSetId = setId, restStartedAt = now, restTargetSec = restTargetSec)
                )
            }
            refreshRecords(exercise.exerciseKey)
            markChanged(session)
        }
    }

    override suspend fun uncompleteSet(setId: String) {
        database.withTransaction {
            val set = sessionDao.getSet(setId) ?: return@withTransaction
            if (set.completedAt == null) return@withTransaction
            val (exercise, session) = parentsOf(set) ?: return@withTransaction
            sessionDao.updateSet(set.copy(completedAt = null, restAfterSec = null, isPr = false))
            if (session.restSetId == setId) sessionDao.updateSession(session.withoutRest())
            refreshRecords(exercise.exerciseKey)
            markChanged(session)
        }
    }

    override suspend fun skipRest(sessionId: String) {
        database.withTransaction {
            val session = sessionDao.getSession(sessionId) ?: return@withTransaction
            val restSetId = session.restSetId ?: return@withTransaction
            val restStartedAt = session.restStartedAt ?: return@withTransaction
            sessionDao.setRestAfter(restSetId, loggedRestSec(restStartedAt, clock.instant(), nextSetReps = null))
            sessionDao.updateSession(session.withoutRest())
        }
    }

    override suspend fun adjustRestTarget(sessionId: String, deltaSec: Int) {
        database.withTransaction {
            val session = sessionDao.getSession(sessionId) ?: return@withTransaction
            val target = session.restTargetSec ?: return@withTransaction
            sessionDao.updateSession(session.copy(restTargetSec = (target + deltaSec).coerceAtLeast(0)))
        }
    }

    override suspend fun finishSession(sessionId: String): Boolean = database.withTransaction {
        val session = sessionDao.getSession(sessionId) ?: return@withTransaction false
        if (session.endedAt != null) return@withTransaction true
        // "N set belum dicentang dan tidak akan disimpan" (design: Selesaikan sesi).
        sessionDao.deleteUncompletedSets(sessionId)
        sessionDao.deleteExercisesWithoutSets(sessionId)
        if (sessionDao.exerciseKeys(sessionId).isEmpty()) {
            sessionDao.deleteSession(sessionId)
            return@withTransaction false
        }
        // The rest after the last set is not counted, so the running timer is dropped without logging.
        val now = clock.instant()
        sessionDao.updateSession(session.withoutRest().copy(endedAt = now))
        pendingSyncDao.enqueue(SyncEntityType.SESSION, sessionId, now)
        true
    }

    override suspend fun deleteSession(sessionId: String) {
        database.withTransaction {
            val session = sessionDao.getSession(sessionId) ?: return@withTransaction
            val exerciseKeys = sessionDao.exerciseKeys(sessionId)
            sessionDao.deleteSession(sessionId)
            exerciseKeys.forEach { refreshRecords(it) }
            if (session.endedAt != null) {
                pendingSyncDao.enqueue(SyncEntityType.SESSION, sessionId, clock.instant(), deleted = true)
            }
        }
    }

    // Helpers below run inside the caller's transaction.

    private suspend fun insertSession(
        name: String,
        routineId: String?,
        snapshot: List<PlannedExercise>
    ): SessionEntity {
        val now = clock.instant()
        val session = SessionEntity(
            id = newId(),
            name = name,
            routineId = routineId,
            startedAt = now,
            endedAt = null,
            day = now.atZone(clock.zone).toLocalDate(),
            routineSnapshot = snapshot,
            restSetId = null,
            restStartedAt = null,
            restTargetSec = null
        )
        sessionDao.insertSession(session)
        return session
    }

    private suspend fun insertExercise(
        sessionId: String,
        position: Int,
        exercise: ExerciseEntity,
        sets: List<PlannedSet>
    ): String {
        val id = newId()
        sessionDao.insertExercise(
            SessionExerciseEntity(
                id = id,
                sessionId = sessionId,
                position = position,
                exerciseKey = exercise.exerciseKey,
                nameSnapshot = exercise.name,
                trackingType = exercise.trackingType
            )
        )
        sessionDao.insertSets(sets.mapIndexed { index, set -> set.toEntity(newId(), id, index) })
        return id
    }

    private suspend fun parentsOf(set: SetEntity): Pair<SessionExerciseEntity, SessionEntity>? {
        val exercise = sessionDao.getExercise(set.sessionExerciseId) ?: return null
        val session = sessionDao.getSession(exercise.sessionId) ?: return null
        return exercise to session
    }

    /**
     * Recomputes the record flags of one exercise from its whole history, so edits and deletes in older sessions
     * keep later flags right. Finished sessions whose flags change are queued for sync.
     */
    private suspend fun refreshRecords(exerciseKey: String) {
        val sets = sessionDao.completedSetsOf(exerciseKey)
        val records = personalRecordSetIds(sets.recordCandidates())
        val changedSessions = mutableSetOf<String>()
        for (row in sets) {
            val isPr = row.setId in records
            if (row.isPr == isPr) continue
            sessionDao.setPr(row.setId, isPr)
            if (row.sessionFinished) changedSessions += row.sessionId
        }
        val now = clock.instant()
        changedSessions.forEach { pendingSyncDao.enqueue(SyncEntityType.SESSION, it, now) }
    }

    /** Edits to a finished session (history "Ubah") have to reach the cloud copy. */
    private suspend fun markChanged(session: SessionEntity) {
        if (session.endedAt != null) pendingSyncDao.enqueue(SyncEntityType.SESSION, session.id, clock.instant())
    }

    // Records exist for load + reps sets only, as in the design prototype (bodyweight, time and distance have none).
    private fun List<CompletedSetRow>.recordCandidates() = filter { it.trackingType == TrackingType.WEIGHT_REPS }
        .map { RecordCandidate(it.setId, it.sessionId, it.weightKg, it.reps, it.isWarmup) }

    private fun SessionEntity.withoutRest() = copy(restSetId = null, restStartedAt = null, restTargetSec = null)

    private fun PlannedSet.toEntity(id: String, sessionExerciseId: String, position: Int) = SetEntity(
        id = id,
        sessionExerciseId = sessionExerciseId,
        position = position,
        weightKg = values.weightKg,
        reps = values.reps,
        durationSec = values.durationSec,
        distanceM = values.distanceM,
        isWarmup = isWarmup,
        completedAt = null,
        restAfterSec = null,
        isPr = false
    )

    private fun newId(): String = UUID.randomUUID().toString()
}
