package com.medstudy.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medstudy.core.designsystem.FilterPill
import com.medstudy.core.designsystem.IconCircleButton
import com.medstudy.core.designsystem.NotchedCard
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.PillStyle
import com.medstudy.core.designsystem.StudyCard
import com.medstudy.core.designsystem.StudyChip
import com.medstudy.core.designsystem.StudyTextField
import com.medstudy.core.designsystem.TabSectionStyle
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.designsystem.SubjectGradient
import com.medstudy.core.designsystem.studySliderColors
import com.medstudy.core.model.pluralize

/** Numbers shown on the Me screen, derived from the learner's attempts. */
data class MeStats(val answered: Int, val accuracyPercent: Int?, val studyDays: Int, val streak: Int)

private val OverlayPresets = listOf(1, 5, 10, 15, 20)
private const val HeaderHeightDp = 196

@Composable
fun MeScreen(
    profile: UserProfile,
    stats: MeStats,
    onSaveProfile: (UserProfile) -> Unit,
    onProfileRestored: (UserProfile) -> Unit,
    account: SupabaseAccount,
    subscription: SubscriptionSummary,
    onSeePlans: () -> Unit,
    onManageSubscription: () -> Unit,
    onSignedOut: () -> Unit,
    onAccountDeleted: () -> Unit,
    overlayInterval: Int,
    overlayEnabled: Boolean,
    overlayAccessGranted: Boolean,
    onOverlayIntervalChange: (Float) -> Unit,
    onEnableOverlay: () -> Unit,
    onDisableOverlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    if (editing) {
        EditProfile(profile, onSave = { onSaveProfile(it); editing = false }, onBack = { editing = false }, modifier = modifier)
        return
    }
    val bottomInset = LocalNavBarInset.current
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        ProfileCard(profile, stats, onEdit = { editing = true }, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp))

        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SectionTitle("Subscription")
            StudyCard {
                Text(subscription.title, style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink)
                Text(subscription.detail, style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.body)
                if (subscription.paid) {
                    PillButton("Manage in Google Play", onClick = onManageSubscription, style = PillStyle.Secondary, modifier = Modifier.fillMaxWidth())
                } else {
                    PillButton("See plans", onClick = onSeePlans, modifier = Modifier.fillMaxWidth())
                }
            }

            SectionTitle("Account")
            StudyCard { AccountSection(account, profile, onProfileRestored, onSignedOut = onSignedOut, onAccountDeleted = onAccountDeleted) }

            SectionTitle("Reminders")
            ReminderCard()

            OverlayCard(
                interval = overlayInterval,
                enabled = overlayEnabled,
                accessGranted = overlayAccessGranted,
                onIntervalChange = onOverlayIntervalChange,
                onEnable = onEnableOverlay,
                onDisable = onDisableOverlay,
            )
            LegalLinks()
            Text(
                "MedStudy v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})" + if (BuildConfig.DEBUG) " · ${BuildConfig.GIT_SHA}" else "",
                style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(bottomInset))
        }
    }
}

/** The profile card: a colour banner, the avatar overlapping it, name and exam, three numbers and the edit button. */
@Composable
private fun ProfileCard(profile: UserProfile, stats: MeStats, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val a = StudyTheme.subjectGradient(5)
    val b = StudyTheme.subjectGradient(0)
    val c = StudyTheme.subjectGradient(1)
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(StudyTheme.colors.surface).padding(12.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(20.dp))
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(a.top, b.top, c.bottom))),
            )
            Box(
                Modifier.align(Alignment.BottomStart).offset(x = 14.dp, y = 38.dp).size(84.dp)
                    .clip(CircleShape).background(StudyTheme.colors.surface).padding(5.dp),
            ) { ProfileBlob(profile.name, size = 74.dp) }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 48.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(profile.name, style = TabSectionStyle.copy(fontSize = 24.sp, lineHeight = 30.sp), color = StudyTheme.colors.ink)
                Text("Joined in ${profile.joinedYear}", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.muted)
            }
            StudyChip(profile.examLabel, container = StudyTheme.colors.canvas)
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 14.dp).height(1.dp).background(StudyTheme.colors.hairline))
        Row(Modifier.fillMaxWidth()) {
            CardStat(Icons.Outlined.Quiz, "${stats.answered}", "Answered", Modifier.weight(1f))
            CardStat(Icons.Outlined.Percent, stats.accuracyPercent?.let { "$it%" } ?: "-", "Accuracy", Modifier.weight(1f))
            CardStat(Icons.Outlined.LocalFireDepartment, "${stats.streak}", "Day streak", Modifier.weight(1f))
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp).clip(CircleShape).background(StudyTheme.colors.ink)
                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "Edit profile", onClick = onEdit)
                .padding(start = 10.dp, end = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = StudyTheme.colors.ink, modifier = Modifier.size(20.dp))
            }
            Text(
                "Edit profile", style = MaterialTheme.typography.titleSmall, color = Color.White,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CardStat(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = StudyTheme.colors.ink, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), color = StudyTheme.colors.ink)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted)
    }
}

