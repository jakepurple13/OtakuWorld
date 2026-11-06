# In-App Banners Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace snackbars, the Sonner toaster and screen-level system toasts with one app-wide floating banner stack driven by a Koin `InAppNotifier`.

**Architecture:** A pure-Kotlin `DefaultInAppNotifier` (`sharedcomponents/commonMain`) owns a `StateFlow<List<Banner>>`, per-banner timers, keys and pause state. A Compose `InAppBannerHost` reads it and draws the stack (mockup B), and it is placed once in `HomeNav` and once in `NotificationSettingsActivity`. Call sites then switch over one area per task, and Sonner is removed once nothing uses it.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (Material 3), Koin, kotlinx.coroutines (`runTest` virtual time), MaterialKolor, androidx lifecycle-runtime-compose.

**Spec:** `docs/superpowers/specs/2026-11-06-in-app-banners-design.md`. Approved mockup: https://claude.ai/artifact/NNSrbvXLroAwXxmriWCHNG (variant B). Inventory: https://claude.ai/artifact/MtkrVeYVHBcxgcqwLwBnaH

## Global Constraints

- Do not touch these 6 system toasts: `mihonsupport/.../CloudflareInterceptor.kt:263`, `mihonsupport/.../WebViewInterceptor.kt:72`, `source_utilities/aniyomi/.../CloudflareInterceptor.kt:126`, `source_utilities/aniyomi/.../WebViewInterceptor.kt:61`, `source_utilities/src/main/java/.../NetworkHelper.kt:174`, `source_utilities/cloudstream/.../cloudstream3/CommonActivity.kt:70`.
- Do not touch `animeworldtv/` or `app/` (neither module is in `settings.gradle.kts`).
- Durations: success 4000 ms, info 5000 ms, success/info with an action 6000 ms, error 7000 ms, progress none. The reader's "Marked as read" uses 2000 ms.
- At most 20 banners are kept (the oldest is dropped). At most 3 are visible in the collapsed deck.
- Phone: top-center, 12dp side margins, max width 480dp, below `statusBars` + `displayCutout`. Expanded width and desktop: top-right, 16dp from the top and end edges, 360dp wide.
- Copy changes are limited to those in the spec's Migration table. All other text keeps its current wording and resource.
- Always build the `noFirebase` flavor. Commit messages follow `type(scope): summary` and end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Every lazy item touched keeps or gets `Modifier.animateItem()` (project rule).

## Review Focus

1. **Banner sent before any host is composed** (cold start, from a worker or an IO thread): it must sit in `banners` and appear once a host collects. Pinned in Task 1 (`bannerSentWithoutCollectorIsKept`).
2. **Undo/Retry tapped after the user left the sending screen:** the action must still run. Pinned in Task 1 (`actionRunsAfterCallerScopeCancelled`).
3. **Progress result after the user swiped the progress card away** (for example, swiping away "Casting…" before the cast result): the result still appears, as a new banner. Pinned in Task 1 (`progressResultAfterDismissShowsNewBanner`).
4. **An `onAction` that throws** (for example, a DB error during Undo): the banner is dismissed, nothing crashes, and later banners still work. Pinned in Task 1 (`throwingActionDoesNotBreakNotifier`).
5. **Rapid same-key sends** (paging quickly through chapters): exactly one card, with its timer reset each time. Pinned in Task 1 (`sameKeyReplacesInPlaceAndResetsTimer`).

---

### Task 1: Notifier core

**Files:**
- Create: `sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/notifier/Banner.kt` (model + `BannerDefaults`)
- Create: `sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/notifier/InAppNotifier.kt` (interfaces)
- Create: `sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/notifier/DefaultInAppNotifier.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.kt` (in `appModule`)
- Test: `sharedcomponents/src/commonTest/kotlin/com/programmersbox/sharedcomponents/notifier/DefaultInAppNotifierTest.kt`

The core lives in `sharedcomponents`: `kmpuiviews` already depends on it (`api(projects.sharedcomponents)`), and `sharedcomponents` must post QR messages (Task 5), so it can't depend on `kmpuiviews`. Package `com.programmersbox.sharedcomponents.notifier`.

