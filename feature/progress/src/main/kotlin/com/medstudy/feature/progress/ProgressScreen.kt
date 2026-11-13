package com.medstudy.feature.progress

import androidx.compose.foundation.background
import com.medstudy.core.designsystem.dimmedIf
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.medstudy.core.designsystem.StudyCard
import com.medstudy.core.designsystem.SubjectGradient
import com.medstudy.core.model.Category
import com.medstudy.core.model.StudyPlanKind
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.medstudy.core.designsystem.TabHeader
import com.medstudy.core.designsystem.FilterPill
import com.medstudy.core.designsystem.bleed
import com.medstudy.core.designsystem.IconCircleButton
import com.medstudy.core.designsystem.NotchedCard
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.PillStyle
import com.medstudy.core.designsystem.SegmentedTabs
import com.medstudy.core.designsystem.StudyChip
import com.medstudy.core.designsystem.StudyProgressBar
import com.medstudy.core.designsystem.PageTitleStyle
import com.medstudy.core.designsystem.SectionTitleStyle
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.designsystem.TaskRow
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.ContentBlock
import com.medstudy.core.model.Question
import com.medstudy.core.model.StudyBlock
import com.medstudy.core.model.pluralize
import com.medstudy.core.model.subjectDisplayName
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TabLabels = listOf("Overview", "Calendar", "Plan")

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onMonthChange: (Long) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggleSubject: (String?) -> Unit,
    onReviewQuestion: (Question) -> Unit,
    onToggleStudyDay: (Int) -> Unit,
    onQuestionTargetChange: (Int) -> Unit,
    onMinuteTargetChange: (Int) -> Unit,
    onRegeneratePlan: () -> Unit,
    onReorderBlock: (String, Int) -> Unit,
    onRemoveBlock: (String) -> Unit,
    onAddBlock: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    /** Extra space at the end of the list so it can scroll clear of a floating bottom bar. */
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    if (state.isLoading) {
        Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(color = StudyTheme.colors.ink)
        }
        return
    }
    if (state.error != null && state.questions.isEmpty()) {
        Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
            Text("Progress could not be loaded.", style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink)
            Text(state.error, color = StudyTheme.colors.error, style = MaterialTheme.typography.bodyMedium)
            PillButton("Retry", onClick = onRetry)
        }
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 20.dp + bottomInset),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TabHeader("Your plan\nand progress", Modifier.bleed(20.dp))
        }
        item { SegmentedTabs(TabLabels, tab, { tab = it }) }
        when (tab) {
            0 -> overviewTab(state, onAddBlock)
            1 -> calendarTab(state, onMonthChange, onSelectDate, onToggleSubject, onReviewQuestion)
            else -> planTab(
                state, onToggleStudyDay, onQuestionTargetChange, onMinuteTargetChange,
                onRegeneratePlan, onReorderBlock, onRemoveBlock,
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ---- Overview -------------------------------------------------------------------------------

private fun LazyListScope.overviewTab(state: ProgressUiState, onAddBlock: (String) -> Unit) {
    val graded = state.attempts.filter { it.result != AttemptResult.UNANSWERED }
    val accuracy = graded.takeIf { it.isNotEmpty() }?.let { list -> list.count { it.result == AttemptResult.CORRECT } * 100 / list.size }
    val answered = state.attempts.mapTo(HashSet()) { it.questionId }.size
    val subjects = subjectSummaries(state.categories, state.questions, state.attempts)

    item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Answered", "$answered", "of ${state.questions.size} questions", 0, Modifier.weight(1f))
            StatCard("Accuracy", accuracy?.let { "$it%" } ?: "-", if (accuracy == null) "No graded answers" else "correct overall", 2, Modifier.weight(1f))
        }
    }
    item { SectionTitle("By subject") }
    if (subjects.none { it.answered > 0 }) {
        item { Hint("Answer a few questions and your subjects show up here.") }
    } else {
        items(subjects.filter { it.answered > 0 }, key = { "subject-${it.id}" }) { subject ->
            SubjectStrengthRow(subject)
        }
    }

    item {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 8.dp)) {
            Text("Needs attention", style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink)
            Text(
                "Topics you've missed or may be forgetting. Tap + to add one to your plan.",
                style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted,
            )
        }
    }
    val planTopicIds = state.plan?.blocks.orEmpty().mapNotNull(StudyBlock::topicId).toSet()
    val areas = state.areas.filter { it.answeredAttempts > 0 && (it.isDue() || it.status != WeakAreaStatus.ON_TRACK) }.take(8)
    if (areas.isEmpty()) {
        item { Hint("Nothing yet. Answer at least 5 questions in a topic and it will show up here if it needs work.") }
    } else {
        items(areas, key = { "area-${it.id}" }) { area ->
            val gradient = StudyTheme.subjectGradient(colorIndexOf(state, area.subjectId))
            TaskRow(
                title = area.title,
                caption = areaDescription(area),
                leading = { Box(Modifier.size(14.dp).clip(CircleShape).background(gradient.top)) },
                trailing = if (area.topicId != null && area.topicId !in planTopicIds) {
                    { IconCircleButton(Icons.Outlined.Add, "Add ${area.title} to study plan", { onAddBlock(area.id) }, container = StudyTheme.colors.canvas) }
                } else null,
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, caption: String, colorIndex: Int, modifier: Modifier = Modifier) {
    NotchedCard(StudyTheme.subjectGradient(colorIndex), modifier, tab = {
        Text(title, style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink, maxLines = 1)
    }) {
        Spacer(Modifier.height(28.dp))
        Text(value, style = MaterialTheme.typography.headlineLarge, color = StudyTheme.colors.ink)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.ink)
    }
}

@Composable
private fun SubjectStrengthRow(subject: SubjectSummary) {
    TaskRow(
        title = subject.name,
        caption = "${subject.answered} of ${subject.questionCount} answered · " + (subject.accuracyPercent?.let { "$it% correct" } ?: "no graded answers"),
        leading = {
            Box(Modifier.size(14.dp).clip(CircleShape).background(StudyTheme.subjectGradient(subject.colorIndex).top))
        },
        trailing = {
            Column(Modifier.size(width = 72.dp, height = 8.dp)) {
                StudyProgressBar(
                    progress = (subject.accuracyPercent ?: 0) / 100f,
                    description = "${subject.name} accuracy ${subject.accuracyPercent ?: 0} percent",
                )
            }
        },
    )
}

// ---- Calendar -------------------------------------------------------------------------------

private fun LazyListScope.calendarTab(
    state: ProgressUiState,
    onMonthChange: (Long) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggleSubject: (String?) -> Unit,
    onReviewQuestion: (Question) -> Unit,
) {
    val today = LocalDate.now()
    val monthTotal = state.dayCounts.filterKeys { YearMonth.from(it) == state.currentMonth }.values.sum()
    val activeDays = state.dayCounts.filter { YearMonth.from(it.key) == state.currentMonth && it.value > 0 }.size
    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(Icons.AutoMirrored.Outlined.ArrowBack, "Previous month", { onMonthChange(-1) })
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    state.currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink,
                )
                Text(
                    "${pluralize(activeDays, "day")} · ${pluralize(monthTotal, "question")}",
                    style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted,
                )
            }
            if (state.currentMonth < YearMonth.now().plusMonths(1)) {
                IconCircleButton(Icons.AutoMirrored.Outlined.ArrowForward, "Next month", { onMonthChange(1) })
            } else {
                Spacer(Modifier.size(44.dp))
            }
        }
    }
    item {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(StudyTheme.colors.surface).padding(horizontal = 8.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        day, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                        color = StudyTheme.colors.muted, textAlign = TextAlign.Center,
                    )
                }
            }
            calendarRows(state.currentMonth).forEach { days ->
                Row {
                    days.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.weight(1f).heightIn(min = 48.dp))
                        } else {
                            CalendarDay(
                                date = date,
                                count = state.dayCounts[date] ?: 0,
                                isToday = date == today,
                                selected = date == state.selectedDate,
                                onClick = { onSelectDate(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Fewer", style = MaterialTheme.typography.labelSmall, color = StudyTheme.colors.muted)
                listOf(0, 1, 5, 10).forEach { sample ->
                    Box(Modifier.size(14.dp).clip(CircleShape).background(heatBrush(sample)))
                }
                Text("More", style = MaterialTheme.typography.labelSmall, color = StudyTheme.colors.muted)
            }
        }
    }
    item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterPill("All subjects", state.selectedSubjectIds.isEmpty(), { onToggleSubject(null) }) }
            items(state.subjects, key = { it.id }) { subject ->
                FilterPill(subject.name, subject.id in state.selectedSubjectIds, { onToggleSubject(subject.id) })
            }
        }
    }

    val entries = state.selectedDayEntries
    val graded = entries.filter { it.attempt.result != AttemptResult.UNANSWERED }
    val correct = graded.count { it.attempt.result == AttemptResult.CORRECT }
    item {
        NotchedCard(
            gradient = StudyTheme.subjectGradient(if (entries.isEmpty()) 4 else 2),
            modifier = Modifier.fillMaxWidth(),
            minHeight = 132.dp,
            verticalArrangement = Arrangement.SpaceBetween,
            decoration = {
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = 30.dp, y = 30.dp).size(100.dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                )
            },
            tab = { Text(if (state.selectedDate == today) "Today" else state.selectedDate.format(DateTimeFormatter.ofPattern("d MMM")), style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink) },
        ) {
            Text(
                state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink, modifier = Modifier.fillMaxWidth(0.62f),
            )
            Text(
                when {
                    entries.isEmpty() -> "No questions answered"
                    graded.isEmpty() -> pluralize(entries.size, "question") + " shown"
                    else -> "${pluralize(entries.size, "question")} · ${correct * 100 / graded.size}% correct"
                },
                style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink,
            )
        }
    }
    if (entries.isEmpty()) {
        item { Hint("Nothing here. Pick another day, or change the subject filter.") }
    } else {
        items(entries, key = { it.attempt.id }) { entry ->
            val gradient = StudyTheme.subjectGradient(colorIndexOf(state, entry.question.subjectId))
            TaskRow(
                title = entry.question.content.filterIsInstance<ContentBlock.Text>().joinToString(" ") { it.text },
                caption = "${subjectDisplayName(entry.question.subjectId)} · ${entry.attempt.completedAtEpochMillis.toLocalTimeLabel()}",
                onClick = { onReviewQuestion(entry.question) },
                leading = { Box(Modifier.size(14.dp).clip(CircleShape).background(gradient.top)) },
                trailing = {
                    when (entry.attempt.result) {
                        AttemptResult.CORRECT -> StudyChip("Correct", container = StudyTheme.colors.successTint, contentColor = StudyTheme.colors.success)
                        AttemptResult.INCORRECT -> StudyChip("Missed", container = StudyTheme.colors.errorTint, contentColor = StudyTheme.colors.error)
                        AttemptResult.UNANSWERED -> StudyChip("Shown", container = StudyTheme.colors.canvas, contentColor = StudyTheme.colors.ink)
                    }
                },
            )
        }
    }
}

