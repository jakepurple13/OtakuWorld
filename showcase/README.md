# Component Showcase

A standalone Compose Desktop app for browsing `@Composable` UI components in isolation. Annotate
any zero-parameter composable in the `:showcase` module with `@ShowcaseComponent`, rebuild, and it
appears in the showcase app automatically (see "Current limitation" below).

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

## Current limitation

Only composables inside the `:showcase` module itself are scanned — the processor runs wherever
KSP is applied, and today that's only `:showcase`. Samples must live in the `:showcase` module
(e.g. under `showcase/src/main/kotlin/com/programmersbox/showcase/samples/`). Annotating a
`@Composable` in another module (MangaWorld, `kmpuiviews`, etc.) has no effect — it will not appear
in the showcase app.

Cross-module aggregation is not supported yet: the generated package
(`com.programmersbox.showcase.generated`) is fixed, so if another module applied the processor it
would generate its own `ShowcaseRegistry` under the same package, colliding with this one.

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
