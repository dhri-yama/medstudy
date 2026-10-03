package com.medstudy.core.data

import com.medstudy.core.model.WeeklyStudyPlan
import com.medstudy.core.model.StudySchedulePreferences

interface WeeklyStudyPlanRepository {
    suspend fun get(planId: String = "weekly"): WeeklyStudyPlan?
    suspend fun save(plan: WeeklyStudyPlan)
    suspend fun preferences(): StudySchedulePreferences
    suspend fun savePreferences(preferences: StudySchedulePreferences)
}
