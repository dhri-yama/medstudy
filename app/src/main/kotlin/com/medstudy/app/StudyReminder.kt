package com.medstudy.app

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.medstudy.core.designsystem.PillButton
import com.medstudy.core.designsystem.PillStyle
import com.medstudy.core.designsystem.StudyCard
import com.medstudy.core.designsystem.StudyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * A once-a-day nudge at a time the learner picks. It stays quiet on days the daily goal is already met.
 * The alarm is inexact (Android may deliver it a few minutes late), which needs no special permission.
 */
object StudyReminder {
    private const val PREFS = "reminder"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val CHANNEL_ID = "study_reminder"
    private const val NOTIFICATION_ID = 41
    const val ACTION_REMIND = "com.medstudy.app.REMIND"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context) = prefs(context).getBoolean(KEY_ENABLED, false)
    fun time(context: Context): LocalTime = LocalTime.of(prefs(context).getInt(KEY_HOUR, 19), prefs(context).getInt(KEY_MINUTE, 0))

    fun set(context: Context, enabled: Boolean, time: LocalTime) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).putInt(KEY_HOUR, time.hour).putInt(KEY_MINUTE, time.minute).apply()
        if (enabled) schedule(context) else alarms(context).cancel(pendingIntent(context))
    }

    /** Also called after a restart, because Android forgets alarms when the phone is switched off. */
    fun schedule(context: Context) {
        var next = ZonedDateTime.of(LocalDate.now(), time(context), ZoneId.systemDefault())
        if (!next.isAfter(ZonedDateTime.now())) next = next.plusDays(1)
        alarms(context).setInexactRepeating(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), AlarmManager.INTERVAL_DAY, pendingIntent(context))
    }

    fun show(context: Context, text: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Daily study reminder", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Without the notification permission (Android 13+) this is silently dropped.
        manager.notify(
            NOTIFICATION_ID,
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Time to practise")
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun alarms(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!StudyReminder.isEnabled(context)) return
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) { StudyReminder.schedule(context); return }
        if (intent.action != StudyReminder.ACTION_REMIND) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val goal = UserProfileStore(context).load()?.dailyQuestionGoal ?: return@launch
                val container = (context.applicationContext as MedStudyApplication).container
                val answered = container.learnerActivity.attemptsForDate(LocalDate.now().toString()).size
                when {
                    answered >= goal -> Unit
                    answered == 0 -> StudyReminder.show(context, "You haven't practised yet today. $goal questions keeps you on track.")
                    else -> StudyReminder.show(context, "${goal - answered} more to reach today's goal of $goal.")
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** The reminder's switch and time, for the Me screen. */
@Composable
fun ReminderCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(StudyReminder.isEnabled(context)) }
    var time by remember { mutableStateOf(StudyReminder.time(context)) }
    fun apply(on: Boolean, at: LocalTime = time) { enabled = on; time = at; StudyReminder.set(context, on, at) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) apply(true) }

    StudyCard(modifier) {
        Text("Daily reminder", style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink)
        Text(
            if (enabled) "A nudge at ${time.format(DateTimeFormatter.ofPattern("h:mm a"))} on days you haven't reached your goal."
            else "Get a nudge at a time you choose, on days you haven't reached your goal.",
            style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.body,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PillButton(
                "Time: ${time.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                onClick = { TimePickerDialog(context, { _, hour, minute -> apply(enabled, LocalTime.of(hour, minute)) }, time.hour, time.minute, false).show() },
                style = PillStyle.Secondary,
            )
            PillButton(
                if (enabled) "Turn off" else "Turn on",
                onClick = {
                    val needsPermission = Build.VERSION.SDK_INT >= 33 &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    when {
                        enabled -> apply(false)
                        needsPermission -> askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else -> apply(true)
                    }
                },
                style = if (enabled) PillStyle.Secondary else PillStyle.Primary, modifier = Modifier.weight(1f),
            )
        }
    }
}
