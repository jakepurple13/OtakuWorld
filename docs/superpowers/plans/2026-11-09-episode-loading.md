# Episode Loading Indicators Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show progress while AnimeWorld finds sources and resolves a stream: a full-screen loading overlay for Play, a row spinner for Download, a source sheet for 2+ sources, and a buffering overlay in the player.

**Architecture:** A new `EpisodeLoader` in `animeworld/shared` commonMain owns one state machine (`EpisodeLoadState?`) for both actions and replaces `isLoadingChapter`, `VideoSourceModel.showVideoSources`, and the duplicated resolve code in `playResolved` / both `downloadResolved`s. A shared `EpisodeLoadHost` composable renders the overlay and source sheet from that state; Android `GenericAnime.DialogSetups()` and desktop `GenericAnimeDesktop.DialogSetups()` call it. The player route (`VideoScreen`) still only receives a resolved link.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (Material3), coroutines, kotlin.test, ExoPlayer (Android), VLC-backed `DesktopPlayer` (JVM).

**Spec:** Flow walkthrough https://claude.ai/artifact/SG4neS96qERD9BAzpkYwRn, per-screen previews https://claude.ai/artifact/FeQ2AYECDbLyqqkKpQt6ci, plus the user's decisions recorded below.

## Global Constraints

- Overlay and sheet code lives in `animeworld/shared`, not `kmpuiviews`. The overlay draws over the details screen (via `DialogSetups()`); it is not a nav route.
- The player (`VideoScreen`) opens only after the link is resolved. Clear the loader state *before* `playOrCast` navigates, so the overlay never covers the player.
- One episode loads at a time: a tap while `state != null` is ignored (today's guard), so two source sheets can never stack.
- The source sheet opens only after the source list returns, with real rows. Single-source episodes never see it.
- Do not remember the last picked source.
- Play errors show on the overlay. Download errors keep the existing banners (`onResolveFailed`, Android `R.string.source_no_download`, desktop `notifyUser("Download Failed", …)`).
- Casting: same overlay; after resolve, `playOrCast` hands off to `launchCast` as today.
- Copy (exact): steps `"Fetching sources"`, `"Choose a source"`, `"Resolving stream"`; resolving label after a pick `"Resolving <source> <quality>"`; errors `"Couldn't load <episode name>"` and `"No sources found for <episode name>"`; buttons `"Cancel"`, `"Retry"`, `"Try another source"`, `"Back"`; buffering `"Buffering <n>%"`.
- Indicators are M3 Expressive: `LoadingIndicator` on the overlay, indeterminate `CircularWavyProgressIndicator` on the row (existing), determinate `CircularWavyProgressIndicator` for buffering. No plain `CircularProgressIndicator`.
- Every lazy item gets `Modifier.animateItem()`. Tall `ModalBottomSheet`s use `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.

## Review Focus

- Back/Esc or Cancel during "Fetching sources" must cancel the source request and leave no state behind; the next tap must work. (Task 1 `cancelClearsStateAndAllowsNextLoad`)
- Dismissing the source sheet without picking ends the load; for Play the overlay closes too. (Task 1 `dismissingSheetEndsLoad`)
- A resolve that returns a blank link counts as a failure, not a navigation to an empty player. (Task 1 `blankLinkIsFailure`)
- "Try another source" after a failed pick reopens the sheet with the same sources, and is absent for single-source episodes. (Task 1 `pickOtherReopensSheet`, `singleSourceFailureCannotPickOther`)
- The overlay must be gone before the player route is pushed, or it covers the player. (Task 1 `stateClearedBeforePlay`)

---

### Task 1: `EpisodeLoader` state machine

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/episodeload/EpisodeLoader.kt`
- Create: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/episodeload/EpisodeLoaderTest.kt`

**Interfaces:**
- Produces:
```kotlin
enum class EpisodeAction { Play, Download }

sealed interface EpisodeLoadStep {
    data object FetchingSources : EpisodeLoadStep
    data class ChooseSource(val sources: List<KmpStorage>) : EpisodeLoadStep
    data class Resolving(val label: String?) : EpisodeLoadStep   // null = single source, show "Resolving stream"
    data class Failed(val message: String, val detail: String?, val canPickOther: Boolean) : EpisodeLoadStep
}

data class EpisodeLoadState(
    val action: EpisodeAction,
    val episodeUrl: String,
    val episodeName: String,
    val showTitle: String,
    val posterUrl: String?,
    val step: EpisodeLoadStep,
)

sealed interface EpisodeLoadDecision {
    data class Pick(val storage: KmpStorage) : EpisodeLoadDecision
    data object Retry : EpisodeLoadDecision
    data object PickOther : EpisodeLoadDecision
    data object Cancel : EpisodeLoadDecision
}

class EpisodeLoader(
    private val onResolved: suspend (EpisodeAction, KmpStorage, KmpChapterModel, KmpInfoModel) -> Unit,
    private val onDownloadFailed: suspend (KmpStorage?, Throwable?) -> Unit, // storage null = no sources
) {
    val state: EpisodeLoadState?            // backed by mutableStateOf, private set
    suspend fun load(action: EpisodeAction, model: KmpChapterModel, infoModel: KmpInfoModel)
    fun decide(decision: EpisodeLoadDecision)
}

/** Resolves and copies headers/extras the way both downloadResolved()s do today; blank link throws. */
suspend fun resolveStream(model: KmpChapterModel, storage: KmpStorage): KmpStorage
```

`load()` suspends for the whole flow, including while waiting on the sheet or on an error, so the caller's coroutine is the load's lifetime. Decisions arrive through a `Channel<EpisodeLoadDecision>(Channel.CONFLATED)`; drain it at the start of `load()`. Behavior:
1. `state != null` → return immediately (guard).
2. Set `FetchingSources`; `model.getChapterInfo().firstOrNull().orEmpty()` on `Dispatchers.Default`-free code (caller supplies the dispatcher). Exceptions other than `CancellationException` count as no sources.
3. 0 sources → Play: `Failed("No sources found for <name>", null, false)` and await decision; Download: clear state, `onDownloadFailed(null, null)`, return.
4. 1 source → resolve it. 2+ → `ChooseSource`, await `Pick` (any `Cancel` ends).
5. Resolving: `Resolving(label = null)` for single source, `"<source> <quality>"` (blank parts dropped) after a pick. Call `resolveStream`.
6. Success → set `state = null`, **then** `onResolved(action, resolved, model, infoModel)`.
7. Failure → Play: `Failed("Couldn't load <name>", e.message, canPickOther = sources.size > 1)` and await `Retry` (rerun step 5 with the same storage), `PickOther` (back to `ChooseSource`), or `Cancel`. Download: clear state, `onDownloadFailed(storage, e)`.
8. `Cancel` or coroutine cancellation → `state = null` (use `try/finally`).

- [ ] **Step 1: Write the failing tests** in `EpisodeLoaderTest.kt`, using a `FakeSource : KmpApiService` like `GenericAnimeDesktopTest.FakeSource` (configurable `storages`, `resolveError`, `resolveLink`) and `runTest`. Tests and key assertions:
  - `singleSourcePlaysWithoutSheet`: `onResolved` called once with action Play and link `"http://v/720p?resolved"`; no recorded state ever had `ChooseSource`; final `state == null`.
  - `stateClearedBeforePlay`: inside `onResolved`, `loader.state` is `null`.
  - `multipleSourcesWaitForPick`: launch `load`, `advanceUntilIdle()`, state step is `ChooseSource` with 2 sources; `decide(Pick(sources[1]))`; `onResolved` gets the 1080p storage; a `Resolving("src 1080p")` step was recorded.
  - `dismissingSheetEndsLoad`: at `ChooseSource`, `decide(Cancel)`; `load` returns, `state == null`, `onResolved` never called.
  - `cancelClearsStateAndAllowsNextLoad`: chapterInfo suspends (`awaitCancellation` gate); cancel the launched job; `state == null`; a second `load` with a normal source calls `onResolved`.
  - `tapWhileLoadingIsIgnored`: during `ChooseSource`, a second `load` for another episode returns at once and the state's `episodeUrl` is unchanged.
  - `playResolveFailureShowsError` → step `Failed("Couldn't load Ep 1", "boom", false)`; `decide(Retry)` after clearing `resolveError` → `onResolved` called.
  - `pickOtherReopensSheet`: 2 sources, pick fails, `canPickOther == true`; `decide(PickOther)` → `ChooseSource` with both sources.
  - `singleSourceFailureCannotPickOther`: `canPickOther == false`.
  - `playNoSourcesShowsError`: `Failed("No sources found for Ep 1", null, false)`.
  - `blankLinkIsFailure`: resolve returns `link = ""` → Play gets `Failed`; Download calls `onDownloadFailed(storage, any)`.
  - `downloadFailureUsesCallback`: Download with `resolveError` → `onDownloadFailed(storage, error)`, `state == null`, no `Failed` step recorded.
  - `downloadNoSourcesUsesCallback`: `onDownloadFailed(null, null)`.
  Record states with `snapshotFlow { loader.state }` collected in `backgroundScope`, or by having the fake source capture `loader.state` at each call.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :animeworld:shared:jvmTest --tests "com.programmersbox.anime.shared.episodeload.EpisodeLoaderTest"`
Expected: FAIL, unresolved reference `EpisodeLoader`.

- [ ] **Step 3: Implement `EpisodeLoader.kt`** per the Interfaces block and the 8 behavior rules above. `resolveStream` carries the header/extras copy from `GenericAnimeDesktop.downloadResolved` and throws `IllegalStateException("No link")` on a blank link.

- [ ] **Step 4: Run tests to verify they pass**

Run: same command. Expected: PASS (13 tests).

- [ ] **Step 5: Commit**

```bash
git add animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/episodeload animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/episodeload
git commit -m "feat(anime): add EpisodeLoader state machine for play/download resolve"
```

### Task 2: Wire `EpisodeLoader` into `GenericSharedAnime` and both platforms

**Files:**
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/GenericSharedAnime.kt:64-150`
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt` (`playOrCast` unchanged; `downloadChapter`, `downloadResolved` 341-384, `DialogSetups` source block ~589)
- Modify: `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericAnimeDesktop.kt` (`DialogSetups` 58-74, `startDownload`, `downloadResolved`)
- Delete: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videochoice/VideoSourceModel.kt`
- Test: `animeworld/desktop/src/jvmTest/kotlin/com/programmersbox/desktop/GenericAnimeDesktopTest.kt`

**Interfaces:**
- Consumes: Task 1 `EpisodeLoader`, `EpisodeAction`, `resolveStream`.
- Produces (on `GenericSharedAnime`):
```kotlin
val episodeLoader: EpisodeLoader
/** Platform hand-off after a successful download resolve. Android: storage permission check + enqueue + success banner. Desktop: enqueueDownload. */
protected abstract suspend fun enqueueResolved(storage: KmpStorage, model: KmpChapterModel, infoModel: KmpInfoModel)
/** Download with zero sources. Android: R.string.source_no_download banner. Desktop: notifyUser("Download Failed", "<name>: no videos found", true). */
protected abstract fun onNoDownloadSources(model: KmpChapterModel)
/** Android returns false and opens storageAccessDialog when permission is missing; default true. Checked before load(Download). */
protected open fun canStartDownload(): Boolean = true
```
`chapterOnClick` becomes `episodeLoader.load(Play, …)`; `onResolved(Play)` calls `playOrCast(navController, resolved, model, infoModel, seedColor)` — capture `navController`/`seedColor` per call (the loader callback receives them through a lambda built in `chapterOnClick`, or store them in a private field set before `load`). `downloadOnClick`, `playResolved`, `isLoadingChapter` are removed; Android/desktop `downloadResolved` shrink to `enqueueResolved`. `onDownloadFailed(storage, e)` → `onNoDownloadSources` when storage is null, else `onResolveFailed(storage, e)`.

- [ ] **Step 1: Update `GenericAnimeDesktopTest`.** Replace the `VideoSourceModel` assertions: `multipleVideosOpenChoice` asserts `info.episodeLoader.state?.step is ChooseSource` with 2 sources and `navigated.isEmpty()` (launch `chapterOnClick` in a child job, then `decide(Cancel)` to finish); `downloadWithMultipleVideosOpensChoiceWithIsStreamingFalse` → renamed `downloadWithMultipleVideosOpensChoice`, asserts action `Download` and step `ChooseSource`; `resolveFailureReported` → Play failure now asserts step `Failed` and `info.messages.isEmpty()`, then `decide(Cancel)`; add `downloadResolveFailureReported` asserting `info.messages.single()` contains `"boom"`. `@AfterTest` cancels any running load instead of resetting `VideoSourceModel`. `singleVideoPlaysImmediately`, `downloadWithSingleVideoResolvesThenEnqueues`, `downloadWithNoVideosReportsError` keep their assertions.

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :animeworld:desktop:jvmTest --tests "com.programmersbox.desktop.GenericAnimeDesktopTest"`
Expected: FAIL, unresolved `episodeLoader`.

- [ ] **Step 3: Implement** the Interfaces above in the three files; remove `VideoSourceModel` and its `DialogSetups` blocks (Task 3 adds the replacement host). Android `downloadChapter` keeps `scope.launch { … }` and checks `canStartDownload()` first.

- [ ] **Step 4: Verify**

Run: `./gradlew :animeworld:desktop:jvmTest --tests "com.programmersbox.desktop.GenericAnimeDesktopTest" :animeworld:shared:jvmTest :animeworld:assembleNoFirebaseDebug`
Expected: tests PASS, build succeeds.

- [ ] **Step 5: Commit**

```bash
git add -A animeworld
git commit -m "refactor(anime): route play/download resolve through EpisodeLoader"
```

### Task 3: Loading overlay and source sheet (`EpisodeLoadHost`)

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/episodeload/EpisodeLoadHost.kt`
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videochoice/VideoChoiceScreen.kt` (becomes the sheet body: takes `sources`, `episodeName`, `onPick: (KmpStorage) -> Unit`, `onDismiss: () -> Unit`; drops `genericInfo`, `navController`, `playing`, the `LinearProgressIndicator`, and all `VideoSourceModel` writes; keeps grouping and quality icons; adds `rememberModalBottomSheetState(true)`)
- Modify: Android `GenericAnime.DialogSetups()`, desktop `GenericAnimeDesktop.DialogSetups()` — add `EpisodeLoadHost(episodeLoader)`
- Test: `animeworld/shared/src/jvmTest/kotlin/com/programmersbox/anime/shared/episodeload/EpisodeLoadHostTest.kt`

**Interfaces:**
- Consumes: Task 1 types; Task 2 `episodeLoader`.
- Produces: `@Composable fun EpisodeLoadHost(loader: EpisodeLoader)` and `@Composable internal fun EpisodeLoadingOverlay(state: EpisodeLoadState, onDecision: (EpisodeLoadDecision) -> Unit)`.

Rendering rules:
- Overlay shown only when `state.action == Play`. `AnimatedVisibility(enter = slideInVertically { it } + fadeIn(), exit = fadeOut())`, full size, black, poster image (`state.posterUrl`, same loader `CastMiniBar` uses) blurred 16.dp and darkened, cover thumbnail 2:3, episode name + show title, M3 Expressive `LoadingIndicator()` (48.dp, as in `PullToRefreshScaling.kt`), checklist, `Cancel` text button pinned to bottom with navigation-bar insets.
- Checklist: `"Fetching sources"`, then `"Choose a source"` only if the load ever had `ChooseSource` (replace text with the picked label once picked), then `"Resolving stream"`. Done = check + onSurface, current = filled dot + primary, pending = hollow dot + muted.
- `Failed` replaces the indicator/checklist via `AnimatedContent`: error icon, `"Couldn't load …"` / `"No sources found for …"`, detail text, buttons `Retry` (filled), `Try another source` (only if `canPickOther`), `Back` (→ `Cancel`).
- `BackHandler(enabled = true) { onDecision(Cancel) }` (`androidx.compose.ui.backhandler.BackHandler`, as `OtakuListDetailScreen` uses). On desktop also handle Esc via `onPreviewKeyEvent` if BackHandler does not.
- Sheet shown for both actions when step is `ChooseSource`: `VideoChoiceScreen(sources, episodeName, onPick = { decide(Pick(it)) }, onDismiss = { decide(Cancel) })`, title `"Play <name>"` / `"Download <name>"`.
- Overlay must consume touches so details underneath is not clickable.

- [ ] **Step 1: Write failing Compose tests** with `runComposeUiTest` (see `kmpuiviews/src/jvmTest/.../RevealRestoredTest.kt` for setup): `showsChecklistWhileFetching` (texts `"Fetching sources"`, `"Resolving stream"`, `"Cancel"` exist), `cancelSendsCancel` (click `"Cancel"` → recorded `Cancel`), `failedShowsActions` (`Failed(…, canPickOther = true)` shows `"Retry"`, `"Try another source"`, `"Back"`; with `false` the middle one does not exist), `downloadShowsNoOverlay` (Download + `FetchingSources` → `"Fetching sources"` does not exist).
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*EpisodeLoadHostTest"` — expect FAIL.
- [ ] **Step 3: Implement** `EpisodeLoadHost.kt`, rework `VideoChoiceScreen`, and call `EpisodeLoadHost(episodeLoader)` from both `DialogSetups()`.
- [ ] **Step 4: Run** the same tests plus `:animeworld:assembleNoFirebaseDebug` and `:animeworld:desktop:compileKotlinJvm` — expect PASS / success.
- [ ] **Step 5: Commit** `feat(anime): full-screen episode loading overlay and source sheet`.

### Task 4: Download row spinner

**Files:**
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/GenericSharedAnime.kt` — override `observeChapterDownloadStates`
- Test: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/episodeload/EpisodeLoaderTest.kt` (or a new `DownloadRowStateTest.kt`)

**Interfaces:**
- Consumes: `episodeLoader.state`; `ChapterDownloadUiState.Queued` from `kmpuiviews/.../GenericInfo.kt:26`.
- Produces: `internal fun downloadRowStates(state: EpisodeLoadState?): Map<String, ChapterDownloadUiState>` — `mapOf(state.episodeUrl to Queued)` when `state.action == Download`, else `emptyMap()`. `observeChapterDownloadStates` returns `snapshotFlow { downloadRowStates(episodeLoader.state) }`.

This reuses the row's existing indeterminate `CircularWavyProgressIndicator` (`DetailsScreen.kt:893`), so `kmpuiviews` is untouched. The mock's tinted row, step subtitle and dimmed neighbors are not built: they need `ChapterItem` changes in `kmpuiviews`. The lock itself still holds via the loader guard.

- [ ] **Step 1: Failing test** `downloadRowStatesMarksOnlyDownload`: Download state → `mapOf("u" to Queued)`; Play state → empty; null → empty.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*downloadRowStates*"` — FAIL.
- [ ] **Step 3: Implement** `downloadRowStates` and the override.
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** `feat(anime): show row spinner while a download resolves`.

### Task 5: Player buffering overlay (Android + desktop)

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videoplayer/PlayerBufferingOverlay.kt`
- Create: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/videoplayer/BufferPercentTest.kt`
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerCompose.android.kt` (remove `setShowBuffering(SHOW_BUFFERING_ALWAYS)` at 446; add overlay)
- Modify: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videoplayer/DesktopPlayerScreen.kt:358-360`

**Interfaces:**
- Produces:
```kotlin
/** Percent of [targetMs] buffered ahead of the playhead, clamped 0..100. */
fun bufferPercent(bufferedMs: Long, positionMs: Long, targetMs: Long = 5_000): Int
@Composable fun PlayerBufferingOverlay(visible: Boolean, percent: Int, posterUrl: String?, showPoster: Boolean, modifier: Modifier = Modifier)
```
`showPoster` is true until the first frame (Android: `Player.Listener.onRenderedFirstFrame`; desktop: first `positionMs > 0`), so later rebuffers show only the ring + `"Buffering n%"` over the video. The ring is a determinate `CircularWavyProgressIndicator(progress = { percent / 100f })`, 48.dp. Poster comes from `StorageHolder.posterUrl`. Fade out with `tween(400)` at ready.

- [ ] **Step 1: Failing tests** in `BufferPercentTest`: `(0,0)→0`, `(2_500,0)→50`, `(12_000,10_000)→40`, `(20_000,0)→100`, `(500,1_000)→0`.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*BufferPercentTest"` — FAIL.
- [ ] **Step 3: Implement** `bufferPercent` and the overlay; wire Android (state from the existing `Player.Listener` at line 423 plus a 250 ms `bufferedPosition` poll while buffering) and desktop (`state.buffering`, `state.bufferedMs`, `state.positionMs`).
- [ ] **Step 4: Run** tests + `:animeworld:assembleNoFirebaseDebug` + `:animeworld:desktop:compileKotlinJvm` — PASS.
- [ ] **Step 5: Commit** `feat(anime): buffering overlay with percent in player`.

### Task 6: Changelog and device check

**Files:**
- Modify: `CHANGELOG.md` (AnimeWorld section, New Features; link the PR number once opened)

- [ ] **Step 1:** Add entry: "Loading screen while an episode's stream is found, source picker sheet, row spinner for downloads, buffering percentage in the player."
- [ ] **Step 2:** `cdm acquire --category phone --android`, install `:animeworld:installNoFirebaseDebug`, and check with plain `adb`: single-source Play (overlay → player → buffering fade), multi-source Play (sheet over overlay), Cancel/Back during fetch, a failing source (Retry / Try another source), Download single and multi (row spinner, "Queued"/"Download started" banner), and Play while casting if a cast target is available. Then `cdm release`.
- [ ] **Step 3:** `./gradlew :animeworld:desktop:run` and repeat Play single/multi and Esc.
- [ ] **Step 4: Commit** `docs(changelog): episode loading indicators`.
