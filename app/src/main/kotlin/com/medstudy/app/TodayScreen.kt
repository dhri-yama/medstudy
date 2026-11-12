package com.medstudy.app

import com.medstudy.feature.progress.SubjectSummary
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.medstudy.core.designsystem.StudyCard
import com.medstudy.core.designsystem.SubjectGradient
import com.medstudy.core.designsystem.dimmedIf
import java.time.format.TextStyle
import java.util.Locale
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medstudy.core.designsystem.NotchedCard
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.PillStyle
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
data class ContinueTarget(val title: String, val subtitle: String, val colorPosition: Int, val tab: String, val detail: String)

/** One day in the "This week" strip: how many questions were answered. */
data class WeekDay(val date: LocalDate, val count: Int)

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
    weekDays: List<WeekDay>,
    mistakeCount: Int,
    trialDaysLeft: Int?,
    freeQuestionsLeft: Int?,
    /** Days until the exam, or null when no date is set. */
    examDaysLeft: Int?,
    onSetExamDate: () -> Unit,
    onUpgrade: () -> Unit,
    onQuickFive: () -> Unit,
    onMistakes: () -> Unit,
    onContinue: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onStartBlock: (StudyBlock) -> Unit,
    onReviewDue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val continueGradient = StudyTheme.subjectGradient(continueTarget.colorPosition)
    // Answers are earned inside practice, so on return the count and bar move from what was last shown here.
    val answeredShown = rememberCatchUp("today.answered", answeredToday)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 20.dp + LocalNavBarInset.current),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Header(name, today, streak, examDaysLeft, onSetExamDate, Modifier.padding(horizontal = 20.dp))

        PlanBanner(trialDaysLeft, freeQuestionsLeft, onUpgrade, Modifier.padding(horizontal = 20.dp))

        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            NotchedCard(
                gradient = continueGradient,
                modifier = Modifier.fillMaxWidth(),
                onClick = onContinue,
                minHeight = 236.dp,
                verticalArrangement = Arrangement.SpaceBetween,
                decoration = {
                    Box(
                        Modifier.align(Alignment.BottomEnd).offset(x = 56.dp, y = 56.dp).size(200.dp)
                            .clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                    )
                    Box(
                        Modifier.align(Alignment.CenterEnd).offset(x = 34.dp, y = 8.dp).size(96.dp)
                            .clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                    )
                },
                tab = { CardTabLabel(continueTarget.tab) },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        continueTarget.title, style = MaterialTheme.typography.headlineSmall, color = StudyTheme.colors.ink,
                        modifier = Modifier.fillMaxWidth(0.6f),
                    )
                    Text(continueTarget.subtitle, style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink)
                    Spacer(Modifier.height(8.dp))
                    StudyChip(continueTarget.detail)
                }
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        SlidingCount(
                            answeredShown, color = StudyTheme.colors.ink,
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp, lineHeight = 52.sp),
                        )
                        Text(
                            " / $goal questions today", style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    StudyProgressBar(
                        progress = if (goal == 0) 0f else answeredShown.toFloat() / goal,
                        description = "$answeredToday of $goal questions today",
                        fill = continueGradient.deep,
                        track = Color.White.copy(alpha = 0.55f),
                    )
                }
            }
        }

        var selectedEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
        val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
        val plannedDates = planRows.mapTo(HashSet()) { it.date }
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            WeekStrip(weekDays, today, selectedDate, plannedDates, continueGradient) { selectedEpochDay = it.toEpochDay() }
        }

        SelectedDayPlan(
            date = selectedDate,
            today = today,
            rows = planRows.filter { it.date == selectedDate },
            onStartBlock = onStartBlock,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Section("Quick practice", Modifier.padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NotchedCard(
                    gradient = StudyTheme.subjectGradient(7),
                    modifier = Modifier.weight(1f),
                    onClick = onQuickFive,
                    minHeight = 184.dp,
                    verticalArrangement = Arrangement.SpaceBetween,
                    decoration = { QuickDecoration() },
                    tab = { CardTabLabel("5") },
                ) {
                    Column {
                        Spacer(Modifier.height(28.dp))
                        Text("Quick 5", style = MaterialTheme.typography.headlineSmall, color = StudyTheme.colors.ink)
                    }
                    Text("Five fresh questions, about 15 minutes", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink)
                }
                NotchedCard(
                    gradient = StudyTheme.subjectGradient(3),
                    modifier = Modifier.weight(1f).dimmedIf(mistakeCount == 0),
                    onClick = if (mistakeCount > 0) onMistakes else null,
                    minHeight = 184.dp,
                    verticalArrangement = Arrangement.SpaceBetween,
                    decoration = { QuickDecoration() },
                    tab = { CardTabLabel("$mistakeCount") },
                ) {
                    Column {
                        Spacer(Modifier.height(28.dp))
                        Text("Mistakes", style = MaterialTheme.typography.headlineSmall, color = StudyTheme.colors.ink)
                    }
                    Text(
                        if (mistakeCount == 0) "None yet" else "Retry what you missed",
                        style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink,
                    )
                }
            }
        }

        if (subjects.isNotEmpty()) {
            Section("Subjects", titleModifier = Modifier.padding(horizontal = 20.dp)) {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(subjects, key = SubjectSummary::id) { subject ->
                        SubjectCard(subject, Modifier.width(268.dp), minHeight = 192.dp, onClick = { onOpenSubject(subject.id) })
                    }
                }
            }
        }

        if (dueAreas.isNotEmpty()) {
            Section("Due now", Modifier.padding(horizontal = 20.dp)) {
                val dueGradient = StudyTheme.subjectGradient(4)
                NotchedCard(
                    gradient = dueGradient,
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
                    PillButton("Review due topics", onClick = onReviewDue, containerColor = dueGradient.deep, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private fun dueLabel(area: WeakArea): String =
    area.retrievability?.takeIf { it < DUE_RETRIEVABILITY }?.let { "~${(it * 100).toInt()}% recall" }
        ?: area.accuracy?.let { "${(it * 100).toInt()}% correct" }.orEmpty()

@Composable
private fun Header(name: String, today: LocalDate, streak: Int, examDaysLeft: Int?, onSetExamDate: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileBlob(name, size = 48.dp)
        Column(Modifier.weight(1f)) {
            Text("Hey, $name", style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink)
            Text(
                today.format(DateTimeFormatter.ofPattern("d MMM, EEEE")),
                style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted,
            )
            // The countdown, or a way to set the date; tapping either opens the date picker.
            Text(
                examDaysLeft?.let(::examCountdownLabel) ?: "Set your exam date",
                style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink,
                modifier = Modifier.clip(CircleShape)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "Change exam date", onClick = onSetExamDate)
                    .padding(vertical = 4.dp),
            )
        }
        if (streak > 0) {
            // Same look as StudyChip, with a number that slides and a flame that pops when the streak grows.
            val streakShown = rememberCatchUp("today.streak", streak)
            val pop = rememberStreakPop(streakShown)
            Row(
                modifier = Modifier.clip(CircleShape).background(StudyTheme.colors.surface).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = StudyTheme.colors.ink,
                    modifier = Modifier.size(14.dp).graphicsLayer { scaleX = pop; scaleY = pop },
                )
                SlidingCount(
                    streakShown, style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink,
                    text = { "${pluralize(it, "day")} streak" },
                )
            }
        }
    }
}

/**
 * Seven tall day pills. Only today is colored (the Continue card's gradient); other days are white with a hairline.
 * A day you select becomes solid ink; if that is today it keeps its color and gains an ink ring.
 * A dot marks answered (solid) or planned (faded) days.
 */
@Composable
private fun WeekStrip(
    days: List<WeekDay>,
    today: LocalDate,
    selected: LocalDate,
    plannedDates: Set<LocalDate>,
    todayGradient: SubjectGradient,
    onSelect: (LocalDate) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { day ->
            DayPill(
                day = day,
                todayGradient = todayGradient,
                isToday = day.date == today,
                isSelected = day.date == selected,
                isPlanned = day.date in plannedDates,
                onClick = { onSelect(day.date) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayPill(
    day: WeekDay,
    todayGradient: SubjectGradient,
    isToday: Boolean,
    isSelected: Boolean,
    isPlanned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val shape = CircleShape
    val ink = StudyTheme.colors.accent
    val inkFilled = isSelected && !isToday
    val content = if (inkFilled) StudyTheme.colors.onAccent else StudyTheme.colors.ink
    val dot = when {
        day.count > 0 -> content
        isPlanned -> content.copy(alpha = 0.45f)
        else -> Color.Transparent
    }
    Column(
        modifier = modifier
            .height(92.dp)
            .clip(shape)
            .background(
                when {
                    inkFilled -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(ink, ink))
                    isToday -> todayGradient.brush
                    else -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(StudyTheme.colors.surface, StudyTheme.colors.surface))
                },
            )
            .then(
                when {
                    isToday && isSelected -> Modifier.border(2.dp, ink, shape)
                    !isToday && !isSelected -> Modifier.border(1.dp, StudyTheme.colors.hairline, shape)
                    else -> Modifier
                },
            )
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = "$label ${day.date.dayOfMonth}, ${pluralize(day.count, "question")} answered" +
                    if (isPlanned) ", plan scheduled" else ""
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = if (inkFilled) 0.85f else 0.75f), maxLines = 1)
        Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = content)
    }
}

