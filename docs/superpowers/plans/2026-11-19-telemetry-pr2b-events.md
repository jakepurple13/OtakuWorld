# Telemetry PR 2b — Event Wiring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fire the 47 catalog events from the apps, plus app-specific crash keys/user properties and the new breadcrumbs and non-fatals.

**Architecture:** Each event fires at the one place its action completes (mapped 2026-11-19, below). Koin-built classes take `Analytics` (and `TelemetrySourceNamer` when a source is sent) as constructor params; composables use `koinInject()`; receivers/workers that are `KoinComponent` use `by inject()`. Origins (`from`) are plumbed through routes and method params with a default of `OpenedFrom.OTHER`.

**Tech Stack:** Kotlin Multiplatform, Koin 4, Compose Multiplatform, WorkManager, kotlin.test, Turbine.

**Spec:** `docs/superpowers/specs/2026-11-18-telemetry-design.md` incl. Amendments (2026-11-19). **Depends on PR 2a** (`AnalyticsEvent` classes in `com.programmersbox.telemetry.events`, `Buckets`, `OpenedFrom`, `TelemetrySourceNamer`, `FakeAnalytics`).

## Global Constraints

- Every `source`/`package` param goes through `TelemetrySourceNamer.name(...)` / `.packageName(...)`.
- `media_type` = `KmpGenericInfo.sourceType` (`manga|anime|novel`).
- Counts and durations via `Buckets`. Exceptions → `e::class.simpleName`, never `message`.
- New route fields and method params default to `OpenedFrom.OTHER` so restores, backups and old saved back stacks keep working (`@Serializable` defaults).
- Backup restore and reader auto-saves must **not** fire `favorite_added` (only user actions pass a `from`; the repository fires only when `from != null`).
- `TELEMETRY.md` updated in the same task as any param change from the catalog.
- Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

- Restoring a backup with 500 favorites → zero `favorite_added` events. Test in Task 2.
- A saved back stack containing an old `Details` route without `from` → deserializes with `"other"`. Test in Task 1.
- `favorites_searched` fires on every debounced keystroke including empty query → only non-blank queries fire. Test in Task 3.
- A reader opened and closed instantly (0 pages) → `reading_session` still well-formed (`pages_read` "0"). Test in Task 5.
- Update check with null report (retry no-op) → no `update_check_finished`. Test in Task 7.

---

## Call-site map (2026-11-19)

Paths: `K` = `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/`, `VMM` = `K/di/ViewModelModule.kt`, `RM` = `K/di/RepositoryModule.kt`, `AM` = `K/di/AppModule.kt`, `S` = `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/`.

