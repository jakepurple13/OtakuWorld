# On-device AI Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add four AI features that run on device: a novel "Previously" recap, a TL;DR on the Details
screen, writing tools in Notes, and name/description suggestions for custom lists. Gemini Nano runs
first on Android, with Koog Gemma as the fallback. Desktop uses Koog Ollama. Every feature reuses
Daily Overview's generating animation.

**Architecture:**
- A new `AiText` facade in `kmpuiviews` commonMain resolves an on-device engine per feature, using the
  existing `EngineResolver` and engines. It exposes streamed `Flow<String>` operations.
- On Android, an `NanoTools` implementation routes recap, TL;DR, proofreading and rewriting to the
  dedicated ML Kit GenAI APIs when Nano is the chosen engine.
- The private pieces of `SummaryCard` become shared, public AI UI components that all four features
  use.

**Tech Stack:**
- Kotlin Multiplatform, Compose Multiplatform, Koin, kotlinx.coroutines
- ML Kit GenAI: Prompt `1.0.0-beta5`; Summarization, Proofreading and Rewriting `1.0.0-beta1`
- Koog `1.3.0`

**Spec:** `docs/superpowers/specs/2026-11-12-gemini-nano-features-design.md`

## Global Constraints

- **Engine order, on device only:**
  - Android: `[SummaryEngineId.Nano, SummaryEngineId.Gemma]`
  - Desktop (JVM): `[SummaryEngineId.Ollama]`
  - iOS: `[]`
  - Never use `ProviderEngine` or `Template`.
  - Ignore `DailyOverviewPrefs.engine` and the Daily Overview on/off switch.
- **No engine ready → the feature is hidden.** No template text fallback.
- **No generation without a user action.**
- **Never hide or replace the original description.** A note changes only on Accept.
- **AI gradient:** `0xFF1F8EF1`, `0xFF9B72F2`, `0xFFE1617A`.
- **Animation timings:**
  - border: 1.5dp stroke, 2400 ms linear loop, 400 ms fade
  - spark: 1600 ms per turn
  - shimmer: 1200 ms; three lines at widths 1f / 0.86f / 0.6f
  - shimmer → text crossfade: 300 ms
  - chunk fade: 350 ms
  - expand/collapse: 400 ms
- **Privacy strings:** reuse `do_privacy_on_device` and `do_privacy_sent`.
- **Strings:** all new strings go in `kmpuiviews/src/commonMain/composeResources/values/strings.xml`.
- **Lazy layouts:** every added lazy item or sticky header gets `Modifier.animateItem()`.
- **Threading:** all AI calls run off Main (`flowOn(Dispatchers.IO)`).
- **Reduced motion:** with animator scale 0, skip the border, spin and shimmer, and show text at once.
- **ViewModel tests:**
  - call `clearAndJoin()` from `kmpuiviews/src/jvmTest/.../testing/ViewModelStoreTesting.kt` before `resetMain`
  - no `Thread.sleep`, no raised timeouts
- **Builds:** use the `noFirebase` flavor locally. Device checks go through `cdm acquire` and the
  `.noFirebase.debug` package.

## Review Focus

1. **The engine fails or disappears mid-stream** (Nano backgrounded, Ollama stopped, Gemma file
   deleted) → the card shows a one-line error with Retry, with no crash and no stuck spinner.
   Pinned in Task 2 (`generation error becomes Error state`) and Task 6 (`recap error then retry`).
2. **Leaving the screen or changing chapter mid-generation** cancels the job. A stale result never
   lands on the new chapter. Pinned in Task 6 (`changing chapter cancels and ignores stale recap`).
3. **Blank or very long inputs:**
   - empty or whitespace note → tools disabled
   - note over the Nano limit → Koog, or disabled with a reason
   - blank description → no chip
   - Pinned in Task 4 and Task 5.
4. **Model output in odd formats:**
   - names as `1. Foo`, `- Foo`, `"Foo"`, duplicates or blank lines
   - TL;DR with more or fewer than 3 bullets
   - Pinned in Task 2 (`parseBullets`) and Task 7 (`parseNames`).
5. **Engine readiness changes between screens** (Nano finishes downloading). Controls appear the next
   time the screen opens, without restarting the app. Pinned in Task 2 (`availability is re-checked
   per call`).

---

## File Structure

New, under `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/`:
- `domain/aitext/AiText.kt`: `AiFeature`, `RewriteStyle`, `AiEngineInfo`, `AiTextState`, `AiText` interface
- `domain/aitext/DefaultAiText.kt`: resolver-backed implementation
- `domain/aitext/AiTextPrompts.kt`: prompts, truncation, `parseBullets`, `parseNames`
- `domain/aitext/NanoTools.kt`: interface for the Nano-specific APIs
- `domain/aitext/OnDeviceOrder.kt`: `expect val onDeviceAiOrder: List<SummaryEngineId>`, with
  android/jvm/ios actuals next to `PlatformAutomaticOrder.*.kt`
