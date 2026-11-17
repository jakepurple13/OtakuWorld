# AI setup centralization — Design

Date: 2026-11-17
Context: PR 1 of the "more Gemini Nano" series. Mockups for the later features:
https://claude.ai/artifact/Vh8wQH9PqvCUFZaxNE8MPv

## Intent

Make it cheap to add an on-device AI feature. Today a new feature touches about eight places:
- the `AiFeature` enum
- two `when` blocks in `AiTextPrompts`
- a new method on `AiText`, plus its implementation in `DefaultAiText`
- two `when` blocks in `MlKitNanoTools`
- five separate fake `AiText`s in tests
- the engine-check and runner code in each ViewModel

After this change, a new feature is one new entry in `AiFeature.kt`, and callers use a shared
`AiTask`. Six features follow in their own PRs (update summary, describe-to-search, "Why?" chip,
mood tags, translated titles, error explainer). Each of them should only need a new `AiFeature`
entry plus its UI.

## Goals

1. **One file holds all features.** `AiFeature` becomes a sealed class. Each entry holds its system
   prompt, how input is trimmed (`prepare`), the user prompt, an optional ML Kit task (`nanoTask`),
   an optional Nano length cap, and an optional output cleanup (`finish`).
2. **`AiText` stops growing.** It has two abstract members, `engineFor` and `run(feature, input)`.
   `availability(...)` is a default method that re-checks every 2 s until an engine is ready. That
   loop is the one Details uses today, now shared by every caller.
3. **`AiTask` replaces `AiTextRunner`.** It resolves the engine, runs the feature, exposes
   `AiTextState`, and applies `finish` on Done. Every ViewModel uses it.
4. **`NanoTools` is keyed by `NanoTask`** (`Summarize`, `Proofread`, `Rewrite(style)`), not by
   feature. `MlKitNanoTools` no longer lists features that it doesn't support.
5. **One engine order.** `onDeviceAiOrder` is deleted. `DefaultAiText` uses `platformAutomaticOrder`
   and always removes `Provider` and `Template` from it, so AI text stays on-device only.
6. **One shared test fake.** `testing/FakeAiText` replaces `NoopAiText` and the four private fakes.

## Non-goals

- Any user-visible change. Screens, text, motion, and the engine picked for each feature all stay
  the same. Every existing test keeps its assertions. Only the fakes and call shapes change.
- A cloud fallback (decided "not now").
- Changing Daily overview's `SummaryEngine` / `EngineResolver` API.

## Decisions

- `nanoMaxChars` is `NANO_TEXT_LIMIT` for `Proofread` and `Rewrite` and `null` for every other
  feature. This keeps today's rule: only proofread and rewrite skip Nano for long input. The other
  features trim their input in `prepare`.
- `ListSuggest` splits into `ListNames` (its `finish` = `parseNames` joined with `\n`) and
  `ListDescription`. Both have Nano length cap `null` and no `nanoTask`, the same as today.
- Prompt strings move into `AiFeature.kt`. `AiTextPrompts` keeps only the text helpers and limits
  (`recapTail`, `tldrHead`, `parseBullets`, `parseNames`, the constants).
- `AiTask.start` is `suspend`. It resolves the engine in the caller's coroutine, checks
  `ensureActive()`, then launches the run in the task's scope. When no engine is ready it returns
  `false` and leaves the state as it was. Callers keep their current "no engine" behavior.
- The single generation `Mutex` in `DefaultAiText` stays.
