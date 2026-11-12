package com.nndiph.liftverse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nndiph.liftverse.data.local.entity.StreakScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakScheduleDao {
    @Query("SELECT * FROM streak_schedule WHERE id = ${StreakScheduleEntity.SINGLE_ROW_ID}")
    fun observe(): Flow<StreakScheduleEntity?>

    @Upsert
    suspend fun upsert(schedule: StreakScheduleEntity)
}
