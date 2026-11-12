package com.nndiph.liftverse.feature.logger.session

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.nndiph.liftverse.core.format.formatEditableDecimal

/**
 * What the numpad shows while the user types (prototype `padKey`). The first key replaces the old value
 * ([fresh]); at most five digits; one comma for decimal fields; ±step never goes below zero.
 */
data class NumpadBuffer(val text: String, val fresh: Boolean = true) {
    val displayText: String get() = text.ifEmpty { "0" }

    fun digit(digit: Int): NumpadBuffer {
        val base = if (fresh) "" else text
        if (base.count(Char::isDigit) >= MAX_DIGITS) return NumpadBuffer(base, fresh = false)
        return NumpadBuffer(if (base == "0") digit.toString() else base + digit, fresh = false)
    }

    fun comma(): NumpadBuffer {
        val base = if (fresh) "" else text
        if (',' in base) return NumpadBuffer(base, fresh = false)
        return NumpadBuffer(base.ifEmpty { "0" } + ",", fresh = false)
    }

    fun delete(): NumpadBuffer = NumpadBuffer(if (fresh) "" else text.dropLast(1), fresh = false)

    fun step(delta: Double, allowDecimal: Boolean): NumpadBuffer =
        NumpadBuffer(format((value() + delta).coerceAtLeast(0.0), allowDecimal), fresh = false)

    fun value(): Double = text.replace(',', '.').toDoubleOrNull() ?: 0.0

    companion object {
        const val MAX_DIGITS = 5

        fun of(value: Double, allowDecimal: Boolean) = NumpadBuffer(format(value, allowDecimal), fresh = true)

        val Saver: Saver<NumpadBuffer, Any> = listSaver(
            save = { listOf(it.text, it.fresh) },
            restore = { NumpadBuffer(it[0] as String, it[1] as Boolean) }
        )

        private fun format(value: Double, allowDecimal: Boolean): String =
            formatEditableDecimal(value, maxFractionDigits = if (allowDecimal) 2 else 0)
    }
}
