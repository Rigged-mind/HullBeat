package app.hullbeat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecimalInputTest {

    @Test
    fun `comma is a decimal separator`() {
        // The case that silently dropped a meter reading: toDoubleOrNull
        // gives null here, and the form read that as an empty field.
        assertNull("1234,5".toDoubleOrNull())
        assertEquals(1234.5, DecimalInput.parse("1234,5")!!, 0.0)
    }

    @Test
    fun `dot still works`() {
        assertEquals(1234.5, DecimalInput.parse("1234.5")!!, 0.0)
        assertEquals(12.0, DecimalInput.parse("12.")!!, 0.0)
        assertEquals(0.5, DecimalInput.parse(",5")!!, 0.0)
    }

    @Test
    fun `zero is a reading`() {
        // A replaced hour counter starts at zero.
        assertEquals(0.0, DecimalInput.parse("0")!!, 0.0)
    }

    @Test
    fun `grouping spaces are dropped`() {
        assertEquals(1234.5, DecimalInput.parse("1 234,5")!!, 0.0)
        assertEquals(1234.0, DecimalInput.parse("1 234")!!, 0.0)
    }

    @Test
    fun `ambiguous or invalid input is refused`() {
        assertNull(DecimalInput.parse("1.234,5"))
        assertNull(DecimalInput.parse("1,2,3"))
        assertNull(DecimalInput.parse("-5"))
        assertNull(DecimalInput.parse("abc"))
        assertNull(DecimalInput.parse("NaN"))
        assertNull(DecimalInput.parse("Infinity"))
        assertNull(DecimalInput.parse("1e5"))
        assertNull(DecimalInput.parse(","))
    }

    @Test
    fun `blank is acceptable, garbage is not`() {
        assertTrue(DecimalInput.isAcceptable(""))
        assertTrue(DecimalInput.isAcceptable("  "))
        assertTrue(DecimalInput.isAcceptable("12,5"))
        assertFalse(DecimalInput.isAcceptable("12,5,"))
        assertFalse(DecimalInput.isAcceptable("-1"))
    }

    @Test
    fun `partial input allows a trailing separator`() {
        assertTrue(DecimalInput.isPartialInput("12,"))
        assertTrue(DecimalInput.isPartialInput("12."))
        assertFalse(DecimalInput.isPartialInput("12,,"))
        assertFalse(DecimalInput.isPartialInput("1a"))
    }
}
