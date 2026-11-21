package com.programmersbox.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals

class BucketsTest {
    @Test
    fun counts() = assertEquals(
        listOf("0", "1-5", "1-5", "6-20", "21+"),
        listOf(0, 1, 5, 6, 21).map(Buckets::count),
    )

    @Test
    fun durations() = assertEquals(
        listOf("<1s", "1-5s", "5-30s", "30s-2m", "2m+"),
        listOf(999L, 1000L, 5000L, 30000L, 120000L).map(Buckets::durationMs),
    )

    @Test
    fun percents() = assertEquals(
        listOf("0-25", "26-50", "51-75", "76-100", "76-100", "0-25"),
        listOf(0, 26, 51, 76, 150, -5).map(Buckets::percent),
    )
}
