# Daily Overview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A full-screen recap of yesterday, opened from quick actions. It has stats, an hour graph,
lists, and an AI summary written by Gemini Nano, Gemma, a Koog provider, Ollama, or a template.
It comes with a settings page, an engine picker, and an Android morning notification.

**Architecture:**
- `DailyFactsBuilder` (commonMain) turns DB rows into a `DailyFacts` value.
- A `SummaryEngine` interface has one implementation per backend.
- `resolveEngine()` picks which engine is used.
- `DailyOverviewViewModel` combines the facts, the cached summary, and the streamed text.
- The Koog engines live in `usesJvmMain`, the only source set that sees `koogintegration`.
- The Nano engine, the worker, and the notification live in `androidMain`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Room 3 KMP, DataStore, Koin
(+ navigation3 DSL, `workerOf`), Koog (LiteRT, Ollama, provider clients), ML Kit GenAI Prompt API,
WorkManager.

**Spec:** `docs/superpowers/specs/2026-11-11-daily-overview-design.md`
**Approved mockup:** https://claude.ai/artifact/11rG7q8iYEf8XzwP5TbP28 (v2). Match its motion and copy.

## Global Constraints

- The overview covers yesterday only, in the system time zone. It never pages back.
- Every number shown on screen or in the notification comes from the DB. The model receives only
  `DailyFacts` JSON.
- Automatic never resolves to `Provider`. Android order: Nano → Gemma → Template. Desktop order:
  Ollama → Template.
- No Gemma download UI in this feature. Link to the AI Helper's models route (`KoogModels`).
- DataStore keys and defaults:
  - `dailyOverviewEnabled` = true
  - `dailyOverviewNotify` = false
  - `dailyOverviewTime` = 480 (minutes of day)
  - `dailyOverviewSkipQuiet` = true
  - `dailyOverviewEngine` = "Automatic"
  - `dailyOverviewCache` = ""
- Notification channel "Daily overview" with low importance. Android only. There is no desktop
  notification.
- The noun comes from `KmpGenericInfo.updateUnit` (`Chapters` / `Episodes`).
- Every lazy item and sticky header gets `Modifier.animateItem()`.
- Use `LocalNavHostPadding` for bottom padding, so content clears the floating nav.
- Use the `noFirebase` flavor for builds. Run a module's tests before each commit.

## Review Focus

1. **Midnight and time-zone boundaries:** a read at 23:59 local belongs to yesterday, and a read at
   00:00 belongs to today. Test this in Task 3 with a non-UTC zone.
2. **Engine fails mid-stream** (LiteRT crash, Ollama stops, network drops): the text already shown
   is replaced by the template sentence, the stats stay, and nothing is cached. Tested in Task 5.
3. **Day rolls over while the screen is open, or the cache is from the day before:** a cached
   summary for another date is never shown. Tested in Task 5.
4. **Feature turned off while work is scheduled:** the work is cancelled and the quick action row
   disappears without restarting the app. Tested in Tasks 9 and 11.
5. **Incognito titles and sources** must not appear in facts sent to a model. `addWatched` already
   skips incognito items. The Task 3 builder also filters out titles whose url or source is in
   `ItemDao.getAllIncognitoSourcesSync()` (this covers rows inserted directly or by sync), and the
   Task 3 test covers it.

---

### Task 1: Timestamp reads

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/FavoritesRepository.kt:50-58`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/FavoritesRepositoryTest.kt`

**Interfaces:**
- Produces: `FavoritesRepository(dao, systemAlerter, authManager, clock: Clock = Clock.System)`.
  `addWatched` stores `createdAt = updatedAt = clock.now().toEpochMilliseconds()`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun `addWatched stamps createdAt and updatedAt with now`() = runTest {
    val fixed = Instant.fromEpochMilliseconds(1_762_800_000_000)
    val repo = repository(clock = FixedClock(fixed))
    repo.addWatched(ChapterWatched(url = "c1", name = "Ch 1", favoriteUrl = "f1"))
    val row = database.itemDao().getChapterByUrl("c1")!!
    assertEquals(fixed.toEpochMilliseconds(), row.createdAt)
    assertEquals(fixed.toEpochMilliseconds(), row.updatedAt)
}

