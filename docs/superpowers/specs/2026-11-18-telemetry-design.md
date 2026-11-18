# Telemetry module design

Date: 2026-11-18 · Branch: `worktree-telemetry`
Companion pages: `2026-11-18-telemetry-design.html` (architecture + event catalog),
`2026-11-18-privacy-screen-mock.html` (option C is the approved screen).

## Intent

Move analytics, crash reporting and crash-log breadcrumbs out of `kmpuiviews`/`UIViews` into a
dedicated `:telemetry` KMP module, then use it to add real product analytics across the apps.

Why: today the only analytic is a screen view, crash logs are ad-hoc `logFirebaseMessage` calls,
and Firebase is compiled into every flavor (noFirebase builds ship Firebase and rely on the calls
failing inside `runCatching`). The owner wants to know what features are used and get better crash
context, while noFirebase builds send nothing and don't carry Firebase.

## Goals

- `:telemetry` KMP module (android, jvm, ios) with `Analytics` and `CrashReporter` interfaces,
  typed events, no-op implementations, injected through Koin.
- Android: `noFirebase` flavor is no-op, `full` flavor uses Firebase Analytics + Crashlytics.
- Desktop and iOS: no-op.
- Migrate every existing call (`logFirebaseMessage`, `recordFirebaseException`, `analyticsScreen`,
  the Firebase calls in `OtakuApp.firebaseSetup()`).
- Add the 47 approved events (list below), user properties, custom keys incl. commit SHA.
- Opt-out toggles in Settings › Privacy & Security (screen option C).
- `TELEMETRY.md` at repo root describing everything sent, linked from `README.md`.

## Non-goals

- Firebase Auth, Firestore sync, Remote Config, Firebase Performance stay where they are.
- Desktop/iOS backends (Sentry etc.). They stay no-op.
- Crashlytics Gradle plugin and the task-disabling logic in `buildSrc` stay unchanged.

## Anti-goals

- Sending item titles, item/chapter URLs, search text, note text, custom list names, account
  emails or file paths, in analytics **or** crash logs.
- Firebase classes on the noFirebase runtime classpath of `kmpuiviews`/`UIViews` (the apps still
  depend on Firebase for Auth/Remote Config; that is out of scope).
- Telemetry calls that can crash the app: every implementation swallows its own failures.

## Constraints

- `otaku-multiplatform` uses `com.android.kotlin.multiplatform.library`, which has no product
  flavors. Flavor split therefore cannot live in `:telemetry`.
- Firebase limits: event name ≤ 40 chars, ≤ 25 params, param values ≤ 100 chars, 500 event names,
  25 user properties. Crashlytics: 64 custom keys, 64 KB log, 8 non-fatals per session.
- `setCrashlyticsCollectionEnabled(false)` applies on next app start.
- Configuration cache on; non-transitive R.

## Approaches

1. **`:telemetry` (KMP) + `:telemetry:android` (Android library with flavors)** — chosen. Flavor
   matching picks no-op vs Firebase automatically; one Koin module for all Android apps.
2. Per-app flavor source sets with a plain `:telemetry:firebase` — rejected: duplicates wiring in
   5 apps.
3. Runtime `BuildConfig.FLAVOR` switch inside `kmpuiviews` — rejected: noFirebase still ships
   Firebase.

## Design

### Modules

**`:telemetry`** (`otaku-multiplatform`, package `com.programmersbox.telemetry`)

commonMain:
```kotlin
interface Analytics {
    val isAvailable: Boolean
    fun log(event: AnalyticsEvent)
    fun screen(name: String)
    fun setUserProperty(name: String, value: String?)
    fun setEnabled(enabled: Boolean)
}

interface CrashReporter {
    val isAvailable: Boolean
    fun log(message: String)              // breadcrumb; also printed in debug like printLogs today
    fun recordException(throwable: Throwable)
    fun setKey(key: String, value: String)
    fun setEnabled(enabled: Boolean)
}

sealed class AnalyticsEvent(val name: String, val params: Map<String, Any>) { /* one subclass per event */ }
object NoOpAnalytics : Analytics      // isAvailable = false
object NoOpCrashReporter : CrashReporter
val noOpTelemetryModule: Module       // binds the no-ops
```
Also commonMain: `Buckets` helper (`count(n)` → `0`, `1-5`, `6-20`, `21+`; `duration(ms)` →
`<1s`, `1-5s`, `5-30s`, `30s-2m`, `2m+`; `percent(p)` → `0-25` … `76-100`), and `TelemetryKeys`
constants for custom keys / user property names.

