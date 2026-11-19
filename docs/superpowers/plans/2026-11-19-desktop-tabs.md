# Desktop Tabs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the three desktop apps into a browser with tabs. Titles, readers/players, search, lists, downloads,
notifications and settings open in tabs with their own history. Tabs can be torn out into windows. Android is
unchanged.

**Architecture:**
- A desktop-only `TabsController` (kmpuiviews `jvmMain`) owns the windows, the tabs, and one `Navigation3Actions`
  per tab.
- Shared code gains only two null-by-default hooks:
  - a `NavInterceptor` on `Navigation3Actions`;
  - a `LocalTabActions` composition local used by cover cards and chapter rows.
- On desktop, every navigation passes through the controller. It enforces one tab per title, routes the
  single-instance pages, and sends "armed" clicks (⌘/Ctrl/middle/right-click menu) to new tabs.
- Only the front tab of each window is composed. Parked tabs keep their saveable state and `ViewModelStore`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform Desktop, Navigation3, Koin 4, kotlinx.serialization,
DataStore (`DataStoreHandlerObject`), Nucleus decorated windows, vlcj (AnimeWorld player).

**Spec:** `docs/superpowers/specs/2026-11-19-desktop-tabs-design.md` (read it first; it is the source of truth for
behavior). Mockup: https://claude.ai/artifact/Jk77Ve8PEkPRSSKBSD7ue8

## Global Constraints

- Android, iOS and Android TV: no behavior or visual change. Every shared-code change has a default that keeps
  today's behavior, and Android never provides the new hooks.
- New desktop code lives in `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/`. Tests go in
  `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/tabs/`.
- One tab per title across all windows. Only each tab's **current page** counts.
- Single-instance families: Settings (root + every `Screen.Settings.*` + the other settings screens), Search
  (`Screen.GlobalSearchScreen*`), Lists (`Screen.CustomListScreen` / `.Home`), Notifications
  (`Screen.NotificationScreen*`), Downloads (per app).
- Click rules:
  - Plain click navigates the same tab. Pref `desktop_tab_click_opens` = `NewTab` turns a plain click on a title
    into a foreground tab.
  - ⌘ (macOS) / Ctrl (other) + click, or middle-click, opens a background tab.
  - ⌘⇧ / Ctrl⇧ + click opens a foreground tab.
- Motion values (ms, easing):
  - background tab: 520, emphasized decelerate
  - grow into content: 450, emphasized decelerate
  - switch: 120, linear
  - close: 180, emphasized accelerate
  - already-open flash: 300, standard
  - layout change: 450, emphasized decelerate
  - mini player: 380, emphasized decelerate
  - tear-out: 450, emphasized decelerate
  - Lift: 380, scale 0.92 → 1 plus fade
  - Reduced motion: every in-window animation becomes a 150 ms crossfade, and tear-out has no animation.
- Emphasized decelerate = `CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)`, emphasized accelerate =
  `CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)`, standard = `CubicBezierEasing(0.2f, 0f, 0f, 1f)`.
- Prefs (DataStore keys):
  - `desktop_tab_position`: `Top` | `Side`, default `Top`
  - `desktop_tab_click_opens`: `SameTab` | `NewTab`, default `SameTab`
  - `desktop_tabs_restore`: boolean, default `true`
  - `desktop_tabs_mini_player`: boolean, default `true`
- Closing the main window quits the app. A torn-out window has no Home tab, and closing its last tab closes it.
- Keyboard:
  - ⌘/Ctrl+T new tab, ⌘/Ctrl+W close tab (ignored on Home), ⌘⇧/Ctrl⇧+T reopen closed tab.
  - ⌘/Ctrl+1…9 switch to tab N, ⌃Tab / ⌃⇧Tab next/previous tab.
  - ⌘[ / ⌘] (macOS) or Alt+← / Alt+→ back/forward.
  - These must not take reader/player bare keys.
- Build and run with `noFirebase`.
- Commands:
  - Tab tests: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.tabs.*"`
  - Desktop: `./gradlew :mangaworld:desktop:run` (likewise `:animeworld:desktop:run`, `:novelworld:desktop:run`)
  - Android: `./gradlew :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug`
- **Deviation from the spec's "my call":** instead of a 70-method delegating `NavigationActions` proxy, the Koin
  `Navigation3Actions` single *is* the Home tab's navigation. Koin users (QuickActions, onboarding redirect,
  shortcuts) reach the right tab through the interceptor:
  - single-instance pages are routed to their own tab;
  - one-per-title applies;
  - navigation on an inactive tab activates it.

  The spec's "Left to the builder" allows this.

## Review Focus

1. **Five ⌘-clicks in quick succession.** Five background tabs appear in click order, right of the active tab. No
   duplicates, even if the same cover is clicked twice. Test: `rapidBackgroundOpensKeepOrderAndDedupe` (Task 2).
2. **A click that never navigates.** An arm whose click never navigates (for example `chapterOnClick` fails to load)
   expires. The next plain click still navigates the same tab. Test: `armExpiresAfterTenSeconds` (Task 2).
3. **Restore with a missing source or unknown key.** A tab whose key no longer deserializes is skipped. A known key
   whose source is uninstalled opens normally (it shows its own error state), and startup does not crash. A corrupt
   or empty session file starts a fresh session. Tests: `restoreSkipsUnknownKeys` and `restoreCorruptFileStartsFresh`
   (Task 12).
4. **Closing a playing tab while the mini player shows it.** Playback stops and the mini player disappears. Test:
   `closingPlayingTabStopsMiniPlayer` (Task 11).