@Test fun `re-reading a chapter moves createdAt to the latest read`() = runTest { /* two adds with clocks t1 < t2 → createdAt == t2 */ }
```

- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*FavoritesRepositoryTest*"`. Expected: FAIL (no `clock` parameter).
- [ ] **Step 3:** Add the `clock` constructor parameter with a default, so Koin `singleOf` still
  works. In `addWatched`, copy the timestamps onto the row before `dao.insertChapter`. Add a
  `FixedClock` test helper to `testing/Fakes.kt`.
- [ ] **Step 4:** Run the same command. Expected: PASS, along with all other `FavoritesRepositoryTest` cases.
- [ ] **Step 5:** Commit `feat(daily-overview): timestamp chapter reads`.

---

### Task 2: Settings keys

**Files:**
- Modify: `datastore/src/commonMain/kotlin/com/programmersbox/datastore/DataStoreHandling.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/dailyoverview/DailyOverviewSettings.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/dailyoverview/DailyOverviewSettingsTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class SummaryEngineChoice { Automatic, Nano, Gemma, Provider, Ollama, Template }
  data class DailyOverviewPrefs(val enabled: Boolean, val notify: Boolean, val minuteOfDay: Int,
                                val skipQuiet: Boolean, val engine: SummaryEngineChoice)
  class DailyOverviewSettings(dataStoreHandling: DataStoreHandling) {
      val prefs: Flow<DailyOverviewPrefs>
      suspend fun setEnabled(v: Boolean); suspend fun setNotify(v: Boolean)
      suspend fun setMinuteOfDay(v: Int); suspend fun setSkipQuiet(v: Boolean)
      suspend fun setEngine(v: SummaryEngineChoice)
      suspend fun cached(): DailySummary?; suspend fun cache(s: DailySummary)
  }
  @Serializable data class DailySummary(val date: LocalDate, val engine: SummaryEngineId, val text: String)
  ```
  `SummaryEngineId` is defined in Task 4. Put it in `SummaryEngine.kt` and import it here. Tasks 2
  and 4 can merge if the executor prefers.

- [ ] **Step 1:** Test that defaults equal the Global Constraints values. Test that each setter
  round-trips. Test that an unknown engine string reads as `Automatic`. Test that a cache
  round-trips and that corrupt JSON in the cache reads as `null`. Use `createTestDataStoreHandling()`.
- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*DailyOverviewSettingsTest*"`. Expected: FAIL.
- [ ] **Step 3:** Add six `DataStoreHandler`s to `DataStoreHandling` with the exact keys and
  defaults. Then implement `DailyOverviewSettings` on top of them, and register
  `singleOf(::DailyOverviewSettings)` in `kmpuiviews/.../di/AppModule.kt`.
- [ ] **Step 4:** Run again. Expected: PASS.
- [ ] **Step 5:** Commit `feat(daily-overview): settings keys`.

---

