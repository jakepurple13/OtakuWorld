# Update Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Per-title "N new chapters" notifications, per-source update-check progress, a finished notification that explains failures, and an in-app Update check errors screen for the most recent check.

**Architecture:** `MediaUpdateChecker` (commonMain) gains per-source progress snapshots, failure capture and a persisted `UpdateCheckReport` (JSON in DataStore, one record, retry merges into it). Pure formatting/layout helpers in commonMain turn progress and report into text and ProgressStyle segments; androidMain only builds notifications from them. The errors screen is a commonMain Navigation3 entry reached by deep link from the finished notification and from Notification settings.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform resources, kotlinx.serialization, DataStore preferences, WorkManager, `NotificationCompat` (androidx.core 1.19.1), Navigation3, Koin.

**Spec:** `docs/superpowers/specs/2026-11-05-update-notifications-design.md`
**Mockup:** https://claude.ai/artifact/HWzcqTJ5471f3gYH3Gn8h7

## Global Constraints

- minSdk 28, targetSdk 37. ProgressStyle only on API 36+ (`Build.VERSION_CODES.BAKLAVA`); 28–35 use `setProgress` + `BigTextStyle`.
- No "Read" action on update notifications. Tapping a per-title notification opens Details (unchanged `info.deepLinkDetails`).
- Only the most recent check is stored. Retry merges into it. A cancelled run writes nothing.
- No "Source not installed" error kind.
- Progress and finished notifications stay on `NotificationChannels.UpdateCheck` (IMPORTANCE_MIN), `onlyAlertOnce = true`.
- Finished notification without failures: `timeoutAfter = 4000L`. With failures or stopped early: no timeout, `autoCancel = true`, tap opens the errors screen.
- Download action only when `genericInfo.supportsBatchDownload` (MangaWorld today).
- No Room schema change.
- `NotificationDslBuilder` is optional: where it lacks a feature (ProgressStyle, segment colors, anything else), build with `NotificationCompat.Builder` directly (user-approved).
- Every lazy list item gets `Modifier.animateItem()`.
- New strings go in `kmpuiviews/src/commonMain/composeResources/values/strings.xml`, following that file's existing apostrophe escaping (`couldn\'t`).
- Copy (English), used verbatim by tests:
  - per-title title: `%1$s · %2$d new chapters` / `%1$s · 1 new chapter` (episodes variant for AnimeWorld)
  - progress title (API 36+ and while loading recents): `Checking %1$d sources · %2$d favorites`
  - progress title (fallback, after recents load): `Checking %1$s · %2$d of %3$d`
  - progress summary parts joined by ` · `: `%d updates`/`1 update`, `%s failed` per failed source, `%d failed`/`1 failed` for item failures
  - source lines: `✓ %1$s · %2$d updates` (`1 update`, `no updates`), `✗ %1$s · %2$s`, `… %s`
  - finished title: `%1$d updates from %2$d favorites` / `1 update from …` / `No updates from %d favorites`
  - finished text: `%d couldn't be checked. Tap to see why.` (`1 couldn't …`); stopped early: `Check stopped early. Tap to see why.`
  - failure messages: `No response after 15s`, `HTTP %d`, `HTTP %d · likely Cloudflare` (403 and 503), `Couldn't read the page`, item of failed source: `Not checked: source failed`
  - screen: title `Update check errors`, buttons `Retry failed`, `Share`, empty `No errors in the last check`, none yet `No update check yet`, banner `Couldn't load %s's recent updates` + `None of its favorites were checked.`, stopped `Check stopped early`
  - settings row: `Update check errors`, summary uses the finished-text count or `No errors in the last check`

## Review Focus

1. **Source lists chapters oldest-first.** Expect the newest chapters to be named, not the oldest. Test in Task 4 (`newChapters picks newest by uploadedTime when list is oldest first`).
2. **Huge jump in chapter count** (e.g. 300 after a source fix). Expect inbox capped at 5 lines + `+295 more`. Test in Task 4.
3. **Stored report can't be decoded** (corrupt, older schema). Expect "no report", not a crash. Test in Task 2.
4. **Retry when an item was un-favorited since the check, or no report exists.** Expect the item to drop off; with no report the retry is a no-op. Tests in Task 3.
5. **User cancels a check.** Expect the previous report untouched and no finished notification claiming results. Test in Task 3.