/** More answers in a day, deeper pink; no answers is a plain white circle. */
@Composable
private fun heatBrush(count: Int): androidx.compose.ui.graphics.Brush {
    val g = StudyTheme.subjectGradient(0)
    val c = when {
        count <= 0 -> StudyTheme.colors.canvas
        count < 5 -> g.bottom
        count < 10 -> g.top
        else -> g.deep
    }
    return androidx.compose.ui.graphics.Brush.verticalGradient(listOf(c, c))
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    count: Int,
    isToday: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val heavy = count >= 10
    val textColor = when {
        selected -> StudyTheme.colors.onAccent
        heavy -> Color.White
        else -> StudyTheme.colors.ink
    }
    Box(modifier.heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (selected) androidx.compose.ui.graphics.Brush.verticalGradient(listOf(StudyTheme.colors.accent, StudyTheme.colors.accent)) else heatBrush(count))
                .then(if (isToday && !selected) Modifier.border(2.dp, StudyTheme.colors.accent, CircleShape) else Modifier)
                .clickable(onClick = onClick)
                .semantics { contentDescription = "${date.dayOfMonth}, ${pluralize(count, "question")} answered" },
            contentAlignment = Alignment.Center,
        ) {
            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge, color = textColor)
        }
    }
}

// ---- Plan -----------------------------------------------------------------------------------

