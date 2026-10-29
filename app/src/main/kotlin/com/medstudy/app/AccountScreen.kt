package com.medstudy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.PillStyle
import com.medstudy.core.designsystem.StudyTextField
import com.medstudy.core.designsystem.StudyTheme
import kotlinx.coroutines.launch

@Composable
fun AccountSection(
    account: SupabaseAccount,
    profile: UserProfile,
    onProfileRestored: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
    onSignedOut: () -> Unit = {},
) {
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

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            !account.isConfigured -> Text("Cloud backup isn't configured in this build.", color = StudyTheme.colors.body, style = MaterialTheme.typography.bodyMedium)
            signedIn -> {
                Text("Signed in as ${account.email}", style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink)
                Text("Your answers are backed up to your account.", style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted)
                PillButton(
                    text = if (busy) "Syncing…" else "Sync now",
                    onClick = { run { account.pushProfile(profile); "Synced, ${account.sync()} new from the cloud" } },
                    enabled = !busy, modifier = Modifier.fillMaxWidth(),
                )
                PillButton("Sign out", onClick = { account.signOut(); signedIn = false; message = null; onSignedOut() }, style = PillStyle.Secondary, modifier = Modifier.fillMaxWidth())
            }
            else -> {
                Text("Sign in to back up your progress and use it on another phone.", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.body)
                StudyTextField(email, { email = it }, "Email", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                StudyTextField(
                    password, { password = it }, "Password (min 6 characters)",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                val ready = email.isNotBlank() && password.length >= 6 && !busy
                PillButton(
                    "Sign in",
                    onClick = { run { account.signIn(email.trim(), password); syncAll(account, profile, onProfileRestored); "Signed in" } },
                    enabled = ready, modifier = Modifier.fillMaxWidth(),
                )
                PillButton(
                    "Create account",
                    onClick = {
                        run {
                            if (account.signUp(email.trim(), password)) { syncAll(account, profile, onProfileRestored); "Account created" }
                            else "Check your email to confirm, then sign in"
                        }
                    },
                    enabled = ready, style = PillStyle.Secondary, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.body) }
    }
}

/** Pull the saved profile if the account has one (new phone), otherwise save this phone's; then sync attempts. */
private suspend fun syncAll(account: SupabaseAccount, profile: UserProfile, onProfileRestored: (UserProfile) -> Unit) {
    val saved = account.pullProfile()
    if (saved != null) onProfileRestored(saved) else account.pushProfile(profile)
    account.sync()
}
