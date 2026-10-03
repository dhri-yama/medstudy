package com.medstudy.core.model

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class QuestionCatalogValidatorTest {
    @Test
    fun `valid demo question passes validation`() {
        val question = demoQuestion()

        val issues = QuestionCatalogValidator().validate(
            questions = listOf(question),
            categories = listOf(Category(question.subjectId, null, "Demo subject")),
            mediaAssets = emptyList(),
        )

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `missing option rationale is reported against its option`() {
        val question = demoQuestion().copy(
            answerKey = demoQuestion().answerKey.copy(
                optionRationales = demoQuestion().answerKey.optionRationales.dropLast(1),
            ),
        )

        val issues = QuestionCatalogValidator().validate(
            questions = listOf(question),
            categories = listOf(Category(question.subjectId, null, "Demo subject")),
            mediaAssets = emptyList(),
        )

        assertEquals(1, issues.count { it.field == "answerKey.optionRationales" })
    }

    @Test
    fun `unknown image asset and empty alt text are reported`() {
        val question = demoQuestion().copy(
            content = listOf(ContentBlock.Image("missing-image", "")),
        )

        val issues = QuestionCatalogValidator().validate(
            questions = listOf(question),
            categories = listOf(Category(question.subjectId, null, "Demo subject")),
            mediaAssets = emptyList(),
        )

        assertTrue(issues.any { it.field == "content[0].assetId" })
        assertTrue(issues.any { it.field == "content[0].altText" })
    }

    private fun demoQuestion(): Question = Question(
        id = "demo.question.001",
        examId = ExamId.DEMO,
        examYear = null,
        subjectId = "demo.subject.general",
        topicIds = emptyList(),
        format = QuestionFormat.SINGLE_BEST_ANSWER,
        languageTag = "en-IN",
        content = listOf(ContentBlock.Text("DEMO ONLY: Select the example answer.")),
        options = listOf("a", "b", "c", "d").mapIndexed { index, id ->
            QuestionOption(
                id = id,
                label = id.uppercase(),
                position = index,
                content = listOf(ContentBlock.Text("Demo option $id")),
            )
        },
        answerKey = AnswerKey(
            correctOptionIds = listOf("b"),
            explanation = "DEMO ONLY: Example explanation.",
            optionRationales = listOf("a", "b", "c", "d").map { id ->
                OptionRationale(id, "DEMO ONLY: Example rationale for $id.")
            },
        ),
        references = emptyList(),
        provenance = ContentProvenance(
            contentKind = ContentKind.DEMO,
            sourceName = "MedStudy demo catalog",
            rightsStatus = RightsStatus.DEMO_ONLY,
        ),
        reviewStatus = ReviewStatus.DEMO_NOT_CLINICALLY_REVIEWED,
    )
}
