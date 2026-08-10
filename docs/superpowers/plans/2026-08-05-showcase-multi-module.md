# Showcase Multi-Module Aggregation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let multiple Gradle modules apply the showcase KSP processor and have all of their annotated components show up together in the `:showcase` app, replacing the current fixed-package-name design that silently drops every module but one.

**Architecture:** `ShowcaseEntry` and a new `ShowcaseRegistryProvider` interface move into `:showcase:annotations` as shared types. The processor reads a required `showcaseModuleId` KSP arg and generates one uniquely-named class per module implementing `ShowcaseRegistryProvider`, plus a `META-INF/services` registration for it. `:showcase`'s `App.kt` discovers all of them at runtime via `java.util.ServiceLoader` instead of importing one fixed generated object.

**Tech Stack:** Same as the original showcase feature — Kotlin, KSP, Compose Multiplatform/Desktop. New: `java.util.ServiceLoader` (JDK standard library, no new dependency).

## Global Constraints

- Design spec: `docs/superpowers/specs/2026-08-05-showcase-multi-module-design.md`.
- `ShowcaseEntry` and `ShowcaseRegistryProvider` live in package `com.programmersbox.showcase.annotations`.
- The generated per-module class stays in package `com.programmersbox.showcase.generated`; uniqueness comes from the class name (derived from `showcaseModuleId`), not a per-module package.
- Missing/blank `showcaseModuleId` must fail the build loudly via `KSPLogger.error(...)` — no silent fallback.
- **Do not modify `showcase/processor/src/test/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessorTest.kt`** — the existing test suite is explicitly left outdated/failing as a deliberate, separately-tracked decision (see the design spec's Testing section). Do not "fix" it as a side effect of any task in this plan.
- Cross-module sort ordering of the final merged list is explicitly out of scope.
- This plan builds on top of already-existing (previously uncommitted, now applied to this worktree) changes to `kmpuiviews/build.gradle.kts` and `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/components/BackButton.kt` — Task 4 commits those together with its own addition.

---

### Task 1: Shared types in `:showcase:annotations`

**Files:**
- Modify: `showcase/annotations/build.gradle.kts`
- Create: `showcase/annotations/src/commonMain/kotlin/com/programmersbox/showcase/annotations/ShowcaseEntry.kt`
- Create: `showcase/annotations/src/commonMain/kotlin/com/programmersbox/showcase/annotations/ShowcaseRegistryProvider.kt`

**Interfaces:**
- Produces: `com.programmersbox.showcase.annotations.ShowcaseEntry` (data class) and `com.programmersbox.showcase.annotations.ShowcaseRegistryProvider` (interface with `val entries: List<ShowcaseEntry>`), consumed by Task 2 (processor codegen) and Task 3 (`App.kt`).

- [ ] **Step 1: Add the compose-runtime dependency**

In `showcase/annotations/build.gradle.kts`, add a `commonMain` dependencies block (there currently isn't one) right after the `kotlin { ... }` block's opening, inside the existing `kotlin { }` block:

```kotlin
kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.programmersbox.showcase.annotations"
        compileSdk = AppInfo.compileVersion
        minSdk = AppInfo.minimumSdk
    }

    val xcfName = "sharedKit"

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = xcfName
            isStatic = true
        }
    }

    jvm()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(commonLibs.runtime)
        }
    }
}
```

(Only the `sourceSets { ... }` block at the end is new — everything above it is the file's existing content, shown for placement context.)

- [ ] **Step 2: Write `ShowcaseEntry`**

`showcase/annotations/src/commonMain/kotlin/com/programmersbox/showcase/annotations/ShowcaseEntry.kt`:

```kotlin
package com.programmersbox.showcase.annotations

import androidx.compose.runtime.Composable

data class ShowcaseEntry(
    val name: String,
    val description: String,
    val group: String,
    val content: @Composable () -> Unit,
)
```

- [ ] **Step 3: Write `ShowcaseRegistryProvider`**

`showcase/annotations/src/commonMain/kotlin/com/programmersbox/showcase/annotations/ShowcaseRegistryProvider.kt`:

```kotlin
package com.programmersbox.showcase.annotations

interface ShowcaseRegistryProvider {
    val entries: List<ShowcaseEntry>
}
```

- [ ] **Step 4: Verify it builds**

Run: `./gradlew :showcase:annotations:build`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add showcase/annotations
git commit -m "feat(showcase): add shared ShowcaseEntry and ShowcaseRegistryProvider types"
```

---

### Task 2: Processor rewrite — per-module id, per-module class, ServiceLoader registration

**Files:**
- Modify: `showcase/processor/src/main/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessor.kt`
- Modify: `showcase/processor/src/main/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessorProvider.kt`

**Interfaces:**
- Consumes: `ShowcaseEntry`, `ShowcaseRegistryProvider` (Task 1).
- Produces: per-module generated class implementing `ShowcaseRegistryProvider` (consumed at runtime via `ServiceLoader` by Task 3's `App.kt`), and the `showcaseModuleId` KSP arg contract (consumed by every module's `build.gradle.kts`, Tasks 3 and 4).

- [ ] **Step 1: Read `showcaseModuleId` in the provider, fail loudly if missing**

Replace `showcase/processor/src/main/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessorProvider.kt` with:

```kotlin
package com.programmersbox.showcase.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ShowcaseSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val moduleId = environment.options["showcaseModuleId"]
        if (moduleId.isNullOrBlank()) {
            environment.logger.error(
                "The showcase processor requires a 'showcaseModuleId' KSP argument. " +
                    "Add `ksp { arg(\"showcaseModuleId\", \"<unique-module-name>\") }` " +
                    "to this module's build.gradle.kts."
            )
        }
        return ShowcaseSymbolProcessor(environment.codeGenerator, environment.logger, moduleId.orEmpty())
    }
}
```

- [ ] **Step 2: Rewrite the processor to generate a per-module class + META-INF/services resource**

Replace `showcase/processor/src/main/kotlin/com/programmersbox/showcase/processor/ShowcaseSymbolProcessor.kt` with:

```kotlin
package com.programmersbox.showcase.processor

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Visibility

private const val SHOWCASE_ANNOTATION = "com.programmersbox.showcase.annotations.ShowcaseComponent"
private const val COMPOSABLE_ANNOTATION = "androidx.compose.runtime.Composable"
private const val PROVIDER_INTERFACE = "com.programmersbox.showcase.annotations.ShowcaseRegistryProvider"
private const val GENERATED_PACKAGE = "com.programmersbox.showcase.generated"

private data class GeneratedEntry(
    val name: String,
    val description: String,
    val group: String,
    val qualifiedReference: String,
)

class ShowcaseSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val moduleId: String,
) : SymbolProcessor {

    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // KSP re-invokes process() in a second round after this processor generates a new file
        // (the round loop continues as long as any processor produced a new source file). Since
        // the generated registry never contains new @ShowcaseComponent-annotated functions, and
        // this processor only needs a single pass over the user's sources, guard against
        // generating (and thus re-creating) the same output file on that second round.
        if (invoked) return emptyList()
        invoked = true

        if (moduleId.isBlank()) return emptyList()

        val functions = resolver.getSymbolsWithAnnotation(SHOWCASE_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        val entries = functions.mapNotNull { function -> toEntryOrReportError(function) }

        val sortedEntries = entries.sortedWith(compareBy({ it.group }, { it.name }, { it.qualifiedReference }))
        val dependencies = Dependencies(
            aggregating = true,
            *functions.mapNotNull { it.containingFile }.toTypedArray(),
        )

        val className = "${sanitizedModuleId()}ShowcaseRegistryProvider"
        val qualifiedClassName = "$GENERATED_PACKAGE.$className"

        codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = GENERATED_PACKAGE,
            fileName = className,
        ).bufferedWriter().use { writer -> writer.write(generateFileContents(className, sortedEntries)) }

        codeGenerator.createNewFileByPath(
            dependencies = dependencies,
            path = "META-INF/services/$PROVIDER_INTERFACE",
            extensionName = "",
        ).bufferedWriter().use { writer -> writer.write(qualifiedClassName) }

        return emptyList()
    }

    private fun sanitizedModuleId(): String {
        val sanitized = moduleId.replace(Regex("[^A-Za-z0-9]"), "_")
        return sanitized.replaceFirstChar { it.uppercase() }
    }

    private fun toEntryOrReportError(function: KSFunctionDeclaration): GeneratedEntry? {
        val functionName = function.simpleName.asString()

        if (function.parentDeclaration != null) {
            logger.error(
                "Function '$functionName' is annotated with @ShowcaseComponent but is not a top-level function. Showcase components must be top-level.",
                function,
            )
            return null
        }

        if (function.getVisibility() == Visibility.PRIVATE) {
            logger.error(
                "Function '$functionName' is annotated with @ShowcaseComponent but is private. Showcase components must not be private.",
                function,
            )
            return null
        }

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

    private fun generateFileContents(className: String, entries: List<GeneratedEntry>): String = buildString {
        appendLine("package $GENERATED_PACKAGE")
        appendLine()
        appendLine("import com.programmersbox.showcase.annotations.ShowcaseEntry")
        appendLine("import com.programmersbox.showcase.annotations.ShowcaseRegistryProvider")
        appendLine()
        appendLine("class $className : ShowcaseRegistryProvider {")
        appendLine("    override val entries: List<ShowcaseEntry> = listOf(")
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

Note: `CodeGenerator.createNewFileByPath(dependencies, path, extensionName)` is the API used for
writing a resource file (as opposed to `createNewFile`, which is for Kotlin/Java source files) —
verify this exact method name and signature against the actual KSP 2.3.10 jar if it doesn't
resolve as written (check `~/.gradle/caches` for the `symbol-processing-api` jar and inspect its
`CodeGenerator` interface) rather than guessing further; this is the same class of
verify-against-the-real-jar risk this project's processor work has hit before.

- [ ] **Step 3: Verify the processor module's production code compiles**

Run: `./gradlew :showcase:processor:compileKotlin`
Expected: `BUILD SUCCESSFUL`. Do NOT run `:showcase:processor:test` — per Global Constraints, the existing test suite is expected to fail against this change and that's a separately-tracked, deliberate gap, not something to fix here.

- [ ] **Step 4: Commit**

```bash
git add showcase/processor
git commit -m "feat(showcase): generate per-module registry providers via ServiceLoader instead of a fixed registry object"
```

---

### Task 3: `:showcase` app — own module id, ServiceLoader consumption

**Files:**
- Modify: `showcase/build.gradle.kts`
- Modify: `showcase/src/main/kotlin/com/programmersbox/showcase/App.kt`

**Interfaces:**
- Consumes: `ShowcaseEntry`, `ShowcaseRegistryProvider` (Task 1); the `showcaseModuleId` KSP arg contract and generated-class/META-INF output (Task 2).
- Produces: nothing new consumed elsewhere — this is the leaf app module.

- [ ] **Step 1: Add the module's own `showcaseModuleId`**

In `showcase/build.gradle.kts`, add a `ksp { }` block after the `dependencies { }` block:

```kotlin
ksp {
    arg("showcaseModuleId", "showcase")
}
```

- [ ] **Step 2: Replace the fixed `ShowcaseRegistry` import with ServiceLoader discovery**

In `showcase/src/main/kotlin/com/programmersbox/showcase/App.kt`:

Replace:
```kotlin
import com.programmersbox.showcase.generated.ShowcaseEntry
import com.programmersbox.showcase.generated.ShowcaseRegistry
```
with:
```kotlin
import com.programmersbox.showcase.annotations.ShowcaseEntry
import com.programmersbox.showcase.annotations.ShowcaseRegistryProvider
import java.util.ServiceLoader
```

Add, near the top of the file (module-level, alongside the existing `private const val ALL_GROUP = "All"`):

```kotlin
private val allEntries: List<ShowcaseEntry> by lazy {
    ServiceLoader.load(ShowcaseRegistryProvider::class.java).flatMap { it.entries }
}
```

Inside `App()`, replace every reference to `ShowcaseRegistry.entries` with `allEntries` (there are
two: the `groups` computation and the `"All"`-selected branch).

- [ ] **Step 3: Verify it builds**

Run: `./gradlew :showcase:build`
Expected: `BUILD SUCCESSFUL`. Inspect the generated output directly to confirm the new shape: `cat showcase/build/generated/ksp/main/kotlin/com/programmersbox/showcase/generated/ShowcaseShowcaseRegistryProvider.kt` should show a class (not the old `object ShowcaseRegistry`) containing the 3 sample entries, and `cat showcase/build/generated/ksp/main/resources/META-INF/services/com.programmersbox.showcase.annotations.ShowcaseRegistryProvider` should contain the fully-qualified class name `com.programmersbox.showcase.generated.ShowcaseShowcaseRegistryProvider`.

- [ ] **Step 4: Commit**

```bash
git add showcase/build.gradle.kts showcase/src/main/kotlin/com/programmersbox/showcase/App.kt
git commit -m "feat(showcase): consume registries via ServiceLoader instead of a fixed import"
```

---

### Task 4: Complete `kmpuiviews` wiring

**Files:**
- Modify: `kmpuiviews/build.gradle.kts` (already has uncommitted changes applied to this worktree — see Global Constraints — this task adds one more block and commits everything together)

**Interfaces:**
- Consumes: the `showcaseModuleId` KSP arg contract (Task 2); `ShowcaseComponent`/`ShowcaseEntry`/`ShowcaseRegistryProvider` (Task 1, plus the pre-existing `:showcase:annotations` module).

`kmpuiviews/build.gradle.kts` already has (from a prior uncommitted change, now applied to this
worktree) `implementation(projects.showcase.annotations)` in `commonMain.dependencies` and
`add("kspJvm", projects.showcase.processor)` in a top-level `dependencies { }` block. `BackButton.kt`
already has `@ShowcaseComponent(name = "Back Button", description = "A simple back button.", group = "Buttons")`
applied. Read both files first to see their current (already-modified, uncommitted) state before
editing.

- [ ] **Step 1: Add the missing `showcaseModuleId` arg**

In `kmpuiviews/build.gradle.kts`, add near the existing `dependencies { add("kspJvm", projects.showcase.processor) }` block:

```kotlin
ksp {
    arg("showcaseModuleId", "kmpuiviews")
}
```

- [ ] **Step 2: Verify it builds**

Run: `./gradlew :kmpuiviews:compileKotlinJvm`
Expected: `BUILD SUCCESSFUL`. Then inspect the generated output: find and `cat` the generated
`KmpuiviewsShowcaseRegistryProvider.kt` under `kmpuiviews/build/generated/ksp/jvm/jvmMain/kotlin/com/programmersbox/showcase/generated/` (path may vary slightly by KMP target layout — locate it with `find kmpuiviews/build/generated -name '*ShowcaseRegistryProvider.kt'` if the exact path differs) and confirm it contains a `Back Button` entry in group `Buttons`.

- [ ] **Step 3: Verify end-to-end via the app**

Run: `./gradlew :showcase:run` and confirm (or, if visual confirmation isn't possible in this environment, confirm via the same non-visual means used previously — process launches cleanly, no exceptions) that a "Buttons" group entry for "Back Button" is now reachable. At minimum, confirm via build artifacts: `:showcase:build` succeeds with `:kmpuiviews` on its runtime classpath (already true via the existing `implementation(projects.kmpuiviews)` dependency in `showcase/build.gradle.kts`), and both `showcase/build/generated/.../META-INF/services/...` and `kmpuiviews`'s equivalent META-INF resource exist and name different classes.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/build.gradle.kts kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/components/BackButton.kt
git commit -m "feat(kmpuiviews): register BackButton with the component showcase"
```

---

### Task 5: README — replace "Current limitation" with real setup instructions

**Files:**
- Modify: `showcase/README.md`

**Interfaces:** None — documentation only.

- [ ] **Step 1: Replace the intro's forward-reference and the "Current limitation" section**

Replace:
```markdown
A standalone Compose Desktop app for browsing `@Composable` UI components in isolation. Annotate
any zero-parameter composable in the `:showcase` module with `@ShowcaseComponent`, rebuild, and it
appears in the showcase app automatically (see "Current limitation" below).
```
with:
```markdown
A standalone Compose Desktop app for browsing `@Composable` UI components in isolation. Annotate
any zero-parameter composable with `@ShowcaseComponent` in any module wired up per "Adding a new
module" below, rebuild, and it appears in the showcase app automatically.
```

Replace the entire `## Current limitation` section with:
```markdown
## Adding a new module

Any module can contribute components to the showcase. To wire one up:

1. Add dependencies: `implementation(projects.showcase.annotations)` and
   `ksp(projects.showcase.processor)` (or the target-specific KSP configuration, e.g. `kspJvm` for
   a Kotlin Multiplatform module's JVM target).
2. Give it a unique module id via the `ksp { }` Gradle DSL:
   ```kotlin
   ksp {
       arg("showcaseModuleId", "your-module-name")
   }
   ```
   This must be unique across every module that applies the processor — it's what keeps each
   module's generated registry class from colliding with another's. Missing or blank fails the
   build with a clear error.
3. Make sure the module ends up as a (direct or transitive) dependency of `:showcase` itself —
   only then will its generated registry actually be on the showcase app's runtime classpath for
   `ServiceLoader` to find.
4. Annotate composables with `@ShowcaseComponent` as usual.

Each module's components are discovered automatically at runtime via `java.util.ServiceLoader` —
no changes to the showcase app itself are needed when a new module is added.
```

- [ ] **Step 2: Commit**

```bash
git add showcase/README.md
git commit -m "docs(showcase): document multi-module setup, replacing the old single-module limitation note"
```
