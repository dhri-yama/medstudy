package com.medstudy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.medstudy.core.designsystem.NotchedCard
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.PillStyle
import com.medstudy.core.designsystem.StudyAvatar
import com.medstudy.core.designsystem.StudyChip
import com.medstudy.core.designsystem.StudyProgressBar
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.designsystem.TaskRow
import com.medstudy.core.model.StudyBlock
import com.medstudy.core.model.pluralize
import com.medstudy.feature.progress.DUE_RETRIEVABILITY
import com.medstudy.feature.progress.WeakArea
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** What the big "Continue learning" card points at. */
data class ContinueTarget(val title: String, val subtitle: String, val colorPosition: Int, val tab: String)

data class PlanRow(val block: StudyBlock, val date: LocalDate)

@Composable
fun TodayScreen(
    name: String,
    goal: Int,
    answeredToday: Int,
    streak: Int,
    continueTarget: ContinueTarget,
    subjects: List<SubjectSummary>,
    planRows: List<PlanRow>,
    dueAreas: List<WeakArea>,
    onContinue: () -> Unit,
    onStartDaily: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onStartBlock: (StudyBlock) -> Unit,
    onReviewDue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Header(name, today, streak, Modifier.padding(horizontal = 20.dp))

        Section("Continue learning", Modifier.padding(horizontal = 20.dp)) {
            NotchedCard(
                gradient = StudyTheme.subjectGradient(continueTarget.colorPosition),
                modifier = Modifier.fillMaxWidth(),
                onClick = onContinue,
                tab = { CardTabLabel(continueTarget.tab) },
            ) {
                Text(
                    continueTarget.title, style = MaterialTheme.typography.headlineSmall, color = StudyTheme.colors.ink,
                    modifier = Modifier.fillMaxWidth(0.6f),
                )
                Text(continueTarget.subtitle, style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink)
                Spacer(Modifier.height(20.dp))
                StudyProgressBar(
                    progress = if (goal == 0) 0f else answeredToday.toFloat() / goal,
                    description = "$answeredToday of $goal questions today",
                    track = Color.White.copy(alpha = 0.4f),
                )
                Spacer(Modifier.height(8.dp))
                Text("$answeredToday of $goal questions today", style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.ink)
            }
            PillButton(
                text = if (answeredToday >= goal) "Practice more" else "Start today's set",
                onClick = onStartDaily,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (subjects.isNotEmpty()) {
            Section("Subjects", titleModifier = Modifier.padding(horizontal = 20.dp)) {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(subjects, key = SubjectSummary::id) { subject ->
                        SubjectCard(subject, onClick = { onOpenSubject(subject.id) })
                    }
                }
            }
        }

        if (planRows.isNotEmpty()) {
            Section("Today's plan", Modifier.padding(horizontal = 20.dp)) {
                planRows.forEach { row ->
                    val isToday = row.date == today
                    val dateLabel = if (isToday) "Today" else row.date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
                    TaskRow(
                        title = row.block.title,
                        caption = "$dateLabel · ${pluralize(row.block.questionTarget, "question")} · ${row.block.minuteTarget} min",
                        leading = { Icon(Icons.Outlined.Event, contentDescription = null, tint = StudyTheme.colors.ink) },
                        trailing = if (isToday) {
                            { PillButton("Start", onClick = { onStartBlock(row.block) }, small = true) }
                        } else null,
                    )
                }
            }
        }

        if (dueAreas.isNotEmpty()) {
            Section("Due now", Modifier.padding(horizontal = 20.dp)) {
                NotchedCard(
                    gradient = StudyTheme.subjectGradient(4),
                    modifier = Modifier.fillMaxWidth(),
                    tab = { StudyChip("Due", container = StudyTheme.colors.warningTint, contentColor = StudyTheme.colors.ink) },
                ) {
                    Text(
                        pluralize(dueAreas.size, "topic") + " to revise",
                        style = MaterialTheme.typography.headlineSmall, color = StudyTheme.colors.ink,
                        modifier = Modifier.fillMaxWidth(0.6f),
                    )
                    Spacer(Modifier.height(12.dp))
                    dueAreas.take(3).forEach { area ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(area.title, style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text(dueLabel(area), style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    PillButton("Review due topics", onClick = onReviewDue, style = PillStyle.Secondary, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private fun dueLabel(area: WeakArea): String =
    area.retrievability?.takeIf { it < DUE_RETRIEVABILITY }?.let { "~${(it * 100).toInt()}% recall" }
        ?: area.accuracy?.let { "${(it * 100).toInt()}% correct" }.orEmpty()

@Composable
private fun Header(name: String, today: LocalDate, streak: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StudyAvatar(name)
        Column(Modifier.weight(1f)) {
            Text("Hey, $name", style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink)
            Text(
                today.format(DateTimeFormatter.ofPattern("d MMM, EEEE")),
                style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted,
            )
        }
        if (streak > 0) {
            StudyChip(
                text = "${pluralize(streak, "day")} streak",
                icon = Icons.Outlined.LocalFireDepartment,
                contentColor = StudyTheme.colors.ink,
            )
        }
    }
}

@Composable
private fun SubjectCard(subject: SubjectSummary, onClick: () -> Unit) {
    NotchedCard(
        gradient = StudyTheme.subjectGradient(subject.colorIndex),
        modifier = Modifier.width(164.dp),
        onClick = onClick,
        tab = { CardTabLabel("${subject.answered}/${subject.questionCount}") },
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            subject.name, style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink,
            maxLines = 2, minLines = 2,
        )
        Text(pluralize(subject.questionCount, "question"), style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.ink)
        Spacer(Modifier.height(10.dp))
        StudyProgressBar(
            progress = if (subject.questionCount == 0) 0f else subject.answered.toFloat() / subject.questionCount,
            description = "${subject.answered} of ${subject.questionCount} ${subject.name} questions answered",
            track = Color.White.copy(alpha = 0.4f),
        )
        Text(
            subject.accuracyPercent?.let { "$it% correct" } ?: "Not started",
            style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.ink, modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun CardTabLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink, maxLines = 1)
}

@Composable
private fun Section(
    title: String,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink, modifier = titleModifier)
        content()
    }
}
