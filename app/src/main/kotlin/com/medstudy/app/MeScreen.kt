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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MeScreen(
    profile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onProfileRestored: (UserProfile) -> Unit,
    account: SupabaseAccount,
    overlayInterval: Int,
    overlayEnabled: Boolean,
    overlayAccessGranted: Boolean,
    onOverlayIntervalChange: (Float) -> Unit,
    onEnableOverlay: () -> Unit,
    onDisableOverlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Me", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)

        SectionTitle("Profile")
        ProfileEditor(profile, onSaveProfile)

        HorizontalDivider()
        SectionTitle("Backup and sync")
        AccountSection(account, profile, onProfileRestored)

        HorizontalDivider()
        SectionTitle("Question overlay")
        Text(
            "A random unattempted question appears above other apps after the phone stays unlocked for this long. Locking the phone resets the timer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Every $overlayInterval minutes", style = MaterialTheme.typography.labelLarge)
        Slider(value = overlayInterval.toFloat(), onValueChange = onOverlayIntervalChange, valueRange = 1f..20f, steps = 18)
        if (overlayEnabled && !overlayAccessGranted) {
            Text("Allow MedStudy to display over other apps to use this feature.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (overlayEnabled) {
            OutlinedButton(onClick = onDisableOverlay, modifier = Modifier.fillMaxWidth()) { Text("Turn off question overlay") }
        } else {
            OutlinedButton(onClick = onEnableOverlay, modifier = Modifier.fillMaxWidth()) { Text("Enable question overlay") }
        }
    }
}

@Composable
private fun ProfileEditor(profile: UserProfile, onSave: (UserProfile) -> Unit) {
    var name by rememberSaveable(profile) { mutableStateOf(profile.name) }
    var exam by rememberSaveable(profile) { mutableStateOf(profile.exam) }
    var goal by rememberSaveable(profile) { mutableFloatStateOf(profile.dailyQuestionGoal.toFloat()) }
    val edited = UserProfile(name.trim(), exam, goal.toInt())

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Preparing for", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UserProfileStore.EXAMS.forEach { option ->
                FilterChip(selected = exam == option, onClick = { exam = option }, label = { Text(option) })
            }
        }
        Text("${goal.toInt()} questions a day", style = MaterialTheme.typography.labelLarge)
        Slider(value = goal, onValueChange = { goal = it }, valueRange = 5f..50f, steps = 8)
        Button(
            onClick = { onSave(edited) },
            enabled = edited != profile && edited.name.isNotBlank(),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save changes") }
    }
}

@Composable
private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