### Task 3: `DailyFacts` builder

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/dailyoverview/DailyFacts.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/dailyoverview/DailyFactsBuilder.kt`
- Modify: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/ItemDao.kt` (two queries)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/dailyoverview/DailyFactsBuilderTest.kt`

**Interfaces:**
- Consumes: Task 1 timestamps.
- Produces:
  ```kotlin
  @Serializable data class TitleRead(val title: String, val url: String, val imageUrl: String, val source: String,
      val firstChapter: String, val lastChapter: String, val count: Int, val caughtUp: Boolean)
  @Serializable data class UpdateItem(val title: String, val url: String, val imageUrl: String?, val source: String, val summary: String)
  @Serializable data class DailyFacts(val date: LocalDate, val unit: UpdateUnit, val readCount: Int,
      val titles: List<TitleRead>, val readsByHour: List<Int> /* size 24 */, val updates: List<UpdateItem>,
      val failedSources: List<String>) { val isQuiet: Boolean get() = readCount == 0 && updates.isEmpty() }
  class DailyFactsBuilder(itemDao: ItemDao, heatMapDao: HeatMapDao, historyDao: HistoryDao,
      reportStore: UpdateCheckReportStore, genericInfo: KmpGenericInfo,
      clock: Clock = Clock.System, zone: () -> TimeZone = { TimeZone.currentSystemDefault() }) {
      suspend fun yesterday(): DailyFacts
      suspend fun forDate(date: LocalDate): DailyFacts
  }
  ```
- New DAO queries:
  - `ItemDao.getChaptersWatchedBetween(start: Long, end: Long): List<ChapterWatched>` (`created_at >= :start AND created_at < :end AND is_deleted = 0`).
  - `ItemDao.getNotificationsCreatedBetween(start: Long, end: Long): List<NotificationItem>`.

- [ ] **Step 1: Write the failing tests.** Seed with `createTestItemDatabase()` and fakes, fixed
  clock = 2026-11-11T10:00, zone `America/Phoenix`.
  - `readCount comes from the heat map, not row count`: heat map 23, 5 watched rows → 23.
  - `reads at 23-59 local count for yesterday and 00-00 does not`.
  - `rows with createdAt 0 are excluded and readsByHour is all zero`.
  - `titles group by favoriteUrl with first and last chapter by createdAt order`.
  - `caughtUp is true when watched count for the title equals DbModel numChapters`.
  - `title falls back to RecentModel then to the url host when no favorite exists`.
  - `updates are notifications created from yesterday start until now`.
  - `failedSources come from the last report's sourceFailures`.
  - `incognito favoriteUrl rows are excluded` (Review Focus 5).
  - `isQuiet when no reads and no updates`.
- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*DailyFactsBuilderTest*"`. Expected: FAIL.
- [ ] **Step 3:** Implement. Day bounds come from
  `date.atStartOfDayIn(zone)` to `date.plus(1, DAY).atStartOfDayIn(zone)`. The hour index is
  `Instant.toLocalDateTime(zone).hour`. Add the two DAO queries. Register
  `factoryOf(::DailyFactsBuilder)` in `AppModule.kt`.
- [ ] **Step 4:** Run again plus `./gradlew :favoritesdatabase:jvmTest`. Expected: PASS.
- [ ] **Step 5:** Commit `feat(daily-overview): build yesterday's facts`.

---

### Task 4: Engine contract, template, prompt, resolver

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/dailyoverview/SummaryEngine.kt`
- Create: `.../domain/dailyoverview/TemplateEngine.kt`, `.../domain/dailyoverview/SummaryPrompt.kt`, `.../domain/dailyoverview/EngineResolver.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/dailyoverview/EngineResolverTest.kt`, `TemplateEngineTest.kt`, `SummaryPromptTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class SummaryEngineId { Nano, Gemma, Provider, Ollama, Template }
  sealed interface EngineStatus { data object Ready; data class NeedsSetup(val reason: String); data object Unavailable }
  interface SummaryEngine {
      val id: SummaryEngineId
      /** Shown on the card, e.g. "Gemma 4 E2B · on device". */
      suspend fun label(): String
      /** True when nothing leaves the device. */
      val onDevice: Boolean
      suspend fun status(): EngineStatus
      fun summarize(facts: DailyFacts): Flow<String>
  }
  class TemplateEngine : SummaryEngine            // always Ready, onDevice = true
  object SummaryPrompt { val system: String; fun user(facts: DailyFacts): String }
  /** Order: Android [Nano, Gemma], desktop [Ollama]. Template is the final fallback. */
  class EngineResolver(engines: List<SummaryEngine>, val automaticOrder: List<SummaryEngineId>) {
      suspend fun resolve(choice: SummaryEngineChoice): SummaryEngine
      suspend fun available(): Map<SummaryEngineId, EngineStatus>
  }
  ```
- `automaticOrder` comes from an `expect val platformAutomaticOrder: List<SummaryEngineId>`.
  Android: `[Nano, Gemma]`. JVM: `[Ollama]`. iOS: `[]`.

- [ ] **Step 1: Failing tests:**
  - `automatic picks first Ready in order`.
  - `automatic never picks Provider even when it is the only Ready engine`.
  - `explicit choice that is not Ready falls back to Template`.
  - `explicit Provider that is Ready is used`.
  - `choice for an engine not registered on this platform falls back to Template`.
  - Template:
    - `full day`: exact text "You read 23 chapters across 4 titles. 11 new chapters arrived. 1 source failed its update check."
    - `episodes unit` says "episodes".
    - `quiet day`: "Nothing new yesterday."
    - Singular forms.
  - Prompt: `user prompt is the facts JSON`; `system prompt names the unit and forbids numbers not in the facts`.
- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*dailyoverview*"`. Expected: FAIL.
- [ ] **Step 3:** Implement. Template plurals go in `composeResources/values/strings.xml` as
  `do_template_*` plurals. The system prompt asks for 2–3 sentences in second person, in plain
  words, using only numbers from the facts and the noun "{chapters|episodes}".
