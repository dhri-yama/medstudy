package com.medstudy.feature.progress

import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.PracticeContext
import com.medstudy.core.model.QuestionAttempt
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionPlannerTest {
    private val today = LocalDate.parse("2026-10-22")

    private fun attempt(q: String, result: AttemptResult, date: LocalDate = today, at: Long = 0) = QuestionAttempt(
        id = "$q-$at", questionId = q, selectedOptionIds = emptyList(), result = result,
        completedAtEpochMillis = at, studyDateIso = date.toString(), timeZoneId = "UTC", context = PracticeContext.TOPIC_PRACTICE,
    )

    @Test
    fun `mistakes are questions whose latest answer was wrong`() {
        val attempts = listOf(
            attempt("q1", AttemptResult.INCORRECT, at = 1),
            attempt("q1", AttemptResult.CORRECT, at = 2),
            attempt("q2", AttemptResult.INCORRECT, at = 3),
        )
        assertEquals(listOf("q2"), SessionPlanner.mistakes(attempts))
    }

    @Test
    fun `streak counts consecutive days and survives until today is studied`() {
        val attempts = listOf(today.minusDays(1), today.minusDays(2), today.minusDays(4))
            .map { attempt("q", AttemptResult.CORRECT, it) }
        assertEquals(2, SessionPlanner.streak(attempts, today))
        assertEquals(3, SessionPlanner.streak(attempts + attempt("q", AttemptResult.CORRECT, today), today))
        assertEquals(0, SessionPlanner.streak(emptyList(), today))
    }
}
