# Desktop Refresh Button Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Refresh button at the end of an address-bar pill in the desktop tab toolbar that reloads the current page; on Details it fetches the item again, skipping the details cache. On Android and iOS, the same Details refresh is a Refresh item at the top of the Details overflow menu.

**Architecture:** Pages announce "I can refresh" through a tiny `commonMain` CompositionLocal (`LocalPageRefreshHost`, default `null`), so on Android and iOS every registration is a no-op. On desktop, `TabContent` provides a per-tab `TabRefreshRegistry` (jvmMain) as that host; the toolbar, tab chip and shortcuts read the registry from `TabState`. Only `DetailsViewModel` gains new logic in shared code (an extracted `load(force)`); every other page registers a method it already has.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (Material 3), Navigation3, Koin, kotlinx-coroutines-test, compose ui-test (jvm).

**Spec:** Desktop mockup https://claude.ai/artifact/S6y5dirXvK9uuVFhvv2wxD (layout C), mobile mockup https://claude.ai/artifact/LLWp9UpW43GfnCu4GYFgv5 (layout A), plus decisions in chat on 2026-11-27, summarized under Global Constraints.

## Global Constraints

- Desktop first. Shared (`commonMain`) changes are limited to: `PageRefresh.kt` (new), `DetailsViewModel` load extraction, one `RegisterPageRefresh(...)` line per screen, and the mobile Details menu item + progress line + field highlight (Task 6). No other Android/iOS UI changes.
- Mobile Details (Task 6): Refresh is the first row of the overflow menu, then a divider, then the existing items unchanged. Tap closes the menu; a `LinearProgressIndicator` shows under the top bar; while loading the row reads `Refreshing…`, disabled, with a small spinner in place of the icon. No success banner. Failure uses the same error banner as desktop. No Stop on mobile. The menu row and the mobile progress line are hidden on desktop (when `LocalPageRefreshHost.current != null`), where the toolbar covers them.
- Field highlight after a successful refresh (all platforms, it lives in the shared Details screen): the chapter count, description and genre row flash `primaryContainer` behind the text for 900 ms when their value changed; new chapter rows enter through their existing `Modifier.animateItem()`.
- Layout C: Back, Forward, then a rounded pill (path text + Refresh at its end), then Search, Lists, Downloads, Settings.
- While loading: the Refresh icon morphs into Stop (X) when the page can stop, a 2dp `LinearProgressIndicator` replaces the toolbar's `HorizontalDivider`, and the tab chip shows a small spinner.
- Click motion: one 360° turn, `tween(450, easing = EmphasizedDecelerateEasing)`. Refresh↔Stop: `AnimatedContent`, `scaleIn + fadeIn togetherWith scaleOut + fadeOut`. Respect reduced motion through `TabMotionState` (no turn, plain crossfade).
- Pages with nothing to reload: button disabled, tooltip `This page is always up to date`.
- Tooltips: `Refresh (⌘R)` on macOS / `Refresh (Ctrl+R)` elsewhere; `Stop loading (Esc)` while loading.
- Shortcuts: ⌘R (macOS), Ctrl+R (others), and bare F5 on every OS (the first bare-key tab shortcut, documented as such). Esc stops a load only while the active page is refreshing and can stop.
- Details refresh: removes `Cached.cache[url]`, fetches with `toInfoModel()`, writes the result back. Old details stay on screen while loading. On failure the old details stay and an error banner shows: title `Couldn't refresh`, subtitle `Showing the details you already had`.
- Every new user-facing string goes in `kmpuiviews/src/commonMain/composeResources/values/strings.xml`.

## Review Focus

1. Refresh pressed twice quickly: the second press is ignored while the first load runs (no double fetch, no double spin). Test in Task 2 and Task 3.
2. Navigating away while a Details refresh runs: the page's registration is removed, so the toolbar never shows another page as loading or calls a disposed ViewModel. Test in Task 1.
3. Details refresh on a page that first failed to load (`DetailState.Error`): refresh retries, and success replaces the error. Test in Task 2.
4. Esc while a text field is focused or a dialog is open and nothing is refreshing: Esc is not consumed. Test in Task 4.
5. Back/forward to a page whose ViewModel already exists: the registration comes back with the page (it registers on composition, not on creation). Test in Task 1.

