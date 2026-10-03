package com.medstudy.feature.progress

import com.medstudy.core.model.Category
import com.medstudy.core.model.StudyBlock
import com.medstudy.core.model.StudyPlanKind
import com.medstudy.core.model.StudySchedulePreferences
import com.medstudy.core.model.WeeklyStudyPlan
import java.time.LocalDate
import java.util.UUID

class WeeklyPlanGenerator(
    private val defaultPreferences: StudySchedulePreferences = StudySchedulePreferences(),
) {
    fun generate(
        startDate: LocalDate,
        areas: List<WeakArea>,
        categories: List<Category>,
        generatedAtEpochMillis: Long,
        preferences: StudySchedulePreferences = defaultPreferences,
    ): WeeklyStudyPlan {
        val categoryById = categories.associateBy(Category::id)
        val reviewAreas = areas.filter { it.status == WeakAreaStatus.NEEDS_REVIEW }
        val topics = if (reviewAreas.isNotEmpty()) {
            reviewAreas
        } else {
            areas.filter { it.topicId != null }.ifEmpty {
                categories.filter { it.parentId != null }.map { topic ->
                    val subject = categoryById[topic.parentId] ?: topic
                    WeakArea(
                        id = topic.id,
                        subjectId = subject.id,
                        topicId = topic.id,
                        title = topic.name,
                        answeredAttempts = 0,
                        accuracy = null,
                        status = WeakAreaStatus.UNASSESSED,
                    )
                }
            }
        }
        val studyDates = (0L..6L).map(startDate::plusDays)
            .filter { it.dayOfWeek.value in preferences.availableWeekdays }
        val blocks = studyDates.mapIndexedNotNull { position, date ->
            val area = topics.getOrNull((position % topics.size.coerceAtLeast(1))) ?: return@mapIndexedNotNull null
            StudyBlock(
                id = UUID.randomUUID().toString(),
                studyDateIso = date.toString(),
                subjectId = area.subjectId,
                topicId = area.topicId,
                title = area.title,
                questionTarget = preferences.questionTargetPerDay,
                minuteTarget = preferences.minuteTargetPerDay,
                position = 0,
            )
        }
        return WeeklyStudyPlan(
            id = "weekly",
            weekStartDateIso = startDate.toString(),
            generatedAtEpochMillis = generatedAtEpochMillis,
            kind = if (reviewAreas.isEmpty()) StudyPlanKind.STARTER else StudyPlanKind.GENERATED,
            blocks = blocks,
        )
    }
}
