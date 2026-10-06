package com.medstudy.app

import android.content.Context
import androidx.room.Room
import com.medstudy.core.data.MedMcqaRepository
import com.medstudy.core.data.RoomLearnerActivityRepository
import com.medstudy.core.data.RoomQuestionCatalogRepository
import com.medstudy.core.database.StudyDatabase

class StudyAppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        StudyDatabase::class.java,
        "medstudy.db",
    ).addMigrations(StudyDatabase.MIGRATION_1_2, StudyDatabase.MIGRATION_2_3).build()

    // Production MedMCQA data repository
    val medMcqaRepository = MedMcqaRepository(context.applicationContext)
    
    // Room-based repositories for user data
    val questionCatalog = RoomQuestionCatalogRepository(database)
    val learnerActivity = RoomLearnerActivityRepository(database.activityDao())
    val weeklyStudyPlan = com.medstudy.core.data.RoomWeeklyStudyPlanRepository(database)
}
