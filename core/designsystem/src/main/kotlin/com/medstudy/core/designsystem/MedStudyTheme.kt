package com.medstudy.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object StudyPalette {
    val Canvas = Color(0xFFFFFFFF)
    val SoftSurface = Color(0xFFF8F9FA)
    val CardSurface = Color(0xFFF5F5F5)
    val Hairline = Color(0xFFE5E7EB)
    val Ink = Color(0xFF111111)
    val Body = Color(0xFF374151)
    val Muted = Color(0xFF6B7280)
    val AccentBlue = Color(0xFF3B82F6)
    val Success = Color(0xFF167A61)
    val Error = Color(0xFFB3261E)
}

private val LightStudyColors = lightColorScheme(
    primary = StudyPalette.Ink,
    onPrimary = StudyPalette.Canvas,
    secondary = StudyPalette.AccentBlue,
    onSecondary = StudyPalette.Canvas,
    background = StudyPalette.Canvas,
    onBackground = StudyPalette.Ink,
    surface = StudyPalette.Canvas,
    onSurface = StudyPalette.Ink,
    surfaceVariant = StudyPalette.CardSurface,
    onSurfaceVariant = StudyPalette.Body,
    outline = StudyPalette.Hairline,
    error = StudyPalette.Error,
    onError = StudyPalette.Canvas,
)

@Composable
fun MedStudyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightStudyColors,
        content = content,
    )
}
