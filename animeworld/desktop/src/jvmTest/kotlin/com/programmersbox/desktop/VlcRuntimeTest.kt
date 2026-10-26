package com.programmersbox.desktop

import com.programmersbox.anime.shared.videoplayer.VlcRuntime
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VlcRuntimeTest {

    @Test
    fun loadsBundledLibVlc() {
        val resourcesDir = File(System.getProperty("compose.application.resources.dir")).absolutePath

        val factory = VlcRuntime.factory()

        assertTrue(factory.isSuccess, "factory failed: ${factory.exceptionOrNull()}")
        assertTrue(VlcRuntime.version!!.startsWith("3.0.24"), "version was ${VlcRuntime.version}")
        assertTrue(VlcRuntime.loadedFrom!!.startsWith(resourcesDir), "loaded from ${VlcRuntime.loadedFrom}")
        assertFalse(VlcRuntime.loadedFrom!!.startsWith("/Applications"))
    }
}
