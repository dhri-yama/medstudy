package com.medstudy.app

import android.content.Context
import androidx.room.Room
import com.medstudy.core.data.RoomLearnerActivityRepository
import com.medstudy.core.data.RoomQuestionCatalogRepository
import com.medstudy.core.database.StudyDatabase

class StudyAppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        StudyDatabase::class.java,
        "medstudy.db",
    ).build()

    val questionCatalog = RoomQuestionCatalogRepository(database)
    val learnerActivity = RoomLearnerActivityRepository(database.activityDao())
}
