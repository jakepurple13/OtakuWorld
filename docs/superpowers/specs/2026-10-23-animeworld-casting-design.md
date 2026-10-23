# AnimeWorld Casting — Design

Date: 2026-10-23
Status: approved in conversation (UI via HTML preview); builder check done, answers folded in
Preview: https://claude.ai/artifact/HSLUAjxtdN81HKvU7mEjrH (Option A, version 2)

## Intent

Watch an AnimeWorld episode on a Chromecast or Google TV with the phone acting as the
remote. Subtitles must show on the TV. The cast controls are a custom Compose UI, not
Google's stock XML cast screens.

Today casting is half-wired: discovery failed on Android 17 (fixed, see below), streams
cast with no headers and no subtitles, Aniyomi/CloudStream sources opt out of casting
entirely (`canCast = false`), downloaded files cast with a placeholder subtitle track,
and the controls are the stock `ExpandedControllerActivity` / `MiniControllerFragment`.

## Goals

1. Cast a **streamed** episode (any source, including Aniyomi and CloudStream) to a
   Google Cast receiver, at the phone player's current position.
2. Cast a **downloaded** episode from the Videos screen.
3. Send **every** subtitle track the episode has to the TV; user can switch track or turn
   subtitles off from the phone, and it changes on the TV immediately.
4. Apply the user's existing subtitle style (`SubtitleStyle`) on the TV.
5. Custom Compose cast UI: device picker, remote screen, style sheet, mini bar.
6. Notification / lock-screen controls keep working and open the new remote screen.

## Non-goals

