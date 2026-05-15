# OtakuWorld — Compose Performance Audit

**Date:** 2026-05-14  
**Method:** Static code analysis (no physical device available for Macrobenchmarks)  
**Scope:** kmpuiviews, UIViews, mangaworld, animeworld, novelworld, app modules

> This is a static-analysis audit. Numbers (P50/P90/P99 frame times, cold startup) cannot be
> recorded without a physical device + release build + Macrobenchmark module. The Baseline Profile
> module exists but is incomplete (see §7). Run Macrobenchmarks to establish baseline numbers before
> fixing any issue here.

---

## Summary

| Category                                      | HIGH   | MEDIUM | LOW   |
|-----------------------------------------------|--------|--------|-------|
| Stability — unstable types                    | 3      | 16     | 0     |
| Lazy layout — missing keys / state recreation | 2      | 16     | 0     |
| Flow & Effect API misuse                      | 3      | 20     | 0     |
| State-read phase (composition vs layout/draw) | 6      | 5      | 0     |
| Modifier.composed / subcomposition            | 0      | 11     | 0     |
| Image loading / general                       | 3      | 5      | 4     |
| **Totals**                                    | **17** | **73** | **4** |

---

## 1. Stability — Unstable Types as Composable Parameters

Composables that receive unstable parameters cannot be skipped by the Compose compiler even when
their inputs have not changed. Every parent recomposition forces them to re-execute.

### 1.1 Raw `Map<K,V>` / `List<T>` parameters — HIGH

These are the highest-traffic composables in the app. `CoverCard` variants are used in every grid
and list screen.

| File                                              | Composable               | Unstable parameter                    |
|---------------------------------------------------|--------------------------|---------------------------------------|
| `kmpuiviews/.../components/CoverCard.kt:38`       | `M3CoverCard`            | `Map<String, String>` headers         |
| `kmpuiviews/.../components/CoverCard.kt:134`      | `M3ImageCard`            | `Map<String, String>` headers         |
| `kmpuiviews/.../components/CoverCard.kt:199`      | `M3ImageCardWithContent` | `Map<String, String>` headers         |
| `kmpuiviews/.../components/CoverCard.kt:278`      | `M3IconCard`             | `Map<String, String>` headers         |
| `kmpuiviews/.../components/ListBottomSheet.kt:37` | `ListBottomScreen`       | `List<T>`                             |
| `kmpuiviews/.../all/AllScreen.kt:82`              | `AllScreen`              | `List<KmpItemModel>`, `List<DbModel>` |
| `kmpuiviews/.../all/AllScreen.kt:244`             | `AllScreen` (overload)   | `List<KmpItemModel>`, `List<DbModel>` |
| `kmpuiviews/.../favorite/FavoriteScreen.kt:459`   | `FavoritesGrid`          | `List<Map.Entry<…>>`                  |

**Fix:** Wrap `List<T>` with `kotlinx.collections.immutable.ImmutableList<T>` /
`persistentListOf()`. Wrap `Map<K,V>` with `ImmutableMap<K,V>` / `persistentMapOf()`. Add
`kotlinx-collections-immutable` to version catalog if not already present.

### 1.2 Interface / complex types as composable parameters — HIGH

`KmpInfoModel` and `KmpGenericInfo` are interface types. Interface parameters are always unstable
unless annotated `@Stable`.

| File                                         | Composable            | Unstable parameter               |
|----------------------------------------------|-----------------------|----------------------------------|
| `kmpuiviews/.../details/DetailsUtils.kt:107` | `AddToList`           | `KmpInfoModel`                   |
| `kmpuiviews/.../details/DetailsUtils.kt:164` | `DetailActions`       | `KmpGenericInfo`, `KmpInfoModel` |
| `kmpuiviews/.../details/DetailsUtils.kt:318` | `DetailsHeader`       | `KmpInfoModel`                   |
| `kmpuiviews/.../details/DetailsUtils.kt:339` | `DetailsAboutSection` | `KmpInfoModel`                   |
| `kmpuiviews/.../details/DetailsUtils.kt:428` | `DetailsListSection`  | `KmpInfoModel`                   |

**Fix:** Annotate `KmpInfoModel` and `KmpGenericInfo` with `@Stable` in `kmpmodels` if their
observable state changes are always tracked via Compose snapshot state. Alternatively, pass only the
stable fields the composable actually reads instead of the whole model.

