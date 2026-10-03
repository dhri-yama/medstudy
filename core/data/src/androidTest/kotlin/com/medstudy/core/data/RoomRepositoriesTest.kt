package com.medstudy.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medstudy.core.database.StudyDatabase
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.PracticeContext
import com.medstudy.core.model.QuestionAttempt
import com.medstudy.core.model.StudyBlock
import com.medstudy.core.model.StudyPlanKind
import com.medstudy.core.model.StudySchedulePreferences
import com.medstudy.core.model.WeeklyStudyPlan
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRepositoriesTest {
    private lateinit var database: StudyDatabase
    private lateinit var catalogRepository: RoomQuestionCatalogRepository
    private lateinit var activityRepository: RoomLearnerActivityRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, StudyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        catalogRepository = RoomQuestionCatalogRepository(database)
        activityRepository = RoomLearnerActivityRepository(database.activityDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun catalogRoundTripPreservesQuestionMediaAnswersAndFilters() = runBlocking {
        catalogRepository.saveCatalog(
            DemoQuestionCatalog.categories,
            DemoQuestionCatalog.mediaAssets,
            DemoQuestionCatalog.questions,
        )

        val imageQuestion = catalogRepository.question("demo.question.anatomy-image")
        assertTrue(imageQuestion?.content?.any { it is com.medstudy.core.model.ContentBlock.Image } == true)
        assertEquals(listOf("b"), imageQuestion?.answerKey?.correctOptionIds)
        assertEquals(4, imageQuestion?.answerKey?.optionRationales?.size)

        val anatomyQuestions = catalogRepository.questions(
            QuestionFilter(subjectIds = setOf("subject.anatomy")),
        )
        assertEquals(listOf("demo.question.anatomy-image"), anatomyQuestions.map { it.id })
        assertEquals(6, catalogRepository.categories().count { it.parentId == null })
    }

    @Test
    fun bookmarksAndAttemptsSurviveRepositoryRoundTrip() = runBlocking {
        catalogRepository.saveCatalog(
            DemoQuestionCatalog.categories,
            DemoQuestionCatalog.mediaAssets,
            DemoQuestionCatalog.questions,
        )
        activityRepository.bookmark("demo.question.anatomy-image", savedAtEpochMillis = 100L)
        activityRepository.recordAttempt(
            QuestionAttempt(
                id = "attempt.demo.001",
                questionId = "demo.question.anatomy-image",
                selectedOptionIds = listOf("b"),
                result = AttemptResult.CORRECT,
                completedAtEpochMillis = 200L,
                studyDateIso = "2026-10-03",
                timeZoneId = "Asia/Kolkata",
                context = PracticeContext.TOPIC_PRACTICE,
            ),
        )

        assertTrue(activityRepository.isBookmarked("demo.question.anatomy-image"))
        assertEquals(1, catalogRepository.bookmarkedQuestions().size)
        assertEquals("attempt.demo.001", activityRepository.attempts().single().id)
        assertEquals(1, activityRepository.attemptsForDate("2026-10-03").size)

        activityRepository.removeBookmark("demo.question.anatomy-image")
        assertFalse(activityRepository.isBookmarked("demo.question.anatomy-image"))
    }

    @Test
    fun editedWeeklyPlanSurvivesRepositoryRoundTrip() = runBlocking {
        val repository = RoomWeeklyStudyPlanRepository(database)
        val plan = WeeklyStudyPlan(
            id = "weekly",
            weekStartDateIso = "2026-10-03",
            generatedAtEpochMillis = 300L,
            kind = StudyPlanKind.EDITED,
            blocks = listOf(
                StudyBlock(
                    id = "block.demo.001",
                    studyDateIso = "2026-10-04",
                    subjectId = "subject.anatomy",
                    topicId = "topic.anatomy.imaging",
                    title = "Image display",
                    questionTarget = 12,
                    minuteTarget = 40,
                    position = 0,
                ),
            ),
        )

        repository.save(plan)

        assertEquals(plan, repository.get())
    }

    @Test
    fun studyPreferencesSurviveRepositoryRoundTrip() = runBlocking {
        val repository = RoomWeeklyStudyPlanRepository(database)
        val preferences = StudySchedulePreferences(
            availableWeekdays = setOf(1, 3, 5),
            questionTargetPerDay = 12,
            minuteTargetPerDay = 45,
        )

        repository.savePreferences(preferences)

        assertEquals(preferences, repository.preferences())
    }
}
