# Translation Models Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the Android translation-models sheet (storage, download, single + bulk delete, global target language) and add real local translation on desktop with Bergamot, Ollama and Custom engines managed from a desktop Translation settings page.

**Architecture:** A shared target-language setting and a reshaped `TranslationModelHandler` (StateFlow of `TranslationModel` rows) feed a common `TranslationViewModel` + sheet. Android implements the handler with ML Kit; desktop implements it with a `DesktopTranslationHandler` that routes to the selected `TranslationEngine` (Bergamot / Ollama / Custom), each with its own store, and a desktop-only settings page.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (M3 expressive), Koin, DataStore proto (`datastore`), DataStore preferences (`MangaDesktopSettings`), Ktor client (+ `ktor-client-mock` in tests), OkHttp, ML Kit Translate (Android `full`/`noCloudFirebase`).

**Spec:** `docs/superpowers/specs/2026-11-06-translation-models-design.md` (approved). Mockup: https://claude.ai/artifact/RmpP9QQ8FQg1spu9Um3oVe — phone = "A, with B's select mode"; desktop section.

## Global Constraints

- Android engine stays ML Kit only; engine choice is desktop only.
- `noFirebase` Android keeps the no-op `TranslateItems`; the sheet must open there and show an empty state ("Translation isn't available in this build").
- Target language default `"en"`; it applies to every `TranslationHandler.translate` / `translateDescription` call; skip translation when detected source == target.
- English is Required on Android: lock icon, never deletable, excluded from select / select all.
- Every delete (single, bulk, Bergamot row, Ollama model) confirms first. Custom folders are never deleted from disk, only removed from the list.
- ML Kit downloads stay Wi-Fi only (`DownloadConditions.requireWifi()`).
- Bergamot: model **files** are the storage unit; a row may need 2 files (pivot via English); totals count each file once; deleting a row deletes only files no other downloaded row uses.
- Desktop page ships in both `mangaworld:desktop` and `animeworld:desktop`.
- Every lazy item gets `Modifier.animateItem()`. Tall `ModalBottomSheet` uses `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.
- Copy: picker note says "Applies everywhere translation is used, like descriptions." (not "chapter text and the dictionary"). Custom folder label: "Folder with a Bergamot (Marian) model".
- Errors on desktop go through `InAppNotifier.error(...)` (`sharedcomponents/.../notifier/InAppNotifier.kt`); failed translation returns the original text.
- Local Android builds: `./gradlew :mangaworld:assembleNoCloudFirebaseDebug` for ML Kit device checks (noFirebase has none).

## Review Focus

1. **Deleting the current target language** (single or bulk) → target resets to `"en"`, no crash, sheet shows English as Target. Test in Task 2.
2. **Source language == target** (e.g. English description, target English; Japanese text, target Japanese) → returns input untouched, no model download, no engine call. Tests in Task 3 (Android adapter) and Task 5 (desktop router).
3. **Bergamot shared pivot file**: ja→fr and ko→fr both downloaded, delete ja→fr → `en-fr` files stay, `ja-en` removed; storage total drops by `ja-en` only. Test in Task 7.
4. **Engine unreachable mid-use** (Ollama stopped, custom server down, Bergamot pair missing) → original text + one error banner naming the address or missing pair; Details stops its progress spinner. Tests in Task 5 (router) and Tasks 8/9.
5. **Download cancelled or fails halfway** (Bergamot/ML Kit/Ollama pull) → no partial file left as "downloaded" (`.part` deleted), row returns to Download with a Failed message, retry works. Tests in Tasks 7 and 8.

---

## File Structure

| File | Responsibility |
|---|---|
| `datastore/src/commonMain/proto/settings.proto` | add `translationTargetLanguage = 46`, `map<string,int64> translationLastUsed = 47` |
| `datastore/.../SettingsHandling.kt` (`NewSettingsHandling`) | `translationTargetLanguage`, `translationLastUsed` `ProtoStoreHandler`s |
| `kmpuiviews/src/commonMain/.../domain/TranslationModelHandler.kt` | reshaped interface + `TranslationModel`, `ModelState` |
| `kmpuiviews/src/commonMain/.../domain/TranslationLanguages.kt` (new) | `TranslationLanguage` catalog (code, english name, native name, glyph) |
| `kmpuiviews/src/commonMain/.../presentation/settings/translationmodels/TranslationViewModel.kt` | sheet UI state + actions |
| `kmpuiviews/src/commonMain/.../presentation/settings/translationmodels/TranslationScreen.kt` | phone sheet (A + select mode) |
| `kmpuiviews/src/commonMain/.../presentation/settings/translationmodels/TranslationSheetParts.kt` (new) | target card, storage meter, rows, bulk bar, picker |
| `sharedutils/src/{full,noCloudFirebase,noFirebase}/java/TranslatorUtils.kt` | ML Kit: target-aware translate, download, sizes, delete |
| `UIViews/src/main/java/.../di/KmpInteropModule.kt` | Android adapters to the new interfaces |
| `kmpuiviews/src/jvmMain/.../translation/` (new package) | desktop engines, router, stores, settings, detector |
| `kmpuiviews/src/jvmMain/.../translation/ui/` (new) | desktop Translation page + `DesktopTranslationViewModel` |
| `kmpuiviews/src/commonMain/.../presentation/Screen.kt` | `Screen.Settings.Translation` |
| `kmpuiviews/src/commonMain/.../Platform.kt` (+ actuals) | `expect val hasTranslationSettingsPage: Boolean` |
| `kmpuiviews/src/jvmTest/.../testing/Fakes.kt` | updated fakes |

---

### Task 1: Shared target language + language catalog

**Files:**
- Modify: `datastore/src/commonMain/proto/settings.proto` (message `Settings`, current max field 45)
- Modify: `datastore/src/commonMain/kotlin/com/programmersbox/datastore/SettingsHandling.kt` (`NewSettingsHandling`)
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/TranslationLanguages.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/TranslationLanguagesTest.kt`

