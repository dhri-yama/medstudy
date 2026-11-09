package com.medstudy.app

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medstudy.core.designsystem.FilterPill
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.PillStyle
import com.medstudy.core.designsystem.StudyAvatar
import com.medstudy.core.designsystem.StudyTextField
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.designsystem.studySliderColors

/** Local learner profile; also the user-id source for later sync. */
/** [joinedYear] is local only (not synced); it is set once, the first time the profile is saved. */
data class UserProfile(val name: String, val exams: List<String>, val dailyQuestionGoal: Int, val joinedYear: Int = java.time.LocalDate.now().year) {
    /** The first chosen exam; the one older app versions and the cloud's single exam column know about. */
    val primaryExam: String get() = exams.firstOrNull() ?: UserProfileStore.EXAMS.first()
    val examLabel: String get() = exams.joinToString(" · ")
}

class UserProfileStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("profile", Context.MODE_PRIVATE)

    fun load(): UserProfile? =
        if (!prefs.getBoolean(KEY_DONE, false)) null
        else UserProfile(
            prefs.getString(KEY_NAME, "").orEmpty(),
            prefs.getString(KEY_EXAM, EXAMS.first()).orEmpty().split(SEPARATOR).filter { it in EXAMS }.ifEmpty { listOf(EXAMS.first()) },
            prefs.getInt(KEY_GOAL, 10),
            prefs.getInt(KEY_JOINED, java.time.LocalDate.now().year),
        )

    fun save(profile: UserProfile) {
        prefs.edit().putString(KEY_NAME, profile.name).putString(KEY_EXAM, profile.exams.joinToString(SEPARATOR))
            .putInt(KEY_GOAL, profile.dailyQuestionGoal).putBoolean(KEY_DONE, true)
            .apply { if (!prefs.contains(KEY_JOINED)) putInt(KEY_JOINED, profile.joinedYear) }.apply()
    }

    companion object {
        /** App label -> exams.id in the cloud database. */
        val EXAM_IDS = mapOf("NEET PG" to "neet_pg", "AIIMS" to "aiims", "INI-CET" to "inicet")
        val EXAMS = EXAM_IDS.keys.toList()
        private const val SEPARATOR = ","
        private const val KEY_DONE = "onboarded"
        private const val KEY_NAME = "name"
        private const val KEY_EXAM = "exam"
        private const val KEY_GOAL = "goal"
        private const val KEY_JOINED = "joined"
    }
}

private val ExamDescriptions = mapOf(
    "NEET PG" to "National postgraduate medical entrance",
    "AIIMS" to "AIIMS postgraduate entrance",
    "INI-CET" to "Institutes of National Importance entrance",
)

private const val NameStep = 1
private const val ExamsStep = 2
private const val LastStep = 4
private const val AccountStep = LastStep
private val StepTitles = listOf(
    "Your PG entrance prep, made simple", "Hi! What should we call you?", "What are you preparing for?", "How many questions a day?", "Create your account",
)
private val StepSubtitles = listOf(
    "Welcome to MedStudy", "Let's get to know you", "Pick one or more, we'll shape your plan around them", "Start small, you can change it in Me",
    "Your 15-day free trial starts now",
)
private val StepColors = listOf(1, 0, 2, 3, 5)

/**
 * Full-screen onboarding: each step is a screen-sized pastel gradient that fades into the next, with
 * floating circles that drift, a live greeting, tappable exam cards and quick presets for the goal.
 */
