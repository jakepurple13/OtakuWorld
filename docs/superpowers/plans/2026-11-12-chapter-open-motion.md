# Chapter Open Motion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the generic slide when opening the manga/novel reader from Details with a circular reveal from the tap point (scrubbable by predictive back), use a "depth" fallback when there is no tap point, and give the AnimeWorld loading overlay a "cover dive" exit into the player.

**Architecture:** The reader NavEntries get metadata that keeps Details on screen (`ExitTransition.KeepUntilTransitionsFinished`) and let the reader content animate itself from `LocalNavAnimatedContentScope.current.transition`. NavDisplay (Nav3 1.2) seeks that transition during predictive back, so content-level `animateFloat` follows the finger. The tap point travels through a tiny in-memory holder (`ChapterRevealOrigins`) recorded by the Details chapter row at click time and taken once by the reader entry into `rememberSaveable`. The video overlay plays its own exit animation inside its Dialog, because shared elements cannot cross the Dialog window.

**Tech Stack:** Compose Multiplatform animation, Navigation3 1.2 (`NavDisplay` metadata, `LocalNavAnimatedContentScope`), Koin navigation3 DSL, kotlin.test + `runComposeUiTest`.

**Spec:** Approved mockup https://claude.ai/artifact/8YC8Sdr2DZ5cYgZa9cf43e — readers = proposal 5 "Reveal from tap", video = proposal 2 "Cover dive", fallback = proposal 3 "Depth".

## Global Constraints

- Reveal: circle grows from the tap point to the farthest corner, 450 ms, easing `CubicBezierEasing(0.2f, 0f, 0f, 1f)`.
- Depth (fallback): reader scales 1.08 → 1 and fades in after the first 24% of 380 ms; Details scales to 0.92 and fades out in 110 ms.
- Cover dive: 620 ms total. Poster grows to cover the screen by 60%, blurs up to 16.dp and dims to 45% black; overlay text fades out in the first 40%; whole overlay fades to 0 from 60% to 100%.
- Pop and predictive back play the enter in reverse (circle closes onto the tap point; depth reader scales back to 1.08 and fades).
- Tap point is stored as a fraction of the window size so it survives rotation; it survives process death via `rememberSaveable`. Missing tap point → depth.
- A recorded tap point is only valid for 2 000 ms and only for the chapter URL it was recorded for.
- Only touch the reader entries (`ReadViewModel.MangaReader`, `ReadViewModel.NovelReader`) and `VideoScreen` entries. The app-wide transition in `Nav3View.kt` stays as is.

## Review Focus

- Opening a chapter from somewhere other than a Details row (Daily overview, notification, Downloads, desktop, the multi-select "Read" button) must use depth, never a reveal from a stale point. → Task 1 tests `takeIgnoresOtherKey`, `expiresAfterTwoSeconds`.
- Predictive back cancelled halfway must leave the reader fully visible and interactive (clip radius back to full). → Task 5 device check, step "cancel".
- Rotating while in the reader, then backing out, must close onto the same relative spot, not off screen. → Task 1 stores fractions; Task 5 device check "rotate".
- Cancel or failure in the episode overlay must NOT play the cover dive (only a finished hand-off does). → Task 4 test `noDiveAfterCancel`.
- A swipe-to-read on a chapter row records the swipe start, not a stale tap. → Task 2 records at click/swipe time only.

---

### Task 1: Reveal origin holder + geometry

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/composables/transitions/ChapterRevealOrigins.kt`
- Test: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/utils/composables/transitions/ChapterRevealOriginsTest.kt`

**Interfaces:**
- Produces:
  - `object ChapterRevealOrigins { fun record(key: String, fraction: Offset, nowMs: Long = now()); fun peek(nowMs: Long = now()): Offset?; fun take(key: String?, nowMs: Long = now()): Offset?; internal fun clear() }` — `fraction` is x/windowWidth, y/windowHeight. `take` returns and clears only when `key` matches and the record is ≤ 2 000 ms old; `peek` does not clear. `now()` = `Clock.System.now().toEpochMilliseconds()`.
  - `fun revealRadius(size: Size, center: Offset): Float` — distance from `center` to the farthest corner.

- [ ] **Step 1: Write failing tests** (`@BeforeTest` calls `ChapterRevealOrigins.clear()`)
  - `takeReturnsRecordedOnce`: record("a", Offset(.5f,.25f), 0); `take("a", 100)` == Offset(.5f,.25f); second `take("a", 100)` == null.
  - `takeIgnoresOtherKey`: record("a", …, 0); `take("b", 100)` == null; `take("a", 100)` != null.
  - `expiresAfterTwoSeconds`: record("a", …, 0); `peek(2_001)` == null; `take("a", 2_001)` == null; `peek(2_000)` != null after re-record.
  - `takeNullKeyReturnsNull`: `take(null, 0)` == null.
  - `radiusReachesFarthestCorner`: `revealRadius(Size(100f, 200f), Offset(10f, 20f))` == hypot(90f, 180f) (±0.01).

