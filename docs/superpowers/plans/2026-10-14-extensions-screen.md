# Extensions Screen Overhaul ("Catalog Shelves") Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Installed/Remote extension screens with a summary-tile Installed pane, a shelf-based Browse pane, a "See all" catalog page, and a detail bottom sheet with live install progress.

**Architecture:** All decision logic (installed ↔ remote matching, update/installing state, filters, sorting, search) lives in pure top-level functions in `ExtensionStates.kt`, unit-tested on JVM. Remote catalog fetching moves into a small `RemoteCatalogs` holder with per-catalog retry. `ExtensionListViewModel` only wires flows into those functions; the Compose UI is split into focused files that render the derived state.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform Material 3 (+ adaptive `ListDetailPaneScaffold`), Navigation3 via Koin `navigation<>` DSL, Koin, Coil3 `AsyncImage`, kotlin.test + kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-10-14-extensions-screen-design.md`

## Global Constraints

- Local builds use the `noFirebase` flavor: `./gradlew :mangaworld:assembleNoFirebaseDebug`.
- `KmpRemoteSources` new fields MUST be defaulted: `lang: String = ""`, `nsfw: Boolean = false` (plugin/serialization compatibility).
- Mihon mappers stop appending `" (18+)"` to `name`.
- Multi-language Mihon v2 extensions get `lang = "all"`; UI displays `"all"` as `"Multi"`.
- Shelf preview = first 6 tiles; grids are 3 columns.
- Shelf/grid sort: update available → installing → not installed → installed, ties by name (case-insensitive).
- Install has no confirm dialog; every uninstall confirms.
- "Check for Source Updates" keeps its 10-minute debounce.
- New nav key: `Screen.ExtensionListScreen.Catalog(catalogName: String)`.
- Match style of surrounding code: hardcoded English UI strings in this screen (as today), Material Icons `Icons.Default.*`, 4-space indent.
- Before installing a debug APK on the user's phone, diff the Room DB versions in this worktree against the main checkout (`/Users/jacobrein/StudioProjects/OtakuWorld`); a higher version in the worktree blocks installing over the user's data.

## Review Focus

1. Installed extensions with a blank `packageName` (OtakuWorld catalog placeholder entries) or blank `apiService.baseUrl` (untrusted stubs) must not match arbitrary remote entries. Pinned in Task 2 (`blankKeysNeverMatch`).
2. "Update all" pressed twice, or while one update is already installing, must not start a duplicate download for the installing item. Pinned in Task 2 (`updateLinksSkipInstalling`).
3. A failed install (`DownloadAndInstallStatus.Error`) must remain visible as the item's state (not silently revert to Install), while `Installed`/`Cancelled` entries must not hold the item in "installing". Pinned in Task 2 (`errorStaysVisibleInstalledDoesNot`).
4. A catalog reload (e.g. custom URL list changes) must not flash already-loaded shelves back to a loading state. Pinned in Task 3 (`reloadKeepsExistingStateUntilResult`).
5. Search with surrounding whitespace (" dex ") should still match. Pinned in Task 2 (`searchTrimsAndIgnoresCase`).

---

## File Structure

| File | Responsibility |
|---|---|
| `kmpmodels/src/commonMain/kotlin/com/programmersbox/kmpmodels/ApiService.kt` (modify) | add `lang`, `nsfw` to `KmpRemoteSources` |
| `mihonsupport/src/commonMain/kotlin/com/programmersbox/mihon/repo/MihonRepoClient.kt` (modify) | fill `lang`/`nsfw`, drop "(18+)" suffix |
| `kmpuiviews/.../settings/extensions/ExtensionStates.kt` (create) | pure state model + functions |
| `kmpuiviews/.../settings/extensions/RemoteCatalogs.kt` (create) | per-catalog fetch state with retry |
| `kmpuiviews/.../settings/extensions/ExtensionListViewModel.kt` (modify) | wiring only |
| `kmpuiviews/.../settings/extensions/ExtensionParts.kt` (create) | icon, tile, summary tiles, installed row, uninstall dialog |
| `kmpuiviews/.../settings/extensions/ExtensionDetailSheet.kt` (create) | bottom sheet for a remote item |
| `kmpuiviews/.../settings/extensions/InstalledPane.kt` (create) | list pane |
| `kmpuiviews/.../settings/extensions/BrowsePane.kt` (create) | detail pane |
| `kmpuiviews/.../settings/extensions/ExtensionCatalogScreen.kt` (create) | "See all" page |
| `kmpuiviews/.../settings/extensions/ExtensionListScreen.kt` (modify) | scaffold, app bar, FAB, custom URL sheet |
| `kmpuiviews/.../presentation/Screen.kt` (modify) | `Catalog` key |
| `kmpuiviews/.../presentation/navigation/Nav3Graph.kt` (modify) | `Catalog` entry |

`kmpuiviews/...` = `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation`. Tests go in `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/`.

Note: `UIViews/.../navigation/Nav3Graph.kt` has an `entryGraph()` that is never called (`grep -rn "entryGraph(" --include='*.kt' .` shows only its declaration). Do not add the new entry there.

---

### Task 1: `lang`/`nsfw` on remote sources

**Files:**
- Modify: `kmpmodels/src/commonMain/kotlin/com/programmersbox/kmpmodels/ApiService.kt:121-128`
- Modify: `mihonsupport/src/commonMain/kotlin/com/programmersbox/mihon/repo/MihonRepoClient.kt:150-166`
- Test: `mihonsupport/src/commonTest/kotlin/com/programmersbox/mihon/repo/MihonRepoClientTest.kt`, `mihonsupport/src/commonTest/kotlin/com/programmersbox/mihon/repo/MihonIndexV2Test.kt`

**Interfaces:**
- Produces: `KmpRemoteSources.lang: String`, `KmpRemoteSources.nsfw: Boolean`.

- [ ] **Step 1: Update tests to the new contract**

In `MihonRepoClientTest.mapsSupportedEntries`, replace `assertEquals("MangaDex (18+)", dex.name)` with:

```kotlin
        assertEquals("MangaDex", dex.name)
        assertEquals("all", dex.lang)
        assertEquals(true, dex.nsfw)
```

(Only MangaDex survives the v1 fixture's lib filter, so the non-nsfw case is covered by the v2 assertions below.)

In `MihonIndexV2Test.kt` `assertV2Mapping`, replace `assertEquals("Dex (18+)", dex.name)` with:

```kotlin
    assertEquals("Dex", dex.name)
    assertEquals(true, dex.nsfw)
    assertEquals("en", dex.lang)
    assertEquals(false, result[1].nsfw)
```

Add to `MihonIndexV2Test`:

```kotlin
    @Test
    fun multiLanguageV2ExtensionIsAll() = runTest {
        val multi = v2Extension("Multi", "pkg.multi", "1.4").copy(
            sources = listOf(
                V2Source(id = 1, name = "Multi", language = "en", homeUrl = "https://m.example"),
                V2Source(id = 2, name = "Multi", language = "es", homeUrl = "https://m.example"),
            )
        )
        val index = ProtoBuf.encodeToByteArray(
            V2Index(name = "Store", signingKey = "indexKey", extensions = V2InlineList(V2ExtensionList(listOf(multi))))
        )
        val repo = MihonRepoClient(v2Client(mapOf("$base/repo.json" to { repoJson(indexUrl) }, indexUrl to { index })))
        assertEquals("all", repo.fetch(listOf(base)).single().lang)
    }
```

(`v2Extension` signature: `v2Extension(name, pkg, lib, warning = ...)`, defined at the top of `MihonIndexV2Test.kt`; check its parameter list and pass `lib = "1.4"` positionally as above.)

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :mihonsupport:jvmTest --tests "com.programmersbox.mihon.repo.*"`
Expected: compile FAIL — `Unresolved reference 'lang'` / `'nsfw'`.

- [ ] **Step 3: Implement**

`ApiService.kt` — add the two fields at the end of `KmpRemoteSources`:

```kotlin
data class KmpRemoteSources(
    val name: String,
    val packageName: String,
    val version: String,
    val iconUrl: String,
    val downloadLink: String,
    val sources: List<KmpSources>,
    val lang: String = "",
    val nsfw: Boolean = false,
)
```

`MihonRepoClient.kt` — replace both mappers:

```kotlin
    private fun MihonIndexEntry.toRemote(base: String) = KmpRemoteSources(
        name = name.removePrefix("Tachiyomi: "),
        packageName = pkg,
        version = version,
        iconUrl = "$base/icon/$pkg.png",
        downloadLink = "$base/apk/$apk",
        sources = sources.map { KmpSources(name = it.name, baseUrl = it.baseUrl, version = version) },
        lang = lang,
        nsfw = nsfw == 1,
    )

    private fun V2Extension.toRemote() = KmpRemoteSources(
        name = name,
        packageName = packageName,
        version = versionName,
        iconUrl = resources?.iconUrl.orEmpty(),
        downloadLink = resources?.apkUrl.orEmpty(),
        sources = sources.map { KmpSources(name = it.name, baseUrl = it.homeUrl, version = versionName) },
        lang = sources.map { it.language }.filter { it.isNotBlank() }.distinct().let {
            when (it.size) {
                0 -> ""
                1 -> it.single()
                else -> "all"
            }
        },
        nsfw = contentWarning == V2ContentWarning.CONTENT_WARNING_NSFW,
    )
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :mihonsupport:jvmTest --tests "com.programmersbox.mihon.repo.*"`
Expected: PASS. Also grep for any other "(18+)" assertion: `grep -rn '18+' mihonsupport/src` → only none or UI code.

