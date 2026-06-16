# Dynamic Translation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `:kmpuiviews:koogintegration:dynamictranslation` — a KMP module that OCRs an image, translates the text locally, inpaints the original text, and renders translated text at the original positions, returning both the modified image and structured coordinate data.

**Architecture:** Single KMP Gradle module (`otaku-multiplatform-no-ios`) with strategy interfaces in `commonMain` and platform implementations in `androidMain`/`jvmMain`. A `TranslateTool` orchestrates OCR→Translation→Render sequentially; `DynamicTranslationAgent` wraps it as the public entry point and implements `Closeable` for NLLB lifecycle. JVM uses Tess4J + OllamaClient + OpenCV; Android uses Tesseract4Android + LiteRT/NLLB + OpenCV Android.

**Tech Stack:** Koog (`koog-agents` 1.0.0), Tess4J 5.13.0 (JVM OCR), OpenCV `org.openpnp:opencv:4.9.0-0` (JVM render), OpenCV Android `org.opencv:opencv:4.9.0` (Android render), Tesseract4Android 4.8.0, TensorFlow Lite 2.14.0 + tensorflow-lite-support 0.4.4 (NLLB inference), `com.github.google.sentencepiece:libsentencepiece-android:0.2.0` (NLLB tokenization), kotlinx-serialization.

---

## File Map

**New module:**
- `kmpuiviews/koogintegration/dynamictranslation/build.gradle.kts`
- `kmpuiviews/koogintegration/dynamictranslation/src/androidMain/AndroidManifest.xml`

**commonMain** (`src/commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/`):
- `model/BoundingBox.kt` — `BoundingBox(x, y, width, height)`
- `model/OcrResult.kt` — `OcrBlock`, `OcrResult`
- `model/TranslationResult.kt` — `TranslatedBlock`, `TranslationResult`
- `model/DynamicTranslationOutput.kt` — output data class with `ByteArray` + translations
- `model/DynamicTranslationConfig.kt` — config data class
- `model/DynamicTranslationException.kt` — exception class
- `strategy/OcrStrategy.kt` — `OcrStrategy` interface
- `strategy/TranslationStrategy.kt` — `TranslationStrategy : Closeable` interface
- `strategy/RenderStrategy.kt` — `RenderStrategy` interface
- `tool/TranslateTool.kt` — orchestrates OCR→Translation→Render
- `agent/DynamicTranslationAgent.kt` — public entry point, implements `Closeable`
- `agent/DynamicTranslationAgentFactory.kt` — `expect fun buildDynamicTranslationAgent`

**jvmMain** (`src/jvmMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/`):
- `strategy/JvmOcrStrategy.kt` — Tess4J OCR
- `strategy/JvmTranslationStrategy.kt` — Koog AIAgent + OllamaClient
- `strategy/JvmRenderStrategy.kt` — OpenCV inpaint + java.awt text render
- `agent/DynamicTranslationAgentFactory.jvm.kt` — JVM `actual`

**androidMain** (`src/androidMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/`):
- `strategy/AndroidOcrStrategy.kt` — Tesseract4Android OCR
- `strategy/AndroidTranslationStrategy.kt` — LiteRT NLLB inference
- `strategy/AndroidRenderStrategy.kt` — OpenCV inpaint + Canvas text render
- `agent/DynamicTranslationAgentFactory.android.kt` — Android `actual`

**Modified:**
- `settings.gradle.kts` — add `include(":kmpuiviews:koogintegration:dynamictranslation")`
- `README.md` — add Dynamic Translation feature section

---

## Task 1: Module Scaffold

**Files:**
- Create: `kmpuiviews/koogintegration/dynamictranslation/build.gradle.kts`
- Create: `kmpuiviews/koogintegration/dynamictranslation/src/androidMain/AndroidManifest.xml`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Register module in settings.gradle.kts**

Open `settings.gradle.kts`. After line 87 (`include(":kmpuiviews:koogintegration:customscraper")`), add:

```kotlin
include(":kmpuiviews:koogintegration:dynamictranslation")
```

- [ ] **Step 2: Create build.gradle.kts**

Create `kmpuiviews/koogintegration/dynamictranslation/build.gradle.kts`:

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
            implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
            implementation("org.opencv:opencv:4.9.0")
            // SentencePiece for NLLB tokenization — provides SentencePieceProcessor
            // If this coordinate is unavailable, use: "com.google.android.gms:play-services-tflite-support"
            // is GMS and forbidden; instead bundle sentencepiece via JNI manually or use
            // "org.tensorflow:tensorflow-lite-support-api:0.4.4" which exposes SentencePieceTokenizer
            implementation("com.github.google.sentencepiece:libsentencepiece-android:0.2.0")
        }
    }
}
```

- [ ] **Step 3: Create AndroidManifest.xml**

Create `kmpuiviews/koogintegration/dynamictranslation/src/androidMain/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest>

</manifest>
```

- [ ] **Step 4: Verify module is recognized**

```bash
./gradlew :kmpuiviews:koogintegration:dynamictranslation:tasks --all 2>&1 | head -20
```

Expected: task list printed without "Project not found" error.

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/koogintegration/dynamictranslation/build.gradle.kts \
        kmpuiviews/koogintegration/dynamictranslation/src/androidMain/AndroidManifest.xml \
        settings.gradle.kts
git commit -m "feat(dynamictranslation): scaffold KMP module"
```

