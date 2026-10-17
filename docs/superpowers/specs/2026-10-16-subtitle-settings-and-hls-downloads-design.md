# Subtitle Settings and HLS Downloads with Subtitles — Design

Date: 2026-10-16
Builds on: `2026-10-16-native-aniyomi-cloudstream-support-design.md` (PRs #2611, #2612)
Approved UI: https://claude.ai/artifact/LwauJDRcPZo88ZqcaNmBp5 (artboards "Settings › Subtitles" and
"In player › CC › Subtitle style")

## Intent

Sidecar subtitles now load and switch in AnimeWorld's player (verified on device with CloudStream AniZone
and Aniyomi AniZone, Dragon Ball Daima). The user wants two things on top:

1. **Customize how subtitles look and behave**, with the approved UI.
2. **Download episodes with their subtitles**, including from Aniyomi and CloudStream sources, whose
   streams are almost all HLS (`.m3u8`), which today's downloader cannot handle.

Success test: on a device, (a) change subtitle size, color, edge, background and position in Settings and
see it in the player; (b) download a Dragon Ball Daima episode from Aniyomi AniZone or CloudStream AniZone,
go offline, play it from the Videos library, and see/switch its subtitles.

## Decisions (agreed with the user)

- The customization UI is the approved proposal (canvas link above). Its composables live in
  `:animeworld:shared`.
- Colors follow **dynamic theming**: every chrome color is a `MaterialTheme.colorScheme` role. Only the
  subtitle text-color swatches are fixed values.
- Downloads: **full** option — turn downloads back on for Aniyomi and CloudStream sources, add HLS
  downloading, save each episode's subtitle files beside the video, and load them in the offline player.

## Goals

1. Subtitle settings screen (Settings › Subtitles) and an in-player quick sheet (CC menu › Subtitle style),
   both built from one shared set of controls, with a live preview.
2. Settings persist and apply to the player for every source.
3. Aniyomi and CloudStream sources can download episodes again, HLS included.
4. Each download keeps its subtitle files; the offline player finds, shows and switches them, using the
   same default-track choice and style settings as streaming.
5. Fix three bugs found on device today (below).

## Non-goals

- Editing or shifting subtitle timing (delay/offset).
- Burning subtitles into the video.
- Downloading audio-only tracks or multiple audio languages.
- DASH downloads (no current source needs them; keep the existing mp4 path and add HLS).
- Casting with subtitles (cast stays off for these sources).
- Desktop/iOS implementations (common code may compile there; behavior is Android-only).

## Anti-goals

- A failed or partial download never leaves a playable-looking broken file in the Videos library.
- Settings never override an `.ass` file's own styling unless the user turns "Use styles from subtitle
  file" off (default: off, so user styling applies — matches the approved mockup; see Behavior).
- No crash from a bad subtitle file: a subtitle that fails to parse is dropped, the video keeps playing.

## Part 1 — Subtitle settings

### Settings model (`:animeworld:shared` commonMain)

```kotlin
data class SubtitleStyle(
    val sizePercent: Int = 100,            // 50..200, step 10
    val font: SubtitleFont = SubtitleFont.Default,   // Default, Bold, Serif, Mono
    val textColor: SubtitleColor = SubtitleColor.White, // White, Yellow, Cyan, Green, Pink, Black
    val edge: SubtitleEdge = SubtitleEdge.Outline,   // None, Outline, Shadow, Raised
    val backgroundPercent: Int = 0,        // 0..100 step 5; 0 = no background box
    val raisePercent: Int = 6,             // 0..30, bottom padding fraction in %
    val useEmbeddedStyles: Boolean = false,
    val autoEnable: Boolean = true,
    val preferredLanguages: List<String> = emptyList(), // empty = device language, then English
    val skipSignsAndSongs: Boolean = true,
)
```

Persisted with a KMP DataStore owned by `:animeworld:shared` (or the app's existing
`AnimeDataStoreHandling` behind an interface in shared — builder's call; the shared composables only see
a `SubtitleSettingsRepository` with `val style: Flow<SubtitleStyle>` and `suspend fun update(...)`).

### Applying to the player (androidMain)

