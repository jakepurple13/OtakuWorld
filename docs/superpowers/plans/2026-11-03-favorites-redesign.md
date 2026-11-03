# Favorites Redesign + FTS5 Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the favorites screen with the approved two-tab Overview/Library design (filter sheet, list/grid, animated), and replace substring search with FTS5 search over title, description and notes.

**Architecture:** Two FTS5 tables, `favorites_fts` in ItemDatabase and `notes_fts` (migrated from FTS4) in NotesDatabase. `FavoritesSearchRepository` merges them by URL. `FavoriteViewModel` owns filters, sort, search, and turns them into section models built by pure functions. The Compose UI splits into Overview, Library and FilterSheet files, all inside one `SharedTransitionLayout`.

**Tech Stack:** Room 3.0.3 (`androidx.room3`, `@Fts5`), `androidx.sqlite:sqlite-bundled` 2.7.1, Compose Multiplatform + Material 3 Expressive, Koin, protobuf DataStore.

**Spec:** `docs/superpowers/specs/2026-11-03-favorites-redesign-design.md` (the prototype it links to is the visual reference; match it).

## Global Constraints

- Use the existing `favoritesdatabase` / `kmpuiviews` KMP source sets. The code goes in `commonMain` except for the Android driver.
- ItemDatabase goes from version 7 to **8**, and NotesDatabase from 2 to **3**. Both use manual `Migration`s, and the schema JSON is exported to `favoritesdatabase/schemas`.
- **Device caution:** the main checkout has an orphan `ItemDatabase/8.json` with a different schema (`Notifications.sourceDisplayName`). Before installing on a phone, check the installed DB version: `adb shell run-as com.programmersbox.mangaworld.noFirebase sqlite3 databases/favoriteItems.db 'PRAGMA user_version'`. If it is 8, clear app data first and tell the user.
- The FTS tokenizer is `unicode61` with `tokenizerArgs = ["remove_diacritics", "2"]` and `prefix = [2, 3]`.
- The highlight markers are `char(2)` (start) and `char(3)` (end). The ellipsis is `…` and snippets are 12 tokens long.
- bm25 weights for `favorites_fts` are `(10.0, 1.0)` for title and description.
- Search debounce is 150 ms. "Recently added" means createdAt > 0 and within 30 days. Added buckets: ≤7 days "This week", ≤30 days "This month", otherwise "Earlier".
- Chapter buckets are `>= 500`, `100..499` and `< 100`, using the group's max `numChapters`.
- Every lazy item and sticky header gets `Modifier.animateItem()`. Specs come from `MaterialTheme.motionScheme`.
- User-facing copy goes in `kmpuiviews/src/commonMain/composeResources/values/strings.xml`, with exact text from the spec.
- Build with the `noFirebase` flavor. Run JVM tests with `./gradlew :favoritesdatabase:jvmTest` / `./gradlew :kmpuiviews:jvmTest`.

## Review Focus

- **FTS query syntax in user input:** quotes, `*`, `-`, `:`, `(`, `AND`/`OR`/`NEAR` typed as words. The query must never throw; it is treated as plain words. Test this in Task 1.
- **Edits after migration:** after the 7→8 migration, editing or deleting a favorite must update `favorites_fts`, since Room recreates the sync triggers after migrating. Test this in Task 2.
- **Soft-deleted favorites:** `is_deleted = 1`, which happens while logged in to Supabase. These must not appear in results. Test this in Task 2.
- **Notes for items that aren't favorites:** a note matches, but its URL has no favorite. The repository output keeps the hit, and the ViewModel drops it. Test this in Task 7.
- **Favorites with `createdAt = 0`:** this is every favorite added before this change. They are never "recent", and they sort to "Earlier" under Added. Test this in Task 6.

---

### Task 1: FTS match builder