5. **Back onto a page that is current in another tab.** This is allowed and does not jump tabs. Forward history is
   cleared by any new navigation. Test: `backOntoTitleOpenElsewhereStays` (Task 2).

---

## File map

| File (under `kmpuiviews/src/jvmMain/.../tabs/` unless noted) | Responsibility |
|---|---|
| `kmpuiviews/src/commonMain/.../presentation/navactions/Navigation3Actions.kt` | + `NavInterceptor`, `interceptor` property, `startKey` constructor |
| `kmpuiviews/src/commonMain/.../di/NavigationModule.kt` | Koin binding uses no-arg constructor explicitly |
| `kmpuiviews/src/commonMain/.../presentation/navigation/Nav3View.kt` | provides `LocalNav3Actions` |
| `kmpuiviews/src/commonMain/.../utils/CompositionLocals.kt` | + `LocalNav3Actions`, `LocalTabActions` |
| `kmpuiviews/src/commonMain/.../utils/composables/transitions/ChapterOpenMotion.kt` | read `LocalNav3Actions` |
| `kmpuiviews/src/commonMain/.../presentation/tabs/TabActions.kt` | common `TabActions` interface + `Modifier.tabTarget` |
| `TabKeys.kt` | `TabKey`, `TabLabel`, `TabKeyProvider`, `CoreTabKeyProvider` |
| `TabModels.kt` | `TabId`, `WindowId`, `TabState`, `BrowserWindowState`, `TabOpenMode`, `TabEvent` |
| `TabsController.kt` | all tab/window logic, interceptor, arming, closed stack |
| `TabPrefs.kt` | the four `DataStoreHandlerObject`s |
| `DesktopAppEffects.kt` | `HomeNav` side effects run once per app |
| `BrowserWindow.kt`, `TabStrip.kt`, `TabToolbar.kt`, `TabContent.kt`, `HomeTab.kt`, `TabShortcuts.kt` | shell UI |
| `DesktopTabActions.kt` | desktop `TabActions`: modifier/middle/right-click handling + menu |
| `TabMotion.kt` | overlay layer, fly-to-tab, grow-into-content, flash |
| `TabsSettings.kt` | Settings → Tabs page |
| `NewTabPage.kt`, `NotesPanel.kt` | new-tab page, Notes side panel |
| `MiniPlayer.kt` | `MiniPlayerSource`, registry, overlay |
| `TabSession.kt` | session save/restore |
| `TearOut.kt` | drag/drop between strips, tear-out overlay, Lift |
| `kmpuiviews/src/jvmMain/.../DesktopUi.kt` | swap `HomeNav` for the shell |
| per-app desktop modules | `TabKeyProvider` + `DownloadsDestination` + serializers |
| `animeworld/shared/src/jvmMain/.../videoplayer/VideoPlayerUi.jvm.kt`, `DesktopPlayerScreen.kt`, `DesktopPlayerSettings.kt` | tab-scoped player holder, `LocalNavActions` |

---

### Task 1: Shared navigation hooks (no behavior change)

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/Navigation3Actions.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/NavigationModule.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/CompositionLocals.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3View.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/composables/transitions/ChapterOpenMotion.kt:102`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/Navigation3ActionsTest.kt`

**Interfaces:**
- Produces:
  - `fun interface NavInterceptor { fun intercept(from: Navigation3Actions, key: NavKey): Boolean }`. Returning
    `true` means the interceptor handled the navigation and nothing is pushed.
  - `class Navigation3Actions(startKey: NavKey = Screen.RecentScreen)` with `var interceptor: NavInterceptor? = null`.
  - Every push through `navBackStack.add(...)` (all `details`, `navigate`, `navigateTo`, the settings helpers and so
    on) first calls `interceptor?.intercept(this, key)`. `addTopLevel` (`recent()`, `homeScreenNavigate`) is not
    intercepted.
  - `val LocalNav3Actions = staticCompositionLocalOf<Navigation3Actions?> { null }`, provided by `Nav3()` around
    `NavDisplay`.

- [ ] **Step 1: Write failing tests** in `Navigation3ActionsTest`:
  - `interceptorTrueBlocksPush`: with an interceptor that returns `true`, `details(...)` leaves `backStack ==
    [Screen.RecentScreen]`, and the interceptor received a `Screen.DetailsScreen.Details`.
  - `interceptorFalseStillPushes`: `backStack.last()` is the details key.
  - `nullInterceptorUnchanged`: the existing tests pass untouched.
  - `topLevelNotIntercepted`: `homeScreenNavigate(Screen.AllScreen)` with an always-`true` interceptor still changes
    the top-level stack.
  - `startKeyIsRoot`: `Navigation3Actions(Screen.Settings).backStack == [Screen.Settings]`.
- [ ] **Step 2: Run them.** `./gradlew :kmpuiviews:jvmTest --tests "*Navigation3ActionsTest*"`. Expected: FAIL to
  compile (no `interceptor`).
- [ ] **Step 3: Implement the hooks.**
  - Route all `navBackStack.add` calls through one private `push(key: NavKey)`.
  - In `NavigationModule`, switch `singleOf(::Navigation3Actions)` to `single { Navigation3Actions() } binds
    arrayOf(NavigationActions::class, Navigator::class)`.
  - In `ChapterOpenMotion`, read `LocalNav3Actions.current ?: koinInject<Navigation3Actions>()`.
- [ ] **Step 4: Run the tests.** Same command. Expected: PASS.
- [ ] **Step 5: Android unchanged.** Run the Android assemble command. Expected: BUILD SUCCESSFUL. Then run
  `./gradlew :kmpuiviews:jvmTest`. Expected: PASS.
- [ ] **Step 6: Commit.** `feat(nav): optional navigation interceptor and LocalNav3Actions`

