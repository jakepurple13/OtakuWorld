# AnimeWorld Desktop: Aniyomi + CloudStream Extensions and Video Player — Design

Date: 2026-10-23
Status: Draft, pending approval
Branch: `feat/animeworld-desktop-extensions`, based on `feat/mangaworld-desktop-mihon-extensions`
(`95ec75bf3`)

## Intent

AnimeWorld Desktop (`animeworld:desktop`, JVM) should use the same Aniyomi and CloudStream
extensions the Android app uses, managed the same way (add a repo, browse, install, trust, update,
uninstall), and the user must be able to **watch episodes inside the app** with nothing else
installed on the machine. Today desktop has no extension support for anime and no video player
(`VideoPlayerUi.jvm.kt` shows `VideoNotSupportedScreen`).

This mirrors the MangaWorld desktop Mihon work
(`2026-10-23-mangaworld-desktop-mihon-extensions-design.md`), which already built the generic
pieces: dex → jar conversion (`DexConverter`, `StackMapFrames`, `ElidedConstructorFix`), JVM
`android.*` / `androidx.preference` stubs, `FileSharedPreferences`, `ApkManifestParser`,
`ApkExtensionStore`, a child-first class loader, `apksig` trust verification, and native-catalog
merging in the JVM `SourceLoader`. This design reuses those and adds the anime runtimes and a
player.

## Goals

- From the existing Extensions screens on desktop, the user can add an Aniyomi repo and a
  CloudStream repo, see their extensions, install one, and its sources appear alongside other
  sources and work: browse, search, details, episode list, video list.
- Every Aniyomi and CloudStream extension that does not need a WebView/Cloudflare solver or
  Android resources should work (user requirement: "all of the extensions"; exclusions below are
  deferred, not dropped).
- Trust behaves as on Android for each ecosystem (Aniyomi: signature + repo key / per-version
  trust; CloudStream: per-repo trust).
- Update and uninstall work from the same screens.
- Episodes play in-app with **no external software installed** (no VLC/mpv on the machine).
- Subtitles work at parity with Android: external tracks (VTT/SRT/ASS) supplied by the extension,
  embedded tracks in the stream, preferred-language auto-selection, and styling from the shared
  `SubtitleSettingsRepository`.
- Works on macOS (arm64 + x64) and Windows (x64) via `./gradlew :animeworld:desktop:run`. Only
  macOS can be tested now.
- Android behavior is unchanged.

## Non-goals (v1, later phases)

- **Cloudflare / WebView-dependent extensions.** Aniyomi `CloudflareInterceptor`, WebView-based
  video extractors, and CloudStream `WebViewResolver` fail with a clear error. Embedded Chromium
  (KCEF/JCEF) is a later phase. (User decision.)
- **Source settings screens.** Aniyomi `ConfigurableAnimeSource` and CloudStream plugin settings
  load with defaults; no settings button on desktop. (User decision.)
- **Casting, volume gestures, brightness gestures** in the desktop player. (User decision.)
- **Packaged installers (DMG/MSI).** v1 is verified via `gradlew run` only. (User decision.) v1
  must not block packaging: bundled native player libs, ProGuard keep rules for the vendored
  extension ABI, jlink modules, and macOS entitlements/signing for native libs are follow-up work.
- CloudStream plugins with `requiresResources = true` (they load Android `Resources` from the
  plugin file). They show as "unsupported on desktop" unless they turn out to work with stubs.
  (My call.)
