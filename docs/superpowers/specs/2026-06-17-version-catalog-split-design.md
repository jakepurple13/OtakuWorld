# Version Catalog Split Design

**Date:** 2026-06-17
**Branch:** tech-debt/better-version-cataloging

## Goal

Split the monolithic `gradle/libs.versions.toml` into 5 platform-scoped catalogs so dependencies are
clearly owned by the platform that uses them.

## Catalog Structure

### `gradle/libs.versions.toml` → accessor `libs` (auto-discovered, no settings change)

Scope: build tooling, Gradle plugins, and their version refs only. No runtime library declarations.

**Versions kept:** `kotlin`, `ksp`, `gradle`, `compose-multiplatform` (plugin version), `room` (plugin
version), `protobufGradlePlugin`, `easylauncher`, `buildKonfig`, `firebaseCrashlyticsGradle`,
`googleServices`, `googlePerformancePlugin`, `googleAndroidLibrariesMapsplatformSecretsGradlePlugin`,
`kotzilla`, `androidxBaselineprofileGradlePlugin`, `kspVersion`, `koog`/`koog-beta` (used in plugin
config).

**Libraries kept:** build classpath only — `kotlinGp`, `kotlinStLib`, `gradle`,
`firebase-crashlytics-gradle`, `google-services`, `protobuf-gradle-plugin`,
`androidx-baselineprofile-gradle-plugin`, `easylauncher`.

**Plugins section:** all plugin aliases stay here.

---

### `gradle/common.versions.toml` → accessor `commonLibs`

Scope: KMP runtime dependencies used across Android, Desktop, and iOS targets.

**Key groups:**
- Coroutines: `coroutinesCore`, `coroutinesTest`, `kotlinx-coroutines`
- Koin: `koin-bom`, `koin-core`, `koinCores`, `koinComposeKmp`, `koinViewModel`, `koinViewModelNavigation`
- Ktor: all `ktor*` libs (`ktorCore`, `ktorAuth`, `ktorLogging`, `ktorSerialization`, `ktorJson`,
  `ktorContentNegotiation`, `ktorOkHttp`, `ktorGson`, `ktorMock`, `ktorJsoup`)
- Room KMP: `roomCompiler`, `roomRuntime`, `roomPaging` (room version ref)
- Compose Multiplatform: `compose-material3`, `foundation`, `runtime`, `ui`, `cmp-ui-util`,
  `ui-backhandler`, `material-icons-extended`, `material3-adaptive-navigation-suite`,
  `components-resources`, `materialAdaptiveCmp`, `materialAdaptiveLayoutCmp`,
  `materialAdaptiveLayoutNavCmp`, `material3-window-size`
- Navigation (KMP): `cmp-navigation3-ui`, `cmp-lifecycle-viewmodel-navigation3`,
  `cmp-material3-adaptive-nav3`, `cmp-navigationevent-compose`, `androidx-navigation3-runtime`,
  `androidx-navigation3-ui`, `androidx-material3-navigation3`, `androidx-navigationevent`,
  `androidx-navigationevent-compose`, `lifecycleViewmodelNavigation3`, `navigationCmp`,
  `cmp-nav3`, `cmp-navevent`, `cmp-nav3-material3`, `navigation3`, `nav3Material`
- Lifecycle (KMP): `lifecycle-viewmodel-compose`, `multiplatform-lifecycle-runtime-compose`,
  `cmpLifecycle`, `androidx-lifecycle`
- Image loading KMP: all `kamel-*` libs
- UI polish: `haze`, `haze-blur`, `haze-materials`, `backdrop`, `material-kolor`, `kmpalette-core`,
  `compottie`, `reorderable`, `qrose`, `human-readable`, `heatmap`, `markdown-renderer`
- Data/storage: all `datastore*` libs, `kotlinxSerialization`, `kotlinx-datetime`,
  `androidx-room-sqlite`, `sqlite`
- Connectivity: all `connectivity-*` libs
- AI/LLM: `koog-agents`, `koog-agents-additions`, `koog-memory`, `anthropic-sdk-kotlin`,
  `generic-ai`, `generativeai-google`, `xemantic-ai-tool-schema`
- KMP utilities: `moko-biometry`, `moko-biometry-compose`, `filekit-core`, `filekit-dialogs-compose`,
  `kfswatch`, `urlencoder-lib`, `paging-compose-common`, `kotlin-test`,
  `compose-webview-multiplatform`, `compose-constraintlayout-compose-multiplatform`,
  `constraintlayout-compose-multiplatform`, `kotzilla-sdk-compose`, `blurhash`