### Task 2: Tab model and `TabsController` core logic

**Files:**
- Create: `TabModels.kt`, `TabsController.kt`
- Test: `kmpuiviews/src/jvmTest/.../tabs/TabsControllerTest.kt`

**Interfaces:**
- Consumes: `NavInterceptor` and `Navigation3Actions(startKey)` from Task 1, and `TabKeys` from Task 3. Write the
  test against a fake `TabKeys` that maps strings to keys, so Task 2 can land first.
- Produces:
  - Identity types: `@JvmInline value class TabId(val v: Long)` and `@JvmInline value class WindowId(val v: Long)`.
  - `enum class TabOpenMode { Background, Foreground, NewWindow }`
  - `class TabState(id, nav: Navigation3Actions, pinned: Boolean)`:
    - properties: `forward: SnapshotStateList<NavKey>`, `notesOpen: MutableState<Boolean>`,
      `playing: MutableState<Boolean>`, `viewModelStoreOwner: ViewModelStoreOwner`;
    - `val current: NavKey get() = nav.backStack.last()`.
  - `class BrowserWindowState(id: WindowId, isMain: Boolean)` with `tabs: SnapshotStateList<TabState>`,
    `activeId: MutableState<TabId>` and `bounds: MutableState<WindowBounds?>`.
  - `data class WindowBounds(val x: Dp, val y: Dp, val width: Dp, val height: Dp)`
  - `sealed interface TabEvent`, with `Opened(tab, window, mode, origin: Rect?)`, `Flash(tab)`, `Closed(tab, window)`
    and `Moved(tab, from, to)`.
  - `class TabsController(homeNav: Navigation3Actions, keys: TabKeys, prefs: TabPrefsSnapshot, now: () -> Long =
    System::currentTimeMillis) : NavInterceptor`:
    - state: `windows`, `focusedWindow`, `events: SharedFlow<TabEvent>`;
    - `fun open(key: NavKey, mode: TabOpenMode, from: TabId, origin: Rect? = null): TabId`
    - `fun activate(tab: TabId)`, `fun close(tab: TabId)`, `fun reopenClosed(window: WindowId): TabId?`
    - `fun back(tab: TabId)`, `fun forward(tab: TabId)`
    - `fun arm(tab: TabId, mode: TabOpenMode, origin: Rect?)`
    - `fun move(tab: TabId, to: WindowId, index: Int)`, `fun tearOut(tab: TabId, bounds: WindowBounds): WindowId`
    - `fun focus(window: WindowId)`, `fun windowOf(tab: TabId): BrowserWindowState`
  - `data class TabPrefsSnapshot(val clickOpensNewTab: Boolean)` (updated live by Task 4).

