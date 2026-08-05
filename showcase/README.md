# Component Showcase

A standalone Compose Desktop app for browsing `@Composable` UI components in isolation. Annotate
any zero-parameter composable with `@ShowcaseComponent` in any module wired up per "Adding a new
module" below, rebuild, and it appears in the showcase app automatically.

This is a developer tool only — it is not part of MangaWorld/AnimeWorld/NovelWorld's runtime.

## Modules

- **`:showcase:annotations`** — a Kotlin Multiplatform module (currently only a `jvm()` target is
  declared; an Android/iOS consumer would need a target added first) holding the
  `@ShowcaseComponent` annotation (`name`, `description`, `group`; source retention; function
  target).
- **`:showcase:processor`** — a KSP `SymbolProcessor` that finds every `@ShowcaseComponent`
  function, validates it, and generates `com.programmersbox.showcase.generated.ShowcaseRegistry`
  — a `List<ShowcaseEntry>` sorted alphabetically by group, then by name.
- **`:showcase`** — the Compose Desktop app itself. Renders the generated registry behind a
  Material 3 `NavigationRail` (one rail item per group, plus "All"), with live-rendered previews
  of each component.

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