**Files:**
- Create: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/FtsQuery.kt`
- Test: `favoritesdatabase/src/jvmTest/kotlin/com/programmersbox/favoritesdatabase/FtsQueryTest.kt`

**Interfaces:**
- Produces: `fun ftsMatch(raw: String, column: String? = null): String?`. It returns null when `raw` has no letter or digit runs. It splits on `Regex("[\\p{L}\\p{N}]+")`, quotes each token as `"tok"*`, and joins with spaces (implicit AND). With `column`, each phrase becomes `column : "tok"*`.

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun `single word becomes quoted prefix`() = assertEquals("\"blue\"*", ftsMatch("blue"))
@Test fun `words AND together`() = assertEquals("\"blu\"* \"lo\"*", ftsMatch("  blu   lo "))
@Test fun `fts syntax is stripped`() = assertEquals("\"a\"* \"b\"* \"NEAR\"* \"c\"*", ftsMatch("a\"* -b: NEAR(c)"))
@Test fun `blank or symbols only gives null`() { assertNull(ftsMatch("")); assertNull(ftsMatch(" *\"- ")) }
@Test fun `column filter per phrase`() = assertEquals("content : \"tour\"* content : \"arc\"*", ftsMatch("tour arc", column = "content"))
@Test fun `unicode letters kept`() = assertEquals("\"進撃\"* \"café\"*", ftsMatch("進撃 café"))
```

- [ ] **Step 2: Run** `./gradlew :favoritesdatabase:jvmTest --tests "*FtsQueryTest"`. Expected: FAIL (unresolved `ftsMatch`).
- [ ] **Step 3: Implement `ftsMatch`** in `FtsQuery.kt`.
- [ ] **Step 4: Run the same command.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(db): add FTS5 match query builder`

### Task 2: favorites_fts in ItemDatabase (v8) + bundled driver on Android

**Files:**
- Modify: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/ItemModels.kt` (add the FTS entity next to `DbModel`)
- Modify: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/ItemDao.kt`
- Modify: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/ItemDatabase.kt` (entities, `version = 8`, `MIGRATION_7_8`)
- Modify: `favoritesdatabase/src/androidMain/kotlin/com/programmersbox/favoritesdatabase/DatabaseBuilder.android.kt` (`.setDriver(BundledSQLiteDriver())` in `getDatabaseBuilder`)
- Modify: `favoritesdatabase/build.gradle.kts` (add `implementation(commonLibs.androidx.room.sqlite)` to `androidMain.dependencies`)
- Create: `favoritesdatabase/schemas/com.programmersbox.favoritesdatabase.ItemDatabase/8.json` (generated)
- Test: `favoritesdatabase/src/jvmTest/kotlin/com/programmersbox/favoritesdatabase/FavoritesFtsTest.kt`, `.../ItemDatabaseMigration7To8Test.kt`

**Interfaces:**
- Consumes: `ftsMatch` (Task 1).
- Produces:
  - `@Entity(tableName = "favorites_fts") @Fts5(contentEntity = DbModel::class, tokenizerArgs = ["remove_diacritics", "2"], prefix = [2, 3]) data class DbModelFts(val title: String, val description: String)`
  - `data class FavoriteFtsHit(val url: String, val rank: Double, val titleHighlight: String, val descriptionSnippet: String)`
  - `ItemDao.searchFavorites(match: String): Flow<List<FavoriteFtsHit>>`. The query:

```sql
SELECT FavoriteItem.url AS url,
       bm25(favorites_fts, 10.0, 1.0) AS rank,
       highlight(favorites_fts, 0, char(2), char(3)) AS titleHighlight,
       snippet(favorites_fts, 1, char(2), char(3), '…', 12) AS descriptionSnippet
FROM favorites_fts JOIN FavoriteItem ON FavoriteItem.rowid = favorites_fts.rowid
WHERE favorites_fts MATCH :match AND FavoriteItem.is_deleted = 0
ORDER BY rank
```

  - `ItemDatabase.MIGRATION_7_8`: runs the `createSql` for `favorites_fts` exactly as Room writes it to `8.json`, then `INSERT INTO favorites_fts(favorites_fts) VALUES('rebuild')`.

