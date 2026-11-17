# Import model file — spec

Approved mockup: https://claude.ai/artifact/LYK87nPyXTZQoQxm6b41XB (2026-11-17).

## Goal

Release builds can't use `adb run-as`, and Gemma 3 1B is gated on Hugging Face, so the app can't
download it. Let users pick a `.litertlm` file they downloaded and have the app copy it into its
models folder, on Android and desktop.

## Behavior

Engine card on the AI page (Settings → Integrations → AI), "Local model" row (Android: the existing
Gemma row; desktop: a new row).

- **No model:** "No model file yet", a tonal **Import model file** button, a **Where do I get a
  model?** link (opens `https://huggingface.co/litert-community/Gemma3-1B-IT`). Desktop also shows a
  drop zone; dropping a file starts the import.
- **Pick:** FileKit picker. Android: any file (unknown extensions have no MIME type). Desktop:
  `litertlm` filter.
- **Wrong file:** name doesn't end in `.litertlm` → error "Not a model file" (no copy).
- **No space:** file size + 200 MB headroom > usable space in the models folder → error
  "Not enough space" with the numbers.
- **Copying:** progress (MB copied of total, %), Cancel. Written as `<name>.part`, renamed when
  complete. Cancel or failure deletes the part file.
- **Checking:** indeterminate bar, "Checking the model…". Runs one tiny generation with the local
  engine on the new file. Failure deletes the copied file and shows "This model didn't load".
- **Ready:** model name, size, Ready, ⋮ menu with **Replace model…** and **Remove**. Replace runs
  the same flow; the previous file is deleted only after the new one passes the check. Remove deletes
  the file and its weight cache files (`cache/models/<file>*`).
- The import keeps running if the user leaves the page (app-scoped), and the page shows its state
  when reopened.
- One imported model at a time. Android files downloaded by AI Helper (Gemma 4 E2B/E4B) are never
  deleted by Replace unless that file is the one shown in the row.

## Engines

- **Android:** `GemmaEngine` unchanged except its fallback: when AI Helper hasn't selected a LiteRT
  model, use the newest `.litertlm` file (was: first listed), so an import takes effect.
- **Desktop:** new `LocalModelEngine` (`SummaryEngineId.Gemma`, jvmMain) on
  `com.google.ai.edge.litertlm:litertlm-jvm:0.18.1` (Koog's JVM LiteRT client is an empty stub).
  CPU backend, cache dir `<userData>/models-cache`, model from `<userData>/models` (AI Helper's
  ModelManager folder). Status Ready when a `.litertlm` file is there, else NeedsSetup. Engine and
  conversation are created per run and closed after, like the Android engine.
- Desktop automatic order becomes `[Gemma, Ollama]`: an imported model is used first.
- The Prefer control stays hidden on desktop (its options name Nano).

## Packaging

`litertlm-jvm` bundles JNI for darwin-aarch64, linux-x86_64/aarch64 and windows-x86_64 (131 MB jar).
Desktop packages strip the JNI folders for other OSes.

## Out of scope

In-app download of gated models, several imported models with a chooser, iOS.
