# Mood tags — Design

Date: 2026-11-17
Mockup (approved, v3): https://claude.ai/artifact/FtX9tjhs4ZQt1LP9DsRFGW
Stacked on: describe to search (PR #2718)

## Intent

Favorites carry moods ("cozy", "melancholy", or your own "comfort read"). You can filter favorites by
mood, account statistics show your top moods, and an AI chip can suggest moods on request.

## Decisions (from the user)

- **Nothing generates on its own.** A favorite's Details page has a Mood row under the genres with:
  - **✦ Suggest moods** (the AI chip, with the spark icon). Shown only when the title has no moods,
    an engine is ready, and the `MoodTags` AI switch is on. Tapping it suggests up to 3 moods.
  - **＋ Add**, which opens a sheet listing the 20 built-in moods plus your own, with this title's moods
    selected. Tapping toggles a mood and saves at once. There is no limit when picking by hand.
  - A "Your own mood" field with **＋ Create**, which adds a custom mood to this title. Custom moods
    show with a dashed outline, appear in the sheet for every title, and disappear once no title uses them.
- **Long-press a mood chip to remove it.** There is no Undo. Removing the last mood brings the AI chip back.
- **The AI switch hides only the AI chip.** Moods, ＋ Add, custom moods, the filter and the stats all
  work without AI.
- **Stored only on this device:** no Supabase, no `DbModel` column, no backup.
- **Account statistics** get a "Moods" section: "Tagged favorites: X of Y" plus the top 3 moods with counts.
- The Mood row appears only for favorites.

## Data

- `ItemDatabase` goes from v11 to v12, adding table `mood_tags` (`url TEXT PK`, `moods TEXT NOT NULL` holding a JSON
  array of mood names, `updated_at INTEGER NOT NULL`). `MIGRATION_11_12` creates it.
- A mood is a string: a built-in id ("cozy", "found family"…) or a custom name. Built-in ids are
  shown through localized labels, and anything else shows as typed.
- An empty list deletes the row, so a title with no moods has no row.
- Custom names are trimmed, have their spaces collapsed, are capped at 30 characters, and are deduped
  case-insensitively against built-ins and existing customs.

## AI

`AiFeature.MoodTags : AiFeature<MoodInput>` with `MoodInput(title, description, choices)`. Toggle `MoodTags`, no `nanoTask`.
- `prepare`: title, the first 1,500 characters of the description, and the allowed moods (built-ins plus the user's customs).
- `finish`: split on newline, comma or semicolon, clean up, and lowercase.
- The repository keeps only names in `choices` (case-insensitive), up to 3.

## Built-in moods (20)

cozy, wholesome, heartwarming, funny, lighthearted, romantic, bittersweet, melancholy, dark, tense,
mysterious, spooky, high stakes, action packed, epic, found family, slow burn, thoughtful, inspiring, chaotic.
