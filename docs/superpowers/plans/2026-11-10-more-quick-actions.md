# More Quick Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Global search, Lists and Saved notifications to quick actions in two labelled groups, plus desktop-only filtering, `⇧⌘F`/`⌘/` shortcuts, and a keyboard-shortcuts list that AnimeWorld extends with its player keys.

**Architecture:** `QuickAction` (common) gains a group and three entries; every surface (card, `⌘K`, Actions menu, title-bar dropdown) keeps reading `QuickAction.entries`. Desktop-only pieces live in `kmpuiviews/jvmMain` behind an `expect` flag for the common card. The shortcuts list gets app sections from Koin (`getAll<ShortcutSection>()`); only AnimeWorld's desktop module registers one.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (Material 3), Koin 4, Navigation3, kotlinx-coroutines-test + Turbine.

**Spec:** `docs/superpowers/specs/2026-11-10-more-quick-actions-design.md` (mockup: https://claude.ai/artifact/JNTS8TZwpQ4FkNgon1tshU v3)

## Global Constraints

- Group order: **Tools** (Check for updates, Scan QR code) then **Go to** (Global search, Lists, Saved notifications, Background tasks, Update check errors).
- Filter field, shortcuts button, shortcuts list, `⇧⌘F`, `⌘/`: desktop only. Android shows none of them.
- Shortcuts: `⌘K`/`Ctrl+K` quick actions, `⇧⌘F`/`Ctrl+Shift+F` Global search, `⌘/`/`Ctrl+/` shortcuts list. macOS wires them as menu-bar `KeyShortcut`s; Windows/Linux in `DecoratedWindow.onPreviewKeyEvent`.
- Key labels: macOS `⌘K`, `⇧⌘F`, `⌘/`, `↵`; Windows/Linux `Ctrl+K`, `Ctrl+Shift+F`, `Ctrl+/`, `Enter`.
- MangaWorld and NovelWorld get no code changes; only AnimeWorld registers a shortcut section.
- The player's `?` sheet must look exactly as today (same key text, "This sheet" label).
- One string locale exists (`kmpuiviews/src/commonMain/composeResources/values/strings.xml`); add every new string there.
- Every lazy item uses `Modifier.animateItem()` (project rule) if any list here becomes lazy.
- Device checks use plain `adb` after `cdm acquire --android`; release with `cdm release`.

## Review Focus

1. **⇧⌘F while the AnimeWorld player has focus** — must open Global search, not toggle fullscreen (`F`). Task 4 adds a matcher test that Shift+Meta+F is consumed by the window handler; Task 6 checks it by hand in the player.
2. **Filter text with surrounding spaces or odd case** ("  QR ") — should still match. Task 3 test `filterTrimsAndIgnoresCase`.
3. **↓ past the last match / filter shrinking under the selection** — selection must clamp, never point at a missing row. Task 3 tests `selectionClampsWhenListShrinks`, `moveSelectionStopsAtEnds`.
4. **Zero lists / zero saved notifications** — show the fallback line, never "0 lists". Task 1 test `zeroCountsAreNull`.
5. **Shortcuts list opened while quick actions is open (footer link)** — card must close so only one overlay shows; Esc closes the list. Task 4 test `openShortcutsClosesQuickActions`.

---

### Task 1: Action model, groups, navigation and counts

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickAction.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionCounts.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsViewModel.kt` (`QuickActionsUiState`, constructor, `uiState`, `run`)
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.kt` (register counts)
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsDesktop.kt` (`label()` must stay exhaustive)
- Test: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsViewModelTest.kt`, `QuickActionsStateTest.kt`

**Interfaces:**
- Produces:
  - `enum class QuickActionGroup { Tools, GoTo }`
  - `enum class QuickAction(val group: QuickActionGroup) { CheckForUpdates(Tools), ScanQrCode(Tools), GlobalSearch(GoTo), Lists(GoTo), SavedNotifications(GoTo), BackgroundTasks(GoTo), UpdateErrors(GoTo) }` — declaration order is display order.
  - `interface QuickActionCounts { val lists: Flow<Int>; val savedNotifications: Flow<Int> }` and `class DaoQuickActionCounts(itemDao: ItemDao, listDao: ListDao) : QuickActionCounts` using `listDao.getAllListsCount()` / `itemDao.getAllNotificationCount()`; registered `singleOf(::DaoQuickActionCounts) { bind<QuickActionCounts>() }`.
  - `QuickActionsUiState` gains `listCount: Int? = null`, `savedNotificationCount: Int? = null` (null when 0, like `scheduledCount`).
  - `QuickActionsViewModel(controller, workRepository, reportStore, backgroundWorkHandler, navigationActions, counts: QuickActionCounts)`.
  - Strings: `qa_group_tools` "Tools", `qa_group_go_to` "Go to", `qa_global_search` "Global search", `qa_global_search_sub` "Search every installed source", `qa_lists` "Lists", `qa_lists_count` "%1$d lists", `qa_lists_sub` "Your custom lists", `qa_saved_notifications` "Saved notifications", `qa_saved_count` "%1$d saved", `qa_saved_notifications_sub` "Saved update notifications".

- [ ] **Step 1: Write the failing tests** (add a `FakeCounts(lists = MutableStateFlow(0), saved = MutableStateFlow(0))` and pass it in `vm()`)

```kotlin
@Test fun globalSearchNavigates() = runTest { vm().run(QuickAction.GlobalSearch); assertEquals(listOf("globalSearch"), nav.calls) }
@Test fun listsNavigates() = runTest { vm().run(QuickAction.Lists); assertEquals(listOf("customList"), nav.calls) }
@Test fun savedNotificationsNavigates() = runTest { vm().run(QuickAction.SavedNotifications); assertEquals(listOf("notifications"), nav.calls) }
@Test fun countsFlowIntoState() = runTest { counts.lists.value = 4; counts.saved.value = 12; /* collect uiState */ assertEquals(4, state.listCount); assertEquals(12, state.savedNotificationCount) }
@Test fun zeroCountsAreNull() = runTest { /* counts 0 */ assertNull(state.listCount); assertNull(state.savedNotificationCount) }
```
In `QuickActionsStateTest`: `@Test fun groupsAndOrder()` asserting `QuickAction.entries.filter { it.group == Tools } == listOf(CheckForUpdates, ScanQrCode)` and GoTo `== listOf(GlobalSearch, Lists, SavedNotifications, BackgroundTasks, UpdateErrors)`.
`RecordingNav` must record `"globalSearch"`, `"customList"`, `"notifications"` from those overrides.

- [ ] **Step 2: Run to verify failure**
Run: `./gradlew :kmpuiviews:jvmTest --tests "*quickactions*"`
Expected: compile FAIL (`GlobalSearch` unresolved).

- [ ] **Step 3: Implement** the interfaces above; add `run` branches `GlobalSearch -> navigationActions.globalSearch()`, `Lists -> navigationActions.customList()`, `SavedNotifications -> navigationActions.notifications()`. Feed counts into the existing `combine` (wrap each with `.onStart { emit(0) }`). Add the strings. Extend `label()` in `QuickActionsDesktop.kt`.

- [ ] **Step 4: Run tests**
Run: `./gradlew :kmpuiviews:jvmTest --tests "*quickactions*"`
Expected: PASS (all old tests too).

- [ ] **Step 5: Commit** — `feat(quick-actions): add global search, lists and saved notifications actions`

---

### Task 2: Grouped card UI

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsList.kt`

**Interfaces:**
- Consumes: Task 1 `QuickAction`, `QuickActionGroup`, `QuickActionsUiState`, strings.
- Produces: `@Composable fun QuickActionsList(state, onAction, modifier, firstRowFocusRequester, actions: List<QuickAction> = QuickAction.entries, showGroupLabels: Boolean = true, selected: QuickAction? = null)`; internal `@Composable fun QuickAction.rowSpec(state): RowSpec` where `data class RowSpec(icon: ImageVector, container: Color, content: Color, title: String, supporting: String, run: Boolean)`. Task 3 uses `actions`, `showGroupLabels`, `selected`, and `rowSpec` (for filter text).

- [ ] **Step 1: Rewrite the list data-driven.** Iterate `actions` grouped by `group` (keep enum order). Per group: optional label (`labelMedium`-ish, uppercase, `onSurfaceVariant`, 10dp above the second group) then a `Column(spacedBy(3.dp))` with First/Middle/Last shapes (single row → all 18dp corners). Rows for the three new actions: `Icons.Default.Search` / `Icons.AutoMirrored.Filled.List` / `Icons.Default.Notifications`; containers: Global search `primaryFixed`-family or a blue `Color` from the scheme the builder picks, Lists violet-ish, Saved notifications `secondaryContainer`-ish (builder's call, keep distinct). Supporting: `listCount?.let { qa_lists_count } ?: qa_lists_sub`, same pattern for saved. Header count stays `QuickAction.entries.size`. `selected` row gets a 2dp `primary` inside border. Keep `firstRowFocusRequester` on the first row.
- [ ] **Step 2: Build** — `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug` → BUILD SUCCESSFUL.
- [ ] **Step 3: Device check** — `cdm acquire --android --category phone`, `adb install -r` the MangaWorld APK, open Recent → bolt. Expect the Tools/Go to layout of the mockup; tap Global search, Lists, Saved notifications and confirm each opens its screen. `adb exec-out screencap -p > /tmp/qa.png` for the record. `cdm release --android`.
- [ ] **Step 4: Commit** — `feat(quick-actions): group the card into Tools and Go to`

---

### Task 3: Desktop filter in the card

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsFilter.kt`
- Create actuals: `kmpuiviews/src/{androidMain,jvmMain,iosMain}/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsFilter.<platform>.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsOverlay.kt` (`QuickActionsCard`)
- Test: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsFilterTest.kt`

**Interfaces:**
- Consumes: Task 2 `QuickActionsList(actions, showGroupLabels, selected)`, `rowSpec`.
- Produces:
  - `internal expect val quickActionsDesktopExtras: Boolean` (jvm `true`, android/ios `false`) — also used by Task 4 for the footer.
  - `fun filterQuickActions(query: String, texts: Map<QuickAction, String>): List<QuickAction>` — `texts` = title + " " + supporting; trims query, case-insensitive `contains`, keeps enum order; blank query → all.
  - `fun moveSelection(current: Int, delta: Int, size: Int): Int` — clamps to `0..size-1`, returns 0 when size is 0.
  - Strings: `qa_filter_hint` "Filter actions", `qa_no_match` "No actions match \"%1$s\"", `qa_footer_hint` "↑↓ move · %1$s run · esc close".

- [ ] **Step 1: Write failing tests**
```kotlin
@Test fun blankQueryReturnsAllInOrder()        // filterQuickActions("", texts) == QuickAction.entries
@Test fun filterTrimsAndIgnoresCase()          // "  QR " -> [ScanQrCode]
@Test fun filterMatchesSupportingText()        // "failed" -> [UpdateErrors] (supporting "See what failed in the last check")
@Test fun noMatchIsEmpty()                     // "zzz" -> []
@Test fun moveSelectionStopsAtEnds()           // moveSelection(0,-1,3)==0; moveSelection(2,1,3)==2
@Test fun selectionClampsWhenListShrinks()     // moveSelection(5,0,2)==1; moveSelection(3,0,0)==0
```
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*QuickActionsFilterTest"` → FAIL (unresolved).
- [ ] **Step 3: Implement** functions and actuals. In `QuickActionsCard`, when `quickActionsDesktopExtras`: a rounded search `TextField`/`BasicTextField` (search icon, hint) above the list, `remember`ed text reset on each open (key it on the open origin), focused on open (replaces `firstRowFocusRequester` on desktop). Build `texts` from `rowSpec`. Selected index starts at 0, resets to 0 on text change, clamps via `moveSelection`. `onPreviewKeyEvent` on the field: `↑`/`↓` move, `Enter` → `onAction(filtered[selected])`. Non-blank query → `showGroupLabels = false`; empty result → `qa_no_match` text instead of the list. Android path unchanged.
- [ ] **Step 4: Run** tests → PASS. `./gradlew :kmpuiviews:compileKotlinJvm :kmpuiviews:compileDebugKotlinAndroid` → SUCCESS (iOS actual compiles in CI; run `:kmpuiviews:compileKotlinIosSimulatorArm64` if the toolchain is present).
- [ ] **Step 5: Commit** — `feat(quick-actions): filter field on desktop`

---

### Task 4: Desktop shortcuts, menus and shortcuts-list state

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsController.kt`
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/presentation/shortcuts/AppShortcut.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsDesktop.kt` (menus)
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/DesktopUi.kt` (`OtakuDecoratedWindow`: key handler, title-bar button)
- Modify: `QuickActionsOverlay.kt` (desktop footer)
- Test: `kmpuiviews/src/commonTest/.../quickactions/QuickActionsControllerTest.kt` (create), `kmpuiviews/src/jvmTest/.../shortcuts/AppShortcutTest.kt` (create), keep `QuickActionsShortcutTest` passing.

**Interfaces:**
- Consumes: Task 1 `QuickAction.group`; Task 3 `quickActionsDesktopExtras`.
- Produces:
  - Controller: `val shortcutsOpen: StateFlow<Boolean>`, `fun openShortcuts()` (also calls `close()`), `fun closeShortcuts()`.
  - `enum class AppShortcut(val key: Key, val shift: Boolean) { QuickActions(Key.K, false), GlobalSearch(Key.F, true), ShortcutsList(Key.Slash, false) }`
  - `fun AppShortcut.matches(event: KeyEvent, isMac: Boolean): Boolean` — KeyDown, exact key, `shift` must equal `isShiftPressed`, no Alt, Meta-only on mac / Ctrl-only elsewhere.
  - `fun AppShortcut.keyCaps(isMac: Boolean): List<String>` — mac `["⌘","K"]`, `["⇧","⌘","F"]`, `["⌘","/"]`; else `["Ctrl","K"]`, `["Ctrl","Shift","F"]`, `["Ctrl","/"]`.
  - `fun AppShortcut.label(isMac: Boolean): String` — mac caps joined with "", else with "+".
  - `fun AppShortcut.menuShortcut(): KeyShortcut` — `KeyShortcut(key, meta = true, shift = shift)`.
  - `isQuickActionsShortcut(event, isMac)` stays as a wrapper over `AppShortcut.QuickActions.matches`.
  - Strings: `qa_menu_shortcuts` "Keyboard shortcuts…", `qa_shortcuts` "Shortcuts", `qa_shortcuts_button` "Keyboard shortcuts".

- [ ] **Step 1: Write failing tests**
```kotlin
// QuickActionsControllerTest
@Test fun openShortcutsClosesQuickActions()  // open(Keyboard); openShortcuts() -> open.value == null, shortcutsOpen.value
@Test fun closeShortcuts()                   // false after closeShortcuts()
// AppShortcutTest
@Test fun macShiftCmdF()           // GlobalSearch.matches(F down, meta+shift, mac) == true
@Test fun cmdFWithoutShiftIgnored()// GlobalSearch.matches(F down, meta, mac) == false
@Test fun windowsCtrlShiftF()      // true for ctrl+shift, isMac=false
@Test fun ctrlSlash()              // ShortcutsList.matches(Slash, ctrl, false) == true
@Test fun ctrlShiftSlashIgnored()  // false (that's '?', the player's key)
@Test fun labels()                 // GlobalSearch.label(true)=="⇧⌘F"; label(false)=="Ctrl+Shift+F"; ShortcutsList.label(false)=="Ctrl+/"
```
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*AppShortcutTest" --tests "*QuickActionsControllerTest" --tests "*QuickActionsShortcutTest"` → FAIL.
- [ ] **Step 3: Implement.**
  - `OtakuDecoratedWindow`: when `showQuickActions`, `koinInject<NavigationActions>()`. In `onPreviewKeyEvent` (non-mac branches, before the Esc branch): GlobalSearch → `controller.close(); navigationActions.globalSearch()`; ShortcutsList → `controller.openShortcuts()`; Esc with `shortcutsOpen` → `closeShortcuts()`.
  - macOS `QuickActionsMenuBar`: items in `QuickAction.entries` order, `Separator()` when `group` changes; Global search item gets `shortcut = AppShortcut.GlobalSearch.menuShortcut()`; after the final separator, "Quick actions…" (existing) and "Keyboard shortcuts…" with `AppShortcut.ShortcutsList.menuShortcut()` → `controller.openShortcuts()`. Global search item routes through `viewModel.run(QuickAction.GlobalSearch)` like the others.
  - `QuickActionsTitleBarMenu`: same order and separators; label Global search with its shortcut text (`AppShortcut.GlobalSearch.label(false)`), replace the hard-coded "Ctrl+K" with `AppShortcut.QuickActions.label(false)`, add "Keyboard shortcuts…".
  - Title bar (all desktop platforms, main window only): an `IconButton` with `Icons.Default.Keyboard`, `TooltipBox` "Keyboard shortcuts", → `controller.openShortcuts()`. On Windows/Linux next to the Actions button; on macOS at the same start position.
  - Card footer (desktop only, `quickActionsDesktopExtras`): `qa_footer_hint` (with `↵`/`Enter` by OS — pass the label in from a jvm helper or use an `expect` string) on the left, `TextButton` "Shortcuts" + `AppShortcut.ShortcutsList.label(isMac)` on the right → `controller.openShortcuts()`. Card keycap for Global search row: small `labelSmall` keycap before the chevron, desktop only.
- [ ] **Step 4: Run** the Step 2 tests → PASS. `./gradlew :mangaworld:desktop:run`: `⇧⌘F` opens Global search; the Actions menu shows the three groups; the keyboard button and footer link are visible. They open nothing visible until Task 5 adds the dialog; the controller tests cover the state.
- [ ] **Step 5: Commit** — `feat(desktop): global search and shortcuts-list shortcuts, grouped Actions menu`

---

### Task 5: Shortcuts list dialog with app sections

**Files:**
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/presentation/shortcuts/ShortcutSection.kt`
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/presentation/shortcuts/ShortcutsDialog.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/DesktopUi.kt` (host the dialog in the main window's content `Box`)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/shortcuts/ShortcutSectionTest.kt`

**Interfaces:**
- Consumes: Task 4 `AppShortcut.keyCaps`, controller `shortcutsOpen`/`closeShortcuts`.
- Produces (Task 6 relies on these exact names):
  - `data class ShortcutEntry(val keys: List<String>, val label: String, val separator: String = " / ", val listLabel: String? = null) { val keysText: String get() = keys.joinToString(separator) }`
  - `data class ShortcutSection(val title: String, val entries: List<ShortcutEntry>)`
  - `fun builtInShortcutSections(isMac: Boolean, strings: BuiltInShortcutStrings): List<ShortcutSection>` — "Anywhere" (Quick actions, Global search, Keyboard shortcuts with `keyCaps`), "In Quick actions" (Move selection `["↑","↓"]`, Run selected action `["↵"]` / `["Enter"]`, Close `["Esc"]`). `BuiltInShortcutStrings` is a plain data class of resolved titles/labels so the function stays pure.
  - `@Composable fun ShortcutsDialog(appName: String, sections: List<ShortcutSection>, isMac: Boolean, onDismiss: () -> Unit)`
  - Strings: `shortcuts_title` "Keyboard shortcuts", `shortcuts_subtitle` "Shortcuts for %1$s", `shortcuts_anywhere` "Anywhere", `shortcuts_in_quick_actions` "In Quick actions", `shortcuts_quick_actions` "Quick actions", `shortcuts_global_search` "Global search", `shortcuts_list` "Keyboard shortcuts", `shortcuts_move` "Move selection", `shortcuts_run` "Run selected action", `shortcuts_close_action` "Close", `shortcuts_close` "Close".

- [ ] **Step 1: Write failing tests**
```kotlin
@Test fun builtInSectionsMac()      // titles == ["Anywhere","In Quick actions"]; Global search keys == ["⇧","⌘","F"]; Run keys == ["↵"]
@Test fun builtInSectionsWindows()  // Global search keys == ["Ctrl","Shift","F"]; Run keys == ["Enter"]
@Test fun keysTextJoins()           // ShortcutEntry(listOf("Space","K"),"x").keysText == "Space / K"; separator " + " -> "Shift + C"
```
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*ShortcutSectionTest"` → FAIL.
- [ ] **Step 3: Implement.** Dialog: in-composition (not a `Dialog` window) — scrim `Box` (40% black, click dismisses) + centered `Surface(surfaceContainer, RoundedCornerShape(28.dp))`, width ≤ 440dp, `heightIn(max = …)` + `verticalScroll`. Title, subtitle (`shortcuts_subtitle` with `koinInject<AppConfig>().appName`), section headers (uppercase label), rows: label left, keycaps right (each key a small bordered `Surface` with `labelMedium`), Close `TextButton`. `scaleIn(0.94f)+fadeIn` / `fadeOut` like the `⌘K` card. Row label uses `listLabel ?: label`. Sections = `builtInShortcutSections(...) + getKoin().getAll<ShortcutSection>()`. Host it in `OtakuDecoratedWindow` content `Box` after `content()` when `showQuickActions`.
- [ ] **Step 4: Run** tests → PASS; `./gradlew :mangaworld:desktop:run` → keyboard button, `⌘/`, menu item and card footer all open the list; it shows only Anywhere + In Quick actions; Esc / scrim / Close dismiss.
- [ ] **Step 5: Commit** — `feat(desktop): keyboard shortcuts list`

---

### Task 6: AnimeWorld Video player section

**Files:**
- Modify: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videoplayer/DesktopPlayerOverlays.kt:192-250`
- Modify: `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt` (`moduleBlock`)
- Test: `animeworld/shared/src/jvmTest/kotlin/com/programmersbox/anime/shared/videoplayer/PlayerShortcutsTest.kt`

**Interfaces:**
- Consumes: Task 5 `ShortcutEntry`, `ShortcutSection`.
- Produces: `internal val PlayerShortcuts: List<ShortcutEntry>` (replaces private `Shortcuts`), `fun playerShortcutSection(): ShortcutSection` titled "Video player".

- [ ] **Step 1: Write failing test** — the `?` sheet text must be unchanged:
```kotlin
@Test fun keysTextMatchesTodaysSheet() {
    assertEquals(
        listOf("Space / K","← / →","J / L","S","↑ / ↓ / wheel","M","F / double-click","C","Shift + C","[ / ]","0–9","H / click","?","Esc"),
        PlayerShortcuts.map { it.keysText },
    )
    assertEquals("This sheet", PlayerShortcuts.single { it.keys == listOf("?") }.label)
    assertEquals("Player shortcuts sheet", PlayerShortcuts.single { it.keys == listOf("?") }.listLabel)
}
@Test fun sectionTitle() { assertEquals("Video player", playerShortcutSection().title) }
```
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*PlayerShortcutsTest"` → FAIL.
- [ ] **Step 3: Implement.** Convert each pair to `ShortcutEntry` (keys split as listed above; "Shift + C" uses `separator = " + "`; `?` gets `listLabel = "Player shortcuts sheet"`). `ShortcutSheet` renders `entry.keysText` and `entry.label` exactly as before. In `animeworld/desktop/.../main.kt` `moduleBlock`: `single(named("videoPlayerShortcuts")) { playerShortcutSection() }` (typed `ShortcutSection`).
- [ ] **Step 4: Run** test → PASS. `./gradlew :animeworld:desktop:run`: the list shows the Video player section; the player's `?` sheet looks unchanged; with the player focused, `⇧⌘F` (mac) / `Ctrl+Shift+F` opens Global search and does **not** toggle fullscreen. If it toggles fullscreen on macOS, have the player's `onKey` ignore keys with Meta/Ctrl pressed. `./gradlew :mangaworld:desktop:run` still shows no Video player section.
- [ ] **Step 5: Commit** — `feat(anime): list player keys in the desktop shortcuts list`

---

### Task 7: Full verification and changelog

**Files:**
- Modify: `CHANGELOG.md` (Unreleased, matching the style of recent entries)

- [ ] **Step 1:** `./gradlew :kmpuiviews:jvmTest :animeworld:shared:jvmTest` → all PASS.
- [ ] **Step 2:** `./gradlew :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug` → BUILD SUCCESSFUL.
- [ ] **Step 3:** Android device pass (`cdm acquire --android --category phone`): grouped card, three new rows navigate, counts show (or fallback at 0), no filter/footer. `cdm release --android`.
- [ ] **Step 4:** Desktop pass on macOS (MangaWorld + AnimeWorld): filter, ↑↓/↵, empty state, ⇧⌘F, ⌘/, menu groups, shortcuts list per app. Windows/Linux untested unless available — say so in the PR.
- [ ] **Step 5:** Changelog entry; commit `docs(changelog): more quick actions`.
