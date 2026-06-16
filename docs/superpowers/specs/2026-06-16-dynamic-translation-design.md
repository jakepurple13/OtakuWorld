# Dynamic Translation Feature — Design Spec

**Date:** 2026-06-16
**Module:** `:kmpuiviews:koogintegration:dynamictranslation`
**Branch:** `feat/dynamic-translations`

---

## Overview

A KMP module that takes an image containing text, runs OCR to extract text and bounding boxes, translates the text to a target language using a local model, inpaints the original text out of the image using OpenCV, renders the translated text at the original positions, and returns both the modified image and structured translation data.

All processing is local/on-device. No cloud APIs, no GMS dependencies.

---

## Use Cases

1. User provides an image with foreign text and a target language → receives image with translated text overlaid at original positions plus structured coordinate data.
2. Android user without GMS translates text in a photo using fully on-device processing.
3. JVM desktop user batch-translates scanned documents, receiving modified images and structured data.
4. Developer uses only `output.translations` (translated text + bounding boxes) and ignores the image output.
5. PNG input → PNG output; JPEG input → JPEG output.

---

## Out of Scope

- Cloud OCR or cloud translation APIs
- Google Mobile Services dependencies anywhere in the Android implementation
- Real-time video / camera stream translation (static images only)
- Model training or fine-tuning
- iOS or non-Android/non-JVM platforms
- Font style-matching of original text
- Unit tests

---

## Module Structure

Single KMP Gradle module using `otaku-multiplatform-no-ios` plugin (matching `customscraper` pattern):

```
kmpuiviews/koogintegration/dynamictranslation/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/
    │   ├── model/          ← data models
    │   ├── strategy/       ← strategy interfaces
    │   ├── tool/           ← TranslateTool (Koog tool)
    │   └── agent/          ← DynamicTranslationAgent + expect factory
    ├── androidMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/
    │   └── strategy/       ← AndroidOcrStrategy, AndroidTranslationStrategy, AndroidRenderStrategy
    └── jvmMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/
        └── strategy/       ← JvmOcrStrategy, JvmTranslationStrategy, JvmRenderStrategy
```

`settings.gradle.kts` addition:
```kotlin
include(":kmpuiviews:koogintegration:dynamictranslation")
```

---

## Data Models (`commonMain`)

```kotlin
data class BoundingBox(val x: Int, val y: Int, val width: Int, val height: Int)

data class OcrBlock(val text: String, val bounds: BoundingBox)
data class OcrResult(val blocks: List<OcrBlock>)

data class TranslatedBlock(
    val original: String,
    val translated: String,
    val bounds: BoundingBox
)
data class TranslationResult(val blocks: List<TranslatedBlock>)

// ByteArray in data class has broken equals/hashCode by default — implementation must override both.
data class DynamicTranslationOutput(
    val imageBytes: ByteArray,        // same format as input
    val translations: List<TranslatedBlock>
) {
    override fun equals(other: Any?): Boolean { ... }
    override fun hashCode(): Int { ... }
}

data class DynamicTranslationConfig(
    val sourceLanguage: String,       // Tesseract lang code, e.g. "eng"
    val targetLanguage: String,       // target lang, e.g. "fra"
    val tessDataPath: String,         // path to tessdata directory
    val ollamaModel: String = "llama3.2",   // JVM only
    val nllbModelPath: String = "",         // Android only, path to .tflite
)
```

All tool input/output types annotated with `@Serializable` (kotlinx-serialization).

---

## Strategy Interfaces (`commonMain`)

```kotlin
interface OcrStrategy {
    suspend fun extract(imageBytes: ByteArray, config: DynamicTranslationConfig): OcrResult
}

interface TranslationStrategy {
    suspend fun translate(ocr: OcrResult, config: DynamicTranslationConfig): TranslationResult
}

interface RenderStrategy {
    suspend fun render(
        imageBytes: ByteArray,
        translations: TranslationResult,
        config: DynamicTranslationConfig
    ): ByteArray  // output encoded in same format as input
}
```

---

## Koog Agent + TranslateTool (`commonMain`)

### TranslateTool

A Koog `SimpleTool` that runs the three-stage pipeline sequentially:

```kotlin
class TranslateTool(
    private val ocr: OcrStrategy,
    private val translation: TranslationStrategy,
    private val render: RenderStrategy,
) : SimpleTool<TranslateToolInput, DynamicTranslationOutput> {
    // 1. ocr.extract(imageBytes, config)
    // 2. If OcrResult.blocks is empty → return original imageBytes + empty translations
    // 3. translation.translate(ocrResult, config)
    // 4. render.render(imageBytes, translationResult, config)
    // 5. Return DynamicTranslationOutput
}

@Serializable
data class TranslateToolInput(
    val imageBase64: String,   // Base64-encoded image bytes
    val config: DynamicTranslationConfig
)
```

### DynamicTranslationAgent

Thin `AIAgent` wrapper. Strategy: single deterministic tool call (no LLM reasoning loop).
Implements `Closeable` — `close()` delegates to the underlying `TranslationStrategy.close()` (no-op on JVM, releases NLLB `Interpreter` on Android).

```kotlin
class DynamicTranslationAgent(
    private val tool: TranslateTool,
    private val executor: PromptExecutor,
    private val model: LLModel,
) : Closeable {
    suspend fun translate(imageBytes: ByteArray, config: DynamicTranslationConfig): DynamicTranslationOutput
    override fun close()  // delegates to TranslationStrategy.close()
}
```

