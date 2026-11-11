# Reader Animations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the seven approved MangaWorld reader animations: A (page-load skeleton, wavy progress, and reveal), B (error shake and spinning retry), C (animated chapter-end card), D (chapter-change slide and banner), F (bookmark pop and burst), G (reading-progress hairline), and H (List ↔ Pager morph).

**Architecture:** Most changes are local to call sites in `mangaworld/shared/.../reader/`. Pure math lives in one new file, `ReaderMotion.kt`, so it can be unit-tested. New UI pieces are small composables, each in the file that owns the feature. H replaces the reader's `Crossfade(readerType)` with `SharedTransitionLayout` + `AnimatedContent`. `ListView`/`PagerView` take a per-page modifier hook, so they stay unaware of the transition.

**Tech Stack:** Compose Multiplatform (Material3 expressive, `MaterialTheme.motionScheme`), Kamel, kotlin.test + `runComposeUiTest` (jvmTest).

**Spec:** HTML previews approved on 2026-11-11: https://claude.ai/artifact/9wF8MqQdiF4UmzgF7e1SwP (cards A, B, C, D, F, G, H; E is not chosen), plus the decisions under Global Constraints, which override the previews where they differ.

All paths below are relative to `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/` (written `reader/`), and tests are under `mangaworld/shared/src/jvmTest/kotlin/com/programmersbox/manga/shared/reader/` (written `test/`).

## Global Constraints

- E (rolling page counter) is out. Do not touch `PageIndicator`.
- F has **no ribbon over the page**. The burst is drawn inside the top bar only.
- Timing: use `MaterialTheme.motionScheme.{default,fast,slow}{Spatial,Effects}Spec()`. Spatial specs are for position, size and scale; effects specs are for alpha and color. A raw `tween`/`spring`/`keyframes` is allowed only for the shimmer, the error shake keyframes, the check-mark path draw, the retry spin loop, the burst, and auto-hide delays.
- A and B apply to the Kamel loader (`ImageLoaderType.Kamel` → `Kamel(...)` in `ReaderComposeImage.kt`). The Glide, Telephoto and Panpf loaders are out of scope.
- A memory-cache hit (`DataSource.Memory`) still shows instantly, with no fade and no scale. This is current behavior and must stay.
- D: the slide plays only for Next/Previous button presses, in Pager, FlipPager and CurlPager modes. List mode gets the banner only. Crossing a chapter boundary by scrolling plays nothing new.
- D: the banner replaces the `InAppNotifier` "Marked as read" toast at every `showToast` call site. Remove `showToast`, the `notifier`/`markedReadTitle` locals, and their imports if nothing else uses them.
- H morphs List ↔ Pager only. Any switch that involves FlipPager or CurlPager uses a plain fade (`fadeIn(defaultEffects) togetherWith fadeOut(fastEffects)`), which matches today's crossfade.
- G has no setting toggle. It shows whenever the bars are hidden.
- B has no haptic. A buzz per failed page would stack when several pages fail together. F uses `HapticFeedbackType.ToggleOn`, and only when a bookmark is added.
- Copy, exact: B: "Page didn't load" / "Tap to retry" (the exception message stays below them, in the error color). C: "Finished", "Next", "Chapter {position} of {count}". D banner: "{chapter name}", then " · {n} pages" once the pages are loaded.
- Every lazy item touched keeps its existing `key` (`lazyKeys()`).
- Local builds use the `noFirebase` flavor. Device checks start with `cdm acquire --android --category phone` and end with `cdm release --android`.

## Review Focus

1. **Opening a chapter that is already bookmarked must not play the burst.** `isBookmarked` comes from `collectAsStateWithLifecycle(initialValue = false)`, so it goes false→true on open. The burst is triggered by the click, not by the state change. Owned by Task 5 (`burstOnlyOnUserAdd`).
2. **Scrolling across a chapter boundary in any mode must not slide the content or show the banner.** `currentChapter` changes while you scroll, because `updateCurrentChapter` runs from the list flow, and that flow is synced in pager modes too. Owned by Task 4: the trigger is wired to the button callbacks, never to `currentChapter`. A review checklist step covers it, plus a device check.
3. **Pages from the memory cache must not fade or scale.** Owned by Task 1 (`memoryHitStartsSettled`).
4. **Scrolling back over a chapter-end card must not replay it.** Owned by Task 3, which uses `rememberSaveable` inside a keyed lazy item. Checked on device.
5. **The progress fraction must stay in 0..1 when `currentPage` runs past the page count** (pages still loading, or a transition item). Owned by Task 6 (`progressClamps`).

