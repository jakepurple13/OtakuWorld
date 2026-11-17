# AI settings page — Design

Date: 2026-11-17
Mockup (approved): https://claude.ai/artifact/6mubAvLUaaYHovgNTexcjb
Stacked on: error explainer (PR #2715)

## Intent

One page, Settings → Integrations → **AI**, that controls every AI feature:
- a master switch
- which on-device engine to prefer
- one switch per feature

Daily overview's AI summary is also controlled from here.

## Decisions (from the user)

- "Prefer" exists: Automatic, Nano, Gemma on Android. Desktop only has Ollama, so the choice is hidden there.
- The master switch also covers Daily overview. Off means Daily overview writes its "Numbers only" sentence.
- Every switch defaults to on, including features added later.
- Mood tags get their section in the mood tags PR, not here.

## Data

New `DataStoreHandling` keys:
- `aiEnabled`: Boolean, default true.
- `aiEnginePreference`: String, default `"Automatic"`.
- `aiDisabledFeatures`: Set<String>, default empty. Storing the *off* set means a new feature starts on.

`enum class AiToggle { Tldr, ChapterRecap, WritingTools, ListSuggestions, ErrorExplanations, DailyOverview }`.
Later PRs add entries. `ChapterRecap` reuses the existing `novelChapterRecap` key, so the reader's
switch and this page's switch are the same setting.

`AiFeature` gains `open val toggle: AiToggle?`. A null toggle is never gated by a feature switch,
only by the master switch. "Try it" uses that.

## Behavior

- `DefaultAiText.engineFor`/`run` treat a feature as having **no engine** when the master switch is
  off or its toggle is off. Every existing "hide when no engine" path then hides the feature. No
  screen changes are needed.
- The engine order comes from the preference. Automatic uses `platformAutomaticOrder`, Nano uses
  [Nano], and Gemma uses [Gemma]. A preferred engine that isn't ready means no engine; it does not
  fall back.
- Daily overview's `DailyOverviewSettings.prefs.engine` becomes `Template` when the master switch
  or the DailyOverview toggle is off. When the stored choice is Automatic and the preference is Nano
  or Gemma, it becomes that engine. Its own engine screen stays and is reached from the AI page.
  Choosing the cloud provider still happens there, and only for Daily overview.
- The Settings page re-checks engine statuses when you come back to it. "Try it" streams a sample
  TL;DR through `AiTask` with the current preference.

## Page

- **Use AI features** (master switch).
- **Engine card:** "Right now: <engine> · on device", one row per on-device engine with a status tag
  (Ready / Not on this phone / Not downloaded + "Set up" → AI Helper), Prefer (Android only), and Try it.
- **Features**, grouped:
  - Details: TL;DR summary
  - Reading & notes: Chapter recap, Writing tools in notes
  - Library: List suggestions
  - Updates: Error explanations
  - Daily overview: AI summary (switch), Summary engine (row → existing engine screen)
- When the master switch is off, everything below it dims and its switches are disabled.
- The Integrations row reads "AI" with "On device · N of M features on", or "Off".

## Non-goals

- "Clear saved AI text". Every AI cache is in memory only, so there is nothing saved to clear.
- Mood tag progress (mood tags PR).
- A smaller "Lite" model (revisit at the end of the series).
