# JVM App Logo (per-app icon as Compose resource) — Design

## Goal

Give each desktop app (MangaWorld, AnimeWorld, NovelWorld) its real icon as an injectable
Compose resource, replacing the current hardcoded placeholder vector icon used everywhere
`painterLogo()` is called on JVM (Tray icon, About screen, QR code, etc.).

## Current state (as of investigation)

- `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/Platform.kt:68` declares
  `expect fun painterLogo(): Painter`, used at ~20 call sites in `kmpuiviews/commonMain`
  (About screen, Settings, QR code, notifications, onboarding, global search) plus the system
  Tray icon at `kmpuiviews/src/jvmMain/.../DesktopUi.kt:164`.
- Android already has the real pattern: `class AppLogo(val logo: Drawable, val logoId: Int)`
  (`kmpuiviews/src/androidMain/.../utils/DIClasses.kt`), registered as a Koin single built from
  `Application.applicationInfo.loadIcon(...)` (`UIViews/.../di/AppModule.kt`), consumed via
  `koinInject<AppLogo>().logo` in the Android actual
  (`kmpuiviews/src/androidMain/.../Platform.android.kt:190`).
- JVM actual (`kmpuiviews/src/jvmMain/.../Platform.jvm.kt:168`) is currently a hardcoded,
  identical-across-all-3-apps placeholder: `rememberVectorPainter(Icons.Default.RememberMe)`.
- Each app already has a packaging-only icon at `<app>/desktop/icons/icon.png` (512×512,
  added for Linux `.deb` packaging in a prior task) — reusable as the source for this feature,
  no new image conversion needed.
- Compose resources (`DrawableResource`, `painterResource`) are already available in
  `kmpuiviews` via `commonLibs.components.resources` in `commonMain.dependencies`
  (`kmpuiviews/build.gradle.kts`) — no new Gradle dependency needed there.
- The only existing precedent for a module-local `composeResources/drawable/` folder +
  `compose.resources {}` Gradle block in this repo is
  `favoritesdatabase/supabase-integration` (`build.gradle.kts:12-17`), which sets
  `publicResClass = true` because its `Res` is consumed from *other* modules. That does not
  apply here — each desktop module will construct its own `Res.drawable.app_icon` and pass it
  into Koin from within the same module, so `publicResClass` is unnecessary.
- None of `mangaworld/desktop`, `animeworld/desktop`, `novelworld/desktop` currently have a
  `composeResources` source folder.

## Design

### 1. `JvmAppLogo` class (new)

New file `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/utils/DIClasses.kt`:

```kotlin
package com.programmersbox.kmpuiviews.utils

import org.jetbrains.compose.resources.DrawableResource

class JvmAppLogo(val logo: DrawableResource)
```

Mirrors the Android `AppLogo` shape exactly, minus the Android-only `logoId: Int` (no JVM
equivalent of an Android resource ID exists or is needed).

### 2. Icon as a Compose resource, per app

- Copy `<app>/desktop/icons/icon.png` → new file
  `<app>/desktop/src/commonMain/composeResources/drawable/app_icon.png` (identical bytes, no
  re-conversion — same 512×512 source already used for Linux packaging).
- Add to each `<app>/desktop/build.gradle.kts` (top level, alongside the `plugins {}` block):

```kotlin
compose.resources {
    packageOfResClass = "com.programmersbox.desktop"
}
```

This generates `Res` and `app_icon` directly in package `com.programmersbox.desktop` — the
same package `main.kt` already lives in for each app, so no import is needed to use
`Res.drawable.app_icon` from `main.kt`. (All 3 apps use this identical package name for their
desktop module already — see `mangaworld/desktop`, `animeworld/desktop`,
`novelworld/desktop`'s existing `main.kt` package declarations. This is safe: each is a
separate compiled Gradle module/app, so there's no runtime collision, exactly like the
existing `GenericMangaDesktop`/`GenericAnimeDesktop`/`GenericNovelDesktop` classes already
sharing that package name across the 3 separate apps.)

### 3. Register in Koin, per app's `main.kt`

Add one `single` next to the existing `AppConfig` single (same `module { }` block):

```kotlin
single { JvmAppLogo(Res.drawable.app_icon) }
```

Requires one new import per file: `com.programmersbox.kmpuiviews.utils.JvmAppLogo`.

### 4. Use it in `painterLogo()`

`kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/Platform.jvm.kt:168`, change:

```kotlin
@Composable
actual fun painterLogo(): Painter = rememberVectorPainter(Icons.Default.RememberMe)
```

to:

```kotlin
@Composable
actual fun painterLogo(): Painter = painterResource(koinInject<JvmAppLogo>().logo)
```

New imports needed: `org.jetbrains.compose.resources.painterResource`,
`org.koin.compose.koinInject` (already used elsewhere in this module, e.g. `DesktopUi.kt`),
`com.programmersbox.kmpuiviews.utils.JvmAppLogo`. Remove the now-unused
`androidx.compose.material.icons.filled.RememberMe` and
`androidx.compose.ui.graphics.vector.rememberVectorPainter` imports (orphaned by this change).

## Out of scope

- Any Android-side change (`AppLogo` already works correctly there).
- Any change to the packaging icons themselves (`desktop/icons/icon.*`) — this reuses the
  existing `icon.png`, byte-for-byte.
- A separate lower-resolution variant of the logo for small UI elements (Tray icon, etc.) —
  the existing single 512×512 PNG is reused as-is, matching how Android's `AppLogo` also uses
  the full-resolution app icon drawable at every call site regardless of render size.
- iOS — no `painterLogo()` actual exists for iOS today; not part of this change.
