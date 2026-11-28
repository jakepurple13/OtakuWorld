# Custom Repos Sheet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the bare `CustomUrlDialog` URL list with the approved "C" repo-cards sheet: one layout in every app, each repo checked on its own and shown with a catalog badge, extension count and a clear error state.

**Architecture:** Each custom catalog gains a per-link `checkRepo(url): RepoCheck` (Found / NotThisFormat / Failed). The repo clients (Mihon, Aniyomi, CloudStream) implement it from their existing fetch code. A `CustomRepoChecker` in `kmpuiviews` asks every installed custom catalog, merges the answers into one `CustomRepoStatus` per link, and keeps them in a snapshot map. A new `CustomReposSheet` composable renders that map. `kmpuiviews` holds no catalog names; all catalog wording comes from `catalog.name`.

**Tech Stack:** `kotlin.time.Instant` / `kotlin.time.Clock` (project convention), Kotlin Multiplatform, Compose Multiplatform Material 3, Ktor (`MockEngine` in tests), kotlinx.coroutines test, Compose UI test (`runComposeUiTest`).

**Spec:** `docs/superpowers/specs/2026-11-27-custom-repos-sheet-mockup.html`, columns **C · 1 CATALOG** and **C · 2 CATALOGS** (mockup v4, approved 2026-11-27). Columns NOW, A and B are rejected alternatives; ignore them.

## Global Constraints