- Bundles: `koinKmp`, `datastoreLibs`, `kamel`, `roomLibs`, `ktorLibs`

---

### `gradle/android.versions.toml` → accessor `androidLibs`

Scope: Android-only dependencies.

**Key groups:**
- Android Compose: `composePlatform` (BOM), `composeUi`, `composeUiTooling`, `composeFoundation`,
  `composeMaterialIconsCore`, `composeMaterialIconsExtended`, `composeViewBinding`,
  `composeAnimation`, `composeRuntimeLivedata`, `uiUtil`, `composeConstraintLayout`,
  `materialYou`, `materialYou-common`, `materialWindow`, `materialAdaptive`,
  `adaptive-layout-android`, `adaptive-navigation-android`, `material-adaptive-navigation-suite`,
  `composeActivity`, `composeLifecycle`, `composeLifecycleRuntime`
- Firebase: `firebasePlatform`, `firebaseDatabase`, `firebaseFirestore`, `firebaseAuth`,
  `crashlytics`, `analytics`, `firebase-perf`, `firebaseUiAuth`
- Media3/ExoPlayer: all `exoplayer*` libs, `media3Version`
- Image loading Android: `coilCompose`, `coilOkHttp`, `coilGif`, `coilVideo`,
  `glide`, `glideCompiler`, `glideRecyclerview`, `glideCompose`,
  `landscapist-bom`, `landscapistGlide`, `landscapistPalette`, `landscapistPlaceholder`,
  all `sketch-*` libs
- Lifecycle Android: `lifecycleRuntime`, `lifecycleLivedata`, `lifecycleViewModel`,
  `lifecycleExtensions`, `lifecycle` (version ref)
- Paging Android: `pagingRuntime`, `pagingCompose`, `pagingVersion`
- WorkManager: `workRuntime`, `koin-workmanager`, `workVersion`
- Koin Android: `koin-android`, `koinCompose`
- OkHttp: `okhttpLib`, `okhttpDns`, `okhttpVersion`
- Protobuf runtime: `protobufJava`, `protobufKotlin`, `protobufVersion`
- Biometric/Security: `biometric`
- APK install: `ackpine-ktx`, `ackpine-core`
- ML Kit: `mlkitTranslate`, `mlkitLanguage`, `barcode-scanning`
- XR: `androidx-arcore`, `androidx-compose-xr`, `androidx-scenecore`, `androidx-xr-material3`
- TV/Leanback: `tv-material`, `tv-foundation`, `leanback`, `leanbackPreference`
- Lottie: `lottie-compose`
- Accompanist: `drawablePainter`, `adaptive`
- Hotswan: `hotswan-compiler` plugin + version
- Android foundation: `androidCore`, `appCompat`, `material`, `coreLibraryDesugaring`,
  `fragmentKtx`, `androidxLegacySupport`, `preference`, `recyclerview`, `constraintlayout`,
  `swiperefresh`, `androidxWindow`, `androidBrowserHelper`, `androidxBrowser`, `androidxWebkit`
- Misc UI: `pagecurl`, `textflow-material3`, `sonner`, `composeCollapsable`, `dragselect`,
  `toolbarCompose`, `lazyColumnScrollbar`, `showMoreLess`, `fileChooser`, `iconicsCore`,
  `zoomableModifier`, `telephoto-zoomable-image-glide`, `zoomable-peek-overlay`,
  `panpf-zoomimage-compose-glide`, `telephoto`, `zoomimageComposeGlide`
- MangaWorld Android: `bigImageViewer`, `bigImageGlideLoader`, `progressPieIndicator`,
  `subsamplingImageView`, `piasy` (version), `coilGif`
- AnimeWorld Android: `slideToAct`, `mediaRouter`, `torrentStream`, `castFramework`,
  `localCast`, `superForwardView`, `autoBindings`, `autoBindingsCompiler`, `autoBinding`
- Source plugin utilities: `duktape`, `rhino`, `kotson`, `karnKhttp`, `ziplineLoader`,
  `ziplineProfiler`, `ziplineVersion`, `jsoup`, `gson`, `palette`, `reactiveNetwork`,
  `retrofit`, `retrofitGson`
- About/misc: `aboutLibrariesCore`, `aboutLibrariesCompose`, `workinspector`,
  `androidBrowserHelper`, `playServices`
