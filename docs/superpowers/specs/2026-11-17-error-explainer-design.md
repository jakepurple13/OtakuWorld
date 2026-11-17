# Update check error explainer — Design

Date: 2026-11-17
Mockup: https://claude.ai/artifact/Vh8wQH9PqvCUFZaxNE8MPv (#10)
Stacked on: AI centralization (PR #2714)

## Intent

The "Update check errors" screen shows codes like `HTTP 429` and raw stack traces. This change
adds a plain-language explanation of what went wrong and what the reader can try.

## Decisions (from review with the user)

- **Placement: inside the expanded row.** Tapping a failing row already expands it to show the
  raw error text. The explanation goes above that text. There is no button on every row, because
  one source often has many rows with the same error.
- **Known errors get fixed text, with no AI.** It appears immediately, under a "Common error"
  label on a plain tonal card.
- **Unknown errors get a Nano explanation** in the shared `AiCard`. Generation starts when the row
  is expanded. It runs once per distinct error (source + first line of the error detail), so 20
  rows with the same error generate once. The result is cached for as long as the screen's
  ViewModel lives.
- When no on-device engine is ready, an unknown error shows only the raw text, as it does today.
  No AI control appears and then fails.

## Known errors

| Condition | KnownError | Text (source name = %1$s) |
|---|---|---|
| `FailureKind.TimedOut` | TimedOut | %1$s didn't answer within 15 seconds. It may be slow or down right now. Try again later. |
| Http 429 | RateLimited | %1$s is limiting how fast the app can ask for updates. This usually clears in a few minutes. |
| Http 403, 503 | Blocked | %1$s is blocking automatic requests, often with a Cloudflare check. Opening the title in the app once can help. |
| Http 404, 410 | NotFound | This title's page no longer exists on %1$s. The source may have moved or removed it. |
| Http 500–599 except 503 | ServerError | %1$s's server had a problem. This is on their side, so try again later. |
| CouldNotRead, and the first detail line matches `UnknownHostException`, `Unable to resolve host`, `ConnectException`, `NoRouteToHostException`, `UnresolvedAddressException` or `Network is unreachable` | Offline | Couldn't reach %1$s. Check your internet connection and try again. |

Everything else is unknown and goes to AI.

## AI feature

`AiFeature.ExplainError : AiFeature<ErrorDetails>`, where `ErrorDetails(sourceName: String, detail: String)`.
- `prepare`: `"Source: $sourceName\nError:\n${detail.take(1_500)}"`
- No `nanoTask`, and no Nano length cap.
- System prompt: "You explain why an app couldn't check a manga, anime or novel source for new
  chapters. The reader is not technical. In 2 sentences, say what most likely went wrong and one
  thing they can try. Don't mention code, exceptions or stack traces. Return only the explanation."

## Non-goals

- Explanations on the source-failure banner. It isn't expandable. A possible follow-up.
- New actions such as "Retry in 5 min". The screen's existing Retry stays the only action.
- Saving explanations across screen visits.
