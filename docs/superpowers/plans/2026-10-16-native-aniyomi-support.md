# Native Aniyomi Support (Plan 1 of 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AnimeWorld loads Aniyomi extension APKs natively and plays their episodes in the in-app
player, with all stream headers, sidecar subtitles and a server-then-quality picker.

**Architecture:** New KMP module `:source_utilities:aniyomi` vendors Aniyomi's
`eu.kanade.tachiyomi.animesource.*` and network runtime, modeled file-for-file on `:mihonsupport`.
It binds an `AniyomiCatalog` into the existing `NativeCatalogs` pipeline. Shared contract changes in
`kmpmodels` (stream fields, `resolve`, `canCast`, catalog install hooks) and player changes in
`animeworld:shared` are reused by Plan 2 (CloudStream).

**Tech Stack:** Kotlin 2.4.20 KMP, Koin, Media3 1.11.0, OkHttp 5.4.0, Injekt, RxJava 1, Ktor,
kotlinx.serialization 1.11.0, nanohttpd.

**Spec:** `docs/superpowers/specs/2026-10-16-native-aniyomi-cloudstream-support-design.md`
**Reference implementation to copy patterns from:** `mihonsupport/` (read
`docs/superpowers/specs/2026-10-13-native-mihon-support-design.md` for its rationale).

## Global Constraints

- minSdk 28, compileSdk 37, Kotlin 2.4.20, OkHttp 5.4.0, Media3 1.11.0, kotlinx.serialization 1.11.0.
- Module path `:source_utilities:aniyomi`, dir `source_utilities/aniyomi/`, plugin
  `otaku-multiplatform`, namespace `com.programmersbox.aniyomi`, license Apache-2.0.
- Vendored files keep upstream package, class names and JVM signatures; header
  `// Copied from aniyomiorg/aniyomi <sha>, Apache-2.0`; local edits marked `// OTAKU:`.
- `:mihonsupport` and `:source_utilities:aniyomi` must never be in the same APK (both define
  `eu.kanade.tachiyomi.network.*`). Only `animeworld` depends on the aniyomi module.
- Supported Aniyomi lib versions: exactly `{14.0, 16.0, 17.0}`.
- Service names: `"aniyomi:${source.id}"`, untrusted `"aniyomi-untrusted:$pkg"`.
- Aniyomi sources report `canDownload = false` and `canCast = false`.
- Non-Android targets are stubs: `getSources() = emptyList()`, `getRemoteSources(...) = emptyList()`.
- A failing extension never crashes the app; it becomes an untrusted placeholder.
- Local builds use the `noFirebase` flavor: `./gradlew :animeworld:assembleNoFirebaseDebug`.

## Review Focus

1. Source headers using `"Referer"` while the player default is `"referer"`: the request must carry
   one Referer, the source's. (Task 3 `playerHeaders_isCaseInsensitive`.)
2. One hoster throws while others succeed: the user still gets the other hosters' videos.
   (Task 8 `chapterInfo_skipsFailingHoster`.)
3. `Video.headers == null`: requests fall back to `source.headers`, so Referer/UA still go out.
   (Task 8 `videoWithoutHeaders_usesSourceHeaders`.)
4. A repo URL pasted as `.../index.min.json`, or a CloudStream `repo.json` in the same list: the
   first normalizes, the second is skipped silently. (Task 6 `normalizesIndexUrl`,
   `skipsCloudstreamRepo`.)
5. A subtitle URL with a query string (`.../en.vtt?token=1`): MIME still detected from the path.
   (Task 3 `subtitleMime_ignoresQuery`.)

---

### Task 1: Shared stream and catalog contract (kmpmodels)

**Files:**
- Modify: `kmpmodels/src/commonMain/kotlin/com/programmersbox/kmpmodels/Models.kt:44-54`
- Modify: `kmpmodels/src/commonMain/kotlin/com/programmersbox/kmpmodels/ApiService.kt`
- Test: `kmpmodels/src/commonTest/kotlin/com/programmersbox/kmpmodels/StreamContractTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class KmpSubtitle(val url: String, val lang: String, val headers: Map<String, String> = emptyMap())`
  - `KmpStorage` gains constructor params with defaults (after `filename`):
    `var subtitles: List<KmpSubtitle> = emptyList()`, `var server: String? = null`,
    `var mimeType: String? = null` (one of `"application/x-mpegURL"`, `"application/dash+xml"`,
    `"video/mp4"`, or null = infer).
  - `KmpStorage` gains body property `val extras = mutableMapOf<String, String>()` (like `headers`;
    not serialized), for source-private keys such as a lazy hoster id.
  - `KmpApiService.canCast: Boolean get() = true`
  - `KmpApiService.suspend fun resolve(storage: KmpStorage): KmpStorage = storage`
  - `KmpExternalCustomApiServicesCatalog.suspend fun install(remote: KmpRemoteSources): Boolean = false`
  - `KmpExternalCustomApiServicesCatalog.suspend fun uninstall(source: KmpSourceInformation): Boolean = false`