- Resume-from-last-position. The Android player does not persist it either. (My call.)
- Background extension update checks (Android doesn't run them for these ecosystems either).

## Anti-goals

- Any regression to Android Aniyomi/CloudStream support, to MangaWorld desktop's Mihon support,
  or to desktop jar-plugin loading.
- Loading an extension that has not passed the same trust check as on Android. Desktop must not be
  a weaker trust path.
- Requiring the user to install VLC, mpv, ffmpeg, a JRE add-on, or anything else to play video.
- Two copies of the same vendored class (e.g. `eu.kanade.tachiyomi.network.*`) on one desktop
  classpath.

## Current state (from recon)

Shared already:

- Extensions UI + ViewModel in `kmpuiviews` commonMain (same as the Mihon design).
- `aniyomi` commonMain: repo client, models, `Gzip` (has a `jvmMain` actual). `cloudstream`
  commonMain: `CloudstreamRepoClient`, `CloudstreamRepoModels`.
- `animeworld:shared` commonMain: `PlayerRequest.kt` (`playerHeaders`, `subtitleMime`,
  `embeddedTrackLabel`, preferred-language matching), `SubtitleSettingsRepository` (interface),
  `SubtitleSettingsViewModel`, `VideoPlayerUi` expect.
- On this branch, `sharedutils/kmpextensionloader` jvmMain has the generic conversion/loading
  pieces and the `android.*` stubs listed in Intent.

Android-only today:

- `source_utilities/aniyomi/src/androidMain`: `AniyomiCatalog`, `AniyomiModule`, `AniyomiRuntime`,
  `AniyomiApiService`, `AniyomiSourceMapper`, `UntrustedAniyomiApiService`, `AniyomiHttpServers`
  (NanoHTTPD local servers for the `HttpServer` model), `loader/AniyomiExtensionLoader` +
  `AniyomiSourceInstantiator`, `preferences/AniyomiPreferenceFragment`, and the vendored runtime
  (`eu.kanade.tachiyomi.animesource.*` 26 files, `eu.kanade.tachiyomi.network.*` 15,
  `eu.kanade.tachiyomi.util.*` 4, `tachiyomi.core.common.util.*`, `mihon.core.common.*`,
  `app.cash.quickjs.QuickJs`). See `source_utilities/aniyomi/VENDORED.md`.
- `source_utilities/cloudstream` has **no JVM target**. `androidMain` holds the OtakuWorld glue
  (`CloudstreamCatalog`, `CloudstreamModule`, `CloudstreamApiService`, `CloudstreamMapper`,
  `CloudstreamShims`, `UntrustedCloudstreamApiService`, `plugins/PluginStore`, `PluginLoader`
  (`PathClassLoader` over the `.cs3`, reads `manifest.json`, calls `Plugin.load(context)`),
  `InstalledPlugin`, `preferences/CloudstreamSettingsActivity`) plus 162 vendored files from
  upstream `library`. Upstream `library` is itself KMP with `jvmMain` actuals; the vendored copy
  collapses everything into `androidMain` and **keeps upstream file names** because plugins link
  against facade classes such as `Coroutines_androidKt`. CloudStream installs never used the
  system package installer: `PluginStore` downloads `.cs3` files into private storage.
- Player: `VideoPlayerCompose.android.kt` + `VideoViewModel` on Media3/ExoPlayer, with subtitle
  quick sheet, skip ±30 s / +90 s, quality list, cast. `DataStoreSubtitleSettingsRepository` is
  androidMain.

Desktop today:

- `animeworld:desktop` registers no catalogs and depends on neither `aniyomi` nor `cloudstream`.
- `VideoPlayerUi` on JVM is a stub. No desktop media library in the version catalogs.

## Design

### 1. Aniyomi runtime on JVM

Same move `mihonsupport` got: introduce a source set compiled for both Android and JVM (follow the
`mihonsupport` layout, `src/jvmAndroidShared`), and move the platform-neutral vendored runtime and
glue into it: `animesource.*`, `network.*` minus WebView pieces, plain-JVM `util.*`,
`AniyomiApiService`, `AniyomiSourceMapper`, `UntrustedAniyomiApiService`, Injekt runtime setup,
`AniyomiHttpServers` (NanoHTTPD is pure Java). Android-specific pieces get the same JVM seams the
Mihon design used (cookie jar, fixed desktop UA, failing `CloudflareInterceptor`, `DeviceUtil`,
logging, `SystemClock`). Keep `VENDORED.md` and its `OTAKU:` edit markers accurate.

The extension ABI stays byte-compatible: packages, class shapes and signatures that anime
extensions compile against are unchanged.

**Duplicate classes with `mihonsupport`.** Both modules define `eu.kanade.tachiyomi.network.*`,
`tachiyomi.core.common.*`, `mihon.core.common.*`, `app.cash.quickjs.QuickJs`. AnimeWorld desktop
must never pull `mihonsupport` onto its classpath (it has no reason to). Sharing those classes
between the two modules is out of scope.

### 2. JVM Aniyomi loader, install, trust

A JVM `AniyomiExtensionLoader` mirrors the Mihon JVM loader on this branch: APKs in a private
Aniyomi directory under the app data dir → manifest parse (feature `tachiyomi.animeextension`,
extension class, nsfw, lib version range from Aniyomi's supported versions) → `apksig` signature
verification → Aniyomi trust rules exactly as Android → `DexConverter` → child-first load →
`AniyomiApiService` or `UntrustedAniyomiApiService`.

Install/update/uninstall/installed-version follow the Mihon design's route: the JVM
`DownloadStateRepository` downloads Aniyomi APK links into the Aniyomi directory, converts, and
triggers a reload, reporting progress and errors through `downloadList`. Generalize the Mihon JVM
install path so it dispatches by ecosystem rather than copying it; how (link → ecosystem
resolution, per-catalog install handler) is the builder's call.

### 3. CloudStream on JVM

- Add a `jvm()` target to `source_utilities/cloudstream` and a shared Android+JVM source set
  holding the vendored `library` and the OtakuWorld glue that is platform-neutral.
- For each upstream expect/actual that was collapsed into `androidMain`, the JVM side uses
  upstream's `jvmMain` actual where one exists. **Facade class names must match what plugins link
  against.** Plugins are built against the Android artifact, so on JVM the top-level functions
  must still live in the `*_androidKt` facade classes (`Coroutines_androidKt` etc.). Keeping the
  same file names in the shared source set achieves that; the builder must check every facade the
  vendored set exposes.
- `WebViewResolver` on JVM fails with a clear error (deferred).
- `Plugin.load(context: Context)`: pass the JVM `Context` stub. `Plugin` / `BasePlugin` and the
  registration functions (`registerMainAPI`, `registerExtractorAPI`) and `PluginLoader.capture`
  stay as on Android.
- JVM `PluginLoader`: `.cs3` → extract `classes.dex` → `DexConverter` → jar plus `manifest.json`
  as a resource → child-first class loader (parent = app loader, so plugins link to the host's
  CloudStream classes) → instantiate `pluginClassName`. `requiresResources` plugins: try with
  stubs; if `Resources` access fails, surface as an unsupported/failed placeholder via the
  existing `placeholder(...)` path.
- `PluginStore` and per-repo trust (`trustedCloudstreamRepos`) work as on Android; only the
  storage directory and class loading differ.
- Any Android types the vendored `library` or plugins link against that are missing from the
  `kmpextensionloader` stubs get added there (one copy of each stub).

GPL-3.0: `cloudstream` is already GPL-3.0 and bundled into the Android app; bundling it into the
desktop app has the same licensing consequence. No change in posture.

### 4. Registration into AnimeWorld desktop

- `animeworld:desktop` depends on `source_utilities:aniyomi` and `source_utilities:cloudstream`,
  and includes JVM `aniyomiModule` / `cloudstreamModule` binding their catalogs as
  `KmpExternalCustomApiServicesCatalog` (or whatever `NativeCatalogs` contribution the JVM
  `SourceLoader` on this branch expects).
- Service names stay identical to Android (`aniyomi:<id>`, CloudStream's current scheme) so
  favorites/history keys sync with Android.
- The settings button is hidden for configurable sources on desktop (builder's call how, matching
  the Mihon approach).

### 5. Desktop video player

**Engine (my call, to be confirmed by a spike first):** vlcj with libVLC **bundled in the app**
per OS (macOS arm64, macOS x64, Windows x64). vlcj's native discovery is pointed at the bundled
copy, never at a system VLC. libVLC is LGPL-2.1; vlcj is GPL-3.0 (compatible with the app already
containing GPL-3.0 CloudStream code). How the libVLC binaries reach the app for `gradlew run`
(Gradle download task into Compose Desktop `appResources`, a Maven artifact, etc.) is the
builder's call, as long as a clean checkout + `./gradlew :animeworld:desktop:run` plays video on a
Mac with no VLC installed.

**Headers.** Extensions return `Video` URLs with arbitrary headers (Referer, Origin, cookies,
custom). VLC only supports Referer/User-Agent. A **localhost HTTP proxy** (NanoHTTPD, already a
dependency of `aniyomi`) fetches upstream with the full `playerHeaders(...)` result and serves to
libVLC. For HLS it must rewrite playlist URIs (variant playlists, segments, keys) so all follow-up
requests also go through the proxy with headers. Subtitle file downloads go through the same
header logic. The proxy binds to `127.0.0.1` only, on a random port, with an unguessable path
token per playback session.

**Fallback.** If the spike shows vlcj cannot render acceptably inside a Compose window on macOS
(embedded `Canvas` vs. callback rendering into a Compose `ImageBitmap`) or cannot be bundled
without a system install, switch to libmpv over JNA, which accepts arbitrary headers natively.

**Spike pass bar (my call).** On the user's Mac, with no VLC installed, from `gradlew run`:
a 1080p HLS stream that needs a custom header plays through the proxy with no visible stutter;
Compose controls and a subtitle line draw on top of the video; CPU stays reasonable (no
pegged core) during playback. Note that Compose overlays on macOS likely force callback
(offscreen) rendering for either engine, so frame-copy cost into Compose is the real risk the spike
measures. **Work stops after the spike** and the result is shown to the user for a go-ahead
before the real player is built.

**Player UI (`VideoPlayerUi.jvm.kt`):**

- Play/pause, seek bar with buffered range, current/total time, skip −30 s / +30 s / +90 s
  (parity with Android), fullscreen toggle.
- Keyboard: Space play/pause, ←/→ seek, F fullscreen, Esc exit fullscreen.
- Volume slider or system volume only (no gesture controls).
- Video choice at parity with Android: the extension's video list is picked **before** playback
  via the existing `VideoChoiceScreen` flow (`GenericSharedAnime.chapterOnClick` /
  `VideoSourceModel`), which desktop must now render; inside the player, quality choice is among
  HLS stream variants only (libVLC track/variant selection), keeping position. No in-player picker
  across extension videos. (My call: parity.)
- Subtitles: external tracks from `KmpSubtitle` (VTT/SRT/ASS via `subtitleMime`), embedded tracks
  from libVLC's SPU list labelled with `embeddedTrackLabel`, preferred-language auto-pick using
  the existing matching in `PlayerRequest.kt`, a track picker sheet, and styling from a **JVM
  `SubtitleSettingsRepository`** (new; Android's is DataStore-based in androidMain — move it to
  common if possible, else add a JVM implementation). How styling maps onto libVLC (freetype
  options vs. rendering subtitles in Compose ourselves) is the builder's call; ASS styling may be
  partially honored.
- Local downloaded files play directly without the proxy.
- Clear on-screen errors for: stream 4xx/5xx, unsupported format, WebView/Cloudflare-required
  source.

### 6. Desktop episode downloads

Desktop already has downloads: `animeworld/shared/src/jvmMain/.../downloads/AnimeDownloadManager.kt`,
wired to `GenericAnimeDesktop.downloadChapter`, does a plain Ktor GET of the first link. With
extension sources that fails or saves junk. v1 makes it work (user decision):

- The download uses the same video the user would play: the chosen/resolved `KmpStorage`
  (same pre-play choice flow as playback), not blindly the first link.
- All requests send the full `playerHeaders(...)` set, through the same header-aware HTTP code the
  proxy uses (share it; do not duplicate the header logic).
- Progressive files (MP4/MKV) download as-is with progress.
- HLS: pick a variant (highest, or the user's choice if the choice flow offers it), fetch every
  segment with headers, handle AES-128 keys, and write a single file the desktop player plays.
  How (concatenate MPEG-TS, remux via libVLC stream output, etc.) is the builder's call; no
  external tool may be required.
- External subtitles the extension returned are saved next to the video and offered when the
  downloaded file is played.
- Progress, failure and retry show in the existing download queue UI (`DownloadQueueSource.jvm.kt`).
- Downloaded files play directly in the player without the proxy.

## Constraints

- Kotlin Multiplatform; follow existing source-set and convention-plugin patterns (CLAUDE.md).
- Android build and Aniyomi/CloudStream behavior unchanged; existing `aniyomi`, `cloudstream`,
  `mihonsupport` and `kmpextensionloader` tests keep passing.
- MangaWorld desktop Mihon support unchanged.
- macOS is the only OS the user can test; Windows must be built-for but is verified later.
- Same supported Aniyomi lib versions as Android.

## Suggested order

1. Player spike (throwaway): vlcj + bundled libVLC plays one HLS stream that needs a custom header
   through the proxy, inside a Compose window on macOS, with no system VLC. Decide vlcj vs. libmpv.
2. Aniyomi runtime + loader on JVM; AnimeWorld desktop shows and browses Aniyomi sources.
3. Desktop player for real (UI, proxy, subtitles, quality switch).
4. Desktop downloads on the shared header/HLS code.
5. CloudStream JVM target + loader.

Each step is shippable on its own branch history; the builder may reorder 2 and 3.

## Decisions log

User's decisions:

- Episodes are watched in the app on desktop; a player is in scope.
- Video must play with nothing else installed; use a VLC or mpv library if one fits.
- Support all Aniyomi and CloudStream extensions; WebView/Cloudflare-dependent ones and source
  settings come in a later phase.
- Subtitles required. No cast, volume or brightness controls on desktop.
- v1 verified through `gradlew run`; installers later.
- Desktop episode downloads must work for extension sources in v1 (headers + HLS).
- Work branches off `feat/mangaworld-desktop-mihon-extensions`.

My calls (flagged for review):

- vlcj + bundled libVLC + localhost header proxy; libmpv as fallback; decided by a spike first.
- Spike pass bar (1080p HLS with custom header, Compose overlay + subtitle on top, no stutter,
  sane CPU) and a hard stop for the user's go-ahead after the spike.
- Video choice at parity: pre-play `VideoChoiceScreen` on desktop, in-player HLS variant choice
  only.
- vlcj's GPL-3.0 accepted given the app already ships GPL-3.0 CloudStream code.
- `requiresResources` CloudStream plugins may be unsupported in v1.
- No resume-from-position (Android doesn't have it).
- Skip ±30/+90 buttons kept for parity; plain volume slider allowed.
- Install path generalized from the Mihon JVM install rather than copied per ecosystem.
- Trust identical to Android per ecosystem.

## Left to the builder

- Source-set layout for shared Android+JVM code in `aniyomi` and `cloudstream`.
- How libVLC binaries are fetched and placed for `gradlew run`.
- vlcj rendering approach (embedded surface vs. callback rendering).
- How subtitle styling is applied.
- Exact extra `android.*` stubs needed (driven by converting and loading real extensions).
- How install links are routed to the right ecosystem on JVM.

## Verification

- Unit tests: JVM Aniyomi loader (manifest parse, lib-version rejection, trust with a signed test
  APK), CloudStream JVM loader (`.cs3` → jar, manifest, capture/remove of registered APIs), proxy
  (header injection, HLS playlist rewriting, binds to loopback only, rejects wrong token),
  subtitle track selection on JVM, HLS download (segment fetch with headers, AES-128, single
  output file).
- Manual on macOS via `./gradlew :animeworld:desktop:run` with no VLC installed:
  - Add the standard Aniyomi repo; install two non-Cloudflare extensions (one HTML/Jsoup, one JSON
    API); browse → details → episodes → play; switch quality; turn on an external subtitle and an
    embedded one; update one; uninstall one; confirm an untrusted APK shows the trust prompt.
  - Add a CloudStream repo; trust it; install two plugins; browse → play with subtitles;
    uninstall one.
  - Download one progressive and one HLS episode from extension sources; both play offline with
    subtitles.
  - A Cloudflare-protected source shows a clear error, not a crash.
- Android: `./gradlew :animeworld:assembleNoFirebaseDebug` and existing module tests pass.
- MangaWorld desktop: `./gradlew :mangaworld:desktop:run` still loads a Mihon extension.
