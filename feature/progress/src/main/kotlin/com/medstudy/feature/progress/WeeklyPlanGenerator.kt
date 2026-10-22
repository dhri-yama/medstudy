package com.medstudy.feature.progress

import com.medstudy.core.model.Category
import com.medstudy.core.model.StudyBlock
import com.medstudy.core.model.StudyPlanKind
import com.medstudy.core.model.StudySchedulePreferences
import com.medstudy.core.model.WeeklyStudyPlan
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

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
        // Review what is weak or likely forgotten, most-forgotten first.
        val reviewAreas = areas
            .filter { it.status == WeakAreaStatus.NEEDS_REVIEW || (it.retrievability ?: 1.0) < DUE_RETRIEVABILITY }
            .sortedWith(compareBy({ it.retrievability ?: 1.0 }, { it.accuracy ?: 1.0 }))
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
                questionTarget = scaledTarget(preferences.questionTargetPerDay, area.retrievability),
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

    /** The more likely a topic is forgotten, the more questions it gets (0.5x when fresh, up to 1.5x). */
    private fun scaledTarget(base: Int, retrievability: Double?): Int =
        if (retrievability == null) base else (base * (0.5 + (1.0 - retrievability))).roundToInt().coerceIn(1, 100)

    private companion object {
        const val DUE_RETRIEVABILITY = 0.85
    }
}
