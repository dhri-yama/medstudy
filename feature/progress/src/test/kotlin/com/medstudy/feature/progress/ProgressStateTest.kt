package com.medstudy.feature.progress

import com.medstudy.core.model.AnswerKey
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.Category
import com.medstudy.core.model.ContentBlock
import com.medstudy.core.model.ContentKind
import com.medstudy.core.model.ContentProvenance
import com.medstudy.core.model.ExamId
import com.medstudy.core.model.OptionRationale
import com.medstudy.core.model.PracticeContext
import com.medstudy.core.model.Question
import com.medstudy.core.model.QuestionAttempt
import com.medstudy.core.model.QuestionFormat
import com.medstudy.core.model.QuestionOption
import com.medstudy.core.model.ReviewStatus
import com.medstudy.core.model.RightsStatus
import com.medstudy.core.model.StudyPlanKind
import com.medstudy.core.model.StudySchedulePreferences
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressStateTest {
    @Test
    fun `subject selection filters both day counts and exact question history`() {
        val date = LocalDate.parse("2026-10-03")
        val anatomyQuestion = question("question.anatomy", "subject.anatomy")
        val physiologyQuestion = question("question.physiology", "subject.physiology")
        val state = ProgressUiState(
            isLoading = false,
            currentMonth = YearMonth.from(date),
            selectedDate = date,
            categories = listOf(
                Category("subject.anatomy", null, "Anatomy"),
                Category("subject.physiology", null, "Physiology"),
            ),
            questions = listOf(anatomyQuestion, physiologyQuestion),
            attempts = listOf(attempt("attempt.1", anatomyQuestion.id, date), attempt("attempt.2", physiologyQuestion.id, date)),
        )

        val allSubjects = state.withDerivedData()
        val anatomyOnly = state.copy(selectedSubjectIds = setOf("subject.anatomy")).withDerivedData()

        assertEquals(2, allSubjects.dayCounts[date])
        assertEquals(setOf(anatomyQuestion.id, physiologyQuestion.id), allSubjects.selectedDayEntries.map { it.question.id }.toSet())
        assertEquals(1, anatomyOnly.dayCounts[date])
        assertEquals(listOf(anatomyQuestion.id), anatomyOnly.selectedDayEntries.map { it.question.id })
    }

    @Test
    fun `starter plan contains one editable block per available demo topic`() {
        val startDate = LocalDate.parse("2026-10-03")
        val categories = listOf(
            Category("subject.anatomy", null, "Anatomy"),
            Category("topic.anatomy.one", "subject.anatomy", "Topic one"),
            Category("topic.anatomy.two", "subject.anatomy", "Topic two"),
        )

        val plan = WeeklyPlanGenerator().generate(
            startDate = startDate,
            areas = emptyList(),
            categories = categories,
            generatedAtEpochMillis = 123L,
        )

        assertEquals(StudyPlanKind.STARTER, plan.kind)
        assertEquals(7, plan.blocks.size)
        assertEquals(7, plan.blocks.map { it.studyDateIso }.distinct().size)
        assertEquals(startDate.toString(), plan.blocks.first().studyDateIso)
        assertTrue(plan.blocks.all { it.questionTarget > 0 && it.minuteTarget > 0 })
    }

    @Test
    fun `generated blocks respect selected weekdays and daily targets`() {
        val startDate = LocalDate.parse("2026-10-03")
        val preferences = StudySchedulePreferences(
            availableWeekdays = setOf(1, 3, 5),
            questionTargetPerDay = 12,
            minuteTargetPerDay = 45,
        )
        val categories = listOf(
            Category("subject.anatomy", null, "Anatomy"),
            Category("topic.anatomy.one", "subject.anatomy", "Topic one"),
        )

        val plan = WeeklyPlanGenerator().generate(
            startDate = startDate,
            areas = emptyList(),
            categories = categories,
            generatedAtEpochMillis = 123L,
            preferences = preferences,
        )

        assertEquals(listOf("2026-10-05", "2026-10-07", "2026-10-09"), plan.blocks.map { it.studyDateIso })
        assertTrue(plan.blocks.all { it.questionTarget == 12 && it.minuteTarget == 45 })
    }

    private fun attempt(id: String, questionId: String, date: LocalDate) = QuestionAttempt(
        id = id,
        questionId = questionId,
        selectedOptionIds = listOf("a"),
        result = AttemptResult.CORRECT,
        completedAtEpochMillis = date.toEpochDay() * 86_400_000,
        studyDateIso = date.toString(),
        timeZoneId = "UTC",
        context = PracticeContext.TOPIC_PRACTICE,
    )

    private fun question(id: String, subjectId: String) = Question(
        id = id,
        examId = ExamId.DEMO,
        examYear = null,
        subjectId = subjectId,
        topicIds = emptyList(),
        format = QuestionFormat.SINGLE_BEST_ANSWER,
        languageTag = "en-IN",
        content = listOf(ContentBlock.Text("DEMO ONLY: Sample question")),
        options = listOf(QuestionOption("a", "A", 0, listOf(ContentBlock.Text("Option A"))), QuestionOption("b", "B", 1, listOf(ContentBlock.Text("Option B")))),
        answerKey = AnswerKey(listOf("a"), "Demo explanation", listOf(OptionRationale("a", "Correct"), OptionRationale("b", "Incorrect"))),
        references = emptyList(),
        provenance = ContentProvenance(ContentKind.DEMO, "Test", rightsStatus = RightsStatus.DEMO_ONLY),
        reviewStatus = ReviewStatus.DEMO_NOT_CLINICALLY_REVIEWED,
    )
}
