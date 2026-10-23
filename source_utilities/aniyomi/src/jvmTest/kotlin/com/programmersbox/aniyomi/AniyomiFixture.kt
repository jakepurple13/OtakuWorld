package com.programmersbox.aniyomi

import java.io.File

/**
 * Real Yuzono Aniyomi extension APK (AnimeOnsen, JSON API, not Cloudflare-protected) used by JVM tests
 * (no network needed). Fetched from the repo listed in its index.min.json:
 * https://raw.githubusercontent.com/yuzono/anime-repo/repo/apk/aniyomi-all.animeonsen-v14.10.apk
 * sha256 566c861c9fc19caeb34a1418c86821b467688ed9a2f87618b0ec7e8bb5aba1bd
 * Repo signing fingerprint: meta.signingKeyFingerprint in the repo's repo.json
 * (https://raw.githubusercontent.com/yuzono/anime-repo/repo/repo.json).
 * It declares its lib as `tachiyomix.extensionLib`.
 */
object AniyomiFixture {
    const val PACKAGE = "eu.kanade.tachiyomi.animeextension.all.animeonsen"
    const val VERSION_NAME = "14.10"
    const val VERSION_CODE = 10L
    /** Value of the manifest `tachiyomix.extensionLib` meta-data. */
    const val LIB_VERSION = 14.0
    const val SOURCE_CLASS = ".AnimeOnsen"
    const val REPO_FINGERPRINT = "cbec121aa82ebb02aaa73806992e0368a97d47b5451ed6524816d03084c45905"
    const val RESOURCE_PATH = "fixtures/aniyomi-all.animeonsen-v14.10.apk"

    val apk: File get() = resource(RESOURCE_PATH)

    /**
     * Keiyoushi MangaPill (a manga extension, not an anime one), copied from the mihonsupport fixtures.
     * Must be rejected by the Aniyomi store.
     */
    val mangaApk: File get() = resource("fixtures/tachiyomi-en.mangapill-v1.6.0.apk")

    private fun resource(path: String): File =
        File(checkNotNull(AniyomiFixture::class.java.classLoader.getResource(path)) { "missing $path" }.toURI())
}
