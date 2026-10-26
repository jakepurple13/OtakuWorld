package com.programmersbox.aniyomi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AniyomiApkInspectorTest {

    @Test
    fun inspectsFixture() {
        val info = AniyomiApkInspector.inspect(AniyomiFixture.apk)
        assertTrue(info.isExtension)
        assertTrue(info.hasMetadata)
        assertEquals(AniyomiFixture.PACKAGE, info.packageName)
        assertEquals(AniyomiFixture.VERSION_NAME, info.versionName)
        assertEquals(AniyomiFixture.VERSION_CODE, info.versionCode)
        assertEquals(AniyomiFixture.SOURCE_CLASS, info.sourceClasses)
        assertEquals("AnimeOnsen", info.label)
        assertFalse(info.nsfw)
        assertEquals(AniyomiFixture.LIB_VERSION, info.libVersion)
        assertTrue(AniyomiLib.isSupported(info.libVersion))
        assertEquals(AniyomiFixture.REPO_FINGERPRINT, info.fingerprints.last())
    }

    @Test
    fun mangaApkIsNotAnimeExtension() {
        assertFalse(AniyomiApkInspector.inspect(AniyomiFixture.mangaApk).isExtension)
    }
}
