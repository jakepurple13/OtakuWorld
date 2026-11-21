# Telemetry PR 2a — Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the typed event catalog, privacy-safe source naming, the two opt-out switches (Privacy & Security option C), common user properties and crash keys, and `TELEMETRY.md` — everything PR 2b needs to wire events, without firing any new events yet.

**Architecture:** Event classes and bucket helpers live in `:telemetry` commonMain. `kmpuiviews` gets `TelemetrySourceNamer` (incognito/custom rules) and `TelemetryController`, a common class started once per process from `KmpOtakuApp.onCreate` and the desktop `LaunchedEffect`; it applies the switches (`setEnabled`), user properties and crash keys by collecting existing flows.

**Tech Stack:** Kotlin Multiplatform, Koin 4, Preferences DataStore, Compose Multiplatform, kotlin.test, Firebase (full flavor only).

**Spec:** `docs/superpowers/specs/2026-11-18-telemetry-design.md` (incl. "Amendments (2026-11-19)"). Mock: `docs/superpowers/specs/2026-11-18-privacy-screen-mock.html` option C.

## Global Constraints

- Never send item titles, item/chapter URLs, search text, note text, list names, account emails or file paths — params, user properties, keys and breadcrumbs.
- Event names: snake_case, ≤ 40 chars. ≤ 25 params per event. Param keys snake_case. Values `String`, `Long`, `Int`, `Double` or `Boolean` only; string values ≤ 100 chars.
- Counts and durations sent bucketed via `Buckets`.
- Source names always go through `TelemetrySourceNamer.name(...)`: incognito → `"incognito"`, package starting `js.` → `"custom"`.
- Both switches default **on**, stored in Preferences DataStore (`DataStoreHandling`), shown only when `analytics.isAvailable || crashReporter.isAvailable`.
- `setCrashlyticsCollectionEnabled(false)` applies next start — UI says so.
- `TELEMETRY.md` must list every event; a test enforces it.
- No new events are fired in this PR.
- Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

- A source that becomes incognito after startup → later events use `"incognito"` immediately (namer collects the incognito table as a flow). Test in Task 3.
- A service name that matches no loaded source (extension removed) → raw name, not a crash, unless an incognito row exists for that name. Test in Task 3.
- Switch toggled while app runs → `setEnabled(false)` called once per change, and toggling back calls `setEnabled(true)`. Test in Task 4.
- noFirebase / desktop (`isAvailable == false`) → section hidden, controller still runs harmlessly. Test in Task 4 (controller with no-ops) + device check in Task 7.
- `TELEMETRY.md` drifting from the catalog when events change → failing test. Task 6.

---

## File Structure

```
telemetry/src/commonMain/kotlin/com/programmersbox/telemetry/
  Buckets.kt                       count/duration/percent bucketing
  events/TelemetryEvents.kt        all 47 event classes (one sealed hierarchy per area)
telemetry/src/commonTest/kotlin/com/programmersbox/telemetry/
  EventCatalogTest.kt              sample of every event + rule checks
  BucketsTest.kt
telemetry/android/src/full/java/.../FirebaseTelemetry.kt   isAvailable fix
datastore/src/commonMain/kotlin/com/programmersbox/datastore/DataStoreHandling.kt   two switches
kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/telemetry/
  TelemetrySourceNamer.kt
  TelemetryController.kt
kmpuiviews/src/commonMain/kotlin/.../presentation/settings/behavior/
  PrivacySecurityScreen.kt         + UsageAndCrashSection
  UsageAndCrashSection.kt
kmpuiviews/src/commonMain/composeResources/values/strings.xml
kmpuiviews/src/commonMain/kotlin/.../presentation/settings/search/SettingsSearchRegistry.kt
kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/telemetry/
  TelemetrySourceNamerTest.kt, TelemetryControllerTest.kt, TelemetryDocTest.kt
TELEMETRY.md, README.md, README-zh.md
```

---

### Task 1: Event catalog and buckets

