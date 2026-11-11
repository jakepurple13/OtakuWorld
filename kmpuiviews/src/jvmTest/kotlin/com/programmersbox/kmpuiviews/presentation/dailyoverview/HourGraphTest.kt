package com.programmersbox.kmpuiviews.presentation.dailyoverview

import com.programmersbox.kmpuiviews.presentation.dailyoverview.components.peakHourLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HourGraphTest {
    private fun hours(vararg pairs: Pair<Int, Int>): List<Int> =
        MutableList(24) { 0 }.apply { pairs.forEach { (hour, count) -> this[hour] = count } }

    @Test
    fun peakIsTheBusiestHour() {
        assertEquals("10 PM", peakHourLabel(hours(9 to 2, 22 to 6, 23 to 1)))
    }

    @Test
    fun tieReturnsTheLaterHour() {
        assertEquals("9 PM", peakHourLabel(hours(8 to 4, 21 to 4)))
    }

    @Test
    fun midnightIsTwelveAm() {
        assertEquals("12 AM", peakHourLabel(hours(0 to 3)))
    }

    @Test
    fun noonIsTwelvePm() {
        assertEquals("12 PM", peakHourLabel(hours(12 to 3)))
    }

    @Test
    fun morningHourIsAm() {
        assertEquals("7 AM", peakHourLabel(hours(7 to 1)))
    }

    @Test
    fun allZeroIsNull() {
        assertNull(peakHourLabel(List(24) { 0 }))
    }
}
