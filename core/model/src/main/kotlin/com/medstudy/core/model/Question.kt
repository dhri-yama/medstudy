package com.medstudy.core.model

enum class ExamId(val wireValue: String) {
    NEET_PG("NEET_PG"),
    INI_CET("INI_CET"),
    DEMO("DEMO"),
}

enum class QuestionFormat {
    SINGLE_BEST_ANSWER,
    MULTI_SELECT,
}

sealed interface ContentBlock {
    data class Text(val text: String) : ContentBlock

    data class Image(
        val assetId: String,
        val altText: String,
        val caption: String? = null,
    ) : ContentBlock
}

data class QuestionOption(
    val id: String,
    val label: String,
    val position: Int,
    val content: List<ContentBlock>,
)

data class OptionRationale(
    val optionId: String,
    val text: String,
)

data class AnswerKey(
    val correctOptionIds: List<String>,
    val explanation: String,
    val optionRationales: List<OptionRationale>,
)

data class QuestionReference(
    val citation: String,
    val url: String? = null,
    val accessedOn: String? = null,
)

data class Question(
    val id: String,
    val examId: ExamId,
    val examYear: Int?,
    val subjectId: String,
    val topicIds: List<String>,
    val format: QuestionFormat,
    val languageTag: String,
    val content: List<ContentBlock>,
    val options: List<QuestionOption>,
    val answerKey: AnswerKey,
    val references: List<QuestionReference>,
    val provenance: ContentProvenance,
    val reviewStatus: ReviewStatus,
    val revision: Int = 1,
)

enum class AttemptResult {
    CORRECT,
    INCORRECT,
    UNANSWERED,
}

enum class PracticeContext {
    TOPIC_PRACTICE,
    BOOKMARK_REVIEW,
    PROGRESS_REVIEW,
}

data class QuestionAttempt(
    val id: String,
    val questionId: String,
    val selectedOptionIds: List<String>,
    val result: AttemptResult,
    val completedAtEpochMillis: Long,
    val studyDateIso: String,
    val timeZoneId: String,
    val context: PracticeContext,
)