**Files:**
- Create: `telemetry/src/commonMain/kotlin/com/programmersbox/telemetry/Buckets.kt`
- Create: `telemetry/src/commonMain/kotlin/com/programmersbox/telemetry/events/TelemetryEvents.kt`
- Test: `telemetry/src/commonTest/kotlin/com/programmersbox/telemetry/BucketsTest.kt`, `EventCatalogTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  object Buckets {
      fun count(n: Int): String        // 0 → "0", 1..5 → "1-5", 6..20 → "6-20", else "21+"
      fun durationMs(ms: Long): String // <1000 "<1s", <5000 "1-5s", <30000 "5-30s", <120000 "30s-2m", else "2m+"
      fun percent(p: Int): String      // clamp 0..100; 0..25 "0-25", 26..50 "26-50", 51..75 "51-75", else "76-100"
  }
  ```
  Events: one Kotlin `data class` per row below (`data object` when it has no params), all `: AnalyticsEvent(name, params)`, so tests can `assertEquals` whole events, in package `com.programmersbox.telemetry.events`. Params map uses exactly these keys. Enum-like params are `String` constants in companion objects (e.g. `OpenedFrom.DETAILS = "details"`). Counts/durations are taken already bucketed (`String`).

| Class | name | params (key: type) |
|---|---|---|
| OnboardingFinished | onboarding_finished | skipped: Boolean, steps_seen: Int |
| OnboardingStep | onboarding_step | step: Int |
| SourceSelected | source_selected | source, media_type |
| Search | search | scope (`global|source`), source_count: Int, result_count: String(bucket) |
| DescribeSearch | describe_search | outcome (`success|empty|error`), result_count: String |
| ItemViewed | item_viewed | source, media_type, from |
| FavoriteAdded | favorite_added | source, from |
| FavoriteRemoved | favorite_removed | source |
| ListItemAdded | list_item_added | list_count: Int |
| ListCreated | list_created | — |
| ListImported | list_imported | item_count: String, kind (`list|full`) |
| ItemShared | item_shared | method (`link|card|qr|save`) |
| NoteSaved | note_saved | is_new: Boolean |
| MoodTagAdded | mood_tag_added | kind (`preset|custom|ai`) |
| UpdateCheckToggled | update_check_toggled | enabled: Boolean |
| ChapterOpened | chapter_opened | source, media_type, from |
| ReadingSession | reading_session | media_type, pages_read: String, duration: String, reached_end: Boolean |
| ReaderSettingChanged | reader_setting_changed | key, value |
| ChapterBookmarked | chapter_bookmarked | source |
| DictionaryLookup | dictionary_lookup | action (`lookup|add`) |
| EpisodePlayRequested | episode_play_requested | source, link_count: Int |
| PlaybackStarted | playback_started | player_style, casting: Boolean, ttff: String |
| PlaybackError | playback_error | source, error_type |
| DownloadStarted | download_started | source, media_type |
| DownloadFinished | download_finished | media_type, outcome (`success|fail|cancel`), duration: String |
| FavoritesFiltered | favorites_filtered | filters: Int, sort |
| FavoritesSearched | favorites_searched | result_count: String |
| HistoryCleared | history_cleared | scope (`all|item`) |
| UpdateCheckFinished | update_check_finished | trigger (`periodic|manual|retry`), items_checked: String, updates_found: String, sources_failed: Int, duration: String |
| UpdateNotificationOpened | update_notification_opened | count: Int |
| CatchUpUsed | catch_up_used | action (`read|skip|delete|remind`) |
| NotificationDismissed | notification_dismissed | — |
| ExtensionInstalled | extension_installed | package, catalog |
| ExtensionInstallFailed | extension_install_failed | catalog, reason |
| ExtensionUninstalled | extension_uninstalled | package |
| ExtensionUpdated | extension_updated | package |
| SourceUpdateSkipped | source_update_skipped | package |
| SourceError | source_error | source, operation (`recent|all|search|info|chapters|pages`), error_class |
| BackupCreated | backup_created | destination (`file|cloud`), success: Boolean |
| RestoreFinished | restore_finished | success: Boolean, categories: Int |
| SyncSignIn | sync_sign_in | provider |
| SyncFailed | sync_failed | provider, error_class |
| AiTaskRun | ai_task | feature, engine, outcome (`success|error|unavailable`), latency: String |
| AiModelInstalled | ai_model_installed | engine, via (`download|import`) |
| TranslationUsed | translation_used | kind (`description|tldr`) |
| QuickActionUsed | quick_action_used | action, surface |
| DailyOverviewOpened | daily_overview_opened | from |

  Also: `object OpenedFrom { const val DETAILS="details"; SEARCH="search"; RECENT="recent"; ALL="all"; FAVORITES="favorites"; HISTORY="history"; NOTIFICATION="notification"; DEEP_LINK="deep_link"; QUICK_ACTION="quick_action"; DOWNLOADS="downloads"; LONG_PRESS="long_press"; LISTS="lists"; OTHER="other" }`.

