# Desktop Subtitle Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On AnimeWorld Desktop, a downloaded episode can get subtitles generated on the user's computer with whisper.cpp, started from the Downloads row menu, with progress under the row and model choice in Subtitles settings.

**Architecture:** A desktop-only `DesktopSubtitleGenerator` (Koin single) runs one job at a time: ffmpeg extracts 16 kHz mono WAV, `whisper-cli` writes a `.vtt`, and `GeneratedSidecars` saves it as a download sidecar so the player and export pick it up unchanged. A `WhisperModelStore` owns built-in model downloads, the user's own model files and the selected model. Common UI (dialog, progress block) lives in `commonMain` behind a `SubtitleGenerator` interface; Android and iOS return `null` through `expect`/`actual`, so nothing shows there.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform Material3, Koin, DataStore (`DataStoreHandler`), OkHttp (+ MockWebServer in tests), kotlinx.serialization, kotlin.test, coroutines-test, system `ffmpeg` and `whisper-cli` processes.

**Spec:** Approved UI preview (version 5): https://claude.ai/artifact/LiQPHhsJtaWVoMYcYTWqnS — every user-facing string below is copied from it. The decisions are also recorded in project memory `project_desktop_subtitle_gen.md`.

## Global Constraints

- Desktop only. Android and iOS `actual`s return `null` / render nothing. No Android behavior changes.
- One generation runs at a time; others wait in a FIFO queue.
- Whisper only translates into English. "To" offers English (`-tr`) and the "From" language (transcribe). Other languages are listed but disabled with "Needs a translation add-on".
- Built-in models download from `https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-<id>.bin` into `File(appDirs.getUserDataDir(), "whisper-models")`, written to `<name>.part` and renamed on success.
- Built-in models (id · label · display size): `tiny` Tiny 75 MB · `base` Base 142 MB · `small` Small 466 MB (default, "Recommended") · `medium` Medium 1.5 GB · `large-v3` Large v3 3.1 GB · `large-v3-turbo` Large v3 Turbo 1.6 GB ("Same-language only").
- Speed / accuracy meters (1–5): tiny 5/1, base 4/2, small 3/3, medium 2/4, large-v3 1/5, large-v3-turbo 3/4.
- A user's own model is a `.bin` whose first 4 bytes are `lmgg` (whisper.cpp ggml magic `0x67676d6c`, little-endian) or `GGUF`. It is stored by absolute path only; removing it from the list never deletes the file.
- A model whose file name contains `.en.` (e.g. `ggml-small.en.bin`) is English-only: "From" is locked to English.
- Generated tracks are labelled `"<Language> (generated)"` and marked `generated = true` in the sidecar index. A run with the same "To" language replaces that generated track; downloaded tracks are never replaced.
- Languages offered (code · name): `auto` Detect automatically · `ja` Japanese · `en` English · `ko` Korean · `zh` Chinese · `es` Spanish. Default From `ja`, To `en`.
- ffmpeg is found with the existing `VideoExportManager.ffmpeg()`; no second lookup.
- Every lazy item gets `Modifier.animateItem()` (project rule). New animated UI uses the Compose APIs named in the preview's animation table.
- Test command for the module: `./gradlew :animeworld:shared:jvmTest` (runs `commonTest` + `jvmTest`).

## Review Focus

1. **Video names with spaces, brackets, quotes or non-ASCII** (`[SubsPlease] Frieren - 07 (1080p).mkv`): process args must be passed as a list, never a shell string; sidecar name goes through `sidecarName`/`sanitizeFileName`. Test in Task 3 and Task 4.
2. **Cancelling mid-run**: the whisper/ffmpeg process is destroyed, temp WAV and partial `.vtt` are deleted, nothing is added to the index, and the next queued job starts. Test in Task 6.
3. **Download folder with no `.subtitles.json` but existing glob sidecars**: adding a generated track must keep the existing tracks visible (seed the index from the glob first). Test in Task 4.
4. **Selected model deleted or user's file moved after selection**: generation fails with "Model file not found" for a missing user file; a deleted built-in model is downloaded again on the next run. Test in Task 6.
5. **App closed while a model downloads**: a stale `<name>.part` must never count as downloaded and is overwritten on the next download. Test in Task 5.

---

## File Structure

`animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/subtitlegen/`
- `SubtitleGenerator.kt` — interface, `GenerationRequest`, `GenerationStatus`, `ModelSummary`, language list and `targetOptions()`.
- `GenerateSubtitlesDialog.kt` — the From → To dialog.
- `GenerationProgress.kt` — bar + status text shown under a row.
- `GeneratedSubtitlesSettings.kt` — `expect` composable for the settings section.

`animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/subtitlegen/`
- `WhisperModels.kt` — built-in catalog, model choice encoding, header and `.en` checks.
- `WhisperCommand.kt` — ffmpeg/whisper args, progress parsing, `findWhisperCli`.
- `GeneratedSidecars.kt` — writes the `.vtt` sidecar and updates the index.
- `WhisperModelStore.kt` — downloads, user files, selection.
- `DesktopSubtitleGenerator.kt` — queue + job runner.
- `GeneratedSubtitlesSettings.jvm.kt` — settings section UI.

Modified: `AnimeDesktopSettings.kt`, `DownloadNaming.kt` (`SidecarEntry`), `VideoLibrarySource.jvm.kt`, `DownloadRows.kt`, `ViewVideoScreen.kt` (+ `.jvm/.android/.ios` actuals), `SubtitleSettingsScreen.kt`, `animeworld/desktop/.../main.kt`, `CHANGELOG.md`.

---

### Task 1: Common contract and language rules

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/subtitlegen/SubtitleGenerator.kt`
- Test: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/subtitlegen/TargetOptionsTest.kt`

**Interfaces:**
- Produces:
  - `data class WhisperLanguage(val code: String, val name: String)`; `val WhisperAuto: WhisperLanguage` (`auto`, "Detect automatically"); `val whisperLanguages: List<WhisperLanguage>` (ja, en, ko, zh, es, in that order; excludes auto).
  - `data class TargetOption(val language: WhisperLanguage, val enabled: Boolean, val note: String)`
  - `fun targetOptions(from: WhisperLanguage): List<TargetOption>`
  - `fun fromOptions(englishOnlyModel: Boolean): List<WhisperLanguage>` — `[WhisperAuto] + whisperLanguages`, or only English when `englishOnlyModel`.
  - `data class GenerationRequest(val from: WhisperLanguage, val to: WhisperLanguage)`; `val GenerationRequest.translate: Boolean get() = to.code == "en" && from.code != "en"`
  - `sealed interface GenerationStatus { data class Queued(val afterTitle: String); data object ExtractingAudio; data class DownloadingModel(val modelName: String, val fraction: Float); data class Transcribing(val from: WhisperLanguage, val to: WhisperLanguage, val fraction: Float, val remainingMs: Long?); data class Failed(val message: String, val request: GenerationRequest) }`
  - `data class ModelSummary(val name: String, val downloaded: Boolean, val sizeLabel: String, val translatesWell: Boolean, val englishOnly: Boolean)`
  - `interface SubtitleGenerator { val statuses: StateFlow<Map<String, GenerationStatus>>; val finished: SharedFlow<String>; val model: StateFlow<ModelSummary>; val lastRequest: StateFlow<GenerationRequest>; fun enqueue(videoPath: String, request: GenerationRequest, remember: Boolean); fun cancel(videoPath: String); fun dismiss(videoPath: String) }` — map key and `finished` value are the video's absolute path.

- [ ] **Step 1: Write the failing test**

```kotlin
class TargetOptionsTest {
    private fun lang(code: String) = whisperLanguages.first { it.code == code }

    @Test fun japaneseOffersEnglishTranslatedAndJapaneseTranscribed() {
        val enabled = targetOptions(lang("ja")).filter { it.enabled }
        assertEquals(listOf("en" to "Translated", "ja" to "Transcribed as spoken"), enabled.map { it.language.code to it.note })
    }
    @Test fun otherLanguagesAreDisabledWithAddOnNote() {
        val disabled = targetOptions(lang("ja")).filterNot { it.enabled }
        assertEquals(listOf("ko", "zh", "es"), disabled.map { it.language.code })
        assertTrue(disabled.all { it.note == "Needs a translation add-on" })
    }
    @Test fun englishFromOffersOnlyEnglishTranscribed() {
        val enabled = targetOptions(lang("en")).filter { it.enabled }
        assertEquals(listOf("en" to "Transcribed as spoken"), enabled.map { it.language.code to it.note })
    }
    @Test fun autoOffersEnglishAndSameAsSpoken() {
        val enabled = targetOptions(WhisperAuto).filter { it.enabled }
        assertEquals(listOf("en", "auto"), enabled.map { it.language.code })
        assertEquals("Same as spoken", enabled[1].language.name)
    }
    @Test fun englishOnlyModelLocksFromToEnglish() {
        assertEquals(listOf("en"), fromOptions(englishOnlyModel = true).map { it.code })
        assertEquals(6, fromOptions(englishOnlyModel = false).size)
    }
    @Test fun translateOnlyWhenTargetIsEnglishAndSourceIsNot() {
        assertTrue(GenerationRequest(lang("ja"), lang("en")).translate)
        assertFalse(GenerationRequest(lang("en"), lang("en")).translate)
        assertFalse(GenerationRequest(lang("ja"), lang("ja")).translate)
    }
}
```

- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*TargetOptionsTest"` — Expected: FAIL (unresolved references).
- [ ] **Step 3: Implement** everything in the Produces list in `SubtitleGenerator.kt`. For `auto`, the transcribe option is `WhisperLanguage("auto", "Same as spoken")` with note "Transcribed as spoken".
- [ ] **Step 4: Run** the same command — Expected: PASS.
- [ ] **Step 5: Commit** `feat(animeworld): subtitle generation contract and language rules`

---

### Task 2: Model catalog and settings keys

**Files:**
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/subtitlegen/WhisperModels.kt`
- Modify: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/AnimeDesktopSettings.kt`
- Test: `animeworld/shared/src/jvmTest/kotlin/com/programmersbox/anime/shared/subtitlegen/WhisperModelsTest.kt`

**Interfaces:**
- Produces:
  - `enum class BuiltInWhisperModel(val id: String, val label: String, val sizeLabel: String, val approxBytes: Long, val speed: Int, val accuracy: Int, val recommended: Boolean = false, val sameLanguageOnly: Boolean = false)` with `val fileName get() = "ggml-$id.bin"`. Values and numbers from Global Constraints; `approxBytes` = 75/142/466/1530/3100/1620 × 1_000_000.
  - `sealed interface WhisperModelChoice { data class BuiltIn(val model: BuiltInWhisperModel); data class Custom(val path: String) }`
  - `fun WhisperModelChoice.encode(): String` → `"builtin:<id>"` / `"file:<path>"`; `fun decodeModelChoice(value: String): WhisperModelChoice` (unknown → `BuiltIn(SMALL)`).
  - `fun isWhisperModelHeader(first4: ByteArray): Boolean`
  - `fun isEnglishOnlyModel(fileName: String): Boolean` — contains `.en.`
  - `fun customModelName(fileName: String): String` — strips leading `ggml-` and trailing `.bin`.
  - In `AnimeDesktopSettings`: `whisperPath` (string, `""`), `whisperModel` (string, `"builtin:small"`), `customWhisperModels` (string set, empty), `generateFrom` (string, `"ja"`), `generateTo` (string, `"en"`). Keys = property names. Doc comment on each, matching the file's style.

- [ ] **Step 1: Write the failing test** — `WhisperModelsTest` with:
  - `choiceRoundTrips`: `decodeModelChoice(BuiltIn(MEDIUM).encode()) == BuiltIn(MEDIUM)` and `decodeModelChoice("file:/m/a b.bin") == Custom("/m/a b.bin")`.
  - `unknownChoiceFallsBackToSmall`: `decodeModelChoice("builtin:huge") == BuiltIn(SMALL)` and `decodeModelChoice("") == BuiltIn(SMALL)`.
  - `headerAcceptsGgmlAndGguf`: `byteArrayOf(0x6c,0x6d,0x67,0x67)` → true; `"GGUF".encodeToByteArray()` → true; `"WEBV".encodeToByteArray()` → false; 2-byte array → false.
  - `englishOnlyDetection`: `ggml-small.en.bin` → true; `ggml-small.bin` → false; `ggml-kotoba-whisper-v2.0.bin` → false.
  - `customName`: `customModelName("ggml-kotoba-whisper-v2.0.bin") == "kotoba-whisper-v2.0"`.
  - `catalogMatchesSpec`: `BuiltInWhisperModel.entries.map { it.id } == listOf("tiny","base","small","medium","large-v3","large-v3-turbo")`, only `SMALL.recommended`, only `LARGE_V3_TURBO.sameLanguageOnly`.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*WhisperModelsTest"` — Expected: FAIL.
- [ ] **Step 3: Implement** the Produces list.
- [ ] **Step 4: Run** — Expected: PASS.
- [ ] **Step 5: Commit** `feat(animeworld): whisper model catalog and desktop settings`

---

### Task 3: ffmpeg / whisper-cli commands and progress parsing

