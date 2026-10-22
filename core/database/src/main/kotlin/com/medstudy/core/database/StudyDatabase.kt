package com.medstudy.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray

class StudyConverters {
    @TypeConverter
    fun intListToJson(values: List<Int>): String = JSONArray().apply {
        values.forEach(::put)
    }.toString()

    @TypeConverter
    fun jsonToIntList(value: String): List<Int> {
        val array = JSONArray(value)
        return List(array.length()) { index -> array.getInt(index) }
    }

    @TypeConverter
    fun stringListToJson(values: List<String>): String = JSONArray().apply {
        values.forEach(::put)
    }.toString()

    @TypeConverter
    fun jsonToStringList(value: String): List<String> {
        val array = JSONArray(value)
        return List(array.length()) { index -> array.getString(index) }
    }
}

@Database(
    entities = [
        CategoryEntity::class,
        MediaAssetEntity::class,
        QuestionEntity::class,
        QuestionContentBlockEntity::class,
        QuestionOptionEntity::class,
        AnswerKeyEntity::class,
        OptionRationaleEntity::class,
        QuestionReferenceEntity::class,
        BookmarkEntity::class,
        QuestionAttemptEntity::class,
        WeeklyStudyPlanEntity::class,
        StudyBlockEntity::class,
        StudySchedulePreferencesEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(StudyConverters::class)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun activityDao(): ActivityDao
    abstract fun studyPlanDao(): StudyPlanDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS weekly_study_plans (
                        id TEXT NOT NULL PRIMARY KEY,
                        weekStartDateIso TEXT NOT NULL,
                        generatedAtEpochMillis INTEGER NOT NULL,
                        kind TEXT NOT NULL
                    )""".trimIndent(),
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS study_blocks (
                        id TEXT NOT NULL PRIMARY KEY,
                        planId TEXT NOT NULL,
                        studyDateIso TEXT NOT NULL,
                        subjectId TEXT NOT NULL,
                        topicId TEXT,
                        title TEXT NOT NULL,
                        questionTarget INTEGER NOT NULL,
                        minuteTarget INTEGER NOT NULL,
                        position INTEGER NOT NULL
                    )""".trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_study_blocks_planId ON study_blocks(planId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_study_blocks_studyDateIso ON study_blocks(studyDateIso)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE question_attempts ADD COLUMN userId TEXT NOT NULL DEFAULT 'local'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_question_attempts_userId ON question_attempts(userId)")
                db.execSQL(
                    """CREATE TABLE bookmarks_new (
                        userId TEXT NOT NULL,
                        questionId TEXT NOT NULL,
                        savedAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(userId, questionId)
                    )""".trimIndent(),
                )
                db.execSQL("INSERT INTO bookmarks_new SELECT 'local', questionId, savedAtEpochMillis FROM bookmarks")
                db.execSQL("DROP TABLE bookmarks")
                db.execSQL("ALTER TABLE bookmarks_new RENAME TO bookmarks")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS study_schedule_preferences (
                        id INTEGER NOT NULL PRIMARY KEY,
                        availableWeekdays TEXT NOT NULL,
                        questionTargetPerDay INTEGER NOT NULL,
                        minuteTargetPerDay INTEGER NOT NULL
                    )""".trimIndent(),
                )
            }
        }
    }
}
