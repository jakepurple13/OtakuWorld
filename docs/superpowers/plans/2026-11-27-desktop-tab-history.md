# Desktop Tab History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give every desktop tab a full, browser-like navigation history with a press-and-hold timeline menu on back/forward, allow the same page in many tabs, and add "Go to open tab" to the item right-click menu.

**Architecture:** A pure `TabHistory` (entries + index) is the source of truth per tab. A desktop subclass `TabNavigation3Actions` routes every navigation call (push, pop, top-level switch) into its `TabHistory` and rebuilds the Nav3 back stack from it, so the 29 existing `popBackStack()` call sites need no change. `TabsController` stops deduplicating tabs; a new menu item finds tabs that already show a title.

**Tech Stack:** Kotlin, Compose Multiplatform (desktop/JVM), Navigation3, Koin, kotlinx.serialization, kotlin.test + Compose UI test (jvmTest).

**Spec:** Approved mockup https://claude.ai/artifact/6qs2Yb7ktQyWNt1rmT5f2R (timeline style C) and these decisions:
1. Desktop only. Each tab keeps one linear history. Opening a page from an earlier entry drops forward entries; jumping keeps both sides.
2. Hold back or forward ~450 ms (a ring fills) or right-click it: a timeline of the whole history opens under the button, newest at top, sections Forward / Now / Back, rows show `+n` / `now` / `−n`. Hold-drag-release on a row jumps. Arrow keys, Enter, Esc work. ⌘[ / ⌘] keep working.
3. The same title, Settings, Search, Lists, Notifications and Downloads can be open in many tabs. Every click inside a tab navigates in that tab (toolbar icons too). ⌘-click / "Open in new tab" always opens a new tab.
4. Item right-click menu gains "Go to open tab", shown only when the title is open in another tab. One match: goes there directly, trailing text names the window. Two or more: a submenu lists each tab, this window first. Going to a tab in another window raises that window; the tab flashes.
5. Toolbar icons get no right-click menu.
6. Session restore saves each tab's history and position.

## Global Constraints

- All new code is in `kmpuiviews/src/jvmMain/.../tabs/` except: `Navigation3Actions.kt` (made `open`, Task 2) and `TabActions.kt` + title surfaces (new optional param, Task 6). Android and iOS behavior must not change.
- New user-visible strings go in `kmpuiviews/src/commonMain/composeResources/values/strings.xml` (only locale present).
- Every lazy item gets `Modifier.animateItem()`.
- Hold delay: `HISTORY_HOLD_MS = 450L`.
- History cap: `MAX_TAB_HISTORY = 50` entries; the oldest is dropped past it.
- Run tests with `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.tabs.*"`; full check at the end `./gradlew :kmpuiviews:jvmTest :mangaworld:desktop:compileKotlinJvm :animeworld:desktop:compileKotlinJvm :novelworld:desktop:compileKotlinJvm` (adjust desktop module names if `./gradlew projects` differs).
- Commit messages: conventional `feat(desktop-tabs): …` / `fix(desktop-tabs): …`, ending with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **Same page twice in one tab** (Recent → Details A → Settings → Recent). Nav3 decorators key saved state by entry; a duplicate key in the back stack can crash or share state. `TabHistory.backStack()` keeps only the last occurrence of each key. Test in Task 1 (`backStackKeepsLastOccurrence`) and Task 2 (`navigatingBackToEarlierPageDoesNotDuplicateStackKeys`).
2. **Session file with an unrestorable entry in the middle** (a reader page, a key from a removed source). Expect the other entries kept and the index moved to the nearest kept entry at or before it, not the whole tail cut. Test in Task 4 (`restoreSkipsUnrestorableEntriesAndRemapsIndex`).
3. **Old session file without `index`** must restore with the last entry current. Test in Task 4 (`oldSessionWithoutIndexOpensOnLastEntry`).
4. **Home tab rail switching** (Recent → Favorites → Recent) must stay in history and highlight the right rail item. Test in Task 2 (`homeRailSwitchesAreHistoryAndHighlightLatestRailPage`).
5. **Onboarding finish** must not leave Onboarding in Home's back history. Test in Task 2 (`onboardingToRecentResetsHistory`).

---

### Task 1: `TabHistory` model