- `domain/aitext/WordDiff.kt`: word-level diff for Fix grammar
- `domain/aitext/ChapterRecaps.kt`: per-chapter recap cache and job control (used by NovelWorld)
- `domain/aitext/AiTextRunner.kt`: collects a `Flow<String>` into `StateFlow<AiTextState>`
- `presentation/components/ai/AiComponents.kt`: `GeneratingBorder` modifier, `Spark`, `AiShimmer`,
  `StreamedText`, `AiCard`

New, Android: `kmpuiviews/src/androidMain/.../aitext/MlKitNanoTools.kt`.

Modified:
- the engines and `KoogSummaryEngine`
- `SummaryCard.kt`
- the AppModule files
- Details, Notes and Lists screens and their view models
- the NovelWorld reader files
- `DataStoreHandling.kt`
- `gradle/android.versions.toml`, `kmpuiviews/build.gradle.kts`

---

### Task 1: General `generate` on summary engines

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/dailyoverview/SummaryEngine.kt`
- Modify: `kmpuiviews/src/usesJvmMain/kotlin/com/programmersbox/kmpuiviews/dailyoverview/KoogSummaryEngine.kt`
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/dailyoverview/NanoEngine.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/dailyoverview/TemplateEngine.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/dailyoverview/KoogEnginesTest.kt`

**Interfaces:**
- Produces: `fun SummaryEngine.generate(system: String, user: String): Flow<String>`. It is a member
  of the interface and emits streamed deltas.
  - `summarize(facts)` stays.
  - In Koog and Nano, `summarize` delegates to `generate(SummaryPrompt.system(facts.unit), SummaryPrompt.user(facts))`.
  - `TemplateEngine.generate` throws `UnsupportedOperationException`.

- [ ] **Step 1: Write the failing test** `generate sends system and user text and streams deltas`.
  Reuse the existing fake-`LLMClient` approach in `KoogEnginesTest`.
  - Assert the client received a prompt whose system message is `"S"` and whose user message is `"U"`.
  - Assert the collected deltas equal the fake stream (`listOf("a", "b")`).
- [ ] **Step 2: Run:** `./gradlew :kmpuiviews:jvmTest --tests "*KoogEnginesTest*"`. Expected: FAIL
  (unresolved `generate`).
- [ ] **Step 3: Implement.** Move the body of `KoogSummaryEngine.summarize` into
  `generate(system, user)`, keeping the prompt id `"daily-overview"`, the `streams` handling and
  `flowOn(Dispatchers.IO)`. In `NanoEngine.generate`, send `system + "\n\n" + user`.
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest --tests "*dailyoverview*"`. Expected: PASS,
  including all existing Daily Overview tests unchanged.
- [ ] **Step 5: Commit** `refactor(dailyoverview): add generate(system, user) to summary engines`.

---

### Task 2: `AiText` facade, prompts, on-device order

**Files:**
- Create: `domain/aitext/AiText.kt`, `DefaultAiText.kt`, `AiTextPrompts.kt`, `NanoTools.kt`,
  `OnDeviceOrder.kt` (+ `OnDeviceOrder.android.kt`, `.jvm.kt`, `.ios.kt`), `AiTextRunner.kt`
- Modify: `kmpuiviews/src/commonMain/.../di/AppModule.kt` (bind `AiText`, resolving `NanoTools` with
  `getOrNull()`). Nothing binds `NanoTools` until Task 3; on JVM it is never bound.
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/aitext/DefaultAiTextTest.kt`,
  `AiTextPromptsTest.kt`, `AiTextRunnerTest.kt`

**Interfaces:**
- Consumes: `SummaryEngine.generate` (Task 1); `EngineResolver(engines, automaticOrder)` and
  `EngineResolver.resolve(SummaryEngineChoice.Automatic)`.