/** The plan for whichever day is selected in the strip, plus how many questions were answered that day. */
@Composable
private fun SelectedDayPlan(
    date: LocalDate,
    today: LocalDate,
    rows: List<PlanRow>,
    onStartBlock: (StudyBlock) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isToday = date == today
    val title = if (isToday) "Today's plan" else "Plan for " + date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    Section(title, modifier) {
        if (rows.isEmpty()) {
            Text("Nothing planned for this day.", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.muted)
        }
        rows.forEach { row ->
            TaskRow(
                title = row.block.title,
                caption = "${pluralize(row.block.questionTarget, "question")} · ${row.block.minuteTarget} min",
                leading = { Icon(Icons.Outlined.Event, contentDescription = null, tint = StudyTheme.colors.ink) },
                trailing = if (isToday) {
                    { PillButton("Start", onClick = { onStartBlock(row.block) }, small = true) }
                } else null,
            )
        }
    }
}

@Composable
private fun BoxScope.QuickDecoration() {
    Box(
        Modifier.align(Alignment.BottomEnd).offset(x = 36.dp, y = 36.dp).size(110.dp)
            .clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
    )
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

/** Trial countdown, or the free plan's remaining allowance; tapping opens the plans screen. Hidden when subscribed. */
@Composable
private fun PlanBanner(trialDaysLeft: Int?, freeQuestionsLeft: Int?, onUpgrade: () -> Unit, modifier: Modifier = Modifier) {
    val text = when {
        trialDaysLeft != null -> "${pluralize(trialDaysLeft, "day")} left in your free trial"
        freeQuestionsLeft != null -> "Free plan · ${pluralize(freeQuestionsLeft, "question")} left today"
        else -> return
    }
    val urgent = (trialDaysLeft != null && trialDaysLeft <= 3) || freeQuestionsLeft != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(if (urgent) StudyTheme.colors.warningTint else StudyTheme.colors.surface)
            .clickable(role = Role.Button, onClickLabel = "See plans", onClick = onUpgrade)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = StudyTheme.colors.ink, modifier = Modifier.weight(1f))
        Text(if (trialDaysLeft != null) "Plans" else "Upgrade", style = MaterialTheme.typography.labelLarge, color = StudyTheme.colors.ink)
    }
}