---

### Task 1: Report model, failure classification, merge

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/updatecheck/UpdateCheckReport.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/updatecheck/FailureClassifier.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/updatecheck/UpdateCheckReportTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Serializable enum class FailureKind { TimedOut, Http, CouldNotRead }
  @Serializable data class SourceFailure(val source: String, val kind: FailureKind, val httpCode: Int?, val detail: String)
  @Serializable data class ItemFailure(val url: String, val title: String, val imageUrl: String, val source: String,
      val kind: FailureKind, val httpCode: Int?, val detail: String, val sourceFailed: Boolean)
  @Serializable data class UpdateCheckReport(val startedAt: Long, val finishedAt: Long, val favoriteCount: Int,
      val checkedCount: Int, val updatedCount: Int, val sourceFailures: List<SourceFailure>,
      val itemFailures: List<ItemFailure>, val stoppedEarly: String? = null, val retriedAt: Long? = null) {
      val hasProblems: Boolean // itemFailures.isNotEmpty() || sourceFailures.isNotEmpty() || stoppedEarly != null
  }
  data class FailureInfo(val kind: FailureKind, val httpCode: Int?, val detail: String)
  fun classifyFailure(t: Throwable): FailureInfo
  fun UpdateCheckReport.mergeRetry(retry: UpdateCheckReport, retriedUrls: Set<String>, retriedSources: Set<String>): UpdateCheckReport
  ```

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun `timeout classifies as TimedOut`() = runTest {
    val e = runCatching { withTimeout(1) { delay(50) } }.exceptionOrNull()!!
    assertEquals(FailureKind.TimedOut, classifyFailure(e).kind)
}
@Test fun `http code is read from message chain`() {
    val info = classifyFailure(RuntimeException("wrapper", IOException("HTTP error 403")))
    assertEquals(FailureKind.Http, info.kind); assertEquals(403, info.httpCode)
}
@Test fun `other exceptions classify as CouldNotRead`() {
    assertEquals(FailureKind.CouldNotRead, classifyFailure(IllegalStateException("no element")).kind)
}
@Test fun `merge drops retried items that succeeded and keeps ones still failing`() {
    val prev = report(items = listOf(fail("a"), fail("b"), fail("c")), updated = 3)
    val retry = report(items = listOf(fail("b")), updated = 1)
    val merged = prev.mergeRetry(retry, retriedUrls = setOf("a", "b"), retriedSources = setOf("S"))
    assertEquals(listOf("b", "c"), merged.itemFailures.map { it.url })
    assertEquals(4, merged.updatedCount)
    assertEquals(prev.checkedCount, merged.checkedCount)
    assertEquals(retry.finishedAt, merged.retriedAt)
}
@Test fun `merge replaces source failures only for retried sources`()
```
(`report(...)`/`fail(url)` are local builders; `fail` uses source `"S"`.)

- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*UpdateCheckReportTest"` → FAIL (unresolved).
- [ ] **Step 3: Implement.** `classifyFailure` walks the cause chain: any `TimeoutCancellationException` → TimedOut; else first match of `Regex("""HTTP\D{0,8}([1-5]\d{2})""", IGNORE_CASE)` on messages, or Ktor `ResponseException.response.status.value` if present → Http; else CouldNotRead. `detail` = `"${t::class.simpleName}: ${t.message}"` + first 5 stack frames. Merge: drop prior item failures whose url ∈ `retriedUrls`, add `retry.itemFailures`; same for source failures by `retriedSources`; `updatedCount += retry.updatedCount`; keep `startedAt/favoriteCount/checkedCount`; `retriedAt = retry.finishedAt`; `stoppedEarly = retry.stoppedEarly`.
- [ ] **Step 4:** Rerun → PASS.
- [ ] **Step 5:** Commit `feat(update-check): add check report model and failure classification`.

---

### Task 2: Report store

**Files:**
- Modify: `datastore/src/commonMain/kotlin/com/programmersbox/datastore/DataStoreHandling.kt` (add `lastUpdateCheckReport = DataStoreHandler(stringPreferencesKey("lastUpdateCheckReport"), "")`)
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/updatecheck/UpdateCheckReportStore.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.kt` (bind store)
- Create: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/testing/FakeUpdateCheckReportStore.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/updatecheck/UpdateCheckReportStoreTest.kt`

