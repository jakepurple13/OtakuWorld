# Desktop Auto-Update (MangaWorld, AnimeWorld, NovelWorld)

Date: 2026-10-30
Status: UI approved (HTML preview); design ready for approval
Preview: https://claude.ai/artifact/81BFGcm9jFBYtiE2sGDiiH (local copy kept out of repo)

## Intent

The three desktop apps ship as DMG/MSI/DEB from CI, but users have no way to get a new build except
re-downloading by hand. Add in-app auto-update with Nucleus (`nucleus.updater-runtime`) so desktop
users, including nightly users, are told about new builds and can install them in two clicks.

Nucleus docs: packaging/auto-update, packaging/publishing, packaging/ci-cd on nucleusframework.dev.

## Goals

- Each desktop app checks for its own update **on launch** and **when the user presses "Check now"**.
- Launch check that finds an update **pops up a dialog** over whatever screen is open.
- An **"Updates" group in Desktop Settings** (`PlatformSettings` screen) in all three apps.
- **Nightly builds get updates** (nightly and stable). Stable builds get stable only.
- CI builds, packages, and publishes update metadata for all three apps into the existing GitHub
  releases (`v*` tags and `nightly_YYYY_MM_DD` prereleases) without the apps colliding.

## Non-goals

- Code signing / notarization. **Ship unsigned first** (user decision). macOS Gatekeeper and Windows
  SmartScreen warnings are accepted for now.
- Linux passwordless (GPG) silent updates. DEB updates will prompt via `pkexec`.
- Store distribution (Mac App Store, MS Store, Snap, Flathub).
- Updating the Android APK flow in `MoreInfoScreen`. Desktop does not change More Info.
- Any other Android change. The pre-release screen filter (below) is the only Android-visible change.
- ARM runners (`ubuntu-24.04-arm`, `windows-11-arm`) and macOS universal binaries. Can be added later.
- Replacing the Compose Desktop ProGuard setup. Packaging keeps the non-ProGuard task.

## Anti-goals

- One app's update metadata overwriting another's in the shared release. Any setup where MangaWorld
  could download AnimeWorld's installer is a failure.
- An update check that runs or crashes in dev runs (`./gradlew run`, `hotRun`).
- A ProGuard'd release build that breaks runtime-loaded extensions (Mihon / Aniyomi / CloudStream
  load classes by reflection).

## Current state (facts from the repo)

- Root `build.gradle.kts:114` already declares `id("dev.nucleusframework")` (`apply false`), version
  `nucleusSystem = "2.5.18"` in `gradle/desktop.versions.toml`.
- `kmpuiviews` jvmMain already depends on `nucleus.updater-runtime`, `nucleus.core-runtime`, etc.
  (`kmpuiviews/build.gradle.kts:215-224`). `DesktopUi.kt` already imports `NucleusApplicationScope`.
- The three `*/desktop/build.gradle.kts` use `compose.desktop.application { }` with
  `TargetFormat.Dmg, Msi, Deb` and `packageVersion = "1.0.0"` hard-coded.
- Shared entry point: `desktopSetup(...)` in `kmpuiviews/src/jvmMain/.../DesktopUi.kt`, called from
  each app's `main.kt`.
- Desktop prefs live in `MangaDesktopSettings` (`kmpuiviews/src/jvmMain/.../Platform.jvm.kt:257`),
  a `DataStoreHandler` holder used by all three apps.
- Each app has its own `PlatformSettings.kt` with `JvmSettingsScreen()`.
- CI: `{manga,anime,novel}world_desktop_build.yaml` (reusable, `packageDistributionForCurrentOS`,
  ubuntu/macos/windows). Desktop jobs run only in `nightly_release.yaml`, which uploads everything with
  `softprops/action-gh-release` to tag `nightly_<date>` as a prerelease. `main_release.yml` (`v*`)
  builds Android only. App version base: `AppInfo.otakuVersionName = "33.0.0"`.

## Decisions

### Channel trick (user decision), as verified against Nucleus v2.5.18 source

- `UpdaterConfig.channel` is a free `String`; the metadata file is `$channel.yml` (Windows),
  `$channel-mac.yml`, `$channel-linux.yml`. Each app uses its own channel: `mangaworld`,
  `animeworld`, `novelworld`.
- The runtime fetches only that channel's file. No fallback to `latest-*.yml`.
- Prerelease versions are allowed automatically when `currentVersion` contains `-`.
- **`GitHubProvider` cannot be used**: for non-`latest` channels it only matches prerelease tags like
  `v1.0-<channel>` and builds download URLs from `v$version`. Neither fits our tags.
