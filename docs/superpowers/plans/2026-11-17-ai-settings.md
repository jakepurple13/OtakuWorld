# AI Settings Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Settings → Integrations → AI: a master switch, an engine preference, per-feature switches, and Daily overview gated by them.

**Architecture:** `AiSettings` wraps three new DataStore keys plus the existing `novelChapterRecap`. `DefaultAiText` consults it, so existing screens hide gated features through their current no-engine paths. `DailyOverviewSettings` maps its effective engine. `AiSettingsScreen`/`AiSettingsViewModel` render the page.

**Tech Stack:** Kotlin Multiplatform, DataStore Preferences, Compose Multiplatform, Koin (+ navigation3 DSL), kotlin.test.

**Spec:** `docs/superpowers/specs/2026-11-17-ai-settings-design.md`

## Global Constraints

- Defaults: `aiEnabled = true`, preference `Automatic`, no features disabled.
- AI text never uses Provider or Template, whatever the settings say.
- Strings go in `kmpuiviews/src/commonMain/composeResources/values/strings.xml`.
- Tests: `./gradlew :kmpuiviews:jvmTest`. Build: `./gradlew :mangaworld:assembleNoFirebaseDebug`.

## Review Focus

1. Turning a feature switch off hides it, even on a screen already open (the Details TL;DR chip). Pinned by `disabled feature has no engine` and the Details availability re-check (Task 1).
2. Preferring Nano when Nano isn't ready must not silently use Gemma. Pinned by `preferred engine not ready means none` (Task 1).
3. The master switch off makes Daily overview use Template even when its stored choice is Provider. Pinned by `master off forces numbers only` (Task 2).
4. The reader's Chapter recap switch and the AI page switch stay the same setting. Pinned by `chapter recap toggle uses the reader key` (Task 1).
5. "Try it" works even when the TL;DR switch is off. Pinned by `sample ignores feature toggles` (Task 3).

---

### Task 1: AiSettings and gating in DefaultAiText

**Files:** create `datastore` keys (`DataStoreHandling.kt`), `kmpuiviews/.../domain/aitext/AiSettings.kt`. Modify `AiFeature.kt` (add `toggle`, add `TrySample`), `DefaultAiText.kt`, the DI binding in `di/AppModule.kt`. Tests: `AiSettingsTest`, `DefaultAiTextTest`.

**Interfaces:**
- Produces:
```kotlin
enum class AiToggle { Tldr, ChapterRecap, WritingTools, ListSuggestions, ErrorExplanations, DailyOverview }
enum class AiEnginePreference { Automatic, Nano, Gemma }
interface AiSettings {
    val enabled: Flow<Boolean>
    val preference: Flow<AiEnginePreference>
    val disabled: Flow<Set<AiToggle>>
    suspend fun setEnabled(v: Boolean)
    suspend fun setPreference(p: AiEnginePreference)
    suspend fun setFeatureEnabled(t: AiToggle, on: Boolean)
}
class DataStoreAiSettings(dataStoreHandling: DataStoreHandling) : AiSettings   // ChapterRecap ↔ novelChapterRecap
class InMemoryAiSettings : AiSettings   // jvmTest/testing
fun AiEnginePreference.order(platform: List<SummaryEngineId>): List<SummaryEngineId>  // Automatic→platform, Nano→[Nano], Gemma→[Gemma]; always intersect platform
```
- `DefaultAiText(engines, nanoTools, settings: AiSettings = AlwaysOnAiSettings, order = platformAutomaticOrder, lock)`. It reads `settings` on every resolve.
- `AiFeature.toggle`: Recap→ChapterRecap, Tldr→Tldr, Proofread/Rewrite→WritingTools, ListNames/ListDescription→ListSuggestions, ExplainError→ErrorExplanations, `TrySample` (Tldr prompt, same prepare)→null.

- [ ] Tests: `disabled feature has no engine`, `master off means no engine even for sample`, `preferred engine not ready means none`, `preference gemma skips ready nano`, `preference outside platform is ignored` (Ollama-only order with Nano preference → none), `chapter recap toggle uses the reader key` (`DataStoreAiSettings` writes `novelChapterRecap`; skip if `DataStoreHandling` can't be built in jvmTest, and test the mapping function instead).
- [ ] RED, implement, GREEN (full `:kmpuiviews:jvmTest`, because existing tests construct `DefaultAiText`).
- [ ] Commit `feat(ai): AI settings with master switch, engine preference and feature switches`.

### Task 2: Daily overview follows AI settings

**Files:** `domain/dailyoverview/DailyOverviewSettings.kt` (constructor gains `aiSettings: AiSettings`), DI, its tests or a new `DailyOverviewSettingsTest`.

- [ ] Tests: `master off forces numbers only` (stored Provider → Template), `daily toggle off forces numbers only`, `automatic follows ai preference` (stored Automatic + preference Gemma → Gemma), `explicit choice kept` (stored Ollama + preference Nano → Ollama).
- [ ] RED, implement as a `combine` in `prefs`, GREEN (full jvmTest).
- [ ] Commit `feat(daily-overview): follow the AI page's master switch and engine preference`.

### Task 3: AiSettingsViewModel

**Files:** `presentation/settings/ai/AiSettingsViewModel.kt`, test `AiSettingsViewModelTest`, DI `viewModelOf(::AiSettingsViewModel)`.

**Interfaces:**
```kotlin
data class AiEngineStatus(val id: SummaryEngineId, val name: String, val status: EngineStatus)
data class AiSettingsUi(
    val enabled: Boolean, val preference: AiEnginePreference, val showPreference: Boolean,
    val current: String?,                 // engine label, or null when nothing is ready
    val engines: List<AiEngineStatus>,    // on-device ids of platformAutomaticOrder only
    val disabled: Set<AiToggle>,
)
class AiSettingsViewModel(settings: AiSettings, aiText: AiText, resolver: EngineResolver, order: List<SummaryEngineId> = platformAutomaticOrder) {
    val ui: StateFlow<AiSettingsUi?>; val sample: StateFlow<AiTextState>
    fun setEnabled(v: Boolean); fun setPreference(p: AiEnginePreference); fun setFeature(t: AiToggle, on: Boolean)
    fun refresh(); fun trySample()
}
fun AiSettingsUi.summary(): Pair<Int, Int>?  // (on, total) or null when off
```
- [ ] Tests: `toggles write settings`, `current follows preference`, `show preference only with more than one engine`, `sample ignores feature toggles`, `summary counts on features`.
- [ ] RED, implement, GREEN.
- [ ] Commit `feat(ai): AI settings view model`.

### Task 4: Screen, navigation and Integrations entry

**Files:** `presentation/settings/ai/AiSettingsScreen.kt`, `Screen.kt` (`Screen.Settings.Ai`), `Nav3Graph.kt` (`detailEntry<Screen.Settings.Ai>`), `IntegrationsScreen.kt` (AI row with the summary line), strings.

- [ ] Build the screen to the spec's layout with `SettingsScaffold` + `CategoryGroupListItem`/`segmentedListItem`, M3 `Switch`, a `SingleChoiceSegmentedButtonRow` for Prefer, `AiCard` for Try it, and an alpha dim on the body when the master switch is off. Re-check engines with `LifecycleResumeEffect { refresh() }`.
- [ ] Run `./gradlew :kmpuiviews:jvmTest :mangaworld:assembleNoFirebaseDebug` (expect PASS) and `./gradlew :mangaworld:desktop:compileKotlinJvm` (expect it to compile).
- [ ] Commit `feat(ai): AI settings page under Integrations`.