**Interfaces:**
- Consumes: `UpdateCheckReport` (Task 1)
- Produces:
  ```kotlin
  interface UpdateCheckReportStore {
      val report: Flow<UpdateCheckReport?>
      suspend fun current(): UpdateCheckReport?
      suspend fun save(report: UpdateCheckReport)
  }
  class DataStoreUpdateCheckReportStore(dataStoreHandling: DataStoreHandling) : UpdateCheckReportStore
  fun UpdateCheckReport.encode(): String
  fun decodeUpdateCheckReport(raw: String): UpdateCheckReport?   // null for blank or undecodable
  class FakeUpdateCheckReportStore(initial: UpdateCheckReport? = null) : UpdateCheckReportStore  // MutableStateFlow-backed
  ```

- [ ] **Step 1: Failing tests:** `round trip keeps all fields`; `blank string decodes to null`; `corrupt json decodes to null` (input `"{\"startedAt\":"`); `unknown fields are ignored` (Json `ignoreUnknownKeys = true`).
- [ ] **Step 2:** Run `--tests "*UpdateCheckReportStoreTest"` → FAIL.
- [ ] **Step 3:** Implement; Koin `single<UpdateCheckReportStore> { DataStoreUpdateCheckReportStore(get()) }`.
- [ ] **Step 4:** Rerun → PASS.
- [ ] **Step 5:** Commit `feat(update-check): persist last check report`.

---

### Task 3: MediaUpdateChecker — progress, failures, report, retry mode

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/updatecheck/UpdateCheckProgress.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/MediaUpdateChecker.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.kt` (constructor gains store)
- Modify call sites: `kmpuiviews/src/androidMain/.../workers/UpdateChecker.kt`, `kmpuiviews/src/androidMain/.../workers/UpdateNotification.kt`, `kmpuiviews/src/jvmMain/.../repository/BackgroundWorkHandlerImpl.kt` (compile only; notification work comes in Tasks 6–7)
- Create: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/testing/FakeApiService.kt` (a `KmpApiService` with scripted `getRecentFlow` / item info results or exceptions)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/MediaUpdateCheckerTest.kt`

**Interfaces:**
- Consumes: Task 1 model + `classifyFailure`, Task 2 `UpdateCheckReportStore`
- Produces:
  ```kotlin
  enum class SourceState { Pending, Running, Done, Failed }
  data class SourceProgress(val name: String, val state: SourceState, val favorites: Int, val toCheck: Int,
      val checked: Int, val updates: Int, val failure: FailureInfo?)
  data class UpdateCheckProgress(val favoriteCount: Int, val loadingRecents: Boolean, val total: Int, val checked: Int,
      val current: String?, val sources: List<SourceProgress>, val updatesFound: Int, val itemFailures: Int)
  enum class UpdateCheckMode { Full, RetryFailed }
  data class UpdatedItem(val info: KmpInfoModel, val dbModel: DbModel, val newChapters: List<KmpChapterModel>)
  data class UpdateCheckResult(val updates: List<UpdatedItem>, val report: UpdateCheckReport?)  // report = this run's own (pre-merge); null when nothing ran
  // MediaUpdateChecker(dao, sourceRepository, sourceLoader, exceptionDao, reportStore: UpdateCheckReportStore)
  suspend fun getFavoritesThatNeedUpdates(checkAll: Boolean, mode: UpdateCheckMode = UpdateCheckMode.Full,
      putMetric: suspend (String, Long) -> Unit, onProgress: suspend (UpdateCheckProgress) -> Unit = {},
      setProgress: suspend (Int, Int, String) -> Unit): UpdateCheckResult
  suspend fun mapDbModel(list: List<UpdatedItem>, notificationUpdate: suspend (Int, Int, String) -> Unit): List<UpdateModel>
  data class UpdateModel(val notificationId: Int, val item: UpdatedItem)
  ```
  `newChapters` is computed with `newChapters(info, oldCount)` from Task 4 **before** `saveUpdatedItems` overwrites `numChapters`. Do Task 4's `newChapters` helper first or stub it as `info.chapters.take(n)` and let Task 4 replace it.

- [ ] **Step 1: Failing tests** (fake services registered in `SourceRepository`):
  - `recent list failure marks source failed and lists its favorites as not checked` → report `sourceFailures` has the source with `Http 403`; its favorites in `itemFailures` with `sourceFailed = true`.
  - `item info timeout is recorded as TimedOut item failure`.
  - `progress marks failed source Failed and finished sources Done` (collect `onProgress`; last snapshot).
  - `full run replaces stored report`.
  - `cancelled run keeps previous report` — launch, cancel mid-check (fake suspends), assert `store.current()` is the seeded report.
  - `crash partway saves report with stoppedEarly` — make `dao` throw after recents via a fake; assert `stoppedEarly != null` and exception rethrown.
  - `retry only checks failed urls and merges` — seeded report fails `a`,`b`; favorites `a`,`b`,`c`; retry succeeds `a` → stored failures = `[b]`, `c` never fetched.
  - `retry drops failures for items no longer favorited`.
  - `retry with no stored report does nothing` → result `UpdateCheckResult(emptyList(), null)`, no fetches.
  - Keep the existing `mapDbModel refreshes…` test passing with the new types.
- [ ] **Step 2:** Run `--tests "*MediaUpdateCheckerTest"` → FAIL.
- [ ] **Step 3: Implement.** `fetchRecentItemsFromAllSources` returns per-source `Result<List<KmpItemModel>>` (catch and timeout become failures, not `emptyList()`; `exceptionDao.insertException` stays). Keep a `MutableStateFlow`-style local state of `UpdateCheckProgress` and call `onProgress` after each change. Rethrow `CancellationException` before any save. Wrap the run in `try { … } catch (e: Exception) { save partial with stoppedEarly = e.message ?: e::class.simpleName; throw e }`. RetryFailed: read `reportStore.current()`; if null or no item/source failures → return early; `onlyUrls` = failed item urls; favorites filtered to those; `retriedUrls` = onlyUrls (including ones no longer favorited); `retriedSources` = their sources; save `previous.mergeRetry(...)`.
- [ ] **Step 4:** Rerun → PASS. Also `./gradlew :kmpuiviews:compileDebugKotlinAndroid :kmpuiviews:compileKotlinJvm` → BUILD SUCCESSFUL.
- [ ] **Step 5:** Commit `feat(update-check): record per-source progress and failures`.

---

### Task 4: Text and segment helpers

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/updatecheck/UpdateCheckText.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/updatecheck/NewChapters.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/GenericInfo.kt` (add `val updateUnit: UpdateUnit get() = UpdateUnit.Chapters`)
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt` (override `UpdateUnit.Episodes`)
- Modify: `kmpuiviews/src/commonMain/composeResources/values/strings.xml`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/updatecheck/UpdateCheckTextTest.kt`

