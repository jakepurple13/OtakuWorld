# On-device AI features (Gemini Nano first, Koog fallback) — Design

Date: 2026-11-12
Mockups (approved): https://claude.ai/artifact/9LSoqHuvpTUXvJ6zpLK7wk (v3)

## Intent

Gemini Nano is already used in OtakuWorld, but only for Daily Overview. Nano is good at short,
single-turn text jobs on text the app already has: summarizing, proofreading and rewriting. This
work adds four features of that kind. They are small, they run on device by default, and when no
engine is available the screens look exactly like they do today.

The user cares a lot about motion. Every AI surface reuses the Daily Overview "generating" animation
so the app has one consistent visual language for AI.

## Goals

1. **Novel recap**: a collapsible "Previously, in ch. N" bar at the top of each NovelWorld chapter.
2. **Details TL;DR**: a three-bullet summary of long descriptions, shown below the full description.
3. **Notes writing tools**: Fix grammar plus six rewrite styles in the note editor.
4. **Custom list suggestions**: suggested names and a generated description for a custom list.

## Non-goals

- **Update-notification digest.** It was dropped. Nano only runs while the app is in the foreground,
  and the update check runs in WorkManager.
- Template (non-AI) text fallbacks for these features.
- Any Koog agent or tool-calling work. The Koog path here is a single prompt in and streamed text out.
- iOS. iOS has no engines (`platformAutomaticOrder` is empty there), so the features stay hidden.

## Anti-goals (a failure even if it works)

- **Hiding or replacing original content.** The full description and the note text stay the user's.
  AI output is shown next to them or as a preview until the user accepts it.
- **Generating without a user action.** The recap generates only when the bar is expanded. TL;DR
  generates only when the chip is tapped. List text generates only from its chip or button.
- **Visible AI controls when no engine is ready.** They must not show and then fail.
- **A different animation per feature.** All four must look and feel like Daily Overview's `SummaryCard`.

## Engine order (decided)

The order is "Gemini Nano first if available, otherwise Koog, just like Daily Overview".

- **Android:** Nano (ML Kit GenAI), then Koog.
- **Desktop:** Koog.

Use the existing `EngineResolver` and engines (`NanoEngine`, `GemmaEngine`, `OllamaEngine`) from `kmpuiviews/.../domain/dailyoverview/` and `.../dailyoverview/`. Do not create a
second resolver.

- If the resolver returns nothing, or returns `Template`, the feature is hidden.
- The engine is resolved once when the screen needs it, not on every keystroke. A change in engine
  readiness, such as Nano finishing its download, may show the controls the next time the screen opens.
- The resolver is not used directly: a new `AiText` facade sits on top of it (see below).

**On-device only (decided by the user):** these features never use a cloud provider, whatever Daily
Overview is set to.
- They ignore `DailyOverviewPrefs.engine` and the Daily Overview on/off switch.
- They always resolve in a fixed order: **Android** Nano → Gemma (LiteRT via Koog); **desktop**
  Ollama (via Koog).
- `ProviderEngine` and `Template` are never used.
- Pass this fixed order to the resolver: build an `EngineResolver` with the on-device order, or add a
  resolve call that takes an explicit order. Don't reuse Daily Overview's `Automatic` choice.
- Ollama is the user's own server. Treat it as on device for this purpose and show its existing label.

### Generalizing the engines

`SummaryEngine.summarize(facts: DailyFacts)` is specific to Daily Overview. Add a general method:

```kotlin
fun generate(system: String, user: String): Flow<String> // streamed deltas
```

- `KoogSummaryEngine` already builds a prompt from system + user text, so move that body into
  `generate`. `summarize` then calls `generate(SummaryPrompt.system(..), SummaryPrompt.user(..))`.
- `NanoEngine.generate` concatenates system and user text into one Prompt API request, as it does today.
- `OllamaEngine` gets the same split.
- Daily Overview's behavior must not change.

### Nano-specific APIs (Android)

When the resolved engine is Nano, use the dedicated ML Kit GenAI APIs where one exists. Use the
Prompt API otherwise.

