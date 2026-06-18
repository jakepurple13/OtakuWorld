# Version Catalog Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (
> recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the monolithic `gradle/libs.versions.toml` into 5 platform-scoped catalogs (`libs`,
`commonLibs`, `androidLibs`, `desktopLibs`, `iosLibs`) so dependencies are clearly owned by the
platform that uses them.

**Architecture:** `libs.versions.toml` retains only build plugin versions/classpath libs. KMP
runtime deps move to `common.versions.toml`. Android-only deps move to `android.versions.toml`.
Desktop-only deps move to `desktop.versions.toml`. An empty `ios.versions.toml` placeholder is
created. All ~30 `build.gradle.kts` files are updated to reference the appropriate catalog.

**Tech Stack:** Gradle version catalogs (TOML), Kotlin DSL, KMP

---

## Catalog accessor naming

| File                           | Gradle accessor          |
|--------------------------------|--------------------------|
| `gradle/libs.versions.toml`    | `libs` (auto-discovered) |
| `gradle/common.versions.toml`  | `commonLibs`             |
| `gradle/android.versions.toml` | `androidLibs`            |
| `gradle/desktop.versions.toml` | `desktopLibs`            |
| `gradle/ios.versions.toml`     | `iosLibs`                |

## Version duplication rule

Gradle does not support cross-catalog `versionRef`. If the same version number is needed in multiple
catalogs (e.g. `coroutinesVersion` in both `commonLibs` and `androidLibs`), each catalog declares
its own `[versions]` entry with the same value.

---

### Task 1: Create `gradle/common.versions.toml`

**Files:**

- Create: `gradle/common.versions.toml`

- [ ] **Step 1: Create the file**

```toml
[versions]
anthropicSdkKotlin = "0.32.5"
androidx-lifecycle = "2.10.0"
backdrop = "2.0.0"
blurhash = "0.4.0-SNAPSHOT"
cmpLifecycle = "2.11.0-beta02"
cmp-nav3 = "1.2.0-alpha01"
cmp-navevent = "1.1.0"
cmp-nav3-material3 = "1.3.0-beta02"
coil = "3.4.0"
compose-multiplatform = "1.12.0-alpha02"
composeWebviewMultiplatform = "2.0.3"
compottie = "2.2.2"
connectivity = "2.4.1"
coroutinesVersion = "1.11.0"
datastore = "1.3.0-alpha09"
dragselect = "3.3.0"
filekitCore = "0.14.1"
foundation = "1.11.1"
genericAi = "0.6.6"
generativeaiGoogle = "0.9.0-1.1.0"
haze = "2.0.0-alpha03"
heatmap = "1.0.5"
humanReadable = "1.12.3"
kamelImage = "1.0.9"
kfswatch = "1.4.0"
koin-bom = "4.2.2"
koog = "1.0.0"
koog-beta = "1.0.0-beta"
kotlinTest = "2.4.0"
kotlinx-coroutines = "1.11.0"
kotlinxDatetime = "0.8.0"
kotzilla = "2.2.2"
ktorVersion = "3.3.3"
latestAboutLibsRelease = "14.2.1"
lifecycleViewmodelNavigation3 = "2.10.0"
markdown-renderer = "0.41.0"
material3AdaptiveNavigationSuite = "1.10.0-alpha05"
material3WindowSizeClassVersion = "1.10.0-alpha05"
materialIconsExtended = "1.7.3"
materialKolor = "5.0.0-alpha07"
mokoBiometry = "0.4.0"
nav3Material = "1.3.0-beta02"
navigation3 = "1.2.0-alpha04"
navigationevent = "1.1.1"
pagingComposeCommon = "3.3.0-alpha02-0.5.1"
qrose = "1.1.2"
reorderable = "3.1.0"
roomVersion = "3.0.0-alpha06"
scanner = "0.7.1"
sqlite = "2.6.2"
urlencoderLib = "1.6.0"
xemanticAiToolSchema = "1.2.0"
kmpalette = "3.1.0"

[libraries]
# About Libraries (KMP)
aboutLibrariesCore = { module = "com.mikepenz:aboutlibraries-core", version.ref = "latestAboutLibsRelease" }
aboutLibrariesCompose = { module = "com.mikepenz:aboutlibraries-compose-m3", version.ref = "latestAboutLibsRelease" }

# AI/LLM
anthropic-sdk-kotlin = { module = "com.xemantic.ai:anthropic-sdk-kotlin", version.ref = "anthropicSdkKotlin" }
generic-ai = { module = "io.github.bay73:generic-ai", version.ref = "genericAi" }
generativeai-google = { module = "dev.shreyaspatil.generativeai:generativeai-google", version.ref = "generativeaiGoogle" }
koog-agents = { module = "ai.koog:koog-agents", version.ref = "koog" }
koog-agents-additions = { module = "ai.koog:koog-agents-additions", version.ref = "koog-beta" }
koog-memory = { module = "ai.koog:agents-features-memory", version.ref = "koog" }
xemantic-ai-tool-schema = { module = "com.xemantic.ai:xemantic-ai-tool-schema", version.ref = "xemanticAiToolSchema" }

# Coil (KMP)
coilCompose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coilOkHttp = { module = "io.coil-kt.coil3:coil-network-okhttp", version.ref = "coil" }

# Compose Multiplatform
compose-material3 = { module = "org.jetbrains.compose.material3:material3", version = "1.12.0-alpha02" }
foundation = { module = "org.jetbrains.compose.foundation:foundation", version.ref = "foundation" }
runtime = { module = "org.jetbrains.compose.runtime:runtime", version.ref = "compose-multiplatform" }
ui = { module = "org.jetbrains.compose.ui:ui", version.ref = "compose-multiplatform" }
cmp-ui-util = { module = "org.jetbrains.compose.ui:ui-util", version.ref = "compose-multiplatform" }
ui-backhandler = { module = "org.jetbrains.compose.ui:ui-backhandler", version.ref = "compose-multiplatform" }
material-icons-extended = { module = "org.jetbrains.compose.material:material-icons-extended", version.ref = "materialIconsExtended" }
material3-adaptive-navigation-suite = { module = "org.jetbrains.compose.material3:material3-adaptive-navigation-suite", version.ref = "material3AdaptiveNavigationSuite" }
components-resources = { module = "org.jetbrains.compose.components:components-resources", version.ref = "compose-multiplatform" }
materialAdaptiveCmp = { group = "org.jetbrains.compose.material3.adaptive", name = "adaptive", version = "1.2.0" }
materialAdaptiveLayoutCmp = { group = "org.jetbrains.compose.material3.adaptive", name = "adaptive-layout", version = "1.2.0" }
materialAdaptiveLayoutNavCmp = { group = "org.jetbrains.compose.material3.adaptive", name = "adaptive-navigation", version = "1.2.0" }
material3-window-size = { module = "org.jetbrains.compose.material3:material3-window-size-class", version.ref = "material3WindowSizeClassVersion" }

# Connectivity
connectivity-core = { module = "dev.jordond.connectivity:connectivity-core", version.ref = "connectivity" }
connectivity-compose = { module = "dev.jordond.connectivity:connectivity-compose", version.ref = "connectivity" }
connectivity-device = { module = "dev.jordond.connectivity:connectivity-device", version.ref = "connectivity" }
connectivity-compose-device = { module = "dev.jordond.connectivity:connectivity-compose-device", version.ref = "connectivity" }
connectivity-http = { module = "dev.jordond.connectivity:connectivity-http", version.ref = "connectivity" }
connectivity-compose-http = { module = "dev.jordond.connectivity:connectivity-compose-http", version.ref = "connectivity" }

# Coroutines
coroutinesCore = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutinesVersion" }
coroutinesTest = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }

# DataStore
datastore = { module = "androidx.datastore:datastore-core", version.ref = "datastore" }
datastorePref = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
datastoreOkio = { module = "androidx.datastore:datastore-core-okio", version.ref = "datastore" }

# FileKit
filekit-core = { module = "io.github.vinceglb:filekit-core", version.ref = "filekitCore" }
filekit-dialogs-compose = { module = "io.github.vinceglb:filekit-dialogs-compose", version.ref = "filekitCore" }

# Haze
haze = { module = "dev.chrisbanes.haze:haze", version.ref = "haze" }
haze-blur = { module = "dev.chrisbanes.haze:haze-blur", version.ref = "haze" }
haze-materials = { module = "dev.chrisbanes.haze:haze-blur-materials", version.ref = "haze" }

# Kamel
kamel-image = { module = "media.kamel:kamel-image-default", version.ref = "kamelImage" }
kamel-decoder-animated-image = { module = "media.kamel:kamel-decoder-animated-image", version.ref = "kamelImage" }
kamel-decoder-image-bitmap = { module = "media.kamel:kamel-decoder-image-bitmap", version.ref = "kamelImage" }
kamel-decoder-image-bitmap-resizing = { module = "media.kamel:kamel-decoder-image-bitmap-resizing", version.ref = "kamelImage" }
kamel-decoder-image-vector = { module = "media.kamel:kamel-decoder-image-vector", version.ref = "kamelImage" }
kamel-decoder-svg-std = { module = "media.kamel:kamel-decoder-svg-std", version.ref = "kamelImage" }
kamel-decoder-svg-batik = { module = "media.kamel:kamel-decoder-svg-batik", version.ref = "kamelImage" }

# Kfswatch
kfswatch = { module = "io.github.irgaly.kfswatch:kfswatch", version.ref = "kfswatch" }

# Koin
koin-bom = { module = "io.insert-koin:koin-bom", version.ref = "koin-bom" }
koin-core = { module = "io.insert-koin:koin-core" }
koinCores = { module = "io.insert-koin:koin-core" }
koinComposeKmp = { module = "io.insert-koin:koin-compose" }
koinViewModel = { module = "io.insert-koin:koin-compose-viewmodel" }
koinViewModelNavigation = { module = "io.insert-koin:koin-compose-viewmodel-navigation" }

# Kotlinx
kotlinxSerialization = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version = "1.11.0" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }
kotlin-test = { group = "org.jetbrains.kotlin", name = "kotlin-test", version.ref = "kotlinTest" }

# Ktor
ktorCore = { module = "io.ktor:ktor-client-core", version.ref = "ktorVersion" }
ktorMock = { module = "io.ktor:ktor-client-mock", version.ref = "ktorVersion" }
ktorAuth = { module = "io.ktor:ktor-client-auth", version.ref = "ktorVersion" }
ktorAndroid = { module = "io.ktor:ktor-client-android", version.ref = "ktorVersion" }
ktorLogging = { module = "io.ktor:ktor-client-logging", version.ref = "ktorVersion" }
ktorSerialization = { module = "io.ktor:ktor-client-serialization", version.ref = "ktorVersion" }
ktorJson = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktorVersion" }
ktorContentNegotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktorVersion" }
ktorOkHttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktorVersion" }
ktorGson = { module = "io.ktor:ktor-serialization-gson", version.ref = "ktorVersion" }
ktorJsoup = "com.tfowl.ktor:ktor-jsoup:2.3.0"

# Lifecycle KMP
lifecycle-viewmodel-compose = { group = "org.jetbrains.androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "cmpLifecycle" }
multiplatform-lifecycle-runtime-compose = { group = "org.jetbrains.androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "androidx-lifecycle" }

# Material
material-kolor = { module = "com.materialkolor:material-kolor", version.ref = "materialKolor" }
kmpalette-core = { module = "com.kmpalette:kmpalette-core", version.ref = "kmpalette" }

# Moko
moko-biometry = { module = "dev.icerock.moko:biometry", version.ref = "mokoBiometry" }
moko-biometry-compose = { module = "dev.icerock.moko:biometry-compose", version.ref = "mokoBiometry" }

# Navigation KMP
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
androidx-material3-navigation3 = { group = "androidx.compose.material3.adaptive", name = "adaptive-navigation3", version.ref = "nav3Material" }
androidx-navigationevent = { module = "androidx.navigationevent:navigationevent", version.ref = "navigationevent" }
androidx-navigationevent-compose = { module = "androidx.navigationevent:navigationevent-compose", version.ref = "navigationevent" }
androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycleViewmodelNavigation3" }
cmp-navigation3-ui = { group = "org.jetbrains.androidx.navigation3", name = "navigation3-ui", version.ref = "cmp-nav3" }
cmp-lifecycle-viewmodel-navigation3 = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "cmpLifecycle" }
cmp-material3-adaptive-nav3 = { module = "org.jetbrains.compose.material3.adaptive:adaptive-navigation3", version.ref = "cmp-nav3-material3" }
cmp-navigationevent-compose = { module = "org.jetbrains.androidx.navigationevent:navigationevent-compose", version.ref = "cmp-navevent" }

# Room KMP
roomCompiler = { module = "androidx.room3:room3-compiler", version.ref = "roomVersion" }
roomRuntime = { module = "androidx.room3:room3-runtime", version.ref = "roomVersion" }
roomPaging = { module = "androidx.room3:room3-paging", version.ref = "roomVersion" }
androidx-room-sqlite = { group = "androidx.sqlite", name = "sqlite-bundled", version.ref = "sqlite" }

# UI Polish
backdrop = { module = "io.github.kyant0:backdrop", version.ref = "backdrop" }
blurhash = { module = "com.vanniktech:blurhash", version.ref = "blurhash" }
compottie = { module = "io.github.alexzhirkevich:compottie", version.ref = "compottie" }
compose-constraintlayout-compose-multiplatform = { module = "tech.annexflow.compose:constraintlayout-compose-multiplatform", version = "0.6.1-shaded-core" }
constraintlayout-compose-multiplatform = { module = "tech.annexflow.compose:constraintlayout-compose-multiplatform", version = "0.6.1-shaded" }
compose-webview-multiplatform = { module = "io.github.kevinnzou:compose-webview-multiplatform", version.ref = "composeWebviewMultiplatform" }
dragselect = { module = "com.dragselectcompose:dragselect", version.ref = "dragselect" }
heatmap = { module = "com.fleeys:heatmap", version.ref = "heatmap" }
human-readable = { module = "nl.jacobras:Human-Readable", version.ref = "humanReadable" }
kotzilla-sdk-compose = { group = "io.kotzilla", name = "kotzilla-sdk-compose", version.ref = "kotzilla" }
markdown-renderer = { module = "com.mikepenz:multiplatform-markdown-renderer-m3", version.ref = "markdown-renderer" }
paging-compose-common = { module = "app.cash.paging:paging-compose-common", version.ref = "pagingComposeCommon" }
qrose = { module = "io.github.alexzhirkevich:qrose", version.ref = "qrose" }
reorderable = { module = "sh.calvin.reorderable:reorderable", version.ref = "reorderable" }
scanner = { module = "io.github.kalinjul.easyqrscan:scanner", version.ref = "scanner" }
sonner = { module = "io.github.dokar3:sonner", version = "0.3.9" }
urlencoder-lib = { module = "net.thauvin.erik.urlencoder:urlencoder-lib", version.ref = "urlencoderLib" }
zoomableModifier = "net.engawapg.lib:zoomable:2.12.0"

[bundles]
datastoreLibs = ["datastore", "datastorePref"]
kamel = [
    "kamel-image",
    "kamel-decoder-animated-image",
    "kamel-decoder-image-bitmap",
    "kamel-decoder-image-bitmap-resizing",
    "kamel-decoder-image-vector",
    "kamel-decoder-svg-batik",
    "kamel-decoder-svg-std"
]
koinKmp = [
    "koinCores",
    "koinComposeKmp",
    "koinViewModel",
    "koinViewModelNavigation"
]
ktorLibs = [
    "ktorCore",
    "ktorAuth",
    "ktorAndroid",
    "ktorLogging",
    "ktorSerialization",
    "ktorJson",
    "ktorContentNegotiation",
    "ktorOkHttp",
    "ktorGson",
    "ktorJsoup"
]
roomLibs = ["roomRuntime"]
```

