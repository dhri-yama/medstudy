package com.medstudy.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(account: SupabaseAccount, profile: UserProfile, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var signedIn by remember { mutableStateOf(account.isSignedIn) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun run(block: suspend () -> String?) {
        scope.launch {
            busy = true
            message = try { block() } catch (e: Exception) { e.message ?: "Something went wrong" }
            signedIn = account.isSignedIn
            busy = false
        }
    }

    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("Account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "${profile.name} · ${profile.exam} · ${profile.dailyQuestionGoal} questions a day",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )
        when {
            !account.isConfigured -> Text("Cloud backup isn't configured in this build.")
            signedIn -> {
                Text("Signed in as ${account.email}", style = MaterialTheme.typography.bodyLarge)
                Text("Your answers are backed up to your account.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Button(
                    onClick = { run { "Synced, ${account.sync()} new from the cloud" } },
                    enabled = !busy, shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) { Text(if (busy) "Syncing…" else "Sync now") }
                OutlinedButton(
                    onClick = { account.signOut(); signedIn = false; message = null },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Sign out") }
            }
            else -> {
                Text("Sign in to back up your progress and use it on another phone.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Password (min 6 characters)") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                val ready = email.isNotBlank() && password.length >= 6 && !busy
                Button(
                    onClick = { run { account.signIn(email.trim(), password); account.sync(); "Signed in" } },
                    enabled = ready, shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) { Text("Sign in") }
                OutlinedButton(
                    onClick = {
                        run {
                            if (account.signUp(email.trim(), password)) { account.sync(); "Account created" }
                            else "Check your email to confirm, then sign in"
                        }
                    },
                    enabled = ready, shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Create account") }
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp)) }
    }
}