`TranslationStrategy` also implements `Closeable`:
```kotlin
interface TranslationStrategy : Closeable {
    suspend fun translate(ocr: OcrResult, config: DynamicTranslationConfig): TranslationResult
    override fun close() {}  // default no-op; Android overrides
}
```

**Android executor:** minimal stub `PromptExecutor` — NLLB handles translation inside `AndroidTranslationStrategy`; the executor is never actually invoked on Android.

**JVM executor:** `OllamaClient()` from `ai.koog.prompt.executor.ollama.client`. The same `OllamaClient` instance is injected into both `DynamicTranslationAgent` and `JvmTranslationStrategy` — a single shared client, not two separate instances.

### Public entry point (`expect`/`actual`)

```kotlin
// commonMain
expect fun buildDynamicTranslationAgent(config: DynamicTranslationConfig): DynamicTranslationAgent

// jvmMain — actual: wires JvmOcrStrategy, JvmTranslationStrategy, JvmRenderStrategy + OllamaClient
// androidMain — actual: wires AndroidOcrStrategy, AndroidTranslationStrategy, AndroidRenderStrategy + stub executor
```

Usage:
```kotlin
// agent implements Closeable — use try-with-resources or close() in onCleared()
val agent = buildDynamicTranslationAgent(config)
agent.use {
    val output: DynamicTranslationOutput = it.translate(imageBytes, config)
    // output.imageBytes  → translated image (same format as input)
    // output.translations → List<TranslatedBlock> with bounds
}
```

---

## JVM Implementations (`jvmMain`)

### `JvmOcrStrategy`
- Library: **Tess4J** (`net.sourceforge.tess4j:tess4j`)
- Creates `Tesseract` instance, sets `datapath` from `config.tessDataPath`, `language` from `config.sourceLanguage`
- Returns word-level bounding boxes via `getWords(image, RenderFormat.ALTO)`

### `JvmTranslationStrategy`
- Library: Koog **`OllamaClient`** (`ai.koog.prompt.executor.ollama.client`)
- Receives shared `OllamaClient` via constructor injection (same instance held by `DynamicTranslationAgent`)
- Builds a translation prompt per block: `"Translate the following from {source} to {target}: {text}"`
- Calls Ollama using `config.ollamaModel`
- Returns `TranslationResult` with one `TranslatedBlock` per `OcrBlock`
- `close()` is a no-op (client lifecycle owned by caller)

### `JvmRenderStrategy`
- Libraries: **OpenCV Java** (`org.openpnp:opencv`) + **`java.awt`**
- For each bounding box: apply `Photo.inpaint()` (INPAINT_TELEA) on the box region
- Overlay translated text using `Graphics2D.drawString()` centered in the original bounding box
- **Format detection:** read first 8 bytes → PNG magic (`89 50 4E 47`) or JPEG magic (`FF D8 FF`) → use matching `ImageIO.write(img, "png"/"jpeg", ...)`

---

## Android Implementations (`androidMain`)

### `AndroidOcrStrategy`
- Library: **Tesseract4Android** (`com.github.adaptech-cz:tesseract4android`)
- `TessBaseAPI`, `init(tessDataPath, sourceLanguage)`, `getWords(bitmap, RenderFormat.ALTO)`

### `AndroidTranslationStrategy`
- Library: **LiteRT** (`org.tensorflow:tensorflow-lite`) + NLLB `.tflite` model
- Lazy-loads `Interpreter` from `config.nllbModelPath` on first call
- Runs inference per block using NLLB tokenizer → translate → detokenize
- Exposes `close()` for lifecycle management — caller (e.g. ViewModel `onCleared`) must call it
- `IllegalStateException` with descriptive message if model file missing at load time

### `AndroidRenderStrategy`
- Libraries: **OpenCV Android SDK** + **`android.graphics.Canvas`**
- `Photo.inpaint()` per bounding box region (INPAINT_TELEA)
- `Canvas.drawText()` for translated text at original bounds
- **Format detection:** `BitmapFactory.decodeByteArray` + `BitmapFactory.Options.outMimeType` → `Bitmap.compress(PNG/JPEG, ...)`

---

## Build Configuration

### `build.gradle.kts`

```kotlin
plugins {
    `otaku-multiplatform-no-ios`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.koogintegration.dynamictranslation"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinStLib)
            implementation(libs.koog.agents)
            implementation(libs.kotlinxSerialization)
            implementation(project(":kmpuiviews:koogintegration"))
        }
        jvmMain.dependencies {
            implementation("net.sourceforge.tess4j:tess4j:5.13.0")
            implementation("org.openpnp:opencv:4.9.0-0")
        }
        androidMain.dependencies {
            implementation("com.github.adaptech-cz:tesseract4android:4.8.0")
            implementation("org.tensorflow:tensorflow-lite:2.14.0")
            implementation("org.opencv:opencv:4.9.0")  // Maven Central
        }
    }
}
```

---

## Error Handling

| Scenario | Behavior |
|----------|----------|
| `OcrResult.blocks` is empty | Short-circuit: return original `imageBytes` + empty `translations` |
| Tess4J / Tesseract4Android failure | Wrap in `DynamicTranslationException`, propagate |
| Ollama unreachable (JVM) | Koog throws, propagates to caller |
| NLLB model file missing (Android) | `IllegalStateException` at load time with path in message |
| Unsupported image format | `DynamicTranslationException("Unsupported image format")` |

No silent swallowing.

---

## README Update

Add a section to the **root-level README only** describing:
- Feature name and description
- Supported platforms (Android, JVM/Desktop)
- Capabilities: OCR → local translation → inpainting → text overlay
- Technologies used per platform
- Basic usage snippet

No module-level README.