- [ ] **Step 5: Commit**

```bash
git add kmpmodels/src/commonMain/kotlin/com/programmersbox/kmpmodels/ApiService.kt mihonsupport/src
git commit -m "feat(extensions): carry lang and nsfw on KmpRemoteSources"
```

---

### Task 2: Pure extension state (`ExtensionStates.kt`)

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionStates.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionStatesTest.kt`

**Interfaces:**
- Consumes: `KmpRemoteSources.lang/nsfw` (Task 1); `RemoteState`, `RemoteViewState`, `RemoteErrorState` (existing in `ExtensionListViewModel.kt`; Task 3 moves them and adds `RemoteLoadingState` and `RemoteErrorState.message`). This task's `catalogUi` only references `RemoteViewState` and `RemoteErrorState` without `message`; Task 3 extends it.
- Produces (all in package `com.programmersbox.kmpuiviews.presentation.settings.extensions`):
  - `sealed interface RemoteItemState { NotInstalled; Installed(installed); UpdateAvailable(installed, installedVersion); Installing(status) }`
  - `data class RemoteItem(remote: KmpRemoteSources, catalog: String, state: RemoteItemState)` with `val key: String`
  - `data class InstalledItem(info, version, update: KmpRemoteSources?, installing: DownloadAndInstallStatus?)` with `val untrusted: KmpUntrustedApiService?`
  - `enum class InstalledFilter { None, Updates, Untrusted }`, `fun InstalledFilter.toggle(tapped: InstalledFilter): InstalledFilter`
  - `data class InstalledSummary(installed: Int, updates: Int, untrusted: Int)`
  - `sealed interface CatalogUi { Loading; Error(message: String?); Loaded(items: List<RemoteItem>) }`
  - `fun KmpRemoteSources.findInstalled(installed: List<KmpSourceInformation>): KmpSourceInformation?`
  - `fun KmpSourceInformation.findRemote(remotes: List<KmpRemoteSources>): KmpRemoteSources?`
  - `fun List<DownloadAndInstallState>.statusFor(url: String): DownloadAndInstallStatus?`
  - `fun resolveState(remote, installed, versionOf: (KmpSourceInformation) -> String, downloads): RemoteItemState`
  - `fun installedItems(installed, remotes, versionOf, downloads): List<InstalledItem>`
  - `fun List<InstalledItem>.summary(): InstalledSummary`
  - `fun List<InstalledItem>.filteredBy(filter: InstalledFilter): List<InstalledItem>`
  - `fun List<InstalledItem>.byCatalog(): Map<String?, List<InstalledItem>>`
  - `fun List<InstalledItem>.updateLinks(): List<String>`
  - `fun List<KmpSourceInformation>.canUninstallAll(): Boolean`
  - `fun catalogUi(states: Map<String, RemoteState>, installed, versionOf, downloads): Map<String, CatalogUi>`
  - `fun List<RemoteItem>.shelfOrder(): List<RemoteItem>`
  - `fun List<RemoteItem>.search(query: String): List<RemoteItem>`
  - `fun List<RemoteItem>.languages(): List<String>`
  - `fun List<RemoteItem>.filterCatalog(lang: String?, hideNsfw: Boolean): List<RemoteItem>`
  - `fun displayLang(lang: String): String`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.extensions

import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.KmpSources
import com.programmersbox.kmpmodels.KmpUntrustedApiService
import com.programmersbox.kmpuiviews.repository.DownloadAndInstallState
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExtensionStatesTest {
    private fun service(url: String) = object : KmpApiService { override val baseUrl = url }
    private fun untrustedService() = object : KmpUntrustedApiService {
        override val baseUrl = ""
        override val reason = "Signing key not trusted"
        override val canTrust = true
        override suspend fun trust() {}
    }

    private fun info(pkg: String, url: String = "https://$pkg.example", service: KmpApiService = service(url)) =
        KmpSourceInformation(service, pkg, null, pkg)

    private fun remote(
        pkg: String,
        version: String = "1.4.10",
        url: String = "https://$pkg.example",
        lang: String = "en",
        nsfw: Boolean = false,
        name: String = pkg,
    ) = KmpRemoteSources(name, pkg, version, "", "https://repo/$pkg.apk", listOf(KmpSources(name, url, version)), lang, nsfw)

    private val versions = mapOf("dex" to "1.4.8", "fire" to "1.4.10")
    private val versionOf: (KmpSourceInformation) -> String = { versions[it.packageName] ?: "1.0.0" }

    @Test
    fun matchesByPackageName() {
        assertEquals("dex", remote("dex", url = "https://other").findInstalled(listOf(info("dex")))?.packageName)
    }

    @Test
    fun fallsBackToBaseUrl() {
        val installed = info("legacy.pkg", url = "https://dex.example")
        assertEquals(installed, remote("dex").findInstalled(listOf(installed)))
        assertEquals(remote("dex"), installed.findRemote(listOf(remote("dex"))))
    }

    @Test
    fun blankKeysNeverMatch() {
        val placeholder = KmpSourceInformation(service(""), "MangaWorld", null, "")
        val blankRemote = remote("").copy(sources = listOf(KmpSources("x", "", "1")))
        assertNull(blankRemote.findInstalled(listOf(placeholder)))
        assertNull(placeholder.findRemote(listOf(blankRemote)))
    }

    @Test
    fun resolvesUpdateInstalledAndNotInstalled() {
        val installed = listOf(info("dex"), info("fire"))
        assertIs<RemoteItemState.UpdateAvailable>(resolveState(remote("dex"), installed, versionOf, emptyList()))
        assertIs<RemoteItemState.Installed>(resolveState(remote("fire"), installed, versionOf, emptyList()))
        assertEquals(RemoteItemState.NotInstalled, resolveState(remote("new"), installed, versionOf, emptyList()))
        assertEquals("1.4.8", (resolveState(remote("dex"), installed, versionOf, emptyList()) as RemoteItemState.UpdateAvailable).installedVersion)
    }

    @Test
    fun installingTakesPrecedence() {
        val downloads = listOf(DownloadAndInstallState("https://repo/dex.apk", "dex", "1", DownloadAndInstallStatus.Downloading(0.4f)))
        val state = resolveState(remote("dex"), listOf(info("dex")), versionOf, downloads)
        assertEquals(RemoteItemState.Installing(DownloadAndInstallStatus.Downloading(0.4f)), state)
    }

    @Test
    fun errorStaysVisibleInstalledDoesNot() {
        val err = DownloadAndInstallStatus.Error(com.programmersbox.kmpuiviews.utils.InstallErrorReason.GENERIC, "boom")
        assertEquals(err, listOf(DownloadAndInstallState("u", "n", "1", err)).statusFor("u"))
        assertNull(listOf(DownloadAndInstallState("u", "n", "1", DownloadAndInstallStatus.Installed)).statusFor("u"))
        assertNull(listOf(DownloadAndInstallState("u", "n", "1", DownloadAndInstallStatus.Cancelled)).statusFor("u"))
        // latest entry for a url wins
        val list = listOf(
            DownloadAndInstallState("u", "n", "1", err),
            DownloadAndInstallState("u", "n", "2", DownloadAndInstallStatus.Installing),
        )
        assertEquals(DownloadAndInstallStatus.Installing, list.statusFor("u"))
    }

    @Test
    fun summaryAndFilters() {
        val untrusted = KmpSourceInformation(untrustedService(), "asura", null, "asura")
        val items = installedItems(listOf(info("dex"), info("dex"), info("fire"), untrusted), listOf(remote("dex"), remote("fire")), versionOf, emptyList())
        assertEquals(InstalledSummary(installed = 3, updates = 1, untrusted = 1), items.summary())
        assertEquals(listOf("dex"), items.filteredBy(InstalledFilter.Updates).map { it.info.packageName })
        assertEquals(listOf("asura"), items.filteredBy(InstalledFilter.Untrusted).map { it.info.packageName })
        assertEquals(3, items.filteredBy(InstalledFilter.None).size)
    }

    @Test
    fun filterToggle() {
        assertEquals(InstalledFilter.Updates, InstalledFilter.None.toggle(InstalledFilter.Updates))
        assertEquals(InstalledFilter.None, InstalledFilter.Updates.toggle(InstalledFilter.Updates))
        assertEquals(InstalledFilter.Untrusted, InstalledFilter.Updates.toggle(InstalledFilter.Untrusted))
        assertEquals(InstalledFilter.None, InstalledFilter.Untrusted.toggle(InstalledFilter.None))
    }

    @Test
    fun updateLinksOnePerUpdate() {
        val items = installedItems(listOf(info("dex"), info("fire")), listOf(remote("dex"), remote("fire")), versionOf, emptyList())
        assertEquals(listOf("https://repo/dex.apk"), items.updateLinks())
    }

    @Test
    fun updateLinksSkipInstalling() {
        val downloads = listOf(DownloadAndInstallState("https://repo/dex.apk", "dex", "1", DownloadAndInstallStatus.Installing))
        val items = installedItems(listOf(info("dex")), listOf(remote("dex")), versionOf, downloads)
        assertEquals(emptyList(), items.updateLinks())
        assertEquals(DownloadAndInstallStatus.Installing, items.single().installing)
    }

    @Test
    fun canUninstallAllOnlyForSinglePackage() {
        assertTrue(listOf(info("a"), info("a")).canUninstallAll())
        assertFalse(listOf(info("a"), info("b")).canUninstallAll())
    }

    @Test
    fun shelfOrderRanksThenName() {
        val items = listOf(
            RemoteItem(remote("b"), "c", RemoteItemState.Installed(info("b"))),
            RemoteItem(remote("Zeta"), "c", RemoteItemState.NotInstalled),
            RemoteItem(remote("alpha"), "c", RemoteItemState.NotInstalled),
            RemoteItem(remote("i"), "c", RemoteItemState.Installing(DownloadAndInstallStatus.Installing)),
            RemoteItem(remote("u"), "c", RemoteItemState.UpdateAvailable(info("u"), "1.0.0")),
        )
        assertEquals(listOf("u", "i", "alpha", "Zeta", "b"), items.shelfOrder().map { it.remote.name })
    }

    @Test
    fun searchTrimsAndIgnoresCase() {
        val items = listOf(RemoteItem(remote("x", name = "MangaDex"), "c", RemoteItemState.NotInstalled))
        assertEquals(1, items.search(" dex ").size)
        assertEquals(1, items.search("").size)
        assertEquals(0, items.search("fire").size)
    }

    @Test
    fun languagesAndCatalogFilter() {
        val items = listOf(
            RemoteItem(remote("a", lang = "es"), "c", RemoteItemState.NotInstalled),
            RemoteItem(remote("b", lang = "all", nsfw = true), "c", RemoteItemState.NotInstalled),
            RemoteItem(remote("c", lang = "en"), "c", RemoteItemState.NotInstalled),
            RemoteItem(remote("d", lang = ""), "c", RemoteItemState.NotInstalled),
        )
        assertEquals(listOf("all", "en", "es"), items.languages())
        assertEquals(listOf("a"), items.filterCatalog("es", hideNsfw = false).map { it.remote.packageName })
        assertEquals(listOf("a", "c", "d"), items.filterCatalog(null, hideNsfw = true).map { it.remote.packageName })
        assertEquals("Multi", displayLang("all"))
        assertEquals("en", displayLang("en"))
    }

    @Test
    fun catalogUiMapsStates() {
        val ui = catalogUi(
            mapOf("ok" to RemoteViewState(listOf(remote("dex"))), "bad" to RemoteErrorState()),
            listOf(info("dex")), versionOf, emptyList(),
        )
        val loaded = assertIs<CatalogUi.Loaded>(ui["ok"])
        assertEquals("ok", loaded.items.single().catalog)
        assertIs<RemoteItemState.UpdateAvailable>(loaded.items.single().state)
        assertIs<CatalogUi.Error>(ui["bad"])
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.settings.extensions.ExtensionStatesTest"`
Expected: compile FAIL — unresolved `resolveState`, `RemoteItemState`, etc.

