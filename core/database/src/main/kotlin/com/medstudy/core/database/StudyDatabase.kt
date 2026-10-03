package com.medstudy.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray

class StudyConverters {
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
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(StudyConverters::class)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun activityDao(): ActivityDao
}