- [ ] **Step 1: Write failing tests** — `defaultsKeepOldBehavior`: a bare `KmpStorage()` has
  `subtitles == emptyList()`, `server == null`, `mimeType == null`, `extras.isEmpty()`; an anonymous `KmpApiService`
  has `canCast == true` and `resolve(s) === s`; an anonymous catalog's `install(remote)` and
  `uninstall(info)` return `false`.
- [ ] **Step 2: Run** `./gradlew :kmpmodels:jvmTest --tests "*StreamContractTest*"` — expect compile FAIL.
- [ ] **Step 3: Add the fields and defaults listed above.** Keep `headers` as the existing body property.
- [ ] **Step 4: Run** the same command — expect PASS. Then `./gradlew :kmpmodels:jvmTest` all PASS.
- [ ] **Step 5: Commit** `feat(kmpmodels): stream subtitles/server/mime, resolve, canCast, catalog install hooks`

### Task 2: Extensions screen routes install/uninstall/update through catalogs

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionStates.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionListViewModel.kt:81-101`
- Test: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/CatalogInstallTest.kt`

**Interfaces:**
- Consumes: Task 1 `install`/`uninstall` hooks.
- Produces (in `ExtensionStates.kt`):
  - `suspend fun installViaCatalogs(catalogs: List<KmpExternalCustomApiServicesCatalog>, remote: KmpRemoteSources): Boolean` — first catalog returning true wins; false if none.
  - `fun List<InstalledItem>.updateRemotes(): List<KmpRemoteSources>` — same filter as `updateLinks()` but returns the `update` objects.

- [ ] **Step 1: Write failing tests** — `firstClaimingCatalogWins` (two fakes, second returns true, first false → true and only second recorded the call after first); `noCatalogClaims_returnsFalse`; `updateRemotes_matchesUpdateLinks` (same items → `updateRemotes().map { it.downloadLink } == updateLinks()`).
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*CatalogInstallTest*"` — FAIL.
- [ ] **Step 3: Implement the two helpers.** Then in the VM: `install(remote)` launches in
  `viewModelScope`: if `installViaCatalogs(nativeCatalogs.catalogs, remote)` → `sourceLoader.blockingLoad()`, else `request(remote.downloadLink)`. `updateAll()` calls `install` per `updateRemotes()`. `uninstall(source)`: if `(source.catalog as? KmpExternalCustomApiServicesCatalog)?.uninstall(source) == true` → `blockingLoad()`, else existing path.
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest` — all PASS.
- [ ] **Step 5: Commit** `feat(extensions): let native catalogs own install/uninstall/update`

### Task 3: Player honors all headers, subtitles and MIME

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videoplayer/PlayerRequest.kt`
- Modify: `animeworld/shared/build.gradle.kts` (add `commonTest` deps `commonLibs.kotlin.test`)
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoViewModel.kt:46-56,198-270`
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerCompose.android.kt:214`
- Test: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/videoplayer/PlayerRequestTest.kt`

**Interfaces:**
- Consumes: Task 1 `KmpStorage.subtitles/mimeType`, `KmpSubtitle`.
- Produces (pure, commonMain):
  - `val DEFAULT_PLAYER_HEADERS: Map<String, String>` — the existing accept/sec-* map from `getHttpDataSourceFactory` (without referer).
  - `fun playerHeaders(sourceHeaders: Map<String, String>, referer: String?): Map<String, String>` — defaults, then `referer` if non-blank, then source headers; keys compared case-insensitively, later wins, source key casing kept.
  - `fun subtitleMime(url: String): String` — by path extension: `vtt`→`text/vtt`, `srt`→`application/x-subrip`, `ass`/`ssa`→`text/x-ssa`, else `text/vtt`.
  - `fun streamMime(storage: KmpStorage): String?` — `storage.mimeType`, else `null`.

- [ ] **Step 1: Write failing tests** — `playerHeaders_isCaseInsensitive` (source `"Referer" to "https://a/"`, referer arg `"https://b/"` → exactly one referer key, value `"https://a/"`); `playerHeaders_keepsDefaults`; `playerHeaders_blankRefererOmitted`; `subtitleMime_byExtension` (four cases); `subtitleMime_ignoresQuery` (`"https://x/en.vtt?token=1"` → `text/vtt`, `"https://x/a.srt#t"` → `application/x-subrip`).
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*PlayerRequestTest*"` — FAIL.
- [ ] **Step 3: Implement PlayerRequest.kt.** Then:
  - `VideoViewModel`: replace `val headers = videoScreen.referer ?: ""` with
    `val requestHeaders = playerHeaders(storageHolder.storageModel?.headers.orEmpty(), videoScreen.referer)`,
    plus `val subtitles = storageHolder.storageModel?.subtitles.orEmpty()` and
    `val mimeType = storageHolder.storageModel?.let(::streamMime)`.
  - `getMediaSource(url, preview, bandwidthMeter, headers: Map<String, String>, subtitles: List<KmpSubtitle> = emptyList(), mimeType: String? = null)`:
    pick factory by `mimeType` first (HLS/DASH/progressive), else the existing `inferContentType`.
    Build the `MediaItem` with `setSubtitleConfigurations` (one per subtitle: `Uri`, `subtitleMime`, `setLanguage(lang)`, `setLabel(lang)`). `getHttpDataSourceFactory` takes the map and calls `setDefaultRequestProperties(headers)`; keep the "AnimeWorld" UA only when `headers` has no `user-agent` key (case-insensitive).
  - Update the call at `VideoPlayerCompose.android.kt:214`.