- [ ] **Step 4:** Run again. Expected: PASS.
- [ ] **Step 5:** Commit `feat(daily-overview): summary engine contract and template`.

---

### Task 5: ViewModel with cache and stream

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dailyoverview/DailyOverviewViewModel.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/dailyoverview/DailyOverviewViewModelTest.kt`

**Interfaces:**
- Consumes: Tasks 2–4.
- Produces:
  ```kotlin
  sealed interface SummaryState {
      data object Loading                                    // shimmer
      data class Streaming(val text: String, val label: String, val onDevice: Boolean)
      data class Done(val text: String, val label: String, val onDevice: Boolean, val engine: SummaryEngineId)
  }
  data class DailyOverviewUiState(val facts: DailyFacts?, val summary: SummaryState, val needsSetup: Boolean)
  class DailyOverviewViewModel(factsBuilder: DailyFactsBuilder, resolver: EngineResolver,
      settings: DailyOverviewSettings, template: TemplateEngine) : ViewModel() {
      val uiState: StateFlow<DailyOverviewUiState>
      fun regenerate()
  }
  ```
- Also produces the shared helper used by the worker in Task 11:
  `suspend fun generateDailySummary(facts, resolver, settings, template, bypassCache: Boolean): DailySummary`,
  in `domain/dailyoverview/GenerateDailySummary.kt`.

- [ ] **Step 1: Failing tests** (`StandardTestDispatcher`, fake engines emitting chunks):
  - `facts render before the summary finishes`.
  - `cached summary for yesterday is shown without calling the engine`.
  - `cached summary for another date is ignored` (Review Focus 3).
  - `regenerate bypasses cache and re-caches`.
  - `quiet day skips the engine and shows template text`.
  - `engine throwing mid-stream ends in Done with template text, nothing cached` (Review Focus 2).
  - `needsSetup true when the choice resolved to Template but the choice was not Template`.
- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*DailyOverviewViewModelTest*"`. Expected: FAIL.
- [ ] **Step 3:** Implement. Text accumulates with `scan`. Register `viewModelOf(::DailyOverviewViewModel)`.
  In `AppModule`, register `TemplateEngine` as a `single`. The `EngineResolver` comes from
  `getAll<SummaryEngine>()` plus `platformAutomaticOrder`.
- [ ] **Step 4:** Run again. Expected: PASS.
- [ ] **Step 5:** Commit `feat(daily-overview): view model with cached streamed summary`.

---

### Task 6: Koog engines (Gemma, Provider, Ollama)

