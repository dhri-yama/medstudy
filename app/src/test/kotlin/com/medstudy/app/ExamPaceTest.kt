package com.medstudy.app

import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.PracticeContext
import com.medstudy.core.model.QuestionAttempt
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExamPaceTest {
    private val today = LocalDate.of(2026, 11, 13)

    private fun attempt(questionId: String, daysAgo: Int) = QuestionAttempt(
        id = "a-$questionId-$daysAgo", questionId = questionId, selectedOptionIds = emptyList(), result = AttemptResult.CORRECT,
        completedAtEpochMillis = 0, studyDateIso = today.minusDays(daysAgo.toLong()).toString(), timeZoneId = "UTC",
        context = PracticeContext.TOPIC_PRACTICE,
    )

    @Test fun noDateMeansNoPace() {
        assertNull(examPace(null, today, 1000, emptyList()))
        assertNull(examPace(today.minusDays(1).toString(), today, 1000, emptyList()))
    }

    @Test fun neededPerDayRoundsUp() {
        val pace = examPace(today.plusDays(10).toString(), today, 1000, emptyList())!!
        assertEquals(100, pace.neededPerDay)
        assertEquals(PaceStatus.NO_DATA, pace.status)
    }

    @Test fun behindWhenRecentPaceIsBelowNeeded() {
        val attempts = (1..14).map { attempt("q$it", daysAgo = it % 7) }
        val pace = examPace(today.plusDays(10).toString(), today, 1000, attempts)!!
        assertEquals(14, pace.covered)
        assertEquals(2, pace.recentPerDay)
        assertEquals(PaceStatus.BEHIND, pace.status)
    }

    @Test fun onTrackWhenRecentPaceMeetsNeeded() {
        val attempts = (1..70).map { attempt("q$it", daysAgo = it % 7) }
        val pace = examPace(today.plusDays(30).toString(), today, 100, attempts)!!
        assertEquals(PaceStatus.ON_TRACK, pace.status)
    }
}
