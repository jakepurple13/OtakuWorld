package com.programmersbox.cloudstream

import java.io.File

/**
 * A real CloudStream plugin, committed as `src/jvmTest/resources/fixtures/HiAnime.cs3`.
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
 */
object CloudstreamFixture {
    const val REPO_URL = "https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/builds/repo.json"
    const val INTERNAL_NAME = "HiAnime"
    const val VERSION = 2
    const val PLUGIN_CLASS = "com.HiAnime.HiAnimeProviderPlugin"
    const val SHA256 = "82e56188b75c022731975a2c1118842e8ad8f3bab4a5a31c0617e3b67fd96fe9"

    val cs3: File
        get() = File(
            requireNotNull(CloudstreamFixture::class.java.getResource("/fixtures/$INTERNAL_NAME.cs3")) {
                "fixtures/$INTERNAL_NAME.cs3 missing from the jvmTest resources"
            }.toURI()
        )
}
