package com.medstudy.feature.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.shadow
import com.medstudy.core.designsystem.NotchedCard
import com.medstudy.core.model.pluralize
import com.medstudy.core.model.splitSourceTag
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import com.medstudy.core.designsystem.IconCircleButton
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.PillStyle
import com.medstudy.core.designsystem.StudyChip
import com.medstudy.core.designsystem.StudyProgressBar
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.medstudy.core.designsystem.StudyMotion
import com.medstudy.core.designsystem.rememberReducedMotion
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.ContentBlock
import com.medstudy.core.model.ContentKind
import com.medstudy.core.model.readableExplanation
import com.medstudy.core.model.subjectDisplayName
import com.medstudy.core.model.Question
import com.medstudy.core.model.QuestionOption

private val ReportReasons = listOf("Wrong answer", "Typo or missing text", "Unclear question", "Other")

@Composable
fun QuestionPracticeScreen(
    state: PracticeUiState,
    onBack: () -> Unit,
    onSelectOption: (String) -> Unit,
    onSubmit: () -> Unit,
    onShowAnswer: () -> Unit,
    onToggleBookmark: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    imageResource: (String) -> Int?,
    modifier: Modifier = Modifier,
    /** Index into the subject palette for a subject id, so the screen uses the same color as the rest of the app. */
    subjectColorIndex: (String) -> Int = { 0 },
    onReport: (questionId: String, reason: String) -> Unit = { _, _ -> },
    onReviewMissed: () -> Unit = {},
    onExit: () -> Unit = onBack,
) {
    val question = state.currentQuestion
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = StudyTheme.colors.ink) }
        return
    }
    if (question == null) {
        Column(
            modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                state.error ?: "No questions are available right now.",
                style = MaterialTheme.typography.titleLarge,
                color = if (state.error != null) StudyTheme.colors.error else StudyTheme.colors.muted,
            )
            PillButton("Go back", onClick = onBack)
        }
        return
    }
    if (state.isFinished) {
        // One subject in the session: use its color; a mix of subjects gets the neutral first palette color.
        val summaryColor = state.questions.map { it.subjectId }.distinct().singleOrNull()?.let(subjectColorIndex) ?: 0
        SessionSummary(state, summaryColor, onReviewMissed, onExit, modifier)
        return
    }

    var zoomedImage by remember(question.id) { mutableStateOf<ContentBlock.Image?>(null) }
    var reporting by remember(question.id) { mutableStateOf(false) }
    var reported by remember(question.id) { mutableStateOf(false) }
    val gradient = StudyTheme.subjectGradient(subjectColorIndex(question.subjectId))
    val isLast = state.position == state.questions.lastIndex
    val stemText = question.content.filterIsInstance<ContentBlock.Text>().firstOrNull()?.text.orEmpty()
    val (cleanStem, sourceTag) = remember(question.id) { splitSourceTag(stemText) }

    // Motion only plays for something the user just did: a reveal that happens while this question is on screen
    // (not when paging back to an answered one) and a bookmark toggle (not the state a question arrives with).
    val reducedMotion = rememberReducedMotion()
    val haptics = LocalHapticFeedback.current
    val revealedOnArrival = remember(question.id) { state.isRevealed }
    LaunchedEffect(question.id, state.isRevealed) {
        if (state.isRevealed && !revealedOnArrival) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val bookmarkScale = remember { Animatable(1f) }
    var bookmarkSeen by remember(question.id) { mutableStateOf(state.isBookmarked) }
    LaunchedEffect(question.id, state.isBookmarked) {
        if (state.isBookmarked != bookmarkSeen) {
            bookmarkSeen = state.isBookmarked
            if (!reducedMotion) {
                bookmarkScale.snapTo(0.8f)
                bookmarkScale.animateTo(1f, StudyMotion.popSpring)
            }
        }
    }
    val bookmarkTint by animateColorAsState(
        if (state.isBookmarked) gradient.deep else StudyTheme.colors.ink, StudyMotion.standard(), label = "bookmarkTint",
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudyTheme.colors.canvas,
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(gradient.brush)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconCircleButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack, size = 60.dp, iconSize = 32.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(subjectDisplayName(question.subjectId), style = MaterialTheme.typography.titleSmall, color = StudyTheme.colors.ink)
                        StudyProgressBar(
                            progress = (state.position + 1).toFloat() / state.questions.size,
                            description = "Question ${state.position + 1} of ${state.questions.size}",
                            fill = gradient.deep,
                            track = Color.White.copy(alpha = 0.55f),
                        )
                        Text(
                            "Question ${state.position + 1} of ${state.questions.size}" +
                                if (state.results.isNotEmpty()) " · ${state.correctCount} correct" else "",
                            style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.ink,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (sourceTag != null) StudyChip(sourceTag, container = Color.White.copy(alpha = 0.7f))
                question.content.forEach { block ->
                    when (block) {
                        is ContentBlock.Text -> Text(
                            if (block.text == stemText) cleanStem else block.text,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 28.sp),
                            color = StudyTheme.colors.ink,
                        )
                        is ContentBlock.Image -> QuestionImage(block, imageResource) { zoomedImage = block }
                    }
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudyTheme.colors.canvas)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.isRevealed) {
                    PillButton("Previous", onClick = onPrevious, style = PillStyle.Secondary, enabled = state.position > 0)
                    PillButton(
                        if (isLast) "See results" else "Next",
                        onClick = if (isLast) onFinish else onNext,
                        containerColor = gradient.deep,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    PillButton("Show answer", onClick = onShowAnswer, style = PillStyle.Secondary)
                    PillButton(
                        "Check answer", onClick = onSubmit,
                        enabled = state.selectedOptionIds.isNotEmpty(),
                        containerColor = gradient.deep, modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    ) { contentPadding ->
        Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                end = 20.dp,
                // extra room so the floating bookmark button never covers the last row
                bottom = contentPadding.calculateBottomPadding() + 84.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (question.provenance.contentKind == ContentKind.DEMO) item {
                Text("DEMO CONTENT ONLY", style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.muted, fontWeight = FontWeight.SemiBold)
            }
            items(question.options, key = { "${question.id}:${it.id}" }) { option ->
                AnswerOption(
                    option = option,
                    selected = option.id in state.selectedOptionIds,
                    revealed = state.isRevealed,
                    question = question,
                    imageResource = imageResource,
                    deep = gradient.deep,
                    onSelect = { onSelectOption(option.id) },
                    onImageClick = { zoomedImage = it },
                )
            }
            if (state.error != null) {
                item { Text(state.error, color = StudyTheme.colors.error, style = MaterialTheme.typography.bodySmall) }
            }
            if (state.isRevealed) {
                item(key = "review:${question.id}") {
                    // Saved, so scrolling the review off screen and back does not replay the entrance.
                    var shown by rememberSaveable(question.id) { mutableStateOf(revealedOnArrival || reducedMotion) }
                    LaunchedEffect(Unit) { shown = true }
                    AnimatedVisibility(
                        visible = shown,
                        enter = fadeIn(StudyMotion.standard()) + expandVertically(StudyMotion.standard()),
                    ) { AnswerReview(question, state.result) }
                }
                item {
                    Text(
                        if (reported) "Thanks, we'll take a look." else "Something wrong with this question? Report it",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudyTheme.colors.muted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !reported, role = androidx.compose.ui.semantics.Role.Button) { reporting = true }
                            .padding(vertical = 6.dp),
                    )
                }
            }
        }
        // Floating bookmark: stays put while the content scrolls, above the answer buttons at the bottom right.
        IconCircleButton(
            icon = if (state.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (state.isBookmarked) "Remove bookmark" else "Bookmark question",
            onClick = onToggleBookmark,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp)
                .shadow(8.dp, CircleShape),
            // The circle stays white; only the icon fills in (in the subject's deep shade) once saved.
            tint = bookmarkTint,
            size = 60.dp,
            iconSize = 32.dp,
            iconModifier = Modifier.graphicsLayer { scaleX = bookmarkScale.value; scaleY = bookmarkScale.value },
        )
        }
    }

    if (reporting) {
        ReportDialog(
            onDismiss = { reporting = false },
            onSubmit = { reason -> onReport(question.id, reason); reported = true; reporting = false },
        )
    }
    zoomedImage?.let { image ->
        ImageZoomDialog(image = image, imageResource = imageResource, onDismiss = { zoomedImage = null })
    }
}

