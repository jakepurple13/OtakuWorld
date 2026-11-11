package com.programmersbox.kmpuiviews.domain.dailyoverview

import kotlin.test.Test
import kotlin.test.assertEquals

class NanoStatusTest {
    @Test
    fun availableIsReady() {
        assertEquals(EngineStatus.Ready, NanoFeatureStatus.Available.toEngineStatus())
    }

    @Test
    fun downloadableNeedsSetup() {
        assertEquals(
            EngineStatus.NeedsSetup("Downloading Gemini Nano"),
            NanoFeatureStatus.Downloadable.toEngineStatus(),
        )
    }

    @Test
    fun downloadingNeedsSetup() {
        assertEquals(
            EngineStatus.NeedsSetup("Downloading Gemini Nano"),
            NanoFeatureStatus.Downloading.toEngineStatus(),
        )
    }

    @Test
    fun reasonCanBeOverridden() {
        assertEquals(
            EngineStatus.NeedsSetup("x"),
            NanoFeatureStatus.Downloading.toEngineStatus("x"),
        )
    }

    @Test
    fun unavailableIsUnavailable() {
        assertEquals(EngineStatus.Unavailable, NanoFeatureStatus.Unavailable.toEngineStatus())
    }
}