- [ ] **Step 2: Verify file parses (Gradle will validate on sync in Task 7)**

```bash
# Quick TOML syntax check — look for duplicate keys or malformed entries
grep -c "^\[" gradle/common.versions.toml
# Expected: 4 (versions, libraries, bundles sections + 1 for the file header check)
```

- [ ] **Step 3: Commit**

```bash
git add gradle/common.versions.toml
git commit -m "build: add common.versions.toml for KMP runtime deps"
```

---

### Task 2: Create `gradle/android.versions.toml`

**Files:**

- Create: `gradle/android.versions.toml`

- [ ] **Step 1: Create the file**

```toml
[versions]
ackpineVersion = "0.23.0"
accompanist = "0.37.3"
activity = "1.13.0"
arcore = "1.0.0-alpha13"
autoBinding = "1.1-beta04"
barcodeScanning = "17.3.0"
benchmark-macro-junit4 = "1.4.1"
biometricVersion = "1.4.0-alpha07"
coil = "3.4.0"
composeBomVersion = "2026.05.02"
composeCollapsable = "0.4.0"
composeXr = "1.0.0-alpha13"
core = "1.7.0"
corePerformance = "1.0.0"
coroutinesVersion = "1.11.0"
dragselect = "3.3.0"
espresso-core = "3.7.0"
firebaseBom = "34.14.1"
generativeai = "0.9.0"
glideVersion = "5.0.7"
junit = "1.3.0"
koin-bom = "4.2.2"
landscapist = "2.9.8"
latestAboutLibsRelease = "14.2.1"
lifecycle = "2.10.0"
lottieCompose = "6.7.1"
media3Version = "1.10.1"
okhttpVersion = "5.3.2"
pagecurl = "1.5.1"
pagingVersion = "3.5.0"
piasy = "1.8.1"
profileinstaller = "1.4.1"
protobufVersion = "4.34.1"
roomVersion = "3.0.0-alpha06"
runner = "1.7.0"
scenecore = "1.0.0-alpha14"
sketchVersion = "3.3.2"
telephoto = "0.19.0"
textflowMaterial3 = "1.2.1"
uiautomator = "2.3.0"
workVersion = "2.11.2"
workinspector = "1.2"
ziplineVersion = "1.27.0"
zoomimageComposeGlide = "1.4.0"

[libraries]
# Accompanist
adaptive = { module = "com.google.accompanist:accompanist-adaptive", version.ref = "accompanist" }
drawablePainter = { module = "com.google.accompanist:accompanist-drawablepainter", version.ref = "accompanist" }

# Ackpine
ackpine-core = { module = "ru.solrudev.ackpine:ackpine-core", version.ref = "ackpineVersion" }
ackpine-ktx = { module = "ru.solrudev.ackpine:ackpine-ktx", version.ref = "ackpineVersion" }

# Android Foundation
androidBrowserHelper = "com.google.androidbrowserhelper:androidbrowserhelper:2.7.1"
androidCore = "androidx.core:core-ktx:1.19.0"
androidxBrowser = "androidx.browser:browser:1.10.0"
androidxLegacySupport = "androidx.legacy:legacy-support-v4:1.0.0"
androidxWebkit = "androidx.webkit:webkit:1.16.0"
androidxWindow = "androidx.window:window:1.5.1"
appCompat = "androidx.appcompat:appcompat:1.7.1"
androidx-activity = { module = "androidx.activity:activity", version.ref = "activity" }
androidx-activity-ktx = { module = "androidx.activity:activity-ktx", version.ref = "activity" }
androidx-core-performance = { module = "androidx.core:core-performance", version.ref = "corePerformance" }
constraintlayout = "androidx.constraintlayout:constraintlayout:2.2.1"
coreLibraryDesugaring = { module = "com.android.tools:desugar_jdk_libs", version = "2.1.5" }
fragmentKtx = "androidx.fragment:fragment-ktx:1.8.9"
iconicsCore = "com.mikepenz:iconics-core:5.5.0"
material = "com.google.android.material:material:1.14.0"
preference = "androidx.preference:preference-ktx:1.2.1"
recyclerview = "androidx.recyclerview:recyclerview:1.4.0"
swiperefresh = "androidx.swiperefreshlayout:swiperefreshlayout:1.2.0"

# AnimeWorld
autoBindings = { module = "io.github.kaustubhpatange:autobindings", version.ref = "autoBinding" }
autoBindingsCompiler = { module = "io.github.kaustubhpatange:autobindings-compiler", version.ref = "autoBinding" }
castFramework = "com.google.android.gms:play-services-cast-framework:22.3.1"
localCast = "com.github.KaustubhPatange:Android-Cast-Local-Sample:0.01"
mediaRouter = "androidx.mediarouter:mediarouter:1.8.1"
slideToAct = "com.ncorti:slidetoact:0.11.0"
superForwardView = "com.github.ertugrulkaragoz:SuperForwardView:0.2"
torrentStream = "com.github.TorrentStream:TorrentStream-Android:3.0.0"

# Barcode
barcode-scanning = { module = "com.google.mlkit:barcode-scanning", version.ref = "barcodeScanning" }

# Biometric
biometric = { module = "androidx.biometric:biometric", version.ref = "biometricVersion" }

# Coil Android (duplicated from commonLibs for compose bundle)
coilCompose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coilGif = { module = "io.coil-kt.coil3:coil-gif", version.ref = "coil" }
coilOkHttp = { module = "io.coil-kt.coil3:coil-network-okhttp", version.ref = "coil" }
coilVideo = { module = "io.coil-kt.coil3:coil-video", version.ref = "coil" }

# Compose BOM + Android Compose
adaptive-layout-android = { group = "androidx.compose.material3.adaptive", name = "adaptive-layout-android" }
adaptive-navigation-android = { group = "androidx.compose.material3.adaptive", name = "adaptive-navigation-android" }
composeActivity = { module = "androidx.activity:activity-compose", version.ref = "activity" }
composeAnimation = { group = "androidx.compose.animation", name = "animation" }
composeCollapsable = { module = "me.tatarka.compose.collapsable:compose-collapsable", version.ref = "composeCollapsable" }
composeConstraintLayout = "androidx.constraintlayout:constraintlayout-compose:1.1.1"
composeFoundation = { group = "androidx.compose.foundation", name = "foundation" }
composeLifecycle = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
composeLifecycleRuntime = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
composeMaterialIconsCore = { group = "androidx.compose.material", name = "material-icons-core" }
composeMaterialIconsExtended = { group = "androidx.compose.material", name = "material-icons-extended" }
composePlatform = { module = "androidx.compose:compose-bom-alpha", version.ref = "composeBomVersion" }
composeRuntimeLivedata = { group = "androidx.compose.runtime", name = "runtime-livedata" }
composeUi = { group = "androidx.compose.ui", name = "ui" }
composeUiTooling = { group = "androidx.compose.ui", name = "ui-tooling" }
composeViewBinding = { group = "androidx.compose.ui", name = "ui-viewbinding" }
material-adaptive-navigation-suite = { group = "androidx.compose.material3", name = "material3-adaptive-navigation-suite" }
materialAdaptive = { group = "androidx.compose.material3.adaptive", name = "adaptive" }
materialWindow = { group = "androidx.compose.material3", name = "material3-window-size-class" }
materialYou = { group = "androidx.compose.material3", name = "material3" }
materialYou-common = { group = "androidx.compose.material3", name = "material3-common" }
uiUtil = { group = "androidx.compose.ui", name = "ui-util" }

# Coroutines Android
coroutinesAndroid = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutinesVersion" }
coroutinesPlayServices = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services", version.ref = "coroutinesVersion" }

# ExoPlayer / Media3
exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "media3Version" }
exoplayerDash = { module = "androidx.media3:media3-exoplayer-dash", version.ref = "media3Version" }
exoplayerHls = { module = "androidx.media3:media3-exoplayer-hls", version.ref = "media3Version" }
exoplayerRtsp = { module = "androidx.media3:media3-exoplayer-rtsp", version.ref = "media3Version" }
exoplayerIma = { module = "androidx.media3:media3-exoplayer-ima", version.ref = "media3Version" }
exoplayerdatasourceCronet = { module = "androidx.media3:media3-datasource-cronet", version.ref = "media3Version" }
exoplayerdatasourceOkhttp = { module = "androidx.media3:media3-datasource-okhttp", version.ref = "media3Version" }
exoplayerdatasourceRtmp = { module = "androidx.media3:media3-datasource-rtmp", version.ref = "media3Version" }
exoplayerui = { module = "androidx.media3:media3-ui", version.ref = "media3Version" }
exoplayersession = { module = "androidx.media3:media3-session", version.ref = "media3Version" }
exoplayerextractor = { module = "androidx.media3:media3-extractor", version.ref = "media3Version" }
exoplayercast = { module = "androidx.media3:media3-cast", version.ref = "media3Version" }
exoplayerWorkmanager = { module = "androidx.media3:media3-exoplayer-workmanager", version.ref = "media3Version" }
exoplayertransformer = { module = "androidx.media3:media3-transformer", version.ref = "media3Version" }
exoplayertestUtils = { module = "androidx.media3:media3-test-utils", version.ref = "media3Version" }
exoplayertestUtilsRobolectric = { module = "androidx.media3:media3-test-utils-robolectric", version.ref = "media3Version" }
exoplayerdatabase = { module = "androidx.media3:media3-database", version.ref = "media3Version" }
exoplayerdecoder = { module = "androidx.media3:media3-decoder", version.ref = "media3Version" }
exoplayerdatasource = { module = "androidx.media3:media3-datasource", version.ref = "media3Version" }
exoplayercommon = { module = "androidx.media3:media3-common", version.ref = "media3Version" }
exoplayerleanback = { module = "androidx.media3:media3-ui-leanback", version.ref = "media3Version" }

# Firebase
analytics = { group = "com.google.firebase", name = "firebase-analytics" }
crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics" }
firebase-perf = { module = "com.google.firebase:firebase-perf" }
firebaseAuth = { group = "com.google.firebase", name = "firebase-auth" }
firebaseDatabase = { group = "com.google.firebase", name = "firebase-database" }
firebaseFirestore = { group = "com.google.firebase", name = "firebase-firestore" }
firebasePlatform = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
firebaseUiAuth = "com.firebaseui:firebase-ui-auth:9.1.1"
generativeai = { module = "com.google.ai.client.generativeai:generativeai", version.ref = "generativeai" }
playServices = "com.google.android.gms:play-services-auth:21.6.0"

# Glide
glide = { module = "com.github.bumptech.glide:glide", version.ref = "glideVersion" }
glideCompose = "com.github.bumptech.glide:compose:1.0.0-beta09"
glideCompiler = { module = "com.github.bumptech.glide:ksp", version.ref = "glideVersion" }
glideRecyclerview = { module = "com.github.bumptech.glide:recyclerview-integration", version.ref = "glideVersion" }

# JSON / HTTP Android
duktape = "com.squareup.duktape:duktape-android:1.4.0"
fileChooser = "com.github.hedzr:android-file-chooser:1.2.0"
gson = "com.google.code.gson:gson:2.13.2"
jsoup = "org.jsoup:jsoup:1.22.2"
karnKhttp = "io.karn:khttp-android:0.1.2"
kotson = "com.github.salomonbrys.kotson:kotson:2.5.0"
palette = "androidx.palette:palette-ktx:1.0.0"
reactiveNetwork = "ru.beryukhov:flowreactivenetwork:1.0.4"
retrofit = "com.squareup.retrofit2:retrofit:3.0.0"
retrofitGson = "com.squareup.retrofit2:converter-gson:3.0.0"
rhino = "org.mozilla:rhino:1.9.1"

# Koin Android (duplicated koin-core for koinLibs bundle)
koin-android = { module = "io.insert-koin:koin-android" }
koin-core = { module = "io.insert-koin:koin-core" }
koin-workmanager = { module = "io.insert-koin:koin-androidx-workmanager" }
koinCompose = { module = "io.insert-koin:koin-androidx-compose" }

# Landscapist
landscapist-bom = { module = "com.github.skydoves:landscapist-bom", version.ref = "landscapist" }
landscapistGlide = { group = "com.github.skydoves", name = "landscapist-glide" }
landscapistPalette = { group = "com.github.skydoves", name = "landscapist-palette" }
landscapistPlaceholder = { group = "com.github.skydoves", name = "landscapist-placeholder" }

# Leanback / TV
leanback = "androidx.leanback:leanback:1.2.0"
leanbackPreference = "androidx.leanback:leanback-preference:1.2.0"
tv-foundation = { module = "androidx.tv:tv-foundation", version = "1.0.0" }
tv-material = { module = "androidx.tv:tv-material", version = "1.1.0" }

# Lifecycle Android
lifecycleExtensions = "androidx.lifecycle:lifecycle-extensions:2.2.0"
lifecycleLivedata = { module = "androidx.lifecycle:lifecycle-livedata-ktx", version.ref = "lifecycle" }
lifecycleRuntime = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
lifecycleViewModel = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "lifecycle" }

# Lottie
lottie-compose = { module = "com.airbnb.android:lottie-compose", version.ref = "lottieCompose" }

# MangaWorld Android
bigImageGlideLoader = { module = "com.github.piasy:GlideImageLoader", version.ref = "piasy" }
bigImageViewer = { module = "com.github.piasy:BigImageViewer", version.ref = "piasy" }
pagecurl = { module = "io.github.oleksandrbalan:pagecurl", version.ref = "pagecurl" }
panpf-zoomimage-compose-glide = { module = "io.github.panpf.zoomimage:zoomimage-compose-glide", version.ref = "zoomimageComposeGlide" }
progressPieIndicator = { module = "com.github.piasy:ProgressPieIndicator", version.ref = "piasy" }
subsamplingImageView = "com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0"
telephoto-zoomable-image-glide = { module = "me.saket.telephoto:zoomable-image-glide", version.ref = "telephoto" }
textflow-material3 = { module = "io.github.oleksandrbalan:textflow-material3", version.ref = "textflowMaterial3" }
zoomable-peek-overlay = { module = "me.saket.telephoto:zoomable-peek-overlay", version.ref = "telephoto" }

# Misc Android UI
lazyColumnScrollbar = "com.github.nanihadesuka:LazyColumnScrollbar:2.2.0"
showMoreLess = "com.github.noowenz:ShowMoreLess:1.0.3"
toolbarCompose = "me.onebone:toolbar-compose:2.3.5"
workinspector = { module = "com.github.koitharu:workinspector", version.ref = "workinspector" }

# ML Kit
mlkitLanguage = "com.google.mlkit:language-id:17.0.6"
mlkitTranslate = "com.google.mlkit:translate:17.0.3"

# OkHttp
okhttpDns = { module = "com.squareup.okhttp3:okhttp-dnsoverhttps", version.ref = "okhttpVersion" }
okhttpLib = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttpVersion" }

# Paging
pagingCompose = { module = "androidx.paging:paging-compose", version.ref = "pagingVersion" }
pagingRuntime = { module = "androidx.paging:paging-runtime-ktx", version.ref = "pagingVersion" }
roomPaging = { module = "androidx.room3:room3-paging", version.ref = "roomVersion" }

# Protobuf
protobufJava = { module = "com.google.protobuf:protobuf-javalite", version.ref = "protobufVersion" }
protobufKotlin = { module = "com.google.protobuf:protobuf-kotlin-lite", version.ref = "protobufVersion" }

# Sketch
sketch-compose = { module = "io.github.panpf.sketch3:sketch-compose", version.ref = "sketchVersion" }
sketch-extensions = { module = "io.github.panpf.sketch3:sketch-extensions", version.ref = "sketchVersion" }
sketch-gif = { module = "io.github.panpf.sketch3:sketch-gif-koral", version.ref = "sketchVersion" }
sketch-zoom = { module = "io.github.panpf.sketch3:sketch-zoom", version.ref = "sketchVersion" }

# Tests
androidx-core = { group = "androidx.test", name = "core", version.ref = "core" }
androidx-runner = { group = "androidx.test", name = "runner", version.ref = "runner" }
benchmark-macro-junit4 = { group = "androidx.benchmark", name = "benchmark-macro-junit4", version.ref = "benchmark-macro-junit4" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso-core" }
junit = { group = "androidx.test.ext", name = "junit", version.ref = "junit" }
profileinstaller = { group = "androidx.profileinstaller", name = "profileinstaller", version.ref = "profileinstaller" }
uiautomator = { group = "androidx.test.uiautomator", name = "uiautomator", version.ref = "uiautomator" }

# WorkManager
workRuntime = { module = "androidx.work:work-runtime", version.ref = "workVersion" }

# XR
androidx-arcore = { module = "androidx.xr.arcore:arcore", version.ref = "arcore" }
androidx-compose-xr = { module = "androidx.xr.compose:compose", version.ref = "composeXr" }
androidx-scenecore = { module = "androidx.xr.scenecore:scenecore", version.ref = "scenecore" }
androidx-xr-material3 = { module = "androidx.xr.compose.material3:material3", version = "1.0.0-alpha16" }

# Zipline
ziplineLoader = { module = "app.cash.zipline:zipline-loader", version.ref = "ziplineVersion" }
ziplineProfiler = { module = "app.cash.zipline:zipline-profiler", version.ref = "ziplineVersion" }

[bundles]
compose = [
    "composeUi", "composeUiTooling", "composeFoundation",
    "composeMaterialIconsCore", "composeMaterialIconsExtended",
    "composeAnimation",
    "composeActivity", "composeLifecycle", "composeLifecycleRuntime",
    "composeRuntimeLivedata",
    "landscapist-bom", "landscapistGlide", "landscapistPalette", "landscapistPlaceholder",
    "coilCompose", "coilOkHttp",
    "composeConstraintLayout",
    "drawablePainter", "uiUtil",
    "materialYou",
    "materialWindow",
]
composeTv = [
    "composeUi", "composeUiTooling",
    "composeMaterialIconsCore", "composeMaterialIconsExtended",
    "composeAnimation",
    "composeActivity", "composeLifecycle", "composeLifecycleRuntime",
    "composeRuntimeLivedata",
    "landscapist-bom", "landscapistGlide", "landscapistPalette", "landscapistPlaceholder",
    "coilCompose",
    "composeConstraintLayout",
    "drawablePainter", "uiUtil",
]
firebaseCrashLibs = ["crashlytics", "analytics", "firebase-perf"]
koinLibs = ["koin-android", "koin-core", "koin-workmanager", "koinCompose"]
leanbackLibs = ["leanback", "leanbackPreference"]
media3 = [
    "exoplayer",
    "exoplayerDash", "exoplayerHls", "exoplayerRtsp", "exoplayerIma",
    "exoplayerdatasourceCronet", "exoplayerdatasource", "exoplayerdatasourceRtmp", "exoplayerdatasourceOkhttp",
    "exoplayerui", "exoplayersession", "exoplayerextractor", "exoplayercast",
    "exoplayerWorkmanager", "exoplayertransformer",
    "exoplayerdatabase", "exoplayerdecoder", "exoplayercommon"
]
okHttpLibs = ["okhttpLib", "okhttpDns"]
pagingLibs = ["pagingRuntime", "roomPaging"]
piasyLibs = ["bigImageViewer", "bigImageGlideLoader", "progressPieIndicator"]
protobuf = ["protobufJava", "protobufKotlin"]
sketch = ["sketch-compose", "sketch-extensions", "sketch-gif", "sketch-zoom"]
xr = ["androidx-arcore", "androidx-compose-xr", "androidx-scenecore", "androidx-xr-material3"]
ziplineLibs = ["ziplineLoader", "ziplineProfiler"]
```

