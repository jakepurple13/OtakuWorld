# Better media update notifications

Date: 2026-11-05
Mockups: proposals https://claude.ai/artifact/BD3c5dkLs7gS2rUkuSo9eT (proposals 1 and 5 chosen),
flow + errors screen https://claude.ai/artifact/HWzcqTJ5471f3gYH3Gn8h7 (approved).

## Why

The current update notifications work but say little and hide failures. The user does not find them
annoying; the goal is to make them more informative:

- Each update names only one chapter (`chapters.firstOrNull()`), even when several dropped.
- The check progress notification is a single bar with no sense of which source is slow or broken.
- When a check fails for an item, nothing tells the user. Errors go to `exceptionDao` with no link
  to the item or the run.

Inspiration: Mihon writes an HTML error report after a library update. We want the same idea as an
in-app screen.

## Goals

1. **Per-title update notification** shows how many new chapters/episodes and lists them.
2. **Check progress notification** shows one segment per source, marks updates found, and marks
   failed sources.
3. **Check finished notification** summarizes the result and explains failures. When the last check
   had errors, tapping it opens a new in-app **Update check errors** screen.
4. **Update check errors screen** lists, for the most recent check only, which items failed and why,
   grouped by source, with Retry failed and Share.

## Non-goals

- No "Read" action on update notifications (user explicitly declined). Tapping the notification
  opens the item's Details screen, which is what it already does via `info.deepLinkDetails`.
- No error history. Only the most recent check is kept; each run replaces the previous record.
- Not doing proposals 2, 3, 4, 6 (summary redesign, digest/quiet hours, priority channels,
  lock-screen privacy).
- No HTML file output. Share sends plain text.

## Anti-goals

- Don't make notifications noisier: the progress and finished notifications stay on the
  low-importance `UpdateCheck` channel and stay silent.
- Don't show the finished notification as an error when nothing failed. No errors: keep today's
  behaviour (short-lived, auto-dismissing).

## Design

### 1. Per-title notification (`UpdateNotification.mapDbModel`)

- Title: `<title> · N new chapters` (plural string; "episodes" for AnimeWorld, "chapters" for
  MangaWorld and NovelWorld).
- Text (collapsed): range or single name, e.g. `Ch. 140 – 142` / the chapter name.
- Expanded: InboxStyle with the new chapter names (cap at ~5 lines + "+N more"). Cover stays as the
  large icon. The current big-picture style is replaced by the list.
- N = `infoModel.chapters.size - oldNumChapters`. "Which chapters are new" must not assume source
  ordering; see open items for the builder.
- Tap: unchanged, opens Details for that item.
- Actions: **Mark read** (unchanged). **Download N** only when
  `genericInfo.supportsBatchDownload` is true (today only MangaWorld); it calls
  `batchDownloadChapters(newChapters, infoModel)` from a broadcast receiver, then behaves like Mark
  read for that notification.
- `SavedNotifications.viewNotificationFromDb` (re-showing saved notifications) should produce the
  same look where the stored data allows. Storing the new chapter names on `NotificationItem` is
  a builder decision (it would need a Room migration).

### 2. Progress notification (`UpdateFlowWorker.createNotification`)

- API 36+: `NotificationCompat.ProgressStyle` (or platform `Notification.ProgressStyle`): one
  segment per source being checked, a point for each update found, failed sources' segments in an
  error color.
- Below API 36 (28–35): one determinate `setProgress` bar across favorites being checked
  (indeterminate while recent lists load). Collapsed title `Checking Asura · 18 of 42`, text
  `3 updates · Comick failed`. Expanded `BigTextStyle`: one line per source (`✓ MangaDex · 1 update`,
  `✗ Comick · HTTP 403`, `… MangaPark`). Built from the same per-source state as the ProgressStyle
  version. Shown in the mockup's "Before Android 16" section.
- Text line: `Checking 5 sources · 42 favorites`. Cancel action stays.
- `MediaUpdateChecker` needs to report per-source state (started / done / failed) and
  updates-found events, not just `(max, progress, source)`.

### 3. Finished notification (`UpdateNotification.sendFinishedNotification`)

- No errors: `3 updates from 42 favorites` (or `No updates from 42 favorites`), auto-dismiss after a
  few seconds as today (current `timeoutAfter = 750L` may be raised to ~4s so it can be read).
- Errors: `3 updates from 42 favorites` + `4 couldn't be checked. Tap to see why.`; does not
  auto-dismiss; tap deep-links to the Update check errors screen.

