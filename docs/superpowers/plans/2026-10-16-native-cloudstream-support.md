# Native CloudStream Support (Plan 2 of 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AnimeWorld downloads, trusts, loads and plays CloudStream `.cs3` plugins natively.

**Architecture:** New KMP module `:source_utilities:cloudstream` (GPL-3.0) vendors CloudStream's
`library` and shims the app-module classes plugins link against. A plugin manager stores `.cs3`
files in app-private storage and loads them with `PathClassLoader`. A `CloudstreamCatalog` joins
`NativeCatalogs` and uses Plan 1's catalog install hook, stream fields and player.

**Tech Stack:** Kotlin 2.4.20 KMP, Koin, NiceHttp 0.4.18, Jackson 2.13.1, Rhino 1.8.1, jsoup,
OkHttp, AppCompat/Material.

**Spec:** `docs/superpowers/specs/2026-10-16-native-aniyomi-cloudstream-support-design.md`
**Prerequisite:** Plan 1 (`docs/superpowers/plans/2026-10-16-native-aniyomi-support.md`) merged:
uses `KmpSubtitle`, `KmpStorage.subtitles/server/mimeType/extras`, `KmpApiService.resolve/canCast`,
`KmpExternalCustomApiServicesCatalog.install/uninstall`, `SourcePreferencesRoute`.

## Global Constraints

- Module path `:source_utilities:cloudstream`, dir `source_utilities/cloudstream/`, plugin
  `otaku-multiplatform`, namespace `com.programmersbox.cloudstream`, license **GPL-3.0** (`LICENSE`
  file with full GPL-3.0 text in the module dir).
- Only `animeworld` depends on it, in all three flavors (`implementation`, no flavor wiring).
- Vendored/shimmed classes keep upstream FQCN and JVM signatures, including deprecated
  constructors (`ExtractorLink(...)`, `Episode(...)`, `SubtitleFile(lang, url)`). Header
  `// Copied from recloudstream/cloudstream <sha>, GPL-3.0`; local edits `// OTAKU:`.
- Pinned deps: `com.github.Blatzar:NiceHttp:0.4.18`,
  `com.fasterxml.jackson.module:jackson-module-kotlin` strictly `2.13.1`,
  `org.mozilla:rhino` strictly `1.8.1`.
- Service names: `"cloudstream:${api.name}"`; on name collision across plugins,
  `"cloudstream:${internalName}:${api.name}"`. Untrusted: `"cloudstream-untrusted:${internalName}"`.
- `canDownload = false`, `canCast = false`.
- Plugins stored at `filesDir/cloudstream/<sha256(repoUrl) first 16 hex>/<internalName>.cs3`,
  read-only.
- Keep `tvTypes` ∩ `{Anime, AnimeMovie, OVA}`; drop `status == 0`; drop TORRENT/MAGNET and
  `DrmExtractorLink` links.
- A failing plugin never crashes the app.

## Review Focus

1. A `.cs3` download whose sha256 does not match `fileHash`: no file is left behind and nothing
   loads. (Task 4 `hashMismatch_leavesNoFile`.)
2. Two plugins both registering an API named "HiAnime": both appear, with distinct service names.
   (Task 5 `nameCollision_getsDistinctServiceNames`.)
3. `loadLinks` calling the callback from several coroutines at once: no lost or duplicated links.
   (Task 5 `concurrentCallbacks_allCollected`.)
4. An `AnimeLoadResponse` with both sub and dub lists: both show, names suffixed, sub first.
   (Task 5 `subAndDub_bothListed`.)
5. A repo `iconUrl` with `%size%` or a plugins.json entry missing `fileHash`: icon rendered at
   128, install proceeds without hash check. (Task 3 `iconSizeReplaced`, Task 4 `noHash_installs`.)

---

### Task 1: Module scaffold + vendored CloudStream library

