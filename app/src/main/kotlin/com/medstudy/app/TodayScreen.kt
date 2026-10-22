package com.medstudy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medstudy.core.model.StudyBlock
import com.medstudy.core.model.pluralize
import com.medstudy.feature.progress.DUE_RETRIEVABILITY
import com.medstudy.feature.progress.WeakArea
import java.time.LocalTime

@Composable
fun TodayScreen(
    name: String,
    goal: Int,
    answeredToday: Int,
    streak: Int,
    todayBlock: StudyBlock?,
    dueAreas: List<WeakArea>,
    onStartDaily: () -> Unit,
    onStartBlock: () -> Unit,
    onReviewDue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("$greeting, $name", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)

        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Today's goal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                    if (streak > 0) Text("${pluralize(streak, "day")} streak", style = MaterialTheme.typography.labelLarge)
                }
                Text("$answeredToday of $goal questions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(
                    progress = { (answeredToday.toFloat() / goal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = onStartDaily, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(if (answeredToday >= goal) "Practice more" else "Start today's set")
                }
            }
        }

        if (todayBlock != null) {
            Section("From your plan") {
                Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(todayBlock.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${pluralize(todayBlock.questionTarget, "question")} · ${todayBlock.minuteTarget} min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = onStartBlock, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Study this topic") }
                    }
                }
            }
        }

        if (dueAreas.isNotEmpty()) {
            Section("Due for review") {
                dueAreas.take(3).forEach { area ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(area.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        // Fresh but inaccurate topics are due for being weak, not forgotten; say which.
                        val label = area.retrievability?.takeIf { it < DUE_RETRIEVABILITY }?.let { "~${(it * 100).toInt()}% recall" }
                            ?: area.accuracy?.let { "${(it * 100).toInt()}% correct" }
                        label?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                OutlinedButton(onClick = onReviewDue, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Text("Review due topics") }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}
