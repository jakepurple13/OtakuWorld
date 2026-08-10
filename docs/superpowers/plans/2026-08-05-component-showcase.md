# Component Showcase System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a self-contained KSP-powered component showcase: an annotation, a symbol processor that discovers annotated `@Composable` functions and generates a sorted registry, and a standalone Compose Desktop app that browses that registry through a Material 3 `NavigationRail`.

**Architecture:** Three Gradle modules — `:showcase:annotations` (KMP, jvm-target-only for now, holds `@ShowcaseComponent`), `:showcase:processor` (plain JVM KSP `SymbolProcessor`, validates + generates `ShowcaseRegistry.kt`), `:showcase` (plain JVM Compose Desktop app, contains its own sample annotated composables, applies the processor via KSP, renders the registry). No other app module in the repo is touched — sample composables live inside `:showcase` itself.

**Tech Stack:** Kotlin 2.4.10, KSP 2.3.10, Compose Multiplatform (Desktop), Material 3, Gradle Kotlin DSL, JUnit 5 + `dev.zacsweers.kctfork` (`kotlin-compile-testing`) for processor tests.

## Global Constraints

- All package names use prefix `com.programmersbox.showcase*` exactly as specified: `com.programmersbox.showcase.annotations`, `com.programmersbox.showcase.processor`, `com.programmersbox.showcase.generated`, `com.programmersbox.showcase`.
- `@ShowcaseComponent` has `name: String`, `description: String`, `group: String`; retention `SOURCE`; target `FUNCTION`.
- Generated `ShowcaseRegistry.entries` must be deterministically sorted: by `group` alphabetically, then by `name` alphabetically within each group.
- KSP error messages must match exactly:
  - `"Function '<name>' is annotated with @ShowcaseComponent but is not a @Composable function"`
  - `"Function '<name>' is annotated with @ShowcaseComponent but has parameters. Showcase components must have zero parameters."`
- No search/filter box anywhere in the app (explicit scope cut — see design spec).
- No wrapper-generation for parameterized functions, no hot-reload of the showcase app, no installer/distributable packaging, desktop-only target, no in-app editing, no theme toggle.
- This feature does not modify `mangaworld`, `animeworld`, `novelworld`, `UIViews`, or `kmpuiviews` — everything lives under the new `:showcase` module tree.
- Design spec: `docs/superpowers/specs/2026-08-05-component-showcase-design.md`.

---

### Task 1: `:showcase:annotations` module

**Files:**
- Modify: `settings.gradle.kts` (add module includes)
- Create: `showcase/annotations/build.gradle.kts`
- Create: `showcase/annotations/src/commonMain/kotlin/com/programmersbox/showcase/annotations/ShowcaseComponent.kt`

