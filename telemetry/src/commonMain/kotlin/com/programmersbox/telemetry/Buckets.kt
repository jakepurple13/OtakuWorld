package com.programmersbox.telemetry

/** Rounds counts, durations and percentages so telemetry never sends exact values. */
object Buckets {
    fun count(n: Int): String = when {
        n <= 0 -> "0"
        n <= 5 -> "1-5"
        n <= 20 -> "6-20"
        else -> "21+"
    }

    fun durationMs(ms: Long): String = when {
        ms < 1_000 -> "<1s"
        ms < 5_000 -> "1-5s"
        ms < 30_000 -> "5-30s"
        ms < 120_000 -> "30s-2m"
        else -> "2m+"
    }

    fun percent(p: Int): String = when (p.coerceIn(0, 100)) {
        in 0..25 -> "0-25"
        in 26..50 -> "26-50"
        in 51..75 -> "51-75"
        else -> "76-100"
    }
}
