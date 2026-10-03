package com.medstudy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import com.medstudy.core.designsystem.MedStudyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MedStudyTheme {
                StudyApp((application as MedStudyApplication).container)
            }
        }
    }
}
