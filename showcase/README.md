# Component Showcase

A standalone Compose Desktop app for browsing `@Composable` UI components in isolation. Annotate
any zero-parameter composable with `@ShowcaseComponent` in any module wired up per "Adding a new
module" below, rebuild, and it appears in the showcase app automatically.

The showcase app (`:showcase`) and processor (`:showcase:processor`) are dev-tools only, never part
of the shipping apps' runtime. `:showcase:annotations` (just the annotation plus two small shared
types) may end up on a production app's classpath if that app depends on a module that's been
wired into the showcase (e.g. `kmpuiviews`), but nothing in it is ever exercised outside a
`@ShowcaseComponent`-annotated module's own showcase-processor pass.

## Modules

- **`:showcase:annotations`** — a Kotlin Multiplatform module (targets: `jvm()`, `android { }`,
  `iosArm64()`, `iosSimulatorArm64()`) holding the `@ShowcaseComponent` annotation (`name`,
  `description`, `group`; source retention; function target), plus the shared `ShowcaseEntry` data
  class and `ShowcaseRegistryProvider` marker interface that every generated provider implements.
- **`:showcase:processor`** — a KSP `SymbolProcessor` that finds every `@ShowcaseComponent`
  function in a module, validates it, and generates a `ShowcaseRegistryProvider` implementation
  for that module (in package `com.programmersbox.showcase.generated`) — a `List<ShowcaseEntry>`
  sorted alphabetically by group, then by name, registered for runtime discovery via
  `java.util.ServiceLoader`.
- **`:showcase`** — the Compose Desktop app itself. Renders the merged entries from every
  discovered provider behind a Material 3 `NavigationRail` (one rail item per group, plus "All"),
  with live-rendered previews of each component.

## Known limitation

`showcaseModuleId` uniqueness is enforced only by convention, not detected. If two different
modules pick the same `showcaseModuleId`, they generate a class with the identical fully-qualified
name and identical `META-INF/services` entry — at runtime only one is discoverable, silently,
reproducing the exact bug this feature was built to fix, just one level up. Always pick a
`showcaseModuleId` that's unique across every module that applies the processor; a duplicate causes
one module's components to silently not appear, with no build error, since the collision only
manifests as runtime classloader/ServiceLoader shadowing, not a compile-time duplicate-class error.

## Adding a new module

Any module can contribute components to the showcase. To wire one up:

0. Apply the KSP Gradle plugin: `alias(libs.plugins.ksp)` in the module's `plugins { }` block (if
   not already applied) — without it, neither the `ksp(...)`/`kspJvm` configuration nor the
   `ksp { }` extension block used below exists.
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

Rebuild (the processor is rebuilt automatically as part of `:showcase:build` — only its `jar`
output is a dependency, not its full `build`/`check`) whenever you add or change a
`@ShowcaseComponent` annotation — there's no hot-reload.
