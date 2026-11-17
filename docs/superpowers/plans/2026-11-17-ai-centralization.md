# AI Setup Centralization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move every on-device AI feature's setup into one sealed `AiFeature` class, with a generic `AiText.run`, a shared `AiTask`, and a shared test fake. Nothing changes for the user.

**Architecture:** `AiFeature<I>` (sealed, in `domain/aitext/AiFeature.kt`) holds each feature's prompts, input trim, ML Kit task and output cleanup. `DefaultAiText` runs any feature generically. `AiTask` replaces `AiTextRunner` and the per-ViewModel engine-check code. `NanoTools` is keyed by `NanoTask`. Every task leaves the module compiling: Task 2 keeps the old per-feature `AiText` methods as default methods, and Task 6 removes them.

**Tech Stack:** Kotlin Multiplatform, coroutines/Flow, Koin, kotlin.test + kotlinx-coroutines-test (jvmTest), ML Kit GenAI (androidMain).

**Spec:** `docs/superpowers/specs/2026-11-17-ai-centralization-design.md`

## Global Constraints

- No user-visible change. Existing test assertions stay the same. Only fakes and call shapes may change.
- AI text never uses `SummaryEngineId.Provider` or `SummaryEngineId.Template`.
- Only `Proofread` and `Rewrite` skip Nano for input over `AiTextPrompts.NANO_TEXT_LIMIT` (4_000).
- Prompt strings are byte-for-byte the same as today's `AiTextPrompts.system/user` and `LIST_DESCRIPTION_SYSTEM`.
- `DefaultAiText` keeps its process-wide generation `Mutex` and runs its work on `Dispatchers.IO`.
- All tests: `./gradlew :kmpuiviews:jvmTest`. Android compile check: `./gradlew :kmpuiviews:compileAndroidMain`. If that task name is wrong, find it with `./gradlew :kmpuiviews:tasks --all | grep -i "compile.*android"`.

## Review Focus

1. Hiding the TL;DR (or discarding a note preview) while the engine is still resolving must not start a run afterwards. Pinned by `AiTaskTest.cancel during engine resolution starts nothing` (Task 3).
2. A long description (over 4_000 chars) must still be allowed to use Nano for TL;DR, while a long note must not use Nano for proofread. Pinned by `DefaultAiTextTest.long tldr input may still use nano` (Task 2).
3. A rewrite must reach ML Kit with the chosen style, not the default Rephrase. Pinned by `DefaultAiTextTest.rewrite passes its style to nano tools` (Task 2).
4. List-name cleanup applies only to the finished text. Streaming shows raw text, as it does today. Pinned by `AiTaskTest.finish applies only to done` (Task 3).
5. If Provider is ever added to the Daily overview automatic order, AI text must still ignore it. Pinned by `DefaultAiTextTest.provider in order is ignored` (Task 1).

---

### Task 1: One engine order

**Files:**
- Delete: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/aitext/OnDeviceOrder.kt`, and the `OnDeviceOrder.{android,jvm,ios}.kt` actuals under `androidMain`, `jvmMain`, `iosMain` `.../domain/aitext/`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/aitext/DefaultAiText.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/aitext/DefaultAiTextTest.kt`

**Interfaces:**
- Produces: `DefaultAiText(engines, nanoTools, order: List<SummaryEngineId> = platformAutomaticOrder, generationLock)`. Internally it uses `order - Provider - Template`.

- [ ] **Step 1: Write the failing test** in `DefaultAiTextTest`