**Interfaces:**
- Produces:
  ```kotlin
  enum class BannerKind { Success, Info, Error, Progress }
  enum class PauseReason { Expanded, Background }
  @JvmInline value class BannerId(val value: Long)
  data class Banner(
      val id: BannerId, val kind: BannerKind, val title: String, val subtitle: String?,
      val icon: ImageVector?, val action: String?, val key: String?,
      val fraction: Float?, val durationMillis: Long?, val onAction: suspend () -> Unit,
  )
  object BannerDefaults {
      const val SuccessMillis = 4_000L; const val InfoMillis = 5_000L
      const val WithActionMillis = 6_000L; const val ErrorMillis = 7_000L
      const val MaxBanners = 20; const val MaxVisible = 3
  }
  interface InAppNotifier {
      val banners: StateFlow<List<Banner>>          // newest first
      fun success(title: String, subtitle: String? = null, icon: ImageVector? = null, action: String? = null,
                  key: String? = null, durationMillis: Long? = null, onAction: suspend () -> Unit = {}): BannerId
      fun info(/* same parameters as success */): BannerId
      fun error(/* same parameters as success */): BannerId
      fun progress(title: String, subtitle: String? = null, key: String? = null): ProgressHandle
      fun dismiss(id: BannerId)
      fun dismissAll()
      fun performAction(id: BannerId)                 // runs onAction in notifier scope, then dismisses
      fun setPaused(reason: PauseReason, paused: Boolean)
      fun setTimeoutAdjuster(adjust: (millis: Long, hasAction: Boolean) -> Long)
  }
  interface ProgressHandle {
      val id: BannerId
      fun update(title: String? = null, subtitle: String? = null, fraction: Float? = null)
      fun success(title: String, subtitle: String? = null, action: String? = null, onAction: suspend () -> Unit = {})
      fun info(/* same */); fun error(/* same */)
      fun dismiss()
  }
  class DefaultInAppNotifier(
      scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
  ) : InAppNotifier
  ```
  Koin: `single<InAppNotifier> { DefaultInAppNotifier() }`.

