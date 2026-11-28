# Reader Page Indicator Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the approved revisions to all 8 MangaWorld reader page-indicator styles and add 3 new styles (Ring, Wavy, Dots).

**Architecture:** All indicator UI lives in `mangaworld/shared/.../reader/` (`PageIndicators.kt`, `ReadingProgress.kt`). Pure layout math goes in `ReaderMotion.kt` and is unit-tested in `PageIndicatorMathTest`. New styles are new values in the Wire proto enum `PageIndicatorStyle`; the pickers already iterate `PageIndicatorStyle.entries`, so they get the new cards for free once `label` and the overlay `when` handle them. The scrubber thumbnail comes from a composable slot the reader fills with a Kamel image.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform Material 3 (Expressive), Wire protobuf, Kamel, kotlin.test (jvmTest).

**Spec:** Approved mockup https://claude.ai/artifact/XaWKRmpeJ8E8GNbZ1vsEfG (local copy: scratchpad `page-indicators.html`). Values below are copied from it. Memory: `project_reader_indicator_polish`, `project_reader_page_number`.

## Global Constraints

- `PageIndicatorStyle.Classic` stays the default; `hidePageNumberInBar` behavior unchanged.
- One style at a time. The top progress line shows for every style except Segments (which replaces it) and Wavy (which replaces it); it springs to new values with `spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)`.
- No leading-edge dot on any line (user rejected it).
- Line turns `tertiary` on the last page in every style that draws progress (line, segments, ring, wavy).
- Page numbers in every chip/pill/flag/bubble use tabular digits: `TextStyle(fontFeatureSettings = "tnum")`, current page via `RollingNumber`, total at 70% alpha (existing `PageCount`).
- New proto values are appended: `Ring = 8; Wavy = 9; Dots = 10;`. Never renumber existing values.
- Keep the plain `Slider(value, onValueChange, …)` overload (the thumb/track overload throws `NoSuchMethodError` between CMP and Android material3).
- Build with the `noFirebase` flavor. Install on one device with `assemble` + `adb -s <serial> install` (Gradle install hits every device). Debug package id: `com.programmersbox.mangaworld.noFirebase.debug`.

## Review Focus

1. **Chapter with 1 page.** Segments, Scrubber, Ring, Dots must not divide by zero or show a "1 / 1" scrubber; expect: scrubber/morph stay collapsed (existing `pages > 1` guards), segments draw one full segment. → `segmentFillSinglePage` test in Task 1.
2. **End card (`currentPage == pages`).** `displayPage` caps at `pages`; segments must show all full, not overflow. → `segmentFillOnEndCard` test in Task 1.
3. **Very long chapters (200+ pages).** Segments group, ticks hide past 40, dots window stays 7 wide. → `segmentCountCapsAtTwenty` and `ticksHiddenPastForty` tests in Task 1.
4. **Classic chip near the edges.** On page 1 and the last page the chip must stay ≥ 8dp from the screen edge. → `chipClampsAtEdges` test in Task 1.
5. **Expressive wavy APIs at runtime on Android.** `LinearWavyProgressIndicator` / `CircularWavyProgressIndicator` can hit the same CMP-vs-Android `NoSuchMethodError` as `Slider`. → Task 8 Step 5 opens the Wavy and Ring styles on the emulator, not only desktop.

---

## File Structure

| File | Change |
|---|---|
| `datastore/mangasettings/src/commonMain/proto/manga_settings.proto` | add `Ring`, `Wavy`, `Dots` |
| `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReaderMotion.kt` | new pure helpers (segments grouping, chip clamp, ticks, dot window) |
| `mangaworld/shared/src/jvmTest/kotlin/com/programmersbox/manga/shared/reader/PageIndicatorMathTest.kt` | tests for those helpers |
| `.../reader/ReadingProgress.kt` | Classic chip, Line-only swell + ticks, Segments B, shared `IndicatorChip` |
| `.../reader/PageIndicators.kt` | Pill, Scrubber, Rail, Folio, Morph revisions; overlay `when` for new styles; thumbnail slot |
| `.../reader/NewPageIndicators.kt` (create) | `PageRing`, `WavyProgressLine`, `PageDotWindow` |
| `.../reader/ReaderCompose.kt:797-807` | pass `pageThumbnail` slot |
| `.../reader/PageIndicatorPicker.kt` | preview passes a `MockPage` thumbnail slot |
| `.../onboarding/OnboardingItems.kt` | only if a `when (style)` there stops compiling |