**Interfaces:**
- Produces: `com.programmersbox.showcase.annotations.ShowcaseComponent` annotation class, consumed by Task 2 (processor) and Task 7 (app's sample composables).

- [ ] **Step 1: Add module includes to settings.gradle.kts**

Add these three lines near the other `include(...)` calls (after the `include(":sharedcomponents")` line):

```kotlin
include(":showcase")
include(":showcase:annotations")
include(":showcase:processor")
```

- [ ] **Step 2: Create the annotations module build file**

`showcase/annotations/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(21)
    jvm()
}
```

- [ ] **Step 3: Write the annotation**

`showcase/annotations/src/commonMain/kotlin/com/programmersbox/showcase/annotations/ShowcaseComponent.kt`:

```kotlin
package com.programmersbox.showcase.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ShowcaseComponent(
    val name: String,
    val description: String,
    val group: String,
)
```

- [ ] **Step 4: Verify it builds**

Run: `./gradlew :showcase:annotations:build`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts showcase/annotations
git commit -m "feat(showcase): add showcase:annotations module with @ShowcaseComponent"
```

---

### Task 2: `:showcase:processor` module scaffold + new catalog entries

**Files:**
- Modify: `gradle/libs.versions.toml` (new version/library/plugin entries)
- Create: `showcase/processor/build.gradle.kts`
- Create: `showcase/processor/src/main/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessor.kt`
- Create: `showcase/processor/src/main/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessorProvider.kt`
- Create: `showcase/processor/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`

**Interfaces:**
- Consumes: `com.programmersbox.showcase.annotations.ShowcaseComponent` (Task 1)
- Produces: `ShowcaseSymbolProcessorProvider` (registered KSP entry point), generated file `com/programmersbox/showcase/generated/ShowcaseRegistry.kt` with `data class ShowcaseEntry(name, description, group, content: @Composable () -> Unit)` and `object ShowcaseRegistry { val entries: List<ShowcaseEntry> }` — consumed by Task 7 (app).

- [ ] **Step 1: Add new version catalog entries**

In `gradle/libs.versions.toml`, add to the `[versions]` block (near `kspVersion`):

```toml
kotlinCompileTestingCore = "0.12.1"
kotlinCompileTestingKsp = "0.8.0"
```

Add to the `[libraries]` block:

```toml
ksp-symbol-processing-api = { module = "com.google.devtools.ksp:symbol-processing-api", version.ref = "kspVersion" }
kotlin-compile-testing-core = { module = "dev.zacsweers.kctfork:core", version.ref = "kotlinCompileTestingCore" }
kotlin-compile-testing-ksp = { module = "dev.zacsweers.kctfork:ksp", version.ref = "kotlinCompileTestingKsp" }
kotlin-test-junit5 = { module = "org.jetbrains.kotlin:kotlin-test-junit5", version.ref = "kotlinTest" }
```

Add to the `[plugins]` block:

```toml
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
```

- [ ] **Step 2: Create the processor module build file**

`showcase/processor/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.showcase.annotations)
    implementation(libs.ksp.symbol.processing.api)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(commonLibs.runtime)
    testImplementation(libs.kotlin.compile.testing.core)
    testImplementation(libs.kotlin.compile.testing.ksp)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

- [ ] **Step 3: Write the processor provider**

`showcase/processor/src/main/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessorProvider.kt`:

```kotlin
package com.programmersbox.showcase.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ShowcaseSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ShowcaseSymbolProcessor(environment.codeGenerator, environment.logger)
}
```

- [ ] **Step 4: Write a no-op processor stub (fleshed out in later tasks)**

`showcase/processor/src/main/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessor.kt`:

```kotlin
package com.programmersbox.showcase.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

class ShowcaseSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        return emptyList()
    }
}
```

- [ ] **Step 5: Register the processor via META-INF/services**

`showcase/processor/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`:

```
com.programmersbox.showcase.processor.ShowcaseSymbolProcessorProvider
```

- [ ] **Step 6: Verify it builds**

