# Update Summary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An AI summary card at the top of Notifications for the latest update check's notifications.

**Architecture:** `AiFeature.UpdateSummary` and `AiToggle.UpdateSummary` follow the established pattern. A new `UpdateSummaries` class, like `ChapterRecaps`, owns the batching, the availability, the `AiTask` and the cache. `NotificationScreenViewModel` exposes it, and `NotificationScreen` renders an `AiCard` item.

**Spec:** `docs/superpowers/specs/2026-11-17-update-summary-design.md`

## Global Constraints

- Use `aiText.availability(...)` and never a one-shot `engineFor`, so the card follows the AI page live.
- Strings go in `kmpuiviews/src/commonMain/composeResources/values/strings.xml`.
- Tests: `./gradlew :kmpuiviews:jvmTest`. Build: `./gradlew :mangaworld:assembleNoFirebaseDebug`.

## Review Focus

1. A single new notification shows no card. Pinned by `one update shows nothing`.
2. Leaving the screen and coming back doesn't regenerate the summary for the same check. Pinned by `same check reuses the cached summary`.
3. A new update check produces a new summary. Pinned by `new check summarizes again`.
4. Turning the switch off while the screen is open hides the card. Pinned by `hides when ai becomes unavailable`.
5. Notifications from before the check aren't included. Pinned by `only notifications from the latest check`.

---

### Task 1: Feature, toggle, AI page row

**Files:** `AiFeature.kt`, `AiSettings.kt` (`AiToggle.UpdateSummary`), `AiSettingsScreen.kt` (Updates group row), `strings.xml`, `AiFeatureTest`, `AiSettingsTest`.
- [ ] Tests: `update summary prepares at most 25 lines and wraps prompt`, and `features map to their toggles` (+UpdateSummary).
- [ ] RED, implement per the spec, GREEN, commit `feat(notifications): update summary AI feature and switch`.

### Task 2: UpdateSummaries

**Files:** create `presentation/notifications/UpdateSummaries.kt` and `UpdateSummaryCache` (Koin single in `AppModule`). Test `UpdateSummariesTest`.

**Interfaces:**
```kotlin
class UpdateSummaryCache { operator fun get(checkFinishedAt: Long): AiTextState.Done?; operator fun set(checkFinishedAt: Long, done: AiTextState.Done) }
class UpdateSummaries(aiText: AiText, store: UpdateCheckReportStore, cache: UpdateSummaryCache, items: Flow<List<NotificationItem>>, scope: CoroutineScope) {
    /** Null when hidden; otherwise Loading/Streaming/Done/Error. */
    val state: StateFlow<AiTextState?>
    fun retry()
}
```
- [ ] Tests: `only notifications from the latest check`, `one update shows nothing`, `same check reuses the cached summary` (a second `UpdateSummaries` with the same cache → Done with 0 new runs), `new check summarizes again`, `hides when ai becomes unavailable`, and `no report shows nothing`.
- [ ] RED, implement, GREEN, commit `feat(notifications): summarize the latest update check`.

### Task 3: ViewModel and UI

**Files:** `NotificationScreenViewModel.kt` (gains `aiText`, `store` and `cache`, and exposes `updateSummary: StateFlow<AiTextState?>` plus `retrySummary()`), `NotificationScreen.kt` (an `item(key = "ai-summary")` before the day buckets when the state isn't null and the filter is All), `NotificationScreenViewModelTest` constructor.
- [ ] Run `./gradlew :kmpuiviews:jvmTest :mangaworld:assembleNoFirebaseDebug`. Expected: PASS.
- [ ] Commit `feat(notifications): update summary card`.
