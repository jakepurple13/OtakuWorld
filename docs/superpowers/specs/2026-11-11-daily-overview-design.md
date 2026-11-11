# Daily Overview — Design

Date: 2026-11-11
Branch: `worktree-daily-overview`
Approved mockup: https://claude.ai/artifact/11rG7q8iYEf8XzwP5TbP28 (version 2)
Builds on: `2026-11-10-more-quick-actions-design.md` (quick actions card, `QuickAction` enum)

## Intent

The user wants a once-a-day recap of **yesterday**. It shows how much they read or watched, what
updated, and a short summary written by an AI model. The summary runs on the device when possible.
It opens from quick actions as its own full screen. It can also arrive as a morning notification
at a time the user picks.

## Goals

1. A new full screen, **Daily overview**, reached from a new quick actions row.
2. Layout from mockup proposal A plus the hour graph from proposal B. From top to bottom:
   - AI summary card.
   - Three stat tiles.
   - "By hour" bar graph.
   - "You read" list.
   - "Updated overnight" list.
3. The summary text comes from a **summary engine**. The Android fallback order is:
   1. Gemini Nano.
   2. Gemma through Koog LiteRT, using the model the AI Helper downloaded.
   3. The Koog AI Helper provider, only when the user chooses it explicitly.
   4. Numbers only (a template sentence).
4. Desktop uses Ollama through Koog. Its fallback is numbers only.
5. Settings page:
   - Feature on/off.
   - Morning notification on/off.
   - Notification time.
   - Skip quiet days.
   - Summary engine.
6. Morning notification (Android) at the chosen time. It has "Open overview" and "Continue <title>" actions.

## Non-goals

- No paging to older days. The overview covers yesterday only.
- No time-spent (minutes) numbers. The overview shows counts only.
- No separate Gemma model choice or download for this feature. The AI Helper's model is used.
- No desktop morning notification. Desktop gets the screen and the settings without the
  notification section.
- No iOS work beyond keeping `commonMain` compiling (iOS uses the template engine).
- No cloud sync of the cached summary.

## Anti-goals

- **Never send data to a cloud provider unless the user chose it.** "Automatic" must never pick
  the AI Helper's cloud provider. It may pick Ollama on desktop, because Ollama runs locally.
- The model must not invent numbers. Every count on screen and in the notification comes from the
  DB. The model only writes prose from a facts object we give it.
- No second Koog configuration screen. Gemma and provider setup stay in the AI Helper. This
  feature links there.
- The screen must never hang on generation. Stats and lists render right away, and the summary
  card fills in when the text is ready.

## Data: `DailyFacts`

Built in `commonMain` from existing tables for a `LocalDate` (yesterday in the system time zone):

| Field | Source |
|---|---|
| `chaptersRead: Int` | `HeatMapItem.count` for the date (already incremented on chapter open in `DetailsScreen` and the manga reader) |
| `titles: List<TitleRead>` | `ChapterWatched` rows whose `createdAt` falls on the date, grouped by `favoriteUrl`, joined to `FavoriteItem`/`RecentlyViewed` for title, cover, source |
| `TitleRead.firstChapter/lastChapter/count/caughtUp` | chapter names from the grouped rows; `caughtUp` = newest watched chapter equals the newest chapter in the favorite's stored list |
| `readsByHour: IntArray(24)` | hour of each `ChapterWatched.createdAt` on the date |
| `updates: List<UpdateItem>` | `NotificationItem` rows whose `createdAt` falls on the date (or after midnight today, up to now) |
| `failedSources: List<String>` | the last update check's errors (existing "Update check errors" data) |

### Required change: read timestamps

`ChapterWatched.createdAt` defaults to `0`, and no insert sets it, so reads have no time today.
`FavoritesRepository.addWatched` will set `createdAt` and `updatedAt` to now before insert.
Re-reading a chapter replaces the row (`OnConflictStrategy.REPLACE`), which moves its time to the
latest read. That's fine for a "yesterday" view. No schema change and no migration.

Old rows keep `createdAt = 0` and never match a day. When no row on the date has a time, the
graph card is hidden, the stat tiles still show the heat map count, and the "You read" list is
hidden.

## Summary engines

```kotlin
interface SummaryEngine {
    val id: SummaryEngineId          // Nano, Gemma, Provider, Ollama, Template
    suspend fun status(): EngineStatus   // Ready, NeedsSetup(reason), Unavailable
    fun summarize(facts: DailyFacts): Flow<String>   // streamed text chunks
}
```

