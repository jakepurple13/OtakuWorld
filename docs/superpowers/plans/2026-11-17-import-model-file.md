# Import Model File Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An "Import model file" flow on the AI page that copies a picked `.litertlm` into the models folder on Android and desktop, plus a desktop on-device engine that uses it.

**Architecture:** A shared `ModelImporter` (usesJvmMain, app-scoped Koin single) owns the import state machine and file work, taking the source as a `RawSource` opener so it is testable without FileKit. Verification runs one generation through the local `SummaryEngine` (`SummaryEngineId.Gemma`). Desktop gains `LocalModelEngine` on `litertlm-jvm`. The AI page's Engine card renders the import states.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Koin, FileKit 0.17, kotlinx-io, LiteRT-LM (`litertlm-jvm` 0.18.1).

**Spec:** `docs/superpowers/specs/2026-11-17-import-model-file.md`

## Global Constraints

- Extension check: name ends with `.litertlm` (case-insensitive).
- Space check: `size + 200 MB > usable space` → NoSpace.
- Copy to `<name>.part`, then atomic move; delete the part on cancel/failure.
- Failed check deletes the copied file. Replace deletes the old file only after the new one passes.
- Remove deletes the model file and cache files whose name starts with the model file name.
- Help link: `https://huggingface.co/litert-community/Gemma3-1B-IT`.
- Desktop automatic order: `[Gemma, Ollama]`. Prefer control hidden on desktop.
- Every lazy item gets `Modifier.animateItem()` (project rule).

## Review Focus

1. Cancel during copy leaves no `.part` and no model file; state returns to Idle.
2. Replace where the new file fails the check keeps the old model in place.
3. Importing a file with the same name as the current model overwrites only after the check passes.
4. Leaving and reopening the AI page mid-copy shows live progress (app-scoped importer).
5. Desktop engine with no model never loads native code (status reads only the folder).

---

### Task 1: ModelImporter

**Files:**
- Create: `kmpuiviews/src/usesJvmMain/kotlin/com/programmersbox/kmpuiviews/domain/aitext/modelimport/ModelImporter.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/aitext/modelimport/ModelImporterTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface ImportState { Idle; Copying(name: String, copied: Long, total: Long); Checking(name: String); Failed(reason: ImportError) }`
  - `sealed interface ImportError { NotAModel; NoSpace(needed: Long, available: Long); WontLoad(message: String?); CopyFailed(message: String?) }`
  - `class ModelImporter(modelsDir: File, cacheDir: File, scope: CoroutineScope, verify: suspend (File) -> Unit, usableSpace: (File) -> Long = { it.usableSpace })`
    - `val state: StateFlow<ImportState>`
    - `fun import(name: String, size: Long, open: () -> RawSource, replacing: String?)` — no-op while busy
    - `fun cancel()`; `fun dismissError()`; `fun remove(fileName: String)`

- [ ] **Step 1: Tests** — `wrong extension fails NotAModel without writing`, `no space fails NoSpace with numbers`, `copies via part file then verifies and goes Idle`, `progress reports copied bytes`, `cancel deletes part and returns Idle`, `failed verify deletes the copy and keeps the replaced file`, `successful replace deletes the old file`, `same name replace keeps old until verify passes` (copy to part, verify the part renamed to a temp name, then move over), `remove deletes model and its cache files`.
- [ ] **Step 2:** Run `./gradlew :kmpuiviews:jvmTest --tests "*ModelImporterTest*"` → FAIL (unresolved).
- [ ] **Step 3:** Implement. Copy in 1 MB chunks on `Dispatchers.IO`, `ensureActive()` per chunk. Verify target: `<name>.part` moved to `<name>.checking` before verify when a file of that name exists, else straight to `<name>`.
- [ ] **Step 4:** Run tests → PASS.
- [ ] **Step 5:** Commit `feat(ai): ModelImporter copies and checks a picked model file`.

### Task 2: Desktop LocalModelEngine