- [ ] **Step 2: Commit**

```bash
git add gradle/android.versions.toml
git commit -m "build: add android.versions.toml for Android-only deps"
```

---

### Task 3: Create `gradle/desktop.versions.toml`

**Files:**

- Create: `gradle/desktop.versions.toml`

- [ ] **Step 1: Create the file**

```toml
[versions]
kfswatch = "1.4.0"
knotify = "0.4.3"
kotlinMultiplatformAppdirsVersion = "2.0.0"
kotlinx-coroutines = "1.11.0"
nucleusSystem = "1.15.7"
javase = "3.5.4"

[libraries]
core = { module = "com.google.zxing:core", version.ref = "javase" }
github-nucleus-notifications-common = { module = "io.github.kdroidfilter:nucleus.notification-common", version.ref = "nucleusSystem" }
github-nucleus-scheduler = { module = "io.github.kdroidfilter:nucleus.scheduler", version.ref = "nucleusSystem" }
github-nucleus-scheduler-testing = { module = "io.github.kdroidfilter:nucleus.scheduler-testing", version.ref = "nucleusSystem" }
github-nucleus-taskbar-progress = { module = "io.github.kdroidfilter:nucleus.taskbar-progress", version.ref = "nucleusSystem" }
javase = { module = "com.google.zxing:javase", version.ref = "javase" }
kfswatch = { module = "io.github.irgaly.kfswatch:kfswatch", version.ref = "kfswatch" }
knotify = { module = "io.github.kdroidfilter:knotify", version.ref = "knotify" }
kotlin-multiplatform-appdirs = { module = "ca.gosyer:kotlin-multiplatform-appdirs", version.ref = "kotlinMultiplatformAppdirsVersion" }
kotlinx-coroutines-swing = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-swing", version.ref = "kotlinx-coroutines" }
nucleus-system-color = { module = "io.github.kdroidfilter:nucleus.system-color", version.ref = "nucleusSystem" }
nucleus-system-info = { module = "io.github.kdroidfilter:nucleus.system-info", version.ref = "nucleusSystem" }

[bundles]
```

