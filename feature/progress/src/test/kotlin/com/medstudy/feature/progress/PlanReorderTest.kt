package com.medstudy.feature.progress

import com.medstudy.core.model.StudyBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanReorderTest {
    private fun block(id: String, date: String, position: Int = 0) =
        StudyBlock(id = id, studyDateIso = date, subjectId = "s", topicId = null, title = id, questionTarget = 10, minuteTarget = 30, position = position)

    private val week = listOf(block("a", "2026-11-16"), block("b", "2026-11-17"), block("c", "2026-11-18"), block("d", "2026-11-19"))

    private fun List<StudyBlock>.days() = inPlanOrder().map { "${it.id}:${it.studyDateIso.takeLast(2)}" }

    @Test
    fun draggingABlockDownGivesItThatDayAndMovesTheOthersUp() {
        assertEquals(listOf("b:16", "c:17", "a:18", "d:19"), reorderPlanBlocks(week, "a", 2).days())
    }

    @Test
    fun draggingABlockUpGivesItThatDayAndMovesTheOthersDown() {
        assertEquals(listOf("d:16", "a:17", "b:18", "c:19"), reorderPlanBlocks(week, "d", 0).days())
    }

    @Test
    fun theSameDaysAreUsedBeforeAndAfter() {
        val twoOnOneDay = listOf(block("a", "2026-11-16", 0), block("b", "2026-11-16", 1), block("c", "2026-11-18"))
        val moved = reorderPlanBlocks(twoOnOneDay, "c", 0)
        assertEquals(listOf("c:16", "a:16", "b:18"), moved.days())
        assertEquals(twoOnOneDay.map { it.studyDateIso }.sorted(), moved.map { it.studyDateIso }.sorted())
    }

    @Test
    fun droppingABlockWhereItStartedOrNamingAnUnknownBlockChangesNothing() {
        assertEquals(week, reorderPlanBlocks(week, "b", 1))
        assertEquals(week, reorderPlanBlocks(week, "zz", 0))
    }
}
