# More Quick Actions — Design

Date: 2026-11-10
Builds on: `docs/superpowers/specs/2026-11-05-quick-actions-design.md` (PR #2674) and the
"Update check errors" row added after it.
Approved mockup: https://claude.ai/artifact/JNTS8TZwpQ4FkNgon1tshU (version 3)

## Intent

Quick actions has become the fast way to reach things the user would otherwise dig for. The user
wants three more places in it: **Global search**, **Lists** and **Saved notifications**. With 7
actions the card needs structure, and on desktop it is now worth filtering. The user also wants
desktop keyboard shortcuts to be discoverable, through a button that lists all of them.

## Goals

1. Add three navigate-actions to the shared action list: Global search, Lists, Saved notifications.
2. Group the card into two labelled sections, in this order:
   - **Tools**: Check for updates (Run), Scan QR code.
   - **Go to**: Global search, Lists, Saved notifications, Background tasks, Update check errors.
3. Desktop only: a filter field at the top of the quick-actions card.
4. Desktop only: `⇧⌘F` (macOS) / `Ctrl+Shift+F` (Windows/Linux) opens Global search from anywhere.
5. Desktop only: a keyboard-shortcuts list, opened from a title-bar button, an Actions-menu item,
   `⌘/` / `Ctrl+/`, and a "Shortcuts" link in the quick-actions card footer.
6. AnimeWorld desktop only: the shortcuts list gains a **Video player** section with the player's
   keys. MangaWorld and NovelWorld are unaffected.

## Non-goals

- No filter field on phone/Android.
- No shortcuts button or shortcuts list on phone/Android.
- No user customization of which actions appear or their order.
- No new shortcuts beyond `⇧⌘F` and `⌘/` (Lists, Saved notifications etc. get no keys).
- No change to the video player's own `?` sheet beyond reading from a shared list.

## Anti-goals

- MangaWorld or NovelWorld showing anything player-related, or needing code changes to opt out.
- The player key list and the shortcuts list drifting apart (two hand-maintained copies).
- A second, divergent action list for desktop. Card, `⌘K`, Actions menu and title-bar dropdown all
  read one definition (unchanged rule from the original spec).

## Behavior

### The card (phone and desktop)

- Header "Quick actions" with the count ("7") as today.
- Two segmented groups, each with its own large outer corners (~18dp) and small inner corners
  (~6dp), 3dp gaps between rows, ~10dp between groups. A small uppercase label sits above each
  group: "Tools", "Go to".
- Rows, in order (icon container tone in brackets; match today's M3 mapping style):
  - Tools
    - Check for updates — refresh, primaryContainer — "Last checked …" — **Run** chip (unchanged).
    - Scan QR code — QR, tertiaryContainer — "Import a title or list" — chevron (unchanged).
  - Go to
    - Global search — search icon, a blue-ish tone — "Search every installed source" — chevron.
    - Lists — list icon, a violet-ish tone — "<n> lists" — chevron.
    - Saved notifications — bell icon, an amber-ish tone — "<n> saved" — chevron.
    - Background tasks — clock, secondaryContainer — "<n> scheduled" (unchanged) — chevron.
    - Update check errors — error icon, errorContainer — unchanged text — chevron.
- Counts: Lists from `ListDao.getAllListsCount()`, saved notifications from
  `ItemDao.getAllNotificationCount()`. When a count is 0, show a plain fallback line instead of
  "0 …" (same pattern as Background tasks' `qa_background_tasks_sub`), e.g. "Your custom lists" /
  "Saved update notifications".
- Navigation: Global search → `navigationActions.globalSearch()`, Lists →
  `navigationActions.customList()`, Saved notifications → `navigationActions.notifications()`.
  All three screens already exist in common `Nav3Graph.kt`, so they work on desktop too.
- The card is ~510dp tall on phone. On a phone too short for it, the card's content scrolls inside
  the card; the header stays fixed. Open/close morph is unchanged.

### Desktop `⌘K` card filter

- A rounded search field sits between the header and the groups, focused when the card opens from
  the keyboard (`⌘K`) or from the bolt.
- Filtering matches, case-insensitively, the row title and its supporting text.
- While the filter is non-empty, group labels are hidden and matches show as one segmented group
  in their normal order.
- The first match is selected. `↑`/`↓` move the selection, `↵` runs it (same as clicking it),
  `Esc` closes the card. With no matches, show "No actions match "<text>"".
- The filter text is cleared each time the card opens.
- Footer line: "↑↓ move · ↵ run · esc close" on the left, a **Shortcuts** text button with its key
  (`⌘/`) on the right, which closes the card and opens the shortcuts list.
- Rows that have a shortcut show it as a small keycap before the chevron (only Global search today).

### Desktop shortcuts

- `⇧⌘F` / `Ctrl+Shift+F`: Global search. `⌘/` / `Ctrl+/`: shortcuts list.
- Wire them like `⌘K` is wired today: on macOS as `KeyShortcut`s on the system menu-bar items;
  on Windows/Linux in `DecoratedWindow.onPreviewKeyEvent` in `DesktopUi.kt`.
- Key labels are shown per platform: `⌘K`, `⇧⌘F`, `⌘/` on macOS; `Ctrl+K`, `Ctrl+Shift+F`, `Ctrl+/`
  elsewhere.

### Desktop Actions menu (macOS menu bar and Windows/Linux title-bar dropdown)

Order, with separators:
1. Check for updates, Scan QR code
2. Global search (`⇧⌘F`), Lists, Saved notifications, Background tasks, Update check errors
3. Quick actions… (`⌘K`), Keyboard shortcuts… (`⌘/`)

### Desktop shortcuts list

- A keyboard-icon button in the title bar of the main HomeNav window, on all desktop platforms
  (on Windows/Linux next to the Actions dropdown; on macOS it is the only title-bar button added).
  Tooltip "Keyboard shortcuts".
- Opens a centered dialog over a scrim, styled like the quick-actions card (large corners,
  surface-container background). Title "Keyboard shortcuts", subtitle "Shortcuts for <app name>".
  Scrolls inside itself if taller than the window. Closes on Close, scrim click, or `Esc`.
- Sections:
  - **Anywhere**: Quick actions `⌘K`; Global search `⇧⌘F`; Keyboard shortcuts `⌘/`.
  - **In Quick actions**: Move selection `↑ ↓`; Run selected action `↵`; Close `Esc`.
  - Then any sections contributed by the app (below).
- Sections contributed by the app come from Koin (e.g. `getAll<ShortcutSection>()` or an equivalent
  the builder picks). kmpuiviews registers none, so apps that add nothing get only the two
  sections above.

### AnimeWorld: Video player section

- AnimeWorld's desktop Koin module contributes a "Video player" section.
- Its rows come from the player's existing key list (`Shortcuts` in
  `animeworld/shared/src/jvmMain/.../videoplayer/DesktopPlayerOverlays.kt`), made non-private (or
  moved to a shared place in that module) so both the player's `?` sheet and this section read the
  same list.
- User decision: the list changes from one string per entry (`"Space / K"`) to structured keys
  (e.g. `listOf("Space", "K")`), so the shortcuts list can draw separate keycaps. The player's `?`
  sheet joins the keys back into today's text, so it looks unchanged (`"Space / K"`,
  `"Shift + C"`, `"F / double-click"` — the builder keeps the join faithful to today's strings).
- The `?` row's label differs by context: "This sheet" in the player's `?` sheet (unchanged),
  "Player shortcuts sheet" in the shortcuts list. Shared entries may carry an optional label
  override for the shortcuts list.
- MangaWorld and NovelWorld register nothing and need no code change.

## Existing pieces

- `QuickAction` enum: `kmpuiviews/commonMain/.../quickactions/QuickAction.kt` (4 entries today).
- `QuickActionsList.kt`: the segmented list, built row by row with First/Middle/Last shapes.
- `QuickActionsViewModel`: `run(action)` dispatch and `QuickActionsUiState` (holds `scheduledCount`).
- `QuickActionsDesktop.kt` (jvmMain): `isQuickActionsShortcut`, `QuickActionsMenuBar`,
  `QuickActionsTitleBarMenu`, `QuickAction.label()`.
- `DesktopUi.kt` (jvmMain): `onPreviewKeyEvent` handler, `MaterialTitleBar` contents, the
  `showQuickActions` flag that limits the menu to the main window.
- `NavigationActions`: `globalSearch()`, `customList()`, `notifications()` already exist.
- DB counts: `ListDao.getAllListsCount()`, `ItemDao.getAllNotificationCount()`.

## Decisions made by Claude (not explicitly by the user)

- Count text and its zero-count fallback lines for Lists and Saved notifications.
- Filter matches supporting text as well as titles.
- Filter clears on each open; the filter is focused on open from the bolt too.
- Only Global search shows a keycap in the card.
- `⌘K` card position stays as today (centered horizontally, near the top as in the mockup); `↵`
  shows as "Enter" on Windows/Linux.
- The shortcuts button appears on every desktop platform, including macOS where the Actions menu
  lives in the system menu bar.
- The shortcuts list is a dialog over a scrim inside the window (in-composition), matching the card.
- App sections are contributed through Koin.

## Left to the builder

- Data shape for groups (e.g. a `group` property on `QuickAction`) and for shortcut sections.
- Exact icons, tonal colors for the three new rows, durations.
- How the shortcuts-list open state is held (likely alongside `QuickActionsController`).
- Making sure `Ctrl+Shift+F` / `⇧⌘F` is caught before the AnimeWorld player's own `F` (fullscreen)
  handling when the player has focus.
- Tests: unit-test grouping/order, the filter (title + supporting text, empty result, selection
  clamp), the new `run()` navigations, count fallbacks, the shortcut matchers, and that the
  shortcuts list shows the AnimeWorld section only when contributed. Run the Android app on a device
  for the grouped card, and run the desktop app for filter, shortcuts and the list.
