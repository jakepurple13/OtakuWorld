# Quick Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A bolt button on Recent and Settings that morphs into a three-row quick-actions card (Check for updates, Scan QR code, Background tasks), plus desktop `⌘K`/`Ctrl+K` and an Actions menu.

**Architecture:** One shared action model and `QuickActionsViewModel` in `kmpuiviews/commonMain`. A Koin singleton `QuickActionsController` holds open/closed state, so the bolt (inside a screen's top bar) and the overlay card (in `HomeNav`'s existing `SharedTransitionLayout`) both see it. The card morphs from the bolt with `Modifier.sharedBounds`. Desktop gets a progress tracker so `WorkRepository.manualCheck` emits real state on JVM.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (Material 3), Koin, Navigation3, Nucleus `DecoratedWindow` (desktop), kotlinx.coroutines + Turbine for tests.

**Spec:** `docs/superpowers/specs/2026-11-05-quick-actions-design.md`
**Mockups:** https://claude.ai/artifact/AyHvpu86CuThQLyhfDCA4V (section "Option 2 at full width", look B; desktop `⌘K` mock)

## Global Constraints

- Bolt appears only on Recent (`RecentScreen.kt`) and Settings home (`SettingScreen.kt`).
- Exactly three actions, fixed order: Check for updates, Scan QR code, Background tasks. Not customizable.
- Phone card is an in-composition overlay, never a `Dialog`/`Popup`.
- Card: full width minus 8dp each side on compact width; capped at 480dp and anchored top-end on wider windows. Top sits just below the status bar.
- Scrim: black at 40% alpha.
- Open: emphasized decelerate `CubicBezierEasing(0.2f, 0f, 0f, 1f)`, ~400ms. Close: emphasized accelerate `CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)`.
- `⌘K`/`Ctrl+K` open: centered, `scaleIn(initialScale = 0.94f) + fadeIn()`, same 480dp cap, no morph.
- Check for updates runs even when scheduled checks are off. Never starts a second check while one is running.
- Result copy (planner's call: app-neutral wording, since the strings are shared by all three apps): "<n> new" / "No new updates" / "Some sources failed" + **View** (opens `Screen.UpdateCheckErrors`). All strings go in `kmpuiviews/src/commonMain/composeResources/values/strings.xml`.
- Every lazy item gets `Modifier.animateItem()` (project rule), if a lazy list is used.
- Build with the `noFirebase` flavor.

## Review Focus

1. **Run while a scheduled check is already running (Android).** Expect no second check, and the bolt still spins. `ExistingWorkPolicy.KEEP` already handles the one-time queue; the ViewModel must also treat a running periodic check as busy. Test in Task 3.
2. **Run tapped twice fast on desktop.** Expect one check. Test in Task 2 (`tryStart` returns false the second time).
3. **The check throws on desktop.** Expect the spinner to stop and the failure result to show, not a spinner stuck forever. Test in Task 2 (`finish(success = false)` after an exception).
4. **No report yet (fresh install).** Expect the row to say "Never checked", not crash or show "0 ms ago". Test in Task 1.
5. **Back pressed or scrim tapped mid-morph.** Expect the card to reverse into the bolt cleanly. Verify manually on device in Task 5.

---

### Task 1: Action model and pure state mapping

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickAction.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsState.kt`
- Test: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsStateTest.kt`

**Interfaces:**
- Produces:
  - `enum class QuickAction { CheckForUpdates, ScanQrCode, BackgroundTasks }`, with `QuickAction.entries` as the display order.
  - `sealed interface ManualCheckState { data object Idle; data class Running(val checked: Int?, val total: Int?); data class Finished(val outcome: CheckOutcome) }`
  - `sealed interface CheckOutcome { data class Updates(val count: Int); data object NoUpdates; data object SourcesFailed }`
  - `fun manualCheckState(work: List<WorkInfoKmp>, report: UpdateCheckReport?, sawRunning: Boolean): ManualCheckState`
  - `fun lastCheckedLabel(report: UpdateCheckReport?, nowMillis: Long): LastChecked`, where `sealed interface LastChecked { data object Never; data class Ago(val minutes: Long) }`

- [ ] **Step 1: Write the failing tests**

```kotlin
class QuickActionsStateTest {
    private fun work(state: String, p: Int? = null, m: Int? = null) =
        WorkInfoKmp(state, "", p, m, LocalDateTime(2026, 1, 1, 0, 0))
    private fun report(updated: Int, failures: Boolean = false) = UpdateCheckReport(
        startedAt = 0, finishedAt = 1, favoriteCount = 42, checkedCount = 42, updatedCount = updated,
        sourceFailures = if (failures) listOf(SourceFailure("MangaRead", FailureKind.TimedOut, null, "timeout")) else emptyList(), itemFailures = emptyList(),
    )

    @Test fun runningWithProgress() = assertEquals(
        ManualCheckState.Running(3, 42), manualCheckState(listOf(work("RUNNING", 3, 42)), null, sawRunning = false))
    @Test fun enqueuedCountsAsRunning() = assertEquals(
        ManualCheckState.Running(null, null), manualCheckState(listOf(work("ENQUEUED")), null, false))
    @Test fun idleWhenNothingRanThisSession() = assertEquals(
        ManualCheckState.Idle, manualCheckState(listOf(work("SUCCEEDED")), report(3), sawRunning = false))
    @Test fun finishedWithUpdates() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.Updates(3)), manualCheckState(listOf(work("SUCCEEDED")), report(3), true))
    @Test fun finishedNoUpdates() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.NoUpdates), manualCheckState(emptyList(), report(0), true))
    @Test fun failuresWinOverUpdates() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.SourcesFailed), manualCheckState(listOf(work("SUCCEEDED")), report(3, failures = true), true))
    @Test fun failedWorkIsSourcesFailed() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.SourcesFailed), manualCheckState(listOf(work("FAILED")), report(0), true))
    @Test fun neverChecked() = assertEquals(LastChecked.Never, lastCheckedLabel(null, 1_000))
    @Test fun minutesAgo() = assertEquals(LastChecked.Ago(180), lastCheckedLabel(report(0).copy(finishedAt = 0), 180 * 60_000L))
}
```

Rules the tests pin: any `RUNNING`/`ENQUEUED`/`BLOCKED` entry means Running, using that entry's `progress`/`max`. Otherwise, if `sawRunning` is false, the state is Idle (an old finished check shouldn't show a result). Otherwise it's Finished: `SourcesFailed` if any work is `FAILED` or `report.hasProblems`, else `Updates(report.updatedCount)` when that is above 0, else `NoUpdates`.