**Files:**
- Modify: `gradle/libs.versions.toml` (add `litertlm-jvm = { module = "com.google.ai.edge.litertlm:litertlm-jvm", version = "0.18.1" }`), `kmpuiviews/build.gradle.kts` (jvmMain dep)
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/dailyoverview/LocalModelEngine.kt`
- Modify: `kmpuiviews/src/jvmMain/.../domain/dailyoverview/PlatformAutomaticOrder.jvm.kt` → `listOf(Gemma, Ollama)`
- Modify: `kmpuiviews/src/jvmMain/.../di/AppModule.jvm.kt` (register as `SummaryEngine`)
- Test: `kmpuiviews/src/jvmTest/.../dailyoverview/LocalModelEngineTest.kt`

**Interfaces:**
- Produces: `class LocalModelEngine(modelsDir: File, cacheDir: File) : SummaryEngine` with `id = Gemma`, `onDevice = true`, `fun model(): File?` (newest `.litertlm`), `suspend fun verify(file: File)` (loads `file`, one short generation, throws on failure).

- [ ] **Step 1: Tests** — `no model means NeedsSetup and model() null`, `newest litertlm wins`, `label uses display name`. Plus `real model generates` gated by env `LITERTLM_MODEL` (skipped when unset).
- [ ] **Step 2:** Run → FAIL.
- [ ] **Step 3:** Implement with `Engine(EngineConfig(path, Backend.CPU(), cacheDir = ...))`, `initialize()`, `createConversation(ConversationConfig(systemInstruction = Contents.of(system)))`, `sendMessage(user)`; emit whole reply; close conversation then engine; `flowOn(Dispatchers.IO)`.
- [ ] **Step 4:** Run tests; run the gated test with `LITERTLM_MODEL=~/Downloads/gemma3-1b-it-int4.litertlm` → PASS on macOS.
- [ ] **Step 5:** Commit `feat(desktop): on-device LiteRT engine for imported models`.

### Task 3: Android engine fallback + verify + Koin

**Files:**
- Modify: `kmpuiviews/koogintegration/src/androidMain/.../AgentMaker.android.kt` (`downloadedLiteRtModels()` sorted newest first)
- Modify: `kmpuiviews/src/androidMain/.../dailyoverview/GemmaEngine.kt` (add `suspend fun verify(file: File)` loading that file through `liteRtClient()` with a one-line prompt)
- Modify: `AppModule.android.kt`, `AppModule.jvm.kt` — `single { ModelImporter(modelsDir, cacheDir, appScope, verify = engine::verify) }`

- [ ] **Step 1:** Test in koogintegration not possible (Android); covered by device check in Task 5. Implement.
- [ ] **Step 2:** `./gradlew :mangaworld:assembleNoFirebaseDebug :mangaworld:desktop:compileKotlinJvm` → BUILD SUCCESSFUL.
- [ ] **Step 3:** Commit `feat(ai): wire ModelImporter on Android and desktop`.

### Task 4: AI page UI

**Files:**
- Modify: `AiSettingsViewModel.kt` (inject `ModelImporter`, expose `importState`, `import(PlatformFile)`, `cancelImport`, `removeModel`, refresh engines when an import ends)
- Modify: `AiSettingsScreen.kt` (`LocalModelRow` inside EngineCard for the Gemma engine; states per spec; `AnimatedContent`; ⋮ `DropdownMenu`; help link via `LocalUriHandler`)
- Create: `kmpuiviews/src/commonMain/.../settings/ai/ModelDropTarget.kt` (`expect fun Modifier.modelFileDrop(onFile: (PlatformFile) -> Unit): Modifier`), actual no-op on Android/iOS, `dragAndDropTarget` on jvm
- Modify: `strings.xml` (new strings)
- Test: `AiSettingsViewModelTest` — `import state follows importer`, `engines refresh after import finishes`

- [ ] **Step 1:** Write the two VM tests → FAIL.
- [ ] **Step 2:** Implement VM + UI.
- [ ] **Step 3:** Tests PASS; `:kmpuiviews:jvmTest` suite green.
- [ ] **Step 4:** Commit `feat(ai): import model file on the AI page`.

### Task 5: Packaging, docs, device check

- [ ] Strip other-OS `com/google/ai/edge/litertlm/jni/*` from desktop packages (Gradle artifact transform or jar filter in the desktop app module). Verify with `./gradlew :mangaworld:desktop:createDistributable` and list the bundled jar.
- [ ] `docs/small-model.md`: add the in-app import as the main path; adb as the debug alternative.
- [ ] Emulator: import Gemma 3 1B through the picker; Ready; Run sample; Remove. Desktop: import, Run sample.
- [ ] Commit, push, open PR against develop.
