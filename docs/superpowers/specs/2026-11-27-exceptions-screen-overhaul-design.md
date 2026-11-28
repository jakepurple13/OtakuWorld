# Exceptions screen overhaul — design

Date: 2026-11-27 · Branch: `worktree-exceptions-screen-ui-overhaul`
Approved mockup: https://claude.ai/artifact/Uaxnjdq5VZ58i6QHYwVaD5 (tabs "D + C detail" and "Desktop")

## Goal

Replace the flat list of raw stack traces at Settings → Diagnostics → Exceptions with:

- a day-grouped **timeline** with a 24-hour bar graph and an AI **digest** of recent errors,
- a **detail page** per exception with a plain-language headline, facts, an explanation, the stack trace and actions,
- the same behavior on Android (phone, tablet) and desktop (JVM).

No database change. Everything is derived from `ExceptionItem.message` (a `stackTraceToString()`).

## Scope

In:
- `ExceptionsScreen` rewrite (timeline + detail) in `kmpuiviews/commonMain`.
- Exception parsing + classification (pure Kotlin, unit-tested).
- Fixed explanations for known errors, AI for the rest, AI digest.
- Copy report, Share (Android) / Save… (desktop), Source ▾ menu.
- Delete with slide-out animation and Undo. Delete all keeps its confirm dialog.
- Desktop keyboard shortcuts and right-click menu.

Out:
- Changing how or where exceptions are recorded.
- Supabase sync behavior (delete keeps using `ExceptionDao.deleteException`).
- iOS beyond compiling (no-op sharer already exists).

## Navigation — Option A (chosen)

`Screen.ExceptionScreen` stays a `twoPaneEntry` (`ListDetailSceneStrategy.extraPane()`) in `Nav3Graph.kt`. No new `NavKey`.

Inside `ExceptionsScreen`, a `NavigableListDetailPaneScaffold` driven by `rememberListDetailPaneScaffoldNavigator<Long>()`. The content key is `ExceptionItem.time`.

- List pane = timeline. Detail pane = detail page.
- **Pane count from available width, not window size.** Wrap in `BoxWithConstraints`. Build the directive with `calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())` and override `maxHorizontalPartitions = if (maxWidth >= 840.dp) 2 else 1`. On desktop the Exceptions pane can be half the window, so window size alone would pick 2 panes in too little space.
- Back and predictive back come from the navigator (`NavigableListDetailPaneScaffold` handles it). When one pane is showing and detail is open, back returns to the timeline.
- The Android-only graph (`UIViews/.../Nav3Graph.kt:231`, `entry<Screen.ExceptionScreen>`) needs no change, since the scaffold lives inside the screen.

### Shared elements (single-pane only)

- `SharedTransitionLayout` wraps the scaffold. The `AnimatedPane` content scope is the `AnimatedVisibilityScope`.
- Timeline row: `Modifier.sharedBounds(key = "exception-$time")`. Title, kind badge, time: `Modifier.sharedElement(key = "exception-$time-title|badge|time")`. The matching keys go on the detail header.
- Two panes: no flight. The detail swaps with `AnimatedContent(fadeIn + slideInVertically(8.dp))` keyed on the selected time, and the selected row is highlighted.
- The rest of the detail content (facts, explanation, stack) fades in with a short stagger after the header lands.

## Parsing and classification

New file `domain/exceptions/ParsedException.kt` (commonMain, pure):

```kotlin
data class ParsedException(
    val time: Long,
    val type: String,          // fully qualified class from line 1, e.g. java.net.SocketTimeoutException
    val message: String,       // rest of line 1 (+ continuation lines before the first "\tat ")
    val frames: List<Frame>,   // "at ..." lines
    val causes: List<String>,  // "Caused by: ..." lines
    val extension: ExtensionRef?, // first frame under eu.kanade.tachiyomi.(anime)?extension.<lang>.<name>
    val kind: ExceptionKind,
    val httpCode: Int?,
)
data class Frame(val text: String, val isLibrary: Boolean)
data class ExtensionRef(val lang: String, val name: String) // id "lang.name", package eu.kanade.tachiyomi.extension.<lang>.<name>
enum class ExceptionKind { Network, Blocked, Parsing, Source, App }
```

- **Library frames**: `java.`, `javax.`, `kotlin.`, `kotlinx.`, `android.`, `androidx.`, `okhttp3.`, `okio.`, `org.jsoup.`, `dalvik.`, `sun.`, `jdk.`. Folded by default ("N library frames hidden", tap to show).
- **httpCode**: first match of `Status=(\d{3})`, `HTTP (\d{3})` or `HTTP error (\d{3})` in line 1.
- **kind**, first that matches:
  1. `Blocked`: httpCode 403, 429 or 503.
  2. `Network`: timeout (`SocketTimeoutException`, `InterruptedIOException: timeout`), the offline regex from `KnownError.kt`, other HTTP 4xx/5xx.
  3. `Parsing`: `SerializationException`, `JsonDecodingException`, `JsonSyntaxException`, `MalformedJsonException`, jsoup `Selector` errors.
  4. `Source`: has an `extension`.
  5. `App`: everything else. Gray badge. The mockup had no App kind; it covers backup, zip and other app errors with no source.
