# Telemetry PR 1 — Module and Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create `:telemetry` + `:telemetry:android`, inject them through Koin, and move every existing Firebase analytics/crash call onto them with no new events.

**Architecture:** `:telemetry` (KMP) holds `Analytics`/`CrashReporter` interfaces, no-op and console implementations, and `noOpTelemetryModule`. `:telemetry:android` (Android library, `noFirebase`/`full` flavors) provides `androidTelemetryModule`; `UIViews` (already flavored) depends on it and includes it from `OtakuApp.models()`. Desktop Koin starts include `noOpTelemetryModule`.

**Tech Stack:** Kotlin Multiplatform, Koin 4, Firebase Analytics + Crashlytics (full flavor only), kotlin.test, Turbine.

**Spec:** `docs/superpowers/specs/2026-11-18-telemetry-design.md` (PR 1 section). PR 2 gets its own plan after this merges.

## Global Constraints

- Package `com.programmersbox.telemetry`; modules `:telemetry` and `:telemetry:android`.
- Firebase deps in `:telemetry:android` are `fullImplementation` only.
- Every telemetry implementation swallows its own failures (`runCatching`); telemetry never crashes the app.
- Never send item titles, item/chapter URLs, search text, note text, list names, account emails, file paths — breadcrumbs included.
- No new analytics events in this PR.
- Use `noFirebase` for local builds; configuration cache on; non-transitive R.
- Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

- Telemetry used before Koin starts or with no Koin at all (KmpOtakuApp before `startKoin`, `dispatchIoAndCatchList` in plain unit tests) → silently skipped, flow still emits `emptyList()`. Test in Task 6.
- Source exceptions whose `message` contains a URL → breadcrumb carries `exception::class.simpleName`, never `message`. Test in Task 5.
- `NavKey` data classes holding user strings (`DetailsScreen.Details(title, url, …)`) → screen name never contains property values. Test in Task 4.
- Full flavor where `FirebaseApp` failed to init → Firebase impl calls are no-ops, app runs. Enforced by `runCatching` in Task 2; checked on device in Task 9.
- A desktop Koin start missing the telemetry module → crash on first inject. Covered by the desktop smoke run in Task 8.

---

## File Structure

```
telemetry/
  build.gradle.kts
  src/commonMain/kotlin/com/programmersbox/telemetry/
    Analytics.kt            Analytics interface + AnalyticsEvent base class
    CrashReporter.kt        CrashReporter interface
    NoOpTelemetry.kt        NoOpAnalytics, NoOpCrashReporter, ConsoleCrashReporter
    TelemetryModule.kt      noOpTelemetryModule(printToConsole)
  src/commonTest/kotlin/com/programmersbox/telemetry/TelemetryTest.kt
  android/
    build.gradle.kts
    src/main/AndroidManifest.xml
    src/main/java/com/programmersbox/telemetry/android/AndroidTelemetryModule.kt
    src/noFirebase/java/com/programmersbox/telemetry/android/PlatformTelemetry.kt
    src/full/java/com/programmersbox/telemetry/android/PlatformTelemetry.kt
    src/full/java/com/programmersbox/telemetry/android/FirebaseTelemetry.kt
kmpuiviews/src/commonMain/.../telemetry/TelemetryStartup.kt     startTelemetry(...)
kmpuiviews/src/commonMain/.../presentation/navigation/AnalyticsName.kt
kmpuiviews/src/commonTest/.../testing/FakeTelemetry.kt           FakeCrashReporter, FakeAnalytics (visible to jvmTest)
```

---

### Task 1: `:telemetry` KMP module

