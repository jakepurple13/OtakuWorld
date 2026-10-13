# Native Mihon Extension Support — Design

Date: 2026-10-13
Branch: `feat/native-mihon-support`

## Goal

MangaWorld loads [Mihon](https://github.com/mihonapp/mihon) extension APKs natively, without the
external "Tachiyomi Bridge" APK. A new module `:mihonsupport` provides the Mihon extension runtime,
maps Mihon's source models to OtakuWorld's `Kmp*` models, lets users add Mihon extension repos and
install/update/uninstall extensions, and renders extension preference screens.

Android only for now. The module is a KMP module (`otaku-multiplatform`) so JVM/iOS targets compile
with stubs and can gain real implementations later.

## Decisions (agreed)

- **Approach A:** vendor Mihon's extension-facing runtime into the new module (Mihon is Apache-2.0).
- **Replaces the bridge:** native loading becomes the way Mihon extensions load. Bridge code paths are
  left in place (not deleted) but are no longer needed.
- **No favorites migration:** existing favorites/history created through the bridge are not migrated.
- **In scope:** load installed extension APKs, extension repos (install/update), source preferences.
- **Out of scope:** search filter UI, Aniyomi/AnimeWorld, Mihon's `index_v2` store format (legacy
  `repo.json` + `index.min.json` only), deleting bridge code.

## Background

- Extension APKs compile against `extensions-lib` stubs (`compileOnly`). At runtime the host must
  supply real classes with identical names: `eu.kanade.tachiyomi.source.**`,
  `eu.kanade.tachiyomi.network.**`, `eu.kanade.tachiyomi.util.**`, plus Injekt, RxJava 1, OkHttp,
  Jsoup, kotlinx.serialization, `androidx.preference`, QuickJS.
- Mihon identifies extensions by `<uses-feature android:name="tachiyomi.extension"/>` and metadata
  `tachiyomi.extension.class` (`;`-separated, `.`-prefixed = relative to package),
  `tachiyomi.extension.factory`, `tachiyomi.extension.nsfw`, `tachiyomix.extensionLib`.
- Mihon currently supports extension lib versions `1.4` and `1.6` (exact set `SUPPORTED_LIB_VERSIONS = listOf(1.4, 1.6)`, not a range).
- Existing pieces reused: `customUrls` setting (`datastore/.../settings.proto:15`), "Custom Tachiyomi
  Urls" sheet (`ExtensionListScreen.kt`), `DownloadStateInterface.downloadAndInstall`,
  `DownloadAndInstaller.uninstall`, `SourceLoader` package broadcast receiver, `SourceRepository`.
- The reader and downloader already merge `KmpStorage.headers` into image requests
  (`ReadViewModel.kt:85`, `MangaDownloadManager.android.kt:131`).

## Module layout

```
:mihonsupport  (otaku-multiplatform, namespace com.programmersbox.mihon)
  commonMain
    com/programmersbox/mihon/
      repo/MihonRepoModels.kt     @Serializable repo.json meta + index.min.json entries
      repo/MihonRepoClient.kt     Ktor client: normalize URLs, fetch, filter, map to KmpRemoteSources
      MihonCatalog.kt             expect class MihonCatalog : KmpExternalCustomApiServicesCatalog
  androidMain
    eu/kanade/tachiyomi/source/**     vendored from mihon source-api
    eu/kanade/tachiyomi/network/**    vendored from mihon core/common (extension-facing parts)
    eu/kanade/tachiyomi/util/**       vendored JsoupExtensions
    com/programmersbox/mihon/
      MihonRuntime.kt             Injekt bootstrap (Application, NetworkHelper, Json)
      MihonCatalog.android.kt     actual: wires loader + repo client
      loader/MihonExtensionLoader.kt   PackageManager scan, lib check, trust gate, class loading
      loader/ChildFirstPathClassLoader.kt
      loader/MihonTrust.kt        pure trust decision function
      MihonApiService.kt          CatalogueSource -> KmpApiService
      MihonSourceMapper.kt        SManga/SChapter/Page <-> Kmp models
      UntrustedMihonApiService.kt placeholder for untrusted / unsupported extensions
      preferences/MihonPreferenceFragment.kt
      preferences/MihonPreferencesScreen.kt
    consumer-rules.pro            keep rules moved from mangaworld/proguard-rules.pro
  jvmMain / iosMain
    MihonCatalog actual stubs: getSources() = emptyList(), getRemoteSources() = emptyList()
```