**Interfaces:**
- Consumes: Tasks 1, 3 types
- Produces:
  ```kotlin
  enum class UpdateUnit { Chapters, Episodes }
  fun newChapters(info: KmpInfoModel, oldCount: Int): List<KmpChapterModel>      // newest first; empty when size <= oldCount
  suspend fun updateTitle(title: String, count: Int, unit: UpdateUnit): String
  fun chapterRange(newChapters: List<KmpChapterModel>): String                    // 1: name; >1: "${oldest.name} – ${newest.name}"
  fun inboxLines(newChapters: List<KmpChapterModel>, max: Int = 5): List<String>   // names + "+N more"
  suspend fun UpdateCheckProgress.headline(): String        // "Checking N sources · M favorites"
  suspend fun UpdateCheckProgress.fallbackTitle(): String   // headline() while loadingRecents, else "Checking X · a of b"
  suspend fun UpdateCheckProgress.summary(): String
  suspend fun UpdateCheckProgress.sourceLines(): List<String>
  suspend fun UpdateCheckReport.finishedTitle(): String
  suspend fun UpdateCheckReport.finishedText(): String?     // null when !hasProblems
  suspend fun failureMessage(kind: FailureKind, httpCode: Int?): String
  data class SegmentLayout(val segments: List<Pair<Int, Boolean>>, val progress: Int, val points: List<Int>) // (length, failed)
  fun UpdateCheckProgress.segmentLayout(): SegmentLayout
  ```
  Segment rules: one segment per source, length `max(1, favorites)`. Progress = sum of full length for Done/Failed, `length * checked / toCheck` for Running, 0 for Pending. One point per source with `updates > 0`, at that segment's end offset.