- Produces:
  ```kotlin
  enum class AiFeature { Recap, Tldr, Proofread, Rewrite, ListSuggest }
  enum class RewriteStyle { Rephrase, Shorten, Elaborate, Friendly, Professional, Emojify }
  data class AiEngineInfo(val label: String, val onDevice: Boolean, val destination: String?)
  sealed interface AiTextState {
      data object Idle : AiTextState
      data object Loading : AiTextState
      data class Streaming(val text: String, val engine: AiEngineInfo) : AiTextState
      data class Done(val text: String, val engine: AiEngineInfo) : AiTextState
      data class Error(val message: String) : AiTextState
  }
  interface AiText {
      /** Null when no on-device engine is ready for [feature]. Re-checks status on every call. */
      suspend fun engineFor(feature: AiFeature, inputLength: Int = 0): AiEngineInfo?
      fun recap(previousChapterText: String): Flow<String>
      fun tldr(description: String): Flow<String>          // emits bullet text, one bullet per line
      fun proofread(text: String): Flow<String>
      fun rewrite(text: String, style: RewriteStyle): Flow<String>
      fun listNames(titles: List<String>): Flow<String>    // one name per line
      fun listDescription(titles: List<String>): Flow<String>
  }
  interface NanoTools {
      suspend fun isReady(feature: AiFeature, inputLength: Int): Boolean
      fun run(feature: AiFeature, input: String, style: RewriteStyle? = null): Flow<String>
  }
  object AiTextPrompts {
      const val RECAP_TAIL_CHARS = 3_000
      const val TLDR_MIN_CHARS = 400
      const val NANO_TEXT_LIMIT = 4_000
      const val MAX_LIST_TITLES = 30
      fun recapTail(chapter: String): String     // last RECAP_TAIL_CHARS, starting at a word boundary
      fun system(feature: AiFeature, style: RewriteStyle? = null): String
      fun user(feature: AiFeature, input: String): String
      fun parseBullets(raw: String): List<String> // strips "-", "•", "*", "1." markers; at most 3
      fun parseNames(raw: String): List<String>   // strips markers and quotes, trims, drops blanks/dupes (case-insensitive); at most 3
  }
  /** Runs [block] in [scope], mapping deltas to Loading → Streaming → Done, exceptions to Error. Cancels the previous run. */
  class AiTextRunner(private val scope: CoroutineScope) {
      val state: StateFlow<AiTextState>
      fun run(engine: AiEngineInfo, block: () -> Flow<String>)
      fun reset()
  }
  ```
- `DefaultAiText(engines: List<SummaryEngine>, nanoTools: NanoTools?, order: List<SummaryEngineId> = onDeviceAiOrder)`:
  - Builds its own `EngineResolver(engines, order)`.
  - For each operation: if the resolved engine is Nano and `nanoTools?.isReady(feature, len)` is
    true, use `nanoTools.run`. Otherwise call `engine.generate(AiTextPrompts.system(..), AiTextPrompts.user(..))`.
  - A Nano engine whose NanoTools aren't ready for that feature uses `engine.generate` (Prompt API).
  - For Proofread and Rewrite:
    - if `inputLength > NANO_TEXT_LIMIT`, skip Nano
    - if no other engine is ready, `engineFor` returns null
  - The label comes from `engine.label()`.

- [ ] **Step 1: Write failing tests** in `DefaultAiTextTest`, using a `FakeEngine(id, status, onDevice, deltas)`:
  - `nano ready uses nano` → `engineFor(Tldr)` returns the Nano label.
  - `nano unavailable falls back to gemma`
  - `nothing ready returns null`
  - `provider and template are never chosen`: order `[Nano, Gemma]`, registered engines include a
    ready Provider and Template, the others aren't ready → `null`.
  - `availability is re-checked per call`: Nano status flips from NeedsSetup to Ready between calls,
    so the second call returns Nano.
  - `long proofread skips nano`: length 4_001, Nano and Gemma ready → Gemma.
  - `long proofread with only nano returns null`
  - `nano tools used when ready`: a fake NanoTools records the feature → `tldr("x")` collects the
    NanoTools output, and `generate` is not called.
- [ ] **Step 2: Write failing tests** in `AiTextPromptsTest`:
  - `recapTail keeps last 3000 chars at word boundary`: length ≤ 3000, doesn't start mid-word, ends
    with the input's ending.
  - `parseBullets strips markers and caps at 3`: `"- a\n• b\n3. c\n* d"` → `[a, b, c]`.
  - `parseNames strips markers quotes and dupes`: `"1. Cozy\n\"cozy\"\n- Slow Evenings\n\n"` →
    `[Cozy, Slow Evenings]`.
  - `system prompt per rewrite style differs`: all 6 styles give distinct, non-blank strings.
- [ ] **Step 3: Write failing tests** in `AiTextRunnerTest` (`StandardTestDispatcher`):
  - `deltas become streaming then done`: emissions `a`, `b` → states `Loading`, `Streaming("a")`,
    `Streaming("ab")`, `Done("ab")`.
  - `generation error becomes Error state`: the flow throws `IllegalStateException("x")` → the last
    state is `Error`.
  - `new run cancels previous`: a second `run` before the first finishes → the first run's deltas
    never appear.
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest --tests "*aitext*"`. Expected: FAIL (unresolved).
- [ ] **Step 5: Implement** the files above.
  - Prompts must say exactly what to return:
    - recap: "2–3 sentences, plain prose, no preamble"
    - TL;DR: "exactly 3 bullets, one per line"
    - proofread: "return only the corrected text"
    - rewrite: one instruction per style, "return only the rewritten text"
    - names: "3 names, one per line, max 4 words"
    - description: "1–2 sentences"
  - Bind in Koin with `single<AiText> { DefaultAiText(getAll(), getOrNull()) }`.
- [ ] **Step 6: Run** the same command. Expected: PASS.
- [ ] **Step 7: Commit** `feat(aitext): on-device AiText facade with Nano-first engine order`.

---

### Task 3: ML Kit Nano tools (Android)

**Files:**
- Modify: `gradle/android.versions.toml`. Add versions `mlkitGenaiSummarization = "1.0.0-beta1"`,
  `mlkitGenaiProofreading = "1.0.0-beta1"`, `mlkitGenaiRewriting = "1.0.0-beta1"` and libraries
  `mlkit-genai-summarization`, `mlkit-genai-proofreading`, `mlkit-genai-rewriting`
  (`com.google.mlkit:genai-*`).
- Modify: `kmpuiviews/build.gradle.kts:185` (androidMain deps, next to `mlkit.genai.prompt`)
- Create: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/aitext/MlKitNanoTools.kt`
- Modify: `kmpuiviews/src/androidMain/.../di/AppModule.android.kt`. Bind
  `single<NanoTools> { MlKitNanoTools(androidContext()) }`, replacing Task 2's placeholder.