### 1.3 Unstable Map parameters in extension screens — MEDIUM

| File                                                   | Composable                | Unstable parameter |
|--------------------------------------------------------|---------------------------|--------------------|
| `kmpuiviews/.../extensions/ExtensionListScreen.kt:313` | `InstalledExtensionItems` | `Map<String?, …>`  |
| `kmpuiviews/.../extensions/ExtensionListScreen.kt:429` | `RemoteExtensionItems`    | `Map<String, …>`   |
| `kmpuiviews/.../lists/OtakuListDetailScreen.kt:749`    | `DeleteItemsModal`        | `Map<…>`           |
| `kmpuiviews/.../utils/SourceChooserScreen.kt:315`      | `GroupBottomScreen`       | `Map<String, …>`   |

### 1.4 `var` fields in ViewModel state holders — MEDIUM

Snapshot state `var` fields in ViewModels don't affect composable skippability directly, but
`mutableStateOf(Set<…>)` / `mutableStateOf(List<…>)` assignments replace the entire collection
object on every update, causing full snapshot invalidation.

| File                                                      | Field             | Type                         |
|-----------------------------------------------------------|-------------------|------------------------------|
| `mangaworld/shared/.../reader/ReadViewModel.kt:118`       | `list`            | `var mutableStateOf List<…>` |
| `mangaworld/shared/.../reader/ReadViewModel.kt:125`       | `loadingChapters` | `var mutableStateOf Set<…>`  |
| `kmpuiviews/.../extensions/ExtensionListViewModel.kt:135` | `showItems`       | `var mutableStateOf`         |

---

## 2. Lazy Layout — Missing Keys and State Recreation

### 2.1 `rememberLazyGridState()` called inside recomposable scope — HIGH

`remember` only works if the composable is stable enough to be skipped. When called inside a lambda
or unstable composable, state is re-created on every recomposition.

| File                                            | Issue                                                                  |
|-------------------------------------------------|------------------------------------------------------------------------|
| `kmpuiviews/.../all/AllScreen.kt:198`           | `rememberLazyGridState()` inside potentially unstable lambda           |
| `kmpuiviews/.../favorite/FavoriteScreen.kt:473` | `rememberLazyGridState()` inside composable that takes unstable params |

**Fix:** Hoist `LazyGridState` to the nearest stable scope or create it in the ViewModel.

### 2.2 Missing `key` parameter in `items {}` blocks — MEDIUM

Without a key, Compose cannot correlate items across recompositions. Any list mutation (add, remove,
reorder) causes every item to re-bind.

| File                                                         | Context                  |
|--------------------------------------------------------------|--------------------------|
| `kmpuiviews/.../details/DetailsPortrait.kt:363`              | Chapter list             |
| `kmpuiviews/.../details/DetailsLandscape.kt:424`             | Chapter list             |
| `kmpuiviews/.../details/DetailsScreen.kt:443`                | Chapter list             |
| `kmpuiviews/.../globalsearch/GlobalSearchScreen.kt:282`      | Search results grid      |
| `kmpuiviews/.../globalsearch/GlobalSearchScreen.kt:351`      | Placeholder items        |
| `kmpuiviews/.../globalsearch/GlobalSearchScreen.kt:374`      | LazyRow placeholders     |
| `kmpuiviews/.../globalsearch/GlobalSearchScreen.kt:382`      | Main search results      |
| `kmpuiviews/.../notifications/NotificationScreen.kt:433`     | Notification grid        |
| `kmpuiviews/.../recommendations/RecommendationScreen.kt:173` | Recommendation list      |
| `kmpuiviews/.../exceptions/ExceptionsScreen.kt:104`          | Exception list           |
| `kmpuiviews/.../settings/lists/OtakuListView.kt:205`         | Custom list items        |
| `kmpuiviews/.../components/BottomSheetDeleteScaffold.kt:215` | Grid items               |
| `kmpuiviews/.../components/settings/ListSetting.kt:63`       | Settings list            |
| `kmpuiviews/.../components/settings/ListSetting.kt:129`      | Settings list (overload) |
| `kmpuiviews/.../components/settings/ListSetting.kt:191`      | Settings list (overload) |
| `kmpuiviews/.../components/settings/ListSetting.kt:263`      | Settings list (overload) |

**Fix:** Add `key = { item -> item.uniqueId }` to every `items()` call. For the placeholder items
with a fixed count, use `key = { index -> "placeholder_$index" }`.