**Files:**
- Create: everything under `telemetry/` except `telemetry/android/`
- Modify: `settings.gradle.kts` (add `include(":telemetry")`, `include(":telemetry:android")` beside the other `include(":…")` lines)
- Test: `telemetry/src/commonTest/kotlin/com/programmersbox/telemetry/TelemetryTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  interface Analytics {
      val isAvailable: Boolean
      fun log(event: AnalyticsEvent)
      fun screen(name: String)
      fun setUserProperty(name: String, value: String?)
      fun setEnabled(enabled: Boolean)
  }
  abstract class AnalyticsEvent(val name: String, val params: Map<String, Any> = emptyMap())
  interface CrashReporter {
      val isAvailable: Boolean
      fun log(message: String)
      fun recordException(throwable: Throwable)
      fun setKey(key: String, value: String)
      fun setEnabled(enabled: Boolean)
  }
  object NoOpAnalytics : Analytics            // isAvailable = false, all no-op
  object NoOpCrashReporter : CrashReporter    // isAvailable = false, all no-op
  class ConsoleCrashReporter(private val print: (String) -> Unit = ::println) : CrashReporter
      // isAvailable = false; log → print(message); recordException → print(throwable.stackTraceToString()); setKey/setEnabled no-op
  fun noOpTelemetryModule(printToConsole: Boolean): Module
      // single<Analytics> { NoOpAnalytics }; single<CrashReporter> { if (printToConsole) ConsoleCrashReporter() else NoOpCrashReporter }
  ```
  `AnalyticsEvent` is `abstract` (not sealed) here; PR 2 adds subclasses in `:telemetry`.

- [ ] **Step 1: Create `telemetry/build.gradle.kts`** — copy the shape of `sharedtools/build.gradle.kts`: `` `otaku-multiplatform` ``, `androidPackageName = "com.programmersbox.telemetry"`, `android { namespace = "com.programmersbox.telemetry" }`, commonMain deps `platform(commonLibs.koin.bom)` + `commonLibs.bundles.koinKmp`, commonTest `commonLibs.kotlin.test` + `commonLibs.koin.test`.

- [ ] **Step 2: Write the failing tests**

```kotlin
class TelemetryTest {
    @Test fun consoleReporterPrintsBreadcrumbs() {
        val out = mutableListOf<String>()
        ConsoleCrashReporter(out::add).log("nav → details")
        assertEquals(listOf("nav → details"), out)
    }
    @Test fun consoleReporterPrintsStackTrace() {
        val out = mutableListOf<String>()
        ConsoleCrashReporter(out::add).recordException(IllegalStateException("boom"))
        assertTrue(out.single().contains("IllegalStateException: boom"))
    }
    @Test fun noOpsAreUnavailable() {
        assertFalse(NoOpAnalytics.isAvailable); assertFalse(NoOpCrashReporter.isAvailable)
        assertFalse(ConsoleCrashReporter().isAvailable)
    }
    @Test fun moduleBindsConsoleWhenPrinting() {
        val koin = koinApplication { modules(noOpTelemetryModule(printToConsole = true)) }.koin
        assertIs<ConsoleCrashReporter>(koin.get<CrashReporter>()); assertSame(NoOpAnalytics, koin.get<Analytics>())
    }
    @Test fun moduleBindsSilentWhenNotPrinting() {
        val koin = koinApplication { modules(noOpTelemetryModule(printToConsole = false)) }.koin
        assertSame(NoOpCrashReporter, koin.get<CrashReporter>())
    }
}
```

- [ ] **Step 3: Run** `./gradlew :telemetry:jvmTest` — Expected: FAIL (unresolved references).
- [ ] **Step 4: Implement the four source files** per the Interfaces block.
- [ ] **Step 5: Run** `./gradlew :telemetry:jvmTest` — Expected: PASS, 5 tests.
- [ ] **Step 6: Commit** `feat(telemetry): add :telemetry KMP module with no-op reporters`

---

### Task 2: `:telemetry:android` with flavors

**Files:**
- Create: everything under `telemetry/android/`