**Interfaces:**
- Consumes: `NanoTools`, `AiFeature`, `RewriteStyle` (Task 2).
- Produces: `class MlKitNanoTools(context: Context) : NanoTools`.

Feature mapping:

| Feature | Client | Settings |
|---|---|---|
| Tldr | Summarization | `OutputType.THREE_BULLETS`, `InputType.ARTICLE` |
| Recap | Summarization | `OutputType.ONE_BULLET`; strip the bullet marker. If it can't produce 2–3 sentences, return `isReady = false` for Recap so Task 2 uses the Prompt API. |
| Proofread | Proofreading | `InputType.KEYBOARD`; take the first suggestion |
| Rewrite | Rewriting | `RewriteStyle` → `OutputType` REPHRASE / SHORTEN / ELABORATE / FRIENDLY / PROFESSIONAL / EMOJIFY; first suggestion |
| ListSuggest | — | always `isReady = false`, so the Prompt API is used through `NanoEngine.generate` |

Behavior:
- `isReady(feature, len)`:
  - false when `len > AiTextPrompts.NANO_TEXT_LIMIT`
  - otherwise the client's feature status is AVAILABLE
  - if the status is DOWNLOADABLE, start the download once, as `NanoEngine.status()` does, and
    return false
- Clients are created lazily and closed in `onTrimMemory`, or left to the process. Don't create them
  per call.
- Wrap callback/`ListenableFuture` APIs in `callbackFlow` / `suspendCancellableCoroutine`, and add
  `flowOn(Dispatchers.IO)`.
- Language: English for Summarization; English, or the device locale when supported, for
  Proofreading and Rewriting.
- **Check the beta1 API surface in the downloaded artifact before writing.** Names above come from
  the ML Kit GenAI docs.

