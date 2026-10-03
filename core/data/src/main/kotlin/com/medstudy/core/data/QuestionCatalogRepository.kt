package com.medstudy.core.data

import com.medstudy.core.model.Category
import com.medstudy.core.model.ExamId
import com.medstudy.core.model.MediaAsset
import com.medstudy.core.model.Question

interface QuestionCatalogRepository {
    suspend fun categories(): List<Category>
    suspend fun questions(filter: QuestionFilter = QuestionFilter()): List<Question>
    suspend fun question(questionId: String): Question?
    suspend fun bookmarkedQuestions(): List<Question>
    suspend fun saveCatalog(
        categories: List<Category>,
        mediaAssets: List<MediaAsset>,
        questions: List<Question>,
    )
}

data class QuestionFilter(
    val examId: ExamId? = null,
    val examYear: Int? = null,
    val subjectIds: Set<String> = emptySet(),
    val topicId: String? = null,
)