- `PlayerView.subtitleView`:
  - `setStyle(CaptionStyleCompat(foreground, background, windowColor = transparent, edgeType, edgeColor = black, typeface))`
    mapping `SubtitleEdge` → `EDGE_TYPE_NONE/OUTLINE/DROP_SHADOW/RAISED`, `SubtitleFont` → `Typeface`.
  - `setFractionalTextSize(DEFAULT_TEXT_SIZE_FRACTION * sizePercent / 100)`.
  - `setBottomPaddingFraction(raisePercent / 100f)`.
  - `setApplyEmbeddedStyles(useEmbeddedStyles)` and `setApplyEmbeddedFontSizes(useEmbeddedStyles)`.
- The subtitle view stays in the screen-sized overlay (current fix).
- Default-track choice (`defaultSubtitleIndex`) takes `preferredLanguages` and `skipSignsAndSongs`;
  `autoEnable = false` starts with text tracks disabled.
- Settings changes made in the quick sheet apply live.

### UI (`:animeworld:shared` commonMain, Compose Multiplatform, Material 3)

- `SubtitleStyleControls(style, onChange, compact: Boolean)` — the shared controls.
- `SubtitleSettingsScreen(...)` — full screen, top app bar with Back and Reset, live preview card, sections
  Text (size slider, font chips, color swatches), Outline and background (edge segmented button,
  background slider), Position (raise slider), Behavior (embedded-styles switch, auto-enable switch,
  preferred language row, skip-signs switch). Matches the "Settings › Subtitles" artboard.
- `SubtitleQuickSheet(...)` — in the player, a right-side sheet in landscape: track list (Off + tracks),
  size −/+, Box toggle, "Subtitle style" button that opens the full screen. Matches the
  "In player › CC › Subtitle style" artboard. Replaces the current CC dropdown.
- Preferred language row opens a simple picker (ordered list of languages; builder's call on the exact
  control, e.g. a multi-select dialog with reorder).
- Navigation: a new `NavKey` (e.g. `SubtitleSettingsRoute`) registered via AnimeWorld's settings nav hook,
  plus an entry row in AnimeWorld's Settings list.

## Part 2 — Downloads with subtitles

### Turning downloads on

- `AniyomiApiService` and `CloudstreamApiService` set `canDownload = true`.
- The download path in `GenericAnime` (`downloadChapter` / picker download branch) must call
  `model.source.resolve(storage)` before downloading (lazy hosters, `"null"` URLs), the same as playback.
- Remove the `.m3u8` filter from the download choices once HLS is supported.

### HLS downloader (new, androidMain of `:animeworld:shared` or the app)

- A WorkManager worker downloads one episode: input = resolved stream URL, headers, MIME hint, subtitle
  list, title/episode name, destination folder (`context.folderLocation`, the existing download folder).
- Progressive (mp4) streams keep using the existing `DownloadManager` path, plus subtitle download.
- HLS: fetch the master playlist, pick the variant matching the chosen quality (else highest bandwidth),
  fetch the media playlist, download every segment in order with the stream headers, and write ONE output
  file:
  - MPEG-TS segments → concatenate into `<name>.ts`.
  - fMP4 segments (`#EXT-X-MAP`) → init segment + segments into `<name>.mp4`.
  - `#EXT-X-KEY` `METHOD=AES-128` → fetch the key (with headers) and decrypt each segment
    (AES/CBC/PKCS7, IV from the tag or the segment sequence number). `SAMPLE-AES` and other methods →
    fail the download with a clear message.
- Write to `<name>.part`, rename to the final name only when complete (anti-goal: no broken files in the
  library). Then `MediaScannerConnection.scanFile` so the Videos library (MediaStore-backed) lists it.
- A progress notification with cancel; retries per segment (3 attempts) before failing.
- The playlist parsing, variant choice, AES decryption and IV derivation live in pure Kotlin with unit
  tests (fixture playlists: TS, fMP4, AES-128 with and without IV).

### Subtitle files

- After the video completes, download each subtitle (with `KmpSubtitle.headers` merged over the stream
  headers) next to the video as `<videoBaseName>.<index>.<sanitizedLabel>.<ext>`, `ext` from the URL path
  (`vtt|srt|ass|ssa`, else sniff the first bytes: `WEBVTT` → vtt, `[Script Info]` → ass, else srt).
- Also write `<videoBaseName>.subtitles.json` listing `{file, lang, label}` in source order, so the
  offline player keeps labels and order.

### Offline playback