---

### Task 1: Layout math helpers

**Files:**
- Modify: `.../reader/ReaderMotion.kt:64-67`
- Test: `mangaworld/shared/src/jvmTest/.../reader/PageIndicatorMathTest.kt`

**Interfaces:**
- Produces:
  - `internal const val SEGMENT_GROUP_THRESHOLD = 30`
  - `internal const val MAX_GROUPED_SEGMENTS = 20`
  - `internal fun pagesPerSegment(pages: Int): Int` — 1 when `pages <= 30`, else `ceil(pages / 20.0)`
  - `internal fun segmentCount(pages: Int): Int` — `ceil(pages / pagesPerSegment)`, min 1
  - `internal fun segmentFill(segment: Int, index: Int, pages: Int): Float` — 1 for segments before the one holding `index`, 0 after, partial `(index - first + 1) / pagesInThisSegment` for the current one; `index` is 0-based and clamped to `pages - 1`
  - `internal fun clampedChipX(tip: Float, chipWidth: Float, width: Float, margin: Float): Float` — `(tip - chipWidth/2).coerceIn(margin, width - chipWidth - margin)`; if the chip is wider than `width - 2*margin`, return `margin`
  - `internal const val MAX_TICK_PAGES = 40`
  - `internal fun showScrubberTicks(pages: Int): Boolean = pages in 2..MAX_TICK_PAGES`
  - `internal const val DOT_WINDOW = 7`
