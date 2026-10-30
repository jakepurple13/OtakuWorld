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
- **Native Aniyomi and CloudStream extensions in AnimeWorld (Android).** Aniyomi extension APKs
  and CloudStream `.cs3` plugins load directly, without a bridge app
  ([#2611](https://github.com/jakepurple13/OtakuWorld/pull/2611),
  [#2612](https://github.com/jakepurple13/OtakuWorld/pull/2612))
- **Subtitle styling and offline subtitles in AnimeWorld.** A subtitle settings screen and an
  in-player quick sheet, plus HLS episode downloads that keep their subtitles
  ([#2613](https://github.com/jakepurple13/OtakuWorld/pull/2613))
- **Redesigned AnimeWorld Downloads screen** with live progress, failed downloads you can retry,
  and readable episode titles ([#2614](https://github.com/jakepurple13/OtakuWorld/pull/2614))
- **Cloud sync keeps each app's data separate.** Requires running a one-time SQL migration on
  existing Supabase projects; see `docs/supabase/SETUP.md`
  ([#2616](https://github.com/jakepurple13/OtakuWorld/pull/2616))

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
- Add to List: pick several lists at once. Lists that already have the title are disabled, and a
  new list can be created and checked right in the sheet
  ([#2625](https://github.com/jakepurple13/OtakuWorld/pull/2625))
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
- New Options Sheet UI to allow for better accessibility
  ([#2620](https://github.com/jakepurple13/OtakuWorld/pull/2620))
- Refreshed onboarding: a Sources page that explains extensions and sets preferred languages, more
  appearance options, chapter swipe actions, a biometric lock shortcut, and a Tips & Gestures page
  where you can try hidden gestures. The tips also live under Settings › Discover, and each app
  adds its own (MangaWorld reader, AnimeWorld player, NovelWorld reader)
  ([#2627](https://github.com/jakepurple13/OtakuWorld/pull/2627))
- Details screen chapter multi-select: long-press a chapter to start selecting, then mark as read
  or unread, bookmark, download or delete downloads, select all, or invert the selection from a
  floating toolbar. Read and Share appear when one chapter is selected. Marking and bookmarking can
  be undone from the snackbar ([#2641](https://github.com/jakepurple13/OtakuWorld/pull/2641))
- Details screen transition: opening an item from a cover or list row flies its cover and title
  into the Details header, and back on return
  ([#2643](https://github.com/jakepurple13/OtakuWorld/pull/2643))
- Global search: tap a source to open its own results screen and keep scrolling to load more pages.
  Pages are marked with dividers, a failed page can be retried, and returning from Details keeps
  your place. This replaces the source results bottom sheet
  ([#2646](https://github.com/jakepurple13/OtakuWorld/pull/2646))

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
- Details screen opens faster: the cover shows right away from the screen you came from, its
  colors are worked out in the background, and the colors stay when you rotate
  ([#2643](https://github.com/jakepurple13/OtakuWorld/pull/2643))
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
- The options sheet now slides up the first time it opens instead of appearing instantly
  ([#2626](https://github.com/jakepurple13/OtakuWorld/pull/2626))
- Fixed Settings deep links ([#2507](https://github.com/jakepurple13/OtakuWorld/pull/2507))
- Cloud sync: MangaWorld, AnimeWorld, and NovelWorld signed into the same account no longer share
  favorites, history, lists, and other synced data. Each app now keeps its own data and its own
  cloud backups. Existing projects must run `docs/supabase/migration_add_app_column.sql` after
  updating every app; until then sync shows a "schema is out of date" error. Each app re-uploads
  its local data on its first sync after the migration. Cloud backups made before this change no
  longer appear in restore ([#2616](https://github.com/jakepurple13/OtakuWorld/pull/2616))

#### Removed

- AI recommendation providers, settings, and screens
  ([#2509](https://github.com/jakepurple13/OtakuWorld/pull/2509))
- Custom server sync ([0e723f117](https://github.com/jakepurple13/OtakuWorld/commit/0e723f117))
- In-app WebView screen ([d3843280b](https://github.com/jakepurple13/OtakuWorld/commit/d3843280b))
- Details screen: the Mark As drawer, the Batch Download sheet, and the chapter long-press options
  sheet, replaced by chapter multi-select
  ([#2641](https://github.com/jakepurple13/OtakuWorld/pull/2641))

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
- Source contract additions in `kmpmodels`: `KmpStorage` gains `subtitles`, `server`, `mimeType`,
  and `extras`; `KmpApiService` gains `canCast` and `resolve()`; external catalogs gain
  `install`/`uninstall` hooks ([#2611](https://github.com/jakepurple13/OtakuWorld/pull/2611))
- Build guard: an app can't depend on both `:mihonsupport` and `:source_utilities:aniyomi`,
  because they share `eu.kanade.tachiyomi.network.*`
  ([#2611](https://github.com/jakepurple13/OtakuWorld/pull/2611))

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
- Smaller release APK, from 73.2 MB to 32.6 MB: code and native libraries are compressed, and
  unused desktop-only libraries are no longer bundled
  ([#2624](https://github.com/jakepurple13/OtakuWorld/pull/2624))
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

#### New Features

- MangaWorld Desktop: install and use Mihon extensions from Mihon repos (non-Cloudflare sources;
  source settings not yet available on desktop)

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

#### New Features

- **Native Aniyomi extension support** (`:source_utilities:aniyomi`)
  ([#2611](https://github.com/jakepurple13/OtakuWorld/pull/2611)):
  - Aniyomi anime extension APKs load directly, without a bridge app, the same way MangaWorld
    loads Mihon extensions
  - Repo client for legacy `index.min.json` and `index_v2`, a signing-key trust gate, and a loader
    for extension lib versions 14, 16, and 17
  - Episodes, hosters, and videos map from the extension, including lazy hosters and the
    extension's local proxy server
  - Per-extension settings open from the gear on the extension
  - Aniyomi sources show up on the Extensions screen, in the installed list, global search, and
    the language sheet
- **Native CloudStream plugin support** (`:source_utilities:cloudstream`, GPL-3.0; AnimeWorld
  only) ([#2612](https://github.com/jakepurple13/OtakuWorld/pull/2612)):
  - CloudStream `.cs3` plugins download from a repo, are checked against their SHA-256, and load
    from app-private storage
  - Trust is per repo; plugins from an untrusted repo are stored but never loaded until you trust
    the repo
  - A plugin that fails to load becomes a placeholder instead of crashing the app
  - Plugin settings open from the extension
- Subtitles for extension streams ([#2611](https://github.com/jakepurple13/OtakuWorld/pull/2611)):
  - Sidecar subtitle tracks load with the video, and a default track is picked (your device
    language as a whole word, then English, preferring full dialogue over signs/songs tracks)
  - CC menu in the player's top bar to switch tracks or turn subtitles off
- The quality picker groups streams by server and resolves lazy links before playing, with
  progress and a message if a link can't be resolved
  ([#2611](https://github.com/jakepurple13/OtakuWorld/pull/2611))
- **Subtitle settings** (Settings › Player › Subtitles)
  ([#2613](https://github.com/jakepurple13/OtakuWorld/pull/2613)):
  - Size, font, text color, edge style, background opacity, and how far subtitles sit above the
    bottom, with a live preview
  - Use embedded styles, turn subtitles on automatically, preferred languages, and skip
    signs/songs tracks
  - Uses your dynamic theme colors
- **In-player subtitle sheet**: tap CC to switch tracks, change the size, toggle the background
  box, or jump to the full settings. A side panel in landscape and a bottom sheet in portrait
  ([#2613](https://github.com/jakepurple13/OtakuWorld/pull/2613))
- **Episode downloads for Aniyomi and CloudStream sources**
  ([#2613](https://github.com/jakepurple13/OtakuWorld/pull/2613)):
  - HLS streams download, including AES-128 encrypted streams and fMP4 segments. Streams the
    downloader can't handle fail with a clear message
  - Subtitle tracks download next to the episode, and offline playback picks them up
  - Downloads run in the background with a progress notification and a Cancel button
  - Partial files are never left behind as finished episodes
  - Deleting a downloaded episode also deletes its subtitle files
- **Redesigned Downloads screen** ([#2614](https://github.com/jakepurple13/OtakuWorld/pull/2614)):
  - Each row shows the show, then "Episode N · Name", then a CC tag with subtitle languages and
    the file size. The raw file path is no longer shown
  - Downloading section with live progress and Cancel
  - Failed downloads stay listed, even after restarting the app, with the reason, Retry, and
    Remove
  - Your downloads are separate from other videos on the phone, which sit in a collapsible
    "Other videos on this phone" section
- **Rebuilt casting to Chromecast and Google TV**:
  - Cast button in the player's top bar: pick a TV and the episode continues there from the
    current position, with the selected subtitle track (an embedded track switches to the
    closest matching subtitle file)
  - While connected, playing an episode from the details page or the Downloads screen sends it
    straight to the TV, starting at your saved position
  - Aniyomi and CloudStream episodes can be cast. Streams that need headers, and downloaded
    episodes, are served to the TV from the phone over Wi-Fi
  - Subtitles on the TV use your subtitle style settings
  - New remote screen with seek, play/pause, subtitle tracks, and volume, plus a mini player
    bar and a media notification while casting. The phone's volume keys control the TV
  - MKV episodes and `.ts` downloads show "This episode can't be cast"; other failures show a
    message
  - The old cast controls and local file server library were removed
  ([#2631](https://github.com/jakepurple13/OtakuWorld/pull/2631))
- Watch progress on the Downloads screen: a border traces each thumbnail clockwise as you watch
  ([#2629](https://github.com/jakepurple13/OtakuWorld/pull/2629))
- **+1:30 button** in the player's controls to skip ahead a minute and a half, e.g. past an
  opening ([#2629](https://github.com/jakepurple13/OtakuWorld/pull/2629))
- **Blurred player controls**: the video player's top and bottom bars use your selected blur
  kind. Turn it off with Settings › Player › Blur Player Controls
  ([#2630](https://github.com/jakepurple13/OtakuWorld/pull/2630))

#### Improvements

- Video library, video player, and quality-choice screens moved to shared code. ExoPlayer stays
  the Android player ([#2505](https://github.com/jakepurple13/OtakuWorld/pull/2505))
- The player sends every header a stream needs and uses the stream's MIME type when the source
  gives one ([#2611](https://github.com/jakepurple13/OtakuWorld/pull/2611))
- Installed badges on the Extensions screen match by catalog, so a source installed from one
  catalog no longer shows as installed in another
  ([#2613](https://github.com/jakepurple13/OtakuWorld/pull/2613))
- Downloading asks for All files access first (Android 11+) or storage permission (Android 9–10)
  instead of failing ([#2613](https://github.com/jakepurple13/OtakuWorld/pull/2613))
- Newly downloaded episodes appear at the top of the video library
  ([#2613](https://github.com/jakepurple13/OtakuWorld/pull/2613))
- The video library loads off the main thread
  ([#2614](https://github.com/jakepurple13/OtakuWorld/pull/2614))
- The screen stays on while the video player is open ([#2629](https://github.com/jakepurple13/OtakuWorld/pull/2629))
- Brightness and volume swipes are finer: a full-height swipe covers the whole range, and the
  on-screen bars show the real level ([#2629](https://github.com/jakepurple13/OtakuWorld/pull/2629))

#### Bug Fixes

- Fixed a possible crash in the video library from `Handler()` Looper use
  ([#2505](https://github.com/jakepurple13/OtakuWorld/pull/2505))
- Fixed a player crash on close, a rotation loop when the player forces landscape, and the
  volume/brightness baseline being lost on rotation
  ([#2611](https://github.com/jakepurple13/OtakuWorld/pull/2611))
- Subtitles are no longer pushed off-screen in zoom mode
  ([#2611](https://github.com/jakepurple13/OtakuWorld/pull/2611))
- Fixed a crash when tapping Download on an episode (the old download dialog needed an AppCompat
  theme) ([#2613](https://github.com/jakepurple13/OtakuWorld/pull/2613))
- Deleting a download on Android 9–10 now removes the video, not just its subtitles
  ([#2613](https://github.com/jakepurple13/OtakuWorld/pull/2613))
- Thumbnails on the Downloads screen load again ([#2629](https://github.com/jakepurple13/OtakuWorld/pull/2629))
- Saved watch progress now shows on the Downloads screen and refreshes after you leave the player
  ([#2629](https://github.com/jakepurple13/OtakuWorld/pull/2629))
- The player no longer zooms in a moment after a video starts; the whole frame stays visible
  ([#2629](https://github.com/jakepurple13/OtakuWorld/pull/2629))
- Brightness and volume swipes no longer jump straight from 0 to 100 ([#2629](https://github.com/jakepurple13/OtakuWorld/pull/2629))
- Download thumbnails no longer crowd out the title (width and height were swapped)
  ([#2614](https://github.com/jakepurple13/OtakuWorld/pull/2614))
- Activity time from the legacy player is migrated and synced
  ([#2513](https://github.com/jakepurple13/OtakuWorld/pull/2513))

### Desktop

#### New Features

- **New: AnimeWorld Desktop app** (`:animeworld:desktop`)
  ([#2505](https://github.com/jakepurple13/OtakuWorld/pull/2505)):
  - Browse, details, favorites, and the other shared screens
  - Desktop download manager
- AnimeWorld Desktop: install and use Aniyomi extensions from Aniyomi repos (non-Cloudflare
  sources; source settings not yet available on desktop)
- AnimeWorld Desktop: install and use CloudStream plugins from CloudStream repos, next to Aniyomi
  extensions (repos must be trusted first; plugins that need Android resources or a WebView, and
  plugin settings, are not available on desktop)
- AnimeWorld Desktop: in-app video player with libVLC bundled in the app (no VLC install needed).
  Seek, skip buttons, keyboard shortcuts, volume, fullscreen, and a video choice when an episode
  has several streams
- AnimeWorld Desktop: subtitles with external and embedded tracks, and an in-player settings dialog
  for style (shares the Android subtitle style options)
- AnimeWorld Desktop: episode downloads for progressive, HLS, and DASH streams, saved as one
  playable file with subtitles kept, with progress, cancel, and retry in the downloads screen
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