- **Headline** (plain text, string resources, `%1$s` = source name or "The app"): one per kind plus specific variants for timeout, offline, 403, 404, 429.
- **Fixed explanation**: reuse `knownError(kind, httpCode, detail)` by mapping to `FailureKind` (timeout → `TimedOut`, httpCode → `Http`, else `CouldNotRead`) and its `uc_explain_*` strings. Desktop gets a desktop variant for `Blocked`, because the desktop app can't pass a Cloudflare check.
- **Grouping key** for "Seen N× this week": `type + first message line + extension id`, counted over items in the last 7 days.

Source display name and extension version: looked up from installed sources by package `eu.kanade.tachiyomi.(anime)?extension.<lang>.<name>`. If not installed, show the id (`en.mangadex`) and leave the version out. The exact repository API is confirmed during planning.

## AI

- New `AiFeature.ExplainException : AiFeature<ErrorDetails>`. Same shape as `ExplainError`, with a general prompt: "explain an error from a manga/anime/novel app", 2 sentences, non-technical, one thing to try. Toggle: `AiToggle.ErrorExplanations`.
- New `AiFeature.ExceptionDigest : AiFeature<List<String>>`. Input: up to 20 most recent lines of `source: headline (type)` from the last 24 h. Output: 2–3 sentences on what they have in common and what to do. Toggle: `AiToggle.ErrorExplanations`.
- Add both to `telemetryName` (`explain_exception`, `exception_digest`).
- Engine: `AiText.availability(feature)`, which follows AI settings live. Nano or Gemma on Android, Ollama on desktop. If none is available, ✦ controls hide and known errors still show fixed text.
- Known errors never call AI.
- Detail page: an unknown error starts explaining when the detail opens. The timeline's expanded row shows a "✦ Explain this one" chip instead.
- Cache: one `AiTask` per grouping key in the ViewModel (same pattern as `UpdateCheckErrorsViewModel.explanationTasks`). Reopening shows the saved answer. Memory only, cleared when the screen's ViewModel is cleared.
- AI card footer: engine name + "can be wrong" + Retry on error.

## Timeline (list pane)

- Top card: "Last 24 hours", `N errors · M sources`, 24 hourly bars (`Canvas`, each bar animates its height in with a stagger), "✦ Summarize recent errors" chip → AI card. Hidden when there are no errors in the last 24 h.
- `stickyHeader` per day (Today / Yesterday / date) with the error count.
- Row: kind-colored dot on a rail (`drawBehind`), headline, time, kind badge, short class name. Every item uses `Modifier.animateItem()`.
- Tap expands in place (`AnimatedVisibility`): explanation (fixed card or ✦ chip), folded stack, then **Copy**, **Delete**, **More info**.
- **More info** → `navigator.navigateTo(Detail, time)`. Single-pane only. In two-pane mode, tapping a row selects it directly and rows don't expand.
- Empty state: "No exceptions" with a short line explaining what shows up here.
- Top bar: back, title, Delete all (confirm dialog, kept).

## Detail page (detail pane)

- Header: kind badge, headline (22sp), "Today at 4:52 PM" (shared elements).
- Fact tiles: Source, Extension (mono), Error (short class), Seen (N× this week). 2 columns when narrow, 4 when wide.
- Explanation: fixed card or AI card.
- Stack trace: `SelectionContainer` + `horizontalScroll`, mono, error line in error color, library frames folded, "Caused by" lines in warning color.
- Single-pane top bar: back, Delete.
- Actions:
  - Single-pane: bottom bar with Copy · Share · Source ▾.
  - Two-pane: buttons at the top right (Copy · Save… on desktop / Share on Android · Source ▾ · Delete icon).

## Report (Copy / Share / Save…)

Markdown. The AI explanation is not included.

````
### MangaWorld error report
- App: MangaWorld 33.0.0
- Device: Android 16 · Pixel 8        (desktop: macOS 26.1 · aarch64)
- When: 2026-11-27 16:52 (America/New_York)
- Source: Comick (all.comickfun v1.4.63)   (omitted for App kind)
- Seen: 1× in the last 7 days

```
<full stack trace, unfolded>
```
````

- App name from `AppConfig`/`appVersion()`. The device line needs a small `expect fun deviceDescription(): String`.
- **Copy**: `LocalClipboard.setClipEntry`, snackbar "Report copied".
- **Share (Android)**: reuse `UpdateCheckReportSharer.share(text)` (`ACTION_SEND`). Add `EXTRA_SUBJECT` "MangaWorld error: <source> <ShortType>". Either extend the interface with an optional subject or add a sibling sharer; this is decided during planning.
- **Save… (desktop)**: FileKit save dialog, default name `MangaWorld-error-<date>-<source>.md`, writes the report, snackbar "Saved".
- App name follows the running app (MangaWorld / AnimeWorld / NovelWorld).

