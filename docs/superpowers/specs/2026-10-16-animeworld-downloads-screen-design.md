# AnimeWorld downloads screen revamp — design

Date: 2026-10-16
Branch: `feat/animeworld-downloads-screen`, stacked on `feat/subtitle-settings-hls-downloads` (PR #2613).
Approved mockup: option **A — Clean list** on https://claude.ai/artifact/XZBooCNfQJapBePnR8a4Ms (artboard `Main.dc.html`).

## Why

The downloads screen ("Downloaded Videos", `ViewVideoScreen` in `:animeworld:shared`) is hard to read.
The thumbnail is sized `size(IMAGE_HEIGHT, IMAGE_WIDTH)` (width and height swapped), so it takes about
two thirds of each row. The title is squeezed into a column about 100 px wide, and the raw
`content://media/...` path fills the rest. Now that Aniyomi and CloudStream episodes download with
subtitles (#2613), this screen is where people find them, and it also mixes in every camera video on
the phone.

Success: on a phone, every row's show and episode are readable at a glance; the user sees what is
downloading, what failed, what is ready, and which ready episodes have subtitles.

## Goals

1. **Readable rows.** A 16:9 thumbnail (about 112×63 dp) on the left, with a duration badge and, when
   the video has been started, a resume bar along its bottom edge. Text on the right:
   - line 1: show name (up to 2 lines);
   - line 2: "Episode 20 · Maximum";
   - line 3: a "CC" tag plus the subtitle languages when sidecars exist, then the file size.
   - The `content://` path is never shown.
2. **Three sections**, top to bottom, each with a header and count, as in the mockup:
   - **Downloading** — episodes being downloaded, waiting to start, or failed. Hidden when empty.
   - **Downloaded** — videos inside AnimeWorld's download folder (`folderLocation`), newest first.
   - **Other videos on this phone** — every other video MediaStore returns (the user wants these
     kept on this screen). Same row style; line 2 shows date and size instead of episode details.
3. **Downloading section rows:**
   - In progress: thumbnail placeholder, show and episode, a progress bar, "42% · 39 MB" (add "of
     92 MB" only when the total is known; HLS totals usually are not), and a cancel button.
   - Waiting (enqueued, not started): same row, "Waiting" instead of progress, cancel button.
   - Failed: the row **stays** with the failure reason (the worker's message, e.g. "All files access
     is needed to save downloads") plus a **Retry** button and a **Remove** button. Retry starts the
     same download again. Remove takes the row away. Failed rows survive an app restart until retried
     or removed.
4. **Titles come from the file name** AnimeWorld writes: `"<show> - <episode name>"`, where the
   episode name is the source's chapter name (e.g. "Dragon Ball Daima - Episode 20 - Maximum").
   Split on the first `" - "`: show = before, episode = after, with `"Episode N - Name"` shown as
   "Episode N · Name". A name that doesn't match shows the file name without its extension on line 1
   and nothing on line 2. In-progress rows use the request's title, split the same way.
5. **Keep what exists:** tap a row to play (or cast when casting); swipe start-to-end plays, swipe
   end-to-start deletes (with the existing confirm dialog); the ⋮ menu's Remove; multi-select delete
   from the top bar; the empty state. Deleting a downloaded video still removes its subtitle sidecars.

## Non-goals

- Options B (grouped by show) and C (big cards).
- Pause/resume of downloads, reordering the queue, or download speed.
- Real poster art for in-progress rows (a plain placeholder block is fine).
- Changes to how downloads are made (naming, folder, HLS, subtitles) beyond what the section above
  needs from the worker.
- Desktop/iOS feature work. They must still compile; desktop shows its downloads folder as
  "Downloaded" with no Downloading section.

## Anti-goals

- Hiding or dropping the user's other videos.
- A failed download that vanishes silently, or a failed row that can't be removed.
- Reading files or MediaStore on the main thread (StrictMode already flags this area).
- Partial files appearing in the Downloaded section.

## Constraints

- All new composables live in `:animeworld:shared`. Colors are `MaterialTheme.colorScheme` roles
  (dynamic theming); the mockup's hexes are just the current dynamic palette. Hardcoded English
  strings match the module's convention.
- Downloads run through `EpisodeDownloadWorker` (WorkManager unique work named `folder + title`,
  tag `episodeDownload`, request stored as a JSON file in `noBackupFilesDir/episode-downloads/`).
  Today the request file is deleted when a download fails; failed rows need it (or an equivalent
  record) kept until the user retries or removes them. WorkManager prunes finished work after about
  a day, so failed rows can't rely on `WorkInfo` alone.
- The worker publishes only a `progress` float today; the screen needs the title for waiting rows
  and bytes downloaded for the progress label.
- "Inside the download folder" is decided by the video's real file path (`MediaColumns.DATA`)
  starting with `folderLocation`. `folderLocation` lives in the app module (`AnimeUtils.kt`), so the
  shared module needs it passed in or injected.
- Subtitle languages come from the `<base>.subtitles.json` index (`OfflineSubtitles`); file size and
  date from MediaStore (`SIZE`, `DATE_ADDED`).
- Library sort stays `COALESCE(DATE_TAKEN, DATE_ADDED * 1000) DESC` within each section.

## Settled details

From the code (verified):
- **Cancel** (in-progress or waiting) removes the row. The worker already deletes the request file and
  every partial file when the app cancels it, as the device test of #2613 confirmed. A cancelled
  download is not a failure.
- **Partial files never reach MediaStore.** The downloader writes raw files named `<name>.part` and
  renames them on success. It scans the file into MediaStore only after that, and `.part` is not a
  video extension. As a safeguard, the Downloaded and Other queries also drop any `_data` ending in
  `.part`.
- **The download folder is a real file path.** The app's folder chooser stores a directory path, and
  downloads write plain `java.io.File`s there. A row whose `DATA` is null or outside the folder goes
  to Other videos.
- **Desktop** keeps scanning what `VideoLibrarySource.jvm` scans today and shows all of it as
  Downloaded.
- **File names:** `sanitizeFileName` replaces illegal characters with a space and collapses
  whitespace; collisions get ` (2)`, ` (3)`…

Calls I made (marked as mine):
- **Retry** re-enqueues the stored request as-is under the same unique name with
  `ExistingWorkPolicy.REPLACE`. Retry is only offered on failed rows, so nothing is running under
  that name. If the stream URL has since expired, the retry fails again and the row shows the new
  reason; the user can Remove it and download again from the details screen. Re-resolving the stream
  from its source is out of scope.
- **A failed request is persisted** next to its request file as a failure marker holding the reason
  and time. It is removed on Retry or Remove. The Downloading section merges live `WorkInfo`
  (running/enqueued) with these markers (failed).
- **Title splitting:** if the name contains `" - Episode "`, split there (show before, "Episode …"
  after), so a show title that itself contains `" - "` still splits right. Otherwise split on the
  first `" - "`. Otherwise use the whole name. A trailing ` (N)` collision suffix stays on line 2 (or
  line 1 when there is no line 2).

## Left to the builder

- How queue state is exposed to the screen (an expect/actual source next to `VideoLibrarySource`,
  a ViewModel, etc.) and how failed requests are persisted.
- Whether "Other videos" starts collapsed (my call: collapsed, with its count in the header, so
  camera clips don't push anime off the screen) and whether that state is remembered.
- Exact spacing, typography roles and placeholder styling, as long as it matches the mockup's
  structure.
- What happens to a failed row's partial files (they must not appear as Downloaded; the downloader
  already removes them on failure).