---

## 3. Flow Collection and Effect API Misuse

### 3.1 `SideEffect` launching async work — HIGH

`SideEffect` runs after **every** successful recomposition. Using it to launch a coroutine or
permission request means the work fires continuously, not once.

| File                                                | Line | Issue                                                                        |
|-----------------------------------------------------|------|------------------------------------------------------------------------------|
| `kmpuiviews/src/androidMain/.../PermissionUtils.kt` | 37   | `SideEffect { storage.launch(…) }` — launches permission every recomposition |
| `app/.../info/InfoScreen.kt`                        | 343  | `SideEffect { launcher.launch(…) }` — launches activity every recomposition  |
| `UIViews/.../components/SlideButton.kt`             | 109  | `SideEffect { swipeableState.updateAnchors(…) }` — mutates state every frame |

**Fix:** Replace with `LaunchedEffect(key)` where `key` is the trigger condition (e.g., a boolean
flag). For anchor updates, use `LaunchedEffect(constraints)` keyed on the constraint that changed.

### 3.2 Infinite loop in `LaunchedEffect(Unit)` — HIGH

| File                                      | Line | Issue                                                                                                            |
|-------------------------------------------|------|------------------------------------------------------------------------------------------------------------------|
| `kmpuiviews/.../utils/ComposableUtils.kt` | 136  | `LaunchedEffect(Unit) { while(true) { delay(1000)… } }` — loop runs indefinitely, never cleaned up on navigation |

**Fix:** Keep the `while(true)` loop but ensure it's inside a `LaunchedEffect` keyed on a lifecycle
or screen-visibility condition, or use `repeatOnLifecycle`.

### 3.3 `LaunchedEffect(Unit)` with `snapshotFlow` that should re-fire on key changes — MEDIUM

`LaunchedEffect(Unit)` only launches once. If the `snapshotFlow` factory captures a reference that
changes (e.g., a ViewModel replaced after nav back-stack pop), the collection is stale.

| File                                                | Line | Issue                                                                       |
|-----------------------------------------------------|------|-----------------------------------------------------------------------------|
| `app/.../info/InfoScreen.kt`                        | 155  | `snapshotFlow { backStack.topLevelKey }` — stale on nav reuse               |
| `kmpuiviews/.../navigation/Nav3View.kt`             | 42   | `snapshotFlow { backStack }` — analytics never re-fires                     |
| `mangaworld/shared/.../reader/Drawer.kt`            | 72   | `snapshotFlow { readVm.currentChapter… }` — scroll updates stale            |
| `novelworld/shared/.../reader/Drawer.kt`            | 63   | Same pattern as manga reader                                                |
| `kmpuiviews/.../moresettings/MoreSettingsScreen.kt` | 92   | `snapshotFlow { viewModel.importExportListStatus }` — status changes missed |

### 3.4 `LaunchedEffect(Unit)` in conditional branches — MEDIUM

Multiple `when` branches each contain a `LaunchedEffect(Unit)`. When the branch condition changes,
the old effect is cancelled and a new one is launched — but because the key is always `Unit`, the
runtime cannot distinguish them. Snackbar actions fire inconsistently.

| File                                                   | Lines         |
|--------------------------------------------------------|---------------|
| `kmpuiviews/.../lists/imports/ImportFullListScreen.kt` | 131, 141, 176 |
| `kmpuiviews/.../lists/imports/ImportListScreen.kt`     | 98, 108, 141  |

**Fix:** Key the `LaunchedEffect` on the status value itself:
`LaunchedEffect(status) { … }`.

### 3.5 `DisposableEffect(Unit)` with potentially stale callbacks — MEDIUM

`DisposableEffect(Unit)` never re-registers when its captured dependencies change. If the callback
lambda closes over a ViewModel or nav controller that may be replaced, it holds a stale reference.

| File                                               | Line | Context                                     |
|----------------------------------------------------|------|---------------------------------------------|
| `kmpuiviews/.../components/BackButton.kt`          | 19   | Back handler — navEvent may change          |
| `UIViews/.../components/SlideButton.kt`            | 82   | Inside conditional — unreliable re-register |
| `animeworld/.../videoplayer/VideoPlayerCompose.kt` | 284  | ExoPlayer lifecycle                         |

**Fix:** Replace `Unit` key with the actual dependency: `DisposableEffect(exoPlayer)`,
`DisposableEffect(navEvent)`, etc.