**Files:**
- Modify: `kmpuiviews/koogintegration/src/commonMain/kotlin/com/programmersbox/koogintegration/AgentMaker.kt`. Add `suspend fun selection(): KoogSelection?`, which returns the company and model without throwing.
- Modify: `kmpuiviews/koogintegration/src/androidMain/kotlin/com/programmersbox/koogintegration/AgentMaker.android.kt`. Add `fun downloadedLiteRtModels(): List<LLModel>`, which matches `ModelManager.listModels()` file names against `LiteRTLLModels.models`, and `fun liteRtClient(): LLMClient`.
- Create: `kmpuiviews/src/usesJvmMain/kotlin/com/programmersbox/kmpuiviews/dailyoverview/KoogSummaryEngine.kt` (shared streaming body)
- Create: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/dailyoverview/GemmaEngine.kt`, `ProviderEngine.kt`
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/dailyoverview/OllamaEngine.kt`
- Modify: `kmpuiviews/src/usesJvmMain/kotlin/com/programmersbox/kmpuiviews/di/KmpModule.usesJvm.kt` and the platform modules to bind the engines with `bind SummaryEngine::class`.
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/dailyoverview/KoogEnginesTest.kt`

**Interfaces:**
- Consumes: Task 4 `SummaryEngine` and `SummaryPrompt`.
- Produces:
  ```kotlin
  data class KoogSelection(val company: String, val model: String)
  abstract class KoogSummaryEngine : SummaryEngine {
      protected abstract suspend fun client(): Pair<LLMClient, LLModel>?
      final override fun summarize(facts: DailyFacts): Flow<String> // LLMClient.executeStreaming(prompt, model), text frames only
  }
  ```
- Gemma picks the AI Helper's LiteRT model when the selected company is LiteRT. Otherwise it picks
  the first downloaded model. None downloaded → `NeedsSetup("Download a model in AI Helper")`.
  Label: "Gemma 4 E2B · on device" (derive the display name from the model id).
- Provider is Ready only when the company is not LiteRT, not Ollama, and not None. Label:
  "<company> · via Koog". `onDevice = false`.
- Ollama is Ready when the company is Ollama and `GET http://localhost:11434/api/tags` succeeds
  within 2s. When the request fails → `Unavailable` with the reason "Ollama isn't running".
  Label: "Ollama · <model>". `onDevice = true`.

- [ ] **Step 1: Failing tests** (jvmTest, fake `KoogDataStore` flows, Ktor `MockEngine`):
  - `ollama ready when company is Ollama and tags responds`.
  - `ollama unavailable when tags fails`.
  - `ollama needs setup when company is not Ollama`.
  - `koog engine emits only text frames in order` (fake `LLMClient`).
- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*KoogEnginesTest*"`. Expected: FAIL.
- [ ] **Step 3:** Implement. Check the Koog version's streaming API name in
  `gradle/libs.versions.toml` / IDE (`executeStreaming` returning `Flow<StreamFrame>`). Map
  `StreamFrame.Append` to text. Gemma and Provider have no jvmTest. They are verified on device in
  Task 12.
- [ ] **Step 4:** Run again, plus `./gradlew :kmpuiviews:koogintegration:jvmTest :mangaworld:assembleNoFirebaseDebug`. Expected: PASS and BUILD SUCCESSFUL.
- [ ] **Step 5:** Commit `feat(daily-overview): koog-backed summary engines`.

---

### Task 7: Gemini Nano engine

**Files:**
- Modify: `kmpuiviews/build.gradle.kts` (androidMain dependency), `gradle/libs.versions.toml` (`mlkit-genai-prompt`)
- Create: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/dailyoverview/NanoEngine.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/dailyoverview/NanoStatus.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/dailyoverview/NanoStatusTest.kt`

**Interfaces:**
- Produces: `enum class NanoFeatureStatus { Available, Downloadable, Downloading, Unavailable }` and
  `fun NanoFeatureStatus.toEngineStatus(): EngineStatus`:
  - `Available` → Ready.
  - `Downloadable` / `Downloading` → `NeedsSetup("Downloading Gemini Nano")`.
  - `Unavailable` → Unavailable.

- [ ] **Step 1:** Load the `ml-kit-genai-prompt-api` skill for the current artifact, version, and
  API (`Generation.getClient()`, `checkStatus()`, `download()`, `generateContentStream`). Write
  the mapping test.
- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*NanoStatusTest*"`. Expected: FAIL.
- [ ] **Step 3:** Implement `NanoEngine`. When `status()` sees Downloadable, it starts `download()`
  once in the app scope. `summarize` uses `SummaryPrompt.system + user`. Label "Gemini Nano",
  `onDevice = true`. Bind it in the android platform module.
