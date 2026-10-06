package com.medstudy.core.model

class QuestionCatalogValidator {
    fun validate(
        questions: List<Question>,
        categories: List<Category>,
        mediaAssets: List<MediaAsset>,
    ): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val categoryIds = categories.map(Category::id).toSet()
        val mediaById = mediaAssets.associateBy(MediaAsset::id)

        if (categoryIds.size != categories.size) {
            issues += ValidationIssue(null, "categories.id", "Category IDs must be unique.")
        }
        if (mediaById.size != mediaAssets.size) {
            issues += ValidationIssue(null, "mediaAssets.id", "Media asset IDs must be unique.")
        }
        if (questions.map(Question::id).toSet().size != questions.size) {
            issues += ValidationIssue(null, "questions.id", "Question IDs must be unique.")
        }

        questions.forEach { question ->
            issues += validateQuestion(question, categoryIds, mediaById)
        }
        return issues
    }

    private fun validateQuestion(
        question: Question,
        categoryIds: Set<String>,
        mediaById: Map<String, MediaAsset>,
    ): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        fun issue(field: String, message: String) {
            issues += ValidationIssue(question.id, field, message)
        }

        if (question.id.isBlank()) issue("id", "Question ID is required.")
        if (question.examYear != null && question.examYear !in 1900..2200) {
            issue("examYear", "Exam year must be a plausible four-digit year.")
        }
        if (question.subjectId !in categoryIds) issue("subjectId", "Subject must reference a known category.")
        if (question.topicIds.distinct().size != question.topicIds.size) {
            issue("topicIds", "Topic IDs must not contain duplicates.")
        }
        question.topicIds.filterNot(categoryIds::contains).forEach { topicId ->
            issue("topicIds", "Unknown topic ID: $topicId")
        }
        if (question.languageTag.isBlank()) issue("languageTag", "Language tag is required.")
        if (question.content.isEmpty()) issue("content", "Question content is required.")
        if (question.revision < 1) issue("revision", "Revision must be a positive integer.")

        val optionIds = question.options.map(QuestionOption::id)
        if (question.options.size < 2) issue("options", "At least two options are required.")
        if (optionIds.any(String::isBlank)) issue("options.id", "Option IDs are required.")
        if (optionIds.distinct().size != optionIds.size) issue("options.id", "Option IDs must be unique.")
        if (question.options.map(QuestionOption::position).distinct().size != question.options.size) {
            issue("options.position", "Option positions must be unique.")
        }
        question.options.forEach { option ->
            validateBlocks(question.id, "options.${option.id}.content", option.content, mediaById, ::issue)
        }

        val correctIds = question.answerKey.correctOptionIds
        if (correctIds.isEmpty()) issue("answerKey.correctOptionIds", "At least one correct option is required.")
        if (correctIds.distinct().size != correctIds.size) {
            issue("answerKey.correctOptionIds", "Correct option IDs must be unique.")
        }
        if (question.format == QuestionFormat.SINGLE_BEST_ANSWER && correctIds.size != 1) {
            issue("answerKey.correctOptionIds", "Single-best-answer questions need exactly one correct option.")
        }
        correctIds.filterNot(optionIds::contains).forEach { optionId ->
            issue("answerKey.correctOptionIds", "Unknown correct option ID: $optionId")
        }
        // Only require explanations for DEMO content; MedMCQA and other sources may not have them
        if (question.provenance.contentKind == ContentKind.DEMO && question.answerKey.explanation.isBlank()) {
            issue("answerKey.explanation", "Overall explanation is required.")
        }

        val rationaleIds = question.answerKey.optionRationales.map(OptionRationale::optionId)
        if (rationaleIds.distinct().size != rationaleIds.size) {
            issue("answerKey.optionRationales", "Each option can have only one rationale.")
        }
        // Only require rationales for DEMO content; MedMCQA and other sources may not have them
        if (question.provenance.contentKind == ContentKind.DEMO) {
            (optionIds - rationaleIds.toSet()).forEach { optionId ->
                issue("answerKey.optionRationales", "Missing rationale for option $optionId.")
            }
        }
        (rationaleIds - optionIds.toSet()).forEach { optionId ->
            issue("answerKey.optionRationales", "Rationale references unknown option $optionId.")
        }
        question.answerKey.optionRationales.filter { it.text.isBlank() }.forEach { rationale ->
            issue("answerKey.optionRationales.${rationale.optionId}", "Rationale text is required.")
        }

        validateBlocks(question.id, "content", question.content, mediaById, ::issue)
        if (question.provenance.contentKind == ContentKind.DEMO &&
            question.provenance.rightsStatus != RightsStatus.DEMO_ONLY
        ) {
            issue("provenance.rightsStatus", "Demo content must use DEMO_ONLY rights status.")
        }
        return issues
    }

    private fun validateBlocks(
        questionId: String,
        field: String,
        blocks: List<ContentBlock>,
        mediaById: Map<String, MediaAsset>,
        report: (String, String) -> Unit,
    ) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is ContentBlock.Text -> if (block.text.isBlank()) {
                    report("$field[$index]", "Text blocks cannot be blank.")
                }
                is ContentBlock.Image -> {
                    if (block.assetId !in mediaById) {
                        report("$field[$index].assetId", "Unknown media asset ID: ${block.assetId}")
                    }
                    if (block.altText.isBlank()) {
                        report("$field[$index].altText", "Image accessibility description is required.")
                    }
                }
            }
        }
    }
}

data class ValidationIssue(
    val questionId: String?,
    val field: String,
    val message: String,
)
