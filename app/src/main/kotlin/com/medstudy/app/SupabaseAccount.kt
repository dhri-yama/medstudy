package com.medstudy.app

import android.content.Context
import com.medstudy.core.database.ActivityDao
import com.medstudy.core.database.BookmarkEntity
import com.medstudy.core.model.StudySchedulePreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.medstudy.core.database.LOCAL_USER
import com.medstudy.core.database.QuestionAttemptEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/** [code] is the HTTP status when the server refused the request, 0 otherwise. */
class AuthException(message: String, val code: Int = 0) : Exception(message)

/**
 * The account is already signed in on the allowed number of devices, so this sign-in was not completed.
 * [resetAvailableAtMillis] is when "Sign out my other devices" may next be used; null means right now.
 */
class DeviceLimitException(val resetAvailableAtMillis: Long?) :
    Exception("You're already signed in on 2 devices. Log out on one of them first.")

/**
 * Supabase email/password auth (GoTrue REST) plus attempt sync (PostgREST).
 * Plain HttpURLConnection keeps the app free of a new HTTP/serialization dependency.
 * shortcut: attempts and profile only, full push/pull each sync, manual/launch trigger; add a watermark and WorkManager when history gets large.
 */
class SupabaseAccount(context: Context, private val activityDao: ActivityDao) {
    private val prefs = context.applicationContext.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val baseUrl = BuildConfig.SUPABASE_URL
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY

    /** Per-account sync bookkeeping (what has been uploaded and downloaded), and which account last used this phone. Survives sign-out. */
    private val syncPrefs = context.applicationContext.getSharedPreferences("sync", Context.MODE_PRIVATE)
    /** Bookmarks removed on this phone and not yet passed on, as "userId|questionId|removedAtMillis". */
    private val removalPrefs = context.applicationContext.getSharedPreferences("bookmark_removals", Context.MODE_PRIVATE)
    private val crashFile = CrashReporter.pendingFile(context)
    private val syncLock = Mutex()
    /** Reads this phone's study-plan settings, for uploading them. Set by the app container. */
    var studySchedule: (suspend () -> StudySchedulePreferences)? = null
    /** Runs when a different account than last time signs in, so the previous learner's plan is not shown to this one. */
    var onDifferentAccount: (suspend () -> Unit)? = null

    val isConfigured get() = baseUrl.isNotBlank() && anonKey.isNotBlank()
    val userId: String get() = prefs.getString(KEY_USER, null) ?: LOCAL_USER
    val email: String? get() = prefs.getString(KEY_EMAIL, null)
    val isSignedIn get() = prefs.getString(KEY_USER, null) != null

    /** Goes up every time this phone is signed out, including when the server ends the session, so the UI can re-check [isSignedIn]. */
    val sessionVersion = MutableStateFlow(0)
    /** Why the server signed this phone out, for the sign-in screen to show once. */
    private var signedOutNotice: String? = null
    fun takeSignedOutNotice(): String? = signedOutNotice.also { signedOutNotice = null }
    /** A sign-in the device limit blocked, kept in memory only so [signOutOtherDevices] can finish it. */
    private var blockedSession: JSONObject? = null

    /** Returns false when the project requires email confirmation before a session exists. */
    suspend fun signUp(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val json = request("auth/v1/signup", "POST", JSONObject().put("email", email).put("password", password), auth = false)
        if (json.has("access_token")) { startSession(json); true } else false
    }

    suspend fun signIn(email: String, password: String) = withContext(Dispatchers.IO) {
        val json = request(
            "auth/v1/token?grant_type=password", "POST",
            JSONObject().put("email", email).put("password", password), auth = false,
        )
        startSession(json)
    }

