package com.nndiph.liftverse.data.local.entity

import androidx.room.Entity
import java.time.Instant

/**
 * Sync queue ("antrean sinkron"): one row per changed record, written in the same transaction as the change.
 * The WorkManager sync worker (not built yet) pushes these to Firestore and removes them.
 * Repeated changes to one record collapse into one row.
 */
@Entity(tableName = "pending_sync", primaryKeys = ["entityType", "entityId"])
data class PendingSyncEntity(
    val entityType: SyncEntityType,
    val entityId: String,
    /** True when the record must be deleted remotely. */
    val deleted: Boolean,
    val queuedAt: Instant
)

enum class SyncEntityType { CUSTOM_EXERCISE, ROUTINE, SESSION, STREAK_SCHEDULE }