- [ ] **Step 4: Run** the test again — PASS. `./gradlew :animeworld:assembleNoFirebaseDebug` — BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(animeworld): player sends all source headers, sidecar subtitles, explicit MIME`

### Task 4: Server-then-quality picker, resolve before play, cast gate

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videochoice/ServerGrouping.kt`
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videochoice/VideoChoiceScreen.kt:51-115`
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/GenericSharedAnime.kt:66-96`
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt:164-190,527-548`
- Modify: `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericAnimeDesktop.kt:39` (signature only)
- Test: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/videochoice/ServerGroupingTest.kt`

**Interfaces:**
- Consumes: Task 1 `server`, `resolve`, `canCast`; Task 3 player reads `StorageHolder.storageModel`.
- Produces:
  - `fun List<KmpStorage>.groupedByServer(): List<Pair<String?, List<KmpStorage>>>` — order of first appearance kept, within a group original order kept.
  - `GenericSharedAnime.suspend fun playResolved(navController, storage, model, infoModel)` = `playOrCast(navController, model.source.resolve(storage), model, infoModel)`; resolve failure is caught, logged, and nothing plays.
  - `VideoChoiceScreen(..., onPlay: suspend (KmpStorage) -> Unit)` replaces the inline streaming navigate.

- [ ] **Step 1: Write failing tests** — `groupsInFirstAppearanceOrder` (servers A,B,A → [A:[1,3], B:[2]]); `nullServerIsOneGroup`.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*ServerGroupingTest*"` — FAIL.
- [ ] **Step 3: Implement.**
  - `chapterOnClick` single-item path calls `playResolved`.
  - `VideoChoiceScreen`: when more than one group, render a header row per server name above its qualities; one group renders exactly as today. Streaming click: `scope.launch { onPlay(it) }` (cast branch stays for sources with `model.source.canCast`).
  - `GenericAnime.DialogSetups` passes `onPlay = { s -> playResolved(navController, s, it.model, it.infoModel) }`.
  - `GenericAnime.playOrCast`: cast only if `MainActivity.cast.isCastActive() && model.source.canCast`; the local branch already sets `storageHolder.storageModel`.
- [ ] **Step 4: Run** test — PASS; `./gradlew :animeworld:assembleNoFirebaseDebug :animeworld:desktop:compileKotlinJvm` — SUCCESS.
- [ ] **Step 5: Device smoke:** with an existing AnimeWorld plugin source, play one episode; picker and playback unchanged.
- [ ] **Step 6: Commit** `feat(animeworld): group sources by server, resolve before play, respect canCast`

