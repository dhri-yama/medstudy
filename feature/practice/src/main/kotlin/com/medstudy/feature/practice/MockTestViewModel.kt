package com.medstudy.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medstudy.core.data.LearnerActivityRepository
import com.medstudy.core.data.QuestionCatalogRepository
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.PracticeContext
import com.medstudy.core.model.Question
import com.medstudy.core.model.QuestionAttempt
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A paper taken under exam conditions: one clock for the whole paper, no answers shown until it is submitted,
 * then a score and a review. Answers are saved (as [PracticeContext.MOCK_TEST] attempts) only on submit.
 * shortcut: the test lives in memory, so it is lost if Android closes the app mid-test; save it to the database if that becomes a complaint.
 */
class MockTestViewModel(
    private val catalogRepository: QuestionCatalogRepository,
    private val activityRepository: LearnerActivityRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MockTestUiState())
    val state: StateFlow<MockTestUiState> = mutableState.asStateFlow()

    fun start(title: String, questionIds: List<String>) {
        viewModelScope.launch {
            mutableState.value = MockTestUiState(isLoading = true, title = title)
            val questions = runCatching { questionIds.mapNotNull { catalogRepository.question(it) } }.getOrDefault(emptyList())
            val now = System.currentTimeMillis()
            mutableState.value = MockTestUiState(
                title = title,
                questions = questions,
                startedAtMillis = now,
                endsAtMillis = now + MockTestRules.durationMinutes(questions.size) * 60_000L,
            )
        }
    }

    /** Picks an option; tapping the chosen option again clears the answer. */
    fun select(optionId: String) = update { state ->
        val question = state.currentQuestion ?: return@update state
        if (state.isSubmitted) return@update state
        state.copy(answers = if (state.answers[question.id] == optionId) state.answers - question.id else state.answers + (question.id to optionId))
    }

    fun toggleFlag() = update { state ->
        val id = state.currentQuestion?.id ?: return@update state
        state.copy(flagged = if (id in state.flagged) state.flagged - id else state.flagged + id)
    }

    fun goTo(position: Int) = update { if (position in it.questions.indices) it.copy(position = position) else it }
    fun next() = goTo(mutableState.value.position + 1)
    fun previous() = goTo(mutableState.value.position - 1)

    /** Called every second by the screen; hands the paper in when the clock runs out. */
    fun tick(nowMillis: Long) {
        val state = mutableState.value
        if (state.questions.isNotEmpty() && !state.isSubmitted && nowMillis >= state.endsAtMillis) submit()
    }

    fun submit() {
        val state = mutableState.value
        if (state.isSubmitted || state.questions.isEmpty()) return
        val now = Instant.now()
        mutableState.value = state.copy(submittedAtMillis = minOf(now.toEpochMilli(), state.endsAtMillis), position = 0)
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            // Skipped questions are not saved, so they still count as unanswered in the paper's progress.
            state.questions.forEach { question ->
                val picked = state.answers[question.id] ?: return@forEach
                runCatching {
                    activityRepository.recordAttempt(
                        QuestionAttempt(
                            id = UUID.randomUUID().toString(),
                            questionId = question.id,
                            selectedOptionIds = listOf(picked),
                            result = state.resultFor(question)!!,
                            completedAtEpochMillis = now.toEpochMilli(),
                            studyDateIso = now.atZone(zone).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                            timeZoneId = zone.id,
                            context = PracticeContext.MOCK_TEST,
                        ),
                    )
                }
            }
        }
    }

    private inline fun update(change: (MockTestUiState) -> MockTestUiState) {
        mutableState.value = change(mutableState.value)
    }

    companion object {
        fun factory(catalogRepository: QuestionCatalogRepository, activityRepository: LearnerActivityRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = MockTestViewModel(catalogRepository, activityRepository) as T
            }
    }
}

/** Timing and marking, following NEET PG: 200 questions in 210 minutes, +4 for a correct answer, -1 for a wrong one, 0 for a skipped one. */
object MockTestRules {
    const val MARKS_CORRECT = 4
    const val MARKS_WRONG = -1
    private const val SECONDS_PER_QUESTION = 63

    /** The same pace as the real paper, rounded up to a whole minute. */
    fun durationMinutes(questionCount: Int): Int = (questionCount * SECONDS_PER_QUESTION + 59) / 60

    fun score(correct: Int, wrong: Int): Int = correct * MARKS_CORRECT + wrong * MARKS_WRONG
}

data class MockTestUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val questions: List<Question> = emptyList(),
    val position: Int = 0,
    /** Chosen option id by question id. */
    val answers: Map<String, String> = emptyMap(),
    /** Questions marked to come back to. */
    val flagged: Set<String> = emptySet(),
    val startedAtMillis: Long = 0L,
    val endsAtMillis: Long = 0L,
    val submittedAtMillis: Long? = null,
) {
    val isSubmitted: Boolean get() = submittedAtMillis != null
    val currentQuestion: Question? get() = questions.getOrNull(position)
    val answeredCount: Int get() = answers.size

    /** Null for a skipped question. */
    fun resultFor(question: Question): AttemptResult? = answers[question.id]?.let { picked ->
        if (setOf(picked) == question.answerKey.correctOptionIds.toSet()) AttemptResult.CORRECT else AttemptResult.INCORRECT
    }

    val correctCount: Int get() = questions.count { resultFor(it) == AttemptResult.CORRECT }
    val wrongCount: Int get() = questions.count { resultFor(it) == AttemptResult.INCORRECT }
    val skippedCount: Int get() = questions.size - answers.size
    val score: Int get() = MockTestRules.score(correctCount, wrongCount)
    val maxScore: Int get() = questions.size * MockTestRules.MARKS_CORRECT
    val minutesUsed: Int get() = (((submittedAtMillis ?: startedAtMillis) - startedAtMillis) / 60_000L).toInt()
}
