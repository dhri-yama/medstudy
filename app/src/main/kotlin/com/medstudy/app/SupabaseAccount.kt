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

class AuthException(message: String) : Exception(message)

/**
 * Supabase email/password auth (GoTrue REST) plus attempt sync (PostgREST).
 * Plain HttpURLConnection keeps the app free of a new HTTP/serialization dependency.
 * shortcut: attempts only, full push/pull each sync, manual/launch trigger; add a watermark and WorkManager when history gets large.
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
                extraHeaders = mapOf("Prefer" to "resolution=ignore-duplicates,return=minimal"),
            )
        }
        val known = local.mapTo(HashSet()) { it.id }
        var offset = 0
        var pulled = 0
        while (true) {
            val rows = requestArray("rest/v1/attempts?select=*&order=completed_at.asc&limit=$PAGE&offset=$offset")
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
        const val PAGE = 1000
    }
}

private fun QuestionAttemptEntity.toRow() = JSONObject()
    .put("id", id)
    .put("question_id", questionId)
    .put("selected_option_ids", JSONArray(selectedOptionIds))
    .put("result", result)
    .put("completed_at", completedAtEpochMillis)
    .put("study_date", studyDateIso)
    .put("time_zone", timeZoneId)
    .put("context", context)

private fun JSONObject.toEntity(userId: String): QuestionAttemptEntity {
    val options = getJSONArray("selected_option_ids")
    return QuestionAttemptEntity(
        id = getString("id"),
        userId = userId,
        questionId = getString("question_id"),
        selectedOptionIds = List(options.length()) { options.getString(it) },
        result = getString("result"),
        completedAtEpochMillis = getLong("completed_at"),
        studyDateIso = getString("study_date"),
        timeZoneId = getString("time_zone"),
        context = getString("context"),
    )
}
