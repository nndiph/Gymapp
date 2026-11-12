package com.nndiph.liftverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nndiph.liftverse.domain.model.SplitPreset
import java.time.DayOfWeek
import java.time.Instant

/** The single streak schedule row ("ScheduleConfig" in the handoff README). */
@Entity(tableName = "streak_schedule")
data class StreakScheduleEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val mode: ScheduleMode,
    /** Used when [mode] is FIXED_DAYS. */
    val days: Set<DayOfWeek>,
    val allowSwap: Boolean,
    /** Used when [mode] is WEEKLY_TARGET. */
    val weeklyTarget: Int,
    val preset: SplitPreset?,
    val updatedAt: Instant
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}

enum class ScheduleMode { FIXED_DAYS, WEEKLY_TARGET }
