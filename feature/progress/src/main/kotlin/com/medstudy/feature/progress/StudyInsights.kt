package com.medstudy.feature.progress

import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.Category
import com.medstudy.core.model.Question
import com.medstudy.core.model.QuestionAttempt
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class StudyInsights(
    private val minimumAnsweredAttempts: Int = 5,
    private val reviewAccuracyThreshold: Double = 0.7,
    private val windowDays: Long = 30,
) {
    init {
        require(minimumAnsweredAttempts > 0)
        require(reviewAccuracyThreshold in 0.0..1.0)
        require(windowDays > 0)
    }

    fun analyze(
        today: LocalDate,
        attempts: List<QuestionAttempt>,
        questions: List<Question>,
        categories: List<Category>,
    ): List<WeakArea> {
        val questionsById = questions.associateBy(Question::id)
        val recentAnswered = attempts.filter { attempt ->
            val daysOld = runCatching { ChronoUnit.DAYS.between(LocalDate.parse(attempt.studyDateIso), today) }
                .getOrDefault(Long.MAX_VALUE)
            attempt.result != AttemptResult.UNANSWERED && daysOld in 0 until windowDays
        }
        val attemptsByTopic = mutableMapOf<String, MutableList<QuestionAttempt>>()
        val attemptsBySubject = mutableMapOf<String, MutableList<QuestionAttempt>>()
        recentAnswered.forEach { attempt ->
            val question = questionsById[attempt.questionId] ?: return@forEach
            attemptsBySubject.getOrPut(question.subjectId, ::mutableListOf).add(attempt)
            question.topicIds.forEach { topicId ->
                attemptsByTopic.getOrPut(topicId, ::mutableListOf).add(attempt)
            }
        }
        // Memory decays over the whole history, not just the accuracy window.
        val reviewsByArea = mutableMapOf<String, MutableList<Review>>()
        attempts.filter { it.result != AttemptResult.UNANSWERED }.forEach { attempt ->
            val question = questionsById[attempt.questionId] ?: return@forEach
            val review = Review(attempt.completedAtEpochMillis, attempt.result == AttemptResult.CORRECT)
            (question.topicIds + question.subjectId).forEach { id ->
                reviewsByArea.getOrPut(id, ::mutableListOf).add(review)
            }
        }
        val nowEpochMillis = today.toEpochDay() * MS_PER_DAY

        val subjects = categories.filter { it.parentId == null }.sortedBy(Category::position)
        val topicsBySubject = categories.filter { it.parentId != null }.groupBy(Category::parentId)
        val areas = mutableListOf<WeakArea>()

        subjects.forEach { subject ->
            val topics = topicsBySubject[subject.id].orEmpty().sortedBy(Category::position)
            val topicResults = topics.map { topic ->
                topic to attemptsByTopic[topic.id].orEmpty()
            }
            val hasTopicEvidence = topicResults.any { (_, topicAttempts) ->
                topicAttempts.size >= minimumAnsweredAttempts
            }
            if (hasTopicEvidence) {
                topicResults.forEach { (topic, topicAttempts) ->
                    areas += topic.toWeakArea(subject, topicAttempts, reviewsByArea, nowEpochMillis)
                }
            } else {
                val subjectAttempts = attemptsBySubject[subject.id].orEmpty()
                if (subjectAttempts.size >= minimumAnsweredAttempts) {
                    areas += subject.toWeakArea(subject, subjectAttempts, reviewsByArea, nowEpochMillis)
                } else {
                    topicResults.forEach { (topic, topicAttempts) ->
                        areas += topic.toWeakArea(subject, topicAttempts, reviewsByArea, nowEpochMillis)
                    }
                }
            }
        }
        return areas.sortedWith(compareBy<WeakArea>({ it.status.ordinal }, { it.accuracy ?: 1.0 }, { -it.answeredAttempts }, { it.title }))
    }

    private fun Category.toWeakArea(
        subject: Category,
        attempts: List<QuestionAttempt>,
        reviewsByArea: Map<String, List<Review>>,
        nowEpochMillis: Long,
    ): WeakArea {
        val reviews = reviewsByArea[id].orEmpty()
        val stability = MemoryModel.stability(reviews)
        val retrievability = stability?.let {
            val elapsedDays = (nowEpochMillis - reviews.maxOf(Review::atEpochMillis)) / MS_PER_DAY.toDouble()
            MemoryModel.retrievability(elapsedDays, it)
        }
        val sampleCount = attempts.size
        val accurateCount = attempts.count { it.result == AttemptResult.CORRECT }
        val accuracy = if (sampleCount == 0) null else accurateCount.toDouble() / sampleCount
        val status = when {
            sampleCount < minimumAnsweredAttempts -> WeakAreaStatus.UNASSESSED
            accuracy!! < reviewAccuracyThreshold -> WeakAreaStatus.NEEDS_REVIEW
            else -> WeakAreaStatus.ON_TRACK
        }
        val isSubjectLevel = parentId == null
        return WeakArea(
            id = id,
            subjectId = subject.id,
            topicId = if (isSubjectLevel) null else id,
            title = name,
            answeredAttempts = sampleCount,
            accuracy = accuracy,
            status = status,
            retrievability = retrievability,
        )
    }

    private companion object {
        const val MS_PER_DAY = 86_400_000L
    }
}

enum class WeakAreaStatus {
    NEEDS_REVIEW,
    UNASSESSED,
    ON_TRACK,
}

data class WeakArea(
    val id: String,
    val subjectId: String,
    val topicId: String?,
    val title: String,
    val answeredAttempts: Int,
    val accuracy: Double?,
    val status: WeakAreaStatus,
    /** Estimated chance the topic is still remembered today (1.0 just reviewed); null if never answered. */
    val retrievability: Double? = null,
)