**Interfaces:**
- Produces:
  - proto: `string translationTargetLanguage = 46; map<string, int64> translationLastUsed = 47;`
  - `NewSettingsHandling.translationTargetLanguage: ProtoStoreHandler<String>` (default `"en"`; treat empty as `"en"`)
  - `NewSettingsHandling.translationLastUsed: ProtoStoreHandler<Map<String, Long>>` (default `emptyMap()`)
  - `data class TranslationLanguage(val code: String, val englishName: String, val nativeName: String, val glyph: String)`
  - `object TranslationLanguages { val all: List<TranslationLanguage>; fun byCode(code: String): TranslationLanguage? }` — the 59 ML Kit codes (BCP-47 short: `af, ar, be, bg, bn, ca, cs, cy, da, de, el, en, eo, es, et, fa, fi, fr, ga, gl, gu, he, hi, hr, ht, hu, id, is, it, ja, ka, kn, ko, lt, lv, mk, mr, ms, mt, nl, no, pl, pt, ro, ru, sk, sl, sq, sv, sw, ta, te, th, tl, tr, uk, ur, vi, zh`), sorted by `englishName`. Glyph = first grapheme of native name for non-Latin scripts (`日`, `한`, `中`, `Ру`→`Р`), else two-letter title-cased code (`Es`).

- [ ] **Step 1: Write failing tests** — `all has 59 unique codes`, `byCode("ja") == TranslationLanguage("ja","Japanese","日本語","日")`, `byCode("xx") == null`, `all is sorted by englishName`.
- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*TranslationLanguagesTest"` → FAIL (unresolved).
- [ ] **Step 3:** Add proto fields + handlers; write the catalog.
- [ ] **Step 4:** Run the test → PASS; run `./gradlew :datastore:jvmTest` → PASS (proto regenerates).
- [ ] **Step 5:** Commit `feat(translation): global target language setting + language catalog`.

---

### Task 2: Reshaped model handler + sheet ViewModel

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/TranslationModelHandler.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/translationmodels/TranslationViewModel.kt`
- Modify: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/testing/Fakes.kt` (`FakeTranslationModelHandler`)
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/ViewModelModule.kt` if constructor changes
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/translationmodels/TranslationViewModelTest.kt` (rewrite)

