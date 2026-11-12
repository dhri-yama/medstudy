package com.medstudy.feature.practice

import org.junit.Assert.assertEquals
import org.junit.Test

class MockTestRulesTest {
    @Test
    fun aFullNeetPgPaperGets210Minutes() {
        assertEquals(210, MockTestRules.durationMinutes(200))
    }

    @Test
    fun shortPapersRoundUpToAWholeMinute() {
        assertEquals(2, MockTestRules.durationMinutes(1))
        assertEquals(11, MockTestRules.durationMinutes(10))
    }

    @Test
    fun wrongAnswersCostAMarkAndSkippedOnesCostNothing() {
        assertEquals(4 * 150 - 30, MockTestRules.score(correct = 150, wrong = 30))
        assertEquals(-5, MockTestRules.score(correct = 0, wrong = 5))
        assertEquals(0, MockTestRules.score(correct = 0, wrong = 0))
    }
}
