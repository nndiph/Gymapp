package com.nndiph.liftverse.data.repository

import androidx.room.withTransaction
import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.data.local.dao.enqueue
import com.nndiph.liftverse.data.local.entity.ExerciseEntity
import com.nndiph.liftverse.data.local.entity.SyncEntityType
import com.nndiph.liftverse.data.local.toDomain
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.Exercise
import com.nndiph.liftverse.domain.model.ExerciseDraft
import com.nndiph.liftverse.domain.model.ExerciseKeys
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.repository.ExerciseRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstExerciseRepository @Inject constructor(
    private val database: LiftverseDatabase,
    private val clock: Clock
) : ExerciseRepository {
    private val exerciseDao = database.exerciseDao()
    private val pendingSyncDao = database.pendingSyncDao()

    override fun observeExercises(
        query: String,
        muscleGroup: MuscleGroup?,
        equipment: Equipment?
    ): Flow<List<Exercise>> = exerciseDao.observeActive(query.trim(), muscleGroup, equipment)
        .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getExercise(key: String): Exercise? = exerciseDao.get(key)?.toDomain()

    override suspend fun createCustomExercise(draft: ExerciseDraft): String {
        require(draft.name.isNotBlank()) { "Exercise name is blank" }
        val key = ExerciseKeys.custom(UUID.randomUUID().toString())
        val now = clock.instant()
        database.withTransaction {
            exerciseDao.upsert(
                ExerciseEntity(
                    exerciseKey = key,
                    name = draft.name.trim(),
                    muscleGroup = draft.muscleGroup,
                    equipment = draft.equipment,
                    trackingType = draft.trackingType,
                    aliases = emptyList(),
                    basedOn = draft.basedOn,
                    notes = draft.notes?.trim()?.ifEmpty { null },
                    archived = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
            pendingSyncDao.enqueue(SyncEntityType.CUSTOM_EXERCISE, key, now)
        }
        return key
    }

    override suspend fun updateCustomExercise(key: String, draft: ExerciseDraft) {
        require(key.startsWith(ExerciseKeys.CUSTOM_PREFIX)) { "Only custom exercises can be edited: $key" }
        require(draft.name.isNotBlank()) { "Exercise name is blank" }
        database.withTransaction {
            val current = requireNotNull(exerciseDao.get(key)) { "Unknown exercise $key" }
            check(draft.trackingType == current.trackingType || !exerciseDao.hasHistory(key)) {
                "Tracking type of $key is locked because sessions use it"
            }
            val now = clock.instant()
            exerciseDao.upsert(
                current.copy(
                    name = draft.name.trim(),
                    muscleGroup = draft.muscleGroup,
                    equipment = draft.equipment,
                    trackingType = draft.trackingType,
                    basedOn = draft.basedOn,
                    notes = draft.notes?.trim()?.ifEmpty { null },
                    updatedAt = now
                )
            )
            pendingSyncDao.enqueue(SyncEntityType.CUSTOM_EXERCISE, key, now)
        }
    }

    override suspend fun archiveCustomExercise(key: String) {
        require(key.startsWith(ExerciseKeys.CUSTOM_PREFIX)) { "Only custom exercises can be archived: $key" }
        database.withTransaction {
            val current = exerciseDao.get(key) ?: return@withTransaction
            if (current.archived) return@withTransaction
            val now = clock.instant()
            exerciseDao.upsert(current.copy(archived = true, updatedAt = now))
            pendingSyncDao.enqueue(SyncEntityType.CUSTOM_EXERCISE, key, now)
        }
    }

    override suspend fun isTrackingTypeLocked(key: String): Boolean = exerciseDao.hasHistory(key)

    override suspend fun upsertSeedExercises(exercises: List<Exercise>) {
        require(exercises.all { it.key.startsWith(ExerciseKeys.SEED_PREFIX) }) { "Seed keys must start with seed:" }
        exerciseDao.upsertAll(
            exercises.map {
                ExerciseEntity(
                    exerciseKey = it.key,
                    name = it.name,
                    muscleGroup = it.muscleGroup,
                    equipment = it.equipment,
                    trackingType = it.trackingType,
                    aliases = it.aliases,
                    basedOn = it.basedOn,
                    notes = it.notes,
                    archived = it.archived,
                    createdAt = null,
                    updatedAt = null
                )
            }
        )
    }
}
