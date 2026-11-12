package com.nndiph.liftverse.domain.repository

import com.nndiph.liftverse.domain.model.StreakSchedule
import kotlinx.coroutines.flow.Flow

interface StreakScheduleRepository {
    /** Emits [StreakSchedule.DEFAULT] until the user saves a schedule. */
    fun observeSchedule(): Flow<StreakSchedule>

    suspend fun setSchedule(schedule: StreakSchedule)
}