### Task 5: Module scaffold + vendored Aniyomi runtime

**Files:**
- Modify: `settings.gradle.kts` (`include(":source_utilities:aniyomi")`)
- Create: `source_utilities/aniyomi/build.gradle.kts` — copy `mihonsupport/build.gradle.kts`, change namespace/package, add androidMain `api` deps: nanohttpd (`org.nanohttpd:nanohttpd:2.3.1`, add to `gradle/android.versions.toml`), `androidLibs.preference`, injekt, rxjava1, jsoup, okhttp(+dns/logging/brotli/zstd), kotlinx serialization json/protobuf/okio, zipline, squareLogcat, androidxWebkit.
- Create: `source_utilities/aniyomi/scripts/vendor-aniyomi.sh` (modeled on `mihonsupport/scripts/vendor-mihon.sh`), `source_utilities/aniyomi/VENDORED.md`
- Create (vendored, via script): `src/androidMain/kotlin/eu/kanade/tachiyomi/animesource/**`, `.../eu/kanade/tachiyomi/network/**`, `tachiyomi/core/common/**` (only files referenced), `mihon/core/common/extensions/**`, `app/cash/quickjs/QuickJs.kt` (copy from `mihonsupport`)
- Create: `source_utilities/aniyomi/consumer-rules.pro` — keep `eu.kanade.tachiyomi.**`, `tachiyomi.**`, `mihon.**`, `app.cash.quickjs.**`, `uy.kohesive.injekt.**`, `rx.**`, `fi.iki.elonen.**`, `org.jsoup.**`, `okhttp3.**`, `kotlinx.serialization.**` (copy shape from mihonsupport's rules, which live in the module's consumer rules)
- Test: `source_utilities/aniyomi/src/androidHostTest/kotlin/com/programmersbox/aniyomi/VendoredRuntimeTest.kt`

**Interfaces:**
- Produces: vendored classes at upstream FQCNs, notably `eu.kanade.tachiyomi.animesource.{AnimeSource, AnimeCatalogueSource, AnimeSourceFactory, ConfigurableAnimeSource}`, `...animesource.online.{AnimeHttpSource, ResolvableAnimeSource}`, `...animesource.model.{SAnime, SEpisode, Video, Hoster, Track, AnimesPage, AnimeFilterList}`, `eu.kanade.tachiyomi.network.NetworkHelper`.

- [ ] **Step 1:** Pin the aniyomi `main` SHA in the script; run it; record the copied file list in `VENDORED.md`. Source paths: `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/`, `source-api/src/androidMain/.../animesource/PreferenceScreen.kt`, `core/common/src/main/java/eu/kanade/tachiyomi/network/`. Drop non-anime (`eu.kanade.tachiyomi.source.*`) files.
- [ ] **Step 2: Write failing test** `VendoredRuntimeTest.lib14VideoConstructorExists` — `Video("u", "720p", "https://v")` compiles and `videoTitle == "720p"`; `hosterListFromVideos` — `listOf(video).toHosterList()` returns one `Hoster` whose `videoList` holds it.
- [ ] **Step 3: Run** `./gradlew :source_utilities:aniyomi:testAndroidHostTest` — fix compile errors by vendoring missing referenced files (record each in `VENDORED.md`) or adding `// OTAKU:` edits, never by renaming upstream classes. Compose-runtime annotations in upstream source may be stripped with an `// OTAKU:` note.
- [ ] **Step 4: Run** again — PASS; `./gradlew :source_utilities:aniyomi:compileKotlinJvm` — SUCCESS.
- [ ] **Step 5: Commit** `feat(aniyomi): module scaffold and vendored aniyomi runtime`

### Task 6: AniyomiLib, AniyomiTrust and repo client (commonMain)

