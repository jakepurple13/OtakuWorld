# Changelog

## [Unreleased] - changes since v32.1.2

_Compare: https://github.com/jakepurple13/OtakuWorld/compare/v32.1.2...develop_

### Highlights

- **Native Mihon extension support in MangaWorld (Android).** Mihon/Tachiyomi extensions now load
  directly, without a bridge app. Includes a repo client with `index_v2` support, a trust gate, and
  per-source settings ([#2592](https://github.com/jakepurple13/OtakuWorld/pull/2592))
- **AnimeWorld and NovelWorld on Desktop.** Both apps now have a Desktop build alongside
  MangaWorld, with packaged installers for Windows, macOS, and Linux
  ([#2505](https://github.com/jakepurple13/OtakuWorld/pull/2505),
  [#2507](https://github.com/jakepurple13/OtakuWorld/pull/2507))
- **JavaScript/TypeScript extensions.** A new sandboxed extension format that runs on Android and
  Desktop ([#2509](https://github.com/jakepurple13/OtakuWorld/pull/2509),
  [#2511](https://github.com/jakepurple13/OtakuWorld/pull/2511))
- **Redesigned Extensions screen** with catalog shelves, extension details, and install progress
  ([#2601](https://github.com/jakepurple13/OtakuWorld/pull/2601))
- **New backup and restore wizard** that lets you pick exactly what to back up or restore, down to
  individual lists ([#2503](https://github.com/jakepurple13/OtakuWorld/pull/2503),
  [#2525](https://github.com/jakepurple13/OtakuWorld/pull/2525))
- **Dictionary and Glossary** for saving terms while you read
  ([#2492](https://github.com/jakepurple13/OtakuWorld/pull/2492))
- **Notifications timeline and Catch-up deck**
  ([#2588](https://github.com/jakepurple13/OtakuWorld/pull/2588))

---

## Shared (all apps)

### All Platforms

#### New Features

- Extensions screen redesigned with catalog shelves, an installed pane, per-catalog pages, and an
  extension detail sheet with install progress. Failed catalogs now show an error with a retry
  button ([#2601](https://github.com/jakepurple13/OtakuWorld/pull/2601))
- JavaScript/TypeScript extension loader: on-device TypeScript stripping, sandboxed QuickJs
  runtime, local/remote/bundled discovery, and hot load/unload. JS extensions show up as regular
  sources ([#2509](https://github.com/jakepurple13/OtakuWorld/pull/2509))
- Extension update mode setting: automatic, notify, or disabled
  ([#2509](https://github.com/jakepurple13/OtakuWorld/pull/2509))
- Backup and restore wizard: step-by-step screens, choose which items to include, per-item results,
  and a peek at a backup's contents before you restore
  ([#2503](https://github.com/jakepurple13/OtakuWorld/pull/2503))
- Backup and restore: choose individual custom lists. A failure in one row no longer fails the
  whole backup ([#2525](https://github.com/jakepurple13/OtakuWorld/pull/2525))
- Backup and restore now shows how long the operation took
  ([f86413762](https://github.com/jakepurple13/OtakuWorld/commit/f86413762))
- Encrypted settings are now included in backups
  ([52fb4ca73](https://github.com/jakepurple13/OtakuWorld/commit/52fb4ca73))
- Dictionary and Glossary: create, edit, search, and sort entries in a list-detail layout. Open it
  from Library settings or settings search
  ([#2492](https://github.com/jakepurple13/OtakuWorld/pull/2492))
- Dictionary entries are included in backup and restore
  ([6103ad532](https://github.com/jakepurple13/OtakuWorld/commit/6103ad532))
- Notifications screen redesigned as a timeline with multi-select. Adds a Catch-up deck with swipe
  gestures and a Skip/Delete toggle. Swipe directions match the buttons
  ([#2588](https://github.com/jakepurple13/OtakuWorld/pull/2588))
- Custom list detail screen overhauled: sort, grid/list view, filters, multi-select, move items
  between lists, and random pick
  ([#2594](https://github.com/jakepurple13/OtakuWorld/pull/2594))
- Info action when a single item is selected in a custom list
  ([4cf4d57c6](https://github.com/jakepurple13/OtakuWorld/commit/4cf4d57c6))
- Pick a cover image for a custom list
  ([#2570](https://github.com/jakepurple13/OtakuWorld/pull/2570))
- Cloud sync: choose which tables back up to the cloud, per device
  ([#2490](https://github.com/jakepurple13/OtakuWorld/pull/2490))
- Sign-out now shows a sheet where you choose what happens to local data (favorites, chapters
  watched, and more) ([#2491](https://github.com/jakepurple13/OtakuWorld/pull/2491))
- Activity timer (time spent reading/watching) now syncs to the cloud and is included in backups
  ([#2513](https://github.com/jakepurple13/OtakuWorld/pull/2513))
- Account info: account statistics detail screen with animated charts, daily average, and top
  heat-map stats. Shows your signed-in user info
  ([#2506](https://github.com/jakepurple13/OtakuWorld/pull/2506),
  [#2486](https://github.com/jakepurple13/OtakuWorld/pull/2486),
  [e07e984ab](https://github.com/jakepurple13/OtakuWorld/commit/e07e984ab),
  [3fe19cb52](https://github.com/jakepurple13/OtakuWorld/commit/3fe19cb52))
- About screen shows the app icon, build time, Git SHA, and other build details. App info moved to
  its own screen ([#2517](https://github.com/jakepurple13/OtakuWorld/pull/2517),
  [757662764](https://github.com/jakepurple13/OtakuWorld/commit/757662764),
  [6137aa303](https://github.com/jakepurple13/OtakuWorld/commit/6137aa303))
- New "Ugly" and "MyEyes" color schemes
  ([#2556](https://github.com/jakepurple13/OtakuWorld/pull/2556))
- New "Glass" blur style using HazeGlass
  ([4797f8eb3](https://github.com/jakepurple13/OtakuWorld/commit/4797f8eb3))
- "Check for updates" option in the item options sheet
  ([08ff7d137](https://github.com/jakepurple13/OtakuWorld/commit/08ff7d137))
- AI chat: manage local LLM models from a new model management screen
  ([#2512](https://github.com/jakepurple13/OtakuWorld/pull/2512))

#### Improvements

- Extension screens updated to Material 3 Expressive components, with layout and visibility
  animations ([c532d1784](https://github.com/jakepurple13/OtakuWorld/commit/c532d1784),
  [809bf9625](https://github.com/jakepurple13/OtakuWorld/commit/809bf9625))
- Remote extensions now show their language and NSFW flag
  ([#2601](https://github.com/jakepurple13/OtakuWorld/pull/2601))
- Catch-up moved to its own screen. Notifications use a Material 3 Scaffold with an animated top
  app bar ([515ce57f8](https://github.com/jakepurple13/OtakuWorld/commit/515ce57f8),
  [b5e7c276d](https://github.com/jakepurple13/OtakuWorld/commit/b5e7c276d))
- Recent screen: the offline indicator moved to the top app bar
  ([#2525](https://github.com/jakepurple13/OtakuWorld/pull/2525))
- Bookmarks display reworked ([#2570](https://github.com/jakepurple13/OtakuWorld/pull/2570))
- Custom list item covers use a new layout with higher resolution
  ([#2570](https://github.com/jakepurple13/OtakuWorld/pull/2570))
- Details header animates size changes
  ([b31f12b7f](https://github.com/jakepurple13/OtakuWorld/commit/b31f12b7f))
- AI chat input moved to the bottom bar and handles the keyboard correctly
  ([#2522](https://github.com/jakepurple13/OtakuWorld/pull/2522))
- Sensitive settings now use encrypted DataStore (Google Tink)
  ([#2532](https://github.com/jakepurple13/OtakuWorld/pull/2532))

#### Bug Fixes

- Fixed removing items from a custom list
  ([#2598](https://github.com/jakepurple13/OtakuWorld/pull/2598))
- Fixed custom list items being soft-deleted by the parent list's id instead of their own
  ([#2489](https://github.com/jakepurple13/OtakuWorld/pull/2489))
- Signing in now updates immediately; an app restart is no longer needed
  ([#2528](https://github.com/jakepurple13/OtakuWorld/pull/2528))
- Fixed the logout loading dialog being hidden behind the auth state transition
  ([#2491](https://github.com/jakepurple13/OtakuWorld/pull/2491))
- Backup/restore wizards now honor the selected items. Fixed a silent list-filter skip and a
  Desktop zip race ([4fda999d4](https://github.com/jakepurple13/OtakuWorld/commit/4fda999d4),
  [#2525](https://github.com/jakepurple13/OtakuWorld/pull/2525))
- JS extensions: fixed crashes on large responses and on concurrent access to the QuickJs runtime
  ([#2511](https://github.com/jakepurple13/OtakuWorld/pull/2511))
- Fixed bottom toolbar and snackbar padding on the Notifications and Catch-up screens
  ([62ab8482a](https://github.com/jakepurple13/OtakuWorld/commit/62ab8482a),
  [#2588](https://github.com/jakepurple13/OtakuWorld/pull/2588))
- Fixed update status transition and disabled state in the options sheet
  ([6e4048f4d](https://github.com/jakepurple13/OtakuWorld/commit/6e4048f4d),
  [266d1fc8c](https://github.com/jakepurple13/OtakuWorld/commit/266d1fc8c))
- Fixed Settings deep links ([#2507](https://github.com/jakepurple13/OtakuWorld/pull/2507))

#### Removed

- AI recommendation providers, settings, and screens
  ([#2509](https://github.com/jakepurple13/OtakuWorld/pull/2509))
- Custom server sync ([0e723f117](https://github.com/jakepurple13/OtakuWorld/commit/0e723f117))
- In-app WebView screen ([d3843280b](https://github.com/jakepurple13/OtakuWorld/commit/d3843280b))

### Android Only

#### New Features

- Screensaver (Daydream) with adaptive layout, date and battery card, and sensor-based rotation.
  Includes cover carousel, history feed, stats, and custom-list screensavers, and a link to the
  system screensaver settings ([#2577](https://github.com/jakepurple13/OtakuWorld/pull/2577),
  [#2579](https://github.com/jakepurple13/OtakuWorld/pull/2579),
  [18cf9bd07](https://github.com/jakepurple13/OtakuWorld/commit/18cf9bd07))
- Home screen widget showing the notification count, with in-app pinning
  ([#2556](https://github.com/jakepurple13/OtakuWorld/pull/2556))
- Sign in with Android Credential Manager, which can also save your password
  ([#2517](https://github.com/jakepurple13/OtakuWorld/pull/2517))
- Extension installs now use the native Android PackageInstaller instead of Ackpine. Installs run
  one at a time and show pending-confirmation, permission-required, and cancelled states. Sources
  reload right after a successful install
  ([#2524](https://github.com/jakepurple13/OtakuWorld/pull/2524))

#### Bug Fixes

- Extension install: fixed retry deleting the downloaded file, a session leak when commit fails,
  installs hanging when the confirmation dialog can't be shown, and duplicate status updates
  ([#2524](https://github.com/jakepurple13/OtakuWorld/pull/2524))
- Fixed a crash when a package icon can't be found
  ([#2524](https://github.com/jakepurple13/OtakuWorld/pull/2524))
- Screensaver: fixed orientation handling and scaffold padding
  ([#2579](https://github.com/jakepurple13/OtakuWorld/pull/2579),
  [ce9b4ca80](https://github.com/jakepurple13/OtakuWorld/commit/ce9b4ca80))
- Fixed bundled JS extensions not building or packaging on Android
  ([#2511](https://github.com/jakepurple13/OtakuWorld/pull/2511))

### Desktop Only

#### New Features

- Biometric authentication using KSafe
  ([70dfd0a3d](https://github.com/jakepurple13/OtakuWorld/commit/70dfd0a3d))
- Desktop apps show their real app icon
  ([#2507](https://github.com/jakepurple13/OtakuWorld/pull/2507))

#### Improvements

- Encrypted credential storage via DataStore and Tink. Improved encryption key handling
  ([#2525](https://github.com/jakepurple13/OtakuWorld/pull/2525),
  [25db5ddca](https://github.com/jakepurple13/OtakuWorld/commit/25db5ddca),
  [52fb4ca73](https://github.com/jakepurple13/OtakuWorld/commit/52fb4ca73))

#### Bug Fixes

- Fixed packaged desktop apps failing to launch: the `jdk.unsupported` module is now included in
  the runtime image ([#2531](https://github.com/jakepurple13/OtakuWorld/pull/2531))

### Under the Hood

- Kotlin 2.4.20, Gradle 9.7.1, KSP 2.3.11, SDK 37.2, Java 21
  ([#2573](https://github.com/jakepurple13/OtakuWorld/pull/2573),
  [e850dba7e](https://github.com/jakepurple13/OtakuWorld/commit/e850dba7e),
  [#2525](https://github.com/jakepurple13/OtakuWorld/pull/2525))
- MaterialKolor 5.0.1 to 6.0.0-beta01
  ([#2591](https://github.com/jakepurple13/OtakuWorld/pull/2591))
- Haze to 2.0.1 ([45eb3173e](https://github.com/jakepurple13/OtakuWorld/commit/45eb3173e))
- Room 3.0.0 stable, supabase-kt 3.7.0, OkHttp 5.4.0, and other dependency updates
  ([#2509](https://github.com/jakepurple13/OtakuWorld/pull/2509),
  [#2517](https://github.com/jakepurple13/OtakuWorld/pull/2517),
  [#2522](https://github.com/jakepurple13/OtakuWorld/pull/2522),
  [377b49c0a](https://github.com/jakepurple13/OtakuWorld/commit/377b49c0a))
- Nucleus framework migrated to `dev.nucleusframework` 2.1.10
  ([8e1cbea83](https://github.com/jakepurple13/OtakuWorld/commit/8e1cbea83))
- Navigation migrated to Koin Navigation3 and a `Navigator` interface
  ([#2509](https://github.com/jakepurple13/OtakuWorld/pull/2509),
  [#2512](https://github.com/jakepurple13/OtakuWorld/pull/2512))
- Deep link handling centralized in a `DeepLinks` utility
  ([ed3fd7370](https://github.com/jakepurple13/OtakuWorld/commit/ed3fd7370))
- `KmpFirebaseConnection` removed
  ([#2506](https://github.com/jakepurple13/OtakuWorld/pull/2506))
- UI utilities and repositories moved to `sharedcomponents`. Koog integration split into
  platform-specific modules ([#2512](https://github.com/jakepurple13/OtakuWorld/pull/2512))
- New `kmpmodels:extensioninterfaces` module with the shared extension contract
  ([#2509](https://github.com/jakepurple13/OtakuWorld/pull/2509))
- iOS platform stubs added; the database migrated to Room Multiplatform
  ([#2535](https://github.com/jakepurple13/OtakuWorld/pull/2535))
- Large unit test suite for DAOs, repositories, and ViewModels. Unit tests now run in CI
  ([#2489](https://github.com/jakepurple13/OtakuWorld/pull/2489),
  [#2535](https://github.com/jakepurple13/OtakuWorld/pull/2535))
- Component showcase: a Compose Desktop dev tool for browsing `@ShowcaseComponent` composables
  ([#2540](https://github.com/jakepurple13/OtakuWorld/pull/2540))
- Baseline profiles and macrobenchmarks for all three apps
  ([#2486](https://github.com/jakepurple13/OtakuWorld/pull/2486))
- Custom `applicationIdSuffix` for Debug and Beta builds
  ([bfba05227](https://github.com/jakepurple13/OtakuWorld/commit/bfba05227))
- Desktop app initialization unified with a `desktopSetup` utility
  ([9504c589b](https://github.com/jakepurple13/OtakuWorld/commit/9504c589b))
- CI: desktop build workflows for all apps, desktop builds in nightly releases, and
  `actions/checkout` v7 ([#2507](https://github.com/jakepurple13/OtakuWorld/pull/2507),
  [#2512](https://github.com/jakepurple13/OtakuWorld/pull/2512),
  [#2560](https://github.com/jakepurple13/OtakuWorld/pull/2560))
- Version bumped to 33.0.0 ([#2486](https://github.com/jakepurple13/OtakuWorld/pull/2486))

---

## MangaWorld

### Android

#### New Features

- Native Mihon extension support. Installed Mihon/Tachiyomi extensions load directly, without the
  bridge app ([#2592](https://github.com/jakepurple13/OtakuWorld/pull/2592)):
  - Fetch Mihon extension repos, including `index_v2` repos
  - Extension trust gate with persisted trusted extensions
  - Settings screen for configurable Mihon sources
  - Cloudflare and WebView interceptors for protected sources
  - Legacy Tachiyomi bridges are skipped when native support is present
- Global search: choose which languages Mihon extensions search in
  ([#2607](https://github.com/jakepurple13/OtakuWorld/pull/2607))
- Mihon sources show their display names instead of internal ids across the app
  ([#2592](https://github.com/jakepurple13/OtakuWorld/pull/2592),
  [#2596](https://github.com/jakepurple13/OtakuWorld/pull/2596))

#### Improvements

- Beta builds use R8 minification and resource shrinking
  ([#2522](https://github.com/jakepurple13/OtakuWorld/pull/2522),
  [#2571](https://github.com/jakepurple13/OtakuWorld/pull/2571))
- OkHttp Brotli support for extensions, plus ProGuard rules for extension loading
  ([9caabf48f](https://github.com/jakepurple13/OtakuWorld/commit/9caabf48f))

#### Bug Fixes

- Fixed MangaDex crash: Brotli removed from the default client, `okhttp-zstd` shipped instead
  ([#2592](https://github.com/jakepurple13/OtakuWorld/pull/2592))
- Fixed global search crash with Mihon extensions (ProtoBuf now registered in Injekt)
  ([#2595](https://github.com/jakepurple13/OtakuWorld/pull/2595))
- Mihon: fixed crashes from partially initialized manga/chapter data, main-thread catalog loading,
  `libquickjs` collisions, and extensions failing under R8
  ([#2592](https://github.com/jakepurple13/OtakuWorld/pull/2592))
- Mihon: one bad extension no longer breaks the rest. The list reloads after an extension is
  uninstalled ([#2592](https://github.com/jakepurple13/OtakuWorld/pull/2592))
- Mihon: repo URLs must use https, and https-to-http redirects are blocked
  ([#2592](https://github.com/jakepurple13/OtakuWorld/pull/2592))
- Mihon: the source chooser saves the source key, not the display name
  ([#2592](https://github.com/jakepurple13/OtakuWorld/pull/2592))
- Activity time from the legacy reader is migrated and synced
  ([#2513](https://github.com/jakepurple13/OtakuWorld/pull/2513))

### Desktop

#### Improvements

- Distinct package name and per-platform app icons for Windows, macOS, and Linux installers
  ([#2507](https://github.com/jakepurple13/OtakuWorld/pull/2507))

#### Bug Fixes

- See [Shared > Desktop Only](#desktop-only) for the packaged-app launch fix

### All Platforms (shared manga code)

#### New Features

- Reader: bookmark chapters ([01b23d302](https://github.com/jakepurple13/OtakuWorld/commit/01b23d302))
- Reader: "Add to Dictionary" quick-add button in the top bar
  ([#2492](https://github.com/jakepurple13/OtakuWorld/pull/2492))

#### Improvements

- Downloads screen uses the Material 3 Expressive API and sorts chapters better
  ([b4fbd59b2](https://github.com/jakepurple13/OtakuWorld/commit/b4fbd59b2))
- Reader pages animate size changes
  ([cf06671d7](https://github.com/jakepurple13/OtakuWorld/commit/cf06671d7))

#### Bug Fixes

- Reader: the current chapter stays in sync while reading
  ([#2570](https://github.com/jakepurple13/OtakuWorld/pull/2570))

---

## AnimeWorld

### Android

#### Improvements

- Video library, video player, and quality-choice screens moved to shared code. ExoPlayer stays
  the Android player ([#2505](https://github.com/jakepurple13/OtakuWorld/pull/2505))

#### Bug Fixes

- Fixed a possible crash in the video library from `Handler()` Looper use
  ([#2505](https://github.com/jakepurple13/OtakuWorld/pull/2505))
- Activity time from the legacy player is migrated and synced
  ([#2513](https://github.com/jakepurple13/OtakuWorld/pull/2513))

### Desktop

#### New Features

- **New: AnimeWorld Desktop app** (`:animeworld:desktop`)
  ([#2505](https://github.com/jakepurple13/OtakuWorld/pull/2505)):
  - Browse, details, favorites, and the other shared screens
  - Desktop download manager
  - Video playback is not supported yet; a placeholder screen explains this
- Packaged installers for Windows, macOS, and Linux with app icons
  ([#2507](https://github.com/jakepurple13/OtakuWorld/pull/2507))

---

## NovelWorld

### Android

No user-facing app-specific changes. Shared logic moved into the new `:novelworld:shared`
module ([#2505](https://github.com/jakepurple13/OtakuWorld/pull/2505))

### Desktop

#### New Features

- **New: NovelWorld Desktop app** (`:novelworld:desktop`), including the novel reader
  ([#2505](https://github.com/jakepurple13/OtakuWorld/pull/2505))
- Packaged installers for Windows, macOS, and Linux with app icons
  ([#2507](https://github.com/jakepurple13/OtakuWorld/pull/2507))
