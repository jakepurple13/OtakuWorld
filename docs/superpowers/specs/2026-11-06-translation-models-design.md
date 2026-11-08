# Translation models: better sheet + desktop local translation

> **2026-11-06:** Bergamot removed from scope by user; desktop engines are Ollama and Custom (OpenAI-compatible server). Custom model folders removed with it.

Date: 2026-11-06
Mockup (approved): https://claude.ai/artifact/RmpP9QQ8FQg1spu9Um3oVe — "A, with B's select mode" for phone, and the desktop section.

## Intent

Translation today has two problems.

1. **Android:** Settings → Integrations → "View translation models" opens a bare `ListBottomScreen` titled
   "choose model to delete" (`kmpuiviews/.../settings/translationmodels/TranslationScreen.kt`). Tapping a row
   deletes it immediately. There is no size, no total, no way to download ahead, and no confirm. Translation
   always goes into English.
2. **Desktop:** translation is a stub. `TranslationItemHandler` in `kmpuiviews/src/jvmMain/.../Platform.jvm.kt`
   returns the input unchanged and `TranslationModelHandlerImpl` lists nothing.

The user wants one place to see what's downloaded, how much space each model and all models take, download
languages ahead of time, and delete any of them (one or many). On desktop they want real translation using
local models, with a choice of engine, including their own.

## Goals

- Phone sheet redesigned per the approved mockup (proposal A layout + proposal B's select mode).
- A **target language** the user picks once; it is used for every translation until changed. Default English.
- Real on-disk sizes per model and a total.
- Desktop: working local translation with two engines the user chooses between: **Ollama** and
  **Custom** (an OpenAI-compatible server).
- Desktop management UI is a full settings page (not a bottom sheet), per the mockup.

## Non-goals

- Engine choice on Android. Android stays ML Kit only. (User decision.)
- Translation in `noFirebase` Android builds. They keep the no-op `TranslateItems`; the sheet must still open
  and show a sensible empty state there. (User decision.)
- New places that translate (reader chapter text, dictionary). The only caller today is the Details description
  (`DetailsViewModel.translateDescription`). The dictionary's `StubTranslationService` stays a stub. The
  mockup's picker note mentions "chapter text and the dictionary"; drop that wording, say "descriptions".
- iOS. Keep the existing iOS stubs compiling.

## Anti-goals

- Deleting without a confirm, anywhere.
- Letting the user delete English on Android. ML Kit routes every pair through English; show it as Required
  with a lock instead of a trash icon, and exclude it from select/select-all.
- Downloading big models over metered connections without the user asking. ML Kit downloads stay Wi-Fi only,
  as today.

## Phone sheet (Android, ML Kit)

Still a `ModalBottomSheet` opened from Integrations. Open it with `rememberModalBottomSheetState(skipPartiallyExpanded = true)` — it is tall.

Top to bottom:

1. Title "Translation", subtitle "On-device with ML Kit · works offline".
2. **Translate into** card (primary container) showing the target language; tapping swaps the sheet content to
   a language picker (`AnimatedContent`, slide). Picker: back arrow, search, radio list of all ML Kit languages
   with "downloaded" or size per row. Choosing one sets the global target; if that model isn't downloaded, the
   download starts (Wi-Fi only) and the sheet returns to the main view showing its progress.
3. **Storage meter**: total size + "for N languages", a stacked bar with one segment per downloaded language
   (weight = size), and a legend. Built-in English is left out of the count, bar and legend, and its row
   shows "Built in" instead of a size.
4. **Downloaded** list. Header right side: "Select" (normal) or "Select all/Clear" + "Done" (select mode).
   - Row: language avatar (native-script glyph), name, chips (Target, Required), meta `size · last used`.
   - Normal mode: trailing trash → the row morphs into an inline error-container confirm
     ("Delete Japanese? Frees 31.4 MB. It downloads again the next time you translate Japanese." Cancel/Delete).
   - Select mode (Select button or long-press a non-English row): avatar becomes a checkbox, trash hides,
     selected rows tint, storage bar dims selected segments and the total shows "→ X after delete". A bulk bar
     slides up from the sheet bottom: "N selected · size" + Delete; Delete morphs the bar into a confirm
     listing the languages. Back exits select mode before closing the sheet.
   - Deleting the current target language resets the target to English.
5. **Download for offline**: search field + list of not-downloaded languages with size and a download button;
   while downloading, a progress bar and "x of y MB" with a cancel button. Finished downloads move up into the
   Downloaded list (`Modifier.animateItem()` on every lazy item).

Motion per the mockup's Compose notes (AnimatedContent, animateItem, AnimatedVisibility for the bulk bar,
animated bar segment weights, wavy progress).