---

### Task 1: Page-load skeleton, wavy progress and reveal (A)

**Files:**
- Modify: `mangaworld/shared/build.gradle.kts` (jvmTest deps)
- Create: `reader/ReaderMotion.kt`
- Modify: `reader/ReaderComposeImage.kt:72-144` (`FADE_IN_MILLIS`, `Kamel` Loading and Success branches)
- Test: `test/ReaderMotionTest.kt`

**Interfaces:**
- Produces: `internal data class RevealStart(val alpha: Float, val scale: Float)`, `internal fun pageRevealStart(source: DataSource): RevealStart`, `internal const val REVEAL_START_SCALE = 1.04f`, and `ReaderMotion.kt`, which later tasks add to.

- [ ] **Step 1: Add the UI-test dependency.** In `mangaworld/shared/build.gradle.kts`, add `implementation(commonLibs.cmp.ui.test)` to `jvmTest.dependencies`. This is the same alias `animeworld/shared` uses.

- [ ] **Step 2: Write the failing test**

```kotlin
class ReaderMotionTest {
    @Test fun memoryHitStartsSettled() =
        assertEquals(RevealStart(alpha = 1f, scale = 1f), pageRevealStart(DataSource.Memory))
    @Test fun freshLoadStartsHiddenAndEnlarged() =
        assertEquals(RevealStart(alpha = 0f, scale = 1.04f), pageRevealStart(DataSource.Network))
}
```

- [ ] **Step 3: Run it and confirm it fails.** Run `./gradlew :mangaworld:shared:jvmTest --tests "*.ReaderMotionTest"`. Expected: compile FAIL, `pageRevealStart` unresolved.

- [ ] **Step 4: Implement it.**
  - Write `pageRevealStart` in `ReaderMotion.kt`.
  - In `Kamel`'s Success branch, replace the single alpha `Animatable` with two, alpha and scale, keyed on `painter` and started from `pageRevealStart(resource.source)`. Animate them in parallel: alpha with `motionScheme.slowEffectsSpec()`, scale with `motionScheme.slowSpatialSpec()`. Apply both in the existing `graphicsLayer`. Delete `FADE_IN_MILLIS`.
  - In the Loading branch, keep `placeholderSize` and replace its contents with a new `private fun PageLoadSkeleton(progress: () -> Float, modifier: Modifier)`. It holds four panels in a `Column`: a wide panel (weight 1f), a `Row` of two panels (weights 1.3f and 1f) at weight 1.2f, and a wide panel at weight .9f, all with 4.dp gaps and 6.dp padding. Each panel is `Modifier.m3placeholder(true, shape = MaterialTheme.shapes.extraSmall, highlight = PlaceholderHighlight.shimmer())`, from `kmpuiviews/.../components/placeholder`. Center a `CircularWavyProgressIndicator(progress = progress)` over the panels.

- [ ] **Step 5: Run the test and confirm it passes.** Same command. Expected: PASS.