**Files:**
- Modify: `settings.gradle.kts` (`include(":source_utilities:cloudstream")`)
- Modify: `gradle/android.versions.toml` (NiceHttp, jackson-module-kotlin 2.13.1 strict, rhino 1.8.1 strict, ksoup, whyoleg cryptography, kotlinx-datetime if missing)
- Create: `source_utilities/cloudstream/{build.gradle.kts, LICENSE, VENDORED.md, consumer-rules.pro, scripts/vendor-cloudstream.sh}`
- Create (vendored): `src/androidMain/kotlin/com/lagradost/**` from cloudstream `library/src/commonMain` + `library/src/androidMain` (expect/actual collapsed into androidMain with `// OTAKU:` notes)
- Test: `source_utilities/cloudstream/src/androidHostTest/kotlin/com/programmersbox/cloudstream/VendoredLibraryTest.kt`

**Interfaces:**
- Produces: upstream FQCNs, notably `com.lagradost.cloudstream3.{MainAPI, SearchResponse, LoadResponse, AnimeLoadResponse, TvSeriesLoadResponse, MovieLoadResponse, Episode, DubStatus, HomePageList, MainPageRequest, SubtitleFile, TvType, APIHolder, MainActivityKt}`, `com.lagradost.cloudstream3.utils.{ExtractorApi, ExtractorLink, ExtractorLinkType, Qualities, loadExtractor, extractorApis, newExtractorLink}`, `com.lagradost.cloudstream3.plugins.BasePlugin`.
- `consumer-rules.pro`: keep `com.lagradost.**`, `com.fasterxml.jackson.**`, `org.mozilla.javascript.**`, `org.jsoup.**`, `okhttp3.**`, `androidx.appcompat.**`, `androidx.fragment.**`, `com.google.android.material.**` (names and members).

- [ ] **Step 1:** Pin cloudstream `master` SHA in the script; copy `library` sources; list files in `VENDORED.md`. If the NewPipe-backed YouTube extractor is the only NewPipe user, delete it (record in `VENDORED.md`) instead of adding NewPipe.
- [ ] **Step 2: Write failing test** — `deprecatedConstructorsCallable` (under `@Suppress("DEPRECATION", "DEPRECATION_ERROR")`, call the deprecated `ExtractorLink(...)`, `Episode(...)` and `SubtitleFile(lang, url)` constructors exactly as declared at the pinned SHA; the test passing compile is the assertion, plus `link.url == "https://x/a.mp4"`); `newExtractorLinkBuildsM3u8` (`newExtractorLink("s","n","https://x/a.m3u8", ExtractorLinkType.M3U8) { referer = "r" }` → `type == M3U8`, `getAllHeaders()["referer"] == "r"`, run in `runTest`).
- [ ] **Step 3: Run** `./gradlew :source_utilities:cloudstream:testAndroidHostTest` — fix compile errors by vendoring more upstream files or adding deps, never renaming.
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** `feat(cloudstream): GPL module scaffold and vendored library`

### Task 2: App-module shims

**Files:**
- Create: `source_utilities/cloudstream/scripts/dex-refs.sh` — given a `.cs3`, unzips `classes.dex` and lists referenced `Lcom/lagradost/...;` types (use `dexdump` from build-tools).
- Create (androidMain, upstream FQCNs): `com/lagradost/cloudstream3/plugins/Plugin.kt`, `com/lagradost/cloudstream3/CloudStreamApp.kt` (+ `AcraApplication.kt` alias), `com/lagradost/cloudstream3/CommonActivity.kt`, `com/lagradost/cloudstream3/utils/DataStore.kt`, `com/lagradost/cloudstream3/network/{CloudflareKiller.kt, DdosGuardKiller.kt}`, `com/lagradost/cloudstream3/utils/AppContextUtils.kt`, `com/lagradost/cloudstream3/actions/VideoClickAction.kt` (abstract, never invoked)
- Create: `source_utilities/cloudstream/shims.md` — table of each referenced type, which plugin needs it, shim status.
- Test: `.../androidHostTest/.../ShimPresenceTest.kt`

**Interfaces:**
- Produces:
  - `abstract class Plugin : BasePlugin() { open fun load(context: Context); var resources: Resources?; var openSettings: ((Context) -> Unit)?; fun registerVideoClickAction(action: VideoClickAction) }` (`load(context)` defaults to `load()`; register is a no-op)
  - `CloudStreamApp.Companion { var context: Context?; fun <T> getKey(path: String, defVal: T?): T?; fun <T> setKey(path: String, value: T); fun removeKey(path: String) }` — Jackson JSON in SharedPreferences `"cloudstream_data"`. Match the upstream overload set exactly (look at the pinned SHA).
  - `CommonActivity { var activity: Activity?; fun showToast(...) overloads as upstream }`
  - `object CloudstreamShims { fun install(application: Application) }` — sets `CloudStreamApp.context`, registers an `ActivityLifecycleCallbacks` that keeps `CommonActivity.activity` = resumed activity.