**Files:**
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabHistory.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/tabs/TabHistoryTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  const val MAX_TAB_HISTORY = 50
  class TabHistory(start: NavKey) {
      val entries: List<NavKey>          // snapshot-state backed
      val index: Int                     // snapshot-state backed
      val current: NavKey
      val canGoBack: Boolean
      val canGoForward: Boolean
      fun push(key: NavKey)              // no-op when key == current
      fun back(): Boolean
      fun forward(): Boolean
      fun jumpTo(target: Int): Boolean   // false for out of range or == index
      fun popTo(key: NavKey, inclusive: Boolean): Boolean
      fun remove(key: NavKey)            // never empties the history
      fun reset(key: NavKey)
      fun restore(entries: List<NavKey>, index: Int)  // require(entries.isNotEmpty()); index coerced
      fun backStack(): List<NavKey>      // entries[0..index], each key only at its last occurrence
  }
  ```

- [ ] **Step 1: Write the failing tests** in `TabHistoryTest` (use `Screen.RecentScreen`, `Screen.Settings`, `Screen.FavoriteScreen`, `Screen.HistoryScreen` as keys):
  - `pushAppendsAndMovesIndex`: push A, B → entries `[R, A, B]`, index 2.
  - `pushSameAsCurrentIsNoOp`: push R on fresh → entries `[R]`.
  - `pushFromEarlierEntryDropsForward`: `[R, A, B]`, back, push C → `[R, A, C]`, index 2, `canGoForward` false.
  - `backAndForwardStepOnce`: back → index 1, returns true; at 0 back returns false; forward at end returns false.
  - `jumpToKeepsBothSides`: `[R, A, B, C]`, `jumpTo(1)` → index 1, entries unchanged; `jumpTo(1)` again false; `jumpTo(9)` false.
  - `popToMovesToLastOccurrenceAtOrBeforeIndex`: `[R, A, R, B]` index 3, `popTo(R, inclusive = false)` → index 2; `popTo(A, inclusive = true)` → index 0; `popTo(Settings, false)` returns false, index unchanged.
  - `removeDropsAllOccurrencesAndKeepsPosition`: `[R, A, B, A]` index 2, `remove(A)` → `[R, B]`, index 1 (still on B); removing the only remaining key is ignored.
  - `capDropsOldest`: push 60 distinct `Screen.SourceSearchScreen("s", "$i")` → `entries.size == MAX_TAB_HISTORY`, current is the last pushed, `entries.first()` is push #11.
  - `backStackKeepsLastOccurrence`: `[R, A, R, B]` index 3 → `backStack() == [A, R, B]`; at index 1 → `[R, A]`.
  - `restoreCoercesIndex`: `restore([R, A], 7)` → index 1; `restore([R, A], -1)` → index 1.

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*TabHistoryTest"` — Expected: FAIL (unresolved `TabHistory`).

- [ ] **Step 3: Implement `TabHistory`** with a `mutableStateListOf<NavKey>` and `mutableIntStateOf`. `restore` with `index < 0` means "last entry" (used by Task 4 for old files). Cap trimming removes from the front and shifts `index`.

- [ ] **Step 4: Run** the same command — Expected: PASS.

- [ ] **Step 5: Commit** `feat(desktop-tabs): TabHistory model for per-tab back/forward history`

---

