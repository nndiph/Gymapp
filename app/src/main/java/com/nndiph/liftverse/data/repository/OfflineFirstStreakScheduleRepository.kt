package com.nndiph.liftverse.data.repository

import androidx.room.withTransaction
import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.data.local.dao.enqueue
import com.nndiph.liftverse.data.local.entity.SyncEntityType
import com.nndiph.liftverse.data.local.toDomain
import com.nndiph.liftverse.data.local.toEntity
import com.nndiph.liftverse.domain.model.StreakSchedule
import com.nndiph.liftverse.domain.repository.StreakScheduleRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstStreakScheduleRepository @Inject constructor(
    private val database: LiftverseDatabase,
    private val clock: Clock
) : StreakScheduleRepository {
    private val scheduleDao = database.streakScheduleDao()
    private val pendingSyncDao = database.pendingSyncDao()

    override fun observeSchedule(): Flow<StreakSchedule> =
        scheduleDao.observe().map { it?.toDomain() ?: StreakSchedule.DEFAULT }

    override suspend fun setSchedule(schedule: StreakSchedule) {
        val now = clock.instant()
        database.withTransaction {
            scheduleDao.upsert(schedule.toEntity(now))
            pendingSyncDao.enqueue(SyncEntityType.STREAK_SCHEDULE, SYNC_ID, now)
        }
    }

    private companion object {
        const val SYNC_ID = "streak_schedule"
    }
}