- [ ] **Step 1:** Download Anizone, AnimePahe, HiAnime, Anichi `.cs3` from `https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/builds/repo.json` into the scratchpad; run `dex-refs.sh`; any `com.lagradost.*` type not in vendored `library` goes in `shims.md` and gets a shim.
- [ ] **Step 2: Write failing test** — `ShimPresenceTest.allReferencedTypesResolve`: for each FQCN listed in `shims.md`, `Class.forName(name)` succeeds; `setGetKeyRoundTrip` (Robolectric-free: back `getKey/setKey` with an injectable `SharedPreferences` fake or test the JSON codec function directly).
- [ ] **Step 3: Run** `./gradlew :source_utilities:cloudstream:testAndroidHostTest` — FAIL.
- [ ] **Step 4: Implement shims.**
- [ ] **Step 5: Run** — PASS.
- [ ] **Step 6: Commit** `feat(cloudstream): app-module shims plugins link against`

### Task 3: Repo client

**Files:**
- Create: `source_utilities/cloudstream/src/commonMain/kotlin/com/programmersbox/cloudstream/repo/{CloudstreamRepoModels.kt, CloudstreamRepoClient.kt}`
- Test: `.../commonTest/.../repo/CloudstreamRepoClientTest.kt` (Ktor `MockEngine`, fixtures trimmed from the phisher98 `repo.json` / `plugins.json`)

**Interfaces:**
- Produces:
  - `@Serializable data class CsRepo(val name: String, val manifestVersion: Int, val pluginLists: List<String>, val description: String? = null, val iconUrl: String? = null)`
  - `@Serializable data class CsPlugin(val url: String, val status: Int, val version: Int, val apiVersion: Int, val name: String, val internalName: String, val authors: List<String> = emptyList(), val description: String? = null, val repositoryUrl: String? = null, val tvTypes: List<String>? = null, val language: String? = null, val iconUrl: String? = null, val fileSize: Long? = null, val fileHash: String? = null)`
  - `class CloudstreamRepoClient(http: HttpClient) { suspend fun fetch(urls: List<String>): List<KmpRemoteSources>; fun pluginFor(downloadLink: String): Pair<String /*repoUrl*/, CsPlugin>? }` — remembers the last fetch so `install` can find repo + hash.
  - Mapping: `KmpRemoteSources(name, packageName = internalName, version = version.toString(), iconUrl = iconUrl?.replace("%size%", "128").orEmpty(), downloadLink = url, sources = listOf(KmpSources(name, repositoryUrl.orEmpty(), version.toString())), lang = language.orEmpty())`.
  - A URL whose JSON lacks `pluginLists` is skipped (Aniyomi repo). https only.

- [ ] **Step 1: Write failing tests** — `filtersToAnimeTypes`, `dropsStatusZero`, `iconSizeReplaced`, `skipsAniyomiRepo`, `failingPluginListSkipped`, `pluginForFindsHash`.
- [ ] **Step 2: Run** `./gradlew :source_utilities:cloudstream:jvmTest` — FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** `feat(cloudstream): repo client`

### Task 4: Plugin store (download, verify, metadata) and trust setting

**Files:**
- Create: `source_utilities/cloudstream/src/androidMain/kotlin/com/programmersbox/cloudstream/plugins/{PluginStore.kt, InstalledPlugin.kt}`
- Modify: `datastore/src/commonMain/proto/settings.proto` (`repeated string trustedCloudstreamRepos = 41;`) and `SettingsHandling.kt` (`trustedCloudstreamRepos: Flow<List<String>>`, `suspend fun addTrustedCloudstreamRepo(url: String)`, no duplicates)
- Test: `.../androidHostTest/.../plugins/PluginStoreTest.kt` (temp dir + OkHttp `MockWebServer`)

