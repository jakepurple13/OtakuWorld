# In-App Banners — Design

Date: 2026-11-06
Approved mockups: https://claude.ai/artifact/NNSrbvXLroAwXxmriWCHNG (variant **B · Floating stack**)
Message inventory: https://claude.ai/artifact/MtkrVeYVHBcxgcqwLwBnaH

## Intent

In-app feedback is split across three systems that look and behave differently: Material
snackbars (each screen owns a `SnackbarHostState`), Android system toasts, and one Sonner toaster.
Messages vanish when you navigate, some are invisible on desktop, and the wording is uneven. The
user wants one app-wide notification surface. They picked a floating, stackable card deck at the
top of the screen, and sending any kind of message should take one line of code.

## Goals

- One **floating stack** host, drawn once per app root, that survives navigation.
- One Koin singleton, `InAppNotifier`, that any screen, ViewModel or plain class can call.
- Four kinds, each sent in one line: **success**, **info** (neutral, often with Undo), **error**,
  **progress** (ongoing work that turns into a result in place).
- Replace every snackbar, the Sonner toaster, and every system toast shown from a screen
  (46 messages; see "Migration").
- Remove the Sonner dependency.
- Works in all three apps (MangaWorld, AnimeWorld, NovelWorld) on Android and desktop. Lives in
  `kmpuiviews/commonMain`, so iOS gets it for free.
- The `:app` companion module is commented out in `settings.gradle.kts` and is not built, so its
  QR toast (`app/.../otakuworld/QrCode.kt:238`) is left as is.

## Non-goals

- No **sticky** kind (for example "You're offline"). It was in the mockups, but nothing in the
  app needs it today.
- No history or inbox of past banners. Nothing persists across process death.
- No per-user settings (position, duration, turning it off).
- No change to system notifications in the notification tray.

## Anti-goals (do not touch)

- **The 6 system toasts in source/plugin code stay exactly as they are.** User decision: "Do not
  touch them."
  - `mihonsupport/.../network/interceptor/CloudflareInterceptor.kt:263`
  - `mihonsupport/.../network/interceptor/WebViewInterceptor.kt:72`
  - `source_utilities/aniyomi/.../network/interceptor/CloudflareInterceptor.kt:126`
  - `source_utilities/aniyomi/.../network/interceptor/WebViewInterceptor.kt:61`
  - `source_utilities/src/main/java/.../NetworkHelper.kt:174`
  - `source_utilities/cloudstream/.../cloudstream3/CommonActivity.kt:70` (plugin `showToast()` API)
- **`animeworldtv` is not touched at all**, including its leftover debug toasts. User decision.
- No real `Popup`/`Dialog` window for the stack. It is an overlay inside the app's root `Box`,
  so it stays under system bars and inside the desktop window.

## Behavior

### Look

- Each banner is a rounded card (~20dp corners) with elevation. From left to right: a 32dp
  circular icon container, the title (one line, ellipsized), an optional subtitle (one line), an
  optional text action, and a close button.
- Tone comes from the theme's color roles, so it follows the seed color and dark mode:

  | Kind     | Container                          | Default icon        |
  |----------|------------------------------------|---------------------|
  | success  | a green tonal pair derived from the scheme (same approach the app already uses for "ok" states; pick in plan) | check |
  | info     | `inverseSurface` / `inverseOnSurface` | info, or the caller's icon |
  | error    | `errorContainer` / `onErrorContainer` | error |
  | progress | `primaryContainer` / `onPrimaryContainer` | spinner (`CircularWavyProgressIndicator` or small `CircularProgressIndicator`) |

- Callers may pass their own `ImageVector` icon (bookmark, trash, cast, download, copy, QR…).
- Timed banners show a thin countdown bar along the bottom edge. Progress banners show an
  indeterminate bar, or a determinate one when a fraction is given.

### Placement

- **Phone (compact/medium width):** top-center, below `WindowInsets.statusBars` and
  `displayCutout`. Full width with 12dp side margins, capped at 480dp.
