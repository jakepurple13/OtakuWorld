# Subtitle Settings and HLS Downloads Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** User-customizable subtitles (approved UI) and downloadable episodes, HLS included, that keep and
play their subtitles offline.

**Architecture:** A `SubtitleStyle` model and repository plus all UI live in `:animeworld:shared`
(commonMain); androidMain maps the style onto Media3's `SubtitleView`. A new episode downloader (pure
parsing in commonMain, I/O + AES in androidMain, wrapped by a WorkManager worker) replaces Android's
`DownloadManager` for all episodes and writes subtitle sidecars that the offline player discovers.

**Tech Stack:** Kotlin 2.4.20 KMP, Compose Multiplatform Material 3, Media3 1.11.0, OkHttp 5.4.0,
WorkManager, DataStore, Koin.

**Spec:** `docs/superpowers/specs/2026-10-16-subtitle-settings-and-hls-downloads-design.md`
**Approved UI:** https://claude.ai/artifact/LwauJDRcPZo88ZqcaNmBp5 (artboards "Settings › Subtitles",
"In player › CC › Subtitle style") — implementers match its layout, copy and controls.

## Global Constraints

- Branch `feat/subtitle-settings-hls-downloads` (stacked on #2612). Android builds need
  `ANDROID_HOME=/Users/jacobrein/Library/Android/sdk`. Local flavor: `noFirebase`.
- All new composables live in `:animeworld:shared` (`animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/subtitles/`).
- Colors: every chrome color is a `MaterialTheme.colorScheme` role (dynamic theming). Only subtitle
  text-color swatches are fixed: White `#FFFFFF`, Yellow `#FFE14D`, Cyan `#7FE7FF`, Green `#9BF08A`,
  Pink `#FFB3D9`, Black `#111111`.
- `SubtitleStyle` ranges/defaults exactly as the spec: size 50..200 step 10 (default 100); font
  Default/Bold/Serif/Mono; color default White; edge None/Outline/Shadow/Raised (default Outline);
  background 0..100 step 5 (default 0); raise 0..30 (default 6); useEmbeddedStyles false; autoEnable true;
  preferredLanguages empty; skipSignsAndSongs true.
- Downloads: write `<name>.part`, rename to the final name only on success; never leave a partial file.
  Files go in `context.folderLocation` (plain `java.io.File`; app holds `MANAGE_EXTERNAL_STORAGE`).
- Sidecars: `<videoBaseName>.<index>.<sanitizedLabel>.<ext>` and `<videoBaseName>.subtitles.json`.
- HLS AES-128 supported; `SAMPLE-AES` and other key methods fail with a clear message.
- A bad subtitle never stops playback; a failed subtitle download never fails the video download.
- Desktop/iOS: no-op/default implementations only.

## Review Focus

1. Cancel mid-download → no `.part`, no video, no sidecars left. (Task 7 `cancelLeavesNoFiles`.)
2. A segment keeps failing after 3 tries → download fails, `.part` removed, notification shows the error. (Task 7 `segmentFailureCleansUp`.)
3. One subtitle URL 404s → video and the other subtitles still saved; json lists only saved ones. (Task 7 `subtitleFailureDoesNotFailVideo`.)
4. Episode name with `/ : ? *` or very long → sanitized, ≤120 chars, unique if a file already exists. (Task 6 `sanitizesAndDeduplicatesFileNames`.)
5. Extensionless subtitle URL (`.../sub?id=3`) → format sniffed from content (`WEBVTT`, `[Script Info]`, else srt), saved with the right extension, offline MIME correct. (Task 6 `sniffsSubtitleFormat`.)

---

### Task 1: Device bug fixes (installed badge, escaped labels, unlabelled track)

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionStates.kt:50-57` (+ callers)
- Modify: `source_utilities/aniyomi/src/androidMain/kotlin/com/programmersbox/aniyomi/AniyomiSourceMapper.kt:132`
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoViewModel.kt:66-80`
- Test: `kmpuiviews/src/commonTest/.../extensions/CatalogInstallTest.kt` (or a new `InstalledMatchTest.kt`), `source_utilities/aniyomi/src/androidHostTest/.../AniyomiSourceMapperTest.kt`, `animeworld/shared/src/commonTest/.../videoplayer/PlayerRequestTest.kt`

**Interfaces:**
- Produces:
  - `fun KmpRemoteSources.findInstalled(installed: List<KmpSourceInformation>, catalogName: String? = null): KmpSourceInformation?` — when `catalogName` is given, only installed sources whose `catalog?.name == catalogName` match, and the base-URL fallback applies only to sources with no native catalog (`catalog !is KmpExternalCustomApiServicesCatalog`). Same rule in `findRemote`. Callers that build a catalog's shelf pass the shelf's catalog name.
  - `fun cleanSubtitleLabel(label: String): String` in `kmpmodels` (commonMain, next to `KmpSubtitle`) — unescapes `\/`, `\"`, `\\`, `\u00XX`, trims. Used by the Aniyomi mapper (and CloudStream mapper for symmetry).
  - `fun embeddedTrackLabel(label: String?, language: String?): String` in `PlayerRequest.kt` — blank/"Unknown" label → `"Embedded"` or `"Embedded (<language>)"`; else the label. Used when building `TextTrackOption`s.

- [ ] **Step 1: Failing tests** — `crossCatalogNameDoesNotMarkInstalled` (CloudStream "Anizone" installed with baseUrl `https://anizone.to`; Aniyomi remote AniZone with same baseUrl and catalogName "Aniyomi" → `null`); `legacyBaseUrlFallbackStillWorks` (no native catalog → match); `cleanSubtitleLabel_unescapes` (`"English - [BestBob] Signs\\/Songs"` → `"English - [BestBob] Signs/Songs"`); `embeddedTrackLabel_names` (null→"Embedded", "Unknown"+"ja"→"Embedded (ja)", "Eng"→"Eng").
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest :kmpmodels:jvmTest :animeworld:shared:jvmTest` — FAIL.
- [ ] **Step 3: Implement** the three functions and wire callers.
- [ ] **Step 4: Run** the same + `:source_utilities:aniyomi:testAndroidHostTest` — PASS.
- [ ] **Step 5: Commit** `fix: catalog-aware installed match, clean subtitle labels, name embedded tracks`

### Task 2: SubtitleStyle model and repository

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/subtitles/SubtitleStyle.kt`
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/subtitles/SubtitleSettingsRepository.kt`
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/subtitles/DataStoreSubtitleSettingsRepository.kt`
- Create: jvm/ios `InMemorySubtitleSettingsRepository` (one commonMain class is fine if it needs no platform code)
- Modify: the shared Koin module that `GenericSharedAnime`/animeworld already include (find where `StorageHolder` is bound; bind `SubtitleSettingsRepository` there, Android → DataStore impl)
- Test: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/subtitles/SubtitleStyleTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class SubtitleStyle(...)` exactly the spec's fields/defaults, plus enums `SubtitleFont { Default, Bold, Serif, Mono }`, `SubtitleColor(val argb: Long) { White, Yellow, Cyan, Green, Pink, Black }` (values from Global Constraints), `SubtitleEdge { None, Outline, Shadow, Raised }`.
  - `fun SubtitleStyle.coerced(): SubtitleStyle` — clamps/steps ranges.
  - `interface SubtitleSettingsRepository { val style: Flow<SubtitleStyle>; suspend fun update(transform: (SubtitleStyle) -> SubtitleStyle); suspend fun reset() }`.
  - Android impl: Jetpack DataStore (Preferences, file `subtitle_settings`) storing the style as one JSON string (kotlinx.serialization) — unknown/old JSON → defaults.

- [ ] **Step 1: Failing tests** — `defaultsMatchSpec`; `coercedClampsAndSteps` (size 37→50, 205→200, 113→110; bg 103→100, 7→5; raise -1→0, 31→30); `jsonRoundTrip`; `unknownJsonFallsBackToDefaults` (decode `{"sizePercent":"x"}` → defaults, via the same decode helper the DataStore impl uses: `fun decodeSubtitleStyle(json: String?): SubtitleStyle`).
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*SubtitleStyleTest*"` — FAIL.
- [ ] **Step 3: Implement**; add DataStore/serialization deps to the shared module only if missing.
- [ ] **Step 4: Run** — PASS; `:animeworld:assembleNoFirebaseDebug` — SUCCESS.
- [ ] **Step 5: Commit** `feat(animeworld): subtitle style model and persisted settings`

### Task 3: Apply style and preferences in the player

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/subtitles/CaptionSpec.kt`
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/subtitles/SubtitleViewStyling.kt`
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videoplayer/PlayerRequest.kt` (`defaultSubtitleIndex`)
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoViewModel.kt`, `VideoPlayerCompose.android.kt`
- Test: `animeworld/shared/src/commonTest/.../subtitles/CaptionSpecTest.kt`, `PlayerRequestTest.kt`

**Interfaces:**
- Consumes: Task 2 `SubtitleStyle`, `SubtitleSettingsRepository`.
- Produces:
  - `data class CaptionSpec(val foregroundArgb: Long, val backgroundArgb: Long, val edge: SubtitleEdge, val font: SubtitleFont, val textScale: Float, val bottomPaddingFraction: Float, val applyEmbedded: Boolean)` and `fun SubtitleStyle.toCaptionSpec(): CaptionSpec` — background = black with alpha `backgroundPercent/100`; `textScale = sizePercent/100f`; `bottomPaddingFraction = raisePercent/100f`.
  - androidMain `fun SubtitleView.apply(spec: CaptionSpec)` — `setStyle(CaptionStyleCompat(fg, bg, Color.TRANSPARENT, edgeType, Color.BLACK, typeface))` (edge None/Outline/Shadow/Raised → `EDGE_TYPE_NONE/OUTLINE/DROP_SHADOW/RAISED`; font Default → `Typeface.DEFAULT`, Bold → `DEFAULT_BOLD`, Serif → `SERIF`, Mono → `MONOSPACE`), `setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * textScale)`, `setBottomPaddingFraction`, `setApplyEmbeddedStyles` + `setApplyEmbeddedFontSizes(applyEmbedded)`.
  - `defaultSubtitleIndex(subtitles, deviceLanguage, deviceLanguageName = "", preferredLanguages: List<String> = emptyList(), skipSignsAndSongs: Boolean = true): Int?` — preferred languages tried in order before the device language; `skipSignsAndSongs = false` disables the partial-track demotion.
  - Player collects `repository.style` and re-applies on change (live); `autoEnable = false` → text track type disabled at start.

- [ ] **Step 1: Failing tests** — `toCaptionSpec_mapsValues` (bg 40 → `0x66000000`, size 150 → 1.5f, raise 10 → 0.10f, color Yellow → `0xFFFFE14D`); `preferredLanguageWins` (preferred ["ja"], device "en", tracks [English, Japanese] → 1); `skipSignsOffKeepsOrder` (tracks ["English - Signs/Songs", "English Subtitles"], skip=false → 0).
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest` — FAIL.
- [ ] **Step 3: Implement**; keep the subtitle view in `overlayFrameLayout` (existing fix).
- [ ] **Step 4: Run** — PASS; `:animeworld:assembleNoFirebaseDebug` — SUCCESS.
- [ ] **Step 5: Commit** `feat(animeworld): apply subtitle style and language preferences in the player`

### Task 4: Subtitle settings screen

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/subtitles/SubtitleStyleControls.kt` (shared controls: size, font chips, color swatches, edge segmented button, background slider, raise slider; `compact: Boolean`)
- Create: `.../subtitles/SubtitlePreview.kt` (preview card rendering two sample cues with the style)
- Create: `.../subtitles/SubtitleSettingsScreen.kt` (+ `@Serializable data object SubtitleSettingsRoute : NavKey`)
- Create: `.../subtitles/SubtitleSettingsViewModel.kt` (exposes `style: StateFlow<SubtitleStyle>`, `update`, `reset`)
- Create: `.../subtitles/PreferredLanguagesDialog.kt`
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt` (`settingsNav3Setup()` add `navGraph.entry<SubtitleSettingsRoute> { SubtitleSettingsScreen() }`; `composeCustomPreferences()` add a "Subtitles" `PreferenceSetting` row navigating there)
- Test: `animeworld/shared/src/commonTest/.../subtitles/SubtitleSettingsViewModelTest.kt`

**Interfaces:**
- Consumes: Task 2 repository; Task 3 `toCaptionSpec` (the preview uses `CaptionSpec` → Compose `TextStyle`/`Shadow` so it matches the player).
- Produces: `@Composable fun SubtitleStyleControls(style: SubtitleStyle, onChange: (SubtitleStyle) -> Unit, compact: Boolean, modifier: Modifier = Modifier)`; `@Composable fun SubtitlePreview(style: SubtitleStyle, modifier: Modifier = Modifier)`; `@Composable fun SubtitleSettingsScreen(viewModel: SubtitleSettingsViewModel = koinViewModel())`.
- Layout/copy exactly the approved "Settings › Subtitles" artboard: top bar Back + "Subtitles" + "Reset"; sections "Text", "Outline and background", "Position", "Behavior" with the artboard's labels and supporting text ("Use styles from subtitle file" / "Keeps .ass fonts, colors and positions. Size and colors above apply when off.", "Turn on automatically" / "Picks a track in your preferred language", "Preferred language" / e.g. "English, then Japanese" or "Device language" when empty, "Skip signs and songs tracks" / "Prefer full dialogue when choosing a track"). Material 3 components: `Slider` (with `steps`), `FilterChip`, `SingleChoiceSegmentedButtonRow`, `Switch`, `ListItem`; swatches are 40dp circles with a selected ring in `colorScheme.primary`; content description on every swatch. Preferred language dialog: checkable list of common languages (English, Japanese, Spanish, Portuguese, French, German, Italian, Arabic, Russian, Korean, Chinese) with up/down reorder of checked ones.

- [ ] **Step 1: Failing tests** — `updateCoercesAndPersists` (fake repo; `update { it.copy(sizePercent = 37) }` → stored 50); `resetRestoresDefaults`.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*SubtitleSettingsViewModelTest*"` — FAIL.
- [ ] **Step 3: Implement** screen, controls, preview, dialog, route and settings row.
- [ ] **Step 4: Run** tests — PASS; `:animeworld:assembleNoFirebaseDebug :animeworld:desktop:compileKotlinJvm` — SUCCESS.
- [ ] **Step 5: Commit** `feat(animeworld): subtitle settings screen`

### Task 5: In-player quick sheet replaces the CC dropdown

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/subtitles/SubtitleQuickSheet.kt`
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerCompose.android.kt` (remove the CC `DropdownMenu`; CC button opens the sheet)
- Modify: `VideoViewModel.kt` (`subtitleMenuOpen` → `subtitleSheetOpen`)

**Interfaces:**
- Consumes: Task 4 `SubtitleStyleControls` (not used here directly — the sheet has its own compact size/box row per the artboard), Task 2 repository, existing `TextTrackOption`.
- Produces: `@Composable fun SubtitleQuickSheet(tracks: List<SubtitleTrackItem>, onSelectTrack: (Int?) -> Unit, style: SubtitleStyle, onStyleChange: (SubtitleStyle) -> Unit, onOpenSettings: () -> Unit, onDismiss: () -> Unit)` with `data class SubtitleTrackItem(val label: String, val selected: Boolean)` (index null = Off). Layout exactly the "In player › CC › Subtitle style" artboard: right-side panel 360dp in landscape (bottom sheet in portrait), title "Subtitles" + close, radio list "Off" + tracks with a check on the selected one, row "Size" A−/value/A+ and a "Box" filter chip (toggles background 0 ↔ 60), and a "Subtitle style" tonal button that navigates to `SubtitleSettingsRoute`. Overlay controls stay visible while the sheet is open.

- [ ] **Step 1:** Implement; changes apply live through the repository.
- [ ] **Step 2: Build** `:animeworld:assembleNoFirebaseDebug` — SUCCESS.
- [ ] **Step 3: Commit** `feat(animeworld): in-player subtitle quick sheet`

### Task 6: Download core (pure logic)

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/downloads/HlsPlaylist.kt`
- Create: `.../downloads/DownloadNaming.kt`
- Test: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/downloads/HlsPlaylistTest.kt`, `DownloadNamingTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface HlsPlaylist { data class Master(val variants: List<HlsVariant>) ; data class Media(val segments: List<HlsSegment>, val map: HlsMap?, val mediaSequence: Long) }`; `data class HlsVariant(val uri: String, val bandwidth: Long, val width: Int?, val height: Int?)`; `data class HlsSegment(val uri: String, val key: HlsKey?, val sequence: Long)`; `data class HlsKey(val method: String, val uri: String?, val iv: ByteArray?)`; `data class HlsMap(val uri: String)`.
  - `fun parseHlsPlaylist(text: String, baseUrl: String): HlsPlaylist` — resolves relative URIs against `baseUrl`; key applies to following segments until replaced; `METHOD=NONE` → null key.
  - `fun HlsPlaylist.Master.chooseVariant(qualityHint: String?): HlsVariant` — match a height in the hint (`"1080p"`, `"1920x1080"`, `"... 720p ..."`), else highest bandwidth.
  - `fun aesIv(segment: HlsSegment): ByteArray` — explicit IV, else 16-byte big-endian `sequence`.
  - `fun sanitizeFileName(name: String, maxLength: Int = 120): String`; `fun uniqueBaseName(base: String, exists: (String) -> Boolean): String` (`base`, `base (2)`, …).
  - `enum class SubtitleFormat(val ext: String, val mime: String) { Vtt("vtt","text/vtt"), Srt("srt","application/x-subrip"), Ass("ass","text/x-ssa") }`; `fun subtitleFormat(url: String, firstBytes: String): SubtitleFormat` (extension first; else `WEBVTT` → Vtt, `[Script Info]` → Ass, else Srt).
  - `fun sidecarName(videoBase: String, index: Int, label: String, format: SubtitleFormat): String`.
  - `@Serializable data class SidecarEntry(val file: String, val lang: String, val label: String)` and json file name `"$videoBase.subtitles.json"`.

- [ ] **Step 1: Failing tests** — `parsesMasterVariants`; `parsesMediaWithRelativeUris`; `keyCarriesUntilReplaced`; `parsesFmp4Map`; `methodNoneClearsKey`; `chooseVariantByHeightThenBandwidth` ("BestBob - 720p (1280x720)" → 720 variant; null → highest); `ivFromSequence` (sequence 5 → 15 zero bytes + 0x05); `sanitizesAndDeduplicatesFileNames` (`"Episode 1: Conspiracy/Part?*"` → no `/:?*`, ≤120; exists("x") → "x (2)"); `sniffsSubtitleFormat` (`.../sub?id=3` + "WEBVTT\n" → Vtt; + "[Script Info]" → Ass; + "1\n00:00" → Srt; `a.ass?x=1` → Ass).
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*HlsPlaylistTest*" --tests "*DownloadNamingTest*"` — FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** `feat(animeworld): HLS playlist parsing and download naming`

### Task 7: Episode downloader and worker

**Files:**
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/downloads/EpisodeDownloader.kt`
- Create: `.../downloads/EpisodeDownloadWorker.kt`
- Modify: `animeworld/shared/build.gradle.kts` (androidMain: `androidLibs.workRuntimeKtx`, okhttp, kotlinx serialization if missing; `withHostTestBuilder {}` + `androidHostTest` deps `commonLibs.kotlin.test`, `commonLibs.coroutinesTest`, `androidLibs.okhttpMockWebServer`, mirroring `source_utilities/aniyomi/build.gradle.kts`)
- Test: `animeworld/shared/src/androidHostTest/kotlin/com/programmersbox/anime/shared/downloads/EpisodeDownloaderTest.kt`

**Interfaces:**
- Consumes: Task 6.
- Produces:
  - `@Serializable data class EpisodeDownloadRequest(val url: String, val headers: Map<String, String>, val mimeType: String?, val qualityHint: String?, val subtitles: List<KmpSubtitle>, val title: String, val folder: String)`.
  - `class EpisodeDownloader(client: OkHttpClient) { suspend fun download(request: EpisodeDownloadRequest, onProgress: (Float) -> Unit): File }` — HLS when `mimeType` is HLS or the URL path/body is an m3u8 (`#EXTM3U`), else progressive; output `.ts` (TS segments), `.mp4` (fMP4 or progressive default), or `.mkv`/`.webm` by Content-Type/URL; `.part` then rename; segment retries 3; AES-128 via `javax.crypto` `AES/CBC/PKCS5Padding` with the key fetched once per key URI (with headers); other methods → `IOException("Unsupported HLS encryption: <method>")`; after the video, subtitles (headers = stream headers + `KmpSubtitle.headers`) and the json sidecar; each subtitle failure logged and skipped; cancellation (coroutine cancel) deletes `.part` and any sidecars written; returns the final video file.
  - `class EpisodeDownloadWorker(context, params) : CoroutineWorker` — input data = JSON `EpisodeDownloadRequest`; foreground notification with progress and a Cancel action; on success `MediaScannerConnection.scanFile(videoPath)`; on failure a notification with the message. `fun enqueueEpisodeDownload(context: Context, request: EpisodeDownloadRequest)` (unique work name = folder+title).

- [ ] **Step 1: Failing tests (MockWebServer)** — `progressiveDownloadWritesFileAndSidecars`; `hlsTsSegmentsConcatenated` (3 segments → bytes equal concat, `.ts`); `hlsFmp4WritesInitThenSegments` (`.mp4`); `aes128SegmentsDecrypted` (encrypt fixtures with a known key/IV in the test); `sampleAesFails`; `cancelLeavesNoFiles`; `segmentFailureCleansUp` (segment 2 returns 500 four times → throws, no `.part`, no final file); `subtitleFailureDoesNotFailVideo` (2 subs, one 404 → video + 1 sidecar + json with 1 entry); `headersSentOnEveryRequest` (Referer on playlist, segments, key, subtitles).
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:testAndroidHostTest` — FAIL.
- [ ] **Step 3: Implement** downloader, then the worker (register a notification channel; reuse the app's existing channel helper if one exists in UIViews/kmpuiviews).
- [ ] **Step 4: Run** — PASS; `:animeworld:assembleNoFirebaseDebug` — SUCCESS.
- [ ] **Step 5: Commit** `feat(animeworld): episode downloader for HLS and progressive streams with subtitles`

### Task 8: Turn downloads on and route them through the worker

**Files:**
- Modify: `source_utilities/aniyomi/src/androidMain/kotlin/com/programmersbox/aniyomi/AniyomiApiService.kt` (`canDownload = true`) and its test asserting false
- Modify: `source_utilities/cloudstream/src/androidMain/kotlin/com/programmersbox/cloudstream/CloudstreamApiService.kt` (`canDownload = true`) and its test
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt:205-330` (`downloadChapter`: drop the `.m3u8` filter; resolve the picked storage with `model.source.resolve(storage)` on IO (copy `headers`/`extras` from the original if the returned one lacks them); build `EpisodeDownloadRequest(url = link, headers = playerHeaders(storage.headers, storage.headers["referer"] ?: storage.source), mimeType = storage.mimeType, qualityHint = storage.quality, subtitles = storage.subtitles, title = "${infoModel.title} - ${model.name}", folder = context.folderLocation)`; `enqueueEpisodeDownload`; delete `downloadVideo` and its `DownloadManager` code)
- Modify: the picker's download branch (`VideoChoiceScreen` `isStreaming = false`) only if it bypasses `downloadChapter`

**Interfaces:**
- Consumes: Task 7 `EpisodeDownloadRequest`, `enqueueEpisodeDownload`; existing `playerHeaders`, `KmpApiService.resolve`.

- [ ] **Step 1:** Update the two service tests to expect `canDownload == true` (failing first).
- [ ] **Step 2:** Implement the wiring; resolve failures show the existing error toast.
- [ ] **Step 3: Run** `:source_utilities:aniyomi:testAndroidHostTest :source_utilities:cloudstream:testAndroidHostTest` — PASS; `:animeworld:assembleNoFirebaseDebug` — SUCCESS.
- [ ] **Step 4: Commit** `feat(animeworld): download Aniyomi and CloudStream episodes through the episode downloader`

### Task 9: Offline subtitles and sidecar cleanup

**Files:**
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/downloads/OfflineSubtitles.kt`
- Modify: `VideoViewModel.kt` (when `downloadOrStream` is true, `subtitles = OfflineSubtitles.find(File(showPath))`)
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videos/VideoLibrarySource.android.kt:81-90` (`delete` also calls `OfflineSubtitles.deleteFor(File(content.path))`)
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerCompose.android.kt:~212` (the download branch builds its source with `getMediaSource(...)` from a `file://` Uri, subtitles and no headers, so offline uses the same subtitle path)
- Test: `animeworld/shared/src/androidHostTest/.../downloads/OfflineSubtitlesTest.kt`

**Interfaces:**
- Consumes: Task 6 naming/`SidecarEntry`/`SubtitleFormat`.
- Produces: `object OfflineSubtitles { fun find(video: File): List<KmpSubtitle>; fun deleteFor(video: File) }` — json first (missing files skipped), else glob `<base>.*.(vtt|srt|ass|ssa)` sorted by index; URLs are `file://` absolute paths; labels from json or the file's label segment.

- [ ] **Step 1: Failing tests** — `findsFromJsonInOrder`; `jsonEntryWithMissingFileSkipped`; `fallsBackToGlob`; `deleteForRemovesSidecarsAndJson` (and leaves other videos' sidecars).
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:testAndroidHostTest --tests "*OfflineSubtitlesTest*"` — FAIL.
- [ ] **Step 3: Implement** and wire.
- [ ] **Step 4: Run** — PASS; `:animeworld:assembleNoFirebaseDebug` — SUCCESS.
- [ ] **Step 5: Commit** `feat(animeworld): offline subtitles for downloaded episodes`

### Task 10: Device verification (controller)

On MBX6P240401889 (temporarily lock portrait for navigation; restore `accelerometer_rotation` after):

- [ ] Settings › Subtitles: change size 150, Yellow, Shadow, background 40, raise 12, Serif → preview matches; play AniZone (Aniyomi) Daima Ep 1 → cues match; quick sheet A+/Box apply live; "Subtitle style" opens the screen.
- [ ] Preferred language Japanese → Japanese track picked on CloudStream AniZone; skip signs off/on behaves.
- [ ] Download Daima Ep 1 from Aniyomi AniZone (HLS) → progress notification → file in `Movies/AnimeWorld/` (`.ts` or `.mp4`), sidecars + json beside it; Videos library lists it.
- [ ] Airplane mode → play the download → subtitles show and switch.
- [ ] Cancel a second download mid-way → no files left. Delete the first from Videos → sidecars gone.
- [ ] Extensions › catalog: Aniyomi AniZone no longer shows "Installed" when only the CloudStream plugin is (uninstall the Aniyomi one first to check); CC labels show "Signs/Songs" and "Embedded".
- [ ] If `.ts` is not listed in Videos, add a Transformer remux to `.mp4` (spec fallback) as a follow-up task before finishing.