- [ ] **Step 3: Implement `ExtensionStates.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.extensions

import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.KmpUntrustedApiService
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.repository.DownloadAndInstallState
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus

sealed interface RemoteItemState {
    data object NotInstalled : RemoteItemState
    data class Installed(val installed: KmpSourceInformation) : RemoteItemState
    data class UpdateAvailable(val installed: KmpSourceInformation, val installedVersion: String) : RemoteItemState
    data class Installing(val status: DownloadAndInstallStatus) : RemoteItemState
}

data class RemoteItem(
    val remote: KmpRemoteSources,
    val catalog: String,
    val state: RemoteItemState,
) {
    val key: String get() = "$catalog/${remote.packageName}"
}

data class InstalledItem(
    val info: KmpSourceInformation,
    val version: String,
    /** Remote entry with a newer version, or null when up to date or unmatched. */
    val update: KmpRemoteSources?,
    /** Active or failed download for [update], if any. */
    val installing: DownloadAndInstallStatus?,
) {
    val untrusted: KmpUntrustedApiService? get() = info.apiService as? KmpUntrustedApiService
}

enum class InstalledFilter { None, Updates, Untrusted }

/** Tapping the active filter (or [InstalledFilter.None]) clears it. */
fun InstalledFilter.toggle(tapped: InstalledFilter): InstalledFilter = if (tapped == this) InstalledFilter.None else tapped

data class InstalledSummary(val installed: Int, val updates: Int, val untrusted: Int)

sealed interface CatalogUi {
    data object Loading : CatalogUi
    data class Error(val message: String?) : CatalogUi
    data class Loaded(val items: List<RemoteItem>) : CatalogUi
}

/** Package name first; falls back to the legacy rule of a shared source base URL. Blank keys never match. */
fun KmpRemoteSources.findInstalled(installed: List<KmpSourceInformation>): KmpSourceInformation? =
    installed.find { packageName.isNotBlank() && it.packageName == packageName }
        ?: installed.find { i -> i.apiService.baseUrl.isNotBlank() && sources.any { it.baseUrl == i.apiService.baseUrl } }

fun KmpSourceInformation.findRemote(remotes: List<KmpRemoteSources>): KmpRemoteSources? =
    remotes.find { packageName.isNotBlank() && it.packageName == packageName }
        ?: remotes.find { r -> apiService.baseUrl.isNotBlank() && r.sources.any { it.baseUrl == apiService.baseUrl } }

/** Latest download entry for [url], unless it already finished or was cancelled. Errors stay visible. */
fun List<DownloadAndInstallState>.statusFor(url: String): DownloadAndInstallStatus? =
    lastOrNull { it.url == url }
        ?.status
        ?.takeUnless { it == DownloadAndInstallStatus.Installed || it == DownloadAndInstallStatus.Cancelled }

fun resolveState(
    remote: KmpRemoteSources,
    installed: List<KmpSourceInformation>,
    versionOf: (KmpSourceInformation) -> String,
    downloads: List<DownloadAndInstallState>,
): RemoteItemState {
    downloads.statusFor(remote.downloadLink)?.let { return RemoteItemState.Installing(it) }
    val match = remote.findInstalled(installed) ?: return RemoteItemState.NotInstalled
    val version = versionOf(match)
    return if (AppUpdate.checkForUpdate(version, remote.version)) {
        RemoteItemState.UpdateAvailable(match, version)
    } else {
        RemoteItemState.Installed(match)
    }
}

fun installedItems(
    installed: List<KmpSourceInformation>,
    remotes: List<KmpRemoteSources>,
    versionOf: (KmpSourceInformation) -> String,
    downloads: List<DownloadAndInstallState>,
): List<InstalledItem> = installed
    .distinctBy { it.packageName.ifBlank { it.name } }
    .map { info ->
        val version = versionOf(info)
        val update = info.findRemote(remotes)?.takeIf { AppUpdate.checkForUpdate(version, it.version) }
        InstalledItem(info, version, update, update?.let { downloads.statusFor(it.downloadLink) })
    }

fun List<InstalledItem>.summary() = InstalledSummary(
    installed = size,
    updates = count { it.update != null },
    untrusted = count { it.untrusted != null },
)

fun List<InstalledItem>.filteredBy(filter: InstalledFilter): List<InstalledItem> = when (filter) {
    InstalledFilter.None -> this
    InstalledFilter.Updates -> filter { it.update != null }
    InstalledFilter.Untrusted -> filter { it.untrusted != null }
}

fun List<InstalledItem>.byCatalog(): Map<String?, List<InstalledItem>> = groupBy { it.info.catalog?.name }

/** Download links for every pending update not already downloading/installing. */
fun List<InstalledItem>.updateLinks(): List<String> =
    filter { it.update != null && it.installing == null }.map { it.update!!.downloadLink }

/** A catalog group can only be uninstalled as a unit when it is one APK (e.g. a bridge). */
fun List<KmpSourceInformation>.canUninstallAll(): Boolean = map { it.packageName }.distinct().size == 1

fun catalogUi(
    states: Map<String, RemoteState>,
    installed: List<KmpSourceInformation>,
    versionOf: (KmpSourceInformation) -> String,
    downloads: List<DownloadAndInstallState>,
): Map<String, CatalogUi> = states.mapValues { (name, state) ->
    when (state) {
        is RemoteViewState -> CatalogUi.Loaded(
            state.sources.map { RemoteItem(it, name, resolveState(it, installed, versionOf, downloads)) }.shelfOrder()
        )
        is RemoteErrorState -> CatalogUi.Error(null)
    }
}

private fun RemoteItemState.rank() = when (this) {
    is RemoteItemState.UpdateAvailable -> 0
    is RemoteItemState.Installing -> 1
    RemoteItemState.NotInstalled -> 2
    is RemoteItemState.Installed -> 3
}

fun List<RemoteItem>.shelfOrder(): List<RemoteItem> =
    sortedWith(compareBy<RemoteItem> { it.state.rank() }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.remote.name })

fun List<RemoteItem>.search(query: String): List<RemoteItem> {
    val q = query.trim()
    return if (q.isEmpty()) this else filter { it.remote.name.contains(q, ignoreCase = true) }
}

/** Distinct non-blank languages, "all" first. */
fun List<RemoteItem>.languages(): List<String> =
    map { it.remote.lang }.filter { it.isNotBlank() }.distinct().sortedWith(compareBy<String> { it != "all" }.thenBy { it })

fun List<RemoteItem>.filterCatalog(lang: String?, hideNsfw: Boolean): List<RemoteItem> =
    filter { (lang == null || it.remote.lang == lang) && !(hideNsfw && it.remote.nsfw) }

fun displayLang(lang: String): String = if (lang == "all") "Multi" else lang
```