---

## Task 2: Data Models (commonMain)

**Files:**
- Create: `src/commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/model/BoundingBox.kt`
- Create: `src/commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/model/OcrResult.kt`
- Create: `src/commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/model/TranslationResult.kt`
- Create: `src/commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/model/DynamicTranslationOutput.kt`
- Create: `src/commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/model/DynamicTranslationConfig.kt`
- Create: `src/commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/model/DynamicTranslationException.kt`

All paths below are relative to `kmpuiviews/koogintegration/dynamictranslation/`.

- [ ] **Step 1: Create BoundingBox.kt**

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.model

import kotlinx.serialization.Serializable

@Serializable
data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)
```

- [ ] **Step 2: Create OcrResult.kt**

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.model

import kotlinx.serialization.Serializable

@Serializable
data class OcrBlock(
    val text: String,
    val bounds: BoundingBox,
)

@Serializable
data class OcrResult(
    val blocks: List<OcrBlock>,
)
```

- [ ] **Step 3: Create TranslationResult.kt**

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.model

import kotlinx.serialization.Serializable

@Serializable
data class TranslatedBlock(
    val original: String,
    val translated: String,
    val bounds: BoundingBox,
)

@Serializable
data class TranslationResult(
    val blocks: List<TranslatedBlock>,
)
```

- [ ] **Step 4: Create DynamicTranslationOutput.kt**

Note: `ByteArray` in a `data class` has broken `equals`/`hashCode` by default — both are overridden here.

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.model

data class DynamicTranslationOutput(
    val imageBytes: ByteArray,
    val translations: List<TranslatedBlock>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DynamicTranslationOutput) return false
        return imageBytes.contentEquals(other.imageBytes) && translations == other.translations
    }

    override fun hashCode(): Int {
        var result = imageBytes.contentHashCode()
        result = 31 * result + translations.hashCode()
        return result
    }
}
```

- [ ] **Step 5: Create DynamicTranslationConfig.kt**

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.model

import kotlinx.serialization.Serializable

@Serializable
data class DynamicTranslationConfig(
    val sourceLanguage: String,
    val targetLanguage: String,
    val tessDataPath: String,
    val ollamaModel: String = "llama3.2",
    val nllbModelPath: String = "",
)
```

- [ ] **Step 6: Create DynamicTranslationException.kt**

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.model

class DynamicTranslationException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

- [ ] **Step 7: Verify commonMain compiles**

```bash
./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileKotlinJvm 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add kmpuiviews/koogintegration/dynamictranslation/src/commonMain/
git commit -m "feat(dynamictranslation): add data models"
```

---

## Task 3: Strategy Interfaces (commonMain)

**Files:**
- Create: `src/commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/strategy/OcrStrategy.kt`
- Create: `src/commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/strategy/TranslationStrategy.kt`
- Create: `src/commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/strategy/RenderStrategy.kt`

- [ ] **Step 1: Create OcrStrategy.kt**

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.OcrResult

interface OcrStrategy {
    suspend fun extract(imageBytes: ByteArray, config: DynamicTranslationConfig): OcrResult
}
```

- [ ] **Step 2: Create TranslationStrategy.kt**

`TranslationStrategy` extends `Closeable` so platform impls can release resources (NLLB `Interpreter` on Android). The default `close()` is a no-op — JVM impl inherits it; Android overrides.

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.TranslationResult
import com.programmersbox.koogintegration.dynamictranslation.model.OcrResult
import java.io.Closeable

interface TranslationStrategy : Closeable {
    suspend fun translate(ocr: OcrResult, config: DynamicTranslationConfig): TranslationResult
    override fun close() {}
}
```

- [ ] **Step 3: Create RenderStrategy.kt**

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.TranslationResult

interface RenderStrategy {
    suspend fun render(
        imageBytes: ByteArray,
        translations: TranslationResult,
        config: DynamicTranslationConfig,
    ): ByteArray
}
```

- [ ] **Step 4: Verify compile**

```bash
./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileKotlinJvm 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/koogintegration/dynamictranslation/src/commonMain/
git commit -m "feat(dynamictranslation): add strategy interfaces"
```

---

## Task 4: TranslateTool (commonMain)

**Files:**
- Create: `src/commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/tool/TranslateTool.kt`

- [ ] **Step 1: Create TranslateTool.kt**

`TranslateTool` is the pipeline orchestrator. It is `Closeable` — `close()` forwards to `TranslationStrategy.close()` (releases NLLB on Android). Short-circuits on empty OCR result, returning original image bytes unchanged.

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.tool

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationOutput
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationException
import com.programmersbox.koogintegration.dynamictranslation.strategy.OcrStrategy
import com.programmersbox.koogintegration.dynamictranslation.strategy.RenderStrategy
import com.programmersbox.koogintegration.dynamictranslation.strategy.TranslationStrategy
import java.io.Closeable

