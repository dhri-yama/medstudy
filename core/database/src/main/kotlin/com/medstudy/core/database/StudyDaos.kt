package com.medstudy.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM categories ORDER BY position, name")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories ORDER BY position, name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun questionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMediaAssets(assets: List<MediaAssetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putContentBlocks(blocks: List<QuestionContentBlockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putOptions(options: List<QuestionOptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAnswerKeys(answerKeys: List<AnswerKeyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putRationales(rationales: List<OptionRationaleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putReferences(references: List<QuestionReferenceEntity>)

    @Transaction
    @Query("""
        SELECT * FROM questions
        WHERE (:examId IS NULL OR examId = :examId)
          AND (:examYear IS NULL OR examYear = :examYear)
          AND (:subjectId IS NULL OR subjectId = :subjectId)
        ORDER BY subjectId, id
    """)
    suspend fun getQuestions(
        examId: String?,
        examYear: Int?,
        subjectId: String?,
    ): List<QuestionWithDetails>

    @Transaction
    @Query("SELECT * FROM questions WHERE id = :questionId")
    suspend fun getQuestion(questionId: String): QuestionWithDetails?

    @Transaction
    @Query("SELECT questions.* FROM questions INNER JOIN bookmarks ON questions.id = bookmarks.questionId ORDER BY bookmarks.savedAtEpochMillis DESC")
    suspend fun getBookmarkedQuestions(): List<QuestionWithDetails>
}

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE questionId = :questionId")
    suspend fun removeBookmark(questionId: String)

    @Query("SELECT * FROM bookmarks WHERE questionId = :questionId")
    suspend fun getBookmark(questionId: String): BookmarkEntity?

    @Query("SELECT * FROM bookmarks ORDER BY savedAtEpochMillis DESC")
    suspend fun getBookmarks(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAttempt(attempt: QuestionAttemptEntity)

    @Query("SELECT * FROM question_attempts ORDER BY completedAtEpochMillis DESC")
    suspend fun getAttempts(): List<QuestionAttemptEntity>

    @Query("SELECT * FROM question_attempts WHERE studyDateIso = :studyDateIso ORDER BY completedAtEpochMillis DESC")
    suspend fun getAttemptsForDate(studyDateIso: String): List<QuestionAttemptEntity>
}