- [ ] **Step 1: Spike Room's compile-time check.** Add the entity and the DAO query, then run `./gradlew :favoritesdatabase:kspKotlinJvm`. If Room rejects `bm25`/`highlight`/`snippet`, keep the same columns and move the query to a `@RawQuery(observedEntities = [DbModel::class, DbModelFts::class])` that takes a `RoomRawQuery`. Note the outcome in the commit message.
- [ ] **Step 2: Write failing DAO tests** in `FavoritesFtsTest` (database built as in `NotesDaoTest`, with `BundledSQLiteDriver`):
  - `title prefix match` inserts "Blue Lock" and searches `ftsMatch("blu lo")`. Expect 1 hit, and `titleHighlight == "\u0002Blue\u0003 \u0002Lock\u0003"`.
  - `description match has snippet and no title marker` searches "strikers". Expect `'\u0002' !in titleHighlight` and `"\u0002strikers\u0003" in descriptionSnippet`.
  - `title outranks description` has A with "mage" in the title and B with "mage" in the description. Expect A to be first.
  - `soft deleted excluded`: after `softDeleteFavorite(url, 1)`, expect no hits.
  - `update reindexes`: after `updateFavoriteItem(copy(title = "Kaiju"))`, the old word finds nothing and "kai" finds the item.
  - `hard delete removes`: after `deleteFavorite`, expect no hits.
  - `accents folded`: "Café Terrace" is found by "cafe".
  - `no substring match`: "ock" finds nothing.
- [ ] **Step 3: Write the failing migration test** `ItemDatabaseMigration7To8Test`. Open a raw `BundledSQLiteDriver` connection to a temp file. Run the `createSql` statements and `setupQueries` from `7.json` (substitute `${TABLE_NAME}`), insert one FavoriteItem row ("Blue Lock"), and set `PRAGMA user_version = 7`. Close it, open Room with `ItemDatabase.getInstance`-equivalent migrations, and assert that `searchFavorites(ftsMatch("lock")!!).first()` has 1 hit. Then insert another favorite through the DAO and assert that it is searchable, which proves the triggers were recreated. Expose the migrations to tests as `internal val ItemDatabase.Companion.migrations` if needed.
- [ ] **Step 4: Run** `./gradlew :favoritesdatabase:jvmTest --tests "*FavoritesFtsTest" --tests "*ItemDatabaseMigration7To8Test"`. Expected: FAIL.
- [ ] **Step 5: Implement.** Bump to version 8, add `MIGRATION_7_8`, and register it in `getInstance`. Build once, so `8.json` is generated, and copy its `favorites_fts` createSql into the migration verbatim.
- [ ] **Step 6: Run the tests again.** Expected: PASS. Then run `./gradlew :favoritesdatabase:jvmTest`. Expected: all tests pass.
- [ ] **Step 7: Verify the Android build** with `./gradlew :mangaworld:assembleNoFirebaseDebug`. Expected: BUILD SUCCESSFUL. Record the APK size change in the commit body.
- [ ] **Step 8: Commit** `feat(db): FTS5 index on favorites (ItemDatabase v8), bundled SQLite on Android` (include `8.json`).

### Task 3: notes_fts FTS4 → FTS5 (NotesDatabase v3)

