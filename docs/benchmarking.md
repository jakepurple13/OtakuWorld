# Baseline Profiles & Macrobenchmarking

How to generate, verify, and measure performance for MangaWorld, AnimeWorld, and NovelWorld.

> **TL;DR** — generate profiles: `./gradlew :<app>:generateNoFirebaseReleaseBaselineProfile`.
> Measure:
`./gradlew :<BenchmarkModule>:connectedNoFirebaseReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark`

---

## Prerequisites

- **Physical low-end device or Cuttlefish emulator** (`aosp_cf_x86_64_phone-userdebug`). High-end
  Pixels mask gains; stock API emulator images are not representative.
- **USB debugging enabled**, device connected and listed in `adb devices`.
- **Release build** — debug builds run Compose interpreted with Live Literals enabled; numbers are
  not representative. See §6 for common mistakes.
- **No google-services.json needed** — use the `noFirebase` flavor throughout.

---

## 1. Generate Baseline Profiles

Run against the connected device. Each task builds a `noFirebaseRelease` APK, installs it, runs the
`BaselineProfileGenerator` test class, collects the binary profile, and writes it to the source
tree.

```bash
# MangaWorld
./gradlew :mangaworld:generateNoFirebaseReleaseBaselineProfile

# AnimeWorld
./gradlew :animeworld:generateNoFirebaseReleaseBaselineProfile

# NovelWorld
./gradlew :novelworld:generateNoFirebaseReleaseBaselineProfile
```

**Output location** (AGP writes variant-specific path):

```
mangaworld/src/release/generated/baselineProfiles/baseline-prof.txt
animeworld/src/release/generated/baselineProfiles/baseline-prof.txt
novelworld/src/release/generated/baselineProfiles/baseline-prof.txt
```

Commit these files. ART reads them on install to AOT-compile the listed methods.

---

## 2. Verify the Profile Is Packaged in the APK

After generating profiles, build a release APK and inspect it:

```bash
./gradlew :mangaworld:assembleNoFirebaseRelease
```

Open the resulting APK in **Android Studio → Build → Analyze APK** and confirm both files exist:

```
assets/dexopt/baseline.prof
assets/dexopt/baseline.profm
```

If these files are **absent**, the profile was generated but not packaged. Check that
`baselineProfile(projects.mangaWorldbaselineprofile)` is present in `mangaworld/build.gradle.kts`
and that `id("androidx.baselineprofile")` is applied in the plugins block.

---

## 3. Run Macrobenchmarks

Macrobenchmarks measure startup and scroll performance against the real compiled APK.

### Startup benchmarks (cold, warm, hot)

```bash
# MangaWorld
./gradlew :MangaWorldbaselineprofile:connectedNoFirebaseReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark

# AnimeWorld
./gradlew :AnimeWorldbaselineprofile:connectedNoFirebaseReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark

# NovelWorld
./gradlew :NovelWorldbaselineprofile:connectedNoFirebaseReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
```

### Run a single test class

```bash
./gradlew :MangaWorldbaselineprofile:connectedNoFirebaseReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.programmersbox.mangaworldbaselineprofile.StartupBenchmarks \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
```

### Results location

```
MangaWorldbaselineprofile/build/outputs/connected_android_test_additional_output/
AnimeWorldbaselineprofile/build/outputs/connected_android_test_additional_output/
NovelWorldbaselineprofile/build/outputs/connected_android_test_additional_output/
```

Each run writes a JSON file per test method. Android Studio also shows a result table inline after
the run completes.

---

## 4. Reading Results

### Startup metrics

`StartupTimingMetric` reports (all in milliseconds):

| Metric                 | What it measures                                                             |
|------------------------|------------------------------------------------------------------------------|
| `timeToInitialDisplay` | Time to first frame drawn (system shell visible)                             |
| `timeToFullDisplay`    | Time until `ReportDrawnWhen` fires — first real content frame. **Use this.** |

`timeToFullDisplay` is wired in `RecentScreen` via
`ReportDrawnWhen { filteredSourceList.isNotEmpty() }`.
If it reads the same as `timeToInitialDisplay`, content loaded before the first frame — that is
fine.

**Report medians across iterations, not means.** A single thermal-throttled run inflates the mean.
Macrobenchmark logs both; the result table in Android Studio shows median.

### Scroll/frame metrics

`FrameTimingMetric` reports:

| Metric               | What it measures                                                        |
|----------------------|-------------------------------------------------------------------------|
| `frameDurationCpuMs` | CPU time per frame (P50 / P90 / P95 / P99)                              |
| `frameOverrunMs`     | Deadline missed by this many ms (negative = on time). **Headline: P95** |