- [ ] **Step 2: Commit**

```bash
git add gradle/desktop.versions.toml
git commit -m "build: add desktop.versions.toml for JVM/Desktop deps"
```

---

### Task 4: Create `gradle/ios.versions.toml`

**Files:**

- Create: `gradle/ios.versions.toml`

- [ ] **Step 1: Create the file**

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

- [ ] **Step 2: Commit**

```bash
git add gradle/ios.versions.toml
git commit -m "build: add ios.versions.toml placeholder"
```

---

### Task 5: Trim `gradle/libs.versions.toml` to build tooling only

**Files:**

- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Replace entire file content**

Keep only build plugin versions, plugin classpath library declarations, and plugin aliases.
Everything else was moved to the platform catalogs in Tasks 1-3.

```toml
[versions]
androidx-baselineprofile = "1.4.1"
androidxBaselineprofileGradlePlugin = "1.3.4"
buildKonfig = "0.21.2"
compose-multiplatform = "1.12.0-alpha02"
easylauncher = "6.4.1"
firebaseCrashlyticsGradle = "3.0.7"
googleAndroidLibrariesMapsplatformSecretsGradlePlugin = "2.0.1"
googlePerformancePlugin = "2.0.2"
googleServices = "4.4.4"
gradle = "9.1.1"
kotlin = "2.4.0"
kotzilla = "2.2.2"
kspVersion = "2.3.9"
latestAboutLibsRelease = "14.2.1"
roomVersion = "3.0.0-alpha06"
protobufGradlePlugin = "0.10.0"

[libraries]
androidx-baselineprofile-gradle-plugin = { module = "androidx.baselineprofile:androidx.baselineprofile.gradle.plugin", version.ref = "androidx-baselineprofile" }
easylauncher = { module = "com.project.starter:easylauncher", version.ref = "easylauncher" }
firebase-crashlytics-gradle = { module = "com.google.firebase:firebase-crashlytics-gradle", version.ref = "firebaseCrashlyticsGradle" }
google-services = { module = "com.google.gms:google-services", version.ref = "googleServices" }
gradle = { module = "com.android.tools.build:gradle", version.ref = "gradle" }
kotlinGp = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin", version.ref = "kotlin" }
kotlinStLib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
protobuf-gradle-plugin = { module = "com.google.protobuf:protobuf-gradle-plugin", version.ref = "protobufGradlePlugin" }

[plugins]
android-kotlin-multiplatform-library = { id = "com.android.kotlin.multiplatform.library", version.ref = "gradle" }
buildKonfig = { id = "com.codingfeline.buildkonfig", version.ref = "buildKonfig" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
google-android-libraries-mapsplatform-secrets-gradle-plugin = { id = "com.google.android.libraries.mapsplatform.secrets-gradle-plugin", version.ref = "googleAndroidLibrariesMapsplatformSecretsGradlePlugin" }
google-firebase-performance = { id = "com.google.firebase.firebase-perf", version.ref = "googlePerformancePlugin" }
hotswan-compiler = { id = "com.github.skydoves.compose.hotswan.compiler", version = "1.3.5" }
koin-compiler = { id = "io.insert-koin.compiler.plugin", version = "1.0.0" }
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlinGradle = { id = "org.jetbrains.kotlin:kotlin.gradle.plugin", version.ref = "kotlin" }
kotlinSerializationGradle = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
kotzilla = { id = "io.kotzilla.kotzilla-plugin", version.ref = "kotzilla" }
ksp = { id = "com.google.devtools.ksp", version.ref = "kspVersion" }
org-jetbrains-kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
room = { id = "androidx.room3", version.ref = "roomVersion" }
```

