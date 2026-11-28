# Exceptions Screen Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the raw stack-trace list at Settings → Diagnostics → Exceptions with a day timeline + AI digest and a detail page (plain headline, facts, explanation, stack, Copy/Share/Save, Source menu), on Android and desktop.

**Architecture:** Pure parsing/classification/report code in `domain/exceptions` (unit-tested). `ExceptionViewModel` turns `ExceptionItem`s into rows, day groups, hourly counts, pending deletes and AI tasks. `ExceptionsScreen` hosts a `NavigableListDetailPaneScaffold` (Option A) inside the existing `twoPaneEntry`, with shared elements in single-pane mode. Task 1 is a gate: if shared elements don't work in the scaffold, Task 1B switches to Nav3 keys (Option B).

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material3 adaptive (`NavigableListDetailPaneScaffold`), Navigation3, Koin, Room 3 (KMP), kotlinx-datetime, FileKit, `AiText`/`AiTask`.

**Spec:** `docs/superpowers/specs/2026-11-27-exceptions-screen-overhaul-design.md` (approved mockup: https://claude.ai/artifact/Uaxnjdq5VZ58i6QHYwVaD5)

## Global Constraints

- No database schema change. Everything is derived from `ExceptionItem.message` (`stackTraceToString()` text).
- NO plain-fade fallback for the single-pane transition. If the shared-element flight can't work in the scaffold, use Option B (Task 1B).
- Known errors never call AI. AI is gated by `AiToggle.ErrorExplanations` through `AiText.availability(feature)`.
- The AI explanation is never part of Copy/Share/Save text.
- Delete all keeps its confirm dialog. Single deletes have no dialog: slide-out + Undo snackbar.
- Every lazy item and sticky header gets `Modifier.animateItem()`.
- UI strings go in `kmpuiviews/src/commonMain/composeResources/values/strings.xml` with the `exc_` prefix.
- Local builds use the `noFirebase` flavor. Debug package id `…noFirebase.debug`. Install on ONE device with `adb -s <serial> install -r <apk>` (Gradle `install*` hits every device).
- VM tests: single-thread Main (`Dispatchers.setMain(newSingleThreadContext(...))`), `viewModelStore.clearAndJoin()` before `resetMain`, join the `onCleared` GlobalScope job, no sleeps or raised timeouts.
- Desktop must work for every feature in this plan.

## Review Focus

1. **Wrapped exceptions** (`java.lang.Exception: …` whose `Caused by:` is a `SocketTimeoutException`): classify by the cause. Covered in Task 2 test `wrappedTimeoutUsesCause`.
2. **Huge messages** (JsonDecodingException embedding a full HTML page): rows show at most 2 lines, and the detail shows all of it, selectable, without layout blowups. Covered in Task 2 `messageKeepsContinuationLines` and Task 7 row `maxLines = 2`.
3. **Extension not installed anymore** (frame points at an uninstalled package): show the id (`en.mangadex`), no version, and Source ▾ shows only "Check for extension update". Covered in Task 5 `uninstalledExtensionFallsBackToId`.
4. **Leaving the screen with an Undo snackbar up**: the delete is still committed. Covered in Task 5 `pendingDeleteCommittedOnClear`.
5. **Empty table and "no errors in last 24 h"**: empty state shows, digest card hidden, two-pane detail shows the empty placeholder. Covered in Task 5 `hourlyCountsEmpty` and Task 7 empty-state composable.

---

### Task 1: Shared-element gate (spike)

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/exceptions/ExceptionsScreen.kt`

**Interfaces:**
- Produces: a decision recorded at the top of this plan file under `## Gate result` ("Option A" or "Option B"). Later tasks read it.

- [ ] **Step 1: Build the spike.** Replace the body of `ExceptionsScreen` temporarily with: `BoxWithConstraints` → `SharedTransitionLayout` → `NavigableListDetailPaneScaffold(navigator = rememberListDetailPaneScaffoldNavigator<Long>(), directive = navigator.scaffoldDirective.copy(maxHorizontalPartitions = if (maxWidth >= 840.dp) 2 else 1))`. List pane: `AnimatedPane { }` with a `LazyColumn` of the real `exceptions` (first line of message as title). Each row's `Text` uses `Modifier.sharedElement(rememberSharedContentState("exception-$time-title"), animatedVisibilityScope = this@AnimatedPane)` and the row uses `sharedBounds(…"exception-$time")`. Detail pane: same keys on a 22sp headline and a full-size `Surface`. Row click → `navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, time)`.
- [ ] **Step 2: Compile.** Run `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug`. Expected: BUILD SUCCESSFUL. If `AnimatedPane`'s content receiver isn't an `AnimatedVisibilityScope`, that's a FAIL for the gate.
- [ ] **Step 3: Check on Android.** Install on the emulator, open Settings → Diagnostics → Exceptions (insert a fake row first via `adb shell` sqlite or by forcing an error). Record with `adb shell screenrecord`, with animator scale 5x. Pass = the title and the box visibly fly between positions both ways, and predictive back (`adb shell input` swipe from the left edge, or `motionevent`) scrubs the flight back.
- [ ] **Step 4: Check on desktop.** `./gradlew :mangaworld:desktop:run`, narrow window (< 840dp Exceptions pane), same pass criteria. Then a wide window: two panes side by side, no crash.
- [ ] **Step 5: Record the result** as `## Gate result: Option A` or `## Gate result: Option B — <what failed>` right under the Review Focus section of this file. Revert the spike body (`git checkout -- ExceptionsScreen.kt`). Commit only the plan file change: `git commit -m "docs(exceptions): record shared-element gate result"`.

### Task 1B: Option B navigation (ONLY if the gate result is Option B)

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt` (add `@Serializable data class ExceptionDetail(val time: Long) : Screen("exception_detail")` next to `ExceptionScreen`)
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt:350`
- Modify: `UIViews/src/main/java/com/programmersbox/uiviews/presentation/navigation/Nav3Graph.kt:231`
- Modify: nav-map override list and `RepoGraphTest` (find them with `grep -rn "ExceptionScreen" navgraph kmpuiviews/src/*Test`)
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsScreenDisplayNames.kt`

**Interfaces:**
- Produces: `Screen.ExceptionDetail(time: Long)`. Entries: `navigation<Screen.ExceptionScreen>(metadata = ListDetailSceneStrategy.listPane())` and `navigation<Screen.ExceptionDetail>(metadata = ListDetailSceneStrategy.detailPane()) { ExceptionDetailScreen(it.time) }`. Each entry gets its own `ExceptionViewModel`. Pending deletes move into a Koin `single { PendingExceptionDeletes() }` (`MutableStateFlow<Set<Long>>` + `add/remove`) injected into `ExceptionViewModel`, so a delete started on the detail entry hides the row in the list entry.
- Shared elements: `LocalSharedElementScope.current` (from `HomeNav.kt`) + `LocalNavAnimatedContentScope.current`, as in `Nav3View.kt:84`.

- [ ] **Step 1: Write the failing graph test.** In `RepoGraphTest` (or the existing nav-map test), assert that `Screen.ExceptionDetail` is registered and reachable from `Screen.ExceptionScreen`.
- [ ] **Step 2: Run it.** `./gradlew :kmpuiviews:jvmTest --tests "*RepoGraphTest*"`. Expected: FAIL.
- [ ] **Step 3: Add the key, the entries, the overrides and the display name** ("Exception").
- [ ] **Step 4: Run it.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -m "feat(exceptions): add ExceptionDetail nav key"`

In later tasks under Option B, read "navigator.navigateTo(Detail, time)" as `navActions.navigate(Screen.ExceptionDetail(time))`, read "two-pane selection" as navigating with the current `ExceptionDetail` entry replaced, and drop the in-screen scaffold.

### Task 2: Parse and classify exceptions

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/exceptions/ParsedException.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/updatecheck/KnownError.kt` (make `offlineRegex` `internal`)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/exceptions/ParsedExceptionTest.kt`

**Interfaces:**
- Produces:
```kotlin
data class ParsedException(
    val type: String, val message: String, val frames: List<Frame>, val causes: List<String>,
    val extension: ExtensionRef?, val kind: ExceptionKind, val headline: ExceptionHeadline, val httpCode: Int?,
) { val shortType: String; val firstLine: String; val groupKey: String }
data class Frame(val text: String, val isLibrary: Boolean)
data class ExtensionRef(val packageName: String, val lang: String, val name: String) { val id: String } // "lang.name"
enum class ExceptionKind { Network, Blocked, Parsing, Source, App }
enum class ExceptionHeadline { TimedOut, Offline, Blocked, RateLimited, NotFound, ServerError, Network, Parsing, Source, App }
fun parseException(text: String): ParsedException
```

Rules (exact):
- Line 1: `type` = text before the first `": "` when that prefix has no spaces, else the whole line. `message` = the rest of line 1 plus following lines until the first line whose trim starts with `at `, `Caused by: ` or `... `.
- `frames` = trimmed lines starting `at ` (the `at ` prefix is kept). `isLibrary` when the text after `at ` starts with one of `java.`, `javax.`, `kotlin.`, `kotlinx.`, `android.`, `androidx.`, `okhttp3.`, `okio.`, `org.jsoup.`, `dalvik.`, `sun.`, `jdk.`, `com.android.`.
- `causes` = trimmed lines starting `Caused by: `.
- `extension` = first match in any frame of `eu\.kanade\.tachiyomi\.(extension|animeextension)\.([a-z]+)\.([a-z0-9]+)\.` → `packageName = "eu.kanade.tachiyomi.$1.$2.$3"`.
- `httpCode` = first match in line 1 or any cause of `Status=(\d{3})`, `HTTP (\d{3})` or `HTTP error (\d{3})`.
- Classification checks line 1 and every cause line. Headline, first match wins: 429 → RateLimited; 403/503 → Blocked; 404/410 → NotFound; other 5xx → ServerError; `SocketTimeoutException` or (`InterruptedIOException` + "timeout") → TimedOut; `offlineRegex` → Offline; `SSLException`, `ConnectException`, `java.net.` types or any other httpCode → Network; `SerializationException`, `JsonDecodingException`, `JsonSyntaxException`, `MalformedJsonException`, `Selector` → Parsing; has extension → Source; else App.
- Kind from headline: RateLimited/Blocked → Blocked; NotFound/ServerError/TimedOut/Offline/Network → Network; Parsing → Parsing; Source → Source; App → App.
- `groupKey = "$type|$firstLine|${extension?.id}"` where `firstLine = message.lineSequence().first()`.

- [ ] **Step 1: Write the failing tests.** Fixtures are the five mockup traces (copy the `EX` entries from the mockup page: type + msg + frames joined as `stackTraceToString()` text with `\n\tat `), plus one App trace (`java.io.FileNotFoundException: /backup.zip` with only `com.programmersbox` frames) and a wrapped one.
```kotlin
@Test fun timeoutIsNetwork() { val p = parseException(mangaDexTimeout); assertEquals(ExceptionHeadline.TimedOut, p.headline); assertEquals(ExceptionKind.Network, p.kind); assertEquals("en.mangadex", p.extension?.id); assertEquals("SocketTimeoutException", p.shortType) }
@Test fun cloudflare403IsBlocked() { val p = parseException(asura403); assertEquals(403, p.httpCode); assertEquals(ExceptionHeadline.Blocked, p.headline) }
@Test fun htmlInsteadOfJsonIsParsing() { val p = parseException(comickJson); assertEquals(ExceptionKind.Parsing, p.kind); assertEquals("eu.kanade.tachiyomi.extension.all.comickfun", p.extension?.packageName) }
@Test fun messageKeepsContinuationLines() { assertTrue(parseException(comickJson).message.contains("JSON input: <!DOCTYPE html>")) }
@Test fun emptyChapterListIsSource() { val p = parseException(weebCentral); assertEquals(ExceptionKind.Source, p.kind); assertEquals(listOf("Caused by: java.util.NoSuchElementException: Collection is empty."), p.causes) }
@Test fun unknownHostIsOffline() { assertEquals(ExceptionHeadline.Offline, parseException(unknownHost).headline) }
@Test fun noExtensionFrameIsApp() { val p = parseException(backupFailure); assertEquals(ExceptionKind.App, p.kind); assertNull(p.extension) }
@Test fun wrappedTimeoutUsesCause() { assertEquals(ExceptionHeadline.TimedOut, parseException("java.lang.Exception: load failed\n\tat a.b(C.kt:1)\nCaused by: java.net.SocketTimeoutException: timeout").headline) }
@Test fun libraryFramesFlagged() { val f = parseException(mangaDexTimeout).frames; assertFalse(f.first().isLibrary); assertTrue(f.last().isLibrary) }
@Test fun groupKeyStable() { assertEquals(parseException(mangaDexTimeout).groupKey, parseException(mangaDexTimeout).groupKey); assertNotEquals(parseException(mangaDexTimeout).groupKey, parseException(unknownHost).groupKey) }
@Test fun singleLineNoFrames() { val p = parseException("boom"); assertEquals("boom", p.type); assertEquals("", p.message); assertEquals(ExceptionKind.App, p.kind) }
```
- [ ] **Step 2: Run.** `./gradlew :kmpuiviews:jvmTest --tests "*ParsedExceptionTest*"`. Expected: FAIL (unresolved `parseException`).
- [ ] **Step 3: Implement** `ParsedException.kt` per the rules above.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -m "feat(exceptions): parse and classify stored exceptions"`

### Task 3: Report text, explanations and grouping helpers

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/exceptions/ExceptionReport.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/exceptions/ExceptionReportTest.kt`

**Interfaces:**
- Consumes: `ParsedException` (Task 2), `KnownError` (existing).
- Produces:
```kotlin
data class ReportContext(val appName: String, val appVersion: String, val device: String, val timeZone: TimeZone)
fun buildExceptionReport(rawText: String, parsed: ParsedException, time: Long, sourceName: String?, extensionVersion: String?, seenThisWeek: Int, ctx: ReportContext): String
fun reportSubject(appName: String, sourceName: String?, parsed: ParsedException): String   // "MangaWorld error: Comick JsonDecodingException" / "MangaWorld error: App FileNotFoundException"
fun reportFileName(appName: String, time: Long, sourceName: String?, timeZone: TimeZone): String // "MangaWorld-error-2026-11-27-Comick.md", spaces removed; "App" when no source
fun ParsedException.knownError(): KnownError?   // TimedOut→TimedOut, Offline→Offline, Blocked→Blocked, RateLimited→RateLimited, NotFound→NotFound, ServerError→ServerError, else null
fun hourlyCounts(times: List<Long>, now: Long): List<Int>   // 24 buckets, index 23 = the hour ending now; older than 24 h ignored
fun seenCounts(items: List<Pair<Long, String>>, now: Long): Map<String, Int> // groupKey → count with time >= now - 7 days
```
- Report format (exact; the "Source:" line is omitted when `sourceName == null`; the "(… vX)" part is omitted when the version is null; `rawText` goes in the fence unchanged):
````
### {appName} error report
- App: {appName} {appVersion}
- Device: {device}
- When: {yyyy-MM-dd HH:mm} ({timeZone.id})
- Source: {sourceName} ({extension.id} v{extensionVersion})
- Seen: {seenThisWeek}× in the last 7 days

```
{rawText}
```
````

- [ ] **Step 1: Write the failing tests.**
```kotlin
@Test fun reportForSourceError() { val r = buildExceptionReport(comickJson, parseException(comickJson), time = t20261127_1652, "Comick", "1.4.63", 1, ReportContext("MangaWorld", "33.0.0", "Android 16 · Pixel 8", TimeZone.of("America/New_York")))
  assertTrue(r.startsWith("### MangaWorld error report\n- App: MangaWorld 33.0.0\n- Device: Android 16 · Pixel 8\n- When: 2026-11-27 16:52 (America/New_York)\n- Source: Comick (all.comickfun v1.4.63)\n- Seen: 1× in the last 7 days\n\n```\n")); assertTrue(r.endsWith("\n```")) }
@Test fun reportForAppErrorHasNoSourceLine() { assertFalse(buildExceptionReport(backupFailure, parseException(backupFailure), t, null, null, 2, ctx).contains("- Source:")) }
@Test fun subjectAndFileName() { assertEquals("MangaWorld error: Asura Scans HttpStatusException", reportSubject("MangaWorld", "Asura Scans", parseException(asura403))); assertEquals("MangaWorld-error-2026-11-27-AsuraScans.md", reportFileName("MangaWorld", t20261127_1652, "Asura Scans", ny)) }
@Test fun knownErrorMapping() { assertEquals(KnownError.TimedOut, parseException(mangaDexTimeout).knownError()); assertNull(parseException(comickJson).knownError()) }
@Test fun hourlyCountsBuckets() { val now = 10 * HOUR; val c = hourlyCounts(listOf(now - 1, now - 1, now - HOUR - 1, now - 25 * HOUR), now); assertEquals(24, c.size); assertEquals(2, c[23]); assertEquals(1, c[22]); assertEquals(3, c.sum()) }
@Test fun hourlyCountsEmpty() { assertEquals(List(24) { 0 }, hourlyCounts(emptyList(), 1000L)) }
@Test fun seenCountsWeekWindow() { assertEquals(mapOf("k" to 2), seenCounts(listOf(now - DAY to "k", now - 2 * DAY to "k", now - 8 * DAY to "k"), now)) }
```
Move the trace fixtures from Task 2 into `kmpuiviews/src/jvmTest/.../domain/exceptions/ExceptionFixtures.kt` so both tests share them.
- [ ] **Step 2: Run.** `./gradlew :kmpuiviews:jvmTest --tests "*ExceptionReportTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement** `ExceptionReport.kt`.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -m "feat(exceptions): report text, known errors and counts"`

### Task 4: AI features

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/aitext/AiFeature.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/aitext/AiFeatureTest.kt`

**Interfaces:**
- Produces:
  - `AiFeature.ExplainException : AiFeature<ErrorDetails>`, toggle `AiToggle.ErrorExplanations`. System prompt: "You explain an error from a manga, anime or novel reading app. The reader is not technical. In 2 sentences, say what most likely went wrong and one thing they can try. Don't mention code, exceptions or stack traces. Return only the explanation." `prepare` = `"Source: ${input.sourceName}\nError:\n${input.detail.take(MAX_ERROR_DETAIL_CHARS)}"`.
  - `AiFeature.ExceptionDigest : AiFeature<List<String>>`, toggle `AiToggle.ErrorExplanations`. System prompt: "You summarize recent errors from a manga, anime or novel reading app for a non-technical reader. In 2 or 3 sentences, say what the errors have in common and what to do. Return only the summary." `prepare` = the first 20 lines, each `- ` prefixed, joined by `\n`.
  - `telemetryName`: `explain_exception`, `exception_digest`.

- [ ] **Step 1: Write the failing tests.**
```kotlin
@Test fun explainExceptionPrepareTrims() { val p = AiFeature.ExplainException.prepare(ErrorDetails("Comick", "x".repeat(2000))); assertTrue(p.startsWith("Source: Comick\nError:\n")); assertEquals(1500, p.substringAfter("Error:\n").length) }
@Test fun digestTakesTwenty() { val p = AiFeature.ExceptionDigest.prepare(List(30) { "line $it" }); assertEquals(20, p.lines().size); assertEquals("- line 0", p.lines().first()) }
@Test fun exceptionFeaturesUseErrorToggle() { assertEquals(AiToggle.ErrorExplanations, AiFeature.ExplainException.toggle); assertEquals(AiToggle.ErrorExplanations, AiFeature.ExceptionDigest.toggle) }
@Test fun telemetryNames() { assertEquals("explain_exception", AiFeature.ExplainException.telemetryName); assertEquals("exception_digest", AiFeature.ExceptionDigest.telemetryName) }
```
- [ ] **Step 2: Run.** `./gradlew :kmpuiviews:jvmTest --tests "*AiFeatureTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement** both features and the `telemetryName` branches.
- [ ] **Step 4: Run.** Expected: PASS (the existing `AiFeatureTest`/`AiSettingsTest` cases still pass).
- [ ] **Step 5: Commit.** `git commit -m "feat(ai): exception explanation and digest features"`

### Task 5: ExceptionViewModel

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/exceptions/ExceptionViewModel.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt:111`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/exceptions/ExceptionViewModelTest.kt`

**Interfaces:**
- Consumes: Tasks 2–4, `SourceRepository` (`list`, `displayNameFor`), `AiText`, `AiTask`, `AiTextState`.
- Produces:
```kotlin
data class ExceptionRow(val item: ExceptionItem, val parsed: ParsedException, val source: KmpSourceInformation?, val sourceName: String?, val extensionVersion: String?, val seenThisWeek: Int) { val time: Long get() = item.time }
data class ExceptionDay(val date: LocalDate, val rows: List<ExceptionRow>)
class ExceptionViewModel(
    exceptionDao: ExceptionDao, sourceRepository: SourceRepository, aiText: AiText,
    versionName: (KmpSourceInformation) -> String?, now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    val rows: StateFlow<List<ExceptionRow>>          // newest first, pending deletes filtered out
    val days: StateFlow<List<ExceptionDay>>          // grouped by local date, newest first
    val hourly: StateFlow<List<Int>>                 // hourlyCounts over rows
    val explainAvailable: StateFlow<Boolean>         // AiText.availability(ExplainException) != null
    val digestAvailable: StateFlow<Boolean>          // AiText.availability(ExceptionDigest) != null
    val digest: StateFlow<AiTextState>
    fun row(time: Long): ExceptionRow?
    fun delete(time: Long)                           // adds to pending
    fun undo(time: Long); fun undoLast()
    fun commitDelete(time: Long)                     // dao.deleteException, removes from pending
    fun deleteAll()
    fun nextAfterDelete(time: Long): Long?           // next row's time, else previous, else null (computed before removal)
    fun explanation(row: ExceptionRow): StateFlow<AiTextState>   // one AiTask per parsed.groupKey
    fun explain(row: ExceptionRow)                   // no-op when parsed.knownError() != null or already running/done
    fun summarize()                                  // ExceptionDigest over last-24h rows: "${sourceName ?: "App"}: ${parsed.shortType} ${parsed.firstLine.take(80)}"
    internal var clearedDeletes: Job?                // onCleared commits pending via GlobalScope (HistoryViewModel pattern)
}
```
- Source lookup: `sourceRepository.list.firstOrNull { it.packageName == parsed.extension?.packageName }`. `sourceName` = that source's `name`, else `extension.id`, else null. `extensionVersion` = `versionName(source)` when found.
- Koin: `viewModel { ExceptionViewModel(get(), get(), get(), versionName = get<SourceInfoRepository>()::versionName) }`.

- [ ] **Step 1: Write the failing tests** (reuse the existing Room setup; switch Main to `newSingleThreadContext`; `FakeAiText` from `testing/`; teardown `viewModelStore.clearAndJoin()` then join `clearedDeletes`, then `resetMain`).
```kotlin
@Test fun rowsParsedAndNewestFirst()            // insert 2 fixtures, rows.map { it.parsed.headline } == [newer, older]
@Test fun daysGroupedByLocalDate()              // now = 2026-11-27 12:00 NY, items today ×2 + yesterday ×1 → days.size == 2, days[0].rows.size == 2
@Test fun deleteHidesThenUndoRestores()         // delete(t) → t !in rows; undo(t) → t in rows; dao still has t
@Test fun commitDeleteRemovesFromDao()          // delete(t); commitDelete(t) → dao.getAllExceptions().first() has no t
@Test fun pendingDeleteCommittedOnClear()       // delete(t); clearAndJoin(); clearedDeletes!!.join() → dao has no t
@Test fun nextAfterDeletePicksNextThenPrevious() // rows [a,b,c]: next(b)==c, next(c)==b, single row → null
@Test fun knownErrorNeverStartsAi()             // explain(timeoutRow) → FakeAiText call count 0, explanation state Idle
@Test fun aiTaskSharedPerGroupKey()             // two rows same groupKey → explain both → 1 call, same StateFlow instance
@Test fun uninstalledExtensionFallsBackToId()   // no source in repo → sourceName == "en.mangadex", extensionVersion == null, source == null
@Test fun installedExtensionUsesNameAndVersion() // repo has packageName match named "MangaDex", versionName returns "1.4.61"
@Test fun summarizeUsesLast24h()                // rows at now-1h and now-30h → FakeAiText input list size 1
```
- [ ] **Step 2: Run.** `./gradlew :kmpuiviews:jvmTest --tests "*ExceptionViewModelTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement** the ViewModel and the Koin change. Follow `UpdateCheckErrorsViewModel.explain` for AiTask start/fail handling, and `HistoryViewModel.onCleared` for `clearedDeletes`.
- [ ] **Step 4: Run.** Expected: PASS. Run it 5 times in a row (`--rerun-tasks`) to check for flakiness.
- [ ] **Step 5: Commit.** `git commit -m "feat(exceptions): view model with parsing, undo and AI"`

### Task 6: Platform pieces (share subject, save, device, website)

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/updatecheck/UpdateCheckReportSharer.kt` → `fun share(text: String, subject: String? = null): Boolean`
- Modify: `…/androidMain/…/updatecheck/UpdateCheckReportSharer.android.kt` (put `EXTRA_SUBJECT` when non-null), `…/jvmMain/…/UpdateCheckReportSharer.jvm.kt`, `…/iosMain/…/UpdateCheckReportSharer.ios.kt`, test fake in `UpdateCheckErrorsViewModelTest.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/exceptions/ExceptionPlatform.kt` + `androidMain`/`jvmMain`/`iosMain` actuals

**Interfaces:**
- Produces:
```kotlin
expect val hasShareSheet: Boolean          // android true, jvm false, ios false
expect fun deviceDescription(): String     // android "Android ${Build.VERSION.RELEASE} · ${Build.MODEL}", jvm "${os.name} ${os.version} · ${os.arch}", ios platform()
```
- Desktop Save… uses `rememberFileSaverLauncher` (as `OtakuListView.kt:319`) with `suggestedName = reportFileName(...)` minus `.md`, `extension = "md"`, and writes the report to the returned file.
- Open website: `if (hasShareSheet) navActions.webView(baseUrl) else LocalUriHandler.current.openUri(baseUrl)`, where `baseUrl = source.apiService.baseUrl`.

- [ ] **Step 1: Write the failing test.** Extend the existing `UpdateCheckErrorsViewModelTest.FakeSharer` to record `subject`; add `@Test fun shareSubjectDefaultsNull()` asserting the update-check share passes `null`. Add `@Test fun deviceDescriptionNotBlank()` in `jvmTest` (`assertTrue(deviceDescription().contains("·"))`).
- [ ] **Step 2: Run.** `./gradlew :kmpuiviews:jvmTest --tests "*UpdateCheckErrorsViewModelTest*" --tests "*ExceptionPlatformTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement** the interface change, the actuals and the expects.
- [ ] **Step 4: Run** the same tests plus `./gradlew :mangaworld:assembleNoFirebaseDebug`. Expected: PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Commit.** `git commit -m "feat(exceptions): share subject, device line, platform flags"`

### Task 7: Timeline (list pane)

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/exceptions/ExceptionTimeline.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/exceptions/ExceptionParts.kt` (shared pieces: `KindBadge`, `kindColor`, `headlineText`, `ExplanationBlock`, `StackTraceBlock`)
- Modify: `kmpuiviews/src/commonMain/composeResources/values/strings.xml`

**Interfaces:**
- Consumes: Task 5 state, Task 3 `knownError()`, `KnownError.explanation(sourceName)`, `AiCard`/AI components from `presentation/components/ai/AiComponents.kt`.
- Produces:
```kotlin
@Composable fun ExceptionTimeline(days: List<ExceptionDay>, hourly: List<Int>, digest: AiTextState, digestAvailable: Boolean, explainAvailable: Boolean,
    twoPane: Boolean, selected: Long?, onSelect: (Long) -> Unit, onMoreInfo: (Long) -> Unit, onDelete: (Long) -> Unit, onCopy: (ExceptionRow) -> Unit,
    onSummarize: () -> Unit, explanation: (ExceptionRow) -> StateFlow<AiTextState>, onExplain: (ExceptionRow) -> Unit, modifier: Modifier = Modifier,
    sharedKeys: SharedKeys? = null)            // SharedKeys(scope: SharedTransitionScope, visibility: AnimatedVisibilityScope); null in two-pane
@Composable fun KindBadge(kind: ExceptionKind, modifier: Modifier = Modifier)
@Composable fun headlineText(row: ExceptionRow): String
@Composable fun StackTraceBlock(raw: ParsedException, modifier: Modifier = Modifier)   // library frames folded behind "N library frames hidden"
@Composable fun ExplanationBlock(row: ExceptionRow, state: AiTextState, available: Boolean, autoStart: Boolean, onExplain: () -> Unit)
```
- Strings (exact; `%1$s` = source name, or "A source" when null for network/blocked kinds):
  - `exc_headline_timed_out` "%1$s took too long to answer"
  - `exc_headline_offline` "No internet connection"
  - `exc_headline_blocked` "%1$s blocked the request"
  - `exc_headline_rate_limited` "%1$s got too many requests"
  - `exc_headline_not_found` "%1$s couldn't find that page"
  - `exc_headline_server_error` "%1$s had a server problem"
  - `exc_headline_network` "Couldn't reach %1$s"
  - `exc_headline_parsing` "%1$s sent something the app couldn't read"
  - `exc_headline_source` "%1$s ran into a problem"
  - `exc_headline_app` "Something went wrong in the app"
  - `exc_kind_network` "Network", `exc_kind_blocked` "Blocked", `exc_kind_parsing` "Parsing", `exc_kind_source` "Source", `exc_kind_app` "App"
  - `exc_last_24h` "Last 24 hours", `exc_counts` "%1$d errors · %2$d sources", `exc_summarize` "Summarize recent errors", `exc_explain_one` "Explain this one", `exc_what_this_means` "What this means", `exc_known_error` "known error", `exc_what_it_might_mean` "What this might mean", `exc_can_be_wrong` "can be wrong", `exc_more_info` "More info", `exc_copy` "Copy", `exc_delete` "Delete", `exc_frames_hidden` "%1$d library frames hidden", `exc_day_count` "%1$d errors", `exc_today` "Today", `exc_yesterday` "Yesterday", `exc_empty_title` "No exceptions", `exc_empty_body` "Errors from sources and the app show up here so you can see what went wrong."
- Visuals (match the mockup tab "D + C detail"): digest card, hidden when `hourly.sum() == 0`, with the ✦ chip only when `digestAvailable` (`Canvas` 24 bars, each bar's height animates in with `animateFloatAsState`, delay `index * 18ms`; zero-count bars at 25% alpha); `stickyHeader` per day; rail + kind-colored ring dot via `drawBehind`; row title `maxLines = 2`; class name in `FontFamily.Monospace`. Single-pane: tap toggles `AnimatedVisibility(expandVertically() + fadeIn())` → `ExplanationBlock(autoStart = false)`, `StackTraceBlock`, actions Copy / Delete / More info (`FilledTonalButton`). Two-pane: tap = `onSelect`, selected row background `secondaryContainer`, no expand.
- Kind colors: Network = `tertiary`, Blocked = a warning tone (`Color(0xFF8A5A00)` light / `Color(0xFFF7BD52)` dark, dark when `MaterialTheme.colorScheme.surface.luminance() < 0.5f`), Parsing = `error`, Source/App = `onSurfaceVariant`.

- [ ] **Step 1: Write a failing Compose UI test** `kmpuiviews/src/jvmTest/.../exceptions/ExceptionTimelineTest.kt` with `runComposeUiTest`: render `ExceptionTimeline` with the fixture rows in single-pane mode; assert text "MangaDex took too long to answer" exists; click it; assert "More info" is displayed; click "More info" → callback received the right time. Second test: empty `days` → "No exceptions" shown and "Last 24 hours" absent.
- [ ] **Step 2: Run.** `./gradlew :kmpuiviews:jvmTest --tests "*ExceptionTimelineTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement** `ExceptionParts.kt`, `ExceptionTimeline.kt` and the strings.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -m "feat(exceptions): timeline with digest and expandable rows"`

### Task 8: Detail pane, scaffold and shared elements

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/exceptions/ExceptionDetail.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/exceptions/SourceActionsMenu.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/exceptions/ExceptionsScreen.kt` (full rewrite)
- Modify: `strings.xml`

**Interfaces:**
- Consumes: Tasks 3, 5, 6, 7.
- Produces:
```kotlin
@Composable fun ExceptionDetail(row: ExceptionRow, explanation: AiTextState, explainAvailable: Boolean, twoPane: Boolean,
    onBack: () -> Unit, onDelete: () -> Unit, onCopy: () -> Unit, onShareOrSave: () -> Unit, onExplain: () -> Unit,
    onSourceAction: (SourceAction) -> Unit, sharedKeys: SharedKeys?, modifier: Modifier = Modifier)
enum class SourceAction { OpenWebsite, CheckUpdate, Browse, Settings }
fun suggestedAction(kind: ExceptionKind): SourceAction?   // Blocked/Parsing/Network → OpenWebsite, Source → CheckUpdate, App → null
fun availableActions(row: ExceptionRow): List<SourceAction> // App → empty; source null → [CheckUpdate]; else all four, suggested first
@Composable fun SourceActionsMenu(row: ExceptionRow, expanded: Boolean, onDismiss: () -> Unit, onAction: (SourceAction) -> Unit)
```
- Shared keys: row container `sharedBounds("exception-$time")`, title `sharedElement("exception-$time-title")`, badge `…-badge`, time `…-time`. Same keys on the detail header. Applied only when `sharedKeys != null` (single-pane).
- Detail layout: header (badge, 22sp headline, "Today at 4:52 PM" with the time part shared); facts grid (2 columns when `twoPane == false`, else 4): Source / Extension (mono) / Error (`shortType`) / Seen ("%1$d× this week"); `ExplanationBlock(autoStart = true)`; "Stack trace" + `StackTraceBlock` in `SelectionContainer` + `horizontalScroll`. Non-header content uses `animateEnterExit(fadeIn(tween(250, delayMillis = 120)) + slideInVertically { it / 12 })` inside the pane scope.
- Actions: single-pane = bottom bar Copy · Share (`hasShareSheet`) / Save… (else) · Source ▾ (hidden when `availableActions` is empty); top bar back + delete. Two-pane = these buttons at the top right plus a delete icon.
- Source action handlers in `ExceptionsScreen`: OpenWebsite → Task 6 rule; CheckUpdate → `navActions.extensionList()`; Browse → `dataStoreHandling.currentService.set(source.apiService.serviceName)` then `navActions.recent()`; Settings → `navActions.navigate(Screen.SourcePreferences(source.apiService.serviceName))`.
- `ExceptionsScreen` (Option A): `BoxWithConstraints { val twoPane = maxWidth >= 840.dp }` → `SharedTransitionLayout` → `NavigableListDetailPaneScaffold(navigator, directive = navigator.scaffoldDirective.copy(maxHorizontalPartitions = if (twoPane) 2 else 1), listPane = { AnimatedPane { ExceptionTimeline(…, sharedKeys = if (twoPane) null else SharedKeys(this@SharedTransitionLayout, this@AnimatedPane)) } }, detailPane = { AnimatedPane { navigator.currentDestination?.contentKey?.let(vm::row)?.let { ExceptionDetail(…) } ?: EmptyDetail() } })`. Two-pane: the detail content is wrapped in `AnimatedContent(targetState = time, transitionSpec = { fadeIn(tween(220)) + slideInVertically { 24 } togetherWith fadeOut(tween(120)) })`. Two-pane with nothing selected: auto-select the first row.
- Strings: `exc_share` "Share", `exc_save` "Save…", `exc_source_menu` "Source", `exc_suggested` "Suggested", `exc_open_website` "Open website", `exc_open_website_desc_android` "Opens the site in the in-app WebView. Passes Cloudflare checks and shows if the site is up.", `exc_open_website_desc_desktop` "Opens the site in your browser to see if it is up.", `exc_check_update` "Check for extension update", `exc_check_update_desc` "Opens Extensions with this extension.", `exc_browse` "Browse source", `exc_browse_desc` "Switches to this source and opens Recent.", `exc_source_settings` "Source settings", `exc_source_settings_desc` "Opens this source's preferences.", `exc_fact_source` "Source", `exc_fact_extension` "Extension", `exc_fact_error` "Error", `exc_fact_seen` "Seen", `exc_seen_week` "%1$d× this week", `exc_stack_trace` "Stack trace", `exc_report_copied` "Report copied", `exc_report_saved` "Saved", `exc_no_selection` "Pick an exception to see details", `exc_explain_blocked_desktop` "%1$s refused the request, often with a Cloudflare check. The desktop app can't pass that check yet. Open the website to see if the site is up, and try again later." (used instead of `uc_explain_blocked` when `!hasShareSheet`).

- [ ] **Step 1: Write the failing tests.** Pure: `SourceActionsTest` — `suggestedAction(ExceptionKind.Blocked) == OpenWebsite`, `suggestedAction(ExceptionKind.App) == null`, `availableActions(appRow).isEmpty()`, `availableActions(uninstalledRow) == listOf(CheckUpdate)`, `availableActions(sourceRow).first() == CheckUpdate`. UI: `ExceptionDetailTest` — render the Comick row single-pane: "Comick sent something the app couldn't read", "all.comickfun", "JsonDecodingException" and "Stack trace" displayed; clicking "Source" shows "Open website" and "Suggested".
- [ ] **Step 2: Run.** `./gradlew :kmpuiviews:jvmTest --tests "*SourceActionsTest*" --tests "*ExceptionDetailTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement** `ExceptionDetail.kt`, `SourceActionsMenu.kt`, and the `ExceptionsScreen` rewrite (Copy → `LocalClipboard.current.setClipEntry` with the report + snackbar `exc_report_copied`; Share → `sharer.share(report, reportSubject(...))`; Save → file saver + snackbar `exc_report_saved`). The report context comes from `AppConfig.appName`, `appVersion()`, `deviceDescription()`.
- [ ] **Step 4: Run** the tests and `./gradlew :kmpuiviews:jvmTest --tests "*Exception*"`. Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -m "feat(exceptions): detail pane, source menu and list-detail scaffold"`

### Task 9: Delete/Undo wiring and desktop keyboard + right-click

**Files:**
- Modify: `ExceptionsScreen.kt`, `ExceptionTimeline.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/exceptions/ExceptionKeys.kt`
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/exceptions/RowContextMenu.jvm.kt` + `androidMain`/`iosMain` actuals (pass-through)

**Interfaces:**
- Produces:
```kotlin
enum class ExceptionKeyAction { Up, Down, Delete, Copy, Undo }
fun exceptionKeyAction(event: KeyEvent): ExceptionKeyAction?   // KeyDown only; DirectionUp/Down; Backspace/Delete; (Meta|Ctrl)+C; (Meta|Ctrl)+Z
@Composable expect fun RowContextMenu(onCopy: () -> Unit, onSave: () -> Unit, onOpenWebsite: (() -> Unit)?, onDelete: () -> Unit, content: @Composable () -> Unit)
// jvm: ContextMenuArea with items "Copy report", "Save report…", "Open website" (when non-null), "Delete"; android/ios: content() only
```
- Delete flow: `vm.delete(time)`. Two-pane: first compute `vm.nextAfterDelete(time)` and select it (or clear). Single-pane detail: in a coroutine, `navigator.navigateBack()` (suspends until the back transition ends), then `vm.delete(time)`. No fixed delays. Then `snackbarHostState.showSnackbar(message = "Exception deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)` → `ActionPerformed` → `vm.undo(time)`, else `vm.commitDelete(time)`.
- Row removal animation: `Modifier.animateItem(fadeInSpec = tween(250), fadeOutSpec = tween(200), placementSpec = spring(stiffness = Spring.StiffnessMediumLow))`. Add a horizontal slide on exit by wrapping each row in `AnimatedVisibility(visibleState)` keyed on pending state, with `exit = slideOutHorizontally { -it / 6 } + fadeOut()`.
- Keys: the timeline pane is `focusable()` with `onPreviewKeyEvent { exceptionKeyAction(it)?.let { … ; true } ?: false }`, two-pane only. Hint bar under the timeline (two-pane only): "↑↓ select · ⌫ delete · ⌘C copy report · ⌘Z undo" (strings `exc_keys_hint_mac` / `exc_keys_hint_other` with Ctrl, picked by `System.getProperty("os.name")` in jvm; Android hides it).

- [ ] **Step 1: Write the failing tests.** `ExceptionKeysTest`: synthesize `KeyEvent`s (`KeyEvent(Key.DirectionDown, KeyEventType.KeyDown)`, `Key.C` with `isMetaPressed = true`, `Key.Z` with `isCtrlPressed = true`, `KeyUp` → null). UI test `ExceptionsScreenUndoTest`: with a real Room DB VM, delete a row → row gone, snackbar "Undo" shown, click Undo → row back.
- [ ] **Step 2: Run.** `./gradlew :kmpuiviews:jvmTest --tests "*ExceptionKeysTest*" --tests "*ExceptionsScreenUndoTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -m "feat(exceptions): animated delete with undo, desktop keys and context menu"`

### Task 10: Device verification, CHANGELOG, cleanup

**Files:**
- Modify: `CHANGELOG.md` (Unreleased)

- [ ] **Step 1: Full test + builds.** `./gradlew :kmpuiviews:jvmTest :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug :mangaworld:desktop:compileKotlinJvm`. Expected: all green.
- [ ] **Step 2: Android emulator check** (`adb -s <emulator serial> install -r mangaworld/build/outputs/apk/noFirebase/debug/*.apk`). Seed exceptions with the emulator sqlite route from memory (the `exceptions` DB; use the 5 fixture traces). Check with screenrecord at 5x animator scale: shared flight both ways, predictive back, delete slide-out + Undo, Share sheet subject, Copy snackbar, Open website → WebView, Explain with Gemma, digest. Save the recordings in the scratchpad and list them in the PR.
- [ ] **Step 3: Desktop check.** `./gradlew :mangaworld:desktop:run`: wide window two panes + selection swap; narrow window flight; ↑↓ / ⌫ / ⌘C / ⌘Z; right-click menu; Save… writes a `.md`; Open website opens the browser; Ollama explanation if configured, otherwise ✦ hidden and fixed text shown.
- [ ] **Step 4: CHANGELOG** under Unreleased: "Exceptions screen redesigned: timeline with a 24-hour graph, plain-language headlines, AI explanations and summaries, a detail page with Copy/Share/Save and source shortcuts, animated delete with Undo, and desktop keyboard shortcuts."
- [ ] **Step 5: Commit.** `git commit -m "docs(changelog): exceptions screen overhaul"`
