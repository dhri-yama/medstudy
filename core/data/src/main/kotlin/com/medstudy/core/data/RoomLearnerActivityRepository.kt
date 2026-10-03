package com.medstudy.core.data

import com.medstudy.core.database.ActivityDao
import com.medstudy.core.database.BookmarkEntity
import com.medstudy.core.database.QuestionAttemptEntity
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.Bookmark
import com.medstudy.core.model.PracticeContext
import com.medstudy.core.model.QuestionAttempt

class RoomLearnerActivityRepository(
    private val activityDao: ActivityDao,
) : LearnerActivityRepository {
    override suspend fun bookmark(questionId: String, savedAtEpochMillis: Long) {
        activityDao.putBookmark(BookmarkEntity(questionId, savedAtEpochMillis))
    }

    override suspend fun removeBookmark(questionId: String) {
        activityDao.removeBookmark(questionId)
    }

    override suspend fun isBookmarked(questionId: String): Boolean =
        activityDao.getBookmark(questionId) != null

    override suspend fun bookmarks(): List<Bookmark> = activityDao.getBookmarks().map { entity ->
        Bookmark(entity.questionId, entity.savedAtEpochMillis)
    }

    override suspend fun recordAttempt(attempt: QuestionAttempt) {
        activityDao.putAttempt(
            QuestionAttemptEntity(
                id = attempt.id,
                questionId = attempt.questionId,
                selectedOptionIds = attempt.selectedOptionIds,
                result = attempt.result.name,
                completedAtEpochMillis = attempt.completedAtEpochMillis,
                studyDateIso = attempt.studyDateIso,
                timeZoneId = attempt.timeZoneId,
                context = attempt.context.name,
            ),
        )
    }

    override suspend fun attempts(): List<QuestionAttempt> =
        activityDao.getAttempts().map(QuestionAttemptEntity::toDomain)

    override suspend fun attemptsForDate(studyDateIso: String): List<QuestionAttempt> =
        activityDao.getAttemptsForDate(studyDateIso).map(QuestionAttemptEntity::toDomain)
}

private fun QuestionAttemptEntity.toDomain() = QuestionAttempt(
    id = id,
    questionId = questionId,
    selectedOptionIds = selectedOptionIds,
    result = AttemptResult.valueOf(result),
    completedAtEpochMillis = completedAtEpochMillis,
    studyDateIso = studyDateIso,
    timeZoneId = timeZoneId,
    context = PracticeContext.valueOf(context),
)