- [ ] **Step 2: Run, expect FAIL (unresolved references)**

Run: `./gradlew :kmpuiviews:jvmTest --tests "*QuickActionsStateTest"`

- [ ] **Step 3: Implement the enum, sealed types and two functions** in the files above.

- [ ] **Step 4: Run, expect PASS** (same command).

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions
git commit -m "feat(quick-actions): add action model and check state mapping"
```

---

### Task 2: Desktop manual-check progress

**Files:**
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/DesktopManualCheckTracker.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/WorkerRepositoryImpl.kt` (whole class)
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt:192-254` (`SyncCheckWorker`)
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/di/RepositoryModule.jvm.kt:24`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/DesktopManualCheckTrackerTest.kt`

**Interfaces:**
- Consumes: `WorkInfoKmp` (existing).
- Produces:
  - `class DesktopManualCheckTracker { val state: StateFlow<List<WorkInfoKmp>>; fun tryStart(): Boolean; fun progress(max: Int, progress: Int, source: String); fun finish(success: Boolean) }`
  - State strings match Android's `WorkInfo.State.toString()`: `"RUNNING"`, `"SUCCEEDED"`, `"FAILED"`.
  - `WorkerRepositoryImpl(tracker: DesktopManualCheckTracker)`. `manualCheck` = `tracker.state`.

- [ ] **Step 1: Write the failing tests**

```kotlin
class DesktopManualCheckTrackerTest {
    @Test fun startsEmpty() = assertEquals(emptyList(), DesktopManualCheckTracker().state.value)
    @Test fun secondStartRejectedWhileRunning() {
        val t = DesktopManualCheckTracker()
        assertTrue(t.tryStart()); assertFalse(t.tryStart())
        assertEquals("RUNNING", t.state.value.single().state)
    }
    @Test fun progressIsReported() {
        val t = DesktopManualCheckTracker(); t.tryStart(); t.progress(42, 3, "MangaRead")
        val w = t.state.value.single(); assertEquals(3, w.progress); assertEquals(42, w.max); assertEquals("MangaRead", w.source)
    }
    @Test fun failureEndsRunAndAllowsRestart() {
        val t = DesktopManualCheckTracker(); t.tryStart(); t.finish(success = false)
        assertEquals("FAILED", t.state.value.single().state); assertTrue(t.tryStart())
    }
    @Test fun successEndsRun() {
        val t = DesktopManualCheckTracker(); t.tryStart(); t.finish(true)
        assertEquals("SUCCEEDED", t.state.value.single().state)
    }
}
```

- [ ] **Step 2: Run, expect FAIL**

Run: `./gradlew :kmpuiviews:jvmTest --tests "*DesktopManualCheckTrackerTest"`

- [ ] **Step 3: Implement the tracker.** Use a `MutableStateFlow`, and make `tryStart` atomic with `compareAndSet`/`update` so two fast taps can't both start. `nextScheduleTimeMillis` can be the current local time; nothing reads it for manual checks.

- [ ] **Step 4: Wire it in.**
  - `WorkerRepositoryImpl.checkManually()` and `retryFailedUpdates()`: `if (!tracker.tryStart()) return` / `return false`. Run the existing `TestTaskRunner` call inside `try { … tracker.finish(true) } catch (e: Exception) { tracker.finish(false) }`. `TaskResult.Failure` also counts as `finish(false)`.
  - `SyncCheckWorker`: `private val tracker: DesktopManualCheckTracker by inject()`. In `setProgress`, call `tracker.progress(max, progress, source)`. Keep the println lines out of the new code path.
  - Koin: `singleOf(::DesktopManualCheckTracker)` beside line 24; `singleOf(::WorkerRepositoryImpl) { bind<WorkRepository>() }` resolves the new parameter.
  - The scheduler process (`setupSyncCheckers`) doesn't report into this tracker. That's accepted by the spec.

- [ ] **Step 5: Run the tests, expect PASS.** Then `./gradlew :kmpuiviews:jvmTest` for the whole JVM suite, which should stay green.

- [ ] **Step 6: Commit** — `feat(desktop): report manual update-check progress and block double runs`

---

### Task 3: QuickActionsController and QuickActionsViewModel

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsController.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsViewModel.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.kt` (add `singleOf(::QuickActionsController)`)
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt` (add `viewModelOf(::QuickActionsViewModel)`)
- Test: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1 types; `WorkRepository` (`manualCheck`, `allWorkCheck`, `checkManually()`); `UpdateCheckReportStore.report`; `BackgroundWorkHandler.workerInfoFlow()`; `NavigationActions.scanQrCode()`, `.workerInfo()`, `.navigate(Screen.UpdateCheckErrors)`.
- Produces:
  - `enum class OpenOrigin { Bolt, Keyboard }`
  - `class QuickActionsController { val open: StateFlow<OpenOrigin?>; fun open(origin: OpenOrigin); fun close(); fun toggle(origin: OpenOrigin) }`
  - `data class QuickActionsUiState(val checkState: ManualCheckState, val lastChecked: LastChecked, val scheduledCount: Int?)`. `scheduledCount` is null when `workerInfoFlow()` is empty, and the UI hides the count.
  - `class QuickActionsViewModel(...) : ViewModel { val uiState: StateFlow<QuickActionsUiState>; fun run(action: QuickAction); fun openErrors(); fun dismissResult() }`
  - `run` always calls `controller.close()` first. `CheckForUpdates` calls `checkManually()` only when not Running (Running includes `allWorkCheck` being `RUNNING`). It sets the internal `sawRunning` flag passed to `manualCheckState`. `ScanQrCode` → `scanQrCode()`. `BackgroundTasks` → `workerInfo()`.

- [ ] **Step 1: Write the failing tests** (fakes for `WorkRepository`, `UpdateCheckReportStore`, `BackgroundWorkHandler` backed by `MutableStateFlow`, plus a recording `NavigationActions` fake; use `Dispatchers.setMain(StandardTestDispatcher())`).

```kotlin
@Test fun runCheckStartsOnceAndCloses() = runTest {
    vm.run(QuickAction.CheckForUpdates); fakeWork.manual.value = listOf(running(1, 42))
    vm.run(QuickAction.CheckForUpdates)
    assertEquals(1, fakeWork.checkCalls); assertNull(controller.open.value)
}
@Test fun runCheckSkippedWhilePeriodicRuns() = runTest {
    fakeWork.all.value = listOf(running(null, null)); vm.run(QuickAction.CheckForUpdates)
    assertEquals(0, fakeWork.checkCalls)
}
@Test fun scanQrNavigates() = runTest { vm.run(QuickAction.ScanQrCode); assertEquals(listOf("scanQrCode"), nav.calls) }
@Test fun backgroundTasksNavigates() = runTest { vm.run(QuickAction.BackgroundTasks); assertEquals(listOf("workerInfo"), nav.calls) }
@Test fun finishedShowsResultAfterRun() = runTest {
    vm.uiState.test {
        vm.run(QuickAction.CheckForUpdates)
        fakeWork.manual.value = listOf(running(1, 42)); fakeWork.manual.value = listOf(succeeded())
        fakeReports.report.value = report(updated = 3)
        assertEquals(ManualCheckState.Finished(CheckOutcome.Updates(3)), expectMostRecentItem().checkState)
    }
}
@Test fun emptyWorkerInfoHidesCount() = runTest { vm.uiState.test { assertNull(expectMostRecentItem().scheduledCount) } }
```

- [ ] **Step 2: Run, expect FAIL**

Run: `./gradlew :kmpuiviews:jvmTest --tests "*QuickActionsViewModelTest"`

- [ ] **Step 3: Implement the controller and ViewModel.** `uiState` = `combine(manualCheck, allWorkCheck, report, workerInfoFlow(), sawRunning)` → `stateIn(viewModelScope, WhileSubscribed(5000), …)`. `nowMillis` comes from `Clock.System`. `dismissResult()` resets `sawRunning` to false, so the state returns to Idle after the result snackbar.

- [ ] **Step 4: Run, expect PASS.** Also `./gradlew :kmpuiviews:jvmTest`.

- [ ] **Step 5: Commit** — `feat(quick-actions): add controller and view model`

---

### Task 4: Quick-actions list UI (segmented list B)

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsList.kt`
- Modify: `kmpuiviews/src/commonMain/composeResources/values/strings.xml`

