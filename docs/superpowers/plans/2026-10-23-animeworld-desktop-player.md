# AnimeWorld Desktop — Video Player and Downloads Implementation Plan (Plan 2 of 3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AnimeWorld Desktop plays extension episodes and downloaded files in-app (no software installed on the machine), with subtitles at parity with Android, and downloads extension episodes (progressive + HLS) to playable files with sidecar subtitles.

**Architecture:** vlcj 4.12.1 over a libVLC 3.0.24 that the build downloads and ships as Compose Desktop app resources. Video is rendered through vlcj's `CallbackVideoSurface` into a Skia bitmap drawn by Compose, so Compose controls and subtitles draw on top. Every network stream goes through a loopback `LocalStreamProxy` (NanoHTTPD + OkHttp) that injects the source's headers and rewrites HLS/DASH manifests. External subtitles are rendered by Compose from parsed cues; embedded tracks are rendered by libVLC into the frame. Downloads reuse the proxy's header logic and the existing commonMain HLS helpers.

**Tech Stack:** Kotlin Multiplatform (jvmMain), Compose Desktop, vlcj 4.12.1, libVLC 3.0.24, NanoHTTPD 2.3.1, OkHttp 5.4.0 (+ mockwebserver3 for tests), Koin, kotlin.test.

**Spec:** `docs/superpowers/specs/2026-10-23-animeworld-desktop-extensions-design.md` (sections 5 and 6). **Spike (approved by user):** `docs/superpowers/spikes/2026-10-23-desktop-player/` — `SPIKE-REPORT.md`, `Main.kt.txt` (vlcj discovery + callback surface + Compose rendering), `Proxy.kt.txt` (header proxy, HLS/DASH rewrite, Range rule), `build.gradle.kts.txt`. Read the report before Tasks 1–3.

## Global Constraints

