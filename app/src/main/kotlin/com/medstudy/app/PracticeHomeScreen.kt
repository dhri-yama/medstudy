package com.medstudy.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PracticeHomeScreen(
    goal: Int,
    dueCount: Int,
    mistakeCount: Int,
    onDaily: () -> Unit,
    onDue: () -> Unit,
    onMistakes: () -> Unit,
    onSubjects: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Practice", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text("Pick how you want to study.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        ModeCard("Daily set", "$goal fresh questions", onDaily)
        ModeCard("Review due", if (dueCount == 0) "Nothing due right now" else "$dueCount topics to revise", onDue, enabled = dueCount > 0)
        ModeCard("Mistakes", if (mistakeCount == 0) "No missed questions yet" else "$mistakeCount questions you got wrong", onMistakes, enabled = mistakeCount > 0)
        ModeCard("Browse by subject", "Pick a subject or topic", onSubjects)
        ModeCard("Saved questions", "Questions you bookmarked", onSaved)
    }
}

@Composable
private fun ModeCard(title: String, subtitle: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