@Composable
private fun ReportDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var reason by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(StudyTheme.colors.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Report this question", style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink)
            Text("What's wrong with it?", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.muted)
            ReportReasons.forEach { option ->
                val selected = option == reason
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) StudyTheme.colors.accentSoft else StudyTheme.colors.canvas)
                        .selectable(selected = selected, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = { reason = option })
                        .padding(14.dp),
                ) { Text(option, style = MaterialTheme.typography.titleSmall, color = StudyTheme.colors.ink) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                PillButton("Cancel", onClick = onDismiss, style = PillStyle.Secondary)
                PillButton("Send report", onClick = { reason?.let(onSubmit) }, enabled = reason != null, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** End-of-session results: a big score, a breakdown, and actions for what to do next. */
@Composable
private fun SessionSummary(
    state: PracticeUiState,
    colorIndex: Int,
    onReviewMissed: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient = StudyTheme.subjectGradient(colorIndex)
    val total = state.questions.size
    val graded = state.correctCount + state.missedCount
    val percent = if (graded == 0) null else state.correctCount * 100 / graded
    val headline = when {
        percent == null -> "Session complete"
        percent >= 80 -> "Great work!"
        percent >= 50 -> "Nice effort"
        else -> "Keep going"
    }
    // The score counts up once, when the summary first appears (not again after a rotation).
    val reducedMotion = rememberReducedMotion()
    var counted by rememberSaveable { mutableStateOf(reducedMotion) }
    val progress = remember { Animatable(if (counted) 1f else 0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(400, easing = StudyMotion.EaseOut))
        counted = true
    }
    Column(
        modifier = modifier.fillMaxSize().background(StudyTheme.colors.canvas).windowInsetsPadding(WindowInsets.systemBars).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NotchedCard(
            gradient = gradient,
            modifier = Modifier.fillMaxWidth(),
            minHeight = 240.dp,
            verticalArrangement = Arrangement.SpaceBetween,
            tab = { Text("$total", style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink) },
        ) {
            Column {
                Text(headline, style = MaterialTheme.typography.headlineSmall, color = StudyTheme.colors.ink, modifier = Modifier.fillMaxWidth(0.6f))
                Text(
                    "You answered ${pluralize(state.results.size, "question")}",
                    style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink,
                )
            }
            Column {
                Text(
                    percent?.let { "${(it * progress.value).toInt()}%" } ?: "-",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 64.sp, lineHeight = 68.sp),
                    color = StudyTheme.colors.ink,
                )
                Text("correct", style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = progress.value },
        ) {
            SummaryStat("Correct", state.correctCount, StudyTheme.colors.successTint, StudyTheme.colors.success, Modifier.weight(1f))
            SummaryStat("Missed", state.missedCount, StudyTheme.colors.errorTint, StudyTheme.colors.error, Modifier.weight(1f))
            SummaryStat("Shown", state.shownCount, StudyTheme.colors.surface, StudyTheme.colors.ink, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        if (state.missedCount > 0) {
            PillButton(
                "Retry ${pluralize(state.missedCount, "mistake")}", onClick = onReviewMissed,
                containerColor = gradient.deep, modifier = Modifier.fillMaxWidth(),
            )
        }
        PillButton("Done", onClick = onExit, style = PillStyle.Secondary, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SummaryStat(label: String, count: Int, container: Color, content: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(container).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$count", style = MaterialTheme.typography.headlineSmall, color = content)
        Text(label, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

@Composable
private fun QuestionContentBlock(
    block: ContentBlock,
    imageResource: (String) -> Int?,
    stem: Boolean = false,
    onImageClick: (ContentBlock.Image) -> Unit,
) {
    when (block) {
        is ContentBlock.Text -> Text(
            block.text,
            style = if (stem) MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 27.sp) else MaterialTheme.typography.bodyMedium,
            color = StudyTheme.colors.ink,
        )
        is ContentBlock.Image -> QuestionImage(block, imageResource, onImageClick)
    }
}

@Composable
private fun AnswerOption(
    option: QuestionOption,
    selected: Boolean,
    revealed: Boolean,
    question: Question,
    imageResource: (String) -> Int?,
    deep: Color,
    onSelect: () -> Unit,
    onImageClick: (ContentBlock.Image) -> Unit,
) {
    val colors = StudyTheme.colors
    val correct = option.id in question.answerKey.correctOptionIds
    val wrongPick = revealed && selected && !correct
    val background = when {
        revealed && correct -> colors.successTint
        wrongPick -> colors.errorTint
        selected -> colors.accentSoft
        else -> colors.surface
    }
    val border = when {
        revealed && correct -> colors.success
        wrongPick -> colors.error
        selected -> deep
        else -> colors.hairline
    }
    val badgeFill = when {
        revealed && correct -> colors.success
        wrongPick -> colors.error
        selected -> deep
        else -> colors.canvas
    }
    val badgeText = if (badgeFill == colors.canvas) colors.ink else colors.onAccent
    // Selecting is quick; the correct / wrong reveal is a touch slower so the change of state reads.
    val spec = if (revealed) StudyMotion.standard<Color>() else StudyMotion.fast()
    val backgroundShown by animateColorAsState(background, spec, label = "optionFill")
    val borderShown by animateColorAsState(border, spec, label = "optionBorder")
    val badgeShown by animateColorAsState(badgeFill, spec, label = "optionBadge")
    val badgeTextShown by animateColorAsState(badgeText, spec, label = "optionBadgeText")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundShown,
        border = BorderStroke(if (selected || revealed && correct) 1.5.dp else 1.dp, borderShown),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .selectable(selected = selected, enabled = !revealed, role = Role.RadioButton, onClick = onSelect)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(badgeShown), contentAlignment = Alignment.Center) {
                Text(option.label, style = MaterialTheme.typography.labelLarge, color = badgeTextShown)
            }
            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                option.content.forEach { block -> QuestionContentBlock(block, imageResource, onImageClick = onImageClick) }
            }
            AnimatedVisibility(revealed && correct, enter = fadeIn(StudyMotion.standard())) {
                Text("Correct", style = MaterialTheme.typography.labelMedium, color = colors.success)
            }
            AnimatedVisibility(wrongPick, enter = fadeIn(StudyMotion.standard())) {
                Text("Your answer", style = MaterialTheme.typography.labelMedium, color = colors.error)
            }
        }
    }
}

@Composable
private fun AnswerReview(question: Question, result: AttemptResult?) {
    val colors = StudyTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (result) {
            AttemptResult.CORRECT -> StudyChip("Correct", container = colors.successTint, contentColor = colors.success)
            AttemptResult.INCORRECT -> StudyChip("Not quite", container = colors.errorTint, contentColor = colors.error)
            AttemptResult.UNANSWERED, null -> StudyChip("Answer shown", container = colors.canvas, contentColor = colors.ink)
        }
        val explanation = readableExplanation(question.answerKey.explanation)
        if (explanation.isNotBlank()) {
            Text("Explanation", style = MaterialTheme.typography.titleLarge, color = colors.ink)
            Text(explanation, style = MaterialTheme.typography.bodyMedium, color = colors.body)
        } else {
            // Previous-year papers carry an answer key but no explanations.
            Text("Correct answer", style = MaterialTheme.typography.titleLarge, color = colors.ink)
            question.options.filter { it.id in question.answerKey.correctOptionIds }.forEach { option ->
                val text = option.content.filterIsInstance<ContentBlock.Text>().joinToString(" ") { it.text }
                Text("${option.label}. $text", style = MaterialTheme.typography.bodyMedium, color = colors.body)
            }
        }
        // Per-option rationales are empty for MedMCQA; skip the repeated "Incorrect option" rows then.
        if (question.answerKey.optionRationales.any { it.text.isNotBlank() }) {
            question.options.sortedBy(QuestionOption::position).forEach { option ->
                val isCorrect = option.id in question.answerKey.correctOptionIds
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${option.label}  ${if (isCorrect) "Correct answer" else "Incorrect option"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isCorrect) colors.success else colors.muted,
                    )
                    question.answerKey.optionRationales.firstOrNull { it.optionId == option.id }?.text
                        ?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.body) }
                }
            }
        }
    }
}