class TranslateTool(
    private val ocr: OcrStrategy,
    private val translation: TranslationStrategy,
    private val render: RenderStrategy,
) : Closeable {

    suspend fun execute(imageBytes: ByteArray, config: DynamicTranslationConfig): DynamicTranslationOutput {
        val ocrResult = try {
            ocr.extract(imageBytes, config)
        } catch (e: Exception) {
            throw DynamicTranslationException("OCR failed: ${e.message}", e)
        }

        if (ocrResult.blocks.isEmpty()) {
            return DynamicTranslationOutput(imageBytes = imageBytes, translations = emptyList())
        }

        val translationResult = try {
            translation.translate(ocrResult, config)
        } catch (e: Exception) {
            throw DynamicTranslationException("Translation failed: ${e.message}", e)
        }

        val renderedImage = try {
            render.render(imageBytes, translationResult, config)
        } catch (e: Exception) {
            throw DynamicTranslationException("Render failed: ${e.message}", e)
        }

        return DynamicTranslationOutput(
            imageBytes = renderedImage,
            translations = translationResult.blocks,
        )
    }

    override fun close() {
        translation.close()
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileKotlinJvm 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/koogintegration/dynamictranslation/src/commonMain/
git commit -m "feat(dynamictranslation): add TranslateTool pipeline orchestrator"
```

---

## Task 5: DynamicTranslationAgent + Expect Factory (commonMain)

**Files:**
- Create: `src/commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/agent/DynamicTranslationAgent.kt`
- Create: `src/commonMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/agent/DynamicTranslationAgentFactory.kt`

- [ ] **Step 1: Create DynamicTranslationAgent.kt**

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.agent

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationOutput
import com.programmersbox.koogintegration.dynamictranslation.tool.TranslateTool
import java.io.Closeable

class DynamicTranslationAgent(
    private val tool: TranslateTool,
) : Closeable {

    suspend fun translate(
        imageBytes: ByteArray,
        config: DynamicTranslationConfig,
    ): DynamicTranslationOutput = tool.execute(imageBytes, config)

    override fun close() {
        tool.close()
    }
}
```

- [ ] **Step 2: Create DynamicTranslationAgentFactory.kt (expect)**

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.agent

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig

expect fun buildDynamicTranslationAgent(config: DynamicTranslationConfig): DynamicTranslationAgent
```

- [ ] **Step 3: Verify (expect/actual will fail until actuals exist — check for expected error)**

```bash
./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileKotlinJvm 2>&1 | tail -10
```

Expected: error about missing `actual` for `buildDynamicTranslationAgent`. This is correct — actuals are added in Tasks 9 and 13.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/koogintegration/dynamictranslation/src/commonMain/
git commit -m "feat(dynamictranslation): add DynamicTranslationAgent and expect factory"
```

---

## Task 6: JvmOcrStrategy

**Files:**
- Create: `src/jvmMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/strategy/JvmOcrStrategy.kt`

- [ ] **Step 1: Create JvmOcrStrategy.kt**

Uses Tess4J. `Tesseract` is created per call (not thread-safe, not reused). `getWords()` at `RIL_WORD` level returns word bounding boxes.

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.programmersbox.koogintegration.dynamictranslation.model.BoundingBox
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationException
import com.programmersbox.koogintegration.dynamictranslation.model.OcrBlock
import com.programmersbox.koogintegration.dynamictranslation.model.OcrResult
import net.sourceforge.tess4j.ITessAPI
import net.sourceforge.tess4j.Tesseract
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class JvmOcrStrategy : OcrStrategy {

    override suspend fun extract(imageBytes: ByteArray, config: DynamicTranslationConfig): OcrResult {
        val image: BufferedImage = ImageIO.read(ByteArrayInputStream(imageBytes))
            ?: throw DynamicTranslationException("Failed to decode image for OCR")

        val tesseract = Tesseract().apply {
            setDatapath(config.tessDataPath)
            setLanguage(config.sourceLanguage)
            setPageSegMode(3) // PSM_AUTO
        }

        val words = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD)

        val blocks = words
            .filter { it.text.isNotBlank() }
            .map { word ->
                OcrBlock(
                    text = word.text.trim(),
                    bounds = BoundingBox(
                        x = word.boundingBox.x,
                        y = word.boundingBox.y,
                        width = word.boundingBox.width,
                        height = word.boundingBox.height,
                    ),
                )
            }

        return OcrResult(blocks)
    }
}
```

- [ ] **Step 2: Verify JVM source compiles**

```bash
./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileKotlinJvm 2>&1 | grep -E "error:|BUILD" | tail -10
```

Expected: only `actual` missing errors remain (not errors in `JvmOcrStrategy`).

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/koogintegration/dynamictranslation/src/jvmMain/
git commit -m "feat(dynamictranslation): add JvmOcrStrategy (Tess4J)"
```

---

## Task 7: JvmTranslationStrategy

**Files:**
- Create: `src/jvmMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/strategy/JvmTranslationStrategy.kt`

- [ ] **Step 1: Create JvmTranslationStrategy.kt**

Uses a Koog `AIAgent<String, String>` backed by `OllamaClient`. A single-call strategy (no compression loop) sends the translation prompt and returns the raw LLM text response. The same `OllamaClient` instance is shared from `buildDynamicTranslationAgent` — no duplicate clients.

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.strategy

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.asUserMessage
import ai.koog.agents.core.dsl.extension.nodeLLMSendMessage
import ai.koog.agents.core.dsl.extension.onTextMessage
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLModel
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.OcrResult
import com.programmersbox.koogintegration.dynamictranslation.model.TranslatedBlock
import com.programmersbox.koogintegration.dynamictranslation.model.TranslationResult

class JvmTranslationStrategy(
    private val client: OllamaClient,
    private val model: LLModel,
) : TranslationStrategy {

    private val executor = MultiLLMPromptExecutor(client)

    private val translationAgent: AIAgent<String, String> by lazy {
        AIAgent(
            promptExecutor = executor,
            agentConfig = AIAgentConfig(
                prompt = prompt("dt-translation") {
                    system(
                        "You are a translation engine. Translate the given text exactly and accurately. " +
                            "Return ONLY the translated text. No explanations, no extra words."
                    )
                },
                model = model,
                maxAgentIterations = 3,
            ),
            strategy = strategy<String, String>("dt-single-llm-call") {
                val nodeCallLLM by nodeLLMSendMessage()
                edge(nodeStart forwardTo nodeCallLLM asUserMessage { it })
                edge(nodeCallLLM forwardTo nodeFinish onTextMessage { true } transformed { it.trim() })
            },
        )
    }

    override suspend fun translate(ocr: OcrResult, config: DynamicTranslationConfig): TranslationResult {
        val blocks = ocr.blocks.mapIndexed { idx, block ->
            val prompt = "Translate from ${config.sourceLanguage} to ${config.targetLanguage}:\n${block.text}"
            val translated = translationAgent.run(prompt, "dt-translate-$idx-${block.text.hashCode()}")
            TranslatedBlock(
                original = block.text,
                translated = translated,
                bounds = block.bounds,
            )
        }
        return TranslationResult(blocks)
    }

    override fun close() {}
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileKotlinJvm 2>&1 | grep -E "error:|BUILD" | tail -10
```

Expected: only `actual` missing errors remain.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/koogintegration/dynamictranslation/src/jvmMain/
git commit -m "feat(dynamictranslation): add JvmTranslationStrategy (Koog + Ollama)"
```

---

## Task 8: JvmRenderStrategy

**Files:**
- Create: `src/jvmMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/strategy/JvmRenderStrategy.kt`

- [ ] **Step 1: Create JvmRenderStrategy.kt**

Uses OpenCV `Photo.inpaint()` (INPAINT_TELEA) to remove original text, then `Graphics2D.drawString()` to overlay translated text. `org.openpnp:opencv` bundles native libs — load via `nu.pattern.OpenCV.loadLocally()`. Format detection reads magic bytes.

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationException
import com.programmersbox.koogintegration.dynamictranslation.model.TranslationResult
import nu.pattern.OpenCV
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.photo.Photo
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class JvmRenderStrategy : RenderStrategy {

    init {
        OpenCV.loadLocally()
    }

    override suspend fun render(
        imageBytes: ByteArray,
        translations: TranslationResult,
        config: DynamicTranslationConfig,
    ): ByteArray {
        val format = detectFormat(imageBytes)
        val original = ImageIO.read(ByteArrayInputStream(imageBytes))
            ?: throw DynamicTranslationException("Failed to decode image for rendering")

        val mat = bufferedImageToMat(original)

        val mask = Mat.zeros(mat.size(), CvType.CV_8UC1)
        for (block in translations.blocks) {
            val b = block.bounds
            if (b.x >= 0 && b.y >= 0 && b.x + b.width <= mat.width() && b.y + b.height <= mat.height()) {
                mask.submat(Rect(b.x, b.y, b.width, b.height)).setTo(Scalar(255.0))
            }
        }

        val inpainted = Mat()
        Photo.inpaint(mat, mask, inpainted, 3.0, Photo.INPAINT_TELEA)

        val result = matToBufferedImage(inpainted)

        val g2d = result.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            color = java.awt.Color.BLACK
            font = Font(Font.SANS_SERIF, Font.BOLD, 14)
        }
        for (block in translations.blocks) {
            val b = block.bounds
            val fm = g2d.fontMetrics
            val textX = b.x + (b.width - fm.stringWidth(block.translated)) / 2
            val textY = b.y + (b.height + fm.ascent - fm.descent) / 2
            g2d.drawString(block.translated, textX, textY)
        }
        g2d.dispose()

        val out = ByteArrayOutputStream()
        ImageIO.write(result, format, out)
        return out.toByteArray()
    }

    private fun detectFormat(bytes: ByteArray): String = when {
        bytes.size >= 4 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "png"
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "jpeg"
        else -> throw DynamicTranslationException("Unsupported image format — only PNG and JPEG are supported")
    }

    private fun bufferedImageToMat(image: BufferedImage): Mat {
        val bgr = BufferedImage(image.width, image.height, BufferedImage.TYPE_3BYTE_BGR)
        val g = bgr.createGraphics()
        g.drawImage(image, 0, 0, null)
        g.dispose()
        val mat = Mat(bgr.height, bgr.width, CvType.CV_8UC3)
        mat.put(0, 0, (bgr.raster.dataBuffer as DataBufferByte).data)
        return mat
    }

    private fun matToBufferedImage(mat: Mat): BufferedImage {
        val image = BufferedImage(mat.width(), mat.height(), BufferedImage.TYPE_3BYTE_BGR)
        val data = ByteArray(mat.width() * mat.height() * mat.channels())
        mat.get(0, 0, data)
        image.raster.setDataElements(0, 0, mat.width(), mat.height(), data)
        return image
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileKotlinJvm 2>&1 | grep -E "error:|BUILD" | tail -10
```

Expected: only `actual` missing errors remain.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/koogintegration/dynamictranslation/src/jvmMain/
git commit -m "feat(dynamictranslation): add JvmRenderStrategy (OpenCV + AWT)"
```

---

## Task 9: JVM Actual Factory

**Files:**
- Create: `src/jvmMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/agent/DynamicTranslationAgentFactory.jvm.kt`

- [ ] **Step 1: Create DynamicTranslationAgentFactory.jvm.kt**

`OllamaClient` is shared: passed to both `JvmTranslationStrategy` (for inference) and is the backing client for the agent's executor. `OllamaModels.models.find` looks up the model; fails fast with a clear error if the model id is unknown.

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.agent

import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLModel
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.strategy.JvmOcrStrategy
import com.programmersbox.koogintegration.dynamictranslation.strategy.JvmRenderStrategy
import com.programmersbox.koogintegration.dynamictranslation.strategy.JvmTranslationStrategy
import com.programmersbox.koogintegration.dynamictranslation.tool.TranslateTool

actual fun buildDynamicTranslationAgent(config: DynamicTranslationConfig): DynamicTranslationAgent {
    val client = OllamaClient()
    val model = OllamaModels.models.find { it.id == config.ollamaModel }
        ?: LLModel(id = config.ollamaModel)

    val ocr = JvmOcrStrategy()
    val translation = JvmTranslationStrategy(client, model)
    val render = JvmRenderStrategy()
    val tool = TranslateTool(ocr, translation, render)

    return DynamicTranslationAgent(tool)
}
```

**Note on `LLModel(id = config.ollamaModel)`:** If `LLModel` cannot be constructed this way (compile error), replace with `OllamaModels.models.first { it.id == config.ollamaModel }` and ensure the Ollama model name is an exact match to one in `OllamaModels.models`.

- [ ] **Step 2: Verify JVM compiles cleanly**

```bash
./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileKotlinJvm 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL` (no more `actual` errors for JVM).

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/koogintegration/dynamictranslation/src/jvmMain/
git commit -m "feat(dynamictranslation): add JVM actual factory"
```

---

## Task 10: AndroidOcrStrategy

**Files:**
- Create: `src/androidMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/strategy/AndroidOcrStrategy.kt`

- [ ] **Step 1: Create AndroidOcrStrategy.kt**

Uses Tesseract4Android's `TessBaseAPI`. Tesseract must be initialised with the `tessDataPath` directory (the directory containing `tessdata/eng.traineddata` etc.) and the source language code. `getWords()` at `TessBaseAPI.PageIteratorLevel.RIL_WORD` returns word bounding boxes.

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.strategy

import android.graphics.BitmapFactory
import com.googlecode.tesseract.android.TessBaseAPI
import com.programmersbox.koogintegration.dynamictranslation.model.BoundingBox
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationException
import com.programmersbox.koogintegration.dynamictranslation.model.OcrBlock
import com.programmersbox.koogintegration.dynamictranslation.model.OcrResult

class AndroidOcrStrategy : OcrStrategy {

    override suspend fun extract(imageBytes: ByteArray, config: DynamicTranslationConfig): OcrResult {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw DynamicTranslationException("Failed to decode image for OCR")

        val api = TessBaseAPI()
        if (!api.init(config.tessDataPath, config.sourceLanguage)) {
            throw DynamicTranslationException(
                "Tesseract init failed — check tessDataPath '${config.tessDataPath}' " +
                    "contains tessdata/${config.sourceLanguage}.traineddata"
            )
        }

        try {
            api.setImage(bitmap)
            api.getHOCRText(0) // trigger recognition

            val iterator = api.resultIterator
                ?: return OcrResult(emptyList())

            val blocks = mutableListOf<OcrBlock>()
            iterator.begin()
            do {
                val word = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                    ?.trim() ?: continue
                if (word.isBlank()) continue

                val bounds = iterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                blocks.add(
                    OcrBlock(
                        text = word,
                        bounds = BoundingBox(
                            x = bounds.left,
                            y = bounds.top,
                            width = bounds.right - bounds.left,
                            height = bounds.bottom - bounds.top,
                        ),
                    )
                )
            } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))

            iterator.delete()
            return OcrResult(blocks)
        } finally {
            api.recycle()
        }
    }
}
```

- [ ] **Step 2: Verify Android source compiles**

```bash
./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileDebugKotlinAndroid 2>&1 | grep -E "error:|BUILD" | tail -10
```

Expected: only `actual` missing errors remain.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/koogintegration/dynamictranslation/src/androidMain/
git commit -m "feat(dynamictranslation): add AndroidOcrStrategy (Tesseract4Android)"
```

---

## Task 11: AndroidTranslationStrategy

**Files:**
- Create: `src/androidMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/strategy/AndroidTranslationStrategy.kt`

- [ ] **Step 1: Create AndroidTranslationStrategy.kt**

Uses TFLite (`Interpreter`) with the NLLB model. The NLLB TFLite model requires:
1. The `.tflite` model file at `config.nllbModelPath`
2. A SentencePiece vocabulary file (NLLB uses `flores200_sacrebleu_tokenizer_spm.model`)
   — by convention expected at `<nllbModelPath parent dir>/nllb_vocab.spm`
3. NLLB language codes differ from Tesseract codes — e.g. `eng` → `eng_Latn`, `fra` → `fra_Latn`

The `Interpreter` is lazy-loaded on first use and released via `close()`. The translation loop runs greedy decoding: encode → decode step by step until `EOS_TOKEN_ID`.

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationException
import com.programmersbox.koogintegration.dynamictranslation.model.OcrResult
import com.programmersbox.koogintegration.dynamictranslation.model.TranslatedBlock
import com.programmersbox.koogintegration.dynamictranslation.model.TranslationResult
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.File
import java.nio.IntBuffer

class AndroidTranslationStrategy : TranslationStrategy {

    private var interpreter: Interpreter? = null
    private var tokenizer: NllbTokenizer? = null

    private fun ensureLoaded(config: DynamicTranslationConfig) {
        if (interpreter != null) return
        val modelFile = File(config.nllbModelPath)
        require(modelFile.exists()) {
            "NLLB model not found at '${config.nllbModelPath}'. Download from Meta/HuggingFace and convert to TFLite."
        }
        val vocabFile = File(modelFile.parent, "nllb_vocab.spm")
        require(vocabFile.exists()) {
            "NLLB SentencePiece vocab not found at '${vocabFile.absolutePath}'. " +
                "Expected flores200_sacrebleu_tokenizer_spm.model renamed to nllb_vocab.spm."
        }
        interpreter = Interpreter(modelFile, Interpreter.Options().apply { numThreads = 4 })
        tokenizer = NllbTokenizer(vocabFile.absolutePath)
    }

    override suspend fun translate(ocr: OcrResult, config: DynamicTranslationConfig): TranslationResult {
        ensureLoaded(config)
        val interp = interpreter ?: throw DynamicTranslationException("NLLB interpreter not loaded")
        val tok = tokenizer ?: throw DynamicTranslationException("NLLB tokenizer not loaded")

        val srcLang = toNllbLangCode(config.sourceLanguage)
        val tgtLang = toNllbLangCode(config.targetLanguage)

        val blocks = ocr.blocks.map { block ->
            val translated = translateText(interp, tok, block.text, srcLang, tgtLang)
            TranslatedBlock(original = block.text, translated = translated, bounds = block.bounds)
        }
        return TranslationResult(blocks)
    }

    private fun translateText(
        interp: Interpreter,
        tok: NllbTokenizer,
        text: String,
        srcLang: String,
        tgtLang: String,
    ): String {
        val inputIds = tok.encode(text, srcLang)
        val maxOutputLen = (inputIds.size * 2).coerceAtLeast(64)

        val inputBuffer = IntBuffer.wrap(inputIds.toIntArray())
        val outputIds = IntArray(maxOutputLen)
        val outputBuffer = IntBuffer.wrap(outputIds)

        val inputs = mapOf("input_ids" to inputBuffer)
        val outputs = mutableMapOf<String, Any>("output_ids" to outputBuffer)

        // NLLB TFLite models expose encoder-decoder as a single signature.
        // Signature key may vary; check the model with: `lite.Interpreter.get_signature_list()`
        interp.runSignature(inputs, outputs, "serving_default")

        val decodedIds = outputIds.takeWhile { it != NllbTokenizer.EOS_TOKEN_ID && it != 0 }
        return tok.decode(decodedIds)
    }

    private fun toNllbLangCode(tesseractLang: String): String = LANG_MAP[tesseractLang]
        ?: throw DynamicTranslationException(
            "No NLLB language code for Tesseract lang '$tesseractLang'. " +
                "Add it to AndroidTranslationStrategy.LANG_MAP."
        )

    override fun close() {
        interpreter?.close()
        interpreter = null
        tokenizer = null
    }

    companion object {
        // Map Tesseract 3-letter codes → NLLB BCP-47 + script codes
        // Add more as needed: https://github.com/facebookresearch/flores/tree/main/flores200
        private val LANG_MAP = mapOf(
            "eng" to "eng_Latn",
            "fra" to "fra_Latn",
            "deu" to "deu_Latn",
            "spa" to "spa_Latn",
            "ita" to "ita_Latn",
            "por" to "por_Latn",
            "rus" to "rus_Cyrl",
            "jpn" to "jpn_Jpan",
            "kor" to "kor_Hang",
            "chi_sim" to "zho_Hans",
            "chi_tra" to "zho_Hant",
            "ara" to "arb_Arab",
        )
    }
}
```

- [ ] **Step 2: Create NllbTokenizer.kt (helper in androidMain)**

Create `src/androidMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/strategy/NllbTokenizer.kt`:

NLLB tokenization uses SentencePiece. `SentencePieceProcessor` (from `libsentencepiece-android`) returns token IDs directly via `encodeAsIds()` and `decodeIds()`. The source language special token is prepended; EOS (id=2) is appended. Target lang token is stripped from decoder output.

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.strategy

import com.google.android.gms.tflite.java.TfLite // do NOT use — GMS forbidden
// Use: com.google.sentencepiece.SentencePieceProcessor from libsentencepiece-android
import com.google.sentencepiece.SentencePieceProcessor

class NllbTokenizer(vocabPath: String) {
    private val spp = SentencePieceProcessor().also { it.load(vocabPath) }

    fun encode(text: String, srcLangCode: String): List<Int> {
        // NLLB input format: <src_lang_code> token_ids... EOS
        val langTokenId = spp.pieceToId(srcLangCode)
        val tokenIds = spp.encodeAsIds(text)
        return listOf(langTokenId) + tokenIds + listOf(EOS_TOKEN_ID)
    }

    fun decode(tokenIds: List<Int>): String {
        val filtered = tokenIds.filter { it != EOS_TOKEN_ID && it != 0 }
        return spp.decodeIds(filtered.toIntArray()).trim()
    }

    companion object {
        const val EOS_TOKEN_ID = 2
    }
}
```

**Note:** `SentencePieceProcessor` package name (`com.google.sentencepiece`) may differ in the specific `libsentencepiece-android` artifact used. Verify with `./gradlew dependencies` after adding the dep. If the artifact is unavailable, bundle the sentencepiece `.so` manually or use the JNI wrapper from [github.com/google/sentencepiece](https://github.com/google/sentencepiece).

- [ ] **Step 3: Verify Android compile**

```bash
./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileDebugKotlinAndroid 2>&1 | grep -E "error:|BUILD" | tail -10
```

Expected: only `actual` missing errors remain.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/koogintegration/dynamictranslation/src/androidMain/
git commit -m "feat(dynamictranslation): add AndroidTranslationStrategy (LiteRT + NLLB)"
```

---

## Task 12: AndroidRenderStrategy

**Files:**
- Create: `src/androidMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/strategy/AndroidRenderStrategy.kt`

- [ ] **Step 1: Create AndroidRenderStrategy.kt**

Uses OpenCV Android `Photo.inpaint()` to remove original text, then Android `Canvas.drawText()` to overlay translated text. `OpenCVLoader.initDebug()` loads native libs. Format detection uses `BitmapFactory.Options.outMimeType`.

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.strategy

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationException
import com.programmersbox.koogintegration.dynamictranslation.model.TranslationResult
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.photo.Photo
import java.io.ByteArrayOutputStream

class AndroidRenderStrategy : RenderStrategy {

    override suspend fun render(
        imageBytes: ByteArray,
        translations: TranslationResult,
        config: DynamicTranslationConfig,
    ): ByteArray {
        if (!OpenCVLoader.initDebug()) {
            throw DynamicTranslationException("OpenCV failed to initialize")
        }

        val formatOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, formatOptions)
        val compressFormat = when {
            formatOptions.outMimeType?.contains("png", ignoreCase = true) == true ->
                Bitmap.CompressFormat.PNG
            else -> Bitmap.CompressFormat.JPEG
        }

        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw DynamicTranslationException("Failed to decode image for rendering")

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val bgrMat = Mat()
        org.opencv.imgproc.Imgproc.cvtColor(mat, bgrMat, org.opencv.imgproc.Imgproc.COLOR_RGBA2BGR)

        val mask = Mat.zeros(bgrMat.size(), CvType.CV_8UC1)
        for (block in translations.blocks) {
            val b = block.bounds
            if (b.x >= 0 && b.y >= 0 &&
                b.x + b.width <= bgrMat.width() && b.y + b.height <= bgrMat.height()
            ) {
                mask.submat(Rect(b.x, b.y, b.width, b.height)).setTo(Scalar(255.0))
            }
        }

        val inpainted = Mat()
        Photo.inpaint(bgrMat, mask, inpainted, 3.0, Photo.INPAINT_TELEA)

        val rgbaMat = Mat()
        org.opencv.imgproc.Imgproc.cvtColor(inpainted, rgbaMat, org.opencv.imgproc.Imgproc.COLOR_BGR2RGBA)
        Utils.matToBitmap(rgbaMat, bitmap)

        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        for (block in translations.blocks) {
            val b = block.bounds
            val textWidth = paint.measureText(block.translated)
            val x = b.x + (b.width - textWidth) / 2f
            val y = b.y + b.height / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(block.translated, x, y, paint)
        }

        val out = ByteArrayOutputStream()
        val quality = if (compressFormat == Bitmap.CompressFormat.PNG) 100 else 90
        bitmap.compress(compressFormat, quality, out)
        return out.toByteArray()
    }
}
```

- [ ] **Step 2: Verify Android compile**

```bash
./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileDebugKotlinAndroid 2>&1 | grep -E "error:|BUILD" | tail -10
```

Expected: only `actual` missing errors remain.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/koogintegration/dynamictranslation/src/androidMain/
git commit -m "feat(dynamictranslation): add AndroidRenderStrategy (OpenCV + Canvas)"
```

---

## Task 13: Android Actual Factory

**Files:**
- Create: `src/androidMain/kotlin/com/programmersbox/koogintegration/dynamictranslation/agent/DynamicTranslationAgentFactory.android.kt`

- [ ] **Step 1: Create DynamicTranslationAgentFactory.android.kt**

```kotlin
package com.programmersbox.koogintegration.dynamictranslation.agent

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.strategy.AndroidOcrStrategy
import com.programmersbox.koogintegration.dynamictranslation.strategy.AndroidRenderStrategy
import com.programmersbox.koogintegration.dynamictranslation.strategy.AndroidTranslationStrategy
import com.programmersbox.koogintegration.dynamictranslation.tool.TranslateTool

actual fun buildDynamicTranslationAgent(config: DynamicTranslationConfig): DynamicTranslationAgent {
    val ocr = AndroidOcrStrategy()
    val translation = AndroidTranslationStrategy()
    val render = AndroidRenderStrategy()
    val tool = TranslateTool(ocr, translation, render)
    return DynamicTranslationAgent(tool)
}
```

- [ ] **Step 2: Verify full module compiles — both targets**

```bash
./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileKotlinJvm \
          :kmpuiviews:koogintegration:dynamictranslation:compileDebugKotlinAndroid 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` with no `actual` errors.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/koogintegration/dynamictranslation/src/androidMain/
git commit -m "feat(dynamictranslation): add Android actual factory — module complete"
```

---

## Task 14: README Update

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Add Dynamic Translation section to root README.md**

Find the `## Features` section (or equivalent). Add the following section. Place it after the existing features list, before any build/contributing sections:

```markdown
## Dynamic Translation

The **Dynamic Translation** feature is available as the `:kmpuiviews:koogintegration:dynamictranslation` module. It takes an image containing text, extracts and translates the text using fully local/on-device processing, inpaints (removes) the original text using OpenCV, and renders the translated text at the original bounding-box positions.

### Capabilities

- **OCR** — extracts text and bounding boxes from any static image
- **Local translation** — no cloud APIs, no internet required, no Google Mobile Services dependency
- **Inpainting** — removes original text from the image using OpenCV's Telea inpainting algorithm
- **Text overlay** — renders translated text at the original positions
- **Format preservation** — PNG input → PNG output, JPEG input → JPEG output
- **Structured output** — also returns translated strings with their x/y/width/height coordinates for use in custom rendering pipelines

### Supported Platforms

| Platform | OCR | Translation | Rendering |
|----------|-----|-------------|-----------|
| **Android** | Tesseract4Android | Meta NLLB 200 (LiteRT/TFLite, on-device) | OpenCV Android + Canvas |
| **JVM/Desktop** | Tess4J | Ollama local LLM (via Koog) | OpenCV Java + AWT |

### Usage

```kotlin
val config = DynamicTranslationConfig(
    sourceLanguage = "eng",       // Tesseract language code
    targetLanguage = "fra",       // target language
    tessDataPath = "/path/to/",   // directory containing tessdata/
    ollamaModel = "llama3.2",     // JVM only
    nllbModelPath = "/path/to/nllb.tflite",  // Android only
)

val agent = buildDynamicTranslationAgent(config)
agent.use { a ->
    val output = a.translate(imageBytes, config)
    // output.imageBytes — translated image (same format as input)
    // output.translations — List<TranslatedBlock> with bounds
}
```

### Android Setup Notes

- Download Tesseract trained data files from [tessdata](https://github.com/tesseract-ocr/tessdata) for the source language.
- Download the NLLB TFLite model from the community (Meta's official release is PyTorch; community TFLite conversions are available on HuggingFace).
- Place the NLLB SentencePiece vocabulary file (`nllb_vocab.spm`) in the same directory as the `.tflite` model.
```

- [ ] **Step 2: Verify no broken markdown**

```bash
grep -n "##" README.md | tail -20
```

Expected: Dynamic Translation section appears in the heading list.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: add Dynamic Translation feature to README"
```

---

## Post-Implementation Checklist

- [ ] Full module build passes: `./gradlew :kmpuiviews:koogintegration:dynamictranslation:build`
- [ ] JVM compile clean: `./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileKotlinJvm`
- [ ] Android compile clean: `./gradlew :kmpuiviews:koogintegration:dynamictranslation:compileDebugKotlinAndroid`
- [ ] `buildDynamicTranslationAgent` returns a non-null agent on JVM with `ollamaModel = "llama3.2"` and valid `tessDataPath`
- [ ] `DynamicTranslationAgent.use { }` block compiles and `close()` is called automatically
- [ ] Empty OCR result (image with no text) returns original bytes unchanged and empty translations list
- [ ] PNG input → PNG output; JPEG input → JPEG output verified by checking first magic bytes of output