**Files:**
- Modify: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/NotesDatabase.kt`
- Create: `favoritesdatabase/schemas/com.programmersbox.favoritesdatabase.NotesDatabase/3.json` (generated)
- Test: `favoritesdatabase/src/jvmTest/kotlin/com/programmersbox/favoritesdatabase/NotesFtsTest.kt`, `.../NotesDatabaseMigration2To3Test.kt`

**Interfaces:**
- Produces:
  - `NoteItemFts` annotated `@Fts5(contentEntity = NoteItem::class, tokenizerArgs = ["remove_diacritics", "2"], prefix = [2, 3])`, with the columns and table name `notes_fts` unchanged.
  - `data class NoteFtsHit(val itemUrl: String, val rank: Double, val contentSnippet: String)`.
  - `NotesDao.searchNoteHits(match: String): Flow<List<NoteFtsHit>>`, which selects `notes.itemUrl`, `bm25(notes_fts)` and `snippet(notes_fts, 0, char(2), char(3), '…', 12)`, with `is_deleted = 0` and `ORDER BY rank`. Callers pass `ftsMatch(q, column = "content")`.
  - The existing `searchNotes` keeps working.
  - `MIGRATION_2_3`: `DROP TABLE IF EXISTS notes_fts`, then the FTS5 createSql from `3.json`, then `INSERT INTO notes_fts(notes_fts) VALUES('rebuild')`.

- [ ] **Step 1: Write failing tests:**
  - `content match returns snippet`: a note with "Wait for the tournament arc to finish" is found by `ftsMatch("tour arc", "content")`, and its snippet contains `"\u0002tournament\u0003"`.
  - `itemTitle not searched`: a note with itemTitle "Kagurabachi" and other content gives no hit for `ftsMatch("kagura", "content")`.
  - `soft deleted excluded`.
  - The migration test does the same as in Task 2, using `2.json`: a note inserted at v2 is searchable after the migration, and a note inserted afterwards is too.
- [ ] **Step 2: Run** `./gradlew :favoritesdatabase:jvmTest --tests "*NotesFtsTest" --tests "*NotesDatabaseMigration2To3Test"`. Expected: FAIL.
- [ ] **Step 3: Implement** the version bump, migration and DAO query.
- [ ] **Step 4: Run the tests again** (expected PASS), then `./gradlew :favoritesdatabase:jvmTest` (expected all pass, including the existing `NotesDaoTest`).
- [ ] **Step 5: Commit** `feat(db): migrate notes search to FTS5 (NotesDatabase v3)`.

### Task 4: Search repository + highlight parsing

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/FavoritesSearchRepository.kt`
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/favorite/HighlightText.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/` (the module that registers `FavoritesRepository`: add `singleOf(::FavoritesSearchRepository)`)
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/FavoritesRepository.kt` (`addFavorite` stamps `createdAt = now` when it is `0L`)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/FavoritesSearchRepositoryTest.kt`, `.../presentation/favorite/HighlightTextTest.kt`

**Interfaces:**
- Consumes: `ItemDao.searchFavorites`, `NotesDao.searchNoteHits`, `ftsMatch`.
- Produces:
  - `enum class MatchField { TITLE, DESCRIPTION, NOTE }`
  - `data class FavoriteSearchHit(val url: String, val group: MatchField, val titleHighlight: String?, val snippet: String?, val snippetField: MatchField?, val matchedFields: Set<MatchField>, val rank: Double)`
  - `class FavoritesSearchRepository(itemDao: ItemDao, notesDao: NotesDao) { fun search(raw: String): Flow<List<FavoriteSearchHit>> }`. This emits `emptyList()` when `ftsMatch(raw)` is null. Otherwise it `combine`s both DAO flows and merges by URL:
    - `group`: TITLE if `titleHighlight` contains `\u0002`, else DESCRIPTION if there is a favorites hit, else NOTE.
    - `snippet`: the note snippet when the note matched, else the description snippet when it contains `\u0002`, else null.
    - Sort by `group.ordinal`, then `rank`.
  - `fun parseHighlight(marked: String): Pair<String, List<IntRange>>` strips `\u0002`/`\u0003` and returns the ranges of the plain text.
  - `@Composable fun highlightedText(marked: String): AnnotatedString`. Its span is `SpanStyle(background = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f))`.

- [ ] **Step 1: Write failing tests:**
  - parseHighlight: `"\u0002Blue\u0003 \u0002Lock\u0003"` gives `"Blue Lock"` with `[0..3, 5..8]`. Text without markers gives no ranges.
  - Repository (real temp DBs from Tasks 2–3):
    - "lock" gives TITLE.
    - "strikers" gives DESCRIPTION, with snippetField DESCRIPTION.
    - "mage", where the description and note both match, gives DESCRIPTION, with snippetField NOTE and matchedFields `{DESCRIPTION, NOTE}`.
    - "tournament arc", with a note-only match, gives NOTE.
    - A word in the title plus a word only in the note gives no hit (no mixing across the indexes).
    - `"\"*"` emits an empty list without throwing.
    - A note whose URL has no favorite is still emitted (the ViewModel filters it).
  - `FavoritesRepository`: `addFavorite` with createdAt 0 stores a value > 0, and with createdAt 123 keeps 123.
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*FavoritesSearchRepositoryTest" --tests "*HighlightTextTest" --tests "*FavoritesRepository*"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run the tests again.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(favorites): merged FTS search repository`.

### Task 5: Persisted view settings

**Files:**
- Modify: `datastore/src/commonMain/proto/settings.proto` (`bool favoritesGridView = 44; bool favoritesOpenLibrary = 45;`)
- Modify: `datastore/src/commonMain/kotlin/com/programmersbox/datastore/SettingsHandling.kt`