private fun LazyListScope.planTab(
    state: ProgressUiState,
    onToggleStudyDay: (Int) -> Unit,
    onQuestionTargetChange: (Int) -> Unit,
    onMinuteTargetChange: (Int) -> Unit,
    onRegeneratePlan: () -> Unit,
    onReorderBlock: (String, Int) -> Unit,
    onRemoveBlock: (String) -> Unit,
) {
    val planBlocks = state.plan?.blocks.orEmpty().inPlanOrder()
    val totalQuestions = planBlocks.sumOf(StudyBlock::questionTarget)
    val totalMinutes = planBlocks.sumOf(StudyBlock::minuteTarget)
    val heroGradient = StudyTheme.subjectGradient(5)
    item {
        NotchedCard(
            gradient = heroGradient,
            modifier = Modifier.fillMaxWidth(),
            minHeight = 208.dp,
            verticalArrangement = Arrangement.SpaceBetween,
            decoration = {
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = 56.dp, y = 56.dp).size(190.dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                )
            },
            tab = {
                Text(
                    if (state.plan?.kind == StudyPlanKind.GENERATED) "Smart" else "Starter",
                    style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink,
                )
            },
        ) {
            Column {
                Text("Next 7 days", style = MaterialTheme.typography.headlineSmall, color = StudyTheme.colors.ink, modifier = Modifier.fillMaxWidth(0.6f))
                Text(
                    "${pluralize(planBlocks.size, "study block")} · ${pluralize(totalQuestions, "question")} · $totalMinutes min",
                    style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (state.plan?.kind == StudyPlanKind.GENERATED) "Built from the topics you've missed or may be forgetting."
                    else "A starter plan. It adapts once you've answered more questions.",
                    style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.ink,
                )
                PillButton("Regenerate plan", onClick = onRegeneratePlan, containerColor = heroGradient.deep, modifier = Modifier.fillMaxWidth(0.62f))
            }
        }
    }
    if (planBlocks.isEmpty()) {
        item { Hint("No study blocks yet. Add a topic from Overview, or turn on some study days below.") }
    } else {
        item(key = "plan-blocks") {
            PlanBlockList(
                blocks = planBlocks,
                gradientOf = { block -> StudyTheme.subjectGradient(colorIndexOf(state, block.subjectId)) },
                onReorder = onReorderBlock,
                onRemove = onRemoveBlock,
            )
        }
        item { Hint("Hold and drag a block to move it to another day. Swipe left to remove it.") }
    }
    item {
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Your schedule", style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink)
            Text("Pick the days you study and how much you want to do on each.", style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted)
        }
    }
    item {
        val days = state.preferences.availableWeekdays
        StudyCard {
            Text("Study days", style = MaterialTheme.typography.labelLarge, color = StudyTheme.colors.muted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").forEachIndexed { index, full ->
                    val day = index + 1
                    val on = day in days
                    val enabled = canToggleStudyDay(day, days)
                    Column(
                        Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(CircleShape)
                            .background(if (on) StudyTheme.colors.accent else StudyTheme.colors.canvas)
                            .selectable(selected = on, enabled = enabled, role = Role.Checkbox, onClick = { onToggleStudyDay(day) })
                            .semantics { contentDescription = full },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            full.take(3), style = MaterialTheme.typography.labelMedium,
                            color = if (on) StudyTheme.colors.onAccent else StudyTheme.colors.ink.copy(alpha = if (enabled) 1f else 0.4f),
                        )
                    }
                }
            }
            Text(
                "${pluralize(days.size, "study day")} a week",
                style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted,
            )
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AmountTile(
                title = "Questions", unit = "per day", value = state.preferences.questionTargetPerDay,
                colorIndex = 2, minimum = 1, maximum = 100,
                onDecrease = { onQuestionTargetChange(-5) }, onIncrease = { onQuestionTargetChange(5) },
                modifier = Modifier.weight(1f),
            )
            AmountTile(
                title = "Minutes", unit = "per day", value = state.preferences.minuteTargetPerDay,
                colorIndex = 3, minimum = 5, maximum = 240,
                onDecrease = { onMinuteTargetChange(-5) }, onIncrease = { onMinuteTargetChange(5) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    item {
        val days = state.preferences.availableWeekdays.size
        val weeklyMinutes = days * state.preferences.minuteTargetPerDay
        Text(
            "That's ${pluralize(days * state.preferences.questionTargetPerDay, "question")} and " +
                (if (weeklyMinutes >= 60) "${weeklyMinutes / 60} h ${weeklyMinutes % 60} min" else "$weeklyMinutes min") + " a week.",
            style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.body,
        )
    }
}

/** A pastel tile with a large number and round - / + buttons. Steps of 5. */
@Composable
private fun AmountTile(
    title: String,
    unit: String,
    value: Int,
    colorIndex: Int,
    minimum: Int,
    maximum: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NotchedCard(
        gradient = StudyTheme.subjectGradient(colorIndex),
        modifier = modifier,
        minHeight = 176.dp,
        verticalArrangement = Arrangement.SpaceBetween,
        decoration = {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 36.dp, y = 36.dp).size(110.dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
            )
        },
        tab = { Text(unit, style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink, maxLines = 1) },
    ) {
        Column {
            Spacer(Modifier.height(28.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink)
            Text(
                "$value", color = StudyTheme.colors.ink,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp, lineHeight = 48.sp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconCircleButton(
                Icons.Outlined.Remove, "Decrease $title $unit", { if (value > minimum) onDecrease() },
                modifier = Modifier.dimmedIf(value <= minimum),
            )
            IconCircleButton(
                Icons.Outlined.Add, "Increase $title $unit", { if (value < maximum) onIncrease() },
                modifier = Modifier.dimmedIf(value >= maximum),
            )
        }
    }
}

/** Same subject, same color everywhere: index in the catalog sorted by position then name (matches subjectSummaries). */
private fun colorIndexOf(state: ProgressUiState, subjectId: String): Int =
    state.subjects.sortedWith(compareBy(Category::position, Category::name)).indexOfFirst { it.id == subjectId }.coerceAtLeast(0)

// ---- Shared ---------------------------------------------------------------------------------

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = SectionTitleStyle, color = StudyTheme.colors.heading, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.muted)
}