- [ ] **Step 1: Write the failing tests**

```kotlin
class BucketsTest {
    @Test fun counts() = assertEquals(listOf("0","1-5","1-5","6-20","21+"), listOf(0,1,5,6,21).map(Buckets::count))
    @Test fun durations() = assertEquals(listOf("<1s","1-5s","5-30s","30s-2m","2m+"), listOf(999L,1000L,5000L,30000L,120000L).map(Buckets::durationMs))
    @Test fun percents() = assertEquals(listOf("0-25","26-50","51-75","76-100","76-100","0-25"), listOf(0,26,51,76,150,-5).map(Buckets::percent))
}

class EventCatalogTest {
    // one instance of every class in the table, with representative values
    private val all: List<AnalyticsEvent> = listOf(/* 47 entries */)
    private val forbidden = setOf("title", "url", "link", "query", "text", "note", "email", "path", "name")
    @Test fun catalogHas47UniqueEvents() = assertEquals(47, all.map { it.name }.toSet().size)
    @Test fun namesAreSnakeCaseUnder40() = all.forEach { assertTrue(Regex("^[a-z][a-z0-9_]{0,39}$").matches(it.name), it.name) }
    @Test fun paramRules() = all.forEach { e ->
        assertTrue(e.params.size <= 25, e.name)
        e.params.forEach { (k, v) ->
            assertTrue(Regex("^[a-z][a-z0-9_]*$").matches(k), "${e.name}.$k")
            assertTrue(k !in forbidden, "${e.name}.$k is forbidden")
            assertTrue(v is String || v is Int || v is Long || v is Double || v is Boolean, "${e.name}.$k type")
            if (v is String) assertTrue(v.length <= 100, "${e.name}.$k length")
        }
    }
}
```
  Also add `val telemetryEventNames: List<String>` in commonMain `events/TelemetryEvents.kt` (the 47 names, in table order; Task 6 uses it) and a test `samplesMatchNameList` asserting `all.map { it.name } == telemetryEventNames`.

- [ ] **Step 2: Run** `./gradlew :telemetry:jvmTest` — Expected: FAIL (unresolved).
- [ ] **Step 3: Implement** `Buckets` and `TelemetryEvents.kt` per the table.
- [ ] **Step 4: Run** `./gradlew :telemetry:jvmTest` — Expected: PASS.
- [ ] **Step 5: Commit** `feat(telemetry): typed event catalog and bucket helpers`

---

### Task 2: `isAvailable` reflects Firebase init

**Files:** Modify `telemetry/android/src/full/java/com/programmersbox/telemetry/android/FirebaseTelemetry.kt`

**Interfaces:** Produces `CrashlyticsReporter.isAvailable` / `FirebaseAnalyticsImpl.isAvailable` = `runCatching { FirebaseApp.getInstance() }.isSuccess` (evaluated lazily, `by lazy`).

No JVM test is possible (Firebase Android only); verified on device in Task 7.

- [ ] **Step 1: Implement.**
- [ ] **Step 2: Run** `./gradlew :telemetry:android:compileFullDebugKotlin :telemetry:android:compileNoFirebaseDebugKotlin` — Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** `fix(telemetry): report unavailable when Firebase did not initialize`

---

### Task 3: TelemetrySourceNamer

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/telemetry/TelemetrySourceNamer.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/RepositoryModule.kt` (`singleOf(::TelemetrySourceNamer)`)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/telemetry/TelemetrySourceNamerTest.kt`