**Interfaces:**
- Produces:
  - `@Serializable data class InstalledPlugin(val repoUrl: String, val internalName: String, val version: Int, val path: String)`
  - `class PluginStore(root: File, client: OkHttpClient) { suspend fun install(repoUrl: String, plugin: CsPlugin): InstalledPlugin; fun uninstall(internalName: String); fun installed(): List<InstalledPlugin> }` — temp file → sha256 check when `fileHash` present (`"sha256-<hex>"`, hex compared case-insensitively) → atomic move → `setReadOnly()` → upsert `root/installed.json`. Mismatch throws and deletes the temp file. Reinstall (update) replaces file and entry.

- [ ] **Step 1: Write failing tests** — `installsAndRecords`, `hashMismatch_leavesNoFile`, `noHash_installs`, `updateReplacesEntry`, `uninstallRemovesFileAndEntry`.
- [ ] **Step 2: Run** `./gradlew :source_utilities:cloudstream:testAndroidHostTest` — FAIL.
- [ ] **Step 3: Implement** (plus the settings field, mirroring Plan 1's Aniyomi pair).
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** `feat(cloudstream): plugin store and repo trust setting`

### Task 5: CloudstreamApiService and mapper

**Files:**
- Create: `source_utilities/cloudstream/src/androidMain/kotlin/com/programmersbox/cloudstream/{CloudstreamApiService.kt, CloudstreamMapper.kt}`
- Test: `.../androidHostTest/.../{CloudstreamApiServiceTest.kt, FakeMainApis.kt}`

**Interfaces:**
- Consumes: Plan 1 contract types.
- Produces:
  - `open class CloudstreamApiService(val api: MainAPI, val internalName: String, override val serviceName: String) : KmpApiService`; `class ConfigurableCloudstreamApiService(..., val openSettings: (Context) -> Unit) : CloudstreamApiService(...), KmpConfigurableApiService`. `canScroll = true`, `canDownload = false`, `canCast = false`, `displayName = "${api.name} (${api.lang})"`, `language = api.lang`, `baseUrl = api.mainUrl`.
  - `fun serviceNamesFor(apis: List<Pair<String /*internalName*/, MainAPI>>): List<String>` — collision rule from Global Constraints.
  - Methods per spec mapper table. `recent(page)`: first `api.mainPage` entry via `getMainPage(page, MainPageRequest(name, data, horizontalImages = false))`; if `mainPage` empty → `search("")` is **not** called, return empty. `itemInfo` episodes: `AnimeLoadResponse` → `episodes[DubStatus.Subbed]` then `[Dubbed]` (names suffixed `" (Sub)"`/`" (Dub)"` only when both exist), `TvSeriesLoadResponse.episodes`, `MovieLoadResponse` → one chapter named title with `data = dataUrl`. Episode data in `extras["csData"]`.
  - `chapterInfo`: `loadLinks(data, false, subCb, linkCb)` collecting into a `Mutex`-guarded list; after return, attach every subtitle to every link. `ExtractorLink → KmpStorage`: `link = url`, `quality = Qualities.getStringByInt(quality)` (upstream helper), `headers = getAllHeaders()`, `server = source`, `mimeType` from `type` (M3U8 → `"application/x-mpegURL"`, DASH → `"application/dash+xml"`, VIDEO → null). Drop TORRENT/MAGNET/`DrmExtractorLink`.

- [ ] **Step 1: Write failing tests** — `nameCollision_getsDistinctServiceNames`, `subAndDub_bothListed`, `movieBecomesOneEpisode`, `concurrentCallbacks_allCollected` (fake `loadLinks` launches 50 callbacks across `Dispatchers.Default` → 50 storages), `drmAndTorrentDropped`, `subtitlesAttachedToEveryLink`, `m3u8MimeSet`.
- [ ] **Step 2: Run** `./gradlew :source_utilities:cloudstream:testAndroidHostTest` — FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** `feat(cloudstream): api service and mapper`

### Task 6: Plugin loader, catalog, trust placeholder, settings activity, wiring

**Files:**
- Create: `source_utilities/cloudstream/src/androidMain/kotlin/com/programmersbox/cloudstream/{CloudstreamCatalog.kt, CloudstreamModule.kt, UntrustedCloudstreamApiService.kt}`
- Create: `.../plugins/PluginLoader.kt`
- Create: `.../preferences/CloudstreamSettingsActivity.kt` + `src/androidMain/AndroidManifest.xml` entry (theme `Theme.Material3.DayNight.NoActionBar`, transparent window)
- Modify: `animeworld/build.gradle.kts` (`implementation(projects.sourceUtilities.cloudstream)`)
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt` (`includes(cloudstreamModule)`)
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/SourcePreferencesRoute.kt` (`"cloudstream:"` → start `CloudstreamSettingsActivity` with `serviceName` extra, then pop back)

**Interfaces:**
- Consumes: Tasks 2–5.
- Produces:
  - `class PluginLoader(context: Context) { fun load(plugin: InstalledPlugin): LoadedPlugin; fun unload(loaded: LoadedPlugin) }`, `data class LoadedPlugin(val plugin: InstalledPlugin, val instance: BasePlugin, val apis: List<MainAPI>, val extractors: List<ExtractorApi>)` — `PathClassLoader(path, context.classLoader)`, `manifest.json` via Jackson (`BasePlugin.Manifest`), `requiresResources` → `AssetManager` + `addAssetPath` reflection → `Plugin.resources`; capture registrations by diffing `APIHolder.allProviders` / `extractorApis` before/after `load` under a lock; `unload` calls `beforeUnload()` and removes them.
  - `class CloudstreamCatalog(application, settings, repoClient, store, loader) : KmpExternalCustomApiServicesCatalog` — `name = "CloudStream"`, `hasRemoteSources = true`; `initialize()` runs `CloudstreamShims.install` once, then for each `store.installed()`: repo trusted → load (failure → `UntrustedCloudstreamApiService(reason = "Failed to load: …", canTrust = false)`), else placeholder `reason = "Untrusted repo: <repo name or url>"`, `canTrust = true`, `trust()` = `addTrustedCloudstreamRepo(repoUrl)`. `install(remote)`: `repoClient.pluginFor(remote.downloadLink)` null → false; else `store.install` → true. `uninstall(source)`: if source's `packageName` is a stored `internalName` → unload + `store.uninstall` → true. `KmpSourceInformation.packageName = internalName`.
  - `CloudstreamSettingsActivity : AppCompatActivity` — looks up the `ConfigurableCloudstreamApiService` by `serviceName` from `SourceRepository`, calls `openSettings(this)`, finishes when the plugin's UI is dismissed (or immediately if it opened its own activity).

- [ ] **Step 1: Implement** the above.
- [ ] **Step 2: Build** `./gradlew :animeworld:assembleNoFirebaseDebug :animeworld:assembleFullDebug` — SUCCESS (full flavor needs `google-services.json`; if absent locally, build `noCloudFirebaseDebug` instead and note it). `./gradlew :source_utilities:cloudstream:allTests` — PASS.
- [ ] **Step 3: Release build** `./gradlew :animeworld:assembleNoFirebaseRelease` — SUCCESS.
- [ ] **Step 4: Commit** `feat(animeworld): wire native CloudStream catalog, trust and plugin settings`

### Task 7: Device verification

Follow `~/.claude/rules/artemis.md`: `adb devices -l`; ask the user which serial if several.

- [ ] **Step 1:** Add `https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/builds/repo.json`. Expect a "CloudStream" shelf with anime plugins only.
- [ ] **Step 2:** Install Anizone. It shows as untrusted ("Untrusted repo: …"). Tap Trust. It loads.
- [ ] **Step 3:** Search "Dragon Ball" → open Dragon Ball GT or Daima → play an episode. Expect picker grouped by server; playback with headers; subtitles if provided.
- [ ] **Step 4:** Install AnimePahe (requires resources); open its settings via the gear; settings UI appears with Material styling.
- [ ] **Step 5:** Global search "Dragon Ball" shows both Aniyomi and CloudStream results.
- [ ] **Step 6:** Uninstall Anizone; restart app; no crash; file gone from `filesDir/cloudstream/` (`adb shell run-as com.programmersbox.animeworld.noFirebase ls files/cloudstream/*`).
- [ ] **Step 7:** Repeat Step 3 on the `noFirebase` release build.
- [ ] **Step 8:** Record results in the PR description.
