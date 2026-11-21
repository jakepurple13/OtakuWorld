# Onboarding Story Steps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current onboarding pager with the approved 9-step "Story Steps" flow (shared frame, morphing hero, per-step motion, two panes at Expanded width) on MangaWorld, AnimeWorld and NovelWorld.

**Architecture:** `kmpuiviews` commonMain owns the frame (progress, hero shape, Back/Next, Skip dialog, two-pane step layout), the shared steps and the motion primitives. Steps are registered through `OnboardingScope.step(spec) { content }`; each app contributes its step-5 through the existing `ComposeSettingsDsl.onboardingSettings` hook plus a new `OnboardingAppConfig` (genres, start label, update noun). The `HorizontalPager` stays so swipe and `pagerStagger` keep working.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.13.0-beta01 (material3 expressive), Koin, DataStore/proto settings, `runComposeUiTest` (jvmTest).

**Spec:** `docs/superpowers/specs/2026-11-19-onboarding-story-steps-design.md` (previews: `docs/superpowers/specs/2026-11-19-onboarding-story-steps-preview.html`). Read both before every task; copy, values and step order come from the spec.

## Global Constraints

- All code shared in commonMain unless a platform API is required (biometric row Android-only, battery alert Android-only).
- Copy is hardcoded `Text("…")` like the existing onboarding; use the spec's exact strings.
- Step order: welcome, look, layout, sources, app (contributed), sync, updates, tips, done. Done is always last.
- Reduce motion = `rememberReducedMotion()` (`kmpuiviews/.../presentation/components/ai/ReducedMotion.kt`): shapes snap, no confetti, no stagger, no looping demos.
- Expanded layout at `LocalWindowSizeClass` width ≥ Expanded (840 dp).
- Every lazy item uses `Modifier.animateItem()`.
- `ModalBottomSheet` uses `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.
- Settings writes go through the existing accessors (`NewSettingsHandling`, `MangaNewSettingsHandling`, `videoPlayerStyle`/`videoPlayerBlur`, `SubtitleSettingsRepository`, `mediaCheckerSettings`); no new storage except `novelTextSize` (Task 13).
- Build with the `noFirebase` flavor. Tests: `./gradlew :kmpuiviews:jvmTest`, `:mangaworld:shared:jvmTest`, `:animeworld:shared:jvmTest`.

## Review Focus

1. Skip on the Done step: Skip is hidden there; tapping system back on Done goes to Tips, not out. (Task 2 test `back_on_done_goes_to_tips`.)
2. App with no step-5 contribution (none today, but `onboardingSettings` may be empty): flow has 8 steps, progress shows 8 segments, no crash. (Task 2 test `flow_without_app_step_has_eight_segments`.)
3. Turning Updates off then back on restores the previous interval/network values (the master switch must only flip `shouldRun`). (Task 8 test `master_toggle_keeps_interval`.)
4. Rotating/resizing between Compact and Expanded mid-flow keeps the current step and entered values. (Task 3 test `width_change_keeps_page`.)
5. Restore-from-backup picker cancelled: row stays in its "Choose" state, no toast. (Task 7 test `restore_cancel_keeps_state`.)

---

## File Structure

`kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/onboarding/` (abbrev. `ONB/`)

| File | Responsibility |
|---|---|
| `ONB/OnboardingStep.kt` (new) | `OnboardingStepSpec`, `HeroShape`, `OnboardingTags` |
| `ONB/OnboardingAppConfig.kt` (new) | `OnboardingAppConfig`, `GenreCard` |
| `ONB/OnboardingScope.kt` (modify) | scope API `step(spec, content)`; pager wrapper |
| `ONB/frame/StoryStepsScaffold.kt` (new) | top bar (progress + Skip), hero, pager, Back/Next, Skip dialog, back + arrow keys |
| `ONB/frame/OnboardingProgress.kt` (new) | segmented progress (replaces `OnboardingIndicator`) |
| `ONB/frame/HeroShape.kt` (new) | morphing hero |
| `ONB/frame/SkipSetupDialog.kt` (new) | dialog |
| `ONB/frame/StepLayout.kt` (new) | compact vs two-pane page layout, sticky preview slot |
| `ONB/motion/Confetti.kt`, `ONB/motion/ColorSweep.kt` (new) | motion primitives |
| `ONB/steps/{Welcome,Look,Layout,Sources,Sync,Updates,Tips,Done}Step.kt` (new) | one step each |
| `ONB/OnboardingScreen.kt` (rewrite) | assembles steps |
| `ONB/composables/*` (delete) | old pages |
| `kmpuiviews/.../utils/ComposeSettingsDsl.kt` (modify) | `onboardingConfig` |
| `mangaworld/shared/.../onboarding/OnboardingItems.kt` (rewrite) → `ReaderSetupStep` | manga step 5 |
| `animeworld/shared/src/commonMain/.../onboarding/PlayerSetupStep.kt` (new; replaces `AnimeOnboarding.kt` usage) | anime step 5 |
| `novelworld/shared/.../onboarding/ReadingComfortStep.kt` (new) + setting | novel step 5 |

Tests: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/onboarding/` (abbrev. `ONBT/`), `mangaworld/shared/src/jvmTest/.../onboarding/`, `animeworld/shared/src/jvmTest/.../onboarding/`. Use `createTestDataStoreHandling()` / `createTestNewSettingsHandling()` from `kmpuiviews/src/jvmTest/.../testing/Fakes.kt` and `runComposeUiTest` (see `RevealRestoredTest.kt`).

---

### Task 1: Step model, app config, scope API

**Files:**
- Create: `ONB/OnboardingStep.kt`, `ONB/OnboardingAppConfig.kt`
- Modify: `ONB/OnboardingScope.kt`, `kmpuiviews/.../utils/ComposeSettingsDsl.kt:15-50`
- Test: `ONBT/OnboardingScopeTest.kt`

**Interfaces:**
- Produces:
  - `data class OnboardingStepSpec(val id: String, val title: String, val body: String, val icon: ImageVector, val shape: HeroShape, val summaryLabel: String? = null, val summary: (@Composable () -> String)? = null)`
  - `enum class HeroShape(val rotation: Float, val scale: Float) { Hidden(0f, 0f), Cookie(-14f, 1f), Diamond(45f, 1f), Arch(0f, 1f), Clover(22f, 1f), Bun(0f, 1f), Sunny(-30f, 1f), Flower(16f, 1f), Done(360f, 1.15f) }`
  - `interface OnboardingScope { fun step(spec: OnboardingStepSpec, content: @Composable () -> Unit); val size: Int; operator fun get(index: Int): OnboardingStepEntry }`, `data class OnboardingStepEntry(val spec: OnboardingStepSpec, val content: @Composable () -> Unit)`. `item()` is removed.
  - `data class GenreCard(val name: String, val icon: ImageVector, val hue: Float)`
  - `data class OnboardingAppConfig(val genres: List<GenreCard>, val startLabel: String, val updateNoun: String, val appStepTab: String)` with `companion val Default` = MangaWorld values from the spec (Action, Romance, Mystery, Comedy, Fantasy; "Start reading"; "chapter"; "Reader").
  - `ComposeSettingsDsl.onboardingConfig: OnboardingAppConfig` (`var`, default `OnboardingAppConfig.Default`).
  - `object OnboardingTags { const val Next = "onb_next"; const val Back = "onb_back"; const val Skip = "onb_skip"; const val SkipConfirm = "onb_skip_confirm"; const val SkipCancel = "onb_skip_cancel"; const val Progress = "onb_progress"; const val Hero = "onb_hero"; const val TwoPane = "onb_two_pane"; const val SinglePane = "onb_single_pane"; const val UpdatesOptions = "onb_updates_options"; fun step(id: String) = "onb_step_$id" }`

- [ ] **Step 1: Write the failing test** `OnboardingScopeTest`:
  - `steps_keep_registration_order`: register 3 specs → `scope[i].spec.id` equals `["a","b","c"]`, `size == 3`.
  - `app_contribution_lands_before_sync`: build with `onboardingSettings = { step(spec("app")) {} }` applied between `sources` and `sync` (same `apply` point as today) → ids in order.
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*OnboardingScopeTest"` → FAIL (unresolved `step`).
- [ ] **Step 3: Implement** the types above; migrate `OnboardingScopeImpl` from `item` to `step` storing `OnboardingStepEntry`. Update the two app call sites (`GenericSharedManga.kt:346`, anime `GenericAnime.kt:515` / `GenericAnimeDesktop.kt:169`) to `step(OnboardingStepSpec(id = "app", …)) { existing content }` so everything compiles until Tasks 11–13 replace the content.
- [ ] **Step 4: Run** the test → PASS; `./gradlew :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug` compiles.
- [ ] **Step 5: Commit** `feat(onboarding): step specs and app config for story steps`

### Task 2: Frame — progress, hero, Back/Next, Skip dialog, back + keys

**Files:**
- Create: `ONB/frame/StoryStepsScaffold.kt`, `ONB/frame/OnboardingProgress.kt`, `ONB/frame/HeroShape.kt`, `ONB/frame/SkipSetupDialog.kt`
- Modify: `ONB/OnboardingScope.kt` (remove `OnboardingIndicator` once unused)
- Test: `ONBT/StoryStepsScaffoldTest.kt`

**Interfaces:**
- Consumes: Task 1 types.
- Produces: `@Composable fun StoryStepsScaffold(scope: OnboardingScope, appName: String, startLabel: String, onFinish: () -> Unit, onSkipConfirmed: () -> Unit, userScrollEnabled: Boolean = true, pagerState: PagerState = rememberPagerState { scope.size }, modifier: Modifier = Modifier)`. `onFinish`/`onSkipConfirmed` are where `OnboardingScreen` sets `hasGoneThroughOnboarding = true` and pops.
- Produces: `@Composable fun HeroShape(shape: HeroShape, icon: ImageVector, size: Dp, modifier: Modifier = Modifier)`.

- [ ] **Step 1: Write failing tests** (`runComposeUiTest`, 3 dummy steps + a `done` step):
  - `next_labels_follow_position`: page 0 → Next text "Let's go"; page 1 → "Next"; last → `startLabel`.
  - `back_disabled_on_first_step`: `onNodeWithTag(Back).assertIsNotEnabled()`.
  - `skip_opens_dialog_and_cancel_keeps_page`: click Skip → "Skip setup?" shown with text `"$appName will start with default settings. You can run setup again from Settings › About › View Onboarding Again."`; click SkipCancel → dialog gone, current page unchanged, `onSkipConfirmed` not called.
  - `skip_confirm_calls_callback`: Skip → SkipConfirm → `onSkipConfirmed` called once.
  - `skip_hidden_on_done`: on last page Skip does not exist.
  - `back_on_done_goes_to_tips`: on last page, `onNodeWithTag(Back).performClick()` → page == last − 1; repeat from last page with `performKeyInput { pressKey(Key.DirectionLeft) }` → same.
  - `arrow_keys_change_step`: `pressKey(Key.DirectionRight)` → page 1.
  - `flow_without_app_step_has_eight_segments`: scope of 8 → progress has 8 children (`onNodeWithTag(Progress).onChildren().assertCountEquals(8)`).
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*StoryStepsScaffoldTest"` → FAIL.
- [ ] **Step 3: Implement.**
  - Progress: `Row` of segments; current weight 2.4f, fill `animateFloatAsState` with `MaterialTheme.motionScheme.fastSpatialSpec()`.
  - Hero: if `MaterialShapes` (+ `toShape()`/`Morph`) resolves in material3 1.13, map `Cookie→MaterialShapes.Cookie9Sided, Diamond→Diamond, Arch→Arch, Clover→Clover4Leaf, Bun→Bun, Sunny→Sunny, Flower→Flower, Done/Hidden→Circle` and morph with `Morph(from, to)` progress from `Animatable` + `defaultSpatialSpec()`; otherwise add `androidx.graphics:graphics-shapes` to `libs.versions.toml` and build the same polygons. Rotation/scale from `HeroShape`; icon counter-rotated, pops in with `scaleIn`. Reduced motion → snap.
  - Next button: `animateContentSize()`, label `AnimatedContent`.
  - Skip dialog: `AlertDialog` with `icon`, title "Skip setup?", buttons "Keep going" / "Skip".
  - Back: `NavigationBackHandler`/`BackHandler` enabled when page > 0 → previous page.
  - Keys: `onPreviewKeyEvent` on the scaffold root for `DirectionLeft/Right`.
  - Pager page content: `scope[page].content()` inside existing `SaveableStateProvider` + `pagerStagger`.
- [ ] **Step 4: Run** tests → PASS.
- [ ] **Step 5: Commit** `feat(onboarding): story steps frame with progress, hero and skip dialog`

### Task 3: Step layout (compact / two-pane) and motion primitives

**Files:**
- Create: `ONB/frame/StepLayout.kt`, `ONB/motion/Confetti.kt`, `ONB/motion/ColorSweep.kt`
- Test: `ONBT/StepLayoutTest.kt`, `ONBT/ConfettiTest.kt`

**Interfaces:**
- Produces: `@Composable fun StepLayout(spec: OnboardingStepSpec, expanded: Boolean = isExpandedWidth(), preview: (@Composable () -> Unit)? = null, controls: @Composable ColumnScope.() -> Unit)`. Compact: hero slot space (92 dp top), title (25 sp display), body, `preview` as sticky header, controls — one `LazyColumn`. Expanded: `Row` with left 0.45f (title 38 sp, body) and right 0.55f `LazyColumn` (preview sticky inside). Tags `SinglePane` / `TwoPane`.
- Produces: `@Composable fun isExpandedWidth(): Boolean` reading `LocalWindowSizeClass`. Hero size 64 dp compact / 104 dp expanded.
- Produces: `val LocalReducedMotionOverride = staticCompositionLocalOf<Boolean?> { null }` and `@Composable fun onboardingReducedMotion(): Boolean = LocalReducedMotionOverride.current ?: rememberReducedMotion()` in `ONB/motion/ReducedMotion.kt`. Every onboarding animation checks `onboardingReducedMotion()` (Tasks 2–13), not `rememberReducedMotion()` directly.
- Produces: `@Composable fun ConfettiBurst(trigger: Any, colors: List<Color>, modifier: Modifier = Modifier)` — ~90 particles, 1.9 s, `Canvas` + `withFrameNanos`; draws nothing when `rememberReducedMotion()`.
- Produces: `@Composable fun ColorSweepHost(content: @Composable (sweepFrom: (Offset) -> Unit) -> Unit)` — snapshots current frame with `rememberGraphicsLayer()`, then draws it over the new theme with a circular hole growing from the offset, 700 ms; no-op when reduced motion.

- [ ] **Step 1: Write failing tests:**
  - `compact_uses_single_pane` / `expanded_uses_two_panes` (pass `expanded` explicitly).
  - `width_change_keeps_page`: in a `StoryStepsScaffold`, go to page 2, type into a test text field, flip `expanded` → page still 2, text kept.
  - `confetti_skipped_when_reduced_motion`: provide `LocalReducedMotionOverride provides true` → `ConfettiBurst` exposes `semantics { stateDescription = "particles=$n" }`; assert `"particles=0"`.
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement.** **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(onboarding): two-pane step layout, confetti and color sweep`

### Task 4: Welcome and Done steps

**Files:**
- Create: `ONB/steps/WelcomeStep.kt`, `ONB/steps/DoneStep.kt`
- Test: `ONBT/WelcomeDoneStepTest.kt`

**Interfaces:**
- Consumes: `StepLayout`, `ConfettiBurst`, `OnboardingAppConfig`, `OnboardingScope`.
- Produces: `fun OnboardingScope.welcomeStep(appName: String, config: OnboardingAppConfig, comingUp: List<Pair<ImageVector, String>>)` and `fun OnboardingScope.doneStep(summaries: List<OnboardingStepSpec>)` (Done shows `summaryLabel` + `summary()` for each spec that has one).

- [ ] **Step 1: Failing tests:** `welcome_shows_app_name_and_genres` (title `"Welcome to $appName"`, all 5 genre names present); `done_lists_summaries` (two fake specs with summaries → both labels and values shown); `done_has_no_summary_for_sources` (spec without summary is not listed).
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement:** genre fan = 5 cards, per-card `Animatable` deal-in (160 dp below → `(k-2)*30dp` x, `(k-2)*9°`, 90 ms apart, 120 ms delay, `spring(DampingRatioMediumBouncy, StiffnessLow)`), card = gradient `hsv(hue)` + icon + name; "Coming up" chips. Done: summary rows with check icons, `ConfettiBurst` on first composition, spec `shape = HeroShape.Done`. **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(onboarding): welcome genre fan and done summary`

### Task 5: Look step

**Files:** Create `ONB/steps/LookStep.kt`; Test `ONBT/LookStepTest.kt`

**Interfaces:**
- Consumes: `NewSettingsHandling.rememberSystemThemeMode()`, `rememberThemeColor()`, `rememberIsAmoledMode()`; `ExpressivenessSetting(handling)` (`ThemeSettingsScreen.kt:204`), `ColorBlindTypeSettings(handling)` (`GeneralSettings.kt:178`), `rememberUsePalette()`; `ColorSweepHost`.
- Produces: `fun OnboardingScope.lookStep(handling: NewSettingsHandling)`; summary e.g. `"Blue · Dark"` / `"Dynamic · AMOLED"`.

- [ ] **Step 1: Failing tests:** `amoled_row_hidden_in_light` (set Light → AMOLED text absent; Dark → present); `swatch_click_writes_theme_color` (click "Blue" swatch → `handling.themeColor` flow emits `ThemeColor.Blue`); `swatches_exclude_custom_ugly_myeyes` (main row has exactly Dynamic, Blue, Red, Green, Yellow, Cyan, Magenta); `more_options_sheet_has_extra_settings` (click "More look options" → sheet shows expressiveness, color-blind, "Color details from cover", Ugly, MyEyes).
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement** with `SingleChoiceSegmentedButtonRow`, swatch corner `animateDpAsState` (circle → 11 dp) + check `AnimatedVisibility(scaleIn)`, AMOLED `AnimatedVisibility(expandVertically)`, swatch tap calls `sweepFrom(tapOffset)` before writing. **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(onboarding): look step with color sweep`

### Task 6: Layout and Sources steps

**Files:** Create `ONB/steps/LayoutStep.kt`, `ONB/steps/SourcesStep.kt`; Test `ONBT/LayoutSourcesStepTest.kt`

**Interfaces:**
- Consumes: `rememberGridChoice()`, `rememberFloatingNavigation()` (`DataStoreUtils.kt:109`), `rememberMiddleNavigationAction()`; for catalogs, the same source `ExtensionListViewModel` uses (`ExtensionListViewModel.kt:78-81`, incl. `hasCustomBridge`).
- Produces: `fun OnboardingScope.layoutStep(handling: NewSettingsHandling)` (summary `"Adaptive grid · floating nav"`), `fun OnboardingScope.sourcesStep(appName: String, catalogs: List<CatalogInfo>)` (no summary), `data class CatalogInfo(val icon: ImageVector, val title: String, val subtitle: String, val builtIn: Boolean)` plus `@Composable fun rememberOnboardingCatalogs(): List<CatalogInfo>` mapping registered catalogs to the spec's copy.

- [ ] **Step 1: Failing tests:** `grid_choice_reflows_preview` (click "Fixed" → preview grid has 2 columns via semantics tag `onb_mini_grid` `cols=2`); `middle_options_exclude_multiple`; `sources_has_no_buttons` (`onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag(OnboardingTags.step("sources")))).assertCountEquals(0)`); `sources_copy_matches_spec` (body text and the three how-it-works titles Find/Install/Browse and closing line); `custom_repos_only_with_bridge` (catalog list without bridge → no "Custom repos").
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement:** mini preview `LazyVerticalGrid` (columns FullAdaptive 4 / Adaptive 3 / Fixed 2) with `animateItem()`, nav dock↔float via `animateDpAsState`, middle icon `AnimatedContent(scaleIn + rotate)`; Sources how-it-works highlight loop `LaunchedEffect { while(true) { delay(1800); i = (i+1)%3 } }` (static when reduced motion), rows `animateColorAsState`. **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(onboarding): layout preview and sources explainer`

### Task 7: Sync & privacy step

**Files:** Create `ONB/steps/SyncStep.kt`; Test `ONBT/SyncStepTest.kt`

**Interfaces:**
- Consumes: `NavigationActions.navigate(SupabaseRoutes)`, `NavigationActions.security()`, `MoreSettingsViewModel.importFullBackup(file)`, `rememberFilePickerLauncher(type = FileKitType.File("zip"))`, `platform()`.
- Produces: `fun OnboardingScope.syncStep(navController: NavigationActions)`; summary joins "Sync on" / "Backup restored" or "Nothing turned on".

- [ ] **Step 1: Failing tests:** `biometric_row_opens_security` (fake `NavigationActions` records `security()`); `supabase_row_navigates`; `restore_cancel_keeps_state` (picker callback with `null` → row still shows "Choose", no "Restored" text); `biometric_hidden_off_android` (jvm → row absent).
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement:** pill button label `AnimatedContent` ("Set up" → "On", "Choose" → "Done"); "On" state for sync reads the real Supabase signed-in state if exposed, else after returning from the route. **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(onboarding): sync and privacy step`

### Task 8: Updates step

**Files:** Create `ONB/steps/UpdatesStep.kt`; Test `ONBT/UpdatesStepTest.kt`

**Interfaces:**
- Consumes: `handling.mediaCheckerSettings.rememberPreference()` (proto fields `shouldRun`, `interval: Long`, `networkType {Connected, Metered, Unmetered}`, `requiresCharging`, `requiresBatteryNotLow`).
- Produces: `fun OnboardingScope.updatesStep(handling: NewSettingsHandling, updateNoun: String)`; title `"Never miss a $updateNoun"`; summary `"Every 3h · any network"` or `"Update checks off"`.

- [ ] **Step 1: Failing tests:** `master_off_hides_options` (toggle "Check for updates" off → `UpdatesOptions` absent, note text present, stored `shouldRun == false`); `master_toggle_keeps_interval` (set interval 6, off, on → stored interval 6, slider shows 6); `network_choice_maps_to_proto` ("Wi‑Fi only" → `Unmetered`, "Any network" → `Connected`); `interval_range_1_to_24`.
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement:** `AnimatedVisibility(expandVertically/shrinkVertically)` for options; dial = `CircularProgressIndicator(progress = { animated / 24f })` with `animateFloatAsState(spring)`; bell rotation keyframes every 2.4 s while on (static when reduced motion). **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(onboarding): updates step with master switch`

### Task 9: Tips step

**Files:** Create `ONB/steps/TipsStep.kt`; Test `ONBT/TipsStepTest.kt`

**Interfaces:**
- Consumes: `ComposeSettingsDsl.tips` (`TipsScope.tip(title, description, gesture, demo)`, `Tip.kt:32`).
- Produces: `fun OnboardingScope.tipsStep(onPracticeChange: (Boolean) -> Unit)`; shows the first tip with `gesture == TipGesture.Hold` and a `demo`, hint "Press and hold the cover." → "Nice. That menu works on every cover." after the demo calls `onDone`. Footnote: "More in Settings › Discover › Tips & Gestures".

- [ ] **Step 1: Failing tests:** `shows_first_hold_tip_only`; `hint_changes_after_done` (fake tip demo calls `onDone`); `practice_reports_change` (pointer down on demo → `onPracticeChange(true)`, up → `false`).
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement.** **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(onboarding): single practice tip step`

### Task 10: Assemble OnboardingScreen, delete old pages

**Files:**
- Rewrite: `ONB/OnboardingScreen.kt`
- Delete: `ONB/composables/{Welcome,Theme,Sources,Account,General,Finish}Content.kt` (keep `NavigationBarSettings`, which lives in `settings/general/GeneralSettings.kt`)
- Modify: `kmpuiviews/.../navigation/Nav3Graph.kt:125-131`, `UIViews/.../navigation/Nav3Graph.kt:78` (drop `accountContent` param if unused)
- Test: `ONBT/OnboardingScreenTest.kt`

**Interfaces:**
- Consumes: everything above; `dataStoreHandling.hasGoneThroughOnboarding`, `appConfig.appName`, `customPreferences.onboardingSettings`, `customPreferences.onboardingConfig`.
- Produces: `fun OnboardingScreen(navController: NavigationActions, customPreferences: ComposeSettingsDsl, dataStoreHandling: DataStoreHandling = koinInject(), appConfig: AppConfig = koinInject(), handling: NewSettingsHandling = koinInject())`.

- [ ] **Step 1: Failing tests:** `step_order_matches_spec` (with a fake app step: tags `onb_step_welcome, look, layout, sources, app, sync, updates, tips, done` in order by paging Next); `finish_sets_flag_and_pops` (Next on Done → `hasGoneThroughOnboarding` true, fake nav `popBackStack()` called); `skip_confirm_sets_flag_and_pops`.
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement:** build scope in spec order, `apply(customPreferences.onboardingSettings)` after `sourcesStep`; Done summaries from look, layout, app, sync, updates specs; keep `HideNavBarWhileOnScreen()`; `userScrollEnabled = !practicingTip`. **Step 4: Run** all `:kmpuiviews:jvmTest` → PASS.
- [ ] **Step 5: Commit** `feat(onboarding): assemble story steps and remove old pages`

### Task 11: MangaWorld reader step

**Files:**
- Rewrite: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/onboarding/OnboardingItems.kt` → `ReaderSetupStep`
- Modify: `mangaworld/shared/.../GenericSharedManga.kt:346` (register step + `onboardingConfig` = manga values)
- Test: `mangaworld/shared/src/jvmTest/.../onboarding/ReaderSetupStepTest.kt`

**Interfaces:**
- Consumes: `MangaNewSettingsHandling` (`rememberReaderType()`, `pagePadding` `SettingInfo.updateSetting`, `rememberPageIndicatorStyle()`, `rememberUseFloatingReaderBottomBar()`, `rememberShowPageNumberInBar()`, `rememberUserGestureEnabled()`, `rememberIncludeInsetsForReader()`, `rememberImageLoaderType()`), `PageIndicatorOverlay` (`reader/PageIndicators.kt:88`), `SampleMangaPage()`, `StepLayout(preview = …)`.
- Produces: `fun OnboardingScope.readerSetupStep(handling: MangaNewSettingsHandling)`; spec id `app`, shape `Clover`, title "Set up the reader", summary e.g. `"Pager reader"`.

- [ ] **Step 1: Failing tests:** `padding_slider_range_0_to_16`; `page_number_chips_list_all_styles` (8 labels: Classic, Rolling pill, Scrubber, Edge rail, Story segments, Printed folio, Morphing pill, Line only); `pages_button_only_with_floating_bar`; `pages_button_inverts_hide_flag` (turn off → `hidePageNumberInBar == true`); `drawer_toggle_peeks_drawer` (turn on → node `onb_reader_drawer` displayed, then gone after `mainClock.advanceTimeBy(1600)`); `image_loader_options` (Kamel, Glide, Telephoto, Panpf).
- [ ] **Step 2: Run** `./gradlew :mangaworld:shared:jvmTest --tests "*ReaderSetupStepTest"` → FAIL.
- [ ] **Step 3: Implement:** sticky preview (≈178 dp): sample pages animated per reader type with gap = padding, real `PageIndicatorOverlay` for the chosen style, bar dock/float, "3 / 24" chip, drawer peek `Animatable` one-shot on enable; loops via `rememberInfiniteTransition` (static when reduced motion).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(manga): reader setup onboarding step`

### Task 12: AnimeWorld player step

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/onboarding/PlayerSetupStep.kt`
- Modify: `animeworld/src/main/java/.../GenericAnime.kt:515`, `animeworld/desktop/.../GenericAnimeDesktop.kt:169` (register step + `onboardingConfig` = anime genres, "Start watching", "episode", "Player"); delete `PlayerOnboarding` from `AnimeOnboarding.kt` once unused
- Test: `animeworld/shared/src/jvmTest/.../onboarding/PlayerSetupStepTest.kt`

**Interfaces:**
- Consumes: `videoPlayerStyle`, `videoPlayerBlur` (`asState()`), `SubtitleSettingsRepository.style` / `update { }` (fields `sizePercent, font, textColor, edge, backgroundPercent, raisePercent, useEmbeddedStyles, autoEnable, preferredLanguages, skipSignsAndSongs`; `coerced()`), `settingsHandling.batteryPercent` (Android), `PlayerStylePreview(style, extras, modifier, animate)`.
- Produces: `fun OnboardingScope.playerSetupStep(repo: SubtitleSettingsRepository, settingsHandling: NewSettingsHandling)`; title "Set up the player"; summary `"Floating player"`.

- [ ] **Step 1: Failing tests** (use `InMemorySubtitleSettingsRepository`): `size_slider_writes_percent` (→ `sizePercent` in 50..200 step 10); `edge_and_font_choices_write_style`; `languages_multi_select`; `subtitle_sample_hidden_when_auto_off`; `battery_hidden_off_android`; `gesture_rows_are_not_clickable`.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*PlayerSetupStepTest"` → FAIL.
- [ ] **Step 3: Implement:** sticky `PlayerStylePreview` with blur from setting and an overlaid subtitle line rendered from `SubtitleStyle` via the player's subtitle text style; controls per spec table.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(anime): player setup onboarding step`

### Task 13: NovelWorld reading-comfort step + text size setting

**Files:**
- Create: `novelworld/shared/src/commonMain/kotlin/com/programmersbox/novel/shared/onboarding/ReadingComfortStep.kt`, novel settings key `novelTextSize` (DataStore int, default 16) in the module where novel reader settings live (add a `DataStoreHandler(intPreferencesKey("novel_text_size"), 16)` next to `NovelModule.kt` bindings)
- Modify: `novelworld/shared/.../reader/ReaderCompose.kt` (body text uses `novelTextSize.sp`), `novelworld/shared/.../GenericSharedNovel.kt` (register step + `onboardingConfig` = novel genres, "Start reading", "chapter", "Reader")
- Test: add `jvmTest` source set to `novelworld/shared/build.gradle.kts` with `commonLibs.cmp.ui.test` (same as `mangaworld/shared:64`); `novelworld/shared/src/jvmTest/.../onboarding/ReadingComfortStepTest.kt`

**Interfaces:**
- Produces: `fun OnboardingScope.readingComfortStep(textSize: DataStoreHandler<Int>)`; title "Reading comfort"; summary `"16px text"`.

- [ ] **Step 1: Failing tests:** `slider_range_12_to_24`; `slider_writes_text_size`; `sample_uses_text_size`.
- [ ] **Step 2: Run** `./gradlew :novelworld:shared:jvmTest` → FAIL.
- [ ] **Step 3: Implement** step + setting; reader reads the setting.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(novel): reading comfort step and text size setting`

### Task 14: Full verification

- [ ] **Step 1:** `./gradlew :kmpuiviews:jvmTest :mangaworld:shared:jvmTest :animeworld:shared:jvmTest :novelworld:shared:jvmTest` → all PASS.
- [ ] **Step 2:** `./gradlew :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug :mangaworld:desktop:compileKotlinJvm :animeworld:desktop:compileKotlinJvm` → BUILD SUCCESSFUL.
- [ ] **Step 3:** On a device/emulator (`adb -s <serial> install` the noFirebaseDebug APK; package suffix `.noFirebase.debug`), clear onboarding flag (`adb shell pm clear` only on a test install) and walk all 9 steps in MangaWorld and AnimeWorld: Skip dialog both buttons, Updates off/on, Look color sweep, tablet/Expanded two-pane (resizable emulator or desktop window). Record a screen recording of the flow.
- [ ] **Step 4:** Commit any fixes; no new features.