Note: `RemoteState` is a sealed class with only `RemoteViewState`/`RemoteErrorState` today, so the `when` is exhaustive. Task 3 adds `RemoteLoadingState` and `RemoteErrorState.message` and updates this `when`.

- [ ] **Step 4: Run tests**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.settings.extensions.*"`
Expected: PASS (`ExtensionStatesTest` and existing `InstalledViewStateTest`).

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionStates.kt kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionStatesTest.kt
git commit -m "feat(extensions): pure state model for installed and remote extensions"
```

---

### Task 3: `RemoteCatalogs` holder with retry

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/RemoteCatalogs.kt`
- Modify: `ExtensionListViewModel.kt` (move `RemoteState` classes out), `ExtensionStates.kt` (`catalogUi` `when`)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/RemoteCatalogsTest.kt`

**Interfaces:**
- Consumes: `CatalogUi` (Task 2).
- Produces:
  - `sealed class RemoteState`; `class RemoteViewState(val sources: List<KmpRemoteSources>)`; `class RemoteErrorState(val message: String? = null)`; `data object RemoteLoadingState : RemoteState()` — all now in `RemoteCatalogs.kt`.
  - `class RemoteCatalogs { val states: SnapshotStateMap<String, RemoteState>; suspend fun load(name: String, fetch: suspend () -> List<KmpRemoteSources>); suspend fun retry(name: String) }`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.extensions

import com.programmersbox.kmpmodels.KmpRemoteSources
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class RemoteCatalogsTest {
    private fun remote(pkg: String) = KmpRemoteSources(pkg, pkg, "1.0.0", "", "", emptyList())

    @Test
    fun loadStoresResultAndError() = runTest {
        val catalogs = RemoteCatalogs()
        catalogs.load("ok") { listOf(remote("a")) }
        catalogs.load("bad") { error("index.min.json returned 404") }
        assertEquals(listOf("a"), assertIs<RemoteViewState>(catalogs.states["ok"]).sources.map { it.packageName })
        assertEquals("index.min.json returned 404", assertIs<RemoteErrorState>(catalogs.states["bad"]).message)
    }

    @Test
    fun retryReplacesOnlyTarget() = runTest {
        val catalogs = RemoteCatalogs()
        var fail = true
        catalogs.load("ok") { listOf(remote("a")) }
        catalogs.load("flaky") { if (fail) error("down") else listOf(remote("b")) }
        val okBefore = catalogs.states["ok"]
        fail = false
        catalogs.retry("flaky")
        assertIs<RemoteViewState>(catalogs.states["flaky"])
        assertSame(okBefore, catalogs.states["ok"])
    }

    @Test
    fun retryUnknownIsNoOp() = runTest {
        val catalogs = RemoteCatalogs()
        catalogs.retry("missing")
        assertEquals(emptyMap(), catalogs.states.toMap())
    }

    @Test
    fun firstLoadShowsLoading() = runTest {
        val catalogs = RemoteCatalogs()
        val gate = CompletableDeferred<Unit>()
        val job = launch { catalogs.load("slow") { gate.await(); listOf(remote("a")) } }
        testScheduler.runCurrent()
        assertSame(RemoteLoadingState, catalogs.states["slow"])
        gate.complete(Unit)
        job.join()
        assertIs<RemoteViewState>(catalogs.states["slow"])
    }

    @Test
    fun reloadKeepsExistingStateUntilResult() = runTest {
        val catalogs = RemoteCatalogs()
        catalogs.load("c") { listOf(remote("a")) }
        val loaded = catalogs.states["c"]
        val gate = CompletableDeferred<Unit>()
        val job = launch { catalogs.load("c") { gate.await(); listOf(remote("b")) } }
        testScheduler.runCurrent()
        assertSame(loaded, catalogs.states["c"])
        gate.complete(Unit)
        job.join()
        assertEquals(listOf("b"), assertIs<RemoteViewState>(catalogs.states["c"]).sources.map { it.packageName })
    }
}
```

