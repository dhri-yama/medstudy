package com.medstudy.core.data

import androidx.room.withTransaction
import com.medstudy.core.database.AnswerKeyEntity
import com.medstudy.core.database.CategoryEntity
import com.medstudy.core.database.MediaAssetEntity
import com.medstudy.core.database.OptionRationaleEntity
import com.medstudy.core.database.QuestionContentBlockEntity
import com.medstudy.core.database.QuestionEntity
import com.medstudy.core.database.QuestionOptionEntity
import com.medstudy.core.database.QuestionReferenceEntity
import com.medstudy.core.database.QuestionWithDetails
import com.medstudy.core.database.StudyDatabase
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
import com.medstudy.core.model.ReviewStatus
import com.medstudy.core.model.RightsStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomQuestionCatalogRepository(
    private val database: StudyDatabase,
) : QuestionCatalogRepository {
    private val catalogDao = database.catalogDao()

    override suspend fun isEmpty(): Boolean = withContext(Dispatchers.IO) {
        catalogDao.questionCount() == 0
    }

    override suspend fun categories(): List<Category> = withContext(Dispatchers.IO) {
        catalogDao.getCategories().map { entity ->
            Category(entity.id, entity.parentId, entity.name, entity.description, entity.position)
        }
    }

    override suspend fun questions(filter: QuestionFilter): List<Question> = withContext(Dispatchers.IO) {
        val questionEntities = if (filter.subjectIds.isNotEmpty()) {
            // If specific subject IDs are requested, query for each
            filter.subjectIds.flatMap { subjectId ->
                catalogDao.getQuestions(filter.examId?.wireValue, filter.examYear, subjectId)
            }
        } else if (filter.examId == null && filter.examYear == null) {
            // If no filters at all, get all questions efficiently
            catalogDao.getAllQuestions()
        } else {
            // If only exam filters but no subject filters, get all questions matching exam
            catalogDao.getQuestions(filter.examId?.wireValue, filter.examYear, null)
        }

        questionEntities
            .distinctBy { it.question.id }
            .map(QuestionWithDetails::toDomain)
            .filter { question -> filter.topicId == null || filter.topicId in question.topicIds }
    }

    override suspend fun question(questionId: String): Question? = withContext(Dispatchers.IO) {
        catalogDao.getQuestion(questionId)?.toDomain()
    }

    override suspend fun bookmarkedQuestions(): List<Question> = withContext(Dispatchers.IO) {
        catalogDao.getBookmarkedQuestions().map(QuestionWithDetails::toDomain)
    }

    override suspend fun saveCatalog(
        categories: List<Category>,
        mediaAssets: List<MediaAsset>,
        questions: List<Question>,
    ) = withContext(Dispatchers.IO) {
        val issueList = com.medstudy.core.model.QuestionCatalogValidator()
            .validate(questions, categories, mediaAssets)
        require(issueList.isEmpty()) { issueList.joinToString { "${it.questionId ?: it.field}: ${it.message}" } }

        database.withTransaction {
            // Clear existing data first to avoid duplicates
            catalogDao.clearAllData()
            
            catalogDao.putCategories(categories.map(Category::toEntity))
            catalogDao.putMediaAssets(mediaAssets.map(MediaAsset::toEntity))
            catalogDao.putQuestions(questions.map(Question::toEntity))
            catalogDao.putContentBlocks(questions.flatMap(Question::contentRows))
            catalogDao.putOptions(questions.flatMap(Question::optionRows))
            catalogDao.putAnswerKeys(questions.map(Question::answerKeyRow))
            catalogDao.putRationales(questions.flatMap(Question::rationaleRows))
            catalogDao.putReferences(questions.flatMap(Question::referenceRows))
        }
    }
}

private fun Category.toEntity() = CategoryEntity(id, parentId, name, description, position)

private fun MediaAsset.toEntity() = MediaAssetEntity(
    id = id,
    relativePath = relativePath,
    mimeType = mimeType,
    sha256 = sha256,
    widthPx = widthPx,
    heightPx = heightPx,
    contentKind = contentKind.name,
    rightsStatus = rightsStatus.name,
)

