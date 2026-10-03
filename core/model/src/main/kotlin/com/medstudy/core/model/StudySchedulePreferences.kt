package com.medstudy.core.model

data class StudySchedulePreferences(
    val availableWeekdays: Set<Int> = (1..7).toSet(),
    val questionTargetPerDay: Int = 10,
    val minuteTargetPerDay: Int = 30,
) {
    init {
        require(availableWeekdays.isNotEmpty())
        require(availableWeekdays.all { it in 1..7 })
        require(questionTargetPerDay in 1..100)
        require(minuteTargetPerDay in 5..240)
    }
}
