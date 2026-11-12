package com.nndiph.liftverse.data.local

import androidx.room.TypeConverter
import com.nndiph.liftverse.domain.model.PlannedExercise
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Column encodings. Dates are ISO text ("2026-11-10"), so `day` sorts and compares like `dayKey` in Firestore,
 * and times are "HH:mm" like `reminderTime` (docs/DATA_MODEL.md).
 */
class Converters {
    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToText(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun textToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun localTimeToText(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun textToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    /** Weekdays as a bit mask: Monday is bit 0, Sunday is bit 6. */
    @TypeConverter
    fun daysToMask(value: Set<DayOfWeek>): Int = value.fold(0) { mask, day -> mask or day.bit }

    @TypeConverter
    fun maskToDays(value: Int): Set<DayOfWeek> = DayOfWeek.entries.filterTo(mutableSetOf()) { value and it.bit != 0 }

    @TypeConverter
    fun stringsToText(value: List<String>): String = value.joinToString(LINE)

    @TypeConverter
    fun textToStrings(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(LINE)

    @TypeConverter
    fun plannedToText(value: List<PlannedExercise>): String =
        value.joinToString(LINE) { it.exerciseKey + FIELD + it.setCount }

    @TypeConverter
    fun textToPlanned(value: String): List<PlannedExercise> = if (value.isEmpty()) {
        emptyList()
    } else {
        value.split(LINE).map { line ->
            val (key, count) = line.split(FIELD)
            PlannedExercise(key, count.toInt())
        }
    }

    private val DayOfWeek.bit: Int get() = 1 shl (value - 1)

    private companion object {
        const val LINE = "\n"
        const val FIELD = "\t"
    }
}
