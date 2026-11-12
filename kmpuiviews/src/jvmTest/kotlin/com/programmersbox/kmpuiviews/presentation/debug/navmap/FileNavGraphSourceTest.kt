package com.programmersbox.kmpuiviews.presentation.debug.navmap

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileNavGraphSourceTest {

    @Test
    fun readsFileFromPath() = runTest {
        val file = File.createTempFile("navgraph", ".json").apply { writeText("{\"app\":\"x\"}"); deleteOnExit() }
        val source = FileNavGraphSource { file.absolutePath }
        assertTrue(source.available)
        assertEquals("{\"app\":\"x\"}", source.read())
    }

    @Test
    fun noPathMeansNoGraph() = runTest {
        val source = FileNavGraphSource { null }
        assertFalse(source.available)
        assertNull(source.read())
    }

    @Test
    fun missingFileMeansNoGraph() = runTest {
        val source = FileNavGraphSource { "/definitely/not/here/navgraph.json" }
        assertFalse(source.available)
        assertNull(source.read())
    }
}
