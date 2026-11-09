# Motion Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the approved motion and effects upgrades: screen transitions, list item animation, loading-state blends, cover press feel, motion-scheme timing, a Notes detail screen with a shared-element card, haptics, onboarding/settings stagger, and player polish.

**Architecture:** Mostly local changes at existing call sites, using stock Compose animation APIs and `MaterialTheme.motionScheme` (already switched expressive/standard by the user's expressiveness setting in `OtakuTheme.kt:17-22`). One new screen, `Screen.NotesScreen.Detail`, replaces the Notes-list bottom sheet so the note card can morph into it through the existing `customSharedElement` helper.

**Tech Stack:** Compose Multiplatform (Material3 expressive), Navigation3, Koin, Room KMP, kotlin.test + `runComposeUiTest` (jvmTest).

**Spec:** HTML previews approved 2026-11-09: https://claude.ai/artifact/EM5tUpGuz6iZDxEVgNbX9V, plus the decisions under Global Constraints (they override the previews where they differ).

## Global Constraints

- Item 8 (reduce motion) is dropped. Android Compose already follows the system animator scale.
- Item 6 covers Notes only. It adds a new Note detail screen, and that screen replaces `NoteBottomSheet` **on the Notes list only**. `DetailsPortrait.kt:157` and `DetailsLandscape.kt:306` keep the sheet.
- Item 4: covers already have `Modifier.bounceClick(.9f)` (`CoverCard.kt:83,225`). Retune the feel and add no second press effect.
- Item 9: `OnboardingIndicator` already stretches into a pill (`OnboardingScope.kt:108-140`). Only its spec changes, plus a content stagger.
- Item 10: the player bars already slide and fade (`PlayerChrome.kt:184-349`). Scope is the spinners and the cast play/pause icon.
- Timing: use `MaterialTheme.motionScheme.{default,fast,slow}{Spatial,Effects}Spec()`. Spatial specs are for position and size; effects specs are for alpha and color. A raw `tween`/`spring` stays only for progress loops, gesture flings, shimmer, `DiagonalWipeIcon`, `FlipPager`, reader image fade-in, and delays used for staggering.
- Every lazy `item`/`items` touched gets `Modifier.animateItem()` and a stable `key`. This is project rule `feedback_lazy_items_animate_item`.
- Glance widgets (`widget/notification/*`) are out of scope. Glance has no `animateItem`.
- Copy: the Note detail top-bar title is the item title. Actions are "Delete note" (icon) and back. The delete dialog reuses the existing strings "Delete note?" / "This note will be permanently deleted." / "Delete" / "Cancel".
- Local builds use the `noFirebase` flavor.

## Review Focus

1. **Leaving Note detail by system back or predictive back must save, just as the toolbar back does.** Content is saved on dispose of the screen, not only from the button. Owned by Task 6, which tests it with `saveOnLeave`.
2. **A note deleted elsewhere while its detail screen is open must not crash the screen or re-create the note.** When `note` becomes null, the screen pops. Owned by Task 6 (`vmMissingNoteEmitsNull`).
3. **Lists without stable keys animate the wrong rows after reorder or removal.** Every `animateItem()` added must sit in an `items(..., key = …)`. `DownloadScreen.kt:141` currently has no key. Owned by Task 2 (review checklist step).
4. **The haptic threshold must fire once per crossing, not on every frame past the threshold.** Owned by Task 7 (`thresholdCrossed` tests).
5. **A predictive-back gesture that is cancelled must leave the page at full scale.** The spec is driven by `NavDisplay`, so only a device check covers it. Owned by Task 1, step 4.

---

### Task 1: Screen transitions (shared-axis X + predictive-back scale)

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3View.kt:104-118`

**Interfaces:** none new. Leave the `Screen.DetailsScreen.Details` metadata in `Nav3Graph.kt:109-111` as a fade, because the shared cover carries that transition.

- [ ] **Step 1: Read `val motion = MaterialTheme.motionScheme` above `NavDisplay(` and replace the three specs.**
  - `transitionSpec`: `(slideInHorizontally(motion.defaultSpatialSpec()) { it / 3 } + fadeIn(motion.defaultEffectsSpec())) togetherWith (slideOutHorizontally(motion.defaultSpatialSpec()) { -it / 3 } + fadeOut(motion.fastEffectsSpec()))`
  - `popTransitionSpec`: the mirror image (`-it / 3` in, `it / 3` out).
  - `predictivePopTransitionSpec`: `(slideInHorizontally(motion.defaultSpatialSpec()) { -it / 4 } + fadeIn(motion.defaultEffectsSpec())) togetherWith (scaleOut(motion.defaultSpatialSpec(), targetScale = 0.9f) + fadeOut(motion.defaultEffectsSpec()))`
  - The specs are typed: use `defaultSpatialSpec<IntOffset>()` for slides and `defaultSpatialSpec<Float>()` for scale.
- [ ] **Step 2: Build.** Run `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Run the existing tests.** Run `./gradlew :kmpuiviews:jvmTest`. Expected: PASS, with no new failures compared with `develop`.
- [ ] **Step 4: Device check.** Run `cdm acquire --category phone --android`, then install the MangaWorld debug build. Check three things: Recent → Settings forward and back shows a short slide with a fade; a predictive-back swipe that you hold shrinks the page toward 0.9; a swipe started and then cancelled returns to full size. Run `cdm release --android`.
- [ ] **Step 5: Commit.** `git commit -am "feat(nav): shared-axis screen transitions and predictive-back scale"`

### Task 2: `animateItem()` on remaining lazy lists

**Files (modify, each lazy `item`/`items`):**
- kmpuiviews/commonMain: `settings/SettingScreen.kt`, `settings/lists/OtakuListDetailParts.kt`, `settings/lists/addtolist/ListChoiceScreen.kt`, `settings/utils/SourceChooserScreen.kt`, `settings/utils/ColorHelperScreen.kt`, `settings/utils/SourceLanguageGrid.kt`, `settings/extensions/ExtensionListScreen.kt`, `settings/extensions/BrowsePane.kt`, `settings/extensions/ExtensionParts.kt`, `settings/incognito/IncognitoScreen.kt`, `settings/backuprestore/RestoreWizardScreen.kt`, `settings/backuprestore/BackupWizardScreen.kt`, `settings/sourceorder/SourceOrderScreen.kt`, `about/AboutLIbrariesScreen.kt`, `components/ListBottomSheet.kt`, `components/BottomSheetDeleteScaffold.kt`, `components/settings/ListSetting.kt`, `components/collapsablecolumn/CollapsableColumn.kt`, `notes/NotesScreen.kt`, `dictionary/DictionaryListScreen.kt`, `globalsearch/GlobalSearchScreen.kt`
- kmpuiviews/koogintegration: `screens/settings/KoogSettingsScreen.kt`, `screens/settings/KoogModelsScreen.kt`, `screens/chatscreen/ChatScreen.kt`
- kmpuiviews/androidMain: `Platform.android.kt`, `screensaver/HistoryFeedScreen.kt`, `screensaver/CustomListRotationScreen.kt`, `presentation/settings/CustomSettings.kt`
- `UIViews/.../presentation/DebugFragment.kt`
- mangaworld: `shared/.../reader/Drawer.kt`, `reader/ReaderCompose.kt`, `reader/Sheets.kt`, `settings/ReaderSettings.kt`, `downloads/DownloadScreen.kt`, `desktop/.../PlatformSettings.kt`
- animeworld: `desktop/.../PlatformSettings.kt`
- novelworld: `shared/.../reader/Drawer.kt`, `desktop/.../PlatformSettings.kt`
- app: `info/InfoScreen.kt`

**Interfaces:** none.

- [ ] **Step 1: Add the modifier.** In each file, pass `Modifier.animateItem()` to the root composable of every `item {}`/`items {}`. Where a parameter is called `modifier`, chain it as `modifier.animateItem()`. Skip pager pages in reader page lists (`ReaderCompose.kt` vertical/horizontal page items): moving images there would fight the scroll. Animate only list rows, not page content.
- [ ] **Step 2: Add stable keys.** Any `items(...)` without `key =` gets one from the model's identity. Specifically, `DownloadScreen.kt:141` gets `key = { it.chapterUrl }`, and the section-header `item {}` calls get `key = "active-header"` / `"downloaded-header"`. Never key on index.
- [ ] **Step 3: Build all apps.** Run `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug :app:assembleNoFirebaseDebug :mangaworld:desktop:compileKotlinJvm`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Grep check.** Run `grep -rlE --include='*.kt' "LazyColumn|LazyRow|LazyVerticalGrid|LazyVerticalStaggeredGrid|LazyHorizontalGrid" kmpuiviews UIViews mangaworld animeworld novelworld app | xargs grep -L animateItem | grep -viE "test|mock|widget"`. Expected: no output.
- [ ] **Step 5: Device check.** Drag to reorder on Source order: the neighbouring rows slide. Queue and cancel a download: rows fade in and out.
- [ ] **Step 6: Commit.** `git commit -am "feat(lists): animateItem on remaining lazy lists"`

### Task 3: Loading / content / empty blends

**Files:**
- Modify: `kmpuiviews/.../globalsearch/GlobalSearchScreen.kt:406-425` (spinner item and empty item)
- Modify: `kmpuiviews/.../settings/extensions/BrowsePane.kt` (catalog loading → rows)
- Modify: `kmpuiviews/.../settings/updatesources/UpdateSourcesScreen.kt` (empty ↔ list)
- Modify: `mangaworld/shared/.../downloads/DownloadScreen.kt:122-170` (`EmptyState` ↔ `LazyColumn`)
- Modify: `animeworld/shared/src/androidMain/.../cast/CastDevicePicker.kt` (searching ↔ devices)

**Interfaces:** none.

- [ ] **Step 1: Lazy-list states.** When the state switch lives inside a lazy list (GlobalSearch), keep it inside the list. The spinner and empty `item`s get keys (`"searching"`, `"empty"`) and `Modifier.animateItem()`, which crossfades them in place.
- [ ] **Step 2: Whole-screen states.** Wrap each whole-screen state switch (`if (empty) EmptyState else List`, or loading/content) in `AnimatedContent(targetState = <state enum or Boolean>, contentKey = { it }, transitionSpec = { (fadeIn(motionScheme.defaultEffectsSpec()) + scaleIn(motionScheme.defaultSpatialSpec(), initialScale = 0.96f)) togetherWith fadeOut(motionScheme.fastEffectsSpec()) using SizeTransform(clip = false) })`. Key on the state kind, never on the list contents, so list updates don't re-trigger the transition.
- [ ] **Step 3: Build.** Run `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Device check.** In Global search, search "one": the spinner fades and the results fade in. Search "zzqx": the empty state fades in, with no jump.
- [ ] **Step 5: Commit.** `git commit -am "feat(ui): crossfade loading, content and empty states"`

### Task 4: Cover press feel (`bounceClick`)

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/composables/modifiers/BounceClick.kt:52-75`
- Modify: `kmpuiviews/.../components/CoverCard.kt:83,225` (`.bounceClick(.9f)` → `.bounceClick(.94f)`)

**Interfaces:** `fun Modifier.bounceClick(scaleAmount: Float = 0.7f)` stays unchanged.

- [ ] **Step 1: Retune the animations.** Press: `scale.animateTo(scaleAmount, tween(110, easing = FastOutLinearInEasing))`. Release: `scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))`. A node can't read `MaterialTheme`, so these two literals are the allowed exception.
- [ ] **Step 2: Build and device check.** Run `./gradlew :mangaworld:assembleNoFirebaseDebug`. On Recent, press and hold a cover: it dips to 0.94. On release it springs back with a small overshoot. A tap still opens Details and the cover shared element still flies.
- [ ] **Step 3: Commit.** `git commit -am "feat(covers): snappier press and springy release"`

### Task 5: Motion-scheme timing

**Files (modify):** `quickactions/QuickActionsBolt.kt:45-46,81-82`, `quickactions/QuickActionsOverlay.kt`, `settings/translationmodels/TranslationSheetParts.kt`, `globalsearch/SearchLanguagesSheet.kt`, `settings/languageorder/LanguageOrderSheet.kt`, `tips/TipsScreen.kt`, `components/LimitedBottomSheetScaffold.kt`, `onboarding/OnboardingScope.kt:119-127` (the indicator's `animateColorAsState`/`animateDpAsState` get explicit specs), `animeworld/shared/.../videoplayer/PlayerChrome.kt:316-317,348-349` (all under kmpuiviews `presentation/` unless a module is named).

**Interfaces:** none.

- [ ] **Step 1: Replace generic timings.** Swap each generic `tween(...)`/`spring(...)` and each default spec for the motion-scheme spec from Global Constraints. Size and position use spatial; alpha and color use effects. Choose by intent: open/enter uses `default`, close/exit uses `fast`. `QuickActionsBolt`'s open/close emphasized tweens become `defaultSpatialSpec()` / `fastSpatialSpec()`. Keep the exceptions listed in Global Constraints.
- [ ] **Step 2: Build and test.** Run `./gradlew :kmpuiviews:compileKotlinJvm :kmpuiviews:jvmTest :animeworld:shared:compileKotlinJvm`. Expected: BUILD SUCCESSFUL, tests PASS.
- [ ] **Step 3: Device check.** Toggle Settings › expressiveness on and off. The quick-actions bolt morph and the translation sheet should visibly change bounce with the setting.
- [ ] **Step 4: Commit.** `git commit -am "refactor(motion): read timings from MaterialTheme.motionScheme"`

### Task 6: Note detail screen with card → screen shared element

**Files:**
- Modify: `kmpuiviews/.../presentation/Screen.kt:238`. `data object NotesScreen` gains a nested `@Serializable data class Detail(val itemUrl: String) : Screen("notes_detail")`. Match the `DictionaryScreen.Detail` style.
- Modify: `kmpuiviews/.../navactions/NavigationActions.kt:61`. Add `fun noteDetail(itemUrl: String)`, and implement it in `Navigation3Actions.kt` (`navBackStack.add(Screen.NotesScreen.Detail(itemUrl))`) and in every test fake that the compiler flags.
- Modify: `kmpuiviews/.../utils/composables/sharedelements/OtakuSharedElement.kt:25`. Add `Container` to `OtakuSharedElementType`, plus `fun OtakuContainerElement(item: SharedItemKey) = OtakuSharedElement(item, OtakuSharedElementType.Container)`.
- Modify: `SharedElementUtils.kt:58-75`. The `Container` branch uses `sharedBounds(rememberSharedContentState(key), animatedScope, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds)`.
- Create: `kmpuiviews/.../presentation/notes/NoteDetailViewModel.kt`
- Create: `kmpuiviews/.../presentation/notes/NoteDetailScreen.kt`
- Modify: `kmpuiviews/.../presentation/notes/NotesScreen.kt`. Remove `selectedNote`/`showNoteSheet` and the `NoteBottomSheet` call. The card's `onClick` becomes `onNoteClick(note.itemUrl)`. Add the parameter `onNoteClick: (String) -> Unit`, and give the `ElevatedCard` `Modifier.customSharedElement(OtakuContainerElement(SharedItemKey(note.itemUrl, "note")))`.
- Modify: `kmpuiviews/.../navigation/Nav3Graph.kt:153-156`. Pass `onNoteClick = navActions::noteDetail` and add a `navigation<Screen.NotesScreen.Detail>` entry (a plain entry, not `detailEntry`, so it is full-screen).
- Modify: the Koin ViewModel module that registers `AllNotesViewModel`. Add `viewModelOf(::NoteDetailViewModel)` in the same style.
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/notes/NoteDetailViewModelTest.kt`

**Interfaces:**
- Produces: `class NoteDetailViewModel(itemUrl: String, notesDao: NotesDao) : ViewModel` with `val note: StateFlow<NoteItem?>`, `fun save(content: String)` (blank → `notesDao.deleteNote(itemUrl)`, else `upsertNote(note.copy(content, timestamp = now))`, and a no-op if `note` is null), and `fun delete()`.
- Produces: `@Composable fun NoteDetailScreen(onBackPress: () -> Unit, vm: NoteDetailViewModel)`
- Produces: `NavigationActions.noteDetail(itemUrl: String)`, `OtakuSharedElementType.Container`, `OtakuContainerElement(...)`

- [ ] **Step 1: Write the failing tests.** Use a `FakeNotesDao : NotesDao` backed by a `MutableStateFlow<Map<String, NoteItem>>` (unused methods `TODO()`), with `Dispatchers.setMain(StandardTestDispatcher())`:
  - `saveWritesContent`: seed note `"u1"` with content `"a"`. After `save("b")` and `advanceUntilIdle()`, the dao holds `content == "b"`.
  - `saveBlankDeletes`: after `save("  ")`, the dao has no `"u1"`.
  - `vmMissingNoteEmitsNull`: delete `"u1"` from the dao while collecting `note`. The last value is `null`.
  - `saveOnLeave`: the screen's leave path. Assert that `save` was called when the composable leaves: run `runComposeUiTest` with a `mutableStateOf(true)` gate around `NoteDetailScreen`, type "c" into the field, flip the gate to `false`, then `waitForIdle()`. The dao holds `"…c"`.
- [ ] **Step 2: Run the tests and confirm they fail.** Run `./gradlew :kmpuiviews:jvmTest --tests "*NoteDetailViewModelTest"`. Expected: FAIL (unresolved `NoteDetailViewModel`).
- [ ] **Step 3: Implement the ViewModel and screen.**
  - ViewModel: `note` is `notesDao.getNote(itemUrl).stateIn(viewModelScope, WhileSubscribed(5_000), null)`. Writes use `Dispatchers.IO`, as `AllNotesViewModel` does.
  - Screen: a `Scaffold` whose `TopAppBar` has the title `note.itemTitle`, a back arrow, and a Delete action with the existing confirm dialog. The body is an `OutlinedTextField` filling the remaining space. The root `Surface` carries the same `customSharedElement` key as the card.
  - Hold the text in `rememberSaveable(note?.itemUrl)`.
  - `DisposableEffect(Unit) { onDispose { vm.save(text) } }` saves on every exit (Review Focus 1). Delete calls `vm.delete()`, sets a `deleted` flag so the dispose doesn't save, then calls `onBackPress()`.
  - `LaunchedEffect(note) { if (loaded && note == null) onBackPress() }` (Review Focus 2).
- [ ] **Step 4: Run the tests and confirm they pass.** Same command as Step 2. Expected: PASS.
- [ ] **Step 5: Build and device check.** Run `./gradlew :mangaworld:assembleNoFirebaseDebug`. Create a test note on a title you added yourself (rule `feedback_device_tests_own_data_only`). Open Notes and tap the card: it grows into the detail screen. Edit the text, then use system back: the card morphs back and shows the edit. Delete from detail: you return to the list and the card disappears. Details › Notes still opens the bottom sheet.
- [ ] **Step 6: Commit.** `git add -A && git commit -m "feat(notes): note detail screen with shared-element card"`

### Task 7: Haptics on key gestures

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/HapticThreshold.kt`
- Modify: `kmpuiviews/.../components/PullToRefreshScaling.kt:88` (fire on crossing `distanceFraction` 1f)
- Modify: `UIViews/.../presentation/components/SlideButton.kt:120` (fire on crossing the end anchor's 90% threshold)
- Modify: `kmpuiviews/.../details/DetailsHeader.kt:~255-266` (favorite toggle: `ToggleOn`/`ToggleOff`)
- Modify: `kmpuiviews/.../favorite/FavoritesLibrary.kt:422,567` and `favorite/FavoritesOverview.kt:115` (group long-press: `LongPress`)
- Test: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/utils/HapticThresholdTest.kt`

**Interfaces:**
- Produces: `fun thresholdCrossed(previous: Float, current: Float, threshold: Float): Boolean`, which is true only when `previous < threshold && current >= threshold`.
- Produces: `@Composable fun ThresholdHaptic(fraction: () -> Float, threshold: Float = 1f)`. It uses `snapshotFlow(fraction)` with a running previous value and calls `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)` when `thresholdCrossed`.

- [ ] **Step 1: Write the failing tests.** `HapticThresholdTest`:
  - `firesOnUpwardCrossing`: `thresholdCrossed(0.8f, 1.0f, 1f) == true`
  - `notWhileStayingAbove`: `thresholdCrossed(1.0f, 1.2f, 1f) == false`
  - `notOnDownwardCrossing`: `thresholdCrossed(1.1f, 0.9f, 1f) == false`
  - `refiresAfterDroppingBelow`: the sequence 0.5 → 1.0 → 0.7 → 1.0 gives exactly 2 trues
- [ ] **Step 2: Run the tests and confirm they fail.** Run `./gradlew :kmpuiviews:jvmTest --tests "*HapticThresholdTest"`. Expected: FAIL (unresolved reference).
- [ ] **Step 3: Implement both functions.** Then wire the call sites listed under Files. Long-press and favorite sites call `performHapticFeedback` directly. They do this before the existing action, and only when the action actually changes state.
- [ ] **Step 4: Run the tests and build.** Run `./gradlew :kmpuiviews:jvmTest --tests "*HapticThresholdTest" :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug`. Expected: PASS and BUILD SUCCESSFUL.
- [ ] **Step 5: Device check.** Pull to refresh on Recent: one tick when the threshold is reached, not repeated while you hold past it. Toggle favorite: a tick each time. Long-press a Favorites group: a tick. AnimeWorld slide-to-delete: a tick at the end.
- [ ] **Step 6: Commit.** `git commit -am "feat(haptics): threshold, toggle and long-press feedback"`

### Task 8: Onboarding content stagger and settings entry stagger

**Files:**
- Modify: `kmpuiviews/.../onboarding/OnboardingScope.kt:88-104` (the `HorizontalPager` page content)
- Modify: `kmpuiviews/.../settings/SettingScreen.kt` (top-level setting groups)
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/composables/modifiers/StaggerIn.kt`

**Interfaces:**
- Produces: `fun Modifier.pagerStagger(pagerState: PagerState, page: Int, index: Int): Modifier`. This is a `graphicsLayer` whose `translationX = pagerState.getOffsetDistanceInPages(page) * size.width * 0.12f * (index + 1)` and whose `alpha = 1f - abs(offset).coerceAtMost(1f) * 0.6f`. It is gesture-driven, so there is no time spec.
- Produces: `@Composable fun Modifier.staggerIn(index: Int, played: MutableState<Boolean>): Modifier`. It plays once per screen visit: from alpha 0 and translationY 16.dp to rest with `motionScheme.defaultSpatialSpec()`, a `40 * index` ms delay, and indices capped at 8. Each item passes `played` from a `rememberSaveable { mutableStateOf(false) }` in the screen, so returning from a sub-screen does not replay it.

- [ ] **Step 1: Implement `StaggerIn.kt`.** Apply `pagerStagger` to each direct child of the onboarding page column (`index` = the child's position) and `staggerIn` to each settings group.
- [ ] **Step 2: Build.** Run `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug`. Expected: BUILD SUCCESSFUL. `AnimeOnboarding.kt` contributes pages through the same scope, so it gets the stagger without its own change. Confirm this on the device.
- [ ] **Step 3: Device check.** Clear the onboarding flag only on a fresh test install. Swiping pages makes the content layers drift at different rates. Opening Settings plays the cascade once. Opening and leaving a sub-screen does not replay it.
- [ ] **Step 4: Commit.** `git commit -am "feat(onboarding,settings): staggered entry"`

### Task 9: Player spinners and cast play/pause

**Files:**
- Modify: `animeworld/shared/src/jvmMain/.../videoplayer/DesktopPlayerScreen.kt:358-360` (buffering spinner)
- Modify: `animeworld/shared/src/jvmMain/.../videoplayer/VideoPlayerUi.jvm.kt:38-50` (libVLC init spinner → player)
- Modify: `animeworld/shared/src/androidMain/.../cast/CastRemoteScreen.kt:297-310` (`TransportRow` play/pause icon)

**Interfaces:** none.

- [ ] **Step 1: Buffering spinner.** Use `AnimatedVisibility(state.buffering, Modifier.align(Alignment.Center), enter = fadeIn(motionScheme.defaultEffectsSpec()) + scaleIn(motionScheme.defaultSpatialSpec(), initialScale = 0.6f), exit = fadeOut(motionScheme.fastEffectsSpec()) + scaleOut(targetScale = 0.6f))`.
- [ ] **Step 2: Init spinner.** Wrap the init `when` in `Crossfade(targetState = factory == null)` with `motionScheme.defaultEffectsSpec()`. Keep the black background behind both states so nothing flashes.
- [ ] **Step 3: Play/pause icon.** Use `AnimatedContent(playing, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.7f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.7f)) })`, with motion-scheme specs, around the `Icon`.
- [ ] **Step 4: Build.** Run `./gradlew :animeworld:shared:compileKotlinJvm :animeworld:assembleNoFirebaseDebug :animeworld:desktop:compileKotlinJvm`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Manual check.** Run `./gradlew :animeworld:desktop:run`. Play a video: the startup spinner fades into the video, and seeking past the buffer fades the spinner in and out. Cast remote: check on a device only if a cast target is available. Otherwise mark it unverified in the PR.
- [ ] **Step 6: Commit.** `git commit -am "feat(player): animated spinners and cast play/pause"`

### Task 10: Wrap-up

- [ ] **Step 1: Run the full test suite.** Run `./gradlew :kmpuiviews:jvmTest :kmpuiviews:testDebugUnitTest`. Expected: PASS.
- [ ] **Step 2: Changelog.** Add a changelog entry in the same style as recent entries (see `git log -p -1 -- '*CHANGELOG*'`).
- [ ] **Step 3: Commit.** `git commit -am "docs(changelog): motion pass"`
