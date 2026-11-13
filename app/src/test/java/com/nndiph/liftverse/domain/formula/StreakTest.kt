package com.nndiph.liftverse.domain.formula

import com.nndiph.liftverse.domain.model.StreakSchedule
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {
    // Thursday; the running week is Monday 9 – Sunday 15 November 2026.
    private val today = LocalDate.of(2026, 11, 12)

    private fun days(vararg dates: String): Map<LocalDate, Int> = dates.groupingBy { LocalDate.parse(it) }.eachCount()

    @Test
    fun `weeks start on Monday`() {
        assertEquals(LocalDate.of(2026, 11, 9), today.weekStart())
        assertEquals(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 15).weekStart())
    }

    @Test
    fun `weekly target counts reached weeks and the running week only once reached`() {
        val sessions = days(
            "2026-11-09",
            "2026-11-11",
            "2026-11-02",
            "2026-11-04",
            "2026-11-06",
            "2026-10-26",
            "2026-10-27",
            "2026-10-28",
            "2026-10-29",
            "2026-10-20"
        )
        val target = StreakSchedule.WeeklyTarget(3)
        // Running week 2 of 3: not counted, not broken. Two reached weeks, then 1 session breaks it.
        assertEquals(2, streakCount(target, sessions, today))
        assertEquals(3, streakCount(target, sessions + (today to 1), today))
    }

    @Test
    fun `fixed days count session days and a missed training day breaks the streak`() {
        val schedule = StreakSchedule.FixedDays(setOf(MONDAY, WEDNESDAY, FRIDAY))
        val sessions = days(
            "2026-11-09",
            "2026-11-11",
            "2026-11-02",
            "2026-11-04",
            "2026-11-06",
            "2026-10-26",
            "2026-10-28"
        )
        // 2 this week + 3 last week; the week before missed Friday 30 October with nothing after it.
        assertEquals(5, streakCount(schedule, sessions, today))
    }

    @Test
    fun `a swap within the week covers a missed training day only when allowed`() {
        val sessions = days("2026-11-09", "2026-11-11", "2026-11-02", "2026-11-04", "2026-11-07")
        val days = setOf(MONDAY, WEDNESDAY, FRIDAY)
        // Last week: Saturday instead of Friday.
        assertEquals(5, streakCount(StreakSchedule.FixedDays(days, allowSwap = true), sessions, today))
        // Without swaps Friday 6 breaks it; only Saturday 7 after it still counts.
        assertEquals(3, streakCount(StreakSchedule.FixedDays(days, allowSwap = false), sessions, today))
    }

    @Test
    fun `the running week may still catch up when swaps are allowed`() {
        val sessions = days("2026-11-09")
        val days = setOf(MONDAY, WEDNESDAY)
        assertEquals(1, streakCount(StreakSchedule.FixedDays(days, allowSwap = true), sessions, today))
        assertEquals(0, streakCount(StreakSchedule.FixedDays(days, allowSwap = false), sessions, today))
    }

    @Test
    fun `sessions on rest days add to the streak and later days are ignored`() {
        val schedule = StreakSchedule.FixedDays(setOf(MONDAY, THURSDAY))
        val sessions = days("2026-11-09", "2026-11-10", "2026-11-12", "2026-11-14")
        assertEquals(3, streakCount(schedule, sessions, today))
    }

    @Test
    fun `no sessions means no streak`() {
        assertEquals(0, streakCount(StreakSchedule.DEFAULT, emptyMap(), today))
        assertEquals(0, streakCount(StreakSchedule.FixedDays(emptySet()), days("2026-11-09"), today))
    }
}
