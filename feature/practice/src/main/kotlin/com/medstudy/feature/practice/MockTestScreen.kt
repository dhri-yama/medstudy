package com.medstudy.feature.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.medstudy.core.designsystem.IconCircleButton
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.PillStyle
import com.medstudy.core.designsystem.StudyChip
import com.medstudy.core.designsystem.StudyProgressBar
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.ContentBlock
import com.medstudy.core.model.Question
import kotlinx.coroutines.delay

/**
 * A timed mock test. While the clock runs: one question at a time, a question grid to jump around, nothing marked right or wrong.
 * After it is handed in: the score, then every question with the learner's answer, the right answer and the explanation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestScreen(
    state: MockTestUiState,
    onSelectOption: (String) -> Unit,
    onToggleFlag: () -> Unit,
    onGoTo: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    onTick: (Long) -> Unit,
    onExit: () -> Unit,
    imageResource: (String) -> Int?,
    modifier: Modifier = Modifier,
) {
    val colors = StudyTheme.colors
    val gradient = StudyTheme.subjectGradient(1)
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.endsAtMillis, state.isSubmitted) {
        while (!state.isSubmitted && state.questions.isNotEmpty()) {
            nowMillis = System.currentTimeMillis()
            onTick(nowMillis)
            delay(1_000)
        }
    }
    var showGrid by rememberSaveable { mutableStateOf(false) }
    var confirmSubmit by rememberSaveable { mutableStateOf(false) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    // After submitting, the results page shows first; picking a question opens the review.
    var reviewing by rememberSaveable(state.isSubmitted) { mutableStateOf(false) }
    var zoomedImage by remember { mutableStateOf<ContentBlock.Image?>(null) }

    BackHandler {
        when {
            !state.isSubmitted && state.questions.isNotEmpty() -> confirmLeave = true
            reviewing -> reviewing = false
            else -> onExit()
        }
    }

    if (state.isLoading) {
        Box(modifier.fillMaxSize().background(colors.canvas), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = colors.accent) }
        return
    }
    if (state.questions.isEmpty()) {
        Column(
            modifier.fillMaxSize().background(colors.canvas).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("This test is no longer open.", style = MaterialTheme.typography.titleLarge, color = colors.ink)
            Text("Start it again from the paper.", style = MaterialTheme.typography.bodyMedium, color = colors.body)
            PillButton("Back to Practice", onClick = onExit)
        }
        return
    }
    if (state.isSubmitted && !reviewing) {
        MockTestResults(state, onOpenQuestion = { onGoTo(it); reviewing = true }, onExit = onExit, modifier = modifier)
        return
    }

    val question = state.currentQuestion ?: return
    val picked = state.answers[question.id]
    val isLast = state.position == state.questions.lastIndex
    val remainingSeconds = ((state.endsAtMillis - nowMillis) / 1000).coerceAtLeast(0)

    Scaffold(
        modifier = modifier,
        containerColor = colors.canvas,
        topBar = {
            Column(
                Modifier.fillMaxWidth().background(gradient.brush).windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconCircleButton(
                        Icons.AutoMirrored.Outlined.ArrowBack, if (state.isSubmitted) "Back to results" else "Leave test",
                        { if (state.isSubmitted) reviewing = false else confirmLeave = true },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(state.title, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1)
                        Text(
                            "Question ${state.position + 1} of ${state.questions.size}" + if (state.isSubmitted) "" else " · ${state.answeredCount} answered",
                            style = MaterialTheme.typography.bodySmall, color = colors.ink,
                        )
                    }
                    if (!state.isSubmitted) {
                        val low = remainingSeconds < 5 * 60
                        Text(
                            formatClock(remainingSeconds),
                            style = MaterialTheme.typography.titleMedium, color = if (low) colors.error else colors.ink,
                            modifier = Modifier.clip(CircleShape).background(if (low) colors.errorTint else Color.White.copy(alpha = 0.7f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .semantics { contentDescription = "Time left ${formatClock(remainingSeconds)}" },
                        )
                    }
                    IconCircleButton(Icons.Outlined.GridView, "All questions", { showGrid = true })
                }
                StudyProgressBar(
                    progress = (state.position + 1).toFloat() / state.questions.size,
                    description = "Question ${state.position + 1} of ${state.questions.size}",
                    fill = gradient.deep, track = Color.White.copy(alpha = 0.55f),
                )
            }
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(colors.canvas).windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PillButton("Previous", onClick = onPrevious, style = PillStyle.Secondary, enabled = state.position > 0)
                when {
                    !isLast -> PillButton("Next", onClick = onNext, containerColor = gradient.deep, modifier = Modifier.weight(1f))
                    state.isSubmitted -> PillButton("Back to results", onClick = { reviewing = false }, containerColor = gradient.deep, modifier = Modifier.weight(1f))
                    else -> PillButton("Submit test", onClick = { confirmSubmit = true }, containerColor = gradient.deep, modifier = Modifier.weight(1f))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding() + 16.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(question.content, key = { "${question.id}:stem:${question.content.indexOf(it)}" }) { block ->
                QuestionContentBlock(block, imageResource, stem = true) { zoomedImage = it }
            }
            items(question.options, key = { "${question.id}:${it.id}" }) { option ->
                AnswerOption(
                    option = option,
                    selected = option.id == picked,
                    revealed = state.isSubmitted,
                    question = question,
                    imageResource = imageResource,
                    deep = gradient.deep,
                    onSelect = { onSelectOption(option.id) },
                    onImageClick = { zoomedImage = it },
                )
            }
            if (state.isSubmitted) {
                item(key = "review:${question.id}") { AnswerReview(question, state.resultFor(question) ?: AttemptResult.UNANSWERED) }
            } else {
                item(key = "flag:${question.id}") {
                    val flagged = question.id in state.flagged
                    Text(
                        if (flagged) "Marked for review · tap to unmark" else "Mark for review",
                        style = MaterialTheme.typography.labelLarge, color = if (flagged) colors.warning else colors.muted,
                        modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onToggleFlag).padding(vertical = 10.dp, horizontal = 4.dp),
                    )
                }
            }
        }
    }

    if (showGrid) {
        ModalBottomSheet(onDismissRequest = { showGrid = false }, containerColor = colors.canvas) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("All questions", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                if (!state.isSubmitted) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StudyChip("${state.answeredCount} answered")
                        StudyChip("${state.skippedCount} left")
                        if (state.flagged.isNotEmpty()) StudyChip("${state.flagged.size} marked")
                    }
                }
                // Above the grid, so a long paper's grid can never push it off the screen.
                if (!state.isSubmitted) {
                    PillButton("Submit test", onClick = { showGrid = false; confirmSubmit = true }, modifier = Modifier.fillMaxWidth())
                }
                QuestionGrid(state, onPick = { onGoTo(it); showGrid = false }, modifier = Modifier.heightIn(max = 360.dp))
            }
        }
    }
    if (confirmSubmit) {
        ConfirmDialog(
            title = "Submit the test?",
            body = if (state.skippedCount == 0) "You answered every question. You can't change answers after this."
            else "${state.skippedCount} of ${state.questions.size} questions are unanswered. You can't change answers after this.",
            confirm = "Submit", onConfirm = { confirmSubmit = false; onSubmit() }, onDismiss = { confirmSubmit = false },
        )
    }
    if (confirmLeave) {
        ConfirmDialog(
            title = "Leave the test?",
            body = "Your answers will not be saved and the test will end.",
            confirm = "Leave", onConfirm = { confirmLeave = false; onExit() }, onDismiss = { confirmLeave = false },
        )
    }
    zoomedImage?.let { image -> ImageZoomDialog(image = image, imageResource = imageResource, onDismiss = { zoomedImage = null }) }
}

@Composable
private fun MockTestResults(state: MockTestUiState, onOpenQuestion: (Int) -> Unit, onExit: () -> Unit, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    Column(
        modifier.fillMaxSize().background(colors.canvas).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(state.title, style = MaterialTheme.typography.titleSmall, color = colors.muted)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${state.score}", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 56.sp, lineHeight = 60.sp), color = colors.ink)
            Text("of ${state.maxScore} marks", style = MaterialTheme.typography.titleMedium, color = colors.muted, modifier = Modifier.padding(bottom = 10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryStat("Correct", state.correctCount, colors.successTint, colors.success, Modifier.weight(1f))
            SummaryStat("Wrong", state.wrongCount, colors.errorTint, colors.error, Modifier.weight(1f))
            SummaryStat("Skipped", state.skippedCount, colors.accentSoft, colors.ink, Modifier.weight(1f))
        }
        Text(
            "+${MockTestRules.MARKS_CORRECT} for a correct answer, ${MockTestRules.MARKS_WRONG} for a wrong one, 0 for a skipped one. " +
                "Time used: ${state.minutesUsed} of ${MockTestRules.durationMinutes(state.questions.size)} min.",
            style = MaterialTheme.typography.bodySmall, color = colors.muted,
        )
        Text("Tap a question to review it", style = MaterialTheme.typography.titleMedium, color = colors.ink)
        QuestionGrid(state, onPick = onOpenQuestion, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PillButton("Done", onClick = onExit, style = PillStyle.Secondary)
            PillButton("Review from Q1", onClick = { onOpenQuestion(0) }, modifier = Modifier.weight(1f))
        }
    }
}

/** One numbered cell per question. During the test it shows answered / marked; after it, right / wrong / skipped. */
@Composable
private fun QuestionGrid(state: MockTestUiState, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    LazyVerticalGrid(
        columns = GridCells.Adaptive(48.dp), modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(state.questions, key = { _, question -> question.id }) { index, question ->
            val result = state.resultFor(question)
            val answered = question.id in state.answers
            val flagged = question.id in state.flagged
            val (container, content, status) = when {
                state.isSubmitted && result == AttemptResult.CORRECT -> Triple(colors.successTint, colors.success, "correct")
                state.isSubmitted && result == AttemptResult.INCORRECT -> Triple(colors.errorTint, colors.error, "wrong")
                state.isSubmitted -> Triple(colors.surface, colors.muted, "skipped")
                answered -> Triple(colors.accent, colors.onAccent, "answered")
                else -> Triple(colors.surface, colors.ink, "not answered")
            }
            Box(
                Modifier.aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(container)
                    .then(if (!state.isSubmitted && flagged) Modifier.border(2.dp, colors.warning, RoundedCornerShape(14.dp)) else Modifier)
                    .then(if (index == state.position && !state.isSubmitted) Modifier.border(2.dp, colors.ink, RoundedCornerShape(14.dp)) else Modifier)
                    .clickable(role = Role.Button) { onPick(index) }
                    .semantics { contentDescription = "Question ${index + 1}, $status" + if (!state.isSubmitted && flagged) ", marked for review" else "" },
                contentAlignment = Alignment.Center,
            ) { Text("${index + 1}", style = MaterialTheme.typography.titleSmall, color = content, textAlign = TextAlign.Center) }
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(StudyTheme.colors.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.body)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                PillButton("Cancel", onClick = onDismiss, style = PillStyle.Secondary)
                PillButton(confirm, onClick = onConfirm, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun formatClock(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}