---

## File map

| File | Change | Responsibility |
|---|---|---|
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/components/PageRefresh.kt` | Create | `PageRefresh`, `PageRefreshHost`, `LocalPageRefreshHost`, `RegisterPageRefresh` |
| `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabRefresh.kt` | Create | `TabRefreshRegistry` (the desktop host), `TabsController.refresh/stopRefresh` |
| `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/RefreshButton.kt` | Create | The button: tooltip, kick turn, Refresh↔Stop morph, disabled state |
| `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabModels.kt` | Modify | `TabState.refresh: TabRefreshRegistry` |
| `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabContent.kt` | Modify | Provide `LocalPageRefreshHost provides tab.refresh` |
| `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabToolbar.kt` | Modify | Path pill with `RefreshButton`; progress line in place of the divider |
| `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabChip.kt` | Modify | Spinner while `tab.refresh.refreshing` |
| `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabShortcuts.kt` | Modify | `TabShortcut.Refresh`, `TabShortcut.StopRefresh`, ⌘R/Ctrl+R/F5/Esc mapping, caps rows |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsViewModel.kt` | Modify | `load(force)`, `isRefreshing`, `refresh()`, `stopRefresh()` |
| `.../presentation/details/DetailsScreen.kt` | Modify | One `RegisterPageRefresh` line |
| `.../presentation/recent/RecentScreen.kt`, `all/AllScreen.kt`, `globalsearch/GlobalSearchScreen.kt`, `settings/prerelease/PrereleaseScreen.kt`, `settings/extensions/ExtensionListScreen.kt` | Modify | One `RegisterPageRefresh` line each |
| `kmpuiviews/src/jvmMain/.../translation/ui/DesktopTranslationViewModel.kt` + its screen | Modify | Make `refreshOllama` reachable as `refresh()`; register it |
| `kmpuiviews/src/commonMain/composeResources/values/strings.xml` | Modify | New strings |
| Tests: `jvmTest/.../tabs/TabRefreshTest.kt` (new), `jvmTest/.../tabs/RefreshButtonTest.kt` (new), `jvmTest/.../tabs/TabShortcutsTest.kt`, `jvmTest/.../presentation/details/DetailsViewModelTest.kt` | Create/Modify | |

---