- **Nano** (`androidMain`): ML Kit GenAI Prompt API (`com.google.mlkit:genai-prompt`).
  `checkStatus()` maps AVAILABLE → Ready and DOWNLOADABLE → NeedsSetup ("Downloading"). The
  download is triggered and waited for in the background. UNAVAILABLE → Unavailable. Text
  streams from `generateContentStream`.
- **Gemma** (`androidMain`, through `koogintegration`): `ModelManager.listModels()` finds a
  downloaded LiteRT Gemma file. If the AI Helper has LiteRT selected, its model is preferred.
  Otherwise the first downloaded Gemma file is used. The engine builds the client the way
  `PlatformAgents.isPlatformProvider` does. Nothing downloaded → NeedsSetup ("Download a model
  in AI Helper").
- **Provider** (Android): `AgentMaker.getAgentInfo()` when the AI Helper company is not LiteRT
  and not None. Never chosen by Automatic.
- **Ollama** (`jvmMain`): `AgentMaker.getAgentInfo()` when the AI Helper company is Ollama. Not
  reachable → Unavailable ("Ollama isn't running").
- **Template** (`commonMain`): builds the sentence from `DailyFacts`. Always Ready.

**Automatic** resolves to the first Ready engine:
- Android: Nano → Gemma → Template.
- Desktop: Ollama → Template.

An explicit pick that isn't Ready falls back to Template. The summary card then shows a
"Set up AI summary" chip that opens the engine page.

Prompt: a short system prompt plus `DailyFacts` serialized as JSON. It asks for 2–3 sentences in
second person, plain words, and no numbers that aren't in the facts. It uses the app noun
("chapters" or "episodes") from the app's `GenericInfo`.

Caching: one `DailySummary(date, engineId, text)` row per date, stored in DataStore as JSON. One
entry is enough, because the overview is yesterday only. Opening the screen shows the cached text
when the date matches. "Regenerate" bypasses the cache.

## Screens

### Quick actions row

Add `QuickAction.DailyOverview(QuickActionGroup.GoTo)`, the first item in "Go to". Subtitle:
"Yesterday: <n> chapters" from the heat map, or "Your recap of yesterday" when n is 0. The row is
hidden when the feature is off. On desktop it also appears in `⌘K` and the Actions menu through
the shared list (existing rule).

### Daily overview screen

- Nav3 key `DailyOverview`. The quick actions row grows into it as a container transform:
  `SharedTransitionLayout` + `sharedBounds`. The floating nav hides while the screen is open.
- Top bar: back button, "Yesterday" with the full date under it, and a settings gear.
- Summary card:
  - The engine label: "Gemini Nano", "Gemma 4 E2B · on device", "<Provider> · via Koog",
    "Ollama · <model>", or "Yesterday at a glance" for the template.
  - A shimmer placeholder until the first text arrives.
  - The text fades in chunk by chunk while it streams.
  - An animated gradient border that shows only while text is generating.
  - Footer: a privacy line ("On device · nothing sent" or "Sent to <provider>") and Regenerate.
- Stat tiles: chapters read, titles, new updates. Digits roll with `AnimatedContent`.
- Hour graph: 24 bars, the peak hour tinted tertiary, labels "12a 6a 12p 6p 11p", and
  "most at <hour>". Bars grow with an `Animatable` per bar and a 25ms stagger.
- "You read": a row per title with cover, name, "Ch. a to b", a "caught up" tag, a progress bar
  that grows in, and a count badge. Tapping a row opens Details.
- "Updated overnight": the first 3 updates plus failed sources (error badge). "All <n>" opens
  Notifications.
- First load: items stagger in with `animateItem()` and about a 40–55ms index delay.
- Empty day (nothing read, nothing updated): an illustration with "Nothing happened yesterday",
  and no summary call.

### Daily overview settings

Opened from the gear and from Settings › Behavior (also found by the settings search).

- A hero switch row, "Use daily overview". When off, every section below collapses with
  `AnimatedVisibility(expandVertically)`.
- Morning notification group (Android only):
  - Switch.
  - Time row (M3 `TimePicker` in a dialog), default 8:00 AM.
  - "Skip quiet days" switch, default on.
  - The time and skip rows collapse when the notification is off.
- Summary group:
  - "Summary engine" row showing the current choice.
  - With Automatic, the row also shows what it resolves to, e.g. "Automatic · Gemma 4 E2B", or
    "(not ready)".
- Foot note: "Numbers always come from your history on this device. The engine only writes the
  sentences."

### Summary engine page

Selectable cards with radio dots. The selected card expands its options with
`animateContentSize()`.

- Android cards:
  - **Automatic**, with the line "Right now: <resolved>".
  - **Gemini Nano**: an "Available" tag, or a disabled "Not available on this phone" tag.
  - **Gemma on this phone**: the AI Helper's model name and Ready tag, or "No model downloaded"
    with a "Set up Gemma in AI Helper" button (`KoogSettings` route, where a LiteRT model is
    picked and downloaded).
  - **Your AI helper**: shows the provider and model. A warning box says what is sent: titles,
    chapter numbers and update counts. Disabled when no provider is set, with a "Set up AI
    Helper" button.
  - **Numbers only**.
- Desktop cards: **Automatic**, **Ollama** (model name, or "Not running" / "Set up in AI
  Helper"), **Numbers only**.
- A **Test** card at the bottom streams a sample summary from the selected engine, built from
  sample facts.

### Morning notification (Android)

- A new channel, "Daily overview", with low importance.
- WorkManager: unique periodic work (24h) with an initial delay to the chosen time minus about
  10 minutes. The worker builds the facts, generates a summary (or reads the cache), saves it,
  and posts the notification at the chosen time. Changing the time or toggling the switch
  re-enqueues the work (`ExistingPeriodicWorkPolicy.UPDATE`). Turning the feature or the
  notification off cancels it.
- With "Skip quiet days" on, a day with 0 reads and 0 updates posts nothing.
- Collapsed: title "Yesterday: <n> chapters, <m> updates". Body: the first sentence of the summary.
- Expanded (`BigTextStyle`): the full summary.
- Actions:
  - "Open overview" deep-links to `DailyOverview`.
  - "Continue <title>" opens the last title read.
- Respects the POST_NOTIFICATIONS permission. If the permission is denied, the settings row shows
  "Notifications are off for this app" with a button to grant it.

## Settings storage

New DataStore keys (`datastore` module, alongside existing settings):

| Key | Type | Default |
|---|---|---|
| `dailyOverviewEnabled` | Boolean | true |
| `dailyOverviewNotify` | Boolean | false (user opts in) |
| `dailyOverviewTime` | Int minutes of day | 480 (8:00) |
| `dailyOverviewSkipQuiet` | Boolean | true |
| `dailyOverviewEngine` | String enum | `Automatic` |
| `dailyOverviewCache` | String JSON | "" |

## Module placement

- `kmpuiviews/commonMain`:
  - `DailyFacts` builder.
  - `SummaryEngine` interface and `TemplateEngine`.
  - Screen, ViewModel, settings screens and Nav3 entries.
  - The quick action row.
- `kmpuiviews/usesJvmMain` (Android + JVM; already depends on `koogintegration`): the Koog-backed
  engines (Gemma, Provider, Ollama), since `koogintegration` is only available there.
- `kmpuiviews/androidMain`:
  - `NanoEngine`.
  - The worker and notification.
  - The ML Kit dependency.
- `koogintegration`: expose small helpers (`downloadedLiteRtModels()` and the selected
  company/model) without changing the AI Helper screens.

## Testing

- `jvmTest`:
  - `DailyFacts` builder with an in-memory Room DB: date boundaries in the local time zone,
    grouping, caught-up, rows with `createdAt = 0` excluded, and the hour histogram.
  - Engine resolution table for every combination of statuses (Automatic never picks Provider).
  - Template text.
  - Prompt contains only fact numbers.
- ViewModel tests: cached vs fresh, Regenerate, the empty day skipping the engine, and the engine
  failing mid-stream (falls back to the template and keeps the stats).
- Android device checks (the cdm-acquired device):
  - The quick action opens the screen with the transform.
  - The notification posts at a time set 2 minutes ahead.
  - The feature off hides the row and cancels the work.
  - Gemma path with a downloaded model.
  - Nano path only if a supported device is available; otherwise unverified, reported as such.
- Desktop: Ollama running vs stopped.

## Open risks

- Gemini Nano runs on a narrow device list. The main test phone (SM-A156U1) likely won't have it,
  so the Nano path may stay unverified on hardware.
- Gemma via LiteRT on mid-range phones may take tens of seconds. That is why the notification
  generates ahead of time and the screen uses the cache.
- The WorkManager periodic timing can drift by minutes. Acceptable for a morning summary.
