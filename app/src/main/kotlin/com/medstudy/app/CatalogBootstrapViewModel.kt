package com.medstudy.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medstudy.core.data.DemoCatalogInitializer
import com.medstudy.core.data.QuestionCatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CatalogBootstrapViewModel(
    private val repository: QuestionCatalogRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BootstrapUiState(isLoading = true))
    val state: StateFlow<BootstrapUiState> = mutableState.asStateFlow()

    init {
        retry()
    }

    fun retry() {
        mutableState.value = BootstrapUiState(isLoading = true)
        viewModelScope.launch {
            runCatching { DemoCatalogInitializer.ensureSeeded(repository) }
                .onSuccess { mutableState.value = BootstrapUiState(isLoading = false, isReady = true) }
                .onFailure { mutableState.value = BootstrapUiState(isLoading = false, error = it.message) }
        }
    }

    companion object {
        fun factory(repository: QuestionCatalogRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CatalogBootstrapViewModel(repository) as T
        }
    }
}

data class BootstrapUiState(
    val isLoading: Boolean = false,
    val isReady: Boolean = false,
    val error: String? = null,
)