private fun areaDescription(area: WeakArea): String = areaStatusText(area) + dueSuffix(area)

private fun dueSuffix(area: WeakArea): String =
    area.retrievability?.takeIf { it < DUE_RETRIEVABILITY }?.let { " · ~${(it * 100).toInt()}% recall, due" }.orEmpty()

private fun areaStatusText(area: WeakArea): String = when (area.status) {
    WeakAreaStatus.NEEDS_REVIEW -> "Needs review · ${area.accuracy?.let { "${(it * 100).toInt()}% correct" } ?: ""} · ${area.answeredAttempts} answered"
    WeakAreaStatus.ON_TRACK -> "On track · ${area.accuracy?.let { "${(it * 100).toInt()}% correct" } ?: ""} · ${area.answeredAttempts} answered"
    WeakAreaStatus.UNASSESSED -> "Unassessed · ${area.answeredAttempts} of 5 answered"
}

private fun calendarRows(month: YearMonth): List<List<LocalDate?>> {
    val firstDayOffset = month.atDay(1).dayOfWeek.value % 7
    val cellCount = ((firstDayOffset + month.lengthOfMonth() + 6) / 7) * 7
    return (0 until cellCount).toList().chunked(7).map { row ->
        row.map { index ->
            val day = index - firstDayOffset + 1
            if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
        }
    }
}

private fun Long.toLocalTimeLabel(): String =
    java.time.Instant.ofEpochMilli(this)
        .atZone(java.time.ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
