package com.medstudy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medstudy.core.designsystem.NotchedCard
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.designsystem.dimmedIf

private data class ModeCard(
    val title: String,
    val subtitle: String,
    val tab: String?,
    val colorPosition: Int,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun PracticeHomeScreen(
    goal: Int,
    dueCount: Int,
    mistakeCount: Int,
    subjectCount: Int,
    onDaily: () -> Unit,
    onDue: () -> Unit,
    onMistakes: () -> Unit,
    onSubjects: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cards = listOf(
        ModeCard("Daily set", "$goal fresh questions", "$goal", 0, true, onDaily),
        ModeCard("Review due", if (dueCount == 0) "Nothing due" else "Topics to revise", dueCount.toString(), 2, dueCount > 0, onDue),
        ModeCard("Mistakes", if (mistakeCount == 0) "None yet" else "Questions you missed", mistakeCount.toString(), 3, mistakeCount > 0, onMistakes),
        ModeCard("Subjects", "Pick a subject or topic", subjectCount.toString(), 1, true, onSubjects),
        ModeCard("Saved", "Your bookmarks", null, 5, true, onSaved),
    )
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Practice", style = MaterialTheme.typography.headlineMedium, color = StudyTheme.colors.ink)
        Text(
            "Pick how you want to study.", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.muted,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        cards.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { card ->
                    NotchedCard(
                        gradient = StudyTheme.subjectGradient(card.colorPosition),
                        modifier = Modifier.weight(1f).dimmedIf(!card.enabled),
                        onClick = if (card.enabled) card.onClick else null,
                        tab = card.tab?.let { label -> { Text(label, style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink) } },
                    ) {
                        Spacer(Modifier.height(28.dp))
                        Text(card.title, style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink)
                        Text(card.subtitle, style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.ink, minLines = 2)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
