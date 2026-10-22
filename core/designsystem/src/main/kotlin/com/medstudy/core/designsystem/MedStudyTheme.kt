package com.medstudy.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val tokens = StudyColors()

// Material roles mapped to the tokens so stock Material components inherit the right look.
private val LightStudyColors = lightColorScheme(
    primary = tokens.accent,
    onPrimary = tokens.onAccent,
    primaryContainer = tokens.accentSoft,
    onPrimaryContainer = tokens.ink,
    secondary = tokens.accent,
    onSecondary = tokens.onAccent,
    secondaryContainer = tokens.successTint,
    onSecondaryContainer = tokens.ink,
    background = tokens.canvas,
    onBackground = tokens.ink,
    surface = tokens.surface,
    onSurface = tokens.ink,
    surfaceVariant = tokens.track,
    onSurfaceVariant = tokens.muted,
    outline = tokens.hairline,
    outlineVariant = tokens.hairline,
    error = tokens.error,
    onError = tokens.onAccent,
    errorContainer = tokens.errorTint,
    onErrorContainer = tokens.ink,
)

private val StudyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// DESIGN.md section 3.3. System sans for now; weights 400, 500, 600 only.
private val StudyTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun MedStudyTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStudyColors provides tokens) {
        MaterialTheme(
            colorScheme = LightStudyColors,
            shapes = StudyShapes,
            typography = StudyTypography,
            content = content,
        )
    }
}