    /** Signs in (or creates the account) with a Google ID token; [rawNonce] is the un-hashed nonce whose SHA-256 was given to Google. Returns the Google display name. */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String): String? = withContext(Dispatchers.IO) {
        val json = request(
            "auth/v1/token?grant_type=id_token", "POST",
            JSONObject().put("provider", "google").put("id_token", idToken).put("nonce", rawNonce), auth = false,
        )
        startSession(json)
        json.getJSONObject("user").optJSONObject("user_metadata")?.let { it.optString("full_name").ifBlank { it.optString("name") } }?.takeIf { it.isNotBlank() }
    }

    /**
     * Finishes a sign-in the device limit blocked by ending this account's sessions on every other device.
     * Throws [DeviceLimitException] again if the server says it is too soon to do that.
     */
    suspend fun signOutOtherDevices() = withContext(Dispatchers.IO) {
        val json = blockedSession ?: throw AuthException("Sign in again first.")
        startSession(json, signOutOthers = true)
    }

    /** Emails a one-time code for choosing a new password. The server answers the same way whether or not the email has an account. */
    suspend fun requestPasswordReset(email: String) = withContext(Dispatchers.IO) {
        request("auth/v1/recover", "POST", JSONObject().put("email", email), auth = false)
        Unit
    }

    /** Checks the emailed code, sets the new password and signs in (so the device limit applies here too). */
    suspend fun resetPassword(email: String, code: String, newPassword: String) = withContext(Dispatchers.IO) {
        val json = request(
            "auth/v1/verify", "POST",
            JSONObject().put("type", "recovery").put("email", email).put("token", code), auth = false,
        )
        // The password can only be changed as the signed-in user, so the session is used once before it is kept.
        storeSession(json)
        try {
            request("auth/v1/user", "PUT", JSONObject().put("password", newPassword))
        } catch (e: Exception) {
            prefs.edit().clear().apply()
            throw e
        }
        startSession(json)
    }

    /**
     * Deletes the account and everything stored under it on the server, then this account's answers and bookmarks on this phone.
     * The phone stays signed in until [forgetSession], so the caller can reset its own state first.
     */
    suspend fun deleteAccount() = withContext(Dispatchers.IO) {
        check(isSignedIn) { "Not signed in" }
        val uid = userId
        request("rest/v1/rpc/delete_my_account", "POST", JSONObject())
        activityDao.deleteAttempts(uid)
        activityDao.deleteBookmarks(uid)
        reportPrefs.edit().clear().apply()
    }

    /** Signs this phone out without contacting the server (the account is already gone there). */
    fun forgetSession() = clearSession(notice = null)

    /** Ends the session on the server too, which frees this phone's device slot. Offline, only the phone is signed out. */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        if (isSignedIn) runCatching { request("auth/v1/logout?scope=local", "POST", null) }
        clearSession(notice = null)
        signedOutNotice = null // a log out the learner asked for needs no explanation
    }

    /** On launch: makes sure this phone holds one of the account's device slots, and signs it out if both are taken by other devices. */
    suspend fun confirmDeviceSlot() = withContext(Dispatchers.IO) {
        if (!isSignedIn) return@withContext
        // Offline or a server hiccup changes nothing; a session the server has ended is handled in refresh().
        val claim = runCatching { claimSlot(signOutOthers = false) }.getOrNull() ?: return@withContext
        if (!claim.allowed) {
            runCatching { request("auth/v1/logout?scope=local", "POST", null) }
            clearSession("This account is already signed in on 2 devices. Log out on one of them, then sign in here.")
        }
    }

    /** Stores the new session if the account has a free device slot; otherwise keeps it aside and throws [DeviceLimitException]. */
    private suspend fun startSession(json: JSONObject, signOutOthers: Boolean = false) {
        storeSession(json)
        val claim = try {
            claimSlot(signOutOthers)
        } catch (e: Exception) {
            prefs.edit().clear().apply()
            throw e
        }
        if (!claim.allowed) {
            prefs.edit().clear().apply()
            blockedSession = json
            throw DeviceLimitException(claim.resetAvailableAtMillis)
        }
        blockedSession = null
        signedOutNotice = null
        val lastUser = syncPrefs.getString(KEY_LAST_USER, null)
        if (lastUser != null && lastUser != userId) onDifferentAccount?.invoke()
        adoptLocalData()
        // Answers adopted from before sign-in are older rows, so the next sync uploads this account's answers from the start.
        syncPrefs.edit().putString(KEY_LAST_USER, userId).remove(KEY_PUSHED + userId).apply()
    }

    private class SlotClaim(val allowed: Boolean, val resetAvailableAtMillis: Long?)

    private fun claimSlot(signOutOthers: Boolean): SlotClaim {
        val body = JSONObject().put("sign_out_others", signOutOthers)
        val row = JSONArray(rawRequest("rest/v1/rpc/claim_device_slot", "POST", body, true, emptyMap(), true)).getJSONObject(0)
        val resetAt = if (row.isNull("reset_available_at")) null
        else OffsetDateTime.parse(row.getString("reset_available_at")).toInstant().toEpochMilli()
        return SlotClaim(row.getBoolean("allowed"), resetAt)
    }

    private fun clearSession(notice: String?) {
        if (!isSignedIn) return
        prefs.edit().clear().apply()
        signedOutNotice = notice
        sessionVersion.update { it + 1 }
    }

    /**
     * Uploads what is new on this phone and downloads what is new on the account: answers, bookmarks, plan settings,
     * question reports and the last crash. Returns how many answers came from the cloud.
     * Only answers are required to succeed; the rest is retried on the next sync.
     */
    suspend fun sync(): Int = withContext(Dispatchers.IO) {
        syncLock.withLock {
            check(isSignedIn) { "Not signed in" }
            val uid = userId
            pushAttempts(uid)
            runCatching { flushReports() }
            runCatching { flushCrashReport() }
            runCatching { syncBookmarks(uid) }
            runCatching { pushStudySchedule(uid) }
            pullAttempts(uid)
        }
    }

    /** Uploads only the answers added to this phone since the last upload. */
    private suspend fun pushAttempts(uid: String) {
        val upTo = activityDao.lastAttemptRowId(uid) ?: 0L
        val after = syncPrefs.getLong(KEY_PUSHED + uid, 0L).coerceAtMost(upTo)
        activityDao.getAttemptsAdded(uid, after, upTo).chunked(500).forEach { chunk ->
            request(
                "rest/v1/attempts?on_conflict=id", "POST",
                JSONArray(chunk.map { it.toRow() }),
                extraHeaders = IGNORE_DUPLICATES,
            )
            val options = chunk.flatMap { attempt ->
                attempt.selectedOptionIds.map { JSONObject().put("attempt_id", attempt.id).put("option_id", it) }
            }
            if (options.isNotEmpty()) {
                request(
                    "rest/v1/attempt_selected_options?on_conflict=attempt_id,option_id", "POST",
                    JSONArray(options), extraHeaders = IGNORE_DUPLICATES,
                )
            }
        }
        syncPrefs.edit().putLong(KEY_PUSHED + uid, upTo).apply()
    }

    /** Downloads only the answers the server received since this phone last asked (everything, the first time). */
    private suspend fun pullAttempts(uid: String): Int {
        val since = syncPrefs.getString(KEY_PULLED + uid, null)
        var newest = since?.let(Instant::parse)
        val filter = since?.let { "&synced_at=gte.$it" }.orEmpty()
        var offset = 0
        var pulled = 0
        while (true) {
            val rows = requestArray(
                "rest/v1/attempts?select=*,attempt_selected_options(option_id)$filter" +
                    "&order=synced_at.asc,id.asc&limit=$PAGE&offset=$offset",
            )
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val syncedAt = OffsetDateTime.parse(row.getString("synced_at")).toInstant()
                if (newest == null || syncedAt > newest) newest = syncedAt
                if (activityDao.hasAttempt(row.getString("id"))) continue
                activityDao.putAttempt(row.toEntity(uid)); pulled++
            }
            if (rows.length() < PAGE) break
            offset += PAGE
        }
        newest?.let { syncPrefs.edit().putString(KEY_PULLED + uid, it.toString()).apply() }
        return pulled
    }

    /** Called when a bookmark is removed on this phone, so the removal reaches the account and the learner's other phone. */
    fun noteBookmarkRemoved(questionId: String, atMillis: Long) {
        if (!isSignedIn) return
        synchronized(removalPrefs) {
            val pending = removalPrefs.getStringSet(KEY_PENDING, emptySet()).orEmpty() + "$userId|$questionId|$atMillis"
            removalPrefs.edit().putStringSet(KEY_PENDING, pending).apply()
        }
    }

    /**
     * Two-way bookmark sync. For each question the newer change wins, whether that is saving or removing it,
     * here or on the account. A question this phone has never touched simply follows the account.
     */
    private suspend fun syncBookmarks(uid: String) {
        val mine = removalPrefs.getStringSet(KEY_PENDING, emptySet()).orEmpty().filter { it.startsWith("$uid|") }
        val removedHere = mine.associate { it.substringAfter('|').substringBeforeLast('|') to it.substringAfterLast('|').toLong() }
        val savedHere = activityDao.getBookmarks(uid).associate { it.questionId to it.savedAtEpochMillis }

        class Remote(val savedAt: Long, val removedAt: Long?) { val changedAt get() = removedAt ?: savedAt }
        val remote = HashMap<String, Remote>()
        var offset = 0
        while (true) {
            val rows = requestArray("rest/v1/bookmarks?select=question_id,saved_at,removed_at&order=question_id.asc&limit=$PAGE&offset=$offset")
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                fun millis(key: String) = OffsetDateTime.parse(row.getString(key)).toInstant().toEpochMilli()
                remote[row.getString("question_id")] = Remote(millis("saved_at"), if (row.isNull("removed_at")) null else millis("removed_at"))
            }
            if (rows.length() < PAGE) break
            offset += PAGE
        }

        val upload = JSONArray()
        for (questionId in savedHere.keys + removedHere.keys + remote.keys) {
            val there = remote[questionId]
            val saved = savedHere[questionId]
            val removed = if (saved == null) removedHere[questionId] else null
            val changedHere = saved ?: removed
            when {
                changedHere == null -> if (there != null && there.removedAt == null) activityDao.putBookmark(BookmarkEntity(uid, questionId, there.savedAt))
                there == null || changedHere > there.changedAt -> upload.put(
                    JSONObject().put("question_id", questionId)
                        .put("saved_at", Instant.ofEpochMilli(saved ?: there?.savedAt ?: changedHere).toString())
                        .put("removed_at", removed?.let { Instant.ofEpochMilli(it).toString() } ?: JSONObject.NULL),
                )
                there.removedAt == null && saved == null -> activityDao.putBookmark(BookmarkEntity(uid, questionId, there.savedAt))
                there.removedAt != null && saved != null -> activityDao.removeBookmark(uid, questionId)
            }
        }
        if (upload.length() > 0) request("rest/v1/bookmarks?on_conflict=user_id,question_id", "POST", upload, extraHeaders = MERGE)
        synchronized(removalPrefs) {
            val left = removalPrefs.getStringSet(KEY_PENDING, emptySet()).orEmpty() - mine.toSet()
            removalPrefs.edit().putStringSet(KEY_PENDING, left).apply()
        }
    }

    /** Marks this phone's plan settings (study days, minutes a day) as changed, to be uploaded on the next sync. */
    fun markStudyScheduleChanged() {
        if (isSignedIn) syncPrefs.edit().putBoolean(KEY_SCHEDULE_CHANGED + userId, true).apply()
    }

    private suspend fun pushStudySchedule(uid: String) {
        if (!syncPrefs.getBoolean(KEY_SCHEDULE_CHANGED + uid, false)) return
        val schedule = studySchedule?.invoke() ?: return
        request(
            "rest/v1/rpc/set_study_schedule", "POST",
            JSONObject().put("minutes", schedule.minuteTargetPerDay).put("weekdays", JSONArray(schedule.availableWeekdays.sorted())),
        )
        syncPrefs.edit().remove(KEY_SCHEDULE_CHANGED + uid).apply()
    }

    /** The plan settings saved on the account, or null if it has none yet. */
    suspend fun pullStudySchedule(): StudySchedulePreferences? = withContext(Dispatchers.IO) {
        check(isSignedIn) { "Not signed in" }
        val days = requestArray("rest/v1/profile_study_days?select=weekday")
        if (days.length() == 0) return@withContext null
        val rows = requestArray("rest/v1/profiles?select=daily_question_goal,daily_minute_goal&limit=1")
        if (rows.length() == 0) return@withContext null
        val row = rows.getJSONObject(0)
        runCatching {
            StudySchedulePreferences(
                availableWeekdays = List(days.length()) { days.getJSONObject(it).getInt("weekday") }.toSet(),
                questionTargetPerDay = row.getInt("daily_question_goal"),
                minuteTargetPerDay = row.getInt("daily_minute_goal"),
            )
        }.getOrNull()
    }

    private fun flushCrashReport() {
        if (!crashFile.exists()) return
        val report = runCatching { JSONObject(crashFile.readText()) }.getOrNull()
        if (report != null) request("rest/v1/crash_reports", "POST", report, extraHeaders = mapOf("Prefer" to "return=minimal"))
        crashFile.delete()
    }

    /** Asks the server what this user may do (full access or the free tier). Null when offline or signed out. */
    suspend fun fetchEntitlement(): Entitlement? = withContext(Dispatchers.IO) {
        if (!isSignedIn) return@withContext null
        runCatching {
            val rows = JSONArray(rawRequest("rest/v1/rpc/my_entitlement", "POST", JSONObject(), true, emptyMap(), true))
            if (rows.length() == 0) return@runCatching null
            val row = rows.getJSONObject(0)
            Entitlement(
                isFull = row.getString("access") == "full",
                inTrial = row.getBoolean("in_trial"),
                endsAtMillis = OffsetDateTime.parse(row.getString("ends_at")).toInstant().toEpochMilli(),
                serverNowMillis = OffsetDateTime.parse(row.getString("server_now")).toInstant().toEpochMilli(),
            )
        }.getOrNull()
    }

    /** Hands a Google Play purchase token to the backend, which checks it with Google and records the paid period. */
    suspend fun verifyPurchase(productId: String, purchaseToken: String) = withContext(Dispatchers.IO) {
        check(isSignedIn) { "Not signed in" }
        request(
            "functions/v1/verify-purchase", "POST",
            JSONObject().put("productId", productId).put("purchaseToken", purchaseToken),
        )
        Unit
    }

    /** Reports waiting to be sent (not signed in, or offline), stored as "questionId|reason" lines. */
    private val reportPrefs = context.applicationContext.getSharedPreferences("reports", Context.MODE_PRIVATE)

    /** Queues a report and sends it right away when signed in; unsent reports go out on the next sync. */
    suspend fun report(questionId: String, reason: String) = withContext(Dispatchers.IO) {
        val pending = reportPrefs.getStringSet(KEY_PENDING, emptySet()).orEmpty() + "$questionId|$reason"
        reportPrefs.edit().putStringSet(KEY_PENDING, pending).apply()
        if (isSignedIn) runCatching { flushReports() }
    }

    private fun flushReports() {
        val pending = reportPrefs.getStringSet(KEY_PENDING, emptySet()).orEmpty()
        if (pending.isEmpty()) return
        val rows = JSONArray(pending.map { line ->
            val (id, reason) = line.split('|', limit = 2)
            JSONObject().put("question_id", id).put("reason", reason)
        })
        request("rest/v1/question_reports", "POST", rows, extraHeaders = mapOf("Prefer" to "return=minimal"))
        reportPrefs.edit().remove(KEY_PENDING).apply()
    }

    suspend fun pushProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        check(isSignedIn) { "Not signed in" }
        val row = JSONObject()
            .put("user_id", userId)
            .put("display_name", profile.name)
            .put("exam_id", UserProfileStore.EXAM_IDS.getValue(profile.primaryExam))
            .put("daily_question_goal", profile.dailyQuestionGoal)
            .put("exam_date", profile.examDateIso ?: JSONObject.NULL)
        request(
            "rest/v1/profiles?on_conflict=user_id", "POST", JSONArray().put(row),
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        )
        // The full list of exams goes in profile_exams. shortcut: best effort, so profile sync still works before that table exists.
        runCatching {
            request("rest/v1/profile_exams?user_id=eq.$userId", "DELETE", null, extraHeaders = mapOf("Prefer" to "return=minimal"))
            request(
                "rest/v1/profile_exams", "POST",
                JSONArray(profile.exams.map { JSONObject().put("user_id", userId).put("exam_id", UserProfileStore.EXAM_IDS.getValue(it)) }),
                extraHeaders = mapOf("Prefer" to "return=minimal"),
            )
        }
        Unit
    }

    /** The account's saved profile, or null if it has none yet. */
    suspend fun pullProfile(): UserProfile? = withContext(Dispatchers.IO) {
        check(isSignedIn) { "Not signed in" }
        val rows = requestArray("rest/v1/profiles?select=display_name,exam_id,daily_question_goal,exam_date&limit=1")
        if (rows.length() == 0) return@withContext null
        val row = rows.getJSONObject(0)
        fun label(id: String) = UserProfileStore.EXAM_IDS.entries.firstOrNull { it.value == id }?.key
        val primary = label(row.getString("exam_id")) ?: return@withContext null
        val all = runCatching {
            val chosen = requestArray("rest/v1/profile_exams?select=exam_id")
            List(chosen.length()) { label(chosen.getJSONObject(it).getString("exam_id")) }.filterNotNull()
        }.getOrDefault(emptyList())
        // The primary exam always leads the list.
        UserProfile(
            row.getString("display_name"), listOf(primary) + (all - primary), row.getInt("daily_question_goal"),
            examDateIso = if (row.isNull("exam_date")) null else row.getString("exam_date"),
        )
    }

    private suspend fun adoptLocalData() {
        activityDao.adoptLocalAttempts(userId)
        activityDao.adoptLocalBookmarks(userId)
    }

    private fun storeSession(json: JSONObject) {
        val user = json.getJSONObject("user")
        prefs.edit()
            .putString(KEY_ACCESS, json.getString("access_token"))
            .putString(KEY_REFRESH, json.getString("refresh_token"))
            .putString(KEY_USER, user.getString("id"))
            .putString(KEY_EMAIL, user.optString("email"))
            .apply()
    }

    /** [staleAccess] is the access token the server just refused; if another request already replaced it, there is nothing to do. */
    @Synchronized
    private fun refresh(staleAccess: String?) {
        if (prefs.getString(KEY_ACCESS, null) != staleAccess) return
        val refresh = prefs.getString(KEY_REFRESH, null) ?: throw AuthException("Session expired, sign in again")
        val json = try {
            request(
                "auth/v1/token?grant_type=refresh_token", "POST",
                JSONObject().put("refresh_token", refresh), auth = false, retry = false,
            )
        } catch (e: AuthException) {
            // The server no longer knows this session (signed out from another device, or removed): sign this phone out too.
            // A busy or failing server (429, 5xx) is not that, so the session is kept.
            if (e.code in 400..499 && e.code != 429) clearSession("You were signed out on this phone. Sign in again to continue.")
            throw e
        }
        storeSession(json)
    }

    private fun requestArray(path: String): JSONArray = JSONArray(rawRequest(path, "GET", null, true, emptyMap(), true))

    private fun request(
        path: String, method: String, body: Any?, auth: Boolean = true,
        extraHeaders: Map<String, String> = emptyMap(), retry: Boolean = true,
    ): JSONObject {
        val text = rawRequest(path, method, body, auth, extraHeaders, retry)
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private fun rawRequest(
        path: String, method: String, body: Any?, auth: Boolean,
        extraHeaders: Map<String, String>, retry: Boolean,
    ): String {
        val conn = URL("$baseUrl/$path").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("apikey", anonKey)
            val access = if (auth) prefs.getString(KEY_ACCESS, null) else null
            conn.setRequestProperty("Authorization", "Bearer ${access ?: anonKey}")
            conn.setRequestProperty("Content-Type", "application/json")
            extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = conn.responseCode
            if (code == 401 && auth && retry) { refresh(access); return rawRequest(path, method, body, auth, extraHeaders, false) }
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(text).let { it.optString("msg").ifBlank { it.optString("message") }.ifBlank { it.optString("error_description") } } }.getOrNull()
                throw AuthException(message?.takeIf { it.isNotBlank() } ?: "Request failed ($code)", code)
            }
            return text
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
        const val KEY_USER = "user"
        const val KEY_EMAIL = "email"
        const val KEY_PENDING = "pending"
        const val PAGE = 1000
        const val KEY_LAST_USER = "last_user"
        const val KEY_PUSHED = "pushed_row."
        const val KEY_PULLED = "pulled_since."
        const val KEY_SCHEDULE_CHANGED = "schedule_changed."
        val MERGE = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal")
        val IGNORE_DUPLICATES = mapOf("Prefer" to "resolution=ignore-duplicates,return=minimal")
    }
}

private fun QuestionAttemptEntity.toRow() = JSONObject()
    .put("id", id)
    .put("question_id", questionId)
    .put("result", result)
    .put("context", context)
    .put("completed_at", Instant.ofEpochMilli(completedAtEpochMillis).toString())
    .put("time_zone", timeZoneId)

private fun JSONObject.toEntity(userId: String): QuestionAttemptEntity {
    val options = getJSONArray("attempt_selected_options")
    val completedAt = OffsetDateTime.parse(getString("completed_at")).toInstant()
    val zone = getString("time_zone")
    // study_date is not stored in the cloud; it is the local date at completion.
    val studyDate = completedAt.atZone(runCatching { ZoneId.of(zone) }.getOrDefault(ZoneId.systemDefault())).toLocalDate()
    return QuestionAttemptEntity(
        id = getString("id"),
        userId = userId,
        questionId = getString("question_id"),
        selectedOptionIds = List(options.length()) { options.getJSONObject(it).getString("option_id") },
        result = getString("result"),
        completedAtEpochMillis = completedAt.toEpochMilli(),
        studyDateIso = studyDate.toString(),
        timeZoneId = zone,
        context = getString("context"),
    )
}
