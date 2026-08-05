# Showcase Multi-Module Aggregation — Design

## Purpose

The original component showcase (see
`docs/superpowers/specs/2026-08-05-component-showcase-design.md`) generates a fixed
`com.programmersbox.showcase.generated.ShowcaseRegistry` object per module that applies the KSP
processor. This breaks the moment a second module (e.g. `kmpuiviews`) applies the processor too:
both modules generate a class with the exact same fully-qualified name, and whichever one is
compiled locally into `:showcase`'s own output silently wins — the other module's annotated
components are generated but never actually shown. This was hit in practice: `kmpuiviews` was
wired up with `BackButton` annotated, and `BackButton` never appeared in the showcase app.

This design lets any number of modules apply the processor and have all of their annotated
components show up together in the showcase app, with no changes to `App.kt` required when a new
module is added — only that new module's own `build.gradle.kts`.

## Shared types move into `:showcase:annotations`

`ShowcaseEntry` and a new `ShowcaseRegistryProvider` marker interface become real, hand-written
classes in `com.programmersbox.showcase.annotations` (same package as `@ShowcaseComponent`) —
every module that applies the processor already depends on this module, so this needs no new
dependency:

```kotlin
package com.programmersbox.showcase.annotations

import androidx.compose.runtime.Composable

data class ShowcaseEntry(
    val name: String,
    val description: String,
    val group: String,
    val content: @Composable () -> Unit,
)

interface ShowcaseRegistryProvider {
    val entries: List<ShowcaseEntry>
}
```

`showcase/annotations/build.gradle.kts` currently has zero dependencies — it needs
`commonMain.dependencies { api(commonLibs.runtime) }` added (`api`, not `implementation`, since
`ShowcaseEntry`'s `content` field type is part of this module's public surface).

## Processor: explicit per-module id, generates a class + a ServiceLoader registration

KSP has no built-in notion of "which Gradle module is this." Each module applying the processor
must pass an explicit, unique id:

```kotlin
ksp {
    arg("showcaseModuleId", "kmpuiviews")
}
```

`ShowcaseSymbolProcessorProvider.create()` reads `environment.options["showcaseModuleId"]`. If
missing or blank, it fails loudly via `environment.logger.error(...)` with an actionable message
(consistent with this processor's existing error-message style) rather than silently defaulting
to something that could collide again.

For a given module id (e.g. `kmpuiviews`), the processor generates ONE class implementing
`ShowcaseRegistryProvider`, named by sanitizing the id into a valid Kotlin identifier and
appending `ShowcaseRegistryProvider` (e.g. `KmpuiviewsShowcaseRegistryProvider`), still in package
`com.programmersbox.showcase.generated` — uniqueness now comes from the class name, not a
per-module package:

```kotlin
package com.programmersbox.showcase.generated

import com.programmersbox.showcase.annotations.ShowcaseEntry
import com.programmersbox.showcase.annotations.ShowcaseRegistryProvider

class KmpuiviewsShowcaseRegistryProvider : ShowcaseRegistryProvider {
    override val entries: List<ShowcaseEntry> = listOf(
        ShowcaseEntry(name = "...", description = "...", group = "...", content = { ... }),
    )
}
```

Alongside the `.kt` source file, the processor also writes a `META-INF/services` resource file —
the same discovery mechanism KSP itself uses — via `CodeGenerator.createNewFileByPath`, at path
`META-INF/services/com.programmersbox.showcase.annotations.ShowcaseRegistryProvider`, containing
the generated class's fully-qualified name. The KSP Gradle plugin automatically adds
`build/generated/ksp/<sourceSet>/resources` to that source set's resources, so this file ends up
packaged into the module's jar without any extra Gradle wiring.

## App: no more special-casing

`:showcase` stops treating itself as special. It gets its own
`ksp { arg("showcaseModuleId", "showcase") }`, and its 3 built-in sample composables become a
provider discovered exactly like any other module's. `App.kt` replaces its direct
`ShowcaseRegistry.entries` reference with:

```kotlin
private val allEntries: List<ShowcaseEntry> by lazy {
    ServiceLoader.load(ShowcaseRegistryProvider::class.java).flatMap { it.entries }
}
```

`kmpuiviews/build.gradle.kts` (already wired with `add("kspJvm", projects.showcase.processor)` and
the `implementation(projects.showcase.annotations)` dependency, both added outside this plan) gets
the missing `ksp { arg("showcaseModuleId", "kmpuiviews") }` block added.

## Sorting

Each module's provider still sorts its own entries by group-then-name internally (unchanged
processor logic). Cross-module sort ordering (i.e. the final merged list `App.kt` displays) is out
of scope for this change — `App.kt`'s `flatMap` simply concatenates providers in whatever order
`ServiceLoader` returns them, which is unspecified. If a fully sorted merged view across modules
matters later, that's a follow-up `allEntries.sortedWith(...)` one-liner in `App.kt`, not a
processor change.

## Testing — explicitly out of scope for this change

`showcase/processor/src/test/kotlin/.../ShowcaseSymbolProcessorTest.kt` was written against the
old single-registry-object output shape and never sets `showcaseModuleId`. This change will leave
that test file outdated/failing. This is a deliberate, explicit decision (confirmed during
brainstorming) — the test suite will be addressed in a separate follow-up, not as part of this
change. No test-writing tasks are included in the implementation plan for this design.

## Documentation

`showcase/README.md`'s existing "Current limitation" section (added when this exact limitation was
first discovered and documented, before this fix) is replaced with real setup instructions for
wiring a new module into the showcase: the `annotations` + `ksp(processor)` dependencies, the
`showcaseModuleId` KSP arg, and a note that the new module must end up as a dependency of
`:showcase` (directly or transitively) for its components to actually appear at runtime.

## Out of scope

- Cross-module sort ordering (see Sorting above).
- Rewriting or fixing the existing processor unit test suite (explicitly deferred).
- Any change to the original per-function validation rules (top-level, non-private, `@Composable`,
  zero-parameters) — unchanged.
- Automatic derivation of `showcaseModuleId` (e.g. from the Gradle project path) — KSP has no
  reliable way to learn this on its own; requiring an explicit, unique string is the deliberate
  choice here.