**Interfaces:**
- Consumes: Task 1 `NewSettingsHandling.translationTargetLanguage`, `TranslationLanguages`.
- Produces (replace the old interface entirely; delete `getModels`, `deleteModel`, `modelList`, `KmpCustomRemoteModel`):
```kotlin
interface TranslationModelHandler {
    /** False on builds without a translator (noFirebase). */
    val isAvailable: Boolean
    /** One row per language the engine knows, downloaded or not. */
    val models: StateFlow<List<TranslationModel>>
    suspend fun refresh()
    fun download(code: String)
    fun cancelDownload(code: String)
    suspend fun delete(codes: Set<String>)
}
data class TranslationModel(
    val language: TranslationLanguage,
    val state: ModelState,
    val sizeBytes: Long?,          // null = unknown
    val sizeIsEstimate: Boolean,
    val required: Boolean,         // English on ML Kit
    val lastUsedMillis: Long?,
)
sealed interface ModelState {
    data object NotDownloaded : ModelState
    data class Downloading(val fraction: Float?) : ModelState   // null = indeterminate
    data object Downloaded : ModelState
    data class Failed(val message: String) : ModelState
}
```
- `TranslationViewModel(handler: TranslationModelHandler, settings: NewSettingsHandling)` exposes `val state: StateFlow<TranslationSheetState>` and actions `openPicker()`, `closePicker()`, `setTarget(code)`, `askDelete(code)`, `cancelDelete()`, `confirmDelete()`, `enterSelect(code: String? = null)`, `exitSelect()`, `toggle(code)`, `selectAll()`, `askBulkDelete()`, `cancelBulkDelete()`, `confirmBulkDelete()`, `download(code)`, `cancelDownload(code)`, `setQuery(q)`, and `onBack(): Boolean` (true = consumed).
```kotlin
data class TranslationSheetState(
    val available: Boolean,
    val target: TranslationLanguage,
    val downloaded: List<TranslationModel>,     // Downloaded, required first, then by name
    val notDownloaded: List<TranslationModel>,  // filtered by query
    val totalBytes: Long,
    val pickerOpen: Boolean,
    val pickerLanguages: List<TranslationModel>,// all, filtered by query
    val query: String,
    val confirmingCode: String?,
    val selecting: Boolean,
    val selected: Set<String>,
    val selectedBytes: Long,
    val confirmingBulk: Boolean,
)
```
- `setTarget(code)`: writes setting, closes picker, and calls `download(code)` if not downloaded.

