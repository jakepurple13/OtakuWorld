package com.programmersbox.kmpuiviews.updates

import dev.nucleusframework.core.runtime.Platform
import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseSelectionTest {
    private fun rel(tag: String, pre: Boolean, vararg assets: String) =
        GhRelease(tag, pre, "https://github.com/jakepurple13/OtakuWorld/releases/tag/$tag", assets.map(::GhAsset))

    @Test
    fun trackFromVersion() {
        assertEquals(UpdateTrack.Nightly, UpdateTrack.fromVersion("33.0.0-alpha.20261030.1415"))
        assertEquals(UpdateTrack.Stable, UpdateTrack.fromVersion("33.0.0"))
    }

    @Test
    fun metadataNames() {
        assertEquals("mangaworld-mac.yml", metadataFileName("mangaworld", Platform.MacOS))
        assertEquals("mangaworld.yml", metadataFileName("mangaworld", Platform.Windows))
        assertEquals("mangaworld-linux.yml", metadataFileName("mangaworld", Platform.Linux))
    }

    @Test
    fun stableSkipsPrereleasesAndOddTags() {
        val r = listOf(
            rel("nightly_2026_10_30", true, "mangaworld-mac.yml"),
            rel("v32.0.0_test", false, "mangaworld-mac.yml"),
            rel("v32.1.2", false, "mangaworld-mac.yml"),
        )
        assertEquals("v32.1.2", selectStable(r, "mangaworld-mac.yml")?.tagName)
    }

    @Test
    fun stableSkipsReleaseMissingThisPlatformsYml() {
        val r = listOf(rel("v33.0.0", false, "mangaworld-linux.yml"), rel("v32.1.2", false, "mangaworld-mac.yml"))
        assertEquals("v32.1.2", selectStable(r, "mangaworld-mac.yml")?.tagName)
    }

    @Test
    fun nightlyIgnoresOtherAppsFiles() {
        val r = listOf(
            rel("nightly_2026_10_30", true, "animeworld-mac.yml"),
            rel("nightly_2026_10_29", true, "mangaworld-mac.yml"),
        )
        assertEquals("nightly_2026_10_29", selectNightly(r, "mangaworld-mac.yml")?.tagName)
    }

    @Test
    fun nightlyPicksStableWhenHigher() {
        val n = rel("nightly_2026_10_30", true) to "33.0.0-alpha.20261030.1415"
        val s = rel("v33.0.0", false) to "33.0.0"
        assertEquals("v33.0.0", pickHigher(n, s)?.tagName)
    }

    @Test
    fun nightlyFallsBackToStableWhenNoNightly() {
        assertEquals("v33.0.0", pickHigher(null, rel("v33.0.0", false) to "33.0.0")?.tagName)
    }

    @Test
    fun sameDayRerunSortsHigher() {
        val a = rel("nightly_2026_10_30", true) to "33.0.0-alpha.20261030.0100"
        val b = rel("nightly_2026_10_30_2", true) to "33.0.0-alpha.20261030.1415"
        assertEquals("nightly_2026_10_30_2", pickHigher(a, b)?.tagName)
        assertEquals("nightly_2026_10_30_2", pickHigher(b, a)?.tagName)
    }
}
