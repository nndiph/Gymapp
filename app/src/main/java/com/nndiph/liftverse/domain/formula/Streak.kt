package com.nndiph.liftverse.domain.formula

import com.nndiph.liftverse.domain.model.StreakSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * How far back screens read history to compute a streak. Local queries are cheap, but a longer streak than this
 * shows as capped.
 */
const val STREAK_LOOKBACK_WEEKS = 104L

/** Weeks run Monday to Sunday (SPEC: "minggu Senin–Minggu"). */
fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * The streak under the user's schedule (SPEC "Rumus", prototype `calcStreak`). [sessionsByDay] holds the finished
 * sessions per local day.
 *
 * - Weekly target: +1 per Monday–Sunday week with at least `target` sessions. The running week only adds once
 *   reached and never breaks the streak.
 * - Fixed days: +1 per day with a session, rest-day sessions included. A training day missed before today breaks
 *   the streak, unless swapping is allowed and the week still has as many session days as training days (the
 *   running week always may still catch up). After a break only the days after the last missed day count.
 */
fun streakCount(schedule: StreakSchedule, sessionsByDay: Map<LocalDate, Int>, today: LocalDate): Int {
    val sessionDays = sessionsByDay.filterValues { it > 0 }.keys.filter { it <= today }
    val firstDay = sessionDays.minOrNull() ?: return 0
    val currentWeek = today.weekStart()
    var weekStart = currentWeek
    var streak = 0
    while (!weekStart.isBefore(firstDay.weekStart())) {
        val week = (0L..6L).map { weekStart.plusDays(it) }.filter { it <= today }
        val isCurrentWeek = weekStart == currentWeek
        when (schedule) {
            is StreakSchedule.WeeklyTarget -> {
                val sessions = week.sumOf { sessionsByDay[it] ?: 0 }
                when {
                    sessions >= schedule.target -> streak++
                    isCurrentWeek -> Unit
                    else -> return streak
                }
            }
            is StreakSchedule.FixedDays -> {
                if (schedule.days.isEmpty()) return 0
                val done = week.filter { (sessionsByDay[it] ?: 0) > 0 }
                val missed = week.filter { it.dayOfWeek in schedule.days && it < today && it !in done }
                val covered = missed.isEmpty() ||
                    (schedule.allowSwap && (isCurrentWeek || done.size >= schedule.days.size))
                if (!covered) {
                    val lastMissed = missed.max()
                    return streak + done.count { it > lastMissed }
                }
                streak += done.size
            }
        }
        weekStart = weekStart.minusWeeks(1)
    }
    return streak
}
