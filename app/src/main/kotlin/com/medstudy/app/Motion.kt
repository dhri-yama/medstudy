package com.medstudy.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import com.medstudy.core.designsystem.StudyMotion
import com.medstudy.core.designsystem.rememberReducedMotion
import kotlinx.coroutines.delay

/** The three things the app can show full-screen. */
enum class RootScreen { PAYWALL, MOCK_TEST, PRACTICE, TABS }

private val NoMotion: ContentTransform = EnterTransition.None togetherWith ExitTransition.None

/** Practice and the paywall rise in slightly over the tabs and drop away when left. */
fun rootTransition(from: RootScreen, to: RootScreen, reduced: Boolean): ContentTransform = when {
    reduced -> NoMotion
    to == RootScreen.TABS ->
        fadeIn(tween(StudyMotion.Standard, easing = StudyMotion.EaseOut)) togetherWith
            (fadeOut(tween(StudyMotion.Fast)) + slideOutVertically(tween(StudyMotion.Fast)) { it / 16 })
    else ->
        (fadeIn(tween(StudyMotion.Screen, easing = StudyMotion.EaseOut)) +
            slideInVertically(tween(StudyMotion.Screen, easing = StudyMotion.EaseOut)) { it / 16 }) togetherWith
            fadeOut(tween(StudyMotion.Fast))
}

/** Bottom-bar tabs fade through: the old one goes first, so two screens are never legible at once. */
fun tabTransition(reduced: Boolean): ContentTransform =
    if (reduced) NoMotion else fadeIn(tween(150, delayMillis = 60)) togetherWith fadeOut(tween(60))

/** Last value shown per counter, for the life of the process, so a screen can show what changed while it was away. */
private val lastShown = mutableMapOf<String, Int>()

/**
 * Returns the number to display for [value]. A count that went up while the screen was away (the streak or today's
 * answers, both earned inside practice) is shown at its old value first and then moved to the new one, so the
 * change is seen. A count that went down (new day) or is shown for the first time is not animated.
 */
@Composable
fun rememberCatchUp(key: String, value: Int): Int {
    var shown by remember(key) { mutableIntStateOf(lastShown[key]?.takeIf { it < value } ?: value) }
    LaunchedEffect(key, value) {
        if (shown < value) delay(350) // let the screen settle first
        shown = value
        lastShown[key] = value
    }
    return shown
}

/** A number (or a phrase containing it) that slides up to its new value when it increases. */
@Composable
fun SlidingCount(
    value: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    text: (Int) -> String = { "$it" },
) {
    val reduced = rememberReducedMotion()
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            if (reduced || targetState < initialState) NoMotion
            else (slideInVertically(StudyMotion.emphasis()) { it / 2 } + fadeIn(StudyMotion.emphasis())) togetherWith
                (slideOutVertically(StudyMotion.fast()) { -it / 2 } + fadeOut(StudyMotion.fast()))
        },
        label = "count",
    ) { shown -> Text(text(shown), style = style, color = color, maxLines = 1) }
}

private val StreakMilestones = setOf(7, 30, 100, 365)

/**
 * Scale for the streak flame: one small pop when [streak] goes up, a larger one with a haptic tick on a milestone day.
 * Stays at 1 otherwise.
 */
@Composable
fun rememberStreakPop(streak: Int): Float {
    val reduced = rememberReducedMotion()
    val haptics = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    var previous by remember { mutableIntStateOf(streak) }
    LaunchedEffect(streak) {
        val rose = streak > previous
        previous = streak
        if (!rose) return@LaunchedEffect
        val milestone = streak in StreakMilestones
        if (milestone) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!reduced) {
            scale.animateTo(if (milestone) 1.45f else 1.25f, StudyMotion.fast())
            scale.animateTo(1f, StudyMotion.popSpring)
        }
    }
    return scale.value
}
