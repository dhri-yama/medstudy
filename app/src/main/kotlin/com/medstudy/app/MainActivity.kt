package com.medstudy.app

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.medstudy.core.designsystem.MedStudyTheme

class MainActivity : ComponentActivity() {
    private var notificationQuestionId by mutableStateOf<String?>(null)

    override fun onResume() {
        super.onResume()
        val preferences = getSharedPreferences(QuestionOverlayService.PREFS_NAME, MODE_PRIVATE)
        if (preferences.getBoolean(QuestionOverlayService.KEY_ENABLED, false) && Settings.canDrawOverlays(this)) {
            QuestionOverlayService.start(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationQuestionId = intent.getStringExtra(EXTRA_QUESTION_ID)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE),
        )
        setContent {
            MedStudyTheme {
                StudyApp(
                    container = (application as MedStudyApplication).container,
                    notificationQuestionId = notificationQuestionId,
                    onNotificationQuestionHandled = { notificationQuestionId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationQuestionId = intent.getStringExtra(EXTRA_QUESTION_ID)
    }

    companion object {
        const val EXTRA_QUESTION_ID = "com.medstudy.extra.QUESTION_ID"
    }
}
