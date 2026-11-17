# Describe to search — Design

Date: 2026-11-17
Mockup (#3): https://claude.ai/artifact/Vh8wQH9PqvCUFZaxNE8MPv
Stacked on: update summary (PR #2717)

## Intent

On Favorites, a sentence like "that isekai where the guy is a slime" finds Tensura. The AI turns the
sentence into keywords, and the existing FTS5 search over favorites and notes runs them.

## Decisions

- **Spark button** in the search field's trailing area, beside Clear. It shows when the query has 4 or
  more words, an engine is ready, and the new `AiToggle.DescribeSearch` switch is on. It pops in with
  a spring scale.
- **Tapping it** asks the AI for keywords. While generating, the field gets the AI `generatingBorder`.
  The keywords appear as removable chips under the field, labelled "Searching for". Search then uses
  **any** keyword (FTS5 OR), with each keyword matched as all its words (prefix), against favorites
  and notes, merged by url as today.
- **Typing again** clears the keywords and returns to normal word search. Removing the last chip does
  the same.
- **AI failure or no keywords:** nothing changes, and the query stays a normal search.
- Plain typing never calls AI.

## AI feature

`AiFeature.SearchKeywords : AiFeature<String>`, toggle `DescribeSearch`, no `nanoTask`.
- `prepare`: the query, trimmed to 300 characters.
- `userPrompt`: "Description: $prepared".
- System prompt: "You turn a reader's description of a manga, anime or novel into search keywords. Return 3 to
  6 short keywords or title words likely to appear in its title or description, one per line,
  lowercase. No numbering, no explanations."
- `finish`: strip list markers and quotes, lowercase, drop blanks and duplicates, take 6, and join with "\n".

## Search

- `ftsMatchAny(keywords, column)` → `(k1a* k1b*) OR (k2*) …`, with the same token sanitizing as
  `ftsMatch`. Returns null when there are no usable tokens.
- `FavoritesSearchRepository.searchAny(keywords)` uses the same merge as `search`.