**Interfaces:**
- Produces: `NewSettingsHandling.rememberFavoritesGridView()` (default `false`) and `NewSettingsHandling.rememberFavoritesOpenLibrary()` (default `false`), built the same way as `rememberShowBlur()`.

- [ ] **Step 1: Add the fields and helpers.**
- [ ] **Step 2: Verify** with `./gradlew :datastore:compileKotlinJvm`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** `feat(settings): favorites view mode and last tab`.

### Task 6: Section + filter model (pure)

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/favorite/FavoriteSections.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/favorite/FavoriteSectionsTest.kt`

**Interfaces:**
- Consumes: `FavoriteSearchHit`, `MatchField` (Task 4).
- Produces:
  - `enum class QuickFilter { RECENT, MULTI_SOURCE, UPDATES_OFF }`
  - `enum class FavoriteSort { TITLE, CHAPTERS, ADDED }`. This replaces `SortFavoritesBy`; delete that class.
  - `data class FavoriteGroup(val title: String, val items: List<DbModel>, val hit: FavoriteSearchHit? = null)`, with `val maxChapters`, `val latestCreatedAt`, `val updatesOff` (all items have `shouldCheckForUpdate == false`) and `val sources`.
  - `sealed interface SectionHeader { data class Letter(val char: Char); enum class Added : SectionHeader { THIS_WEEK, THIS_MONTH, EARLIER }; enum class Chapters : SectionHeader { OVER_500, FROM_100, UNDER_100 }; data class Match(val field: MatchField) }`
  - `data class FavoriteSection(val header: SectionHeader, val groups: List<FavoriteGroup>)`
  - `fun groupFavorites(items: List<DbModel>): List<FavoriteGroup>`, grouped by title.
  - `fun QuickFilter.matches(g: FavoriteGroup, now: Long): Boolean`. RECENT: `latestCreatedAt > 0 && now - latestCreatedAt <= 30.days`. MULTI_SOURCE: `items.size > 1`. UPDATES_OFF: `updatesOff`.
  - `fun filterGroups(groups: List<FavoriteGroup>, quick: Set<QuickFilter>, sources: Set<String>, now: Long): List<FavoriteGroup>`. An empty `sources` means all. With sources selected, keep the groups that have any item in `sources`.
  - `fun buildSections(groups: List<FavoriteGroup>, sort: FavoriteSort, now: Long): List<FavoriteSection>`:
    - TITLE: `title.first().uppercaseChar()`, with non-letters bucketed under `'#'` first, in A–Z order.
    - CHAPTERS: max chapters, descending.
    - ADDED: createdAt, descending, with 0 last in EARLIER.
  - `fun buildSearchSections(groups: List<FavoriteGroup>, hits: List<FavoriteSearchHit>): List<FavoriteSection>`: attach to each group its best hit (lowest group ordinal, then rank) among its items' URLs, drop groups without a hit, and section by `Match(hit.group)` in TITLE → DESCRIPTION → NOTE order.

- [ ] **Step 1: Write failing tests** with `now = 1_000 * DAY`:
  - Letter sections are in order, and `'#'` comes first for a title like "86".
  - The chapter buckets split at 500 and 100.
  - The Added buckets split at 7 and 30 days, and `createdAt = 0` falls in EARLIER at the end.
  - RECENT excludes `createdAt = 0`.
  - The source filter matches a multi-source group through any of its sources.
  - Quick filters combine with AND.
  - Search sections are ordered TITLE, DESCRIPTION, NOTE, and hits for URLs that aren't favorites are dropped.
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*FavoriteSectionsTest"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run the same command.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(favorites): section and filter model`.

### Task 7: FavoriteViewModel rewrite

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/favorite/FavoriteViewModel.kt`
- Modify: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/favorite/FavoriteViewModelTest.kt` (update it for the new API; keep the `awaitCondition` pattern)

