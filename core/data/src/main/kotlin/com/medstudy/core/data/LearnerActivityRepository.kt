package com.medstudy.core.data

import com.medstudy.core.model.Bookmark
import com.medstudy.core.model.QuestionAttempt

interface LearnerActivityRepository {
    suspend fun bookmark(questionId: String, savedAtEpochMillis: Long)
    suspend fun removeBookmark(questionId: String)
    suspend fun isBookmarked(questionId: String): Boolean
    suspend fun bookmarks(): List<Bookmark>
    suspend fun recordAttempt(attempt: QuestionAttempt)
    suspend fun attempts(): List<QuestionAttempt>
    suspend fun attemptsForDate(studyDateIso: String): List<QuestionAttempt>
}
