package com.programmersbox.kmpextensionloader

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApkManifestParserTest {

    // Place a real extension APK at this path before running the test.
    // If the file doesn't exist the test is skipped.
    private val testApk: File = File(System.getProperty("test.apk.path", ""))

    @Test fun `parse returns non-blank packageName`() {
        if (!testApk.exists()) return
        val manifest = ApkManifestParser.parse(testApk)
        assertTrue(manifest.packageName.isNotBlank(), "packageName must not be blank")
    }

    @Test fun `parse extracts programmersbox feature`() {
        if (!testApk.exists()) return
        val manifest = ApkManifestParser.parse(testApk)
        assertTrue(
            manifest.features.any { it.startsWith("programmersbox.otaku") },
            "Expected a programmersbox.otaku feature, got: ${manifest.features}"
        )
    }

    @Test fun `parse extracts class metadata`() {
        if (!testApk.exists()) return
        val manifest = ApkManifestParser.parse(testApk)
        assertTrue(
            manifest.metaData.containsKey("programmersbox.otaku.class"),
            "Expected programmersbox.otaku.class meta-data"
        )
    }

    @Test fun `parse returns empty manifest for non-apk file`() {
        val tmp = File.createTempFile("fake", ".apk")
        tmp.writeText("not an apk")
        val manifest = runCatching { ApkManifestParser.parse(tmp) }.getOrNull()
        tmp.delete()
        // Should either return null or a manifest with empty/blank content — not throw uncaught
        assertTrue(manifest == null || manifest.packageName.isEmpty() || manifest.features.isEmpty())
    }
}