- [ ] **Step 2: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: trim libs.versions.toml to build tooling only"
```

---

### Task 6: Update `settings.gradle.kts`

**Files:**

- Modify: `settings.gradle.kts`

- [ ] **Step 1: Add new catalog declarations**

Find the existing `versionCatalogs` block and replace it:

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

- [ ] **Step 2: Commit**

```bash
git add settings.gradle.kts
git commit -m "build: declare new platform version catalogs in settings"
```

---

### Task 7: Verify Gradle sync

**Files:** none

- [ ] **Step 1: Run sync**

```bash
./gradlew help --no-daemon 2>&1 | tail -20
```

Expected: no `Unresolved reference` or `Could not find` errors for the new catalogs. If Gradle
complains about missing libraries (because `build.gradle.kts` files still reference old `libs.*`
entries that were removed), that is expected — those will be fixed in Tasks 8–15.

If there are TOML parse errors (duplicate keys, malformed entries), fix them before proceeding.

- [ ] **Step 2: Commit if any fixes were needed**

```bash
git add gradle/
git commit -m "build: fix TOML parse errors after catalog sync"
```

---

### Task 8: Update KMP data modules

**Files:**

- Modify: `kmpmodels/build.gradle.kts`
- Modify: `favoritesdatabase/build.gradle.kts`
- Modify: `datastore/build.gradle.kts`
- Modify: `datastore/mangasettings/build.gradle.kts`
- Modify: `sharedutils/kmpextensionloader/build.gradle.kts`

- [ ] **Step 1: Update `kmpmodels/build.gradle.kts`**

Change these references:

```kotlin
// BEFORE                                    AFTER
libs.coroutinesCore                       -> commonLibs.coroutinesCore
libs.kotlinxSerialization                 -> commonLibs.kotlinxSerialization
libs.koin.bom                             -> commonLibs.koin.bom
libs.koinCores                            -> commonLibs.koinCores
libs.kotlin.test                          -> commonLibs.kotlin.test
```

Final dependencies block:

```kotlin
sourceSets {
    commonMain {
        dependencies {
            implementation(libs.kotlinStLib)
            implementation(commonLibs.coroutinesCore)
            implementation(commonLibs.kotlinxSerialization)
            implementation(project.dependencies.platform(commonLibs.koin.bom))
            implementation(commonLibs.koinCores)
        }
    }

    commonTest {
        dependencies {
            implementation(commonLibs.kotlin.test)
        }
    }

    androidMain {
        dependencies {
            implementation(projects.models)
        }
    }

    iosMain {
        dependencies {
        }
    }
}
```

- [ ] **Step 2: Update `favoritesdatabase/build.gradle.kts`**

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(commonLibs.kotlinxSerialization)
        implementation(commonLibs.roomRuntime)
        implementation(commonLibs.roomPaging)
        implementation(projects.kmpmodels)
        implementation(commonLibs.kotlinx.datetime)
    }

    jvmMain.dependencies {
        implementation(commonLibs.androidx.room.sqlite)
        implementation(desktopLibs.kotlin.multiplatform.appdirs)
    }

    androidMain.dependencies {
        implementation(projects.models)
    }
}

dependencies {
    add("ksp", commonLibs.roomCompiler)
}
```

- [ ] **Step 3: Update `datastore/build.gradle.kts`**

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(commonLibs.kotlinxSerialization)
        implementation(commonLibs.bundles.datastoreLibs)
        implementation(compose.runtime)
        implementation(commonLibs.multiplatform.lifecycle.runtime.compose)
        implementation(commonLibs.datastoreOkio)
        implementation(commonLibs.material.kolor)
        implementation(commonLibs.kmpalette.core)
    }
}
```

- [ ] **Step 4: Update `datastore/mangasettings/build.gradle.kts`**

Replace the string-literal datastore-core-okio dependency with the catalog entry:

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(commonLibs.kotlinxSerialization)
        implementation(commonLibs.bundles.datastoreLibs)
        implementation(compose.runtime)
        implementation(commonLibs.multiplatform.lifecycle.runtime.compose)
        implementation(commonLibs.datastoreOkio)   // replaces the string literal
        implementation(projects.datastore)
    }
}
```

- [ ] **Step 5: Update `sharedutils/kmpextensionloader/build.gradle.kts`**

```kotlin
sourceSets {
    commonMain {
        dependencies {
            implementation(libs.kotlinStLib)
            implementation(commonLibs.coroutinesCore)
            implementation(projects.kmpmodels)
        }
    }

    androidMain {
        dependencies {
            implementation(projects.models)
        }
    }

    jvmMain {
        dependencies {
            implementation("net.dongliu:apk-parser:2.6.10")
            implementation("com.github.ThexXTURBOXx.dex2jar:dex-tools:v76")
            implementation("com.github.ThexXTURBOXx.dex2jar:d2j-base-cmd:v76")
            implementation(desktopLibs.kotlin.multiplatform.appdirs)
        }
    }

    jvmTest {
        dependencies {
            implementation(kotlin("test"))
            implementation(kotlin("test-junit"))
        }
    }
}
```

- [ ] **Step 6: Build check**

```bash
./gradlew :kmpmodels:compileKotlinAndroid :favoritesdatabase:compileKotlinAndroid :datastore:compileKotlinAndroid --no-daemon 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add kmpmodels/build.gradle.kts favoritesdatabase/build.gradle.kts datastore/build.gradle.kts datastore/mangasettings/build.gradle.kts sharedutils/kmpextensionloader/build.gradle.kts
git commit -m "build: migrate KMP data modules to platform version catalogs"
```

---

### Task 9: Update `kmpuiviews` and KMP UI modules

**Files:**

- Modify: `kmpuiviews/build.gradle.kts`
- Modify: `kmpuiviews/koogintegration/build.gradle.kts`
- Modify: `mangaworld/shared/build.gradle.kts`
- Modify: `novelworld/shared/build.gradle.kts`

- [ ] **Step 1: Update `kmpuiviews/build.gradle.kts`**

Full updated `sourceSets` block (plugins block is unchanged — all plugin aliases stay as `libs.*`):

```kotlin
sourceSets {
    commonMain {
        dependencies {
            implementation(libs.kotlinStLib)
            implementation(commonLibs.compose.material3)
            implementation(commonLibs.material.icons.extended)
            implementation(commonLibs.runtime)
            implementation(commonLibs.ui)
            implementation(commonLibs.cmp.ui.util)
            implementation(commonLibs.foundation)
            implementation(commonLibs.material3.adaptive.navigation.suite)
            implementation(commonLibs.components.resources)
            api(commonLibs.ui.backhandler)
            implementation(commonLibs.material3.window.size)
            api(commonLibs.haze)
            api(commonLibs.haze.blur)
            api(commonLibs.haze.materials)
            api(commonLibs.backdrop)
            implementation(commonLibs.material.kolor)
            api(commonLibs.kamel.image)
            api(commonLibs.kamel.decoder.animated.image)
            api(commonLibs.kamel.decoder.image.bitmap)
            api(commonLibs.kamel.decoder.image.vector)
            api(commonLibs.kamel.decoder.svg.std)
            api(commonLibs.coilCompose)
            api(commonLibs.kotlinxSerialization)
            api(commonLibs.ktorCore)
            implementation(commonLibs.ktorAuth)
            implementation(commonLibs.ktorLogging)
            implementation(commonLibs.ktorSerialization)
            implementation(commonLibs.ktorJson)
            implementation(commonLibs.ktorContentNegotiation)
            implementation(commonLibs.coroutinesCore)
            implementation(project.dependencies.platform(commonLibs.koin.bom))
            implementation(commonLibs.bundles.koinKmp)
            implementation(commonLibs.kmpalette.core)
            implementation(projects.favoritesdatabase)
            api(projects.datastore)
            api(projects.kmpmodels)
            implementation(projects.sharedutils.kmpextensionloader)
            implementation(commonLibs.bundles.datastoreLibs)
            api(commonLibs.kotlinx.datetime)
            implementation(commonLibs.roomRuntime)
            api(commonLibs.compose.webview.multiplatform)
            implementation(commonLibs.connectivity.core)
            implementation(commonLibs.connectivity.compose)
            api(commonLibs.filekit.core)
            implementation(commonLibs.filekit.dialogs.compose)
            implementation(commonLibs.lifecycle.viewmodel.compose)
            implementation(commonLibs.aboutLibrariesCore)
            implementation(commonLibs.aboutLibrariesCompose)
            implementation(commonLibs.sonner)
            implementation(commonLibs.urlencoder.lib)
            implementation(commonLibs.dragselect)
            implementation(commonLibs.compottie)
            implementation(commonLibs.roomPaging)
            implementation(commonLibs.constraintlayout.compose.multiplatform)
            implementation(commonLibs.compose.constraintlayout.compose.multiplatform)
            implementation(commonLibs.qrose)
            implementation(commonLibs.androidx.navigation3.runtime)
            implementation(commonLibs.scanner)
            implementation(commonLibs.multiplatform.lifecycle.runtime.compose)
            implementation(commonLibs.materialAdaptiveCmp)
            implementation(commonLibs.materialAdaptiveLayoutCmp)
            implementation(commonLibs.materialAdaptiveLayoutNavCmp)
            implementation(commonLibs.reorderable)
            implementation(commonLibs.paging.compose.common)
            implementation(commonLibs.generativeai.google)
            implementation(commonLibs.generic.ai)
            implementation(commonLibs.anthropic.sdk.kotlin)
            implementation(commonLibs.xemantic.ai.tool.schema)
            implementation(commonLibs.cmp.navigation3.ui)
            implementation(commonLibs.cmp.lifecycle.viewmodel.navigation3)
            implementation(commonLibs.cmp.navigationevent.compose)
            implementation(commonLibs.cmp.material3.adaptive.nav3)
        }
    }

    commonTest {
        dependencies {
            implementation(commonLibs.kotlin.test)
            implementation(commonLibs.coroutinesTest)
        }
    }

    androidMain {
        dependencies {
            implementation(commonLibs.heatmap)
            implementation(commonLibs.kamel.decoder.image.bitmap.resizing)
            implementation(commonLibs.kamel.decoder.svg.batik)
            implementation(commonLibs.ktorAndroid)
            implementation(androidx.browser.browser)
            implementation(androidLibs.androidBrowserHelper)
            implementation(project.dependencies.platform(androidLibs.firebasePlatform))
            implementation(androidLibs.firebaseAuth)
            implementation(androidLibs.playServices)
            implementation(androidLibs.bundles.firebaseCrashLibs)
            implementation(androidLibs.drawablePainter)
            implementation(androidLibs.ackpine.core)
            implementation(androidLibs.ackpine.ktx)
            implementation(androidLibs.glideCompose)
            implementation(androidLibs.landscapist.bom)
            implementation(androidLibs.landscapistGlide)
            implementation(androidLibs.landscapistPalette)
            implementation(androidLibs.landscapistPlaceholder)
            implementation(androidLibs.zoomable.peek.overlay)
            implementation(androidLibs.barcode.scanning)
            implementation(androidLibs.biometric)
            implementation(androidx.activity.activityKtx)
            implementation(androidLibs.lazyColumnScrollbar)
            implementation(androidLibs.workRuntime)
            implementation(androidLibs.koin.workmanager)
            implementation(androidx.paging.pagingCompose)
        }
    }

    iosMain {
        dependencies {
            implementation(commonLibs.moko.biometry)
            implementation(commonLibs.moko.biometry.compose)
        }
    }

    jvmMain {
        dependencies {
            implementation(commonLibs.heatmap)
            implementation(desktopLibs.core)
            implementation(desktopLibs.javase)
            implementation(desktopLibs.knotify)
            implementation(desktopLibs.kotlinx.coroutines.swing)
            api(desktopLibs.kotlin.multiplatform.appdirs)
            api(desktopLibs.kfswatch)
            implementation(desktopLibs.nucleus.system.color)
            api(desktopLibs.github.nucleus.scheduler)
            api(desktopLibs.github.nucleus.scheduler.testing)
            api(desktopLibs.github.nucleus.taskbar.progress)
            api(desktopLibs.github.nucleus.notifications.common)
            api(desktopLibs.nucleus.system.info)
        }
    }

    val deviceMain by creating {
        dependsOn(commonMain.get())
        androidMain.get().dependsOn(this)
        iosMain.get().dependsOn(this)
        dependencies {
            implementation(commonLibs.connectivity.device)
            implementation(commonLibs.connectivity.compose.device)
        }
    }

    val httpMain by creating {
        dependsOn(commonMain.get())
        jvmMain.get().dependsOn(this)
        dependencies {
            implementation(commonLibs.connectivity.http)
            implementation(commonLibs.connectivity.compose.http)
        }
    }

    val usesJvmMain by creating {
        dependsOn(commonMain.get())
        androidMain.get().dependsOn(this)
        jvmMain.get().dependsOn(this)
    }
}
```

