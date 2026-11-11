package com.programmersbox.manga.shared.reader

import io.kamel.core.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderMotionTest {

    @Test
    fun memoryHitStartsSettled() =
        assertEquals(RevealStart(alpha = 1f, scale = 1f), pageRevealStart(DataSource.Memory))

    @Test
    fun freshLoadStartsHiddenAndEnlarged() =
        assertEquals(RevealStart(alpha = 0f, scale = 1.04f), pageRevealStart(DataSource.Network))
}
