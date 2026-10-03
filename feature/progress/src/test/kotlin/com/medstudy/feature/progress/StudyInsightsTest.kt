package com.medstudy.feature.progress

import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.Category
import com.medstudy.core.model.ContentBlock
import com.medstudy.core.model.ContentKind
import com.medstudy.core.model.ContentProvenance
import com.medstudy.core.model.ExamId
import com.medstudy.core.model.OptionRationale
import com.medstudy.core.model.Question
import com.medstudy.core.model.QuestionFormat
import com.medstudy.core.model.QuestionOption
import com.medstudy.core.model.QuestionAttempt
import com.medstudy.core.model.PracticeContext
import com.medstudy.core.model.AnswerKey
import com.medstudy.core.model.ReviewStatus
import com.medstudy.core.model.RightsStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudyInsightsTest {
    private val today = LocalDate.parse("2026-10-03")
    private val subject = Category("subject.demo", null, "Demo subject")
    private val topic = Category("topic.demo", subject.id, "Demo topic")

    @Test
    fun `small sample and unanswered reveals remain unassessed`() {
        val result = StudyInsights().analyze(
            today = today,
            attempts = listOf(attempt(1, AttemptResult.INCORRECT), attempt(2, AttemptResult.UNANSWERED)),
            questions = listOf(question("question.1"), question("question.2")),
            categories = listOf(subject, topic),
        )

        assertEquals(WeakAreaStatus.UNASSESSED, result.single().status)
        assertEquals(1, result.single().answeredAttempts)
    }

    @Test
    fun `five answered attempts with low accuracy marks topic for review`() {
        val attempts = listOf(
            attempt(1, AttemptResult.CORRECT),
            attempt(2, AttemptResult.INCORRECT),
            attempt(3, AttemptResult.INCORRECT),
            attempt(4, AttemptResult.INCORRECT),
            attempt(5, AttemptResult.CORRECT),
        )

        val result = StudyInsights().analyze(today, attempts, attempts.map { question(it.questionId) }, listOf(subject, topic))

        assertEquals(WeakAreaStatus.NEEDS_REVIEW, result.single().status)
        assertEquals(0.4, result.single().accuracy!!, 0.001)
    }

    @Test
    fun `sparse topic results fall back to subject evidence`() {
        val secondTopic = Category("topic.demo.two", subject.id, "Another demo topic")
        val attempts = (1..5).map { index ->
            attempt(index, if (index < 4) AttemptResult.CORRECT else AttemptResult.INCORRECT)
                .copy(questionId = "question.$index")
        }
        val questions = attempts.mapIndexed { index, attempt ->
            question(attempt.questionId, if (index % 2 == 0) topic.id else secondTopic.id)
        }

        val result = StudyInsights().analyze(today, attempts, questions, listOf(subject, topic, secondTopic))

        assertEquals(1, result.size)
        assertEquals(subject.id, result.single().id)
        assertNull(result.single().topicId)
        assertEquals(5, result.single().answeredAttempts)
    }

    private fun attempt(index: Int, result: AttemptResult) = QuestionAttempt(
        id = "attempt.$index",
        questionId = "question.$index",
        selectedOptionIds = emptyList(),
        result = result,
        completedAtEpochMillis = today.toEpochDay() * 86_400_000,
        studyDateIso = today.toString(),
        timeZoneId = "UTC",
        context = PracticeContext.TOPIC_PRACTICE,
    )

    private fun question(id: String, topicId: String = topic.id) = Question(
        id = id,
        examId = ExamId.DEMO,
        examYear = null,
        subjectId = subject.id,
        topicIds = listOf(topicId),
        format = QuestionFormat.SINGLE_BEST_ANSWER,
        languageTag = "en-IN",
        content = listOf(ContentBlock.Text("Demo")),
        options = listOf(QuestionOption("a", "A", 0, listOf(ContentBlock.Text("A"))), QuestionOption("b", "B", 1, listOf(ContentBlock.Text("B")))),
        answerKey = AnswerKey(listOf("a"), "Demo explanation", listOf(OptionRationale("a", "Correct"), OptionRationale("b", "Incorrect"))),
        references = emptyList(),
        provenance = ContentProvenance(ContentKind.DEMO, "Test", rightsStatus = RightsStatus.DEMO_ONLY),
        reviewStatus = ReviewStatus.DEMO_NOT_CLINICALLY_REVIEWED,
    )
}