- [ ] **Step 2: Update `kmpuiviews/koogintegration/build.gradle.kts`**

```kotlin
sourceSets {
    commonMain {
        dependencies {
            implementation(libs.kotlinStLib)
            implementation(commonLibs.koog.agents)
            implementation(commonLibs.koog.agents.additions)
            implementation(commonLibs.koog.memory)
            implementation(commonLibs.compose.material3)
            implementation(commonLibs.material.icons.extended)
            implementation(commonLibs.runtime)
            implementation(commonLibs.ui)
            implementation(commonLibs.cmp.ui.util)
            implementation(commonLibs.foundation)
            implementation(commonLibs.markdown.renderer)
            implementation(project.dependencies.platform(commonLibs.koin.bom))
            implementation(commonLibs.bundles.koinKmp)
            implementation(projects.favoritesdatabase)
            implementation(commonLibs.kotlinx.datetime)
        }
    }

    commonTest {
        dependencies {
            implementation(commonLibs.kotlin.test)
        }
    }

    jvmMain { }
    androidMain { dependencies { } }
    iosMain { dependencies { } }
}
```

- [ ] **Step 3: Update `mangaworld/shared/build.gradle.kts`**

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(libs.kotlinStLib)
        implementation(projects.kmpuiviews)
        implementation(commonLibs.compose.material3)
        implementation(compose.materialIconsExtended)
        implementation(compose.runtime)
        implementation(compose.ui)
        implementation(compose.uiUtil)
        implementation(compose.foundation)
        implementation(compose.material3AdaptiveNavigationSuite)
        implementation(compose.components.resources)
        implementation(commonLibs.material.kolor)
        implementation(project.dependencies.platform(commonLibs.koin.bom))
        implementation(commonLibs.bundles.koinKmp)
        implementation(projects.favoritesdatabase)
        implementation(projects.datastore)
        implementation(projects.datastore.mangasettings)
        implementation(projects.kmpmodels)
        implementation(commonLibs.bundles.datastoreLibs)
        implementation(commonLibs.androidx.navigation3.runtime)
        implementation(commonLibs.multiplatform.lifecycle.runtime.compose)
        implementation(commonLibs.zoomableModifier)
        implementation(commonLibs.coilCompose)
    }

    androidMain.dependencies {
        implementation(androidLibs.panpf.zoomimage.compose.glide)
        implementation(androidLibs.telephoto.zoomable.image.glide)
        implementation(androidLibs.workRuntime)
    }

    jvmMain.dependencies {
        implementation(compose.desktop.currentOs)
        implementation(desktopLibs.kotlinx.coroutines.swing)
    }

    jvmTest.dependencies {
        implementation(kotlin("test"))
        implementation(commonLibs.coroutinesTest)
        implementation(commonLibs.ktorMock)
    }
}
```

- [ ] **Step 4: Update `novelworld/shared/build.gradle.kts`**

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(libs.kotlinStLib)
        implementation(projects.kmpuiviews)
        implementation(commonLibs.compose.material3)
        implementation(compose.materialIconsExtended)
        implementation(compose.runtime)
        implementation(compose.ui)
        implementation(compose.foundation)
        implementation(compose.material3AdaptiveNavigationSuite)
        implementation(compose.components.resources)
        implementation(commonLibs.material.kolor)
        implementation(project.dependencies.platform(commonLibs.koin.bom))
        implementation(commonLibs.bundles.koinKmp)
        implementation(projects.favoritesdatabase)
        implementation(projects.datastore)
        implementation(projects.kmpmodels)
        implementation(commonLibs.bundles.datastoreLibs)
        implementation(commonLibs.androidx.navigation3.runtime)
    }

    jvmMain.dependencies {
        implementation(compose.desktop.currentOs)
        implementation(desktopLibs.kotlinx.coroutines.swing)
    }
}
```

- [ ] **Step 5: Build check**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid :mangaworld:shared:compileKotlinAndroid :novelworld:shared:compileKotlinAndroid --no-daemon 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add kmpuiviews/build.gradle.kts kmpuiviews/koogintegration/build.gradle.kts mangaworld/shared/build.gradle.kts novelworld/shared/build.gradle.kts
git commit -m "build: migrate KMP UI modules to platform version catalogs"
```

---

### Task 10: Update Android shared modules

**Files:**

- Modify: `UIViews/build.gradle.kts`
- Modify: `sharedutils/build.gradle.kts`
- Modify: `imageloader/build.gradle.kts`
- Modify: `source_utilities/build.gradle.kts`
- Modify: `Models/build.gradle.kts`

- [ ] **Step 1: Update `UIViews/build.gradle.kts`**

Replace `libs.*` dependency references:

```kotlin
dependencies {
    implementation(androidLibs.material)
    implementation(androidx.legacy.legacySupportV4)
    implementation(androidx.preference.preferenceKtx)
    implementation(androidx.window.window)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(platform(androidLibs.firebasePlatform))
    implementation(androidLibs.firebaseAuth)
    implementation(androidLibs.playServices)
    implementation(androidLibs.bundles.firebaseCrashLibs)

    api(platform(commonLibs.koin.bom))
    api(androidLibs.bundles.koinLibs)

    implementation(projects.kmpmodels)
    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)
    api(projects.datastore)
    api(projects.kmpuiviews)
    api(projects.sharedutils.kmpextensionloader)

    implementation(androidx.constraintlayout.constraintlayout)
    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.coroutinesAndroid)
    implementation(androidx.fragment.fragmentKtx)
    implementation(androidx.lifecycle.lifecycleExtensions)
    implementation(androidx.lifecycle.lifecycleRuntimeKtx)
    implementation(androidx.lifecycle.lifecycleLivedataKtx)
    implementation(androidx.lifecycle.lifecycleViewmodelKtx)

    implementation(androidLibs.iconicsCore)
    implementation(Deps.materialTypeface)
    implementation(Deps.fontawesomeTypeface)

    implementation(androidLibs.gson)

    implementation(androidLibs.recyclerview)
    implementation(commonLibs.bundles.roomLibs)

    implementation(androidLibs.glide)
    ksp(androidLibs.glideCompiler)
    implementation(androidLibs.glideRecyclerview) { isTransitive = false }

    api(androidLibs.workRuntime)

    implementation(commonLibs.kotlinxSerialization)

    implementation(Deps.jakepurple13Libs)

    val composeBom = platform(androidLibs.composePlatform)
    implementation(composeBom)
    implementation(androidLibs.bundles.compose)
    implementation(androidLibs.adaptive)
    implementation(commonLibs.bundles.datastoreLibs)

    implementation(commonLibs.bundles.ktorLibs)

    implementation(androidx.activity.activityKtx)

    api(commonLibs.bundles.kamel)

    api(commonLibs.haze)
    api(commonLibs.haze.blur)
    api(commonLibs.haze.materials)

    implementation(androidLibs.composeCollapsable)

    implementation(androidLibs.materialAdaptive)
    implementation(androidLibs.adaptive.layout.android)
    implementation(androidLibs.adaptive.navigation.android)

    implementation(androidLibs.glideCompose)

    implementation(commonLibs.material.kolor)

    debugImplementation(androidLibs.workinspector)

    implementation(commonLibs.kotlinx.datetime)

    implementation(androidLibs.androidx.core.performance)

    implementation(commonLibs.filekit.core)
    implementation(commonLibs.filekit.dialogs.compose)

    api(commonLibs.androidx.navigation3.runtime)
    api(commonLibs.androidx.navigation3.ui)
    api(commonLibs.androidx.material3.navigation3)
    api(commonLibs.androidx.lifecycle.viewmodel.navigation3)
    implementation(androidLibs.androidx.activity.ktx)
    implementation(androidLibs.composeActivity)
    implementation(androidLibs.androidx.activity)
}
```

- [ ] **Step 2: Update `sharedutils/build.gradle.kts`**

```kotlin
dependencies {
    implementation(androidLibs.material)
    testImplementation(TestDeps.junit)
    testImplementation("com.jakewharton.picnic:picnic:0.7.0")
    testImplementation("com.lordcodes.turtle:turtle:0.10.0")
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    noCloudFirebaseImplementation(androidLibs.mlkitTranslate)
    noCloudFirebaseImplementation(androidLibs.mlkitLanguage)
    noCloudFirebaseImplementation(androidLibs.playServices)
    noCloudFirebaseImplementation(androidLibs.coroutinesPlayServices)

    fullImplementation(androidLibs.mlkitTranslate)
    fullImplementation(androidLibs.mlkitLanguage)
    fullImplementation(platform(androidLibs.firebasePlatform))
    fullImplementation(androidLibs.firebaseDatabase)
    fullImplementation(androidLibs.firebaseFirestore)
    fullImplementation(androidLibs.firebaseAuth)
    fullImplementation(androidLibs.firebaseUiAuth)
    fullImplementation(androidLibs.playServices)
    fullImplementation(androidLibs.coroutinesPlayServices)

    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.coroutinesAndroid)

    implementation(commonLibs.bundles.ktorLibs)

    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(platform(commonLibs.koin.bom))
    implementation(androidLibs.bundles.koinLibs)
    implementation(Deps.jakepurple13Libs)
    implementation(androidLibs.uiUtil)
}
```

- [ ] **Step 3: Update `imageloader/build.gradle.kts`**

```kotlin
otakuDependencies {
    commonDependencies {
        implementation(compose.dependencies.runtime)
        api(commonLibs.kamel.image)
    }

    androidDependencies {
        api(commonLibs.ktorAndroid)
    }
}

