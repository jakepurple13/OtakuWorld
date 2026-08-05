# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## What this project is

OtakuWorld is a multi-app Android project with three main consumer apps: **MangaWorld** (manga
reader), **AnimeWorld** (anime streamer), and **NovelWorld** (novel reader). They share a large
common codebase and are actively being migrated to Kotlin Multiplatform (KMP) to eventually support
JVM/Desktop (via Compose Multiplatform). A Desktop build of MangaWorld (`mangaworld:desktop`)
already exists.

Apps contain no bundled sources — sources are loaded as external plugins/APKs at runtime via the
extension loader system.

## Build commands

```bash
# Build a specific app (use noFirebase flavor for local builds — no google-services.json needed)
./gradlew :mangaworld:assembleNoFirebaseDebug
./gradlew :animeworld:assembleNoFirebaseDebug
./gradlew :novelworld:assembleNoFirebaseDebug

# Build the desktop app
./gradlew :mangaworld:desktop:run

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :UIViews:test
./gradlew :kmpuiviews:test

# Run a single test class
./gradlew :UIViews:test --tests "com.programmersbox.uiviews.ExampleUnitTest"

# Clean build
./gradlew clean
```

**Important:** Always use the `noFirebase` build variant for local development. This is set as the
default (`isDefault = true`). The `full` flavor requires `google-services.json` secrets.

## Build flavors and types

Three product flavors (dimension `version`):

- `noFirebase` — default for local dev, no Firebase dependency, appId suffix `.noFirebase`
- `noCloudFirebase` — Firebase crashlytics only, no cloud sync
- `full` — complete Firebase integration

Build types: `debug`, `release`, `beta` (beta = non-debuggable debug).

Flavor-specific Firebase utility implementations live in
`sharedutils/src/{noFirebase,noCloudFirebase,full}/java/`.

## Module structure

| Module                    | Purpose                                                                                                                         |
|---------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `kmpmodels`               | KMP data models (`KmpItemModel`, `KmpInfoModel`, `KmpChapterModel`, `KmpStorage`, `KmpApiService`) — the source plugin contract |
| `kmpuiviews`              | KMP shared UI, ViewModels, repositories, DI modules, navigation (targets: Android, JVM, iOS)                                    |
| `UIViews`                 | Android-only shared UI layer extending `kmpuiviews`; `BaseMainActivity`, `GenericInfo` interface                                |
| `favoritesdatabase`       | KMP Room database for favorites, history, custom lists, recommendations                                                         |
| `datastore`               | KMP DataStore/protobuf settings handling                                                                                        |
| `datastore:mangasettings` | Manga-specific protobuf settings                                                                                                |
| `sharedutils`             | Firebase utilities with flavor-specific implementations                                                                         |
| `source_utilities`        | `NetworkHelper` for HTTP source plugins                                                                                         |
| `mangaworld`              | MangaWorld Android app, `GenericManga` implementation                                                                           |
| `mangaworld:shared`       | Shared manga reader UI (KMP)                                                                                                    |
| `mangaworld:desktop`      | JVM/Desktop Compose app for MangaWorld                                                                                          |
| `animeworld`              | AnimeWorld Android app                                                                                                          |
| `novelworld`              | NovelWorld Android app                                                                                                          |
| `novelworld:shared`       | Shared novel reader UI (KMP)                                                                                                    |
| `app`                     | OtakuWorld companion/manager app                                                                                                |
| `showcase`                | Standalone Compose Desktop app for browsing @ShowcaseComponent-annotated composables (dev tool, not part of app runtime)        |
| `showcase:annotations`    | `@ShowcaseComponent` annotation (KMP)                                                                                           |
| `showcase:processor`      | KSP processor that discovers annotated composables and generates the showcase registry                                         |

## Source plugin architecture

**This is the core extensibility pattern.** Sources are not bundled — they are loaded as external
plugins at runtime.

- `KmpApiService` (`kmpmodels`) — interface all sources implement. Key methods: `recent()`,
  `allList()`, `itemInfo()`, `chapterInfo()`, `search()`
- `KmpItemModel` / `KmpInfoModel` / `KmpChapterModel` — data model hierarchy flowing from source to
  UI
