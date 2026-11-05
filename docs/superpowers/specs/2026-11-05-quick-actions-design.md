# Quick Actions — Design

Date: 2026-11-05
Approved mockups: https://claude.ai/artifact/AyHvpu86CuThQLyhfDCA4V (section "Option 2 at full width", look **B · segmented list**; desktop `⌘K` mockup)

## Intent

The user wanted a faster way to run things they currently dig through Settings for, starting with
"run update check". Quick actions are a short, fixed list of shortcuts reachable from a bolt
button, so a manual update check (and two screens the user reaches often) are one tap away instead
of several.

## Goals

- A bolt `IconButton` in the top bar of **Recent** and **Settings** (the Settings home screen) on
  Android and desktop.
- Tapping the bolt morphs the button into a full-width quick-actions card holding three actions:
  1. **Check for updates** — runs the manual favorites update check in place.
  2. **Scan QR code** — opens the existing QR scan screen.
  3. **Background tasks** — opens the existing worker info screen.
- Desktop also gets `⌘K` (macOS) / `Ctrl+K` (Windows/Linux) to open the same list, and a new
  **Actions** menu in a menu bar with the same three items.
- Applies to all three apps (MangaWorld, AnimeWorld, NovelWorld). The UI lives in shared code, so
  no per-app work is expected. Desktop needs platform work for progress reporting (see "Desktop
  update-check progress").

## Non-goals

- No search/filter box. With three actions it was judged too thin (the user replaced the palette
  with a plain list).
- No user customization of which actions appear or their order.
- No launcher shortcuts, Quick Settings tile, floating-nav bolt or Settings chip row (all explored
  and passed over).
- No other screens get the bolt.
- No new actions beyond the three.

## Anti-goals

- Using a real `Dialog` / `Popup` window on phone. The morph must visibly connect the bolt to the
  card; a separate window breaks the shared-bounds animation.
- A second, divergent action list for desktop. Phone, desktop card, `⌘K` and the menu bar all read
  one definition.

## Behavior

### The list (look B, segmented list)

- Card title "Quick actions" with a small count ("3") at the trailing edge.
- Three rows forming one connected group: each row is its own surface (`surfaceContainerHigh`-ish)
  with ~3dp gaps; the group's outer corners are large (~18dp at mock scale), inner corners small
  (~6dp). This is the Material 3 Expressive segmented list style.
- Each row: leading 32dp rounded-square icon container with its own tonal color, title, one line
  of supporting text, trailing affordance.
  - Check for updates: refresh icon, pink/primary tonal; supporting text "Last checked <relative
    time>"; trailing filled **Run** chip.
  - Scan QR code: QR icon, teal/tertiary-ish tonal; "Import a title or list"; trailing chevron.
  - Background tasks: clock icon, amber tonal; "<n> scheduled"; trailing chevron.
- "Run" means it executes where you are; a chevron means it navigates.

### Opening and closing (phone and desktop bolt click)

- Container transform: the bolt's circular container grows and recolors into the card. The card is
  pinned to the top: it starts at the bolt's position and ends spanning the window width with 8dp
  side margins, top just below the status bar (it covers the app bar). Row content fades in once
  the card is mostly expanded.
- Wide windows (desktop, tablets; user decision): the card's width is capped at about 480dp and it
  stays anchored top-end, under the bolt. Phones (compact width) get the full width.
- A light scrim (~40% black) dims the screen behind.
- Dismiss by tapping the scrim, pressing back (Android back / `Esc` on desktop), or choosing an
  action. Dismissal runs the morph in reverse into the bolt.
- Motion: emphasized decelerate in, emphasized accelerate out (mockup used
  `cubic-bezier(.2,0,0,1)` / `cubic-bezier(.3,0,.8,.15)`). Exact durations are the builder's call;
  aim for ~400ms open.

### Running "Check for updates"

- Card morphs back into the bolt.
- The bolt shows a spinning progress ring while the manual check is running (driven by
  `WorkRepository.manualCheck`).
- A snackbar shows progress while the check runs (e.g. "Checking 42 favorites for new chapters…").
  Wording should fit each app (chapters vs episodes) if the existing strings make that easy;
  otherwise neutral wording is fine.
- If a check is already running, tapping Run does not start a second one.
- When the check finishes (user decision), the snackbar is replaced by the result: "<n> new
  chapters" (episodes/chapters per app) or "No new chapters". If any sources failed, it says
  "Some sources failed" with a **View** action that opens `Screen.UpdateCheckErrors`. Read the result
  from the existing end-of-check report (`UpdateCheckReportStore` / `lastUpdateCheckReport`).
- The action still runs when the user has turned scheduled update checks off (Claude's call: a
  manual tap is explicit). The row is never hidden or disabled.

### Desktop update-check progress (user decision: match Android)

The JVM side does not report progress today, so this is part of the work:
- `WorkerRepositoryImpl.manualCheck` (JVM) is `emptyFlow()`. Make it emit running/progress/finished
  state for the manual check, the same shape Android exposes (`progress` / `max`), so the bolt
  spinner and the snackbar work.
- JVM `checkManually()` has no guard and overwrites `manualRun`. Add the "don't start a second
  check" rule there.
- JVM `workerInfoFlow()` returns an empty list, so "<n> scheduled" would always read 0. Make it
  report the scheduled checks, or hide the count on desktop if that turns out impractical (builder
  may decide; say which in the PR).
- Scheduled checks on desktop run in a separate scheduler process (`setupSyncCheckers(args)`). An
  in-process guard only needs to cover manual checks. Coordinating with the scheduler process is
  out of scope unless it is cheap.

### Desktop `⌘K` / `Ctrl+K`

- Opens the same list centered in the window, scale 0.94→1 + fade in, with the scrim. No morph:
  there is no button to grow from. Same ~480dp width cap as the wide-window card.
- Works from any screen, not only Recent and Settings.
- Clicking the bolt on desktop uses the same morph as phone.

### Desktop menu bar

- Add a menu bar with an **Actions** menu: Check for updates, Scan QR code, Background tasks, a
  separator, then "Quick actions…" (`⌘K`).
- The desktop windows currently have no menu bar; they use the Nucleus `DecoratedWindow` with a
  custom `MaterialTitleBar` (`kmpuiviews/src/jvmMain/.../DesktopUi.kt`). The menu bar is new work.
- macOS: the system menu bar (Compose `MenuBar`).
- Windows/Linux (user decision): an "Actions" text button inside the custom `MaterialTitleBar` that
  opens a Compose dropdown with the same items. No Swing menu strip.
- Only the main app window (the one hosting HomeNav) gets the menu. `OtakuDecoratedWindow` is also
  used by `InternalBaseDesktopUi` and the showcase `BaseWindow`; those don't (Claude's call).

