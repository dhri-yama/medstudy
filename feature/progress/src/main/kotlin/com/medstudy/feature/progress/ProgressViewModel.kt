package com.medstudy.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medstudy.core.data.LearnerActivityRepository
import com.medstudy.core.data.QuestionCatalogRepository
import com.medstudy.core.data.QuestionFilter
import com.medstudy.core.data.WeeklyStudyPlanRepository
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.Category
import com.medstudy.core.model.Question
import com.medstudy.core.model.QuestionAttempt
import com.medstudy.core.model.StudySchedulePreferences
import com.medstudy.core.model.StudyBlock
import com.medstudy.core.model.StudyPlanKind
import com.medstudy.core.model.WeeklyStudyPlan
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val catalogRepository: QuestionCatalogRepository,
    private val activityRepository: LearnerActivityRepository,
    private val planRepository: WeeklyStudyPlanRepository,
) : ViewModel() {
    private val insights = StudyInsights()
    private val planGenerator = WeeklyPlanGenerator()
    private val mutableState = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, error = null)
            runCatching {
                val categories = catalogRepository.categories()
                // Progress must be able to resolve attempts against every imported question.
                val questions = catalogRepository.questions(QuestionFilter())
                val attempts = activityRepository.attempts()
                val today = LocalDate.now()
                val areas = insights.analyze(today, attempts, questions, categories)
                val preferences = planRepository.preferences()
                val storedPlan = planRepository.get()
                val activePlan = storedPlan?.takeIf { plan ->
                    val start = LocalDate.parse(plan.weekStartDateIso)
                    !today.isBefore(start) && today.isBefore(start.plusDays(7))
                } ?: planGenerator.generate(today, areas, categories, System.currentTimeMillis(), preferences).also {
                    planRepository.save(it)
                }
                ProgressUiState(
                    isLoading = false,
                    currentMonth = mutableState.value.currentMonth,
                    selectedDate = mutableState.value.selectedDate,
                    selectedSubjectIds = mutableState.value.selectedSubjectIds,
                    preferences = preferences,
                    categories = categories,
                    questions = questions,
                    attempts = attempts,
                    areas = areas,
                    plan = activePlan,
                ).withDerivedData()
            }.onSuccess { mutableState.value = it }
                .onFailure { mutableState.value = mutableState.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun selectDate(date: LocalDate) {
        mutableState.value = mutableState.value.copy(selectedDate = date).withDerivedData()
    }

    fun changeMonth(offset: Long) {
        val current = mutableState.value
        val nextMonth = current.currentMonth.plusMonths(offset)
        val nextSelectedDate = if (YearMonth.from(current.selectedDate) == nextMonth) {
            current.selectedDate
        } else {
            nextMonth.atDay(current.selectedDate.dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth()))
        }
        mutableState.value = current.copy(currentMonth = nextMonth, selectedDate = nextSelectedDate).withDerivedData()
    }

    fun toggleSubject(subjectId: String?) {
        val current = mutableState.value
        val selected = when {
            subjectId == null -> emptySet()
            subjectId in current.selectedSubjectIds -> current.selectedSubjectIds - subjectId
            else -> current.selectedSubjectIds + subjectId
        }
        mutableState.value = current.copy(selectedSubjectIds = selected).withDerivedData()
    }

    fun regeneratePlan() {
        val current = mutableState.value
        val generated = planGenerator.generate(
            startDate = LocalDate.now(),
            areas = current.areas,
            categories = current.categories,
            generatedAtEpochMillis = System.currentTimeMillis(),
            preferences = current.preferences,
        )
        savePlan(generated)
    }

    fun toggleStudyDay(isoWeekday: Int) {
        val current = mutableState.value
        val selectedDays = current.preferences.availableWeekdays
        if (isoWeekday in selectedDays && selectedDays.size == 1) return
        val updated = current.preferences.copy(
            availableWeekdays = if (isoWeekday in selectedDays) selectedDays - isoWeekday else selectedDays + isoWeekday,
        )
        mutableState.value = current.copy(preferences = updated)
        viewModelScope.launch {
            runCatching { planRepository.savePreferences(updated) }
                .onFailure { mutableState.value = mutableState.value.copy(error = it.message) }
        }
    }

    fun adjustQuestionTargetPerDay(delta: Int) {
        updatePreferences { it.copy(questionTargetPerDay = (it.questionTargetPerDay + delta).coerceIn(1, 100)) }
    }

    fun adjustMinuteTargetPerDay(delta: Int) {
        updatePreferences { it.copy(minuteTargetPerDay = (it.minuteTargetPerDay + delta).coerceIn(5, 240)) }
    }

    private fun updatePreferences(update: (StudySchedulePreferences) -> StudySchedulePreferences) {
        val updated = update(mutableState.value.preferences)
        mutableState.value = mutableState.value.copy(preferences = updated)
        viewModelScope.launch {
            runCatching { planRepository.savePreferences(updated) }
                .onFailure { mutableState.value = mutableState.value.copy(error = it.message) }
        }
    }

    fun moveBlock(blockId: String, dayOffset: Long) {
        val currentPlan = mutableState.value.plan ?: return
        val start = LocalDate.parse(currentPlan.weekStartDateIso)
        val availableDates = (0L..6L).map(start::plusDays)
            .filter { it.dayOfWeek.value in mutableState.value.preferences.availableWeekdays }
        if (availableDates.isEmpty()) return
        val updatedBlocks = currentPlan.blocks.map { block ->
            if (block.id != blockId) block else {
                val currentIndex = availableDates.indexOfFirst { it.toString() == block.studyDateIso }
                val nextIndex = (currentIndex + dayOffset.toInt()).coerceIn(0, availableDates.lastIndex)
                block.copy(studyDateIso = availableDates.getOrNull(nextIndex)?.toString() ?: block.studyDateIso)
            }
        }
        savePlan(currentPlan.copy(kind = StudyPlanKind.EDITED, blocks = updatedBlocks))
    }

    fun removeBlock(blockId: String) {
        val currentPlan = mutableState.value.plan ?: return
        savePlan(currentPlan.copy(kind = StudyPlanKind.EDITED, blocks = currentPlan.blocks.filterNot { it.id == blockId }))
    }

    fun addBlock(areaId: String) {
        val current = mutableState.value
        val area = current.areas.firstOrNull { it.id == areaId } ?: return
        val plan = current.plan ?: return
        val start = LocalDate.parse(plan.weekStartDateIso)
        val countsByDate = plan.blocks.groupingBy(StudyBlock::studyDateIso).eachCount()
        val targetDate = (0L..6L)
            .map(start::plusDays)
            .filter { it.dayOfWeek.value in current.preferences.availableWeekdays }
            .minByOrNull { countsByDate[it.toString()] ?: 0 }
            ?: start
        val block = StudyBlock(
            id = UUID.randomUUID().toString(),
            studyDateIso = targetDate.toString(),
            subjectId = area.subjectId,
            topicId = area.topicId,
            title = area.title,
            questionTarget = current.preferences.questionTargetPerDay,
            minuteTarget = current.preferences.minuteTargetPerDay,
            position = plan.blocks.count { it.studyDateIso == targetDate.toString() },
        )
        savePlan(plan.copy(kind = StudyPlanKind.EDITED, blocks = plan.blocks + block))
    }

    private fun savePlan(plan: WeeklyStudyPlan) {
        mutableState.value = mutableState.value.copy(plan = plan, error = null)
        viewModelScope.launch {
            runCatching { planRepository.save(plan) }
                .onFailure { mutableState.value = mutableState.value.copy(error = it.message) }
        }
    }

    companion object {
        fun factory(
            catalogRepository: QuestionCatalogRepository,
            activityRepository: LearnerActivityRepository,
            planRepository: WeeklyStudyPlanRepository,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProgressViewModel(catalogRepository, activityRepository, planRepository) as T
        }
    }
}