@Composable
fun OnboardingScreen(account: SupabaseAccount, existingProfile: UserProfile? = null, onDone: (UserProfile) -> Unit) {
    // An existing user who just isn't signed in yet goes straight to the account step with their profile filled in.
    var step by rememberSaveable { mutableIntStateOf(if (existingProfile != null) AccountStep else 0) }
    var name by rememberSaveable { mutableStateOf(existingProfile?.name.orEmpty()) }
    // Kept in the order the learner picked them; the first one is the primary exam.
    var exams by rememberSaveable { mutableStateOf(existingProfile?.exams ?: emptyList()) }
    var goal by rememberSaveable { mutableFloatStateOf((existingProfile?.dailyQuestionGoal ?: 10).toFloat()) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var signInMode by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val gradient = StudyTheme.subjectGradient(StepColors[step])
    val top by animateColorAsState(gradient.top, tween(600), label = "top")
    val bottom by animateColorAsState(gradient.bottom, tween(600), label = "bottom")
    val deep by animateColorAsState(gradient.deep, tween(600), label = "deep")
    val canContinue = when (step) {
        NameStep -> name.isNotBlank()
        ExamsStep -> exams.isNotEmpty()
        AccountStep -> email.isNotBlank() && password.length >= 6 && !busy
        else -> true
    }

    fun submitAccount() {
        scope.launch {
            busy = true
            error = null
            try {
                val local = UserProfile(name.trim().ifBlank { email.substringBefore('@') }, exams.ifEmpty { listOf(UserProfileStore.EXAMS.first()) }, goal.toInt())
                if (signInMode) {
                    account.signIn(email.trim(), password)
                    val saved = account.pullProfile()
                    runCatching { account.sync() }
                    onDone(saved ?: local)
                } else if (account.signUp(email.trim(), password)) {
                    account.pushProfile(local)
                    runCatching { account.sync() }
                    onDone(local)
                } else {
                    error = "Check your email to confirm your account, then sign in."
                    signInMode = true
                }
            } catch (e: Exception) {
                error = e.message ?: "Something went wrong. Try again."
            } finally {
                busy = false
            }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    fun continueWithGoogle() {
        scope.launch {
            busy = true
            error = null
            try {
                val token = GoogleSignIn.request(context)
                if (token != null) {
                    val googleName = account.signInWithGoogle(token.idToken, token.rawNonce) ?: token.displayName
                    // An account that already has a saved profile keeps it; a brand-new one uses what was chosen here.
                    val saved = account.pullProfile()
                    val local = UserProfile(
                        name.trim().ifBlank { googleName ?: email.substringBefore('@') }.ifBlank { "Learner" },
                        exams.ifEmpty { listOf(UserProfileStore.EXAMS.first()) }, goal.toInt(),
                    )
                    if (saved == null) account.pushProfile(local)
                    runCatching { account.sync() }
                    onDone(saved ?: local)
                }
            } catch (e: Exception) {
                error = e.message ?: "Something went wrong. Try again."
            } finally {
                busy = false
            }
        }
    }

    fun next() {
        if (!canContinue) return
        if (step == AccountStep) submitAccount() else step += 1
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom)))) {
        Bubbles(step)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Step ${step + 1} of ${LastStep + 1}" },
            ) {
                repeat(LastStep + 1) { index ->
                    Spacer(
                        Modifier.weight(1f).height(6.dp).clip(CircleShape)
                            .background(if (index <= step) deep else Color.White.copy(alpha = 0.55f)),
                    )
                }
            }

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(400)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(400))) togetherWith
                        (slideOutHorizontally(tween(300)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(200)))
                },
                label = "step",
            ) { current ->
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            StepSubtitles[current], style = MaterialTheme.typography.titleSmall, color = StudyTheme.colors.ink,
                        )
                        Text(
                            StepTitles[current],
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp),
                            color = StudyTheme.colors.ink,
                        )
                    }
                    when (current) {
                        0 -> IntroStep(onHaveAccount = { signInMode = true; step = AccountStep })
                        NameStep -> NameStep(name, { name = it }, onDone = ::next)
                        ExamsStep -> ExamStep(exams, deep) { picked ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            exams = if (picked in exams) exams - picked else exams + picked
                        }
                        3 -> GoalStep(goal, { goal = it }, name, exams) { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
                        else -> AccountFields(
                            email, { email = it }, password, { password = it }, signInMode,
                            onToggleMode = { signInMode = !signInMode; error = null }, error = error, onSubmit = ::next,
                            onGoogle = ::continueWithGoogle, busy = busy,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (step > 0 && existingProfile == null) PillButton("Back", onClick = { step -= 1 }, style = PillStyle.Secondary)
                PillButton(
                    text = when {
                        step == 0 -> "Get started"
                        step != AccountStep -> "Next"
                        busy -> "Please wait…"
                        signInMode -> "Sign in"
                        else -> "Start free trial"
                    },
                    onClick = ::next,
                    enabled = canContinue,
                    containerColor = deep,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Three soft circles that glide to new spots on each step so the background feels alive. */
@Composable
private fun Bubbles(step: Int) {
    val spec = tween<androidx.compose.ui.unit.Dp>(900)
    val ax by animateDpAsState(listOf(70.dp, 20.dp, 110.dp, 40.dp, 50.dp)[step], spec, label = "ax")
    val ay by animateDpAsState(listOf(-60.dp, 40.dp, -20.dp, 20.dp, 10.dp)[step], spec, label = "ay")
    val bx by animateDpAsState(listOf(-90.dp, -30.dp, -120.dp, -70.dp, -60.dp)[step], spec, label = "bx")
    val by by animateDpAsState(listOf(120.dp, 60.dp, 150.dp, 100.dp, 90.dp)[step], spec, label = "by")
    val cx by animateDpAsState(listOf(40.dp, 90.dp, 10.dp, 70.dp, 60.dp)[step], spec, label = "cx")
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = ax, y = ay).size(240.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)))
        Box(Modifier.align(Alignment.BottomStart).offset(x = bx, y = -by).size(280.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
        Box(Modifier.align(Alignment.CenterEnd).offset(x = cx, y = 60.dp).size(120.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)))
    }
}

@Composable
private fun NameStep(name: String, onNameChange: (String) -> Unit, onDone: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        StudyTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Your name",
            modifier = Modifier.focusRequester(focus),
            hintInside = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onDone() }),
        )
        AnimatedVisibility(visible = name.isNotBlank(), enter = fadeIn(tween(250)), exit = fadeOut(tween(150))) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudyAvatar(name, size = 56.dp)
                Text("Hey, ${name.trim()}", style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink)
            }
        }
    }
}