- [ ] **Step 2: Run, expect FAIL** — `./gradlew :kmpuiviews:jvmTest --tests "*ChapterRevealOriginsTest*"` → unresolved references.
- [ ] **Step 3: Implement** the object (single slot: key, fraction, timestamp) and `revealRadius`.
- [ ] **Step 4: Run, expect PASS** — same command.
- [ ] **Step 5: Commit** — `feat(motion): add chapter reveal origin holder`

### Task 2: Reveal / depth motion + wire readers

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/composables/transitions/ChapterOpenMotion.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsScreen.kt` (chapter row, ~lines 750–852: `swipeBehavior(...)` Read branch and `ListItem(onClick = …)`)
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/MangaModule.kt:32`
- Modify: `novelworld/shared/src/commonMain/kotlin/com/programmersbox/novel/shared/NovelModule.kt:21`

**Interfaces:**
- Consumes: Task 1 `ChapterRevealOrigins`, `revealRadius`.
- Produces (all in `ChapterOpenMotion.kt`):
  - `object ChapterOpenMotion { const val REVEAL_MS = 450; const val DEPTH_MS = 380; val Emphasized: Easing; fun readerMetadata(): Map<String, Any>; fun depthMetadata(): Map<String, Any> }`
    - `readerMetadata()`: `NavDisplay.transitionSpec { if (ChapterRevealOrigins.peek() != null) EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished else EnterTransition.None togetherWith (scaleOut(tween(DEPTH_MS, easing = Emphasized), 0.92f) + fadeOut(tween(110))) }` + `popTransitionSpec` and `predictivePopTransitionSpec` both `EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished`.
    - `depthMetadata()`: push = `EnterTransition.None togetherWith (scaleOut(0.92f)+fadeOut(110))`, pop/predictive = `EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished` (reader content does the motion).
  - `@Composable fun rememberChapterRevealOrigin(key: String?): Offset?` — `rememberSaveable(saver = listSaver of x,y)` around `ChapterRevealOrigins.take(key)`.
  - `@Composable fun Modifier.chapterOpenTransition(origin: Offset?): Modifier` — `progress = LocalNavAnimatedContentScope.current.transition.animateFloat({ tween(if (origin != null) REVEAL_MS else DEPTH_MS, easing = Emphasized) }) { if (it == EnterExitState.Visible) 1f else 0f }`. With origin: convert fraction → local via `LocalWindowInfo.current.containerSize` minus `positionInWindow()` (from `onGloballyPositioned`), then `graphicsLayer { clip = true; shape = circle shape centered there with radius = revealRadius(size, center) * progress }`. Without: `graphicsLayer { scale = lerp(1.08f, 1f, progress); alpha = ((progress - 0.24f) / 0.76f).coerceIn(0f, 1f) }`.
  - `class RevealTapState { var lastFraction: Offset? }`, `@Composable fun rememberRevealTapState()`, `Modifier.trackRevealTap(state)` — `pointerInput { awaitEachGesture { awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial) } }` stores the down position as window fraction (`localToWindow` / container size). Never consumes.