private fun Question.toEntity() = QuestionEntity(
    id = id,
    examId = examId.wireValue,
    examYear = examYear,
    subjectId = subjectId,
    topicIds = topicIds,
    format = format.name,
    languageTag = languageTag,
    provenanceKind = provenance.contentKind.name,
    sourceName = provenance.sourceName,
    sourceReference = provenance.sourceReference,
    rightsStatus = provenance.rightsStatus.name,
    attribution = provenance.attribution,
    reviewStatus = reviewStatus.name,
    revision = revision,
)

private fun Question.contentRows(): List<QuestionContentBlockEntity> =
    content.toRows(questionId = id, optionId = null) + options.flatMap { option ->
        option.content.toRows(questionId = id, optionId = option.id)
    }

private fun List<ContentBlock>.toRows(
    questionId: String,
    optionId: String?,
): List<QuestionContentBlockEntity> = mapIndexed { index, block ->
    when (block) {
        is ContentBlock.Text -> QuestionContentBlockEntity(
            questionId = questionId,
            optionId = optionId,
            position = index,
            type = "TEXT",
            text = block.text,
            assetId = null,
            altText = null,
            caption = null,
        )
        is ContentBlock.Image -> QuestionContentBlockEntity(
            questionId = questionId,
            optionId = optionId,
            position = index,
            type = "IMAGE",
            text = null,
            assetId = block.assetId,
            altText = block.altText,
            caption = block.caption,
        )
    }
}

private fun Question.optionRows() = options.map { option ->
    QuestionOptionEntity(id = option.id, questionId = id, label = option.label, position = option.position)
}

private fun Question.answerKeyRow() = AnswerKeyEntity(
    questionId = id,
    correctOptionIds = answerKey.correctOptionIds,
    explanation = answerKey.explanation,
)

private fun Question.rationaleRows() = answerKey.optionRationales.map { rationale ->
    OptionRationaleEntity(questionId = id, optionId = rationale.optionId, text = rationale.text)
}

private fun Question.referenceRows() = references.map { reference ->
    QuestionReferenceEntity(questionId = id, citation = reference.citation, url = reference.url, accessedOn = reference.accessedOn)
}

private fun QuestionWithDetails.toDomain(): Question {
    val answerKey = answerKeys.singleOrNull()
        ?: error("Question ${question.id} must have exactly one answer key")
    val orderedBlocks = contentBlocks.sortedBy(QuestionContentBlockEntity::position)
    return Question(
        id = question.id,
        examId = ExamId.entries.single { it.wireValue == question.examId },
        examYear = question.examYear,
        subjectId = question.subjectId,
        topicIds = question.topicIds,
        format = QuestionFormat.valueOf(question.format),
        languageTag = question.languageTag,
        content = orderedBlocks.filter { it.optionId == null }.map(QuestionContentBlockEntity::toDomain),
        options = options.sortedBy(QuestionOptionEntity::position).map { option ->
            QuestionOption(
                id = option.id,
                label = option.label,
                position = option.position,
                content = orderedBlocks.filter { it.optionId == option.id }
                    .map(QuestionContentBlockEntity::toDomain),
            )
        },
        answerKey = AnswerKey(
            correctOptionIds = answerKey.correctOptionIds,
            explanation = answerKey.explanation,
            optionRationales = rationales.map { OptionRationale(it.optionId, it.text) },
        ),
        references = references.map { QuestionReference(it.citation, it.url, it.accessedOn) },
        provenance = ContentProvenance(
            contentKind = ContentKind.valueOf(question.provenanceKind),
            sourceName = question.sourceName,
            sourceReference = question.sourceReference,
            rightsStatus = RightsStatus.valueOf(question.rightsStatus),
            attribution = question.attribution,
        ),
        reviewStatus = ReviewStatus.valueOf(question.reviewStatus),
        revision = question.revision,
    )
}

private fun QuestionContentBlockEntity.toDomain(): ContentBlock = when (type) {
    "TEXT" -> ContentBlock.Text(requireNotNull(text))
    "IMAGE" -> ContentBlock.Image(
        assetId = requireNotNull(assetId),
        altText = requireNotNull(altText),
        caption = caption,
    )
    else -> error("Unknown content block type: $type")
}
