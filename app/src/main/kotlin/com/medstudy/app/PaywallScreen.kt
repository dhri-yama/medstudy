package com.medstudy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medstudy.core.designsystem.ArcHeader
import com.medstudy.core.designsystem.IconCircleButton
import com.medstudy.core.designsystem.NotchedCard
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.StudyTheme

/** Shown until Google Play returns the localized price. Keep in sync with the Play Console price. */
private const val FALLBACK_PRICE = "₹99"

private val Perks = listOf(
    "Unlimited questions, every day",
    "Question pop-ups over other apps",
    "Smart weekly study plan",
    "Review due topics and Mistakes sessions",
)

/**
 * Shown when a free-tier user hits a locked feature or the daily limit, and from Me.
 * [trialDaysLeft] is null once the trial is over.
 */
@Composable
fun PaywallScreen(
    trialDaysLeft: Int?,
    price: String?,
    message: String?,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // One color theme on this page: the same lavender as the arc header, deep purple for checks and the button.
    val gradient = StudyTheme.subjectGradient(5)
    Column(modifier.fillMaxSize().background(StudyTheme.colors.canvas).verticalScroll(rememberScrollState())) {
        ArcHeader(height = 170.dp) {
            Box(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 20.dp, top = 12.dp)) {
                IconCircleButton(Icons.Outlined.Close, "Close", onDismiss)
            }
            Text(
                "Go unlimited", style = MaterialTheme.typography.headlineMedium, color = StudyTheme.colors.ink,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 22.dp),
            )
        }
        Column(Modifier.padding(horizontal = 20.dp).windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                if (trialDaysLeft != null) "You're on your free trial, $trialDaysLeft days left. Subscribe any time to keep everything."
                else "Your 15-day free trial has ended. Subscribe to unlock everything, or keep the free plan (${Entitlement.FREE_DAILY_QUESTIONS} questions a day).",
                style = MaterialTheme.typography.bodyLarge, color = StudyTheme.colors.body,
            )
            NotchedCard(
                gradient = gradient,
                modifier = Modifier.fillMaxWidth(),
                minHeight = 168.dp,
                verticalArrangement = Arrangement.SpaceBetween,
                decoration = {
                    Box(
                        Modifier.align(Alignment.BottomEnd).offset(x = 50.dp, y = 50.dp).size(160.dp)
                            .clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                    )
                },
                tab = { Text("Monthly", style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink) },
            ) {
                Text("MedStudy Plus", style = MaterialTheme.typography.headlineSmall, color = StudyTheme.colors.ink, modifier = Modifier.fillMaxWidth(0.6f))
                Column {
                    Text(
                        price ?: FALLBACK_PRICE,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp, lineHeight = 44.sp), color = StudyTheme.colors.ink,
                    )
                    Text("per month, cancel any time", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Perks.forEach { perk ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(gradient.deep), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text(perk, style = MaterialTheme.typography.bodyLarge, color = StudyTheme.colors.ink)
                    }
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.error) }
            PillButton("Subscribe", onClick = onSubscribe, containerColor = gradient.deep, modifier = Modifier.fillMaxWidth())
            Text(
                "Restore purchase", style = MaterialTheme.typography.labelLarge, color = StudyTheme.colors.ink, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(CircleShape).clickable(role = Role.Button, onClick = onRestore).padding(vertical = 12.dp),
            )
            Text(
                if (trialDaysLeft != null) "Not now" else "Continue with the free plan",
                style = MaterialTheme.typography.labelLarge, color = StudyTheme.colors.muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(CircleShape).clickable(role = Role.Button, onClick = onDismiss).padding(vertical = 12.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
