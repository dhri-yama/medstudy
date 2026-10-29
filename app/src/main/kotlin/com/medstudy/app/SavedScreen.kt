package com.medstudy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.designsystem.TaskRow
import com.medstudy.core.model.ContentBlock
import com.medstudy.core.model.Question
import com.medstudy.core.model.pluralize
import com.medstudy.core.model.subjectDisplayName

@Composable
fun SavedScreen(
    questions: List<Question>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onOpen: (Question) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues20(LocalNavBarInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Saved", onBack, subtitle = if (isLoading) null else pluralize(questions.size, "question")) }
        when {
            isLoading -> item { CircularProgressIndicator(color = StudyTheme.colors.ink, modifier = Modifier.padding(32.dp)) }
            questions.isEmpty() -> item {
                Text(
                    "Bookmark a question while practising and it shows up here.",
                    style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.muted,
                )
            }
            else -> items(questions, key = Question::id) { question ->
                TaskRow(
                    title = question.content.filterIsInstance<ContentBlock.Text>().firstOrNull()?.text.orEmpty(),
                    caption = subjectDisplayName(question.subjectId),
                    onClick = { onOpen(question) },
                    leading = { Icon(Icons.Outlined.Bookmark, contentDescription = null, tint = StudyTheme.colors.ink) },
                )
            }
        }
    }
}
