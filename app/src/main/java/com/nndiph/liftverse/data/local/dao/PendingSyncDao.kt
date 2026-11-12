package com.nndiph.liftverse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nndiph.liftverse.data.local.entity.PendingSyncEntity
import com.nndiph.liftverse.data.local.entity.SyncEntityType
import java.time.Instant

@Dao
interface PendingSyncDao {
    /** Adds the record to the queue, or refreshes its entry if it is already queued. */
    @Upsert
    suspend fun upsert(item: PendingSyncEntity)

    @Query("SELECT * FROM pending_sync ORDER BY queuedAt")
    suspend fun getAll(): List<PendingSyncEntity>
}

suspend fun PendingSyncDao.enqueue(type: SyncEntityType, id: String, now: Instant, deleted: Boolean = false) =
    upsert(PendingSyncEntity(entityType = type, entityId = id, deleted = deleted, queuedAt = now))