The vendored files keep their original package names and an attribution header
(`Copied from mihonapp/mihon <commit>, Apache-2.0`). The Mihon commit SHA is recorded in
`mihonsupport/VENDORED.md` with the list of copied files, so re-syncing is mechanical.

## Runtime bootstrap

`MihonRuntime.init(application)` runs once before any extension class loads (called from
`MihonCatalog.initialize()`, idempotent). It registers in Injekt:

- `Application`
- `NetworkHelper(application)` (vendored; cookie jar, DoH-free default, user agent,
  Cloudflare/WebView interceptors as vendored)
- `Json { ignoreUnknownKeys = true; explicitNulls = false }`

Anything else extensions request from Injekt is not provided; such extensions fail to load and are
reported as errors (logged), not crashes.

## Extension loading

`MihonExtensionLoader.load(context, repoSigningKeys, trustedIds): List<KmpSourceInformation>`

1. Query installed packages (`GET_CONFIGURATIONS | GET_SIGNING_CERTIFICATES`), keep those whose
   `reqFeatures` contains `tachiyomi.extension`.
2. Read `ApplicationInfo.metaData` (`GET_META_DATA`). Read `tachiyomix.extensionLib` as float; if
   not in `{1.4, 1.6}`, emit an `UntrustedMihonApiService` with reason `"Unsupported lib x.y"`.
3. Compute signing-certificate SHA-256 fingerprints. Apply `MihonTrust.isTrusted(...)` (below). If
   untrusted, emit `UntrustedMihonApiService` with reason `"Untrusted"`; do not class-load.
4. Create a `ChildFirstPathClassLoader(appInfo.sourceDir, nativeLibraryDir, appClassLoader)`. Parent-
   first for: `eu.kanade.tachiyomi.`, `kotlin`, `kotlinx.`, `okhttp3.`, `okio.`, `rx.`,
   `uy.kohesive.injekt.`, `org.jsoup.`, `androidx.`, `app.cash.quickjs.`, `java.`, `android.`.
   Everything else child-first.
5. For each class in `tachiyomi.extension.class` (resolve leading `.` against package name),
   instantiate via no-arg constructor. `Source` -> one source; `SourceFactory` ->
   `createSources()`. Keep only `CatalogueSource`.
6. Wrap each in `MihonApiService` and emit `KmpSourceInformation(apiService, name = source.name +
   " (" + source.lang + ")", icon = null, packageName = pkg, catalog = mihonCatalog)`.
7. Per-package failures (class not found, `LinkageError`, Injekt miss) are caught, logged, and that
   package is emitted as `UntrustedMihonApiService` with reason `"Failed to load: <message>"`.

Packages are loaded in parallel on `Dispatchers.Default.limitedParallelism(4)`, matching
`ExtensionLoader`.

### Trust

```kotlin
fun isTrusted(
    pkg: String,
    versionCode: Long,
    fingerprints: Set<String>,
    repoSigningKeys: Set<String>,
    trustedIds: Set<String>,
): Boolean =
    fingerprints.any { it in repoSigningKeys } ||
        fingerprints.any { "$pkg:$versionCode:$it" in trustedIds }
```

- `repoSigningKeys` = `signingKeyFingerprint` of every configured repo whose `repo.json` was fetched
  (cached in memory per session; on fetch failure, the last cached set is used).
- `trustedIds` stored in a new `repeated string trustedMihonExtensions` field in
  `datastore/src/commonMain/proto/settings.proto`, with add/remove helpers in `SettingsHandling`
  next to `customUrls`.
- A new version of a user-trusted extension requires trusting again (Mihon behavior).
- Extensions with no signatures are never trusted.

`UntrustedMihonApiService` has `notWorking = true`, `serviceName = "mihon-untrusted:<pkg>"`, and
exposes `packageName`, `versionCode`, `fingerprint`, `reason`. The extension list shows **Trust**
(when reason is `Untrusted`) and **Uninstall** for these entries. Trusting adds the id and triggers
`sourceLoader.blockingLoad()`.

## Catalog

`MihonCatalog : KmpExternalCustomApiServicesCatalog` (Koin singleton registered in `mangaworld`'s
app module):

- `name = "Mihon"`
- `initialize()`: `MihonRuntime.init`, refresh repo signing keys, run loader.
- `getSources()`: last load result.
- `hasRemoteSources = true`
- `getRemoteSources(customUrls)`: `MihonRepoClient.fetch(customUrls)`.
- `shouldReload(packageName)`: true if the package declares `tachiyomi.extension`.

