package com.medstudy.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The top of a tab page: an avatar with a greeting and a slim bar for today's goal, an action on the right, then a
 * big two-line serif title with an optional round action beside it.
 */
@Composable
fun TabHeader(
    title: String,
    modifier: Modifier = Modifier,
    greeting: String? = null,
    /** Today's progress toward the daily goal, 0..1; drawn as a slim bar under the greeting. */
    progress: Float? = null,
    avatar: (@Composable () -> Unit)? = null,
    topAction: (@Composable () -> Unit)? = null,
    titleAction: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 6.dp)) {
        if (greeting != null || avatar != null || topAction != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                avatar?.invoke()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (greeting != null) Text(greeting, style = TabGreetingStyle, color = StudyTheme.colors.ink, maxLines = 1)
                    if (progress != null) {
                        Box(Modifier.fillMaxWidth(0.8f).height(6.dp).clip(CircleShape).background(StudyTheme.colors.hairline)) {
                            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(6.dp).clip(CircleShape).background(StudyTheme.colors.ink))
                        }
                    }
                }
                topAction?.invoke()
            }
        }
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title, style = TabTitleStyle, color = StudyTheme.colors.ink,
                modifier = Modifier.weight(1f),
            )
            titleAction?.invoke()
        }
    }
}

/** A tappable pastel tile: an icon chip and label on top, a big number below, an arrow circle in the corner. */
@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    gradient: SubjectGradient,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier.alpha(if (enabled) 1f else 0.55f).clip(RoundedCornerShape(24.dp)).background(gradient.brush)
            .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(StudyTheme.colors.surface), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = StudyTheme.colors.ink, modifier = Modifier.size(18.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, color = StudyTheme.colors.ink, maxLines = 1)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 38.sp), color = StudyTheme.colors.ink, modifier = Modifier.weight(1f))
            if (enabled) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(StudyTheme.colors.surface.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = StudyTheme.colors.ink, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
