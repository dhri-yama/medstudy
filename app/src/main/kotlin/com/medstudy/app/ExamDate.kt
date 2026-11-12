package com.medstudy.app

import android.app.DatePickerDialog
import android.content.DialogInterface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Whole days from [today] to the exam; null when no date is set or it has passed. */
fun examDaysLeft(examDateIso: String?, today: LocalDate): Int? =
    examDateIso?.let { runCatching { ChronoUnit.DAYS.between(today, LocalDate.parse(it)).toInt() }.getOrNull() }?.takeIf { it >= 0 }

fun examCountdownLabel(daysLeft: Int): String = when (daysLeft) {
    0 -> "Exam today"
    1 -> "1 day to exam"
    else -> "$daysLeft days to exam"
}

fun examDateLabel(examDateIso: String?): String =
    examDateIso?.let { runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("d MMM yyyy")) }.getOrNull() } ?: "Not set"

/**
 * Picks the exam day with Android's own date dialog. [onPick] gets yyyy-MM-dd, or null when the learner removes the date.
 * (Material's Compose date picker is not used: it crashes with the Compose version this app resolves at run time.)
 */
@Composable
fun ExamDatePickerDialog(currentIso: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val start = currentIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now().plusMonths(3)
        val dialog = DatePickerDialog(
            context, { _, year, month, day -> onPick(LocalDate.of(year, month + 1, day).toString()) },
            start.year, start.monthValue - 1, start.dayOfMonth,
        )
        dialog.datePicker.minDate = System.currentTimeMillis()
        if (currentIso != null) dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Remove date") { _, _ -> onPick(null) }
        dialog.setOnCancelListener { onDismiss() }
        dialog.show()
        onDispose { dialog.dismiss() }
    }
}
