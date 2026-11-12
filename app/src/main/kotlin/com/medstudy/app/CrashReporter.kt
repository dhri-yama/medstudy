package com.medstudy.app

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * Saves the app's last crash to a file; [SupabaseAccount.sync] uploads it to the crash_reports table and deletes it.
 * shortcut: one crash at a time, Kotlin/Java crashes only (no freezes or native crashes); move to Crashlytics or Sentry if that is needed.
 */
object CrashReporter {
    fun pendingFile(context: Context) = File(context.applicationContext.filesDir, "pending_crash.json")

    fun install(context: Context) {
        val file = pendingFile(context)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString().take(MAX_TRACE)
                file.writeText(
                    JSONObject()
                        .put("app_version", BuildConfig.VERSION_NAME)
                        .put("version_code", BuildConfig.VERSION_CODE)
                        .put("android_sdk", Build.VERSION.SDK_INT)
                        .put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                        .put("crashed_at", Instant.now().toString())
                        .put("stack_trace", trace)
                        .toString(),
                )
            }
            // Android's own handler still runs, so the app closes and Play's crash statistics see it as usual.
            previous?.uncaughtException(thread, error)
        }
    }

    /** Matches the length check on crash_reports.stack_trace. */
    private const val MAX_TRACE = 20_000
}
