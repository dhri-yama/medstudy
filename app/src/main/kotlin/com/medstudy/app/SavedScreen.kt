package com.medstudy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medstudy.core.designsystem.FilterPill
import com.medstudy.core.designsystem.IconCircleButton
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.StudyChip
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.designsystem.TabHeader
import com.medstudy.core.designsystem.bleed
import com.medstudy.core.model.ContentBlock
import com.medstudy.core.model.Question
import com.medstudy.core.model.pluralize
import com.medstudy.core.model.subjectDisplayName

/** Bookmarked questions: a back button and big title, a count, then one white card per question with its subject. */
@Composable
fun SavedScreen(
    questions: List<Question>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onOpen: (Question) -> Unit,
    modifier: Modifier = Modifier,
) {
    val subjects = questions.map { it.subjectId }.distinct().sortedBy(::subjectDisplayName)
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    // A subject whose last saved question was removed falls back to All.
    val filter = selected?.takeIf { it in subjects }
    val shown = if (filter == null) questions else questions.filter { it.subjectId == filter }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 20.dp + LocalNavBarInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.bleed(20.dp)) {
                TabHeader("Saved\nquestions", avatar = { IconCircleButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack) })
                if (!isLoading && questions.isNotEmpty()) {
                    Text(
                        pluralize(shown.size, "question"),
                        style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.muted,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        }
        // Only worth showing when the saved questions span more than one subject.
        if (!isLoading && subjects.size > 1) {
            item {
                LazyRow(
                    Modifier.bleed(20.dp), contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { FilterPill("All", filter == null, { selected = null }) }
                    items(subjects, key = { it }) { id -> FilterPill(subjectDisplayName(id), filter == id, { selected = id }) }
                }
            }
        }
        when {
            isLoading -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StudyTheme.colors.ink)
                }
            }
            questions.isEmpty() -> item { EmptySaved(onBack) }
            else -> items(shown, key = Question::id) { question -> SavedCard(question, onClick = { onOpen(question) }) }
        }
    }
}

@Composable
private fun SavedCard(question: Question, onClick: () -> Unit) {
    val gradient = StudyTheme.subjectGradient(question.subjectId.hashCode().let { if (it < 0) -it else it })
    val text = question.content.filterIsInstance<ContentBlock.Text>().firstOrNull()?.text.orEmpty()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(StudyTheme.colors.surface)
            .border(1.dp, StudyTheme.colors.hairline, RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(gradient.brush), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Bookmark, contentDescription = null, tint = gradient.deep, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, style = MaterialTheme.typography.titleSmall, color = StudyTheme.colors.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
            StudyChip(subjectDisplayName(question.subjectId), container = StudyTheme.colors.canvas)
        }
    }
}

@Composable
private fun EmptySaved(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val gradient = StudyTheme.subjectGradient(0)
        Box(Modifier.size(72.dp).clip(CircleShape).background(gradient.brush), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Bookmark, contentDescription = null, tint = gradient.deep, modifier = Modifier.size(34.dp))
        }
        Text("Nothing saved yet", style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink)
        Text(
            "Tap the bookmark on a question while you practise and it will wait for you here.",
            style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.muted, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        PillButton("Back to practice", onClick = onBack, modifier = Modifier.padding(top = 4.dp))
    }
}