- **Expanded width (tablets) and desktop:** **top-right**, 16dp from the top and end edges,
  360dp wide. On desktop it sits under the Nucleus title bar.
- Drawn above all screen content, including the floating nav bar and the quick-actions overlay.
  Drawn below real dialogs and bottom sheets (those are separate windows).

### The stack

- Newest banner on top. Older ones peek out underneath, each offset 9dp down and scaled down 5%.
  At most 3 are visible; content on cards behind the top one fades out.
- When a new banner arrives it slides down from above the top edge and fades in.
  `animateFloatAsState` with a spring drives each card's `graphicsLayer` (translationY, scale,
  alpha) from its index.
- **Tap the deck** when it holds 2 or more banners to fan them out into a vertical list (8dp
  gaps). A scrim appears behind them and a **Clear all** chip shows under the last card. Tapping
  the scrim collapses the list again. When only 1 banner is showing, tapping its body does
  nothing; only its action and close buttons respond.
- **Timers pause** while the deck is fanned out. They also pause while the app is not in the
  foreground (`Lifecycle.State.RESUMED` on Android, window focus on desktop).
- **Dismiss:** swipe a card sideways or upward, tap its close button, or tap its action (an action
  also dismisses). Swiping uses `anchoredDraggable`, with a threshold of about 40% of the width,
  or a 40dp upward drag.
- The list keeps at most 20 banners; past that, the oldest is dropped.

### Durations

| Kind                          | Default |
|-------------------------------|---------|
| success                       | 4 s     |
| info                          | 5 s     |
| info / success with an action | 6 s     |
| error                         | 7 s     |
| progress                      | none, until finished or dismissed |
| result after progress         | uses its own kind's duration |

All timeouts go through `LocalAccessibilityManager.calculateRecommendedTimeoutMillis` (the same
call `SnackbarHost` makes), so users with accessibility services get longer times.

### Keys (replace instead of stack)

- A banner can carry a `key`. If a banner with the same key is still showing, the new one
  replaces it in place, keeping its position and resetting its timer, instead of adding a card.
- Used by the readers' "Marked as read" message (`key = "reader-chapter"`) and by progress
  handles.

### Accessibility

- Each card is a `liveRegion` (`Polite`; `Assertive` for errors), and the announced text is
  title + subtitle.
- The close button has a content description ("Dismiss"), and the action is a real button.
- Swiping is never the only way to dismiss.

## API

The model, the interfaces and `DefaultInAppNotifier` live in `sharedcomponents/commonMain`,
package `com.programmersbox.sharedcomponents.notifier`. `kmpuiviews` already depends on that
module (`api`), and `sharedcomponents` has to post the QR messages itself. The host UI lives in
`kmpuiviews`.

```kotlin
interface InAppNotifier {
    val banners: StateFlow<List<Banner>>

    fun success(title: String, subtitle: String? = null, icon: ImageVector? = null,
                action: String? = null, key: String? = null,
                onAction: suspend () -> Unit = {}): BannerId
    fun info(/* same parameters */): BannerId
    fun error(/* same parameters */): BannerId
    fun progress(title: String, subtitle: String? = null, key: String? = null): ProgressHandle

    fun dismiss(id: BannerId)
    fun dismissAll()
}

interface ProgressHandle {
    fun update(title: String? = null, subtitle: String? = null, fraction: Float? = null)
    fun success(title: String, subtitle: String? = null, action: String? = null, onAction: suspend () -> Unit = {})
    fun info(/* same */)
    fun error(/* same */)
    fun dismiss()
}
```

Usage:

```kotlin
notifier.success("Added to Reading", "Solo Leveling")
notifier.info("Removed Blue Lock", action = "Undo") { historyDao.insert(item) }
notifier.error("Couldn't load MangaDex", "Timed out", action = "Retry") { vm.reset() }

val check = notifier.progress("Checking for updates…")
check.update("Checking 3 of 14…", fraction = 3 / 14f)
check.success("3 new", action = "View") { navigationActions.updateErrors() }
```

- `onAction` runs in the notifier's own scope (`SupervisorJob() + Dispatchers.Main`), not the
  caller's. So Undo and Retry still work after the user has left the screen that sent the banner.