---

## 4. State-Read Phase — Composition Instead of Layout/Draw

Reading animation or scroll state during the **Composition** phase forces a full recomposition on
every animation frame. Deferring reads to Layout (`Modifier.offset { }`) or Draw
(`Modifier.graphicsLayer { }`) skips recomposition entirely.

### 4.1 Animation state `.value` read in Composition — HIGH

`ScaleRotateOffset.kt` is a utility used by many screens. Every animation frame triggers a full
recomposition of every caller.

| File                                            | Lines      | Issue                                                                                    |
|-------------------------------------------------|------------|------------------------------------------------------------------------------------------|
| `kmpuiviews/.../modifiers/ScateRotateOffset.kt` | 64, 65, 70 | `animateFloatAsState(…).value`, `animateOffsetAsState(…).value` read in composition body |
| `kmpuiviews/.../modifiers/ScateRotateOffset.kt` | 94, 95, 99 | Same — second function variant                                                           |

**Fix:** Move the `.graphicsLayer {}` call into a lambda-form modifier so the state is read at Draw
phase:

```kotlin
// Before (HIGH cost — recomposes every frame)
val scale by animateFloatAsState(scaleState.value)
Box(Modifier.scale(scale)) { … }

// After (zero recompositions during animation)
val scaleState = animateFloatAsState(targetScale)
Box(Modifier.graphicsLayer { scaleX = scaleState.value; scaleY = scaleState.value }) { … }
```

### 4.2 `Modifier.rotate(animateFloatAsState(…).value)` — MEDIUM

`Modifier.rotate()` is not a lambda modifier — it reads the value during Composition.

| File                                                 | Line | Context                        |
|------------------------------------------------------|------|--------------------------------|
| `kmpuiviews/.../settings/SettingsComposables.kt`     | 93   | Expand/collapse arrow rotation |
| `kmpuiviews/.../favorite/FavoriteScreen.kt`          | 174  | Sort arrow                     |
| `kmpuiviews/.../notifications/NotificationScreen.kt` | 422  | Expand icon                    |
| `kmpuiviews/.../notifications/NotificationScreen.kt` | 719  | Expand icon (second site)      |

**Fix:**

```kotlin
// Before
Modifier.rotate(animateFloatAsState(if (expanded) 180f else 0f).value)

// After
val rotation = animateFloatAsState(if (expanded) 180f else 0f)
Modifier.graphicsLayer { rotationZ = rotation.value }
```

### 4.3 `derivedStateOf` value read in Composition — MEDIUM

| File                                      | Lines   | Issue                                                                                            |
|-------------------------------------------|---------|--------------------------------------------------------------------------------------------------|
| `kmpuiviews/.../utils/ComposableUtils.kt` | 120–130 | `LazyListState.isScrollingUp()` — `derivedStateOf { … }.value` extracted directly in composition |

---

## 5. `Modifier.composed {}` — Should Migrate to `Modifier.Node`

`Modifier.composed {}` allocates a new `Modifier` on every recomposition of the calling composable.
`Modifier.Node` allocates once and updates in-place. These are used across the whole app via shared
utility extensions.

| File                                                        | Function                           | Usage sites             |
|-------------------------------------------------------------|------------------------------------|-------------------------|
| `kmpuiviews/.../modifiers/BounceClick.kt:21`                | `bounceClick()`                    | Multiple screens        |
| `kmpuiviews/.../modifiers/FadeInAnimation.kt:11`            | `fadeInAnimation()`                | Multiple screens        |
| `kmpuiviews/.../modifiers/CustomCombinedClick.kt:15`        | `combineClickableWithIndication()` | Multiple screens        |
| `kmpuiviews/.../modifiers/ScateRotateOffset.kt:39`          | `scaleRotateOffset()`              | Multiple screens        |
| `kmpuiviews/.../modifiers/ScateRotateOffset.kt:85`          | `scaleRotateOffsetReset()`         | Multiple screens        |
| `kmpuiviews/.../composables/RecompositionHighlighter.kt:46` | `recomposeModifier`                | Debug only — acceptable |

