package com.medstudy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.lightColorScheme

private val StudyColorScheme = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color.White,
    secondary = Color(0xFF3B82F6),
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF374151),
    outline = Color(0xFFE5E7EB),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = StudyColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Text(text = "MedStudy")
                }
            }
        }
    }
}
