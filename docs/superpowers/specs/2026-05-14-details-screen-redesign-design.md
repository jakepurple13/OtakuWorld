# DetailsScreen UI Redesign

**Date:** 2026-05-14
**Status:** Approved

## Goals

- Elevate the visual design of the details screen with a cinematic hero aesthetic
- Keep dynamic Material theming (palette-driven color via `DynamicMaterialTheme`)
- Two distinct but visually consistent layouts: mobile (portrait/compact) and larger screens (
  expanded width)
- No new features — purely a UI improvement

---

## Constraints

- `DynamicMaterialTheme` wrapping and palette extraction must stay untouched
- All existing functionality preserved: FAB menu, mark-as drawer, swipe-to-dismiss chapters, chapter
  options sheet, add-to-list bottom sheet, snackbar
- KMP — changes must stay in `commonMain` (no Android-specific APIs in layout code)

---

## Affected Files

| File                  | Change                                                  |
|-----------------------|---------------------------------------------------------|
| `DetailsHeader.kt`    | Full hero section redesign (shared by both layouts)     |
| `DetailsPortrait.kt`  | Remove `CollapsableColumn`, adjust scaffold             |
| `DetailsLandscape.kt` | Pass missing `blurHash`/`onBitmapSet`, enhance gradient |

---

## Section 1 — DetailsHeader (shared)

### Current

`DetailsHeader` uses a `Box` where a blurred image fills `matchParentSize` as the background. The
cover art `Surface` and info `Column` sit inside a `Row` on top of it. The banner height is entirely
content-driven (no fixed height anchor).

### Redesigned

**Hero banner:**

- Outer `Box` with `fillMaxWidth()` — no explicit height on the outer container.
- Inner banner layer: `ImageLoaderChoice` with `Modifier.fillMaxWidth().height(180.dp)` +
  `blur(8.dp)` + steeper gradient overlay (`primary.copy(alpha = 0.85f)` → `surface`). This replaces
  the current `matchParentSize` background approach.
- A `Column` (no explicit height) holds the cover+info row, genre chips, and description slot —
  positioned inside the outer `Box` alongside the banner layer.

**Floating cover art:**

- The content `Column` uses `Modifier.padding(top = 110.dp, horizontal = 16.dp)` —
  `110.dp = 180.dp banner − 70.dp overlap`. This pushes the content into the banner's lower region,
  making the cover visually overlap ("float over") the banner bottom edge without absolute
  positioning.
- Inside: a `Row(verticalAlignment = Alignment.Bottom)` holds the cover `Surface` and info `Column`.
- Cover `Surface` uses `elevation = 8.dp`, `shape = MaterialTheme.shapes.medium`, size unchanged (
  `ComposableUtils.IMAGE_WIDTH × IMAGE_HEIGHT`).
- `zoomOverlay()` and shared element modifiers stay on the cover `Surface`.

**Info column:**

- Sits to the right of the cover art inside the same `Row`, `verticalAlignment = Alignment.Bottom`
  so text aligns with the cover base.
- Contents: source label (`labelSmall`), title (`titleMedium`, combinedClickable), favorite button
  row (`Crossfade`), chapter count (`bodyMedium`).

**Below the cover row:**

- `FlowRow` of genre `AssistChip`s — unchanged.
- `possibleDescription` composable slot — unchanged.
- Bottom padding `16.dp` to give breathing room before the chapter list.

**Signature change vs current:**
The banner is now a fixed-height visual anchor. Everything else builds off that anchor via padding,
not content-driven sizing.

---

## Section 2 — Mobile layout (DetailsPortrait)

### Current

- `CollapsableColumn` wraps both `TopAppBar` and `DetailsHeader`, with an expand/collapse toggle
  button in the app bar.
- `DetailsHeader` uses `Modifier.collapse()` to animate away on scroll.

### Redesigned

**Remove `CollapsableColumn`:**

- `OtakuScaffold` stays. The change is inside its `topBar` lambda:
  `CollapsableColumn { TopAppBar + DetailsHeader }` becomes just `TopAppBar { ... }`.
- `DetailsHeader` moves out of the `topBar` slot entirely and becomes the first `item {}` in the
  `LazyColumn` — it scrolls naturally with the list.
- The collapse/expand `IconButton` (`ArrowDropDownCircle`) is removed from `TopAppBar` actions.
- `rememberCollapsableTopBehavior`, `collapsableBehavior`, and
  `nestedScroll(collapsableBehavior.nestedScrollConnection)` on the `Scaffold` modifier are all
  removed.
- `fabBlur` (driven by `fabMenuExpanded`) is unaffected — stays.

**TopAppBar:**

- `containerColor = Color.Transparent` (already set) — stays.
- `zIndex(2f)` stays so it renders above the scrolling content.
- Actions: back button, `DetailActions` (share + dropdown) — unchanged.
- `scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()` stays for the scroll-linked alpha
  effect.

**LazyColumn:**

- `item { DetailsHeader(...) }` as the first item (no `collapse()` modifier).
- `stickyHeader { ChapterListHeader(...) }` — unchanged.
- Chapter items — unchanged.
- `contentPadding`, `ScrollBar`, FAB menu, `SnackbarHost`, `ModalNavigationDrawer` — all unchanged.

**FAB menu:** `DetailFloatingActionButtonMenu` — no change.

---

## Section 3 — Landscape layout (DetailsViewLandscape / DetailsLandscapeContent)

### Current issues

- `DetailsHeader` in the landscape `listPane` is missing `blurHash` and `onBitmapSet` parameters (
  landscape `DetailsViewLandscape` signature doesn't accept them, so they're never passed down).
- Left pane gradient uses `MaterialTheme.colorScheme.primary` → `surface` but could be richer.

### Redesigned

**Signature fix:**

- Add `blurHash: BitmapPainter?` and `onBitmapSet: (ImageBitmap) -> Unit` to `DetailsViewLandscape`
  and `DetailsLandscapeContent`.
- Pass them through to `DetailsHeader` in the `listPane`.

**Left pane gradient:**

- Existing: `Brush.verticalGradient(listOf(c, b))` where `c = primary`, `b = surface`.
- Change to `primary.copy(alpha = 0.85f)` as top stop, `surface` as bottom — matches the new banner
  gradient alpha for visual cohesion.

**Structure:** No structural changes. `ListDetailPaneScaffold`, `NormalOtakuScaffold`,
`DetailBottomBar`, `VerticalDragHandle`, `MarkAsScreen` drawer — all unchanged.

---

## What Does NOT Change

- `DetailsScreen.kt` — state, theme, `DynamicMaterialTheme`, `AnimatedContent`, `DetailsActions`,
  `MarkAsScreen`, `DetailLoading`, `DetailError`, `ChapterItem`, `chapterItemOptions` — all
  unchanged.
- `DetailsUtils.kt` — `AddToList`, `DetailActions`, `ShareButton`, `DetailBottomBar`,
  `DetailFloatingActionButtonMenu`, `ChapterListHeader` — all unchanged.
- `DetailsViewModel.kt` — no changes.
- Dynamic Material theming, palette extraction, amoled mode, theme follow-system — untouched.

---

## Implementation Order

1. Redesign `DetailsHeader` (hero banner + floating cover layout)
2. Update `DetailsPortrait` (remove `CollapsableColumn`, move header into `LazyColumn`)
3. Update `DetailsLandscape` (add missing params, enhance gradient)