### Task 1: Refresh registration (shared contract + desktop registry)

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/components/PageRefresh.kt`
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabRefresh.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabModels.kt` (`TabState`), `TabContent.kt:37-42`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/tabs/TabRefreshTest.kt`

**Interfaces:**
- Produces (commonMain):
  - `class PageRefresh(val refreshing: () -> Boolean, val refresh: () -> Unit, val stop: (() -> Unit)?)`
  - `fun interface PageRefreshHost { fun register(page: PageRefresh): () -> Unit }` (returns the unregister call)
  - `val LocalPageRefreshHost: ProvidableCompositionLocal<PageRefreshHost?>` (`staticCompositionLocalOf { null }`)
  - `@Composable fun RegisterPageRefresh(refreshing: Boolean, onRefresh: () -> Unit, onStop: (() -> Unit)? = null)`: no-op when the host is null; uses `rememberUpdatedState` for all three so recomposition doesn't re-register; registers in a `DisposableEffect(host)`.
- Produces (jvmMain):
  - `class TabRefreshRegistry : PageRefreshHost` with `val page: PageRefresh?` (snapshot state, last registered wins, unregister restores the previous one), `val canRefresh: Boolean`, `val refreshing: Boolean`, `val canStop: Boolean` (= `refreshing && page?.stop != null`)
  - `TabState.refresh: TabRefreshRegistry` (`val`, created with the tab)
  - `fun TabsController.refresh(id: TabId)`: calls `page.refresh()` unless already refreshing
  - `fun TabsController.stopRefresh(id: TabId)`: calls `page.stop()` if `canStop`

- [ ] **Step 1: Write the failing tests** in `TabRefreshTest`:
  - `registry is empty until a page registers`: `canRefresh == false`, `refreshing == false`.
  - `last registration wins and unregister restores the previous`: register A then B, `page === B`; call B's unregister, `page === A`.
  - `refresh is ignored while refreshing`: register with `refreshing = { true }`, counter in `refresh`; `controller.refresh(tab.id)` leaves counter at 0.
  - `stop only when the page can stop`: page with `stop = null` and `refreshing = { true }`: `canStop == false`.
  - Compose test `registration leaves with the page` (`runComposeUiTest`): provide `LocalPageRefreshHost provides registry`, show `RegisterPageRefresh(...)` behind a `var shown by mutableStateOf(true)`; assert `canRefresh`; set `shown = false`, `waitForIdle()`, assert `!canRefresh`; set back to `true`, assert `canRefresh` again (Review Focus 2 and 5).
  - `no host means no-op`: `RegisterPageRefresh` composed with no provider does not throw.

- [ ] **Step 2: Run, expect FAIL** (unresolved references)

Run: `./gradlew :kmpuiviews:jvmTest --tests "*TabRefreshTest*"`

- [ ] **Step 3: Implement** the interfaces above; add `TabState.refresh`; in `TabContent` add `LocalPageRefreshHost provides tab.refresh` to the existing `CompositionLocalProvider`.

- [ ] **Step 4: Run, expect PASS**, then `./gradlew :kmpuiviews:jvmTest --tests "*Tab*"` still green.

- [ ] **Step 5: Commit** `feat(desktop-tabs): page refresh registration per tab`

---

### Task 2: Details refresh without the cache

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsViewModel.kt:196-234`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsScreen.kt`
- Modify: `kmpuiviews/src/commonMain/composeResources/values/strings.xml` (`details_refresh_failed` = `Couldn't refresh`, `details_refresh_failed_subtitle` = `Showing the details you already had`)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsViewModelTest.kt`