**Files:**
- Create: `source_utilities/aniyomi/src/commonMain/kotlin/com/programmersbox/aniyomi/AniyomiLib.kt`, `AniyomiTrust.kt`
- Create: `source_utilities/aniyomi/src/commonMain/kotlin/com/programmersbox/aniyomi/repo/{AniyomiRepoModels.kt, AniyomiIndexV2.kt, AniyomiRepoClient.kt, Gzip.kt}` + platform `Gzip` actuals (copy mihonsupport's)
- Test: `source_utilities/aniyomi/src/commonTest/kotlin/com/programmersbox/aniyomi/{AniyomiLibTest.kt, AniyomiTrustTest.kt}`, `.../repo/AniyomiRepoClientTest.kt` with fixture strings trimmed from `/private/tmp/claude-501/-Users-jacobrein-StudioProjects-OtakuWorld/ecfda456-3c6d-4aac-b168-f2f7bc6acdaf/scratchpad/yuzono-index.json` (copy 2–3 entries inline; the scratch file may be gone, then fetch `https://raw.githubusercontent.com/yuzono/anime-repo/repo/index.min.json`)

**Interfaces:**
- Produces:
  - `object AniyomiLib { val SUPPORTED = setOf(14.0, 16.0, 17.0); fun libVersion(metadataLib: Int, versionName: String?): Double?; fun libVersion(versionName: String): Double?; fun isSupported(lib: Double?): Boolean }` — metadata int `0` means absent; `"16.55"` → `16.0`.
  - `object AniyomiTrust` — same API as `MihonTrust` (`trustId`, `isTrusted`).
  - `class AniyomiRepoClient(http: HttpClient) { suspend fun fetch(urls: List<String>): List<KmpRemoteSources>; val signingKeys: Set<String> }` — behavior as `MihonRepoClient` with: lib from `version` / `extensionLib`; filter by `AniyomiLib.isSupported`; name `removePrefix("Aniyomi: ")`; `nsfw` from `nsfw == 1` or `contentWarning > 0`; skip any URL whose `repo.json` has a `pluginLists` key.

- [ ] **Step 1: Write failing tests** — `libVersion_fromMetadataThenVersionName` (17 → 17.0; 0 + "16.55" → 16.0; 0 + "15.3" → unsupported); trust tests ported from `MihonTrustTest`; repo: `parsesLegacyIndex` (lib 14 and 16 kept, lib 15 dropped, apk/icon URLs built from base), `parsesIndexV2Json`, `normalizesIndexUrl` (`.../index.min.json` and `.../repo.json` and trailing `/` all hit the same base), `skipsCloudstreamRepo` (repo.json `{"name":"x","manifestVersion":1,"pluginLists":[...]}` → empty, no throw), `failingRepoSkipped`.
- [ ] **Step 2: Run** `./gradlew :source_utilities:aniyomi:jvmTest` — FAIL.
- [ ] **Step 3: Implement**, copying from `mihonsupport/src/commonMain/.../repo/*` and adapting the listed differences.
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** `feat(aniyomi): lib gate, trust rule, repo client`

### Task 7: Runtime bootstrap, loader, untrusted placeholder

**Files:**
- Create: `source_utilities/aniyomi/src/androidMain/kotlin/com/programmersbox/aniyomi/AniyomiRuntime.kt`
- Create: `.../aniyomi/loader/AniyomiExtensionLoader.kt`, `.../aniyomi/loader/AniyomiSourceInstantiator.kt`
- Create: `.../aniyomi/UntrustedAniyomiApiService.kt`
- Modify: `datastore/src/commonMain/proto/settings.proto` (add `repeated string trustedAniyomiExtensions = 40;`)
- Modify: `datastore/src/commonMain/kotlin/com/programmersbox/datastore/SettingsHandling.kt:166-170` (`trustedAniyomiExtensions: Flow<List<String>>`, `suspend fun addTrustedAniyomiExtension(id: String)`, no duplicates — mirror the Mihon pair; datastore has no test source set, so no test)
- Test: `source_utilities/aniyomi/src/androidHostTest/kotlin/com/programmersbox/aniyomi/loader/SourceInstantiationTest.kt`, `FakeSources.kt`

**Interfaces:**
- Consumes: Task 6 `AniyomiLib`, `AniyomiTrust`.
- Produces:
  - `object AniyomiRuntime { fun init(application: Application) }` — idempotent; Injekt: `Application`, `Context`, `NetworkHelper`, `JavaScriptEngine`, `Json { ignoreUnknownKeys = true; explicitNulls = false }`, `ProtoBuf`.
  - `class AniyomiExtensionLoader(context: Context) { suspend fun load(catalog: KmpExternalCustomApiServicesCatalog, repoSigningKeys: Set<String>, trustedIds: Set<String>, onTrust: suspend (String) -> Unit): List<KmpSourceInformation>; fun isExtension(packageName: String): Boolean }`
  - `object AniyomiSourceInstantiator { fun instantiate(classNames: String, pkg: String, loader: ClassLoader): List<AnimeCatalogueSource> }` — `;`-split, leading `.` → `pkg + name`, `AnimeSourceFactory.createSources()`, keep `AnimeCatalogueSource`, any failure throws for the whole package.
  - `class UntrustedAniyomiApiService(pkg, versionCode, fingerprint, reason, canTrust, onTrust) : KmpUntrustedApiService` — `serviceName = "aniyomi-untrusted:$pkg"`, `notWorking = true`.
- Loader rules (from spec): feature `tachiyomi.animeextension`; metadata `tachiyomi.animeextension.class`, `tachiyomi.animeextension.nsfw`, `aniyomix.extensionLib`, `aniyomix.contentWarning`; `DelegateLastClassLoader(sourceDir, null, context.classLoader)`; reasons `"Unsupported lib X"`, `"Untrusted"`, `"Failed to load: <message>"`.

- [ ] **Step 1: Write failing tests** (port `mihonsupport` `SourceInstantiationTest`): `relativeClassNamesResolved`, `factoryExpanded`, `nonCatalogueDropped`, `anyFailureFailsPackage`.
- [ ] **Step 2: Run** `./gradlew :source_utilities:aniyomi:testAndroidHostTest` — FAIL.
- [ ] **Step 3: Implement**, copying `MihonExtensionLoader`/`MihonRuntime`/`UntrustedMihonApiService` structure.
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** `feat(aniyomi): runtime, extension loader, untrusted placeholder, trust setting`

### Task 8: AniyomiApiService and mapper

**Files:**
- Create: `source_utilities/aniyomi/src/androidMain/kotlin/com/programmersbox/aniyomi/{AniyomiApiService.kt, AniyomiSourceMapper.kt, AniyomiHttpServers.kt}`
- Test: `source_utilities/aniyomi/src/androidHostTest/kotlin/com/programmersbox/aniyomi/{AniyomiApiServiceTest.kt, AniyomiSourceMapperTest.kt}`

**Interfaces:**
- Consumes: Task 1 contract; Task 7 `FakeSources.kt` (extend with fake lib-14 and lib-16 sources).
- Produces:
  - `open class AniyomiApiService(val source: AnimeCatalogueSource, val lib: Double) : KmpApiService` and `class ConfigurableAniyomiApiService(...) : AniyomiApiService(...), KmpConfigurableApiService` when `source is ConfigurableAnimeSource`. `canScroll = true`, `canDownload = false`, `canCast = false`, `displayName = "${source.name} (${source.lang})"`, `language = source.lang`.
  - Methods per spec mapper table. `chapterInfo`: lib `< 16` → `getVideoList(episode)` as one group with `server = null`; else `getHosterList(episode)`; non-lazy hosters with `videoList` use it, otherwise `getVideoList(hoster)`; each hoster in its own `runCatching` (failure logged, skipped); lazy hosters (`hoster.lazy`) become one placeholder `KmpStorage(quality = hoster.hosterName, server = hoster.hosterName)` with `extras` key `"aniyomiHoster"` stored in a per-service `Map<String, Hoster>` keyed by `hosterUrl` (KmpStorage has no extras; store the key in `filename`).
  - `resolve(storage)`: placeholder hoster → first video of `getVideoList(hoster)`; video needing resolve (`!video.initialized` or empty `videoUrl`) → `resolveVideo(video)`; `video.usesHttpServer()` → `AniyomiHttpServers.portFor(source)` then `video.copyHttpServer(port)`.
  - `object AniyomiHttpServers { fun portFor(source: AnimeHttpSource): Int }` — starts `source.createHttpServer()` once per `source.id`, keeps it for the process.
  - Mapper: `SAnime ↔ KmpItemModel`, details → `KmpInfoModel`, `SEpisode ↔ KmpChapterModel` (`extras["aniyomiUrl"]`, `extras["aniyomiMemo"]`, `extras["episodeNumber"]`, `extras["filler"]`), `Video → KmpStorage` (`link = videoUrl`, `quality = videoTitle`, `headers = (video.headers ?: source.headers).toMap()`, `subtitles = subtitleTracks.map { KmpSubtitle(it.url, it.lang) }`, `server = hosterName`, `mimeType = "application/x-mpegURL"` when the URL path ends in `.m3u8`, else null).
  - `chapterInfo` keeps the source's `sortVideos()` order when the source is an `AnimeHttpSource`.

- [ ] **Step 1: Write failing tests** — `lib14_usesEpisodeVideoList`, `lib16_usesHosters_serverNamesSet`, `chapterInfo_skipsFailingHoster` (one of two hosters throws → other's videos returned), `lazyHoster_resolvesOnResolve`, `videoWithoutHeaders_usesSourceHeaders`, `subtitlesMapped`, `relativeUrlRoundTrip` (port from `MihonSourceMapperTest`).
- [ ] **Step 2: Run** `./gradlew :source_utilities:aniyomi:testAndroidHostTest` — FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** `feat(aniyomi): api service, hosters, lazy resolve, http server`

### Task 9: Catalog, preferences, AnimeWorld wiring

**Files:**
- Create: `source_utilities/aniyomi/src/commonMain/kotlin/com/programmersbox/aniyomi/AniyomiCatalog.kt` (expect) + androidMain actual + jvm/ios stubs (copy mihonsupport's split; if mihon has no expect/actual, use an androidMain-only class and keep jvm/ios empty)
- Create: `.../androidMain/.../aniyomi/AniyomiModule.kt` (`val aniyomiModule`), `.../preferences/AniyomiPreferenceFragment.kt`, `.../preferences/AniyomiPreferencesScreen.kt`
- Modify: `animeworld/build.gradle.kts` (`implementation(projects.sourceUtilities.aniyomi)`)
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt:105-115` (`includes(aniyomiModule)`, `navigation<Screen.SourcePreferences> { SourcePreferencesRoute(it.serviceName) }`)
- Create: `animeworld/src/main/java/com/programmersbox/animeworld/SourcePreferencesRoute.kt` — dispatches on `serviceName` prefix: `"aniyomi:"` → `AniyomiPreferencesScreen`; Plan 2 adds `"cloudstream:"`.

**Interfaces:**
- Consumes: Tasks 6, 7, 8.
- Produces: `class AniyomiCatalog(application, settings: NewSettingsHandling, repoClient: AniyomiRepoClient) : KmpExternalCustomApiServicesCatalog` with `name = "Aniyomi"`, `hasRemoteSources = true`, `initialize()` = `AniyomiRuntime.init` + refresh keys + `loader.load(...)` using `trustedAniyomiExtensions` / `addTrustedAniyomiExtension`; `shouldReload(pkg)`; `getRemoteSources(customUrls) = repoClient.fetch(customUrls)`. `install` / `uninstall` keep the defaults (APK path).

- [ ] **Step 1: Implement** the catalog, Koin module and preferences (copy `MihonCatalog`, `MihonModule`, `MihonPreferenceFragment`, mangaworld's `MihonPreferencesScreen`; the fragment casts to `AniyomiApiService` and uses `ConfigurableAnimeSource` preference key `"source_${source.id}"`).
- [ ] **Step 2: Build** `./gradlew :animeworld:assembleNoFirebaseDebug` — SUCCESS; `./gradlew :source_utilities:aniyomi:allTests` — PASS.
- [ ] **Step 3: Release build** `./gradlew :animeworld:assembleNoFirebaseRelease` — SUCCESS (consumer rules applied).
- [ ] **Step 4: Commit** `feat(animeworld): wire native Aniyomi catalog and source preferences`

### Task 10: Device verification

Follow `~/.claude/rules/artemis.md`: `adb devices -l`; if more than one device, ask the user which
serial. Install `noFirebase` debug.

- [ ] **Step 1:** Settings → Extensions → custom repos → add `https://raw.githubusercontent.com/yuzono/anime-repo/repo/index.min.json`. Expect an "Aniyomi" shelf.
- [ ] **Step 2:** Install an anikoto-theme extension (Anikoto or Anichi). It loads trusted (repo-signed). Search "Dragon Ball", open a result, tap an episode. Expect picker grouped by server, playback starts, subtitles selectable if offered.
- [ ] **Step 3:** Install AnimePahe; play a Dragon Ball episode (exercises lazy resolve + HttpServer).
- [ ] **Step 4:** Open an extension's settings via the gear; change a pref; it persists across restart.
- [ ] **Step 5:** With a Chromecast session active (if available), play: it plays locally. Download button absent/disabled for Aniyomi sources.
- [ ] **Step 6:** Uninstall both extensions; app reloads without crash.
- [ ] **Step 7:** Repeat Step 2 on the `noFirebase` release build.
- [ ] **Step 8:** Record results (device serial, extension versions, pass/fail per step) in the PR description.