**`:telemetry:android`** (`otaku-library`, flavors via `ProductFlavorTypes` like `sharedutils`)

- `src/main`: `androidTelemetryModule` that binds `Analytics`/`CrashReporter` to
  `platformAnalytics()`/`platformCrashReporter()`; depends on `api(projects.telemetry)`.
- `src/noFirebase`: those functions return the no-ops.
- `src/full`: `FirebaseAnalyticsImpl`, `CrashlyticsReporter`. Firebase deps are
  `fullImplementation` only.

### Wiring

- `kmpuiviews`, `UIViews`, `mangaworld:shared`, `novelworld:shared`, `animeworld:shared` depend on
  `:telemetry` only.
- Android apps (mangaworld, animeworld, novelworld, animeworldtv, app) include
  `androidTelemetryModule` in their Koin start; desktop apps (`DesktopUi.kt` start) and iOS include
  `noOpTelemetryModule`.
- Call sites: constructor injection in ViewModels/repositories; `by inject()` in workers and
  receivers (already `KoinComponent`); `koinInject()` in composables. Top-level helpers with no
  Koin scope (`FlowUtils.dispatchIoAndCatchList`) resolve via `KoinPlatform.getKoinOrNull()` and
  skip if absent.
- `OtakuApp.firebaseSetup()` keeps `FirebaseApp.initializeApp` and Firebase IDs; the custom keys
  and user properties move to a `TelemetryStartup` call in common code run at app start.

### Removals

- `expect`/`actual` `recordFirebaseException`, `logFirebaseMessage`, `analyticsScreen` from
  `Platform.kt` and its three actuals.
- `firebaseCrashLibs` from `kmpuiviews` androidMain and `UIViews` (apps keep their own Firebase deps
  for Auth/Remote Config/Perf; whether apps can drop `firebaseCrashLibs` too is the builder's call
  once nothing else uses it).

### Screen names

`Nav3View` sends `screen.toString()` today, which leaks `Details(title=…, url=…)`. New:
`Screen.analyticsName` = the `route` string (`details`, `notes_detail`, …). Non-`Screen` NavKeys
use `simpleName`.

### Settings (option C)

Settings › Privacy & Security gets a "Usage & crash data" section, rendered only when
`analytics.isAvailable || crashReporter.isAvailable` (false in noFirebase):

- Switch **Share usage analytics** — "Which features get used."
- Switch **Send crash reports** — "Helps fix bugs faster." Turning it off reveals
  "Crash reporting turns off the next time the app starts." (`AnimatedVisibility`).
- Chip groups: *Usage analytics sends*, *Crash reports send*, *Firebase adds to both*,
  *Never sent* (struck-through). A group fades to 40% when its switch is off; the Firebase group
  fades when both are off. Contents exactly as in the mock.
- Row **Full details** → opens `https://github.com/jakepurple13/OtakuWorld/blob/develop/TELEMETRY.md`.
- Both settings default on, stored in DataStore, applied via `setEnabled` at start and on change.
- Register both toggles in `SettingsSearchRegistry`.

### Custom keys (Crashlytics)

`buildType`, `buildFlavor` (existing), `commit_sha` (from `kmpuiviews` `BuildConfig.COMMIT_SHA`),
`current_screen`, `current_source`, `extension_loader`, `last_worker`, `image_loader`,
`ai_engine`, `incognito`, `player_style`.

### User properties

