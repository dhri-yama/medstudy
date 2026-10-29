package com.medstudy.app

import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.Category
import com.medstudy.core.model.Question
import com.medstudy.core.model.QuestionAttempt

data class SubjectSummary(
    val id: String,
    val name: String,
    /** Index into the subject palette: this subject's place in the sorted list (catalog positions are all 0). */
    val colorIndex: Int,
    val questionCount: Int,
    val answered: Int,
    /** Percent of graded answers that were correct, or null with no graded answers. */
    val accuracyPercent: Int?,
)

fun subjectSummaries(
    categories: List<Category>,
    questions: List<Question>,
    attempts: List<QuestionAttempt>,
): List<SubjectSummary> {
    val subjectByQuestion = questions.associate { it.id to it.subjectId }
    val attemptsBySubject = attempts.groupBy { subjectByQuestion[it.questionId] }
    val countBySubject = questions.groupingBy { it.subjectId }.eachCount()
    return categories.filter { it.parentId == null }.sortedWith(compareBy(Category::position, Category::name))
        .mapIndexedNotNull { index, subject ->
        val count = countBySubject[subject.id] ?: return@mapIndexedNotNull null
        val subjectAttempts = attemptsBySubject[subject.id].orEmpty()
        val graded = subjectAttempts.filter { it.result != AttemptResult.UNANSWERED }
        SubjectSummary(
            id = subject.id,
            name = subject.name,
            colorIndex = index,
            questionCount = count,
            answered = subjectAttempts.mapTo(HashSet()) { it.questionId }.size,
            accuracyPercent = graded.takeIf { it.isNotEmpty() }
                ?.let { list -> list.count { it.result == AttemptResult.CORRECT } * 100 / list.size },
        )
    }
}
