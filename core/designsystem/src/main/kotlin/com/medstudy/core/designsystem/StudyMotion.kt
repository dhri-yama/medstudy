package com.medstudy.core.designsystem

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * Motion tokens (DESIGN.md section 3.4). Motion in MedStudy is feedback on something the user did: small, fast, and
 * never something that has to finish before the next tap.
 */
object StudyMotion {
    /** Press feedback and option selection. */
    const val Fast = 120

    /** Answer reveal, progress fill, fades. */
    const val Standard = 200

    /** Progress fill and number changes. */
    const val Emphasis = 250

    /** Screen-level enter; exits use [Fast]. */
    const val Screen = 250

    val EaseOut: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** How far a pressable shrinks while held. */
    const val PressScale = 0.97f

    /** Release after a press. */
    val pressSpring: AnimationSpec<Float> = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow * 4)

    /** The one bouncy spec: bookmark toggle and streak change only. */
    val popSpring: AnimationSpec<Float> = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)

    fun <T> fast(): FiniteAnimationSpec<T> = tween(Fast, easing = EaseOut)
    fun <T> standard(): FiniteAnimationSpec<T> = tween(Standard, easing = EaseOut)
    fun <T> emphasis(): FiniteAnimationSpec<T> = tween(Emphasis, easing = EaseOut)
}

/**
 * True when the system "Remove animations" setting is on (animator duration scale 0). Movement (scale, slide, pop,
 * count-up) is skipped then; plain colour changes still happen, instantly.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Shrinks slightly while [interactionSource] is pressed and springs back on release. Put it before `clip`. */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, enabled: Boolean = true): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val reduced = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) StudyMotion.PressScale else 1f,
        animationSpec = when {
            reduced -> snap()
            pressed -> StudyMotion.fast()
            else -> StudyMotion.pressSpring
        },
        label = "pressScale",
    )
    return if (reduced) this else graphicsLayer { scaleX = scale; scaleY = scale }
}