- [ ] **Step 1: Failing tests** (values from Global Constraints):
  - `newChapters picks newest by uploadedTime when list is oldest first` — 5 chapters, uploadedTime ascending, oldCount 3 → names of the last two, newest first.
  - `newChapters takes head of list when uploadedTime missing`.
  - `newChapters is empty when count did not grow`.
  - `inboxLines caps at 5 and adds more line` — 300 new → 6 lines, last `"+295 more"`.
  - `updateTitle uses singular and episodes` — `"Frieren · 3 new chapters"`, `"Frieren · 1 new chapter"`, `"Show · 2 new episodes"`.
  - `summary joins parts` — 3 updates, Comick failed, 0 item failures → `"3 updates · Comick failed"`; nothing → `""`.
  - `sourceLines` → `"✓ MangaDex · 1 update"`, `"✗ Comick · HTTP 403 · likely Cloudflare"`, `"… MangaPark"`.
  - `finishedTitle` → `"3 updates from 42 favorites"`, `"No updates from 42 favorites"`; `finishedText` → `"3 couldn't be checked. Tap to see why."`, stopped → `"Check stopped early. Tap to see why."`, clean → `null`.
  - `segmentLayout` — sources [Done 10 favs updates 1, Failed 4, Running 6 (3/6 checked), Pending 2] → segments `[(10,false),(4,true),(6,false),(2,false)]`, progress `17`, points `[10]`.
- [ ] **Step 2:** Run `--tests "*UpdateCheckTextTest"` → FAIL.
- [ ] **Step 3:** Implement with `getString`/`getPluralString`; replace Task 3's stub of `newChapters` if one was used.
- [ ] **Step 4:** Rerun → PASS.
- [ ] **Step 5:** Commit `feat(update-check): add notification text and segment helpers`.

---

