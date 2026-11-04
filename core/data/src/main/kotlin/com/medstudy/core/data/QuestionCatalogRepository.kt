package com.medstudy.core.data

import com.medstudy.core.model.Category
import com.medstudy.core.model.ExamId
import com.medstudy.core.model.ExamPaper
import com.medstudy.core.model.MediaAsset
import com.medstudy.core.model.Question

interface QuestionCatalogRepository {
    suspend fun isEmpty(): Boolean
    suspend fun categories(): List<Category>
    suspend fun examPapers(): List<ExamPaper>
    suspend fun questions(filter: QuestionFilter = QuestionFilter()): List<Question>
    suspend fun question(questionId: String): Question?
    suspend fun bookmarkedQuestions(): List<Question>
    suspend fun saveCatalog(
        categories: List<Category>,
        mediaAssets: List<MediaAsset>,
        questions: List<Question>,
        examPapers: List<ExamPaper> = emptyList(),
    )
}

data class QuestionFilter(
    val examId: ExamId? = null,
    val examYear: Int? = null,
    val subjectIds: Set<String> = emptySet(),
    val topicId: String? = null,
    /** Restrict to one exam paper, e.g. "NEETPG2025"; results come back in the paper's original order. */
    val examPaperId: String? = null,
)