- Coroutines Android: `coroutinesAndroid`, `coroutinesPlayServices`
- Tests: `junit`, `espresso-core`, `uiautomator`, `benchmark-macro-junit4`, `profileinstaller`,
  `androidx-runner`, `androidx-core`
- Bundles: `compose`, `composeTv`, `media3`, `leanbackLibs`, `okHttpLibs`, `firebaseCrashLibs`,
  `koinLibs`, `piasyLibs`, `ziplineLibs`, `pagingLibs`, `protobuf`, `sketch`, `xr`

---

### `gradle/desktop.versions.toml` → accessor `desktopLibs`

Scope: JVM/Desktop-only dependencies.

**Libraries:**
- `kotlinx-coroutines-swing` (Swing dispatcher for Compose Desktop)
- `javase` + `core` (ZXing — QR on Desktop)
- `kotlin-multiplatform-appdirs` (OS-specific app directories)
- `knotify` (desktop notifications)
- All nucleus system libs: `github-nucleus-scheduler`, `github-nucleus-scheduler-testing`,
  `github-nucleus-taskbar-progress`, `github-nucleus-notifications-common`,
  `nucleus-system-color`, `nucleus-system-info`

**Bundles:** none initially.

---

### `gradle/ios.versions.toml` → accessor `iosLibs`

Scope: iOS-specific. Empty placeholder — skeleton only, ready for future iOS deps.

```toml
[versions]
# iOS-specific versions go here

[libraries]
# iOS-specific libraries go here

[bundles]
# iOS-specific bundles go here

[plugins]
# iOS-specific plugins go here
```

---

## `settings.gradle.kts` Changes

Add to the existing `versionCatalogs` block:

```kotlin
versionCatalogs {
    create("androidx") {
        from("androidx.gradle:gradle-version-catalog:2026.05.00")
    }
    create("commonLibs") {
        from(files("gradle/common.versions.toml"))
    }
    create("androidLibs") {
        from(files("gradle/android.versions.toml"))
    }
    create("desktopLibs") {
        from(files("gradle/desktop.versions.toml"))
    }
    create("iosLibs") {
        from(files("gradle/ios.versions.toml"))
    }
}
```

---

## Build File Update Strategy

### Scope
~30 `build.gradle.kts` files. All `libs.*` dependency references get replaced with the appropriate
catalog accessor. `libs.*` plugin aliases are never moved — they stay in `libs` always.

### Module classification

| Module(s) | Catalogs used |
|---|---|
| `buildSrc` | `libs` only (plugin versions) |
| `mangaworld`, `animeworld`, `novelworld`, `app`, `animeworldtv` | `libs` (plugins) + `androidLibs` + `commonLibs` |
| `UIViews`, `sharedutils`, `source_utilities`, `imageloader` | `libs` + `androidLibs` + `commonLibs` |
| `kmpuiviews`, `kmpmodels`, `favoritesdatabase`, `datastore`, `datastore:mangasettings` | `libs` + `commonLibs` |
| `kmpuiviews:koogintegration` submodules | `libs` + `commonLibs` |
| `mangaworld:shared`, `novelworld:shared` | `libs` + `commonLibs` + `androidLibs` (Android source sets) |
| `mangaworld:desktop` | `libs` + `commonLibs` + `desktopLibs` |
| `Models` | `libs` + `commonLibs` |
| `MangaWorldbaselineprofile` | `libs` + `androidLibs` |
| `manga_sources`, `anime_sources`, `novel_sources` and sub-modules | `libs` + `androidLibs` |

### Version duplication rule

Gradle does not support cross-catalog `versionRef`. If the same version number is needed in multiple
catalogs (e.g., `kotlin` version used in both `libs` and `commonLibs`), each catalog declares its
own `[versions]` entry with the same value. No single source of truth for version numbers across
catalogs — update all affected catalogs when bumping a version.

### Migration steps per module
1. Grep all `libs.*` references in the module's `build.gradle.kts`
2. For each reference, determine target catalog per the tables above
3. Replace `libs.foo` → `commonLibs.foo` / `androidLibs.foo` / `desktopLibs.foo` as appropriate
4. Keep `alias(libs.plugins.*)` references unchanged
5. Verify with `./gradlew :moduleName:assembleNoFirebaseDebug`

### Verification
After all modules are updated:
```bash
./gradlew assembleNoFirebaseDebug        # Android apps
./gradlew :mangaworld:desktop:run        # Desktop
./gradlew test                           # All tests
```
