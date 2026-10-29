package com.medstudy.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medstudy.core.designsystem.MedStudyTheme
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.PillStyle
import com.medstudy.core.designsystem.StudyChip
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.ContentBlock
import com.medstudy.core.model.Question
import com.medstudy.core.model.QuestionOption
import com.medstudy.core.model.readableExplanation
import kotlinx.coroutines.launch

/**
 * The "Quick question" bottom sheet shown over other apps: a dimmed screen with a rounded white card sliding up from the bottom.
 * Subject chip and option highlights use the subject's own color so it reads as part of the app.
 * [onRecord] saves the attempt and returns false if saving failed.
 */
@Composable
fun OverlayQuestionSheet(
    question: Question,
    subjectName: String,
    colorIndex: Int,
    onRecord: suspend (selectedId: String?, result: AttemptResult) -> Boolean,
    onSkip: () -> Unit,
    onDone: () -> Unit,
) {
    MedStudyTheme {
        val gradient = StudyTheme.subjectGradient(colorIndex)
        val scope = rememberCoroutineScope()
        var selectedId by remember { mutableStateOf<String?>(null) }
        var result by remember { mutableStateOf<AttemptResult?>(null) }
        var saving by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val revealed = result != null

        fun submit(showAnswer: Boolean) {
            if (saving || revealed) return
            val pick = if (showAnswer) null else selectedId
            val outcome = when {
                showAnswer -> AttemptResult.UNANSWERED
                pick != null && pick in question.answerKey.correctOptionIds -> AttemptResult.CORRECT
                else -> AttemptResult.INCORRECT
            }
            saving = true
            error = null
            scope.launch {
                if (onRecord(pick, outcome)) result = outcome else error = "Could not save your answer. Try again."
                saving = false
            }
        }

        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(enabled = false) {}) {
            AnimatedVisibility(
                visible = shown,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = StudyTheme.colors.canvas,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        // Colored header in the subject's gradient: handle, subject chip, Skip, and the question itself.
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                                .background(gradient.brush),
                        ) {
                            Box(
                                Modifier.align(Alignment.BottomEnd).offset(x = 50.dp, y = 50.dp).size(150.dp)
                                    .clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                            )
                            Column(
                                Modifier.padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 22.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp)
                                        .clip(CircleShape).background(Color.White.copy(alpha = 0.7f)),
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StudyChip(subjectName, container = Color.White.copy(alpha = 0.75f), contentColor = StudyTheme.colors.ink)
                                    Text("Quick question", style = MaterialTheme.typography.labelLarge, color = StudyTheme.colors.ink, modifier = Modifier.weight(1f))
                                    if (!revealed) {
                                        Text(
                                            "Skip",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = StudyTheme.colors.ink,
                                            modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.6f))
                                                .clickable(role = Role.Button, onClick = onSkip).padding(horizontal = 14.dp, vertical = 8.dp),
                                        )
                                    }
                                }
                                Column(Modifier.heightIn(max = 190.dp).verticalScroll(rememberScrollState())) {
                                    question.content.filterIsInstance<ContentBlock.Text>().forEach { block ->
                                        Text(block.text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp), color = StudyTheme.colors.ink)
                                    }
                                }
                            }
                        }

                        Column(
                            Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                        Column(
                            Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            question.options.sortedBy(QuestionOption::position).forEach { option ->
                                OverlayOption(
                                    option = option,
                                    selected = option.id == selectedId,
                                    revealed = revealed,
                                    correct = option.id in question.answerKey.correctOptionIds,
                                    deep = gradient.deep,
                                    onClick = { if (!revealed) selectedId = option.id },
                                )
                            }
                            if (revealed) {
                                val verdict = when (result) {
                                    AttemptResult.CORRECT -> "Correct"
                                    AttemptResult.INCORRECT -> "Not quite"
                                    else -> "Answer shown"
                                }
                                val verdictColor = when (result) {
                                    AttemptResult.CORRECT -> StudyTheme.colors.success
                                    AttemptResult.INCORRECT -> StudyTheme.colors.error
                                    else -> StudyTheme.colors.ink
                                }
                                Column(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(StudyTheme.colors.surface).padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(verdict, style = MaterialTheme.typography.titleMedium, color = verdictColor)
                                    val explanation = readableExplanation(question.answerKey.explanation)
                                    Text(
                                        explanation.ifBlank {
                                            "Answer: " + question.options.filter { it.id in question.answerKey.correctOptionIds }.joinToString { it.label }
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = StudyTheme.colors.body,
                                    )
                                }
                            }
                        }

                        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.error) }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            if (revealed) {
                                PillButton("Done", onClick = onDone, containerColor = gradient.deep, modifier = Modifier.fillMaxWidth())
                            } else {
                                PillButton("Show answer", onClick = { submit(true) }, style = PillStyle.Secondary, enabled = !saving)
                                PillButton(
                                    "Check answer", onClick = { submit(false) },
                                    enabled = selectedId != null && !saving,
                                    containerColor = gradient.deep, modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayOption(
    option: QuestionOption,
    selected: Boolean,
    revealed: Boolean,
    correct: Boolean,
    deep: Color,
    onClick: () -> Unit,
) {
    val colors = StudyTheme.colors
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
    val badge = when {
        revealed && correct -> colors.success
        wrongPick -> colors.error
        selected -> deep
        else -> colors.canvas
    }
    val badgeText = if (badge == colors.canvas) colors.ink else colors.onAccent
    val label = option.content.filterIsInstance<ContentBlock.Text>().joinToString(" ") { it.text }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = BorderStroke(if (selected || (revealed && correct)) 1.5.dp else 1.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.selectable(selected = selected, enabled = !revealed, role = Role.RadioButton, onClick = onClick).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(badge), contentAlignment = Alignment.Center) {
                Text(option.label, style = MaterialTheme.typography.labelLarge, color = badgeText)
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
            if (revealed && correct) Text("Correct", style = MaterialTheme.typography.labelMedium, color = colors.success)
            if (wrongPick) Text("Your answer", style = MaterialTheme.typography.labelMedium, color = colors.error)
        }
    }
}