compose {
    kotlinCompilerPlugin = "org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:${libs.versions.kotlin.get()}"
}
```

- [ ] **Step 4: Update `source_utilities/build.gradle.kts`**

```kotlin
dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(androidLibs.bundles.okHttpLibs)
    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.jsoup)
    implementation(androidLibs.gson)
    implementation(androidLibs.kotson)
    implementation(androidLibs.karnKhttp)
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)

    implementation(androidLibs.androidxWebkit)

    implementation(projects.models)

    implementation(platform(commonLibs.koin.bom))
    implementation(androidLibs.bundles.koinLibs)

    implementation(commonLibs.bundles.ktorLibs)
    implementation(commonLibs.kotlinxSerialization)
}
```

- [ ] **Step 5: Update `Models/build.gradle.kts`**

```kotlin
dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.coroutinesAndroid)

    implementation(commonLibs.bundles.ktorLibs)
    implementation(commonLibs.kotlinxSerialization)
}
```

- [ ] **Step 6: Build check**

```bash
./gradlew :UIViews:compileNoFirebaseDebugKotlin :sharedutils:compileNoFirebaseDebugKotlin :source_utilities:compileDebugKotlin :Models:compileDebugKotlin --no-daemon 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add UIViews/build.gradle.kts sharedutils/build.gradle.kts imageloader/build.gradle.kts source_utilities/build.gradle.kts Models/build.gradle.kts
git commit -m "build: migrate Android shared modules to platform version catalogs"
```

---

### Task 11: Update Android app modules

**Files:**

- Modify: `mangaworld/build.gradle.kts`
- Modify: `animeworld/build.gradle.kts`
- Modify: `novelworld/build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `animeworldtv/build.gradle.kts`

- [ ] **Step 1: Update `mangaworld/build.gradle.kts`**

```kotlin
dependencies {
    implementation(androidLibs.material)
    implementation(androidx.constraintlayout.constraintlayout)
    implementation(androidx.swiperefreshlayout.swiperefreshlayout)
    implementation(androidx.recyclerview.recyclerview)
    implementation(androidx.preference.preferenceKtx)
    implementation(androidx.profileinstaller.profileinstaller)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    implementation(platform(androidLibs.firebasePlatform))
    implementation(androidLibs.bundles.firebaseCrashLibs)

    implementation(androidLibs.fileChooser)

    implementation(projects.uiViews)
    implementation(projects.kmpmodels)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)
    implementation(projects.sourceUtilities)
    implementation(projects.datastore.mangasettings)
    implementation(projects.mangaworld.shared)

    implementation(commonLibs.kamel.image)
    implementation(androidLibs.duktape)
    implementation(androidLibs.bundles.ziplineLibs)
    implementation(commonLibs.ktorAndroid)

    implementation(androidLibs.glide)
    ksp(androidLibs.glideCompiler)
    implementation(androidLibs.glideRecyclerview) { isTransitive = false }

    implementation(androidLibs.bundles.piasyLibs)
    implementation(androidLibs.subsamplingImageView)

    implementation(androidLibs.iconicsCore)
    implementation(Deps.materialTypeface)

    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.coroutinesAndroid)

    implementation(commonLibs.bundles.roomLibs)
    ksp(commonLibs.roomCompiler)

    implementation(Deps.jakepurple13Libs)

    implementation(platform(androidLibs.composePlatform))
    implementation(androidLibs.bundles.compose)
    implementation(androidLibs.coilGif)

    implementation(androidx.datastore.datastore)
    implementation(androidx.datastore.datastorePreferences)

    implementation(androidLibs.glideCompose)

    implementation(commonLibs.zoomableModifier)

    implementation(androidLibs.pagecurl)

    implementation(androidLibs.panpf.zoomimage.compose.glide)

    implementation(androidLibs.telephoto.zoomable.image.glide)

    implementation(commonLibs.sonner)

    implementation(commonLibs.lifecycle.viewmodel.compose)
}
```

- [ ] **Step 2: Update `animeworld/build.gradle.kts`**

```kotlin
dependencies {
    implementation(androidLibs.material)
    implementation(androidx.constraintlayout.constraintlayout)
    implementation(androidx.preference.preferenceKtx)
    implementation(platform(androidLibs.firebasePlatform))
    implementation(androidLibs.bundles.firebaseCrashLibs)
    implementation(androidx.recyclerview.recyclerview)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(androidLibs.fileChooser)
    implementation(androidLibs.slideToAct)

    implementation(androidx.mediarouter.mediarouter)

    implementation(androidLibs.gson)

    implementation(androidLibs.iconicsCore)
    implementation(Deps.materialTypeface)
    implementation(Deps.fontawesomeTypeface)

    implementation(projects.uiViews)
    implementation(projects.kmpmodels)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)

    implementation(commonLibs.bundles.roomLibs)
    ksp(commonLibs.roomCompiler)

    implementation(androidLibs.autoBindings)
    kapt(androidLibs.autoBindingsCompiler)

    implementation(androidLibs.castFramework)
    implementation(androidLibs.localCast)

    implementation(androidLibs.glide)
    ksp(androidLibs.glideCompiler)
    implementation(androidLibs.glideRecyclerview) { isTransitive = false }

    implementation(Deps.jakepurple13Libs)
    val composeBom = platform(androidLibs.composePlatform)
    implementation(composeBom)
    implementation(androidLibs.bundles.compose)
    implementation(androidLibs.coilVideo)
    implementation(androidLibs.composeViewBinding)
    implementation(commonLibs.bundles.datastoreLibs)

    implementation(androidLibs.bundles.media3)

    implementation(commonLibs.ktorAndroid)
}
```

- [ ] **Step 3: Update `novelworld/build.gradle.kts`**

```kotlin
dependencies {
    implementation(androidLibs.material)
    implementation(androidx.preference.preference)
    implementation(androidx.recyclerview.recyclerview)
    implementation(androidx.constraintlayout.constraintlayout)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(androidLibs.iconicsCore)
    implementation(Deps.materialTypeface)

    implementation(projects.uiViews)
    implementation(projects.kmpmodels)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)

    implementation(commonLibs.bundles.roomLibs)
    ksp(commonLibs.roomCompiler)

    implementation(platform(androidLibs.firebasePlatform))
    implementation(androidLibs.bundles.firebaseCrashLibs)
    val composeBom = platform(androidLibs.composePlatform)
    implementation(composeBom)
    implementation(androidLibs.bundles.compose)

    implementation(androidx.datastore.datastore)
    implementation(androidx.datastore.datastorePreferences)

    implementation(Deps.jakepurple13Libs)

    implementation(commonLibs.ktorAndroid)

    implementation(projects.novelworld.shared)
}
```

- [ ] **Step 4: Update `app/build.gradle.kts`**

Find and replace all `libs.*` dependency references:

```kotlin
dependencies {
    implementation(androidLibs.material)
    implementation(androidLibs.constraintlayout)
    implementation(androidLibs.androidxWebkit)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(androidLibs.recyclerview)

    implementation(commonLibs.bundles.roomLibs)
    ksp(commonLibs.roomCompiler)

    implementation(commonLibs.kotlinxSerialization)
    implementation(androidLibs.jsoup)
    implementation(androidLibs.preference) {
        isTransitive = true
    }
    implementation(platform(commonLibs.koin.bom))
    implementation(androidLibs.bundles.koinLibs)

    implementation(Deps.jakepurple13Libs)
    val composeBom = platform(androidLibs.composePlatform)
    implementation(composeBom)
    implementation(androidLibs.bundles.compose)

    implementation(androidLibs.androidxWindow)

    implementation(commonLibs.androidx.navigation3.runtime)
    implementation(commonLibs.androidx.navigation3.ui)
    implementation(commonLibs.androidx.material3.navigation3)
    implementation(commonLibs.androidx.lifecycle.viewmodel.navigation3)

    implementation(commonLibs.qrose)

    implementation(commonLibs.ktorCore)
    implementation(commonLibs.ktorAndroid)
    implementation(commonLibs.ktorAuth)
    implementation(commonLibs.ktorLogging)
    implementation(commonLibs.ktorSerialization)
    implementation(commonLibs.ktorJson)
    implementation(commonLibs.ktorContentNegotiation)

    implementation(commonLibs.bundles.datastoreLibs)
    implementation(androidLibs.biometric)
}
```

Also update the `configurations.all` block at the top of android block:

```kotlin
android {
    // ...
    configurations.all {
        resolutionStrategy {
            force(androidLibs.preference)
        }
    }
}
```

- [ ] **Step 5: Update `animeworldtv/build.gradle.kts`**

```kotlin
android {
    // ...
    configurations.all {
        resolutionStrategy.force(androidLibs.lifecycleViewModel)
    }
}

dependencies {
    implementation(androidLibs.bundles.leanbackLibs)
    implementation(androidLibs.glide)
    kapt(androidLibs.glideCompiler)
    implementation(androidLibs.androidxLegacySupport)
    implementation(androidLibs.material)
    implementation(androidLibs.constraintlayout)
    implementation(platform(androidLibs.firebasePlatform))
    implementation(androidLibs.bundles.firebaseCrashLibs)
    implementation(androidLibs.firebaseAuth)
    implementation(androidLibs.playServices)
    implementation(androidLibs.bundles.media3)
    implementation(androidLibs.exoplayerleanback)

    implementation(projects.models)
    implementation(projects.animeSources)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)

    implementation(Deps.jakepurple13Libs)
    implementation(platform(commonLibs.koin.bom))
    implementation(androidLibs.koin.android)
    implementation(commonLibs.bundles.roomLibs)
    implementation(androidLibs.gson)

    implementation(platform(androidLibs.composePlatform))
    implementation(androidLibs.bundles.composeTv)
    implementation(androidLibs.coilGif)
    implementation(androidLibs.tv.foundation)
    implementation(androidLibs.tv.material)
}
```

