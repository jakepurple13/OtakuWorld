# Component Showcase System — Design

## Purpose

Standalone Compose Desktop app that discovers `@Composable` functions annotated with a custom
`@ShowcaseComponent` annotation (via KSP) and renders them in a browsable catalog UI. Lets a
developer visually browse UI components grouped by category, with live rendered previews,
without touching the main app's runtime.

This first version is **self-contained**: the annotated sample composables live inside the
`:showcase` module itself. No other app module (`mangaworld`, `UIViews`, `kmpuiviews`, etc.) is
touched. Real components can be wired in later by any module that adds the `annotations` +
`ksp` dependencies itself — out of scope here.

## Modules

```
:showcase                 (parent container AND the Compose Desktop app itself, JVM-only)
:showcase:annotations      (KMP common — the @ShowcaseComponent annotation)
:showcase:processor        (JVM-only KSP SymbolProcessor)
```

### `:showcase:annotations`

- KMP library module (`otaku-multiplatform`-style convention plugin, full default hierarchy
  template, mirroring `kmpmodels`), even though only the `jvm` target is consumed today — kept
  KMP so any Android/iOS-targeting module can later depend on it without a rewrite.
- Package: `com.programmersbox.showcase.annotations`
- Single file, `ShowcaseComponent.kt`:

```kotlin
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ShowcaseComponent(
    val name: String,
    val description: String,
    val group: String,
)
```

### `:showcase:processor`

- Plain `kotlin("jvm")` module (not KMP — only ever runs as a KSP processor on the JVM).
- Package: `com.programmersbox.showcase.processor`
- Depends on: `:showcase:annotations` (jvm), `com.google.devtools.ksp:symbol-processing-api`
  (new lib entry in `gradle/libs.versions.toml`, version pinned to `kspVersion` = `2.3.10`).
- `ShowcaseSymbolProcessor : SymbolProcessor`:
  1. Query `resolver.getSymbolsWithAnnotation("com.programmersbox.showcase.annotations.ShowcaseComponent")`.
  2. For each annotated `KSFunctionDeclaration`:
     - If not also annotated `@Composable` → `logger.error("Function '<name>' is annotated with @ShowcaseComponent but is not a @Composable function", node)`, skip it (don't generate an entry for it).
     - If `parameters.isNotEmpty()` → `logger.error("Function '<name>' is annotated with @ShowcaseComponent but has parameters. Showcase components must have zero parameters.", node)`, skip it.
     - Otherwise extract `name`/`description`/`group` from the annotation arguments and record `(name, description, group, qualifiedFunctionRef)`.
  3. On the pass that has processed all sources (first `process()` invocation collects, since we need every file before we can sort — implemented as a single-round aggregating processor that emits nothing until `finish()`, using `Dependencies(aggregating = true, *allSourceFiles)`), emit `com/programmersbox/showcase/generated/ShowcaseRegistry.kt`:

```kotlin
package com.programmersbox.showcase.generated

data class ShowcaseEntry(
    val name: String,
    val description: String,
    val group: String,
    val content: @Composable () -> Unit,
)

object ShowcaseRegistry {
    val entries: List<ShowcaseEntry> = listOf(
        ShowcaseEntry("...", "...", "...", { com.example.SomeComposable() }),
        // sorted by group, then name
    )
}
```

  - Entries sorted with `sortedWith(compareBy({ it.group }, { it.name }))` before being written,
    guaranteeing deterministic output across rebuilds regardless of source-file processing order.
- Registered via `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`
  in `src/main/resources`.
- Incremental: pass real source `KSFile`s into `Dependencies(aggregating = true, ...)` rather
  than `Dependencies(false)`.

### `:showcase` (app module)

- Plain `kotlin("jvm")` + `org.jetbrains.compose` (Compose Desktop) + `ksp` plugin. Not KMP —
  simplest option since this app never targets anything but desktop.
- Package: `com.programmersbox.showcase`
- Depends on `:showcase:annotations`; `ksp(project(":showcase:processor"))`.
- Contains a handful of sample `@ShowcaseComponent`-annotated composables across 2–3 groups
  (e.g. "Buttons", "Cards") purely to prove the pipeline end-to-end.
- `main.kt` — standalone `fun main()` launching a Compose Desktop `Window`/`application`.
- `App.kt` — root layout:
  - `Row` — `NavigationRail` (M3) on the left, content `Box` on the right.
  - Rail: one `NavigationRailItem` for `"All"` plus one per distinct `group` present in
    `ShowcaseRegistry.entries` (derived at runtime, not hardcoded). Same generic icon
    (`Icons.Default.Widgets`) on every item — labels differentiate groups. Selected state via
    `remember { mutableStateOf<String?>(null) }` (`null` = nothing selected yet / show
    placeholder; `"All"` is a distinct selectable state showing everything).
  - Content area:
    - Nothing selected (initial load) → centered placeholder/welcome text.
    - `"All"` selected → scrollable list of every entry (`LazyColumn`), sorted as generated.
    - A specific group selected → scrollable list filtered to that group.
    - Each entry rendered as a `Card`: `name` (title style), `description` (body/subtitle
      style), then `entry.content()` rendered live beneath.
  - No search/filter box (explicitly dropped per user decision — deviates from the original
    prompt's "search across all groups" requirement).

## Gradle wiring

- `settings.gradle.kts`: add `include(":showcase", ":showcase:annotations", ":showcase:processor")`.
- `gradle/libs.versions.toml`: add `ksp-api` library entry (`com.google.devtools.ksp:symbol-processing-api:2.3.10`, matching `kspVersion`), and `kotlin-compile-testing-ksp` (test-only, for the processor's unit tests — exact version resolved at implementation time to whatever's compatible with Kotlin 2.4.10 / KSP 2.3.10).
- `:showcase:processor/build.gradle.kts` depends on `:showcase:annotations`.
- `:showcase/build.gradle.kts` applies Compose Desktop + KSP, depends on `:showcase:annotations`, `ksp(project(":showcase:processor"))`.

## Testing

`:showcase:processor` gets unit tests using `kotlin-compile-testing-ksp` (`com.tschuchort:kotlin-compile-testing-ksp`), covering:

1. Valid zero-arg `@Composable` + `@ShowcaseComponent` function processes successfully.
2. Error: annotated function missing `@Composable` → exact expected error message asserted.
3. Error: annotated function has parameters → exact expected error message asserted.
4. `name`/`description`/`group` correctly extracted into the generated `ShowcaseEntry`.
5. Deterministic sort: entries across multiple groups come out sorted by group then name, regardless of declaration order in the source.
6. Multi-group scenario: several annotated functions across 3+ distinct groups all appear, correctly grouped.

## README

Add `showcase/README.md` (module-level docs, matching the existing `app/readme.md` convention in this repo) with:
- What the showcase system is and why it exists
- How to annotate a composable with `@ShowcaseComponent`
- How to run the showcase desktop app (`./gradlew :showcase:run`)
- The three-module breakdown
- A short usage example

## Out of scope (unchanged from original prompt)

- Auto-generating wrapper composables for parameterized functions
- Hot-reload of the showcase app itself
- Distributable/installer packaging
- Non-desktop targets for the `:showcase` app module
- Editing component code from within the showcase app
- Any interaction with the main app's runtime/entry point
- Dark/light theme toggle within the showcase app
- **Search/filter box** (dropped per explicit user decision during brainstorming — original prompt called for one, user opted out)
