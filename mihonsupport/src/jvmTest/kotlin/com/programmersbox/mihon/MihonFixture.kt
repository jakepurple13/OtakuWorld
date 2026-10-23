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
        get() = File(checkNotNull(MihonFixture::class.java.classLoader.getResource(RESOURCE_PATH)) { "missing $RESOURCE_PATH" }.toURI())
}