| Feature | Nano API | Koog fallback |
|---|---|---|
| Recap | ML Kit **Summarization** (`ONE_BULLET`/paragraph, see below) | `generate` with a recap prompt |
| TL;DR | ML Kit **Summarization**, `OutputType.THREE_BULLETS` | `generate` asking for exactly 3 bullets |
| Fix grammar | ML Kit **Proofreading** | `generate` with a proofread prompt |
| Rewrite | ML Kit **Rewriting**: REPHRASE, SHORTEN, ELABORATE, FRIENDLY, PROFESSIONAL, EMOJIFY | `generate`, one prompt per style |
| List name/description | ML Kit **Prompt API** (`genai-prompt`, already a dependency) | `generate` |

- These need new dependencies (`com.google.mlkit:genai-summarization`, `genai-proofreading`,
  `genai-rewriting`), added to `gradle/android.versions.toml` next to `mlkit-genai-prompt`.
- Each of these APIs has its own feature-status and download check. The status check that decides
  whether to show a control must use that feature's own client, not the Prompt API client.
- **Fallback if a dedicated API is unavailable on a device where Prompt API is ready (builder's call):**
  use the Prompt API with the same prompt the Koog path uses.
- Recap output should be 2–3 plain sentences, not bullets. If the Summarization API can't produce
  that, use the Prompt API for the recap.

### The `AiText` facade

Add a small domain service in `kmpuiviews` commonMain. Each method returns `Flow<String>` (streamed
deltas):

- `recap(previousChapterTail)`
- `tldr(description)`
- `proofread(text)`
- `rewrite(text, style)`
- `listNames(titles)`
- `listDescription(titles)`

It also exposes:

- `isAvailable(feature): Boolean`, to show or hide controls
- the engine label and privacy line for the card footer (`label()`, `onDevice`, `destination()`), the
  same way `SummaryState` is built in `DailyOverviewViewModel`

Placement:
- The Android implementation chooses Nano-specific APIs vs `generate` (androidMain).
- The JVM implementation always uses `generate` on the resolved Koog engine.
- Register it in Koin next to the `EngineResolver` binding in `kmpuiviews/.../di/AppModule.kt` and the
  platform modules.

Prompts live in one file, like `SummaryPrompt`. They must say plainly what to return and not to add
anything else.

## Shared UI: `AiSummaryCard` and `GeneratingBorder` (decided)

Extract the private pieces of
`kmpuiviews/.../presentation/dailyoverview/components/SummaryCard.kt` into a shared, public component
file, for example `presentation/components/ai/`:

- `GeneratingBorder`: the AI gradient (`0xFF1F8EF1`, `0xFF9B72F2`, `0xFFE1617A`), a 1.5dp stroke that
  flows on a 2400 ms linear infinite loop and fades in and out over 400 ms. Make it usable as a
  `Modifier` so text fields can wear it, not only cards.
- `Spark`: the four-point gradient star. It spins on a 1600 ms loop while generating and stops when done.
- `Shimmer`: three lines at 100%, 86% and 60% width, swept on a 1200 ms loop.
- `StreamedText`: each new chunk fades in over 350 ms, with `animateContentSize`.
- The 300 ms crossfade from shimmer to text (`AnimatedContent` keyed on loading).
- The footer privacy line uses the existing strings `do_privacy_on_device` ("On device · nothing sent")
  and `do_privacy_sent` ("Sent to %1$s"), together with the engine label.

Rules:
- Daily Overview's `SummaryCard` is rebuilt on top of these pieces with no visual change.
- New cards use the same `surfaceContainer` background and 24dp shape.
- Keep `@ShowcaseComponent` samples, and add samples for the new card states.

## Feature 1: Novel recap

**Where:** NovelWorld reader, `novelworld/shared/.../reader/ReaderCompose.kt` and `ReadViewModel.kt`.

**Behavior (decided):**
- On every chapter open, a collapsed bar sits above the chapter text: the spark (static), a
  "PREVIOUSLY, IN CH. N" label, and a chevron.
- It does not generate until the user taps it. A tap expands the card and starts generating.
- The result is cached per chapter URL in `ReadViewModel` for the reader session. Collapsing and
  re-expanding, or returning to that chapter, shows the cached text with no regeneration.
- If the bar is collapsed mid-generation, generation keeps going and the text is ready on reopen.
  Leaving the chapter cancels it.
- Input is the last ~3,000 characters of the previous chapter's text, kept within Nano's context.
  Fetch the previous chapter through the existing chapter-loading path.
  - There is no previous chapter (first chapter): no bar.
  - The previous chapter fails to load: the card shows a one-line error with Retry inside the card.
    No toast.
- No close button. The setting is the only way to turn the bar off.

**Setting (decided):** a "Chapter recap" switch, on by default, stored in datastore.
- The reader's settings icon (`AppBars.kt`, currently `onSettingsClick = { }`) opens a modal bottom
  sheet holding this switch.
- **Builder's call:** the sheet holds only this switch for now. The "Text size" and "Line spacing" rows
  in the mockup were set dressing, not part of this work.
- Turning the switch off removes the bar with shrinkVertically + fadeOut.
- The switch is shown only when an engine is available.

**Motion:**
- Chapter change: the outgoing page slides 40dp left and fades over 220 ms. The incoming page slides
  in from the right over 380 ms (`AnimatedContent` on chapter index).
- Expand: the chevron rotates 180° and the body expandVertically over 400 ms into the shimmer, then
  the standard generate → stream → done sequence.
- Collapse reverses it.

## Feature 2: Details TL;DR

**Where:** the Details `LazyColumn` in `kmpuiviews/.../presentation/details/DetailsPortrait.kt`
(and `DetailsLandscape.kt`). Add a new item between the description and the sticky chapter header.

**Behavior (decided):**
- The full description is never hidden or replaced. The summary appears below it.
- A "TL;DR" `AssistChip` with the spark shows when the description is longer than about 400
  characters (exact threshold is the builder's call) and an engine is available.
- Tap: the chip shrinks away and an `AiSummaryCard` labeled "TL;DR" expandVertically below the
  description, then streams three bullets one after another.
- The card has an X that collapses it and brings the chip back.
- Summaries are cached in memory per item URL, so reopening during the session is instant.
  **Builder's call:** in-memory only, not persisted.
- **Non-English descriptions are included (decided):**
  - First translate with the existing `TranslationHandler` (the same path long-press translate uses
    in `DetailsViewModel.translateDescription`), then summarize.
  - The card label shows "Translating" during that step and crossfades to "TL;DR" (200 ms).
  - **Builder's call — target language:** translate to the user's global translation target
    (`settings.translationTargetLanguage`) and write the summary in that language. "Non-English"
    means "not already in the target language". The ML Kit language-id check is the builder's choice.
  - **Builder's call — engine:** if Nano's Summarization API doesn't support the target language,
    use the Prompt API, then Koog.
  - **Builder's call — footer:** the footer says "Translated" without naming the source language, so
    the `TranslationHandler` interface doesn't change.
  - **Builder's call — failure:** if translation fails, summarize the original text with the Koog
    path, or hide the chip when only Nano is available.
- Long-press translate and tap-to-expand on the description itself are unchanged.

## Feature 3: Notes writing tools

**Where:** `kmpuiviews/.../presentation/notes/NoteBottomSheet.kt` and `NoteDetailScreen.kt`, plus
their view models.

**Behavior (decided):**
- A spark `IconButton` in the `TopAppBar` actions, to the left of Delete, opens a `DropdownMenu`:
  - Fix grammar
  - a "Rewrite" group: Rephrase, Shorten, Elaborate, Friendlier, More formal, Add emoji
  - It is disabled when the note is empty.
- While generating:
  - the text field's outline becomes `GeneratingBorder`
  - the note text dims to 35% alpha
  - the top-bar spark spins
  - the field is read-only
- Results:
  - Fix grammar shows an inline diff: insertions on a green background, deletions struck through on
    red. Compute the diff locally (word-level) between the original and the corrected text.
  - Rewrites stream the new text into the field as a preview.
- A review bar slides up (24dp + fade, 320 ms) with Discard and Accept.
- Nothing changes the saved note until Accept.
  - Accept: deletions fade and collapse their width, insertion highlights fade to clear, the bar slides
    away, and the accepted text becomes the field's value. Normal save-on-dismiss saves it.
  - Discard restores the original text.
- **Builder's call — long notes:** never truncate a note, because accepting the result would delete
  the cut-off text.
  - Over the selected engine's input limit (about 4,000 characters for Nano's Proofreading and
    Rewriting APIs; check the real limit at build time), use Koog if it is ready.
  - Otherwise the menu items are disabled, with a one-line reason in the menu ("Note is too long for
    on-device tools").
- Diff colors need light and dark tokens. **Builder's call:** derive them from the color scheme rather
  than hard-coding them.

## Feature 4: Custom list suggestions

**Where:** `kmpuiviews/.../presentation/settings/lists/OtakuListDetailParts.kt` (`ListHeroHeader`,
`EditListSheet`). `CustomListItem.description` already exists, so the database doesn't change.

**Behavior (decided):**
- In `ListHeroHeader`, when the description is blank, the list has at least 3 titles, and an engine
  is available, a "Write a description" `AssistChip` with the spark shows under the name.
- Tapping the chip opens `EditListSheet`. Once the sheet settles, the description field wears
  `GeneratingBorder`, its placeholder crossfades out, and the generated text streams in.
  - The text stays editable.
  - It is saved only with the sheet's Save button. Cancel or dismiss discards it.
- In `EditListSheet`:
  - The Name field's trailing icon is a spark button. It generates three name suggestions, shown as a
    `FlowRow` of `SuggestionChip`s that expand in and pop with a 70 ms stagger. Tapping a chip fills
    the name.
  - The Description field's trailing icon is a spark button that generates a description, like the
    header chip does.
- Inputs: up to 30 titles plus each title's source name. **Builder's call:** no genres, because
  `CustomListInfo` doesn't store them and fetching them would mean network calls per title. The
  output for names is one per line.
  **Builder's call:** parse it defensively and drop empty or duplicate lines.
- Nothing changes in the create-list dialog, because a new list is empty.
- After Save, the header description expands in (`animateContentSize`) and the chip fades out.

## Constraints

- Nano runs only while the app is in the foreground.
  - Leaving the screen cancels in-flight generation.
  - If the app is backgrounded mid-stream, the flow errors. Show the in-card error with Retry; don't crash.
- Context limits:
  - Nano: a few thousand tokens. Truncate inputs as specified above, keeping the end of a chapter and
    the start of a description.
  - The Koog path may use the same inputs.
- All AI calls run off Main. `KoogSummaryEngine` already uses `flowOn(Dispatchers.IO)`; the ML Kit
  calls need the same.
- Respect reduced motion: when animations are off (animator scale 0), skip the border flow, spin and
  shimmer, and show text immediately.
- Strings go in compose resources (`kmpuiviews/src/commonMain/composeResources/values/strings.xml`),
  as the Daily Overview strings do.
- Every lazy item and sticky header added uses `Modifier.animateItem()` (project convention).

## Testing

- Unit tests for:
  - the `AiText` facade's engine selection: Nano ready, Nano not ready + Gemma ready, none ready, and
    an engine that resolves to Template
  - prompt building and input truncation
  - name-list parsing
  - the word-level diff
  - recap caching (expand → collapse → expand doesn't regenerate)
- Daily Overview tests must still pass after the `generate` refactor.
- ViewModel tests follow the project teardown rule: `clearAndJoin()` before `resetMain`, with no
  sleeps or raised timeouts.
- Device check on Android: one phone with Nano and one without, to confirm the Koog fallback and the
  hidden state. Desktop check: Koog/Ollama path for TL;DR, Notes and Lists.
- The NovelWorld recap needs a desktop check only if NovelWorld desktop exists in this branch.

## Left to the builder

- File and class names.
- The TL;DR length threshold.
- Exact prompt wording.
- How the `generate` refactor is split across engines.
- The error copy.