- `usesSegments(pages)` becomes `pages >= 1` (grouping removes the 60-page cap); delete `MAX_SEGMENTS` and update `segmentsOnlyForShortChapters`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun oneSegmentPerPageUpToThirty() { assertEquals(1, pagesPerSegment(30)); assertEquals(30, segmentCount(30)) }
@Test fun segmentCountCapsAtTwenty() { assertEquals(3, pagesPerSegment(60)); assertEquals(20, segmentCount(60)); assertTrue(segmentCount(250) <= MAX_GROUPED_SEGMENTS) }
@Test fun segmentFillPartialInGroup() {
    // 60 pages, 3 per segment; index 4 = page 5 is the 2nd page of segment 1
    assertEquals(1f, segmentFill(0, 4, 60)); assertEquals(2f / 3f, segmentFill(1, 4, 60), 0.001f); assertEquals(0f, segmentFill(2, 4, 60))
}
@Test fun segmentFillSinglePage() = assertEquals(1f, segmentFill(0, 0, 1))
@Test fun segmentFillOnEndCard() = assertEquals(1f, segmentFill(segmentCount(24) - 1, 24, 24))
@Test fun segmentsForAnyLength() { assertTrue(usesSegments(1)); assertTrue(usesSegments(500)); assertFalse(usesSegments(0)) }
@Test fun chipClampsAtEdges() {
    assertEquals(8f, clampedChipX(tip = 10f, chipWidth = 70f, width = 260f, margin = 8f))
    assertEquals(182f, clampedChipX(tip = 260f, chipWidth = 70f, width = 260f, margin = 8f))
    assertEquals(95f, clampedChipX(tip = 130f, chipWidth = 70f, width = 260f, margin = 8f))
}
@Test fun ticksHiddenPastForty() { assertTrue(showScrubberTicks(40)); assertFalse(showScrubberTicks(41)); assertFalse(showScrubberTicks(1)) }
```

- [ ] **Step 2: Run, expect compile FAIL (unresolved references)**

Run: `./gradlew :mangaworld:shared:jvmTest --tests "com.programmersbox.manga.shared.reader.PageIndicatorMathTest"`

- [ ] **Step 3: Implement the helpers in `ReaderMotion.kt`; replace `MAX_SEGMENTS`/`usesSegments`**

- [ ] **Step 4: Run the same command, expect PASS**

- [ ] **Step 5: Commit** `feat(reader): page indicator layout math for grouped segments, chip clamp, ticks`

---

### Task 2: Classic chip and Line-only swell

**Files:** Modify `.../reader/ReadingProgress.kt`, `.../reader/PageIndicators.kt:117-124`

**Interfaces:**
- Consumes: `clampedChipX` (Task 1).
- Produces: `@Composable internal fun IndicatorChip(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit)` — shape `CircleShape`, background `Color(0xFF101114).copy(alpha = .72f)`, padding `horizontal = 12.dp, vertical = 5.dp`, content color White, text style `labelMedium` (12sp) + `fontFeatureSettings = "tnum"`. Used by Tasks 2, 3. Replaces the private `PageChip`.
- `ReadingProgressLine` gains `swellOnTurn: Boolean = false` and `quarterTicks: Boolean = false`. Its `pageLabel: String?` becomes `page: Int?` + `pages: Int` so the chip can roll.

- [ ] **Step 1: Classic chip.** Chip = `IndicatorChip { Text("p. "); PageCount(page, pages) }` positioned 10.dp below the line, x from `clampedChipX(tip = width * shown, chipWidth, width, 8.dp.toPx())` in `Modifier.layout`. Use the springing `shown` value so the chip rides with the line. Notch: 8.dp rotated square (`Modifier.rotate(45f)`) centered on the tip at the chip's top edge, same color. Enter/exit unchanged (fade, 600ms linger).
- [ ] **Step 2: Line only.** When `swellOnTurn`, height = `animateDpAsState(if (showChip) 5.dp else 2.dp, MaterialTheme.motionScheme.defaultSpatialSpec())`; while swollen add a glow (`Modifier.shadow(6.dp, RoundedEndShape, ambientColor = color, spotColor = color)` on the fill). `quarterTicks` draws 1.dp × 6.dp ticks at 25/50/75% with `Color.Gray.copy(alpha = .6f)` in `drawBehind`. Overlay passes both `true` for `LineOnly` (pass `showChip = recentlyTurned` for it too).
- [ ] **Step 3: Verify** `./gradlew :mangaworld:shared:compileKotlinJvm` passes, then run `./gradlew :mangaworld:desktop:run`, Reader Settings → page indicator picker: Classic chip follows the line tip and stays inside the edges on page 1 and the last page; Line only swells per turn.
- [ ] **Step 4: Commit** `feat(reader): classic chip rides the progress line; line-only swells on turn`

---

### Task 3: Story segments (B with roomy chip)

**Files:** Modify `.../reader/ReadingProgress.kt` (`SegmentedProgressLine`)

**Interfaces:** Consumes `pagesPerSegment`, `segmentCount`, `segmentFill` (Task 1), `IndicatorChip` (Task 2).

- [ ] **Step 1: Layout.** One `Row(verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(8.dp))`, padding `start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp`. Segment row (`weight(1f)`, `spacedBy(3.dp)`) of `segmentCount(pages)` boxes; then `IndicatorChip { PageCount(index + 1, pages) }` at the end, always visible, alpha `animateFloatAsState(if (showChip) 1f else .75f)`. Remove the old `Column` + chip below.
- [ ] **Step 2: Segments.** Height 3.dp, current segment 4.dp (`animateDpAsState`). Track: `drawRect(Color.White.copy(alpha = .42f))` then a 0.75.dp `Stroke` in `Color.Black.copy(alpha = .35f)`; fill `drawRect(color, width * fill)` where `fill` animates to `segmentFill(i, index, pages)` with `defaultSpatialSpec()`. Delete `SegmentTrack`.
- [ ] **Step 3: Verify** on desktop picker preview at 24 pages; temporarily set `PREVIEW_PAGES = 60` locally to see grouping (20 segments, partial fill), then revert.
- [ ] **Step 4: Commit** `feat(reader): story segments get inline roomy count, readable track, grouping past 30 pages`

---

### Task 4: Pill and Morph collapsed state

**Files:** Modify `.../reader/PageIndicators.kt` (`PageCount`, `PagePill`, `MorphingPageIndicator`)

- [ ] **Step 1:** `PageCount` applies `fontFeatureSettings = "tnum"` to both texts.
- [ ] **Step 2: Pill.** Padding `horizontal = 16.dp, vertical = 8.dp`. Background fill: `drawBehind { drawRect(primary.copy(alpha = .26f), size = size.copy(width = size.width * shown)) }` with `shown` springing (0.6f damping) to `page / pages`. Idle: alpha `.7f` and `scaleX/scaleY .9f` via `graphicsLayer`, animated with `defaultEffectsSpec` / `defaultSpatialSpec`.
- [ ] **Step 3: Morph collapsed.** `Column(horizontalAlignment = CenterHorizontally, verticalArrangement = spacedBy(3.dp))` with `PageCount` and a 2.dp-tall, rounded bar (`White.copy(.18f)` track, `primary` fill at `shown`), padding `horizontal = 16.dp, vertical = 7.dp`. Idle alpha `.7f` (was `.5f`).
- [ ] **Step 4: Verify** desktop picker: pill width does not jump between 9→10 and 19→20.
- [ ] **Step 5: Commit** `feat(reader): pill shows progress fill; morph pill gets mini progress bar`

---

### Task 5: Scrubber ticks and thumbnail bubble

**Files:** Modify `.../reader/PageIndicators.kt` (`PageIndicatorOverlay`, `PageScrubber`, `MorphingPageIndicator`), `.../reader/ReaderCompose.kt:797-807`, `.../reader/PageIndicatorPicker.kt` (`PageIndicatorPreview`)

**Interfaces:**
- Consumes: `showScrubberTicks` (Task 1).
- Produces: `PageIndicatorOverlay(..., pageThumbnail: @Composable (index: Int) -> Unit = {})` threaded to `PageScrubber(..., pageThumbnail)`.

- [ ] **Step 1: Ticks.** When `showScrubberTicks(pages)`, draw 2.dp dots in `White.copy(.55f)` for each page along the track, inset `ScrubberThumbInset` at both ends, in a `drawBehind` on the slider's `Box` (behind the `Slider`).
- [ ] **Step 2: Bubble.** Replace the number-only bubble with a `Column` (primary background, `RoundedCornerShape(14.dp)`, padding 6.dp, `spacedBy(4.dp)`): a 40×56.dp clipped (4.dp corners) `pageThumbnail(target)` box, then `Text("p. ${target + 1}", labelMedium, onPrimary, tnum)`. Enter: `scaleIn(initialScale = .9f) + fadeIn`, keep the existing zero-size `Modifier.layout` placement. Row height 56.dp, horizontal padding 18.dp; the current-page `RollingNumber` gets `Modifier.widthIn(min = 20.dp)` so the track doesn't shift.
- [ ] **Step 3: Slot wiring.** In `ReaderCompose.kt` pass `pageThumbnail = { i -> currentChapterPageUrls.getOrNull(i)?.let { url -> KamelImage(resource = { asyncPainterResource(url) { requestBuilder { viewModel.headers.forEach { (k, v) -> header(k, v) } } } }, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) } }` (same loader as `SheetView`, `Sheets.kt:332`). In `PageIndicatorPreview` pass `pageThumbnail = { MockPage(it) }` scaled to fit.
- [ ] **Step 4: Verify** `./gradlew :mangaworld:assembleNoFirebaseDebug` and `:mangaworld:desktop:run`; in a real chapter on desktop drag the scrubber: bubble shows the target page image and "p. N".
- [ ] **Step 5: Commit** `feat(reader): scrubber page ticks and thumbnail bubble`

---

### Task 6: Edge rail and Printed folio

**Files:** Modify `.../reader/PageIndicators.kt` (`PageRail`, `PageFolio`, constants)

- [ ] **Step 1: Rail.** Flag padding `horizontal = 10.dp, vertical = 5.dp`, shape `RoundedCornerShape(12.dp, 3.dp, 3.dp, 12.dp)`, tabular `PageCount`. Read dots `lerp(primary, Color.White, .45f)`; current dot `RailCurrentDot = 20.dp`; `RailPadding = 10.dp`, horizontal 3.dp. Idle alpha `.5f` with `scrim.copy(alpha = .18f)` background (active stays `.4f`). Update `flagCenter` for the new sizes.
- [ ] **Step 2: Folio.** Remove the `surface .85` box. Background `Brush.radialGradient(0f to Color(0xC70A0A0C), .6f to Color(0x730A0A0C), 1f to Transparent, center = bottom-center, radius = 1.2 × max(size))`, `RoundedCornerShape(14.dp)`, padding `start/end = 14.dp, top = 8.dp, bottom = 6.dp`. Content: 22.dp × 1.dp hairline (`White.copy(.6f)`), 4.dp gap, then `Row(Bottom, spacedBy(6.dp))` of serif `RollingNumber` (30sp, White) and `"OF $pages"` (`labelSmall`, `letterSpacing = 0.12.em`, alpha .75, bottom padding 7.dp). Column aligns start/end to match `folioOnStart`. Enter/exit `slideInHorizontally { if (start) -it / 4 else it / 4 } + fadeIn` (and mirrored exit).
- [ ] **Step 3: Verify** desktop picker shows both; rail flag text no longer touches its edges.
- [ ] **Step 4: Commit** `feat(reader): roomier rail flag with tinted read dots; folio without the box`

---

### Task 7: New enum values and Progress ring

**Files:** Modify `datastore/mangasettings/src/commonMain/proto/manga_settings.proto:40-49`, `.../reader/PageIndicators.kt` (`label`, overlay `when`); Create `.../reader/NewPageIndicators.kt`

**Interfaces:**
- Produces: `PageIndicatorStyle.Ring | Wavy | Dots`; labels `"Progress ring"`, `"Wavy line"`, `"Dot window"`.
- `@Composable internal fun PageRing(page: Int, pages: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1:** Append `Ring = 8; Wavy = 9; Dots = 10;` to the proto; add labels; overlay `when` gets `Ring -> PageRing(...)` aligned `BottomEnd` with `aboveBars` + `padding(12.dp)`, and `Wavy, Dots ->` stubs `Unit` until Task 8.
- [ ] **Step 2: `PageRing`.** Size `animateDpAsState(if (active) 44.dp else 26.dp, spring(DampingRatioMediumBouncy))`, `CircleShape` background `IndicatorContainer`, `CircularWavyProgressIndicator(progress = { shown }, color = primary or tertiary on last page, trackColor = White.copy(.18f))` filling it; centered `RollingNumber(page)` (13sp, semibold, tnum) with `AnimatedVisibility(active, fadeIn + scaleIn(.6f))`. Idle alpha .75. Click → `onClick` (opens Pages).
- [ ] **Step 3: Verify** `./gradlew :mangaworld:shared:jvmTest` (enum regenerated, existing tests pass) and desktop picker shows the Progress ring card.
- [ ] **Step 4: Commit** `feat(reader): progress ring page indicator`

---

### Task 8: Wavy line and Dot window

**Files:** Modify `.../reader/NewPageIndicators.kt`, `.../reader/PageIndicators.kt` (overlay)

**Interfaces:**
- Consumes: `DOT_WINDOW` (Task 1).
- Produces: `@Composable internal fun WavyProgressLine(progress: Float, visible: Boolean, turning: Boolean, modifier: Modifier = Modifier)`; `@Composable internal fun PageDotWindow(index: Int, pages: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: Wavy.** In the overlay, `Wavy` uses `WavyProgressLine` in place of `ReadingProgressLine`. Body: `LinearWavyProgressIndicator(progress = { shown }, amplitude = { amp }, color = primary/tertiary, trackColor = Color.Gray.copy(.35f), modifier = fillMaxWidth())` where `shown` springs (0.6f) and `amp = animateFloatAsState(if (turning) 1f else 0f, tween(400))`. `turning = recentlyTurned`. Hidden while `showItems` (same fade as the line).
- [ ] **Step 2: Dot window.** Container 150.dp × 28.dp, `IndicatorContainer`, `CircleShape`, aligned `BottomCenter` above bars, alpha `.7f` idle. Inside, a `Row(spacedBy(6.dp))` of all `pages` dots (6.dp circles; read = `primary.copy(.55f)`, unread = `White.copy(.45f)`, current = `primary`), offset with `animateIntOffsetAsState` so the current dot's center sits at 75.dp (`defaultSpatialSpec`). Current dot width `animateDpAsState(if (active) 30.dp else 16.dp, spring(DampingRatioMediumBouncy))`, height 18.dp/6.dp, shows `Text("${index + 1}", 11sp semibold, onPrimary, tnum)` when active. Fade the edges: `graphicsLayer(compositingStrategy = Offscreen)` + `drawWithContent { drawContent(); drawRect(Brush.horizontalGradient(0f to Transparent, .22f to Black, .78f to Black, 1f to Transparent), blendMode = BlendMode.DstIn) }`. Only compose dots within `index ± DOT_WINDOW` to keep long chapters cheap.
- [ ] **Step 3: Verify desktop:** both styles animate in the picker.
- [ ] **Step 4: Verify Android:** `./gradlew :mangaworld:assembleNoFirebaseDebug`, `adb -s <emulator> install -r mangaworld/build/outputs/apk/noFirebase/debug/*.apk`, open a chapter with Wavy, then Ring: no `NoSuchMethodError` in `adb logcat -s AndroidRuntime`.
- [ ] **Step 5: Commit** `feat(reader): wavy line and dot window page indicators`

---

### Task 9: Whole-feature check

- [ ] **Step 1:** `./gradlew :mangaworld:shared:jvmTest :mangaworld:assembleNoFirebaseDebug :mangaworld:desktop:compileKotlinJvm` all green.
- [ ] **Step 2:** On the emulator, open a real chapter and cycle all 11 styles from the in-reader settings sheet; record with `adb shell screenrecord` at 5× animator scale for review (see `reference_emulator_motion_testing`). Check a dark page for Segments contrast.
- [ ] **Step 3:** Onboarding page-indicator step shows 11 options and still compiles (`OnboardingItems.kt:203`).
- [ ] **Step 4:** Commit any fixes; leave PR creation to the user.

---

## ETA

| Task | Native | Subagent-driven |
|---|---|---|
| 1 Math | 15m | 20m |
| 2 Classic + Line | 25m | 35m |
| 3 Segments | 20m | 30m |
| 4 Pill + Morph | 15m | 25m |
| 5 Scrubber | 30m | 40m |
| 6 Rail + Folio | 20m | 30m |
| 7 Enum + Ring | 20m | 30m |
| 8 Wavy + Dots | 30m | 40m |
| 9 Whole check | 20m | 30m |
| **Total** | **~3h 15m** | **~4h 20m** |

Started 6:30 PM EST → native done ≈ 9:45 PM, subagent-driven ≈ 10:50 PM (Gradle builds dominate; add ~20m if the expressive wavy APIs need a workaround).
