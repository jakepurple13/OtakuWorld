# Describe To Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A spark button in the Favorites search field turns a descriptive sentence into keywords and runs an any-keyword FTS search.

**Spec:** `docs/superpowers/specs/2026-11-17-describe-search-design.md`

## Global Constraints

- Use `aiText.availability` for the button, so it follows the AI page live.
- Plain typing never calls AI.
- Tests: `./gradlew :favoritesdatabase:jvmTest :kmpuiviews:jvmTest`. Build: `./gradlew :mangaworld:assembleNoFirebaseDebug`.

## Review Focus

1. Typing after a describe returns to normal search and drops the keywords. Pinned by `typing again clears keywords`.
2. FTS syntax inside an AI keyword (quotes, `*`, `-`, `OR`) can't break the query. Pinned by `ftsMatchAny sanitizes keywords`.
3. An AI error leaves the normal search untouched. Pinned by `failed describe keeps normal search`.
4. Turning the switch off hides the button while the screen is open. Pinned by `describe availability follows settings`.
5. Fewer than 4 words shows no button. Pinned by `short queries cannot describe`.

### Task 1: Any-keyword FTS search
**Files:** `favoritesdatabase/.../FtsQuery.kt` (`ftsMatchAny`), `FtsQueryTest`, `kmpuiviews/.../repository/FavoritesSearchRepository.kt` (`searchAny`), `FavoritesSearchRepositoryTest`.
- [ ] Tests: `ftsMatchAny joins keywords with OR`, `ftsMatchAny sanitizes keywords`, `ftsMatchAny scopes every token to the column`, `ftsMatchAny returns null without tokens`, and `searchAny finds titles matching any keyword` (repository, real Room).
- [ ] RED, implement, GREEN, commit `feat(favorites): any-keyword search`.

### Task 2: SearchKeywords feature and ViewModel
**Files:** `AiFeature.kt`, `AiSettings.kt` (`AiToggle.DescribeSearch`), `AiSettingsScreen.kt` (Library row), strings, `FavoriteViewModel.kt` (gains `aiText`; exposes `canDescribe: Boolean`, `keywords: List<String>?`, `describing: Boolean`, `describe()`, `removeKeyword(k)`), `FavoriteViewModelTest`, and `AiFeatureTest`.
- [ ] Tests: `search keywords finish cleans and caps`, `short queries cannot describe`, `describe searches any keyword`, `typing again clears keywords`, `removing the last keyword returns to normal search`, `failed describe keeps normal search`, and `describe availability follows settings`.
- [ ] RED, implement, GREEN, commit `feat(favorites): describe to search`.

### Task 3: UI
**Files:** `FavoriteScreen.kt` (`FavoriteSearchBar`). The spark `IconButton` goes in an `AnimatedVisibility(scaleIn(spring(0.5f)))`. While `describing`, the field gets `Modifier.generatingBorder(true, CircleShape)`. Below the bar sits an `AnimatedVisibility(expandVertically)` `FlowRow` of `InputChip`s with a "Searching for" label.
- [ ] Run `./gradlew :kmpuiviews:jvmTest :mangaworld:assembleNoFirebaseDebug`. Expected: PASS.
- [ ] Commit `feat(favorites): describe-to-search UI`.