**`intercept(from, key)` order (this algorithm is the core of the feature):**
1. If `from`'s tab is armed and the arm is ≤ 10 000 ms old, consume the arm and `open(key, arm.mode, fromTab,
   arm.origin)`. Return `true`.
2. Let `tk = keys.keyOf(key)`.
   - If another tab's (not `from`'s own) `current` maps to the same `tk`, activate that tab and emit `Flash`. For a
     single-instance family whose current page differs from `key`, also push `key` onto that tab's nav, bypassing
     interception. Return `true`.
   - If `tk` is a single-instance family and `from`'s tab is not already in that family, `open(key, Foreground,
     ...)`. Return `true`.
3. If `prefs.clickOpensNewTab` and `tk is TabKey.Title`, `open(key, Foreground, ...)`. Return `true`.
4. If `from`'s tab is not the active tab of the focused window, activate it (the Koin/Home case). Clear `from`'s
   `forward`. Return `false`.

**`open` placement:**
- `open` inserts right of the active tab, after any background tabs opened in a row from it. That run resets when
  another tab is activated or a foreground tab opens.
- `Foreground` activates the new tab. `NewWindow` calls `tearOut` with default bounds.
- Each new tab gets `Navigation3Actions(startKey = key).also { it.interceptor = this }`.

- [ ] **Step 1: Write failing tests** in `TabsControllerTest`, each one asserting exact tab order, active id and
  events:
  - `homeIsPinnedAndCannotClose`
  - `backgroundOpenInsertsRightOfActiveWithoutActivating`
  - `rapidBackgroundOpensKeepOrderAndDedupe`: open A, B, C, B, D in the background from Home. Expected tabs:
    [Home, A, B, C, D], 4 `Opened` events and 1 `Flash`.
  - `foregroundOpenActivates`
  - `plainNavigateToTitleOpenElsewhereSwitchesAndFlashes`: Home navigates to A while tab A exists. Expected: A is
    active, Home's stack is unchanged, and `intercept` returned `true`.
  - `navigatingWithinOwnTabIsNotDeduped`
  - `settingsFamilyPushesOntoExistingSettingsTab`: navigating to `Screen.Settings.Sources` from Home while the
    Settings tab shows `Screen.Settings` → the Settings tab's stack is `[Settings, Settings.Sources]` and it is
    active.
  - `settingsSubpagesInsideSettingsTabPushNormally`
  - `searchReusesSearchTabWithNewQuery`
  - `clickOpensNewTabPrefOpensTitlesInForeground`
  - `armSendsNextNavigationToNewTab`
  - `armExpiresAfterTenSeconds`: with `now` advanced 10 001 ms, the click navigates the same tab.
  - `navigationOnInactiveTabActivatesIt`
  - `closeActivatesRightNeighborElseLeft`
  - `closedStackReopensMostRecentFirst`
  - `backMovesToForwardAndForwardRestores`
  - `backOntoTitleOpenElsewhereStays`: tab X has `[A, B]` while tab Y shows A. `back(X)` leaves X showing A, Y
    still exists, and nothing is activated.
  - `newNavigationClearsForward`
  - `moveToOtherWindowActivatesThere`
  - `tearOutCreatesWindowWithoutHome`
  - `closingLastTabOfTornWindowClosesWindow`
  - `mainWindowCannotLoseHome`
- [ ] **Step 2: Run them.** Tab tests command. Expected: FAIL (types missing).
- [ ] **Step 3: Implement** `TabModels.kt` and `TabsController.kt` to the interfaces and algorithm above. Use
  Compose snapshot state so the UI observes it. Emit events on a `MutableSharedFlow(extraBufferCapacity = 64)`.
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Commit.** `feat(desktop-tabs): tab model and controller`

### Task 3: Tab keys and labels

**Files:**
- Create: `TabKeys.kt`
- Modify (each app's desktop Koin module): `mangaworld/desktop/src/jvmMain/.../main.kt`,
  `animeworld/desktop/src/jvmMain/.../main.kt`, `novelworld/desktop/src/jvmMain/.../main.kt`
- Test: `kmpuiviews/src/jvmTest/.../tabs/TabKeysTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface TabKey` with:
    - `Title(url, source)` and `Chapter(url)`;
    - `Settings`, `Search`, `Lists` and `Notifications` (`data object`s), plus `SingleList(uuid)` and `Downloads`;
    - `val TabKey.isSingleInstanceFamily` (true for Settings, Search, Lists, Notifications and Downloads).
  - `data class TabLabel(val text: String, val imageUrl: String?, val icon: ImageVector?)`
  - `interface TabKeyProvider { fun keyOf(key: NavKey): TabKey?; fun labelOf(key: NavKey): TabLabel? }`
  - `class TabKeys(providers: List<TabKeyProvider>)`: `keyOf` and `labelOf` return the first non-null result, and the
    label falls back to `TabLabel(key::class.simpleName, null, Icons.Default.Tab)`.
  - `object CoreTabKeyProvider` maps:
    - `Screen.DetailsScreen.Details` → `Title(url, source)`, labelled with its title and image;
    - `Screen.Settings` and every nested `Screen.Settings.*`, plus `SettingsScreen`, `GeneralSettings`,
      `OtherSettings`, `MoreInfoSettings`, `MoreSettings`, `NotificationsSettings` and `UpdateSourcesSettings` →
      `Settings`;
    - `GlobalSearchScreen` and `.Home` → `Search`;
    - `CustomListScreen` and `.Home` → `Lists`, and `CustomListItem(uuid)` → `SingleList(uuid)`;
    - `NotificationScreen*` → `Notifications`;
    - `RecentScreen`, `AllScreen`, `FavoriteScreen`, `HistoryScreen` and `ExtensionListScreen*` → `null` (they live
      in Home).
  - App providers, each registered as `single { ... } bind TabKeyProvider::class`:
    - MangaWorld: `MangaReader` → `Chapter(mangaUrl)`, labelled with its title; `DownloadRoute` → `Downloads`.
    - NovelWorld: `NovelReader` → `Chapter(novelUrl)`.
    - AnimeWorld: `VideoScreen` → `Chapter(episodeUrl ?: showPath)`, labelled with `showName`; `VideoViewerRoute` →
      `Downloads`.
  - `data class DownloadsDestination(val key: NavKey?)`. Each app registers it in Koin: MangaWorld `DownloadRoute`,
    AnimeWorld `VideoViewerRoute`, NovelWorld `null`.

- [ ] **Step 1: Write failing tests:**
  - `detailsMapsToTitleByUrlAndSource`
  - `allSettingsScreensMapToSettings`: iterate the listed objects.
  - `searchWithDifferentQueriesSameKey`
  - `listItemsHaveOwnKey`
  - `homeSectionsHaveNoKey`
  - `firstProviderWins`
  - `unknownKeyFallsBackToClassNameLabel`
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement** `TabKeys.kt`, then register the app providers and `DownloadsDestination` in the three
  desktop modules.
- [ ] **Step 4: Run them.** Expected: PASS. Then run `./gradlew :mangaworld:desktop:compileKotlinJvm
  :animeworld:desktop:compileKotlinJvm :novelworld:desktop:compileKotlinJvm`. Expected: success. (If the task names
  differ, use the module's jvm compile task.)
- [ ] **Step 5: Commit.** `feat(desktop-tabs): tab keys, labels and per-app providers`

### Task 4: Prefs and Koin wiring

**Files:**
- Create: `TabPrefs.kt`
- Modify: `kmpuiviews/src/jvmMain/.../DesktopUi.kt` (the Koin module in `InternalDesktopUi`)
- Test: `kmpuiviews/src/jvmTest/.../tabs/TabPrefsTest.kt`

**Interfaces:**
- Produces:
  - `enum class TabPosition { Top, Side }` and `enum class ClickOpens { SameTab, NewTab }`
  - `val tabPositionPref`, `val tabClickOpensPref`, `val tabsRestorePref`, `val tabsMiniPlayerPref`: each a
    `DataStoreHandlerObject` with the key and default from Global Constraints, mapped by enum name.
  - Koin (desktop only):
    - `single { TabKeys(getAll()) }`
    - `single { CoreTabKeyProvider } bind TabKeyProvider::class`
    - `single { TabsController(homeNav = get<Navigation3Actions>(), keys = get(), prefs = ...) }`
    - the controller sets `get<Navigation3Actions>().interceptor = controller` at creation;
    - a collector keeps `TabPrefsSnapshot` in sync with `tabClickOpensPref`.

- [ ] **Step 1: Write a failing test.** `unknownEnumNameFallsBackToDefault`: the mapper returns `Top` / `SameTab` for
  `"garbage"`.
- [ ] **Step 2: Run it.** Expected: FAIL.
- [ ] **Step 3: Implement** the prefs and the Koin wiring.
- [ ] **Step 4: Run it.** Expected: PASS. Then launch `:mangaworld:desktop:run`. The app still shows today's UI,
  because `HomeNav` is not replaced yet, and Recent → Details still navigates.
- [ ] **Step 5: Commit.** `feat(desktop-tabs): prefs and controller wiring`

### Task 5: Tab shell replaces `HomeNav` on desktop (top layout), with full parity

**Files:**
- Create: `DesktopAppEffects.kt`, `BrowserWindow.kt`, `TabStrip.kt`, `TabToolbar.kt`, `TabContent.kt`,
  `HomeTab.kt`, `TabShortcuts.kt`
- Modify: `kmpuiviews/src/jvmMain/.../DesktopUi.kt:316-340` (replace the `HomeNav(...)` call)
- Modify: `animeworld/shared/src/jvmMain/.../videoplayer/DesktopPlayerScreen.kt:531` and `DesktopPlayerSettings.kt`
  (`koinInject<NavigationActions>()` → `LocalNavActions.current`)
- Test: `kmpuiviews/src/jvmTest/.../tabs/TabShortcutsTest.kt`

**Interfaces:**
- Consumes: `TabsController`, `TabKeys`, the prefs and `LocalNav3Actions`.
- Produces:
  - `@Composable fun DesktopAppEffects()`: everything `HomeNav` ran once — `InitialSetup()`,
    `koinInject<SyncManager>().start()`, `genericInfo.DialogSetups()`, `QuickActionsOverlay()`, and the providers for
    theme/AMOLED/blur and `LocalWindowSizeClass`.
  - `@Composable fun BrowserWindow(window: BrowserWindowState, controller: TabsController)`
  - `@Composable fun TabContent(tab: TabState, genericInfo: KmpGenericInfo, windowSize: WindowSizeClass,
    customPreferences: ComposeSettingsDsl)`:
    - `saveableStateHolder.SaveableStateProvider(tab.id)`;
    - `CompositionLocalProvider(LocalViewModelStoreOwner provides tab.viewModelStoreOwner, LocalTabId provides
      tab.id)`;
    - `KmpLocalCompositionSetup(actions = tab.nav)`;
    - `Nav3(..., navigation3Actions = tab.nav)`.
  - `val LocalTabId = staticCompositionLocalOf<TabId?> { null }`
  - `@Composable fun HomeTab(...)`:
    - a fixed side nav with Recent, All, Notifications, Lists, Search, Favorites, Extensions and Settings, using the
      same icons and `navActions.homeScreenNavigate`/`navigate` calls as `HomeNav`'s `Rail`;
    - Notifications, Lists, Search and Settings go through the controller, so they open tabs;
    - then `TabContent` for Home.
  - `fun tabShortcut(event: KeyEvent, isMac: Boolean): TabShortcut?`, where `sealed interface TabShortcut` has
    `NewTab`, `CloseTab`, `ReopenClosed`, `SwitchTo(n)`, `Next`, `Previous`, `Back` and `Forward`.
- Behavior:
  - Only the active tab of a window is composed. The others are parked.
  - The strip lives in the Nucleus `MaterialTitleBar` row.
  - The toolbar has back, forward, a path built from `TabKeys.labelOf` over the tab's back stack, and buttons for
    Search, Lists, Downloads (hidden when `DownloadsDestination.key == null`) and Settings (with the
    `updateCheck()` badge).
  - Middle-click on a tab closes it. The "+" button opens the new-tab page (a placeholder `Screen.RecentScreen` tab
    until Task 9).
  - Each tab's `ViewModelStoreOwner` is cleared on close.

- [ ] **Step 1: Write a failing test.** `TabShortcutsTest` covers every shortcut for macOS and for Windows/Linux, and
  `bareKeysReturnNull` (Space, ←, →, F and C without modifiers return `null`).
- [ ] **Step 2: Run it.** Expected: FAIL.
- [ ] **Step 3: Implement** the shell and swap it into `DesktopUi.kt`. Keep the existing `onPreviewKeyEvent`
  handlers and add the tab shortcuts after them.
- [ ] **Step 4: Run it.** Expected: PASS.
- [ ] **Step 5: Check that a parked tab keeps its state.** Run `:mangaworld:desktop:run`.
  1. Open a title in a tab.
  2. Scroll its chapter list.
  3. Switch to Home and back.

  Expected: same scroll position, and no network reload (watch the log for the details request). If ViewModels are
  re-created, the `rememberViewModelStoreNavEntryDecorator` is clearing stores when the tab's `NavDisplay` leaves
  composition. In that case, give each tab's `Nav3` a stable parent owner (`tab.viewModelStoreOwner`) and keep it
  until `close`.
- [ ] **Step 6: Parity hand check, ticked on macOS for all three desktop apps.**
  - Every Home side-nav item works.
  - The Settings badge shows when an update exists.
  - Onboarding appears on first run.
  - Extensions load.
  - The updater dialog and the just-updated snackbar work.
  - Quick actions (⌘K and the menu bar) work.
  - The shortcuts dialog works.
  - The reader's bare keys work.
  - The AnimeWorld player's keys work.
  - The list-detail layout works on a wide window.
  - App-specific routes work: MangaWorld downloads, AnimeWorld library folders and video export.
- [ ] **Step 7: Android unchanged.** Run the Android assemble command. Expected: success.
- [ ] **Step 8: Commit.** `feat(desktop-tabs): tab shell replaces HomeNav on desktop`

### Task 6: Open in tabs from covers and chapter rows

**Files:**
- Create: `kmpuiviews/src/commonMain/.../presentation/tabs/TabActions.kt` and `DesktopTabActions.kt`
- Modify: `kmpuiviews/src/commonMain/.../presentation/components/CoverCard.kt` (`M3CoverCard` at both overloads and
  `M3CoverCard2`)
- Modify: `kmpuiviews/src/commonMain/.../presentation/details/DetailsScreen.kt` (`ChapterItem`)
- Modify: `BrowserWindow.kt` (provide `LocalTabActions`)
- Test: `kmpuiviews/src/jvmTest/.../tabs/DesktopTabActionsTest.kt`

**Interfaces:**
- Produces (common):
  - `interface TabActions { fun onPress(mode: TabOpenMode?, origin: Rect?); val menuEnabled: Boolean }`
  - `val LocalTabActions = staticCompositionLocalOf<TabActions?> { null }`
  - `@Composable fun Modifier.tabTarget(onOpen: () -> Unit): Modifier`: returns `this` when `LocalTabActions.current
    == null` (Android).
- Produces (desktop): `DesktopTabActions(controller, tabId)`.
- Behavior of `tabTarget` on desktop:
  - **Primary press (`PointerEventPass.Initial`, not consumed) with modifiers:**
    - ⌘ / Ctrl → `arm(Background, bounds)`;
    - ⌘⇧ / Ctrl⇧ → `arm(Foreground, bounds)`.

    The element's normal click then navigates, and the interceptor redirects it.
  - **Middle press:** `arm(Background, bounds)`, then call `onOpen()`.
  - **Right press:** show a cursor dropdown menu. Each entry arms (or doesn't) and then calls `onOpen()`:
    - Open (no arm)
    - Open in new tab (Background)
    - Open in new tab and switch (Foreground)
    - Open in new window (NewWindow)
    - More… (calls the card's existing `onLongPress`, when the card has one).
- `bounds` is the element's bounds in window coordinates, from `onGloballyPositioned`.
- `fun modeFor(isMac: Boolean, meta: Boolean, ctrl: Boolean, shift: Boolean, button: PointerButton): TabOpenMode?`
  is a pure function, so it can be unit-tested.

- [ ] **Step 1: Write failing tests:**
  - `modeFor` truth table: ⌘ on mac → Background; Ctrl on mac → `null`; Ctrl on Windows → Background; ⌘⇧ →
    Foreground; middle → Background; plain → `null`.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement** `tabTarget` and `DesktopTabActions`, and apply `Modifier.tabTarget(onClick)` in
  `M3CoverCard`/`M3CoverCard2` and `ChapterItem` (passing the same lambda each already calls on click).
- [ ] **Step 4: Run them.** Expected: PASS. Run the Android assemble command. Expected: success.
- [ ] **Step 5: Hand check on desktop.** ⌘-click, middle-click, ⌘⇧-click and every right-click entry work on Recent
  covers, search results and chapter rows. An already-open title flashes its tab.
- [ ] **Step 6: Commit.** `feat(desktop-tabs): open covers and chapters in tabs`

### Task 7: Open animations

**Files:**
- Create: `TabMotion.kt`
- Modify: `BrowserWindow.kt`, `TabStrip.kt`

**Interfaces:**
- Consumes: `TabsController.events` (`Opened` with `origin`, and `Flash`).
- Produces:
  - `@Composable fun TabMotionLayer(controller: TabsController, window: BrowserWindowState, tabSlotBounds: (TabId) ->
    Rect?, contentBounds: () -> Rect)`: an overlay `Box` above the content;
  - `fun reducedMotion(): Boolean`: use the launch/quit branch's `ReducedMotion` if it has merged; otherwise read
    macOS `defaults read -g com.apple.universalaccess reduceMotion` / GNOME `enable-animations`, and on Windows
    `SystemParametersInfo` is skipped (false).
- Behavior, with durations from Global Constraints:
  - Background: the strip slot animates its width open, then a cover image (`TabLabel.imageUrl` through Kamel) flies
    from `origin` to the tab icon. A tab with no image just flashes.
  - Foreground: the overlay fills `origin` with the cover, grows it to `contentBounds()` (alpha 1 → 0 from 45 %),
    and the content fades in from 40 %. Details' existing cover handoff lands the cover in its header.
  - Close: `animateItem()` in the strip.
  - Switch: a 120 ms crossfade keyed on tab id.
  - Flash: a 300 ms accent ring on the tab.
  - Reduced motion: every one of these becomes a 150 ms crossfade.

- [ ] **Step 1: Hand check with animations slowed.** Use a debug-only `TAB_MOTION_SCALE` env var (default 1, applied
  to all durations). Check:
  - five quick ⌘-clicks each fly to the right tab in order;
  - ⌘⇧-click grows from the cover;
  - the already-open flash works;
  - reduced motion turned on gives crossfades only.
- [ ] **Step 2: Commit.** `feat(desktop-tabs): tab open, close, switch and flash motion`

### Task 8: Side layout, animated layout change, Tabs settings page

**Files:**
- Create: `TabsSettings.kt`
- Modify: `BrowserWindow.kt`, `TabStrip.kt`, and each desktop app's `PlatformSettings.kt` (add the Tabs entry)

**Interfaces:**
- Consumes: `tabPositionPref`, `tabClickOpensPref`, `tabsRestorePref`, `tabsMiniPlayerPref`.
- Produces: `@Composable fun TabsSettingsPage()`. It holds the four controls with the copy from the spec's Settings
  section, and the mini player row only shows in AnimeWorld (`DownloadsDestination`-style Koin flag
  `MiniPlayerSupported(true)` registered only by AnimeWorld).
- Behavior:
  - Top: the strip is in the title bar.
  - Side: a 200 dp column left of the content, with tabs as full rows and "+ New tab" at the bottom.
  - Changing the pref animates every tab from its old bounds to its new bounds (450 ms) using
    `LookaheadScope` + `Modifier.animateBounds` keyed by tab id. The side column's width animates 0 ↔ 200 dp and the
    content slides with it.

- [ ] **Step 1: Hand check.**
  - Switch Top ↔ Side from Settings → Tabs with 6 tabs open. Each tab glides to its slot, nothing jumps, and the
    content keeps its scroll position.
  - The setting persists across a restart.
  - "Opens a new tab" makes plain cover clicks open foreground tabs.
- [ ] **Step 2: Commit.** `feat(desktop-tabs): side tabs, animated layout switch, Tabs settings`

### Task 9: Single-instance pages, toolbar buttons, new-tab page

**Files:**
- Create: `NewTabPage.kt`
- Modify: `TabToolbar.kt`, `HomeTab.kt`, `BrowserWindow.kt`
- Modify (each desktop app's Koin module): register a `navigation<NewTabRoute> { NewTabPage() }` entry
- Test: extend `TabsControllerTest` with `toolbarDownloadsHiddenWhenNoDestination` (controller exposes
  `downloadsKey: NavKey?`)

**Interfaces:**
- Produces:
  - `@Serializable data object NewTabRoute : NavKey` (in `tabs/`, jvmMain), whose `TabKeys` label is "New tab".
  - `@Composable fun NewTabPage()`: a search field (submitting navigates to `GlobalSearchScreen(query)`), a
    Continue reading row (existing history source, the 4 most recent), and Recently closed (the controller's closed
    stack, 5 entries; clicking one reopens it).
- Toolbar buttons call `navActions.globalSearch()`, `customList()`, `navigate(downloadsKey)` and `settings()`. The
  interceptor turns these into open-or-switch.

- [ ] **Step 1: Write the failing test** above and run it. Expected: FAIL.
- [ ] **Step 2: Implement.**
- [ ] **Step 3: Run it.** Expected: PASS.
- [ ] **Step 4: Hand check.** Each toolbar button opens its tab once and then switches to it. Search from Details
  reuses the Search tab. ⌘T and "+" open the new-tab page. Reopen closed works.
- [ ] **Step 5: Commit.** `feat(desktop-tabs): single-instance tabs and new-tab page`

### Task 10: Notes side panel

**Files:**
- Create: `NotesPanel.kt`
- Modify: `TabContent.kt`, and the Details top bar actions in `kmpuiviews/src/commonMain/.../presentation/details/`
  (add a `LocalNotesPanelToggle` hook, `null` by default, shown as a "Notes" action only when non-null)

**Interfaces:**
- Produces:
  - `val LocalNotesPanelToggle = staticCompositionLocalOf<(() -> Unit)?> { null }` (common, null on Android)
  - `@Composable fun NotesPanel(itemUrl: String)`, which hosts the existing `Screen.NotesScreen.Detail` content. It
    is 280 dp wide and animates its width with the standard easing.
  - `TabState.notesOpen` drives it, so the panel only shows while the tab's current page is Details.

- [ ] **Step 1: Hand check.** Open Notes on a title tab, switch tabs and come back: the panel is still open. Another
  title tab has its own state. Android Details shows no new action (run the Android assemble command and verify by
  eye on the emulator).
- [ ] **Step 2: Commit.** `feat(desktop-tabs): notes side panel`

### Task 11: Mini player (AnimeWorld)

**Files:**
- Create: `MiniPlayer.kt`
- Modify: `animeworld/shared/src/jvmMain/.../videoplayer/VideoPlayerUi.jvm.kt:72-90` (`PlayerHost`)
- Test: `kmpuiviews/src/jvmTest/.../tabs/MiniPlayerTest.kt`

**Interfaces:**
- Produces:
  - `interface MiniPlayerSource { val frame: StateFlow<ImageBitmap?>; val title: String; fun pause(); fun resume();
    fun stop() }`
  - `class MiniPlayerRegistry(controller: TabsController)`:
    - `fun register(tab: TabId, source: MiniPlayerSource)` sets `TabState.playing = true`;
    - `fun unregister(tab: TabId)`;
    - `val showing: StateFlow<TabId?>`;
    - rules: leaving a registered tab shows it in the mini player if `tabsMiniPlayerPref`, otherwise pauses it; a
      second tab taking over pauses the previous one; closing the tab stops it; returning to the tab hides the mini
      player; the mini player follows its tab across `move` and `tearOut`.
  - `@Composable fun MiniPlayerOverlay(registry, window)`: 250 dp wide, bottom-right, 16 dp inset, in the window that
    owns the tab. It draws `frame` and the title, has a × button (calls `stop()` and unregisters), and a click
    activates the tab.
- AnimeWorld change:
  - `PlayerHost` keeps `DesktopPlayer` in a tab-scoped `ViewModel` (`PlayerHolderViewModel`, created through the tab's
    `LocalViewModelStoreOwner`), so parking the tab doesn't close it.
  - It registers a `MiniPlayerSource` adapter with `LocalTabId.current` when non-null.
  - `onCleared` closes the player and the proxy.

- [ ] **Step 1: Write failing tests** with a fake source:
  - `leavingPlayingTabShowsMini`
  - `leavingWithPrefOffPauses`
  - `secondPlayingTabTakesOverAndPausesFirst`
  - `returningHidesMini`
  - `closingPlayingTabStopsMiniPlayer`
  - `miniFollowsTabOnMove`
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement**, with the 380 ms shrink/grow between the player bounds and the corner.
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Hand check (`:animeworld:desktop:run`).** Playback continues without a restart when switching tabs,
  and resumes in the tab at the same position. The speaker icon shows on the tab.
- [ ] **Step 6: Commit.** `feat(desktop-tabs): mini player for playing tabs`

### Task 12: Restore on launch

**Files:**
- Create: `TabSession.kt`
- Modify: `DesktopUi.kt` (load before first composition and save on change), and each app's desktop module
  (register its NavKey serializers)
- Test: `kmpuiviews/src/jvmTest/.../tabs/TabSessionTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class TabSession(val windows: List<WindowSession>)`, where `WindowSession(bounds:
    WindowBounds?, isMain: Boolean, activeIndex: Int, tabs: List<TabSessionEntry>)` and `TabSessionEntry(pinned:
    Boolean, backStack: List<JsonElement>)`. Keys are stored as `JsonElement`, so one bad key can't fail the whole
    file.
  - `class NavKeySerializers(val module: SerializersModule)`. Each app registers its own (MangaReader, DownloadRoute,
    NovelReader, VideoScreen, VideoViewerRoute, ...). kmpuiviews registers `Screen` (sealed) and `NewTabRoute`.
  - `class TabSessionStore(file: File, json: Json)`:
    - `fun save(controller: TabsController)`, debounced 1 s after any tab or stack change and run on quit;
    - `fun load(): TabSession?`, which returns `null` for a missing, corrupt or empty file.
  - The session file is `appDirs.getUserDataDir()/tabs-session.json`.
  - `fun TabsController.restore(session: TabSession, decode: (JsonElement) -> NavKey?)`:
    - restores tabs, dropping any whose stack decodes to nothing and keeping the decodable prefix of a stack;
    - the Home tab restores only its current top-level stack;
    - skipped when `tabsRestorePref` is false.

- [ ] **Step 1: Write failing tests:**
  - `roundTripRestoresWindowsTabsStacksAndActive`
  - `restoreSkipsUnknownKeys`
  - `restoreCorruptFileStartsFresh`
  - `restoreOffStartsWithHomeOnly`
  - `missingBoundsUsesDefaultWindowSize`
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Hand check.**
  - Quit with 2 windows and 6 tabs, then relaunch: the same layout comes back, and only the active tabs load.
  - Uninstall a source, relaunch: its tab shows the page's own error, with no crash.
- [ ] **Step 6: Commit.** `feat(desktop-tabs): restore tabs on launch`

### Task 13: Tear-out windows and moving tabs between windows

**Files:**
- Create: `TearOut.kt`
- Modify: `DesktopUi.kt` (render one `OtakuDecoratedWindow` + `BrowserWindow` per `controller.windows`; only the main
  window's `onCloseRequest` quits), `TabStrip.kt` (drag gestures)
- Test: extend `TabsControllerTest` with `dropOnOwnStripReorders`, `dropIndexNeverBeforeHome` and
  `tearOutFromSingleTabWindowJustMovesWindow`

**Interfaces:**
- Produces:
  - `fun dropTarget(screenPoint: Offset, windows: List<Pair<WindowId, Rect>>, strips: Map<WindowId, Rect>):
    DropTarget`, a pure function. `sealed interface DropTarget` has `Reorder(window, index)`, `MoveTo(window,
    index)` and `TearOut(bounds)`.
    - A drop more than 46 dp below the top strip (or 50 dp right of the side strip) and over no strip means
      `TearOut`.
    - A drop over another window's strip means `MoveTo`.
  - `@Composable fun TearOutOverlay(from: Rect, to: WindowBounds, onDone: () -> Unit)`: a transparent, undecorated,
    non-focusable, always-on-top `Window` spanning the union of `from` and `to`. It animates a rounded rect from tab
    bounds to window bounds (450 ms). It runs only when `overlaySupported()`, which is true on macOS (and Linux if
    the hand test passes) and false on Windows. Otherwise the new window plays Lift (380 ms). Under reduced motion
    there is no animation.
  - `overlaySupported()` and `reducedMotion()` move to the launch/quit branch's shared policy once it is merged.
- Window rules:
  - A torn-out window has no Home tab, and closing its last tab closes it.
  - The main window's close button quits the app (and closes every window).
  - The strip shows a drop indicator while dragging, and a dashed preview rect follows the pointer once a drop would
    tear out.

- [ ] **Step 1: Write failing tests** for `dropTarget` (inside the strip, below it, over another strip, side layout
  thresholds) plus the three controller tests above.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Hand check on macOS.**
  - Tear out a title tab: the window grows from the tab.
  - Drag it back onto the main strip: it merges.
  - Tear out a playing anime tab: the mini player follows.
  - Close a torn window's last tab: the window closes.
  - Close the main window: the app quits.
  - With `overlaySupported()` forced false, Lift plays.
- [ ] **Step 6: Commit.** `feat(desktop-tabs): tear-out windows and moving tabs between windows`

### Task 14: Final verification and PR

- [ ] **Step 1: Run everything.** Run `./gradlew :kmpuiviews:jvmTest` and the Android assemble command. Expected:
  PASS and success.
- [ ] **Step 2: Repeat the Task 5 parity checklist** on all three desktop apps with tabs in both layouts.
- [ ] **Step 3: Android smoke check.** Install `:mangaworld:assembleNoFirebaseDebug` on the emulator and confirm
  Recent → Details → chapter, long-press on a cover, and the nav bar all behave as before.
- [ ] **Step 4: Open the PR to `develop`** with the spec and mockup links and the hand-check results (including
  anything not verified, such as Linux tear-out).
