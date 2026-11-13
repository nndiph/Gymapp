package com.nndiph.liftverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.Instant

/** Built-in and custom exercises in one table (handoff README: "Room untuk Exercise (bawaan + custom)"). */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    /** `seed:<slug>` or `custom:<id>`. */
    @PrimaryKey val exerciseKey: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
    val trackingType: TrackingType,
    /** Extra search terms, mostly for seed exercises. */
    val aliases: List<String>,
    val basedOn: String?,
    val notes: String?,
    val archived: Boolean,
    /** Null for seed rows. */
    val createdAt: Instant?,
    val updatedAt: Instant?,
    /** Load step; null uses the default for [equipment]. Added in schema version 2. */
    val weightStepKg: Double? = null
)
