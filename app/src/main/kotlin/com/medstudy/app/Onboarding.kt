package com.medstudy.app

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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

/** Local learner profile; also the user-id source for later sync. */
data class UserProfile(val name: String, val exam: String, val dailyQuestionGoal: Int)

class UserProfileStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("profile", Context.MODE_PRIVATE)

    fun load(): UserProfile? =
        if (!prefs.getBoolean(KEY_DONE, false)) null
        else UserProfile(
            prefs.getString(KEY_NAME, "").orEmpty(),
            prefs.getString(KEY_EXAM, EXAMS.first()).orEmpty(),
            prefs.getInt(KEY_GOAL, 10),
        )

    fun save(profile: UserProfile) {
        prefs.edit().putString(KEY_NAME, profile.name).putString(KEY_EXAM, profile.exam)
            .putInt(KEY_GOAL, profile.dailyQuestionGoal).putBoolean(KEY_DONE, true).apply()
    }

    companion object {
        val EXAMS = listOf("NEET PG", "AIIMS", "INI-CET")
        private const val KEY_DONE = "onboarded"
        private const val KEY_NAME = "name"
        private const val KEY_EXAM = "exam"
        private const val KEY_GOAL = "goal"
    }
}

@Composable
fun OnboardingScreen(onDone: (UserProfile) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var exam by rememberSaveable { mutableStateOf(UserProfileStore.EXAMS.first()) }
    var goal by rememberSaveable { mutableFloatStateOf(10f) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Welcome to MedStudy", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "Two quick questions so we can set your daily target.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Preparing for", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UserProfileStore.EXAMS.forEach { option ->
                FilterChip(selected = exam == option, onClick = { exam = option }, label = { Text(option) })
            }
        }
        Text("${goal.toInt()} questions a day", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 20.dp))
        Slider(value = goal, onValueChange = { goal = it }, valueRange = 5f..50f, steps = 8)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onDone(UserProfile(name.trim(), exam, goal.toInt())) },
            enabled = name.isNotBlank(),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Get started") }
    }
}