```kotlin
@Test
fun `provider in order is ignored`() = runTest {
    val ai = DefaultAiText(
        listOf(TemplateEngine(), FakeEngine(SummaryEngineId.Provider)),
        nanoTools = null,
        order = listOf(SummaryEngineId.Provider),
    )
    assertNull(ai.engineFor(AiFeature.Tldr))
}
```
(`FakeEngine` is the existing test helper. If it hard-codes `onDevice = true`, that's fine. The test is about the id.)

- [ ] **Step 2: Run it.** `./gradlew :kmpuiviews:jvmTest --tests "*DefaultAiTextTest*"`. Expected: FAIL (Provider is returned).
- [ ] **Step 3: Implement.** Default `order` becomes `platformAutomaticOrder`. Build both resolvers from `order - SummaryEngineId.Provider - SummaryEngineId.Template`. Delete the four `OnDeviceOrder*` files and the `onDeviceAiOrder` import.
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest --tests "*DefaultAiTextTest*"` and `./gradlew :kmpuiviews:compileAndroidMain`. Expected: PASS, BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `refactor(ai): one engine order for AI text and daily overview`

---

### Task 2: Sealed `AiFeature`, generic `AiText.run`, `NanoTask`, shared fake

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/aitext/AiFeature.kt`
- Modify: `.../domain/aitext/AiText.kt` (remove the `AiFeature` enum, new interface), `.../domain/aitext/DefaultAiText.kt`, `.../domain/aitext/NanoTools.kt`, `.../domain/aitext/AiTextPrompts.kt` (delete `system`, `user`, `LIST_DESCRIPTION_SYSTEM`; keep helpers and constants)
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/aitext/MlKitNanoTools.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/lists/OtakuCustomListViewModel.kt` (`AiFeature.ListSuggest` → `AiFeature.ListNames`, 2 places)
- Create: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/testing/FakeAiText.kt`. Delete `testing/NoopAiText.kt`.
- Modify tests: `DefaultAiTextTest`, `AiTextPromptsTest` (the rewrite-prompt test moves to `AiFeatureTest`), `ChapterRecapsTest`, `DetailsViewModelTest`, `NoteAiAssistantTest`, `OtakuCustomListViewModelTest`, `DetailsNotesViewModelTest`, `NoteDetailViewModelTest`. Each one's private fake is replaced by `FakeAiText`.
- Create test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/aitext/AiFeatureTest.kt`

**Interfaces:**
- Produces:
```kotlin
sealed interface NanoTask {
    data object Summarize : NanoTask
    data object Proofread : NanoTask
    data class Rewrite(val style: RewriteStyle) : NanoTask
}

sealed class AiFeature<in I>(
    val system: String,
    val nanoTask: NanoTask? = null,
    /** Input longer than this skips Nano. Null = no cap. */
    val nanoMaxChars: Int? = null,
) {
    /** The text actually sent; its length picks the engine. */
    abstract fun prepare(input: I): String
    open fun userPrompt(prepared: String): String = prepared
    /** Applied to the finished text only. */
    open fun finish(raw: String): String = raw

    data object Recap : AiFeature<String>(...)            // prepare = recapTail, user = "Chapter ending:\n$p"
    data object Tldr : AiFeature<String>(..., NanoTask.Summarize)   // prepare = tldrHead, user = "Description:\n$p"
    data object Proofread : AiFeature<String>(..., NanoTask.Proofread, NANO_TEXT_LIMIT)  // prepare = identity
    data class Rewrite(val style: RewriteStyle) : AiFeature<String>(rewriteSystem(style), NanoTask.Rewrite(style), NANO_TEXT_LIMIT)
    data object ListNames : AiFeature<List<String>>(...)  // prepare = take(MAX_LIST_TITLES).joinToString("\n"), user = "Titles in the list:\n$p", finish = parseNames(raw).joinToString("\n")
    data object ListDescription : AiFeature<List<String>>(LIST_DESCRIPTION_SYSTEM)  // same prepare/user as ListNames
}

interface AiText {
    /** One status check. Null when no on-device engine can take input of [inputLength] (prepared length). */
    suspend fun engineFor(feature: AiFeature<*>, inputLength: Int = 0): AiEngineInfo?
    /** Streams deltas. Throws IllegalStateException("No on-device AI engine is ready") when none is ready. */
    fun <I> run(feature: AiFeature<I>, input: I): Flow<String>
    /** Emits null and re-checks every [retry] until ready, then emits the engine and completes. */
    fun availability(feature: AiFeature<*>, inputLength: Int = 0, retry: Duration = 2.seconds): Flow<AiEngineInfo?>  // default impl

    // Temporary (removed in Task 6), default impls delegating to run():
    fun recap(previousChapterText: String) = run(AiFeature.Recap, previousChapterText)
    fun tldr(description: String) = run(AiFeature.Tldr, description)
    fun proofread(text: String) = run(AiFeature.Proofread, text)
    fun rewrite(text: String, style: RewriteStyle) = run(AiFeature.Rewrite(style), text)
    fun listNames(titles: List<String>) = run(AiFeature.ListNames, titles)
    fun listDescription(titles: List<String>) = run(AiFeature.ListDescription, titles)
}

interface NanoTools {
    suspend fun isReady(task: NanoTask, inputLength: Int): Boolean
    fun run(task: NanoTask, input: String): Flow<String>
}

// jvmTest testing/FakeAiText.kt
class FakeAiText(
    var engine: AiEngineInfo? = ENGINE,
    /** Overrides [engine] when set; may return null. */
    var engineRule: (suspend (AiFeature<*>, Int) -> AiEngineInfo?)? = null,
    var output: (AiFeature<*>, Any?) -> Flow<String> = { _, _ -> flowOf("out") },
) : AiText {
    val runs: MutableList<Pair<AiFeature<*>, Any?>>   // every run(feature, input), in order
    var engineCalls: Int
    companion object { val ENGINE = AiEngineInfo(label = "Fake", onDevice = true, destination = null) }
}
```

- [ ] **Step 1: Write the failing tests**

`AiFeatureTest`:
```kotlin
@Test fun `each rewrite style has its own prompt`() =
    assertEquals(RewriteStyle.entries.size, RewriteStyle.entries.map { AiFeature.Rewrite(it).system }.toSet().size)
@Test fun `tldr prepares head and wraps prompt`() {
    val p = AiFeature.Tldr.prepare("a ".repeat(3_000))
    assertTrue(p.length <= AiTextPrompts.NANO_TEXT_LIMIT)
    assertTrue(AiFeature.Tldr.userPrompt(p).startsWith("Description:\n"))
}
@Test fun `list names caps titles and parses on finish`() {
    assertEquals(AiTextPrompts.MAX_LIST_TITLES, AiFeature.ListNames.prepare(List(40) { "t$it" }).lines().size)
    assertEquals("A\nB", AiFeature.ListNames.finish("1. A\n2. a\n- B"))
}
@Test fun `only proofread and rewrite cap nano length`() {
    assertEquals(AiTextPrompts.NANO_TEXT_LIMIT, AiFeature.Proofread.nanoMaxChars)
    assertEquals(AiTextPrompts.NANO_TEXT_LIMIT, AiFeature.Rewrite(RewriteStyle.Shorten).nanoMaxChars)
    listOf(AiFeature.Recap, AiFeature.Tldr, AiFeature.ListNames, AiFeature.ListDescription).forEach { assertNull(it.nanoMaxChars) }
}
```

`DefaultAiTextTest`: port the existing tests to `run(...)` / `engineFor(AiFeature.X, ...)`, update `FakeNanoTools` to record `tasks: MutableList<NanoTask>`, and add:
```kotlin
@Test fun `long tldr input may still use nano`() = runTest {
    val ai = aiText(FakeEngine(SummaryEngineId.Nano), FakeEngine(SummaryEngineId.Gemma))
    assertEquals("Nano", ai.engineFor(AiFeature.Tldr, AiTextPrompts.NANO_TEXT_LIMIT + 1)?.label)
}
@Test fun `rewrite passes its style to nano tools`() = runTest {
    val tools = FakeNanoTools()
    aiText(FakeEngine(SummaryEngineId.Nano), tools = tools).run(AiFeature.Rewrite(RewriteStyle.Emojify), "hi").toList()
    assertEquals(listOf<NanoTask>(NanoTask.Rewrite(RewriteStyle.Emojify)), tools.tasks)
}
@Test fun `availability retries until ready`() = runTest {
    val nano = FakeEngine(SummaryEngineId.Nano).apply { status = EngineStatus.Unavailable }
    val seen = mutableListOf<AiEngineInfo?>()
    val job = launch { aiText(nano).availability(AiFeature.Tldr, retry = 2.seconds).toList(seen) }
    runCurrent(); assertEquals(listOf(null), seen)
    nano.status = EngineStatus.Ready
    advanceTimeBy(2.seconds); runCurrent()
    assertEquals("Nano", seen.last()?.label); assertTrue(job.isCompleted)
}
```
(If `FakeEngine` has no mutable `status`, add `var status: EngineStatus = EngineStatus.Ready`. `engineFor` switches to `Dispatchers.IO`. If virtual time can't drive that, inject the dispatcher: `DefaultAiText(..., ioDispatcher: CoroutineDispatcher = Dispatchers.IO)`.)

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*aitext*"`. Expected: compile FAIL (types don't exist).
- [ ] **Step 3: Implement** `AiFeature.kt` (copy the prompt strings verbatim from `AiTextPrompts.system/user` and `LIST_DESCRIPTION_SYSTEM`), the new `AiText`, and `NanoTools`. `DefaultAiText.run`: `prepared = feature.prepare(input)`. Resolve with `withoutNano` when `feature.nanoMaxChars?.let { prepared.length > it } == true`. Inside the lock, use `nanoTools.run(task, prepared)` when the engine is Nano, `feature.nanoTask != null`, and `isReady(task, prepared.length)`. Otherwise use `engine.generate(feature.system, feature.userPrompt(prepared))`. In `MlKitNanoTools`, replace the feature `when`s with `NanoTask` `when`s (key `downloadStarted` by `task::class`; `Rewrite` checks status with `rewriter(task.style)`). Write `FakeAiText`, then move every test to it.
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest` and `./gradlew :kmpuiviews:compileAndroidMain`. Expected: all PASS, BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `refactor(ai): sealed AiFeature with generic AiText.run`

---

### Task 3: `AiTask` + Details

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/aitext/AiTask.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsViewModel.kt` (`tldrAvailable` around line 129, `tldrRunner` around line 146, the run block around lines 390-415)
- Test: create `.../jvmTest/.../domain/aitext/AiTaskTest.kt`. `DetailsViewModelTest` keeps its assertions.

**Interfaces:**
- Consumes: `AiText.engineFor/run/availability`, `AiFeature.finish`, `FakeAiText` (Task 2).
- Produces:
```kotlin
class AiTask(private val aiText: AiText, private val scope: CoroutineScope) {
    val state: StateFlow<AiTextState>
    /** Cancels any previous run. Returns false, with state unchanged, when no engine is ready. Otherwise launches in [scope] and returns true. Done text = feature.finish(raw). */
    suspend fun <I> start(feature: AiFeature<I>, input: I): Boolean
    fun reset()
}
```
`errorMessage(e)` moves into `AiTask.kt` with the same visibility.

- [ ] **Step 1: Write the failing tests.** Port `AiTextRunnerTest`'s cases (Loading → Streaming → Done, exception → Error, a new run cancels the old one, reset → Idle) to `AiTaskTest` via `FakeAiText`, and add:
```kotlin
@Test fun `no engine returns false and stays idle`() = runTest {
    val task = AiTask(FakeAiText(engine = null), backgroundScope)
    assertFalse(task.start(AiFeature.Tldr, "x")); assertEquals(AiTextState.Idle, task.state.value)
}
@Test fun `finish applies only to done`() = runTest {
    val ai = FakeAiText(output = { _, _ -> flowOf("1. A\n", "2. a") })
    val task = AiTask(ai, this); val seen = collectStates(task)
    task.start(AiFeature.ListNames, listOf("t")); advanceUntilIdle()
    assertEquals("1. A\n", (seen.first { it is AiTextState.Streaming } as AiTextState.Streaming).text)
    assertEquals("A", (task.state.value as AiTextState.Done).text)
}
@Test fun `cancel during engine resolution starts nothing`() = runTest {
    val gate = CompletableDeferred<Unit>()
    val ai = FakeAiText(engineRule = { _, _ -> gate.await(); FakeAiText.ENGINE })
    val task = AiTask(ai, backgroundScope)
    val job = launch(Dispatchers.Default) { task.start(AiFeature.Tldr, "x") }
    job.cancel(); gate.complete(Unit); job.join(); advanceUntilIdle()
    assertEquals(AiTextState.Idle, task.state.value); assertTrue(ai.runs.isEmpty())
}
```

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*AiTaskTest*"`. Expected: compile FAIL.
- [ ] **Step 3: Implement `AiTask`.** Make `tldrAvailable` = `aiText.availability(AiFeature.Tldr, AiFeature.Tldr.prepare(text).length).map { it != null }` inside the existing length guard. This drops the `TLDR_ENGINE_RETRY` loop. Replace `tldrRunner` with `AiTask(aiText, viewModelScope)`. The run becomes `withContext(Dispatchers.Main) { tldrTask.start(AiFeature.Tldr, text) }`, then return if false. `hideTldr` calls `tldrTask.reset()`. Delete `TLDR_ENGINE_RETRY` if it's now unused.
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest --tests "*AiTaskTest*" --tests "*DetailsViewModelTest*"`. Expected: PASS.
- [ ] **Step 5: Commit** `refactor(ai): AiTask, used by Details TL;DR`

---

### Task 4: Notes on `AiTask`

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/notes/NoteAiAssistant.kt`
- Test: `NoteAiAssistantTest` (assertions unchanged)

**Interfaces:** Consumes `AiTask.start/reset/state` (Task 3) and `AiFeature.Proofread`, `AiFeature.Rewrite(style)`.

- [ ] **Step 1:** Run `./gradlew :kmpuiviews:jvmTest --tests "*NoteAiAssistantTest*"` to record the green baseline.
- [ ] **Step 2: Implement.** `runner` becomes `AiTask(aiText, scope)`. `start(...)` takes `feature: AiFeature<String>` and calls `if (!task.start(feature, text)) pending.value = null`. Remove the `block` lambda parameter. `resolveEngines` keeps its four `engineFor` checks, now with `AiFeature.Rewrite(RewriteStyle.Rephrase)` standing in for "rewrite".
- [ ] **Step 3: Run** the same test. Expected: PASS.
- [ ] **Step 4: Commit** `refactor(ai): notes tools use AiTask`

---

### Task 5: Custom lists on `AiTask`

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/lists/OtakuCustomListViewModel.kt` (around lines 175-230)
- Test: `OtakuCustomListViewModelTest` (assertions unchanged, including the parsed-names test)

**Interfaces:** Consumes `AiTask` and `AiFeature.ListNames` / `ListDescription`.

- [ ] **Step 1:** Run `./gradlew :kmpuiviews:jvmTest --tests "*OtakuCustomListViewModelTest*"` to record the baseline.
- [ ] **Step 2: Implement.** Use `nameTask` and `descriptionTask` (`AiTask`). `nameSuggestions = nameTask.state` (drop the `parseNames` map; `ListNames.finish` does it now). `suggest(task, feature)` launches `task.start(feature, titles)`. Titles keep the format `"${it.title} (${it.source})"`; the 30-title cap moves into `prepare`.
- [ ] **Step 3: Run** the same test. Expected: PASS.
- [ ] **Step 4: Commit** `refactor(ai): custom list suggestions use AiTask`

---

### Task 6: Recaps + remove the old API

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/aitext/ChapterRecaps.kt`
- Modify: `.../domain/aitext/AiText.kt` (delete the six temporary methods)
- Delete: `.../domain/aitext/AiTextRunner.kt`, `kmpuiviews/src/jvmTest/.../domain/aitext/AiTextRunnerTest.kt`
- Test: `ChapterRecapsTest` (assertions unchanged)

**Interfaces:** Consumes `AiText.engineFor(AiFeature.Recap, …)` and `AiText.run(AiFeature.Recap, text)`.

- [ ] **Step 1: Implement.** `ChapterRecaps`: `available` uses `engineFor(AiFeature.Recap)`. The run uses `aiText.run(AiFeature.Recap, text)`, and the second `engineFor` passes `AiFeature.Recap.prepare(text).length`. Remove the now-redundant explicit `recapTail` call. Recaps keep their own per-chapter state map; they don't use `AiTask`. Delete the temporary methods and `AiTextRunner`.
- [ ] **Step 2: Run** `grep -rnE "AiTextRunner|\.tldr\(|\.recap\(|\.proofread\(|\.rewrite\(|listNames\(|listDescription\(|onDeviceAiOrder|AiFeature\.ListSuggest" --include='*.kt' . | grep -v /build/`. Expected: no output.
- [ ] **Step 3: Run** `./gradlew :kmpuiviews:jvmTest :kmpuiviews:compileAndroidMain :novelworld:shared:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug`. Expected: all PASS / BUILD SUCCESSFUL.
- [ ] **Step 4: Commit** `refactor(ai): recaps on generic run; drop per-feature AiText methods`
