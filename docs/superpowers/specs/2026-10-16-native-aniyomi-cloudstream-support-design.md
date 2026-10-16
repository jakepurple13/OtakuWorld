# Native Aniyomi and CloudStream Support in AnimeWorld — Design

Date: 2026-10-16
Branch: `worktree-animeworld-extension-support`

## Intent

AnimeWorld loads [Aniyomi](https://github.com/aniyomiorg/aniyomi) anime extensions and
[CloudStream](https://github.com/recloudstream/cloudstream) plugins natively, the same way
MangaWorld loads Mihon extensions through `:mihonsupport` (see
`docs/superpowers/specs/2026-10-13-native-mihon-support-design.md`). No bridge APK. Users add a
repo, install from the existing extensions screen, then browse, search and play.

**Success test:** search "Dragon Ball", open a series, and play an episode, once through an Aniyomi
extension and once through a CloudStream plugin, on a real device.

## Decisions (agreed with the user)

- Each runtime lives in its own module, nested under `source_utilities`:
  `:source_utilities:aniyomi` and `:source_utilities:cloudstream`.
- **AnimeWorld accepts GPL-3.0.** CloudStream is GPL-3.0; vendoring its `library` and shimming its
  app classes makes AnimeWorld a derivative work. OtakuWorld itself stays Apache-2.0.
- CloudStream ships in **all three flavors** (`noFirebase`, `noCloudFirebase`, `full`); the user
  accepts the GPL/Firebase combination. Plain `implementation`, no per-flavor wiring.
- **Casting and downloading are off** for Aniyomi and CloudStream sources for now.
- Build order: Aniyomi end to end first, then CloudStream. The player work lands with Aniyomi and
  CloudStream reuses it.
- Both plug into the existing `NativeCatalogs` / `KmpExternalCustomApiServicesCatalog` pipeline, so
  the extensions screen, installed list, global search and language sheet need no new screens.

## Goals

1. Aniyomi extension APKs (lib 14, 16, 17; aniyomi `main` itself still accepts all three and keeps
   the lib-14 `getVideoList(episode)` / `Video(url, quality, videoUrl, ...)` paths) load, browse, search, show episodes and play.
2. CloudStream `.cs3` plugins download from a repo, load, browse, search, show episodes and play.
3. The AnimeWorld player honors every stream header, sidecar subtitles, and a two-level
   "server, then quality" choice.
4. Source preferences screens work for both (Aniyomi `ConfigurableAnimeSource`; CloudStream
   `Plugin.openSettings`).
5. Trust gate before any third-party code is class-loaded.

## Non-goals

- Desktop/JVM and iOS. Modules are KMP like `:mihonsupport`, but non-Android targets are stubs
  (`getSources() = emptyList()`).
- Aniyomi seasons (`FetchType.Seasons`, `getSeasonList`). Season-type anime show their episode list
  only if the extension returns one; otherwise they show empty.
- Aniyomi search filters UI, related anime, video thumbnails/timestamps, mpv/ffmpeg args.
- CloudStream DRM links (`DrmExtractorLink`), torrent/magnet links, `ExtractorLinkPlayList`,
  `VideoClickAction`, JVM `jarUrl` plugins, short-code repo URLs (`cloudstreamrepo://`, cutt.ly).
- Casting and downloading for Aniyomi/CloudStream sources. Both API services report
  `canDownload = false`; `GenericAnime.playOrCast` skips the cast branch and plays locally when the
  source's `serviceName` starts with `aniyomi:` or `cloudstream:` (builder may use a marker
  interface instead). Revisit later.
- Downloading HLS/DASH streams (existing behavior: m3u8 filtered out of downloads stays).
- Deleting the legacy bridge code. (Native loading replaces the bridge at runtime; see
  "Legacy bridge" below.)
- Migrating favorites created through the old bridge.

## Anti-goals

- A failing or malicious extension must never crash the app on load. Per-package failures become
  placeholder sources (same as `UntrustedMihonApiService`).
- No GPL code reaches MangaWorld or NovelWorld. Only `animeworld` depends on
  `:source_utilities:cloudstream`.
- No behavior change for existing AnimeWorld plugin-APK sources.

## Constraints

- minSdk 28, compileSdk 37, Kotlin 2.4.20, OkHttp 5.4.0, Media3 1.11.0, kotlinx.serialization
  1.11.0 (`buildSrc/.../AppInfo.kt`, `gradle/*.versions.toml`).
- Extensions link to host classes by fully-qualified name through the parent class loader. Every
  vendored/shimmed class keeps its upstream package, name and JVM signature, and R8 must keep them
  (consumer `proguard` rules in each module).
- `:mihonsupport` and `:source_utilities:aniyomi` both contain `eu.kanade.tachiyomi.network.*`. They
  must never be in the same APK. Today only `mangaworld` depends on `:mihonsupport`; keep it so.
- Settings are per app (AnimeWorld has its own DataStore), so AnimeWorld's `customUrls` list holds
  only anime repo URLs.

## Module layout

```
settings.gradle.kts: include(":source_utilities:aniyomi", ":source_utilities:cloudstream")

:source_utilities:aniyomi       (otaku-multiplatform, namespace com.programmersbox.aniyomi, Apache-2.0)
  commonMain   repo client + models, AniyomiTrust, AniyomiCatalog (expect)
  androidMain  vendored eu.kanade.tachiyomi.animesource.**, eu.kanade.tachiyomi.network.**,
               tachiyomi.core.common.**, mihon.core.common.extensions.*;
               AniyomiRuntime, loader, AniyomiApiService, mapper, untrusted placeholder,
               preferences fragment/screen, consumer-rules.pro
  VENDORED.md + scripts/vendor-aniyomi.sh (pinned aniyomi SHA, file list, `// OTAKU:` edits)

:source_utilities:cloudstream   (otaku-multiplatform, namespace com.programmersbox.cloudstream, GPL-3.0)
  LICENSE (GPL-3.0 text), VENDORED.md + scripts/vendor-cloudstream.sh
  commonMain   repo client + models, CloudstreamCatalog (expect)
  androidMain  vendored com.lagradost.cloudstream3.** from `library` (MainAPI, ExtractorApi,
               extractors/**, utils/**, plugins/BasePlugin, MainActivityKt `app`),
               app-module shims (see below), plugin manager, CloudstreamApiService, mapper,
               preferences screen, consumer-rules.pro
```

`animeworld/build.gradle.kts` adds both modules. `GenericAnime`'s Koin module includes
`aniyomiModule` and `cloudstreamModule`, each binding its catalog as
`KmpExternalCustomApiServicesCatalog`, and registers `navigation<Screen.SourcePreferences>`
(same as `GenericManga.kt:62`), dispatching to the right preferences screen by `serviceName`
prefix.

Code from `:mihonsupport` that is generic (trust function, cert fingerprinting, legacy/v2 index
parsing) is copied into `:source_utilities:aniyomi`, not extracted into a shared module. Builder's
call whether a later cleanup extracts it.

## Legacy bridge

`SourceLoader` already skips `com.programmersbox.tachiyomibridge` and
`com.programmersbox.customtachiyomibridge` whenever `NativeCatalogs` is non-empty
(`sharedutils/kmpextensionloader/.../SourceLoader.kt:32-54`). Binding the Aniyomi catalog in
AnimeWorld therefore disables the bridge automatically: no duplicate sources and no class clash
with the app's vendored `eu.kanade.tachiyomi.*`. Same outcome as the Mihon work. No loader change.
*(Designer's call, following the Mihon precedent.)* "No behavior change for existing plugin-APK
sources" applies to OtakuWorld-format anime plugins, not the bridge.

## Shared changes (kmpmodels / kmpuiviews / animeworld)

### Catalog install hook

CloudStream plugins are files, not APKs. Add to `KmpExternalCustomApiServicesCatalog`:

```kotlin
/** Return true if this catalog installed [remote] itself; false lets the APK installer run. */
suspend fun install(remote: KmpRemoteSources): Boolean = false
suspend fun uninstall(source: KmpSourceInformation): Boolean = false
```

`ExtensionListViewModel.install/uninstall/updateAll` asks the owning catalog first (the catalog
that produced the remote shelf / the source's `catalog`) and falls back to the existing
`DownloadAndInstaller` path. After a catalog install, call `sourceLoader.blockingLoad()`.

### Stream model

`KmpStorage` already has `headers`, `quality`, `sub`, `source`. Add, with defaults so existing
sources are unaffected:

```kotlin
val subtitles: List<KmpSubtitle> = emptyList()   // KmpSubtitle(url, lang, headers)
val server: String? = null                        // hoster / extractor name for grouping
```

### Player

- `VideoViewModel` builds its `DataSource.Factory` from an OkHttp data source with **all**
  `storage.headers` as default request properties (Referer kept, hardcoded "AnimeWorld" UA only when
  the source sets none).
- Sidecar subtitles: each `KmpSubtitle` becomes a `MediaItem.SubtitleConfiguration` (MIME inferred
  from extension: vtt, srt, ass/ssa).
- Content type: use an explicit MIME when the source gives one (CloudStream `ExtractorLinkType`,
  stored as `extras`-style hint on `KmpStorage` or a new `mimeType` field, builder's call), else
  `Util.inferContentType`.

### Source picker

`chapterOnClick` already shows `VideoChoiceScreen` when there are several `KmpStorage`. Group the
list by `server`, then quality. One server: same as today. Builder chooses the exact UI (sections
in the existing sheet is enough).

### Lazy resolve

Aniyomi hosters and videos can be lazy. `chapterInfo` returns what it can cheaply; resolving a
lazy item happens on pick. Add an optional `suspend fun resolve(storage: KmpStorage): KmpStorage`
on `KmpApiService` (default returns `storage`) called by `GenericAnime.playOrCast` before playing.

## Aniyomi module

### Vendored runtime

From aniyomi `main` (pin the SHA in `VENDORED.md`):

- `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/**` and
  `source-api/src/androidMain/.../animesource/PreferenceScreen.kt`.
- `core/common/src/main/java/eu/kanade/tachiyomi/network/**` (NetworkHelper, Requests,
  OkHttpExtensions, JavaScriptEngine, AndroidCookieJar, DohProviders, interceptors).
- Small deps: `mihon.core.common.extensions.EMPTY`, `tachiyomi.core.common.util.lang.awaitSingle`,
  logcat helpers.
- `app.cash.quickjs.QuickJs` shim over Zipline, copied from `:mihonsupport`.

Host-provided libs (androidMain `api`): injekt (`com.github.null2264.injekt`), RxJava 1.3.8, OkHttp,
jsoup, kotlinx.serialization json/protobuf/json-okio, nanohttpd (`fi.iki.elonen`),
androidx.preference, coroutines.

### Runtime bootstrap

`AniyomiRuntime.init(application)` (idempotent, before any class load) registers in Injekt:
`Application`, `Context`, `NetworkHelper` (with `client`, `cloudflareClient`,
`defaultUserAgentProvider()`), `JavaScriptEngine`, `Json`, `ProtoBuf`. Same as `MihonRuntime`.

### Loader

Mirrors `MihonExtensionLoader`:

1. Installed packages with feature `tachiyomi.animeextension`.
2. Lib version from `aniyomix.extensionLib`, else `versionName.substringBeforeLast('.')`. Accept
   `{14, 16, 17}`; others become an untrusted placeholder with reason `"Unsupported lib X"`.
3. SHA-256 cert fingerprints, `AniyomiTrust.isTrusted(pkg, versionCode, fps, repoKeys, trustedIds)`
   (same rule as Mihon). Unsigned never trusted.
4. `DelegateLastClassLoader(sourceDir, null, context.classLoader)`; instantiate each class in
   `tachiyomi.animeextension.class` (`;`-split, leading `.` = package-relative). `AnimeSource` or
   `AnimeSourceFactory.createSources()`. Keep `AnimeCatalogueSource` only.
5. NSFW from `tachiyomi.animeextension.nsfw` or `aniyomix.contentWarning > 0`.
6. Any per-package failure becomes `UntrustedAniyomiApiService` (`serviceName =
   "aniyomi-untrusted:$pkg"`).

User trust persists in a new `repeated string trustedAniyomiExtensions` field in
`datastore/.../settings.proto` with helpers next to `trustedMihonExtensions`.

### Repo client

Same shape as `MihonRepoClient`: `repo.json` meta (`signingKeyFingerprint`), legacy
`index.min.json` (`version: "16.55"` gives lib 16), and `index_v2` (JSON or protobuf, maybe
gzipped) with `extensionLib`. Filter to supported libs. APK at `<base>/apk/<apk>`, icon at
`<base>/icon/<pkg>.png`. A URL whose `repo.json` has `pluginLists` (a CloudStream repo) is skipped.

Reference repo for testing: `https://raw.githubusercontent.com/yuzono/anime-repo/repo/index.min.json`.

### Mapper (`AniyomiApiService : KmpApiService`, + `KmpConfigurableApiService` when configurable)

| KmpApiService          | Aniyomi                                                                 |
|------------------------|-------------------------------------------------------------------------|
| `serviceName`          | `"aniyomi:${source.id}"`                                                |
| `recent(page)`         | `getLatestUpdates` if `supportsLatest` else `getPopularAnime`           |
| `allList(page)`        | `getPopularAnime`                                                       |
| `search`               | `getSearchAnime(page, query, getFilterList())`                          |
| `itemInfo`             | `getAnimeDetails` + `getEpisodeList` concurrently                       |
| `chapterInfo(episode)` | lib 14: `getVideoList(episode)`; lib 16/17: `getHosterList(episode)` then `getVideoList(hoster)` for non-lazy hosters; flatten to `KmpStorage` with `server = hoster.hosterName` |
| `resolve(storage)`     | lazy hoster: `getVideoList(hoster)`; lazy video: `resolveVideo(video)`  |

`Video -> KmpStorage`: `link = videoUrl`, `quality = videoTitle`, `headers = video.headers ?:
source.headers`, `subtitles = subtitleTracks`. Audio tracks ignored (non-goal).

`SAnime`/`SEpisode` mapping follows `MihonSourceMapper` (relative url and memo in `extras`,
`episode_number`, `date_upload`, `fillermark` into extras).

**HttpServer:** when `video.usesHttpServer()`, start the extension's `createHttpServer()` once per
source (kept for the app process), then `video.copyHttpServer(port)` before handing the URL to the
player. Needed by AnimePahe; not needed by the anikoto-theme extensions.

## CloudStream module

### Vendored runtime

From cloudstream `master` (pin SHA in `VENDORED.md`): `library/src/commonMain` and
`library/src/androidMain` under `com.lagradost.cloudstream3.**` and `com.lagradost.api.**`,
including the built-in `extractors/**` (plugins call `loadExtractor` which matches them).

**App-module shims** (same FQCN and JVM signatures as upstream, minimal bodies), driven by what
real plugins reference:

- `com.lagradost.cloudstream3.plugins.Plugin : BasePlugin` — `load(context)`, `resources`,
  `openSettings`, `registerVideoClickAction` (no-op).
- `com.lagradost.cloudstream3.CloudStreamApp` (+ deprecated `AcraApplication` alias) — companion
  `context`, `getKey`/`setKey`/`removeKey` backed by SharedPreferences.
- `com.lagradost.cloudstream3.CommonActivity` — `showToast`, `activity` (current Activity).
- `com.lagradost.cloudstream3.utils.DataStore` — key/value helpers over the same prefs.
- `com.lagradost.cloudstream3.network.CloudflareKiller`, `DdosGuardKiller`.
- `com.lagradost.cloudstream3.utils.AppContextUtils` (methods plugins actually call).

The builder confirms the exact list by dumping class references from the dex of the test plugins
(Anizone, AnimePahe, HiAnime, Anichi) and keeps that dump script in `scripts/`. A plugin that hits a
missing class at load becomes an untrusted/failed placeholder, not a crash.

Host-provided libs: NiceHttp 0.4.18 (`com.github.Blatzar:NiceHttp`), jackson-module-kotlin
strictly 2.13.1, jsoup, rhino strictly 1.8.1, coroutines, kotlinx.serialization, ksoup,
ktor-http, whyoleg cryptography, kotlinx-datetime/io, appcompat, fragment, material. NewPipe
extractor only if a vendored extractor needs it at compile time (builder may drop the YouTube
extractor instead).

Deprecated constructors (`ExtractorLink(...)`, `Episode(...)`, `SubtitleFile(lang, url)`) stay in
the vendored code exactly as upstream has them, since older plugins call them.

### Plugin manager

1. Install: download `plugin.url` to a temp file in `filesDir/cloudstream/<repo-hash>/`, verify
   `fileHash` (`sha256-<hex>`) when present, atomic move to `<internalName>.cs3`, `setReadOnly()`.
2. Load: `PathClassLoader(file, context.classLoader)`, read `manifest.json`, instantiate
   `pluginClassName` as `BasePlugin`. If `requiresResources`, build `Resources` from an
   `AssetManager` with `addAssetPath` (reflection, as upstream). Call `Plugin.load(context)` or
   `load()`.
3. Each `registerMainAPI` call becomes one `CloudstreamApiService`; `registerExtractorAPI` adds to
   the global `extractorApis`. Keep the plugin → APIs mapping for unload.
4. Uninstall: `beforeUnload()`, remove its APIs/extractors, delete the file.
5. Installed-plugin metadata (repo url, internalName, version, file path) in a small JSON file next
   to the plugins; updates compare `version` against the repo.

### Trust

`.cs3` files are unsigned. Trust is per repo, with no new dialog *(designer's call)*: installing
downloads the file but does not load it. A plugin from an untrusted repo shows as an untrusted
placeholder (existing `KmpUntrustedApiService` flow) whose `reason` names the repo; its Trust
action trusts the whole repo. Trusted repo URLs persist in a new
`repeated string trustedCloudstreamRepos` settings field.

### Repo client

`repo.json` `{name, manifestVersion, pluginLists[]}` then each `plugins.json` array of
`{url, status, version, apiVersion, name, internalName, tvTypes, language, iconUrl, fileHash, ...}`.
Keep entries whose `tvTypes` intersect `{Anime, AnimeMovie, OVA}`; drop `status == 0`.
`iconUrl` `%size%` → `128`. A URL whose `repo.json` lacks `pluginLists` is skipped (it is an
Aniyomi repo). Map to `KmpRemoteSources(downloadLink = url, packageName = internalName, ...)`.

Reference repo for testing:
`https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/builds/repo.json`
(Anizone confirmed to list Dragon Ball GT and Daima).

### Mapper (`CloudstreamApiService(api: MainAPI)`)

| KmpApiService          | CloudStream                                                         |
|------------------------|---------------------------------------------------------------------|
| `serviceName`          | `"cloudstream:${api.name}"` (plugin internalName prefix if names collide) |
| `language`             | `api.lang`                                                          |
| `recent(page)`         | first `mainPage` entry via `getMainPage(page, request)`             |
| `allList(page)`        | `mainPage` entries flattened                                        |
| `search`               | `search(query, page)` falling back to `search(query)`               |
| `itemInfo`             | `load(url)`; episodes from `AnimeLoadResponse.episodes` (sub first, then dub, names suffixed), `TvSeriesLoadResponse.episodes`, or one entry for `MovieLoadResponse.dataUrl` |
| `chapterInfo(episode)` | `loadLinks(episode.data, false, subCb, linkCb)`; collect all `ExtractorLink`s and `SubtitleFile`s, attach all subtitles to every link |

`ExtractorLink -> KmpStorage`: `link = url`, `quality = Qualities` name, `headers =
getAllHeaders()`, `server = link.source`, MIME from `type` (M3U8 → HLS, DASH, VIDEO →
progressive). TORRENT/MAGNET and DRM links are dropped.

Episode `data` is stored in `KmpChapterModel.extras["csData"]`.

### Preferences

If a plugin sets `openSettings`, the source is a `KmpConfigurableApiService` and the preferences
entry calls `openSettings(context)`. `BaseMainActivity` is a `FragmentActivity`, not
`AppCompatActivity`, and plugin settings often show `BottomSheetDialogFragment`s with
AppCompat/Material themes. The module ships a small transparent `AppCompatActivity`
(`CloudstreamSettingsActivity`, Material theme) that the preferences entry launches and that
calls `openSettings(this)`.

## Testing

- Unit (host JVM): repo clients (fixture JSON for yuzono index, phisher98 repo/plugins), trust
  functions, mappers with fake sources/APIs, header/subtitle mapping, lib version parsing.
- Device (ARTEMIS + adb), noFirebase debug build of `animeworld`:
  1. Add yuzono repo, install an anikoto-theme extension (Anikoto/Anichi), trust it, search
     "Dragon Ball", open, play an episode. Then AnimePahe (HttpServer path).
  2. Add phisher98 repo, trust repo, install Anizone, search "Dragon Ball", open, play an episode.
  3. Uninstall each; app restarts cleanly.
- Release build with R8 runs step 1 and 2 again (keep rules).

## Left to the builder

- Exact grouping UI in the source picker.
- Whether MIME hint is a new `KmpStorage` field or an `extras` entry.
- Whether to extract shared Mihon/Aniyomi code later.
- Exact CloudStream shim list (from dex dumps).
- Cache/eviction of CloudStream HTTP clients and Aniyomi `HttpServer` lifetimes.
