package com.nndiph.liftverse.data.repository

import androidx.room.withTransaction
import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.data.local.dao.enqueue
import com.nndiph.liftverse.data.local.entity.RoutineEntity
import com.nndiph.liftverse.data.local.entity.SyncEntityType
import com.nndiph.liftverse.data.local.toDomain
import com.nndiph.liftverse.data.local.toEntities
import com.nndiph.liftverse.domain.model.Routine
import com.nndiph.liftverse.domain.model.RoutineDraft
import com.nndiph.liftverse.domain.repository.RoutineRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstRoutineRepository @Inject constructor(
    private val database: LiftverseDatabase,
    private val clock: Clock
) : RoutineRepository {
    private val routineDao = database.routineDao()
    private val pendingSyncDao = database.pendingSyncDao()

    override fun observeRoutines(): Flow<List<Routine>> =
        routineDao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getRoutine(id: String): Routine? =
        routineDao.get(id)?.takeUnless { it.routine.archived }?.toDomain()

    override suspend fun createRoutine(draft: RoutineDraft): String {
        draft.validate()
        val id = UUID.randomUUID().toString()
        val now = clock.instant()
        database.withTransaction {
            routineDao.insert(
                RoutineEntity(
                    id = id,
                    name = draft.name.trim(),
                    weekdays = draft.weekdays,
                    reminderEnabled = draft.reminderEnabled,
                    reminderTime = draft.reminderTime,
                    archived = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
            routineDao.insertExercises(draft.exercises.toEntities(id))
            pendingSyncDao.enqueue(SyncEntityType.ROUTINE, id, now)
        }
        return id
    }

    override suspend fun updateRoutine(id: String, draft: RoutineDraft) {
        draft.validate()
        database.withTransaction {
            val current = routineDao.get(id)?.routine?.takeUnless { it.archived } ?: return@withTransaction
            val now = clock.instant()
            routineDao.update(
                current.copy(
                    name = draft.name.trim(),
                    weekdays = draft.weekdays,
                    reminderEnabled = draft.reminderEnabled,
                    reminderTime = draft.reminderTime,
                    updatedAt = now
                )
            )
            routineDao.deleteExercises(id)
            routineDao.insertExercises(draft.exercises.toEntities(id))
            pendingSyncDao.enqueue(SyncEntityType.ROUTINE, id, now)
        }
    }

    override suspend fun deleteRoutine(id: String) {
        val now = clock.instant()
        database.withTransaction {
            if (routineDao.archive(id, now) > 0) pendingSyncDao.enqueue(SyncEntityType.ROUTINE, id, now)
        }
    }

    private fun RoutineDraft.validate() {
        require(name.isNotBlank()) { "Routine name is blank" }
        require(exercises.all { it.targetSets > 0 }) { "Every routine exercise needs at least one set" }
    }
}
