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

enum class PaceStatus { ON_TRACK, BEHIND, NO_DATA }

/**
 * Where the learner stands against the exam: how many of the question bank they have covered, how many a day they would
 * need to cover the rest by exam day, and how many a day they have actually been answering over the last week.
 */
data class ExamPace(
    val daysLeft: Int,
    val total: Int,
    val covered: Int,
    val neededPerDay: Int,
    val recentPerDay: Int,
    val status: PaceStatus,
) {
    val remaining: Int get() = (total - covered).coerceAtLeast(0)
}

/** Null when no exam date is set (or it has passed). [attempts] is every answer, newest or oldest first. */
fun examPace(examDateIso: String?, today: LocalDate, totalQuestions: Int, attempts: List<com.medstudy.core.model.QuestionAttempt>): ExamPace? {
    val daysLeft = examDaysLeft(examDateIso, today) ?: return null
    val covered = attempts.map { it.questionId }.distinct().size.coerceAtMost(totalQuestions)
    val remaining = (totalQuestions - covered).coerceAtLeast(0)
    val needed = (remaining + maxOf(daysLeft, 1) - 1) / maxOf(daysLeft, 1)
    // ISO dates compare correctly as text. The last seven days including today.
    val from = today.minusDays(6).toString()
    val to = today.toString()
    val recentCount = attempts.count { it.studyDateIso in from..to }
    val recent = (recentCount + 3) / 7
    val status = when {
        attempts.isEmpty() -> PaceStatus.NO_DATA
        remaining == 0 || recent >= needed -> PaceStatus.ON_TRACK
        else -> PaceStatus.BEHIND
    }
    return ExamPace(daysLeft, totalQuestions, covered, needed, recent, status)
}