- All functions are non-suspending and thread-safe (state is held in a `MutableStateFlow`, and
  every update goes through `update { }`). Callers on IO threads, such as `GenericAnime`, don't
  need to switch to Main.
- In Compose: `val notifier = koinInject<InAppNotifier>()`. ViewModels and plain classes get it
  through constructor injection (the koin skill applies when wiring).
- Koin: `singleOf(::DefaultInAppNotifier) { bind<InAppNotifier>() }` in the base `kmpuiviews`
  app module.
- A small private helper covers the 4 copies of the "source not installed" banner:
  `InAppNotifier.sourceMissing(title, sourceName, onMoreOptions)`.

### Host

```kotlin
@Composable
fun InAppBannerHost(modifier: Modifier = Modifier)
```

Reads `koinInject<InAppNotifier>().banners` and draws the stack. It is added at:

1. `kmpuiviews/.../presentation/HomeNav.kt`: inside the root `Box`, after `QuickActionsOverlay()`.
   This covers all three apps on Android and desktop (desktop reaches `HomeNav` through
   `DesktopUi.kt`).
2. `UIViews/.../NotificationSettingsActivity.kt`: a separate activity that shows
   `NotificationSettingsScreen`, whose two messages move to the notifier.

Because the notifier is a singleton, a banner sent while another activity is in front (for
example `CloudstreamSettingsActivity`, which finishes right after its error) appears on whichever
host is visible next.

## Migration

Every row in the inventory artifact marked **Move to stack**. Summary by file:

| File | Now | Becomes |
|------|-----|---------|
| `kmpuiviews/.../details/DetailsUtils.kt:141` | snackbar "Added to …" | `success` |
| `kmpuiviews/.../details/DetailsPortrait.kt:233`, `DetailsLandscape.kt:215` | "Added to / Already in For Later" | `success` / `info` |
| `kmpuiviews/.../details/ChapterSelection.kt:258, 268` | message (+ Undo) | `info` (+ Undo) |
| `kmpuiviews/.../settings/lists/OtakuListDetailScreen.kt:284` | message | `info` |
| `kmpuiviews/.../favorite/FavoriteScreen.kt:152`, `history/HistoryScreen.kt:223`, `notifications/NotificationScreen.kt:224`, `notifications/NotificationCatchUp.kt:174` | "Something went wrong. Source might not be installed" + More Options | `sourceMissing(…)` → `error` "Source not installed", subtitle "‹title› · ‹source›", action "More options" |
| `kmpuiviews/.../history/HistoryScreen.kt:204`, `NotificationScreen.kt:273`, `NotificationCatchUp.kt:223` | removed/deleted + Undo | `info` + Undo |
| `kmpuiviews/.../notifications/NotificationScreen.kt:310` | sent to tray | `success` |
| `kmpuiviews/.../recent/RecentViewModel.kt:145` | "Something went wrong" | `error` "Couldn't load ‹source›", action "Retry" → `reset()` |
| `kmpuiviews/.../settings/moresettings/MoreSettingsScreen.kt:50` | **Sonner** Working → Completed / Error | `progress` → `success` / `error` |
| `kmpuiviews/.../settings/lists/imports/ImportListScreen.kt:100, 111, 144`, `ImportFullListScreen.kt:132, 143, 178` | Importing → Completed / "Error" | `progress` → `success` / `error` "Couldn't import list" with the exception message as subtitle |
| `kmpuiviews/.../settings/extensions/ExtensionListScreen.kt:402` | "Copied" | `success` "Link copied" |
| `kmpuiviews/.../settings/notifications/NotificationSettingsScreen.kt:123, 332` | "Deleted N rows" / "Cleared" | `success` (text unchanged in this pass) |
| `kmpuiviews/.../quickactions/QuickActionsOverlay.kt:240, 252` | custom running/result snackbar | `progress` handle → `success` / `info` / `error` with "View" where it exists today |
| `kmpuiviews/.../updatecheck/UpdateCheckErrorsScreen.kt:119` | rechecking / already running / copied | `info` / `success` |
| `mangaworld/shared/.../reader/ReaderCompose.kt:194`, `novelworld/shared/.../reader/ReaderCompose.kt:111` | "Added Chapter" | `info("Marked as read", subtitle = ‹chapter name›, key = "reader-chapter")`, about 2 s, no Undo (user approved) |
| `animeworld/shared/.../videoplayer/VideoPlayerCompose.android.kt:216, 226, 418` | toasts: Casting… / cast error / playback error | `progress` → `success` / `error`; `error` |
| `animeworld/.../MainActivity.kt:66` | toast: cast playback errors | `error` |
| `animeworld/.../GenericAnime.kt:207, 299, 312, 327, 381` | toasts: resolve failed / Casting… / cast error / can't download / Downloading… | `error`; `progress` → result; `error`; `error`; `success` "Download started" |
| `animeworld/shared/.../videos/ViewVideoScreen.kt:173` | snackbar "Subtitles ready" + Play | `success` + Play |
| `animeworld/.../DeleteDialog.kt:56, 60` | toasts: deleted / not deleted / went wrong | `success` / `error` |
| `sharedcomponents/.../qrcode/QrCodeViewer.kt:283` | snackbar "Link copied" | `success` (keep the existing skip when Android shows its own copy confirmation) |
| `sharedcomponents/.../qrcode/QrCodeRepository.android.kt:68, 160` | toasts: saved / copied | `success` |
| `kmpuiviews/koogintegration/.../ChatViewModel.kt:340, 347`, `ChatScreen.kt:181` | saved / deleted / copied | `success` / `info` / `success` |
| `kmpuiviews/src/jvmMain/.../updates/UpdateLaunchDialog.kt:126` | "Updated to …" + What's new | `success` + "What's new" |
| `source_utilities/cloudstream/.../CloudstreamSettingsActivity.kt:44` | toast: could not open settings | `error` |