@Composable
private fun QuestionImage(
    image: ContentBlock.Image,
    imageResource: (String) -> Int?,
    onClick: (ContentBlock.Image) -> Unit,
) {
    val painter = rememberQuestionPainter(image.assetId, imageResource)
    if (painter == null) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
            Text("Image unavailable", modifier = Modifier.fillMaxWidth().padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Image(
            painter = painter,
            contentDescription = image.altText,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClick(image) },
        )
        Text(image.caption ?: "Tap to enlarge image", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ImageZoomDialog(
    image: ContentBlock.Image,
    imageResource: (String) -> Int?,
    onDismiss: () -> Unit,
) {
    val painter = rememberQuestionPainter(image.assetId, imageResource) ?: return
    var scale by remember(image.assetId) { mutableFloatStateOf(1f) }
    var offsetX by remember(image.assetId) { mutableFloatStateOf(0f) }
    var offsetY by remember(image.assetId) { mutableFloatStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Box(Modifier.fillMaxSize()) {
                Image(
                    painter = painter,
                    contentDescription = image.altText,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                        .transformable(transformState),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close image")
                }
                Text("Pinch to zoom", modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Decoded paper images, kept small: a question screen shows one or two at a time. */
private val assetBitmapCache = object : android.util.LruCache<String, android.graphics.Bitmap>(24 * 1024 * 1024) {
    override fun sizeOf(key: String, value: android.graphics.Bitmap) = value.byteCount
}

/** A drawable-backed image (demo catalog) or, for ids that are asset paths ("pyq/images/..."), a bitmap decoded from the APK assets. */
@Composable
private fun rememberQuestionPainter(assetId: String, imageResource: (String) -> Int?): androidx.compose.ui.graphics.painter.Painter? {
    imageResource(assetId)?.let { return painterResource(it) }
    if (!assetId.startsWith("pyq/")) return null
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(assetBitmapCache.get(assetId), assetId) {
        if (value == null) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    context.assets.open(assetId).use { android.graphics.BitmapFactory.decodeStream(it) }
                }.getOrNull()?.also { assetBitmapCache.put(assetId, it) }
            }
        }
    }
    val loaded = bitmap ?: return null
    return androidx.compose.runtime.remember(loaded) {
        androidx.compose.ui.graphics.painter.BitmapPainter(loaded.asImageBitmap())
    }
}