- One layout in every app. No totals tiles, no filter chips, nothing that branches on catalog count except wording.
- No "Mihon", "Aniyomi" or "CloudStream" string literals anywhere in `kmpuiviews`. Catalog wording is built from `catalog.name`.
- "Not supported" copy never points to another app (no "Add it in MangaWorld").
- Badge colour comes from the catalog's index in the custom-catalog list (even index = `primaryContainer`, odd = `tertiaryContainer`), never from its name.
- Failed links (Not supported / Couldn't load) show no badge.
- Problem repos (Not supported, Couldn't load) sort above working ones; otherwise keep saved order.
- Only `https://` links are checked; others are `Failed("Only https links are supported")`.
- Every lazy item gets `Modifier.animateItem()`.
- Tall `ModalBottomSheet` uses `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.
- Sheet bottom content respects the app's floating nav padding (`LocalNavHostPadding`) where the FAB sits.
- UI copy is hard-coded English strings, matching the existing `CustomUrlDialog` style.
- Exact copy (`{names}` = catalog names in list order):
  - Title: `Custom repos`
  - Summary: `{total} extensions from {okCount} repo|repos` + (if issues) ` · {n} needs|need attention` + `. Tap a repo for details.`
  - Loading pill: `Checking {names joined " + "}…`
  - Found pill: `{count} extensions`
  - Not supported pill: `Not supported`; detail: 1 catalog `{name} can't read this link. Supported: {name}.`, 2+ `Neither {names joined " nor "} can read this link. Supported: {names joined ", "}.`
  - Failed pill: `Couldn't load`; detail: `Last check failed: {message}. Check the link or try again later.`
  - Add sheet: title `Add a repo`, body `Paste a repo link. Supported: {names joined ", "}.`, buttons `Paste`, `Cancel`, `Add`
  - Card actions: Found → `Browse`, `Refresh`, `Copy link`, `Remove`; Failed → `Retry`, `Copy link`, `Remove`; Not supported → `Copy link`, `Remove`
  - Stats labels: `extensions`, `installed`, `languages`; footer `Checked {relative time}`
  - FAB: `Add repo`

## Review Focus

1. **Aniyomi link pasted in MangaWorld (and Mihon link in AnimeWorld).** Both formats share `repo.json` + `index.min.json`; the repo parses but every entry fails the lib-version filter. Expected: `NotThisFormat`, shown as Not supported, not "0 extensions". Tests in Tasks 2 and 3.
2. **Same repo typed two ways** (`…/repo`, `…/repo/`, `…/repo/index.min.json`). Expected: Add opens the existing card instead of adding a duplicate. Test in Task 5.
3. **Network down while the sheet is open.** Every catalog returns `Failed`. Expected: Couldn't load + Retry, never Not supported. Test in Task 5 (merge precedence).
4. **Legacy "Custom Tachiyomi Bridge" catalog** that doesn't implement `checkRepo`. Expected: the interface default still gives Found / Not supported from `getRemoteSources(listOf(url))`. Test in Task 1.
5. **Removing a repo while its check is in flight.** Expected: the late result is dropped, no card reappears. Test in Task 5.

---

### Task 1: `RepoCheck` contract in kmpmodels

**Files:**
- Create: `kmpmodels/src/commonMain/kotlin/com/programmersbox/kmpmodels/RepoCheck.kt`
- Modify: `kmpmodels/src/commonMain/kotlin/com/programmersbox/kmpmodels/ApiService.kt:116-130` (`KmpExternalCustomApiServicesCatalog`)
- Test: `kmpmodels/src/commonTest/kotlin/com/programmersbox/kmpmodels/RepoCheckDefaultTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  sealed interface RepoCheck {
      /** [name] is the repo's own name from repo.json; blank when unknown. */
      data class Found(val name: String, val remotes: List<KmpRemoteSources>) : RepoCheck
      data object NotThisFormat : RepoCheck
      data class Failed(val message: String) : RepoCheck
  }
  // on KmpExternalCustomApiServicesCatalog:
  suspend fun checkRepo(url: String): RepoCheck
  ```
  Default body: `getRemoteSources(listOf(url))`; empty → `NotThisFormat`, else `Found("", it)`; a thrown non-cancellation exception → `Failed(e.message ?: "Unknown error")`.

- [ ] **Step 1: Write failing tests** with a fake catalog whose `getRemoteSources` returns a fixed list or throws:
  - `defaultCheckFoundWhenSourcesReturned`: 2 remotes → `Found("", thoseRemotes)`
  - `defaultCheckNotThisFormatWhenEmpty`: empty → `NotThisFormat`
  - `defaultCheckFailedWhenThrows`: throws `IllegalStateException("boom")` → `Failed("boom")`
- [ ] **Step 2: Run** `./gradlew :kmpmodels:jvmTest --tests "*RepoCheckDefaultTest"` — expect compile FAIL (`RepoCheck` unresolved).
- [ ] **Step 3: Add `RepoCheck.kt` and the default `checkRepo`.** Rethrow `CancellationException`.
- [ ] **Step 4: Run** the same command — expect PASS.
- [ ] **Step 5: Commit** `feat(kmpmodels): add per-link RepoCheck to custom catalogs`

### Task 2: Mihon `check(url)`

**Files:**
- Modify: `mihonsupport/src/commonMain/kotlin/com/programmersbox/mihon/repo/MihonRepoClient.kt`
- Modify: `mihonsupport/src/androidMain/kotlin/com/programmersbox/mihon/MihonCatalog.kt`, `mihonsupport/src/jvmMain/kotlin/com/programmersbox/mihon/MihonCatalog.kt`
- Test: `mihonsupport/src/commonTest/kotlin/com/programmersbox/mihon/repo/MihonRepoClientTest.kt`

**Interfaces:**
- Consumes: `RepoCheck` (Task 1).
- Produces: `suspend fun MihonRepoClient.check(url: String): RepoCheck`; both `MihonCatalog`s override `checkRepo(url) = repoClient.check(url)`.

Classification rules (all three clients follow the same shape):
- Blank / non-https after `normalize` → `Failed("Only https links are supported")`.
- HTTP non-success or IO error on `repo.json` → `Failed("<status or exception message>")`.
- `repo.json` parses but has `pluginLists`, or has no `meta` → `NotThisFormat`.
- Index fetched, raw entry list non-empty, zero entries pass `MihonLib.isSupported` → `NotThisFormat`.
- Otherwise `Found(repo.meta.name, supportedRemotes)` (an empty raw index is `Found(name, emptyList())`).
- `check` must not touch `keysByRepo` / signing-key state.

Refactor `fetchRepo` so `fetch` and `check` share the parsing; `fetch` behaviour and its existing tests stay unchanged.

- [ ] **Step 1: Write failing tests** in `MihonRepoClientTest` (reuse `client()`):
  - `checkFoundReturnsRepoNameAndSupported`: → `Found("Keiyoushi", …)` with packages `[…mangadex]` (the `1.3.5` and `1.5.1` entries filtered, matching `mapsSupportedEntries`)
  - `checkAniyomiRepoIsNotThisFormat`: index entries with versions `"14.12"`, `"15.3"` → `NotThisFormat`
  - `checkCloudstreamRepoIsNotThisFormat`: `repo.json` = `{"name":"X","manifestVersion":1,"pluginLists":[]}` → `NotThisFormat`
  - `check404IsFailed`: `client(failHost = "repo.example")` → `Failed` (message contains `404`)
  - `checkHttpIsFailed`: `"http://repo.example"` → `Failed("Only https links are supported")`
  - `checkDoesNotChangeSigningKeys`: after `check`, `signingKeys` is empty
- [ ] **Step 2: Run** `./gradlew :mihonsupport:jvmTest --tests "*MihonRepoClientTest"` — expect FAIL.
- [ ] **Step 3: Implement `check`** and the two `checkRepo` overrides.
- [ ] **Step 4: Run** the same command — all tests (old and new) PASS.
- [ ] **Step 5: Commit** `feat(mihon): per-link repo check`

### Task 3: Aniyomi `check(url)`

**Files:**
- Modify: `source_utilities/aniyomi/src/commonMain/kotlin/com/programmersbox/aniyomi/repo/AniyomiRepoClient.kt`
- Modify: `source_utilities/aniyomi/src/androidMain/kotlin/com/programmersbox/aniyomi/AniyomiCatalog.kt`, `source_utilities/aniyomi/src/jvmMain/kotlin/com/programmersbox/aniyomi/AniyomiCatalog.kt`
- Create test: `source_utilities/aniyomi/src/commonTest/kotlin/com/programmersbox/aniyomi/repo/AniyomiRepoClientCheckTest.kt`

**Interfaces:**
- Consumes: `RepoCheck` (Task 1).
- Produces: `suspend fun AniyomiRepoClient.check(url: String): RepoCheck`; both `AniyomiCatalog`s override `checkRepo`.

Same classification rules as Task 2 with `AniyomiLib.isSupported`. Mirror `MihonRepoClientTest`'s `MockEngine` helper.

- [ ] **Step 1: Write failing tests:**
  - `checkFoundReturnsRepoName`: meta name `"Kohi-den"`, entries `"14.12"`, `"15.3"` → `Found("Kohi-den", 2 remotes)`
  - `checkMihonRepoIsNotThisFormat`: entries `"1.4.190"`, `"1.4.12"` → `NotThisFormat`
  - `checkCloudstreamRepoIsNotThisFormat`: `pluginLists` repo → `NotThisFormat`
  - `check404IsFailed`, `checkHttpIsFailed` as in Task 2
- [ ] **Step 2: Run** `./gradlew :source_utilities:aniyomi:jvmTest --tests "*AniyomiRepoClientCheckTest"` — expect FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :source_utilities:aniyomi:jvmTest` — PASS, existing tests included.
- [ ] **Step 5: Commit** `feat(aniyomi): per-link repo check`

### Task 4: CloudStream `check(url)`

**Files:**
- Modify: `source_utilities/cloudstream/src/commonMain/kotlin/com/programmersbox/cloudstream/repo/CloudstreamRepoClient.kt`
- Modify: `source_utilities/cloudstream/src/jvmAndroidShared/kotlin/com/programmersbox/cloudstream/CloudstreamCatalog.kt`
- Test: `source_utilities/cloudstream/src/commonTest/kotlin/com/programmersbox/cloudstream/repo/CloudstreamRepoClientTest.kt`

**Interfaces:**
- Consumes: `RepoCheck` (Task 1).
- Produces: `suspend fun CloudstreamRepoClient.check(url: String): RepoCheck`; `CloudstreamCatalog.checkRepo` override.

Rules: no `pluginLists` key → `NotThisFormat`. `repo.json` unreachable → `Failed`. Plugin lists: if every list fails → `Failed("<first list error>")`; else `Found(repo.name, animePlugins)` (non-anime plugins are dropped as in `fetch`, so a repo with lists but zero anime plugins is `Found(name, emptyList())`). `check` must not replace `pluginsByLink`.

- [ ] **Step 1: Write failing tests** (reuse the file's `client()`):
  - `checkFoundReturnsRepoName`: → `Found(<repo name in fixture>, <anime plugins only>)`
  - `checkAniyomiRepoIsNotThisFormat`: `{"meta":{"name":"A","signingKeyFingerprint":"x"}}` → `NotThisFormat`
  - `checkAllPluginListsFailIsFailed`: plugin list URL 500 → `Failed`
  - `checkDoesNotReplacePluginLookup`: `fetch` then `check` of another repo → `pluginFor(<first repo link>)` still non-null
  - `checkHttpIsFailed`
- [ ] **Step 2: Run** `./gradlew :source_utilities:cloudstream:jvmTest --tests "*CloudstreamRepoClientTest"` — expect FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :source_utilities:cloudstream:jvmTest` — PASS.
- [ ] **Step 5: Commit** `feat(cloudstream): per-link repo check`

### Task 5: `CustomRepoChecker` (merge + state) in kmpuiviews

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/CustomRepoChecker.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionListViewModel.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/CustomRepoCheckerTest.kt`

**Interfaces:**
- Consumes: `RepoCheck`, `KmpExternalCustomApiServicesCatalog.checkRepo` (Task 1).
- Produces:
  ```kotlin
  sealed interface CustomRepoStatus {
      data object Checking : CustomRepoStatus
      data class Found(val catalogIndex: Int, val catalogName: String, val repoName: String,
                       val remotes: List<KmpRemoteSources>, val checkedAt: Instant) : CustomRepoStatus
      data class Unsupported(val checkedAt: Instant) : CustomRepoStatus
      data class Failed(val message: String, val checkedAt: Instant) : CustomRepoStatus
  }
  /** Found (first catalog in list order) > Failed (first message) > Unsupported. */
  fun mergeRepoChecks(checks: List<RepoCheck>, catalogs: List<KmpExternalCustomApiServicesCatalog>, now: Instant): CustomRepoStatus
  /** Lower-cased, trimmed, trailing "/", "/index.min.json", "/repo.json" removed. Used only for duplicate detection. */
  fun repoKey(url: String): String

  class CustomRepoChecker(
      private val catalogs: () -> List<KmpExternalCustomApiServicesCatalog>,
      private val scope: CoroutineScope,
      private val clock: () -> Instant = { Clock.System.now() },
  ) {
      val statuses: SnapshotStateMap<String, CustomRepoStatus>   // keyed by saved url
      fun sync(urls: List<String>)        // checks urls with no status, drops statuses for removed urls
      fun recheck(url: String)
      fun recheckAll()
      fun existing(url: String, saved: List<String>): String?   // saved url with same repoKey, or null
  }
  ```
  `ExtensionListViewModel` exposes `val customCatalogs: List<KmpExternalCustomApiServicesCatalog>` (the same deduped list `refresh()` builds at lines 156-160, filtered `hasRemoteSources`, list order = installed custom catalogs then `nativeCatalogs.catalogs`) and `val customRepos = CustomRepoChecker({ customCatalogs }, viewModelScope)`; it calls `customRepos.sync(urls)` from the existing `customUrls` collector.

Each check asks all catalogs in parallel; status is `Checking` until all answer. A result for a url no longer in the map (removed meanwhile) is dropped.

Follow the project's VM-test rules: `setMain(newSingleThreadContext(...))`, poll on Main, `clearAndJoin()` before `resetMain`; never sleep or raise timeouts. Here test `CustomRepoChecker` directly with a `TestScope` and fake catalogs, so no `Dispatchers.Main` is needed.

- [ ] **Step 1: Write failing tests:**
  - `mergeFoundBeatsFailedAndUnsupported`: `[NotThisFormat, Failed("x"), Found("R", l)]` with catalogs `[A, B, C]` → `Found(catalogIndex = 2, catalogName = "C", repoName = "R", …)`
  - `mergeFailedBeatsUnsupported`: `[NotThisFormat, Failed("timeout")]` → `Failed("timeout", …)` (Review Focus 3)
  - `mergeAllNotThisFormatIsUnsupported`
  - `mergeFoundBlankNameFallsBackToHost`: `Found("", l)` for `https://repo.example/x` → `repoName == "repo.example"`
  - `repoKeyMatchesVariants`: `…/repo`, `…/repo/`, `…/repo/index.min.json`, `…/repo/repo.json`, `  …/REPO  ` → equal (Review Focus 2)
  - `syncChecksNewAndDropsRemoved`: sync `[a, b]` → both Found; sync `[b]` → map keys `[b]`, catalog called once for `b`
  - `removedDuringCheckIsDropped`: fake catalog suspends on a `CompletableDeferred`; sync `[a]`, sync `[]`, complete → map empty (Review Focus 5)
  - `recheckGoesThroughChecking`: status is `Checking` while the fake is suspended, then the new result
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*CustomRepoCheckerTest"` — expect FAIL.
- [ ] **Step 3: Implement** `CustomRepoChecker.kt` and the VM wiring.
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest --tests "*CustomRepoCheckerTest" --tests "*RemoteCatalogsTest" --tests "*ExtensionStatesTest"` — PASS.
- [ ] **Step 5: Commit** `feat(extensions): per-repo status checker`

### Task 6: `CustomReposSheet` UI

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/CustomReposSheet.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionListScreen.kt` (call site at line ~143, delete `CustomUrlDialog` at 295-460 and imports it leaves unused)
- Modify: `CHANGELOG.md` (Unreleased → Improvements)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/CustomReposSheetTest.kt`

**Interfaces:**
- Consumes: `CustomRepoStatus`, `CustomRepoChecker`, `repoKey`, `ExtensionListViewModel.customCatalogs` (Task 5).
- Produces:
  ```kotlin
  @Composable
  internal fun CustomReposSheet(
      urls: List<String>,
      statuses: Map<String, CustomRepoStatus>,
      catalogNames: List<String>,
      installedPackages: Set<String>,
      onAdd: (String) -> Unit,
      onRemove: (String) -> Unit,
      onRecheck: (String) -> Unit,
      onRecheckAll: () -> Unit,
      onBrowse: (catalogName: String) -> Unit,
      onCopy: (String) -> Unit,
      onDismissRequest: () -> Unit,
  )
  ```
  Plus a stateless `CustomReposContent(...)` (same params minus `onDismissRequest`) that the sheet hosts and the test renders.

Layout per spec column C: top bar (close, `Custom repos`, refresh-all), summary line, `LazyColumn` of expandable cards (avatar letter, repo name, badge, status pill, chevron; expanded: stats row or error text, full url, `Checked …`, action chips), `ExtendedFloatingActionButton` `Add repo` opening a small `ModalBottomSheet` with the url field. Motion: card expand via `AnimatedVisibility(expandVertically() + fadeIn())`, chevron `animateFloatAsState` 0→180°, pill via `AnimatedContent`, cards `Modifier.animateItem()`.

Behaviour:
- Card title = `Found.repoName`; otherwise url host.
- `installed` stat = `remotes.count { it.packageName in installedPackages }`; `languages` = distinct non-blank `lang`.
- `Browse` → `onBrowse(catalogName)`, which navigates to `Screen.ExtensionListScreen.Catalog(catalogName)` (opens that catalog; per-repo filtering is out of scope).
- Add: if `repoKey` matches a saved url, expand that card instead of calling `onAdd` (Review Focus 2). Paste reads the clipboard via `LocalClipboard`.
- Copy keeps the existing `sourceInfoRepository.copyUrl` + `banner_link_copied` notifier.
- Remove is immediate (no confirm dialog, per spec).

- [ ] **Step 1: Write failing Compose UI tests** (`runComposeUiTest`, pattern from `LayoutSourcesStepTest`):
  - `oneCatalogShowsBadgeAndSummary`: names `["Alpha"]`, two Found (100, 20) + one Failed → text `120 extensions from 2 repos · 1 needs attention. Tap a repo for details.`; two nodes with text `Alpha`
  - `twoCatalogsUnsupportedCopy`: names `["Alpha", "Beta"]`, one Unsupported, expand it → `Neither Alpha nor Beta can read this link. Supported: Alpha, Beta.`
  - `checkingPillListsAllCatalogs`: Checking → `Checking Alpha + Beta…`
  - `problemReposSortFirst`: urls `[ok1, bad, ok2]` → first card title is `bad`'s host
  - `failedHasRetryNotBrowse`: expanded Failed shows `Retry`, no `Browse`; Unsupported shows neither
  - `duplicateAddExpandsExisting`: add `…/repo/index.min.json` when `…/repo` saved → `onAdd` not called
  - `noCatalogNameLiteralsInSource`: read `CustomReposSheet.kt`, `CustomRepoChecker.kt` source text; assert none contain `Mihon`, `Aniyomi`, `CloudStream`
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*CustomReposSheetTest"` — expect FAIL.
- [ ] **Step 3: Implement** `CustomReposSheet.kt`, swap the call site, delete `CustomUrlDialog`.
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest` — PASS. Then `./gradlew :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :mangaworld:desktop:compileKotlinJvm` — BUILD SUCCESSFUL.
- [ ] **Step 5: Device check with adb** (not ARTEMIS). Before installing, diff Room versions with the main checkout. Install with `adb -s <serial> install` (Gradle install hits every device). Package `…​.noFirebase.debug`. In MangaWorld and AnimeWorld: open Extensions ⋮ → Custom repos, add a valid repo, a wrong-format repo and `https://example.invalid/repo`; confirm badge, Not supported and Couldn't load cards; remove only the repos you added. Screenshot each state.
- [ ] **Step 6: Add CHANGELOG line** under Unreleased: `Custom repos sheet shows each repo's name, catalog and extension count, explains links that can't be read, and lets you refresh, browse, copy or remove a repo.`
- [ ] **Step 7: Commit** `feat(extensions): custom repos sheet redesign`