/** The overlay timer: a colored card with a large minutes number, -/+ buttons and quick presets. */
@Composable
private fun OverlayCard(
    interval: Int,
    enabled: Boolean,
    accessGranted: Boolean,
    onIntervalChange: (Float) -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
) {
    val gradient = StudyTheme.subjectGradient(7)
    NotchedCard(
        gradient = gradient,
        modifier = Modifier.fillMaxWidth(),
        minHeight = 280.dp,
        decoration = {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 60.dp, y = 60.dp).size(190.dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
            )
        },
        tab = { Text(if (enabled) "On" else "Off", style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.ink) },
    ) {
        Text("Question overlay", style = MaterialTheme.typography.headlineSmall, color = StudyTheme.colors.ink, modifier = Modifier.fillMaxWidth(0.6f))
        Text(
            "A random question pops up over other apps after your phone stays unlocked this long.",
            style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.ink, modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconCircleButton(Icons.Outlined.Remove, "Decrease minutes", { onIntervalChange((interval - 1).coerceAtLeast(1).toFloat()) })
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$interval", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 64.sp, lineHeight = 68.sp), color = StudyTheme.colors.ink)
                Text(" min", style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink, modifier = Modifier.padding(bottom = 10.dp))
            }
            IconCircleButton(Icons.Outlined.Add, "Increase minutes", { onIntervalChange((interval + 1).coerceAtMost(20).toFloat()) })
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            OverlayPresets.forEach { preset ->
                FilterPill("$preset", selected = interval == preset, onClick = { onIntervalChange(preset.toFloat()) })
            }
        }
        if (enabled && !accessGranted) {
            Text(
                "Allow MedStudy to display over other apps to use this.",
                style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.error, modifier = Modifier.padding(top = 10.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        if (enabled) {
            PillButton("Turn off overlay", onClick = onDisable, style = PillStyle.Secondary, modifier = Modifier.fillMaxWidth())
        } else {
            PillButton("Turn on overlay", onClick = onEnable, containerColor = gradient.deep, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun EditProfile(profile: UserProfile, onSave: (UserProfile) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var name by rememberSaveable(profile) { mutableStateOf(profile.name) }
    var exams by rememberSaveable(profile) { mutableStateOf(profile.exams) }
    var examDate by rememberSaveable(profile) { mutableStateOf(profile.examDateIso) }
    var pickingExamDate by rememberSaveable { mutableStateOf(false) }
    val edited = profile.copy(name = name.trim(), exams = exams, examDateIso = examDate)
    if (pickingExamDate) {
        ExamDatePickerDialog(examDate, onPick = { examDate = it; pickingExamDate = false }, onDismiss = { pickingExamDate = false })
    }
    Column(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Edit profile", onBack)
        StudyCard {
            StudyTextField(name, { name = it }, "Name")
            Text("Preparing for", style = MaterialTheme.typography.labelLarge, color = StudyTheme.colors.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserProfileStore.EXAMS.forEach { option ->
                    FilterPill(option, option in exams, { exams = if (option in exams) exams - option else exams + option })
                }
            }
            Text("Exam date", style = MaterialTheme.typography.labelLarge, color = StudyTheme.colors.muted)
            PillButton(examDateLabel(examDate), onClick = { pickingExamDate = true }, style = PillStyle.Secondary)
            PillButton(
                "Save changes", onClick = { onSave(edited) },
                enabled = edited != profile && edited.name.isNotBlank() && exams.isNotEmpty(), modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(LocalNavBarInset.current))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = TabSectionStyle, color = StudyTheme.colors.ink)
}
