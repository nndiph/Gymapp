package com.nndiph.liftverse.domain.model

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY

/** The user's training schedule. Streaks follow it instead of calendar days (SPEC "Rumus"). */
sealed interface StreakSchedule {
    /** +1 per training day done. A missed training day breaks the streak unless a swap within the week covers it. */
    data class FixedDays(
        val days: Set<DayOfWeek>,
        val allowSwap: Boolean = true,
        /** The split preset the days came from, so the schedule screen can show the choice. */
        val preset: SplitPreset? = null
    ) : StreakSchedule

    /** +1 per Monday–Sunday week with at least [target] sessions. */
    data class WeeklyTarget(val target: Int) : StreakSchedule {
        init {
            require(target in 1..7) { "Weekly target must be 1..7, was $target" }
        }
    }

    companion object {
        /** Used when the user skips the schedule step of onboarding. */
        val DEFAULT: StreakSchedule = WeeklyTarget(3)
    }
}

/** Split presets only fill [StreakSchedule.FixedDays.days]. A null [days] is filled from elsewhere. */
enum class SplitPreset(val days: Set<DayOfWeek>?) {
    /** "Ikuti routine": the union of all routine days at the moment it is chosen. */
    FOLLOW_ROUTINES(null),
    PPL_UPPER_LOWER(setOf(TUESDAY, WEDNESDAY, THURSDAY, SATURDAY, SUNDAY)),
    UPPER_LOWER(setOf(MONDAY, TUESDAY, THURSDAY, FRIDAY)),
    PPL_TWICE(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)),
    FULL_BODY(setOf(MONDAY, WEDNESDAY, FRIDAY)),

    /** "Atur sendiri": the user picks the days. */
    CUSTOM(null)
}
