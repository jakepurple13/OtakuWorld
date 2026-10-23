# AnimeWorld Casting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cast streamed and downloaded AnimeWorld episodes to Chromecast / Google TV with every subtitle track, controlled from a custom Compose remote.

**Architecture:** Pure, testable logic (subtitle→VTT, HLS rewrite, compatibility, style mapping, track matching) lives in `animeworld/shared` `commonMain` under package `com.programmersbox.anime.shared.cast`. Android pieces (NanoHTTPD media server, `CastController` over `RemoteMediaClient`, foreground service, Compose UI) live in `animeworld/shared` `androidMain`, same package, so the shared player can call them directly. The app module keeps `CastOptions`, wiring, and the mini bar; the old `CastHelper` stack is deleted.

**Tech Stack:** Kotlin, Compose M3, Koin + Nav3 (`navigation<Key> {}`), `play-services-cast-framework` 22.3.1 (`RemoteMediaClient`), `androidx.mediarouter`, NanoHTTPD 2.3.1 (`androidLibs.nanohttpd`, already in catalog), OkHttp (already a shared dep), kotlin.test + MockWebServer.

**Spec:** `docs/superpowers/specs/2026-10-23-animeworld-casting-design.md`

## Global Constraints

- Default Media Receiver only (`CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID`). No custom receiver.
- No phone-side remux/transcode. MKV is blocked with the copy **"This episode can't be cast"**.
- No new heavy deps. Allowed new dep: `androidLibs.nanohttpd`. Remove `androidLibs.localCast`.
- Subtitles served as `text/vtt` with `Access-Control-Allow-Origin: *`. All proxied HLS responses also carry that header.
- Transport row order and amounts (ms): ⟲15 `-15_000`, ⟲10 `-10_000`, play/pause, +85 `85_000`, 15⟳ `15_000`.
- Mini bar only inside `MainActivity.BottomBarAdditions` (no `kmpuiviews` change).
- Do not touch MangaWorld / NovelWorld / shared episode list UI.
- Local builds: `noFirebase` flavor. Before installing on a phone, compare `favoritesdatabase` Room version with the main checkout.
- `CHANGELOG.md` entry under AnimeWorld.

## Review Focus

1. **Phone Wi-Fi IP changes / no Wi-Fi (mobile data only):** server base URL must come from the current Wi-Fi link address; with no Wi-Fi, casting shows "Connect to the same Wi-Fi as your TV" instead of loading. Test in Task 4.
2. **Subtitle URL fails to download (404, timeout):** that track is omitted from the TV track list; casting still starts. Test in Task 4.
3. **Session ends from the TV side / other phone** (user presses stop on the TV remote): `CastUiState.connection` returns to `Disconnected`, server stops, mini bar disappears. Test in Task 5 via `CastStateReducer`.
4. **HLS master playlist with relative, absolute and `URI="..."` attribute references** (keys, `EXT-X-MEDIA` subtitles, `EXT-X-MAP`): every reference rewritten through the proxy. Test in Task 2.
5. **SRT with comma timestamps, BOM, CRLF, and ASS with `\N`, override tags `{\i1}`, and `Comment:` lines:** VTT output is clean text, correct timing. Test in Task 1.

---

### Task 1: Subtitle conversion + cue parsing (commonMain)

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/cast/SubtitleVtt.kt`
- Test: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/cast/SubtitleVttTest.kt`

**Interfaces:**
- Produces:
  - `enum class SubtitleFormat { Vtt, Srt, Ass; companion object { fun fromUrl(url: String): SubtitleFormat } }` (map via existing `subtitleMime(url)`).
  - `fun toWebVtt(text: String, format: SubtitleFormat): String`
  - `data class VttCue(val startMs: Long, val endMs: Long, val text: String)`
  - `fun parseVttCues(vtt: String): List<VttCue>`
  - `fun List<VttCue>.cueAt(positionMs: Long): String?`