**Interfaces:**
- Consumes: `SourceRepository.toSourceByApiServiceName(name)` (`kmpmodels/.../SourceRepository.kt:42`), `ItemDao.getAllIncognitoSources(): Flow<List<IncognitoSource>>` (`IncognitoSource.source` = package name, `.name`, `.isIncognito`).
- Produces:
  ```kotlin
  class TelemetrySourceNamer(private val sourceRepository: SourceRepository, private val dao: ItemDao) {
      fun start(scope: CoroutineScope)            // collects incognito rows into an internal StateFlow<Set<String>> of package names AND names where isIncognito
      fun name(serviceName: String): String       // "incognito" | "custom" | serviceName
      fun packageName(packageName: String): String // same rules for package-keyed events
  }
  ```
  Rules, in order: incognito (package or name in set) → `"incognito"`; package starts with `"js."` → `"custom"`; else input.

- [ ] **Step 1: Write the failing tests** (use `createTestItemDatabase()`, `SourceRepository().register(FakeApiService(...))` from `testing/`; give FakeApiService sources package names via `register` — check its helper and pass `packageName` if supported, else construct `KmpSourceInformation` directly)

```kotlin
@Test fun `plugin source keeps its name`() = runTest { assertEquals("MangaDex", namer().name("MangaDex")) }
@Test fun `js package becomes custom`() = runTest { assertEquals("custom", namer(jsSource = "Example").name("Example")) }
@Test fun `incognito source becomes incognito`() = runTest {
    dao.insertIncognitoSource(IncognitoSource(source = "pkg.mangadex", name = "MangaDex", isIncognito = true))
    val n = namer(); advanceUntilIdle()
    assertEquals("incognito", n.name("MangaDex"))
}
@Test fun `incognito added after start applies`() = runTest { /* start, then insert, advanceUntilIdle, assert "incognito" */ }
@Test fun `unknown service name passes through`() = runTest { assertEquals("Gone", namer().name("Gone")) }
@Test fun `incognito by package name`() = runTest { /* insert row, assert packageName("pkg.mangadex") == "incognito" */ }
```
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*TelemetrySourceNamerTest"` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** the test — Expected: PASS.
- [ ] **Step 5: Commit** `feat(telemetry): privacy-safe source names for telemetry`

---

### Task 4: Switches, user properties, crash keys (TelemetryController)

**Files:**
- Modify: `datastore/src/commonMain/kotlin/com/programmersbox/datastore/DataStoreHandling.kt` — add beside `floatingNavigation` (:44):
  `val shareUsageAnalytics = DataStoreHandler(booleanPreferencesKey("shareUsageAnalytics"), true)`, `val sendCrashReports = DataStoreHandler(booleanPreferencesKey("sendCrashReports"), true)`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/telemetry/TelemetryController.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/RepositoryModule.kt` (`singleOf(::TelemetryController)`)
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/KmpOtakuApp.kt` — after `koinSetup()` (:91): `get<TelemetryController>().start(GlobalScope)`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/DesktopUi.kt` — in the `LaunchedEffect` beside `setupRepository.setup(this)` (:287): `telemetryController.start(this)` (inject with `koinInject()`)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/telemetry/TelemetryControllerTest.kt`

**Interfaces:**
- Consumes: Task 3 `TelemetrySourceNamer`; `FakeAnalytics`/`FakeCrashReporter` (`kmpuiviews/src/commonTest/.../testing/FakeTelemetry.kt`); `createTestDataStoreHandling()`, `createTestNewSettingsHandling()`, `InMemoryAiSettings` from `jvmTest/.../testing/`.
- Produces:
  ```kotlin
  class TelemetryController(
      analytics: Analytics, crashReporter: CrashReporter, dataStore: DataStoreHandling,
      newSettings: NewSettingsHandling, aiSettings: AiSettings, sourceRepository: SourceRepository,
      dao: ItemDao, currentSource: CurrentSourceRepository, namer: TelemetrySourceNamer, appConfig: AppConfig,
  ) { fun start(scope: CoroutineScope) }
  ```
  `start` calls `namer.start(scope)` then launches collectors (each `distinctUntilChanged`, each body in `runCatching`):
  - `dataStore.shareUsageAnalytics.asFlow()` → `analytics.setEnabled(it)`
  - `dataStore.sendCrashReports.asFlow()` → `crashReporter.setEnabled(it)`
  - user props: `app_name` = `appConfig.appName` (once); `extension_count` = `Buckets.count(sourceRepository.sources.value.size)` from `sources`; `favorites_count` = `Buckets.count(dao.getAllFavoritesCount())`; `ai_engine` = `aiSettings.preference.name.lowercase()`; `theme_mode` = `newSettings.preferences.data.map { it.themeSetting.name.lowercase() }`; `floating_nav` = `dataStore.floatingNavigation.asFlow()` as `"on"/"off"`
  - crash keys: `current_source` = `namer.name(currentSource.asFlow() source serviceName)` (or `"none"`); `ai_engine` same as prop; `incognito` = `"true"/"false"` whether the current source is incognito (`namer.name(x) == "incognito"`)