### 4. Update check errors screen (new, `kmpuiviews/commonMain`)

Layout as in the approved mockup:

- Top bar: back + "Update check errors".
- Summary card: time of last check, duration, counts checked / updated / failed.
- Groups by source. Group header: source name, a pill for the dominant error kind, item count.
- Source-level failure (the source's recent list failed): banner "Couldn't load <source>'s recent
  updates" + reason + "None of its favorites were checked". Rows for that source's favorites read
  "Not checked: source failed".
- Item rows: cover, title, one-line error. Tap row expands full message (request URL / HTTP status /
  exception text). Tap the chevron opens Details.
- Bottom actions: **Retry failed** (re-run the normal update check pipeline limited to the failed items:
  recents are fetched again for their sources, then matching favorites are checked) and
  **Share** (plain-text report through the platform share sheet).
- Empty state when the last check had no errors (screen is reachable from settings).
- Also reachable from Notification settings ("Last update check" row) so it isn't only behind the
  notification.

Error kinds to classify:

| Kind | Source |
|---|---|
| Timed out | `withTimeout` / `withTimeoutOrNull` (15s) |
| HTTP error | HTTP status from source (403 Cloudflare, 404, 5xx) when available |
| Couldn't read page | parse / other exception from `toInfoModel()` |
| Recent list failed | `fetchRecentItemsFromAllSources` failure/timeout for a source; all favorites from that source are listed as not checked |

### 5. Last-check record

- Written by `MediaUpdateChecker` (commonMain) at the end of each run, replacing the previous one.
- Contents: started/finished time, counts, and a list of failures (item url, title, imageUrl,
  source, kind, short message, full detail).
- Storage: **my call** — a single kotlinx.serialization JSON file (or DataStore entry) rather than a
  Room table, because only one record is ever kept and it avoids a DB migration. Builder may choose
  differently with reason.
- The existing `exceptionDao.insertException` calls stay.
- **My call:** a cancelled run (user tapped Cancel) does not write a record; the previous record
  stays. A run that crashes partway writes what it has, with a run-level "Check stopped early"
  banner on the errors screen.
- Recent-list failures are currently swallowed into `emptyList()` by both the `catch` and the
  timeout in `fetchRecentItemsFromAllSources`; that code must surface the failure per source.

### Failed source (progress notification) — my call

A source's segment turns the error color only for a source-level failure (its recent list failed or
timed out). Individual item failures don't color the segment; they are counted in the text
(`3 updates · 2 failed so far`) and listed on the errors screen. The callback therefore needs
source-level events in real time; item failures can be collected for the record.

### Counts — my call

"from 42 favorites" = favorites considered for the run (notifying favorites, or all with Check all).
"checked" on the errors screen = items actually fetched. Desktop has no notification work (its
notification code is still a TODO); only the record and the screen apply there.

## Constraints

- KMP: record, classification, and screen live in `kmpuiviews/commonMain`; notifications in
  `androidMain`. Desktop's `BackgroundWorkHandlerImpl` also runs `MediaUpdateChecker`, so the record
  gets written there too.
- minSdk 28, targetSdk 37. ProgressStyle is API 36+, needs a fallback.
- Navigation3: add a `NavKey` for the screen, register via Koin `navigation<Key>`, deep link like
  existing `deepLinkDetails` / `deepLinkSettings`.
- Applies to all three apps (shared code); per-app noun for chapters/episodes.
- Lazy list items use `Modifier.animateItem()`.
- `MediaUpdateCheckerTest` (jvmTest) should cover failure classification and the record.

## Left to the builder

- How to determine which chapters are "new" robustly (source ordering varies).
- Exact ProgressStyle colors/icons; exact strings.
- Shape of the progress callback API from `MediaUpdateChecker`.
- How Retry failed passes the URL list to the worker (input data vs. stored record).

## Decided after review

- Retry uses the normal pipeline filtered to the failed items (user's choice).
- Retry results **merge** into the last-check record (user's choice): items that now succeed drop
  off, ones still failing stay, updates found are added; summary keeps the original run's context.
  The retry's finished notification reports the retry's own result.
- "Source not installed" is not an error kind (user's choice). Favorites of uninstalled sources are
  filtered out before the recents step (`mapNotNull` on `apiServiceList`), so they never reach the
  item check; the `apiService == null` branch is effectively unreachable.
- Whether to persist new chapter names on `NotificationItem`.