**Interfaces:**
- Consumes: Task 1 and Task 3 types.
- Produces: `@Composable fun QuickActionsList(state: QuickActionsUiState, onAction: (QuickAction) -> Unit, modifier: Modifier = Modifier)`, plus `@Preview`s for idle, running and never-checked.

- [ ] **Step 1: Add strings:** `quick_actions` "Quick actions"; `qa_check_updates` "Check for updates"; `qa_scan_qr` "Scan QR code"; `qa_scan_qr_sub` "Import a title or list"; `qa_background_tasks` "Background tasks"; `qa_scheduled` "%1$d scheduled"; `qa_background_tasks_sub` "See what's scheduled"; `qa_last_checked` "Last checked %1$s"; `qa_never_checked` "Never checked"; `qa_run` "Run"; `qa_checking` "Checking %1$d of %2$d…"; `qa_checking_indeterminate` "Checking for updates…"; `qa_new_updates` "%1$d new"; `qa_no_updates` "No new updates"; `qa_sources_failed` "Some sources failed"; `qa_view` "View".

- [ ] **Step 2: Build the composable.**
  - Title row: "Quick actions" (`titleMedium`), and the trailing count "3" (`labelMedium`, `onSurfaceVariant`).
  - Rows in a `Column(verticalArrangement = spacedBy(3.dp))`. Each row is a `Surface(color = surfaceContainerHigh)`. Shapes: first row `RoundedCornerShape(18.dp, 18.dp, 6.dp, 6.dp)`, middle `6.dp`, last `RoundedCornerShape(6.dp, 6.dp, 18.dp, 18.dp)`. Use `ListItem` content with a leading 32dp `RoundedCornerShape(11.dp)` icon box.
  - Icon tones: Check = `primaryContainer`/`onPrimaryContainer`; Scan QR = `tertiaryContainer`/`onTertiaryContainer`; Background tasks = `secondaryContainer`/`onSecondaryContainer`.
  - Icons: `Icons.Default.Refresh`, `Icons.Default.QrCodeScanner`, `Icons.Default.Schedule`.
  - Trailing: Check → a filled small `Button`-style chip labeled "Run" (`primary`/`onPrimary`, pill shape). The other two → `Icons.AutoMirrored.Filled.KeyboardArrowRight`.
  - The whole row is clickable and calls `onAction`.
  - Supporting text for Check: Running → `qa_checking`, else `qa_last_checked`/`qa_never_checked`. Format minutes as "<n>m ago" / "<n>h ago" / "<n>d ago".