- [ ] **Step 1: Write failing tests** — `srtToVtt_convertsCommaTimestamps` (`00:00:01,500 --> 00:00:03,000` → `00:00:01.500 --> 00:00:03.000`, output starts with `WEBVTT`), `srtToVtt_stripsBomAndCrlf`, `assToVtt_usesDialogueTextOnly` (drops `Comment:` lines, `\N`→newline, strips `{\i1}`/`{\pos(..)}`, ASS `0:00:01.50` → `00:00:01.500`), `vttPassthrough_unchanged`, `parseVttCues_readsTimesAndText`, `cueAt_returnsNullBetweenCues`.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*SubtitleVttTest*"` — expect FAIL (unresolved references).
- [ ] **Step 3: Implement** the functions above. ASS: read `[Events]` `Format:` line to find the `Start`, `End`, `Text` column indexes; `Text` is everything after the last needed comma.
- [ ] **Step 4: Run** same command — expect PASS.
- [ ] **Step 5: Commit** `feat(cast): convert SRT/ASS subtitles to WebVTT`

### Task 2: HLS playlist rewrite for the proxy (commonMain)

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/cast/HlsRewrite.kt`
- Test: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/cast/HlsRewriteTest.kt`

**Interfaces:**
- Consumes: `resolveUrl(base, ref)` and `parseAttributes(list)` from `downloads/HlsPlaylist.kt` (internal, same module).
- Produces: `fun rewriteHlsPlaylist(text: String, baseUrl: String, proxy: (absoluteUrl: String) -> String): String` — rewrites every non-tag URI line and every `URI="..."` attribute (`EXT-X-KEY`, `EXT-X-MAP`, `EXT-X-MEDIA`, `EXT-X-I-FRAME-STREAM-INF`) to `proxy(resolveUrl(baseUrl, ref))`; all other lines byte-identical.

- [ ] **Step 1: Failing tests** — `rewritesRelativeSegment`, `rewritesAbsoluteVariant`, `rewritesKeyAndMapUriAttributes`, `rewritesMediaSubtitleRendition`, `leavesTagsAndCommentsUntouched`, `keepsQueryStringOnResolvedUrl`. Use `proxy = { "http://p/$it" }`.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*HlsRewriteTest*"` — FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** `feat(cast): rewrite HLS playlists through local proxy`

### Task 3: Cast decisions — compatibility, content type, track match, style (commonMain)

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/cast/CastRules.kt`
- Test: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/cast/CastRulesTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface CastCompat { data object Ok : CastCompat; data object Blocked : CastCompat }`
  - `fun castCompatibility(url: String, mimeType: String?): CastCompat` — Blocked when mime is `video/x-matroska`/`video/mkv` or path extension is `mkv`.
  - `fun castContentType(url: String, mimeType: String?): String` — `mimeType` if set; else `.m3u8`→`application/x-mpegURL`, `.mpd`→`application/dash+xml`, `.webm`→`video/webm`, else `video/mp4`.
  - `fun needsProxy(url: String, mimeType: String?, headers: Map<String, String>): Boolean` — true for HLS always, otherwise `headers.isNotEmpty()`.
  - `fun closestSubtitleIndex(language: String?, label: String?, subtitles: List<KmpSubtitle>): Int?` — match by ISO code prefix (`en` vs `en-US`), then by display name contained in `lang` (case-insensitive), preferring non signs/songs tracks; null if none.
  - `enum class CastEdge { None, Outline, DropShadow, Raised }`, `enum class CastFont { SansSerif, Serif, Monospace }`
  - `data class CastTextStyle(val fontScale: Float, val foregroundArgb: Long, val backgroundArgb: Long, val edge: CastEdge, val edgeArgb: Long, val font: CastFont)`
  - `fun SubtitleStyle.toCastTextStyle(): CastTextStyle` — `fontScale = sizePercent / 100f`; background black with alpha `backgroundPercent`; `Default`/`Bold`→SansSerif, `Serif`→Serif, `Mono`→Monospace; edge maps 1:1 (`Shadow`→DropShadow); edge color black.
  - `object CastJumps { const val BACK_15 = -15_000L; const val BACK_10 = -10_000L; const val OPENING = 85_000L; const val FORWARD_15 = 15_000L }`