| Event | Fire at | DI |
|---|---|---|
| onboarding_finished / onboarding_step | `K/presentation/onboarding/OnboardingScreen.kt` :115 (skip) :159 (finish); step via `snapshotFlow { pagerState.currentPage }` at :98 | composable koinInject |
| source_selected | user picks only: `SourceChooserScreen.kt:110/:140`, `RecentScreen.kt:110`, `InstalledPane.kt:117/:125` → route through one new `CurrentSourceRepository.select(source)` that sets + logs | singleOf RM:19 |
| search (global) | `GlobalSearchViewModel.kt:83` (source_count `sourcesToSearch().size`, results `searchListPublisher.sumOf { it.data.size }` when done) | VMM:66 |
| search (source) | `AllViewModel.kt:115`, `SourceSearchViewModel.kt:75` | VMM:69, :67 |
| describe_search | `FavoriteViewModel.kt:243-249` | VMM:65 |
| item_viewed | `DetailsViewModel.kt:202` | VMM:93 |
| favorite_added / favorite_removed | `FavoritesRepository.kt:30` `addFavorite(db, from: String? = null)` fires only when `from != null`; `:42` removeFavorite(db, user: Boolean = false). Callers: `DetailsViewModel:493` (details), `OptionsSheet.kt:720` (long_press), `RecentViewModel:181` (recent) | `single{}` RM:22 |
| list_item_added | route `DetailsUtils.kt:131`, `OptionsSheet.kt:607`, `DetailsScreen.kt:369`, `DetailsLandscape.kt:204`, `DetailsPortrait.kt:223` through `ListRepository.addToList` (`K/repository/ListRepository.kt:32`), which fires with `list_count` | singleOf RM:27 |
| list_created | `ListChoiceScreen.kt:131`, `OtakuListView.kt:173` → `ListRepository.create(...)` (:37) fires; NOT `createList` (:62, used by import/backup) | RM:27 |
| list_imported | `ImportListViewModel.kt:60`, `ImportFullListViewModel.kt:72` | VMM:71, :70 |
| item_shared | `sharedcomponents/.../qrcode/QrCodeViewer.kt:258` card, :270 save, :292 link (koinInject at :174); `qr` when `ShareViaQrCode` opens (`DetailsUtils.kt:317`, `OptionsSheet.kt:570`); `link` at `DetailsScreen.kt:358/:588/:739`. sharedcomponents needs `implementation(projects.telemetry)` | composable |
| note_saved | `DetailsNotesViewModel.kt:45` (is_new = current == null); edits `NoteDetailViewModel.kt:47`, `AllNotesViewModel.kt:52` (false) | VMM:111-113 |
| mood_tag_added | `MoodRepository.kt:47` toggle-add (preset if `isBuiltIn`), :57 create (custom), :68 suggest accept (ai) | singleOf AM:106 |
| update_check_toggled | `FavoritesRepository.kt:82` `toggleNotify` | RM:22 |
| favorites_filtered | `FavoriteScreen.kt:267` onApply (filters = `activeFilterCount`, sort) | composable |
| favorites_searched | `FavoriteViewModel.kt:225` (non-blank only) | VMM:65 |
| history_cleared | `HistoryViewModel.kt:172` all, :163 item | VMM:105 |
| backup_created | `backuprestore/BackupWizardViewModel.kt:110` (file); cloud `S/ui/viewmodel/BackupRestoreViewModel.kt:33` (supabase-integration needs `implementation(projects.telemetry)`) | VMM:74; S/di/SupabaseModule.kt:101 |
| restore_finished | `RestoreWizardViewModel.kt:139` (categories = `results.size`) | VMM:82 |
| sync_sign_in | `S/auth/AuthManagerImpl.kt:41` on transition to `Authenticated` from not-authenticated only (skip session restore) | `single<AuthManager>` S/di:67 |
| sync_failed | `S/sync/SyncManager.kt` catches :108 :120 :131 :152 :179 | S/di:80 |
| ai_task | `K/domain/aitext/DefaultAiText.kt:44-56` `run` (time around collection; outcome via try/catch); daily overview: `DailyOverviewViewModel.kt:105`, `DailyOverviewWorker.kt:55` (feature `daily_overview`) | `single<AiText>` AM:107 |
| ai_model_installed | `kmpuiviews/src/usesJvmMain/.../modelimport/ModelImporter.kt:91` (import); Nano `androidMain/.../dailyoverview/NanoEngine.kt:42` (download) | PlatformModelImports |
| translation_used | `DetailsViewModel.kt:376` (description), :396 (tldr) | VMM:93 |
| quick_action_used | `QuickActionsViewModel.kt:117` (surface read before `closeAll()` :118; null → `"menu"`) | VMM:122 |
| daily_overview_opened | `DailyOverviewViewModel.kt:66` init, from route field | VMM:59 |
| chapter_opened | manga `mangaworld/shared/.../reader/ReadViewModel.kt:203`; novel `novelworld/shared/.../reader/ReadViewModel.kt:167`; anime `animeworld/shared/.../GenericSharedAnime.kt:69` | MangaModule.kt:35, NovelModule.kt:26, GenericAnime singleOf |
| reading_session | reader composables (`ReaderCompose.kt` pager), `DisposableEffect` onDispose | composable |
| reader_setting_changed | `mangaworld/shared/.../reader/Sheets.kt:86` SettingsSheet controls; novel `NRV:66` `setRecapEnabled` | composable / VM |
| chapter_bookmarked | `BookmarkRepository.insertBookmark` `K/repository/BookmarkRepository.kt:28` | singleOf RM:29 |
| dictionary_lookup | `DictionaryFormViewModel.kt:53` (add when existing == null), `DictionaryDetailViewModel.kt:19` (lookup) | VMM:115-116 |
| episode_play_requested | `animeworld/shared/.../episodeload/EpisodeLoader.kt:203` (`sources.size`) | plain val — pass Analytics in |
| playback_started | `animeworld/shared/src/androidMain/.../VideoPlayerCompose.android.kt:415` onRenderedFirstFrame; desktop `DesktopPlayer.kt:298` | composable |
| playback_error | Android :488 onPlayerError (`errorCodeName`); desktop `DesktopPlayer.kt:348`; cast `cast/CastController.kt:310` | composable |
| download_started | manga Android `MangaDownloadManager.android.kt:135`, desktop `.jvm.kt:196`; anime Android `GenericAnime.kt:364`, desktop `AnimeDownloadManager.kt:98` | singleOf |
| download_finished | `DownloadChapterWorker.kt:129/130/146`; `EpisodeDownloadWorker.kt:152/153/165`; desktop manga `.jvm.kt:106/107/110`; desktop anime `AnimeDownloadManager.kt:156/157/160` (duration measured in doWork/run) | workerOf / KoinComponent |
| update_check_finished | Android `kmpuiviews/src/androidMain/.../workers/UpdateChecker.kt:88`; desktop `BackgroundWorkHandlerImpl.kt` SyncCheckWorker after `getFavoritesThatNeedUpdates` | workerOf WorkerModule.kt:24 / KoinComponent |
| update_notification_opened | add extra `"telemetry_from"="update_notification"` to `info.deepLinkDetails` intent (`UpdateNotification.kt:135`); fire in `UIViews/.../BaseMainActivity.kt:120` handleDeepLink | KoinComponent |
| catch_up_used | `NotificationCatchUp.kt:245-254` call sites → `CatchUpViewModel.advanceCatchUp(action)` (:99) | VMM:61 |
| notification_dismissed | `receivers/SwipeAwayReceiver.kt:15` | KoinComponent |
| extension installed/updated/uninstalled (APK) | `sharedutils/kmpextensionloader/src/androidMain/.../SourceLoader.kt:100-134` receiver ADDED/REPLACED/REMOVED (sharedutils:kmpextensionloader needs `implementation(projects.telemetry)` or pass a callback from kmpuiviews) | Koin ctor |
| extension_install_failed | `receivers/PackageInstallReceiver.kt:74` fail() | KoinComponent |
| extension (catalog/desktop) | `ExtensionListViewModel.kt:88` installAll, :106 uninstall; desktop `jvmMain/.../repository/DownloadStateRepository.kt:75-96` | VMM:63 |
| source_update_skipped | `UpdateSourcesViewModel.kt:99` setSkipped (one event per package) | VMM:120 |
| source_error | catches: `RecentViewModel.kt:156`, `DetailsViewModel.kt:192`, `SourceSearchViewModel.kt:80`, `GlobalSearchViewModel.kt:104`, manga `ReadViewModel` :118/:287/:328/:464/:498, `MediaUpdateChecker.kt:304/:351`; add a catch to `AllViewModel` `getListFlow` (:95) | per class |