- [ ] **Step 3: Verify.** Run `./gradlew :kmpuiviews:compileKotlinJvm` (BUILD SUCCESSFUL). Render the previews in the IDE or the showcase, and compare against mockup look B.

- [ ] **Step 4: Commit** — `feat(quick-actions): add segmented quick-actions list`

---

### Task 5: Bolt, morph overlay and phone wiring

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsBolt.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsOverlay.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/HomeNav.kt:188-255` (wrap the `Row` in a `Box`; add the overlay as the last child, still inside `SharedTransitionLayout`)
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/recent/RecentScreen.kt:168` (`actions`: add the bolt before the source `VerticalPager`)
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/SettingScreen.kt:111` (`actions`: add the bolt before `SyncIconComposable`)

**Interfaces:**
- Consumes: `QuickActionsController`, `QuickActionsViewModel`, `QuickActionsList`, `LocalSharedElementScope` (`utils/composables/sharedelements/SharedElementUtils.kt`).
- Produces:
  - `const val QuickActionsSharedKey = "quick-actions-card"`
  - `@Composable fun QuickActionsBolt(modifier: Modifier = Modifier)`
  - `@Composable fun SharedTransitionScope.QuickActionsOverlay()`

- [ ] **Step 1: `QuickActionsBolt`.** It's a fixed 48dp `Box`, so the action row never shifts. Inside, `AnimatedVisibility(visible = open != OpenOrigin.Bolt)` holds a 40dp `primaryContainer` circle with `Icons.Default.Bolt`. Give that circle `Modifier.sharedBounds(rememberSharedContentState(QuickActionsSharedKey), this@AnimatedVisibility, resizeMode = RemeasureToBounds, clipInOverlayDuringTransition = OverlayClip(CircleShape))`, taking the scope from `LocalSharedElementScope`. While `uiState.checkState is Running`, draw an indeterminate `CircularProgressIndicator` ring (40dp, 2dp stroke) over it. Clicking calls `controller.toggle(OpenOrigin.Bolt)`. Content description = `quick_actions`.

- [ ] **Step 2: `QuickActionsOverlay`.**
  - Scrim: `AnimatedVisibility(open != null, fadeIn/fadeOut)`, a full-size `Box` with `Color.Black.copy(alpha = 0.4f)`; tapping it calls `controller.close()`.
  - Bolt origin: `AnimatedVisibility(open == OpenOrigin.Bolt)` holds a `Surface(color = surfaceContainer, shape = RoundedCornerShape(26.dp))` with the same `sharedBounds` key. Place it top-end with `windowInsetsPadding(WindowInsets.statusBars)` and 8dp padding. Width: `fillMaxWidth()` on compact width, `widthIn(max = 480.dp)` otherwise (read `LocalWindowSizeClass`). Content is `QuickActionsList`, wrapped in `Modifier.animateEnterExit(fadeIn(tween(delayMillis = 150)), fadeOut(tween(100)))` so rows appear once the card is mostly open. Use the spec's open/close easings via `boundsTransform`.
  - Keyboard origin: a separate `AnimatedVisibility(open == OpenOrigin.Keyboard, enter = scaleIn(initialScale = 0.94f) + fadeIn(), exit = fadeOut())` that holds the same card centered, with no `sharedBounds`.
  - Back: `BackHandler(enabled = open != null) { controller.close() }` (multiplatform `BackHandler` from `androidx.compose.ui.backhandler`, or the project's existing back-handler helper if one exists).
  - Snackbar: a `SnackbarHost` aligned bottom-center above the nav, padded by `LocalNavHostPadding`. A `LaunchedEffect(uiState.checkState)` shows Running text (`qa_checking`/`qa_checking_indeterminate`, `SnackbarDuration.Indefinite`). Finished replaces it: `qa_new_updates`, `qa_no_updates`, or `qa_sources_failed` with action `qa_view` → `vm.openErrors()`. After a Finished snackbar is dismissed, call `vm.dismissResult()`.

- [ ] **Step 3: Wire it into HomeNav, Recent and Settings** at the lines above.

- [ ] **Step 4: Build and install on a device.** `./gradlew :mangaworld:assembleNoFirebaseDebug`, then install with `adb`. Check Room versions against the main checkout before installing (project memory). Check each item:
  - Recent bolt → the card morphs out full-width, under the status bar.
  - Tap the scrim → the card shrinks back into the bolt.
  - Back press mid-open → it reverses cleanly.
  - Run → the card closes, the bolt spins, the snackbar counts, and the result snackbar appears.
  - Scan QR → the QR screen opens. Background tasks → Worker info opens.
  - The Settings bolt does the same.
  - A rotated phone or tablet shows the card capped at 480dp, top-end.
  - Take an `adb exec-out screencap` of the open card for the PR.

- [ ] **Step 5: Commit** — `feat(quick-actions): add bolt morph overlay on Recent and Settings`

---

### Task 6: Desktop ⌘K and Actions menu

**Files:**
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsDesktop.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/DesktopUi.kt:382-430` (`OtakuDecoratedWindow`) and its HomeNav call sites (~282, ~350)