**Interfaces:**
- Consumes: Tasks 4 and 6.
- Produces: `FavoriteViewModel(sourceRepository, favoritesRepository, searchRepository: FavoritesSearchRepository)` with:
  - `searchText: TextFieldState`, and `sort` (default `TITLE`).
  - `quickFilters: Set<QuickFilter>` with `toggleQuickFilter(q)` and `removeQuickFilter(q)`.
  - `selectedSources: Set<String>` with `toggleSource(s)`, `onlySource(s)`, `clearSources()`, `selectAll(visible: List<String>)`, `selectNone(visible: List<String>)`, `resetFilters()` and `activeFilterCount: Int`.
  - `seeAll(q: QuickFilter)`: sets `quickFilters = setOf(q)`, clears sources, and sets sort to ADDED for RECENT and TITLE otherwise.
  - `sourceCounts: List<Pair<String, Int>>`: every installed source plus every favorite source, with counts, sorted by count descending, then name.
  - `overview: FavoritesOverview(titleCount: Int, sourceCount: Int, chapterCount: Int, recent: List<FavoriteGroup> /* top 10 by createdAt, >0 */, multiSource: List<FavoriteGroup>, updatesOff: List<FavoriteGroup>)`.
  - `isSearching: Boolean`, `sections: List<FavoriteSection>` and `resultCount: Int`.
  - The search pipeline is `snapshotFlow { searchText.text.toString() }.debounce(150).flatMapLatest(searchRepository::search)`, fed into `buildSearchSections` after `filterGroups`.

- [ ] **Step 1: Write failing VM tests:**
  - `seeAll(RECENT)` sets the sort to ADDED and the quick filter.
  - `onlySource` leaves exactly that source selected.
  - `selectNone(visible)` removes only the visible sources.
  - `activeFilterCount` counts sources as 1.
  - Typing "lock" yields one `Match(TITLE)` section after the debounce.
  - A note-only hit for a URL that isn't a favorite yields no section.
  - Filters apply to search results.
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "*FavoriteViewModelTest"`. Expected: FAIL.
- [ ] **Step 3: Implement.** Update the Koin `viewModelOf(::FavoriteViewModel)` if constructor resolution needs it.
- [ ] **Step 4: Run** `./gradlew :kmpuiviews:jvmTest`. Expected: all pass.
- [ ] **Step 5: Commit** `feat(favorites): view model for tabs, filters and FTS search`.

### Task 8: Screen shell, tabs, Overview

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/favorite/FavoriteScreen.kt`. Rewrite the body to hold the search bar, tabs and `SharedTransitionLayout`. Keep the `SourceNotInstalledModal`, the long-press options sheet and the multi-source chooser behavior.
- Create: `.../presentation/favorite/FavoritesOverview.kt`
- Modify: `kmpuiviews/src/commonMain/composeResources/values/strings.xml`

**Interfaces:**
- Consumes: the Task 7 VM and the Task 5 settings.
- Produces:
  - `@Composable internal fun FavoriteCover(group: FavoriteGroup, modifier: Modifier)`: the cover image with the in-screen shared key `FavoriteCoverKey(group.title)` (a private data class), using `sharedBounds(resizeMode = ScaleToBounds())` against the tab/view `AnimatedContent` scope. Tasks 9–10 reuse it. Don't put `OtakuSharedElement` keys on this layer.
  - `FavoritesOverview(overview, onSeeAll: (QuickFilter) -> Unit, onOpenFilters: () -> Unit, onGroupClick: (FavoriteGroup) -> Unit)`.

- [ ] **Step 1: Implement the search bar.** It holds a `TextField` bound to `viewModel.searchText` with the placeholder "Title, description or note", a clear button shown only when there is text, and the filter `IconButton` in a `BadgedBox`. The badge count uses `AnimatedContent` with `scaleIn`. Typing while on Overview sets the tab to Library.
- [ ] **Step 2: Implement the tabs.** The pill indicator offset animates with `motionScheme.defaultSpatialSpec()`. The tab content is `AnimatedContent(tab)` with a slide + fade whose direction follows the tab index. The tab is persisted with `rememberFavoritesOpenLibrary()`.
- [ ] **Step 3: Implement `FavoritesOverview`** to the spec:
  - It has stats, the Recently added shelf, the 2+ sources shelf and the updates-off row.
  - Shelves with no items are hidden.
  - Covers use `FavoriteCover` so that "See all" flies covers into Library.
- [ ] **Step 4: Verify** with `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(favorites): tabs and overview`.

### Task 9: Library list/grid, headers, rail, chips