- [ ] **Step 4:** Run the test and `./gradlew :mangaworld:assembleNoFirebaseDebug`. Expected: PASS and BUILD SUCCESSFUL.
- [ ] **Step 5:** Commit `feat(daily-overview): gemini nano engine`.

---

### Task 8: Daily overview screen

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt`. Add `data object DailyOverview : Screen("daily_overview") { data object Settings; data object Engine }`.
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dailyoverview/DailyOverviewScreen.kt`
- Create: `.../presentation/dailyoverview/components/SummaryCard.kt`, `StatTiles.kt`, `HourGraph.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt`. Add `detailEntry<Screen.DailyOverview>`.
- Modify: `kmpuiviews/src/commonMain/composeResources/values/strings.xml`. Add `do_*` strings.
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/dailyoverview/HourGraphTest.kt`

**Interfaces:**
- Consumes: `DailyOverviewViewModel.uiState`, `regenerate()`, `NavigationActions`.
- Produces: `fun peakHourLabel(readsByHour: List<Int>): String?`. It returns "10 PM", or null
  when all counts are 0.

- [ ] **Step 1:** Write a test for `peakHourLabel`: a tie returns the later hour, `0` returns
  "12 AM", and all zero returns null.
- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*HourGraphTest*"`. Expected: FAIL.
- [ ] **Step 3:** Build the screen to the mockup. Each part maps to these APIs:
  - Summary card:
    - The gradient border runs on `rememberInfiniteTransition` only while `Loading`/`Streaming`.
    - Shimmer while `Loading`.
    - Text uses `animateContentSize()`.
    - The footer has a privacy line ("On device · nothing sent" / "Sent to <label>") and
      Regenerate.
    - When `needsSetup`, show a "Set up AI summary" chip that navigates to `Screen.DailyOverview.Engine`.
  - Stat tiles: `AnimatedContent` per digit with `slideInVertically` + spring.
  - Hour graph: one `Animatable` per bar with a 25ms stagger. The peak bar is tinted tertiary.
    The card is hidden when every hour is 0.
  - "You read" rows: a progress bar using `animateFloatAsState`. Tapping a row opens Details.
  - "Updated overnight": the first 3 updates plus failed sources. "All <n>" calls
    `navigationActions.notifications()`.
  - A `LazyColumn` with `animateItem()` and a first-load index delay of about 50ms.
  - The empty-day state.
  - Back button, title "Yesterday" with the date, and a gear that opens `Screen.DailyOverview.Settings`.
  - Add `@ShowcaseComponent` previews for the summary card states.
- [ ] **Step 4:** Run the test and `./gradlew :mangaworld:desktop:run`. Open the screen through a
  temporary debug link or Task 9, and compare it with the mockup. Expected: PASS, and the screen
  renders.
- [ ] **Step 5:** Commit `feat(daily-overview): overview screen`.

---

### Task 9: Quick action row

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickAction.kt`. Add `DailyOverview(QuickActionGroup.GoTo)` as the first `GoTo` entry.
- Modify: `QuickActionsList.kt` (`rowSpec`, header count uses `actions.size`), `QuickActionsViewModel.kt` (`run`, `uiState.dailyReadCount`, `visibleActions`), `QuickActionCounts.kt` (add `yesterdayReads: Flow<Int>` from the heat map), `QuickActionsFilter.kt` (`texts` map), and the overlay/desktop surfaces that pass `actions`.
- Modify: the shared-element source. The row and the screen root use `Modifier.sharedBounds(rememberSharedContentState("daily-overview"), …)`.
- Test: `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/presentation/quickactions/QuickActionsViewModelTest.kt`, `QuickActionsStateTest.kt`, `QuickActionsFilterTest.kt`

**Interfaces:**
- Produces: `QuickActionsViewModel.visibleActions: StateFlow<List<QuickAction>>`. It excludes
  `DailyOverview` when `DailyOverviewPrefs.enabled` is false.
  `QuickActionsUiState.yesterdayReads: Int?`.

- [ ] **Step 1: Failing tests:**
  - `run DailyOverview navigates to Screen.DailyOverview`.
  - `DailyOverview hidden when disabled and reappears when enabled` (flip the setting in-test, no
    VM recreation; Review Focus 4).
  - Update the group-order expectations.
  - Filter text "Daily overview Yesterday: 23 chapters".
- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*quickactions*"`. Expected: FAIL.
- [ ] **Step 3:** Implement. Row details:
  - Icon: `Icons.Default.AutoAwesome`.
  - Container: a gradient-feel `tertiaryContainer`.
  - Subtitle: "Yesterday: <n> chapters/episodes", or "Your recap of yesterday" when n is 0.
