# Update summary — Design

Date: 2026-11-17
Mockup (#1): https://claude.ai/artifact/Vh8wQH9PqvCUFZaxNE8MPv
Stacked on: AI settings page (PR #2716)

## Intent

The Notifications screen opens with a short AI summary of the newest update check, for example:
"Tensura has 3 new chapters, and the titles point to the Walpurgis banquet. Frieren and Dandadan
each have 1." The notification itself stays plain, because Nano can't run in the background.

## Decisions

- **What gets summarized:** notifications created at or after the latest update check's `startedAt`
  that are still in the list. If there is no report, nothing is summarized.
- **When it shows:** at least 2 such notifications, an engine is ready, the `UpdateSummary` switch is
  on, and the filter is All. One update needs no summary.
- **Starts on its own** when the screen opens. The mockup was approved that way, and the card exists
  to be read on arrival.
- **Cached per update check** (`report.finishedAt`) in memory, for the app session. Coming back shows
  the finished text with no shimmer. A new check makes a new summary.
- **Input:** each notification's `summaryText` (e.g. "Tensura · 3 new chapters: Ch 112 – Ch 114"), at
  most 25 lines. The prompt asks for 2 plain sentences and no invented plot.
- **Card:** the shared `AiCard`, labelled "From the last update check", as the first list item above
  the day headers, with `animateItem()`.
- **AI page:** a new `AiToggle.UpdateSummary` row, "Update summary" in the Updates group.

## AI feature

`AiFeature.UpdateSummary : AiFeature<List<String>>`, toggle `UpdateSummary`, no `nanoTask`.
- `prepare`: take 25 lines, joined with "\n".
- `userPrompt`: "New updates:\n$prepared".
- System prompt: "You summarize new chapter and episode updates for a reader. In 2 short sentences, say
  which titles got the most new chapters and anything notable in the chapter names. Use only what's
  given; don't invent plot. Plain prose, no lists, no preamble."
