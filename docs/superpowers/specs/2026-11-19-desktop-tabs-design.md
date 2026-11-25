# Desktop Tabs — Design

Date: 2026-11-19
Status: Draft, awaiting approval
Mockup (approved direction): https://claude.ai/artifact/Jk77Ve8PEkPRSSKBSD7ue8
Earlier, superseded windows mockup: https://claude.ai/artifact/XUDsYV2T67NNXiPh9CUy7P

## Intent

The user wants several items open at once on the desktop apps (MangaWorld, AnimeWorld, NovelWorld desktop). A first
idea, opening screens in separate windows, was mocked up and then replaced by a browser model: the desktop app becomes
a tabbed browser. Titles, chapters, episodes, search, lists, downloads and settings open in tabs. Tabs can be torn out
into their own windows.

The Android apps must not change in behavior or look. The project is Kotlin Multiplatform, so shared code may change
only in small, behavior-neutral ways.

## Goals

- One desktop window holds many tabs. Each tab has its own back/forward history.
- Home (today's `HomeNav` content: Recent / All / Favorites / etc.) is a pinned tab that can't be closed or dragged.
- Open things in tabs:
  - Plain click on a title: navigates the **current** tab (default). A setting can make plain click open a new
    foreground tab instead.
  - ⌘-click (macOS) / Ctrl-click (Windows, Linux) or middle-click: new **background** tab.
  - ⌘⇧-click / Ctrl⇧-click: new tab, switch to it.
  - Right-click: context menu (Open, Open in new tab, Open in new tab and switch, Open in new window, Read/Play latest
    in new tab, Add to favorites, Add to list).
  - The same applies to chapter/episode rows on Details.
- **One tab per title.** Opening a title that is already open in any tab of any window switches to that tab and
  flashes it. No duplicates. The same rule covers single-instance pages: Search, Lists, Downloads, Settings.
- Tab position is a user setting: **across the top** or **down the side**. Changing it animates tabs from the old
  layout to the new one.
- Tabs are **restored on next launch** (all windows, each tab's back stack and current page).
- Tear-out: drag a tab off the strip → it becomes a new window that animates out of the tab. Dropping a tab on another
  window's strip moves it there. A torn-out window has no Home tab; closing its last tab closes it.
- Closing the main window quits the app (all windows).
- New tab (⌘T / Ctrl+T or "+"): a new-tab page with search, Continue reading, and Recently closed.
- Reopen closed tab (⌘⇧T / Ctrl⇧T).
- AnimeWorld: leaving a tab that is playing a video moves the video into an in-window **mini player** in the corner.
  Clicking it returns to that tab. Tabs that are playing show a speaker icon. The mini player can be turned off in
  settings.
- Notes for a title open as a **side panel** inside that title's tab.
- v1 screens in tabs: Details, Reader / Player / Novel reader, Settings, Global search, Notes (side panel), Downloads,
  Custom lists (Lists page and a single list).

## Non-goals

- No change to Android, iOS, or Android TV apps.
- No tab groups, no tab search, no syncing tabs across devices.
- Cover viewer, notifications, extensions catalog, daily overview, and history are **not** given special tab
  treatment in v1. They keep working as pages inside whatever tab navigates to them.
- Onboarding, backup/restore wizard, quick actions (⌘K) and source preferences stay as they are (overlay or dialog over
  the current window).

## Anti-goals

- Any visible change on Android (layout, navigation, long-press behavior, transitions).
- Two tabs showing the same title.
- A background tab that keeps playing audio or video unseen. Leaving a playing tab must either hand off to the mini
  player or pause.
- Tabs that reload from the network when you switch back to them.

## Constraints and current state

- Desktop entry: `kmpuiviews/src/jvmMain/.../DesktopUi.kt`, `InternalDesktopUi` → `OtakuDecoratedWindow` (Nucleus
  `DecoratedWindow` + `MaterialTitleBar`) → `HomeNav(...)`. One window today.
- Navigation: `Navigation3Actions` (`kmpuiviews/commonMain/.../navactions/Navigation3Actions.kt`) is a Koin `single`
  bound to `NavigationActions` and `Navigator`. It owns one `TopLevelBackStack`. `Nav3()` in `Nav3View.kt` takes it
  as a parameter with a `koinInject()` default, and uses `rememberViewModelStoreNavEntryDecorator()` and
  `rememberSaveableStateHolderNavEntryDecorator()`.
- About 70 screens read navigation through `LocalNavActions`, provided by `KmpLocalCompositionSetup(actions = ...)`
  (default `koinInject<Navigation3Actions>()`). These need no change: each tab provides its own.
- Direct Koin injection of the shared navigation object (will act on the wrong tab unless handled):
  - `kmpuiviews/.../utils/composables/transitions/ChapterOpenMotion.kt` reads `koinInject<Navigation3Actions>().backStack`
  - `kmpuiviews/.../navigation/Nav3View.kt` (default parameter)
  - `kmpuiviews/.../utils/CompositionLocals.kt` (default parameter)
  - `kmpuiviews/jvmMain/.../DesktopUi.kt` (onboarding redirect, quick-actions shortcuts)
  - `animeworld/shared/jvmMain/.../DesktopPlayerScreen.kt`, `DesktopPlayerSettings.kt`
  - `QuickActionsViewModel` (constructor-injected `NavigationActions`)
  - `animeworld/src/main/.../SourcePreferencesRoute.kt` is Android-only and out of scope.
  - The two `ReadViewModel`s take `NavigationActions` as a function parameter, so callers already pass one.
- NavKeys (`Screen` and app keys) are `@Serializable`, so back stacks can be saved for restore.
- Shared cover card: `kmpuiviews/commonMain/.../components/CoverCard.kt` (`M3CoverCard`, `M3CoverCard2`). Callers pass
  their own `onClick` that calls `navigationActions.details(model)`.
- Desktop player: `DesktopPlayerScreen(player: DesktopPlayer, ...)` receives the player object from its caller.
- The launch/quit animation work (branch `worktree-desktop-launch-quit-animations`, not merged) has a transparent
  overlay window pattern, `ReducedMotion`, and a per-OS motion policy (motion is always off on Windows). Its
  `QuitOverlayWindow` is pinned to the main window's bounds, so it is not directly reusable for tear-out.
- Configuration cache on, non-transitive R, iOS opt-in. Use `noFirebase` builds.

## Approaches

### Inactive tabs

- **A. Keep every tab composed.** Instant switching, no state handling. With many tabs, every hidden screen keeps
  loading images and collecting flows. Hidden players need manual pausing.
- **B. Only the front tab is composed; others are parked. (Chosen.)** Each tab owns its back stack, a
  `SaveableStateHolder` scope, and a `ViewModelStoreOwner`. Switching tabs re-composes the front tab from saved state.
  ViewModels survive, so data does not reload. Memory stays flat with many tabs, which is how browsers behave. A
  playing video hands off to the mini player.

### Tear-out overlay window

- **A. Build on the launch/quit branch.** Blocked on an unfinished branch whose overlay is the wrong shape.
- **B. Own small overlay. (Chosen.)** Write a tear-out overlay composable with the same transparent-window pattern.
  Reuse `ReducedMotion` and the OS policy if launch/quit has merged by then. Otherwise add a small local check and
  switch to the shared one when it lands. Tear-out is the last part of the plan.

### Routing navigation to the right tab (my call, see "Left to the builder")

Each tab gets its own `Navigation3Actions`, provided through `KmpLocalCompositionSetup(actions = tab.nav)` and passed
to `Nav3(navigation3Actions = tab.nav)`. Direct Koin users get a desktop-only fix:

- The Koin `NavigationActions`/`Navigator` binding on desktop becomes a delegating object that forwards to the
  **active tab of the focused window**. This keeps `QuickActionsViewModel`, the onboarding redirect, and global
  shortcuts working with no shared-code change.
- `ChapterOpenMotion` and the player files are changed to read the tab's navigation from a composition local instead
  of Koin. Android still provides the same single instance, so its behavior is unchanged.

## Design decisions (from the user)

| Topic | Decision |
|---|---|
| Model | Browser-style tabs, replacing the separate-windows idea |
| Duplicates | One tab per title, everywhere; reopening switches + flashes |
| Plain click | Same tab by default; setting to open a new tab |
| Modifier clicks | ⌘/Ctrl-click + middle-click = background tab; ⌘⇧/Ctrl⇧-click = foreground tab |
| Tab position | User setting, top or side, animated switch |
| Restore | Restore all windows and tabs on launch (setting, on by default) |
| Home | Pinned tab |
| Tear-out | In v1 |
| Main window close | Quits app |
| Inactive tabs | Parked (approach B) |
| Tear-out overlay | Own small overlay (approach B); Lift fallback where the overlay can't run |
| Android | No behavior or visual change |

## How it works

### Model (jvmMain, `kmpuiviews`)

- `TabState`: id, its own `Navigation3Actions` (back stack), `pinned`, `notesOpen`, `playing`, a saveable-state key, and
  a `ViewModelStoreOwner` cleared when the tab closes.
- `BrowserWindowState`: ordered tabs, active tab id, window position/size, `isMain`.
- `TabsController` (Koin single, desktop only): all windows, focused window, open/activate/close/move/tear-out,
  recently-closed stack (session only), one-per-title lookup, and save/restore.
- Tab identity for one-per-title: the details key uses the title's URL (plus source). The reader key uses title URL +
  chapter/episode URL. Search, Lists, Downloads and Settings are single-instance keys. The new-tab page and Home have
  no key.
- The tab title and icon come from the current page: title text plus a small cover for Details and Reader, an icon
  otherwise.

### Shell (jvmMain)

- The main window shows a tab strip in the Nucleus title bar (top layout) or a ~200 dp side column (side layout), a
  toolbar with back/forward, a path (app / source / title / chapter), and buttons for Search, Lists, Downloads and
  Settings. Below that is the active tab's `Nav3` + an in-window overlay layer used for animations and the mini player.
- Torn-out windows use the same shell without a Home tab.
- `HomeNav`'s rail/bottom bar is not used inside tabs. Home's sections (Recent, All, Favorites, Lists, History…) are
  navigation inside the Home tab.

### Opening in tabs from shared screens (the only shared-UI hook)

- Add a `LocalTabActions` composition local in `commonMain`, defaulting to `null`. Android never provides it.
- Desktop provides it with: `open(key, background: Boolean, origin: Rect?)`, `openInNewWindow(key, origin)`, and the
  context-menu entries.
- Shared cover cards and chapter/episode rows use it only when it is non-null: they read modifier keys and the mouse
  button on click, and attach a right-click menu. With `null` they behave exactly as today.
- `origin` is the clicked element's bounds in the window, used by the open animations.

### Motion

All durations are scaled by the system animator setting and replaced by a 150 ms crossfade under reduced motion.

| Moment | Duration | Easing | Behavior |
|---|---|---|---|
| Background tab | 520 ms | emphasized decelerate | Tab slides open in the strip; a copy of the cover shrinks into the tab icon; tab flashes once |
| New tab, switch to it | 450 ms | emphasized decelerate | Content area grows from the clicked cover (starts as cover art, fades to page); cover lands in the Details header |
| Same-tab navigation | existing | existing | Existing Nav3 shared-axis transition; cover flies to the Details header |
| Switch tab | 120 ms | linear | Crossfade |
| Close tab | 180 ms | emphasized accelerate | Tab collapses, neighbors fill in (`animateItem()`) |
| Already open | 300 ms | standard | Switch + flash |
| Tab position change | 450 ms | emphasized decelerate | Each tab moves to its new slot (shared bounds per tab id); side column opens/closes; page slides |
| Mini player | 380 ms | emphasized decelerate | Video shrinks to the corner; reverses on return |
| Tear-out | 450 ms | emphasized decelerate | New window grows from the dragged tab to the drop point (transparent overlay window); Lift fallback |

Fly-to-tab, grow-into-content and the mini player run in an in-window overlay, so they need no OS-specific support.

### Mini player (AnimeWorld)

- The player object must outlive the player screen, so it is owned by a tab-scoped holder rather than the composable.
  When the tab is parked, the video surface re-attaches in the mini player. Returning re-attaches it in the tab.
- Mini player off in settings → leaving a playing tab pauses it.
- Closing the tab or the mini player stops playback.

### Notes

The Details tab gets a "Notes" toggle that opens the existing Notes detail content (`Screen.NotesScreen.Detail`)
in a side panel inside the tab. The open/closed state is per tab.

### Settings (desktop only)

A "Tabs" page with:
- Clicking a title opens: this tab | a new tab
- Tab position: across the top | down the side
- Reopen tabs on launch (default on)
- Mini player when leaving a playing tab (AnimeWorld, default on)

Stored in desktop-only preferences. Android settings are unchanged.

### Restore

On launch, if restore is on: recreate windows (position, size), tabs (order, pinned Home, active tab) and each tab's
serialized back stack. Only the active tab of each window is composed. Others load when first shown. Keys whose source
is no longer installed open their page's normal error/empty state, with no crash.

### Keyboard (desktop)

The shortcuts match normal browsers (the user asked for "normal browser shortcuts", 2026-11-25). ⌘ is used on macOS
and Ctrl on Windows/Linux.

| Action | macOS | Windows / Linux |
|---|---|---|
| New tab (new-tab page) | ⌘T | Ctrl+T |
| Close tab (ignored on Home; the last tab of a torn-out window closes that window) | ⌘W | Ctrl+W, Ctrl+F4 |
| Reopen closed tab | ⌘⇧T | Ctrl+Shift+T |
| Next tab | ⌃Tab, ⌘⌥→, ⌘⇧], ⌃PageDown | Ctrl+Tab, Ctrl+PageDown |
| Previous tab | ⌃⇧Tab, ⌘⌥←, ⌘⇧[, ⌃PageUp | Ctrl+Shift+Tab, Ctrl+PageUp |
| Go to tab 1–8 | ⌘1…⌘8 | Ctrl+1…Ctrl+8 |
| Go to last tab | ⌘9 | Ctrl+9 |
| Back / forward | ⌘[ / ⌘], ⌘← / ⌘→ | Alt+← / Alt+→ |
| Search tab (the "address bar"): open or switch to it and focus its field | ⌘L | Ctrl+L |
| New window (opens a new-tab page, no Home tab) | ⌘N | Ctrl+N |
| Close window (torn-out windows only; ignored on the main window) | ⌘⇧W | Ctrl+Shift+W |

- Next and previous tab wrap around at the ends.
- The existing global-search shortcut also opens or switches to the Search tab.
- Every tab shortcut needs a modifier, so reader and player bare keys keep working.
- ⌘← / ⌘→ only act as back/forward when no text field has focus.
- All of these appear in the existing shortcuts dialog (⌘/).
- Reload (⌘R / F5) is not included, because pages have no generic reload hook.

## Order of work (suggested)

1. Per-tab navigation plumbing, delegating Koin binding, the direct-injection fixes, and the optional
   `Navigation3Actions` interceptor (no UI change yet; Android verified unchanged).
2. Single-window tab shell with pinned Home, top layout, plain/modifier/middle-click, one-per-title, back/forward,
   close, reopen, keyboard.
3. `LocalTabActions` hook in shared cards and rows + right-click menus.
4. Open animations (fly-to-tab, grow-into-content).
5. Side layout + animated switch; Tabs settings page.
6. Single-instance pages (Search, Lists, Downloads, Settings), new-tab page, Notes side panel.
7. Mini player (AnimeWorld).
8. Restore on launch.
9. Tear-out windows, move-between-windows, tear-out overlay with Lift fallback.

## Feature parity (no desktop feature may be dropped)

The tab shell replaces the desktop window's use of `HomeNav`. Everything `HomeNav` does on desktop today must survive:

- **Every rail destination is still one click away.** The Home tab keeps a side nav with all of today's desktop rail
  items: Recent, All, Notifications, Lists, Search, Favorites, Extensions and Settings. The Settings update badge
  (`updateCheck()`) moves to the toolbar's Settings button. Notifications, Search, Lists and Settings open their
  single-instance tabs, and the others navigate inside the Home tab. The mockup's shorter Home nav was illustrative
  only.
- **`HomeNav`'s side effects move into the shell, run once per app (not per tab):** `InitialSetup()`,
  `SyncManager.start()`, `genericInfo.DialogSetups()`, `QuickActionsOverlay()`, theme/AMOLED/blur handling, and
  `LocalWindowSizeClass`.
- **The existing setup in `InternalDesktopUi` keeps working:** the onboarding redirect, extension loading, the
  JS-extension scan, background checkers, the updater dialog, the just-updated snackbar, and quick actions with their
  shortcuts.
- **App-specific hooks still apply inside every tab:** `KmpGenericInfo` nav setup hooks (`globalNav3Setup`,
  `settingsNav3Setup`) and the app's Koin `navigation<Key>` entries. The list-detail, dialog and bottom-sheet scene
  strategies in `Nav3` also work per tab.
- **Mobile-only bar settings don't apply on desktop, just as today:** floating nav, middle nav action, and multiple
  actions. Today's desktop windows are wide enough to use the rail, which ignores them. Their settings stay
  untouched, and Android keeps using them.
- **Desktop shortcuts carry over:** reader keys, player keys, the shortcuts dialog and global search. Tab shortcuts
  must not override existing reader/player bare-key shortcuts.
- **Parity check:** before the shell replaces `HomeNav` on desktop, a checklist of the above is ticked by hand on
  macOS for all three desktop apps.

## Calls made while writing this (user may override)

1. **Where one-tab-per-title applies.** Every navigation to a keyed page checks for duplicates: plain same-tab clicks
   and navigations started from inside screens, as well as new-tab opens. Only each tab's **current page** counts.
   If the page is open elsewhere, the app switches to that tab instead of navigating. Back/forward may land on a page
   that is current in another tab, as in a browser. This needs an optional interceptor on `Navigation3Actions`, a
   small `commonMain` change. The constructor parameter is `null` by default, which keeps Android's behavior.
   `TabsController` supplies it on desktop.
2. **Single-instance keys:**
   - Settings: the root and every `Screen.Settings.*` page belong to the one Settings tab, and navigating to one
     pushes onto that tab.
   - Search: `GlobalSearchScreen` reuses the Search tab and pushes the new query onto its back stack.
   - Lists: the Lists page is one key, and each single list (`CustomListItem`) has its own key.
   - Downloads: one key.
3. **Mini player:**
   - There is one mini player for the whole app, shown in the window that owns the playing tab.
   - If you leave a second playing tab while the mini player is showing, the new video takes over the mini player
     and the old one pauses.
   - The mini player moves with its tab when the tab is moved or torn out.
4. **Downloads button per app:**
   - Each app supplies its downloads page through a `GenericInfo` hook: MangaWorld `DownloadRoute`, AnimeWorld its
     video library/downloads page (`VideoViewerRoute`).
   - NovelWorld has no downloads page, so the button is hidden there.
5. **Lift and motion policy:**
   - Lift means the new window appears at its final spot, scaling up from 0.92 and fading in (380 ms).
   - Tear-out uses Lift wherever the transparent overlay is off. Following the launch/quit policy, that means
     Windows and any OS where the transparent window fails. If the window can't fade either, it simply appears.
   - Under reduced motion, tear-out has no animation.
   - In-window tab animations run on every OS. Only reduced motion swaps them for the 150 ms crossfade.

## Left to the builder

- The exact API names and file layout under `kmpuiviews/jvmMain`.
- How the delegating Koin binding is wired (my call above, but any approach that keeps Android untouched and makes
  Koin users act on the focused tab is fine).
- Where desktop-only preferences live and the restore file format/location (under the app's user-data dir).
- The tab strip implementation (lazy row/column vs plain layout), and how shared-bounds animation is done for the
  layout switch.
- How the player handoff to the mini player is implemented, as long as playback continues without a restart.
- Cap on restored tabs, if any is needed for startup time.
- Tests: unit tests for `TabsController` (open/dup/close/move/restore), desktop UI tests where practical; Android
  build must stay green.