---

### Task 1: Origin plumbing

**Files:** `K/presentation/Screen.kt` (`DetailsScreen.Details` + `val from: String = OpenedFrom.OTHER`; `DailyOverview` → `data class DailyOverview(val from: String = OpenedFrom.OTHER)` keeping nested `Settings`/`Engine` objects; references to the instance become `Screen.DailyOverview()`), `K/presentation/navactions/Navigation3Actions.kt` (`details(..., from)`, `dailyOverview(from)`), every caller of those nav actions, manga/novel reader nav args (`from`), `FavoritesRepository.addFavorite(db, from: String? = null)`.

**Interfaces:** Produces `Screen.DetailsScreen.Details.from`, `Screen.DailyOverview.from`, `NavigationActions.details(..., from: String = OpenedFrom.OTHER)`, reader routes `from`.

- [ ] **Step 1: Failing test** `kmpuiviews/src/commonTest/.../presentation/ScreenOriginTest.kt`:
```kotlin
@Test fun oldDetailsJsonDefaultsToOther() {
    val json = """{"title":"t","description":"d","url":"u","imageUrl":"i","source":"s"}"""
    assertEquals("other", Json.decodeFromString<Screen.DetailsScreen.Details>(json).from)
}
@Test fun dailyOverviewDefaultsToOther() = assertEquals("other", Screen.DailyOverview().from)
```
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*ScreenOriginTest"` — FAIL.
- [ ] **Step 3: Implement**; set `from` at each caller: Recent → `RECENT`, All → `ALL`, GlobalSearch/SourceSearch → `SEARCH`, Favorites → `FAVORITES`, History → `HISTORY`, Lists → `LISTS`, Notifications/deep links → `NOTIFICATION`/`DEEP_LINK`, QuickActions → `QUICK_ACTION`, Downloads → `DOWNLOADS`. Update `AnalyticsNameTest` if a route's name changes (it must not).
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest :mangaworld:compileNoFirebaseDebugKotlin :animeworld:compileNoFirebaseDebugKotlin :novelworld:compileNoFirebaseDebugKotlin :mangaworld:desktop:compileKotlinJvm` — PASS.
- [ ] **Step 5: Commit** `feat(telemetry): carry navigation origin for analytics`

---

### Task 2: Details, favorites, lists, notes, moods, sharing

Events: item_viewed, favorite_added, favorite_removed, update_check_toggled, list_item_added, list_created, list_imported, item_shared, note_saved, mood_tag_added, translation_used, chapter_bookmarked. Sites per map.

- [ ] **Step 1: Failing tests** (jvmTest, existing VM/repo tests + `FakeAnalytics` injected):
```kotlin
// FavoritesRepositoryTelemetryTest
@Test fun `user add fires with from`() = runTest { repo.addFavorite(db, from = OpenedFrom.DETAILS); assertEquals(FavoriteAdded("Src", "details"), analytics.events.single()) }
@Test fun `restore add fires nothing`() = runTest { repeat(500) { repo.addFavorite(db(it)) }; assertTrue(analytics.events.isEmpty()) }
@Test fun `incognito source is renamed`() = runTest { /* incognito row */ repo.addFavorite(db, from = "details"); assertEquals("incognito", analytics.events.single().params["source"]) }
// DetailsViewModelTest: item_viewed once per load with source/media_type/from
// ListRepositoryTelemetryTest: addToList fires list_item_added(list_count), create fires list_created, createList (import path) fires nothing
// MoodRepositoryTest: toggle-add preset → kind preset; toggle-remove → nothing; create → custom; suggest → ai
// DetailsNotesViewModelTest: first save is_new true, second false
```
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest` — new tests FAIL.
- [ ] **Step 3: Implement** at the mapped sites; route composable `listDao.addToList` calls through `ListRepository.addToList`.
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest :sharedcomponents:compileKotlinJvm` — PASS.
- [ ] **Step 5: Commit** `feat(telemetry): details, favorites, lists, notes and sharing events`

---

### Task 3: Browse, search, library, onboarding, quick actions

Events: source_selected, search (global + source), describe_search, favorites_filtered, favorites_searched, history_cleared, onboarding_finished, onboarding_step, quick_action_used, daily_overview_opened.

- [ ] **Step 1: Failing tests:** `GlobalSearchViewModel` fires `search(scope=global, source_count, result_count bucket)` once per submitted query; `FavoriteViewModel` fires `favorites_searched` for "abc" but not for ""; `HistoryViewModel` clear-all → `history_cleared(all)`; `QuickActionsViewModel.run` → `quick_action_used(action, surface)`; `CurrentSourceRepository.select` → `source_selected` with namer name (startup restore via `SetupRepository` does **not** fire).
- [ ] **Step 2: Run** — FAIL. **Step 3: Implement.** **Step 4: Run** `./gradlew :kmpuiviews:jvmTest` — PASS.
- [ ] **Step 5: Commit** `feat(telemetry): browse, search, library and onboarding events`

---

### Task 4: AI and backup/sync

Events: ai_task, ai_model_installed, backup_created, restore_finished, sync_sign_in, sync_failed.

- [ ] **Step 1: Failing tests:** `DefaultAiText.run` success → `ai_task(feature, engine, "success", latency bucket)`; engine throws → outcome `error`, no message in params; no engine → `unavailable`. `BackupWizardViewModel.confirm` → `backup_created(file, success)`. `SyncManager` failure → `sync_failed(supabase, error_class)` (supabase-integration jvmTest).
- [ ] **Step 2: Run** — FAIL. **Step 3: Implement.** **Step 4: Run** `./gradlew :kmpuiviews:jvmTest :favoritesdatabase:supabase-integration:jvmTest` — PASS.
- [ ] **Step 5: Commit** `feat(telemetry): AI, backup and sync events`

---

### Task 5: Readers (manga, novel)

Events: chapter_opened, reading_session, reader_setting_changed (+ source_error at manga ReadViewModel catches). Crash key `image_loader` and user property for manga reader mode set from `mangaworld/shared` (`MangaNewSettingsHandling.preferences.data.map { it.imageLoaderType }`) set in manga `ReadViewModel` init (`crashReporter.setKey("image_loader", …)` from `mangaSettingsHandling` once per reader open).

- [ ] **Step 1: Failing tests:** manga `ReadViewModel` init fires `chapter_opened(source, "manga", from)`; a pure helper `ReadingSessionTracker` (new, `mangaworld/shared` commonMain; pages seen set + start time via injected clock) produces `ReadingSession(media_type, pages_read bucket, duration bucket, reached_end)`; tracker with 0 pages → pages_read "0".
- [ ] **Step 2: Run** `./gradlew :mangaworld:shared:jvmTest` — FAIL. **Step 3: Implement**; reader composables feed `tracker.pageSeen(i)` from pager `snapshotFlow`, send on `DisposableEffect` dispose; each settings-sheet control calls `analytics.log(ReaderSettingChanged(key, value))` in its onChange. Same for novel reader.
- [ ] **Step 4: Run** `./gradlew :mangaworld:shared:jvmTest :novelworld:shared:compileKotlinJvm :mangaworld:compileNoFirebaseDebugKotlin` — PASS.
- [ ] **Step 5: Commit** `feat(telemetry): reader events`

---

### Task 6: AnimeWorld playback and downloads

Events: chapter_opened (anime), episode_play_requested, playback_started, playback_error, download_started, download_finished (all apps), dictionary_lookup. User property/crash key `player_style` from `videoPlayerStyle.asFlow()` started in `GenericSharedAnime`.

- [ ] **Step 1: Failing tests:** `EpisodeLoader.run` with 3 sources → `episode_play_requested(source, 3)` (pass `Analytics` into `EpisodeLoader`); desktop `AnimeDownloadManager` success/fail/cancel → `download_finished` with outcome (animeworld:desktop jvmTest if present, else animeworld:shared).
- [ ] **Step 2: Run** — FAIL. **Step 3: Implement** incl. Android player listeners and workers (record `start = Clock.System.now()` at doWork start).
- [ ] **Step 4: Run** `./gradlew :animeworld:shared:jvmTest :animeworld:compileNoFirebaseDebugKotlin :animeworld:desktop:compileKotlinJvm :mangaworld:compileNoFirebaseDebugKotlin` — PASS.
- [ ] **Step 5: Commit** `feat(telemetry): playback and download events`

---

### Task 7: Updates, notifications, extensions, source errors, workers

Events: update_check_finished, update_notification_opened, catch_up_used, notification_dismissed, extension_installed/install_failed/uninstalled/updated, source_update_skipped, source_error (remaining catch sites). Crash key `last_worker` set at the start of every worker `doWork` (Android) / `DesktopTask.doWork` (desktop) via `koinCrashReporter().setKey("last_worker", javaClass.simpleName)`. New breadcrumbs: extension load ok/fail in `SourceLoader`, DB migration in Room callbacks if present. Non-fatals: extension load/install failures, player errors (Task 6), AI engine init failures, Room migration failures.

- [ ] **Step 1: Failing tests:** `UpdateCheckFinished` built from an `UpdateCheckReport` by a pure function `updateCheckEvent(report, trigger): UpdateCheckFinished?` (null report → null); `UpdateSourcesViewModel.setSkipped(listOf(a,b))` → two events; `AllViewModel` source failure → `source_error(source, "all", class)` and list still empty; `CatchUpViewModel.advanceCatchUp(read)` → `catch_up_used(read)`.
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest` — FAIL. **Step 3: Implement.** **Step 4: Run** `./gradlew :kmpuiviews:jvmTest :UIViews:compileNoFirebaseDebugKotlin :sharedutils:kmpextensionloader:compileKotlinJvm` — PASS.
- [ ] **Step 5: Commit** `feat(telemetry): update, notification, extension and source error events`

---

### Task 8: Docs sync, full build, device check

- [ ] **Step 1:** Update `TELEMETRY.md` for any param changes made during wiring; `./gradlew :kmpuiviews:jvmTest --tests "*TelemetryDocTest"` PASS.
- [ ] **Step 2:** Full build list from PR 2a Task 7 Step 1 plus `:mangaworld:shared:jvmTest :animeworld:shared:jvmTest`.
- [ ] **Step 3:** Samsung `R5CX12GDWPZ`, full release with copied `google-services.json`, `setprop debug.firebase.analytics.app com.programmersbox.mangaworld` + `log.tag.FA-SVC VERBOSE`: open a title from Recent → `item_viewed(from=recent)`; favorite → `favorite_added(from=details)`; open a chapter, read 3 pages, close → `chapter_opened`, `reading_session`; search → `search`. Capture FA-SVC lines. Delete `google-services.json` after.
- [ ] **Step 4:** Open PR 2b.