Target: **P95 `frameOverrunMs` < 0 ms** (all high-percentile frames hit their deadline).

### A/B comparison

Each `StartupBenchmarks` class has a sibling `startupCompilationNone()` test using
`CompilationMode.None`. Run both and compare medians to prove the profile moved the number:

```
startupCompilationNone:             median 460 ms
startupCompilationBaselineProfiles: median 318 ms  (-31%)
```

---

## 5. CI/CD Workflow

The `.github/workflows/baseline-profiles.yml` workflow runs on pushes to `develop`/`main` that
touch app or benchmark code, and can also be triggered manually via **Actions → Baseline Profiles &
Macrobenchmarks → Run workflow**.

### What it does

1. **generate-baseline-profiles** job:
    - Starts an API 34 x86_64 emulator per app (3 sequential runs)
    - Runs `generateNoFirebaseReleaseBaselineProfile` for each app
    - Commits generated `baseline-prof.txt` files back to the branch with `[skip ci]`
    - Uploads profiles as a **baseline-profiles** artifact (30-day retention)

2. **macrobenchmarks** job (runs after profiles are committed):
    - Runs all `StartupBenchmarks` tests for each app
    - Uploads JSON results as per-app artifacts (**90-day retention**)

### Artifacts

| Artifact name               | Contents                               |
|-----------------------------|----------------------------------------|
| `baseline-profiles`         | `baseline-prof.txt` for all three apps |
| `macrobenchmark-mangaworld` | JSON benchmark results for MangaWorld  |
| `macrobenchmark-animeworld` | JSON benchmark results for AnimeWorld  |
| `macrobenchmark-novelworld` | JSON benchmark results for NovelWorld  |

Download artifacts from the Actions run summary page.

### Required secrets

| Secret                  | Used for                                                                                 |
|-------------------------|------------------------------------------------------------------------------------------|
| `GRADLE_ENCRYPTION_KEY` | Gradle build cache encryption (optional — remove `cache-encryption-key` line if not set) |

No signing secrets needed — `noFirebase` release builds use `debugSigningConfig` from AGP defaults.

---

## 6. Common Mistakes

### Measuring a debug build

Debug builds run Compose interpreted with Live Literals converting constants to getters. Cold-start
numbers are 2–4× inflated. **Only measure on `noFirebaseRelease` or `fullRelease` variants.**

### `BaselineProfileMode.UseIfAvailable` in the benchmark

The benchmark tests use `BaselineProfileMode.Require`. If a profile is missing from the APK, the
test **fails loudly** rather than silently measuring an unprofiled build. Do not change this to
`UseIfAvailable`.

### Profile generated but not in APK

Symptoms: `assets/dexopt/baseline.prof` missing in Analyze APK.

Checklist:

- `id("androidx.baselineprofile")` present in the app's plugins block
- `baselineProfile(projects.<module>)` present in the app's `dependencies` block
- Profile `.txt` file committed to `src/release/generated/baselineProfiles/`

### Fling intercepted by system gesture area

The generator and scroll benchmarks call `setGestureMargin(device.displayWidth / 5)` before each
fling. Do not remove this — without it, edge swipes are intercepted as back/recents gestures and
the scroll never happens.

### `timeToFullDisplay` equals `timeToInitialDisplay`

`ReportDrawnWhen { filteredSourceList.isNotEmpty() }` fires only when `filteredSourceList` is
non-empty. If the source has no items or loading fails, it never fires and TTFD falls back to TTID.
Check whether the device has at least one source installed.

---

## 7. Adding More Journeys

Edit the `BaselineProfileGenerator` in the relevant module to cover more user flows. Each
`rule.collect` block runs the journey once per iteration (the plugin iterates automatically).

Add `Modifier.testTag("tag_name")` to key composables to make `By.res("tag_name")` selectors
reliable across locales:

```kotlin
// In the Compose source:
LazyVerticalGrid(modifier = Modifier.testTag("browse_grid"), ...)

// In the generator:
val grid = device.findObject(By.res("browse_grid"))
grid.setGestureMargin(device.displayWidth / 5)
grid.fling(Direction.DOWN)
```

Good journeys to add per app:

| App        | Journey                                          |
|------------|--------------------------------------------------|
| MangaWorld | Open chapter list → scroll chapters → start read |
| AnimeWorld | Open episode list → scroll episodes              |
| NovelWorld | Open chapter list → scroll chapters → start read |
| All apps   | Global search → scroll results                   |
| All apps   | Open Favorites → scroll grid                     |
