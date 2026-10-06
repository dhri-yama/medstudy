package com.medstudy.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.medstudy.core.data.MedMcqaRepository
import com.medstudy.core.data.QuestionCatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CatalogBootstrapViewModel(
    private val repository: QuestionCatalogRepository,
    private val medMcqaRepository: MedMcqaRepository? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BootstrapUiState(isLoading = true))
    val state: StateFlow<BootstrapUiState> = mutableState.asStateFlow()

    init {
        retry()
    }

    fun retry() {
        mutableState.value = BootstrapUiState(isLoading = true)
        viewModelScope.launch {
            try {
                // Try to load MedMCQA data first
                if (medMcqaRepository != null) {
                    val questions = medMcqaRepository.getAllQuestions()
                    val categories = medMcqaRepository.getAllCategories()
                    
                    if (questions.isNotEmpty() && categories.isNotEmpty()) {
                        // Save MedMCQA data to database
                        repository.saveCatalog(
                            categories = categories,
                            mediaAssets = emptyList(), // No media assets in MedMCQA yet
                            questions = questions,
                        )
                        mutableState.value = BootstrapUiState(isLoading = false, isReady = true)
                        return@launch
                    }
                } else {
                    // If MedMCQA data is empty or repository not available, show error
                    mutableState.value = BootstrapUiState(isLoading = false, error = "No question data available")
                }
                
            } catch (e: Exception) {
                mutableState.value = BootstrapUiState(isLoading = false, error = e.message)
            }
        }
    }

    companion object {
        fun factory(
            repository: QuestionCatalogRepository,
            medMcqaRepository: MedMcqaRepository? = null
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CatalogBootstrapViewModel(repository, medMcqaRepository) as T
        }
    }
}

data class BootstrapUiState(
    val isLoading: Boolean = false,
    val isReady: Boolean = false,
    val error: String? = null,
)
