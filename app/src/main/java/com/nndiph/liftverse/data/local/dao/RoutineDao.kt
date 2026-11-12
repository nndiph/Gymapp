package com.nndiph.liftverse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nndiph.liftverse.data.local.entity.RoutineEntity
import com.nndiph.liftverse.data.local.entity.RoutineExerciseEntity
import com.nndiph.liftverse.data.local.entity.RoutineWithExercises
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Transaction
    @Query("SELECT * FROM routines WHERE archived = 0 ORDER BY createdAt")
    fun observeActive(): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun get(id: String): RoutineWithExercises?

    @Insert
    suspend fun insert(routine: RoutineEntity)

    @Update
    suspend fun update(routine: RoutineEntity)

    /** Returns the number of routines changed (0 when unknown or already deleted). */
    @Query("UPDATE routines SET archived = 1, updatedAt = :now WHERE id = :id AND archived = 0")
    suspend fun archive(id: String, now: Instant): Int

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteExercises(routineId: String)

    @Insert
    suspend fun insertExercises(exercises: List<RoutineExerciseEntity>)
}
