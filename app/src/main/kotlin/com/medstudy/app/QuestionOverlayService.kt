package com.medstudy.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.medstudy.core.model.AttemptResult
import com.medstudy.core.model.PracticeContext
import com.medstudy.core.model.Question
import com.medstudy.core.model.QuestionAttempt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class QuestionOverlayService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var screenUnlocked = false
    private var unlockedAtElapsed = 0L
    private var overlay: View? = null
    private var receiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenUnlocked = false
                    unlockedAtElapsed = 0L
                    dismissOverlay()
                }
                Intent.ACTION_USER_PRESENT -> {
                    // Each unlock begins a fresh interval, even if an earlier interval had elapsed.
                    screenUnlocked = true
                    unlockedAtElapsed = SystemClock.elapsedRealtime()
                    dismissOverlay()
                    handler.removeCallbacks(tick)
                    handler.post(tick)
                }
            }
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!screenUnlocked) return
            val interval = preferences().getInt(KEY_INTERVAL, 10).coerceIn(1, 20) * 60_000L
            if (SystemClock.elapsedRealtime() - unlockedAtElapsed >= interval) {
                handler.removeCallbacks(this)
                showRandomQuestion()
            } else {
                handler.postDelayed(this, 1_000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            SERVICE_NOTIFICATION_ID,
            serviceNotification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(screenReceiver, filter)
        receiverRegistered = true
        val keyguard = getSystemService(KEYGUARD_SERVICE) as android.app.KeyguardManager
        if (!keyguard.isKeyguardLocked) {
            screenUnlocked = true
            unlockedAtElapsed = SystemClock.elapsedRealtime()
            handler.post(tick)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (receiverRegistered) unregisterReceiver(screenReceiver)
        dismissOverlay()
        scope.cancel()
        super.onDestroy()
    }

    private fun showRandomQuestion() {
        scope.launch {
            val container = (application as MedStudyApplication).container
            val chosen = withContext(Dispatchers.IO) {
                val attempted = container.learnerActivity.attempts().mapTo(mutableSetOf()) { it.questionId }
                val seen = preferences().getStringSet(KEY_SEEN_IDS, emptySet()).orEmpty()
                container.questionCatalog.questions().filterNot { it.id in attempted || it.id in seen }.randomOrNull()
            }
            if (!screenUnlocked) return@launch
            if (chosen == null) {
                postReminder("You’ve completed all available questions.")
                resetInterval()
            } else if (Settings.canDrawOverlays(this@QuestionOverlayService)) {
                rememberAsSeen(chosen.id)
                showQuestion(chosen)
            } else {
                rememberAsSeen(chosen.id)
                postReminder("A MedStudy question is ready. Tap to answer it.", chosen.id)
                resetInterval()
            }
        }
    }

    private fun showQuestion(question: Question) {
        if (overlay != null || !screenUnlocked) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
            setBackgroundColor(Color.WHITE)
            elevation = dp(12).toFloat()
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        val title = textView("MedStudy · Quick question", 18f, true)
        content.addView(title)
        question.content.forEach { block ->
            if (block is com.medstudy.core.model.ContentBlock.Text) content.addView(textView(block.text, 16f))
        }
        val choices = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        question.options.sortedBy { it.position }.forEach { option ->
            val label = option.content.filterIsInstance<com.medstudy.core.model.ContentBlock.Text>()
                .joinToString(" ") { it.text }
            choices.addView(RadioButton(this).apply {
                id = View.generateViewId()
                tag = option.id
                text = "${option.label}. $label"
                textSize = 15f
                setPadding(0, dp(6), 0, dp(6))
            })
        }
        content.addView(choices)
        val response = textView("", 14f)
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        val show = Button(this).apply { text = "Show answer" }
        val check = Button(this).apply { text = "Check answer"; isEnabled = false }
        actions.addView(show)
        actions.addView(check)
        panel.addView(scroll, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        panel.addView(response, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        panel.addView(actions, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= 26) LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") LayoutParams.TYPE_PHONE,
            LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.FILL
        }
        overlay = panel
        wm.addView(panel, params)
        choices.setOnCheckedChangeListener { group, checkedId -> check.isEnabled = checkedId != -1 }

        fun submit(showAnswer: Boolean) {
            val selectedId = if (showAnswer) null else choices.findViewById<RadioButton>(choices.checkedRadioButtonId)?.tag as? String
            check.isEnabled = false
            show.isEnabled = false
            scope.launch {
                val result = when {
                    showAnswer -> AttemptResult.UNANSWERED
                    selectedId != null && selectedId in question.answerKey.correctOptionIds -> AttemptResult.CORRECT
                    else -> AttemptResult.INCORRECT
                }
                val now = Instant.now()
                val zone = ZoneId.systemDefault()
                val saveResult = runCatching {
                    (application as MedStudyApplication).container.learnerActivity.recordAttempt(
                        QuestionAttempt(
                        UUID.randomUUID().toString(), question.id, listOfNotNull(selectedId), result,
                        now.toEpochMilli(), now.atZone(zone).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        zone.id, PracticeContext.TOPIC_PRACTICE,
                        ),
                    )
                }
                if (saveResult.isFailure) {
                    response.text = "Could not save your response. Please try again."
                    check.isEnabled = !showAnswer && choices.checkedRadioButtonId != -1
                    show.isEnabled = true
                    return@launch
                }
                response.text = buildString {
                    append(if (showAnswer) "Answer revealed. " else if (result == AttemptResult.CORRECT) "Correct. " else "Review the answer. ")
                    append(question.answerKey.explanation.ifBlank { "Answer: ${question.options.filter { it.id in question.answerKey.correctOptionIds }.joinToString { it.label }}" })
                }
                question.answerKey.optionRationales.forEach { rationale ->
                    val option = question.options.firstOrNull { it.id == rationale.optionId }
                    if (option != null && rationale.text.isNotBlank()) response.append("\n${option.label}: ${rationale.text}")
                }
                actions.removeAllViews()
                actions.addView(Button(this@QuestionOverlayService).apply {
                    text = "Done"
                    setOnClickListener { dismissOverlay(); resetInterval() }
                })
            }
        }
        check.setOnClickListener { submit(false) }
        show.setOnClickListener { submit(true) }
    }

    private fun resetInterval() {
        if (!screenUnlocked) return
        unlockedAtElapsed = SystemClock.elapsedRealtime()
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, 1_000L)
    }

    private fun dismissOverlay() {
        val current = overlay ?: return
        overlay = null
        runCatching { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(current) }
    }

    private fun postReminder(message: String, questionId: String? = null) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            questionId?.let { putExtra(MainActivity.EXTRA_QUESTION_ID, it) }
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MedStudy")
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(this, questionId?.hashCode() ?: 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(REMINDER_NOTIFICATION_ID, notification)
    }

    private fun serviceNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_info_details)
        .setContentTitle("MedStudy question reminders are on")
        .setContentText("A question will appear after each unlocked interval.")
        .setOngoing(true)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Question reminders", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    private fun textView(text: String, size: Float, bold: Boolean = false) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.rgb(25, 35, 45))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun preferences() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun rememberAsSeen(questionId: String) {
        val prefs = preferences()
        val seen = prefs.getStringSet(KEY_SEEN_IDS, emptySet()).orEmpty().toMutableSet()
        seen += questionId
        prefs.edit().putStringSet(KEY_SEEN_IDS, seen).apply()
    }

    companion object {
        const val ACTION_START = "com.medstudy.action.START_OVERLAY_QUESTIONS"
        const val ACTION_STOP = "com.medstudy.action.STOP_OVERLAY_QUESTIONS"
        const val PREFS_NAME = "question_overlay_settings"
        const val KEY_ENABLED = "enabled"
        const val KEY_INTERVAL = "interval_minutes"
        private const val KEY_SEEN_IDS = "overlay_seen_question_ids"
        private const val CHANNEL_ID = "medstudy_questions"
        private const val SERVICE_NOTIFICATION_ID = 8301
        private const val REMINDER_NOTIFICATION_ID = 8302

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, QuestionOverlayService::class.java).setAction(ACTION_START))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, QuestionOverlayService::class.java).setAction(ACTION_STOP))
        }
    }
}
