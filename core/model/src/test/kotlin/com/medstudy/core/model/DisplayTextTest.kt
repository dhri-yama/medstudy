package com.medstudy.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DisplayTextTest {
    @Test
    fun `splits a single trailing source tag`() {
        assertEquals("Lendrum's stain is done for:" to "NEET 13", splitSourceTag("Lendrum's stain is done for: NEET 13"))
    }

    @Test
    fun `splits a multi source tag with no space after the colon`() {
        val (stem, tag) = splitSourceTag("Questionnaire is used in:AP 07; MP 09; NEET 13")
        assertEquals("Questionnaire is used in:", stem)
        assertEquals("AP 07; MP 09; NEET 13", tag)
    }

    @Test
    fun `leaves stems that merely end in a number alone`() {
        val text = "The drug of choice in Type 2 Diabetes 10"
        assertEquals(text to null, splitSourceTag(text))
        assertNull(splitSourceTag("What is 2 + 2 in 12").second)
    }

    @Test
    fun `leaves normal questions alone`() {
        val text = "Anti-centromere antibodies are seen in which of the following conditions?"
        assertEquals(text to null, splitSourceTag(text))
    }
}