- [ ] **Step 1: Failing tests** — `mkvByExtensionBlocked`, `mkvByMimeBlocked`, `mp4Ok`, `contentTypeFromMimeWins`, `contentTypeFromM3u8`, `hlsAlwaysProxied`, `mp4WithoutHeadersNotProxied`, `mp4WithRefererProxied`, `closestMatchesIsoPrefix` (`"en"` vs `["Español","English (Signs & Songs)","English"]` → 2), `closestNullWhenNoLanguage`, `styleMapsSizeAndBackground` (`sizePercent=150, backgroundPercent=50` → `fontScale 1.5f`, `backgroundArgb 0x80000000`), `styleMapsFonts`.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*CastRulesTest*"` — FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** `feat(cast): add cast compatibility, track matching and style rules`

### Task 4: Local media server (androidMain)

**Files:**
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/cast/CastMediaServer.kt`
- Modify: `animeworld/shared/build.gradle.kts` (androidMain: add `androidLibs.nanohttpd`, `androidLibs.castFramework`, `androidLibs.mediarouter` or whatever alias the app uses for `androidx.mediarouter`)
- Test: `animeworld/shared/src/androidHostTest/kotlin/com/programmersbox/anime/shared/cast/CastMediaServerTest.kt`

**Interfaces:**
- Consumes: `toWebVtt`, `SubtitleFormat.fromUrl`, `rewriteHlsPlaylist`, `parseVttCues` (Tasks 1–2).
- Produces: `class CastMediaServer(private val okHttp: OkHttpClient, private val hostProvider: () -> String?, port: Int = 8081)` with
  - `fun start(): Boolean` (false when `hostProvider()` is null → no Wi-Fi), `fun stop()`, `val baseUrl: String?`
  - `suspend fun registerSubtitle(sub: KmpSubtitle): RegisteredSubtitle?` — fetches with `sub.headers`, converts, caches; null on failure. `data class RegisteredSubtitle(val id: Int, val url: String, val cues: List<VttCue>)`
  - `fun registerStream(url: String, headers: Map<String, String>): String` — returns proxy URL; HLS bodies rewritten so children inherit the same headers.
  - `fun registerFile(file: File): String`
  - Routes: `/sub/{id}.vtt`, `/p/{token}` (token → `(url, headers)` map kept in memory; headers never in the URL), `/f/{id}`. Every response adds `Access-Control-Allow-Origin: *`; `/f` and progressive `/p` honor `Range`.
  - Host helper in same file: `fun wifiIpv4(context: Context): String?` — IPv4 of the active network when it has `TRANSPORT_WIFI` or `TRANSPORT_ETHERNET` (replaces `Utils.findIPAddress`, which picked `linkAddresses[1]` blindly).

- [ ] **Step 1: Failing tests** (MockWebServer upstream, `hostProvider = { "127.0.0.1" }`, random port): `subtitleServedAsVttWithCors`, `subtitleFetchUsesSubtitleHeaders`, `failedSubtitleReturnsNull` (Review Focus 2), `proxyForwardsHeadersUpstream`, `proxyRewritesHlsChildrenThroughProxy`, `fileServedWithRange` (206 + `Content-Range`), `startFailsWithoutWifiHost` (Review Focus 1).
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:testAndroidHostTest --tests "*CastMediaServerTest*"` — FAIL.
- [ ] **Step 3: Implement** on `fi.iki.elonen.NanoHTTPD`; upstream calls via `okHttp`.
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** `feat(cast): local server for subtitles, proxied streams and files`

### Task 5: CastController + foreground service (androidMain)