`SourceLoader` (Android actual) gets `MihonCatalog` via constructor injection and merges
`mihonCatalog.getSources()` (after `initialize()`) into the list passed to
`sourceRepository.setSources(...)` in both `load()` and `blockingLoad()`. Its broadcast receiver
also reloads when `mihonCatalog.shouldReload(packageString)`.

`ExtensionListViewModel.hasCustomBridge` becomes true when any installed source's catalog is a
`MihonCatalog` **or** the existing bridge condition holds, so the custom-URL sheet stays reachable.

## Repo client (commonMain)

- URL normalization: trim; strip trailing `/index.min.json`, `/repo.json`, and `/`. Result is the
  repo base.
- Fetch `<base>/repo.json` -> `meta { name, shortName?, website, signingKeyFingerprint }`.
- Fetch `<base>/index.min.json` -> list of
  `{ name, pkg, apk, lang, code, version, nsfw, sources: [{ name, lang, id, baseUrl }] }`.
  `extensionLib` is parsed from `version` (`"1.4.12"` -> `1.4`).
- Drop entries whose lib is not in `{1.4, 1.6}`.
- Map to `KmpRemoteSources(name = entry.name.removePrefix("Tachiyomi: ") + (if nsfw " (18+)"
  else ""), packageName = pkg, version = version, iconUrl = "<base>/icon/<pkg>.png",
  downloadLink = "<base>/apk/<apk>", sources = sources.map { KmpSources(name, baseUrl, version) })`.
- Repos are fetched concurrently; a failing repo is logged and skipped.
- Exposes `signingKeys(): Set<String>` from the last successful fetch for the trust gate.

Install/update/uninstall use the existing `DownloadStateInterface.downloadAndInstall(downloadLink)`
and `DownloadAndInstaller.uninstall(pkg)`; no new installer code.

## Mapper

`MihonApiService(val source: CatalogueSource) : KmpApiService` (also `KmpConfigurableApiService`
when `source is ConfigurableSource`).

| KmpApiService            | Mihon                                                                    |
|--------------------------|--------------------------------------------------------------------------|
| `baseUrl`                | `(source as? HttpSource)?.baseUrl.orEmpty()`                             |
| `serviceName`            | `"mihon:${source.id}"`                                                   |
| `canScroll`              | `true`                                                                   |
| `recent(page)`           | `getLatestUpdates(page)` if `supportsLatest` else `getPopularManga(page)` |
| `allList(page)`          | `getPopularManga(page)`                                                  |
| `search(text, page, _)`  | `getSearchManga(page, text.toString(), getFilterList())`                 |
| `itemInfo(model)`        | `getMangaDetails` and `getChapterList` concurrently                      |
| `chapterInfo(chapter)`   | `getPageList`, then `getImageUrl(page)` where `imageUrl == null`         |
| `sourceByUrl(url)`       | `(source as? ResolvableSource)?.getManga(url)` else `error(...)`         |

All calls run on `Dispatchers.IO`. Older extensions that only override `fetch*` Rx methods work
through the vendored `HttpSource` defaults.

Model mapping (`MihonSourceMapper`):

- `SManga -> KmpItemModel`: `title`, `description.orEmpty()`, `url = absolute(sManga.url)`,
  `imageUrl = thumbnail_url.orEmpty()`; `extras["mihonUrl"] = sManga.url`.
- `KmpItemModel -> SManga`: `url = extras["mihonUrl"] ?: relative(model.url)` where `relative` strips
  `baseUrl` if present; `title`, `thumbnail_url` copied.
- Details -> `KmpInfoModel`: `genres = genre?.split(",")?.map(String::trim).orEmpty()`,
  `alternativeNames = emptyList()`, `extras["author"]`, `extras["artist"]`, `extras["status"]`
  (`SManga` status int mapped to `"Ongoing"`, `"Completed"`, etc.), `extras["mihonUrl"]`.
- `SChapter -> KmpChapterModel`: `name`, `url = absolute(sChapter.url)`,
  `sourceUrl = info.url`, `uploaded` = formatted `date_upload` or `""`,
  `uploadedTime = date_upload.takeIf { it > 0 }`, `extras["mihonUrl"]`,
  `extras["chapterNumber"] = chapter_number`.
- `KmpChapterModel -> SChapter`: same URL rule as manga.
- `Page -> KmpStorage`: `link = imageUrl`, `headers` = header map of
  `(source as HttpSource).imageRequest(page)` (falls back to `source.headers`).
