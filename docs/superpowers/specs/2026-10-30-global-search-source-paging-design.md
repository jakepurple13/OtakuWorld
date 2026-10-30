# Global Search: per-source screen with infinite scroll

**Date:** 2026-10-30
**Status:** Approved (HTML preview v2)

## Goal

From Global Search, pick one source and keep scrolling its search results page after page.

## Design

- The Global Search results list is unchanged (source rows with carousels, page 1 only).
- Tapping a source row or its count opens a new screen, `Screen.SourceSearchScreen(serviceName, query)`.
  The bottom sheet that used to show a source's results is removed.
- The new screen:
  - Top bar: back, source name + language badge, subtitle `query · N results · page P`.
  - Adaptive grid of `SearchCoverCard`s. Tap opens Details (biometric/incognito aware), long-press opens the options sheet.
  - A full-width "Page N" divider before each page after the first.
  - Next page loads when the end of the grid comes into view (`InfiniteListHandler`).
  - States at the bottom: placeholder covers while loading, error text + Retry, "That's everything" at the end.
    First page empty: "No results" message.
  - Scroll-to-top FAB once scrolled past the first rows.
- `SourceSearchViewModel` owns paging. It lives with the Nav3 entry, so going to Details and back keeps
  pages and scroll position.

## Paging rules

- Page N is fetched with `KmpApiService.searchListFlow(query, N, emptyList())`.
  `searchSourceList` is not used here because it swallows errors into an empty list, which would look
  like the end of results instead of a failure.
- Items whose `url` was already loaded are dropped.
- End of results: a page that returns no new items (empty, or only duplicates). Sources that ignore the
  page number therefore stop after page 2.
- A failed page keeps loaded results, records the exception to `ExceptionDao`, and shows Retry.
- Page 1 is fetched again on the new screen rather than handed over from Global Search. This keeps nav
  keys small and behaves the same after process death.
- Source lookup waits for `SourceRepository.sources` to contain the `serviceName`, so a restored back
  stack works once extensions finish loading.

## Out of scope

- Feeding extra pages back into the Global Search list.
- A search field on the per-source screen.
