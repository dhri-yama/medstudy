package com.medstudy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import com.medstudy.core.designsystem.PillStyle
import com.medstudy.core.designsystem.SegmentedTabs
import com.medstudy.core.designsystem.StudyProgressBar
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.medstudy.core.designsystem.ArcHeader
import com.medstudy.core.designsystem.NotchedCard
import com.medstudy.core.designsystem.bleed
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.StudyChip
import com.medstudy.core.designsystem.StudyTextField
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.designsystem.dimmedIf
import com.medstudy.feature.progress.SubjectSummary

/** One previous-year paper for the Practice tab; [questionIds] are in the paper's original order. */
data class PaperSummary(
    val id: String,
    val title: String,
    val year: Int,
    val isRecall: Boolean,
    val questionIds: List<String>,
    val answeredIds: Set<String>,
) {
    val answered get() = answeredIds.size
    /** What is left, still in paper order. */
    val unansweredIds get() = questionIds.filterNot { it in answeredIds }
}

private enum class PracticeTab(val label: String) { PAPERS("Papers"), SUBJECTS("Subjects") }

/**
 * Practice tab: one Daily set hero, two quiet quick actions (Review due, Mistakes), then Papers / Subjects tabs
 * so only one long list is on screen at a time. Saved questions is a pill in the header with a live count.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeHomeScreen(
    goal: Int,
    dueCount: Int,
    mistakeCount: Int,
    savedCount: Int,
    subjects: List<SubjectSummary>,
    papers: List<PaperSummary>,
    onStartPaper: (List<String>) -> Unit,
    onDaily: () -> Unit,
    onDue: () -> Unit,
    onMistakes: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var tabName by rememberSaveable { mutableStateOf(if (papers.isNotEmpty()) PracticeTab.PAPERS.name else PracticeTab.SUBJECTS.name) }
    val tab = PracticeTab.valueOf(tabName)
    var openPaperId by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = subjects.filter { it.name.contains(query, ignoreCase = true) }
    val dailyGradient = StudyTheme.subjectGradient(0)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 20.dp + LocalNavBarInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ArcHeader(Modifier.bleed(20.dp), height = 150.dp) {
                Row(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 22.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Practice", style = MaterialTheme.typography.headlineMedium, color = StudyTheme.colors.ink)
                        Text("Pick how you want to study.", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink)
                    }
                    Box(Modifier.clip(CircleShape).background(StudyTheme.colors.surface).androidClickable(onSaved)) {
                        StudyChip(
                            text = "Saved · $savedCount",
                            icon = Icons.Outlined.Bookmark,
                            container = StudyTheme.colors.surface,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        item {
            NotchedCard(
                gradient = dailyGradient,
                modifier = Modifier.fillMaxWidth(),
                onClick = onDaily,
                minHeight = 150.dp,
                verticalArrangement = Arrangement.SpaceBetween,
                decoration = { Bubble(150.dp, 48.dp, 48.dp, 0.22f) },
                tab = { Text("$goal", style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink) },
            ) {
                Column {
                    Text("Daily set", style = MaterialTheme.typography.headlineSmall, color = StudyTheme.colors.ink)
                    Text("$goal fresh questions · about ${goal * 3} min", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink)
                }
                PillButton("Start", onClick = onDaily, containerColor = dailyGradient.deep, modifier = Modifier.fillMaxWidth(0.5f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Review due", dueCount, StudyTheme.subjectGradient(2).deep, dueCount > 0, onDue, Modifier.weight(1f))
                QuickAction("Mistakes", mistakeCount, StudyTheme.subjectGradient(3).deep, mistakeCount > 0, onMistakes, Modifier.weight(1f))
            }
        }

        item {
            SegmentedTabs(
                labels = PracticeTab.entries.map { if (it == PracticeTab.PAPERS) "${it.label} · ${papers.size}" else it.label },
                selectedIndex = tab.ordinal,
                onSelect = { tabName = PracticeTab.entries[it].name },
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        when (tab) {
            PracticeTab.PAPERS -> {
                if (papers.isEmpty()) {
                    item { Text("No previous year papers in this build.", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.muted) }
                }
                items(papers, key = PaperSummary::id) { paper -> PaperRow(paper, onClick = { openPaperId = paper.id }) }
            }
            PracticeTab.SUBJECTS -> {
                item { StudyTextField(query, { query = it }, "Search subjects", hintInside = true) }
                if (visible.isEmpty()) {
                    item { Text("No subject matches \"$query\".", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.muted) }
                }
                items(visible.chunked(2), key = { row -> row.first().id }) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { SubjectTile(it, Modifier.weight(1f)) { onOpenSubject(it.id) } }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }

    papers.firstOrNull { it.id == openPaperId }?.let { paper ->
        ModalBottomSheet(onDismissRequest = { openPaperId = null }, containerColor = StudyTheme.colors.canvas) {
            PaperSheet(paper) { ids -> openPaperId = null; onStartPaper(ids) }
        }
    }
}

@Composable
private fun QuickAction(title: String, count: Int, dot: Color, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.dimmedIf(!enabled).clip(CircleShape).background(StudyTheme.colors.surface)
            .border(1.dp, StudyTheme.colors.hairline, CircleShape)
            .then(if (enabled) Modifier.androidClickable(onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        Text(title, style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink, modifier = Modifier.weight(1f), maxLines = 1)
        Text("$count", style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.muted)
    }
}

/** A plain white row: the year in a tinted square, the paper's name and kind, a thin progress bar. */
@Composable
private fun PaperRow(paper: PaperSummary, onClick: () -> Unit) {
    val gradient = StudyTheme.subjectGradient(1)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(StudyTheme.colors.surface).androidClickable(onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(gradient.brush), contentAlignment = Alignment.Center) {
            Text("'${paper.year % 100}", style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(paper.title, style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink, maxLines = 1)
                if (paper.isRecall) StudyChip("Recall", container = StudyTheme.colors.canvas)
            }
            StudyProgressBar(
                progress = if (paper.questionIds.isEmpty()) 0f else paper.answered.toFloat() / paper.questionIds.size,
                description = "${paper.answered} of ${paper.questionIds.size} questions answered",
            )
        }
        Text(
            "${paper.answered}/${paper.questionIds.size}", style = MaterialTheme.typography.labelLarge, color = StudyTheme.colors.muted,
            modifier = Modifier.width(64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End, maxLines = 1,
        )
    }
}

