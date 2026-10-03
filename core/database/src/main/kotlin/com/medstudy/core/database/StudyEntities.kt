package com.medstudy.core.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "categories", indices = [Index("parentId")])
data class CategoryEntity(
    @PrimaryKey val id: String,
    val parentId: String?,
    val name: String,
    val description: String?,
    val position: Int,
)

@Entity(tableName = "media_assets")
data class MediaAssetEntity(
    @PrimaryKey val id: String,
    val relativePath: String,
    val mimeType: String,
    val sha256: String,
    val widthPx: Int,
    val heightPx: Int,
    val contentKind: String,
    val rightsStatus: String,
)

@Entity(tableName = "questions", indices = [Index("examId"), Index("subjectId")])
data class QuestionEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val examYear: Int?,
    val subjectId: String,
    val topicIds: List<String>,
    val format: String,
    val languageTag: String,
    val provenanceKind: String,
    val sourceName: String,
    val sourceReference: String?,
    val rightsStatus: String,
    val attribution: String?,
    val reviewStatus: String,
    val revision: Int,
)

@Entity(tableName = "question_content_blocks", indices = [Index("questionId")])
data class QuestionContentBlockEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val questionId: String,
    val optionId: String?,
    val position: Int,
    val type: String,
    val text: String?,
    val assetId: String?,
    val altText: String?,
    val caption: String?,
)

@Entity(tableName = "question_options", primaryKeys = ["questionId", "id"])
data class QuestionOptionEntity(
    val questionId: String,
    val id: String,
    val label: String,
    val position: Int,
)

@Entity(tableName = "answer_keys")
data class AnswerKeyEntity(
    @PrimaryKey val questionId: String,
    val correctOptionIds: List<String>,
    val explanation: String,
)

@Entity(tableName = "option_rationales", primaryKeys = ["questionId", "optionId"])
data class OptionRationaleEntity(
    val questionId: String,
    val optionId: String,
    val text: String,
)

@Entity(tableName = "question_references", primaryKeys = ["questionId", "citation"])
data class QuestionReferenceEntity(
    val questionId: String,
    val citation: String,
    val url: String?,
    val accessedOn: String?,
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val questionId: String,
    val savedAtEpochMillis: Long,
)

@Entity(
    tableName = "question_attempts",
    indices = [Index("studyDateIso"), Index("questionId")],
)
data class QuestionAttemptEntity(
    @PrimaryKey val id: String,
    val questionId: String,
    val selectedOptionIds: List<String>,
    val result: String,
    val completedAtEpochMillis: Long,
    val studyDateIso: String,
    val timeZoneId: String,
    val context: String,
)

@Entity(tableName = "weekly_study_plans")
data class WeeklyStudyPlanEntity(
    @PrimaryKey val id: String,
    val weekStartDateIso: String,
    val generatedAtEpochMillis: Long,
    val kind: String,
)

@Entity(tableName = "study_blocks", indices = [Index("planId"), Index("studyDateIso")])
data class StudyBlockEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val studyDateIso: String,
    val subjectId: String,
    val topicId: String?,
    val title: String,
    val questionTarget: Int,
    val minuteTarget: Int,
    val position: Int,
)

@Entity(tableName = "study_schedule_preferences")
data class StudySchedulePreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val availableWeekdays: List<Int>,
    val questionTargetPerDay: Int,
    val minuteTargetPerDay: Int,
)

data class QuestionWithDetails(
    @Embedded val question: QuestionEntity,
    @Relation(parentColumn = "id", entityColumn = "questionId")
    val contentBlocks: List<QuestionContentBlockEntity>,
    @Relation(parentColumn = "id", entityColumn = "questionId")
    val options: List<QuestionOptionEntity>,
    @Relation(parentColumn = "id", entityColumn = "questionId")
    val answerKeys: List<AnswerKeyEntity>,
    @Relation(parentColumn = "id", entityColumn = "questionId")
    val rationales: List<OptionRationaleEntity>,
    @Relation(parentColumn = "id", entityColumn = "questionId")
    val references: List<QuestionReferenceEntity>,
)

data class WeeklyStudyPlanWithBlocks(
    @Embedded val plan: WeeklyStudyPlanEntity,
    @Relation(parentColumn = "id", entityColumn = "planId")
    val blocks: List<StudyBlockEntity>,
)
