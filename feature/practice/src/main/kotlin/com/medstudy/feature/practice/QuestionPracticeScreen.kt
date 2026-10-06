package com.medstudy.feature.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Bookmark
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
import androidx.compose.runtime.Composable
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
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.ContentBlock
import com.medstudy.core.model.Question
import com.medstudy.core.model.QuestionOption

@OptIn(ExperimentalMaterial3Api::class)
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
) {
    val question = state.currentQuestion
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (question == null) {
        Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text(
                state.error ?: "No questions are available right now.",
                style = MaterialTheme.typography.titleLarge,
                color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onBack) { Text("Back to subjects") }
        }
        return
    }

    var zoomedImage by remember(question.id) { mutableStateOf<ContentBlock.Image?>(null) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to subjects")
                    }
                },
                title = {
                    Column {
                        Text(question.subjectId.substringAfterLast('.').replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.titleMedium)
                        Text("DEMO QUESTION  ·  ${state.position + 1} OF ${state.questions.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            if (state.isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (state.isBookmarked) "Remove bookmark" else "Bookmark question",
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.isRevealed) {
                        OutlinedButton(onClick = onPrevious, enabled = state.position > 0) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Previous")
                        }
                        Button(
                            onClick = if (state.position == state.questions.lastIndex) onFinish else onNext,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (state.position == state.questions.lastIndex) "Finish" else "Next")
                            if (state.position < state.questions.lastIndex) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                            }
                        }
                    } else {
                        TextButton(onClick = onShowAnswer) { Text("Show answer") }
                        Button(
                            onClick = onSubmit,
                            enabled = state.selectedOptionIds.isNotEmpty(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Check answer")
                        }
                    }
                }
            }
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("DEMO CONTENT ONLY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                Text("This fictional sample is for testing the app experience. It is not an actual exam question or medical advice.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            items(question.content) { block ->
                QuestionContentBlock(block, imageResource) { zoomedImage = block as? ContentBlock.Image }
            }
            items(question.options, key = QuestionOption::id) { option ->
                AnswerOption(
                    option = option,
                    selected = option.id in state.selectedOptionIds,
                    revealed = state.isRevealed,
                    question = question,
                    imageResource = imageResource,
                    onSelect = { onSelectOption(option.id) },
                    onImageClick = { zoomedImage = it },
                )
            }
            if (state.error != null) {
                item { Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            if (state.isRevealed) {
                item { AnswerReview(question, state.result) }
            }
        }
    }

    zoomedImage?.let { image ->
        ImageZoomDialog(image = image, imageResource = imageResource, onDismiss = { zoomedImage = null })
    }
}

@Composable
private fun QuestionContentBlock(
    block: ContentBlock,
    imageResource: (String) -> Int?,
    onImageClick: (ContentBlock.Image) -> Unit,
) {
    when (block) {
        is ContentBlock.Text -> Text(block.text, style = MaterialTheme.typography.bodyLarge)
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
    onSelect: () -> Unit,
    onImageClick: (ContentBlock.Image) -> Unit,
) {
    val correct = option.id in question.answerKey.correctOptionIds
    val background = when {
        revealed && correct -> MaterialTheme.colorScheme.secondaryContainer
        revealed && selected && !correct -> MaterialTheme.colorScheme.errorContainer
        selected -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        revealed && correct -> MaterialTheme.colorScheme.secondary
        revealed && selected && !correct -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        onClick = onSelect,
        enabled = !revealed,
        shape = RoundedCornerShape(8.dp),
        color = background,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onSelect, enabled = !revealed)
            Text(option.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(28.dp))
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                option.content.forEach { block ->
                    QuestionContentBlock(block, imageResource, onImageClick)
                }
            }
            if (revealed && correct) Text("Correct", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            if (revealed && selected && !correct) Text("Your answer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun AnswerReview(question: Question, result: AttemptResult?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider()
        Text(
            when (result) {
                AttemptResult.CORRECT -> "Correct"
                AttemptResult.INCORRECT -> "Review this answer"
                AttemptResult.UNANSWERED, null -> "Answer and explanation"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(question.answerKey.explanation, style = MaterialTheme.typography.bodyMedium)
        question.options.sortedBy(QuestionOption::position).forEach { option ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${option.label}  ${if (option.id in question.answerKey.correctOptionIds) "Correct answer" else "Incorrect option"}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (option.id in question.answerKey.correctOptionIds) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                question.answerKey.optionRationales
                    .firstOrNull { it.optionId == option.id }
                    ?.text
                    ?.takeIf(String::isNotBlank)
                    ?.let { rationale ->
                        Text(rationale, style = MaterialTheme.typography.bodyMedium)
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
    val resourceId = imageResource(image.assetId)
    if (resourceId == null) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
            Text("Image unavailable", modifier = Modifier.fillMaxWidth().padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Image(
            painter = painterResource(resourceId),
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
    val resourceId = imageResource(image.assetId) ?: return
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
                    painter = painterResource(resourceId),
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