**Files:**
- Create: `.../presentation/favorite/FavoritesLibrary.kt`
- Modify: `FavoriteScreen.kt` (wire the Library tab), `strings.xml`

**Interfaces:**
- Consumes: `FavoriteCover`, `highlightedText`, and the VM state.
- Produces: `FavoritesLibrary(sections, isSearching, query, gridView: Boolean, onGridViewChange, sort, onSortChange, activeChips, onRemoveQuick, onClearSources, onOpenFilters, onGroupClick, onGroupLongPress)`.

- [ ] **Step 1: Implement the active filter chip row.** Use a `LazyRow` whose items use `animateItem`. Quick filter chips show their label with ×. The sources chip shows the name if one source is selected, or "N sources" if several. Tapping its body opens the sheet, and × clears the sources.
- [ ] **Step 2: Implement the controls row.** It has a `SingleChoiceSegmentedButtonRow` for list/grid (persisted with `rememberFavoritesGridView()`) and one for A–Z/Chapters/Added. While searching, the sort control is replaced by the text "Best match first". Below sits the result line.
- [ ] **Step 3: Implement list view** as a `LazyColumn`:
  - Use `stickyHeader` per section with the header label and count. Rows follow the spec.
  - In search mode, the title uses `highlightedText`, followed by one snippet line labeled "Note"/"Description" (max 2 lines).
  - Every item and header gets `animateItem`.
- [ ] **Step 4: Implement grid view** as a `LazyVerticalGrid` with `adaptiveGridCell()`. Use full-span section headers. Tiles follow the spec, and in search mode show "In …" and the note badge.
- [ ] **Step 5: Implement the letter rail.** Show it only when `!isSearching && sort == TITLE && !gridView && sections.size > 3`. Tap or drag on a letter calls `listState.animateScrollToItem(indexOfHeader)`.
- [ ] **Step 6: Animate the view switch.** `AnimatedContent(gridView)` crossfades the text while `FavoriteCover` animates the bounds. Add the empty states with the exact copy from the spec (search and filters).
- [ ] **Step 7: Verify** with `./gradlew :mangaworld:assembleNoFirebaseDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 8: Commit** `feat(favorites): library list, grid and search results`.

### Task 10: Filter sheet

**Files:**
- Create: `.../presentation/favorite/FavoritesFilterSheet.kt`
- Modify: `FavoriteScreen.kt`, `strings.xml`

**Interfaces:**
- Consumes: `sourceCounts`, the VM filter functions and `resultCount`.
- Produces: `FavoritesFilterSheet(onDismiss, onApply)`.

- [ ] **Step 1: Implement** with `ModalBottomSheet(sheetState = rememberModalBottomSheetState(true))` (see memory: tall sheets need skipPartiallyExpanded):
  - Title and Reset at the top.
  - Show only: `FilterChip`s with counts.
  - The sources label, an `OutlinedTextField` search (local state), and the hide-empty `Switch` (default on) showing the hidden count.
  - "N shown" with All/None.
  - A `LazyColumn` of checkbox rows, using `combinedClickable(onClick = toggle, onLongClick = onlySource)` and `animateItem`.
  - A footer button whose text animates with `AnimatedContent` + `slideInVertically`: "Show N titles" / "No titles match".
  - Apply closes the sheet and, from Overview, switches to Library.
- [ ] **Step 2: Verify** with `./gradlew :mangaworld:assembleNoFirebaseDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** `feat(favorites): filter sheet with searchable sources`.

### Task 11: Device verification + changelog

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Check the installed DB version** per Global Constraints, then install `mangaworld` noFirebase debug with plain adb (memory: adb over ARTEMIS).
- [ ] **Step 2: Walk through and screenshot** with `adb exec-out screencap -p`:
  - the tab switch;
  - each See all;
  - list ↔ grid;
  - every sort;
  - the filter sheet (search, hide-empty, long-press only, All/None, Reset);
  - search "lock", a description word, a note word, and "ock";
  - cover → Details, to confirm the shared transition still works.
- [ ] **Step 3: Confirm the migration on an existing install.** Favorites and notes from before the upgrade must be searchable.
- [ ] **Step 4: Add the CHANGELOG entry**, matching the existing format, and commit `docs(changelog): favorites redesign and FTS5 search`.
