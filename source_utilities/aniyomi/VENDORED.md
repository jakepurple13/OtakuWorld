# Vendored Aniyomi code

Source: https://github.com/aniyomiorg/aniyomi at `97414446b8a95994c72dd33c41c971a89d4d25b8` (Apache-2.0).

Anime extension APKs are compiled against Aniyomi's `extensions-lib` stubs, so the host app must
provide these classes with their original package names and JVM signatures. Re-sync with
`scripts/vendor-aniyomi.sh <sha>`, then re-apply every edit marked `OTAKU:` (listed below).

This module defines `eu.kanade.tachiyomi.network.*`, `tachiyomi.core.common.*`,
`mihon.core.common.*` and `app.cash.quickjs.QuickJs`, as `:mihonsupport` does. The two modules
must never be packaged into the same APK.

## Files (copied verbatim unless noted)

From `source-api/src/commonMain/kotlin` (under `src/androidMain/kotlin/`):

- `eu/kanade/tachiyomi/animesource/AnimeCatalogueSource.kt`
- `eu/kanade/tachiyomi/animesource/AnimeSource.kt`
- `eu/kanade/tachiyomi/animesource/AnimeSourceFactory.kt`
- `eu/kanade/tachiyomi/animesource/ConfigurableAnimeSource.kt`
- `eu/kanade/tachiyomi/animesource/UnmeteredSource.kt`
- `eu/kanade/tachiyomi/animesource/model/AnimeFilter.kt`
- `eu/kanade/tachiyomi/animesource/model/AnimeFilterList.kt` — **edited** (see below)
- `eu/kanade/tachiyomi/animesource/model/AnimeRelation.kt`
- `eu/kanade/tachiyomi/animesource/model/AnimeUpdateStrategy.kt`
- `eu/kanade/tachiyomi/animesource/model/AnimesPage.kt`
- `eu/kanade/tachiyomi/animesource/model/FetchType.kt`
- `eu/kanade/tachiyomi/animesource/model/Hoster.kt`
- `eu/kanade/tachiyomi/animesource/model/HttpServer.kt`
- `eu/kanade/tachiyomi/animesource/model/SAnime.kt`
- `eu/kanade/tachiyomi/animesource/model/SAnimeEpisodeUpdate.kt`
- `eu/kanade/tachiyomi/animesource/model/SAnimeImpl.kt`
- `eu/kanade/tachiyomi/animesource/model/SAnimeSeasonUpdate.kt`
- `eu/kanade/tachiyomi/animesource/model/SEpisode.kt`
- `eu/kanade/tachiyomi/animesource/model/SEpisodeImpl.kt`
- `eu/kanade/tachiyomi/animesource/model/ThumbnailInfo.kt`
- `eu/kanade/tachiyomi/animesource/model/Video.kt`
- `eu/kanade/tachiyomi/animesource/online/AnimeHttpSource.kt`
- `eu/kanade/tachiyomi/animesource/online/ParsedAnimeHttpSource.kt`
- `eu/kanade/tachiyomi/animesource/online/ResolvableAnimeSource.kt`
- `eu/kanade/tachiyomi/animesource/utils/Preferences.kt`
- `eu/kanade/tachiyomi/util/JsonExtensions.kt`
- `eu/kanade/tachiyomi/util/JsoupExtensions.kt`

From `source-api/src/androidMain/kotlin`:

- `eu/kanade/tachiyomi/animesource/PreferenceScreen.kt` — **edited** (see below)

From `core/common/src/main/java`:

