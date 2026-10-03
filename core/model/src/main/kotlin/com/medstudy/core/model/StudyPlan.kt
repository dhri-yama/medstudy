package com.medstudy.core.model

enum class StudyPlanKind {
    GENERATED,
    STARTER,
    EDITED,
}

data class StudyBlock(
    val id: String,
    val studyDateIso: String,
    val subjectId: String,
    val topicId: String?,
    val title: String,
    val questionTarget: Int,
    val minuteTarget: Int,
    val position: Int,
)

data class WeeklyStudyPlan(
    val id: String,
    val weekStartDateIso: String,
    val generatedAtEpochMillis: Long,
    val kind: StudyPlanKind,
    val blocks: List<StudyBlock>,
)