data class AttemptHistoryEntry(
    val attempt: QuestionAttempt,
    val question: Question,
)

data class ProgressUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedSubjectIds: Set<String> = emptySet(),
    val preferences: StudySchedulePreferences = StudySchedulePreferences(),
    val categories: List<Category> = emptyList(),
    val questions: List<Question> = emptyList(),
    val attempts: List<QuestionAttempt> = emptyList(),
    val dayCounts: Map<LocalDate, Int> = emptyMap(),
    val selectedDayEntries: List<AttemptHistoryEntry> = emptyList(),
    val areas: List<WeakArea> = emptyList(),
    val plan: WeeklyStudyPlan? = null,
) {
    val subjects get() = categories.filter { it.parentId == null }.sortedBy(Category::position)

    fun withDerivedData(): ProgressUiState {
        val questionById = questions.associateBy(Question::id)
        val filteredAttempts = attempts.filter { attempt ->
            val question = questionById[attempt.questionId]
            question != null && (selectedSubjectIds.isEmpty() || question.subjectId in selectedSubjectIds)
        }
        val counts = filteredAttempts.groupingBy { attempt ->
            runCatching { LocalDate.parse(attempt.studyDateIso) }.getOrNull()
        }.eachCount().mapNotNull { (date, count) -> date?.let { it to count } }.toMap()
        val selectedEntries = filteredAttempts
            .filter { it.studyDateIso == selectedDate.toString() }
            .sortedByDescending(QuestionAttempt::completedAtEpochMillis)
            .mapNotNull { attempt -> questionById[attempt.questionId]?.let { AttemptHistoryEntry(attempt, it) } }
        return copy(dayCounts = counts, selectedDayEntries = selectedEntries)
    }
}
