package com.nndiph.liftverse.core.format

import java.text.NumberFormat
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