- [ ] **Step 1: Implement `ChapterOpenMotion.kt`** per the Interfaces block.
- [ ] **Step 2: Wire the Details row** — `val tap = rememberRevealTapState()`; add `.trackRevealTap(tap)` to the `SwipeToDismissBox` modifier; immediately before both `detailsActions.onClick(c)` calls (swipe Read branch and `ListItem.onClick` when not in selection mode) call `tap.lastFraction?.let { ChapterRevealOrigins.record(c.url, it) }`.
- [ ] **Step 3: Wire readers** — `navigation<ReadViewModel.MangaReader>(metadata = ChapterOpenMotion.readerMetadata()) { val origin = rememberChapterRevealOrigin(it.mangaUrl); Box(Modifier.fillMaxSize().chapterOpenTransition(origin)) { ReadView(...) } }`; same for `NovelReader` with `it.novelUrl`. (`mangaUrl`/`novelUrl` = `model.url` passed by `chapterOnClick`, matching the row's `c.url`.)
- [ ] **Step 4: Build** — `./gradlew :mangaworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug :kmpuiviews:jvmTest` → BUILD SUCCESSFUL, tests pass.
- [ ] **Step 5: Commit** — `feat(motion): reveal readers from the tapped chapter, depth fallback`

### Task 3: Depth fallback for the video player entry

**Files:**
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt:149`
- Modify: `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt:127`

**Interfaces:**
- Consumes: `ChapterOpenMotion.depthMetadata()`, `Modifier.chapterOpenTransition(null)`.

- [ ] **Step 1:** `navigation<VideoScreen>(metadata = ChapterOpenMotion.depthMetadata()) { Box(Modifier.fillMaxSize().chapterOpenTransition(null)) { VideoPlayerUi(it) } }` in both files.
- [ ] **Step 2: Build** — `./gradlew :animeworld:assembleNoFirebaseDebug :animeworld:desktop:compileKotlinJvm` → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** — `feat(motion): depth transition into the video player`

### Task 4: Cover dive exit for the episode loading overlay

**Files:**
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/episodeload/EpisodeLoadHost.kt`
- Test: `animeworld/shared/src/jvmTest/kotlin/com/programmersbox/anime/shared/episodeload/EpisodeLoadHostTest.kt`

**Interfaces:**
- Produces:
  - `internal const val COVER_DIVE_MS = 620`
  - `internal fun coverDiveFrom(previous: EpisodeLoadState?, current: EpisodeLoadState?): EpisodeLoadState?` — returns `previous` only when `current == null`, `previous.action == Play` and `previous.step == EpisodeLoadStep.OpeningPlayer`.
  - `EpisodeLoadingOverlay(..., closing: Boolean = false, onClosed: () -> Unit = {}, onDecision)` — new params before `onDecision` so the trailing lambda call sites keep compiling.

- [ ] **Step 1: Write failing tests**
  - `diveAfterHandoff`: `coverDiveFrom(state(OpeningPlayer), null)` == that state.
  - `noDiveAfterCancel`: `coverDiveFrom(state(Resolving(null)), null)` == null; `coverDiveFrom(state(Failed(...)), null)` == null.
  - `noDiveForDownload`: `coverDiveFrom(state(OpeningPlayer, EpisodeAction.Download), null)` == null.
  - `noDiveWhileStillShowing`: `coverDiveFrom(state(OpeningPlayer), state(OpeningPlayer))` == null.
  - `closingCallsOnClosedAfterDive`: `runComposeUiTest { mainClock.autoAdvance = false; setContent { EpisodeLoadingOverlay(state(OpeningPlayer), closing = true, onClosed = { closed = true }) {} } ; mainClock.advanceTimeBy(300); assertFalse(closed); mainClock.advanceTimeBy(400); assertTrue(closed) }`.
- [ ] **Step 2: Run, expect FAIL** — `./gradlew :animeworld:shared:jvmTest --tests "*EpisodeLoadHostTest*"`.
- [ ] **Step 3: Implement**
  - Host: track the last shown state (`remember { mutableStateOf<EpisodeLoadState?>(null) }`, updated in `SideEffect` while non-null); when `coverDiveFrom(last, state)` is non-null keep the Dialog composed with that state and `closing = true`, `onClosed` clears it. Ignore back/cancel while closing.
  - Overlay: `Animatable(0f)` animated to 1f over `COVER_DIVE_MS` with `CubicBezierEasing(0.2f, 0f, 0f, 1f)` when `closing`, then `onClosed()`. Measure the poster box (`onGloballyPositioned`, in overlay coordinates) and the overlay size; poster `graphicsLayer` translates to the overlay center and scales by `max(W/w, H/h)` reaching full at progress 0.6; poster `blur(16.dp * p)` and a black `drawWithContent` scrim at `0.45f * p`; poster corner radius 12.dp → 0; texts/checklist/Cancel alpha `1 - p / 0.4`; root alpha `1 - ((p - 0.6f) / 0.4f).coerceIn(0f, 1f)`.
- [ ] **Step 4: Run, expect PASS** — same command, plus `./gradlew :animeworld:assembleNoFirebaseDebug`.
- [ ] **Step 5: Commit** — `feat(motion): cover dive from the episode loading screen into the player`

### Task 5: Device verification

Run on the emulator (`cdm acquire --android --virtual`), using a test title the agent opens itself; no destructive actions on user rows.

- [ ] **Manga:** open a chapter from a Details row → circle grows from the finger. Back button → circle closes onto that point. Predictive back: drag halfway and hold → circle half size; release → finishes; **cancel** (drag back) → reader fully visible and scrollable.
- [ ] **Rotate** in the reader, then back → circle closes on the same relative spot.
- [ ] **Fallback:** open via the multi-select "Read" button and via Daily overview / Continue → depth (Details sinks, reader rises), no reveal.
- [ ] **Novel:** same open/back check.
- [ ] **AnimeWorld:** Play an episode → overlay poster grows, blurs, overlay fades onto the playing video. Cancel during loading → no dive.
- [ ] `cdm release`; record results in the PR body.
