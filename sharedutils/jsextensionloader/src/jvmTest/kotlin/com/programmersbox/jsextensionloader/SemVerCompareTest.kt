package com.programmersbox.jsextensionloader

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SemVerCompareTest {

    @Test
    fun newerPatchVersionIsNewer() {
        assertTrue(SemVerCompare.isNewer("1.0.0", "1.0.1"))
    }

    @Test
    fun newerMinorVersionIsNewer() {
        assertTrue(SemVerCompare.isNewer("1.0.9", "1.1.0"))
    }

    @Test
    fun newerMajorVersionIsNewer() {
        assertTrue(SemVerCompare.isNewer("1.9.9", "2.0.0"))
    }

    @Test
    fun sameVersionIsNotNewer() {
        assertFalse(SemVerCompare.isNewer("1.0.0", "1.0.0"))
    }

    @Test
    fun olderVersionIsNotNewer() {
        assertFalse(SemVerCompare.isNewer("1.2.0", "1.1.0"))
    }

    @Test
    fun malformedVersionReturnsFalse() {
        assertFalse(SemVerCompare.isNewer("not-a-version", "1.0.0"))
    }

    @Test
    fun differentPartCountsCompareCorrectly() {
        assertTrue(SemVerCompare.isNewer("1.0", "1.0.1"))
    }
}
