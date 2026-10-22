package com.medstudy.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Vertical two-stop gradient used for a subject's cards. See DESIGN.md section 3.2. */
@Immutable
data class SubjectGradient(val top: Color, val bottom: Color) {
    val brush: Brush get() = Brush.verticalGradient(listOf(top, bottom))
}

/** Design tokens from DESIGN.md that Material's color roles cannot express. */
@Immutable
class StudyColors(
    val canvas: Color = Color(0xFFF6F6F8),
    val surface: Color = Color(0xFFFFFFFF),
    val ink: Color = Color(0xFF14142B),
    val body: Color = Color(0xFF3B3F51),
    val muted: Color = Color(0xFF5F6675),
    val hairline: Color = Color(0xFFE6E7EC),
    val accent: Color = Color(0xFF5B5BD6),
    val accentSoft: Color = Color(0xFFD9D9F5),
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
    SubjectGradient(Color(0xFFE9569F), Color(0xFFF59AC5)), // pink
    SubjectGradient(Color(0xFF8EC7F7), Color(0xFFC3E2FB)), // blue
    SubjectGradient(Color(0xFF4FC384), Color(0xFF8FE0AE)), // green
    SubjectGradient(Color(0xFFF5C93B), Color(0xFFF9DE72)), // yellow
    SubjectGradient(Color(0xFFA5D3F6), Color(0xFFCDE8FB)), // sky
    SubjectGradient(Color(0xFFB9A7F0), Color(0xFFD8CCF8)), // lavender
    SubjectGradient(Color(0xFFF7A66B), Color(0xFFFAC9A0)), // peach
    SubjectGradient(Color(0xFF5FCFC7), Color(0xFF9EE4DF)), // teal
)

internal val LocalStudyColors = staticCompositionLocalOf { StudyColors() }

object StudyTheme {
    val colors: StudyColors
        @Composable @ReadOnlyComposable get() = LocalStudyColors.current

    /** Same subject, same color on every screen: palette index is the subject's catalog position. */
    fun subjectGradient(position: Int): SubjectGradient =
        StudySubjectGradients[position.mod(StudySubjectGradients.size)]
}
