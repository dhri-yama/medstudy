package com.medstudy.core.data

import androidx.room.withTransaction
import com.medstudy.core.database.StudyBlockEntity
import com.medstudy.core.database.StudyDatabase
import com.medstudy.core.database.WeeklyStudyPlanEntity
import com.medstudy.core.database.WeeklyStudyPlanWithBlocks
import com.medstudy.core.database.StudySchedulePreferencesEntity
import com.medstudy.core.model.StudyBlock
import com.medstudy.core.model.StudySchedulePreferences
import com.medstudy.core.model.StudyPlanKind
import com.medstudy.core.model.WeeklyStudyPlan

class RoomWeeklyStudyPlanRepository(
    private val database: StudyDatabase,
) : WeeklyStudyPlanRepository {
    private val dao = database.studyPlanDao()

    override suspend fun get(planId: String): WeeklyStudyPlan? =
        dao.getPlan(planId)?.toDomain()

    override suspend fun preferences(): StudySchedulePreferences =
        dao.getPreferences()?.toDomain() ?: StudySchedulePreferences()

    override suspend fun savePreferences(preferences: StudySchedulePreferences) {
        dao.putPreferences(preferences.toEntity())
    }

    override suspend fun save(plan: WeeklyStudyPlan) {
        database.withTransaction {
            dao.putPlan(
                WeeklyStudyPlanEntity(
                    id = plan.id,
                    weekStartDateIso = plan.weekStartDateIso,
                    generatedAtEpochMillis = plan.generatedAtEpochMillis,
                    kind = plan.kind.name,
                ),
            )
            dao.clearBlocks(plan.id)
            dao.putBlocks(plan.blocks.map { block ->
                StudyBlockEntity(
                    id = block.id,
                    planId = plan.id,
                    studyDateIso = block.studyDateIso,
                    subjectId = block.subjectId,
                    topicId = block.topicId,
                    title = block.title,
                    questionTarget = block.questionTarget,
                    minuteTarget = block.minuteTarget,
                    position = block.position,
                )
            })
        }
    }
}

private fun StudySchedulePreferences.toEntity() = StudySchedulePreferencesEntity(
    availableWeekdays = availableWeekdays.sorted(),
    questionTargetPerDay = questionTargetPerDay,
    minuteTargetPerDay = minuteTargetPerDay,
)

private fun StudySchedulePreferencesEntity.toDomain() = StudySchedulePreferences(
    availableWeekdays = availableWeekdays.toSet(),
    questionTargetPerDay = questionTargetPerDay,
    minuteTargetPerDay = minuteTargetPerDay,
)

private fun WeeklyStudyPlanWithBlocks.toDomain() = WeeklyStudyPlan(
    id = plan.id,
    weekStartDateIso = plan.weekStartDateIso,
    generatedAtEpochMillis = plan.generatedAtEpochMillis,
    kind = StudyPlanKind.valueOf(plan.kind),
    blocks = blocks.sortedWith(compareBy(StudyBlockEntity::studyDateIso, StudyBlockEntity::position)).map { block ->
        StudyBlock(
            id = block.id,
            studyDateIso = block.studyDateIso,
            subjectId = block.subjectId,
            topicId = block.topicId,
            title = block.title,
            questionTarget = block.questionTarget,
            minuteTarget = block.minuteTarget,
            position = block.position,
        )
    },
)