Run: `./gradlew :showcase:processor:build`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml showcase/processor
git commit -m "feat(showcase): scaffold showcase:processor KSP module"
```

---

### Task 3: TDD — valid processing + name/description/group extraction

**Files:**
- Create: `showcase/processor/src/test/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessorTest.kt`
- Modify: `showcase/processor/src/main/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessor.kt`

**Interfaces:**
- Consumes: `ShowcaseSymbolProcessorProvider` (Task 2)
- Produces: working symbol-discovery + file-generation logic in `ShowcaseSymbolProcessor.process()`, reused unchanged by Tasks 4–6.

- [ ] **Step 1: Write the failing test**

`showcase/processor/src/test/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessorTest.kt`:

```kotlin
package com.programmersbox.showcase.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ShowcaseSymbolProcessorTest {

    private fun compile(source: SourceFile): KotlinCompilation.Result {
        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(ShowcaseSymbolProcessorProvider())
            inheritClassPath = true
        }
        return compilation.compile()
    }

    private fun generatedRegistrySource(source: SourceFile): String {
        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(ShowcaseSymbolProcessorProvider())
            inheritClassPath = true
        }
        compilation.compile()
        return File(
            compilation.kspSourcesDir,
            "kotlin/com/programmersbox/showcase/generated/ShowcaseRegistry.kt",
        ).readText()
    }

    @Test
    fun `valid zero-arg composable processes successfully and extracts name, description, group`() {
        val source = SourceFile.kotlin(
            "Sample.kt",
            """
            package test

            import androidx.compose.runtime.Composable
            import com.programmersbox.showcase.annotations.ShowcaseComponent

            @ShowcaseComponent(name = "Sample Button", description = "A sample button", group = "Buttons")
            @Composable
            fun SampleButton() {}
            """.trimIndent(),
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val generated = generatedRegistrySource(source)
        assertTrue(generated.contains("name = \"Sample Button\""))
        assertTrue(generated.contains("description = \"A sample button\""))
        assertTrue(generated.contains("group = \"Buttons\""))
        assertTrue(generated.contains("content = { test.SampleButton() }"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :showcase:processor:test --tests "*ShowcaseSymbolProcessorTest"`
Expected: FAIL — generated file assertions fail because `process()` currently returns `emptyList()` without generating anything (or file doesn't exist).

- [ ] **Step 3: Implement the processor's discovery, validation-free path, and generation logic**

Replace `showcase/processor/src/main/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessor.kt` with:

```kotlin
package com.programmersbox.showcase.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

private const val SHOWCASE_ANNOTATION = "com.programmersbox.showcase.annotations.ShowcaseComponent"
private const val COMPOSABLE_ANNOTATION = "androidx.compose.runtime.Composable"

private data class GeneratedEntry(
    val name: String,
    val description: String,
    val group: String,
    val qualifiedReference: String,
)

class ShowcaseSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val functions = resolver.getSymbolsWithAnnotation(SHOWCASE_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        val entries = functions.mapNotNull { function -> toEntryOrReportError(function) }

        val sortedEntries = entries.sortedWith(compareBy({ it.group }, { it.name }))
        val dependencies = Dependencies(
            aggregating = true,
            *functions.mapNotNull { it.containingFile }.toTypedArray(),
        )

        codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = "com.programmersbox.showcase.generated",
            fileName = "ShowcaseRegistry",
        ).bufferedWriter().use { writer -> writer.write(generateFileContents(sortedEntries)) }

        return emptyList()
    }

    private fun toEntryOrReportError(function: KSFunctionDeclaration): GeneratedEntry? {
        val functionName = function.simpleName.asString()

        val isComposable = function.annotations.any {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == COMPOSABLE_ANNOTATION
        }
        if (!isComposable) {
            logger.error(
                "Function '$functionName' is annotated with @ShowcaseComponent but is not a @Composable function",
                function,
            )
            return null
        }

        if (function.parameters.isNotEmpty()) {
            logger.error(
                "Function '$functionName' is annotated with @ShowcaseComponent but has parameters. Showcase components must have zero parameters.",
                function,
            )
            return null
        }

        val annotation = function.annotations.first {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == SHOWCASE_ANNOTATION
        }
        val args = annotation.arguments.associateBy { it.name?.asString() }
        val name = args["name"]?.value as? String ?: ""
        val description = args["description"]?.value as? String ?: ""
        val group = args["group"]?.value as? String ?: ""

        val packageName = function.packageName.asString()
        val qualifiedReference = if (packageName.isEmpty()) functionName else "$packageName.$functionName"

        return GeneratedEntry(name, description, group, qualifiedReference)
    }

    private fun generateFileContents(entries: List<GeneratedEntry>): String = buildString {
        appendLine("package com.programmersbox.showcase.generated")
        appendLine()
        appendLine("import androidx.compose.runtime.Composable")
        appendLine()
        appendLine("data class ShowcaseEntry(")
        appendLine("    val name: String,")
        appendLine("    val description: String,")
        appendLine("    val group: String,")
        appendLine("    val content: @Composable () -> Unit,")
        appendLine(")")
        appendLine()
        appendLine("object ShowcaseRegistry {")
        appendLine("    val entries: List<ShowcaseEntry> = listOf(")
        entries.forEach { entry ->
            appendLine("        ShowcaseEntry(")
            appendLine("            name = ${entry.name.quoted()},")
            appendLine("            description = ${entry.description.quoted()},")
            appendLine("            group = ${entry.group.quoted()},")
            appendLine("            content = { ${entry.qualifiedReference}() },")
            appendLine("        ),")
        }
        appendLine("    )")
        appendLine("}")
    }

    private fun String.quoted(): String =
        "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :showcase:processor:test --tests "*ShowcaseSymbolProcessorTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add showcase/processor
git commit -m "feat(showcase): implement KSP discovery, extraction, and registry generation"
```

---

### Task 4: TDD — error when missing `@Composable`

**Files:**
- Modify: `showcase/processor/src/test/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessorTest.kt`

**Interfaces:**
- Consumes: `ShowcaseSymbolProcessor` validation branch from Task 3 (already implemented — this task locks the behavior down with a test).

- [ ] **Step 1: Write the failing test**

Add to `ShowcaseSymbolProcessorTest`:

```kotlin
    @Test
    fun `missing Composable annotation produces the exact expected error`() {
        val source = SourceFile.kotlin(
            "Sample.kt",
            """
            package test

            import com.programmersbox.showcase.annotations.ShowcaseComponent

            @ShowcaseComponent(name = "Sample", description = "desc", group = "Group")
            fun NotComposable() {}
            """.trimIndent(),
        )

        val result = compile(source)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(
            result.messages.contains(
                "Function 'NotComposable' is annotated with @ShowcaseComponent but is not a @Composable function"
            )
        )
    }
```

- [ ] **Step 2: Run test to verify it currently passes (behavior already implemented in Task 3)**

Run: `./gradlew :showcase:processor:test --tests "*ShowcaseSymbolProcessorTest"`
Expected: PASS — this locks down existing behavior with an explicit regression test; no production code change needed for this task.

- [ ] **Step 3: Commit**

```bash
git add showcase/processor/src/test
git commit -m "test(showcase): lock down missing-@Composable error message"
```

---

### Task 5: TDD — error when function has parameters

**Files:**
- Modify: `showcase/processor/src/test/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessorTest.kt`

**Interfaces:**
- Consumes: `ShowcaseSymbolProcessor` validation branch from Task 3 (already implemented).

- [ ] **Step 1: Write the failing test**

Add to `ShowcaseSymbolProcessorTest`:

```kotlin
    @Test
    fun `function with parameters produces the exact expected error`() {
        val source = SourceFile.kotlin(
            "Sample.kt",
            """
            package test

            import androidx.compose.runtime.Composable
            import com.programmersbox.showcase.annotations.ShowcaseComponent

            @ShowcaseComponent(name = "Sample", description = "desc", group = "Group")
            @Composable
            fun WithParams(text: String) {}
            """.trimIndent(),
        )

        val result = compile(source)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(
            result.messages.contains(
                "Function 'WithParams' is annotated with @ShowcaseComponent but has parameters. Showcase components must have zero parameters."
            )
        )
    }
```

- [ ] **Step 2: Run test to verify it currently passes**

Run: `./gradlew :showcase:processor:test --tests "*ShowcaseSymbolProcessorTest"`
Expected: PASS — locks down existing behavior; no production code change needed.

- [ ] **Step 3: Commit**

```bash
git add showcase/processor/src/test
git commit -m "test(showcase): lock down has-parameters error message"
```

---

### Task 6: TDD — deterministic sort across multiple groups

**Files:**
- Modify: `showcase/processor/src/test/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessorTest.kt`

**Interfaces:**
- Consumes: `ShowcaseSymbolProcessor` sorting behavior from Task 3 (already implemented — `sortedWith(compareBy({ it.group }, { it.name }))`).

- [ ] **Step 1: Write the failing test**

Add to `ShowcaseSymbolProcessorTest`:

```kotlin
    @Test
    fun `entries across multiple groups are sorted by group then name regardless of declaration order`() {
        val source = SourceFile.kotlin(
            "Sample.kt",
            """
            package test

            import androidx.compose.runtime.Composable
            import com.programmersbox.showcase.annotations.ShowcaseComponent

            @ShowcaseComponent(name = "Zeta", description = "z", group = "Widgets")
            @Composable
            fun ZetaWidget() {}

            @ShowcaseComponent(name = "Alpha", description = "a", group = "Widgets")
            @Composable
            fun AlphaWidget() {}

            @ShowcaseComponent(name = "Only", description = "c", group = "Cards")
            @Composable
            fun CardSample() {}
            """.trimIndent(),
        )

        val generated = generatedRegistrySource(source)

        val cardIndex = generated.indexOf("name = \"Only\"")
        val alphaIndex = generated.indexOf("name = \"Alpha\"")
        val zetaIndex = generated.indexOf("name = \"Zeta\"")

        assertTrue(cardIndex in 0 until alphaIndex, "Cards group ('Only') must come before Widgets group entries")
        assertTrue(alphaIndex in 0 until zetaIndex, "Alpha must come before Zeta within the Widgets group")
    }
```

- [ ] **Step 2: Run test to verify it currently passes**

Run: `./gradlew :showcase:processor:test --tests "*ShowcaseSymbolProcessorTest"`
Expected: PASS — sorting was already implemented in Task 3; this test covers the multi-group + deterministic-sort spec requirements explicitly.

- [ ] **Step 3: Run the full processor test suite**

Run: `./gradlew :showcase:processor:test`
Expected: all 4 tests PASS (valid processing + extraction, missing-@Composable error, has-parameters error, multi-group sort).

- [ ] **Step 4: Commit**

```bash
git add showcase/processor/src/test
git commit -m "test(showcase): lock down deterministic multi-group sort order"
```

---

### Task 7: `:showcase` Compose Desktop app

**Files:**
- Create: `showcase/build.gradle.kts`
- Create: `showcase/src/main/kotlin/com/programmersbox/showcase/Main.kt`
- Create: `showcase/src/main/kotlin/com/programmersbox/showcase/App.kt`
- Create: `showcase/src/main/kotlin/com/programmersbox/showcase/samples/ButtonSamples.kt`
- Create: `showcase/src/main/kotlin/com/programmersbox/showcase/samples/CardSamples.kt`

**Interfaces:**
- Consumes: `com.programmersbox.showcase.annotations.ShowcaseComponent` (Task 1), KSP-generated `com.programmersbox.showcase.generated.ShowcaseRegistry` / `ShowcaseEntry` (Task 2-6's processor, applied here via `ksp(projects.showcase.processor)`).
- Produces: `fun main()` entry point; nothing else depends on this module.

- [ ] **Step 1: Create the app module build file**

`showcase/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.showcase.annotations)
    ksp(projects.showcase.processor)

    implementation(commonLibs.compose.material3)
    implementation(commonLibs.material.icons.extended)
    implementation(commonLibs.runtime)
    implementation(commonLibs.ui)
    implementation(commonLibs.foundation)
    implementation(commonLibs.cmp.ui.util)
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.programmersbox.showcase.MainKt"
    }
}
```

- [ ] **Step 2: Write sample annotated composables (Buttons group)**

`showcase/src/main/kotlin/com/programmersbox/showcase/samples/ButtonSamples.kt`:

```kotlin
package com.programmersbox.showcase.samples

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.programmersbox.showcase.annotations.ShowcaseComponent

