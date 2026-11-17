# Using a small on-device model (Gemma 3 1B)

The AI features (TL;DR, chapter recap, notes writing tools, list suggestions, error explanations,
describe-to-search, mood suggestions) run on **Gemini Nano** when the phone has it. On phones
without Nano, they can fall back to a **LiteRT-LM** model stored inside the app. Gemma 3 1B is the
recommended one: about 584 MB, and it runs on the CPU of low-end phones.

The update summary on the Notifications screen works with Gemma, but Gemma 3 1B writes poor
summaries (it ignores the format and can invent plot details).

Android only. On desktop, the AI features use Ollama (AI Helper settings).

## 1. Download the model

Gemma is gated on Hugging Face, so the app can't download it for you.

1. Sign in at <https://huggingface.co> and open
   <https://huggingface.co/litert-community/Gemma3-1B-IT>.
2. Accept the Gemma license on that page.
3. Under **Files and versions**, download `gemma3-1b-it-int4.litertlm`.

Any other `.litertlm` text model works the same way (for example
`litert-community/Qwen3-0.6B`). The app picks up every `.litertlm` file in its models folder.

## 2. Copy it into the app

The model must be in the app's private folder `files/models/`. Getting a file there needs
`adb` and a **debug build**: `run-as` only works on debuggable apps.

| App        | Debug package id (noFirebase flavor)            |
|------------|-------------------------------------------------|
| MangaWorld | `com.programmersbox.mangaworld.noFirebase.debug` |
| AnimeWorld | `com.programmersbox.animeworld.noFirebase.debug` |
| NovelWorld | `com.programmersbox.novelworld.noFirebase.debug` |

Install and open the app once first, then run the following. Replace `<serial>` with the
device from `adb devices` and `<package>` with the id above:

```bash
adb -s <serial> push ~/Downloads/gemma3-1b-it-int4.litertlm /data/local/tmp/
adb -s <serial> shell run-as <package> mkdir -p files/models
adb -s <serial> shell run-as <package> cp /data/local/tmp/gemma3-1b-it-int4.litertlm files/models/
adb -s <serial> shell rm /data/local/tmp/gemma3-1b-it-int4.litertlm
```

Check the copy (the size should match the download, 584417280 bytes for Gemma 3 1B):

```bash
adb -s <serial> shell run-as <package> ls -l files/models
```

Force-stop the app and open it again.

## 3. Check it in the app

1. Open **Settings → Integrations → AI**.
2. The engine card lists the model (for example "Gemma3 1b It Int4 · Ready").
3. **Prefer** decides which engine runs: *Automatic* uses Nano when it's ready and Gemma
   otherwise; *Gemma* always uses Gemma.
4. Tap **Run sample**. The first run loads the model (about 8–10 seconds on a low-end phone);
   later runs take a few seconds.

The first load also writes a prepared copy of the weights to the app's cache
(`cache/models/*.xnnpack_cache_*`). Later loads use it and need much less memory. Clearing the app's
cache only makes the next load slow again.

## Removing the model

```bash
adb -s <serial> shell run-as <package> rm files/models/gemma3-1b-it-int4.litertlm
```

Clearing the app's storage in Android settings also removes it.