@Composable
private fun PaperSheet(paper: PaperSummary, onStart: (List<String>) -> Unit) {
    val started = paper.answered > 0 && paper.unansweredIds.isNotEmpty()
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(paper.title, style = MaterialTheme.typography.headlineSmall, color = StudyTheme.colors.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyChip("${paper.questionIds.size} questions")
            if (paper.isRecall) StudyChip("Recall-based")
        }
        Text(
            if (paper.isRecall) "Questions are recalled by students after the exam, so a few may differ from the original paper. Answers only, no explanations."
            else "Answers only, no explanations.",
            style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.body,
        )
        StudyProgressBar(
            progress = if (paper.questionIds.isEmpty()) 0f else paper.answered.toFloat() / paper.questionIds.size,
            description = "${paper.answered} of ${paper.questionIds.size} questions answered",
        )
        Text("${paper.answered} of ${paper.questionIds.size} done", style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted)
        if (started) {
            PillButton("Continue · ${paper.unansweredIds.size} left", onClick = { onStart(paper.unansweredIds) }, modifier = Modifier.fillMaxWidth())
            PillButton("Start over from Q1", onClick = { onStart(paper.questionIds) }, style = PillStyle.Secondary, modifier = Modifier.fillMaxWidth())
        } else {
            PillButton("Start in paper order", onClick = { onStart(paper.questionIds) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** A compact subject tile: colour dot, name, count and a thin progress bar. */
@Composable
private fun SubjectTile(subject: SubjectSummary, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val gradient = StudyTheme.subjectGradient(subject.colorIndex)
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(StudyTheme.colors.surface).androidClickable(onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(gradient.deep))
            Text("${subject.answered}/${subject.questionCount}", style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.muted)
        }
        Text(subject.name, style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink, maxLines = 2, minLines = 2)
        StudyProgressBar(
            progress = if (subject.questionCount == 0) 0f else subject.answered.toFloat() / subject.questionCount,
            description = "${subject.answered} of ${subject.questionCount} ${subject.name} questions answered",
            fill = gradient.deep, 
        )
        Text(
            subject.accuracyPercent?.let { "$it% correct" } ?: "Not started",
            style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted,
        )
    }
}

@Composable
private fun BoxScope.Bubble(size: androidx.compose.ui.unit.Dp, x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp, alpha: Float) {
    Box(Modifier.align(Alignment.BottomEnd).offset(x = x, y = y).size(size).clip(CircleShape).background(Color.White.copy(alpha = alpha)))
}

private fun Modifier.androidClickable(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick))