**Interfaces:**
- Consumes: Task 1 interfaces.
- Produces: `fun androidTelemetryModule(printToConsole: Boolean): Module` binding `Analytics` → `platformAnalytics()` and `CrashReporter` → `platformCrashReporter(printToConsole)`.
  - `noFirebase`: `internal fun platformAnalytics(): Analytics = NoOpAnalytics`; `internal fun platformCrashReporter(printToConsole: Boolean): CrashReporter = if (printToConsole) ConsoleCrashReporter() else NoOpCrashReporter`.
  - `full`: returns `FirebaseAnalyticsImpl()` / `CrashlyticsReporter(printToConsole)`.

- [ ] **Step 1: `telemetry/android/build.gradle.kts`** — copy the flavor block from `sharedutils/build.gradle.kts` (`id("otaku-library")`, `ProductFlavorTypes.NoFirebase { isDefault = true }`, `ProductFlavorTypes.Full`, per-flavor `java.srcDirs`), `namespace = "com.programmersbox.telemetry.android"`. Deps: `api(projects.telemetry)`, `implementation(platform(commonLibs.koin.bom))`, `implementation(androidLibs.bundles.koinLibs)`, `fullImplementation(platform(androidLibs.firebasePlatform))`, `fullImplementation(androidLibs.crashlytics)`, `fullImplementation(androidLibs.analytics)` (catalog entries `crashlytics`/`analytics` in `gradle/android.versions.toml:154-155`). Copy the `fullImplementation` helper function from sharedutils.

