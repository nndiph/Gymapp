package com.nndiph.liftverse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nndiph.liftverse.data.local.entity.ExerciseEntity
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.MuscleGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    // instr() instead of LIKE so "%" and "_" typed by the user are not wildcards.
    @Query(
        """
        SELECT * FROM exercises
        WHERE archived = 0
          AND (:query = '' OR instr(lower(name), lower(:query)) > 0 OR instr(lower(aliases), lower(:query)) > 0)
          AND (:muscleGroup IS NULL OR muscleGroup = :muscleGroup)
          AND (:equipment IS NULL OR equipment = :equipment)
        ORDER BY name COLLATE NOCASE
        """
    )
    fun observeActive(query: String, muscleGroup: MuscleGroup?, equipment: Equipment?): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE exerciseKey = :key")
    suspend fun get(key: String): ExerciseEntity?

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity)

    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM session_exercises WHERE exerciseKey = :key)")
    suspend fun hasHistory(key: String): Boolean
}