## Existing pieces to build on

- Manual update check: `WorkRepository.checkManually()` and `WorkRepository.manualCheck` (common
  interface; Android impl enqueues `UpdateFlowWorker` with `CHECK_ALL`, JVM impl runs
  `SyncCheckWorker` via `TestTaskRunner`). This is what Notification settings' "check now" uses.
- Navigation: `NavigationActions` already has calls that add `Screen.ScanQrCodeScreen` and
  `Screen.WorkerInfoScreen` (`Navigation3Actions.kt` lines ~71 and ~163). Both entries are
  registered in common `Nav3Graph.kt`, so they exist on desktop.
- Shared transitions: `HomeNav.kt` (kmpuiviews common) wraps the app in `SharedTransitionLayout` and
  provides `LocalSharedElementScope`. HomeNav is used by both Android (`BaseMainActivity`) and
  desktop (`DesktopUi.kt`), so the overlay card can live inside it and morph from the bolt.
- Top bars: `RecentScreen.kt` (`TopAppBar` with `actions`), Settings home (`SettingScreen.kt`,
  `LargeTopAppBar`).

## Decisions made by Claude (not explicitly by the user)

- One shared `QuickAction` definition in `kmpuiviews/commonMain`, read by every surface.
- The phone card is an in-composition overlay inside HomeNav's `SharedTransitionLayout`, not a
  `Dialog`, because shared bounds cannot cross windows.
- The bolt spinner and snackbar are driven by `WorkRepository.manualCheck`.
- `⌘K` is window-wide on desktop.
- "Check for updates" runs even when scheduled update checks are turned off.
- The desktop menu goes only on the main HomeNav window.
- Note: `NavigationActions.scanQrCode()` has no callers today, so quick actions become the only way
  into the QR screen.

## Left to the builder

- Exact component structure, state holder (ViewModel vs plain state), and Koin wiring.
- Exact durations, colors mapped to the M3 color scheme, and icons (Material icons equivalent to
  refresh / QR / schedule / bolt).
- How the open/closed state reaches both the bolt (inside a screen) and the overlay (HomeNav root),
  e.g. a small Koin singleton or a composition local.
- Tests: unit-test the action list and the "don't start a second check" rule; device-verify the
  morph on Android and run the desktop app.