- Both tracks use one custom `UpdateProvider` (public Nucleus interface). *(Builder-check call:
  `GenericProvider` on `/releases/latest/download` was dropped, because one stable release with
  missing desktop assets would break every stable user.)*
  - List `GET https://api.github.com/repos/jakepurple13/OtakuWorld/releases?per_page=100`, paginating
    as needed.
  - Stable candidate: newest non-prerelease whose tag matches `^v\d+\.\d+\.\d+$` and that has a
    `<channel>-<os>.yml` asset. Old tags like `v32.0.0_test` are ignored.
  - Nightly candidate: newest prerelease whose tag starts with `nightly_` and that has that asset.
  - Stable track reads only the stable candidate. Nightly track reads both ymls and picks the higher
    version (Nucleus `Version` ordering: `33.0.0-alpha.X < 33.0.0`).
  - Downloads resolve to `.../releases/download/<thatTag>/<file>`.
  - Track is chosen at startup: nightly if the running version contains `-`, else stable.
- CI only uploads an app's ymls when every matrix build for that app succeeded, so a release never
  has a yml without its installers. A failed desktop build does not block the release; that app's
  users just stay on the previous release, because the provider skips releases without the asset.

### Versioning

| Build | Tag | Top-level `packageVersion` | Track |
|---|---|---|---|
| Stable | `v33.0.0` | `33.0.0` | stable |
| Nightly | `nightly_2026_10_30` | `33.0.0-alpha.20261030` | nightly |

- CI passes `DESKTOP_VERSION`. Local builds fall back to `AppInfo.otakuVersionName`.
- Nucleus validation rejects a `-` suffix for DMG/PKG and NSIS/MSI/EXE. Set the top-level
  `nativeDistributions.packageVersion` to the full version (the runtime reads this via
  `nucleus-app.properties`), and set `macOS.packageVersion` and the Windows package version to the
  numeric part (`33.0.0`). DEB, AppImage, and ZIP accept the full version.
- Nightly suffix is `alpha.<YYYYMMDD>.<HHMM>` (UTC), all numeric so it sorts correctly. Same-day
  reruns reuse the `nightly_<date>` tag but get a higher version, so users still see the update.
  The table above shortens it.
- *My call:* nightly base = `AppInfo.otakuVersionName`, unless that is <= the latest stable `v*` tag.
  Then CI uses latest stable with the minor version bumped by one (`33.0.0` becomes `33.1.0`). Without
  this, nightlies sort below the stable release until someone bumps the constant.

### Gradle (per desktop module)

- Apply `id("dev.nucleusframework")` next to the existing Compose plugin.
- Rename `compose.desktop.application { }` to `nucleus.application { }`.
- Switch `TargetFormat` import to `dev.nucleusframework.desktop.application.dsl.TargetFormat`.
- Targets: `Dmg, Zip` (macOS needs both), `Nsis` (Windows; replaces MSI), `Deb, AppImage` (Linux).
  No one has installed a desktop build yet (user), so no MSI-to-NSIS migration is needed.
- `homepage = "https://github.com/jakepurple13/OtakuWorld"` (required for DEB).
- No `publish { }` block. The `latest*.yml` files the package tasks write on their own are not uploaded.
- Keep `packageDistributionForCurrentOS` (no ProGuard). Do not switch to the `Release` task.
- AnimeWorld's `appResourcesRootDir` / `prepareVlc` wiring stays as-is.

### Runtime (shared, `kmpuiviews` jvmMain)

- One `DesktopUpdater` singleton in Koin, created in `desktopSetup` from the app `name` (channel =
  `name.lowercase()`).
- It exposes a `StateFlow` with the states shown in the preview: Idle, Checking, UpToDate,
  Available(info, level), Downloading(percent, bytes, total), Ready(file), Error(message),
  Unsupported. Download progress survives leaving the screen.
- `isUpdateSupported() == false` (dev runs) sets Unsupported. No check runs, and the rows are disabled.
- **Launch check:** runs once after the window opens, if "Check automatically" is on.
- **Launch dialog:** shown once per launch when the launch check returns Available and that version
  is not the skipped version. Steps: Update found, then Downloading, then Ready to restart.
  - Update found: "Skip this version", "Later", "Download", and a "Download from GitHub" link that
    opens the found release's GitHub page in the browser (the page also carries the release notes).
  - Downloading: "Cancel", "Hide" (download continues).
  - Ready: "Later", "Restart now" (`installAndRestart`).
- **"Check now"** in Settings always checks, ignoring the skipped version, and never opens the
  launch dialog. Results show inline in the row.
- **Just updated:** on startup, `consumeUpdateEvent()` non-null shows a one-time "Updated to X"
  snackbar.

### Settings UI (Desktop Settings, all three apps)

