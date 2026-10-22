package com.medstudy.feature.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryModelTest {
    private val day = 86_400_000L

    @Test
    fun `no reviews has no stability`() = assertNull(MemoryModel.stability(emptyList()))

    @Test
    fun `retrievability is 90 percent after one stability period and decays`() {
        assertEquals(1.0, MemoryModel.retrievability(0.0, 3.0), 1e-9)
        assertEquals(0.9, MemoryModel.retrievability(3.0, 3.0), 1e-9)
        assertTrue(MemoryModel.retrievability(30.0, 3.0) < MemoryModel.retrievability(3.0, 3.0))
    }

    @Test
    fun `spaced correct reviews grow stability more than cramming`() {
        val spaced = MemoryModel.stability(listOf(Review(0, true), Review(5 * day, true)))!!
        val crammed = MemoryModel.stability(listOf(Review(0, true), Review(1000, true)))!!
        assertTrue(spaced > crammed)
        assertEquals(2.0, crammed, 0.01)
    }

    @Test
    fun `a miss cuts stability and never goes below the floor`() {
        val after = MemoryModel.stability(listOf(Review(0, true), Review(day, false)))!!
        assertEquals(0.8, after, 1e-9)
        val floored = MemoryModel.stability(List(5) { Review(it * day, false) })!!
        assertEquals(0.5, floored, 1e-9)
    }

    @Test
    fun `reviews are replayed in time order regardless of input order`() {
        val a = listOf(Review(0, true), Review(3 * day, false))
        assertEquals(MemoryModel.stability(a), MemoryModel.stability(a.reversed()))
    }
}