## Source ▾ menu

Hidden for `App` kind (no source). Items, with the suggested item first and marked "Suggested":

| Item | Android | Desktop | Suggested for |
|---|---|---|---|
| Open website | `Screen.WebViewScreen(baseUrl)` (passes Cloudflare) | `LocalUriHandler.openUri(baseUrl)` | Blocked, Parsing, Network |
| Check for extension update | `Screen.ExtensionListScreen` | same | Source |
| Browse source | switch current source → `Screen.RecentScreen` | same | — |
| Source settings | `Screen.SourcePreferences(serviceName)` | same | — |

An item is hidden when its data is missing: no base URL, or extension not installed (except "Check for extension update", which still opens the list).

## Delete and Undo

- Delete (row, detail, ⌫, right-click): the ViewModel adds the time to `pendingDeletes`, and the list filters those out. That gives the slide-out (`animateItem` fade-out + placement) and the gap closes.
- Snackbar "Exception deleted · Undo". Undo removes it from `pendingDeletes` and the item animates back in. On dismiss without Undo → `exceptionDao.deleteException(item)`.
- Deleting from the single-pane detail page: navigate back first, then the row slides out.
- Two-pane: after delete, select the next row (or the previous one if it was last), or show the empty detail.
- Pending deletes are committed in `onCleared` so leaving the screen doesn't lose them.

## Desktop extras (jvmMain or common with `isDesktop` guards)

- Pane focusable. `onPreviewKeyEvent`: ↑/↓ select, ⌫/Delete delete, ⌘C/Ctrl+C copy report, ⌘Z/Ctrl+Z undo.
- Right-click on a row: `ContextMenuArea` with Copy report, Save report…, Open website, Delete.
- Shortcut hint bar at the bottom of the timeline (two-pane only).

## Testing

- `ParsedExceptionTest` (jvmTest): parsing of the 5 sample traces from the mockup plus an App error and a multi-cause trace. Covers type/message split, frames, library folding, httpCode, kind, extension ref, grouping key.
- `ExceptionReportTest`: report text for a source error and an App error.
- `ExceptionViewModelTest` (extend the existing one): pending delete + undo, commit on dismiss, commit on clear, selection moves to next after delete, AI task reuse per grouping key, known errors never start AI (use `FakeAiText`). Tear down with `clearAndJoin()` before `resetMain`, and use a single-thread Main dispatcher.
- Compose UI test: More info → detail shows headline; back returns; delete then Undo restores the row.
- Manual: Android phone (shared-element flight, predictive back), tablet/unfolded (two-pane), desktop wide + narrow window, keyboard + right-click, Share sheet, Save dialog, AI with Gemma (emulator) and Ollama (desktop).

## Risks

- `NavigableListDetailPaneScaffold` + `SharedTransitionLayout`: confirm that `AnimatedPane`'s scope works for `sharedElement` in the pinned adaptive version. A plain fade is NOT an acceptable fallback (user decision). See "Shared-element gate" below.

## Shared-element gate (first plan task)

Before building the screen, spike the flight: a minimal `NavigableListDetailPaneScaffold` in `ExceptionsScreen` with one row and one detail, `sharedBounds` + `sharedElement` via the `AnimatedPane` scope. Check it on the Android emulator (single pane, including predictive back) and on desktop with a narrow window.

- **Works** → continue with Option A as written above.
- **Doesn't work** (no flight, wrong scope type, jumps, or breaks predictive back) → switch to **Option B, Nav3 keys**:
  - `Screen.ExceptionScreen` becomes `ListDetailSceneStrategy.listPane()`.
  - New `@Serializable data class Screen.ExceptionDetail(val time: Long)` with `ListDetailSceneStrategy.detailPane()`.
  - Register both in `kmpuiviews/.../Nav3Graph.kt` and the Android `UIViews/.../Nav3Graph.kt`. Add the new key to the nav-map overrides / `RepoGraphTest`.
  - Shared elements use the app's existing `SharedTransitionLayout` (`HomeNav.kt`, `LocalSharedElementScope`) with `LocalNavAnimatedContentScope.current` as the visibility scope, the same pattern `Nav3View.kt:84` already uses.
  - More info → `navigationActions.navigate(Screen.ExceptionDetail(time))`. Two-pane selection = navigate with replace of the current detail entry.
  - Known trade-off: on a wide window, opening Exceptions hides the Settings list.
  - Everything else in this spec (parsing, AI, report, menu, delete, desktop extras) is unchanged.
- Extension frame detection only covers Mihon/Aniyomi packages. Other plugin sources fall into `App`.
- Large traces (JsonDecodingException embeds HTML input): truncate the message to 2 lines in rows, full text on the detail page.
