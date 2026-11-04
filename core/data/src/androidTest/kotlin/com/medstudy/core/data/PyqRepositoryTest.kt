package com.medstudy.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.medstudy.core.database.StudyDatabase
import com.medstudy.core.model.ContentBlock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PyqRepositoryTest {
    @Test
    fun bundledPapersLoadValidateAndRoundTripInPaperOrder() = runBlocking {
        val assetContext = InstrumentationRegistry.getInstrumentation().context
        val pyq = PyqRepository(assetContext).load()
        assertEquals(6, pyq.papers.size)
        assertTrue(pyq.questions.size > 1000)

        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), StudyDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val repo = RoomQuestionCatalogRepository(database)
            val categories = MedMcqaRepository(assetContext).getAllCategories() + pyq.topics
            repo.saveCatalog(categories, pyq.mediaAssets, pyq.questions, pyq.papers)

            assertEquals(6, repo.examPapers().size)
            val paper = repo.questions(QuestionFilter(examPaperId = "NEETPG2022"))
            assertEquals(200, paper.size)
            assertEquals(paper.map { it.sourceNumber }.sortedBy { it }, paper.map { it.sourceNumber })
            val withImage = paper.first { q -> q.content.any { it is ContentBlock.Image } }
            val image = withImage.content.filterIsInstance<ContentBlock.Image>().first()
            assertTrue(assetContext.assets.open(image.assetId).use { it.read() } >= 0)
        } finally {
            database.close()
        }
    }
}