- [ ] **Step 1: Write failing tests** (use `FakeTranslationModelHandler` seeded with en, ja, ko, zh, es downloaded; fr, de not):
  - `delete of target resets target to en` — target `ja`, `askDelete("ja")`, `confirmDelete()` → handler received `setOf("ja")`, `state.target.code == "en"`.
  - `bulk delete of target resets target to en` — same via `enterSelect("ja")`, `toggle("ko")`, `confirmBulkDelete()`.
  - `english is never selectable` — `enterSelect()`, `toggle("en")`, `selectAll()` → `"en" !in selected`; `selected == setOf("ja","ko","zh","es")`.
  - `selectAll twice clears` → `selected.isEmpty()`.
  - `selectedBytes sums selected sizes`.
  - `totalBytes sums downloaded sizes`.
  - `onBack exits select before closing` → first `onBack()` true and `selecting == false`; second `onBack()` false.
  - `onBack closes picker first`.
  - `setTarget not downloaded triggers download` → fake recorded `download("fr")`, `pickerOpen == false`.
  - `query filters notDownloaded and picker by english or native name` (`"deu"` matches German's native `Deutsch`? use `"deut"`).
  - `unavailable handler gives available false`.
- [ ] **Step 2:** `./gradlew :kmpuiviews:jvmTest --tests "*TranslationViewModelTest"` → FAIL.
- [ ] **Step 3:** Implement the interface change, fake, VM. Fix compile in `KmpInteropModule.kt` and `Platform.jvm.kt` with temporary `isAvailable = false` stubs returning empty flows (replaced in Tasks 3 and 5).
- [ ] **Step 4:** Test → PASS; `./gradlew :kmpuiviews:jvmTest` → PASS.
- [ ] **Step 5:** Commit `feat(translation): model handler rows + sheet view model`.

---

### Task 3: Android ML Kit implementation

**Files:**
- Modify: `sharedutils/src/full/java/TranslatorUtils.kt`, `sharedutils/src/noCloudFirebase/java/TranslatorUtils.kt` (identical changes), `sharedutils/src/noFirebase/java/TranslatorUtils.kt` (signature-only stubs)
- Modify: `UIViews/src/main/java/com/programmersbox/uiviews/di/KmpInteropModule.kt`
- Test: `UIViews/src/test/java/com/programmersbox/uiviews/di/MlKitModelRowsTest.kt` (pure mapping)

**Interfaces:**
- Consumes: Task 1, Task 2 interfaces.
- Produces in `TranslatorUtils` (both ML Kit flavors):
  - `class TranslateItems { suspend fun translate(text: String, target: String): String; fun clear() }` — identify language; return `text` if `und` or `== target`; else translator (source→target), `downloadModelIfNeeded(requireWifi)`, translate. Cache one translator keyed by `source to target`; close on key change.
  - `object TranslatorUtils { val isAvailable: Boolean; suspend fun downloadedCodes(): Set<String>; suspend fun download(code: String); suspend fun delete(code: String); fun modelSizeBytes(context: Context, code: String): Long? }`
  - noFirebase: `isAvailable = false`, `translate` returns `text`, others no-op/empty.
- Produces in `KmpInteropModule.kt`: `MlKitTranslationModelHandler(context, settings, scope) : TranslationModelHandler` and `TranslationItemHandler(settings)` reading `translationTargetLanguage` and writing `translationLastUsed[source]` after each successful translation.
- Pure helper (tested): `fun mlKitRows(downloaded: Set<String>, downloading: Map<String, ModelState>, sizes: (String) -> Long?, lastUsed: Map<String, Long>): List<TranslationModel>` — every catalog language; `en` `required = true`; size `null` → `30_000_000L` with `sizeIsEstimate = true`; `Downloading(fraction = null)` (ML Kit has no progress).

- [ ] **Step 1: Find the on-disk model location.** Acquire a device (`cdm acquire --category phone --android`), install `:mangaworld:installNoCloudFirebaseDebug`, translate one description, then `adb shell run-as com.programmersbox.mangaworld.noCloudFirebase find . -path '*mlkit*' -maxdepth 4` (adjust app id from `adb shell pm list packages | grep manga`). Record the directory naming per language in a KDoc on `modelSizeBytes`. If no per-language directory is identifiable, `modelSizeBytes` returns null (estimate path). `cdm release` when done.
- [ ] **Step 2: Write failing tests** for `mlKitRows`: `english is required`, `unknown size uses 30MB estimate`, `downloading state overrides downloaded`, `lastUsed is mapped`.
- [ ] **Step 3:** `./gradlew :UIViews:testNoFirebaseDebugUnitTest --tests "*MlKitModelRowsTest"` → FAIL.
- [ ] **Step 4:** Implement utils + adapters; `download` launches in `scope`, sets `Downloading(null)`, then `refresh()`; failure → `Failed(e.message ?: "Download failed")`. `cancelDownload` cancels the job (ML Kit continues in background; row returns to NotDownloaded and next refresh reflects reality).
- [ ] **Step 5:** Tests → PASS; compile all flavors: `./gradlew :mangaworld:compileNoFirebaseDebugKotlin :mangaworld:compileNoCloudFirebaseDebugKotlin :mangaworld:compileFullDebugKotlin` (full may need `google-services.json`; if missing, skip and note it).
- [ ] **Step 6:** Commit `feat(translation): ML Kit target language, downloads and sizes`.

---

### Task 4: Phone sheet UI

**Files:**
- Modify: `kmpuiviews/src/commonMain/.../presentation/settings/translationmodels/TranslationScreen.kt`
- Create: `kmpuiviews/src/commonMain/.../presentation/settings/translationmodels/TranslationSheetParts.kt`
- Modify: `kmpuiviews/src/commonMain/composeResources/values/strings.xml` (new strings; keep `viewTranslationModels`, delete `chooseModelToDelete` if unused)

**Interfaces:**
- Consumes: Task 2 `TranslationViewModel`, `TranslationSheetState`.
- Produces: `@Composable fun showTranslationScreen(): MutableState<Boolean>` (same signature, callers unchanged) and `@Composable fun TranslationScreen(vm: TranslationViewModel = koinViewModel())`.

Build to the mockup's "A, with B's select mode" phone (open the artifact). Pieces in `TranslationSheetParts.kt`: `TargetLanguageCard`, `StorageMeter` (stacked `Row` of weighted boxes, weights animated with `animateFloatAsState`; selected segments alpha 0.25; subtitle "→ X after delete" while selecting), `DownloadedRow` (leading `AnimatedContent` avatar⇄`Checkbox`; trailing trash/lock hidden in select mode via `AnimatedVisibility`; `combinedClickable(onLongClick = enterSelect)`; inline confirm via `AnimatedContent` into an `errorContainer` surface with copy "Delete {name}? Frees {size}. It downloads again the next time you translate {name}."), `AvailableRow` (download icon / `LinearWavyProgressIndicator` indeterminate when `fraction == null`, cancel icon), `BulkDeleteBar` (`AnimatedVisibility(slideInVertically)` aligned bottom inside the sheet; morphs to confirm with `AnimatedContent` + `SizeTransform`), `LanguagePicker` (back, search, radio list; note copy from Global Constraints). Main ⇄ picker via `AnimatedContent` horizontal slide. `BackHandler`/`NavigationBackHandler` calls `vm.onBack()`. Unavailable build: centered empty state "Translation isn't available in this build". Sizes formatted with one decimal MB / two decimals GB, prefixed "about " when `sizeIsEstimate`.

- [ ] **Step 1:** Implement.
- [ ] **Step 2:** `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:compileNoFirebaseDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 3:** Device check on `noCloudFirebase` (acquire with `cdm acquire --category phone --android`): open Settings → Integrations → translation; screenshot main, picker, single confirm, select mode with bulk bar. Only download/delete a language the agent downloaded itself in this session (never delete languages already on the phone). `cdm release`.
- [ ] **Step 4:** Commit `feat(translation): redesigned translation models sheet`.

---

### Task 5: Desktop translation core (router, detector, settings)

**Files:**
- Create in `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/translation/`: `TranslationEngine.kt`, `LanguageDetector.kt`, `DesktopTranslationSettings.kt`, `DesktopTranslationHandler.kt`
- Modify: `kmpuiviews/src/jvmMain/.../Platform.jvm.kt` (remove stub `TranslationItemHandler`/`TranslationModelHandlerImpl`), `kmpuiviews/src/jvmMain/.../di/AppModule.jvm.kt`
- Modify: `kmpuiviews/build.gradle.kts` (jvmMain detector dependency)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/translation/DesktopTranslationHandlerTest.kt`

**Interfaces:**
- Produces:
```kotlin
enum class EngineKind { Bergamot, Ollama, Custom }
interface TranslationEngine {
    val kind: EngineKind
    /** Codes this engine can translate into, given what is installed/reachable now. */
    suspend fun targets(): List<String>
    /** Throws TranslationUnavailable with a user-facing message when it can't. */
    suspend fun translate(text: String, source: String, target: String): String
}
class TranslationUnavailable(message: String) : Exception(message)
interface LanguageDetector { fun detect(text: String): String? }   // ISO 639-1 or null
class DesktopTranslationSettings(appDirs: AppDirs) {
    val engine: DataStoreHandler<String>              // EngineKind.name, default "Bergamot"
    val ollamaUrl: DataStoreHandler<String>           // "http://localhost:11434"
    val ollamaModel: DataStoreHandler<String>         // ""
    val customMode: DataStoreHandler<String>          // "folder" | "server", default "folder"
    val customFolders: DataStoreHandler<Set<String>>  // absolute paths
    val customServerUrl: DataStoreHandler<String>     // "http://localhost:1234/v1"
    val customServerModel: DataStoreHandler<String>   // ""
    val customServerKey: DataStoreHandler<String>     // ""
}
class DesktopTranslationHandler(
    engines: Map<EngineKind, TranslationEngine>, detector: LanguageDetector,
    settings: DesktopTranslationSettings, appSettings: NewSettingsHandling, notifier: InAppNotifier,
) : TranslationHandler
```
  (Follow `MangaDesktopSettings`' `DataStoreHandler` pattern in `Platform.jvm.kt:259`.)
- `translate(text)`: detect → null or == target → return `text` (no engine call); else selected engine's `translate`; on `TranslationUnavailable` or IO error → `notifier.error(title = "Couldn't translate", subtitle = e.message)` and return `text`. `translateDescription` wraps it, always calling `progress(false)` in `finally`. `clear()` cancels in-flight work.
- Detector: pick a JVM library adding **≤ 15 MB** to the desktop distributable that correctly detects `ja, ko, zh, es, fr, de, en, ru` from one manga-description sentence (candidates: `com.github.pemistahl:lingua` with only those language models loaded via `LanguageDetectorBuilder.fromLanguages`, check jar size first; else Optimaize `language-detector`). Wrap it as `class LibraryLanguageDetector : LanguageDetector`.

- [ ] **Step 1: Write failing tests** with a recording fake engine + fake detector + fake notifier:
  - `same language returns input without engine call` (detector "en", target "en").
  - `undetected returns input`.
  - `routes to selected engine` (engine setting "Ollama" → Ollama fake called with ("ja","en")).
  - `unavailable returns input and shows one error banner with the message`.
  - `translateDescription always ends progress false` (engine throws).
  - Detector test: 8 sample sentences (one per language above) detect correctly.
- [ ] **Step 2:** `./gradlew :kmpuiviews:jvmTest --tests "*DesktopTranslationHandlerTest" --tests "*LanguageDetectorTest"` → FAIL.
- [ ] **Step 3:** Implement; Koin: `single { DesktopTranslationSettings(get()) }`, `factory<TranslationHandler> { DesktopTranslationHandler(...) }`; engines registered as `single` by later tasks — until then register an empty map.
- [ ] **Step 4:** Tests → PASS.
- [ ] **Step 5:** Commit `feat(desktop-translation): engine router, detector, settings`.

---

### Task 6: Bergamot runtime spike (decision gate)

No official Bergamot binary or Java binding exists. Decide how to run it and prove one ja→en sentence end-to-end on this Mac.

**Files:**
- Create: `kmpuiviews/src/jvmMain/.../translation/bergamot/BergamotRuntime.kt`
- Create: `docs/superpowers/notes/2026-11-06-bergamot-runtime.md` (decision record)

**Interfaces:**
- Produces:
```kotlin
interface BergamotRuntime {
    /** Null when ready; otherwise a user-facing reason (e.g. "Install the translator (9 MB)"). */
    suspend fun unavailableReason(): String?
    suspend fun install(onProgress: (Float) -> Unit)   // no-op if nothing to install
    suspend fun translate(text: String, model: BergamotModelFiles): String
}
data class BergamotModelFiles(val model: File, val lex: File?, val srcVocab: File, val trgVocab: File)
```
- Try in order, stop at the first that passes the gate:
  1. **WASM in the JVM:** Mozilla's bergamot-translator WASM release run under Chicory (`com.dylibso.chicory`, AOT). Gate: ja→en sentence < 2 s after warm-up, no native deps.
  2. **Native CLI:** bergamot-translator's `app/bergamot` built per OS in a GitHub Actions workflow in this repo (`.github/workflows/bergamot-cli.yml`, macOS arm64/x64, Windows x64, Linux x64), published as a release asset; downloaded on demand with pinned sha256 like `WhisperCliInstaller.kt` (`animeworld/shared/src/jvmMain/.../subtitlegen/`). Gate: builds on CI, runs locally via `ProcessBuilder` with stdin/stdout.
- [ ] **Step 1:** Download one model pair (ja→en) from Mozilla Remote Settings (`https://firefox.settings.services.mozilla.com/v1/buckets/main/collections/translations-models/records`, attachments base from `https://firefox.settings.services.mozilla.com/v1/` → `capabilities.attachments.base_url`) into the scratchpad; note file types and whether attachments are compressed.
- [ ] **Step 2:** Try option 1; record timing or failure.
- [ ] **Step 3:** If option 1 fails the gate, do option 2.
- [ ] **Step 4:** Write the decision record (what was tried, numbers, chosen option, install size per OS) and implement `BergamotRuntime` for the chosen option with a jvmTest `@EnabledIfEnvironmentVariable("BERGAMOT_SMOKE", "1")` smoke test translating `"彼は静かに剣を抜いた。"` to a non-empty English string.
- [ ] **Step 5:** **Stop and report to the user** with the decision record before Task 7 (the choice affects download size and CI). Commit `feat(desktop-translation): bergamot runtime (<option>)`.

---

### Task 7: Bergamot model store + engine

**Files:**
- Create: `kmpuiviews/src/jvmMain/.../translation/bergamot/BergamotIndex.kt`, `BergamotModelStore.kt`, `BergamotEngine.kt`
- Test: `kmpuiviews/src/jvmTest/.../translation/bergamot/BergamotModelStoreTest.kt`, `BergamotIndexTest.kt` (fixture JSON in `src/jvmTest/resources/bergamot/records.json`, a trimmed real response)

**Interfaces:**
- Consumes: Task 6 `BergamotRuntime`, `BergamotModelFiles`; Task 5 `TranslationEngine`.
- Produces:
```kotlin
data class BergamotFile(val pair: String /* "ja-en" */, val type: String, val url: String, val sha256: String, val sizeBytes: Long)
data class BergamotPair(val from: String, val to: String, val files: List<BergamotFile>)
class BergamotIndex(client: HttpClient) { suspend fun pairs(): List<BergamotPair> }  // latest version per pair; cached to disk for offline
/** Returns the pair chain for source→target: direct if present, else [source→en, en→target]; null if impossible. */
fun planChain(source: String, target: String, pairs: List<BergamotPair>): List<BergamotPair>?
data class BergamotRow(val source: String, val target: String, val chain: List<BergamotPair>,
                       val state: ModelState, val ownBytes: Long, val sharedBytes: Long, val freedOnDelete: Long)
class BergamotModelStore(index: BergamotIndex, dir: File, client: OkHttpClient, scope: CoroutineScope) {
    val rows: StateFlow<List<BergamotRow>>     // one per source language reachable into current target
    fun setTarget(code: String)
    fun download(source: String)               // fetches only missing pair files; `.part` then rename; sha256 checked
    fun cancel(source: String)
    suspend fun delete(source: String)         // removes pairs no other downloaded row's chain uses
    val totalBytes: StateFlow<Long>            // each file once
    fun filesFor(source: String, target: String): List<BergamotModelFiles>?
}
class BergamotEngine(store: BergamotModelStore, runtime: BergamotRuntime) : TranslationEngine
```
- Store dir: `File(appDirs.getUserDataDir(), "translation/bergamot")`.
- `BergamotEngine.translate` runs each chain step in order; missing files → `TranslationUnavailable("Download Japanese → French in Settings → Translation")`.

- [ ] **Step 1: Write failing tests:**
  - `planChain direct when available`, `planChain pivots via en`, `planChain null when no route`, `planChain en to en is empty list` (caller skips).
  - `index keeps latest version per pair` (fixture).
  - `delete keeps shared pivot file` — ja→fr and ko→fr downloaded; `delete("ja")` → `en-fr` files exist, `ja-en` gone; `totalBytes` drops by ja-en size only.
  - `freedOnDelete excludes shared files`.
  - `failed download leaves no part file and state Failed`; `retry after failure succeeds` (MockWebServer or fake client returning 500 then 200).
  - `sha mismatch fails download`.
- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*Bergamot*"` → FAIL.
- [ ] **Step 3:** Implement; register `single { BergamotModelStore(...) }`, engine in the engines map.
- [ ] **Step 4:** Tests → PASS.
- [ ] **Step 5:** Commit `feat(desktop-translation): bergamot models with english pivot`.

---

### Task 8: Ollama engine

**Files:**
- Create: `kmpuiviews/src/jvmMain/.../translation/ollama/OllamaClient.kt`, `OllamaEngine.kt`
- Test: `kmpuiviews/src/jvmTest/.../translation/ollama/OllamaClientTest.kt` (Ktor `MockEngine`)

**Interfaces:**
- Produces:
```kotlin
data class OllamaModel(val name: String, val sizeBytes: Long)
sealed interface OllamaStatus { data class Connected(val version: String, val models: List<OllamaModel>) : OllamaStatus; data class Unreachable(val url: String) : OllamaStatus }
class OllamaClient(http: HttpClient, settings: DesktopTranslationSettings) {
    suspend fun status(): OllamaStatus                 // GET /api/version + /api/tags
    fun pull(name: String): Flow<Float?>              // POST /api/pull stream; completed/total
    suspend fun delete(name: String)                  // DELETE /api/delete
    suspend fun chat(model: String, system: String, user: String): String  // POST /api/chat stream=false
}
class OllamaEngine(client: OllamaClient, settings: DesktopTranslationSettings) : TranslationEngine
```
- System prompt (exact): `"You are a translation engine. Translate the user's text from {sourceName} to {targetName}. Reply with only the translation, no notes, no quotes."` Trim the reply; strip one pair of surrounding quotes if present.
- `targets()` = all `TranslationLanguages.all` codes when connected and a model is selected, else empty.
- Unreachable → `TranslationUnavailable("Can't reach Ollama at {url}. Is it running?")`; no model selected → `TranslationUnavailable("Pick an Ollama model in Settings → Translation")`.

- [ ] **Step 1: Write failing tests:** `status parses version and tags sizes`, `status unreachable on connect error`, `pull emits fractions then 1f`, `pull failure surfaces error`, `chat sends system prompt and returns trimmed content`, `engine strips surrounding quotes`, `engine without model throws unavailable`.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement (Ktor client with OkHttp engine on JVM, `ContentNegotiation` json). **Step 4:** Run → PASS.
- [ ] **Step 5:** Commit `feat(desktop-translation): ollama engine`.

---

### Task 9: Custom engine (folders + OpenAI-compatible server)

**Files:**
- Create: `kmpuiviews/src/jvmMain/.../translation/custom/CustomFolders.kt`, `OpenAiCompatClient.kt`, `CustomEngine.kt`
- Test: `kmpuiviews/src/jvmTest/.../translation/custom/CustomFoldersTest.kt`, `OpenAiCompatClientTest.kt`

**Interfaces:**
- Consumes: Task 6 runtime + `BergamotModelFiles`; Task 7 `planChain` (folders become `BergamotPair`s with local files); Task 8 prompt + quote stripping (extract to `translation/LlmPrompt.kt` shared by both).
- Produces:
```kotlin
sealed interface FolderCheck { data class Valid(val from: String, val to: String, val files: BergamotModelFiles, val sizeBytes: Long) : FolderCheck; data class Invalid(val reason: String) : FolderCheck }
fun inspectFolder(dir: File): FolderCheck   // model*.bin (+ optional lex*.s2t.bin) + vocab*.spm or srcvocab/trgvocab; pair from file names "<from><to>" e.g. model.jaen.intgemm.alphas.bin
class OpenAiCompatClient(http: HttpClient, settings: DesktopTranslationSettings) {
    suspend fun check(): Result<Int>           // GET {url}/models → count
    suspend fun chat(system: String, user: String): String   // POST {url}/chat/completions, Bearer key if non-empty
}
class CustomEngine(settings, runtime: BergamotRuntime, client: OpenAiCompatClient) : TranslationEngine
```
- Folder mode chains like Bergamot via `planChain`; no route → `TranslationUnavailable("Add a folder for {source} → {target}, or {source} → English and English → {target}")`. Removing a folder only updates `customFolders`.

- [ ] **Step 1: Write failing tests:** `valid folder detects pair and size` (temp dir with dummy files), `missing vocab is invalid with reason`, `folders chain through english`, `server sends bearer only when key set`, `server unreachable gives unavailable message with url`.
- [ ] **Step 2–4:** Run → FAIL; implement; run → PASS.
- [ ] **Step 5:** Commit `feat(desktop-translation): custom folders and openai-compatible server`.

---

### Task 10: Desktop Translation page + navigation

**Files:**
- Modify: `kmpuiviews/src/commonMain/.../presentation/Screen.kt` (add `@Serializable data object Translation : Screen("translation")` under `Settings`), `SettingsScreenDisplayNames.kt` ("Translation"), `NavigationActions` + `Navigation3Actions.kt` (`fun translationSettings()`)
- Modify: `kmpuiviews/src/commonMain/.../Platform.kt` + actuals (`expect val hasTranslationSettingsPage: Boolean`; jvm `true`, android/ios `false`)
- Modify: `IntegrationsScreen.kt` — row click: `if (hasTranslationSettingsPage) navActions.translationSettings() else showTranslation = true`
- Create: `kmpuiviews/src/jvmMain/.../translation/ui/DesktopTranslationViewModel.kt`, `DesktopTranslationScreen.kt`, `EnginePanels.kt`; register `navigation<Screen.Settings.Translation> { DesktopTranslationScreen() }` in a jvm Koin module included by `AppModule.jvm.kt`
- Test: `kmpuiviews/src/jvmTest/.../translation/ui/DesktopTranslationViewModelTest.kt`

**Interfaces:**
- Consumes: Tasks 5, 7, 8, 9.
- Produces: `DesktopTranslationViewModel` with `state: StateFlow<DesktopTranslationState>` (target, targets for selected engine, engine, storage label + value, bergamot rows, ollama status/models/selected, custom mode/folders/server fields, test input/output/timing/running, confirmingId) and actions mirroring the mockup (setTarget, setEngine, download/cancel/askDelete/confirmDelete for bergamot rows and ollama models, pull, check, add/remove folder via FileKit picker, edit server fields, runTest).

Build to the mockup's desktop section: top row (target dropdown, Storage box), three engine radio cards with tags, engine panel `AnimatedContent`, Try-it box with shimmer and timing line `"{Engine} · {src}→{tgt} · {ms} ms"` (seconds with one decimal when ≥ 1000 ms).

- [ ] **Step 1: Write failing VM tests:** `switching engine changes storage label` (Bergamot "{n} language pairs", Ollama "Ollama models (managed by Ollama)", Custom "Your files aren't counted"), `target list follows engine targets`, `delete ollama model in use selects next`, `runTest records timing and engine label`, `confirm needed before delete` (no store call until confirm).
- [ ] **Step 2–4:** Run → FAIL; implement; run → PASS.
- [ ] **Step 5:** `./gradlew :mangaworld:desktop:run` and `./gradlew :animeworld:desktop:run`: open Settings → Integrations → translation → page opens in both; screenshot each engine panel; translate a Japanese description from Details with Bergamot (ja→en downloaded by this session).
- [ ] **Step 6:** Commit `feat(desktop-translation): translation settings page`.

---

### Task 11: Wrap-up

**Files:**
- Modify: `CHANGELOG.md` (Unreleased: Android sheet redesign + global target language; desktop local translation with Bergamot/Ollama/Custom)
- Modify: `CLAUDE.md` module notes only if a new module was added (none planned)

- [ ] **Step 1:** `./gradlew :kmpuiviews:jvmTest :UIViews:testNoFirebaseDebugUnitTest :mangaworld:compileNoFirebaseDebugKotlin :mangaworld:desktop:compileKotlinJvm :animeworld:desktop:compileKotlinJvm` → all green.
- [ ] **Step 2:** Changelog entry; commit `docs(changelog): translation models redesign + desktop engines`.
- [ ] **Step 3:** Push branch, open PR → `develop` listing what was device/desktop verified and what wasn't (Windows/Linux, full flavor).