@Composable
private fun ExamStep(selected: List<String>, deep: Color, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UserProfileStore.EXAMS.forEach { option ->
            val isSelected = option in selected
            val scale by animateFloatAsState(if (isSelected) 1.03f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "scale")
            val shape = RoundedCornerShape(24.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(shape)
                    .background(StudyTheme.colors.surface)
                    .border(if (isSelected) 2.dp else 0.dp, if (isSelected) deep else Color.Transparent, shape)
                    .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle(option) })
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(option, style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.ink)
                    Text(ExamDescriptions[option].orEmpty(), style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted)
                }
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(if (isSelected) deep else StudyTheme.colors.canvas),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** First screen: what the product is, in a few lines. Nothing is asked of the learner yet. */
@Composable
private fun IntroStep(onHaveAccount: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(
            "Thousands of exam-style questions" to "Every question comes with the reasoning behind the answer.",
            "Previous year papers" to "NEET PG, JIPMER and DNB papers, practised in their original order.",
            "A plan that fits your day" to "Set a daily goal, keep a streak and see what to revise next.",
        ).forEach { (title, body) ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.7f)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.body)
            }
        }
        Text(
            "I already have an account",
            style = MaterialTheme.typography.labelLarge, color = StudyTheme.colors.ink,
            modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onHaveAccount).padding(vertical = 8.dp, horizontal = 4.dp),
        )
    }
}

@Composable
private fun GoalStep(goal: Float, onGoalChange: (Float) -> Unit, name: String, exams: List<String>, onPreset: () -> Unit) {
    val value = goal.toInt()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedContent(
            targetState = value,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
            label = "goal",
        ) { n ->
            Text("$n", fontSize = 96.sp, lineHeight = 100.sp, color = StudyTheme.colors.ink, style = MaterialTheme.typography.headlineLarge)
        }
        Text(
            "questions a day, about ${value * 3} minutes",
            style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink,
        )
        Slider(value = goal, onValueChange = onGoalChange, valueRange = 5f..50f, steps = 8, colors = studySliderColors())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20, 30, 50).forEach { preset ->
                FilterPill("$preset", selected = value == preset, onClick = { onGoalChange(preset.toFloat()); onPreset() })
            }
        }
        Text(
            "${name.trim().ifBlank { "You" }}, preparing for ${exams.joinToString(", ")}.",
            style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink, modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AccountFields(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    signInMode: Boolean,
    onToggleMode: () -> Unit,
    error: String?,
    onSubmit: () -> Unit,
    onGoogle: () -> Unit,
    busy: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PillButton("Continue with Google", onClick = onGoogle, enabled = !busy, style = PillStyle.Secondary, modifier = Modifier.fillMaxWidth())
        Text("or use your email", style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink, modifier = Modifier.align(Alignment.CenterHorizontally))
        StudyTextField(
            email, onEmailChange, "Email", hintInside = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        StudyTextField(
            password, onPasswordChange, "Password (min 6 characters)", hintInside = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
        error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.error, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.8f)).padding(10.dp)) }
        if (!signInMode) {
            Text(
                "No card needed. After 15 days you can subscribe, or keep a limited free plan.",
                style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.ink,
            )
        }
        Text(
            if (signInMode) "New here? Create an account" else "Already have an account? Sign in",
            style = MaterialTheme.typography.labelLarge, color = StudyTheme.colors.ink,
            modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onToggleMode).padding(vertical = 8.dp, horizontal = 4.dp),
        )
    }
}
