package com.medstudy.app

import android.app.Application

class MedStudyApplication : Application() {
    val container: StudyAppContainer by lazy { StudyAppContainer(this) }
}