- [ ] **Step 2: Implement `FirebaseTelemetry.kt` (full)**
  - `CrashlyticsReporter(printToConsole)`: `isAvailable = true`; `log` → `if (printToConsole) println(message)` then `Firebase.crashlytics.log(message)`; `recordException` → `recordException`; `setKey` → `setCustomKey`; `setEnabled` → `setCrashlyticsCollectionEnabled(enabled)`.
  - `FirebaseAnalyticsImpl`: `isAvailable = true`; `screen(name)` → `logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) { param(SCREEN_NAME, name) }` (same as today's `Platform.android.kt:202-208`); `log(event)` → `logEvent(event.name)` with each param mapped by type: `String`/`Long`/`Double` direct, `Int` → `Long`, `Boolean` → `"true"/"false"` string, anything else → `toString()`; `setUserProperty` → same; `setEnabled` → `setAnalyticsCollectionEnabled`.
  - Every method body wrapped in `runCatching { }`.

- [ ] **Step 3: Verify both flavors compile**
  Run: `./gradlew :telemetry:android:compileNoFirebaseDebugKotlin :telemetry:android:compileFullDebugKotlin`
  Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Verify noFirebase has no Firebase on its classpath**
  Run: `./gradlew :telemetry:android:dependencies --configuration noFirebaseDebugRuntimeClasspath | grep -c firebase`
  Expected: `0`.
- [ ] **Step 5: Commit** `feat(telemetry): add :telemetry:android with noFirebase and full flavors`

---

### Task 3: Koin wiring and startup keys

**Files:**
- Modify: `kmpuiviews/build.gradle.kts` (commonMain `api(projects.telemetry)`), `UIViews/build.gradle.kts` (`implementation(projects.telemetry.android)`), `mangaworld/shared`, `novelworld/shared`, `animeworld/shared` build files only if Tasks 6–7 touch them (`implementation(projects.telemetry)`)
- Modify: `UIViews/src/main/java/com/programmersbox/uiviews/OtakuApp.kt:70-91`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/DesktopUi.kt` (each `modules(module { … })` block at ~148, 187, 251, 362)
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/telemetry/TelemetryStartup.kt`
- Create: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/testing/FakeTelemetry.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/telemetry/TelemetryStartupTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  fun startTelemetry(crashReporter: CrashReporter, analytics: Analytics, buildType: String, buildFlavor: String)
      // crashReporter.setKey("buildType"/"buildFlavor"/"commit_sha"); analytics.setUserProperty("buildType"/"buildFlavor")
      // commit_sha = BuildKonfig.COMMIT_SHA (BuildKonfig is internal to kmpuiviews, hence this lives here)
  class FakeCrashReporter : CrashReporter { val logs: MutableList<String>; val exceptions: MutableList<Throwable>; val keys: MutableMap<String, String>; isAvailable = true }
  class FakeAnalytics : Analytics { val events: MutableList<AnalyticsEvent>; val screens: MutableList<String>; val userProperties: MutableMap<String, String?>; isAvailable = true }
  ```

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun `startTelemetry sets build and commit keys`() {
    val crash = FakeCrashReporter(); val analytics = FakeAnalytics()
    startTelemetry(crash, analytics, buildType = "debug", buildFlavor = "full")
    assertEquals("debug", crash.keys["buildType"]); assertEquals("full", crash.keys["buildFlavor"])
    assertEquals(BuildKonfig.COMMIT_SHA, crash.keys["commit_sha"])
    assertEquals("full", analytics.userProperties["buildFlavor"])
}
```

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*TelemetryStartupTest"` — Expected: FAIL.
- [ ] **Step 3: Implement** `startTelemetry`, the fakes, and the wiring:
  - `OtakuApp.models()`: add `androidTelemetryModule(printToConsole = BuildConfig.DEBUG)` to `includes(...)`.
  - `OtakuApp.firebaseSetup()`: keep `FirebaseApp.initializeApp(this)` and the `createFirebaseIds()` block; replace the `Firebase.crashlytics.setCustomKeys {…}` and two `Firebase.analytics.setUserProperty` lines with `startTelemetry(get(), get(), BuildConfig.BUILD_TYPE, BuildConfig.FLAVOR)`. `firebaseSetup()` runs before `get<>()` calls in `onCreate`, after `super.onCreate()` (Koin is started in `KmpOtakuApp.onCreate`), so `get()` is safe.
  - `DesktopUi.kt`: add `noOpTelemetryModule(printToConsole = true)` to every `modules(...)` list.
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest --tests "*TelemetryStartupTest"` — Expected: PASS.
- [ ] **Step 5: Commit** `feat(telemetry): wire telemetry into Koin and set commit_sha key`

---

### Task 4: Screen names without user data

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/AnalyticsName.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3View.kt:48-57`
- Test: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/AnalyticsNameTest.kt`

**Interfaces:**
- Produces: `fun NavKey?.analyticsName(): String` — `is Screen` → `route`; `null` → `"none"`; otherwise `this::class.simpleName ?: "unknown"`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun screenUsesRoute() = assertEquals("recent", Screen.RecentScreen.analyticsName())
@Test fun dataScreenDropsArguments() = assertEquals("notes_detail", Screen.NotesScreen.Detail("https://x/y").analyticsName())
@Test fun detailsNeverLeaksTitleOrUrl() {
    val name = Screen.DetailsScreen.Details("Secret Title", "d", "https://site/item", "i", "Src").analyticsName()
    assertEquals("Details", name)
}
@Test fun nullIsNone() = assertEquals("none", (null as NavKey?).analyticsName())
```

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*AnalyticsNameTest"` — Expected: FAIL.
- [ ] **Step 3: Implement** `analyticsName()`; in `Nav3` add params `analytics: Analytics = koinInject()`, `crashReporter: CrashReporter = koinInject()`, and replace the two calls with `crashReporter.log("nav → ${screen.analyticsName()}")` and `analytics.screen(screen.analyticsName())`.
- [ ] **Step 4: Run** the test — Expected: PASS.
- [ ] **Step 5: Commit** `feat(telemetry): send route names instead of NavKey toString`

---

### Task 5: MediaUpdateChecker breadcrumbs without titles

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/MediaUpdateChecker.kt` (constructor + lines 124–383)
- Modify: wherever `MediaUpdateChecker` is registered in Koin (if `singleOf(::MediaUpdateChecker)`, nothing to change)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/MediaUpdateCheckerTest.kt`

**Interfaces:**
- Consumes: `CrashReporter`, `FakeCrashReporter` (Task 3).
- Produces: `MediaUpdateChecker(…, reportStore: UpdateCheckReportStore, crashReporter: CrashReporter)` — new last param.

- [ ] **Step 1: Write the failing test** (add to `MediaUpdateCheckerTest`; pass a `FakeCrashReporter` from `checker()`)

```kotlin
@Test fun `breadcrumbs never contain titles or exception messages`() = runTest {
    val crash = FakeCrashReporter()
    // favorite titled "Title https://example.com/secret" on a FakeApiService whose itemInfo throws
    // IllegalStateException("failed at https://example.com/secret")
    checker(crash = crash).checkForUpdates(/* existing call shape */)
    assertTrue(crash.logs.isNotEmpty())
    assertTrue(crash.logs.none { "Title" in it || "https://" in it })
    assertTrue(crash.logs.any { "IllegalStateException" in it })
}
```

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*MediaUpdateCheckerTest"` — Expected: new test FAILS.
- [ ] **Step 3: Replace all 15 `logFirebaseMessage` calls with `crashReporter.log(...)`**, rewritten to use only source names, counts and `e::class.simpleName`. E.g. line 321 `"Checking for update: ${model.title} from ${model.source}"` → `"update check: item from ${model.source}"`; line 341 → `"update: ${model.source} chapters ${model.numChapters} → ${infoModel.chapters.size}"`; line 353 → `"update failed: ${model.source} (${e::class.simpleName})"`.
- [ ] **Step 4: Run** the test class — Expected: all PASS.
- [ ] **Step 5: Commit** `refactor(telemetry): MediaUpdateChecker logs through CrashReporter without titles`

---

### Task 6: Remaining common and JVM call sites

**Files (modify):**
- `kmpuiviews/src/commonMain/.../utils/FlowUtils.kt`
- `kmpuiviews/src/commonMain/.../domain/aitext/AiTask.kt:73`
- `kmpuiviews/src/commonMain/.../presentation/recent/RecentViewModel.kt`
- `kmpuiviews/src/commonMain/.../presentation/details/DetailsViewModel.kt`
- `kmpuiviews/src/jvmMain/.../repository/WorkerRepositoryImpl.kt`, `BackgroundWorkHandlerImpl.kt`
- `mangaworld/shared/src/commonMain/.../settings/ImageLoaderSettings.kt:60` (+ `implementation(projects.telemetry)` in its build file)
- Test: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/utils/FlowUtilsTest.kt` (create if absent)

**Rules for every call site:** ViewModels and classes built by Koin take `CrashReporter` as a constructor param; composables use `koinInject()`; top-level functions with no receiver scope use `KoinPlatform.getKoinOrNull()?.getOrNull<CrashReporter>()` and do nothing when null. Messages keep their meaning but drop titles/URLs; exceptions log `::class.simpleName`, not `message`. `AiTask.kt:73` keeps the user-facing string but logs `"ai failed: ${e::class.simpleName}"`.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun catchListEmitsEmptyWithoutKoin() = runTest {
    stopKoin()
    val result = flow<List<Int>> { throw IllegalStateException("x") }.dispatchIoAndCatchList().first()
    assertEquals(emptyList(), result)
}
@Test fun catchListRecordsWhenKoinHasReporter() = runTest {
    val crash = FakeCrashReporter()
    startKoin { modules(module { single<CrashReporter> { crash } }) }
    flow<List<Int>> { throw IllegalStateException("x") }.dispatchIoAndCatchList().first()
    assertIs<IllegalStateException>(crash.exceptions.single()); stopKoin()
}
```

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*FlowUtilsTest"` — Expected: FAIL.
- [ ] **Step 3: Migrate the listed files** per the rules.
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest :mangaworld:shared:compileKotlinJvm` — Expected: PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `refactor(telemetry): migrate common call sites to injected reporters`

---

### Task 7: Android call sites

**Files (modify):** in `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/`: `KmpOtakuApp.kt:171`, `workers/{UpdateChecker, WorkerUtils, DownloadAndInstallWorker, AppCheckWorker, AppCleanupWorker, UpdateNotification, SourceUpdateChecker, RestoreWorker, DailyOverviewWorker, BackupWorker}.kt`, `receivers/{BootReceived, SwipeAwayReceiver, DownloadNewChaptersReceiver, ClearUpdateNotification}.kt`, `utils/{Zipper, DownloadAndInstaller}.kt`; in `UIViews/src/main/java/com/programmersbox/uiviews/`: `CrashActivity.kt:57`, `datastore/RemoteConfigKeys.kt`, `OtakuApp.kt:145`.

**Rules:** same as Task 6. Workers created by Koin's `workerOf` take a constructor param; receivers and others already `KoinComponent` use `by inject()`; `KmpOtakuApp` uses `get()` (after `startKoin`). No test — behavior-preserving swap; verified by build in Task 8.

- [ ] **Step 1: Migrate the listed files.**
- [ ] **Step 2: Run** `./gradlew :UIViews:compileNoFirebaseDebugKotlin :kmpuiviews:compileAndroidMain` — Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** `refactor(telemetry): migrate Android call sites to injected reporters`

---

### Task 8: Remove old API and Firebase deps, full build

**Files (modify):**
- `kmpuiviews/src/commonMain/.../Platform.kt:53-57` — delete the three `expect` funs.
- `Platform.android.kt:191-208`, `Platform.jvm.kt` (`recordFirebaseException`, `logFirebaseMessage`, `analyticsScreen`), `Platform.ios.kt` — delete the actuals and now-unused imports.
- `kmpuiviews/build.gradle.kts:176` — remove `implementation(androidLibs.bundles.firebaseCrashLibs)`.
- `UIViews/build.gradle.kts:50` — remove `implementation(androidLibs.bundles.firebaseCrashLibs)` if nothing in UIViews still imports `crashlytics`/`analytics`/`perf`.
- App modules keep their `firebaseCrashLibs` (they apply the Firebase Performance plugin, which needs `firebase-perf`).

- [ ] **Step 1: Delete, then confirm nothing references the old API**
  Run: `grep -rn "logFirebaseMessage\|recordFirebaseException\|analyticsScreen\|Firebase.crashlytics\|Firebase.analytics" --include='*.kt' . | grep -v /build/ | grep -v telemetry/android`
  Expected: no output.
- [ ] **Step 2: Run tests** `./gradlew :telemetry:jvmTest :kmpuiviews:jvmTest` — Expected: PASS.
- [ ] **Step 3: Build apps** `./gradlew :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug :mangaworld:desktop:compileKotlinJvm :novelworld:desktop:compileKotlinJvm :animeworld:desktop:compileKotlinJvm` — Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Desktop smoke** `./gradlew :mangaworld:desktop:run`, open Recent then a Details screen — Expected: console shows `nav → recent`, `nav → Details`; no Koin `NoDefinitionFoundException`.
- [ ] **Step 5: Commit** `refactor(telemetry): remove Platform firebase expect/actuals and crash libs from shared modules`

---

### Task 9: Device verification (full and noFirebase)

**Files:** none committed. Copy `../../../mangaworld/google-services.json` into `mangaworld/` (worktree). Never `git add` it.

- [ ] **Step 1: noFirebase** — `./gradlew :mangaworld:assembleNoFirebaseDebug`, `adb -s <serial> install -r` the APK, open the app, navigate Recent → Details. Run `adb logcat -d | grep "nav →"` — Expected: `nav → recent`, `nav → Details`. No crash.
- [ ] **Step 2: full** — `./gradlew :mangaworld:assembleFullDebug`, install, `adb shell setprop debug.firebase.analytics.app <package>`, navigate the same path. Expected: Firebase DebugView shows `screen_view` with `screen_name` = `recent`, `Details` (no titles). Ask the user to confirm DebugView if console access is not available.
- [ ] **Step 3:** `git status` — Expected: `google-services.json` not staged; delete it from the worktree.

Then open PR 1 to `develop` per the finishing-a-development-branch skill.