- [ ] **Step 1: Write the failing tests** (`runTest`, `backgroundScope` as the start scope, `advanceUntilIdle()` between steps)

```kotlin
@Test fun `switches drive setEnabled`() = runTest {
    controller.start(backgroundScope); advanceUntilIdle()
    assertTrue(analytics.collecting); assertTrue(crash.collecting)
    dataStore.shareUsageAnalytics.set(false); dataStore.sendCrashReports.set(false); advanceUntilIdle()
    assertFalse(analytics.collecting); assertFalse(crash.collecting)
    dataStore.shareUsageAnalytics.set(true); advanceUntilIdle(); assertTrue(analytics.collecting)
}
@Test fun `user properties are bucketed`() = runTest {
    /* insert 7 favorites */ controller.start(backgroundScope); advanceUntilIdle()
    assertEquals("6-20", analytics.userProperties["favorites_count"])
    assertEquals("test", analytics.userProperties["app_name"]) // AppConfig("test", …)
    assertEquals("automatic", analytics.userProperties["ai_engine"])
}
@Test fun `current source key uses namer`() = runTest {
    /* incognito row for "Src", currentSource.emit(source "Src") */
    controller.start(backgroundScope); advanceUntilIdle()
    assertEquals("incognito", crash.keys["current_source"]); assertEquals("true", crash.keys["incognito"])
}
@Test fun `runs with no-op telemetry`() = runTest {
    TelemetryController(NoOpAnalytics, NoOpCrashReporter, /* … */).start(backgroundScope); advanceUntilIdle() // no throw
}
```
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*TelemetryControllerTest"` — Expected: FAIL.
- [ ] **Step 3: Implement** settings keys, controller, Koin binding, both start calls.
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest --tests "*TelemetryControllerTest" :UIViews:compileNoFirebaseDebugKotlin :mangaworld:desktop:compileKotlinJvm` — Expected: PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(telemetry): opt-out switches, user properties and crash keys`

---

### Task 5: Privacy & Security "Usage & crash data" section

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/behavior/UsageAndCrashSection.kt`
- Modify: `PrivacySecurityScreen.kt` (call the section after the existing `CategoryGroupListItem`)
- Modify: `kmpuiviews/src/commonMain/composeResources/values/strings.xml`
- Modify: `SettingsSearchRegistry.kt` Privacy section (:312-325)

**Interfaces:** Produces `@Composable fun UsageAndCrashSection(analytics: Analytics = koinInject(), crashReporter: CrashReporter = koinInject(), dataStore: DataStoreHandling = koinInject())` — renders nothing when `!analytics.isAvailable && !crashReporter.isAvailable`.

Layout and copy are fixed by mock option C (exact strings):
- Label `Usage & crash data`
- Switch row `Share usage analytics` / `Which features get used.` (icon `Icons.Default.BarChart`) bound to `dataStore.shareUsageAnalytics.asState()`
- Switch row `Send crash reports` / `Helps fix bugs faster.` (icon `Icons.Default.BugReport`) bound to `sendCrashReports`
- When crash switch off: `AnimatedVisibility(expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut())` text `Crash reporting turns off the next time the app starts.`
- Chip groups (non-interactive `AssistChip`/`SuggestionChip` in `FlowRow`), group alpha `animateFloatAsState(if (on) 1f else 0.4f)`:
  - `Usage analytics sends`: Features used, Screen names, Source & extension names, Settings choices, Rounded counts, Rounded durations, AI engine used — fades when analytics off
  - `Crash reports send`: Crash stack trace, Recent app steps, Current screen, Current source, App version, Commit, Build type — fades when crash off
  - `Firebase adds to both`: Device model, Android version, Language, Country (from IP), App instance ID — fades when both off
  - `Never sent` (struck-through `TextDecoration.LineThrough`, outlined): Titles, Links, Searches, Notes, List names, Account email, Files
  - Footnote: `Incognito sources are sent as "incognito".`
- Row `Full details` / `Every event and field, on GitHub` (icon `Icons.Default.Description`, trailing `Icons.AutoMirrored.Filled.OpenInNew`) → `LocalUriHandler.current.openUri("https://github.com/jakepurple13/OtakuWorld/blob/develop/TELEMETRY.md")`
- All strings in `strings.xml` (ids prefixed `telemetry_`). Lazy/animated items follow existing settings style.
- Search entries: `Share usage analytics` (keywords analytics, usage, tracking, telemetry, privacy) and `Send crash reports` (crash, crashlytics, bug, report, telemetry), `targetScreen = Screen.Settings.PrivacySecurity`, breadcrumb `[Screen.Settings.Behavior, Screen.Settings.PrivacySecurity]`.

UI-only; verified on device in Task 7.

- [ ] **Step 1: Implement.**
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest :mangaworld:compileNoFirebaseDebugKotlin` — Expected: PASS / BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** `feat(telemetry): usage and crash data section in Privacy & Security`

