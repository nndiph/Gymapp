package com.nndiph.liftverse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nndiph.liftverse.data.local.dao.ExerciseDao
import com.nndiph.liftverse.data.local.dao.PendingSyncDao
import com.nndiph.liftverse.data.local.dao.RoutineDao
import com.nndiph.liftverse.data.local.dao.SessionDao
import com.nndiph.liftverse.data.local.dao.StreakScheduleDao
import com.nndiph.liftverse.data.local.entity.ExerciseEntity
import com.nndiph.liftverse.data.local.entity.PendingSyncEntity
import com.nndiph.liftverse.data.local.entity.RoutineEntity
import com.nndiph.liftverse.data.local.entity.RoutineExerciseEntity
import com.nndiph.liftverse.data.local.entity.SessionEntity
import com.nndiph.liftverse.data.local.entity.SessionExerciseEntity
import com.nndiph.liftverse.data.local.entity.SetEntity
import com.nndiph.liftverse.data.local.entity.StreakScheduleEntity

/**
 * The on-device source of truth for training data (handoff README: "Room sebagai sumber kebenaran").
 * Schemas are exported to app/schemas; write a migration for every version bump once the app is released.
 */
@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        SessionEntity::class,
        SessionExerciseEntity::class,
        SetEntity::class,
        StreakScheduleEntity::class,
        PendingSyncEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LiftverseDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    abstract fun routineDao(): RoutineDao

    abstract fun sessionDao(): SessionDao

    abstract fun streakScheduleDao(): StreakScheduleDao

    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        const val NAME = "liftverse.db"
    }
}
