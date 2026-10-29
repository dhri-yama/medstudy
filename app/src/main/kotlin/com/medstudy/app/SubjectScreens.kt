package com.medstudy.app

import com.medstudy.feature.progress.SubjectSummary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.medstudy.core.designsystem.IconCircleButton
import com.medstudy.core.designsystem.HeroCard
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.SegmentedTabs
import com.medstudy.core.designsystem.StudyChip
import com.medstudy.core.designsystem.SubjectGradient
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import com.medstudy.core.designsystem.StudyProgressBar
import com.medstudy.core.designsystem.StudyTextField
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.designsystem.TaskRow
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.Category
import com.medstudy.core.model.ContentBlock
import com.medstudy.core.model.Question
import com.medstudy.core.model.QuestionAttempt
import com.medstudy.core.model.pluralize

data class TopicSummary(
    val id: String,
    val name: String,
    val questionCount: Int,
    val answered: Int,
    val accuracyPercent: Int?,
) {
    /** Enough answers and mostly right. */
    val mastered: Boolean get() = answered >= minOf(5, questionCount) && (accuracyPercent ?: 0) >= 80
}

fun topicSummaries(
    subjectId: String,
    categories: List<Category>,
    questions: List<Question>,
    attempts: List<QuestionAttempt>,
): List<TopicSummary> {
    val attemptsByQuestion = attempts.groupBy(QuestionAttempt::questionId)
    return categories.filter { it.parentId == subjectId }.sortedBy(Category::name).mapNotNull { topic ->
        val topicQuestions = questions.filter { topic.id in it.topicIds }
        if (topicQuestions.isEmpty()) return@mapNotNull null
        val topicAttempts = topicQuestions.flatMap { attemptsByQuestion[it.id].orEmpty() }
        val graded = topicAttempts.filter { it.result != AttemptResult.UNANSWERED }
        TopicSummary(
            id = topic.id,
            name = topic.name,
            questionCount = topicQuestions.size,
            answered = topicQuestions.count { it.id in attemptsByQuestion },
            accuracyPercent = graded.takeIf { it.isNotEmpty() }
                ?.let { list -> list.count { it.result == AttemptResult.CORRECT } * 100 / list.size },
        )
    }
}

/** Title row with a back button, used by sub-screens that live under a tab. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconCircleButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
        Column {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = StudyTheme.colors.ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted)
        }
    }
}

@Composable
fun SubjectDetailScreen(
    subject: SubjectSummary,
    topics: List<TopicSummary>,
    mistakes: List<Question>,
    onBack: () -> Unit,
    onPracticeAll: () -> Unit,
    onPracticeTopic: (String) -> Unit,
    onPracticeMistakes: () -> Unit,
    onOpenQuestion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues20(LocalNavBarInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val gradient = StudyTheme.subjectGradient(subject.colorIndex)
            HeroCard(gradient, onBack, subject.name) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudyChip(subject.accuracyPercent?.let { "$it% correct" } ?: "Not started")
                    StudyChip("${subject.answered} of ${subject.questionCount} answered")
                }
                StudyProgressBar(
                    progress = if (subject.questionCount == 0) 0f else subject.answered.toFloat() / subject.questionCount,
                    description = "${subject.answered} of ${subject.questionCount} questions answered",
                    fill = gradient.deep,
                    track = Color.White.copy(alpha = 0.55f),
                )
                PillButton("Practice all ${subject.questionCount}", onClick = onPracticeAll, containerColor = gradient.deep, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            SegmentedTabs(listOf("Topics", "Mistakes"), tab, { tab = it })
        }
        if (tab == 0) {
            if (topics.isEmpty()) {
                item { Text("This subject has no topics. Use Practice all.", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.muted) }
            }
            items(topics, key = TopicSummary::id) { topic ->
                TopicCard(topic, gradient = StudyTheme.subjectGradient(subject.colorIndex), onPractice = { onPracticeTopic(topic.id) })
            }
        } else {
            if (mistakes.isEmpty()) {
                item { Text("No missed questions in ${subject.name}.", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.muted) }
            } else {
                item { PillButton("Practice ${pluralize(mistakes.size, "mistake")}", onClick = onPracticeMistakes, modifier = Modifier.fillMaxWidth()) }
                items(mistakes, key = Question::id) { question ->
                    TaskRow(
                        title = question.content.filterIsInstance<ContentBlock.Text>().firstOrNull()?.text.orEmpty(),
                        caption = "Missed · tap to retry",
                        onClick = { onOpenQuestion(question.id) },
                        leading = { Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = StudyTheme.colors.error) },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** A topic with its progress and a Practice button. Plain, no expand arrows; a check marks a mastered topic. */
@Composable
private fun TopicCard(topic: TopicSummary, gradient: SubjectGradient, onPractice: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StudyTheme.colors.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(topic.name, style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink, modifier = Modifier.weight(1f, fill = false))
                if (topic.mastered) StudyChip("Mastered", container = StudyTheme.colors.successTint, contentColor = StudyTheme.colors.success)
            }
            StudyProgressBar(
                progress = if (topic.questionCount == 0) 0f else topic.answered.toFloat() / topic.questionCount,
                description = "${topic.answered} of ${topic.questionCount} answered",
                fill = gradient.deep,
                track = gradient.bottom,
            )
            Text(
                "${pluralize(topic.questionCount, "question")} · " + (topic.accuracyPercent?.let { "$it% correct" } ?: "not started"),
                style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted,
            )
        }
        PillButton("Practice", onClick = onPractice, small = true, containerColor = gradient.deep)
    }
}