---

### Task 6: TELEMETRY.md, README links, drift test

**Files:**
- Create: `TELEMETRY.md` (repo root)
- Modify: `README.md` — new `## Privacy & Telemetry` after `## Cloud Sync` (before `## Contents` at :51) and a TOC link; `README-zh.md` — same after `## 云同步` (:35-43), link text in Chinese, linking the English `TELEMETRY.md`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/telemetry/TelemetryDocTest.kt`

**Interfaces:** Consumes `telemetryEventNames` (Task 1).

`TELEMETRY.md` sections: When anything is sent (only `full` builds from the Play Store/GitHub releases with google-services, only with the switches on; noFirebase and desktop send nothing); How to turn it off; Usage analytics — a table of all 47 events with "fires when" and params (copy from Task 1 table, plain-English "fires when" from the spec catalog); User properties; Crash reports — custom keys, breadcrumb categories, non-fatals; What Firebase adds automatically; Never sent; Source names (incognito/custom rule). Note at top: "Update this file in the same PR as any event change."

- [ ] **Step 1: Write the failing test**

```kotlin
class TelemetryDocTest {
    private val doc = File("../TELEMETRY.md").readText() // jvmTest working dir is the module dir
    @Test fun `every event is documented`() = telemetryEventNames.forEach { assertTrue("`$it`" in doc, "$it missing from TELEMETRY.md") }
    @Test fun `readme links the doc`() = assertTrue("TELEMETRY.md" in File("../README.md").readText())
}
```
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*TelemetryDocTest"` — Expected: FAIL (file missing).
- [ ] **Step 3: Write** `TELEMETRY.md` and the README sections.
- [ ] **Step 4: Run** the test — Expected: PASS.
- [ ] **Step 5: Commit** `docs(telemetry): TELEMETRY.md and README links`

---

### Task 7: Full build and device check

- [ ] **Step 1:** `./gradlew :telemetry:jvmTest :kmpuiviews:jvmTest :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug :mangaworld:desktop:compileKotlinJvm :animeworld:desktop:compileKotlinJvm :novelworld:desktop:compileKotlinJvm` — Expected: all pass.
- [ ] **Step 2:** Copy `../../../mangaworld/google-services.json` to `mangaworld/`, `./gradlew :mangaworld:assembleFullRelease`, install on Samsung `R5CX12GDWPZ` (`adb -s R5CX12GDWPZ install -r`), open Settings › Behavior › Privacy & Security. Expected: section visible; toggling analytics off then logs `FA-SVC` "Analytics collection disabled" (with `setprop log.tag.FA-SVC VERBOSE`); user props `favorites_count`, `ai_engine`, `app_name` appear in FA-SVC logcat. Screenshot the section.
- [ ] **Step 3:** noFirebase debug on `MBX6P240401889`: section absent. Screenshot.
- [ ] **Step 4:** Delete `mangaworld/google-services.json`; `git status` clean of it.

Then open PR 2a per finishing-a-development-branch.