**Files:**
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/cast/CastController.kt`
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/cast/CastSessionService.kt`
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/cast/CastState.kt`
- Test: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/cast/CastStateTest.kt`
- Modify: the shared Koin module that registers `VideoViewModel` (add `single { CastController(...) }`, `single { CastMediaServer(...) }`)

**Interfaces:**
- Consumes: Tasks 1–4.
- Produces (commonMain `CastState.kt`):
  - `enum class CastConnection { Unavailable, NoDevices, Disconnected, Connecting, Connected }`
  - `data class CastTrack(val id: Long, val label: String, val language: String?, val origin: String)` (`origin` = `"source"`, `"downloaded"`, or `"stream"` for in-manifest)
  - `data class CastUiState(val connection: CastConnection = CastConnection.Unavailable, val deviceName: String? = null, val showTitle: String? = null, val episodeName: String? = null, val posterUrl: String? = null, val positionMs: Long = 0, val durationMs: Long = 0, val playing: Boolean = false, val tracks: List<CastTrack> = emptyList(), val activeTrackId: Long? = null, val volume: Float = 0f, val currentCue: String? = null)`
  - `sealed interface CastEvent { data class Connected(val deviceName: String); data object Disconnected; data class Status(...fields from MediaStatus...) }` and `fun reduce(state: CastUiState, event: CastEvent): CastUiState` — `Disconnected` clears media fields and sets `connection = Disconnected`.
- Produces (androidMain):
  - `data class CastRequest(val showTitle: String, val episodeName: String, val posterUrl: String?, val url: String, val mimeType: String?, val headers: Map<String, String>, val subtitles: List<KmpSubtitle>, val selectedSubtitle: Int?, val startPositionMs: Long, val localFile: File? = null, val subtitleOrigin: String = "source")`
  - `class CastController(context: Context, server: CastMediaServer, subtitleSettings: SubtitleSettingsRepository, scope: CoroutineScope)`:
    `val state: StateFlow<CastUiState>`, `suspend fun cast(request: CastRequest): CastResult`, `fun togglePlay()`, `fun seekBy(deltaMs: Long)`, `fun seekTo(ms: Long)`, `fun selectTrack(id: Long?)` (null = Off), `fun setVolume(v: Float)`, `fun stop()`.
    `sealed interface CastResult { data object Started; data object Unsupported; data object NoWifi; data class Failed(val message: String) }`
  - Behavior: `CastContext.getSharedInstance` wrapped so missing GMS → `Unavailable` (no crash, no empty catch). `cast()` checks `castCompatibility` (Unsupported), starts server (NoWifi), registers subtitles in parallel (drops nulls), builds `MediaInfo` (content type from `castContentType`, `MediaTrack` per subtitle with `SUBTYPE_SUBTITLES`, `setContentType("text/vtt")`), `MediaLoadRequestData` with `setCurrentTime(startPositionMs)` and `setActiveTrackIds(selected)`, then `setTextTrackStyle` from `toCastTextStyle()`. Collects `subtitleSettings.style` while connected and re-applies style. `currentCue` from registered cues of the active track at `positionMs` (poll `RemoteMediaClient.ProgressListener`, 500 ms). In-manifest HLS text tracks from `MediaStatus.mediaInfo.mediaTracks` are appended as `origin = "stream"`. Receiver load failure → `CastResult.Failed`. Session end → server stop + service stop.
  - `CastSessionService : Service` foreground type `mediaPlayback`, started on `Started`, stopped on disconnect; ongoing notification text "Casting to <device>". Declare in `animeworld/src/main/AndroidManifest.xml` with `android:foregroundServiceType="mediaPlayback"` and add `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission.

- [ ] **Step 1: Failing tests** for the reducer: `connectedSetsDeviceName`, `statusUpdatesPositionAndTracks`, `disconnectClearsMedia` (Review Focus 3).
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*CastStateTest*"` — FAIL.
- [ ] **Step 3: Implement** `CastState.kt`, then `CastController` and `CastSessionService`.
- [ ] **Step 4: Run** test — PASS; `./gradlew :animeworld:assembleNoFirebaseDebug` — BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(cast): CastController over RemoteMediaClient with foreground service`

### Task 6: Cast button + device picker sheet (androidMain UI)

**Files:**
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/cast/CastDevicePicker.kt`