### Task 2: `TabNavigation3Actions` routes navigation into history

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/Navigation3Actions.kt` — `open class Navigation3Actions`; `push` becomes `protected open fun push(key: NavKey)`; mark `open`: `recent`, `settings`, `popBackStack()`, `popBackStack(route, inclusive)`, `homeScreenNavigate`, `clearBackStack`, `remove`, `onboardingToRecent`, `toOnboarding`, `currentDestination`. Add to `TopLevelBackStack`: `fun replaceStack(keys: List<T>)` (one stack under `startKey` holding `keys`, `topLevelKey = startKey`, then `updateBackStack()`).
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabNavigation3Actions.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabPrefs.kt` (`desktopTabsModule`): `single<Navigation3Actions> { TabNavigation3Actions(Screen.RecentScreen) } binds arrayOf(NavigationActions::class, Navigator::class)` — overrides `navigationModule`'s binding because `desktopTabsModule` is included after `kmpModule` (`DesktopUi.kt:346`, `:592`).
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/tabs/TabNavigation3ActionsTest.kt`

**Interfaces:**
- Consumes: `TabHistory` (Task 1).
- Produces:
  ```kotlin
  class TabNavigation3Actions(start: NavKey) : Navigation3Actions(start) {
      val history: TabHistory
      fun forward()
      fun jumpTo(index: Int)
      fun restoreHistory(entries: List<NavKey>, index: Int)
  }
  ```
  Mapping (each mutates `history` then calls a private `sync()` = `navBackStack.replaceStack(history.backStack())`):
  - `push(key)`: if `interceptor?.intercept(this, key) == true` return; else `history.push(key)`.
  - `recent()`, `settings()`, `homeScreenNavigate(nav)`, `toOnboarding()`, `clearBackStack(nav)`: `push(key)` (goes through the interceptor, so ⌘-click arms still work). `homeScreenNavigate` also adds the key to a private `railKeys` set.
  - `onboardingToRecent()`: `history.reset(Screen.RecentScreen)`.
  - `popBackStack()`: `history.back()`. `popBackStack(route, inclusive)`: `history.popTo`. `remove(route)`: `history.remove`.
  - `currentDestination(screen)`: `screen == history.entries.take(history.index + 1).lastOrNull { it in railKeys }` (rail highlight; read state so it recomposes).

- [ ] **Step 1: Write the failing tests** in `TabNavigation3ActionsTest`:
  - `pushRecordsAndRebuildsBackStack`: `navigate(HistoryScreen)` → `history.entries == [R, HistoryScreen]`, `backStack == [R, HistoryScreen]`.
  - `popBackStackStepsBackAndKeepsForward`: then `popBackStack()` → `backStack == [R]`, `history.canGoForward`; `forward()` → `backStack == [R, HistoryScreen]`.
  - `settingsAndRecentNavigateInPlace`: `settings()` then `recent()` → `history.entries == [R, Settings, R]`; `popBackStack()` → current `Settings`.
  - `navigatingBackToEarlierPageDoesNotDuplicateStackKeys`: after the above, `backStack.toList() == [Settings, R]` (no duplicate `R`).
  - `interceptedPushRecordsNothing`: set `interceptor = NavInterceptor { _, _ -> true }`, `navigate(FavoriteScreen)` → entries `[R]`.
  - `popToRouteJumps`: `[R, A, B]`, `popBackStack(A, inclusive = false)` → current A.
  - `homeRailSwitchesAreHistoryAndHighlightLatestRailPage`: `homeScreenNavigate(R)`, `homeScreenNavigate(FavoriteScreen)`, `navigate(title("X"))` → entries `[R, Favorite, X]`; `currentDestination(FavoriteScreen)` true, `currentDestination(R)` false (call inside `runComposeUiTest { setContent { … } }` or read through a `@Composable` test helper; whichever the existing tests use).
  - `onboardingToRecentResetsHistory`: `toOnboarding()`, `onboardingToRecent()` → entries `[R]`, `canGoBack` false.
  - `koinHomeNavIsTabNavigation`: `koinApplication { modules(navigationModule, tabNavigationModule) }` → `get<Navigation3Actions>() is TabNavigation3Actions` and `get<NavigationActions>() === get<Navigation3Actions>()`. (`tabNavigationModule` holds only the override line and is `includes(...)`-ed by `desktopTabsModule`, so the test needn't start the controller.)

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*TabNavigation3ActionsTest"` — Expected: FAIL (unresolved class).

- [ ] **Step 3: Implement** the `Navigation3Actions` / `TopLevelBackStack` changes, `TabNavigation3Actions`, and `tabNavigationModule` (included by `desktopTabsModule`).

- [ ] **Step 4: Run** the test, then `./gradlew :kmpuiviews:testDebugUnitTest` (Android side compiles and existing nav tests pass) — Expected: PASS.

- [ ] **Step 5: Commit** `feat(desktop-tabs): route tab navigation through TabHistory`

---

### Task 3: `TabsController` uses history and allows duplicates