- [ ] **Step 4:** Run again. Expected: PASS, with all existing quick action tests green.
- [ ] **Step 5:** Commit `feat(daily-overview): quick action row`.

---

### Task 10: Settings and engine pages

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/dailyoverview/settings/DailyOverviewSettingsScreen.kt`, `SummaryEngineScreen.kt`, `DailyOverviewSettingsViewModel.kt`
- Modify: `Nav3Graph.kt`. Add entries for `Screen.DailyOverview.Settings` and `.Engine`.
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/general/GeneralSettings.kt`. Add a "Daily overview" row.
- Create: `expect val supportsMorningNotification: Boolean` (android true, jvm/ios false)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/dailyoverview/DailyOverviewSettingsViewModelTest.kt`

**Interfaces:**
- Consumes: `DailyOverviewSettings`, `EngineResolver.available()`, `resolve()`, `SummaryPrompt`, and
  the Task 5 helper with fixed sample `DailyFacts` (for Test).
- Produces:
  ```kotlin
  data class EngineRow(val id: SummaryEngineId, val status: EngineStatus, val label: String, val onDevice: Boolean)
  data class SettingsUi(val prefs: DailyOverviewPrefs, val resolvedLabel: String, val engines: List<EngineRow>)
  class DailyOverviewSettingsViewModel(...) : ViewModel() {
      val ui: StateFlow<SettingsUi>; val test: StateFlow<SummaryState?>
      fun setEnabled(v: Boolean); fun setNotify(v: Boolean); fun setTime(minuteOfDay: Int)
      fun setSkipQuiet(v: Boolean); fun choose(c: SummaryEngineChoice); fun runTest()
  }
  ```
  The scheduler side effect lives in Task 11 (`DailyOverviewScheduler`). The ViewModel must not
  call WorkManager directly.

- [ ] **Step 1: Failing tests:**
  - `resolvedLabel reads Automatic · Gemma 4 E2B when Gemma ready`.
  - `resolvedLabel appends (not ready) when explicit choice is not Ready`.
  - `runTest streams from the chosen engine using sample facts`.
  - `setTime stores minutes of day`.
- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*DailyOverviewSettingsViewModelTest*"`. Expected: FAIL.
- [ ] **Step 3:** Build the screens to the mockup:
  - Settings page:
    - A hero switch row.
    - Sections collapse with `AnimatedVisibility(expandVertically)`.
    - The notification group shows only when `supportsMorningNotification`.
    - The time row opens an M3 `TimePicker` dialog.
  - Engine page:
    - Selectable cards in `selectableGroup`. The radio dot scales with a spring.
    - The selected card expands with `animateContentSize()`.
    - The Gemma card's "Open AI Helper models" button navigates to `KoogModels`.
    - The provider card has a warning box and a "Set up AI Helper" button that navigates to
      `KoogSettings`.
    - A Test card at the bottom.
    - Cards per platform: Android shows Automatic, Nano, Gemma, Provider, Numbers only. Desktop
      shows Automatic, Ollama, Numbers only.
- [ ] **Step 4:** Run the tests, then `./gradlew :mangaworld:desktop:run`, and check the desktop
  engine page with Ollama running and stopped. Expected: PASS.
- [ ] **Step 5:** Commit `feat(daily-overview): settings and summary engine pages`.

---

### Task 11: Morning notification (Android)

