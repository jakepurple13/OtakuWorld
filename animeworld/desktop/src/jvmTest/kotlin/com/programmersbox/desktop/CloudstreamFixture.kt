package com.programmersbox.desktop

import org.junit.Assume.assumeTrue
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

/**
 * A real CloudStream plugin, downloaded at test time (never committed: its source repo has no license).
 *
 * - Source: https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/f6b772570e09540aae5740285dd0ede75591802e/HiAnime.cs3
 *   (the `builds` branch at that commit, 2026-10-15)
 * - Repo (what a user adds): [REPO_URL]; its plugin list is
 *   https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/refs/heads/builds/plugins.json
 * - internalName `HiAnime`, version 2, tvTypes Anime/OVA, 87083 bytes
 * - sha256 `82e56188b75c022731975a2c1118842e8ad8f3bab4a5a31c0617e3b67fd96fe9`
 * - manifest.json: `{"pluginClassName":"com.HiAnime.HiAnimeProviderPlugin","name":"HiAnime","version":2,"requiresResources":false}`
 *
 * Chosen because it needs neither resources nor a WebView, and it links against the appcompat/fragment
 * settings-dialog types every phisher98 plugin uses. Untrusted code: only convert, link and load it in tests.
 *
 * Cached at `<cloudstream.fixture.cache>/<sha256>.cs3` (the Gradle test tasks set the property to
 * `<gradle user home>/caches/otaku-test-fixtures/cloudstream`; fallback `build/test-fixtures`). A download whose
 * SHA-256 differs from [SHA256] fails the test; one that cannot be fetched at all skips it ([assumeAvailable]).
 * `-Pcloudstream.fixture.url=...` / `-Pcloudstream.fixture.cache=...` override the URL and cache dir (offline checks).
 *
 * NOTE: copy of `:source_utilities:cloudstream`'s jvmTest CloudstreamFixture (test sources are not shared between modules);
 * keep the two in sync.
 */
object CloudstreamFixture {
    const val REPO_URL = "https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/builds/repo.json"
    const val INTERNAL_NAME = "HiAnime"
    const val VERSION = 2
    const val PLUGIN_CLASS = "com.HiAnime.HiAnimeProviderPlugin"
    const val SHA256 = "82e56188b75c022731975a2c1118842e8ad8f3bab4a5a31c0617e3b67fd96fe9"
    const val URL = "https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/f6b772570e09540aae5740285dd0ede75591802e/HiAnime.cs3"

    private const val TIMEOUT_MS = 15_000

    private val url get() = System.getProperty("cloudstream.fixture.url")?.takeIf { it.isNotBlank() } ?: URL
    private val cacheDir get() = File(System.getProperty("cloudstream.fixture.cache")?.takeIf { it.isNotBlank() } ?: "build/test-fixtures")

    // Download problems (offline, 404, timeout) skip; a wrong hash is an error.
    private val resolved: Result<File> by lazy { runCatching { fetch() } }

    /** The verified plugin file. Skips the calling test if it cannot be downloaded; fails on a hash mismatch. */
    val cs3: File
        get() {
            assumeAvailable()
            return resolved.getOrThrow()
        }

    /** Skips the calling test when the fixture cannot be fetched. Throws if the fetched file has the wrong hash. */
    fun assumeAvailable() {
        val error = resolved.exceptionOrNull()
        if (error is HashMismatch) throw error
        assumeTrue("CloudStream fixture unavailable: $error", error == null)
    }

    private class HashMismatch(message: String) : AssertionError(message)

    private fun fetch(): File {
        val target = File(cacheDir, "$SHA256.cs3")
        // Files only land here after verification (atomic move below), but re-check in case the cache was tampered with.
        if (target.isFile && sha256(target) == SHA256) return target
        target.delete()

        val uri = URI(url)
        require(uri.scheme == "https") { "fixture URL must be https: $uri" }
        target.parentFile.mkdirs()
        val tmp = File(target.parentFile, "${target.name}.${UUID.randomUUID()}.tmp")
        try {
            val connection = uri.toURL().openConnection() as HttpsURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = true
            check(connection.responseCode == 200) { "GET $uri -> ${connection.responseCode}" }
            connection.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            val actual = sha256(tmp)
            if (actual != SHA256) throw HashMismatch("fixture from $uri has sha256 $actual, expected $SHA256")
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            return target
        } finally {
            tmp.delete()
        }
    }

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
}