### Task 5: Update check errors screen, retry, share, settings row

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt` (add `@Serializable data object UpdateCheckErrors : Screen("update_check_errors")`)
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/updatecheck/UpdateCheckErrorsScreen.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/updatecheck/UpdateCheckErrorsViewModel.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/updatecheck/UpdateCheckReportSharer.kt` + `androidMain` (ACTION_SEND chooser, `FLAG_ACTIVITY_NEW_TASK`), `jvmMain` (copy to system clipboard), `iosMain` (no-op) implementations, bound in each platform's Koin module
- Modify: `kmpuiviews/src/commonMain/.../navigation/Nav3Graph.kt` (`detailEntry<Screen.UpdateCheckErrors> { UpdateCheckErrorsScreen() }`), ViewModel module
- Modify: `kmpuiviews/src/commonMain/.../repository/WorkRepository.kt` (+ `fun retryFailedUpdates()`), `androidMain/.../WorkRepositoryImpl.kt` (unique one-time `UpdateFlowWorker` with `UpdateFlowWorker.RETRY_FAILED to true`, `ExistingWorkPolicy.KEEP`), `jvmMain/.../WorkerRepositoryImpl.kt` (mirror its `checkManually` with RetryFailed mode), `jvmTest/.../NotificationSettingsViewModelTest.kt` `FakeWorkRepository`
- Modify: `kmpuiviews/src/androidMain/.../workers/UpdateChecker.kt` (add `const val RETRY_FAILED = "retry_failed"`; pass `UpdateCheckMode.RetryFailed` when set)
- Modify: `kmpuiviews/src/commonMain/.../settings/notifications/NotificationSettingsScreen.kt` + ViewModel (row under "Last update check")
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/updatecheck/UpdateCheckErrorsViewModelTest.kt`

**Interfaces:**
- Consumes: Tasks 1–4
- Produces:
  ```kotlin
  data class FailureGroup(val source: String, val sourceFailure: SourceFailure?, val items: List<ItemFailure>, val dominantKind: FailureKind)
  fun UpdateCheckReport.groupFailures(): List<FailureGroup>   // groups by source; source-failure groups first, then by item count desc, then name
  suspend fun UpdateCheckReport.toShareText(): String
  interface UpdateCheckReportSharer { fun share(text: String) }
  class UpdateCheckErrorsViewModel(store: UpdateCheckReportStore, workRepository: WorkRepository, sharer: UpdateCheckReportSharer) : ViewModel {
      val report: StateFlow<UpdateCheckReport?>
      val groups: StateFlow<List<FailureGroup>>
      val canRetry: StateFlow<Boolean>   // report has item failures
      fun retryFailed()
      fun share()
  }
  ```
  Share text format:
  ```
  Update check · <finishedAt as local date-time>
  42 favorites · 3 updated · 3 failed

  Comick — Couldn't load Comick's recent updates (HTTP 403 · likely Cloudflare)
    Dandadan — Not checked: source failed
  MangaPark
    Blue Lock — No response after 15s
      <detail>
  ```

- [ ] **Step 1: Failing tests:** `groups put source failures first`; `canRetry false without item failures`; `retryFailed calls workRepository once`; `share sends text containing every failed title`; `toShareText matches format` (fixed report, timezone UTC).
- [ ] **Step 2:** Run `--tests "*UpdateCheckErrorsViewModelTest"` → FAIL.
- [ ] **Step 3: Implement ViewModel and screen per mockup.** Scaffold + TopAppBar (back via `NavigationActions`), LazyColumn: summary card (time, duration, checked/updated/failed with `tabular-nums`-style `FontFeature`), optional "Check stopped early" banner, per group a header row (source, kind pill, count), source banner, item rows (cover via the project's existing cover image composable, title, one-line `failureMessage`, tap toggles expanded `detail`, trailing IconButton navigates to `Screen.DetailsScreen.Details(title, "", url, imageUrl, source)`). Bottom bar: `Share`, `Retry failed` (disabled when `!canRetry`). Empty states per Global Constraints. All items `Modifier.animateItem()`. Settings row navigates to `Screen.UpdateCheckErrors`.
- [ ] **Step 4:** Rerun tests → PASS; `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug` → BUILD SUCCESSFUL.
- [ ] **Step 5:** Commit `feat(update-check): add update check errors screen`.

---

### Task 6: Progress and finished notifications (Android)

**Files:**
- Create: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/UpdateCheckNotifications.kt`
- Modify: `kmpuiviews/src/androidMain/.../workers/UpdateChecker.kt` (`createNotification` takes `UpdateCheckProgress`; finished handling)
- Modify: `kmpuiviews/src/androidMain/.../workers/UpdateNotification.kt` (`sendFinishedNotification(report: UpdateCheckReport?)`)
- Modify: `kmpuiviews/src/androidMain/.../utils/DeepLinks.kt` (+ `updateCheckErrors` route, add to `deepLinkMatching`)
- Modify: `kmpuiviews/src/androidMain/.../GenericInfo.android.kt` (+ `fun deepLinkUpdateCheckErrorsUri(): Uri`, abstract `fun deepLinkUpdateCheckErrors(context: Context): PendingIntent?`)
- Modify: `mangaworld/.../GenericManga.kt`, `animeworld/.../GenericAnime.kt`, `novelworld/.../GenericNovel.kt` (override like `deepLinkSettings`), `UIViews/.../utils/MockData.kt` (`= null`)

**Interfaces:**
- Consumes: Task 3 `onProgress`, Task 4 text + `segmentLayout`, Task 5 `Screen.UpdateCheckErrors`
- Produces:
  ```kotlin
  suspend fun buildCheckProgressNotification(context: Context, icon: NotificationLogo, progress: UpdateCheckProgress, cancelIntent: PendingIntent): Notification
  ```

- [ ] **Step 1: Implement `buildCheckProgressNotification`.** API ≥ 36: `NotificationCompat.ProgressStyle` with one `Segment(length)` per `segmentLayout().segments` (failed → color `0xFFB3261E.toInt()`), `setProgress(layout.progress)`, a `Point(offset)` per `points` (color `0xFF2F7D4F.toInt()`), content title `headline()`, text `summary()`. If `NotificationCompat.ProgressStyle` is missing from core 1.19.1, use platform `Notification.ProgressStyle` with `Notification.Builder` under the same SDK check. API < 36: existing DSL with `progress { max = total; progress = checked; indeterminate = loadingRecents }`, title `fallbackTitle()`, text `summary()`, `bigTextStyle { bigText = sourceLines().joinToString("\n") }`. Both: `UpdateCheck` channel, `onlyAlertOnce`, `ongoing`, Cancel action (`WorkManager.createCancelPendingIntent(id)`).
- [ ] **Step 2: Finished.** Worker: on success `update.sendFinishedNotification(result.report)`; on `CancellationException` cancel notification 13 and post nothing; on other exceptions the checker already saved a stopped-early report → read `reportStore.current()` and send that. `sendFinishedNotification`: title `finishedTitle()`, text `finishedText()`; `hasProblems` → no timeout, `autoCancel`, content intent `info.deepLinkUpdateCheckErrors(context)`; else `timeoutAfter = 4000L`. Remove the unconditional `finally { sendFinishedNotification() }`.
- [ ] **Step 3:** `./gradlew :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug` → BUILD SUCCESSFUL; `./gradlew :kmpuiviews:jvmTest` → PASS.
- [ ] **Step 4:** Commit `feat(update-check): per-source progress and finished notifications`.

