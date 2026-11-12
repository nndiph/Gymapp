package com.nndiph.liftverse.core.format

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
    @Test
    fun `numbers use the id-ID format from the design`() {
        assertEquals("41.200", formatDecimal(41_200.0))
        assertEquals("116,7", formatDecimal(116.6666, maxFractionDigits = 1))
        assertEquals("2,5", formatDecimal(2.5))
        assertEquals("80", formatDecimal(80.0))
        assertEquals("1250,5", formatEditableDecimal(1_250.5))
    }

    @Test
    fun `clock shows minutes and seconds, hours from one hour`() {
        assertEquals("0:00", formatClock(0))
        assertEquals("1:12", formatClock(72))
        assertEquals("24:51", formatClock(1_491))
        assertEquals("1:05:12", formatClock(3_912))
        assertEquals("0:00", formatClock(-5))
    }

    @Test
    fun `distance is shown in kilometres`() {
        assertEquals("2", formatKilometres(2_000))
        assertEquals("2,1", formatKilometres(2_100))
    }
}
