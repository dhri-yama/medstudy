package com.medstudy.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementTest {
    private val day = Entitlement.DAY_MILLIS
    private val t0 = 1_000_000_000_000L

    /** Trial that the server said started at t0 and ends 15 days later, fetched at [fetchedAt]. */
    private fun trial(fetchedAt: Long = t0) = Entitlement(isFull = true, inTrial = true, endsAtMillis = t0 + 15 * day, serverNowMillis = fetchedAt)

    @Test
    fun `trial counts down and ends on day 15`() {
        assertEquals(15, trial().trialDaysLeft(t0))
        assertEquals(14, trial().trialDaysLeft(t0 + day))
        assertEquals(1, trial().trialDaysLeft(t0 + 14 * day + 1))
        assertTrue(trial().isFullAt(t0 + 14 * day))
        assertFalse(trial().isFullAt(t0 + 15 * day))
        assertNull(trial().trialDaysLeft(t0 + 16 * day))
    }

    @Test
    fun `an expired trial is the free tier with a daily allowance`() {
        val expired = Entitlement(isFull = false, inTrial = false, endsAtMillis = t0 + 15 * day, serverNowMillis = t0 + 16 * day)
        val now = t0 + 16 * day
        assertFalse(expired.isFullAt(now))
        assertEquals(10, expired.questionsLeftToday(0, now))
        assertEquals(3, expired.questionsLeftToday(7, now))
        assertEquals(0, expired.questionsLeftToday(25, now))
    }

    @Test
    fun `full access has no daily limit`() {
        assertEquals(Int.MAX_VALUE, trial().questionsLeftToday(500, t0 + day))
    }

    @Test
    fun `setting the phone clock back does not extend the trial`() {
        // The server last answered on day 14 (1 day left); the user rolls the phone clock back to day 1.
        val lastSeen = trial(fetchedAt = t0 + 14 * day)
        assertEquals(1, lastSeen.trialDaysLeft(t0 + day))
        assertFalse(lastSeen.isFullAt(t0 + 15 * day))
    }

    @Test
    fun `a paid period gets a 3 day offline grace but a trial does not`() {
        val paid = Entitlement(isFull = true, inTrial = false, endsAtMillis = t0 + 30 * day, serverNowMillis = t0 + 29 * day)
        assertTrue(paid.isFullAt(t0 + 32 * day))
        assertFalse(paid.isFullAt(t0 + 34 * day))
        assertFalse(trial().isFullAt(t0 + 15 * day + 1))
    }

    @Test
    fun `before the first server answer nobody is locked out`() {
        assertTrue(Entitlement.Unknown.isFullAt(t0))
        assertNull(Entitlement.Unknown.trialDaysLeft(t0))
    }
}