**Files:**
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/subtitlegen/WhisperCommand.kt`
- Test: `animeworld/shared/src/jvmTest/kotlin/com/programmersbox/anime/shared/subtitlegen/WhisperCommandTest.kt`

**Interfaces:**
- Consumes: `GenerationRequest`, `translate` (Task 1).
- Produces:
  - `fun extractAudioArgs(ffmpeg: String, video: File, wav: File): List<String>` = `[ffmpeg, "-hide_banner", "-nostdin", "-y", "-i", video.path, "-vn", "-ac", "1", "-ar", "16000", "-c:a", "pcm_s16le", wav.path]`
  - `fun whisperArgs(whisper: String, model: File, wav: File, outBase: File, request: GenerationRequest): List<String>` = `[whisper, "-m", model.path, "-f", wav.path, "-l", request.from.code, "-ovtt", "-of", outBase.path, "-pp"]` plus `"-tr"` appended when `request.translate`.
  - `fun parseWhisperProgress(line: String): Int?` — regex `progress\s*=\s*(\d{1,3})%`, clamped 0..100.
  - `suspend fun findWhisperCli(configured: String): File?` — configured path if executable; else `whisper-cli` (`whisper-cli.exe` on Windows) in `PATH` dirs, then `/opt/homebrew/bin`, `/usr/local/bin`, `/usr/bin`. Runs on `Dispatchers.IO`. Same shape as `VideoExportManager.ffmpeg()`.
  - `fun remainingMs(elapsedMs: Long, fraction: Float): Long?` — null when `fraction < 0.02f`, else `(elapsedMs / fraction * (1 - fraction)).toLong()`.

- [ ] **Step 1: Write the failing test** — `WhisperCommandTest`:
  - `extractArgsKeepPathsWhole`: video `File("/v/[SubsPlease] Frieren - 07 (1080p).mkv")` appears as one element right after `"-i"`.
  - `whisperArgsTranscribe`: ja→ja contains `"-l","ja"`, `"-ovtt"`, `"-pp"`, and no `"-tr"`.
  - `whisperArgsTranslate`: ja→en ends with `"-tr"`.
  - `whisperArgsAuto`: auto→en contains `"-l","auto"` and `"-tr"`.
  - `parsesProgressLines`: `"whisper_print_progress_callback: progress =  45%"` → 45; `"progress = 100%"` → 100; `"[00:00.000 --> 00:02.000] hi"` → null.
  - `remainingTime`: `remainingMs(60_000, 0.5f) == 60_000L`; `remainingMs(1_000, 0.01f) == null`.
  - `findsConfiguredWhisper`: a temp file set executable → `findWhisperCli(it.path) == it`.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*WhisperCommandTest"` — Expected: FAIL.
- [ ] **Step 3: Implement** the Produces list.
- [ ] **Step 4: Run** — Expected: PASS.
- [ ] **Step 5: Commit** `feat(animeworld): whisper and ffmpeg command builders`

---

### Task 4: Save generated tracks as sidecars, show CC labels on desktop

**Files:**
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/subtitlegen/GeneratedSidecars.kt`
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/downloads/DownloadNaming.kt` (`SidecarEntry`)
- Modify: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/downloads/OfflineSubtitles.kt` (expose index read)
- Modify: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videos/VideoLibrarySource.jvm.kt:164-173`
- Test: `animeworld/shared/src/jvmTest/kotlin/com/programmersbox/anime/shared/subtitlegen/GeneratedSidecarsTest.kt`

