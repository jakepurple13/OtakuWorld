# Update Check Error Explainer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expanded rows on the Update check errors screen explain the error: fixed text for known errors, and a Nano explanation for unknown ones.

**Architecture:** A pure `knownError()` maps a failure to a `KnownError` with a localized string. The new `AiFeature.ExplainError` handles everything else. `UpdateCheckErrorsViewModel` keeps one `AiTask` per distinct error key. `FailureRow` renders a static card or an `AiCard` above the raw detail.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform resources, coroutines, Koin, kotlin.test.

**Spec:** `docs/superpowers/specs/2026-11-17-error-explainer-design.md`

## Global Constraints

- All user-facing text lives in `kmpuiviews/src/commonMain/composeResources/values/strings.xml`, copied verbatim from the spec table.
- No AI control is visible when `explainAvailable` is false.
- One generation per distinct error key: `source + "\n" + detail.lineSequence().first()`.
- Tests: `./gradlew :kmpuiviews:jvmTest`. App build: `./gradlew :mangaworld:assembleNoFirebaseDebug`.

## Review Focus

1. Two rows with the same error expand, and only one generation runs. Pinned by `explain runs once per distinct error` (Task 2).
2. Expanding a row with a known error never calls AI. Pinned by `known errors never reach ai` (Task 2).
3. A blank detail (no first line) must not crash the key or the prompt. Pinned by `blank detail still keys and prepares` (Task 2).
4. HTTP 503 is Blocked, not ServerError. Pinned by `http codes map` (Task 1).
5. A failed generation can be retried. Pinned by `failed explanation can retry` (Task 2).

---

### Task 1: Known error mapping

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/domain/updatecheck/KnownError.kt`
- Modify: `kmpuiviews/src/commonMain/composeResources/values/strings.xml` (add `uc_explain_common`, `uc_explain_timed_out`, `uc_explain_rate_limited`, `uc_explain_blocked`, `uc_explain_not_found`, `uc_explain_server_error`, `uc_explain_offline`, `uc_explain_ai_label`)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/domain/updatecheck/KnownErrorTest.kt`

**Interfaces:**
- Produces: `enum class KnownError { TimedOut, RateLimited, Blocked, NotFound, ServerError, Offline }`, `fun knownError(kind: FailureKind, httpCode: Int?, detail: String): KnownError?`, `suspend fun KnownError.explanation(sourceName: String): String`, and `val KnownError.resource: StringResource` for the UI.

- [ ] Step 1: Write the tests: `timeout is known`, `http codes map` (429→RateLimited, 403→Blocked, 503→Blocked, 404→NotFound, 410→NotFound, 500→ServerError, 502→ServerError, 401→null, null code→null), `offline signatures map` (each spec pattern in the first line → Offline; `SerializationException: ...` → null; an offline pattern on line 2 only → null), and `every known error has text naming the source` (`explanation("MangaDex")` contains "MangaDex" for every entry).
- [ ] Step 2: Run `./gradlew :kmpuiviews:jvmTest --tests "*KnownErrorTest*"`. Expected: compile FAIL.
- [ ] Step 3: Implement it, plus the strings (the `uc_explain_common` label is "Common error"; `uc_explain_ai_label` is "What this means").
- [ ] Step 4: Run the same command. Expected: PASS.
- [ ] Step 5: Commit `feat(update-errors): fixed explanations for known update errors`.

### Task 2: ExplainError feature and ViewModel cache

**Files:**
- Modify: `.../domain/aitext/AiFeature.kt` (add `ErrorDetails` and `ExplainError`)
- Modify: `.../presentation/updatecheck/UpdateCheckErrorsViewModel.kt` (constructor gains `aiText: AiText` last)
- Test: `AiFeatureTest` (prepare/cap), `UpdateCheckErrorsViewModelTest` (new cases; pass `FakeAiText` to the existing constructor calls)

**Interfaces:**
- Produces: `data class ErrorDetails(val sourceName: String, val detail: String)`, `AiFeature.ExplainError`, `fun ItemFailure.explanationKey(): String`, and on the VM: `val explainAvailable: StateFlow<Boolean>`, `fun explanation(item: ItemFailure): StateFlow<AiTextState>`, `fun explain(item: ItemFailure)` (no-op for known errors, when unavailable, or when already Loading/Streaming/Done; restarts from Error).

- [ ] Step 1: Write the tests: `explain error prepares source and capped detail`, `explain runs once per distinct error`, `different errors run separately`, `known errors never reach ai`, `no engine means not available and no run`, `failed explanation can retry`, and `blank detail still keys and prepares`.
- [ ] Step 2: Run `./gradlew :kmpuiviews:jvmTest --tests "*AiFeatureTest*" --tests "*UpdateCheckErrorsViewModelTest*"`. Expected: compile FAIL.
- [ ] Step 3: Implement. The VM gets the source name from `sourceRepository.displayNameFor(item.source)`. It keeps the tasks in a `mutableMapOf<String, AiTask>()` touched only on Main.
- [ ] Step 4: Run the same command. Expected: PASS.
- [ ] Step 5: Commit `feat(update-errors): Nano explanation for unknown update errors`.

### Task 3: UI

**Files:**
- Modify: `.../presentation/updatecheck/UpdateCheckErrorsScreen.kt` (`FailureRow`)

- [ ] Step 1: In `FailureRow`, the row is clickable when `detail.isNotBlank() || known != null`. When expanded: a known error shows a `Surface(color = surfaceContainer, shape = large)` with the `uc_explain_common` label (labelSmall, uppercase) and its text. Otherwise, when `explainAvailable` is true, it shows `AiCard(label = uc_explain_ai_label, state, onRetry = { explain(item) })`, with `LaunchedEffect(Unit) { explain(item) }` inside the expanded branch. The raw detail follows. Wrap the expanded content in `AnimatedVisibility(expandVertically() + fadeIn())`.
- [ ] Step 2: Run `./gradlew :kmpuiviews:jvmTest :mangaworld:assembleNoFirebaseDebug`. Expected: PASS and BUILD SUCCESSFUL.
- [ ] Step 3: Commit `feat(update-errors): show explanations in expanded error rows`.