**Interfaces:**
- Consumes: `QuickActionsController`, `QuickActionsViewModel.run`, `QuickAction.entries`, Task 4 strings.
- Produces:
  - `fun isQuickActionsShortcut(event: KeyEvent, isMac: Boolean): Boolean` (pure, testable): KeyDown + `Key.K` + (`isMetaPressed` on mac, `isCtrlPressed` elsewhere), with no other modifiers.
  - `@Composable fun FrameWindowScope.QuickActionsMenuBar()` (macOS)
  - `@Composable fun QuickActionsTitleBarMenu()` (Windows/Linux)
  - `OtakuDecoratedWindow(…, showQuickActions: Boolean = false, …)`. Only the HomeNav window passes `true`.

- [ ] **Step 1: Write the failing test** `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsShortcutTest.kt`. It covers: mac ⌘K → true; mac Ctrl+K → false; Windows Ctrl+K → true; Ctrl+Shift+K → false; KeyUp → false. Build `KeyEvent`s with `KeyEvent(Key.K, KeyEventType.KeyDown, isMetaPressed = …)`.

- [ ] **Step 2: Run, expect FAIL:** `./gradlew :kmpuiviews:jvmTest --tests "*QuickActionsShortcutTest"`

- [ ] **Step 3: Implement `isQuickActionsShortcut`, then run, expect PASS.**