@ShowcaseComponent(
    name = "Primary Button",
    description = "Standard filled Material 3 button.",
    group = "Buttons",
)
@Composable
fun PrimaryButtonSample() {
    Button(onClick = {}) {
        Text("Primary Button")
    }
}

@ShowcaseComponent(
    name = "Text Button",
    description = "Low-emphasis text-only Material 3 button.",
    group = "Buttons",
)
@Composable
fun TextButtonSample() {
    TextButton(onClick = {}) {
        Text("Text Button")
    }
}
```

- [ ] **Step 3: Write sample annotated composables (Cards group)**

`showcase/src/main/kotlin/com/programmersbox/showcase/samples/CardSamples.kt`:

```kotlin
package com.programmersbox.showcase.samples

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.showcase.annotations.ShowcaseComponent

@ShowcaseComponent(
    name = "Simple Card",
    description = "A basic Material 3 card with text content.",
    group = "Cards",
)
@Composable
fun SimpleCardSample() {
    Card {
        Text(
            text = "Card content",
            modifier = Modifier.padding(16.dp),
        )
    }
}
```

- [ ] **Step 4: Write the root App composable with NavigationRail**

`showcase/src/main/kotlin/com/programmersbox/showcase/App.kt`:

```kotlin
package com.programmersbox.showcase

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.showcase.generated.ShowcaseEntry
import com.programmersbox.showcase.generated.ShowcaseRegistry