**Interfaces:**
- Consumes: `RegisterPageRefresh` (Task 1); `InAppNotifier.error(title, subtitle)` (inject it if the VM doesn't have it; the Koin binding exists).
- Produces on `DetailsViewModel`:
  - `var isRefreshing: Boolean` (`mutableStateOf`, private set)
  - `fun refresh()`: no-op while `isRefreshing`; sets `isRefreshing = true`, cancels the running load job, `Cached.cache.remove(url)`, then `load(force = true)`
  - `fun stopRefresh()`: cancels the load job, `isRefreshing = false`, state unchanged
  - `private fun load(force: Boolean): Job`: the current `init` flow; `force` skips the cache read. On failure: if the state is already `Success`, keep it and send the banner; otherwise set `DetailState.Error` as today. Every exit clears `isRefreshing` (use `onCompletion`). Error reporting (`crashReporter`, `SourceError`, `exceptionDao`) stays as is for both paths.
- `init` calls `load(force = false)`. `setup(item)` must tolerate a second call; if it starts collectors, guard it so it runs once and later loads only update `info`/`detailState`/`description`.

- [ ] **Step 1: Write the failing tests** (follow the file's existing `setMain(newSingleThreadContext)` / `clearAndJoin()` teardown pattern):
  - `refresh skips the cache`: seed `Cached.cache[url]` with info titled `"Old"`, fake source returns `"New"`; create VM, wait for Success `"Old"`; `refresh()`; wait; state is Success `"New"` and `Cached.cache[url]!!.title == "New"`.
  - `refresh failure keeps old details`: cached `"Old"`, source throws; `refresh()`; state still Success `"Old"`, `isRefreshing == false`, fake notifier got one `error` call.
  - `refresh after an error retries`: source fails first, then succeeds with `"New"`; state Error, `refresh()`, state Success `"New"` (Review Focus 3).
  - `second refresh while refreshing is ignored`: source suspends on a `CompletableDeferred`; call `refresh()` twice; source called once (Review Focus 1).
  - `stopRefresh cancels and keeps state`: suspended source; `refresh()`, `stopRefresh()`; `isRefreshing == false`, state still Success `"Old"`.
  - `ItemViewed logs once across refresh`: one analytics `ItemViewed` after load + refresh.

- [ ] **Step 2: Run, expect FAIL**

Run: `./gradlew :kmpuiviews:jvmTest --tests "*DetailsViewModelTest*"`

- [ ] **Step 3: Implement** per Interfaces; add `RegisterPageRefresh(details.isRefreshing, details::refresh, details::stopRefresh)` near the top of `DetailsScreen`.

- [ ] **Step 4: Run, expect PASS.** Also `./gradlew :mangaworld:assembleNoFirebaseDebug` compiles (shared code changed).

- [ ] **Step 5: Commit** `feat(details): refresh that skips the details cache`

---

### Task 3: Refresh button in the path pill

**Files:**
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/RefreshButton.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabToolbar.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabChip.kt`
- Modify: `strings.xml`: `tabs_refresh` = `Refresh`, `tabs_refresh_tooltip` = `Refresh (%1$s)`, `tabs_stop_loading` = `Stop loading (Esc)`, `tabs_refresh_up_to_date` = `This page is always up to date`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/tabs/RefreshButtonTest.kt`

**Interfaces:**
- Consumes: `TabRefreshRegistry`, `TabsController.refresh/stopRefresh` (Task 1); `TabMotionState` for reduced motion.
- Produces: `@Composable internal fun RefreshButton(registry: TabRefreshRegistry, reduceMotion: Boolean, onRefresh: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier)`. 30dp `IconButton` (18dp icon) inside a `TooltipBox` built like `ShortcutsTitleBarButton` (`DesktopUi.kt:628`). Content description: `Refresh` / `Stop loading`.

Toolbar layout: Back, Forward, then a `Surface(shape = CircleShape, border = outlineVariant, color = surface, modifier = Modifier.weight(1f).height(32.dp))` holding the existing path `Text` (weight 1f) and `RefreshButton`, then the existing right-hand icons. Replace `HorizontalDivider()` with a 2dp `LinearProgressIndicator` while `tab.refresh.refreshing`, keeping the same height so nothing shifts. `TabChip`: a 10dp `CircularProgressIndicator(strokeWidth = 2.dp)` before the label while `tab.refresh.refreshing`.

- [ ] **Step 1: Write the failing tests** (`runComposeUiTest`):
  - `disabled with up-to-date tooltip when nothing registered`: node `Refresh` is not enabled.
  - `click refreshes`: registered page; click `Refresh`; `onRefresh` called once.
  - `loading with stop shows Stop`: `refreshing = { true }`, `stop` set: node `Stop loading` exists; click calls `onStop`.
  - `loading without stop stays Refresh and disabled`: `refreshing = { true }`, `stop = null`: `Refresh` not enabled, no `Stop loading` node.

- [ ] **Step 2: Run, expect FAIL**

Run: `./gradlew :kmpuiviews:jvmTest --tests "*RefreshButtonTest*"`

- [ ] **Step 3: Implement.** Kick turn: `Animatable(0f)`; on click `snapTo(0f)` then `animateTo(360f, tween(450, easing = EmphasizedDecelerateEasing))` on `Modifier.graphicsLayer { rotationZ = ... }`; skipped when `reduceMotion`. Morph: `AnimatedContent(targetState = registry.canStop)` with `(scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut())`, plus `rotate(±90f)` on enter; `fadeIn() togetherWith fadeOut()` when `reduceMotion`.

- [ ] **Step 4: Run, expect PASS**; `./gradlew :kmpuiviews:jvmTest --tests "*Tab*"` green.

- [ ] **Step 5: Commit** `feat(desktop-tabs): refresh button in the toolbar path pill`

---

### Task 4: Shortcuts ⌘R / Ctrl+R / F5 / Esc

**Files:**
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabShortcuts.kt` (sealed interface `:15-32`, `tabShortcut()` `:61-127`, `tabShortcutCaps` `:136-173`, `perform` `:176-197`)
- Modify: callers of `tabShortcut()` (`DesktopTabWindows.kt:84-90`, `BrowserWindow.kt:105-108`) to pass `refreshing`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/tabs/TabShortcutsTest.kt`

**Interfaces:**
- Consumes: `TabsController.refresh/stopRefresh`, `TabRefreshRegistry.canStop` (Task 1).
- Produces: `TabShortcut.Refresh`, `TabShortcut.StopRefresh`; `tabShortcut(...)` gains `canStopRefresh: Boolean = false` (the active tab's `refresh.canStop`).
- Mapping: ⌘R (mac) / Ctrl+R (others) and bare F5 (all OS) → `Refresh`; bare Esc → `StopRefresh` only when `canStopRefresh`, else null. Update the KDoc: "Every shortcut needs a modifier, except F5 and Esc-while-loading."
- Caps rows: `"Refresh page" to listOf(mod, "R")`, `"Refresh page" to listOf("F5")`, `"Stop loading" to listOf("Esc")`.

- [ ] **Step 1: Write the failing tests:**
  - `cmd R refreshes on mac`, `ctrl R refreshes elsewhere`, `F5 refreshes on every OS`.
  - `ctrl R on mac is not refresh` (mac uses ⌘).
  - `Esc stops only while stoppable`: `canStopRefresh = true` → `StopRefresh`; `false` → null (Review Focus 4).
  - `Esc with a text field focused and nothing loading is not consumed` → null.
  - The existing caps/mapping sync test must pass with the new rows.

- [ ] **Step 2: Run, expect FAIL**

Run: `./gradlew :kmpuiviews:jvmTest --tests "*TabShortcutsTest*"`

- [ ] **Step 3: Implement**; `perform` maps `Refresh -> refresh(active)`, `StopRefresh -> stopRefresh(active)`.

- [ ] **Step 4: Run, expect PASS**

- [ ] **Step 5: Commit** `feat(desktop-tabs): refresh and stop shortcuts`

---

### Task 5: Register the other pages

**Files (one line each, inside the screen composable that already holds the ViewModel):**
- `kmpuiviews/src/commonMain/.../presentation/recent/RecentScreen.kt`: `RegisterPageRefresh(vm.isRefreshing, vm::reset)`
- `.../all/AllScreen.kt`: `RegisterPageRefresh(isRefreshing, { onReset(source) })` where `source` is the currently selected source (skip registering when it is null)
- `.../globalsearch/GlobalSearchScreen.kt`: `RegisterPageRefresh(vm.isRefreshing, vm::searchForItems)`; register only while the search text is not blank
- `.../settings/prerelease/PrereleaseScreen.kt`: its `reload()` with the screen's existing loading flag
- `.../settings/extensions/ExtensionListScreen.kt`: `refreshExtensions()` with its loading flag, if it has one; otherwise `refreshing = false`
- `kmpuiviews/src/jvmMain/.../translation/ui/DesktopTranslationViewModel.kt`: add public `fun refresh()` launching `refreshOllama()`; register it in the desktop translation screen with its existing loading flag

Not registered (disabled, "always up to date"): Favorites, History, Lists, Downloads, Notifications, other settings, AI settings, Daily overview settings, readers, players.

**Interfaces:**
- Consumes: `RegisterPageRefresh` (Task 1).

- [ ] **Step 1: Add the registrations.**
- [ ] **Step 2: Verify:** `./gradlew :kmpuiviews:jvmTest` green; `./gradlew :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug` compile.
- [ ] **Step 3: Manual check on desktop** (`./gradlew :mangaworld:desktop:run`): Recent, All, Global search, Details each spin and reload; Favorites shows the disabled button with the tooltip; ⌘R and F5 work; Esc stops a Details load.
- [ ] **Step 4: Commit** `feat(desktop-tabs): refresh on recent, all, search and settings pages`

---

### Task 6: Mobile Details menu Refresh + field highlight

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsUtils.kt` (`DetailActions` `:157-309`)
- Modify: `.../presentation/details/DetailsPortrait.kt:209`, `DetailsLandscape.kt:190` (pass the new params; progress line under the top bar)
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/HighlightOnChange.kt`
- Modify: the composables that draw the chapter count, description and genre row (in `DetailsPortrait.kt` / `DetailsLandscape.kt` / shared header)
- Modify: `strings.xml`: `details_refresh` = `Refresh`, `details_refreshing` = `Refreshing…`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailRefreshMenuTest.kt`

**Interfaces:**
- Consumes: `DetailsViewModel.isRefreshing`, `refresh()` (Task 2); `LocalPageRefreshHost` (Task 1).
- Produces:
  - `DetailActions(..., isRefreshing: Boolean, onRefresh: () -> Unit)`: when `LocalPageRefreshHost.current == null`, adds the Refresh row + `HorizontalDivider` at the top of the `DropdownMenu`. Tap: `dropDownDismiss()` then `onRefresh()`. While `isRefreshing`: row disabled, text `Refreshing…`, leading `CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)`.
  - `fun Modifier.highlightOnChange(key: Any?): Modifier`: no flash on first composition; when `key` changes, background animates from `primaryContainer` to transparent over 900 ms (`animateColorAsState` / `Animatable`), shape `RoundedCornerShape(4.dp)`. Respects reduced motion by skipping the flash.
  - Progress: `AnimatedVisibility(isRefreshing && LocalPageRefreshHost.current == null, enter = fadeIn(), exit = fadeOut()) { LinearProgressIndicator(Modifier.fillMaxWidth()) }` directly under the Details top app bar in portrait and landscape.

- [ ] **Step 1: Write the failing tests** (`runComposeUiTest`, no host provided):
  - `menu shows Refresh first`: open the menu; the first `DropdownMenuItem` text is `Refresh`.
  - `tapping Refresh closes the menu and calls onRefresh`: click `Refresh`; `onRefresh` called once; `Open in Browser` no longer displayed.
  - `refreshing row is disabled`: `isRefreshing = true`; node `Refreshing…` is not enabled; click does not call `onRefresh`.
  - `hidden in the desktop tab shell`: provide `LocalPageRefreshHost provides TabRefreshRegistry()`-like fake; no `Refresh` node in the menu.
  - `highlightOnChange does not flash on first composition and flashes on change`: with `mainClock.autoAdvance = false`, assert the node's background pixel (`captureToImage()`) equals the surface color at first; change `key`; advance 50 ms; pixel differs; advance 1000 ms; pixel equals surface again.

- [ ] **Step 2: Run, expect FAIL**

Run: `./gradlew :kmpuiviews:jvmTest --tests "*DetailRefreshMenuTest*"`

- [ ] **Step 3: Implement** per Interfaces; pass `details.isRefreshing` and `details::refresh` at both `DetailActions` call sites; apply `highlightOnChange(chapters.size)`, `highlightOnChange(description)`, `highlightOnChange(genres)` to those three elements.

- [ ] **Step 4: Run, expect PASS**; `./gradlew :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug` compile.

- [ ] **Step 5: Device check** (Android, `noFirebaseDebug`, package `.noFirebase.debug`): open any Details, ⋮ → Refresh; menu closes, line runs, chapter count flashes if it changed; airplane mode → Refresh shows the `Couldn't refresh` banner and keeps the page.

- [ ] **Step 6: Commit** `feat(details): refresh in the mobile details menu`