- Phones without Google Play Services (e.g. the NUU test phone). Google Cast needs GMS.
- DLNA / UPnP / AirPlay / Roku.
- Prev/next episode, quality switching, playback speed (explicitly cut by the user).
- Desktop (`animeworld:desktop`) casting.
- MKV (and other containers the Default Media Receiver can't play reliably): no remux or
  transcode on the phone. These are blocked with a message instead (see Compatibility).
  Remux via Media3 Transformer was considered (~1–2 MB, large effort, still fails for
  HEVC/Opus/FLAC/AC3) and declined.
- Extracting subtitles embedded inside MKV/MP4 files.
- Marking the casting episode in the shared details episode list (my call: avoids
  touching shared `kmpuiviews` UI used by MangaWorld/NovelWorld; not requested).
- A custom Cast receiver app. Use the Default Media Receiver.

## Anti-goals (would be a failure even if it "works")

- Casting a stream that plays on the phone but fails silently on the TV. If a stream
  can't be cast, say so in the UI.
- Subtitles that show on the phone but not on the TV.
- Keeping the stock Google cast screens as the main control surface.

## Already done (this branch)

- `ACCESS_LOCAL_NETWORK` declared in `animeworld/src/main/AndroidManifest.xml` and
  requested at launch on API 37+ in `MainActivity.onCreate`. Android 17 targets hid all
  LAN cast devices without it. Verified compile only; not yet tested on an Android 17 phone.
  Samsung A15 (Android 16) discovers devices fine.

## The UI (decided — see preview)

### 1. Entry points
- Cast icon in the video player's top bar, next to CC. Visible only when
  `CastState != NO_DEVICES_AVAILABLE`.
- Cast icon stays on the details page (`DetailActions`) and the Videos (downloads)
  screen, as today.

### 2. Device picker
- Compose bottom sheet "Cast to", subtitle "Same Wi-Fi as this phone". Rows: device
  icon (TV vs Chromecast), name, device type. Selected row highlighted. A
  "Looking for more devices…" row with spinner while scanning.
- Empty state: says no devices were found and offers a button to Wi-Fi settings.
- If launched from the player: on connect, load the current episode at the player's
  current position with the currently selected subtitle track, pause/close the phone
  player, and open the remote screen.

### 3. Remote screen (Option A "Now playing")
Top to bottom:
- App bar: collapse (down chevron) · "Casting to **<device name>**" · cast-connected icon.
- Artwork (16:9, rounded), "On TV" badge, current subtitle cue overlaid at the bottom
  (hidden when subtitles are off).
- Show title (small) and "Episode N · Name" (larger).
- Seek slider with elapsed and remaining time.
- Transport row, **five buttons in this order**: ⟲15 · ⟲10 · play/pause (large,
  primary) · +85 · 15⟳. +85 skips an anime opening (new; the phone player's skip is 30 s).
- **Subtitle list card**, inline on the screen: header "Subtitles" with a "Style" text
  button on the right; rows "Off" then each track (label + "source"/"downloaded" hint),
  check mark on the active one. Tapping a row switches the TV immediately. Scrolls
  internally if long.
- TV volume slider (also driven by the phone's hardware volume keys).
- "Stop casting" outlined button.
- Swipe down / collapse minimizes to the mini bar; casting continues.

### 4. Subtitle style sheet
- "Subtitle style on TV". Same controls as Subtitle settings: size, text color swatches,
  edge, background opacity, plus a small TV preview. Edits write to the same
  `SubtitleSettingsRepository` and apply on the TV live.

### 5. Mini bar
- Compose bar shown **only where the bottom nav bar shows** (user decision), while a
  session is active. Lives in `MainActivity.BottomBarAdditions`; no shared-module change.
  It does not appear in floating-nav / side-rail modes or on pushed screens (details,
  settings) — accepted. thumbnail,
  episode title, cast icon + device name, play/pause, × (stop casting), thin progress bar.
  Tap opens the remote screen. Replaces `MyMiniControllerFragment` / `MiniControllerBinding`.

## How it works (decided)

### Cast stack
- Keep `play-services-cast-framework` + Default Media Receiver. Drive it through
  `RemoteMediaClient` (load, seek, play/pause, `setActiveMediaTracks`,
  `setTextTrackStyle`, `setStreamVolume`). Not Media3 `CastPlayer`: it gives less control
  over text tracks and styling.
- Replace the singleton `CastHelper` (`MainActivity.cast`) with a lifecycle-safe
  controller that exposes state as `StateFlow` (session state, device name, media status,
  position/duration, playing, tracks, active track, volume) for Compose. Remove the empty
  `try/catch` around init; if GMS is missing, the controller reports "unavailable" and
  cast UI stays hidden.
- `CastOptions` notification/expanded-controller target points at the new remote screen.

### Phone-side server
The receiver fetches media and subtitles itself, so it can't send source headers and
subtitle hosts rarely allow CORS. The phone runs a local HTTP server (LAN IP, fixed port)
while a cast session is active:
- **Subtitles:** every `KmpSubtitle` (stream) or offline subtitle file
  (`OfflineSubtitles.find`) is fetched with its own headers, converted to WebVTT
  (SRT and ASS/SSA → VTT; VTT passes through), and served with
  `Access-Control-Allow-Origin: *` and `Content-Type: text/vtt`. Each becomes a
  `MediaTrack` (`TYPE_TEXT`, `SUBTYPE_SUBTITLES`, name = cleaned label, language).
  ASS styling flattens to plain text. The "skip signs & songs" setting still applies to
  default selection, same rule as `defaultSubtitleIndex`.
- **Streams:** all HLS is proxied (my call: many anime CDNs lack the CORS headers the
  receiver needs for playlists/segments, which fails silently), plus any non-HLS stream
  that needs source headers (`playerHeaders(...)`). The proxy rewrites playlist URIs
  (variants, segments, keys, in-manifest subtitle renditions) to go back through it.
  Progressive MP4 without required headers goes to the receiver directly.
- **Subtitles inside HLS playlists** (in-manifest WebVTT renditions) pass through the
  proxy and appear in the receiver's track list; include them in the phone's subtitle list.
- A foreground service (media-playback type, tied to the cast notification) keeps the
  server alive while casting with the screen off (my call).
- **Downloads:** served from the app's download folder (replaces the
  `Environment.getExternalStorageDirectory()` root used today).
- Server stops when the session ends.

### Starting a cast (all entry points)
- **From the player:** current position; carry over the selected subtitle track. If that
  track is embedded in the video and can't go to the TV, pick the separate subtitle file
  whose language best matches it (user decision); if none matches, use the default rule.
- **From details or Videos while already connected** (my calls): resolve the stream
  first (same resolve path as the player, not the raw `storage.link`), start at the saved
  watch position, choose subtitles with `defaultSubtitleIndex`, replace whatever is on
  the TV without asking, and open the remote screen.

### Compatibility
- Before loading, check the container/MIME. MKV (by `KmpStorage.mimeType`, URL
  extension, or downloaded file extension) is blocked: show "This episode can't be cast"
  (user decision). Other unknown types are attempted; receiver load errors show a toast.

### Sources
- Flip `canCast` to allow Aniyomi and CloudStream once proxying works, or remove the
  gate; the UI must show an error toast if the receiver reports a load failure.

### Content type
- Use `KmpStorage.mimeType` when set (`application/x-mpegURL`, `application/dash+xml`,
  `video/mp4`); otherwise infer from URL as the phone player does.

## Constraints

- APK size matters (recent work cut AnimeWorld to ~38 MB). Avoid heavy new deps; a small
  HTTP server (NanoHTTPD-class) is fine. The existing `localCast` JitPack dep can be
  replaced.
- R8/minify is on for release; keep rules for anything reflective.
- Room DB version: check against the installed app before putting a worktree build on a
  device.
- Use `noFirebase` flavor for local builds. Changelog entry required (repo convention).

## Left to the builder

- Module placement (app module vs `animeworld/shared/androidMain`) and how the shared
  player gets the cast button/hook (Koin interface is the expected pattern).
- Remote screen as a Nav3 entry vs separate activity (notification must be able to
  open it).
- HTTP server library choice within the size constraint.
- Exact empty/error copy, icons, animation.
- Test strategy: unit tests for SRT/ASS→VTT conversion and HLS playlist rewriting are
  expected; on-device check with a real Chromecast/Google TV on the Samsung A15.