- `eu/kanade/tachiyomi/network/AndroidCookieJar.kt`
- `eu/kanade/tachiyomi/network/DohProviders.kt`
- `eu/kanade/tachiyomi/network/HttpException.kt`
- `eu/kanade/tachiyomi/network/JavaScriptEngine.kt`
- `eu/kanade/tachiyomi/network/OkHttpExtensions.kt`
- `eu/kanade/tachiyomi/network/ProgressListener.kt`
- `eu/kanade/tachiyomi/network/ProgressResponseBody.kt`
- `eu/kanade/tachiyomi/network/Requests.kt`
- `eu/kanade/tachiyomi/network/interceptor/CloudflareInterceptor.kt` — **edited** (see below)
- `eu/kanade/tachiyomi/network/interceptor/RateLimitInterceptor.kt`
- `eu/kanade/tachiyomi/network/interceptor/SpecificHostRateLimitInterceptor.kt`
- `eu/kanade/tachiyomi/network/interceptor/UncaughtExceptionInterceptor.kt`
- `eu/kanade/tachiyomi/network/interceptor/UserAgentInterceptor.kt`
- `eu/kanade/tachiyomi/network/interceptor/WebViewInterceptor.kt` — **edited** (see below)
- `eu/kanade/tachiyomi/util/system/DeviceUtil.kt`
- `eu/kanade/tachiyomi/util/system/WebViewUtil.kt`
- `tachiyomi/core/common/util/lang/CoroutinesExtensions.kt`
- `tachiyomi/core/common/util/lang/RxCoroutineBridge.kt`
- `tachiyomi/core/common/util/system/LogcatExtensions.kt`
- `mihon/core/common/extensions/JsonObject.kt`

### Local edits (`OTAKU:`)

- `AnimeFilterList.kt`: removed the `androidx.compose.runtime.Stable` import and `@Stable`
  annotation; this module has no Compose dependency. The annotation has no effect on extension
  binary compatibility.
- `PreferenceScreen.kt`: `actual typealias` became a plain `typealias`. The commonMain
  `expect class PreferenceScreen` is not vendored because the runtime is Android-only.
- `CloudflareInterceptor.kt`: removed imports `util.system.toast`, `core.common.i18n.stringResource`,
  `tachiyomi.i18n.MR` (moko-resources not vendored). The bypass-failure `IOException` message is
  the literal "Failed to bypass Cloudflare"; the outdated-WebView toast is a `Toast.makeText(...)`
  with a literal string, posted via the main executor (the call site is an OkHttp thread).
- `WebViewInterceptor.kt`: removed imports `util.system.toast`, `tachiyomi.i18n.MR`; the
  WebView-required toast is a plain `Toast.makeText(...)` with a literal string (already inside
  `launchUI`, so no thread hop needed).

## Hand-written replacements

- `eu/kanade/tachiyomi/network/NetworkHelper.kt` — replaces Aniyomi's `NetworkHelper`, which
  takes `NetworkPreferences` (not vendored; it needs Aniyomi's preference store). No DoH or
  verbose-logging toggles; fixed default user agent. Keeps the extensions-lib surface: `client`,
  `cloudflareClient` (deprecated alias of `client`), `cookieJar`, `defaultUserAgentProvider()`.
- `app/cash/quickjs/QuickJs.kt` — copied from `:mihonsupport`. Replaces the `quickjs-android`
  artifact; delegates to Zipline's `app.cash.zipline.QuickJs` so only one `libquickjs.so` ships.

## Not vendored

- `eu.kanade.tachiyomi.source.*` (manga API), `torrentutils/**`, `util/RxExtension.kt`
  (expect/actual duplicate of `tachiyomi.core.common.util.lang.awaitSingle`), `util/VideoInfo.kt`
  (unreferenced).
- `NetworkPreferences`, `ToastExtensions`, i18n, and the rest of `core/common` (preferences,
  storage, torrent, FFmpeg helpers).

## Build notes

- nanohttpd (`org.nanohttpd:nanohttpd:2.3.1`) backs `HttpServer`.
- `OkHttpExtensions.kt` (`parseAs` / `decodeFromJsonResponse`) needs
  `kotlinx-serialization-json-okio`; extensions inline `parseAs` and call into it.
- `consumer-rules.pro` keeps the vendored and extension-facing library packages in consuming apps,
  because extensions bind to them by name at runtime.