- [ ] **Step 6: Build check**

```bash
./gradlew :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug :novelworld:assembleNoFirebaseDebug --no-daemon 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add mangaworld/build.gradle.kts animeworld/build.gradle.kts novelworld/build.gradle.kts app/build.gradle.kts animeworldtv/build.gradle.kts
git commit -m "build: migrate Android app modules to platform version catalogs"
```

---

### Task 12: Update source modules

**Files:**

- Modify: `manga_sources/build.gradle.kts`
- Modify: `manga_sources/defaultmangasources/build.gradle.kts`
- Modify: `anime_sources/build.gradle.kts`
- Modify: `anime_sources/defaultanimesources/build.gradle.kts`
- Modify: `novel_sources/build.gradle.kts`
- Modify: `novel_sources/novelupdates/build.gradle.kts`
- Modify: `novel_sources/bestlightnovel/build.gradle.kts` (same pattern as novelupdates)

- [ ] **Step 1: Update `manga_sources/build.gradle.kts`**

```kotlin
dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    testImplementation(TestDeps.mockitoCore)
    androidTestImplementation(TestDeps.mockitoAndroid)

    implementation(androidLibs.bundles.okHttpLibs)
    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.jsoup)
    implementation(androidLibs.duktape)
    implementation(androidLibs.bundles.ziplineLibs)
    implementation(androidLibs.gson)
    implementation(androidLibs.kotson)
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)
    implementation(commonLibs.kotlinxSerialization)
    implementation(androidLibs.androidxWebkit)

    implementation(androidLibs.uiUtil)

    implementation(projects.models)
    api(projects.sourceUtilities)

    implementation("com.github.KotatsuApp:kotatsu-parsers:8709c3dd0c") {
        exclude("org.json", "json")
    }

    implementation(androidLibs.bundles.koinLibs)
    implementation(commonLibs.bundles.ktorLibs)
}
```

- [ ] **Step 2: Update `manga_sources/defaultmangasources/build.gradle.kts`**

```kotlin
dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    implementation(androidLibs.bundles.okHttpLibs)

    implementation(commonLibs.coroutinesCore)

    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)
    implementation(androidLibs.gson)

    implementation(androidLibs.jsoup)

    implementation(projects.models)
    implementation(projects.mangaSources)
    api(projects.sourceUtilities)
    implementation(commonLibs.bundles.ktorLibs)

    implementation(androidLibs.bundles.koinLibs)
}
```

- [ ] **Step 3: Update `anime_sources/build.gradle.kts`**

```kotlin
dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(androidLibs.bundles.okHttpLibs)
    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.jsoup)
    implementation(androidLibs.duktape)
    implementation(androidLibs.bundles.ziplineLibs)
    implementation(androidLibs.rhino)
    implementation(androidLibs.gson)
    implementation(androidLibs.kotson)
    implementation(androidLibs.karnKhttp)
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)

    implementation(androidLibs.uiUtil)

    implementation(androidLibs.retrofit)
    implementation(androidLibs.retrofitGson)

    implementation(projects.models)
    api(projects.sourceUtilities)

    implementation(platform(commonLibs.koin.bom))
    implementation(androidLibs.bundles.koinLibs)

    implementation(commonLibs.bundles.ktorLibs)
    implementation(commonLibs.kotlinxSerialization)
}
```

- [ ] **Step 4: Update `anime_sources/defaultanimesources/build.gradle.kts`**

```kotlin
dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    implementation(androidLibs.bundles.okHttpLibs)

    implementation(commonLibs.coroutinesCore)

    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)
    implementation(androidLibs.gson)

    implementation(androidLibs.jsoup)

    implementation(projects.models)
    implementation(projects.animeSources)
    api(projects.sourceUtilities)
    implementation(commonLibs.bundles.ktorLibs)

    implementation(androidLibs.bundles.koinLibs)
}
```

- [ ] **Step 5: Update `novel_sources/build.gradle.kts`**

```kotlin
dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    implementation(androidLibs.bundles.okHttpLibs)

    implementation(commonLibs.coroutinesCore)

    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)
    implementation(androidLibs.gson)

    implementation(androidLibs.jsoup)

    implementation(androidLibs.uiUtil)

    implementation(projects.models)
    api(projects.sourceUtilities)
    implementation(commonLibs.bundles.ktorLibs)

    implementation(androidLibs.bundles.koinLibs)
}
```

- [ ] **Step 6: Update `novel_sources/novelupdates/build.gradle.kts`** (same pattern as
  novel_sources)

```kotlin
dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    implementation(androidLibs.bundles.okHttpLibs)

    implementation(commonLibs.coroutinesCore)

    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)
    implementation(androidLibs.gson)

    implementation(androidLibs.jsoup)

    implementation(commonLibs.bundles.ktorLibs)

    implementation(androidLibs.bundles.koinLibs)
    implementation(projects.models)
}
```

- [ ] **Step 7: Update `novel_sources/bestlightnovel/build.gradle.kts`**

Apply the same pattern as novelupdates (same structure). Replace `libs.*` with `androidLibs.*` /
`commonLibs.*` per the same mapping as novelupdates.

- [ ] **Step 8: Build check**

```bash
./gradlew :manga_sources:compileDebugKotlin :anime_sources:compileDebugKotlin :novel_sources:compileDebugKotlin --no-daemon 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add manga_sources/ anime_sources/ novel_sources/
git commit -m "build: migrate source modules to platform version catalogs"
```

---

### Task 13: Update `mangaworld/desktop` and root `build.gradle.kts`

**Files:**

- Modify: `mangaworld/desktop/build.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `buildSrc/src/main/kotlin/plugins/AndroidPluginBase.kt`

- [ ] **Step 1: Update `mangaworld/desktop/build.gradle.kts`**

```kotlin
kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinStLib)
            implementation(projects.kmpuiviews)
            implementation(commonLibs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.uiUtil)
            implementation(compose.foundation)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(compose.components.resources)
            implementation(commonLibs.material.kolor)
            implementation(project.dependencies.platform(commonLibs.koin.bom))
            implementation(commonLibs.bundles.koinKmp)
            implementation(projects.favoritesdatabase)
            implementation(projects.datastore)
            implementation(projects.datastore.mangasettings)
            implementation(projects.kmpmodels)
            implementation(projects.mangaworld.shared)
            implementation(commonLibs.bundles.datastoreLibs)
            implementation(commonLibs.coroutinesCore)
            implementation(desktopLibs.kotlinx.coroutines.swing)
            api(commonLibs.androidx.navigation3.runtime)
            api(commonLibs.filekit.core)
            api(commonLibs.filekit.dialogs.compose)
            implementation(projects.kmpuiviews.koogintegration)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(desktopLibs.kotlinx.coroutines.swing)
        }
    }
}
```

- [ ] **Step 2: Update root `build.gradle.kts` buildscript block**

The `latestAboutLibsRelease` version is now in `libs` (kept there for classpath use). No change
needed to the classpath reference.

Update the `configureAndroidBasePlugin` function which references `libs.coreLibraryDesugaring`:

```kotlin
fun Project.configureAndroidBasePlugin() {
    composeFeatureFlags()
    extensions.findByType<com.android.build.gradle.BaseExtension>()?.apply {
        compileOptions {
            isCoreLibraryDesugaringEnabled = true
        }

        dependencies {
            val coreLibraryDesugaring by configurations
            coreLibraryDesugaring(androidLibs.coreLibraryDesugaring)
        }
    }
}
```

Note: `androidLibs` is accessible in the root `build.gradle.kts` since it's declared in
`settings.gradle.kts`.

- [ ] **Step 3: Update `buildSrc/src/main/kotlin/plugins/AndroidPluginBase.kt`**

Find usages of `libs.androidCore` and `libs.appCompat` and replace:

```kotlin
// BEFORE
implementation(libs.androidCore.get())
implementation(libs.appCompat.get())

// AFTER
implementation(androidLibs.androidCore.get())
implementation(libs.appCompat.get())  // NOTE: appCompat stays in libs IF it's declared there, otherwise androidLibs
```

Wait — `androidCore` and `appCompat` moved to `androidLibs`. Access from a convention plugin
requires using the `VersionCatalogBuilder` extension. In buildSrc plugins, you access catalogs via
`project.extensions.getByType<VersionCatalogsExtension>()`. However, Gradle makes convention plugin
catalog access simpler in newer versions.

The correct approach in buildSrc:

```kotlin
// In AndroidPluginBase.kt, get the androidLibs catalog:
val androidLibs = project.extensions
    .getByType(VersionCatalogsExtension::class.java)
    .named("androidLibs")

// Then use:
implementation(androidLibs.findLibrary("androidCore").get())
implementation(androidLibs.findLibrary("appCompat").get())
```

Or keep `androidCore` and `appCompat` available in `libs.versions.toml` as well (duplicate). The
simplest fix is to keep both in `libs` AND in `androidLibs`. Add them back to `libs.versions.toml`:

In `gradle/libs.versions.toml` `[libraries]` section, add:

```toml
androidCore = "androidx.core:core-ktx:1.19.0"
appCompat = "androidx.appcompat:appcompat:1.7.1"
```

This keeps the buildSrc plugins working without a catalog-lookup change. The duplication is
intentional (buildSrc has limited catalog access).

- [ ] **Step 4: Build check**

```bash
./gradlew :mangaworld:desktop:run --dry-run --no-daemon 2>&1 | grep -E "error:|BUILD"
```

- [ ] **Step 5: Commit**

```bash
git add mangaworld/desktop/build.gradle.kts build.gradle.kts buildSrc/ gradle/libs.versions.toml
git commit -m "build: migrate desktop module and root build to platform version catalogs"
```

---

### Task 14: Final verification build

**Files:** none

- [ ] **Step 1: Build all Android apps**

```bash
./gradlew assembleNoFirebaseDebug --no-daemon 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Build desktop**

```bash
./gradlew :mangaworld:desktop:jar --no-daemon 2>&1 | grep -E "error:|BUILD"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run tests**

```bash
./gradlew test --no-daemon 2>&1 | grep -E "FAILED|BUILD"
```

Expected: `BUILD SUCCESSFUL` with no `FAILED` test suites

- [ ] **Step 4: Final commit**

If any fixes were made during verification:

```bash
git add -A
git commit -m "build: fix remaining catalog reference issues after platform split"
```
