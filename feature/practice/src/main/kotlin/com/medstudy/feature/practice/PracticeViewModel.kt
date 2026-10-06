package com.medstudy.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medstudy.core.data.LearnerActivityRepository
import com.medstudy.core.data.QuestionCatalogRepository
import com.medstudy.core.data.QuestionFilter
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.ExamId
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

class PracticeViewModel(
    private val catalogRepository: QuestionCatalogRepository,
    private val activityRepository: LearnerActivityRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PracticeUiState())
    val state: StateFlow<PracticeUiState> = mutableState.asStateFlow()

    fun start(subjectId: String? = null, topicId: String? = null, questionId: String? = null) {
        viewModelScope.launch {
            mutableState.value = PracticeUiState(isLoading = true)
            runCatching {
                when {
                    questionId != null -> listOfNotNull(catalogRepository.question(questionId))
                    else -> catalogRepository.questions(
                        QuestionFilter(
                            subjectIds = subjectId?.let(::setOf).orEmpty(),
                            topicId = topicId,
                        ),
                    )
                }
            }.onSuccess { questions ->
                mutableState.value = PracticeUiState(
                    isLoading = false,
                    questions = questions,
                    isBookmarked = questions.firstOrNull()?.let {
                        activityRepository.isBookmarked(it.id)
                    } ?: false,
                )
            }.onFailure {
                mutableState.value = PracticeUiState(isLoading = false, error = it.message)
            }
        }
    }

    fun startRandomUnattempted() {
        viewModelScope.launch {
            mutableState.value = PracticeUiState(isLoading = true)
            runCatching {
                val questions = catalogRepository.questions()
                val attemptedQuestionIds = activityRepository.attempts()
                    .asSequence()
                    .map(QuestionAttempt::questionId)
                    .toSet()
                questions.filterNot { it.id in attemptedQuestionIds }.randomOrNull()
            }.onSuccess { question ->
                mutableState.value = if (question == null) {
                    PracticeUiState(
                        isLoading = false,
                        error = "You’ve completed all available questions. Check back when more are added.",
                    )
                } else {
                    PracticeUiState(
                        isLoading = false,
                        questions = listOf(question),
                        isBookmarked = activityRepository.isBookmarked(question.id),
                    )
                }
            }.onFailure { error ->
                mutableState.value = PracticeUiState(
                    isLoading = false,
                    error = error.message ?: "Could not find an unattempted question.",
                )
            }
        }
    }

    fun selectOption(optionId: String) {
        val current = mutableState.value.currentQuestion ?: return
        if (mutableState.value.isRevealed || current.options.none { it.id == optionId }) return
        mutableState.value = mutableState.value.copy(selectedOptionIds = setOf(optionId), error = null)
    }

    fun submit() {
        completeAttempt(showAnswer = false)
    }

    fun showAnswer() {
        completeAttempt(showAnswer = true)
    }

    private fun completeAttempt(showAnswer: Boolean) {
        val currentState = mutableState.value
        val question = currentState.currentQuestion ?: return
        if (currentState.isRevealed || (!showAnswer && currentState.selectedOptionIds.isEmpty())) {
            if (!showAnswer && currentState.selectedOptionIds.isEmpty()) {
                mutableState.value = currentState.copy(error = "Choose an option or show the answer.")
            }
            return
        }

        viewModelScope.launch {
            val result = when {
                showAnswer -> AttemptResult.UNANSWERED
                currentState.selectedOptionIds == question.answerKey.correctOptionIds.toSet() -> AttemptResult.CORRECT
                else -> AttemptResult.INCORRECT
            }
            val completedAt = Instant.now()
            val zoneId = ZoneId.systemDefault()
            val attempt = QuestionAttempt(
                id = UUID.randomUUID().toString(),
                questionId = question.id,
                selectedOptionIds = currentState.selectedOptionIds.toList(),
                result = result,
                completedAtEpochMillis = completedAt.toEpochMilli(),
                studyDateIso = completedAt.atZone(zoneId).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                timeZoneId = zoneId.id,
                context = PracticeContext.TOPIC_PRACTICE,
            )
            runCatching { activityRepository.recordAttempt(attempt) }
                .onSuccess {
                    mutableState.value = currentState.copy(isRevealed = true, result = result, error = null)
                }
                .onFailure { mutableState.value = currentState.copy(error = it.message ?: "Could not save this attempt.") }
        }
    }

    fun toggleBookmark() {
        val question = mutableState.value.currentQuestion ?: return
        viewModelScope.launch {
            runCatching {
                if (mutableState.value.isBookmarked) {
                    activityRepository.removeBookmark(question.id)
                } else {
                    activityRepository.bookmark(question.id, System.currentTimeMillis())
                }
            }.onSuccess {
                mutableState.value = mutableState.value.copy(isBookmarked = !mutableState.value.isBookmarked)
            }.onFailure {
                mutableState.value = mutableState.value.copy(error = it.message ?: "Could not update bookmark.")
            }
        }
    }

    fun next() {
        moveTo(mutableState.value.position + 1)
    }

    fun previous() {
        moveTo(mutableState.value.position - 1)
    }

    private fun moveTo(position: Int) {
        val current = mutableState.value
        if (position !in current.questions.indices) return
        val question = current.questions[position]
        viewModelScope.launch {
            val bookmarked = activityRepository.isBookmarked(question.id)
            mutableState.value = current.copy(
                position = position,
                selectedOptionIds = emptySet(),
                isRevealed = false,
                isBookmarked = bookmarked,
                result = null,
                error = null,
            )
        }
    }

    companion object {
        fun factory(
            catalogRepository: QuestionCatalogRepository,
            activityRepository: LearnerActivityRepository,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PracticeViewModel(catalogRepository, activityRepository) as T
        }
    }
}

data class PracticeUiState(
    val isLoading: Boolean = false,
    val questions: List<Question> = emptyList(),
    val position: Int = 0,
    val selectedOptionIds: Set<String> = emptySet(),
    val isRevealed: Boolean = false,
    val isBookmarked: Boolean = false,
    val result: AttemptResult? = null,
    val error: String? = null,
) {
    val currentQuestion: Question? get() = questions.getOrNull(position)
}