New "Updates" group after the existing rows, matching the preview:

1. **Check for updates** row: icon, title, status line, progress bar while downloading, current
   version, and a trailing action (Check now / spinner / Download / Cancel / Restart / Retry).
   Highlighted when an update is available, downloading, or ready. Error tint on failure.
2. **Check automatically** toggle, default on. Subtitle: "Look for updates each time the app starts".
3. **Update track** (read-only): "Nightly · Gets nightly and stable releases" or
   "Stable · Gets stable releases only. Set by the build you installed."
4. **Download from GitHub** row, always enabled (also in dev runs and on error). Opens the release
   page in the browser: the release found by the last check if there is one, else the newest release
   for the track (stable: `https://github.com/jakepurple13/OtakuWorld/releases/latest`; nightly: the
   newest `nightly_*` release, falling back to `/releases`).

**Fallback (user decision):** "Download from GitHub" is the manual path whenever the in-app install
fails, including unsigned macOS if Gatekeeper blocks the relaunch. No custom macOS install code.

The group is one shared composable (e.g. `UpdatesSettingsGroup()` in kmpuiviews jvmMain) that each
app's `JvmSettingsScreen()` calls.

New prefs in `MangaDesktopSettings`: `checkUpdatesOnLaunch` (Boolean, default true) and
`skippedUpdateVersion` (String, default "").

### Pre-release screen filter (shared, affects Android)

`PrereleaseScreen` (`kmpuiviews/commonMain/.../settings/prerelease/PrereleaseScreen.kt:96`) lists every
asset of the newest GitHub release. With this design the nightly gains ~30 desktop files (ZIP,
AppImage, NSIS, second macOS build, `.blockmap`, `.yml`). Filter the list by platform (user decision):

- **Android:** only assets ending in `.apk`. All three apps' APKs still show, the same as today.
- **Desktop:** *my call* — only this app's installers for the current OS: name starts with the app's
  package name (`MangaWorld`, `AnimeWorld`, `NovelWorld`), and the extension is `.dmg`/`.zip` on macOS,
  `.exe` on Windows, `.deb`/`.AppImage` on Linux. Never `.blockmap` or `.yml`.
- The filter runs in the view/repository layer, platform-specific (e.g. an `expect`/`actual`
  predicate in `kmpuiviews`). Releases and download logic are unchanged.

### CI

- One reusable workflow `desktop_release.yaml`, inputs `app` (mangaworld | animeworld | novelworld)
  and `version`. It replaces the three `*_desktop_build.yaml` files.
- Build matrix: `ubuntu-latest`, `windows-latest`, `macos-latest` (arm64), `macos-15-intel` (x64).
  `NucleusFramework/Nucleus/.github/actions/setup-nucleus@v2.5.18` with JDK 21 and packaging tools.
  Run `:<app>:desktop:packageDistributionForCurrentOS` with `DESKTOP_VERSION`. Timeout 45 min.
- Artifacts named `<app>-release-assets-<os>-<arch>`. Installer filenames must be unique per app
  and arch (packageName already differs per app; arch must be in the name for the two mac builds).
- Metadata job per app: run `generate-update-yml` on that app's artifacts only. The action maps any
  channel to `latest`, so rename `latest*.yml` to `<app>*.yml` afterwards. The macOS yml must list
  both arm64 and x64 files.
- `nightly_release.yaml`: compute `DESKTOP_VERSION` as described under Versioning, call `desktop_release.yaml` for the three apps, and upload
  installers, `.blockmap` files, and the renamed ymls with the existing release step.
- `main_release.yml`: add the three desktop calls with `DESKTOP_VERSION` from the tag (`v33.0.0`
  becomes `33.0.0`) and a release step that uploads desktop assets to that tag's release.

## Left to the builder

- Exact class and file names, and whether the provider lives in `kmpuiviews` or a new small module.
- How the dialog and snackbar are hosted inside `BaseDesktopUi`.
- String resources and wording beyond what the preview shows.
- Unit tests: the provider's release selection (nightly vs stable, missing assets) and the
  `DesktopUpdater` state machine are the parts worth covering. The GitHub API can be faked.
- GitHub API rate limit (60/hour unauthenticated) is fine for one launch check plus manual checks.

## Unverified / risks

- Whether electron-builder itself accepts a prerelease version for ZIP/DEB/AppImage. Only the
  Nucleus plugin validation was read.
- Unsigned macOS updates: `open <dmg>` / ZIP extraction on an ad-hoc signed app may trigger
  Gatekeeper on relaunch. Needs a real install test, but it does not block merge: "Download from
  GitHub" covers it.
- If `main_release.yml` and `nightly_release.yaml` change, the first real nightly is the first
  end-to-end test.
