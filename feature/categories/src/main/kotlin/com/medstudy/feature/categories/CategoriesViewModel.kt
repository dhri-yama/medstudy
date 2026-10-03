package com.medstudy.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medstudy.core.data.QuestionCatalogRepository
import com.medstudy.core.data.QuestionFilter
import com.medstudy.core.model.Category
import com.medstudy.core.model.ExamId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val repository: QuestionCatalogRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CategoriesUiState())
    val state: StateFlow<CategoriesUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, error = null)
            runCatching {
                val categories = repository.categories()
                val questions = repository.questions(QuestionFilter(examId = ExamId.DEMO))
                CategoriesUiState(
                    isLoading = false,
                    categories = categories,
                    questionCounts = categories.associate { category ->
                        category.id to questions.count { question ->
                            question.subjectId == category.id || category.id in question.topicIds
                        }
                    },
                )
            }.onSuccess { mutableState.value = it }
                .onFailure { mutableState.value = CategoriesUiState(isLoading = false, error = it.message) }
        }
    }

    companion object {
        fun factory(repository: QuestionCatalogRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CategoriesViewModel(repository) as T
        }
    }
}

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val questionCounts: Map<String, Int> = emptyMap(),
    val error: String? = null,
)
