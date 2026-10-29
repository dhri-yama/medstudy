package com.medstudy.feature.progress

import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.Question
import com.medstudy.core.model.QuestionAttempt
import java.time.LocalDate

/** Picks question ids for each practice mode. Pure so it can be tested without Compose or Room. */
object SessionPlanner {
    /** Fresh questions first, shuffled. */
    fun daily(questions: List<Question>, attempts: List<QuestionAttempt>, size: Int): List<String> {
        val seen = attempts.mapTo(HashSet(), QuestionAttempt::questionId)
        return questions.filterNot { it.id in seen }.map(Question::id).shuffled().take(size)
    }

    /** Questions whose latest answer was wrong, newest mistake first. */
    fun mistakes(attempts: List<QuestionAttempt>): List<String> =
        attempts.sortedByDescending(QuestionAttempt::completedAtEpochMillis)
            .distinctBy(QuestionAttempt::questionId)
            .filter { it.result == AttemptResult.INCORRECT }
            .map(QuestionAttempt::questionId)

    /** Questions from due topics, most-forgotten topic first, unseen questions before seen ones. */
    fun due(areas: List<WeakArea>, questions: List<Question>, attempts: List<QuestionAttempt>, size: Int): List<String> {
        val seen = attempts.mapTo(HashSet(), QuestionAttempt::questionId)
        return areas.filter(WeakArea::isDue)
            .sortedWith(compareBy({ it.retrievability ?: 1.0 }, { it.accuracy ?: 1.0 }))
            .flatMap { area ->
                questions.filter { question ->
                    if (area.topicId != null) area.topicId in question.topicIds else question.subjectId == area.subjectId
                }.sortedBy { it.id in seen }
            }
            .map(Question::id).distinct().take(size)
    }

    /** Consecutive study days ending today (or yesterday, so the streak survives until you've studied today). */
    fun streak(attempts: List<QuestionAttempt>, today: LocalDate): Int {
        val days = attempts.mapNotNull { runCatching { LocalDate.parse(it.studyDateIso) }.getOrNull() }.toSet()
        var day = if (today in days) today else today.minusDays(1)
        var count = 0
        while (day in days) { count++; day = day.minusDays(1) }
        return count
    }
}

/** A study day can be switched on any time; it can only be switched off while at least one other day stays on. */
fun canToggleStudyDay(isoWeekday: Int, selectedDays: Set<Int>): Boolean =
    isoWeekday !in selectedDays || selectedDays.size > 1
