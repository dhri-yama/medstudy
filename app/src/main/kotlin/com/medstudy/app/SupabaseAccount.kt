package com.medstudy.app

import android.content.Context
import com.medstudy.core.database.ActivityDao
import com.medstudy.core.database.LOCAL_USER
import com.medstudy.core.database.QuestionAttemptEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class AuthException(message: String) : Exception(message)

/**
 * Supabase email/password auth (GoTrue REST) plus attempt sync (PostgREST).
 * Plain HttpURLConnection keeps the app free of a new HTTP/serialization dependency.
 * shortcut: attempts and profile only, full push/pull each sync, manual/launch trigger; add a watermark and WorkManager when history gets large.
 */
class SupabaseAccount(context: Context, private val activityDao: ActivityDao) {
    private val prefs = context.applicationContext.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val baseUrl = BuildConfig.SUPABASE_URL
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY

    val isConfigured get() = baseUrl.isNotBlank() && anonKey.isNotBlank()
    val userId: String get() = prefs.getString(KEY_USER, null) ?: LOCAL_USER
    val email: String? get() = prefs.getString(KEY_EMAIL, null)
    val isSignedIn get() = prefs.getString(KEY_USER, null) != null

    /** Returns false when the project requires email confirmation before a session exists. */
    suspend fun signUp(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val json = request("auth/v1/signup", "POST", JSONObject().put("email", email).put("password", password), auth = false)
        if (json.has("access_token")) { storeSession(json); adoptLocalData(); true } else false
    }

    suspend fun signIn(email: String, password: String) = withContext(Dispatchers.IO) {
        val json = request(
            "auth/v1/token?grant_type=password", "POST",
            JSONObject().put("email", email).put("password", password), auth = false,
        )
        storeSession(json)
        adoptLocalData()
    }

    fun signOut() = prefs.edit().clear().apply()

    suspend fun sync(): Int = withContext(Dispatchers.IO) {
        check(isSignedIn) { "Not signed in" }
        val uid = userId
        val local = activityDao.getAttempts(uid)
        local.chunked(500).forEach { chunk ->
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
        runCatching { flushReports() }
        val known = local.mapTo(HashSet()) { it.id }
        var offset = 0
        var pulled = 0
        while (true) {
            val rows = requestArray(
                "rest/v1/attempts?select=*,attempt_selected_options(option_id)" +
                    "&order=completed_at.asc,id.asc&limit=$PAGE&offset=$offset",
            )
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                if (row.getString("id") in known) continue
                activityDao.putAttempt(row.toEntity(uid)); pulled++
            }
            if (rows.length() < PAGE) break
            offset += PAGE
        }
        pulled
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
            .put("exam_id", UserProfileStore.EXAM_IDS.getValue(profile.exam))
            .put("daily_question_goal", profile.dailyQuestionGoal)
        request(
            "rest/v1/profiles?on_conflict=user_id", "POST", JSONArray().put(row),
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        )
        Unit
    }

    /** The account's saved profile, or null if it has none yet. */
    suspend fun pullProfile(): UserProfile? = withContext(Dispatchers.IO) {
        check(isSignedIn) { "Not signed in" }
        val rows = requestArray("rest/v1/profiles?select=display_name,exam_id,daily_question_goal&limit=1")
        if (rows.length() == 0) return@withContext null
        val row = rows.getJSONObject(0)
        val exam = UserProfileStore.EXAM_IDS.entries.firstOrNull { it.value == row.getString("exam_id") }?.key
            ?: return@withContext null
        UserProfile(row.getString("display_name"), exam, row.getInt("daily_question_goal"))
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

    private fun refresh() {
        val refresh = prefs.getString(KEY_REFRESH, null) ?: throw AuthException("Session expired, sign in again")
        val json = request(
            "auth/v1/token?grant_type=refresh_token", "POST",
            JSONObject().put("refresh_token", refresh), auth = false, retry = false,
        )
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
            conn.setRequestProperty("Authorization", "Bearer ${if (auth) prefs.getString(KEY_ACCESS, anonKey) else anonKey}")
            conn.setRequestProperty("Content-Type", "application/json")
            extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = conn.responseCode
            if (code == 401 && auth && retry) { refresh(); return rawRequest(path, method, body, auth, extraHeaders, false) }
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(text).let { it.optString("msg").ifBlank { it.optString("message") }.ifBlank { it.optString("error_description") } } }.getOrNull()
                throw AuthException(message?.takeIf { it.isNotBlank() } ?: "Request failed ($code)")
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
