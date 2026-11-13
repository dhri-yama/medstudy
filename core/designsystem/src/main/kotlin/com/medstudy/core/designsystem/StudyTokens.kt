package com.medstudy.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Vertical two-stop gradient used for a subject's cards, plus [deep]: a darker shade of the same hue used for the
 * progress bar and buttons inside that card. White text on [deep] is at least 5:1. See DESIGN.md section 3.2.
 */
@Immutable
data class SubjectGradient(val top: Color, val bottom: Color, val deep: Color) {
    val brush: Brush get() = Brush.verticalGradient(listOf(top, bottom))
}

/** Design tokens from DESIGN.md that Material's color roles cannot express. */
@Immutable
class StudyColors(
    val canvas: Color = Color(0xFFF6F6F8),
    val surface: Color = Color(0xFFFFFFFF),
    val ink: Color = Color(0xFF14142B),
    /** Screen and section headings: a deep indigo from the app's purple, so titles belong with the pastel cards and arcs. */
    val heading: Color = Color(0xFF2B2063),
    val body: Color = Color(0xFF3B3F51),
    val muted: Color = Color(0xFF5F6675),
    val hairline: Color = Color(0xFFE6E7EC),
    /** The action color: primary buttons, active nav, progress fill. Deliberately neutral so it sits with every pastel. */
    val accent: Color = Color(0xFF14142B),
    val accentSoft: Color = Color(0xFFE6E6EE),
    val onAccent: Color = Color(0xFFFFFFFF),
    val success: Color = Color(0xFF167A61),
    val successTint: Color = Color(0xFFDDF3EA),
    val error: Color = Color(0xFFB3261E),
    val errorTint: Color = Color(0xFFFBE4E2),
    val warning: Color = Color(0xFF9A6700),
    val warningTint: Color = Color(0xFFFFF1CC),
    val track: Color = Color(0xFFEBEBF0),
)

val StudySubjectGradients = listOf(
    SubjectGradient(Color(0xFFE9569F), Color(0xFFF59AC5), Color(0xFFA3226B)), // pink
    SubjectGradient(Color(0xFF8EC7F7), Color(0xFFC3E2FB), Color(0xFF1F5F99)), // blue
    SubjectGradient(Color(0xFF4FC384), Color(0xFF8FE0AE), Color(0xFF12744A)), // green
    SubjectGradient(Color(0xFFF5C93B), Color(0xFFF9DE72), Color(0xFF8A6200)), // yellow
    SubjectGradient(Color(0xFFA5D3F6), Color(0xFFCDE8FB), Color(0xFF2B6A9C)), // sky
    SubjectGradient(Color(0xFFB9A7F0), Color(0xFFD8CCF8), Color(0xFF6B3FA0)), // lavender
    SubjectGradient(Color(0xFFF7A66B), Color(0xFFFAC9A0), Color(0xFFA8450B)), // peach
    SubjectGradient(Color(0xFF5FCFC7), Color(0xFF9EE4DF), Color(0xFF0F766E)), // teal
)

internal val LocalStudyColors = staticCompositionLocalOf { StudyColors() }

object StudyTheme {
    val colors: StudyColors
        @Composable @ReadOnlyComposable get() = LocalStudyColors.current

    /** Same subject, same color on every screen: palette index is the subject's catalog position. */
    fun subjectGradient(position: Int): SubjectGradient =
        StudySubjectGradients[position.mod(StudySubjectGradients.size)]
}
