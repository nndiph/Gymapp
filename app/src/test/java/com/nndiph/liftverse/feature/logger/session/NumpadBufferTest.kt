package com.nndiph.liftverse.feature.logger.session

import org.junit.Assert.assertEquals
import org.junit.Test

class NumpadBufferTest {
    @Test
    fun `the first key replaces the old value`() {
        val typed = NumpadBuffer.of(80.0, allowDecimal = true).digit(8).digit(2).comma().digit(5)
        assertEquals("82,5", typed.text)
        assertEquals(82.5, typed.value(), 0.0)
    }

    @Test
    fun `delete on a fresh value clears it, then removes one character`() {
        assertEquals("", NumpadBuffer.of(80.0, allowDecimal = true).delete().text)
        assertEquals("8", NumpadBuffer("82", fresh = false).delete().text)
    }

    @Test
    fun `steps keep the value at zero or above`() {
        assertEquals("85", NumpadBuffer.of(82.5, allowDecimal = true).step(2.5, allowDecimal = true).text)
        assertEquals("0", NumpadBuffer.of(1.0, allowDecimal = true).step(-2.5, allowDecimal = true).text)
        assertEquals("9", NumpadBuffer.of(8.0, allowDecimal = false).step(1.0, allowDecimal = false).text)
    }

    @Test
    fun `at most five digits, one comma, and no leading zero`() {
        var buffer = NumpadBuffer.of(0.0, allowDecimal = true)
        repeat(7) { buffer = buffer.digit(9) }
        assertEquals("99999", buffer.text)
        assertEquals("1,5", NumpadBuffer("0", fresh = false).digit(1).comma().comma().digit(5).text)
        assertEquals("0,", NumpadBuffer.of(3.0, allowDecimal = true).comma().text)
    }
}