### Data needed (Android)

- **Size**: ML Kit doesn't expose it. Measure the model's files on disk; if the location can't be found
  reliably, fall back to an estimate (~30 MB) and label it "about". Builder decides how after checking where
  ML Kit stores models.
- **Last used**: not tracked today. Builder's call whether to record it (e.g., timestamp per language in
  settings when a translation runs) or drop "last used" from the meta line.
- **Download progress**: ML Kit `downloadModelIfNeeded` has no progress callback. Show an indeterminate
  progress indicator instead of "x of y MB" on Android.
- **Translate to target**: `TranslateItems` must use the target language instead of hard-coded English,
  and skip translating when the detected language equals the target.

## Target language (shared)

- Stored in `NewSettingsHandling` (KMP DataStore proto) as a language code, default `"en"`, so both platforms
  read it.
- Applies to every translation call (`TranslationHandler.translate` / `translateDescription`).
- On desktop it is the "Translate into" dropdown at the top of the page. The list shown should be the
  languages the engines can produce (a fixed common list for Ollama/Custom).

## Desktop page

A new settings destination "Translation" (desktop only). On desktop, the Integrations "View translation
models" entry navigates to it instead of opening the sheet. **Ships in both `mangaworld:desktop` and
`animeworld:desktop` (user decision)**, and both desktop builds get tested. Layout per mockup:

1. **Top row**: "Translate into · used everywhere" dropdown; **Storage** box (engine-specific total).
2. **Engine** cards (radio): Ollama, Custom, each with a one-line description and tags.
3. **Engine panel** (swaps with the selection):
   - **Ollama** — server URL field (default `http://localhost:11434`), Check button, status line
     ("Connected · Ollama x.y · N models" / "Can't reach Ollama at this address. Is it running?"), radio list of
     installed models (name, size, "In use" chip) with trash + inline confirm warning that other apps lose it
     too, and a "Pull another model" field with progress. Storage = sum of installed models, labelled
     "managed by Ollama". Uses Ollama's HTTP API (tags, generate/chat, pull, delete).
   - **Custom** — OpenAI-compatible server: URL, model name, optional API key (stored like other secrets in
     the app), status line. Calls `/v1/chat/completions`.
4. **Try it**: input box (prefilled sample), shows detected source language, Translate button, output with
   loading shimmer and a timing line ("Ollama · ja→en · 840 ms").

### Desktop engine behaviour

- Source language detection runs for both engines (skip translation when source == target, or when the
  detector isn't confident). Builder picks a JVM detector (e.g. Lingua).
- Ollama/Custom server unreachable → original text + error banner naming the address.
- LLM prompts must return only the translation (no preamble); builder owns the prompt.
- All engines run off the main thread and can be cancelled when Details leaves.

## Constraints

- KMP layout: shared UI and the target-language setting in `kmpuiviews`/`datastore` `commonMain`; ML Kit stays in
  `UIViews`/`sharedutils` (Android); engines and the desktop page in `jvmMain`.
- Koin DI throughout; the existing `TranslationHandler` / `TranslationModelHandler` interfaces may change shape
  (they only have one caller each plus test fakes) — update `Fakes.kt` and existing tests.
- Use `noFirebase` for local Android builds; it has no translation, so on-device verification of the ML Kit
  path needs the `noCloudFirebase` or `full` flavor.
- Every lazy item gets `Modifier.animateItem()`.

## Decided by the user

- Phone: mockup proposal A + B's select mode.
- Desktop: full settings page with Ollama / Custom, as mocked (Bergamot dropped later, see top note).
- Target language: user-picked, global until changed.
- Engine choice: desktop only.
- `noFirebase` Android: stays without translation for now.
- Desktop page + engines ship in both MangaWorld and AnimeWorld desktop.

## Left to the builder

- Measuring ML Kit model size on disk; "last used" tracking.
- Exact persistence of desktop engine settings (`AppDirs`/desktop settings store).
- Language list for Ollama/Custom targets.