`buildType`, `buildFlavor` (existing), `app_name`, `extension_count` (bucketed),
`favorites_count` (bucketed), `ai_engine`, `theme_mode`, `floating_nav`, `player_style` (anime).

### Breadcrumbs

Existing ones migrate; titles in `MediaUpdateChecker` are replaced with source names and counts.
New breadcrumbs: source calls (start/done/fail + ms), extension load, reader/player open and state,
downloads, DB migrations, backup/restore/sync steps.

### Non-fatals

Existing (RecentViewModel, DetailsViewModel, FlowUtils, CrashActivity) plus: worker failures,
extension load/install failures, player errors, AI engine init failures, Room migration/write
failures. Image decode failures are excluded (noise).

### Events (approved, 47)

Params as in the catalog page; tiers no longer matter, all are in.

onboarding_finished, onboarding_step, source_selected, search, describe_search, item_viewed,
favorite_added, favorite_removed, list_item_added, list_created, list_imported, item_shared,
note_saved, mood_tag_added, update_check_toggled, chapter_opened, reading_session,
reader_setting_changed, chapter_bookmarked, dictionary_lookup, episode_play_requested,
playback_started, playback_error, download_started, download_finished, favorites_filtered,
favorites_searched, history_cleared, update_check_finished, update_notification_opened,
catch_up_used, notification_dismissed, extension_installed, extension_install_failed, source_error,
extension_uninstalled, extension_updated, source_update_skipped, backup_created, restore_finished,
sync_sign_in, sync_failed, ai_task, ai_model_installed, translation_used, quick_action_used,
daily_overview_opened.

Not chosen: setting_changed, search_result_opened, url_opened, incognito_toggled,
playback_session, cast_session_started, player_style_changed.

### TELEMETRY.md

Repo root. Sections: what's collected and when (only `full` builds, only with toggles on);
usage analytics (every event with its params); crash reports (custom keys, breadcrumb categories,
non-fatals); what Firebase adds automatically; never sent; how to turn it off. Linked from
`README.md` in a new "Privacy & Telemetry" section after "Cloud Sync" (same link added to
`README-zh.md`). Must be updated in the same PR as any event change.

### Rollout

- **PR 1** — modules, Koin wiring, migrate existing calls, remove expect/actuals, titles out of
  breadcrumbs, `screen.analyticsName`, `commit_sha` key. No new events.
- **PR 2** — the 47 events, user properties, new custom keys/breadcrumbs/non-fatals, settings
  section, `TELEMETRY.md` + README links.

## Incognito sources (owner's decision)

Sources marked incognito (`IncognitoRepository`) are still counted, but their name is replaced with
`incognito` everywhere: event `source`/`package` params, the `current_source` custom key, and
source breadcrumbs. `telemetrySourceName(...)` applies this together with the `custom` rule, so
every call site goes through it. `TELEMETRY.md` and the settings chips mention this.

## Calls made during review (planner's, not the owner's)

- **No-ops still print.** `NoOpCrashReporter` prints breadcrumbs and stack traces to the console
  (same as today's jvm/iOS actuals and Android `printLogs`), so local noFirebase/desktop dev keeps
  its output. `NoOpAnalytics` stays silent. `isAvailable` stays false for both.
- **Source names.** `source`/`package` params use the plugin-declared service name and package.
  User-defined sources (custom scraper, user-added JS extensions) send `custom`. One shared
  `telemetrySourceName(...)` helper does this.
- **animeworldtv and app** are commented out in `settings.gradle.kts`; they are skipped and noted
  in the PR, not re-enabled.
- **Full-flavor verification.** The main checkout has `google-services.json` for mangaworld and
  animeworld. Copy them into the worktree (never commit) to check events in Firebase DebugView and
  keys on a forced test crash.

## Left to the builder

- Exact file placement of each event call (choose the ViewModel/worker where the action completes).
- Whether apps can drop `firebaseCrashLibs` after PR 1.
- Unit tests: `Buckets`, event param maps (name ≤ 40, ≤ 25 params, no forbidden keys), a fake
  `Analytics` in existing ViewModel tests where events are added.