**Interfaces:**
- Consumes: `CastController.state`.
- Produces: `@Composable fun CastButton(onClick: () -> Unit, modifier: Modifier = Modifier)` — hidden unless `connection` ∉ {`Unavailable`, `NoDevices`}; `CastConnected` icon when `Connected`. `@Composable fun CastDevicePickerSheet(onDismiss: () -> Unit, onConnected: () -> Unit)` — lists routes from `MediaRouter.getInstance(ctx)` filtered by `CastContext.mergedSelector`, with active-scan callback while shown (`CALLBACK_FLAG_PERFORM_ACTIVE_SCAN`); selecting calls `MediaRouter.selectRoute`; `onConnected` fires when state becomes `Connected`. When connected, rows are replaced by "Connected to <device>" + "Stop casting". Copy: title "Cast to", subtitle "Same Wi-Fi as this phone", spinner row "Looking for more devices…", empty "No TVs found on this Wi-Fi" + button "Wi-Fi settings" (`Settings.ACTION_WIFI_SETTINGS`). Use `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.

- [ ] **Step 1: Implement.**
- [ ] **Step 2: Verify** `./gradlew :animeworld:assembleNoFirebaseDebug` — BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** `feat(cast): Compose cast button and device picker`

### Task 7: Remote screen, style sheet, mini bar, notification target

**Files:**
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/cast/CastRemoteScreen.kt`
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/cast/CastMiniBar.kt`
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/cast/CastRemoteRoute.kt` (`@Serializable data object CastRemoteRoute : NavKey`)
- Create: `animeworld/src/main/java/com/programmersbox/animeworld/cast/CastRemoteTrampolineActivity.kt`
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt` (add `navigation<CastRemoteRoute> { CastRemoteScreen() }` next to `navigation<VideoScreen>`)
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/cast/CastOptions.kt` (notification + expanded controller target → `CastRemoteTrampolineActivity`)
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/MainActivity.kt` (`BottomBarAdditions` → `CastMiniBar(onOpen = { navigate(CastRemoteRoute) })`)
- Modify: `animeworld/src/main/AndroidManifest.xml` (register trampoline; remove `ExpandedControlsActivity`, `WebService`)

**Interfaces:**
- Consumes: `CastController`, `CastUiState`, `CastJumps`, existing subtitle style controls (reuse the composables used by `SubtitleQuickSheet` / `SubtitleSettingsScreen` for size, color, edge, background) and `SubtitleSettingsRepository.update`.
- Produces: `@Composable fun CastRemoteScreen()`, `@Composable fun CastMiniBar(onOpen: () -> Unit)`.
- Layout per spec §3 and preview v2: app bar (collapse = pop back; "Casting to" / device name); 16:9 poster (Kamel/Glide as the details page does) with "On TV" badge and `currentCue` overlay; show title / episode; seek `Slider` (seek on release) with elapsed / `-remaining`; transport row in Global Constraints order, play/pause as large primary button; subtitle list card ("Subtitles" header, "Style" text button, rows "Off" + `tracks` with check on `activeTrackId`, hint = `origin`), internally scrollable; volume slider; outlined "Stop casting". Style opens `ModalBottomSheet` "Subtitle style on TV" with a small preview. Screen pops itself when `connection != Connected`. Hardware volume keys: override in `MainActivity.dispatchKeyEvent` to call `setVolume(±0.05f)` while `Connected`.
- Trampoline: starts `MainActivity` with `Uri.parse("animeworld://$CastRemoteRoute")` (same scheme as `MainActivity.VIEW_VIDEOS`) and finishes.

- [ ] **Step 1: Implement.**
- [ ] **Step 2: Verify** `./gradlew :animeworld:assembleNoFirebaseDebug` — BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** `feat(cast): Compose remote screen, style sheet and mini bar`

### Task 8: Entry points, sources, and removal of the old stack

**Files:**
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerCompose.android.kt` (`VideoTopBar` actions: `CastButton` before the CC button; picker sheet; on connect → build `CastRequest` from `VideoViewModel`, pause ExoPlayer, `cast()`, then pop player and navigate to `CastRemoteRoute`)
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoViewModel.kt` (`fun castRequest(): CastRequest` — position from `exoPlayer.currentPosition`; selected subtitle: index of selected sidecar track; if the selected track is embedded, `closestSubtitleIndex(format.language, format.label, subtitles)` then fall back to `defaultSubtitleIndex(...)`)
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt` (`playOrCast`: when `Connected`, build request from the already-resolved `storage` — start position `getSharedPreferences("videos").getLong(storage.link, 0)`, subtitle `defaultSubtitleIndex(...)` — call `cast()`, then navigate to `CastRemoteRoute`; drop the `model.source.canCast` check. `VideoViewerRoute` `onCastLoad` same with `localFile` + `OfflineSubtitles.find`, `subtitleOrigin = "downloaded"`. Replace `castButton`/`DetailActions` `MediaRouteButton` with `CastButton` + picker. Settings "Cast" item opens the picker or remote. Show toasts for `Unsupported` → "This episode can't be cast", `NoWifi` → "Connect to the same Wi-Fi as your TV", `Failed` → "Couldn't cast: <message>")
- Modify: `source_utilities/aniyomi/src/androidMain/kotlin/com/programmersbox/aniyomi/AniyomiApiService.kt:34`, `source_utilities/cloudstream/src/androidMain/kotlin/com/programmersbox/cloudstream/CloudstreamApiService.kt:39` (remove the `canCast = false` override) and their tests (`assertFalse(service.canCast)` → `assertTrue`)
- Delete: `animeworld/src/main/java/com/programmersbox/animeworld/cast/ExpandedControlsActivity.kt`, `MyMiniControllerFragment.kt`, `CastHelper`/`Utils`/`WebService` from `cast/CastOptions.kt`, `res/menu/expanded_controller.xml`, mini-controller layout + `CustomCast` styleable / theme attrs if now unused
- Modify: `animeworld/build.gradle.kts` (remove `androidLibs.localCast`); `MainActivity` (remove `cast` companion; keep the `ACCESS_LOCAL_NETWORK` request)
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Implement.**
- [ ] **Step 2: Verify** `./gradlew :animeworld:assembleNoFirebaseDebug :animeworld:shared:jvmTest :animeworld:shared:testAndroidHostTest :source_utilities:aniyomi:testAndroidHostTest :source_utilities:cloudstream:testAndroidHostTest` — all pass. `grep -rn "CastHelper\|MainActivity.cast\|localCast" animeworld` — no hits.
- [ ] **Step 3: Commit** `feat(cast): wire custom cast flow into player, details and downloads`

### Task 9: On-device verification

- [ ] **Step 1:** Check Room version vs main checkout; `./gradlew :animeworld:installNoFirebaseDebug` on the Samsung A15 (`adb-R5CX12GDWPZ-tKl7wT._adb-tls-connect._tcp`) and, if attached, the Android 17 phone.
- [ ] **Step 2:** With a real Chromecast/Google TV on the same Wi-Fi, verify: Aniyomi HLS stream with subtitles casts from the player at the current position; switch track and Off from the remote list; change style and see it on TV; all five transport buttons; volume slider + hardware keys; collapse → mini bar on home; notification opens remote; stop from TV clears mini bar; downloaded MP4 casts from Videos with its subtitles; MKV shows "This episode can't be cast".
- [ ] **Step 3:** Record results (what passed, what wasn't testable) in the PR description.