**Files:**
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabModels.kt` — `TabState.nav: TabNavigation3Actions`; delete `forward`; `current` = `nav.history.current`; `ClosedTab(val entries: List<NavKey>, val index: Int, internal val handoffs: List<Any?>?)` with `val current get() = entries[index]`; `isUntouchedNewTab` = `current == NewTabRoute && nav.history.entries.size == 1`.
- Modify: `TabsController.kt`:
  - ctor `homeNav: TabNavigation3Actions`; `newTab` builds `TabNavigation3Actions(key)`.
  - `intercept()`: delete the `existing`/`reuse` block (lines ~131-145) and the `isSingleInstanceFamily` branch; delete `fromTab.forward.clear()`. Keep arms and `clickOpensNewTab`.
  - `open()`: delete the `existing` lookup; always creates a tab.
  - `reopenClosed()`: delete the dedupe block; restore with `tab.nav.restoreHistory(entries, index)`.
  - `close()`: record `ClosedTab(nav.history.entries.toList(), nav.history.index, held)`.
  - `back/forward`: `state.nav.popBackStack()` / `state.nav.forward()`. Add `fun jumpTo(tab: TabId, index: Int)`.
  - `restoredTab(entries: List<NavKey>, index: Int)`; `markRestored` gets the entries.
  - Delete `reuse()`.
  - Add `fun tabsShowingTitle(url: String, source: String, except: TabId): List<TabState>` — tabs whose `keys.keyOf(current) == TabKey.Title(url, source)` (url as stored in `Details`, i.e. `UrlEncoderUtil.encode(raw)`; take the raw url and encode inside), excluding `except`, ordered: tabs in `windowOf(except)` first, then other windows in `windows` order.
  - Add `fun goTo(tab: TabId)`: `activate(tab)` (focus = true, which raises a background window) then `emit(TabEvent.Flash(tab))`.
- Modify: `TabKeys.kt` — delete `isSingleInstanceFamily` (now unused) and its test lines in `TabKeysTest.kt:110-113`.
- Modify: `NewTabSections.kt:178` — `entry.stack.last()` → `entry.current`.
- Modify: `TabToolbar.kt:67,72` — `enabled = tab.nav.history.canGoBack` / `canGoForward` (full UI is Task 5).
- Modify: `TabSessionRestore.kt` — only what is needed to compile (`restoredTab(stack, -1)`); real change is Task 4.
- Modify tests: `TabsControllerTest.kt` (replace `forward`-list assertions with `history` ones; tests that assert dedupe/`Flash` on reopen now assert a new tab opens), `AnimeTabKeyProviderTest.kt:31` (`TabNavigation3Actions`), any other test constructing `TabsController(homeNav = Navigation3Actions(...))`.
- Test: `TabsControllerTest.kt`

**Interfaces:**
- Consumes: `TabNavigation3Actions` (Task 2).
- Produces: `TabsController.jumpTo(tab: TabId, index: Int)`, `tabsShowingTitle(url: String, source: String, except: TabId): List<TabState>`, `goTo(tab: TabId)`, `restoredTab(entries: List<NavKey>, index: Int)`, `ClosedTab(entries, index, handoffs)`.

- [ ] **Step 1: Write the failing tests** (new, in `TabsControllerTest`):
  - `navigatingToTitleOpenElsewhereStaysInTab`: open A in tab x; from tab y `nav.navigate(title("A"))` → y's current is A, active tab is y, no `Flash`.
  - `settingsNavigatesInPlace`: from tab with A, `nav.navigate(Screen.Settings)` → same tab's current is Settings, tab count unchanged.
  - `openAlwaysMakesNewTab`: `open(title("A"))` twice → two tabs showing A.
  - `reopenClosedRestoresHistoryAndPosition`: tab `[A, History, Favorite]` at index 1, close, `reopenClosed` → new tab entries equal, index 1.
  - `jumpToMovesIndex`: `jumpTo(t, 0)` → current A, forward available.
  - `tabsShowingTitleOrdersThisWindowFirst`: A in main tab m1, A in torn-out window tab w1, A in main tab m2; `tabsShowingTitle("url-A", "src", except = homeId)` → `[m1, m2, w1]`; `except = m1` → `[m2, w1]`. (`url-A` has no characters that URL-encoding changes, so the test helper's raw url works.)
  - `goToActivatesAndFlashes`: `goTo(w1)` → `focusedWindow` is w1's window, active is w1, events end with `Flash(w1)`.
  - Update `backMovesToForwardAndForwardRestores`, `newNavigationClearsForward`, `backOntoTitleOpenElsewhereStays` to assert on `t.nav.history`.

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*TabsControllerTest"` — Expected: FAIL.

- [ ] **Step 3: Implement** the changes listed under Files.

- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.tabs.*"` and `./gradlew :animeworld:desktop:jvmTest --tests "*AnimeTabKeyProviderTest"` — Expected: PASS.

- [ ] **Step 5: Commit** `feat(desktop-tabs): allow a page in many tabs and keep in-tab navigation in history`

---

### Task 4: Save and restore history in the session