- `absolute(path)`: `path` if it starts with `http`, else `baseUrl + path`.
- Paging: `MangasPage.hasNextPage` stored on each returned item as `extras["hasNextPage"]`.

Errors propagate into the existing `catch` / `Result` paths of `KmpApiService`. No extra retry
logic.

## Source preferences

- New marker in `kmpmodels/commonMain`: `interface KmpConfigurableApiService : KmpApiService`.
- New `NavKey` in `kmpuiviews/commonMain/.../navigation/Screen.kt`:
  `@Serializable data class SourcePreferences(val serviceName: String)`.
- Extension list: installed sources whose `apiService is KmpConfigurableApiService` show a gear
  `IconButton` that calls `navigationActions.navigate(Screen.SourcePreferences(serviceName))`
  (adding the corresponding `NavigationActions` function).
- `mangaworld` (Android) registers the entry in `globalNav3Setup()`. It resolves the source through
  `SourceRepository.toSourceByApiServiceName` and shows `MihonPreferencesScreen`: Scaffold + top app
  bar (source name, back) + `AndroidFragment<MihonPreferenceFragment>` with the source id as an
  argument.
- `MihonPreferenceFragment : PreferenceFragmentCompat` sets
  `preferenceManager.sharedPreferencesName = "source_$id"`, creates a `PreferenceScreen`, calls
  `source.setupPreferenceScreen(screen)`, assigns it. The vendored
  `ConfigurableSource.getSourcePreferences()` uses the same `"source_$id"` name, so extensions see
  the edited values.
- Theme: module resource theme extending `PreferenceThemeOverlay`, day/night aware.
- Desktop registers no entry; no desktop source implements the marker, so the gear never appears.

## Build / dependencies

Added to the version catalogs (exact versions resolved at implementation time to match Mihon's
current catalog where binary compatibility matters):

- Android: `injekt` (`com.github.mihonapp:injekt`), `io.reactivex:rxjava` 1.x, `jsoup`,
  `androidx.preference:preference-ktx`, `app.cash.quickjs:quickjs-android`, `okhttp`,
  `okhttp-dnsoverhttps`, `okhttp-brotli`, `androidx.fragment:fragment-compose`.
- Common: Ktor client + content negotiation + kotlinx.serialization json.

`:mihonsupport` added to `settings.gradle.kts`; `mangaworld` depends on it;
`sharedutils:kmpextensionloader` androidMain depends on it (for `MihonCatalog` in `SourceLoader`).

R8: move `-keep` rules for `eu.kanade.**`, `tachiyomi.**`, `mihon.**` and the extension-dependency
block (`mangaworld/proguard-rules.pro:238-262`) into `mihonsupport/consumer-rules.pro`.

## Testing

- `commonTest` (Ktor `MockEngine`, keiyoushi-format fixtures):
  repo/index parsing, URL normalization cases, lib filtering, one failing repo does not drop others,
  signing keys exposed.
- Android host tests:
  - `MihonSourceMapper` against a fake in-test `CatalogueSource`/`HttpSource`: URL round-trip
    (absolute <-> `mihonUrl`, missing extras fallback), `hasNextPage`, details + chapters mapping,
    `Page` -> `KmpStorage` including headers and `getImageUrl` resolution.
  - `MihonTrust.isTrusted`: repo key match, user-trusted id match, version bump requires re-trust,
    no signatures.
  - Lib version gate (`1.4` and `1.6` accepted; `1.3`, `1.5`, `1.7` rejected).
- Build gates: `:mihonsupport:allTests`, `:mihonsupport:compileKotlinJvm`,
  `:mangaworld:assembleNoFirebaseDebug`, `:mangaworld:assembleNoFirebaseBeta`.
- Device verification on an emulator (ARTEMIS): add keiyoushi repo URL -> install one extension
  (e.g. MangaDex) -> appears untrusted -> trust -> browse popular/latest, search, open details ->
  read a chapter (images load) -> open gear, change a preference, confirm it takes effect ->
  uninstall -> disappears.

## Risks

- Vendored runtime drift: when Mihon bumps the lib version, `VENDORED.md` lists what to re-sync.
- Extensions depending on host classes not vendored here fail to load; they surface as
  `"Failed to load"` entries instead of crashing.
- Class-loader conflicts if the app's dependency versions (OkHttp, kotlinx.serialization) diverge
  from what extensions were compiled against; mitigated by aligning to Mihon's catalog versions.