Copy changes in this pass are limited to the ones listed above. All other text keeps its current
wording and string resources.

### Clean-up that follows from the migration

- Delete `kmpuiviews/.../utils/ToasterUtils.kt` (`ToasterSetup`, `ToasterItemsSetup`,
  `ToastItems`, `DefaultToastItems`, `ErrorToast`; `ToastItems` has no callers today).
- Remove `implementation(commonLibs.sonner)` from `kmpuiviews/build.gradle.kts` and
  `mangaworld/build.gradle.kts` (the mangaworld one is unused), and remove the `sonner` entry
  from `gradle/common.versions.toml`.
- Remove `SnackbarHostState` / `SnackbarHost` from screens and ViewModels where they existed only
  for these messages (`RecentViewModel.snackbarHostState`, `ChatViewModel.snackbarHostState`, the
  per-screen hosts). Screens that use a snackbar host for anything else keep it.
- Remove imports that become unused.

## Testing

- **Unit tests** (`kmpuiviews/commonTest`, `runTest` with virtual time) on `DefaultInAppNotifier`:
  newest-first order; timers expire at each kind's duration; pausing and resuming keeps the
  remaining time; a matching key replaces in place and resets the timer; progress → result keeps
  the same id and position; the 20-item cap drops the oldest; `onAction` runs once and dismisses;
  `dismissAll`.
- **Build:** `./gradlew :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug
  :novelworld:assembleNoFirebaseDebug` and the desktop compile.
- **Device check (adb, Android):** add to list from Details, History remove + Undo, quick-action
  update check (progress → result), reader chapter change (one card replacing itself),
  AnimeWorld download started. Check light and dark, swipe dismiss, fan-out with 3 banners.
- **Desktop (MangaWorld):** top-right placement under the title bar, the "Updated to" banner,
  and fan-out with a mouse.

## Open questions for the plan

- Exact success-green roles: the scheme has no green, so derive one consistently (an existing
  helper, or MaterialKolor harmonized from the seed color).
- Whether the `sent_to_tray` (`NotificationScreen`) and `cleared` (`NotificationSettingsScreen`) strings read well as banners, or
  should get new wording later. Out of scope here unless trivial.