- [ ] **Step 1: Add dependencies** and run `./gradlew :kmpuiviews:compileAndroidMain` (or the
  module's Android compile task). Expected: BUILD SUCCESSFUL.
- [ ] **Step 2: Implement `MlKitNanoTools`** and the Koin binding.
- [ ] **Step 3: Build** with `./gradlew :mangaworld:assembleNoFirebaseDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Commit** `feat(aitext): ML Kit summarization/proofreading/rewriting for Nano`.

(There's no unit test here because these are thin wrappers over Play Services. They are exercised by
Task 9's device check.)

---

### Task 4: Shared AI components + Details TL;DR

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/components/ai/AiComponents.kt`
- Modify: `kmpuiviews/src/commonMain/.../presentation/dailyoverview/components/SummaryCard.kt`.
  Rebuild it on the shared pieces with no visual change. Delete its private copies.
- Modify: `kmpuiviews/src/commonMain/.../presentation/details/DetailsViewModel.kt`,
  `DetailsPortrait.kt`, `DetailsLandscape.kt`
- Modify: `strings.xml`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/details/DetailsViewModelTest.kt`

**Interfaces:**
- Consumes: `AiText`, `AiTextRunner`, `AiTextState`, `AiTextPrompts.parseBullets`,
  `TLDR_MIN_CHARS` (Task 2); `TranslationHandler.translate` (existing).
- Produces, for Tasks 5–7:
  ```kotlin
  fun Modifier.generatingBorder(active: Boolean, shape: Shape): Modifier
  @Composable fun Spark(spinning: Boolean, modifier: Modifier = Modifier)
  @Composable fun AiShimmer(modifier: Modifier = Modifier)
  @Composable fun StreamedText(text: String, modifier: Modifier = Modifier, style: TextStyle = …)
  @Composable fun AiCard(
      label: String,
      state: AiTextState,
      modifier: Modifier = Modifier,
      onRetry: () -> Unit,
      headerAction: (@Composable () -> Unit)? = null,  // X or chevron
      footerPrefix: String? = null,                     // e.g. "Translated"; prepended to the engine label
      body: @Composable (String) -> Unit = { StreamedText(it) },
  )
  ```
  - `AiCard` footer: engine label + privacy line (strings `do_privacy_on_device` / `do_privacy_sent`).
  - Error state: the message plus a Retry `TextButton`.
- DetailsViewModel:
  ```kotlin
  val tldrAvailable: StateFlow<Boolean>   // description.length > TLDR_MIN_CHARS && aiText.engineFor(Tldr) != null
  val tldr: StateFlow<AiTextState>
  val tldrTranslating: StateFlow<Boolean>
  fun showTldr()
  fun hideTldr()
  ```
  - Cache: `Map<itemUrl, AiTextState.Done>`, held in a companion object or Koin single. In memory,
    for the session.
  - `showTldr`:
    1. sets `tldrTranslating = true`
    2. calls `translationHandler.translate(description)`
    3. sets `tldrTranslating = false`
    4. summarizes the result
  - If the translated text differs from the description, the Done footer adds "Translated".
  - If translation throws, summarize the original.

- [ ] **Step 1: Write failing tests** in `DetailsViewModelTest`, with fake `AiText` and fake translation:
  - `tldr chip hidden for short description`: 400 chars → `tldrAvailable == false`; 401 → true.
  - `tldr chip hidden when no engine`
  - `tldr chip hidden for blank description`
  - `showTldr streams bullets to Done`
  - `showTldr uses cache on second call`: the fake `AiText.tldr` is invoked once.
  - `translated description marks footer`: the fake translation returns different text →
    `Done.text` comes from the translated input and a translated flag is set.
  - `hideTldr returns to Idle and does not cancel cache`
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*DetailsViewModelTest*"`. Expected:
  FAIL.
- [ ] **Step 3: Implement** `AiComponents.kt`, moving the code from `SummaryCard.kt`. Then the VM,
  then the UI.
  - **UI:** add a new `item(key = "tldr") { … Modifier.animateItem() }` between the description and
    the chapter sticky header. It holds `AnimatedContent(chip ↔ AiCard)`:
    - chip: `AssistChip` with `Spark(false)` leading and label "TL;DR"
    - chip exit: `fadeOut + shrinkVertically`
    - card enter: `expandVertically(tween(400)) + fadeIn`
    - card body: a bullet list from `parseBullets`, streamed with `StreamedText`
    - card label: "Translating" while `tldrTranslating`, else "TL;DR", crossfading over 200 ms
    - `headerAction`: X → `hideTldr()`
  - Add strings `ai_tldr`, `ai_translating`, `ai_translated`, `ai_retry`, `ai_error`.
- [ ] **Step 4: Run** the Details tests and `./gradlew :kmpuiviews:jvmTest --tests "*dailyoverview*"`.
  Expected: PASS.
- [ ] **Step 5: Showcase.** Add `@ShowcaseComponent` samples for `AiCard` in Loading, Streaming, Done
  and Error. Run `./gradlew :showcase:run` and check visually that SummaryCard looks unchanged.
- [ ] **Step 6: Commit** `feat(details): on-device TL;DR card below description`.

---

### Task 5: Notes writing tools

**Files:**
- Create: `kmpuiviews/src/commonMain/.../domain/aitext/WordDiff.kt`
- Create: `kmpuiviews/src/commonMain/.../presentation/notes/NoteAiAssistant.kt`. It is shared by both
  note view models so the logic isn't duplicated.
- Modify: `presentation/notes/NoteBottomSheet.kt`, `NoteDetailScreen.kt`, `DetailsNotesViewModel.kt`,
  `NoteDetailViewModel.kt`; `strings.xml`
- Test: `kmpuiviews/src/jvmTest/.../domain/aitext/WordDiffTest.kt`,
  `kmpuiviews/src/jvmTest/.../presentation/notes/NoteAiAssistantTest.kt`

**Interfaces:**
- Consumes: `AiText.proofread`, `AiText.rewrite`, `engineFor(Proofread|Rewrite, len)`, `AiTextRunner`,
  `generatingBorder`, `Spark` (Tasks 2 and 4).
- Produces:
  ```kotlin
  sealed interface DiffSpan { val text: String
      data class Same(override val text: String) : DiffSpan
      data class Inserted(override val text: String) : DiffSpan
      data class Deleted(override val text: String) : DiffSpan }
  object WordDiff { fun diff(old: String, new: String): List<DiffSpan> } // LCS over whitespace-split tokens, whitespace kept with the following token
  class NoteAiAssistant(private val aiText: AiText, scope: CoroutineScope) {
      val toolsEnabled: StateFlow<Boolean>          // engine available for current text length and text not blank
      val disabledReason: StateFlow<String?>        // "Note is too long for on-device tools" when length rules it out
      val preview: StateFlow<NotePreview?>          // null when not previewing
      fun onTextChanged(text: String)
      fun fixGrammar(text: String)
      fun rewrite(text: String, style: RewriteStyle)
      fun accept(): String?                         // returns accepted text, clears preview
      fun discard()
  }
  data class NotePreview(val original: String, val state: AiTextState, val diff: List<DiffSpan>?, val label: String)
  ```
  Both note view models expose `val ai: NoteAiAssistant`.

- [ ] **Step 1: Write failing tests** in `WordDiffTest`:
  - `identical text is all Same`
  - `fixed typos produce deleted then inserted pairs`: `"Stoped at ch 45"` → `"Stopped at ch 45"` →
    `[Deleted("Stoped"), Inserted("Stopped"), Same(" at ch 45")]`. Whitespace may sit inside spans,
    but joining all Same+Inserted equals new, and joining all Same+Deleted equals old.
  - `round trip holds for random sentences`: property check over 20 fixed pairs.
- [ ] **Step 2: Write failing tests** in `NoteAiAssistantTest`:
  - `blank note disables tools`
  - `long note with only nano disables with reason`
  - `fixGrammar preview has diff and accept returns corrected text`
  - `rewrite preview has no diff`
  - `discard returns null preview and accept after discard returns null`
  - `fixGrammar error shows Error state and leaves text unchanged`
- [ ] **Step 3: Run** `./gradlew :kmpuiviews:jvmTest --tests "*WordDiffTest*" --tests "*NoteAiAssistantTest*"`.
  Expected: FAIL.
- [ ] **Step 4: Implement** `WordDiff`, `NoteAiAssistant` and the view model wiring.
- [ ] **Step 5: Implement the UI** in `NoteBottomSheet` and `NoteDetailScreen`.
  - **Top bar:** a `Spark` `IconButton` left of Delete. It opens a `DropdownMenu`:
    - Fix grammar
    - a "Rewrite" label, then Rephrase / Shorten / Elaborate / Friendlier / More formal / Add emoji
    - when `!toolsEnabled`, the items are disabled and `disabledReason` shows as a disabled menu item
  - **While Loading/Streaming:**
    - `Modifier.generatingBorder(true, OutlinedTextFieldDefaults.shape)` on the field
    - the text field is `readOnly`, and its text alpha animates to 0.35f
    - the top-bar spark spins
  - **Preview:** read-only `AnnotatedString`:
    - Inserted: background `tertiaryContainer`-derived green, or a scheme color the implementer picks
      with light/dark contrast
    - Deleted: `errorContainer` background + `LineThrough`
  - **Review bar:** `AnimatedVisibility(slideInVertically { it / 3 } + fadeIn(tween(320)))` with
    Discard (`TextButton`) and Accept (`Button`).
  - **Accept animation:** Deleted span alpha → 0 over 250 ms, then removal. Insert backgrounds fade
    over 400 ms. Then set the field to the accepted text. Save-on-dismiss saves as today.
  - Strings: `ai_fix_grammar`, `ai_rewrite`, `ai_style_rephrase`, `ai_style_shorten`,
    `ai_style_elaborate`, `ai_style_friendly`, `ai_style_professional`, `ai_style_emojify`,
    `ai_accept`, `ai_discard`, `ai_note_too_long`, `ai_grammar_fixes`.
- [ ] **Step 6: Run** `./gradlew :kmpuiviews:jvmTest --tests "*notes*" --tests "*aitext*"`.
  Expected: PASS, including the existing note VM tests.
- [ ] **Step 7: Commit** `feat(notes): on-device fix grammar and rewrite with diff preview`.

---

### Task 6: Novel chapter recap

**Files:**
- Create: `kmpuiviews/src/commonMain/.../domain/aitext/ChapterRecaps.kt`
- Modify: `datastore/src/commonMain/kotlin/com/programmersbox/datastore/DataStoreHandling.kt`. Add
  `val novelChapterRecap = DataStoreHandler(key = booleanPreferencesKey("novelChapterRecap"), defaultValue = true)`.
- Modify: `novelworld/shared/src/commonMain/kotlin/com/programmersbox/novel/shared/reader/ReadViewModel.kt`,
  `ReaderCompose.kt`, `AppBars.kt` (wire `onSettingsClick`), `NovelModule.kt` (inject `AiText` and
  `DataStoreHandling` into `ReadViewModel`)
- Modify: `strings.xml`
- Test: `kmpuiviews/src/jvmTest/.../domain/aitext/ChapterRecapsTest.kt`

**Interfaces:**
- Consumes: `AiText.recap`, `engineFor(Recap)`, `AiTextPrompts.recapTail`, `AiCard`, `Spark`,
  `AiShimmer` (Tasks 2 and 4).
- Produces:
  ```kotlin
  class ChapterRecaps(private val aiText: AiText, private val scope: CoroutineScope) {
      val available: StateFlow<Boolean>
      fun stateFor(chapterUrl: String): StateFlow<AiTextState>   // Idle until requested; Done is cached
      fun request(chapterUrl: String, loadPreviousText: suspend () -> String?) // no-op if Loading/Streaming/Done; null text → Error
      fun onChapterChanged(newChapterUrl: String)                 // cancels in-flight jobs for other chapters
  }
  ```
  - `ReadViewModel` keeps one `ChapterRecaps`. The previous chapter is `list.getOrNull(currentChapter + 1)`.
    The list is newest-first: Next does `--currentChapter` in `AppBars.kt:275`.
  - Load its text with `getChapterInfo()` → first `KmpStorage.link`, the same mapping as
    `loadPages`. For downloaded novels, use the file path list.
  - Pass `AiTextPrompts.recapTail(text)`.

- [ ] **Step 1: Write failing tests** in `ChapterRecapsTest`:
  - `starts idle and does not generate`: a new chapter → `Idle`, and fake `recap` is not called.
  - `request generates once and caches`: request, Done, request again → recap called once.
  - `collapsing does not cancel`: there is no cancel API; a second request during Streaming is a no-op.
  - `changing chapter cancels and ignores stale recap`: request ch13, `onChapterChanged(ch14)` before
    Done → ch13 isn't Done, and ch14 is `Idle`.
  - `recap error then retry`: the first flow throws → `Error`; request again → `Done`.
  - `no previous chapter gives Error`: `loadPreviousText` returns null → `Error`.
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*ChapterRecapsTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement** `ChapterRecaps`, the datastore key, and the `ReadViewModel` wiring.
  - `val recapEnabled = dataStoreHandling.novelChapterRecap.asFlow()`
  - `fun setRecapEnabled(Boolean)`
  - Call `onChapterChanged` from `addChapterToWatched`.
- [ ] **Step 4: Implement the UI** in `ReaderCompose.kt`.
  - **Recap bar:** at the top of the scrolling `Column`, before the chapter `Text`. Shown when
    `recapEnabled && available && previous chapter exists`.
  - **Collapsed:** a card with `Spark(spinning = state is Loading|Streaming)`, a label
    `"PREVIOUSLY, IN CH. ${prevNumberOrName}"`, and a chevron. The chevron is `rotate(animateFloat 0→180)`.
  - **Tap:** toggles a local `expanded` (`rememberSaveable(chapterUrl)`). The first expand calls `request`.
  - **Body:** `AnimatedVisibility(expanded, expandVertically(tween(400)) + fadeIn, shrinkVertically + fadeOut)`
    containing `AiCard` with the recap state.
  - **Setting off:** the whole bar sits inside `AnimatedVisibility(recapEnabled, …shrinkVertically + fadeOut)`.
  - **Chapter change:** wrap the page content in `AnimatedContent(targetState = currentChapter)` with
    `slideInHorizontally { it / 8 } + fadeIn(tween(380)) togetherWith slideOutHorizontally { -it / 8 } + fadeOut(tween(220))`.
    Reverse the direction for Previous.
  - **Settings:** `onSettingsClick` opens a `ModalBottomSheet` with one `SwitchSetting`-style row,
    "Chapter recap" / "Show a "Previously" bar above each chapter", bound to `setRecapEnabled`.
    The row is shown only when `available`.
  - Strings: `ai_previously_in`, `ai_chapter_recap`, `ai_chapter_recap_summary`, `reader_settings`.
- [ ] **Step 5: Build** with `./gradlew :novelworld:assembleNoFirebaseDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 6: Run** `./gradlew :kmpuiviews:jvmTest --tests "*aitext*"`. Expected: PASS.
- [ ] **Step 7: Commit** `feat(novel): collapsed chapter recap with on-device AI and reader setting`.

---

### Task 7: Custom list suggestions

**Files:**
- Modify: `kmpuiviews/src/commonMain/.../presentation/settings/lists/OtakuCustomListViewModel.kt`,
  `OtakuListDetailParts.kt` (`ListHeroHeader` at :106, `EditListSheet` at :529), and the screen that
  hosts them
- Modify: `strings.xml`
- Test: `kmpuiviews/src/jvmTest/.../presentation/settings/lists/` (the existing list VM test file;
  add cases)

**Interfaces:**
- Consumes: `AiText.listNames`, `AiText.listDescription`, `engineFor(ListSuggest)`,
  `AiTextPrompts.parseNames`, `MAX_LIST_TITLES`, `generatingBorder`, `Spark` (Tasks 2 and 4).
- Produces, on `OtakuCustomListViewModel`:
  ```kotlin
  val suggestAvailable: StateFlow<Boolean>          // engine ready && list has >= 3 titles
  val nameSuggestions: StateFlow<AiTextState>       // Done.text is newline-joined parseNames output
  val descriptionDraft: StateFlow<AiTextState>
  fun suggestNames()
  fun draftDescription()
  fun clearDrafts()
  ```
  - Inputs: the first `MAX_LIST_TITLES` items as `"$title ($sourceName)"`.
  - `updateDetails(name, description)` (existing, :147) is unchanged. Saving still goes through it.

- [ ] **Step 1: Write failing tests:**
  - `suggest hidden with fewer than 3 titles`
  - `suggest hidden when no engine`
  - `suggestNames parses to 3 unique names`: the fake returns `"1. A\n2. a\n3. B\n4. C"` → `[A, B, C]`.
  - `draftDescription streams to Done and does not save`: `updateDetails` isn't called, and the DAO
    is unchanged.
  - `only first 30 titles are sent`
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*lists*"`. Expected: FAIL.
- [ ] **Step 3: Implement** the view model.
- [ ] **Step 4: Implement the UI.**
  - **`ListHeroHeader`:** when `description.isBlank() && suggestAvailable`, an `AssistChip` with
    `Spark` and "Write a description" sits under the name.
    - The tap opens `EditListSheet`, with a new parameter `autoDraftDescription = true`.
    - The sheet calls `draftDescription()` once the sheet state is fully expanded
      (`LaunchedEffect(sheetState.currentValue)`).
  - **`EditListSheet` description field:**
    - `Modifier.generatingBorder(state is Loading|Streaming, shape)`
    - the placeholder crossfades out
    - while streaming, the field value follows the streamed text; after Done it is a normal editable
      value
    - `trailingIcon` = a `Spark` `IconButton` that calls `draftDescription()`
  - **Name field:** `trailingIcon` = a `Spark` `IconButton` that calls `suggestNames()`. Below the field:
    - `AnimatedVisibility(expandVertically)` with a `FlowRow` of `SuggestionChip`s
    - each chip uses `animateEnterExit(scaleIn(spring(dampingRatio = 0.6f)) + fadeIn)`, with a
      70 ms × index delay
    - tapping a chip sets the name and shows it as selected
  - **Cancel/dismiss** calls `clearDrafts()`. **Save** keeps the existing `onSave(name, description)`.
  - **After Save:** the header description appears with `animateContentSize`, and the chip leaves
    with `AnimatedVisibility(fadeOut + shrinkVertically)`.
  - Strings: `ai_write_description`, `ai_suggest_names`.
- [ ] **Step 5: Run** the same tests. Expected: PASS.
- [ ] **Step 6: Commit** `feat(lists): on-device name suggestions and description drafts`.

---

### Task 8: Reduced motion + desktop wiring check

**Files:**
- Modify: `presentation/components/ai/AiComponents.kt`
- Modify: `kmpuiviews/src/jvmMain/.../di/AppModule.jvm.kt`. Confirm `OllamaEngine` is bound as a
  `SummaryEngine` (it is, at :86) and that `NanoTools` isn't bound on JVM.

**Interfaces:**
- Consumes: Task 4 components.
- Produces: no new API.

- [ ] **Step 1: Implement reduced motion.**
  - `GeneratingBorder`, `Spark` and `AiShimmer` read a `LocalReducedMotion`-style flag:
    - Android: `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`, through an expect/actual
      `rememberReducedMotion()`
    - JVM: `false`
  - When it is on: draw the border statically, don't spin, show a static shimmer, and have
    `StreamedText` skip the alpha fade.
- [ ] **Step 2: Build** with `./gradlew :mangaworld:desktop:run` and open Details on a long
  description with Ollama running. Expected: the TL;DR chip appears, and the footer reads
  "Ollama … · On device · nothing sent". With Ollama stopped, there is no chip. Also check Notes and
  Lists on desktop, and the NovelWorld recap in the NovelWorld desktop app (it has a JVM target).
- [ ] **Step 3: Commit** `feat(aitext): reduced-motion fallbacks for AI components`.

---

### Task 9: Device verification

**Files:** none (verification only).

- [ ] **Step 1:** Run `cdm acquire --android --category phone`, then
  `./gradlew :novelworld:installNoFirebaseDebug :mangaworld:installNoFirebaseDebug`.
- [ ] **Step 2: On a Nano-capable phone, check:**
  - The recap bar starts collapsed, and nothing generates until the tap. The tap expands it with the
    gradient border, shimmer and stream. Collapse and expand don't regenerate. The reader settings
    switch hides it.
  - Details TL;DR appears below a long description, and the description stays visible.
  - Notes: Fix grammar shows the diff, and Accept collapses the deletions.
  - Lists: the header chip opens the sheet, and the description writes in.
  - Use your own test items only (see memory: device tests own data only).
- [ ] **Step 3: On a phone without Nano**, with a Gemma model downloaded in AI Helper, repeat
  Step 2. The footer reads Gemma.
- [ ] **Step 4:** Without Nano and without Gemma, confirm no AI controls appear anywhere.
- [ ] **Step 5:** Record motion with `adb shell settings put global animator_duration_scale 5` and
  `screenrecord`, then restore the scale to 1. Check the timings against the Global Constraints.
- [ ] **Step 6:** Run `cdm release`. Run the full `./gradlew :kmpuiviews:jvmTest`. Expected: PASS.