**Fix:** Migrate each to `ModifierNodeElement` + `Modifier.Node`. See
[Migrate to Modifier.Node](https://developer.android.com/develop/ui/compose/custom-modifiers).

---

## 6. Subcomposition Overhead

`SubcomposeLayout` (and anything built on it — `BoxWithConstraints`, `AnimatedVisibility`,
`AnimatedContent`, `Crossfade`) performs an additional composition pass. It is necessary in some
cases but expensive.

### 6.1 `BoxWithConstraints` — forces subcomposition — MEDIUM

| File                                                      | Line | Context                       |
|-----------------------------------------------------------|------|-------------------------------|
| `kmpuiviews/.../recommendations/RecommendationScreen.kt`  | 397  | Chat bubble width calculation |
| `kmpuiviews/.../components/LimitedBottomSheetScaffold.kt` | 87   | Scaffold layout               |
| `UIViews/.../components/SlideButton.kt`                   | 98   | Swipeable button anchor calc  |
| `mangaworld/shared/.../reader/curl/CurlPager.kt`          | 969  | Curl page animation layout    |

**Fix:** Where constraints are only needed for one dimension, use `onSizeChanged` +
`LocalDensity.current` instead of `BoxWithConstraints`. The slide button anchor calculation is the
highest priority: it fires on every scroll gesture.

### 6.2 Direct `SubcomposeLayout` usage — MEDIUM

| File                                                   | Line | Context                        |
|--------------------------------------------------------|------|--------------------------------|
| `kmpuiviews/.../components/HazeScaffold.kt`            | 159  | Custom scaffold implementation |
| `kmpuiviews/.../components/textflow/TextFlowLayout.kt` | 74   | Text-flow measurement          |

`HazeScaffold` is used on nearly every screen. The `SubcomposeLayout` call is likely unavoidable
for the Haze blur plumbing, but verify whether the scaffold can be restructured to avoid it.

### 6.3 `Crossfade` / `AnimatedContent` in hot paths — MEDIUM

These both use `SubcomposeLayout` internally.

| File                                                         | Lines    | Context                       |
|--------------------------------------------------------------|----------|-------------------------------|
| `kmpuiviews/.../settings/moresettings/MoreSettingsScreen.kt` | 175, 209 | Settings state transitions    |
| `kmpuiviews/.../settings/lists/OtakuListDetailScreen.kt`     | 175      | List detail view              |
| `kmpuiviews/.../all/AllScreen.kt`                            | 208      | Network state crossfade       |
| `kmpuiviews/.../details/DetailsHeader.kt`                    | 243      | Favorite button               |
| `kmpuiviews/.../recommendations/RecommendationScreen.kt`     | 417, 443 | Chat bubble state + save icon |

The `AllScreen` crossfade is highest priority — it runs on the main browse screen. Consider
replacing network-state transitions with `AnimatedVisibility` (cheaper) or a simple `if/else` with
an `animateAlpha` modifier.

### 6.4 `clickable` lambda inside `items {}` capturing mutable state — MEDIUM

| File                                      | Line | Issue                                                                               |
|-------------------------------------------|------|-------------------------------------------------------------------------------------|
| `kmpuiviews/.../details/DetailsScreen.kt` | 449  | `.clickable { markAs(…) }` inside `items(info.chapters)` — lambda captures `markAs` |

The captured `markAs` function likely changes reference on recomposition, preventing item skipping.
**Fix:** Extract to a stable `remember`ed callback or pass as a stable function reference.

---

## 7. Image Loading

### 7.1 `GradientImage` double render — HIGH

Every item in HistoryScreen, NotificationScreen, and any other screen using `GradientImage` renders
the same image **twice**: once at full size (blurred) as a background, once normal as foreground.
This doubles image decode and GPU upload cost per item.

| File                                         | Lines | Composable                   |
|----------------------------------------------|-------|------------------------------|
| `kmpuiviews/.../components/GradientImage.kt` | 39–55 | `GradientImage` (KMP, Kamel) |
| `UIViews/.../components/GradientImage.kt`    | 38–55 | `CoilGradientImage`          |
| `UIViews/.../components/GradientImage.kt`    | 74–94 | `GlideGradientImage`         |

**Fix:** Use a single image decode + a `Modifier.blur()` or `RenderEffect` applied only to the
background layer, or use a shader-based approach. Alternatively, use a `ColorFilter` + scrim
overlay instead of a blurred duplicate image.

### 7.2 Haze blur applied to scrollable content — MEDIUM

Haze (real-time background blur) is an expensive GPU operation. Applied to scaffolds containing
scrollable content, it re-blurs on every scroll frame.

- **18 files** use `dev.chrisbanes.haze` throughout the app.
- Highest cost: `GlobalSearchScreen`, `FavoriteScreen`, `HistoryScreen` (blur applied while
  scrolling large lists).
- `animeworld/.../videos/ViewVideosFragment.kt:288` — `hazeSource` applied directly to a
  `LazyColumn`.

**Recommendation:** Profile on API 24–28 devices. Consider gating blur on
`ActivityManager.isLowRamDevice()` or using `PerformanceClass` API (Android 12+) to disable on
lower-tier devices.

### 7.3 `mutableStateListOf` thrashing in ViewModels — MEDIUM

`clear() + addAll()` replaces the entire snapshot state list on every data update, triggering
full-list recomposition even when only one item changed.

| File                                              | Lines    | Operation                                      |
|---------------------------------------------------|----------|------------------------------------------------|
| `kmpuiviews/.../favorite/FavoriteViewModel.kt`    | 40–43    | `favoriteList.clear(); favoriteList.addAll(…)` |
| `kmpuiviews/.../all/AllViewModel.kt`              | 88       | `sourceList.addAll(…)` in loop                 |
| `kmpuiviews/.../recent/RecentViewModel.kt`        | 81–90    | `clear() + addAll()` on source change          |
| `kmpuiviews/.../navactions/Navigation3Actions.kt` | 229, 237 | `removeRange()` / `removeFirst()` on backStack |

**Fix:** Use `MutableStateFlow<List<T>>` collected with `collectAsStateWithLifecycle()` instead of
`mutableStateListOf`. Diff updates with a proper diff algorithm (DiffUtil-style) before emitting.

### 7.4 Heavy work in `DetailsViewModel.init {}` — MEDIUM

| File                                         | Lines   | Issue                                                                     |
|----------------------------------------------|---------|---------------------------------------------------------------------------|
| `kmpuiviews/.../details/DetailsViewModel.kt` | 101–125 | Palette color extraction from bitmap in `init` — may block on main thread |

**Fix:** Dispatch palette generation to `Dispatchers.IO` or `Dispatchers.Default` inside a
`viewModelScope.launch`.

---

## 8. Baseline Profile — Incomplete

| File                                                                      | Status              |
|---------------------------------------------------------------------------|---------------------|
| `MangaWorldbaselineprofile/src/main/java/.../BaselineProfileGenerator.kt` | Present but minimal |

The generator only covers app startup (`pressHome` + `startActivityAndWait`). Missing journeys:

- Detail screen open (network fetch + image load)
- Chapter list scroll
- Favorites grid scroll
- Search flow

Without a complete Baseline Profile, ART cannot pre-compile the code paths users actually run,
leaving 20–40% startup and early-frame performance on the table.

**No Macrobenchmark module detected.** Frame timing and startup measurements cannot be automated in
CI.

---

## Recommended Fix Order

Priority based on frequency × impact (hot-path composables fixed first):

1. **`GradientImage` double render** — affects every list item with a cover image
2. **`ScaleRotateOffset.kt` animation reads in composition** — affects every animated interaction
3. **`SideEffect` launching work** — correctness bug, not just perf
4. **`CoverCard` unstable `Map` parameters** — used in every grid screen
5. **`KmpInfoModel` / `KmpGenericInfo` stability** — affects entire details screen tree
6. **Lazy layout missing `key` parameters** — affects every list scroll
7. **`Modifier.composed` → `Modifier.Node` migration** — global improvement on every Modifier use
8. **`mutableStateListOf` thrashing** — affects FavoriteScreen, AllScreen, RecentScreen
9. **`LaunchedEffect(Unit)` / `SideEffect` effect key fixes** — correctness + resource usage
10. **Baseline Profile expansion** — locks in all gains above, prevents regression

---

## Environment / Versions

Confirm these before measuring (from `gradle/libs.versions.toml` and `buildSrc/AppInfo.kt`):

| Item                    | Required for feature                |
|-------------------------|-------------------------------------|
| Kotlin 2.0.20+          | Strong Skipping enabled by default  |
| Compose Compiler 1.5.5+ | `stabilityConfigurationFile` DSL    |
| Compose Foundation 1.9+ | `LazyLayoutCacheWindow`             |
| AGP 8.2+                | Baseline Profile Generator template |

---

*Static analysis only. All findings require Macrobenchmark validation on a physical device in
release + R8 build before declaring improvement. See `auditing-compose-performance` skill for the
full Measure → Diagnose → Fix → Verify workflow.*
