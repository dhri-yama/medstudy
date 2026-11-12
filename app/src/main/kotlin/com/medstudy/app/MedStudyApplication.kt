package com.medstudy.app

import android.app.Application

class MedStudyApplication : Application() {
    val container: StudyAppContainer by lazy { StudyAppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
    }
}
