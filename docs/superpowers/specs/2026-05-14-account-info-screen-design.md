# AccountInfoScreen Redesign

**Date:** 2026-05-14  
**File:**
`kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/accountinfo/AccountInfoScreen.kt`

## Goal

Replace the current plain list of ~14 separate `CategoryGroup` entries with a structured dashboard
layout: profile identity at top, 3 hero stat chips, activity heatmap, and 4 semantic stat sections.

No database schema changes required. All data comes from existing DAOs via `AccountInfoViewModel`.

---

## Layout Structure (top → bottom)

```
TopAppBar
  ← back button | "Account Info" title

Profile Strip Card
  gradient (M3 primary → tertiary)
  avatar (profileUrl) | email/name | "OtakuWorld member"

Hero Chips Row  [3 chips, equal width]
  Favorites  → M3 primary tint    (totalFavorites, animated)
  Chapters   → M3 secondary tint  (chapters, animated)
  Time Spent → M3 tertiary tint   (timeSpentDoing, string)

── Section: Activity ──────────────────────────
  HeatMapWrapper (full width, inside CategoryGroup card)
  Tap-to-select detail text below heatmap (existing behavior)

── Section: Collection ────────────────────────
  Local Favorites      (localFavorites)           [primary]
  Cloud Favorites      (cloudFavorites)            [primary]  hidden if !Full
  Notifications        (notifications)             [primary]
  Incognito Sources    (incognitoSources)          [primary]

── Section: Discovery ─────────────────────────
  Sources              (sourceCount)               [primary]
  Search History       (history)                   [primary]
  Global Search History(globalSearchHistory)       [primary]
  Saved Recommendations(savedRecommendations)      [primary]
  Lists                (lists, itemsInLists)       [primary]  — supporting text = "X items total"

── Section: System ────────────────────────────
  Blur Hash Cache      (blurHashes)                [primary]
  Translation Models   (translationModels)         [primary]  hidden if NoFirebase
  Logged Exceptions    (exceptionCount)            [error color when > 0]
```

---

## Components

### `ProfileStripCard`

New private composable. Gradient background using `MaterialTheme.colorScheme.primary` →
`MaterialTheme.colorScheme.tertiary`. Displays:

- `ImageLoaderChoice` avatar (36dp, CircleShape, white 25% alpha border)
- Primary text: "OtakuWorld" (static)
- Secondary text: "OtakuWorld member" (static)

No email displayed — `AccountInfoScreen` only receives `profileUrl: String?` and fetching account
email would require Firebase (unavailable in NoFirebase builds). Avatar moves out of the TopAppBar.
TopAppBar becomes back + title only.

### `HeroChipsRow`

New private composable. `Row` with 3 equal-weight `HeroStatChip`s.

### `HeroStatChip`

New private composable. Parameters: `label: String`, `value: String`, `color: Color`,
`containerColor: Color`.

- `ElevatedCard` or `Surface(shape=RoundedCornerShape(10.dp))` with tinted background
- Large bold number/text, small uppercase label below
- Animated via `animateIntAsState` for Int values; pass pre-formatted string for time

### `AccountInfoItem` (existing — keep, minor update)

Add optional `valueColor: Color = MaterialTheme.colorScheme.primary` parameter so exception count
can use `MaterialTheme.colorScheme.error`.

### Section headers

Use `Text` with `MaterialTheme.typography.labelSmall`, `MaterialTheme.colorScheme.onSurfaceVariant`,
uppercase, 0.07em letter-spacing. Emoji prefix + section name. Placed above each `CategoryGroup`.

---

## Section groupings

| Section    | Stats                                                                               |
|------------|-------------------------------------------------------------------------------------|
| Activity   | heatmap, (time spent is in hero chip — not repeated)                                |
| Collection | localFavorites, cloudFavorites (Full only), notifications, incognitoSources         |
| Discovery  | sourceCount, history, globalSearchHistory, savedRecommendations, lists+itemsInLists |
| System     | blurHashes, translationModels (non-NoFirebase only), exceptionCount                 |

### Lists row consolidation

The current two rows ("Lists" count + "Items in Lists" count) collapse into one `AccountInfoItem`:

- Headline: "Lists"
- Supporting: "X items total"
- Trailing: lists count

---

## Color mapping (M3)

| Chip / value           | Color token                                                       |
|------------------------|-------------------------------------------------------------------|
| Favorites chip         | `colorScheme.primary` / `primary.copy(alpha=0.12f)` container     |
| Chapters chip          | `colorScheme.secondary` / `secondary.copy(alpha=0.12f)` container |
| Time chip              | `colorScheme.tertiary` / `tertiary.copy(alpha=0.12f)` container   |
| Stat values (default)  | `colorScheme.primary`                                             |
| Exception count (> 0)  | `colorScheme.error`                                               |
| Profile gradient start | `colorScheme.primary`                                             |
| Profile gradient end   | `colorScheme.tertiary`                                            |

---

## BuildType gates (unchanged)

- `Cloud Favorites` row: only when `appConfig.buildType == BuildType.Full`
- `Translation Models` row: only when `appConfig.buildType != BuildType.NoFirebase`

---

## Animations (unchanged)

All Int stat values continue to use `animateIntAsState`. `timeSpentDoing` is a String — no animation
needed.

---

## Files changed

| File                      | Change                                                                                                                                                                             |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AccountInfoScreen.kt`    | Full rewrite of composable layout; add `ProfileStripCard`, `HeroChipsRow`, `HeroStatChip`; update `AccountInfoItem` with optional `valueColor` param; remove avatar from TopAppBar |
| `AccountInfoViewModel.kt` | No changes                                                                                                                                                                         |
| `favoritesdatabase/`      | No changes                                                                                                                                                                         |

---

## Out of scope

- No new data tracked
- No navigation changes
- No ViewModel changes
- No DB migrations