private const val ALL_GROUP = "All"

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            var selectedGroup by remember { mutableStateOf<String?>(null) }
            val groups = remember { ShowcaseRegistry.entries.map { it.group }.distinct().sorted() }

            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    NavigationRailItem(
                        selected = selectedGroup == ALL_GROUP,
                        onClick = { selectedGroup = ALL_GROUP },
                        icon = { Icon(Icons.Default.Apps, contentDescription = ALL_GROUP) },
                        label = { Text(ALL_GROUP) },
                    )
                    groups.forEach { group ->
                        NavigationRailItem(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            icon = { Icon(Icons.Default.Apps, contentDescription = group) },
                            label = { Text(group) },
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when (val group = selectedGroup) {
                        null -> WelcomePlaceholder()
                        else -> {
                            val entries = if (group == ALL_GROUP) {
                                ShowcaseRegistry.entries
                            } else {
                                ShowcaseRegistry.entries.filter { it.group == group }
                            }
                            ComponentList(entries)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Select a group from the rail to browse components")
    }
}

@Composable
private fun ComponentList(entries: List<ShowcaseEntry>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(entries) { entry ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(entry.name, style = MaterialTheme.typography.titleMedium)
                    Text(entry.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    entry.content()
                }
            }
        }
    }
}
```

- [ ] **Step 5: Write the entry point**

`showcase/src/main/kotlin/com/programmersbox/showcase/Main.kt`:

```kotlin
package com.programmersbox.showcase

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Component Showcase") {
        App()
    }
}
```

- [ ] **Step 6: Verify it builds**

Run: `./gradlew :showcase:build`
Expected: `BUILD SUCCESSFUL` (this also runs KSP, generating `ShowcaseRegistry.kt` from the 3 sample composables above).

- [ ] **Step 7: Manually verify the app launches**

Run: `./gradlew :showcase:run`
Expected: a desktop window titled "Component Showcase" opens with a `NavigationRail` showing "All", "Buttons", "Cards". Clicking each shows the matching sample components with live previews; nothing selected shows the placeholder text.

- [ ] **Step 8: Commit**

```bash
git add showcase/build.gradle.kts showcase/src
git commit -m "feat(showcase): add Compose Desktop showcase app with NavigationRail UI"
```

---

### Task 8: README

**Files:**
- Create: `showcase/README.md`

**Interfaces:** None — documentation only.

- [ ] **Step 1: Write the module README**

`showcase/README.md`:

```markdown
# Component Showcase

