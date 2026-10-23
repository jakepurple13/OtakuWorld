package com.programmersbox.mihon

import java.io.File

/**
 * Real Keiyoushi extension APK used by JVM tests (no network needed).
 * Fetched from the Keiyoushi GitHub release listed in the repo's index.pb:
 * https://github.com/keiyoushi/extensions/releases/download/bdcf84f/tachiyomi-en.mangapill-v1.6.0.apk
 * Repo signing fingerprint: signingKeyFingerprint in the repo's repo.json
 * (https://raw.githubusercontent.com/keiyoushi/extensions/repo/repo.json).
 */
object MihonFixture {
    const val PACKAGE = "eu.kanade.tachiyomi.extension.en.mangapill"
    const val VERSION_NAME = "1.6.0"
    const val VERSION_CODE = 106000L
    const val SOURCE_CLASS = "keiyoushi.source.Generated"
    const val REPO_FINGERPRINT = "9add655a78e96c4ec7a53ef89dccb557cb5d767489fac5e785d671a5a75d4da2"
    const val RESOURCE_PATH = "fixtures/tachiyomi-en.mangapill-v1.6.0.apk"

    val apk: File
        get() = resource(RESOURCE_PATH)

    /**
     * Multi-language Keiyoushi MangaDex (61 sources from one factory). Its settings code references
     * androidx.preference/android.widget types in signatures the JVM verifier checks when the source
     * class links, so it guards against missing stub types, not just missing members.
     * sha256 12fde77601d48c8b231340694db8446fba5a555ced4109b231c9d2e3000915fb, signed with [REPO_FINGERPRINT].
     */
    object MangaDex {
        const val PACKAGE = "eu.kanade.tachiyomi.extension.all.mangadex"
        const val VERSION_NAME = "1.6.0"
        const val VERSION_CODE = 106000L
        const val SOURCE_COUNT = 61
        const val RESOURCE_PATH = "fixtures/tachiyomi-all.mangadex-v1.6.0.apk"

        val apk: File get() = resource(RESOURCE_PATH)
    }

    private fun resource(path: String): File =
        File(checkNotNull(MihonFixture::class.java.classLoader.getResource(path)) { "missing $path" }.toURI())
}
