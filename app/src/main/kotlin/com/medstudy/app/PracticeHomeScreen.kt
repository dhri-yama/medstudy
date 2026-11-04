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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    val isRecall: Boolean,
    val questionIds: List<String>,
    val answered: Int,
)

/**
 * Practice tab: a Daily set hero, Review due and Mistakes cards, then every subject as a grid with search.
 * Saved questions is a pill in the header with a live count.
 */
@Composable
fun PracticeHomeScreen(
    goal: Int,
    dueCount: Int,
    mistakeCount: Int,
    savedCount: Int,
    subjects: List<SubjectSummary>,
    papers: List<PaperSummary>,
    onOpenPaper: (PaperSummary) -> Unit,
    onDaily: () -> Unit,
    onDue: () -> Unit,
    onMistakes: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = subjects.filter { it.name.contains(query, ignoreCase = true) }
    val dailyGradient = StudyTheme.subjectGradient(0)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 20.dp + LocalNavBarInset.current),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
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

        item(span = { GridItemSpan(2) }) {
            NotchedCard(
                gradient = dailyGradient,
                modifier = Modifier.fillMaxWidth(),
                onClick = onDaily,
                minHeight = 208.dp,
                verticalArrangement = Arrangement.SpaceBetween,
                decoration = { Bubble(190.dp, 56.dp, 56.dp, 0.22f) },
                tab = { Text("$goal", style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink) },
            ) {
                Column {
                    Text("Daily set", style = MaterialTheme.typography.headlineSmall, color = StudyTheme.colors.ink, modifier = Modifier.fillMaxWidth(0.6f))
                    Text("$goal fresh questions · about ${goal * 3} min", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink)
                }
                PillButton("Start", onClick = onDaily, containerColor = dailyGradient.deep, modifier = Modifier.fillMaxWidth(0.55f))
            }
        }

        item {
            ModeCard("Review due", if (dueCount == 0) "Nothing due" else "Topics to revise", dueCount, 2, dueCount > 0, onDue)
        }
        item {
            ModeCard("Mistakes", if (mistakeCount == 0) "None yet" else "Questions you missed", mistakeCount, 3, mistakeCount > 0, onMistakes)
        }

        if (papers.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text("Previous year papers", style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink, modifier = Modifier.padding(top = 8.dp))
            }
            items(papers, key = PaperSummary::id) { paper ->
                PaperCard(paper, colorIndex = paper.title.takeLast(2).toIntOrNull() ?: 0, onClick = { onOpenPaper(paper) })
            }
        }

        item(span = { GridItemSpan(2) }) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Subjects", style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink)
                StudyTextField(query, { query = it }, "Search subjects", hintInside = true)
            }
        }
        if (visible.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text("No subject matches \"$query\".", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.muted)
            }
        }
        items(visible, key = SubjectSummary::id) { subject ->
            SubjectCard(subject, minHeight = 184.dp, onClick = { onOpenSubject(subject.id) })
        }
    }
}

@Composable
private fun PaperCard(paper: PaperSummary, colorIndex: Int, onClick: () -> Unit) {
    NotchedCard(
        gradient = StudyTheme.subjectGradient(colorIndex),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        minHeight = 130.dp,
        verticalArrangement = Arrangement.SpaceBetween,
        decoration = { Bubble(96.dp, 30.dp, 30.dp, 0.2f) },
        tab = { Text("${paper.questionIds.size}", style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink) },
    ) {
        Column {
            Spacer(Modifier.height(24.dp))
            Text(paper.title, style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink)
        }
        Text(
            (if (paper.isRecall) "Recall · " else "") + "${paper.answered} of ${paper.questionIds.size} done",
            style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink,
        )
    }
}

@Composable
private fun ModeCard(title: String, subtitle: String, count: Int, colorIndex: Int, enabled: Boolean, onClick: () -> Unit) {
    NotchedCard(
        gradient = StudyTheme.subjectGradient(colorIndex),
        modifier = Modifier.fillMaxWidth().dimmedIf(!enabled),
        onClick = if (enabled) onClick else null,
        minHeight = 160.dp,
        verticalArrangement = Arrangement.SpaceBetween,
        decoration = { Bubble(110.dp, 36.dp, 36.dp, 0.2f) },
        tab = { Text("$count", style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink) },
    ) {
        Column {
            Spacer(Modifier.height(28.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, color = StudyTheme.colors.ink)
        }
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink)
    }
}

@Composable
private fun BoxScope.Bubble(size: androidx.compose.ui.unit.Dp, x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp, alpha: Float) {
    Box(Modifier.align(Alignment.BottomEnd).offset(x = x, y = y).size(size).clip(CircleShape).background(Color.White.copy(alpha = alpha)))
}

private fun Modifier.androidClickable(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick))
