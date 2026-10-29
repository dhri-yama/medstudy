package com.medstudy.app

import com.medstudy.feature.progress.SubjectSummary
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.medstudy.core.designsystem.NotchedCard
import com.medstudy.core.designsystem.StudyProgressBar
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.model.pluralize

/** Subject card shared by the Today row and the Subjects grid. */
@Composable
fun SubjectCard(subject: SubjectSummary, modifier: Modifier = Modifier, minHeight: Dp = 0.dp, onClick: () -> Unit) {
    val gradient = StudyTheme.subjectGradient(subject.colorIndex)
    NotchedCard(
        gradient = gradient,
        modifier = modifier,
        onClick = onClick,
        minHeight = minHeight,
        verticalArrangement = if (minHeight > 0.dp) Arrangement.SpaceBetween else Arrangement.Top,
        decoration = if (minHeight > 0.dp) {
            {
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = 40.dp, y = 40.dp).size(120.dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                )
            }
        } else null,
        tab = { Text("${subject.answered}/${subject.questionCount}", style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink, maxLines = 1) },
    ) {
        Column {
            Spacer(Modifier.height(28.dp))
            Text(subject.name, style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink, maxLines = 2, minLines = 2)
            Text(pluralize(subject.questionCount, "question"), style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.ink)
        }
        Column {
            Spacer(Modifier.height(10.dp))
            StudyProgressBar(
                progress = if (subject.questionCount == 0) 0f else subject.answered.toFloat() / subject.questionCount,
                description = "${subject.answered} of ${subject.questionCount} ${subject.name} questions answered",
                fill = gradient.deep,
                track = Color.White.copy(alpha = 0.55f),
            )
            Text(
                subject.accuracyPercent?.let { "$it% correct" } ?: "Not started",
                style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.ink, modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
