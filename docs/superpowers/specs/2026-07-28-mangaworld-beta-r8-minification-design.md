# MangaWorld Beta R8 Minification — Design

## Goal

Enable R8 code shrinking, resource shrinking, and obfuscation for the MangaWorld Android app's
existing **Beta** build type only. Decrease APK size and improve runtime performance while
preserving all existing functionality (including the external source/extension plugin loading
mechanism). No other app (AnimeWorld, NovelWorld), the Desktop/JVM target, or the Debug/Release
build types are affected.

## Current State

- `buildSrc/src/main/kotlin/plugins/ApplicationBuildTypes.kt` defines the `Beta` build type used by
  **all** app modules (mangaworld, animeworld, novelworld, and others) via the `otaku-application`
  convention plugin. It does `initWith(Debug)`, then hardcodes `isDebuggable = false`,
  `isShrinkResources = false`, `isMinifyEnabled = false`.
- `mangaworld/proguard-rules.pro` exists but only contains the default commented-out AGP template —
  no real keep rules.
- `mangaworld/build.gradle.kts` has no `buildTypes` override — it inherits Beta as-is from the
  convention plugin.
- `gradle.properties` already sets `android.r8.strictFullModeForKeepRules=false` and
  `android.r8.optimizedResourceShrinking=false`. No flag disables R8 full mode, so AGP's default
  (full mode) applies once minification is turned on.

## Architecture Decision

**Do not touch `buildSrc/ApplicationBuildTypes.kt`.** That file is shared by every app; flipping
`isMinifyEnabled`/`isShrinkResources` there would enable minification everywhere, violating the
MangaWorld-only scope.

Instead, add a second `android { buildTypes { getByName("beta") { ... } } }` block in
`mangaworld/build.gradle.kts`. Gradle merges this onto the build type the convention plugin already
configured (`initWith(debug)`, `isDebuggable = false`). This block sets:

```kotlin
isMinifyEnabled = true
isShrinkResources = true
proguardFiles(
    getDefaultProguardFile("proguard-android-optimize.txt"),
    "proguard-rules.pro"
)
```

This is additive-only from mangaworld's perspective and invisible to animeworld/novelworld/other
app modules, which keep getting the convention plugin's `isMinifyEnabled = false` Beta.

## ProGuard Rules

Replace `mangaworld/proguard-rules.pro`'s template content with a single comprehensive rules file
(no `consumer-rules.pro` added to shared modules — keeping all rules local to the app module per
scope). Categories, tailored to what `mangaworld/build.gradle.kts` actually depends on:

1. **Kotlin** — `kotlin.Metadata`, `$WhenMappings`, `dontwarn kotlin.**`
2. **Kotlin Coroutines** — internals (`MainDispatcherFactory`, `CoroutineExceptionHandler`, volatile
   fields), `dontwarn kotlinx.coroutines.**`
3. **Kotlinx Serialization** — keep `@Serializable` classes, generated `$$serializer` companions,
   `KSerializer serializer(...)` factory functions, for `com.programmersbox.**`
4. **Compose / Compose Multiplatform** — keep `androidx.compose.**`, `@Composable`-annotated
   members, `@Preview`-annotated members
5. **Koin** — keep `org.koin.**`, Koin-annotated classes
6. **Room 3** (`androidx.room3` artifact, package `androidx.room`) — keep `@Entity`, `@Database`,
   `@Dao` annotated classes and `RoomDatabase` subclasses
7. **Extension/source-loading contract** — keep `kmpmodels` interfaces/models
   (`KmpApiService`, `KmpItemModel`, `KmpInfoModel`, `KmpChapterModel`, `KmpStorage`) and any
   `com.programmersbox.**` types implementing them, so externally-loaded Mihon-style source plugins
   keep resolving these contracts at runtime
8. **Ktor** — keep `io.ktor.**`
9. **OkHttp** — keep `okhttp3.**`, `okio.**`
10. **Firebase / Crashlytics** — keep `com.google.firebase.**`, `com.google.android.gms.**`
11. **Glide** — standard Glide keep rules (GlideModule implementations, generated API,
    `ImageHeaderParser` enum)
12. **MangaWorld-specific libs actually in its deps**: Duktape/Zipline (JS interpreter for source
    plugins), Piasy BigImageViewer, SubsamplingScaleImageView, Iconics, pagecurl, panpf zoomimage,
    telephoto, AboutLibraries, jakepurple13 HelpfulTools, Supabase
13. **Stack traces** — `keepattributes SourceFile,LineNumberTable`, `renamesourcefileattribute
    SourceFile`

R8 full mode is AGP's default and is not explicitly disabled anywhere in the project, so no extra
Gradle flag is needed to get full-mode optimizations.

## Verification

- `./gradlew :mangaworld:assembleNoFirebaseBeta` must succeed.
- Inspect `mangaworld/build/outputs/mapping/noFirebaseBeta/missing_rules.txt` (if AGP emits one) —
  absent or empty is the expected signal that no reflectively-accessed class was stripped without a
  keep rule.
- No automated device/emulator smoke test in this environment. The user will install and manually
  test the resulting Beta APK on their own device after implementation; if something breaks, report
  back for a targeted keep-rule fix.

## Out of Scope

- AnimeWorld, NovelWorld, `app`, `animeworldtv`, or any other app module
- Desktop/JVM (`mangaworld:desktop`) target
- Debug or Release build types, or any existing CI workflow steps
- New dependencies, code refactors, or unit tests
