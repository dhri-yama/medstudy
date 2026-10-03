package com.medstudy.core.data

import com.medstudy.core.model.AnswerKey
import com.medstudy.core.model.Category
import com.medstudy.core.model.ContentBlock
import com.medstudy.core.model.ContentKind
import com.medstudy.core.model.ContentProvenance
import com.medstudy.core.model.ExamId
import com.medstudy.core.model.MediaAsset
import com.medstudy.core.model.OptionRationale
import com.medstudy.core.model.Question
import com.medstudy.core.model.QuestionFormat
import com.medstudy.core.model.QuestionOption
import com.medstudy.core.model.QuestionReference
import com.medstudy.core.model.QuestionCatalogValidator
import com.medstudy.core.model.ReviewStatus
import com.medstudy.core.model.RightsStatus

object DemoQuestionCatalog {
    const val DEMO_NOTICE = "Demo content only. These are fictional UI samples, not exam questions or medical advice."

    val categories = listOf(
        Category("subject.anatomy", null, "Anatomy", "Demo question fixtures", 0),
        Category("topic.anatomy.imaging", "subject.anatomy", "Image display", position = 0),
        Category("subject.physiology", null, "Physiology", "Demo question fixtures", 1),
        Category("topic.physiology.review", "subject.physiology", "Answer review", position = 0),
        Category("subject.biochemistry", null, "Biochemistry", "Demo question fixtures", 2),
        Category("topic.biochemistry.content", "subject.biochemistry", "Question content", position = 0),
        Category("subject.pathology", null, "Pathology", "Demo question fixtures", 3),
        Category("topic.pathology.progress", "subject.pathology", "Progress tracking", position = 0),
        Category("subject.pharmacology", null, "Pharmacology", "Demo question fixtures", 4),
        Category("topic.pharmacology.bookmarks", "subject.pharmacology", "Bookmarks", position = 0),
        Category("subject.microbiology", null, "Microbiology", "Demo question fixtures", 5),
        Category("topic.microbiology.offline", "subject.microbiology", "Offline state", position = 0),
    )

    val mediaAssets = listOf(
        MediaAsset(
            id = "demo.media.placeholder-landscape",
            relativePath = "drawable/demo_imaging_placeholder.xml",
            mimeType = "image/vnd.android.vector",
            sha256 = "4a9e7badf012aad72bf8905e4004a68b39412bb5025c10fc494ba9d1422d7dca",
            widthPx = 1080,
            heightPx = 648,
            contentKind = ContentKind.DEMO,
            rightsStatus = RightsStatus.DEMO_ONLY,
        ),
    )

    val questions = listOf(
        demoQuestion(
            id = "demo.question.anatomy-image",
            subjectId = "subject.anatomy",
            topicId = "topic.anatomy.imaging",
            image = true,
        ),
        demoQuestion(
            id = "demo.question.physiology-review",
            subjectId = "subject.physiology",
            topicId = "topic.physiology.review",
        ),
        demoQuestion(
            id = "demo.question.biochemistry-content",
            subjectId = "subject.biochemistry",
            topicId = "topic.biochemistry.content",
        ),
        demoQuestion(
            id = "demo.question.pathology-progress",
            subjectId = "subject.pathology",
            topicId = "topic.pathology.progress",
        ),
        demoQuestion(
            id = "demo.question.pharmacology-bookmarks",
            subjectId = "subject.pharmacology",
            topicId = "topic.pharmacology.bookmarks",
        ),
        demoQuestion(
            id = "demo.question.microbiology-offline",
            subjectId = "subject.microbiology",
            topicId = "topic.microbiology.offline",
        ),
    )

    fun validate() = QuestionCatalogValidator().validate(questions, categories, mediaAssets)

    private fun demoQuestion(
        id: String,
        subjectId: String,
        topicId: String,
        image: Boolean = false,
    ): Question {
        val stem = mutableListOf<ContentBlock>(
            ContentBlock.Text(
                "DEMO ONLY: This fictional sample tests the question screen and answer review. " +
                    "Which option is designated as the example answer?",
            ),
        )
        if (image) {
            stem += ContentBlock.Image(
                assetId = "demo.media.placeholder-landscape",
                altText = "Illustrative demo placeholder, not a diagnostic or medical image.",
                caption = "Demo image placeholder",
            )
        }

        val options = listOf("a", "b", "c", "d").mapIndexed { index, optionId ->
            QuestionOption(
                id = optionId,
                label = optionId.uppercase(),
                position = index,
                content = listOf(ContentBlock.Text("Demo option ${optionId.uppercase()}")),
            )
        }

        return Question(
            id = id,
            examId = ExamId.DEMO,
            examYear = null,
            subjectId = subjectId,
            topicIds = listOf(topicId),
            format = QuestionFormat.SINGLE_BEST_ANSWER,
            languageTag = "en-IN",
            content = stem,
            options = options,
            answerKey = AnswerKey(
                correctOptionIds = listOf("b"),
                explanation = "DEMO ONLY: This text verifies the overall explanation layout.",
                optionRationales = options.map { option ->
                    OptionRationale(
                        optionId = option.id,
                        text = "DEMO ONLY: Sample rationale for option ${option.label}.",
                    )
                },
            ),
            references = emptyList<QuestionReference>(),
            provenance = ContentProvenance(
                contentKind = ContentKind.DEMO,
                sourceName = "MedStudy demo catalog",
                rightsStatus = RightsStatus.DEMO_ONLY,
            ),
            reviewStatus = ReviewStatus.DEMO_NOT_CLINICALLY_REVIEWED,
        )
    }
}