Also add to `ExtensionStatesTest.catalogUiMapsStates` (replace its body's map and asserts):

```kotlin
        val ui = catalogUi(
            mapOf(
                "ok" to RemoteViewState(listOf(remote("dex"))),
                "bad" to RemoteErrorState("404"),
                "wait" to RemoteLoadingState,
            ),
            listOf(info("dex")), versionOf, emptyList(),
        )
        val loaded = assertIs<CatalogUi.Loaded>(ui["ok"])
        assertEquals("ok", loaded.items.single().catalog)
        assertIs<RemoteItemState.UpdateAvailable>(loaded.items.single().state)
        assertEquals(CatalogUi.Error("404"), ui["bad"])
        assertEquals(CatalogUi.Loading, ui["wait"])
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.settings.extensions.*"`
Expected: compile FAIL — unresolved `RemoteCatalogs`, `RemoteLoadingState`, `message`.

- [ ] **Step 3: Implement**

Delete these from the bottom of `ExtensionListViewModel.kt`:

```kotlin
sealed class RemoteState

class RemoteViewState(
    val sources: List<KmpRemoteSources>,
) : RemoteState()

class RemoteErrorState : RemoteState()
```

Create `RemoteCatalogs.kt`:

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.runtime.mutableStateMapOf
import com.programmersbox.kmpmodels.KmpRemoteSources
import kotlin.coroutines.cancellation.CancellationException

sealed class RemoteState

class RemoteViewState(
    val sources: List<KmpRemoteSources>,
) : RemoteState()

class RemoteErrorState(val message: String? = null) : RemoteState()

data object RemoteLoadingState : RemoteState()

/** Remote extension lists per catalog name, each independently loadable and retryable. */
class RemoteCatalogs {
    val states = mutableStateMapOf<String, RemoteState>()
    private val fetchers = mutableMapOf<String, suspend () -> List<KmpRemoteSources>>()

    /** Shows [RemoteLoadingState] only on first load, so reloads never blank a loaded shelf. */
    suspend fun load(name: String, fetch: suspend () -> List<KmpRemoteSources>) {
        fetchers[name] = fetch
        if (name !in states) states[name] = RemoteLoadingState
        states[name] = fetchState(fetch)
    }

    suspend fun retry(name: String) {
        val fetch = fetchers[name] ?: return
        states[name] = RemoteLoadingState
        states[name] = fetchState(fetch)
    }

    private suspend fun fetchState(fetch: suspend () -> List<KmpRemoteSources>): RemoteState = try {
        RemoteViewState(fetch())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.printStackTrace()
        RemoteErrorState(e.message)
    }
}
```

In `ExtensionStates.kt`, replace the `when` in `catalogUi`:

```kotlin
    when (state) {
        is RemoteViewState -> CatalogUi.Loaded(
            state.sources.map { RemoteItem(it, name, resolveState(it, installed, versionOf, downloads)) }.shelfOrder()
        )
        is RemoteErrorState -> CatalogUi.Error(state.message)
        RemoteLoadingState -> CatalogUi.Loading
    }
```

`ExtensionListViewModel.kt` still constructs `RemoteErrorState()` — compiles unchanged (default arg). Task 4 rewrites that code.

- [ ] **Step 4: Run tests**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.settings.extensions.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions
git commit -m "feat(extensions): per-catalog remote state with retry and error message"
```

---

### Task 4: ViewModel wiring

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionListViewModel.kt` (full rewrite below)
- Delete: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/InstalledViewStateTest.kt` (covered by `ExtensionStatesTest.canUninstallAllOnlyForSinglePackage`)

**Interfaces:**
- Consumes: everything from Tasks 2-3; `SourceInfoRepository.versionName(KmpSourceInformation): String` and `.uninstall(KmpSourceInformation)` (expect class, already in Koin — the current screen `koinInject`s it); `DownloadStateInterface.downloadList: Flow<List<DownloadAndInstallState>>`, `.downloadAndInstall(url: String)`.
- Produces (used by Tasks 5-8):
  - `val installedItems: List<InstalledItem>`
  - `val summary: InstalledSummary`
  - `var filter: InstalledFilter`
  - `val visibleInstalled: Map<String?, List<InstalledItem>>`
  - `val expanded: SnapshotStateMap<String?, Boolean>`
  - `val catalogs: Map<String, CatalogUi>`
  - `fun item(key: String): RemoteItem?`
  - `fun install(remote: KmpRemoteSources)`
  - `fun updateAll()`
  - `fun retry(catalog: String)`
  - `fun trust(source: KmpSourceInformation)`
  - `fun uninstall(source: KmpSourceInformation)`
  - `fun refreshExtensions()`, `var extensionSharing`, `fun shareExtensions(PlatformFile)`, `val hasCustomBridge` (unchanged)

- [ ] **Step 1: Rewrite the ViewModel**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpmodels.KmpExternalApiServicesCatalog
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.KmpUntrustedApiService
import com.programmersbox.kmpmodels.NativeCatalogs
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.OtakuWorldCatalog
import com.programmersbox.kmpuiviews.repository.DownloadAndInstallState
import com.programmersbox.kmpuiviews.repository.DownloadStateInterface
import com.programmersbox.kmpuiviews.repository.SourceInfoRepository
import com.programmersbox.kmpuiviews.utils.DownloadAndInstaller
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ExtensionListViewModel(
    private val sourceRepository: SourceRepository,
    private val sourceLoader: SourceLoader,
    otakuWorldCatalog: OtakuWorldCatalog,
    settingsHandling: NewSettingsHandling,
    val downloadAndInstaller: DownloadAndInstaller,
    private val downloadStateRepository: DownloadStateInterface,
    private val extensionShareHandler: ExtensionShareHandler,
    private val nativeCatalogs: NativeCatalogs,
    private val sourceInfoRepository: SourceInfoRepository,
) : ViewModel() {
    private val installedSources = mutableStateListOf<KmpSourceInformation>()
    private val downloads = mutableStateListOf<DownloadAndInstallState>()
    private val remoteCatalogs = RemoteCatalogs()
    private val versionOf: (KmpSourceInformation) -> String = sourceInfoRepository::versionName

    private val allRemote by derivedStateOf {
        remoteCatalogs.states.values.filterIsInstance<RemoteViewState>().flatMap { it.sources }
    }

    val installedItems by derivedStateOf { installedItems(installedSources, allRemote, versionOf, downloads) }

    val summary by derivedStateOf { installedItems.summary() }

    var filter by mutableStateOf(InstalledFilter.None)

    val visibleInstalled by derivedStateOf { installedItems.filteredBy(filter).byCatalog() }

    /** User expand/collapse choices per catalog; absent = default (expanded when it needs attention). */
    val expanded = mutableStateMapOf<String?, Boolean>()

    val catalogs by derivedStateOf { catalogUi(remoteCatalogs.states, installedSources, versionOf, downloads) }

    fun item(key: String): RemoteItem? = catalogs.values
        .filterIsInstance<CatalogUi.Loaded>()
        .firstNotNullOfOrNull { c -> c.items.find { it.key == key } }

    val hasCustomBridge by derivedStateOf {
        nativeCatalogs.catalogs.isNotEmpty() ||
            installedSources.any { it.catalog is KmpExternalCustomApiServicesCatalog && it.name == "Custom Tachiyomi Bridge" }
    }

    fun install(remote: KmpRemoteSources) {
        downloadStateRepository.downloadAndInstall(remote.downloadLink)
    }

    fun updateAll() {
        installedItems.updateLinks().forEach(downloadStateRepository::downloadAndInstall)
    }

    fun retry(catalog: String) {
        viewModelScope.launch { remoteCatalogs.retry(catalog) }
    }

    fun uninstall(source: KmpSourceInformation) {
        sourceInfoRepository.uninstall(source)
    }

    init {
        downloadStateRepository.downloadList
            .onEach {
                downloads.clear()
                downloads.addAll(it)
            }
            .launchIn(viewModelScope)

        sourceRepository.sources
            .onEach {
                installedSources.clear()
                installedSources.addAll(it)
            }
            .onEach { sources ->
                remoteCatalogs.load("${otakuWorldCatalog.name}World") { otakuWorldCatalog.getRemoteSources() }
                sources
                    .asSequence()
                    .map { it.catalog }
                    .filterIsInstance<KmpExternalApiServicesCatalog>()
                    .filter { it.hasRemoteSources }
                    .distinct()
                    .toList()
                    .forEach { c -> remoteCatalogs.load(c.name) { c.getRemoteSources() } }
            }
            .combine(settingsHandling.customUrls) { sources, urls ->
                (sources.asSequence().map { it.catalog }.filterIsInstance<KmpExternalCustomApiServicesCatalog>() +
                    nativeCatalogs.catalogs.asSequence())
                    .filter { it.hasRemoteSources }
                    .distinct()
                    .toList()
                    .forEach { c -> remoteCatalogs.load(c.name) { c.getRemoteSources(urls) } }
            }
            .launchIn(viewModelScope)
    }

    fun trust(source: KmpSourceInformation) {
        viewModelScope.launch {
            (source.apiService as? KmpUntrustedApiService)?.trust()
            sourceLoader.blockingLoad()
        }
    }

    fun refreshExtensions() {
        viewModelScope.launch { sourceLoader.blockingLoad() }
    }

    var extensionSharing by mutableStateOf(false)

    fun shareExtensions(platformFile: PlatformFile) {
        viewModelScope.launch {
            extensionSharing = true
            extensionShareHandler.shareExtensions(
                platformFile,
                installedSources.distinctBy { it.packageName }
            )
            extensionSharing = false
        }
    }
}
```

Removed vs. old VM (all only used by the old screen, which Task 8 rewrites): `remoteSources`, `remoteSourcesShowing`, `installed`, `remoteSourcesVersions`, `downloadAndInstall(link, dest)`, `uninstall(packageName)`, class `InstalledViewState`. Before deleting each, confirm no other caller:

Run: `grep -rn "remoteSourcesShowing\|remoteSourcesVersions\|InstalledViewState\|\.installed\b" --include='*.kt' . | grep -v /build/`
Expected: only hits in `ExtensionListScreen.kt` and `InstalledViewStateTest.kt`.

The old screen won't compile against this VM. To keep this task's build green, also edit `ExtensionListScreen.kt` in this step:
1. Delete the private composables `InstalledExtensionItems`, `RemoteExtensionItems`, `ExtensionItem`, `RemoteItem`, `RemoteSourceItem`, and `rotateWithBoolean`.
2. Replace the `listPane` and `detailPane` `AnimatedPane` contents with `Box(Modifier.fillMaxSize())` (Task 8 fills them).
3. Remove the now-unused `sourceInfoRepository` and `iconLoader` locals in `ExtensionList` and any imports the compiler reports unused.

- [ ] **Step 2: Delete `InstalledViewStateTest.kt`**

```bash
git rm kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/InstalledViewStateTest.kt
```

- [ ] **Step 3: Compile and test**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.presentation.settings.extensions.*" :kmpuiviews:compileKotlinJvm`
Expected: PASS / BUILD SUCCESSFUL. If Koin fails at runtime later because `SourceInfoRepository` isn't declared, find its module with `grep -rn "SourceInfoRepository" --include='*.kt' kmpuiviews/src/*/kotlin/**/di` and confirm a `singleOf(::SourceInfoRepository)` (or equivalent) exists for Android and JVM.

- [ ] **Step 4: Commit**

```bash
git add -A kmpuiviews/src
git commit -m "refactor(extensions): wire ViewModel to pure state and RemoteCatalogs"
```

---

### Task 5: Shared UI parts (`ExtensionParts.kt`)

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionParts.kt`

**Interfaces:**
- Consumes: `RemoteItem`, `RemoteItemState`, `InstalledItem`, `InstalledSummary`, `InstalledFilter`, `displayLang` (Task 2); `SourceIcon(iconLoader, sourceInfo)` (`com.programmersbox.kmpuiviews.SourceIcon`); `IconLoader`.
- Produces (all `internal`):
  - `@Composable fun RemoteIcon(name: String, iconUrl: String, size: Dp, modifier: Modifier = Modifier)`
  - `@Composable fun StatusText(status: DownloadAndInstallStatus): String` (non-composable is fine: `fun statusText(status): String`)
  - `@Composable fun SummaryRow(summary, filter, onFilter: (InstalledFilter) -> Unit, modifier)`
  - `@Composable fun ExtensionTile(item: RemoteItem, showCatalog: Boolean, onClick: () -> Unit, modifier)`
  - `@Composable fun TileRows(items: List<RemoteItem>, showCatalog: Boolean, onClick: (RemoteItem) -> Unit)` (non-lazy 3-col rows, for shelves inside a LazyColumn)
  - `@Composable fun InstalledRow(item, isCurrent, iconLoader, onClick, onTrust, onUpdate, onSettings: (() -> Unit)?, onSetCurrent, onUninstall)`
  - `@Composable fun ConfirmUninstallDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit)`

- [ ] **Step 1: Write the file**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.programmersbox.kmpuiviews.IconLoader
import com.programmersbox.kmpuiviews.SourceIcon
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus

/** Letter avatar under the remote icon, so a missing/failed icon still reads. */
@Composable
internal fun RemoteIcon(name: String, iconUrl: String, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Text(
            name.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        if (iconUrl.isNotBlank()) {
            AsyncImage(model = iconUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}

internal fun statusText(status: DownloadAndInstallStatus): String = when (status) {
    is DownloadAndInstallStatus.Downloading -> "Downloading ${(status.progress * 100).toInt()}%"
    DownloadAndInstallStatus.Downloaded -> "Downloaded"
    DownloadAndInstallStatus.Installing -> "Installing"
    DownloadAndInstallStatus.PendingUserAction -> "Waiting for you"
    DownloadAndInstallStatus.PermissionRequired -> "Permission needed"
    DownloadAndInstallStatus.Installed -> "Installed"
    DownloadAndInstallStatus.Cancelled -> "Cancelled"
    is DownloadAndInstallStatus.Error -> "Failed: ${status.message}"
}

/** Determinate only while downloading; other in-flight states show indeterminate. */
@Composable
internal fun InstallProgress(status: DownloadAndInstallStatus, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            statusText(status),
            style = MaterialTheme.typography.labelSmall,
            color = if (status is DownloadAndInstallStatus.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        when (status) {
            is DownloadAndInstallStatus.Downloading -> LinearProgressIndicator(progress = { status.progress }, modifier = Modifier.fillMaxWidth())
            is DownloadAndInstallStatus.Error -> Unit
            else -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun SummaryRow(
    summary: InstalledSummary,
    filter: InstalledFilter,
    onFilter: (InstalledFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        SummaryTile(
            count = summary.installed,
            label = "installed",
            container = MaterialTheme.colorScheme.surfaceContainer,
            content = MaterialTheme.colorScheme.onSurface,
            selected = false,
            onClick = { onFilter(InstalledFilter.None) }.takeIf { filter != InstalledFilter.None },
            modifier = Modifier.weight(1f)
        )
        SummaryTile(
            count = summary.updates,
            label = "updates",
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            selected = filter == InstalledFilter.Updates,
            onClick = { onFilter(InstalledFilter.Updates) }.takeIf { summary.updates > 0 },
            modifier = Modifier.weight(1f)
        )
        SummaryTile(
            count = summary.untrusted,
            label = "untrusted",
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
            selected = filter == InstalledFilter.Untrusted,
            onClick = { onFilter(InstalledFilter.Untrusted) }.takeIf { summary.untrusted > 0 },
            modifier = Modifier.weight(1f)
        )
    }
}

/** Count tiles with nothing to show render neutral and are not clickable. */
@Composable
private fun SummaryTile(
    count: Int,
    label: String,
    container: Color,
    content: Color,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val active = count > 0
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        colors = CardDefaults.cardColors(
            containerColor = if (active) container else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (active) content else MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = if (active) container else MaterialTheme.colorScheme.surfaceContainer,
            disabledContentColor = if (active) content else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun RemoteItem.subtitle(): String = when (val s = state) {
    is RemoteItemState.UpdateAvailable -> "Update"
    is RemoteItemState.Installing -> (s.status as? DownloadAndInstallStatus.Downloading)
        ?.let { "${(it.progress * 100).toInt()}%" } ?: statusText(s.status)
    else -> remote.lang.takeIf { it.isNotBlank() }?.let(::displayLang) ?: "v${remote.version}"
}

@Composable
internal fun ExtensionTile(
    item: RemoteItem,
    showCatalog: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Box {
            RemoteIcon(item.remote.name, item.remote.iconUrl, 56.dp)
            if (item.state is RemoteItemState.Installed) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Installed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                )
            }
        }
        Text(item.remote.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (showCatalog) item.catalog else item.subtitle(),
            style = MaterialTheme.typography.labelSmall,
            color = if (!showCatalog && item.state is RemoteItemState.UpdateAvailable) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Non-lazy 3-column rows, for grids nested inside a LazyColumn item. */
@Composable
internal fun TileRows(
    items: List<RemoteItem>,
    showCatalog: Boolean,
    onClick: (RemoteItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 12.dp)) {
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    ExtensionTile(item, showCatalog, onClick = { onClick(item) }, modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
internal fun InstalledRow(
    item: InstalledItem,
    isCurrent: Boolean,
    iconLoader: IconLoader,
    onClick: () -> Unit,
    onTrust: () -> Unit,
    onUpdate: () -> Unit,
    onSettings: (() -> Unit)?,
    onSetCurrent: () -> Unit,
    onUninstall: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var confirmUninstall by remember { mutableStateOf(false) }
    if (confirmUninstall) {
        ConfirmUninstallDialog(item.info.name, onConfirm = onUninstall, onDismiss = { confirmUninstall = false })
    }
    val untrusted = item.untrusted
    val trustAction = untrusted?.canTrust == true
    val updateAction = !trustAction && item.update != null
    val settingsAction = !trustAction && !updateAction && onSettings != null
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.info.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (isCurrent) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                        Text("Current", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        },
        supportingContent = {
            when {
                untrusted != null -> Text(untrusted.reason, color = MaterialTheme.colorScheme.error, maxLines = 2, overflow = TextOverflow.Ellipsis)
                item.installing != null -> InstallProgress(item.installing, Modifier.width(160.dp))
                item.update != null -> Text("${item.version} → ${item.update.version}")
                else -> Text("v${item.version}")
            }
        },
        leadingContent = { SourceIcon(iconLoader, item.info) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    trustAction -> FilledTonalButton(
                        onClick = onTrust,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    ) { Text("Trust") }

                    updateAction && item.installing == null -> FilledTonalButton(onClick = onUpdate) {
                        Icon(Icons.Default.Update, null, modifier = Modifier.size(18.dp))
                        Text(item.update!!.version, modifier = Modifier.padding(start = 4.dp))
                    }

                    settingsAction -> IconButton(onClick = onSettings!!) { Icon(Icons.Default.Settings, "Settings") }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "More") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Set as current source") },
                            enabled = untrusted == null && !isCurrent,
                            onClick = { showMenu = false; onSetCurrent() }
                        )
                        if (onSettings != null && !settingsAction) {
                            DropdownMenuItem(text = { Text("Settings") }, onClick = { showMenu = false; onSettings() })
                        }
                        DropdownMenuItem(
                            text = { Text("Uninstall", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; confirmUninstall = true }
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
internal fun ConfirmUninstallDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uninstall $name?") },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(); onDismiss() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Uninstall") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
```

