package com.medstudy.core.data

object DemoCatalogInitializer {
    suspend fun ensureSeeded(repository: QuestionCatalogRepository) {
        if (repository.isEmpty()) {
            repository.saveCatalog(
                categories = DemoQuestionCatalog.categories,
                mediaAssets = DemoQuestionCatalog.mediaAssets,
                questions = DemoQuestionCatalog.questions,
            )
        }
    }
}
