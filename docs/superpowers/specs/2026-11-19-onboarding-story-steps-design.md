# Onboarding: Story Steps — Design

Date: 2026-11-19
Status: Approved (HTML previews approved by user 2026-11-19)
Previews: `docs/superpowers/specs/2026-11-19-onboarding-story-steps-preview.html`
(published: https://claude.ai/artifact/Qvo6yrTYX7h5VgeVQT7mPE). Proposal comparison:
https://claude.ai/artifact/4i2X22E1xoagRLtfrUJLxd (A was chosen).

## Problem

The current onboarding (`kmpuiviews/.../presentation/onboarding/`) is a pager of mixed pages.
"General" mixes grid, navigation and update checks. Settings are not grouped by what people
change first, and there is little motion.

## Goal

A linear 9-step flow, one topic per step, with one motion moment per step and a shared frame
(progress bar, morphing hero shape, Back/Next) that stays put while step content swaps. Same
flow on MangaWorld, AnimeWorld and NovelWorld; step 5 is app-specific. Android, Desktop (JVM)
and iOS share the code in `kmpuiviews` commonMain.

## Steps

| # | Id | Title | Contents |
|---|----|-------|----------|
| 1 | welcome | Welcome to {appName} | One line ("Your library, set up your way. About a minute, and you can change all of it later."), 5 genre cards dealt into a fan, "Coming up" chips (Look, Layout, Sources, {Reader/Player}, Sync, Updates). Next reads "Let's go". |
| 2 | look | Make it yours | Theme System/Light/Dark; color swatches Dynamic, Blue, Red, Green, Yellow, Cyan, Magenta; AMOLED (only when dark); "More look options" row opens a sheet: expressiveness, color-blind mode, cover-tinted details, Ugly/MyEyes colors. |
| 3 | layout | Arrange your shelf | Mini live preview (top bar, cover grid, nav bar); grid Full adaptive/Adaptive/Fixed; Floating navigation; middle button Notifications/Lists/Favorites/Search ("Multiple" stays in Settings). |
| 4 | sources | Plug in sources | Explain only. Body: "{appName} doesn't come with any titles. Extensions connect it to sites that have them, and you choose which to add." "How it works": 1 Find (Settings › Sources & Extensions › Get extensions), 2 Install (Tap Install on one. Android may ask once to allow installs from this app.), 3 Browse (Its titles show up in Recent, All and Search. Switch sources from the source picker.). "Where extensions come from", per app (below). Closing line: "You'll add extensions after setup, from Settings › Sources & Extensions." **No buttons, no language picker.** |
| 5 | app | per app (below) | Contributed by each app. |
| 6 | sync | Keep it safe | Sync with Supabase ("Set up" → `SupabaseRoutes`, back returns here, then shows "On"); Restore from backup (.zip picker → `importFullBackup`, toast with count); Biometric lock row (Android only) — there is no global biometric setting, so the row opens Privacy & Security (`navActions.security()`), as today's Account page does. |
| 7 | updates | Never miss a {chapter/episode} | Master switch "Check for updates" (`shouldRun`). When on: interval dial + slider 1–24 h, network Any network/Wi‑Fi only, Only when charging, Skip on low battery. When off: options collapse; note "You can still pull to refresh on Notifications. Turn checks on anytime in Settings › Notifications." "Notify on boot" stays in Settings. |
| 8 | tips | Hidden gestures | One practice cover: press and hold opens quick menu (Favorite, Add to list, Share). Hint changes after success. Pager swipe disabled while practicing (existing `practicingTip`). |
| 9 | done | You're all set | Summary rows (Look, Layout, {Reader/Player}, Sync & privacy, Updates) with chosen values; confetti; main button "Start reading" (manga, novel) / "Start watching" (anime) sets `hasGoneThroughOnboarding` and leaves. |

Genre cards (icon + name on gradient card):
- MangaWorld: Action, Romance, Mystery, Comedy, Fantasy
- AnimeWorld: Action, Romance, Sci-fi, Slice of life, Fantasy
- NovelWorld: Fantasy, Romance, Mystery, Isekai, Adventure

"Where extensions come from":
- All apps: OtakuWorld catalog — "Built in, ready to browse" (badge "Built in")
- MangaWorld: Mihon & Tachiyomi repos — "Most manga extensions work here"
- AnimeWorld: Aniyomi repos — "Anime extensions"; CloudStream — ".cs3 plugins"
- Custom repos — "Paste a repo link": only when the app has a custom bridge (`hasCustomBridge`)
- Confirm in code which catalogs each app registers; the list must be driven by what is registered.

### Step 5 per app

All app steps put a live preview in a sticky header; controls scroll under it.

**MangaWorld — "Set up the reader"** (body: "Pick how pages move. More options live in Settings › Reader.")

| Setting | Control | Stored |
|---|---|---|
| Reader type | List / Pager / Flip / Curl | `readerType` |
| Space between pages | Slider 0–16 dp, default 4 | `pagePadding` |
| Page number | Chips: Classic, Rolling pill, Scrubber, Edge rail, Story segments, Printed folio, Morphing pill, Line only | `pageIndicatorStyle` |
| Floating bottom bar | Switch | `useFloatingReaderBottomBar` |
| Pages button in the bar | Switch, visible only with floating bar | `hidePageNumberInBar` (inverted) |
| Swipe for chapter list | Switch | `allowUserDrawerGesture` |
| Include insets | Switch | `includeInsetsForReader` |
| Image loader | Kamel / Glide / Telephoto / Panpf + hint "Telephoto and Panpf add pinch-to-zoom." | `imageLoaderType` |

Preview reacts: pages animate per reader type with the chosen gap; page indicator swaps (real
indicator composables); bar docks/floats; "3 / 24" chip shows per Pages setting; turning on
chapter swipe makes the drawer peek in once. Left out: `readerWidthFraction`.

**AnimeWorld — "Set up the player"** (body: "Pick the player look. More options live in Settings › Player.")

| Setting | Control | Stored |
|---|---|---|
| Player style | Classic / Floating / Theater | `video_player_style` |
| Blur player controls | Switch | `video_player_blur` |
| Subtitles on automatically | Switch | `SubtitleStyle` |
| Preferred language | Chips, multi-select | `SubtitleStyle` |
| Size | Slider 50–200 %, step 10 | `SubtitleStyle` |
| Font | Default / Bold / Serif / Mono | `SubtitleStyle` |
| Color | 6 swatches (SubtitleStyle colors) | `SubtitleStyle` |
| Edge | None / Outline / Shadow / Raised | `SubtitleStyle` |
| Background | Slider 0–100 %, step 5 | `SubtitleStyle` |
| Raise from bottom | Slider 0–30 | `SubtitleStyle` |
| Use styles from subtitle file | Switch | `SubtitleStyle` |
| Skip signs and songs tracks | Switch | `SubtitleStyle` |
| Battery alert | Slider 5–50 %, default 20 (Android) | `batteryPercent` |
| Gestures | Info rows only: drag sides for brightness/volume; double-tap to play/pause | — |

Preview: still frame with real player chrome in the chosen style (blur on/off) fading on a loop,
and a subtitle line rendered with the current subtitle style. Left out: use new player, ignore
SSL, desktop Whisper generation.

**NovelWorld — "Reading comfort"** (body: "Set a text size you can read for hours."): text-size
slider 12–24 with a live sample paragraph. NovelWorld has no text-size setting today, so this
adds `novelTextSize` (default 16 sp) and makes the novel reader use it.

## Shared frame

- **Progress:** 9 segments; current segment weight 2.4× and filled; earlier filled.
- **Hero shape:** 64 dp (104 dp at Expanded), `primaryContainer` with step icon. Morphs + rotates
  between steps; icon pops in counter-rotated. Hidden (scale 0) on Welcome; on Done becomes a
  circle, spins 360°, scales 1.15, icon = check.
  Shapes: Look `Cookie9Sided` −14°, Layout `Diamond` 45°, Sources `Arch`, App `Clover4Leaf` 22°,
  Sync `Bun`, Updates `Sunny` −30°, Tips `Flower` 16°, Done `Circle`.
- **Content transition:** keep `HorizontalPager` (swipe stays) with `Modifier.pagerStagger` per row,
  45 ms stagger from travel direction.
- **Back / Next:** Back disabled on step 1. Next label "Let's go" (step 1) / "Next" / app start verb
  (step 9); width animates.
- **Skip** (top bar, hidden on Done): opens dialog. Title "Skip setup?", text "{appName} will start
  with default settings. You can run setup again from Settings › About › View Onboarding Again.",
  buttons "Keep going" (dismiss) and "Skip" (sets `hasGoneThroughOnboarding`, pops). Icon on top.
- **System back:** previous step; on step 1 leaves onboarding (current behavior otherwise).
- **Desktop:** ← / → change step.
- **Reduce motion** (animator scale 0 / system setting): shapes snap, no confetti, no stagger,
  no looping demos; content still crossfades.

## Motion moments

| Step | Moment | Implementation |
|---|---|---|
| Welcome | Cards deal in from 160 dp below to ±9° fan, 90 ms apart, 120 ms initial delay | per-card `Animatable`, `spring(MediumBouncy, StiffnessLow)`, `graphicsLayer` |
| Look | New theme sweeps out as a circle from the tapped swatch, 700 ms | snapshot old frame (`rememberGraphicsLayer`), draw over new theme clipped by growing circle |
| Look | Selected swatch circle → rounded square, check pops | `animateDpAsState` corner, `AnimatedVisibility(scaleIn)` |
| Layout | Grid reflow | `LazyVerticalGrid` + `Modifier.animateItem()` |
| Layout | Nav dock ↔ float | `animateDpAsState` padding + corner |
| Layout | Middle icon pop | `AnimatedContent(scaleIn + rotate)` |
| Sources | How-it-works highlight cycles 1→2→3 every 1.8 s; current row tonal, number square → circle | `LaunchedEffect` loop, `animateColorAsState`, `animateDpAsState` |
| App | Sticky preview, live demo loops | `stickyHeader`, `rememberInfiniteTransition` |
| Sync | Button → "On" pill; lock shackle drops shut | `AnimatedContent`, spring offset |
| Updates | Options fold when master off; dial arc springs; bell rings every 2.4 s when on | `AnimatedVisibility(expand/shrinkVertically)`, `CircularProgressIndicator(progress = {})`, infinite keyframes |
| Tips | Hold traces outline over 500 ms; menu scales from card, items 50 ms apart | `pointerInput`, `Animatable`, popup `scaleIn(transformOrigin)` |
| Done | Hero morph + spin; ~90 confetti pieces in theme colors, 1.9 s; summary rows stagger | `Morph`, `Canvas` + `withFrameNanos` |

## Layout by width

- Compact/Medium (< 840 dp): single column.
- Expanded (≥ 840 dp) and desktop: two panes inside each page. Left ~45 %: hero (104 dp), title one
  size up, body. Right ~55 %: controls (scroll independently; app-step preview sticky in this pane).
  Progress bar spans the top; Back/Next at the bottom.

## Settings regroup

| Group | Settings | Today |
|---|---|---|
| Look | theme mode, color, AMOLED; More: expressiveness, color-blind, cover-tinted details | Theme page |
| Layout | grid type, floating nav, middle nav | General page (moved) |
| Sources | explainer only | Sources page (language picker removed from onboarding) |
| App step | per app above | app pages (player subtitles, novel step new) |
| Sync & privacy | Supabase, restore, biometric | Account page |
| Updates | shouldRun, interval, network, charging, battery | General page (moved) |
| Tips | one practice gesture | Tips page |

## Out of scope

Settings screens themselves; onboarding content for the `app` (OtakuWorld manager) module; new
settings other than the novel text size (if missing).

## Open items to resolve in code

- Which extension catalogs each app registers (drives the "Where" list).
- Exact Android install prompt wording (copy stays general).
- `MaterialShapes` availability in the current Compose Multiplatform material3 version (not used in
  the repo yet); fall back to `androidx.graphics:graphics-shapes` `RoundedPolygon` if missing.