Add import `androidx.compose.foundation.BorderStroke`. Spec says the Untrusted tile uses a "warning-container" color; M3 has none, so tertiaryContainer stands in (same as the Trust button).

- [ ] **Step 2: Compile**

Run: `./gradlew :kmpuiviews:compileKotlinJvm`
Expected: BUILD SUCCESSFUL (warnings about unused composables are fine until Task 6).

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionParts.kt
git commit -m "feat(extensions): tiles, summary row, installed row components"
```

---

### Task 6: Detail sheet (`ExtensionDetailSheet.kt`)

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionDetailSheet.kt`

**Interfaces:**
- Consumes: `ExtensionListViewModel.item(key)`, `.install(remote)`, `.uninstall(info)`; `RemoteIcon`, `InstallProgress`, `ConfirmUninstallDialog`, `displayLang` (Tasks 2, 5); `KmpConfigurableApiService`; `Screen.SourcePreferences(serviceName)`; `LocalNavActions`.
- Produces: `@Composable internal fun RemoteItemSheet(viewModel: ExtensionListViewModel, itemKey: String?, onDismiss: () -> Unit)` — renders nothing when `itemKey == null` or the item vanished.

- [ ] **Step 1: Write the file**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.LocalNavActions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun RemoteItemSheet(
    viewModel: ExtensionListViewModel,
    itemKey: String?,
    onDismiss: () -> Unit,
) {
    val item = itemKey?.let { viewModel.item(it) } ?: return
    val navActions = LocalNavActions.current
    val remote = item.remote
    var confirmUninstall by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RemoteIcon(remote.name, remote.iconUrl, 56.dp)
                Column(Modifier.weight(1f)) {
                    Text(remote.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.catalog, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (remote.nsfw) Pill("18+")
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val version = when (val s = item.state) {
                    is RemoteItemState.UpdateAvailable -> "${s.installedVersion} → ${remote.version}"
                    else -> remote.version
                }
                KeyValue("Version", version)
                KeyValue("Package", remote.packageName, mono = true)
                if (remote.lang.isNotBlank()) KeyValue("Language", displayLang(remote.lang))
            }

            if (remote.sources.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sources in this extension", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        remote.sources.map { it.name }.distinct().forEach { Pill(it) }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                when (val s = item.state) {
                    RemoteItemState.NotInstalled -> Button(onClick = { viewModel.install(remote) }) {
                        Icon(Icons.Default.Download, null)
                        Text("Install", modifier = Modifier.padding(start = 6.dp))
                    }

                    is RemoteItemState.UpdateAvailable -> Button(onClick = { viewModel.install(remote) }) {
                        Text("Update to ${remote.version}")
                    }

                    is RemoteItemState.Installing -> {
                        InstallProgress(s.status, Modifier.width(200.dp))
                        if (s.status is com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus.Error) {
                            Button(onClick = { viewModel.install(remote) }) { Text("Try again") }
                        }
                    }

                    is RemoteItemState.Installed -> {
                        TextButton(
                            onClick = { confirmUninstall = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Uninstall") }
                        val api = s.installed.apiService
                        if (api is KmpConfigurableApiService) {
                            Button(onClick = {
                                onDismiss()
                                navActions.navigate(Screen.SourcePreferences(api.serviceName))
                            }) { Text("Settings") }
                        }
                        if (confirmUninstall) {
                            ConfirmUninstallDialog(
                                remote.name,
                                onConfirm = { viewModel.uninstall(s.installed) },
                                onDismiss = { confirmUninstall = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyValue(key: String, value: String, mono: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(76.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) FontFamily.Monospace else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun Pill(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(6.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}
```

Replace the fully qualified `com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus.Error` with an import of `com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus` and `DownloadAndInstallStatus.Error`.

- [ ] **Step 2: Compile**

Run: `./gradlew :kmpuiviews:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionDetailSheet.kt
git commit -m "feat(extensions): remote extension detail sheet with install progress"
```

---

### Task 7: Installed pane, Browse pane, catalog page, nav key

**Files:**
- Create: `.../settings/extensions/InstalledPane.kt`, `.../settings/extensions/BrowsePane.kt`, `.../settings/extensions/ExtensionCatalogScreen.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt:170-174`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt:278-280`

**Interfaces:**
- Consumes: VM members from Task 4; parts from Tasks 5-6.
- Produces:
  - `@Composable internal fun InstalledPane(viewModel: ExtensionListViewModel, modifier: Modifier = Modifier)`
  - `@Composable internal fun BrowsePane(viewModel: ExtensionListViewModel, modifier: Modifier = Modifier)`
  - `@Composable fun ExtensionCatalogScreen(catalogName: String, viewModel: ExtensionListViewModel = koinViewModel())`
  - `Screen.ExtensionListScreen.Catalog(val catalogName: String)`

- [ ] **Step 1: Nav key**

In `Screen.kt` change the `ExtensionListScreen` block to:

```kotlin
    @Serializable
    data object ExtensionListScreen : Screen("extension_list") {
        @Serializable
        data object Home : Screen("home")

        @Serializable
        data class Catalog(val catalogName: String) : Screen("extension_catalog")
    }
```

In `Nav3Graph.kt` after the `navigation<Screen.ExtensionListScreen> { ... }` block add:

```kotlin
    navigation<Screen.ExtensionListScreen.Catalog> {
        ExtensionCatalogScreen(catalogName = it.catalogName)
    }
```

and import `com.programmersbox.kmpuiviews.presentation.settings.extensions.ExtensionCatalogScreen`.

- [ ] **Step 2: `InstalledPane.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import com.programmersbox.kmpuiviews.IconLoader
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.LocalCurrentSource
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun InstalledPane(
    viewModel: ExtensionListViewModel,
    modifier: Modifier = Modifier,
) {
    val iconLoader = koinInject<IconLoader>()
    val navActions = LocalNavActions.current
    val currentSourceRepository = LocalCurrentSource.current
    val currentSource by currentSourceRepository.asFlow().collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val filtering = viewModel.filter != InstalledFilter.None

    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "summary") {
            SummaryRow(
                summary = viewModel.summary,
                filter = viewModel.filter,
                onFilter = { viewModel.filter = viewModel.filter.toggle(it) },
            )
        }

        if (viewModel.filter == InstalledFilter.Updates && viewModel.summary.updates > 0) {
            item(key = "updateAll") {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Button(onClick = viewModel::updateAll) { Text("Update all") }
                }
            }
        }

        viewModel.visibleInstalled.forEach { (catalog, rows) ->
            item(key = "catalog:$catalog") {
                val needsAttention = rows.any { it.update != null || it.untrusted != null }
                val open = filtering || (viewModel.expanded[catalog] ?: needsAttention)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).animateItem()
                ) {
                    CatalogHeader(
                        title = catalog ?: rows.firstOrNull()?.info?.name ?: "Single Source",
                        rows = rows,
                        open = open,
                        iconLoader = iconLoader,
                        onToggle = { if (!filtering) viewModel.expanded[catalog] = !open },
                        onUninstallAll = rows.map { it.info }
                            .takeIf { catalog != null && it.canUninstallAll() }
                            ?.let { infos -> { viewModel.uninstall(infos.first()) } },
                    )
                    if (open) {
                        rows.forEach { row ->
                            val api = row.info.apiService
                            InstalledRow(
                                item = row,
                                isCurrent = currentSource?.serviceName == api.serviceName,
                                iconLoader = iconLoader,
                                onClick = { if (row.untrusted == null) currentSourceRepository.tryEmit(api) },
                                onTrust = { viewModel.trust(row.info) },
                                onUpdate = { row.update?.let(viewModel::install) },
                                onSettings = { navActions.navigate(Screen.SourcePreferences(api.serviceName)) }
                                    .takeIf { api is KmpConfigurableApiService },
                                onSetCurrent = { scope.launch { currentSourceRepository.emit(api) } },
                                onUninstall = { viewModel.uninstall(row.info) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogHeader(
    title: String,
    rows: List<InstalledItem>,
    open: Boolean,
    iconLoader: IconLoader,
    onToggle: () -> Unit,
    onUninstallAll: (() -> Unit)?,
) {
    val updates = rows.count { it.update != null }
    val rotation by animateFloatAsState(if (open) 180f else 0f)
    var showMenu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    if (confirm && onUninstallAll != null) {
        ConfirmUninstallDialog(title, onConfirm = onUninstallAll, onDismiss = { confirm = false })
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
            rows.take(3).forEach { Box { com.programmersbox.kmpuiviews.SourceIcon(iconLoader, it.info) } }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row {
                Text("${rows.size} extensions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (updates > 0) {
                    Text(" · $updates updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (onUninstallAll != null) {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "More") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Uninstall all", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; confirm = true }
                    )
                }
            }
        }
        Icon(Icons.Default.ExpandMore, null, modifier = Modifier.rotate(rotation).padding(end = 8.dp))
    }
}
```

Replace the fully qualified `com.programmersbox.kmpuiviews.SourceIcon` with an import. Check `SourceIcon`'s actual size: if it renders larger than ~32dp and the overlap looks wrong on device, wrap in `Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)))`. `LocalCurrentSource.current.asFlow()` returns `StateFlow<KmpApiService?>`, so `collectAsStateWithLifecycle()` needs no initial value; `tryEmit` is used exactly as the old screen did.

- [ ] **Step 3: `BrowsePane.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.LocalNavActions

private const val SHELF_SIZE = 6

@Composable
internal fun BrowsePane(
    viewModel: ExtensionListViewModel,
    modifier: Modifier = Modifier,
) {
    val navActions = LocalNavActions.current
    var query by rememberSaveable { mutableStateOf("") }
    var sheetKey by rememberSaveable { mutableStateOf<String?>(null) }
    RemoteItemSheet(viewModel, sheetKey, onDismiss = { sheetKey = null })

    val results by remember {
        derivedStateOf {
            viewModel.catalogs.values.filterIsInstance<CatalogUi.Loaded>().flatMap { it.items }.search(query).shelfOrder()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ExtensionSearchField(query, onQueryChange = { query = it }, placeholder = "Search all catalogs")

        if (query.isNotBlank()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { it.key }) { item ->
                    ExtensionTile(item, showCatalog = true, onClick = { sheetKey = item.key })
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
                viewModel.catalogs.forEach { (name, ui) ->
                    item(key = "header:$name") {
                        ShelfHeader(
                            name = name,
                            count = (ui as? CatalogUi.Loaded)?.items?.size,
                            onSeeAll = { navActions.navigate(Screen.ExtensionListScreen.Catalog(name)) },
                        )
                    }
                    item(key = "body:$name") {
                        when (ui) {
                            CatalogUi.Loading -> Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                            is CatalogUi.Error -> CatalogError(ui.message, onRetry = { viewModel.retry(name) })
                            is CatalogUi.Loaded -> TileRows(ui.items.take(SHELF_SIZE), showCatalog = false, onClick = { sheetKey = it.key })
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExtensionSearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Clear, "Clear") }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun ShelfHeader(name: String, count: Int?, onSeeAll: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp)
    ) {
        Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (count != null && count > SHELF_SIZE) {
            TextButton(onClick = onSeeAll) { Text("See all $count") }
        }
    }
}

@Composable
internal fun CatalogError(message: String?, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        ListItem(
            headlineContent = { Text("Couldn't load this catalog") },
            supportingContent = message?.let { { Text(it) } },
            leadingContent = { Icon(Icons.Default.CloudOff, null) },
            trailingContent = { TextButton(onClick = onRetry) { Text("Retry") } },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = MaterialTheme.colorScheme.onErrorContainer,
                supportingColor = MaterialTheme.colorScheme.onErrorContainer,
                leadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        )
    }
}
```

Note `Arrangement` import is unused if not needed — remove unused imports before committing. `Icons.Default.CloudOff` and `Icons.Default.ExpandMore` require `material-icons-extended`; confirm with `grep -rn "Icons.Default.CloudOff\|Icons.Default.HideSource" --include='*.kt' kmpuiviews/src | head -2` (`HideSource` is already used by the current screen, so extended icons are available).

- [ ] **Step 4: `ExtensionCatalogScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionCatalogScreen(
    catalogName: String,
    viewModel: ExtensionListViewModel = koinViewModel(),
) {
    val ui = viewModel.catalogs[catalogName]
    val all = (ui as? CatalogUi.Loaded)?.items.orEmpty()
    var query by rememberSaveable { mutableStateOf("") }
    var lang by rememberSaveable { mutableStateOf<String?>(null) }
    var hideNsfw by rememberSaveable { mutableStateOf(false) }
    var sheetKey by rememberSaveable { mutableStateOf<String?>(null) }
    RemoteItemSheet(viewModel, sheetKey, onDismiss = { sheetKey = null })

    val languages = all.languages()
    val shown = all.filterCatalog(lang, hideNsfw).search(query)

    OtakuScaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ui is CatalogUi.Loaded) "$catalogName · ${all.size}" else catalogName) },
                navigationIcon = { BackButton() },
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ExtensionSearchField(query, onQueryChange = { query = it }, placeholder = "Search $catalogName")
            if (languages.size > 1 || all.any { it.remote.nsfw }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    item { FilterChip(selected = lang == null, onClick = { lang = null }, label = { Text("All") }) }
                    items(languages) { l ->
                        FilterChip(selected = lang == l, onClick = { lang = l }, label = { Text(displayLang(l)) })
                    }
                    if (all.any { it.remote.nsfw }) {
                        item { FilterChip(selected = hideNsfw, onClick = { hideNsfw = !hideNsfw }, label = { Text("Hide 18+") }) }
                    }
                }
            }
            when (ui) {
                null, CatalogUi.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is CatalogUi.Error -> CatalogError(ui.message, onRetry = { viewModel.retry(catalogName) })
                is CatalogUi.Loaded -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shown, key = { it.key }, contentType = { "tile" }) { item ->
                        ExtensionTile(item, showCatalog = false, onClick = { sheetKey = item.key })
                    }
                }
            }
        }
    }
}
```

This screen gets its own `ExtensionListViewModel` instance (new nav entry → new ViewModelStore), so it fetches catalogs again on open and shows a spinner until its catalog arrives. That is expected; the spec line "reuses the same ViewModel" means the same class, not instance.

- [ ] **Step 5: Compile**

Run: `./gradlew :kmpuiviews:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation
git commit -m "feat(extensions): installed pane, browse shelves, and catalog page"
```

---

### Task 8: Screen scaffold (`ExtensionListScreen.kt`)

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionListScreen.kt:124-315`

**Interfaces:**
- Consumes: `InstalledPane`, `BrowsePane` (Task 7), VM (Task 4). Keeps `CustomUrlDialog` unchanged.
- Produces: `ExtensionList()` signature unchanged.

- [ ] **Step 1: Rewrite `ExtensionList`**

Replace the `ExtensionList` composable with:

```kotlin
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun ExtensionList(
    settingsHandling: NewSettingsHandling = koinInject(),
    backgroundWorkHandler: BackgroundWorkHandler = koinInject(),
    viewModel: ExtensionListViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()

    val appConfig = koinInject<AppConfig>()
    val appName = appConfig.appName

    val navController = LocalNavActions.current

    val createZip = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault()
    ) { document ->
        document?.let { viewModel.shareExtensions(it) }
    }

    LoadingDialog(
        showLoadingDialog = viewModel.extensionSharing,
        onDismissRequest = {/* viewModel.extensionSharing = false */ },
    )

    var showUrlDialog by remember { mutableStateOf(false) }
    if (showUrlDialog) {
        CustomUrlDialog(
            onDismissRequest = { showUrlDialog = false },
            onAddUrl = { scope.launch { settingsHandling.addCustomUrl(it) } },
            onRemoveUrl = { scope.launch { settingsHandling.removeCustomUrl(it) } },
            urls = settingsHandling.customUrls.collectAsStateWithLifecycle(initialValue = emptyList()).value
        )
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<Int>()
    val listVisible = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
    val detailVisible = navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded

    BackHandler(navigator.canNavigateBack()) {
        scope.launch { navigator.navigateBack() }
    }

    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(if (detailVisible && !listVisible) "Browse" else "Extensions") },
                navigationIcon = { BackButton() },
                actions = {
                    var showDropDown by remember { mutableStateOf(false) }
                    var checked by remember { mutableStateOf(false) }

                    IconButton(onClick = { showDropDown = true }) { Icon(Icons.Default.MoreVert, null) }

                    DropdownMenu(
                        expanded = showDropDown,
                        onDismissRequest = { showDropDown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Check for Source Updates") },
                            onClick = {
                                showDropDown = false
                                if (!checked) backgroundWorkHandler.sourceUpdate()
                                scope.launch {
                                    checked = true
                                    delay(10.minutes)
                                    checked = false
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Update, null) },
                            enabled = !checked
                        )

                        DropdownMenuItem(
                            text = { Text("Refresh Extensions") },
                            onClick = {
                                showDropDown = false
                                viewModel.refreshExtensions()
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) }
                        )

                        DropdownMenuItem(
                            text = { Text("Enable Incognito") },
                            onClick = {
                                showDropDown = false
                                navController.incognito()
                            },
                            leadingIcon = { Icon(Icons.Default.HideSource, null) }
                        )

                        DropdownMenuItem(
                            text = { Text("Share Extensions") },
                            onClick = {
                                showDropDown = false
                                createZip.launch("${appName}_extensions", "zip")
                            },
                            leadingIcon = { Icon(Icons.Default.Share, null) }
                        )

                        if (viewModel.hasCustomBridge) {
                            DropdownMenuItem(
                                text = { Text("Custom repos") },
                                onClick = {
                                    showUrlDialog = true
                                    showDropDown = false
                                },
                                leadingIcon = { Icon(Icons.Default.AddCircleOutline, null) },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (!detailVisible) {
                ExtendedFloatingActionButton(
                    text = { Text("Get extensions") },
                    icon = { Icon(Icons.Default.Add, null) },
                    onClick = { scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, 1) } },
                )
            }
        },
    ) { paddingValues ->
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            paneExpansionState = rememberPaneExpansionState(keyProvider = navigator.scaffoldValue),
            paneExpansionDragHandle = { state ->
                val interactionSource = remember { MutableInteractionSource() }
                VerticalDragHandle(
                    interactionSource = interactionSource,
                    modifier = Modifier.paneExpansionDraggable(
                        state = state,
                        minTouchTargetSize = LocalMinimumInteractiveComponentSize.current,
                        interactionSource = interactionSource,
                        semanticsProperties = state.defaultDragHandleSemantics()
                    )
                )
            },
            listPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    Surface { InstalledPane(viewModel) }
                }
            },
            detailPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    Surface { BrowsePane(viewModel) }
                }
            },
            modifier = Modifier.padding(paddingValues),
        )
    }
}
```

Imports: add `androidx.compose.material3.ExtendedFloatingActionButton`, `androidx.compose.material3.adaptive.layout.PaneAdaptedValue`. Remove now-unused imports (`SendTimeExtension`, `ToolTipWrapper`, `IconLoader`, `SourceInfoRepository` if `CustomUrlDialog` no longer needs it — it does use `koinInject<SourceInfoRepository>()`, so keep that one; `currentSource` resource, `Res`, `stringResource`, `AsyncImage`, `coil3.pathSegments`, `coil3.toUri`, `SnapshotStateMap`, `AppUpdate`, `KmpConfigurableApiService`, `KmpRemoteSources`, `KmpSourceInformation`, `KmpUntrustedApiService`, `LocalCurrentSource`, `Screen`, `ElevatedCard`, `OutlinedCard` only if `CustomUrlDialog` doesn't use them — it uses `OutlinedCard` and `ElevatedCard`, keep). Let the compiler's unused-import warnings guide the cleanup.

If `OtakuScaffold` has no `floatingActionButton` parameter, check its signature (`grep -n "fun OtakuScaffold" -A20 kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/components/*.kt`) and either pass it through if present under another name, or overlay the FAB inside the list pane with `Box { InstalledPane(...); ExtendedFloatingActionButton(..., Modifier.align(Alignment.BottomEnd).padding(16.dp)) }`.

The `res/strings` entry `currentSource` becomes unused by this screen; leave the resource (other screens may use it).

- [ ] **Step 2: Compile JVM and Android**

Run: `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Full test pass**

Run: `./gradlew :kmpuiviews:jvmTest :mihonsupport:jvmTest`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionListScreen.kt
git commit -m "feat(extensions): catalog-shelves extensions screen"
```

---

### Task 9: Device verification

**Files:** none unless fixes are needed.

- [ ] **Step 1: DB version safety check**

Run:
```bash
diff <(grep -rn "version *=" favoritesdatabase/src/commonMain --include='*Database*.kt') <(grep -rn "version *=" /Users/jacobrein/StudioProjects/OtakuWorld/favoritesdatabase/src/commonMain --include='*Database*.kt')
```
Expected: no differences. If the worktree is higher, stop and ask the user before installing.

- [ ] **Step 2: Pick device**

Run: `adb devices -l`. If more than one device is listed, ask the user which serial to use.

- [ ] **Step 3: Install and open**

```bash
adb -s <serial> install -r mangaworld/build/outputs/apk/noFirebase/debug/*.apk
```
Open MangaWorld → Settings → Extensions.

- [ ] **Step 4: Verify on device (ARTEMIS or manual), each item:**
  - Installed shows three count tiles; Updates/Untrusted tiles are inert at 0.
  - Tapping Updates filters and shows "Update all"; tapping it again clears.
  - Catalog cards: stacked icons, counts; cards with updates start expanded.
  - Row Current pill matches the current source; ⋮ → Uninstall shows a confirm dialog.
  - FAB "Get extensions" opens Browse; Back returns.
  - Browse shelves: 6 tiles per catalog, "See all N" opens the catalog page.
  - Catalog page: language chips, Hide 18+, search all filter the grid.
  - Tile → sheet shows version/package/language/sources; Install shows progress live in the sheet and on the tile.
  - Search on Browse returns tiles from multiple catalogs captioned with catalog names.
  - Rotate to landscape / tablet: both panes visible, FAB hidden.

- [ ] **Step 5: Fix and commit anything found**, then report results with screenshots to the user.
