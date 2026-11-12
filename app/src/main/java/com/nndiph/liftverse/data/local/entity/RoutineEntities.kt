package com.nndiph.liftverse.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val weekdays: Set<DayOfWeek>,
    val reminderEnabled: Boolean,
    val reminderTime: LocalTime?,
    /** Soft delete ("Hapus routine"). */
    val archived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Entity(
    tableName = "routine_exercises",
    primaryKeys = ["routineId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoutineExerciseEntity(
    val routineId: String,
    val position: Int,
    val exerciseKey: String,
    val nameSnapshot: String,
    val targetSets: Int,
    val targetReps: Int?
)

data class RoutineWithExercises(
    @Embedded val routine: RoutineEntity,
    @Relation(parentColumn = "id", entityColumn = "routineId")
    val exercises: List<RoutineExerciseEntity>
)
