# Favorites Redesign + FTS5 Search — Design

Approved prototype: https://claude.ai/artifact/PQGdG1bmcgQpBL2zueUe8S (direction "E", round 4).
Earlier rounds: https://claude.ai/artifact/Joeh1m5yYwjZPKHHfvwffT (A/B/C), https://claude.ai/artifact/Sv1FxcjZ7o1jF5iAYLVnSf (D/E/F).

## Screen structure

- A search bar at the top holds a search field, a clear button (shown only when there is text), and a **filter button**. The filter button shows a badge with the number of active filters: each quick filter counts as 1, and any source selection counts as 1 in total.
- Under the search bar are two tabs, **Overview** and **Library**. The screen reopens on the tab used last, which is persisted.
- **Overview** has:
  - a stats row: titles, sources, chapters. Tapping the sources stat opens the filter sheet.
  - a "Recently added" shelf of large covers showing how long ago each was added, with a "See all" link.
  - an "On more than one source" shelf with ×N badges and a "See all" link.
  - a "Not checking for updates" row with a count and the first two titles.
  - no source chips.
- **Library** has:
  - a row of active filters as chips you can remove. Several sources collapse into one "N sources" chip, and tapping it reopens the sheet. The row is hidden when no filters are on.
  - a list/grid switch, persisted in one setting that applies to every filter and sort. The default is list.
  - sort buttons for A–Z, Chapters and Added.
  - a result line: "X of Y titles", or "N results for “q”" while searching.
- Library section headers follow the sort:
  - A–Z: by first letter.
  - Added: This week (≤7 days), This month (≤30 days), Earlier.
  - Chapters: 500+, 100–499, Under 100.
- In list view sorted A–Z, a letter rail lets you jump to a section. There is no rail in grid view or for other sorts.
- A **list row** shows a cover thumbnail, the title, a tag for each source, an "updates off" tag when that applies, "Added X ago" when sorted by Added, and the chapter count right-aligned.
- A **grid tile** shows the cover with a ×N badge and a bell-off mark when updates are off. Under it are the title and either "N ch · Source +k" or "Added X ago".
- A title saved from several sources stays one group (grouped by title, as today). Tapping it opens the existing source chooser sheet. Long-press keeps the existing options sheet.

## See all / navigation rules

| Tap on Overview | Library opens with |
|---|---|
| Recently added · See all | quick filter Recently added (createdAt within 30 days), sort Added |
| More than one source · See all | quick filter 2+ sources, sort A–Z |
| Not checking for updates | quick filter Updates off, sort A–Z |

- Tapping × on a filter chip removes that filter.
- Typing in the search field while on Overview switches to Library.
- Applying filters from the sheet while on Overview switches to Library.

## Filter sheet (opened by the filter button)

- The sheet has a title "Filter" and a Reset button, which clears all quick filters and sources and is disabled when none are on.
- **Show only** chips toggle Recently added, 2+ sources and Updates off, each with its count. Quick filters combine with AND.
- **Sources** is a list you can search:
  - A label shows "All" or "N selected", above a search field.
  - A switch "Hide sources with no favorites" is on by default and shows the hidden count.
  - "N shown" has All / None buttons that act on the sources currently visible.
  - Each row has a checkbox, the source name, a language tag and a count.
  - Rows are sorted by count (highest first), then by name. Sources with no favorites are dimmed.
- Tap a row to toggle that source. Long-press a row to show only that source.
- An empty source selection means all sources.
- Changes apply immediately, and the Library behind the sheet updates.
- The footer button reads "Show N titles", or "No titles match" when nothing matches. It closes the sheet.

## Search

- Search covers title, description and the item's note, using SQLite FTS5 with **two indexes merged by URL**:
  - `favorites_fts` in ItemDatabase: external content on `FavoriteItem`, columns title and description.
  - `notes_fts` in NotesDatabase: migrated from FTS4 to FTS5, with external content on `notes`. Only the `content` column is searched.
- Matching uses the `unicode61` tokenizer with accents removed, by word prefix:
  - Each typed word becomes `"word"*`, and all words must match (AND).
  - "blu lo" matches "Blue Lock". "ock" matches nothing.
- An item matches if every word is found in its title+description index, or every word is found in its note. Words are not combined across the two indexes.
- Results are sorted by best match and grouped:
  - **Title matches**: found in the favorites index with a hit in the title.
  - **Description matches**: found in the favorites index with no hit in the title.
  - **Note matches**: found only in the note.
- Within each group, results are ordered by bm25 rank, with title hits weighted 10 against 1 for description.
- Matched words are highlighted, using FTS5 `highlight()` for titles and `snippet()` for description and note.
- A row shows one snippet line. The note snippet is preferred when the note matched, otherwise the description snippet. The line is labeled "Note" or "Description".
- A grid tile shows "In title · description · note" (only the parts that matched) and a note badge when the note matched.
- While searching, the sort buttons are replaced by the text "Best match first". The list/grid switch stays.
- Source and quick filters still apply to search results.
- Empty result copy: **No favorites match “q”** — "Search checks titles, descriptions and your notes. Words match from their start, so “lock” finds Blue Lock but “ock” doesn't."
- The query runs 150 ms after the last keystroke. A blank query shows the normal Library.

## Motion

Use Material 3 Expressive springs from `MaterialTheme.motionScheme`: the default spatial spring for movement and the fast effects spring for fades. No fade overshoots.

| Moment | Behavior | Compose |
|---|---|---|
| Switch tabs | The pill indicator slides. The page slides in from the side you tapped, with a fade. | `animateDpAsState`/`animateFloatAsState` for the indicator; `AnimatedContent(tab)` with `slideInHorizontally + fadeIn togetherWith slideOutHorizontally + fadeOut` |
| See all | Shelf covers fly to their place in the Library. Text crossfades. | `SharedTransitionLayout` around the tab content; `sharedElement` keyed per group title; `animateEnterExit(fadeIn/fadeOut)` |
| List ↔ grid | Each cover morphs from thumbnail to tile. Text crossfades. | `AnimatedContent(viewMode)` inside the same layout; `sharedBounds(resizeMode = ScaleToBounds())` |
| Sort / filter / search | Rows and headers slide into place. Rows that leave fade out. | `Modifier.animateItem()` on every lazy item and sticky header |
| Filter sheet | Sheet spring, checkbox pop, the button's count rolls, the list behind reflows | `ModalBottomSheet`; `AnimatedContent(count)` with `slideInVertically`; `animateItem` |
| Filter chips / badge | Chips scale in and out. The badge pops when its count changes. | `LazyRow` + `animateItem`; `BadgedBox` + `AnimatedContent` + `scaleIn` |

Shared keys used inside the screen must not collide with the nav-level keys `OtakuSharedElement(SharedItemKey)`, so the existing cover → Details transition keeps working.

## Platform

- FTS5 needs SQLite with FTS5 compiled in. Android's built-in SQLite doesn't have it, so Android switches to `BundledSQLiteDriver` (`androidx.sqlite:sqlite-bundled`), which JVM already uses. iOS keeps its default driver, since the system SQLite includes FTS5. iOS compiles but is not verified on a device.

## Out of scope

- Swipe to remove (it was shown in round 2 but is not part of E).
- Sorting by number of sources and the reverse-sort toggle are both removed; the "2+ sources" quick filter replaces the count sort.