- `OtakuWorldCatalog` — fetches the remote extension index from `OtakuWorldSources` repo and
  provides `KmpRemoteSources` for in-app installation
- `KmpExternalApiServicesCatalog` / `KmpSources` — catalog abstraction for extension marketplace

Sources compatible with Mihon (Tachiyomi forks) work with MangaWorld after the bridge is installed.
Aniyomi-compatible sources work with AnimeWorld similarly.

## GenericInfo pattern

Each app supplies a `GenericInfo` implementation that customizes app-specific behavior injected via
Koin:

1. `KmpGenericInfo` (interface, `kmpuiviews/commonMain`) — KMP contract: `chapterOnClick`,
   `downloadChapter`, list composables, nav setup hooks
2. `PlatformGenericInfo` (expect/actual per platform) — platform-specific extension point
3. `GenericInfo` (`UIViews`, Android) — extends `PlatformGenericInfo`, provides account UI defaults
4. App-specific class e.g. `GenericManga` (`mangaworld`) — final implementation registered in Koin:
   `singleOf(::GenericManga) { bindsGenericInfo() }`

## Navigation

Uses **Navigation3** (AndroidX). Navigation graph is built in `entryGraph()` (
`kmpuiviews/commonMain/.../navigation/Nav3Graph.kt`). Screens are `NavKey` data objects/classes
defined in `Screen.kt`.

`KmpGenericInfo` has `globalNav3Setup()` and `settingsNav3Setup()` context functions that let each
app inject additional nav entries into the shared graph.

`NavigationActions` is a Koin singleton that abstracts navigation calls — use it instead of directly
accessing the nav controller.

## Dependency injection

Koin is used throughout. Module registration follows this pattern:

- `kmpuiviews` provides base KMP modules (`AppModule.kt`, `NavigationModule.kt`, platform
  `RepositoryModule`, `ViewModelModule`)
- Each app module adds its own `appModule` (e.g., `mangaworld/GenericManga.kt`)
- `UIViews` provides Android-specific additions (`di/AppModule.kt`, `di/ViewModelModule.kt`)

## KMP source set layout

`kmpuiviews` and `kmpmodels` use this hierarchy:

- `commonMain` — shared logic and interfaces
- `androidMain` — Android implementations
- `jvmMain` — Desktop/JVM implementations
- `iosMain` — iOS stubs
- `deviceMain` / `httpMain` — intermediate groupings defined in `applyDefaultHierarchyTemplate`

## Convention plugins (buildSrc)

Custom Gradle plugins in `buildSrc/src/main/kotlin/plugins/`:

- `otaku-application` → `AndroidApplicationPlugin` — Android app with Firebase, Compose, product
  flavors
- `otaku-library` → `AndroidLibraryPlugin` — Android library
- `otaku-multiplatform` → `MultiplatformLibraryPlugin` — KMP library

Apply with backtick syntax in `build.gradle.kts`: `` `otaku-application` ``

`AppInfo.kt` in `buildSrc` holds `compileSdk`, `minSdk`, `targetSdk`, and version name constants.

## Key external dependencies

- **Compose Multiplatform** — UI across Android and Desktop
- **Koin** — DI (with `koin-compose`, `koin-androidx-compose`)
- **Kamel** — KMP image loading
- **Ktor** — KMP HTTP client for source catalog
- **Room (KMP)** — favorites database
- **Haze** — blur/glassmorphism effects
- **kotlinx.serialization** — JSON throughout (replacing Gson in progress)
- **Hotswan** — Compose hot-reload support (Desktop dev)

## Version catalog

`gradle/libs.versions.toml` is the primary version catalog. An additional `androidx` catalog is
imported from `androidx.gradle:gradle-version-catalog`. Reference libs as `libs.*` or `androidx.*`
in build files.

## Active migration notes

The project is mid-migration to KMP. Key in-progress changes (see `Multiplatform Roadmap.md`):

- Gson → kotlinx.serialization removal in progress
- ViewModels being moved from Android modules into `kmpuiviews`
- `UIViews` is the Android-specific layer that will shrink over time as more moves to `kmpuiviews`

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions
as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:

- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:

- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:

- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:

```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant
clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to
overcomplication, and clarifying questions come before implementation rather than after mistakes.