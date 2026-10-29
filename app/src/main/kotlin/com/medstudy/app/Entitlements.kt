package com.medstudy.app

import android.content.Context

/**
 * What the signed-in user may do, as last reported by the server (`my_entitlement()`), plus the rules that apply it.
 * The phone clock is never trusted alone: time only ever moves forward from the server time at the last fetch.
 */
data class Entitlement(
    val isFull: Boolean,
    val inTrial: Boolean,
    val endsAtMillis: Long,
    val serverNowMillis: Long,
) {
    /** Full access right now. A paid period gets a short offline grace; a trial does not. */
    fun isFullAt(nowMillis: Long): Boolean {
        if (!isFull) return false // the server already said free; the offline grace never upgrades that
        val effectiveNow = maxOf(nowMillis, serverNowMillis)
        val grace = if (inTrial) 0L else OFFLINE_GRACE_MILLIS
        return effectiveNow < endsAtMillis + grace
    }

    fun trialDaysLeft(nowMillis: Long): Int? {
        if (!inTrial || !isFullAt(nowMillis)) return null
        val left = endsAtMillis - maxOf(nowMillis, serverNowMillis)
        return ((left + DAY_MILLIS - 1) / DAY_MILLIS).toInt().coerceAtLeast(0)
    }

    /** How many more questions may be started today: unlimited with full access, the rest of the daily allowance on the free tier. */
    fun questionsLeftToday(answeredToday: Int, nowMillis: Long): Int =
        if (isFullAt(nowMillis)) Int.MAX_VALUE else (FREE_DAILY_QUESTIONS - answeredToday).coerceAtLeast(0)

    companion object {
        const val FREE_DAILY_QUESTIONS = 10
        const val OFFLINE_GRACE_MILLIS = 3 * 24 * 60 * 60 * 1000L
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L

        /** Before the first successful fetch (for example offline right after sign-up) we don't lock anyone out. */
        val Unknown = Entitlement(isFull = true, inTrial = false, endsAtMillis = Long.MAX_VALUE / 2, serverNowMillis = 0L)
    }
}

/** The last entitlement the server reported, kept so the offline-first app keeps working without a connection. */
class EntitlementStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("entitlement", Context.MODE_PRIVATE)

    fun load(): Entitlement =
        if (!prefs.contains(KEY_ENDS)) Entitlement.Unknown
        else Entitlement(
            isFull = prefs.getBoolean(KEY_FULL, true),
            inTrial = prefs.getBoolean(KEY_TRIAL, false),
            endsAtMillis = prefs.getLong(KEY_ENDS, 0L),
            serverNowMillis = prefs.getLong(KEY_NOW, 0L),
        )

    fun save(value: Entitlement) {
        prefs.edit().putBoolean(KEY_FULL, value.isFull).putBoolean(KEY_TRIAL, value.inTrial)
            .putLong(KEY_ENDS, value.endsAtMillis).putLong(KEY_NOW, value.serverNowMillis).apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_FULL = "full"
        const val KEY_TRIAL = "trial"
        const val KEY_ENDS = "ends"
        const val KEY_NOW = "now"
    }
}
