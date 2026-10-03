package com.medstudy.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.Category
import com.medstudy.core.model.Question
import com.medstudy.core.model.StudyBlock
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    onMoveBlock: (String, Long) -> Unit,
    onRemoveBlock: (String) -> Unit,
    onAddBlock: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (state.error != null && state.questions.isEmpty()) {
        Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Progress could not be loaded.", style = MaterialTheme.typography.titleLarge)
            Text(state.error, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Retry") }
        }
        return
    }

    val calendarRows = calendarRows(state.currentMonth)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Progress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Review your daily work and plan the next seven days.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { onMonthChange(-1) }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Previous month")
                }
                Text(
                    state.currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = { onMonthChange(1) }, enabled = state.currentMonth < YearMonth.now().plusMonths(1), modifier = Modifier.size(44.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Next month")
                }
            }
        }
        item {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                        Text(day, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                calendarRows.forEach { days ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        days.forEach { date ->
                            if (date == null) {
                                Spacer(Modifier.weight(1f).heightIn(min = 48.dp))
                            } else {
                                CalendarDay(
                                    date = date,
                                    count = state.dayCounts[date] ?: 0,
                                    selected = date == state.selectedDate,
                                    onClick = { onSelectDate(date) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("Subjects", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 4.dp)) {
                item {
                    FilterChip(
                        selected = state.selectedSubjectIds.isEmpty(),
                        onClick = { onToggleSubject(null) },
                        label = { Text("All") },
                    )
                }
                items(state.subjects, key = Category::id) { subject ->
                    FilterChip(
                        selected = subject.id in state.selectedSubjectIds,
                        onClick = { onToggleSubject(subject.id) },
                        label = { Text(subject.name) },
                    )
                }
            }
        }
        item {
            val formattedDate = state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
            Text(formattedDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${state.selectedDayEntries.size} completed questions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.selectedDayEntries.isEmpty()) {
            item {
                Text(
                    "No questions match this date and subject filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(state.selectedDayEntries, key = { it.attempt.id }) { entry ->
                QuestionHistoryRow(entry = entry, onClick = { onReviewQuestion(entry.question) })
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp)) }
        item {
            Text("Schedule preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Available days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(top = 2.dp)) {
                items(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").mapIndexed { index, label -> (index + 1) to label }) { (day, label) ->
                    FilterChip(
                        selected = day in state.preferences.availableWeekdays,
                        enabled = day in state.preferences.availableWeekdays || state.preferences.availableWeekdays.size > 1,
                        onClick = { onToggleStudyDay(day) },
                        label = { Text(label) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("Questions per day", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TargetStepper(
                    value = state.preferences.questionTargetPerDay,
                    onDecrease = { onQuestionTargetChange(-1) },
                    onIncrease = { onQuestionTargetChange(1) },
                    valueLabel = "${state.preferences.questionTargetPerDay}",
                    minimum = 1,
                    maximum = 100,
                    targetName = "questions per day",
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Minutes per day", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TargetStepper(
                    value = state.preferences.minuteTargetPerDay,
                    onDecrease = { onMinuteTargetChange(-5) },
                    onIncrease = { onMinuteTargetChange(5) },
                    valueLabel = "${state.preferences.minuteTargetPerDay}",
                    minimum = 5,
                    maximum = 240,
                    targetName = "minutes per day",
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Next seven days", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("A plan based on recent answered questions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRegeneratePlan, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Regenerate study plan")
                }
            }
        }
        val planBlocks = state.plan?.blocks.orEmpty().sortedWith(compareBy(StudyBlock::studyDateIso, StudyBlock::position))
        if (planBlocks.isEmpty()) {
            item { Text("No study blocks yet. Add a topic below.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(planBlocks, key = StudyBlock::id) { block ->
                StudyBlockRow(
                    block = block,
                    onMoveEarlier = { onMoveBlock(block.id, -1) },
                    onMoveLater = { onMoveBlock(block.id, 1) },
                    onRemove = { onRemoveBlock(block.id) },
                )
            }
        }
        item {
            Text("Focus areas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
        if (state.areas.isEmpty()) {
            item { Text("No topic data yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.areas.take(8), key = { "area-${it.id}" }) { area ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(area.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            Text(areaDescription(area), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (area.topicId != null && planBlocks.none { it.topicId == area.topicId }) {
                            IconButton(onClick = { onAddBlock(area.id) }) {
                                Icon(Icons.Outlined.Add, contentDescription = "Add ${area.title} to study plan")
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.size(12.dp)) }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        modifier = modifier.padding(vertical = 2.dp).heightIn(min = 48.dp)
            .semantics { contentDescription = "${date.dayOfMonth}, $count questions completed" },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge)
            if (count > 0) Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun QuestionHistoryRow(
    entry: AttemptHistoryEntry,
    onClick: () -> Unit,
) {
    val question = entry.question
    val excerpt = question.content.filterIsInstance<com.medstudy.core.model.ContentBlock.Text>()
        .joinToString(" ") { it.text }
    val result = when (entry.attempt.result) {
        AttemptResult.CORRECT -> "Correct"
        AttemptResult.INCORRECT -> "Incorrect"
        AttemptResult.UNANSWERED -> "Unanswered"
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(question.subjectId.substringAfterLast('.').replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                Text(result, style = MaterialTheme.typography.labelMedium, color = if (entry.attempt.result == AttemptResult.CORRECT) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(excerpt, style = MaterialTheme.typography.bodyMedium, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
            Text("${entry.attempt.completedAtEpochMillis.toLocalTimeLabel()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun StudyBlockRow(
    block: StudyBlock,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(block.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    "${LocalDate.parse(block.studyDateIso).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))}  ·  ${block.questionTarget} questions  ·  ${block.minuteTarget} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onMoveEarlier, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Move ${block.title} one day earlier")
            }
            IconButton(onClick = onMoveLater, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Move ${block.title} one day later")
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove ${block.title} from study plan")
            }
        }
    }
}

@Composable
private fun TargetStepper(
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    valueLabel: String,
    minimum: Int,
    maximum: Int,
    targetName: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDecrease, enabled = value > minimum, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Outlined.Remove, contentDescription = "Decrease $targetName")
        }
        Text(valueLabel, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 4.dp))
        IconButton(onClick = onIncrease, enabled = value < maximum, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = "Increase $targetName")
        }
    }
}

private fun areaDescription(area: WeakArea): String = when (area.status) {
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