A standalone Compose Desktop app for browsing this repo's `@Composable` UI components in
isolation. Annotate any zero-parameter composable with `@ShowcaseComponent`, rebuild, and it
appears in the showcase app automatically.

This is a developer tool only — it is not part of MangaWorld/AnimeWorld/NovelWorld's runtime.

## Modules

- **`:showcase:annotations`** — Kotlin Multiplatform module holding the `@ShowcaseComponent`
  annotation (`name`, `description`, `group`; source retention; function target).
- **`:showcase:processor`** — a KSP `SymbolProcessor` that finds every `@ShowcaseComponent`
  function, validates it, and generates `com.programmersbox.showcase.generated.ShowcaseRegistry`
  — a `List<ShowcaseEntry>` sorted alphabetically by group, then by name.
- **`:showcase`** — the Compose Desktop app itself. Renders the generated registry behind a
  Material 3 `NavigationRail` (one rail item per group, plus "All"), with live-rendered previews
  of each component.

## Annotating a composable

```kotlin
import androidx.compose.runtime.Composable
import com.programmersbox.showcase.annotations.ShowcaseComponent

@ShowcaseComponent(
    name = "Primary Button",
    description = "Standard filled Material 3 button.",
    group = "Buttons",
)
@Composable
fun PrimaryButtonSample() {
    Button(onClick = {}) { Text("Primary Button") }
}
```

Requirements, enforced at compile time by the processor:
- The function must also be annotated `@Composable`.
- The function must take zero parameters (wrap parameterized components in a zero-arg
  composable if you want to showcase them with fixed sample data).

## Running the app

```bash
./gradlew :showcase:run
```

Rebuild (`./gradlew :showcase:processor:build` runs automatically as part of `:showcase:build`)
whenever you add or change a `@ShowcaseComponent` annotation — there's no hot-reload.
```

- [ ] **Step 2: Commit**

```bash
git add showcase/README.md
git commit -m "docs(showcase): add showcase module README"
```