- [ ] **Step 6: Build.** Run `./gradlew :mangaworld:assembleNoFirebaseDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit.** `git commit -m "feat(reader): page skeleton, wavy progress and scale reveal"`

---

### Task 2: Error shake and spinning retry (B)

**Files:**
- Modify: `reader/ReaderComposeImage.kt` (`Kamel`, Failure branch)
- Test: `test/ReaderMotionUiTest.kt`

**Interfaces:**
- Produces: `@Composable internal fun PageLoadFailed(message: String?, onRetry: () -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the failing test**

```kotlin
@OptIn(ExperimentalTestApi::class)
class ReaderMotionUiTest {
    @Test fun failedPageShowsCopyAndRetries() = runComposeUiTest {
        var retries = 0
        setContent { PageLoadFailed(message = "timeout", onRetry = { retries++ }) }
        onNodeWithText("Page didn't load").assertIsDisplayed()
        onNodeWithText("timeout").assertIsDisplayed()
        onNodeWithText("Tap to retry").performClick()
        assertEquals(1, retries)
    }
}
```

- [ ] **Step 2: Run it and confirm it fails.** Run `./gradlew :mangaworld:shared:jvmTest --tests "*.ReaderMotionUiTest"`. Expected: compile FAIL.

- [ ] **Step 3: Implement `PageLoadFailed`.**
  - Layout: a column with the copy from Global Constraints, and a 40.dp `errorContainer` circle holding `Icons.Default.Refresh` (contentDescription "Retry"). The whole column is clickable.
  - Shake: on first composition, run an `Animatable(0f)` through `keyframes { durationMillis = 500; -6f at 150; 6f at 250; -3f at 350; 0f at 500 }`. Apply it in dp as `graphicsLayer.translationX`.
  - Spin: clicking sets `var retrying by remember { mutableStateOf(false) }` to true and calls `onRetry`. While `retrying` is true, rotate the icon with `rememberInfiniteTransition().animateFloat(0f, 360f, infiniteRepeatable(tween(700, easing = LinearEasing)))`.
  - The Failure branch in `Kamel` calls `PageLoadFailed(resource.exception.message, onRefresh, …)` and keeps its existing `printStackTrace` effect and size modifiers. When the resource leaves Failure, the composable leaves too, which ends the spin.

- [ ] **Step 4: Run the test and confirm it passes.** Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "feat(reader): failed page shakes and spins retry"`

---

### Task 3: Chapter-end card that plays on scroll-in (C)

**Files:**
- Modify: `reader/Pages.kt:34-112` (`ChapterTransitionItem`)
- Modify: `reader/ReaderCompose.kt` (both `ChapterTransitionItem(` call sites: the `reader()` list item and `PagerView`)
- Modify: `reader/ReaderMotion.kt`
- Test: `test/ReaderMotionTest.kt`, `test/ReaderMotionUiTest.kt`

**Interfaces:**
- Produces:
  - `internal fun chapterPosition(listIndex: Int, chapterCount: Int): Int` = `chapterCount - listIndex`
  - `internal fun seriesProgress(listIndex: Int, chapterCount: Int): Float` = position / count, clamped to 0..1, and 0f when count ≤ 0
  - `ChapterTransitionItem(fromChapterName: String?, toChapterName: String?, toChapterListIndex: Int, chapterCount: Int, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun positionCountsFromOldest() = assertEquals(43, chapterPosition(listIndex = 26, chapterCount = 69))
@Test fun seriesProgressIsFraction() = assertEquals(43f / 69f, seriesProgress(26, 69), 0.0001f)
@Test fun seriesProgressEmptyIsZero() = assertEquals(0f, seriesProgress(0, 0))
// ReaderMotionUiTest
@Test fun endCardShowsNextAndPosition() = runComposeUiTest {
    setContent { ChapterTransitionItem("Ch. 42", "Ch. 43", toChapterListIndex = 26, chapterCount = 69) }
    mainClock.advanceTimeBy(2_000)
    onNodeWithText("Finished").assertIsDisplayed()
    onNodeWithText("Ch. 43").assertIsDisplayed()
    onNodeWithText("Chapter 43 of 69").assertIsDisplayed()
}
```

- [ ] **Step 2: Run them and confirm they fail.** Expected: compile FAIL.

- [ ] **Step 3: Implement it.**
  - Hold `val played = rememberSaveable { mutableStateOf(false) }`, and set it to true in `LaunchedEffect(Unit)`. A lazy item's saveable state survives scrolling away, which is what keeps the card from replaying.
  - Put each group in `AnimatedVisibility(visibleState = MutableTransitionState(played.value).apply { targetState = true })`, entering with `slideInVertically { it / 3 } + fadeIn(defaultEffects)`. Stagger: the check draws over 0–500 ms, "Finished" enters at +100 ms, the divider grows with `animateFloatAsState` → `fillMaxWidth(.7f * t)` at +500 ms, and "Next" enters at +700 ms. Do the delays with a `LaunchedEffect` that flips per-group booleans after `delay`.
  - Check mark: a 40.dp `Canvas` that draws a circle and a tick with `PathMeasure.getSegment(0f, length * t, dst)`. Colour is `colorScheme.tertiary`. `t` comes from an `Animatable` and is 1f immediately when `played` was already true.
  - Under "Next", show `LinearWavyProgressIndicator(progress = { seriesProgress(...) })` with the text "Chapter N of M". Skip both when `toChapterName == null`.
  - Pass `toChapterListIndex = item.toChapterListIndex` and `chapterCount = vm.chapterCount` from both call sites.

- [ ] **Step 4: Run the tests and confirm they pass.** Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "feat(reader): chapter-end card animates in once"`

---

### Task 4: Chapter-change slide and banner (D)

**Files:**
- Create: `reader/ChapterBanner.kt`
- Modify: `reader/ReaderCompose.kt` (`showToast` at :195, the bottom-bar callbacks at :445-446, the FAB at :480, the content `Box` at :499)
- Modify: `reader/AppBars.kt:129-197` (`FloatingFloatingActionButton`), `:199-290` (`BottomBar`)
- Test: `test/ReaderMotionUiTest.kt`

**Interfaces:**
- Produces:
  - `@Composable internal fun ChapterBanner(state: ChapterBannerState, pageCount: Int?, modifier: Modifier = Modifier)`
  - `@Stable internal class ChapterBannerState { var chapterName: String?; fun show(name: String) }`. `show` sets the name and bumps an internal `Int` token. The banner auto-hides 2_000 ms after the last `show`.
  - `internal const val CHAPTER_BANNER_MILLIS = 2_000L`
  - In `AppBars.kt`, `chapterChange: (String) -> Unit` becomes `chapterChange: (name: String, isNext: Boolean) -> Unit` on `FloatingFloatingActionButton` and `BottomBar`.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun bannerShowsThenHides() = runComposeUiTest {
    val state = ChapterBannerState()
    setContent { ChapterBanner(state, pageCount = 24) }
    runOnIdle { state.show("Ch. 43") }
    onNodeWithText("Ch. 43").assertIsDisplayed()
    onNodeWithText(" · 24 pages", substring = true).assertIsDisplayed()
    mainClock.advanceTimeBy(CHAPTER_BANNER_MILLIS + 1_000)
    onNodeWithText("Ch. 43").assertDoesNotExist()
}
```

- [ ] **Step 2: Run it and confirm it fails.** Expected: compile FAIL.

- [ ] **Step 3: Implement `ChapterBanner`.**
  - A pill shaped `CircleShape`, with `primary` container and `onPrimary` text. Shadow elevation is 6.dp.
  - Enter: `slideInVertically { -it } + scaleIn(spring(dampingRatio = 0.6f))` (approved overshoot) + `fadeIn(fastEffects)`.
  - Exit: `slideOutVertically { -it } + scaleOut(targetScale = .8f) + fadeOut(fastEffects)`.
  - Auto-hide runs from a `LaunchedEffect(token)` → `delay(CHAPTER_BANNER_MILLIS)`.
  - The page-count suffix shows when `pageCount != null`.

- [ ] **Step 4: Wire it into `ReaderCompose`.**
  - Replace `showToast` with `fun onChapterChange(name: String, isNext: Boolean)`. It calls `banner.show(name)`. When `readerType != ReaderType.List`, it also launches `chapterSlide.snapTo(if (isNext) 1f else -1f); chapterSlide.animateTo(0f, motionScheme.defaultSpatialSpec())`.
  - `chapterSlide` is a `remember { Animatable(0f) }`. It applies as `graphicsLayer { translationX = chapterSlide.value * size.width }` on the reader content `Box`.
  - The bottom bar passes `{ onChapterChange(it, true) }` to `loadNextChapter` and `{ onChapterChange(it, false) }` to `loadPreviousChapter`. Inside the FAB and `BottomBar`, use `nextChapter = { chapterChange(it, true) }` and `previousChapter = { chapterChange(it, false) }`.
  - Draw `ChapterBanner` in the content `Box`, aligned `TopCenter`, with top padding `p.calculateTopPadding() + 8.dp`. Pass `pageCount = pagesInCurrentChapter.takeUnless { viewModel.isLoadingPages }`.
  - Remove the notifier leftovers listed in Global Constraints.

- [ ] **Step 5: Review checklist (Review Focus 2).** Run `grep -n "chapterSlide\|banner.show" reader/ReaderCompose.kt`. Every hit must sit inside `onChapterChange`, and nothing may read `viewModel.currentChapter` to trigger either one.

- [ ] **Step 6: Run the test and build.** Run the jvmTest command, then `./gradlew :mangaworld:assembleNoFirebaseDebug`. Expected: PASS, then BUILD SUCCESSFUL.

- [ ] **Step 7: Commit.** `git commit -m "feat(reader): chapter banner and slide replace marked-read toast"`

---

### Task 5: Bookmark pop and burst (F)

**Files:**
- Modify: `reader/AppBars.kt:73-125` (`ReaderTopBar`, bookmark `IconToggleButton`)
- Test: `test/ReaderMotionUiTest.kt`

**Interfaces:**
- Produces: `@Composable internal fun BookmarkToggle(isBookmarked: Boolean, onBookmarkClick: (Boolean) -> Unit, modifier: Modifier = Modifier)`, which draws its burst under the test tag `"bookmarkBurst"` while the burst is running.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun burstOnlyOnUserAdd() = runComposeUiTest {
    mainClock.autoAdvance = false
    var bookmarked by mutableStateOf(false)
    setContent { BookmarkToggle(bookmarked, onBookmarkClick = { bookmarked = it }) }
    bookmarked = true                       // flow catching up on open
    mainClock.advanceTimeByFrame()
    onNodeWithTag("bookmarkBurst").assertDoesNotExist()
    bookmarked = false; mainClock.advanceTimeBy(1_000)
    onNodeWithContentDescription("Bookmark chapter").performClick()
    mainClock.advanceTimeByFrame()
    onNodeWithTag("bookmarkBurst").assertExists()
    mainClock.advanceTimeBy(1_000)
    onNodeWithTag("bookmarkBurst").assertDoesNotExist()
}
```

- [ ] **Step 2: Run it and confirm it fails.** Expected: compile FAIL.

- [ ] **Step 3: Implement `BookmarkToggle`.**
  - Keep the existing `IconToggleButton`, icons, tint and content descriptions.
  - Fill: draw the outlined icon, then the filled icon clipped by `drawWithContent { clipRect(bottom = size.height * fill) { this@drawWithContent.drawContent() } }`. `fill` is `animateFloatAsState(if (isBookmarked) 1f else 0f, motionScheme.defaultSpatialSpec())`.
  - Pop: an `Animatable` scale 1f→1.35f→1f using `spring(dampingRatio = Spring.DampingRatioMediumBouncy)`. Run it, plus the burst and `haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)`, only inside `onCheckedChange` when the new value is `true`.
  - Burst: a `burstToken` counter that the click bumps. While its `Animatable` (0→1, `tween(500)`) is running, a `Canvas` tagged `"bookmarkBurst"` draws six 1.5.dp primary dots. Their unit vectors are (-1,-.6), (1,-.6), (-1,.4), (1,.4), (0,-1) and (0,1), placed at 14.dp × t from the center, with alpha 1-t. The `Canvas` is not clipped, so the dots spill into the top bar. Nothing is drawn below the bar.
  - `ReaderTopBar` calls `BookmarkToggle(isBookmarked, onBookmarkClick)` inside its existing `if (allowBookmark)`.

- [ ] **Step 4: Run the test and confirm it passes.** Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "feat(reader): bookmark pop and burst on add"`

---

### Task 6: Reading-progress hairline (G)

**Files:**
- Create: `reader/ReadingProgress.kt`
- Modify: `reader/ReaderMotion.kt`, `reader/ReaderCompose.kt` (the content `Box`)
- Test: `test/ReaderMotionTest.kt`

**Interfaces:**
- Produces:
  - `internal fun chapterProgress(currentPage: Int, pages: Int): Float` = `(currentPage + 1f) / pages`, clamped to 0..1, and 0f when pages ≤ 0
  - `@Composable internal fun ReadingProgressLine(progress: Float, visible: Boolean, pageLabel: String, scrolling: Boolean, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun progressIsFraction() = assertEquals(0.5f, chapterProgress(currentPage = 11, pages = 24))
@Test fun progressClamps() {
    assertEquals(1f, chapterProgress(30, 24)); assertEquals(0f, chapterProgress(0, 0))
}
```

- [ ] **Step 2: Run them and confirm they fail.** Expected: compile FAIL.

- [ ] **Step 3: Implement it.**
  - `ReadingProgressLine` is an `AnimatedVisibility(visible, fadeIn(defaultEffects), fadeOut(fastEffects))`.
  - Inside it is a `Box(Modifier.fillMaxWidth(shown).height(3.dp))`, where `shown = animateFloatAsState(progress, motionScheme.defaultSpatialSpec())`. Its colour animates with `animateColorAsState` (defaultEffects) from `primary` to `tertiary` once `progress >= 1f`.
  - The chip shows `pageLabel` in `labelSmall` on a `scrim.copy(alpha = .55f)` pill, top-end, 8.dp inset. It is visible while `scrolling`, and stays 600 ms after scrolling stops: `LaunchedEffect(scrolling)` + `delay`.
  - In `ReaderCompose`, draw it aligned `TopStart` in the content `Box`, with:
    - `visible = !showItems`
    - `progress = chapterProgress(currentPage, pagesInCurrentChapter)`
    - `pageLabel = "p. ${currentPage + 1} / $pagesInCurrentChapter"`
    - `scrolling = listState.isScrollInProgress || pagerState.isScrollInProgress`

- [ ] **Step 4: Run the tests and build.** Expected: PASS, then BUILD SUCCESSFUL.

- [ ] **Step 5: Commit.** `git commit -m "feat(reader): progress hairline while bars are hidden"`

---

### Task 7: List ↔ Pager morph (H)

**Files:**
- Modify: `reader/ReaderCompose.kt:537-606` (`Crossfade(readerType)`), `:623-682` (`ListView`, `PagerView`), `:758-800` (`reader()`, `lazyKeys`)
- Modify: `reader/ReaderMotion.kt`
- Test: `test/ReaderMotionTest.kt`

**Interfaces:**
- Produces:
  - `internal fun morphsPages(from: ReaderType, to: ReaderType): Boolean`, true only for List↔Pager
  - `ListView(..., pageModifier: (key: String) -> Modifier = { Modifier })`
  - `PagerView(..., pageModifier: (key: String) -> Modifier = { Modifier })`
  - `lazyKeys()` goes from `private` to `internal`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun onlyListAndPagerMorph() {
    assertTrue(morphsPages(ReaderType.List, ReaderType.Pager))
    assertTrue(morphsPages(ReaderType.Pager, ReaderType.List))
    assertFalse(morphsPages(ReaderType.List, ReaderType.FlipPager))
    assertFalse(morphsPages(ReaderType.CurlPager, ReaderType.Pager))
}
```

- [ ] **Step 2: Run it and confirm it fails.** Expected: compile FAIL.

- [ ] **Step 3: Implement it.**
  - Wrap the reader in `SharedTransitionLayout`. Replace `Crossfade` with `AnimatedContent(readerType, modifier = <existing zoomable modifier>, transitionSpec = { fadeIn(defaultEffects) togetherWith fadeOut(fastEffects) })`.
  - In the content lambda, build `pageModifier = { key -> if (morphsPages(initialState, targetState)) Modifier.sharedElement(rememberSharedContentState(key), this@AnimatedContent, boundsTransform = { _, _ -> motionScheme.defaultSpatialSpec() }) else Modifier }`. Read `initialState`/`targetState` from `transition`, and pass `pageModifier` to `ListView` and `PagerView`.
  - In `reader()` and `PagerView`, wrap each `PageItem.Page`'s `ChapterPage` in `Box(pageModifier(keys[index]))`. Transition items get no shared element.
  - The existing `snapshotFlow` syncs already open the target view at the same index, so add no new scroll code.

- [ ] **Step 4: Run the test and build.** Expected: PASS, then BUILD SUCCESSFUL.

- [ ] **Step 5: Commit.** `git commit -m "feat(reader): list and pager morph with shared pages"`

---

### Task 8: Device check

- [ ] **Step 1:** Run `cdm acquire --android --category phone`, then `./gradlew :mangaworld:installNoFirebaseDebug`.
- [ ] **Step 2:** Check each item in a real chapter and note pass or fail:
  - A: a fresh page shows the skeleton and settles in. Scrolling back shows no fade.
  - B: turn on airplane mode, open a page, then turn it off and tap retry.
  - C: the card plays once, and scrolling back does not replay it.
  - D: Next/Previous in Pager slides and shows the banner. List mode shows the banner only. Scrolling across a chapter boundary in Pager and in List does neither (Review Focus 2).
  - F: opening an already-bookmarked chapter shows no burst. Adding a bookmark bursts, and removing one does not.
  - G: the line shows only with the bars hidden and turns tertiary on the last page.
  - H: List↔Pager morphs, the switch to Flip or Curl fades, and the scroll position is kept.
- [ ] **Step 3:** Run `cdm release --android`. Fix any failure in its owning task's files, and commit with `fix(reader): …`.