- [ ] **Step 4: Wire the window** (when `showQuickActions`):
  - Pass `onPreviewKeyEvent = { if (isQuickActionsShortcut(it, isMac)) { controller.toggle(OpenOrigin.Keyboard); true } else false }` to `DecoratedWindow`. `Esc` while open → `controller.close()`.
  - macOS: call `QuickActionsMenuBar()` inside the `DecoratedWindow` content. Its scope is `AwtDecoratedWindowScope : FrameWindowScope`, so Compose `MenuBar { Menu("Actions") { … } }` works. Items are the three actions (`Item(onClick = { vm.run(it) })`), a separator, then "Quick actions…" with `shortcut = KeyShortcut(Key.K, meta = true)` → `controller.open(OpenOrigin.Keyboard)`.
  - Windows/Linux: put a `TextButton("Actions")` inside the `MaterialTitleBar` content, aligned start after the window icon area. It opens a Material3 `DropdownMenu` with the same items; label the last one "Quick actions…  Ctrl+K".
  - `isMac` comes from `System.getProperty("os.name").startsWith("Mac")`, or an existing platform helper if the project has one.

- [ ] **Step 5: Run the desktop app.** `./gradlew :mangaworld:desktop:run`, then check:
  - ⌘K opens the centered card (scale and fade), and Esc closes it.
  - The Recent bolt morphs (capped at 480dp, top-end).
  - Actions menu → Check for updates → the bolt spins, the snackbar counts up, and the result shows.
  - Actions → Scan QR and Background tasks navigate.
  - On Windows/Linux, run only if a machine is available; otherwise say in the PR that they're untested.

- [ ] **Step 6: Commit** — `feat(desktop): add Cmd/Ctrl+K and Actions menu for quick actions`

---

### Task 7: Changelog and full verification

**Files:**
- Modify: `CHANGELOG.md` (add an entry under the unreleased section, following existing style)

- [ ] **Step 1:** `./gradlew :kmpuiviews:jvmTest :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug` → BUILD SUCCESSFUL.
- [ ] **Step 2:** Add the changelog entry.
- [ ] **Step 3: Commit** — `docs(changelog): add quick actions`
