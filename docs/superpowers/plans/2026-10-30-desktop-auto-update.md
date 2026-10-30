# Desktop Auto-Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** MangaWorld, AnimeWorld, and NovelWorld desktop apps check GitHub for their own new build on launch and on demand, then download, install, and restart via Nucleus. CI publishes per-app update metadata into the shared releases.

**Architecture:** A custom Nucleus `UpdateProvider` (`OtakuReleaseProvider`) picks the right GitHub release per app channel and track. A Koin singleton `DesktopUpdater` wraps `NucleusUpdater` behind a small `UpdaterEngine` interface and exposes a `StateFlow` the Settings group and the launch dialog render. CI builds each app on 4 runners, generates `latest*.yml` with Nucleus's action, renames them to `<app>*.yml`, and uploads them with the existing release steps.

**Tech Stack:** Kotlin 2.4.21, Compose Multiplatform 1.13.0-beta01, Nucleus 2.5.18 (`dev.nucleusframework` plugin, `nucleus.updater-runtime`), Koin, kotlinx.serialization, DataStore, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-10-30-desktop-auto-update-design.md`

**UI reference:** https://claude.ai/artifact/81BFGcm9jFBYtiE2sGDiiH (approved preview: launch dialog + Desktop Settings group)

## Global Constraints

- Nucleus version comes from `desktopLibs.versions.nucleusSystem` (`2.5.18`); never hard-code a second version.
- Channel per app = app name lowercased: `mangaworld`, `animeworld`, `novelworld`. Metadata files: `<channel>.yml` (Windows), `<channel>-mac.yml`, `<channel>-linux.yml`.
- Repo: owner `jakepurple13`, repo `OtakuWorld`.
- Stable tags match `^v\d+\.\d+\.\d+$`. Nightly tags start with `nightly_` and are GitHub prereleases.
- Nightly version: `<base>-alpha.<YYYYMMDD>.<HHMM>` (UTC). Stable version: tag without `v`.
- Track: version contains `-` means Nightly (gets nightly and stable), else Stable (stable only).
- Package with `packageDistributionForCurrentOS`. Never the `Release` (ProGuard) variant.
- Targets: `Dmg, Zip, Nsis, Deb, AppImage`. `homepage = "https://github.com/jakepurple13/OtakuWorld"`.
- macOS and Windows package versions = numeric part only (`33.0.0`). Top-level `packageVersion` = full version.
- No code signing. No Android code changes except the pre-release asset filter (Task 6).
- Every lazy item gets `Modifier.animateItem()` (project rule).
- `Unsupported` (dev runs: `./gradlew run`, `hotRun`): no check runs, update rows disabled, "Download from GitHub" still enabled.

## Review Focus

1. **Offline / GitHub API 403 at launch:** launch check fails quietly (no dialog, no crash). Settings row shows the error with Retry. Test: Task 3 `launchCheckErrorShowsNoDialog`.
2. **Release has another OS's yml but not this one (one desktop leg failed):** provider skips it and falls back to the previous release with this OS's yml. Test: Task 2 `stableSkipsReleaseMissingThisPlatformsYml`.
3. **User skipped version X, then X+1 ships:** the launch dialog appears for X+1. Test: Task 3 `skippedVersionOnlyHidesThatVersion`.
4. **"Check now" pressed while a download runs:** ignored; download continues. Test: Task 3 `checkNowDuringDownloadIsIgnored`.
5. **Nightly user when no nightly exists yet, or stable is newer:** offered the stable release. Tests: Task 2 `nightlyFallsBackToStableWhenNoNightly`, `nightlyPicksStableWhenHigher`.

---

## File Structure

| File | Responsibility |
|---|---|
| `{mangaworld,animeworld,novelworld}/desktop/build.gradle.kts` | Nucleus plugin, targets, versions |
| `kmpuiviews/src/jvmMain/.../updates/UpdateTrack.kt` | Track enum + `fromVersion` |
| `kmpuiviews/src/jvmMain/.../updates/ReleaseSelection.kt` | `GhRelease` model + pure selection functions |
| `kmpuiviews/src/jvmMain/.../updates/OtakuReleaseProvider.kt` | Nucleus `UpdateProvider` over the GitHub API |
| `kmpuiviews/src/jvmMain/.../updates/UpdaterEngine.kt` | Interface + `NucleusUpdaterEngine` |
| `kmpuiviews/src/jvmMain/.../updates/UpdatePrefs.kt` | Interface + DataStore impl over `MangaDesktopSettings` |
| `kmpuiviews/src/jvmMain/.../updates/DesktopUpdater.kt` | State machine, launch dialog flag, release page URL |
| `kmpuiviews/src/jvmMain/.../updates/UpdatesSettingsGroup.kt` | Settings rows (LazyListScope extension) |
| `kmpuiviews/src/jvmMain/.../updates/UpdateLaunchDialog.kt` | Launch dialog + just-updated snackbar |
| `kmpuiviews/src/jvmMain/.../Platform.jvm.kt` | 2 new prefs in `MangaDesktopSettings` |
| `kmpuiviews/src/jvmMain/.../DesktopUi.kt` | Koin registration, launch check, dialog host |
| `*/desktop/src/jvmMain/.../PlatformSettings.kt` (×3) | Call `updatesSettingsGroup()` |
| `kmpuiviews/src/commonMain/.../prerelease/PrereleaseAssetFilter.kt` | Pure filter + `expect val currentAssetPlatform` |
| `kmpuiviews/src/{androidMain,jvmMain,iosMain}/.../prerelease/PrereleaseAssetFilter.*.kt` | `actual`s |
| `.github/scripts/desktop-version.sh` | Computes `DESKTOP_VERSION` |
| `.github/scripts/rename-update-yml.sh` | `latest*.yml` to `<app>*.yml`, gated on 4 legs |
| `.github/workflows/desktop_release.yaml` | Reusable build + metadata workflow |
| `.github/workflows/{nightly_release.yaml,main_release.yml}` | Call it, upload |

`...` = `com/programmersbox/kmpuiviews`. New package `com.programmersbox.kmpuiviews.updates`. Tests mirror it under `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/updates/`.

---

### Task 1: Nucleus packaging for the three desktop apps

**Files:**
- Modify: `mangaworld/desktop/build.gradle.kts`, `animeworld/desktop/build.gradle.kts`, `novelworld/desktop/build.gradle.kts` (the `compose.desktop { application { ... } }` blocks and the `TargetFormat` import)

**Interfaces:**
- Produces: env var contract `DESKTOP_VERSION` (full version), read at configuration time; fallback `AppInfo.otakuVersionName`. Installer outputs under `<app>/desktop/build/compose/binaries/main/<format>/`.

- [ ] **Step 1: Apply plugin and rename block in all three modules**

Add `id("dev.nucleusframework")` to `plugins {}` (root already declares the version with `apply false`). Change import to `dev.nucleusframework.desktop.application.dsl.TargetFormat`. Replace `compose.desktop {\n    application {` with `nucleus.application {` (one level less nesting). Everything else inside stays, including AnimeWorld's `appResourcesRootDir` and the `tasks.named { it == "prepareAppResources" || it == "run" }` hook.

- [ ] **Step 2: Set targets, homepage, versions**

```kotlin
val desktopVersion = System.getenv("DESKTOP_VERSION") ?: AppInfo.otakuVersionName
val numericVersion = desktopVersion.substringBefore('-')
nativeDistributions {
    targetFormats(TargetFormat.Dmg, TargetFormat.Zip, TargetFormat.Nsis, TargetFormat.Deb, TargetFormat.AppImage)
    packageVersion = desktopVersion
    homepage = "https://github.com/jakepurple13/OtakuWorld"
    macOS { packageVersion = numericVersion /* + existing iconFile */ }
    windows { packageVersion = numericVersion /* + existing iconFile */ }
    // packageName, modules, linux icon unchanged
}
```

If the Nucleus DSL names the per-OS version properties differently (e.g. `dmgPackageVersion`, `exePackageVersion`), use those. The validator in `validatePackageVersions.kt` must pass for a `-alpha` top-level version.

- [ ] **Step 3: Package with a nightly-style version**

Run: `DESKTOP_VERSION=33.0.0-alpha.20261030.1415 ./gradlew :mangaworld:desktop:packageDistributionForCurrentOS`
Expected: BUILD SUCCESSFUL. On macOS, both `binaries/main/dmg/*.dmg` and `binaries/main/zip/*.zip` exist.

- [ ] **Step 4: Confirm the runtime sees the full version and note file names**

Run: `unzip -p <the .zip> '*/nucleus/nucleus-app.properties' 2>/dev/null || find mangaworld/desktop/build -name nucleus-app.properties -exec cat {} \;`
Expected: `app.version=33.0.0-alpha.20261030.1415`.
Record the installer file names in the commit message body. **If they lack an arch marker** (`arm64`/`x64`/`amd64`), Task 7's rename script must add one (see Task 7 Step 2).

- [ ] **Step 5: Run the app**

Run: `./gradlew :mangaworld:desktop:run`
Expected: app opens as before. Repeat Step 3 for `:animeworld:desktop` (VLC prep still runs) and `:novelworld:desktop`.

- [ ] **Step 6: Commit**

```bash
git add mangaworld/desktop/build.gradle.kts animeworld/desktop/build.gradle.kts novelworld/desktop/build.gradle.kts
git commit -m "build(desktop): package with Nucleus, add Zip/NSIS/AppImage targets"
```

---

### Task 2: Release selection and `OtakuReleaseProvider`

**Files:**
- Create: `kmpuiviews/src/jvmMain/.../updates/UpdateTrack.kt`, `ReleaseSelection.kt`, `OtakuReleaseProvider.kt`
- Test: `kmpuiviews/src/jvmTest/.../updates/ReleaseSelectionTest.kt`, `OtakuReleaseProviderTest.kt`

**Interfaces:**
- Produces:
  - `enum class UpdateTrack { Stable, Nightly; companion object { fun fromVersion(version: String): UpdateTrack } }`
  - `@Serializable data class GhRelease(@SerialName("tag_name") val tagName: String, val prerelease: Boolean, @SerialName("html_url") val htmlUrl: String, val assets: List<GhAsset>)`, `@Serializable data class GhAsset(val name: String)`
  - `fun metadataFileName(channel: String, platform: Platform): String` (Windows/Unknown `"$channel.yml"`, MacOS `"$channel-mac.yml"`, Linux `"$channel-linux.yml"`)
  - `fun selectStable(releases: List<GhRelease>, ymlName: String): GhRelease?`
  - `fun selectNightly(releases: List<GhRelease>, ymlName: String): GhRelease?`
  - `fun pickHigher(a: Pair<GhRelease, String>?, b: Pair<GhRelease, String>?): GhRelease?` (pair = release + its yml `version`; compares with `dev.nucleusframework.updater.Version.fromString`)
  - `class OtakuReleaseProvider(track: UpdateTrack, owner: String = "jakepurple13", repo: String = "OtakuWorld", apiBase: String = "https://api.github.com", webBase: String = "https://github.com") : UpdateProvider` with `val resolvedRelease: GhRelease?` (last release `resolveMetadataUrl` picked)

- [ ] **Step 1: Write failing selection tests**

```kotlin
class ReleaseSelectionTest {
    private fun rel(tag: String, pre: Boolean, vararg assets: String) =
        GhRelease(tag, pre, "https://github.com/jakepurple13/OtakuWorld/releases/tag/$tag", assets.map(::GhAsset))

    @Test fun trackFromVersion() {
        assertEquals(UpdateTrack.Nightly, UpdateTrack.fromVersion("33.0.0-alpha.20261030.1415"))
        assertEquals(UpdateTrack.Stable, UpdateTrack.fromVersion("33.0.0"))
    }
    @Test fun metadataNames() {
        assertEquals("mangaworld-mac.yml", metadataFileName("mangaworld", Platform.MacOS))
        assertEquals("mangaworld.yml", metadataFileName("mangaworld", Platform.Windows))
        assertEquals("mangaworld-linux.yml", metadataFileName("mangaworld", Platform.Linux))
    }
    @Test fun stableSkipsPrereleasesAndOddTags() {
        val r = listOf(rel("nightly_2026_10_30", true, "mangaworld-mac.yml"), rel("v32.0.0_test", false, "mangaworld-mac.yml"), rel("v32.1.2", false, "mangaworld-mac.yml"))
        assertEquals("v32.1.2", selectStable(r, "mangaworld-mac.yml")?.tagName)
    }
    @Test fun stableSkipsReleaseMissingThisPlatformsYml() {
        val r = listOf(rel("v33.0.0", false, "mangaworld-linux.yml"), rel("v32.1.2", false, "mangaworld-mac.yml"))
        assertEquals("v32.1.2", selectStable(r, "mangaworld-mac.yml")?.tagName)
    }
    @Test fun nightlyIgnoresOtherAppsFiles() {
        val r = listOf(rel("nightly_2026_10_30", true, "animeworld-mac.yml"), rel("nightly_2026_10_29", true, "mangaworld-mac.yml"))
        assertEquals("nightly_2026_10_29", selectNightly(r, "mangaworld-mac.yml")?.tagName)
    }
    @Test fun nightlyPicksStableWhenHigher() {
        val n = rel("nightly_2026_10_30", true) to "33.0.0-alpha.20261030.1415"
        val s = rel("v33.0.0", false) to "33.0.0"
        assertEquals("v33.0.0", pickHigher(n, s)?.tagName)
    }
    @Test fun nightlyFallsBackToStableWhenNoNightly() {
        assertEquals("v33.0.0", pickHigher(null, rel("v33.0.0", false) to "33.0.0")?.tagName)
    }
    @Test fun sameDayRerunSortsHigher() {
        val a = rel("nightly_2026_10_30", true) to "33.0.0-alpha.20261030.0100"
        val b = rel("nightly_2026_10_30", true) to "33.0.0-alpha.20261030.1415"
        assertEquals("33.0.0-alpha.20261030.1415", listOf(a, b).maxBy { Version.fromString(it.second) }.second)
    }
}
```

`releases` are in GitHub API order (newest first). Selection takes the first match.

- [ ] **Step 2: Run and see it fail**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.updates.ReleaseSelectionTest"`
Expected: FAIL (unresolved references).

- [ ] **Step 3: Implement `UpdateTrack.kt` and `ReleaseSelection.kt`**

- [ ] **Step 4: Run, expect PASS**

- [ ] **Step 5: Write failing provider tests** against a loopback `com.sun.net.httpserver.HttpServer` that serves `/repos/jakepurple13/OtakuWorld/releases` (JSON) and `/<owner>/<repo>/releases/download/<tag>/<yml>` (body `version: X`). Construct the provider with `apiBase`/`webBase` = `http://127.0.0.1:<port>`.

```kotlin
@Test fun stableResolvesMetadataAndDownloadUrlsToChosenTag() {
    // server: releases = [v33.0.0 with mangaworld-mac.yml]
    val p = OtakuReleaseProvider(UpdateTrack.Stable, apiBase = base, webBase = base)
    assertEquals("$base/jakepurple13/OtakuWorld/releases/download/v33.0.0/mangaworld-mac.yml",
        p.resolveMetadataUrl("mangaworld", Platform.MacOS, HttpClient.newHttpClient()))
    assertEquals("$base/jakepurple13/OtakuWorld/releases/download/v33.0.0/MangaWorld-33.0.0.dmg",
        p.getDownloadUrl("MangaWorld-33.0.0.dmg", "33.0.0"))
    assertEquals("v33.0.0", p.resolvedRelease?.tagName)
}
@Test fun nightlyReadsBothYmlsAndPicksHigher() { /* nightly yml 33.0.0-alpha.20261030.1415, stable yml 32.1.2 -> nightly tag */ }
@Test fun paginatesUntilShortPage() { /* page 1: 100 nightlies without the asset, page 2: v32.1.2 with it -> v32.1.2; assert ?per_page=100&page=2 was requested */ }
@Test fun noMatchingReleaseThrowsNoSuchElement() { assertFailsWith<NoSuchElementException> { /* empty list */ } }
```

- [ ] **Step 6: Run, expect FAIL**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.updates.OtakuReleaseProviderTest"`

- [ ] **Step 7: Implement `OtakuReleaseProvider`**

Override `resolveMetadataUrl(channel, platform, httpClient)`:
1. Fetch `"$apiBase/repos/$owner/$repo/releases?per_page=100&page=N"` with the given `httpClient`, header `Accept: application/vnd.github+json`, decoding with a `Json { ignoreUnknownKeys = true }`. Stop when a page has fewer than 100 items, or after 5 pages.
2. Stable: `selectStable`. Nightly: `selectStable` and `selectNightly`, fetch each chosen yml, read the `version:` line, then `pickHigher`.
3. Store the result in `@Volatile var resolvedRelease`, and return `"$webBase/$owner/$repo/releases/download/${tag}/${metadataFileName(channel, platform)}"`. Throw `NoSuchElementException("No release with $ymlName")` when nothing matches.

`getDownloadUrl(fileName, version)` = same base with `resolvedRelease!!.tagName`. `getUpdateMetadataUrl` throws `UnsupportedOperationException` (Nucleus calls `resolveMetadataUrl`).

- [ ] **Step 8: Run both test classes, expect PASS**

- [ ] **Step 9: Commit**

```bash
git add kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/updates kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/updates
git commit -m "feat(desktop): add per-app GitHub release provider for Nucleus updater"
```

---

### Task 3: `DesktopUpdater` state machine and prefs

**Files:**
- Create: `kmpuiviews/src/jvmMain/.../updates/UpdaterEngine.kt`, `UpdatePrefs.kt`, `DesktopUpdater.kt`
- Modify: `kmpuiviews/src/jvmMain/.../Platform.jvm.kt:257` (`MangaDesktopSettings`)
- Test: `kmpuiviews/src/jvmTest/.../updates/DesktopUpdaterTest.kt`

**Interfaces:**
- Consumes: `OtakuReleaseProvider`, `UpdateTrack` (Task 2).
- Produces:
  - `interface UpdaterEngine { val currentVersion: String; val track: UpdateTrack; fun isSupported(): Boolean; suspend fun check(): UpdateResult; fun download(info: UpdateInfo): Flow<DownloadProgress>; fun installAndRestart(file: File); fun consumeUpdateEvent(): UpdateEvent?; val foundReleasePage: String? }`
  - `class NucleusUpdaterEngine(channel: String) : UpdaterEngine`: builds `NucleusUpdater { provider = OtakuReleaseProvider(track); this.channel = channel }`. `currentVersion` comes from the Nucleus default `UpdaterConfig` value. `foundReleasePage` = `provider.resolvedRelease?.htmlUrl`.
  - `interface UpdatePrefs { suspend fun checkOnLaunch(): Boolean; suspend fun skippedVersion(): String; suspend fun setSkippedVersion(version: String) }`, plus `class DataStoreUpdatePrefs(settings: MangaDesktopSettings) : UpdatePrefs`
  - `MangaDesktopSettings.checkUpdatesOnLaunch` (`booleanPreferencesKey("checkUpdatesOnLaunch")`, default `true`), `MangaDesktopSettings.skippedUpdateVersion` (`stringPreferencesKey("skippedUpdateVersion")`, default `""`)
  - `sealed interface DesktopUpdateState { data object Idle; data object Checking; data object UpToDate; data class Available(val version: String, val level: UpdateLevel); data class Downloading(val version: String, val percent: Float, val bytes: Long, val total: Long); data class Ready(val version: String); data class Error(val message: String); data object Unsupported }`
  - `class DesktopUpdater(engine: UpdaterEngine, prefs: UpdatePrefs, scope: CoroutineScope)` with:
    - `val state: StateFlow<DesktopUpdateState>`
    - `val showLaunchDialog: StateFlow<Boolean>`
    - `val currentVersion: String`, `val track: UpdateTrack`
    - `suspend fun checkOnLaunch()`, `fun checkNow()`, `fun download()`, `fun cancelDownload()`, `fun restart()`, `fun skipThisVersion()`, `fun dismissLaunchDialog()`
    - `fun consumeJustUpdated(): String?` (new version or null)
    - `fun releasePageUrl(): String`

- [ ] **Step 1: Write failing tests** with a `FakeUpdaterEngine` (scripted `check()` result, a `MutableSharedFlow<DownloadProgress>` for downloads, and a recorded `installAndRestart` file) and an in-memory `FakeUpdatePrefs`. Use `runTest` + `StandardTestDispatcher` scope + Turbine.

```kotlin
@Test fun unsupportedEngineNeverChecks()            // isSupported=false -> state Unsupported; checkOnLaunch + checkNow -> engine.checkCalls == 0
@Test fun launchCheckOffDoesNothing()               // prefs.checkOnLaunch=false -> checkCalls == 0, state Idle
@Test fun launchCheckAvailableShowsDialog()         // Available("33.0.1") -> state Available("33.0.1", PATCH), showLaunchDialog true
@Test fun launchCheckErrorShowsNoDialog()           // UpdateResult.Error(NetworkException) -> state Error(msg), showLaunchDialog false
@Test fun skippedVersionOnlyHidesThatVersion()      // skipped "33.0.1": result 33.0.1 -> no dialog; result 33.0.2 -> dialog
@Test fun checkNowIgnoresSkipAndNeverOpensDialog()  // skipped "33.0.1", checkNow -> state Available("33.0.1"), showLaunchDialog false
@Test fun downloadProgressThenReady()               // emit 50% then file -> Downloading(percent=50f) then Ready("33.0.1")
@Test fun checkNowDuringDownloadIsIgnored()         // while Downloading, checkNow -> checkCalls unchanged, still Downloading
@Test fun cancelDownloadReturnsToAvailable()
@Test fun restartInstallsDownloadedFile()           // Ready -> restart() -> engine.installed == that file
@Test fun skipThisVersionPersistsAndHidesDialog()   // prefs.skipped == "33.0.1", showLaunchDialog false
@Test fun justUpdatedConsumedOnce()                 // engine event newVersion "33.0.1" -> "33.0.1", then null
@Test fun releasePageUrlPrefersFoundRelease()       // foundReleasePage set -> that URL
@Test fun releasePageUrlFallbackStable()            // null, Stable -> "https://github.com/jakepurple13/OtakuWorld/releases/latest"
@Test fun releasePageUrlFallbackNightly()           // null, Nightly -> "https://github.com/jakepurple13/OtakuWorld/releases"
```

- [ ] **Step 2: Run, expect FAIL**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.updates.DesktopUpdaterTest"`

- [ ] **Step 3: Implement prefs, engine, and `DesktopUpdater`**

Download runs as a `Job` in `scope`; `cancelDownload` cancels it. Keep the last `UpdateInfo` from `Available` for `download()`. The launch dialog step comes from `state` (Available, Downloading, Ready), so no separate step flow. Error message: `exception.message ?: "Couldn’t reach GitHub. Check your connection and retry."`.

- [ ] **Step 4: Run, expect PASS**

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/updates kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/Platform.jvm.kt kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/updates
git commit -m "feat(desktop): add DesktopUpdater state machine and update prefs"
```

---

### Task 4: Updates group in Desktop Settings

**Files:**
- Create: `kmpuiviews/src/jvmMain/.../updates/UpdatesSettingsGroup.kt`
- Modify: `mangaworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/PlatformSettings.kt`, same path in `animeworld/desktop` and `novelworld/desktop`

**Interfaces:**
- Consumes: `DesktopUpdater`, `MangaDesktopSettings.checkUpdatesOnLaunch` (Task 3).
- Produces: `fun LazyListScope.updatesSettingsGroup()` (renders a group header item plus 4 items; each composable gets `DesktopUpdater` / settings via `koinInject()`).

- [ ] **Step 1: Implement `updatesSettingsGroup()`** matching the approved preview and the existing rows' style (`SegmentedListItem`, `ListItemDefaults.segmentedShapes(index, count = 4)`, `surfaceContainer` colors, `Modifier.animateItem()` on every item):
  1. "Check for updates": icon `Icons.Default.SystemUpdateAlt`. Title per state: "Update available" + level chip / "Ready to install" / "Check for updates". Status line per state, using the preview copy:
     - Idle: "Not checked yet"
     - Checking: "Checking GitHub…"
     - UpToDate: "Up to date · checked just now"
     - Available: "`<v>` is ready to download"
     - Downloading: "Downloading `<v>` · `<x>` of `<y>` MB", plus a `LinearProgressIndicator`
     - Ready: "`<v>` downloaded · restart to finish"
     - Error: message
     - Unsupported: "Updates are off when running from Gradle"

     Then "Current: `<currentVersion>`". The trailing action matches the state: Check now / `CircularProgressIndicator` / Download / Cancel / Restart / Retry / none. Use `primaryContainer` when Available, Downloading, or Ready, and `errorContainer` on Error.
  2. "Check automatically": switch bound to `checkUpdatesOnLaunch`, subtitle "Look for updates each time the app starts". Disabled when Unsupported.
  3. "Update track · Nightly|Stable": "Gets nightly and stable releases" / "Gets stable releases only", followed by ". Set by the build you installed."
  4. "Download from GitHub": "Open the `<track>` release page and install it yourself", `onClick = { uriHandler.openUri(updater.releasePageUrl()) }`. Always enabled.

- [ ] **Step 2: Call it from each app's `JvmSettingsScreen()`** after the existing items, preceded by an "Updates" header item styled like other settings group titles.

- [ ] **Step 3: Compile all three**

Run: `./gradlew :mangaworld:desktop:compileKotlinJvm :animeworld:desktop:compileKotlinJvm :novelworld:desktop:compileKotlinJvm`
Expected: BUILD SUCCESSFUL. (Koin wiring lands in Task 5; the screen is not opened yet.)

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/updates/UpdatesSettingsGroup.kt */desktop/src/jvmMain/kotlin/com/programmersbox/desktop/PlatformSettings.kt
git commit -m "feat(desktop): add Updates group to Desktop Settings"
```

---

### Task 5: Koin wiring, launch check, launch dialog, just-updated snackbar

**Files:**
- Create: `kmpuiviews/src/jvmMain/.../updates/UpdateLaunchDialog.kt`
- Modify: `kmpuiviews/src/jvmMain/.../DesktopUi.kt` (`desktopSetup` at :96, `InternalDesktopUi` window content around :281-330)

**Interfaces:**
- Consumes: everything from Tasks 3-4.
- Produces: `@Composable fun UpdateLaunchDialog(updater: DesktopUpdater)`, `@Composable fun JustUpdatedSnackbar(updater: DesktopUpdater)`.

- [ ] **Step 1: Register in Koin** inside the `module { }` that `desktopSetup` builds (after `single { appConfig() }`):

```kotlin
single<UpdatePrefs> { DataStoreUpdatePrefs(get()) }
single<UpdaterEngine> { NucleusUpdaterEngine(channel = name.lowercase()) }
single { DesktopUpdater(get(), get(), CoroutineScope(Dispatchers.IO + SupervisorJob())) }
```

`baseDesktopSetup` is not changed. Only the three World apps use `desktopSetup`; confirm with a grep before editing.

- [ ] **Step 2: Implement `UpdateLaunchDialog`.** It is visible while `showLaunchDialog` is true, and its step comes from `state`. Copy comes from the preview:
  - Available: title "Update available". Body "`<App>` `<v>` is ready to download. You’re on `<current>`." Meta "`<level>` · `<track>` track". Text button "Download from GitHub" opens `releasePageUrl()`. Actions: "Skip this version" (`skipThisVersion`), "Later" (`dismissLaunchDialog`), "Download" (`download`).
  - Downloading: title "Downloading `<v>`", a progress bar, "`<x>` of `<y>` MB", and "You can keep reading. We’ll ask before restarting." Actions: "Cancel" (`cancelDownload` + dismiss), "Hide" (`dismissLaunchDialog`).
  - Ready: title "Restart to update". Body "`<App>` `<v>` is downloaded and verified. The app will close, install the update, and reopen where you left off." Actions: "Later" (dismiss), "Restart now" (`restart`).

  `<App>` = `koinInject<AppConfig>().appName`.

- [ ] **Step 3: Implement `JustUpdatedSnackbar`.** On first composition, call `consumeJustUpdated()`. If it is non-null, show a `Snackbar` "Updated to `<v>`" with a "What’s new" action that opens `releasePageUrl()`. Host it in a `Box` overlaying the window `Column` content.

- [ ] **Step 4: Hook into the window.** Inside the `Window { ... }` content, after `HomeNav(...)`:

```kotlin
val updater = koinInject<DesktopUpdater>()
LaunchedEffect(Unit) { updater.checkOnLaunch() }
UpdateLaunchDialog(updater)
JustUpdatedSnackbar(updater)
```

- [ ] **Step 5: Run in dev and check the Unsupported path**

Run: `./gradlew :mangaworld:desktop:run`
Expected: no dialog. Desktop Settings shows the Updates group with "Updates are off when running from Gradle", Check now absent, and the toggle disabled. "Download from GitHub" opens `https://github.com/jakepurple13/OtakuWorld/releases/latest`.

- [ ] **Step 6: Check the packaged path**

Run: `DESKTOP_VERSION=32.0.0 ./gradlew :mangaworld:desktop:packageDistributionForCurrentOS`, then install and open the DMG/DEB/EXE.
Expected: the launch check runs. With no `mangaworld-*.yml` in any release yet, Settings shows the Error state and no dialog appears (Review Focus 1). The full update flow is verified after the first CI nightly (Task 7 Step 7).

- [ ] **Step 7: Commit**

```bash
git add kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/updates/UpdateLaunchDialog.kt kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/DesktopUi.kt
git commit -m "feat(desktop): check for updates on launch and show update dialog"
```

---

### Task 6: Platform-aware pre-release asset filter

**Files:**
- Create: `kmpuiviews/src/commonMain/.../presentation/settings/prerelease/PrereleaseAssetFilter.kt`, plus `actual`s in `androidMain`, `jvmMain`, `iosMain` (same package)
- Modify: `kmpuiviews/src/commonMain/.../presentation/settings/prerelease/PrereleaseViewModel.kt:20-58`
- Test: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/settings/PrereleaseAssetFilterTest.kt`

**Interfaces:**
- Produces:
  - `enum class AssetPlatform { Android, MacOS, Windows, Linux, Other }`
  - `expect val currentAssetPlatform: AssetPlatform` (jvm: from `System.getProperty("os.name")`; android: `Android`; ios: `Other`)
  - `fun isPrereleaseAssetForPlatform(name: String, appName: String, platform: AssetPlatform): Boolean`

- [ ] **Step 1: Write failing test**

```kotlin
class PrereleaseAssetFilterTest {
    @Test fun androidShowsAllApks() {
        assertTrue(isPrereleaseAssetForPlatform("animeworld-noFirebase-release.apk", "MangaWorld", AssetPlatform.Android))
        assertFalse(isPrereleaseAssetForPlatform("MangaWorld-33.0.0.dmg", "MangaWorld", AssetPlatform.Android))
        assertFalse(isPrereleaseAssetForPlatform("mangaworld-mac.yml", "MangaWorld", AssetPlatform.Android))
    }
    @Test fun macShowsOnlyThisAppsDmgAndZip() {
        assertTrue(isPrereleaseAssetForPlatform("MangaWorld-33.0.0-arm64.dmg", "MangaWorld", AssetPlatform.MacOS))
        assertTrue(isPrereleaseAssetForPlatform("MangaWorld-33.0.0-arm64.zip", "MangaWorld", AssetPlatform.MacOS))
        assertFalse(isPrereleaseAssetForPlatform("AnimeWorld-33.0.0-arm64.dmg", "MangaWorld", AssetPlatform.MacOS))
        assertFalse(isPrereleaseAssetForPlatform("MangaWorld-33.0.0-arm64.zip.blockmap", "MangaWorld", AssetPlatform.MacOS))
    }
    @Test fun windowsShowsExe() { assertTrue(isPrereleaseAssetForPlatform("MangaWorld-33.0.0-x64.exe", "MangaWorld", AssetPlatform.Windows)) }
    @Test fun linuxShowsDebAndAppImage() {
        assertTrue(isPrereleaseAssetForPlatform("MangaWorld-33.0.0-amd64.deb", "MangaWorld", AssetPlatform.Linux))
        assertTrue(isPrereleaseAssetForPlatform("MangaWorld-33.0.0-x86_64.AppImage", "MangaWorld", AssetPlatform.Linux))
        assertFalse(isPrereleaseAssetForPlatform("mangaworld-linux.yml", "MangaWorld", AssetPlatform.Linux))
    }
    @Test fun otherShowsEverything() { assertTrue(isPrereleaseAssetForPlatform("anything.bin", "MangaWorld", AssetPlatform.Other)) }
}
```

Desktop name match: `name.startsWith(appName, ignoreCase = true)` and the extension is in the OS set. Android: `endsWith(".apk")`.

- [ ] **Step 2: Run, expect FAIL**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.settings.PrereleaseAssetFilterTest"`

- [ ] **Step 3: Implement the filter and `actual`s.** In `PrereleaseViewModel`, add an `AppConfig` constructor parameter (`viewModelOf` resolves it). Change the `.map` at :51 to filter assets with `isPrereleaseAssetForPlatform(it.name, appConfig.appName, currentAssetPlatform)` before `sortedBy`.

- [ ] **Step 4: Run, expect PASS; compile Android**

Run: `./gradlew :kmpuiviews:jvmTest --tests "*PrereleaseAssetFilterTest" :mangaworld:assembleNoFirebaseDebug`
Expected: PASS, BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/*/kotlin/com/programmersbox/kmpuiviews/presentation/settings/prerelease kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/settings/PrereleaseAssetFilterTest.kt
git commit -m "feat: filter pre-release assets by platform"
```

---

### Task 7: CI build, update metadata, publishing

**Files:**
- Create: `.github/scripts/desktop-version.sh`, `.github/scripts/rename-update-yml.sh`, `.github/scripts/test-desktop-scripts.sh`, `.github/workflows/desktop_release.yaml`
- Modify: `.github/workflows/nightly_release.yaml` (desktop jobs ~:74-98, `release` job :106-138), `.github/workflows/main_release.yml`
- Delete: `.github/workflows/{mangaworld,animeworld,novelworld}_desktop_build.yaml` (grep first; nothing else may reference them)

**Interfaces:**
- Consumes: `DESKTOP_VERSION` contract (Task 1). Metadata names (Global Constraints).
- Produces:
  - `desktop-version.sh nightly <appinfo-path> <now-UTC-YYYYMMDDHHMM>` prints e.g. `33.1.0-alpha.20261030.1415`
  - `desktop-version.sh stable <tag>` prints `33.0.0`; exits 1 for tags not matching `^v\d+\.\d+\.\d+$`
  - `rename-update-yml.sh <app> <dir>` renames `latest.yml`, `latest-mac.yml`, `latest-linux.yml` to `<app>.yml` etc. If fewer than 4 `release-assets-<app>-*` dirs exist in `<dir>`, it deletes all ymls and prints a warning (exit 0)
  - Reusable workflow inputs: `app` (string), `version` (string). Artifacts: `release-assets-<app>-<label>-<arch>` (installers, `.blockmap`; no `.yml`), `update-yml-<app>` (renamed ymls)

- [ ] **Step 1: Write script tests** (`test-desktop-scripts.sh`, plain bash asserts, using a temp git repo for tags):
  - `otakuVersionName = "33.0.0"`, latest stable tag `v32.1.2`, now `202610301415`: nightly prints `33.0.0-alpha.20261030.1415`
  - Same, but latest stable tag `v33.0.0`: prints `33.1.0-alpha.20261030.1415`
  - `stable v33.0.0` prints `33.0.0`; `stable v32.0.0_test` exits 1
  - Rename with 4 `release-assets-mangaworld-*` dirs: `mangaworld-mac.yml`, `mangaworld.yml`, `mangaworld-linux.yml` exist, no `latest*`
  - Rename with 3 dirs: no yml files remain

Run: `bash .github/scripts/test-desktop-scripts.sh`. Expected: FAIL (scripts missing).

- [ ] **Step 2: Implement both scripts; run tests, expect PASS.**
  - The latest stable tag comes from `git tag -l 'v*' | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -1`.
  - If Task 1 recorded installer names without an arch marker, `rename-update-yml.sh` gains a pre-step that renames installers and blockmaps in each `release-assets-<app>-<label>-<arch>` dir to `<name>-<arch>.<ext>` **before** yml generation. Add a test case for it.

- [ ] **Step 3: Write `desktop_release.yaml`** (`on: workflow_call` with inputs `app`, `version`).
  - Job `build`:
    - `continue-on-error: true`, `timeout-minutes: 45`, `fail-fast: false`.
    - Matrix `include`:
      - `{os: ubuntu-latest, label: linux, arch: amd64}`
      - `{os: windows-latest, label: windows, arch: amd64}`
      - `{os: macos-latest, label: macos, arch: arm64}`
      - `{os: macos-15-intel, label: macos, arch: x64}`
    - Steps:
      1. `actions/checkout@v7`.
      2. `NucleusFramework/Nucleus/.github/actions/setup-nucleus@v2.5.18` with `java-version: '21'`, `packaging-tools: 'true'`.
      3. `./gradlew :${{ inputs.app }}:desktop:packageDistributionForCurrentOS --no-daemon --stacktrace` with env `DESKTOP_VERSION: ${{ inputs.version }}`.
      4. `actions/upload-artifact@v7`, name `release-assets-${{ inputs.app }}-${{ matrix.label }}-${{ matrix.arch }}`, path `${{ inputs.app }}/desktop/build/compose/binaries/main/**/*` plus `!**/*.yml`.
  - Job `metadata` (`needs: build`, `ubuntu-latest`):
    1. `download-artifact@v8` with pattern `release-assets-${{ inputs.app }}-*` into `assets/`.
    2. `generate-update-yml@v2.5.18` with `artifacts-path: assets`, `version`, `channel: latest`.
    3. `bash .github/scripts/rename-update-yml.sh ${{ inputs.app }} assets/update-yml` (pass the dir the action wrote).
    4. Upload artifact `update-yml-${{ inputs.app }}`, `if-no-files-found: ignore`.

  Check the generate action's directory matching: dir names contain `linux`/`windows`/`macos`, and it scans `release-assets-*/`.

- [ ] **Step 4: Wire `nightly_release.yaml`.**
  - Add a `desktopVersion` job (`needs: checkNightly`, same `if`): `actions/checkout@v7` with `fetch-depth: 0`, then `desktop-version.sh nightly buildSrc/src/main/kotlin/AppInfo.kt $(date -u +%Y%m%d%H%M)`, exposed as output `version`.
  - Replace the three `*Desktop` jobs with `uses: ./.github/workflows/desktop_release.yaml`, `with: { app: mangaworld|animeworld|novelworld, version: ${{ needs.desktopVersion.outputs.version }} }`, `needs: [checkNightly, desktopVersion]`.
  - In `release`, keep `download-artifact` into `release-artifacts`. Change `files:` to only installers, blockmaps, ymls, and APKs (`release-artifacts/**/*.{apk,dmg,zip,exe,deb,AppImage,blockmap,yml}`), so duplicate names from nested dirs never upload twice.

- [ ] **Step 5: Wire `main_release.yml`.** Add the same three desktop jobs. Version comes from a `desktopVersion` job running `desktop-version.sh stable ${{ github.ref_name }}`; if it fails, the desktop jobs are skipped. Add the desktop jobs to the existing release step's `needs`, with the same `files:` glob as Step 4.

- [ ] **Step 6: Lint**

Run: `actionlint .github/workflows/desktop_release.yaml .github/workflows/nightly_release.yaml .github/workflows/main_release.yml` (install with `brew install actionlint` if missing; ask first).
Expected: no errors.

- [ ] **Step 7: Commit, then trigger one nightly by hand (ask the user first; it publishes a public prerelease)**

```bash
git add .github
git commit -m "ci(desktop): build per-app installers and publish Nucleus update metadata"
```

After the user approves: `gh workflow run nightly_release.yaml --ref <branch>`. Expected release contents:
- `mangaworld.yml`, `mangaworld-mac.yml`, `mangaworld-linux.yml` (×3 apps), and no `latest*.yml`
- the macOS yml lists both arm64 and x64 files
- installers carry unique names

Then install the Task 5 Step 6 build (`32.0.0`), relaunch, and confirm the dialog offers the nightly. Download, then Restart; the app reopens on the new version and shows the "Updated to" snackbar. Do this on macOS at least, and record whether Gatekeeper blocked the relaunch.

---

### Task 8: Changelog

**Files:**
- Modify: `CHANGELOG.md` (follow existing entry format, e.g. the #2646/#2648 entries)

- [ ] **Step 1: Add an entry:** desktop auto-update (launch check + dialog, Desktop Settings "Updates" group, Download from GitHub fallback), the pre-release screen filter, and the new desktop formats (ZIP, NSIS, AppImage). Note that builds are unsigned.
- [ ] **Step 2: Commit**

```bash
git add CHANGELOG.md
git commit -m "docs(changelog): add desktop auto-update"
```
