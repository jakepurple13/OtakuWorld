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