- Plays with **nothing installed on the machine**: libVLC is loaded only from the app's bundled directory, never from a system VLC; log the directory it loaded from.
- macOS arm64 is the verified target via `./gradlew :animeworld:desktop:run`. Build logic must also cover macOS x64 and Windows x64 (unverified). Installers (DMG/MSI) are out of scope; don't block them.
- libVLC 3.0.24 (official VideoLAN builds), vlcj 4.12.1. Add `vlcj` to `gradle/desktop.versions.toml`; use the existing `androidLibs.nanohttpd`, `androidLibs.okhttpLib`, `androidLibs.okhttpMockWebServer` catalog entries (they are plain JVM artifacts).
- Proxy binds `127.0.0.1` only, random port, unguessable per-session path token; never forwards `Range` for manifests (always 200 with the rewritten body); passes `Range`/`Content-Range`/206 through for segments and progressive files.
- Request headers are always `playerHeaders(storage.headers, referer)` (commonMain `PlayerRequest.kt`); subtitle downloads use `playerHeaders(subtitle.headers, referer)`.
- Adaptive streams are capped at 1080p (`--adaptive-maxheight=1080`); decoded frames are cropped to the display size (VLC reports 1920x1088 for 1080p).
- Desktop player controls: play/pause, seek bar with buffered range, current/total time, −30 s / +30 s / +1:30 buttons (Android values), fullscreen toggle, volume slider, subtitle (CC) button opening the existing `SubtitleQuickSheet`. Keyboard: Space play/pause, ←/→ seek −/+10 s, F fullscreen, Esc exit fullscreen. **No** cast, brightness or volume gestures.
- No in-player quality picker (Android has none; the extension's video is chosen before playback in `VideoChoiceScreen`). Ruling recorded in the spec's spirit: the spec's "in-player HLS variant choice" was based on a wrong assumption about Android; parity wins.
- Subtitles: external `KmpSubtitle` tracks (VTT/SRT/ASS via existing `toWebVtt`/`parseVttCues`), embedded tracks via libVLC SPU, default track via existing `defaultSubtitleIndex`, styling from `SubtitleSettingsRepository`, honoring `autoEnable`. ASS styling may be lost (converted to plain cues).
- Desktop subtitle settings persist across restarts (replace `InMemorySubtitleSettingsRepository` on desktop).
- Native player resources are released when the player screen leaves composition; closing the window or navigating away mid-playback must not crash the JVM.
- Android behavior unchanged except Task 8's loopback edit to the vendored Aniyomi `HttpServer` (applies to both platforms; Android extensions only ever connect from the same device). Android builds and existing tests keep passing.
- Clear on-screen errors (not crashes) for: HTTP 4xx/5xx, unsupported format, Cloudflare/WebView-required source, libVLC failing to load.

## Review Focus

- VLC sends `Range: bytes=0-` on playlists; the proxy must answer 200 with the full rewritten manifest. → Task 2 test `manifestRangeRequestGetsFullRewrittenBody`.
- HLS keys and segments with relative URIs and `URI="..."` attributes (EXT-X-KEY, EXT-X-MAP, EXT-X-MEDIA) must all route back through the proxy with headers. → Task 2 test `rewritesKeyMapAndMediaUris`.
- Leaving the player (back, episode switch, window close) while a stream is buffering must release libVLC and the proxy session without a native crash. → Task 3 test `releaseWhilePlayingDoesNotCrash`.
- Downloaded file paths with spaces/Unicode play (the player gets a filesystem path, not a URI). → Task 3 test `playsLocalFileWithSpacesAndUnicode`.
- An HLS download whose segments are AES-128 encrypted with an explicit IV and a relative key URI produces a playable file. → Task 6 test `hlsAes128DownloadDecryptsSegments`.

---

### Task 1: Bundle libVLC and load it

**Files:**
- Create: `animeworld/desktop/vlc.gradle.kts` (applied from `animeworld/desktop/build.gradle.kts`) — task `prepareVlc` downloads the official VLC 3.0.24 build for the current OS/arch into `build/vlc-download/` (macOS arm64: `https://get.videolan.org/vlc/3.0.24/macosx/vlc-3.0.24-arm64.dmg`; macOS x64: `…-intel64.dmg`; Windows x64: `https://get.videolan.org/vlc/3.0.24/win64/vlc-3.0.24-win64.zip`; verify URLs, pin SHA-256 checksums in the script), extracts only `lib/` + `plugins/` (macOS: `hdiutil attach -nobrowse -readonly`, copy `VLC.app/Contents/MacOS/{lib,plugins}`, detach; Windows: `libvlc.dll`, `libvlccore.dll`, `plugins/`) into `build/appResources/<os>-<arch>/vlc/`, strips `com.apple.quarantine`, prunes plugin dirs not needed for network/local playback (start from the spike report's list: bluray, dvd, skins, lua, GUI, upnp…; keep codec, demux, access(http/file), stream_filter, packetizer, video_chroma, video_output(vmem), audio_output, audio_filter, spu, text_renderer, misc), and is cached (skips when output exists and checksum matches).
- Modify: `animeworld/desktop/build.gradle.kts` — `compose.desktop.application.nativeDistributions.appResourcesRootDir.set(layout.buildDirectory.dir("appResources"))`; make `prepareAppResources` (and `run`) depend on `prepareVlc`.
- Modify: `gradle/desktop.versions.toml` (vlcj 4.12.1), `animeworld/shared/build.gradle.kts` jvmMain deps (+vlcj, nanohttpd, okhttp; jvmTest + kotlin.test, mockwebserver3).
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videoplayer/VlcRuntime.kt`
- Test: `animeworld/desktop/src/jvmTest/kotlin/com/programmersbox/desktop/VlcRuntimeTest.kt` (desktop module, because it owns the natives; its `jvmTest` task depends on `prepareVlc` and passes the natives dir as system property `compose.application.resources.dir`).

**Interfaces:**
- Produces:
  ```kotlin
  object VlcRuntime {
      /** Loads libVLC from <resourcesDir>/vlc only (resourcesDir = System.getProperty("compose.application.resources.dir")). Idempotent. */
      fun factory(): Result<MediaPlayerFactory>   // failure → message shown by the player
      val loadedFrom: String?                      // absolute lib dir actually used
      val version: String?                         // libVLC version string
  }
  ```
  Factory options always include `--adaptive-maxheight=1080`, `--no-video-title-show`, `--quiet`. Discovery and `VLC_PLUGIN_PATH` set natively before `libvlc_new`, `libvlccore` preloaded by absolute path (see spike `Main.kt.txt`). Generate the plugin cache into a writable app-cache dir on first run if libVLC supports it (`--reset-plugins-cache`), else rely on pruning.

- [ ] **Step 1: Failing test** — `VlcRuntimeTest.loadsBundledLibVlc`: `VlcRuntime.factory().isSuccess`, `VlcRuntime.version!!.startsWith("3.0.24")`, `VlcRuntime.loadedFrom!!.startsWith(resourcesDir)`, and `!VlcRuntime.loadedFrom!!.startsWith("/Applications")`.
- [ ] **Step 2: Run** `./gradlew :animeworld:desktop:jvmTest --tests '*VlcRuntimeTest*'` → fails (no `VlcRuntime`).
- [ ] **Step 3: Implement** the Gradle script, catalog/deps, and `VlcRuntime`.
- [ ] **Step 4: Run** the same command → PASS. Also `./gradlew :animeworld:desktop:compileKotlinJvm`. Report the pruned bundle size.
- [ ] **Step 5: Commit** — `build(animeworld-desktop): bundle libVLC 3.0.24 as app resources and load it via vlcj`

---

### Task 2: Loopback stream proxy

**Files:**
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videoplayer/LocalStreamProxy.kt`
- Test: `animeworld/shared/src/jvmTest/kotlin/com/programmersbox/anime/shared/videoplayer/LocalStreamProxyTest.kt`

Port the proxy half of androidMain `cast/CastMediaServer.kt` (routes, `parseRange`, `upstreamStatus`, streaming with Range) and the spike's `Proxy.kt.txt` (DASH path-based base URL, manifest Range rule). Reuse commonMain `rewriteHlsPlaylist` (cast/HlsRewrite.kt). No Android imports.

**Interfaces:**
- Produces:
  ```kotlin
  class LocalStreamProxy(client: OkHttpClient = OkHttpClient()) : AutoCloseable {
      fun start()                                               // binds 127.0.0.1, random port
      /** URL libVLC should open for [upstreamUrl], fetched upstream with [headers] on every hop. */
      fun register(upstreamUrl: String, headers: Map<String, String>, mimeType: String?): String
      override fun close()                                      // stops server; invalidates all tokens
  }
  ```
  HLS detection: mimeType `application/x-mpegURL`/`application/vnd.apple.mpegurl`, response Content-Type, or `.m3u8` path. DASH: `application/dash+xml` or `.mpd` — serve the manifest under a path that preserves the upstream directory so relative `BaseURL`/`SegmentTemplate` resolve back through the proxy; rewrite absolute `BaseURL`s.

- [ ] **Step 1: Failing tests** (MockWebServer upstream that records request headers):
  - `injectsHeadersOnEveryHop` — master → variant → segment → key all carry `X-Test: 1` and `Referer`.
  - `rewritesKeyMapAndMediaUris` — playlist with relative/absolute segment URIs, `#EXT-X-KEY:...URI="k.key"`, `#EXT-X-MAP:URI="init.mp4"`, `#EXT-X-MEDIA:...URI="sub.m3u8"`: every URI in the returned text starts with the proxy base.
  - `manifestRangeRequestGetsFullRewrittenBody` — request with `Range: bytes=0-` → 200, full rewritten body, upstream did not receive `Range`.
  - `segmentRangePassesThrough` — `Range: bytes=10-19` on a segment → 206, `Content-Range` forwarded.
  - `dashRelativeSegmentsResolveThroughProxy` — `.mpd` with `SegmentTemplate media="seg-$Number$.m4s"`; fetching proxied manifest URL + `seg-1.m4s` hits upstream `/dir/seg-1.m4s` with headers.
  - `rejectsWrongToken` (404), `bindsLoopbackOnly` (server socket address is 127.0.0.1), `upstreamErrorStatusPropagates` (upstream 403 → 403).
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests '*LocalStreamProxyTest*'` → fail.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** — `feat(anime-desktop): loopback header proxy for HLS, DASH and progressive streams`

---

### Task 3: Player engine and screen

**Files:**
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videoplayer/DesktopPlayer.kt` (vlcj wrapper), `DesktopPlayerScreen.kt` (Compose UI)
- Modify: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerUi.jvm.kt` (replace stub)
- Test: `animeworld/desktop/src/jvmTest/kotlin/com/programmersbox/desktop/DesktopPlayerTest.kt` + fixture `animeworld/desktop/src/jvmTest/resources/fixtures/tiny clip ü.mp4` (generate once with `ffmpeg -f lavfi -i testsrc=size=320x180:rate=25 -f lavfi -i sine -t 3 -c:v libx264 -c:a aac -shortest`, plus a `mov_text` subtitle stream for Task 4; keep < 100 KB)

**Interfaces:**
- Consumes: `VlcRuntime.factory()`, `LocalStreamProxy`.
- Produces:
  ```kotlin
  sealed interface PlayerSource {
      data class Stream(val url: String, val headers: Map<String, String>, val mimeType: String?) : PlayerSource
      data class LocalFile(val path: String) : PlayerSource
  }
  class DesktopPlayer(factory: MediaPlayerFactory, proxy: LocalStreamProxy) : AutoCloseable {
      val frame: StateFlow<ImageBitmap?>          // cropped to display size
      val state: StateFlow<PlayerState>           // data class PlayerState(positionMs, durationMs, bufferedMs, playing, buffering, error: String?)
      val spuTracks: StateFlow<List<SpuTrack>>    // embedded subtitle tracks (id, label via embeddedTrackLabel, language)
      fun open(source: PlayerSource, startMs: Long = 0)
      fun playPause(); fun seekTo(ms: Long); fun seekBy(deltaMs: Long); fun setVolume(percent: Int)
      fun selectSpu(id: Int?)                     // null disables embedded subs
      override fun close()                        // stop + release native player; safe to call twice and from any thread
  }
  ```
- `VideoPlayerUi(videoScreen)`: `downloadOrStream=true` → `PlayerSource.LocalFile(videoScreen.showPath)`; else `Stream(storage.link ?: showPath, playerHeaders(storage.headers, videoScreen.referer), streamMime(storage))` with `storage = koinInject<StorageHolder>().storageModel`. `DisposableEffect` closes the player. Controls per Global Constraints; controls auto-hide after 3 s of no mouse movement while playing. Fullscreen toggles the Compose window placement (`WindowPlacement.Fullscreen`) — find the window via `LocalWindow`/the existing desktop window state; builder's call. On `VlcRuntime` failure or `state.error` show an error panel with a Back button.

- [ ] **Step 1: Failing tests** (real libVLC, fixture file):
  - `playsLocalFileWithSpacesAndUnicode` — open `LocalFile(fixture path)`, within 5 s `durationMs in 2900..3100`, a non-null `frame` with width 320, `positionMs` advances.
  - `seekByMovesPosition`, `playPauseToggles`.
  - `releaseWhilePlayingDoesNotCrash` — open, wait for first frame, `close()` from a non-UI thread while playing, then `close()` again; JVM still alive; a new `DesktopPlayer` can open the file afterwards.
  - `streamGoesThroughProxy` — `Stream(mockWebServerUrl/clip.mp4, mapOf("X-Test" to "1"), "video/mp4")`; MockWebServer saw `X-Test: 1`; frame appears.
- [ ] **Step 2: Run** `./gradlew :animeworld:desktop:jvmTest --tests '*DesktopPlayerTest*'` → fail.
- [ ] **Step 3: Implement** engine (callback surface → `ImageBitmap` per spike, crop to `videoDimension`), screen, keyboard handling, `VideoPlayerUi` actual.
- [ ] **Step 4: Run** → PASS; `./gradlew :animeworld:desktop:compileKotlinJvm`.
- [ ] **Step 5: Commit** — `feat(anime-desktop): in-app video player on vlcj with Compose controls`

---

### Task 4: Subtitles on desktop

**Files:**
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videoplayer/DesktopSubtitles.kt` (track list + selection + cue fetching), `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/subtitles/JvmSubtitleSettingsRepository.kt`
- Modify: `DesktopPlayerScreen.kt` (CC button, `SubtitleQuickSheet`, overlay), `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt` (bind `JvmSubtitleSettingsRepository` instead of `InMemorySubtitleSettingsRepository`)
- Test: `animeworld/shared/src/jvmTest/kotlin/com/programmersbox/anime/shared/videoplayer/DesktopSubtitlesTest.kt`, `.../subtitles/JvmSubtitleSettingsRepositoryTest.kt`; extend `DesktopPlayerTest` with `embeddedSubtitleTrackListed`.

**Interfaces:**
- Produces:
  ```kotlin
  sealed interface SubtitleTrack { val label: String
      data class External(val subtitle: KmpSubtitle, override val label: String) : SubtitleTrack
      data class Embedded(val spuId: Int, override val label: String) : SubtitleTrack }
  /** External first (labels via cleanSubtitleLabel), then embedded; default = defaultSubtitleIndex over externals, else first embedded matching preferred languages; null when !style.autoEnable. */
  fun buildSubtitleTracks(external: List<KmpSubtitle>, embedded: List<SpuTrack>, style: SubtitleStyle, deviceLanguage: String): Pair<List<SubtitleTrack>, Int?>
  class SubtitleCueLoader(client: OkHttpClient) { suspend fun load(sub: KmpSubtitle, referer: String?): List<VttCue> }  // playerHeaders(sub.headers, referer); SubtitleFormat.fromUrl + toWebVtt + parseVttCues; local files read from disk
  class JvmSubtitleSettingsRepository(file: File) : SubtitleSettingsRepository  // encodeSubtitleStyle/decodeSubtitleStyle, file under AppDirs user data dir
  ```
- External selected → disable SPU, draw `cues.cueAt(positionMs)` in Compose using `style.toCaptionSpec()` (size, colors, edge, background, `bottomPaddingFraction`). Embedded selected → `player.selectSpu(id)`, no overlay. Offline (`LocalFile`): external tracks come from the `*.subtitles.json` sidecar index next to the file (`sidecarIndexName`, `SidecarEntry`) — port the jvm-usable part of androidMain `downloads/OfflineSubtitles.kt` to jvmMain.

- [ ] **Step 1: Failing tests:** `prefersConfiguredLanguage` (`preferredLanguages = listOf("es")` picks the Spanish external), `skipsSignsAndSongs`, `autoEnableOffMeansNoDefault`, `embeddedUsedWhenNoExternalMatches`, `cueLoaderSendsHeadersAndConvertsSrt` (MockWebServer SRT with required header → cues), `cueLoaderReadsLocalSidecar`, `settingsPersistAcrossInstances`, `embeddedSubtitleTrackListed` (fixture's mov_text track appears in `spuTracks`).
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests '*DesktopSubtitlesTest*' --tests '*JvmSubtitleSettingsRepositoryTest*' :animeworld:desktop:jvmTest --tests '*DesktopPlayerTest*'` → fail.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** — `feat(anime-desktop): subtitles in the desktop player with persisted style`

---

### Task 5: Video choice and resolve on desktop

**Files:**
- Modify: `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericAnimeDesktop.kt`
- Test: `animeworld/desktop/src/jvmTest/kotlin/com/programmersbox/desktop/GenericAnimeDesktopTest.kt`

**Interfaces:**
- Desktop overrides `@Composable DialogSetups()` (the `KmpGenericInfo` hook Android uses at `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt:550-585`) to render `VideoChoiceScreen` from `VideoSourceModel.showVideoSources`, with `onPlay = { s -> if (it.isStreaming) playResolved(nav, s, it.model, it.infoModel) else downloadResolved(it.model, it.infoModel, s) }`.
- `playOrCast` also sets `storageHolder.showTitle = infoModel.title` and `posterUrl = infoModel.imageUrl` (Android parity).
- `onResolveFailed(storage, error)` surfaces a visible error (snackbar/toast/tray notification — use what desktop already has).
- `downloadChapter(...)` now goes through `downloadOnClick(model, infoModel, onNone = { /* visible "no videos" error */ }, download = { downloadResolved(model, infoModel, it) })`; `downloadResolved` resolves via `model.source.resolve(storage)` then calls Task 6's `AnimeDownloadManager.download(...)`. Until Task 6 lands, `downloadResolved` calls the existing manager path.

- [ ] **Step 1: Failing tests** (fake `KmpApiService` + fake `NavigationActions`): `singleVideoPlaysImmediately` (navigates to `VideoScreen`, `StorageHolder.storageModel` is the resolved storage, `showTitle` set), `multipleVideosOpenChoice` (`VideoSourceModel.showVideoSources` non-null, `isStreaming = true`), `resolveFailureReported`, `downloadWithMultipleVideosOpensChoiceWithIsStreamingFalse`.
- [ ] **Step 2: Run** `./gradlew :animeworld:desktop:jvmTest --tests '*GenericAnimeDesktopTest*'` → fail.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** — `feat(anime-desktop): pick and resolve extension videos before playing or downloading`

---

### Task 6: Downloads for extension sources

**Files:**
- Modify: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/downloads/AnimeDownloadManager.kt` (rewrite), `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/downloads/DownloadQueueSource.jvm.kt` (real implementation), `animeworld/desktop/.../GenericAnimeDesktop.kt` (`downloadResolved` → new API), desktop `main.kt` DI if constructor changes
- Test: `animeworld/shared/src/jvmTest/kotlin/com/programmersbox/anime/shared/downloads/AnimeDownloadManagerTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  class AnimeDownloadManager(scope: CoroutineScope, animeDesktopSettings: AnimeDesktopSettings, trayState: TrayState?, client: OkHttpClient = OkHttpClient()) {
      val queue: StateFlow<List<QueuedDownload>>      // commonMain QueuedDownload; Running with progress/bytes, Waiting, Failed with error
      fun download(storage: KmpStorage, referer: String?, animeTitle: String, episodeName: String): String  // returns queue key
      fun cancel(key: String); fun retry(key: String); fun remove(key: String)
  }
  ```
  `DownloadQueueSource` (jvm actual) delegates `observe/cancel/retry/remove` to the manager. One download at a time, others Waiting (builder may allow 2).
- Behavior: headers `playerHeaders(storage.headers, referer)` on every request (share the header logic with Task 2 — e.g. a small `upstreamRequest(url, headers)` helper used by both). Output dir `downloadsDirectory/<sanitizeFileName(animeTitle)>/`, base name `uniqueBaseName(sanitizeFileName(episodeName))`.
  - Progressive (`video/mp4` or not HLS/DASH): stream to `<base>.<ext from URL or mp4>.part`, progress from Content-Length, rename on success.
  - HLS: `parseHlsPlaylist` → `Master.chooseVariant(storage.quality)` → media playlist; fetch segments sequentially with headers, decrypt AES-128 (`HlsKey`, `aesIv(segment)`, keys cached per URI), prepend `HlsMap` init segment when present; write `<base>.ts` (or `<base>.mp4` when the playlist uses an fMP4 `EXT-X-MAP`). Progress = segments done / total. `SAMPLE-AES`/unsupported → Failed with `UnsupportedHlsException` message.
  - DASH: Failed with "DASH downloads aren't supported yet" (v1; ruling: spec requires progressive + HLS only).
  - Subtitles: each `storage.subtitles` entry saved as `sidecarName(base, i, label, format)` converted with `toWebVtt` when needed, plus `sidecarIndexName(base)` JSON of `SidecarEntry` — the same layout Task 4 reads.
  - Cancel deletes the `.part`; failure keeps the error for retry; tray notification on finish/failure (as today).
  - `VideoLibrarySource.jvm.kt` already lists `mp4/mkv/avi/mov/webm`; add `ts`.

- [ ] **Step 1: Failing tests** (MockWebServer, temp downloads dir):
  - `progressiveDownloadSendsHeadersAndReportsProgress`
  - `hlsAes128DownloadDecryptsSegments` — 3 segments AES-128 encrypted with explicit IV, key at relative URI requiring a header; output bytes == concatenated plaintext.
  - `hlsFmp4WritesInitSegmentFirst`
  - `subtitleSidecarsWritten` (index JSON + VTT files, SRT converted)
  - `failureThenRetrySucceeds`, `cancelRemovesPartFile`, `dashReportsUnsupported`, `queueShowsWaitingWhileOneRuns`.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests '*AnimeDownloadManagerTest*'` → fail.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS; `./gradlew :animeworld:desktop:compileKotlinJvm`.
- [ ] **Step 5: Commit** — `feat(anime-desktop): download extension episodes (progressive and HLS) with subtitles`

---

### Task 7: Loopback-only Aniyomi HttpServer

**Files:**
- Modify: `source_utilities/aniyomi/src/jvmAndroidShared/kotlin/eu/kanade/tachiyomi/animesource/model/HttpServer.kt` (`NanoHTTPD(0)` → `NanoHTTPD("127.0.0.1", 0)`, marked `OTAKU:`), `source_utilities/aniyomi/VENDORED.md`
- Test: extend `source_utilities/aniyomi/src/jvmTest/kotlin/com/programmersbox/aniyomi/AniyomiRuntimeJvmTest.kt` with `httpServerBindsLoopback`.

- [ ] **Step 1:** failing test asserting the started server's hostname/bound address is loopback.
- [ ] **Step 2:** `./gradlew :source_utilities:aniyomi:jvmTest --tests '*AniyomiRuntimeJvmTest*'` → fail.
- [ ] **Step 3:** edit + VENDORED.md entry.
- [ ] **Step 4:** run → PASS; `ANDROID_HOME=… ./gradlew :source_utilities:aniyomi:testAndroidHostTest` → PASS.
- [ ] **Step 5: Commit** — `fix(aniyomi): bind extension local http servers to loopback`

---

### Task 8: Wire-up, regression, changelog

**Files:**
- Modify: `CHANGELOG.md` (AnimeWorld → Desktop: in-app player, subtitles, downloads), any leftover DI in `animeworld/desktop/.../main.kt`.

- [ ] **Step 1:** `./gradlew :animeworld:shared:jvmTest :animeworld:desktop:jvmTest :source_utilities:aniyomi:jvmTest :kmpuiviews:jvmTest :mangaworld:desktop:compileKotlinJvm` and `ANDROID_HOME=… ./gradlew :animeworld:assembleNoFirebaseDebug :animeworld:shared:testAndroidHostTest` → all PASS.
- [ ] **Step 2:** Headless smoke: `./gradlew :animeworld:desktop:run` in background, log shows `VlcRuntime` loaded from the app resources dir and no exceptions; stop it.
- [ ] **Step 3:** Opt-in live check: extend `AniyomiEndToEndNetworkTest` (env `ANIYOMI_E2E=1`) or add a desktop-module opt-in test that resolves the AnimeOnsen fixture's first episode and plays it through `DesktopPlayer` until a frame arrives (DASH through the proxy). Run once; report output.
- [ ] **Step 4: Commit** — `docs: changelog for AnimeWorld desktop player and downloads`

Manual checklist for the user (macOS, `./gradlew :animeworld:desktop:run`): play an Aniyomi episode (AnimeOnsen) with video choice; seek, skip buttons, keyboard, fullscreen, volume; turn on an external and an embedded subtitle and change the style; leave mid-playback; download an episode (HLS source if one is available) and play it offline with subtitles; cancel and retry a download.
