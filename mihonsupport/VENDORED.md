# Vendored Mihon code

Source: https://github.com/mihonapp/mihon at `446f6df82c390ab760802c2fce3b677bce3f31c1` (Apache-2.0).

Extension APKs are compiled against `extensions-lib` stubs, so the host app must provide these
classes with their original package names. Re-sync with `scripts/vendor-mihon.sh <sha>`, then
re-apply every edit marked `OTAKU:` (listed below).

## Files (copied verbatim unless noted)

From `source-api/src/main/kotlin` (under `src/androidMain/kotlin/`):

- `eu/kanade/tachiyomi/source/CatalogueSource.kt`
- `eu/kanade/tachiyomi/source/ConfigurableSource.kt`
- `eu/kanade/tachiyomi/source/Source.kt`
- `eu/kanade/tachiyomi/source/SourceFactory.kt`
- `eu/kanade/tachiyomi/source/UnmeteredSource.kt`
- `eu/kanade/tachiyomi/source/model/Filter.kt`
- `eu/kanade/tachiyomi/source/model/FilterList.kt` — **edited** (see below)
- `eu/kanade/tachiyomi/source/model/MangasPage.kt`
- `eu/kanade/tachiyomi/source/model/Page.kt`
- `eu/kanade/tachiyomi/source/model/SChapter.kt`
- `eu/kanade/tachiyomi/source/model/SChapterImpl.kt`
- `eu/kanade/tachiyomi/source/model/SManga.kt`
- `eu/kanade/tachiyomi/source/model/SMangaImpl.kt`
- `eu/kanade/tachiyomi/source/model/SMangaUpdate.kt`
- `eu/kanade/tachiyomi/source/model/UpdateStrategy.kt`
- `eu/kanade/tachiyomi/source/online/HttpSource.kt`
- `eu/kanade/tachiyomi/source/online/ParsedHttpSource.kt`
- `eu/kanade/tachiyomi/source/online/ResolvableSource.kt`
- `eu/kanade/tachiyomi/util/JsoupExtensions.kt`

From `core/common/src/main/kotlin`:

- `eu/kanade/tachiyomi/network/AndroidCookieJar.kt`
- `eu/kanade/tachiyomi/network/DohProviders.kt`
- `eu/kanade/tachiyomi/network/HttpException.kt`
- `eu/kanade/tachiyomi/network/OkHttpExtensions.kt`
- `eu/kanade/tachiyomi/network/ProgressListener.kt`
- `eu/kanade/tachiyomi/network/ProgressResponseBody.kt`
- `eu/kanade/tachiyomi/network/Requests.kt`
- `eu/kanade/tachiyomi/network/interceptor/RateLimitInterceptor.kt`
- `eu/kanade/tachiyomi/network/interceptor/SpecificHostRateLimitInterceptor.kt`
- `eu/kanade/tachiyomi/network/interceptor/UncaughtExceptionInterceptor.kt`
- `eu/kanade/tachiyomi/network/interceptor/UserAgentInterceptor.kt`
- `tachiyomi/core/common/util/lang/RxCoroutineBridge.kt`
- `tachiyomi/core/common/util/system/LogcatExtensions.kt`
- `mihon/core/common/extensions/JsonObject.kt`
- `eu/kanade/tachiyomi/network/interceptor/CloudflareInterceptor.kt` — **edited** (see below)
- `eu/kanade/tachiyomi/network/interceptor/WebViewInterceptor.kt` — **edited** (see below)
- `eu/kanade/tachiyomi/util/system/DeviceUtil.kt`
- `eu/kanade/tachiyomi/util/system/WebViewUtil.kt`

(The last four are fetched only with `WITH_WEBVIEW=1`. Re-running the script overwrites all edits
below, including `FilterList.kt`; re-apply them.)

### Local edits (`OTAKU:`)

- `eu/kanade/tachiyomi/source/model/FilterList.kt`: removed the `androidx.compose.runtime.Stable`
  import and `@Stable` annotation; this module has no Compose dependency. The annotation has no
  effect on extension binary compatibility.

- `CloudflareInterceptor.kt`: removed imports `util.system.toast`, `core.common.i18n.stringResource`,
  `tachiyomi.i18n.MR` (moko-resources not vendored). The bypass-failure `IOException` message is the
  literal "Failed to bypass Cloudflare"; the outdated-WebView toast is a plain
  `Toast.makeText(...)` with a literal string.
- `WebViewInterceptor.kt`: removed imports `util.system.toast`, `tachiyomi.i18n.MR`; the
  WebView-required toast is a plain `Toast.makeText(...)` with a literal string.
- `NetworkHelper.kt` (hand-written): `client` now adds `CloudflareInterceptor`.

### Build notes

- `OkHttpExtensions.kt` (`parseAs` / `decodeFromJsonResponse`) needs
  `org.jetbrains.kotlinx:kotlinx-serialization-json-okio`; it is a dependency of `androidMain`
  rather than an edit, because extensions inline `parseAs` and call into it.

## Hand-written replacements

- `eu/kanade/tachiyomi/network/NetworkHelper.kt` — replaces Mihon's Metro-injected `NetworkHelper`
  (no `NetworkPreferences`, no DoH/verbose-logging toggles). Keeps the extensions-lib surface:
  `client`, `cloudflareClient` (deprecated alias of `client`), `cookieJar`,
  `defaultUserAgentProvider()`.
- `eu/kanade/tachiyomi/network/JavaScriptEngine.kt` — Mihon's `JavaScriptEngine` without Metro
  annotations.
- `eu/kanade/tachiyomi/AppInfo.kt` — implements the extensions-lib `AppInfo` surface from the
  host `Application` in Injekt.
- `com/programmersbox/mihon/MihonRuntime.kt` — registers `Application`, `Context`, `NetworkHelper`,
  `JavaScriptEngine` and `Json` in Injekt before extensions load.

Not vendored: Mihon's `NetworkHelper`, `NetworkPreferences`, `JavaScriptEngine` and
`ApolloExtensions` (depend on Metro DI / Apollo).
