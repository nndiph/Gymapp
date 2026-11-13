package com.nndiph.liftverse.core.format

import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAccessor
import java.util.Locale

// Display formats from the design: id-ID numbers (41.200 · 116,7 · 2,5) and clock times (2:30, 1:05:12).

private val INDONESIAN: Locale = Locale.forLanguageTag("id-ID")

/** id-ID number with up to [maxFractionDigits] decimals and no trailing zeros: 82,5 · 2.440 · 2. */
fun formatDecimal(value: Double, maxFractionDigits: Int = 2): String = NumberFormat.getNumberInstance(INDONESIAN)
    .apply {
        minimumFractionDigits = 0
        maximumFractionDigits = maxFractionDigits
    }
    .format(value)

/** The same without thousands separators, for an editable value such as the numpad display: 1250,5. */
fun formatEditableDecimal(value: Double, maxFractionDigits: Int = 2): String =
    NumberFormat.getNumberInstance(INDONESIAN)
        .apply {
            isGroupingUsed = false
            minimumFractionDigits = 0
            maximumFractionDigits = maxFractionDigits
        }
        .format(value)

/** Seconds as m:ss, or h:mm:ss from one hour. Negative values count as zero. */
fun formatClock(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    val rest = seconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, rest)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, rest)
    }
}

/** Metres shown as kilometres: 2000 → 2, 2500 → 2,5. */
fun formatKilometres(meters: Int): String = formatDecimal(meters / 1000.0)

private val DAY_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM", INDONESIAN)
private val DAY_DATE_PLAIN = DateTimeFormatter.ofPattern("EEEE d MMMM", INDONESIAN)
private val MONTH = DateTimeFormatter.ofPattern("MMMM", INDONESIAN)
private val TIME = DateTimeFormatter.ofPattern("HH.mm", INDONESIAN)

/** "Kamis, 12 November". */
fun formatDayDate(date: LocalDate): String = DAY_DATE.format(date)

/** "Senin 16 November", for "Berikutnya Push Day, Senin 16 November". */
fun formatDayDatePlain(date: LocalDate): String = DAY_DATE_PLAIN.format(date)

/** "Senin", "Kamis". */
fun formatWeekday(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, INDONESIAN)

/** "November". */
fun formatMonth(date: LocalDate): String = MONTH.format(date)

/** "18.30" (id-ID uses a dot between hours and minutes). */
fun formatTime(time: TemporalAccessor): String = TIME.format(time)

/** "+23%", "−5%" (true minus sign, as in the design), "0%". */
fun formatSignedPercent(percent: Int): String = signed(percent) + "%"

/** "+8", "−3", "0". */
fun formatSigned(value: Int): String = signed(value)

private fun signed(value: Int): String = when {
    value > 0 -> "+$value"
    value < 0 -> "−${-value}"
    else -> "0"
}