**Interfaces:**
- Consumes: `sidecarName`, `sidecarIndexName`, `SubtitleFormat.Vtt`, `OfflineSubtitles.find`.
- Produces:
  - `SidecarEntry` gains `val generated: Boolean = false` (default keeps old index files readable).
  - `object GeneratedSidecars { fun add(video: File, vtt: File, language: WhisperLanguage): File }` — moves `vtt` to `sidecarName(base, nextIndex, "${language.name} (generated)", Vtt)`, writes the index with entry `SidecarEntry(file = name, lang = language.code, label = "${language.name} (generated)", generated = true)`, returns the new file. Index is written to a temp file and renamed.
  - Desktop `toContent` sets `subtitleLabels = if (isDownload) OfflineSubtitles.find(file).map { it.lang } else emptyList()` (same as Android's `VideoLibrarySource.android.kt:74`).

- [ ] **Step 1: Write the failing test** — `GeneratedSidecarsTest` (temp dir, video `Frieren - 07.mkv`):
  - `addsTrackAndIndex`: after `add(video, vtt, English)`, `OfflineSubtitles.find(video).map { it.lang } == listOf("English (generated)")` and the source `vtt` no longer exists.
  - `keepsGlobSidecarsWhenNoIndex`: pre-create `Frieren - 07.0.English.vtt` and `Frieren - 07.1.Spanish.vtt` with no index; after `add(..., Japanese)` labels are `["English", "Spanish", "Japanese (generated)"]`.
  - `sameLanguageReplacesGeneratedOnly`: index has downloaded `English` + generated `English (generated)`; adding English again leaves exactly one `English (generated)` entry, deletes the old generated file, keeps the downloaded one.
  - `differentLanguageAdds`: generated English then generated Japanese → both present.
  - `oldIndexWithoutGeneratedFieldStillReads`: an index JSON without `generated` decodes and `find` returns its entries.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*GeneratedSidecarsTest"` — Expected: FAIL.
- [ ] **Step 3: Implement** `GeneratedSidecars.add`. Seed: read the index; if missing, build entries from `OfflineSubtitles.find(video)` (file name from the `file:` URI, `lang = label = found lang`). Next index = max existing numeric index + 1. Make `OfflineSubtitles.readIndex` `internal` rather than duplicating it.
- [ ] **Step 4: Implement** the `subtitleLabels` line in `VideoLibrarySource.jvm.kt`.
- [ ] **Step 5: Run** `./gradlew :animeworld:shared:jvmTest` — Expected: PASS (whole module, since `SidecarEntry` changed).
- [ ] **Step 6: Commit** `feat(animeworld): save generated subtitles as download sidecars`

---

### Task 5: Model store (downloads, user files, selection)

**Files:**
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/subtitlegen/WhisperModelStore.kt`
- Test: `animeworld/shared/src/jvmTest/kotlin/com/programmersbox/anime/shared/subtitlegen/WhisperModelStoreTest.kt`

**Interfaces:**
- Consumes: Task 2 (catalog, choice encoding, header check, settings keys).
- Produces:
  - `sealed interface ModelDownload { data object Missing; data class Downloading(val fraction: Float, val bytes: Long); data object Ready }`
  - `data class ModelRow(val choice: WhisperModelChoice, val name: String, val sizeLabel: String, val download: ModelDownload, val selected: Boolean, val path: String? /* custom only */)`
  - `class WhisperModelStore(settings: AnimeDesktopSettings, private val modelsDir: File, private val client: OkHttpClient, private val scope: CoroutineScope, private val baseUrl: String = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/")`
    - `val rows: StateFlow<List<ModelRow>>` — built-ins in catalog order, then custom models in the order added.
    - `val summary: StateFlow<ModelSummary>` — for the selected model; custom `translatesWell = true`, `sizeLabel` from file length.
    - `fun select(choice: WhisperModelChoice)`
    - `fun download(model: BuiltInWhisperModel)` — starts in `scope`; no-op if already downloading.
    - `fun delete(model: BuiltInWhisperModel)`
    - `suspend fun addCustom(file: File): AddModelResult` where `sealed interface AddModelResult { data object Added; data class Rejected(val message: String) }`. Rejected message: `"${file.name} isn't a whisper.cpp model. Pick a ggml .bin file."` Adding selects it; adding an already-listed path just selects it.
    - `fun removeCustom(path: String)` — never deletes the file; if it was selected, selects `BuiltIn(SMALL)`.
    - `suspend fun ensureSelected(onProgress: (Float) -> Unit): File` — returns the model file, downloading a missing built-in first; throws `ModelMissingException("Model file not found")` for a missing custom file.
    - `val downloadedBytes: StateFlow<Long>` — built-ins with `Ready` only.

- [ ] **Step 1: Write the failing test** — `WhisperModelStoreTest` (temp dirs, MockWebServer as `baseUrl`, a fresh DataStore per test the way other jvmTests create `AnimeDesktopSettings`):
  - `defaultSelectionIsSmall`: `rows.value.single { it.selected }.choice == BuiltIn(SMALL)`.
  - `downloadWritesPartThenRenames`: server returns 1 000 bytes for `/ggml-tiny.bin`; after `download(TINY)` completes, `ggml-tiny.bin` exists with 1 000 bytes, no `.part` remains, row is `Ready`.
  - `stalePartIsNotReady`: pre-create `ggml-tiny.bin.part`; row is `Missing`; a download replaces it.
  - `ensureSelectedDownloadsMissingBuiltIn`: select BASE, call `ensureSelected {}` → file exists and progress callback reached `1f`.
  - `addCustomRejectsNonModel`: file starting with `WEBVTT` → `Rejected("notes.bin isn't a whisper.cpp model. Pick a ggml .bin file.")` and rows unchanged.
  - `addCustomAddsAndSelects`: file starting with `lmgg` → `Added`, last row is custom, selected, name `kotoba-whisper-v2.0`.
  - `removeCustomKeepsFileAndFallsBack`: after `removeCustom`, file still exists and SMALL is selected.
  - `missingCustomFileThrows`: add then delete the file on disk → `ensureSelected` throws `ModelMissingException` with message `"Model file not found"`.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*WhisperModelStoreTest"` — Expected: FAIL.
- [ ] **Step 3: Implement** `WhisperModelStore`. Download: stream `response.body` to `<file>.part` in 64 KiB chunks, reporting `bytes / contentLength`; rename on success; delete `.part` on failure or cancel. `Ready` = final file exists (never the `.part`).
- [ ] **Step 4: Run** — Expected: PASS.
- [ ] **Step 5: Commit** `feat(animeworld): whisper model store with downloads and user models`

---

### Task 6: Generator queue and job runner

**Files:**
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/subtitlegen/DesktopSubtitleGenerator.kt`
- Test: `animeworld/shared/src/jvmTest/kotlin/com/programmersbox/anime/shared/subtitlegen/DesktopSubtitleGeneratorTest.kt`

**Interfaces:**
- Consumes: Tasks 1–5.
- Produces:
  - `fun interface CommandRunner { suspend fun run(args: List<String>, onLine: (String) -> Unit): Int }` and `object ProcessCommandRunner : CommandRunner` (real `ProcessBuilder`; stdout+stderr lines to `onLine`; destroys the process on cancellation via `runInterruptible`).
  - `class DesktopSubtitleGenerator(scope: CoroutineScope, settings: AnimeDesktopSettings, models: WhisperModelStore, ffmpeg: suspend () -> File?, whisper: suspend () -> File? = { findWhisperCli(settings.whisperPath.get()) }, runner: CommandRunner = ProcessCommandRunner, onSidecarsChanged: () -> Unit, trayState: TrayState?) : SubtitleGenerator`
  - `ModelSummary.translatesWell` = `!sameLanguageOnly` for built-ins (set in Task 5's `summary`).

**Behavior (decided here, not in the tests alone):**
- One worker coroutine reads a FIFO queue. While waiting, a job's status is `Queued(afterTitle)` where `afterTitle` = `splitEpisodeTitle(active.nameWithoutExtension).episode ?: nameWithoutExtension`.
- Run order: `ExtractingAudio` → (if needed) `DownloadingModel` → `Transcribing` (fraction from `parseWhisperProgress`, `remainingMs` from Task 3) → `GeneratedSidecars.add` → remove status → emit `finished` → `onSidecarsChanged()` → tray notification "Subtitles ready", `"<title>"`.
- Work in `createTempDirectory("animeworld-subgen")`, always deleted in `finally`.
- Failure messages: no ffmpeg → `"ffmpeg not found. Install it or set its location in Video export."`; no whisper → `"whisper-cli not found. Install whisper.cpp or set its location in Settings › Subtitles."`; non-zero exit → last non-blank output line, else `"whisper-cli exited with code N"`; `ModelMissingException` → its message. Failed keeps the status (`Failed`), notifies with tray error "Subtitles Failed", and the queue moves on.
- `enqueue` with `remember = true` saves `generateFrom`/`generateTo`; `lastRequest` reads them.
- `cancel` on the active job cancels it and starts the next; on a queued job removes it. `dismiss` removes a `Failed` status.

- [ ] **Step 1: Write the failing test** — `DesktopSubtitleGeneratorTest` with `StandardTestDispatcher`, a fake `CommandRunner` that records args, prints `progress = 50%` then `progress = 100%`, and writes `<outBase>.vtt` (`WEBVTT\n`) when it sees `-ovtt`; a model store with SMALL pre-written as a `lmgg` file:
  - `runsAndSavesSidecar`: enqueue ja→en; after idle, `statuses` is empty, `finished` emitted the path, `OfflineSubtitles.find(video)` has `"English (generated)"`, `onSidecarsChanged` called once.
  - `secondJobQueuesBehindFirst`: runner suspends on a gate; enqueue A then B; B's status is `Queued(afterTitle = A's episode)`; release gate → both finish, A before B.
  - `cancelActiveCleansUpAndStartsNext`: cancel A while running → no sidecar for A, temp dir gone, B runs.
  - `failureKeepsStatusAndMovesOn`: runner returns 1 with last line `"error: failed to open model"` → A is `Failed("error: failed to open model", request)`, B still runs.
  - `missingCustomModelFails`: selected custom file deleted → `Failed("Model file not found", …)`.
  - `missingWhisperFails`: `whisperPath` blank and no binary found (pass `whisper = { null }`) → `Failed("whisper-cli not found. Install whisper.cpp or set its location in Settings › Subtitles.", …)`.
  - `rememberSavesLanguages`: enqueue with `remember = true` ko→en → `lastRequest.value` is ko→en.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*DesktopSubtitleGeneratorTest"` — Expected: FAIL.
- [ ] **Step 3: Implement** `DesktopSubtitleGenerator` and `ProcessCommandRunner`, following `VideoExportManager`'s job/notify structure.
- [ ] **Step 4: Run** — Expected: PASS.
- [ ] **Step 5: Wire DI** in `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt` next to `VideoExportManager`: `single { WhisperModelStore(settings = get(), modelsDir = File(get<AppDirs>().getUserDataDir(), "whisper-models"), client = OkHttpClient(), scope = CoroutineScope(Dispatchers.IO + SupervisorJob())) }` and `single { DesktopSubtitleGenerator(scope = CoroutineScope(Dispatchers.IO + SupervisorJob()), settings = get(), models = get(), ffmpeg = { get<VideoExportManager>().ffmpeg() }, onSidecarsChanged = { get<VideoLibrarySource>().rescan() }, trayState = get()) } bind SubtitleGenerator::class`. Use whichever `OkHttpClient` the desktop module already registers if one exists.
- [ ] **Step 6: Run** `./gradlew :animeworld:desktop:compileKotlinJvm` — Expected: BUILD SUCCESSFUL.
- [ ] **Step 7: Commit** `feat(animeworld): desktop subtitle generator queue`

---

### Task 7: Generated subtitles section in Subtitles settings

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/subtitlegen/GeneratedSubtitlesSettings.kt` (`@Composable internal expect fun GeneratedSubtitlesSettings()`)
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/subtitlegen/GeneratedSubtitlesSettings.jvm.kt`
- Create: `.../androidMain/.../subtitlegen/GeneratedSubtitlesSettings.android.kt`, `.../iosMain/.../subtitlegen/GeneratedSubtitlesSettings.ios.kt` (empty bodies)
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/subtitles/SubtitleSettingsScreen.kt` — call `GeneratedSubtitlesSettings()` after the "Skip signs and songs tracks" switch, before the bottom `Spacer`.

**Interfaces:**
- Consumes: `WhisperModelStore` (`rows`, `select`, `download`, `delete`, `addCustom`, `removeCustom`, `downloadedBytes`), `AnimeDesktopSettings.whisperPath`, `findWhisperCli`.

**UI (copy from the preview):**
- `HorizontalDivider` + `SectionHeader("Generated subtitles")`; intro "The speech model turns an episode's audio into subtitles on this computer. Bigger models are more accurate but slower and take more disk space."
- One `ListItem` per row with `Modifier.selectable(selected, role = Role.RadioButton)`, leading `RadioButton(onClick = null)`. Headline = name + badges ("Recommended" / "Same-language only" / "Your file"). Supporting = size + "Speed" and "Accuracy" 5-segment meters (built-in) or path (monospace) + size + "Speed and accuracy depend on the model" (custom). Turbo adds "Fast and accurate, but translates poorly into English."
- Trailing `AnimatedContent(download)`: `OutlinedButton("Download")` → nothing (bar shows under text via `AnimatedVisibility(expandVertically())` + `LinearProgressIndicator` + `"<done MB> of <size> · N%"`) → check icon + delete `IconButton`. Custom rows: delete `IconButton` with `contentDescription = "Remove <name> from the list"`.
- "Add model file…" row (+ supporting "A whisper.cpp model (.bin), such as a fine-tuned or quantized one") opens `JFileChooser` (same pattern as `VideoExportScreen.kt:429-431`, title "Choose a whisper.cpp model"); a `Rejected` message shows below in `colorScheme.error`.
- "whisper-cli location" row: found path + " · found automatically" (or the configured path), trailing `OutlinedButton("Choose…")` → `JFileChooser` → `settings.whisperPath.set(...)`. When none found: "Not found. Install whisper.cpp (brew install whisper-cpp) or choose the file."
- Footer: "Downloaded models use <X MB|X.X GB>. Models not downloaded yet download the first time you generate. Your own files stay where they are and aren't counted."
- The settings section is in a `Column` (not a lazy list), so `animateItem()` does not apply; animate added/removed custom rows with `AnimatedVisibility` keyed by path.

- [ ] **Step 1: Implement** the expect/actuals and the jvm section.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:compileKotlinJvm :animeworld:shared:compileAndroidMain` — Expected: BUILD SUCCESSFUL. (If the Android compile task name differs, use the one listed by `./gradlew :animeworld:shared:tasks --all | grep -i compile.*Android`.)
- [ ] **Step 3: Verify by hand** with `./gradlew :animeworld:desktop:run`: open Settings › Subtitles; Small is selected; Download on Tiny shows the bar and ends with ✓; Add model file… with a non-model file shows the error; with a real ggml file adds and selects it; Remove keeps the file on disk.
- [ ] **Step 4: Commit** `feat(animeworld): generated subtitles settings section`

---

### Task 8: Menu item, dialog, progress under the row, snackbar

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/subtitlegen/GenerateSubtitlesDialog.kt`
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/subtitlegen/GenerationProgress.kt`
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videos/DownloadRows.kt` (`VideoRow`)
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videos/ViewVideoScreen.kt` (+ `ViewVideoScreen.jvm.kt`, `.android.kt`, `.ios.kt`)
- Modify: `CHANGELOG.md`

**Interfaces:**
- Consumes: `SubtitleGenerator` (Task 1), `targetOptions`, `fromOptions`.
- Produces:
  - `@Composable internal expect fun rememberSubtitleGenerator(): SubtitleGenerator?` — jvm: `koinInject<SubtitleGenerator>()`; android/ios: `null`.
  - `@Composable fun GenerateSubtitlesDialog(title: EpisodeTitle, initial: GenerationRequest, model: ModelSummary, onOpenSettings: () -> Unit, onGenerate: (GenerationRequest, remember: Boolean) -> Unit, onDismiss: () -> Unit)`
  - `@Composable fun GenerationProgress(status: GenerationStatus, onRetry: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier)`
  - `VideoRow` gains `onGenerateSubtitles: (() -> Unit)? = null`, `generation: GenerationStatus? = null`, `onCancelGeneration: () -> Unit = {}`, `onRetryGeneration: () -> Unit = {}`, `onDismissGeneration: () -> Unit = {}`. `VideoRowContent` gains a `below: @Composable ColumnScope.() -> Unit = {}` slot rendered at the end of the text `Column`.

**UI (copy from the preview):**
- Menu: first item "Generate subtitles…" with supporting "Runs on this computer" and a closed-caption icon; shown when `onGenerateSubtitles != null` and the item `isDownload`. Shown even when the episode already has subtitles.
- Trailing slot: `AnimatedContent(generation is Queued/ExtractingAudio/DownloadingModel/Transcribing)` swaps ⋮ for ✕ (`contentDescription = "Cancel generating subtitles"`) with `scaleIn() + fadeIn()`.
- `GenerationProgress` inside `AnimatedVisibility(enter = expandVertically() + fadeIn())`: `LinearProgressIndicator` — indeterminate for `ExtractingAudio`, empty for `Queued`, else `animateFloatAsState(fraction, ProgressIndicatorDefaults.ProgressAnimationSpec)`. Status text via `AnimatedContent` (`slideInVertically { it / 2 } + fadeIn() togetherWith fadeOut()`):
  - Queued: "Queued · starts after <afterTitle>"
  - ExtractingAudio: "Extracting audio…"
  - DownloadingModel: "Downloading <modelName>" · "N%"
  - Transcribing: "<From> → <To>" or "Transcribing <From>" when equal · "N% · about M min left" / "less than a minute" (ceil minutes; no ETA text when `remainingMs == null`)
  - Failed: bar hidden, text in `colorScheme.error` "Couldn't generate subtitles: <message>" with `TextButton`s "Retry" / "Dismiss".
- Dialog (`AlertDialog`): title "Generate subtitles"; subtitle "<show> · <episode>"; two `ExposedDropdownMenuBox` fields "From" → "To" in a `Row` with an arrow; disabled To items via `DropdownMenuItem(enabled = false)` showing the note as supporting text; hint "Whisper translates into English only. Run it again with another “To” language to add a second track."; model hint "Model: <name> · Change" (or "Model: <name> (downloads first, <size>) · Change"); when `!model.translatesWell && request.translate`, the hint is error-colored "<name> translates poorly into English. Choose another model"; checkbox "Use these choices next time" (checked by default); buttons "Cancel" / "Generate". If `model.englishOnly`, From is fixed to English. "Change" / "Choose another model" call `onOpenSettings`, which navigates to `SubtitleSettingsRoute`.
- `ViewVideoScreen`: `val generator = rememberSubtitleGenerator()`; collect `statuses`; add `SnackbarHostState` to the existing `Scaffold`; on `finished` show `showSnackbar("Subtitles ready for <episode>", actionLabel = "Play")` and on action navigate to the same `VideoScreen(...)` the row click uses.
- `CHANGELOG.md` under `## [Unreleased]`: a bullet "**Generate subtitles on Desktop (AnimeWorld).** Downloaded episodes can get subtitles made on your computer with whisper.cpp, from the Downloads menu. Pick From and To languages, track progress under the episode, and choose or add your own speech model in Settings › Subtitles." PR link added when the PR exists.

- [ ] **Step 1: Implement** the dialog, progress composable, `VideoRow`/`VideoRowContent` changes, expect/actuals and `ViewVideoScreen` wiring.
- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest :animeworld:desktop:compileKotlinJvm :animeworld:assembleNoFirebaseDebug` — Expected: tests PASS, both builds succeed (Android must still compile with the new optional params).
- [ ] **Step 3: Verify by hand** with `./gradlew :animeworld:desktop:run` and `brew install whisper-cpp`: on a downloaded episode, ⋮ → Generate subtitles… → Japanese → English → Generate. The bar moves Extracting audio → Downloading Small (first run) → Japanese → English with percent; ✕ appears; a second episode shows "Queued · starts after …"; on finish the CC tag shows "English (generated)", the snackbar's Play opens the player and the track is selectable. Cancel mid-run leaves no new sidecar.
- [ ] **Step 4: Commit** `feat(animeworld): generate subtitles from the desktop Downloads screen`
