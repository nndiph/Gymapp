package com.nndiph.liftverse.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.nndiph.liftverse.domain.model.PlannedExercise
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.Instant
import java.time.LocalDate

/** A session; running while [endedAt] is null. The rest timer lives here so it survives process death. */
@Entity(tableName = "sessions", indices = [Index("day"), Index("endedAt")])
data class SessionEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Routine the session started from. Not a foreign key: routines are only soft-deleted. */
    val routineId: String?,
    val startedAt: Instant,
    val endedAt: Instant?,
    val day: LocalDate,
    val routineSnapshot: List<PlannedExercise>,
    /** The set whose rest is being timed, when its timer runs. */
    val restSetId: String?,
    val restStartedAt: Instant?,
    val restTargetSec: Int?
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("exerciseKey")]
)
data class SessionExerciseEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val position: Int,
    /** Not a foreign key: history must outlive exercises removed from the seed. */
    val exerciseKey: String,
    val nameSnapshot: String,
    val trackingType: TrackingType
)

/** One set. `set_entries` avoids the SQL keyword SET. */
@Entity(
    tableName = "set_entries",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionExerciseId")]
)
data class SetEntity(
    @PrimaryKey val id: String,
    val sessionExerciseId: String,
    val position: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
    val distanceM: Int?,
    val isWarmup: Boolean,
    val completedAt: Instant?,
    val restAfterSec: Int?,
    val isPr: Boolean
)

data class SessionExerciseWithSets(
    @Embedded val exercise: SessionExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId")
    val sets: List<SetEntity>
)

data class SessionWithExercises(
    @Embedded val session: SessionEntity,
    @Relation(entity = SessionExerciseEntity::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<SessionExerciseWithSets>
)

/** Query row: one finished session with its totals. */
data class SessionSummaryRow(
    val id: String,
    val name: String,
    val day: LocalDate,
    val startedAt: Instant,
    val endedAt: Instant,
    val exerciseCount: Int,
    val setCount: Int,
    val volumeKg: Double,
    val restSec: Int,
    val prCount: Int
)

/** Query row: totals of one day. */
data class DayStatsRow(
    val day: LocalDate,
    val sessionCount: Int,
    val setCount: Int,
    val volumeKg: Double,
    val durationSec: Long
)

/** Query row: a completed set of one exercise, for recomputing record flags. */
data class CompletedSetRow(
    val setId: String,
    val sessionId: String,
    val sessionStartedAt: Instant,
    val sessionFinished: Boolean,
    val trackingType: TrackingType,
    val weightKg: Double?,
    val reps: Int?,
    val isWarmup: Boolean,
    val isPr: Boolean
)

/** Query row: volume of one exercise in one finished session. */
data class ExerciseVolumeRow(val day: LocalDate, val volumeKg: Double)