**Files:**
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabSession.kt` — `TabSessionEntry(val pinned: Boolean, val backStack: List<JsonElement>, val index: Int = -1)`. `backStack` keeps its name (it now holds all history entries) so old files parse. No `CURRENT_VERSION` bump.
- Modify: `TabSessionRestore.kt`:
  - `toSession`: for every tab (Home included) encode `nav.history.entries`, skipping keys `encode` returns null for; `index` = position of the last kept entry at or before `history.index` (0 if none before it).
  - `restore`: decode each element, skipping nulls, remapping `index` the same way (`-1` = last). Home: `home.nav.restoreHistory(entries, index)`. Others: `restoredTab(entries, index)`.
  - Delete `decodablePrefix`, `currentTopLevelStack`, `restoreTopLevelStack`.
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/tabs/TabSessionTest.kt`

**Interfaces:**
- Consumes: `TabNavigation3Actions.restoreHistory`, `TabsController.restoredTab(entries, index)` (Tasks 2-3).
- Produces: a private `fun <A, B : Any> keepConvertible(items: List<A>, index: Int, convert: (A) -> B?): Pair<List<B>, Int>` used by both directions.

- [ ] **Step 1: Write the failing tests**:
  - `sessionRoundTripsHistoryAndIndex`: tab `[A, History, Favorite]` at index 1 → save → restore into a fresh controller → same entries, index 1.
  - `restoreSkipsUnrestorableEntriesAndRemapsIndex`: entries `[A, X, B, C]` index 2 where `decode(X) == null` → restored `[A, B, C]`, index 1.
  - `oldSessionWithoutIndexOpensOnLastEntry`: JSON with `"backStack": [A, B]` and no `index` → restored current B.
  - `homeHistoryIsSaved`: Home `[R, Favorite, A]` index 2 round-trips.
  - Update existing tests that relied on prefix-cut behavior (the reader-cut case: `restorable(reader) == false` now drops only the reader entry).

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*TabSessionTest"` — Expected: FAIL.

- [ ] **Step 3: Implement.**

- [ ] **Step 4: Run** the tabs test package — Expected: PASS.

- [ ] **Step 5: Commit** `feat(desktop-tabs): save each tab's full history in the session`

---

### Task 5: Timeline history menu on back/forward

**Files:**
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/tabs/TabHistoryMenu.kt`
- Modify: `TabToolbar.kt` — back/forward become `HistoryButton`s; menu anchored under the pressed button.
- Modify: `kmpuiviews/src/commonMain/composeResources/values/strings.xml` — add `tabs_history` "Tab history", `tabs_history_pages` "%1$d pages", `tabs_history_forward` "Forward", `tabs_history_back` "Back", `tabs_history_now` "now".
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/tabs/TabHistoryMenuTest.kt`

**Interfaces:**
- Consumes: `TabHistory`, `TabsController.back/forward/jumpTo` (Tasks 1, 3), `TabKeys.labelOf`.
- Produces:
  ```kotlin
  const val HISTORY_HOLD_MS = 450L
  enum class HistorySection { Forward, Now, Back }
  data class HistoryRow(val index: Int, val key: NavKey, val delta: Int, val section: HistorySection)
  fun historyRows(entries: List<NavKey>, index: Int): List<HistoryRow>   // newest first
  @Composable fun HistoryButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit, onOpenMenu: () -> Unit, modifier: Modifier = Modifier)
  @Composable fun TabHistoryMenu(tab: TabState, expanded: Boolean, onDismiss: () -> Unit, onJump: (Int) -> Unit, hotIndex: Int?)
  ```

Behavior (from the mockup): `HistoryButton` draws a ring that fills over `HISTORY_HOLD_MS` (`Animatable` 0→1, `LinearEasing`) while pressed; release before full = `onClick`; full = `onOpenMenu`, and while still held the pointer position picks a row (`hotIndex`), release on a row = `onJump`. Right-click = `onOpenMenu` at once. Menu: `DropdownMenu` under the button, header "Tab history" + "N pages", section labels, rows with a timeline rail dot (filled accent for Now, dashed for Forward, Forward rows at 72% alpha), title from `keys.labelOf(key).text`, trailing `+n` / `now` / `−n` in a monospace style; Now row not clickable; opens scrolled so Now is centred; rows use `Modifier.animateItem()` in a `LazyColumn` capped at 340.dp height; enter = `scaleIn(0.9f, TransformOrigin(0f, 0f)) + fadeIn` via the menu's default motion. Arrow keys move, Enter jumps, Esc closes (DropdownMenu default focus handling plus `onKeyEvent`).