**Files:**
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/NotificationEnums.kt`. Add `DailyOverview("dailyOverviewChannel", IMPORTANCE_LOW)`.
- Create: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/DailyOverviewWorker.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/dailyoverview/DailyOverviewScheduler.kt` (interface plus `fun initialDelay(now: LocalDateTime, minuteOfDay: Int, leadMinutes: Int = 10): Duration`). Implementations: `androidMain` (WorkManager) and no-op on jvm/ios.
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/di/WorkerModule.kt` (`workerOf(::DailyOverviewWorker)`).
- Modify: the app startup (where other periodic work is ensured). Collect `DailyOverviewSettings.prefs` and call `scheduler.sync(prefs)`.
- Modify: the deep-link handling for the "Open overview" action (`Screen.DailyOverview`).
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/dailyoverview/DailyOverviewSchedulerTest.kt`

**Interfaces:**
- Produces: `interface DailyOverviewScheduler { fun sync(prefs: DailyOverviewPrefs) }`. The work is
  scheduled only when `enabled && notify`, and cancelled otherwise. Unique name
  `"daily_overview"`. Re-enqueue with `ExistingPeriodicWorkPolicy.UPDATE`.

- [ ] **Step 1: Failing tests for `initialDelay`:**
  - 07:00 with 8:00 → 50m.
  - 07:55 with 8:00 → 0m, clamped (the lead window has passed, run now).
  - 09:00 with 8:00 → 22h50m.
  - Across DST in `America/New_York` uses wall-clock 8:00.
- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*DailyOverviewSchedulerTest*"`. Expected: FAIL.
- [ ] **Step 3:** Implement. Worker behavior:
  - Build the facts with `yesterday()`.
  - When `skipQuiet && facts.isQuiet`, return success with no post.
  - Otherwise call `generateDailySummary(bypassCache = false)` and post the notification.
    - Title: "Yesterday: <n> chapters, <m> updates".
    - Body: the first sentence.
    - `BigTextStyle`: the full text.
    - Actions: "Open overview" and "Continue <last title>" (opens Details for `titles.last()`).
  - Check POST_NOTIFICATIONS. When it's denied, the settings row shows "Notifications are off for
    this app" and a grant button (the existing permission helper).
  - The worker generates at about T-10 min and posts right away. WorkManager drift is accepted.
- [ ] **Step 4:** Run the tests and `./gradlew :mangaworld:assembleNoFirebaseDebug`. Expected: PASS and BUILD SUCCESSFUL.
- [ ] **Step 5:** Commit `feat(daily-overview): morning notification`.

---

### Task 12: Device and desktop verification, changelog

**Files:**
- Modify: `CHANGELOG.md` (Unreleased: Daily overview)

- [ ] **Step 1:** Run `./gradlew :kmpuiviews:jvmTest :favoritesdatabase:jvmTest :kmpuiviews:koogintegration:jvmTest`. Expected: all pass.
- [ ] **Step 2:** Run `cdm acquire --android --category phone --device R5CX12GDWPZ`. Before
  installing, diff the Room versions with the main checkout (memory: worktree DB version). Then
  run `./gradlew :mangaworld:installNoFirebaseDebug`.
- [ ] **Step 3:** Check the following with adb and screenshots, using only test data the agent
  creates:
  - The quick action opens the screen with the transform.
  - The stats and graph after reading 2 chapters of a test title, with the device clock unchanged
    (check the next day, or seed through a debug build).
  - Engine page states: Nano unavailable expected, and Gemma after an AI Helper download if
    present.
  - The notification posts with the time set 2 minutes ahead.
  - Turning the feature off removes the row and `adb shell dumpsys jobscheduler | grep daily_overview` shows nothing.
- [ ] **Step 4:** Desktop: `./gradlew :mangaworld:desktop:run`. Check Ollama running and stopped,
  and confirm there is no notification section.
- [ ] **Step 5:** Run `cdm release`. Add the CHANGELOG entry. Commit `docs(changelog): daily overview`.
  List what went unverified: the Nano path without supported hardware, AnimeWorld/NovelWorld nouns
  if not run, iOS.
