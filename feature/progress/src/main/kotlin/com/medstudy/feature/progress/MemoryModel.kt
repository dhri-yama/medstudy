package com.medstudy.feature.progress

import kotlin.math.max

/** One graded answer: when it happened and whether it was right. */
data class Review(val atEpochMillis: Long, val correct: Boolean)

/**
 * Per-topic forgetting curve (FSRS-style power law, simplified).
 * Stability S is the number of days until recall drops to 90%; retrievability decays as (1 + t / 9S)^-1.
 * A correct review grows S more the more had been forgotten; a miss cuts it.
 * shortcut: fixed constants instead of per-user fitted parameters, tune once real review logs exist.
 */
object MemoryModel {
    private const val MS_PER_DAY = 86_400_000.0
    private const val FIRST_CORRECT_DAYS = 2.0
    private const val FIRST_MISS_DAYS = 0.5
    private const val GROWTH = 2.0
    private const val MISS_FACTOR = 0.4
    private const val MIN_STABILITY_DAYS = 0.5

    fun retrievability(elapsedDays: Double, stabilityDays: Double): Double =
        1.0 / (1.0 + max(0.0, elapsedDays) / (9.0 * stabilityDays))

    /** Stability in days after replaying [reviews] in time order, or null with no reviews. */
    fun stability(reviews: List<Review>): Double? {
        val ordered = reviews.sortedBy(Review::atEpochMillis)
        val first = ordered.firstOrNull() ?: return null
        var stability = if (first.correct) FIRST_CORRECT_DAYS else FIRST_MISS_DAYS
        for (i in 1 until ordered.size) {
            val review = ordered[i]
            val elapsed = (review.atEpochMillis - ordered[i - 1].atEpochMillis) / MS_PER_DAY
            stability = if (review.correct) {
                stability * (1.0 + GROWTH * (1.0 - retrievability(elapsed, stability)))
            } else {
                max(MIN_STABILITY_DAYS, stability * MISS_FACTOR)
            }
        }
        return stability
    }
}