- [ ] **Step 1: Write the failing tests**:
  - `historyRowsNewestFirstWithSections`: entries `[R, A, B, C]`, index 1 → indexes `[3, 2, 1, 0]`, sections `[Forward, Forward, Now, Back]`, deltas `[2, 1, 0, -1]`.
  - `holdOpensMenuAndShortClickSteps` (Compose UI test, `runComposeUiTest`, `mainClock.autoAdvance = false`): press `HistoryButton`, advance 200 ms, release → `onClick` once, menu not opened; press, advance 500 ms → `onOpenMenu` called, release → `onClick` not called.
  - `rightClickOpensMenu`: secondary-button press → `onOpenMenu`.
  - `clickingRowJumps`: open `TabHistoryMenu` for `[R, A, B]` index 2, click the row with text of R → `onJump(0)`.

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*TabHistoryMenuTest"` — Expected: FAIL.

- [ ] **Step 3: Implement** `TabHistoryMenu.kt` and wire into `TabToolbar` (`onJump = { controller.jumpTo(tab.id, it) }`).

- [ ] **Step 4: Run** the test, then the tabs test package — Expected: PASS.

- [ ] **Step 5: Commit** `feat(desktop-tabs): press-and-hold timeline history on back and forward`

---

### Task 6: "Go to open tab" in the item right-click menu

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/tabs/TabActions.kt` — `fun Modifier.target(onOpen: () -> Unit, onMore: (() -> Unit)?, item: SharedItemKey?)` and `fun Modifier.tabTarget(onOpen, onMore = null, item: SharedItemKey? = null)`. KDoc: "[item], the title this opens, lets the menu offer tabs already showing it."
- Modify: `DesktopTabActions.kt` — `MenuRequest` carries `item`; `Menu()` computes `controller.tabsShowingTitle(item.url, item.source, except = tabId)` when the menu opens and, when non-empty, adds the first rows + a divider:
  - 1 match: `DropdownMenuItem` "Go to open tab", trailing "This window" or "Window N" → `controller.goTo(match.id)`.
  - 2+ matches: "Go to open tab" with trailing count and chevron; hover or click opens a nested `DropdownMenu` to its right listing each match (`window tag` + `keys.labelOf(current).text`) → `controller.goTo`.
  - Window number = 1-based position in `controller.windows`.
- Modify call sites to pass `item` where the url and source are in scope: `CoverCard.kt:84` (`item = sharedItemKey`), `GlobalSearchScreen.kt:490`, `HistoryScreen.kt:759`, `NotificationScreen.kt:686`, `NewTabSections.kt:153`. Other `tabTarget` sites (chapters, groups, buttons) keep `item = null`.
- Modify `strings.xml`: `tabs_go_to_open_tab` "Go to open tab", `tabs_this_window` "This window", `tabs_window_n` "Window %1$d".
- Test: `DesktopTabActionsTest.kt`

**Interfaces:**
- Consumes: `tabsShowingTitle`, `goTo` (Task 3).

- [ ] **Step 1: Write the failing tests** (Compose UI test with a real `TabsController`):
  - `goToHiddenWhenTitleNotOpenElsewhere`: right-click a `Modifier.tabTarget({}, item = SharedItemKey("u", "s"))` box → no "Go to open tab" node.
  - `singleMatchGoesStraightThere`: another tab shows `Details(url = encode("u"), source = "s")` → node "Go to open tab" with "This window"; click → that tab active, `Flash` emitted.
  - `multipleMatchesShowSubmenu`: two other tabs (one in a torn-out window) → click "Go to open tab" → submenu shows "This window" row before "Window 2" row; click "Window 2" row → that window focused.

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*DesktopTabActionsTest"` — Expected: FAIL.

- [ ] **Step 3: Implement.**

- [ ] **Step 4: Run** the tabs test package, then `./gradlew :kmpuiviews:testDebugUnitTest` (commonMain signature change compiles on Android) — Expected: PASS.

- [ ] **Step 5: Commit** `feat(desktop-tabs): Go to open tab in the item right-click menu`

---

### Task 7: Whole-branch check

- [ ] **Step 1:** Run the full check from Global Constraints. Expected: all pass, all three desktop apps compile.
- [ ] **Step 2:** Run `RepoGraphTest` (nav map orphan check): `./gradlew :navgraph:test` (or the module that owns it; `grep -rl RepoGraphTest`). Expected: PASS.
- [ ] **Step 3:** Hand-test list for the user (MangaWorld desktop, `./gradlew :mangaworld:desktop:run`): hold/right-click back and forward; hold-drag-release; ⌘[ ⌘]; Settings from toolbar then back; same title in two tabs; Go to open tab across windows; quit and relaunch restores history position; AnimeWorld mini player with the same episode in two tabs.
- [ ] **Step 4:** Commit any fixes.