- When the player opens a local file (the `downloadOrStream` path), it looks for
  `<videoBaseName>.subtitles.json` (fallback: files matching `<videoBaseName>.*.(vtt|srt|ass|ssa)`), builds
  `KmpSubtitle`s with `file://` URLs, and uses the same `getMediaSource` subtitle path, default-track choice,
  CC sheet and style settings as streaming.
- Deleting a video from the Videos library also deletes its subtitle files and the json.

### Answers to the builder check (settled)

- **Storage I/O:** AnimeWorld holds `MANAGE_EXTERNAL_STORAGE` (plus WRITE/READ external and
  `READ_MEDIA_VIDEO`) and today's `DownloadManager` path already writes into `context.folderLocation`
  (default `Movies/AnimeWorld/`, user-changeable). The worker uses plain `java.io.File` I/O there.
  *(Designer's call.)* Sidecars (`.vtt/.srt/.ass/.json`) are plain files beside the video; MediaStore
  does not need to index them.
- **Library listing and delete:** `VideoLibrarySource.android.kt` queries `MediaStore.Video` and reads
  `MediaStore.Video.Media.DATA`, so every entry has a real file path; delete goes through
  `contentResolver.delete` then `File(path).delete()` — sidecar cleanup hooks in right there, by path.
  `.ts` is indexed by MediaStore as video (`video/mp2t`); the device test confirms it is listed and plays.
  If it is not listed on the test device, the builder remuxes TS to MP4 with Media3 Transformer
  (transmux, no re-encode) instead — no other part of the design changes.
- **Which URL, quality and subtitles reach the download:** the picker's chosen `KmpStorage` is the quality.
  `KmpApiService.resolve(storage)` is the same call playback uses and returns a (possibly new) storage —
  use the returned one, copying `headers`/`extras` by hand (body properties). `KmpStorage.subtitles`
  carries the subtitle list with headers. If the resolved URL is itself a master playlist, the worker
  picks the variant whose resolution or bandwidth matches the storage's `quality` text, else the highest
  bandwidth.
- **Progressive downloads move onto the new worker too** *(designer's call)*: one code path with `.part`
  handling, headers, cancel and failure cleanup, and subtitles written in the same worker. The old
  `DownloadManager` call (with its `image/*` MIME and fixed `.mp4`) is removed; the output extension
  follows the stream (`.mp4`, `.mkv`, `.webm` from Content-Type or URL; default `.mp4`).
- **Offline detection:** the Videos library passes the file path to the player (`downloadOrStream = true`,
  `showPath` = path), so sidecar lookup by base name works.
- **Other targets:** `animeworldtv` is not in the build; desktop and iOS keep no-op/default
  implementations of `SubtitleSettingsRepository` and ignore offline subtitles.
- **Branch:** the worktree's current branch has the same content as #2612; the new branch is cut from it.

## Part 3 — Bugs found on device today

1. **Wrong "Installed" badge across catalogs.** The Aniyomi AniZone tile showed "Installed" while only the
   CloudStream AniZone plugin was installed. Installed matching must include the catalog (or package and
   catalog), not the name alone.
2. **Escaped track labels.** Aniyomi AniZone labels arrive as `English - [BestBob] Signs\/Songs`. Unescape
   `\/` (and other JSON escapes) in subtitle labels when mapping to `KmpSubtitle`.
3. **Unlabelled embedded text track.** HLS can carry its own text track with no label; the CC list showed
   "Unknown". Label it "Embedded" (with language when known) or hide it when it has no cues; builder's call.

## Branch and PR

New branch `feat/subtitle-settings-hls-downloads`, stacked on `feat/native-cloudstream-support` (#2612),
new PR targeting that branch. Bug 1 touches shared extensions code from #2611/#2612 and may be
cherry-picked down if the user wants it there.

## Testing

- Unit: subtitle style mapping (CaptionStyleCompat values), `defaultSubtitleIndex` with preferred languages
  and skip-signs, HLS playlist parsing/variant choice/AES decrypt/IV, subtitle filename and format sniff,
  offline sidecar discovery, label unescape, catalog-aware installed match.
- Device (MBX6P240401889): the success test above, plus: cancel mid-download leaves no file; airplane mode
  playback of a downloaded episode; delete removes sidecars.

## Left to the builder

- DataStore placement for `SubtitleStyle`.
- Preferred-language picker control.
- Where the HLS worker lives (shared androidMain vs app) and notification styling.
- Concurrency limit for segment downloads (sequential is acceptable).
- Embedded-track handling choice (bug 3).