- [ ] **Step 1: Write the failing tests** in `DefaultInAppNotifierTest`. Use `runTest`, build `DefaultInAppNotifier(backgroundScope)` (pass the test's `StandardTestDispatcher` scope so `delay` follows virtual time), and use `advanceTimeBy` / `runCurrent`.
  - `newestFirst`: success("A"), info("B") → `banners.value.map { it.title } == listOf("B","A")`.
  - `defaultDurations`: success with no action is gone at 4001 ms and present at 3999 ms; with `action="View"` it's still present at 5999 ms; error is present at 6999 ms and gone at 7001 ms; `durationMillis = 2_000` is gone at 2001 ms.
  - `pauseKeepsRemainingTime`: success at t=0; at 1000 ms `setPaused(Expanded, true)`; advance 10_000 → still present; `setPaused(Expanded,false)`; advance 2999 → present; advance 2 → gone.
  - `pausedByEitherReason`: Expanded=false while Background=true → timers stay paused.
  - `sameKeyReplacesInPlaceAndResetsTimer`: info("Ch 1", key="r", durationMillis=2000); at 1500 ms info("Ch 2", key="r", durationMillis=2000) → size 1, same `id`, title "Ch 2"; at 3000 ms it's still present; at 3501 ms it's gone.
  - `progressNeverExpiresAndMorphsInPlace`: `val h = progress("Checking")`; advance 60_000 → present; `h.update("Checking 3 of 14", fraction = 3/14f)` → the fraction is set; `h.success("3 new")` → same id, kind Success, `fraction == null`, gone after 4000 ms.
  - `progressResultAfterDismissShowsNewBanner`: `h = progress("Casting…")`; `dismiss(h.id)`; `h.error("Couldn't cast")` → one Error banner whose id differs from `h.id`.
  - `capDropsOldest`: 21 successes → size 20, and the first one sent is missing.
  - `actionRunsOnceAndDismisses`: counter action; `performAction(id)` twice → counter == 1, banner gone.
  - `actionRunsAfterCallerScopeCancelled`: send from a child `Job` scope, cancel that scope, then `performAction` → the action runs.
  - `throwingActionDoesNotBreakNotifier`: action throws `IllegalStateException`; `performAction` → banner gone; then success("next") appears.
  - `bannerSentWithoutCollectorIsKept`: send with no collector → `banners.value.size == 1`.
  - `timeoutAdjusterApplies`: `setTimeoutAdjuster { ms, _ -> ms * 2 }` → success is still present at 7999 ms.
  - `dismissAllClears`.

- [ ] **Step 2: Run, expect a compile failure.** `./gradlew :sharedcomponents:jvmTest --tests "*DefaultInAppNotifierTest"` → FAIL (unresolved references).

- [ ] **Step 3: Implement** `Banner.kt`, `InAppNotifier.kt` and `DefaultInAppNotifier.kt`. State is one `MutableStateFlow<NotifierState(nextId: Long, banners: List<Banner>)>`, changed only through `update {}`. That makes id generation and list edits atomic from any thread, and `banners` is mapped from it with `stateIn`/a derived `StateFlow`. Timers live in a `Map<BannerId, TimerState(remaining: Long, startedAt: TimeMark?, job: Job?)>` that is touched only inside `scope.launch {}` blocks on the notifier's dispatcher, so it needs no lock. Public calls update the state and then `launch` the timer bookkeeping. Pause cancels the job and stores the remaining time; resume relaunches `delay(remaining)`. Elapsed time is measured with `scope.coroutineContext[TestCoroutineScheduler]?.timeSource ?: TimeSource.Monotonic`, which keeps virtual time working. `performAction` wraps `onAction` in `runCatching` and prints the failure, ignores ids that are already gone, and dismisses before invoking. A progress handle that loses its banner sends its result as a new banner.

- [ ] **Step 4: Register in Koin.** Add `single<InAppNotifier> { DefaultInAppNotifier() }` to `appModule` in `di/AppModule.kt`.

- [ ] **Step 5: Run the tests.** `./gradlew :sharedcomponents:jvmTest --tests "*DefaultInAppNotifierTest"` → PASS.

- [ ] **Step 6: Commit.** `feat(notifier): add InAppNotifier with timers, keys and progress`

---

### Task 2: Banner stack UI and hosts

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/notifier/BannerStackLayout.kt` (pure layout math)
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/notifier/BannerCard.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/notifier/InAppBannerHost.kt`
- Modify: `kmpuiviews/src/commonMain/composeResources/values/strings.xml` (add `banner_dismiss` "Dismiss", `banner_clear_all` "Clear all")
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/HomeNav.kt` (root `Box`, after `QuickActionsOverlay()` around line 272)
- Modify: `UIViews/src/main/java/com/programmersbox/uiviews/NotificationSettingsActivity.kt` (wrap content in a `Box` and add the host)
- Test: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/notifier/BannerStackLayoutTest.kt`

**Interfaces:**
- Consumes: `InAppNotifier`, `Banner`, `BannerKind`, `PauseReason`, `BannerDefaults` (Task 1).
- Produces:
  ```kotlin
  data class CardSlot(val translationY: Float, val scale: Float, val alpha: Float, val contentAlpha: Float, val zIndex: Float, val interactive: Boolean)
  fun bannerSlots(heightsPx: List<Float>, expanded: Boolean, peekPx: Float, gapPx: Float): List<CardSlot>
  @Composable fun InAppBannerHost(modifier: Modifier = Modifier)
  ```

- [ ] **Step 1: Write the failing test** `BannerStackLayoutTest`:
  - Collapsed, heights [100,80,90,70], peek 9, gap 8: translationY == [0,9,18,27]; scale == [1f,0.95f,0.90f,0.85f]; alpha of index 3 == 0f; contentAlpha == [1f,0f,0f,0f]; interactive == [true,true,true,false].
  - Expanded, same heights: translationY == [0,108,196,294]; every scale == 1f; every alpha == 1f; every contentAlpha == 1f; every card interactive.
  - zIndex decreases with index.

- [ ] **Step 2: Run, expect failure.** `./gradlew :kmpuiviews:jvmTest --tests "*BannerStackLayoutTest"` → FAIL.

- [ ] **Step 3: Implement `bannerSlots`** to make the test pass.

- [ ] **Step 4: Implement `BannerCard(banner, contentAlpha, onDismiss, onAction, modifier)`**: a `Surface` with 20dp corners and 6dp shadow elevation. Row: 32dp circular icon container (spinner for Progress), title (1 line, ellipsized), subtitle (1 line), `TextButton` action, close `IconButton` with contentDescription `banner_dismiss`. Bottom edge: a countdown `LinearProgressIndicator` (3dp) for timed banners, indeterminate or `fraction` for Progress. Colors by kind:
  - Info → `inverseSurface`/`inverseOnSurface`
  - Error → `errorContainer`/`onErrorContainer`
  - Progress → `primaryContainer`/`onPrimaryContainer`
  - Success → `primaryContainer`/`onPrimaryContainer` of `rememberDynamicColorScheme(seedColor = Color(0xFF3C9A5F).harmonize(MaterialTheme.colorScheme.primary), isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f)`. These are the MaterialKolor APIs already used in `theme/OtakuColorScheme.kt`; `harmonize` comes from `com.materialkolor.ktx`. Memoize per scheme.
  - Default icons: Success `Icons.Default.Check`, Info `Icons.Default.Info`, Error `Icons.Default.ErrorOutline`.
  - Semantics: `liveRegion = Polite` (`Assertive` for Error), with title + subtitle merged.

- [ ] **Step 5: Implement `InAppBannerHost`.**
  - `val notifier = koinInject<InAppNotifier>()`; collect `banners`; keep `expanded` in `rememberSaveable`. When fewer than 2 banners remain, set `expanded = false`. Call `notifier.setPaused(Expanded, expanded)`.
  - `LifecycleResumeEffect(Unit) { setPaused(Background,false); onPauseOrDispose { setPaused(Background,true) } }`.
  - `val am = LocalAccessibilityManager.current`; `LaunchedEffect(am) { notifier.setTimeoutAdjuster { ms, a -> am?.calculateRecommendedTimeoutMillis(ms, containsIcons = true, containsText = true, containsControls = a) ?: ms } }`.
  - Placement: use `currentWindowAdaptiveInfo().windowSizeClass` (already used for `windowSize` in HomeNav) to pick between phone top-center (12dp margins, `widthIn(max = 480.dp)`) and expanded top-end (16dp, `width(360.dp)`). Pad with `windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout).only(Top + Horizontal))`.
  - Measure each card's height with `onSizeChanged`, call `bannerSlots`, and animate each slot field with `animateFloatAsState(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))` into `graphicsLayer`. Key items by `banner.id`. Entry: start at `translationY = -height`, `alpha = 0`.
  - Swipe: `anchoredDraggable` on horizontal anchors (−width, 0, width) with a 40% positional threshold → `notifier.dismiss`. Upward: a `draggable(Vertical)` that dismisses at −40dp.
  - Tapping the collapsed deck (`clickable` on the top card body, only when there are 2 or more banners) sets `expanded = true`. When expanded, a full-size scrim `Box` (`Color.Black.copy(alpha = 0.32f)`, clickable → collapse) sits behind the cards. The `banner_clear_all` `AssistChip` under the last card calls `dismissAll()`.

- [ ] **Step 6: Add the hosts.**
  - `HomeNav.kt`: call `InAppBannerHost()` after the `QuickActionsOverlay()` block, inside the same root `Box`.
  - `NotificationSettingsActivity.kt`: wrap the existing content in `Box(Modifier.fillMaxSize()) { …; InAppBannerHost() }`.

- [ ] **Step 7: Verify.** Run `./gradlew :kmpuiviews:jvmTest --tests "*BannerStackLayoutTest" :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug` → PASS / BUILD SUCCESSFUL.

- [ ] **Step 8: Commit.** `feat(notifier): add floating banner stack host`

---

### Task 3: Migrate Details, lists and library screens

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/notifier/BannerHelpers.kt`
- Modify: `kmpuiviews/src/commonMain/composeResources/values/strings.xml` (`banner_source_missing` "Source not installed", `banner_more_options` "More options", `banner_couldnt_load` "Couldn’t load %1$s", `banner_retry` "Retry")
- Modify (paths under `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/`): `details/DetailsUtils.kt:141`, `details/DetailsPortrait.kt:233`, `details/DetailsLandscape.kt:215`, `details/ChapterSelection.kt:258,268`, `settings/lists/OtakuListDetailScreen.kt:284`, `favorite/FavoriteScreen.kt:152`, `history/HistoryScreen.kt:204,223`, `notifications/NotificationScreen.kt:224,273,310`, `notifications/NotificationCatchUp.kt:174,223`, `recent/RecentViewModel.kt:145` (+ its screen's `SnackbarHost`)

**Interfaces:**
- Consumes: `InAppNotifier` (Task 1). Host already placed (Task 2).
- Produces: `fun InAppNotifier.sourceMissing(title: String, sourceName: String, onMoreOptions: suspend () -> Unit): BannerId` (internal, in `BannerHelpers.kt`). It is non-suspending, so callers resolve strings with `getString` before calling, or the helper takes already-resolved `label`/`actionLabel` parameters if that's simpler.

- [ ] **Step 1: Add the helper and strings.** `sourceMissing` → `error(title = <banner_source_missing>, subtitle = "$title · $sourceName", action = <banner_more_options>, onAction = onMoreOptions)`. Because resolving a compose resource string is suspending, give the helper `missingLabel: String, moreOptionsLabel: String` parameters, and resolve them at the call site with `stringResource` captured in composition.
- [ ] **Step 2: Convert each call site** following the spec's Migration table.
  - `showSnackbar(msg)` becomes `notifier.success/info(...)`.
  - `if (result == SnackbarResult.ActionPerformed) undo()` becomes `action = undoLabel, onAction = { undo() }`.
  - Delete the `currentSnackbarData?.dismiss()` lines, which are no longer needed.
  - DetailsPortrait/Landscape: `result == true` → success "Added to …"; `false` → info "Already in …".
  - RecentViewModel: inject `InAppNotifier` through the constructor, call `error(getString(banner_couldnt_load, sourceName), it.message, action = retryLabel) { reset() }`, then delete `snackbarHostState` and the screen's `SnackbarHost`. Update the ViewModel's Koin definition if it doesn't already use `viewModelOf`.
  - Remove `SnackbarHostState`/`SnackbarHost` from these screens when nothing else uses them.
- [ ] **Step 3: Verify.** `./gradlew :kmpuiviews:jvmTest :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug` → BUILD SUCCESSFUL, and `grep -n "showSnackbar" <the files above>` → no matches.
- [ ] **Step 4: Commit.** `refactor(notifier): move details, lists and library messages to banners`

---

### Task 4: Migrate settings, imports, quick actions and remove Sonner

**Files:**
- Modify: `kmpuiviews/.../presentation/settings/moresettings/MoreSettingsScreen.kt:41-111` (Sonner → progress handle)
- Modify: `kmpuiviews/.../presentation/settings/lists/imports/ImportListScreen.kt:100,111,144`, `ImportFullListScreen.kt:132,143,178`
- Modify: `kmpuiviews/.../presentation/settings/extensions/ExtensionListScreen.kt:402`
- Modify: `kmpuiviews/.../presentation/settings/notifications/NotificationSettingsScreen.kt:123,332`
- Modify: `kmpuiviews/.../presentation/quickactions/QuickActionsOverlay.kt:230-256` (and delete its `SnackbarHost`, `RunningVisuals`, `ResultVisuals` if unused)
- Modify: `kmpuiviews/.../presentation/updatecheck/UpdateCheckErrorsScreen.kt:119`
- Modify: strings: `banner_couldnt_import` "Couldn’t import list", `banner_link_copied` "Link copied"
- Delete: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/ToasterUtils.kt`
- Modify: `kmpuiviews/build.gradle.kts:114`, `mangaworld/build.gradle.kts:105` (remove `implementation(commonLibs.sonner)`), `gradle/common.versions.toml:205` (remove `sonner`)

**Interfaces:**
- Consumes: `InAppNotifier`, `ProgressHandle` (Task 1).

- [ ] **Step 1: Convert the progress flows.**
  - Import screens and MoreSettings: on `Loading`, `handle = notifier.progress("Importing...")` (MoreSettings: "Working...", keeping today's text) held in `remember`. On `Success`, `handle.success("Completed!")`. On `Error`, `handle.error(<banner_couldnt_import>, throwable.message)`. MoreSettings keeps `"Error: ${it.throwable.message}"` as the subtitle.
  - QuickActionsOverlay: `Running` with `userRequested` → `progress(<qa_checking_indeterminate>)`, and update it from progress ticks with `<qa_checking>`. `Finished` maps to: Updates → success(`qa_new_updates`), NoUpdates → info(`qa_no_updates`), SourcesFailed → error(`qa_sources_failed`, action `qa_view`, onAction = onView), CheckFailed → error(`qa_check_failed`). Call `onResultDismissed()` right after sending (the banner owns its own lifetime now). `Idle` → `handle?.dismiss()`.
- [ ] **Step 2: Convert the simple sites.** ExtensionList → success(`banner_link_copied`). NotificationSettings 123 → success(deleted_notifications). NotificationSettings 332 → success(cleared). UpdateCheckErrors: Rechecking/AlreadyRunning → info, Copied → success.
- [ ] **Step 3: Remove Sonner.** Delete `ToasterUtils.kt`, the two gradle lines and the catalog entry. Then `grep -rn "dokar\|sonner\|Toaster" --include='*.kt' --include='*.kts' --include='*.toml' .` (excluding `/build/`) → no matches.
- [ ] **Step 4: Verify.** `./gradlew :kmpuiviews:jvmTest :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug :mangaworld:desktop:compileKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 5: Commit.** `refactor(notifier): move settings and update-check messages to banners, drop Sonner`

---

### Task 5: Migrate readers, AI chat, QR and desktop update message

**Files:**
- Modify: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/reader/ReaderCompose.kt:191-196` and its callers (`Drawer.kt:63,109`, `ReaderCompose.kt:357,439,440,474`)
- Modify: `novelworld/shared/src/commonMain/kotlin/com/programmersbox/novel/shared/reader/ReaderCompose.kt:108-113` and its callers (`Drawer.kt:54,101`, `ReaderCompose.kt:155,203,204`)
- Modify: `kmpuiviews/koogintegration/src/commonMain/kotlin/com/programmersbox/koogintegration/screens/chatscreen/ChatViewModel.kt:44,340,347`, `ChatScreen.kt:181`
- Modify: `sharedcomponents/src/commonMain/kotlin/com/programmersbox/sharedcomponents/qrcode/QrCodeViewer.kt:283`, `sharedcomponents/src/androidMain/kotlin/com/programmersbox/sharedcomponents/qrcode/QrCodeRepository.android.kt:68,160`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/updates/UpdateLaunchDialog.kt:126`
- Modify: strings `banner_marked_read` "Marked as read" (kmpuiviews; the readers depend on kmpuiviews)

**Interfaces:**
- Consumes: `InAppNotifier` (Task 1).

- [ ] **Step 1: Readers.** Change `showToast: () -> Unit` to pass the chapter name: `showToast: (chapterName: String) -> Unit`. Each caller passes the name of the chapter it just marked (the `KmpChapterModel` given to `addChapterToWatched`, using its `name`). The body becomes `notifier.info(markedRead, subtitle = chapterName, key = "reader-chapter", durationMillis = 2_000)`. If a ViewModel callback only has an index, resolve the name from the ViewModel's chapter list at that index. Remove the reader's `SnackbarHost` if nothing else uses it.
- [ ] **Step 2: Chat.** Inject `InAppNotifier` into `ChatViewModel` (update its Koin definition) and remove `snackbarHostState`. Line 340 → success("${title} saved"). Line 347 → info("${title} deleted"). `ChatScreen` line 181 → success("Copied to clipboard"). Remove the `SnackbarHost` from `ChatScreen`.
- [ ] **Step 3: QR.**
  - `QrCodeViewer`: success("Link copied"), keeping the `if (!systemConfirmsCopy)` guard.
  - `QrCodeRepository.android.kt`: get `InAppNotifier` through Koin (`KoinComponent` + `inject()`, or the constructor if the class is Koin-created; check its definition), then success("Qr Code Saved!") and success("Qr Code Copied!"). Keep the SDK guard on the copy message.
- [ ] **Step 4: Desktop.** In `UpdateLaunchDialog`: `success("Updated to $version", action = "What’s new") { uriHandler.openUri(updater.releasePageUrl()) }`. Capture `uriHandler` before the call. Remove the host state parameter if it becomes unused, and update callers.
- [ ] **Step 5: Verify.** `./gradlew :kmpuiviews:jvmTest :mangaworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug :mangaworld:desktop:compileKotlin :novelworld:desktop:compileKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 6: Commit.** `refactor(notifier): move reader, chat, QR and desktop update messages to banners`

---

### Task 6: Migrate AnimeWorld and CloudStream settings

**Files:**
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerCompose.android.kt:216,226,418`
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/MainActivity.kt:66`
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt:207,299,312,327,381`
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videos/ViewVideoScreen.kt:173`
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/DeleteDialog.kt:56,60`
- Modify: `source_utilities/cloudstream/src/androidMain/kotlin/com/programmersbox/cloudstream/preferences/CloudstreamSettingsActivity.kt:44`
- Modify: `animeworld/src/main/res/values/strings.xml` (`download_started` "Download started", `couldnt_cast` "Couldn’t cast")

**Interfaces:**
- Consumes: `InAppNotifier`, `ProgressHandle` (Task 1, `com.programmersbox.sharedcomponents.notifier`).

- [ ] **Step 1: Casting.** Player (216/226) and GenericAnime (299/312): `val h = notifier.progress("Casting…")`. On `CastResult.Started`, call `h.dismiss()` (the remote screen opens, so no result banner). On error, `h.error(getString(couldnt_cast), errorMessage)`. Dismiss in `finally` if neither path ran.
- [ ] **Step 2: Errors.**
  - Player 418 → error("Playback failed", error.message), sent before `popBackStack()`.
  - MainActivity 66 → error(it).
  - GenericAnime 207 → error("Couldn't load $name", error.message ?: error::class.simpleName). The notifier is thread-safe, so the `runOnUIThread` wrapper can go.
  - GenericAnime 327 → error(source_no_download text).
  - DeleteDialog 56 → success(fileDeleted) or error(fileNotDeleted); DeleteDialog 60 → error(something_went_wrong).
  - CloudstreamSettings 44 → error("Could not open settings", t.message), keeping `finish()`. The activity gets the notifier through `KoinComponent`/`inject()`.
- [ ] **Step 3: Success messages.** GenericAnime 381 → success(download_started) (delete the `withContext(Main)` wrapper). ViewVideoScreen 173 → `success("Subtitles ready", subtitle = title.episode ?: title.show, action = "Play") { navActions.navigate(item.videoScreen…) }`, keeping the existing navigate target exactly. Remove the screen's `SnackbarHost` if unused.
- [ ] **Step 4: Verify.** `./gradlew :animeworld:assembleNoFirebaseDebug :animeworld:desktop:compileKotlin` → BUILD SUCCESSFUL. Then `grep -n "Toast.makeText" animeworld/src animeworld/shared/src` → no matches.
- [ ] **Step 5: Commit.** `refactor(notifier): move AnimeWorld and CloudStream settings messages to banners`

---

### Task 7: Changelog, full verification, device check

**Files:**
- Modify: `CHANGELOG.md` (entry under the unreleased section, in the existing style)

- [ ] **Step 1: Full build.** `./gradlew :sharedcomponents:jvmTest :kmpuiviews:jvmTest :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug :mangaworld:desktop:compileKotlin :animeworld:desktop:compileKotlin :novelworld:desktop:compileKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 2: Leftover scan.** `grep -rn "showSnackbar\|Toast.makeText" --include='*.kt' .` (excluding `/build/`) → only the 6 constrained files, `animeworldtv/` and `app/`.
- [ ] **Step 3: Device check with adb** (no ARTEMIS). Before installing, compare the Room versions with the main checkout (project memory). On the MangaWorld debug APK:
  - add to list from Details → green card
  - History remove → Undo restores the item after you navigate away
  - quick-action Check for updates → progress card turns into the result
  - reader next/previous ×4 → one card replaces itself
  - fire 3 messages → fan out, Clear all, swipe dismiss
  - light and dark themes

  Capture screenshots with `adb exec-out screencap -p`.
- [ ] **Step 4: Desktop check.** `./gradlew :mangaworld:desktop:run`: the stack sits top-right under the title bar, and fan-out works with a mouse.
- [ ] **Step 5: Changelog and commit.** `docs(changelog): add in-app banners`