---

### Task 7: Per-title update notification + Download action

**Files:**
- Modify: `kmpuiviews/src/androidMain/.../workers/UpdateNotification.kt` (`mapDbModel` uses `UpdateModel.item.newChapters`)
- Modify: `kmpuiviews/src/commonMain/.../domain/MediaUpdateChecker.kt` (`mapDbModel` stores `summaryText = "${updateTitle(...)}: ${chapterRange(...)}"`)
- Create: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/receivers/DownloadNewChaptersReceiver.kt`
- Modify: the androidMain manifest that registers `DeleteNotificationReceiver` (register the new receiver, `exported="false"`)

**Interfaces:**
- Consumes: Task 3 `UpdateModel`/`UpdatedItem`, Task 4 `updateTitle`, `chapterRange`, `inboxLines`, `newChapters`, `KmpGenericInfo.updateUnit`
- Produces: receiver extras `url: String`, `source: String`, `count: Int`, `id: Int`

- [ ] **Step 1: Notification.** Title `updateTitle(dbModel.title, newChapters.size, info.updateUnit)`, text `chapterRange(newChapters)`, `largeIconBitmap` = cover (keep current fetch), `inboxStyle` with `inboxLines(newChapters)` (drop the big-picture style). Actions: `Mark read` (unchanged); `Download N` (`"Download"` when N = 1) only if `info.supportsBatchDownload`, PendingIntent to `DownloadNewChaptersReceiver` with request code `notificationId + 1`.
- [ ] **Step 2: Receiver.** `goAsync()`; look up the favorite by url, resolve the source via `sourceRepository.toSourceByApiServiceName(source)`, load `toItemModel(api).toInfoModel().firstOrNull()?.getOrNull()`, take `newChapters(info, info.chapters.size - count)`, call `genericInfo.batchDownloadChapters(chapters, info)`, then do what `DeleteNotificationReceiver` does (delete saved item, cancel id, cancel summary 42 when last). Failures → `recordFirebaseException`.
- [ ] **Step 3:** Rerun `./gradlew :kmpuiviews:jvmTest` (existing `mapDbModel refreshes…` still PASS) and the three `assembleNoFirebaseDebug` builds → BUILD SUCCESSFUL.
- [ ] **Step 4:** Commit `feat(update-check): list new chapters in update notifications`.

---

### Task 8: On-device verification (adb, no ARTEMIS)

- [ ] **Step 1:** `adb devices -l`. More than one device → ask the user which.
- [ ] **Step 2:** Install `:mangaworld:assembleNoFirebaseDebug` (`adb -s <serial> install -r …`). No Room change, so no DB-version risk.
- [ ] **Step 3:** Settings → Notifications → tap "Last update check" (manual check). `adb shell dumpsys notification --noredact | grep -A30 otakuworld` while running: progress notification present on channel `updateCheckChannel`; on API 36 device `android.progressSegments` extra present.
- [ ] **Step 4:** After finish: per-title notifications show `· N new chapters` and inbox lines; finished notification title matches `finishedTitle()`.
- [ ] **Step 5:** Force a failure (airplane mode during check, or a source known to 403), tap the finished notification → Update check errors screen opens; Retry failed runs a check; Share opens the share sheet. Screenshot with `adb exec-out screencap -p > /tmp/…png` and show the user.
- [ ] **Step 6:** Report results, including anything untested (API < 36 fallback if no such device, AnimeWorld/NovelWorld, desktop screen).
