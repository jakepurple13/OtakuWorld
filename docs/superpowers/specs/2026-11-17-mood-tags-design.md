# Mood tags — Design

Date: 2026-11-17
Mockup (#5): https://claude.ai/artifact/Vh8wQH9PqvCUFZaxNE8MPv
Stacked on: describe to search (PR #2718)

## Intent

Favorites get up to 3 moods from a fixed list ("cozy", "melancholy", "found family"…). You can filter
favorites by mood, and account statistics show your top moods.

## Decisions (from the user)

- **Stored only on this device, in its own table.** No Supabase, no `DbModel` column, and no
  backup/restore, because the AI can regenerate them.
- **Tagging is lazy, with no background or bulk work.** Gemma is too heavy for some phones, and Nano
  can't run in the background. A favorite is tagged when it's favorited on Details, or when a
  favorite with no tags is opened on Details.
- **Account statistics** get a "Moods" section: "Tagged favorites: X of Y" plus the top 3 moods with counts.

## Data

- `ItemDatabase` goes from v11 to v12, adding table `mood_tags` (`url TEXT PK`, `moods TEXT NOT NULL`, `created_at INTEGER NOT NULL`).
  - `moods` holds comma-joined mood ids. An empty string means "tagged, no moods", so it is never retried.
  - Manual migration `MIGRATION_11_12` (CREATE TABLE).
- DAO: `moodTagsFlow(url)`, `upsertMoodTags(row)`, and `favoriteMoodTags()` (a join of non-deleted favorites with `mood_tags`, giving url + moods).
- Unfavoriting keeps the row. Stats and filters join with favorites, so the row doesn't count.

## Moods

There are 20 fixed ids: cozy, wholesome, heartwarming, funny, lighthearted, romantic, bittersweet,
melancholy, dark, tense, mysterious, spooky, high stakes, action packed, epic, found family, slow
burn, thoughtful, inspiring, chaotic. Labels come from strings, and the stored id never changes.

## AI feature

`AiFeature.MoodTags : AiFeature<MoodInput>`, where `MoodInput(title, description)`. Toggle `MoodTags`, no `nanoTask`.
- `prepare`: "Title: …\nDescription: <first 1,500 chars>".
- System prompt: lists the 20 moods and says "Pick 1 to 3 that fit best, one per line, exactly as written."
- `finish`: split on newline, comma or semicolon; keep only exact (case-insensitive) mood ids; at most 3; join with "\n".

## Behavior

- **Details:** when the title is a favorite, has no `mood_tags` row, has a description, and the engine
  is ready, tagging runs once per screen. While it runs, a "Mood" row shows a one-line shimmer; when
  it's done, chips pop in. A row with moods shows chips, and long-pressing a chip removes that mood.
- **Favorites filter sheet:** a "Mood" section lists only the moods present among favorites, with
  counts. Selecting moods filters to favorites that have any of them, and they count as an active
  filter. A note reads "N favorites not tagged yet · open them to tag".
- **AI page:** a "Mood tags" row in Library. Off stops new tagging and hides the Details row and the
  filter section, and keeps the stored tags.
- **Stats:** a `MoodStatisticsProvider` with priority after Collection.
