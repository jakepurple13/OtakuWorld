# JVM APK Extension Loader Design

**Date:** 2026-05-01  
**Branch:** desktop-setup  
**Module:** `sharedutils/kmpextensionloader`

## Problem

The JVM `ExtensionLoader` has stub implementations for manifest parsing and feature detection, uses `URLClassLoader` which cannot execute Dalvik bytecode (`.dex`), and `SourceLoader` only handles `KmpApiService` — ignoring the three catalog types the Android side supports. Plugins ship as APKs only; plugin source is not owned by this project.

## Goals

- Load real APK extension plugins on JVM/desktop
- Parse binary APK manifests for feature filtering and metadata
- Convert DEX bytecode to JVM bytecode at load time
- Provide functional JVM-equivalent implementations of Android APIs plugins call at runtime
- Handle all four plugin types: `ApiService`, `ApiServicesCatalog`, `ExternalApiServicesCatalog`, `ExternalCustomApiServicesCatalog`

## Architecture

```
ApkFile
  │
  ├─► ApkManifestParser   → binary XML → ApkManifest(features, metaData, packageName)
  │
  ├─► DexConverter        → DEX → JAR (SHA-256 cached in appDirs cache dir)
  │
  └─► PluginClassLoader   → URLClassLoader(convertedJar, parent)
           │                  parent provides: models JVM stubs + android mock layer
           ▼
      loadedInstance (ApiService / *Catalog)
           │
           ▼
      SourceLoader        → type-switch → initialize(mockApplication) → map to KmpSourceInformation
```

All new components live in `kmpextensionloader/jvmMain`. No changes to `commonMain` or `androidMain`.

**New dependencies (jvmMain only):**
- `net.dongliu:apk-parser` — binary APK manifest parsing
- `com.github.ThexXTURBOXx:dex2jar-core` — DEX → JVM bytecode conversion

## Component: ApkManifestParser

Wraps `net.dongliu:apk-parser`. Returns:

```kotlin
data class ApkManifest(
    val packageName: String,
    val versionName: String?,
    val features: Set<String>,        // uses-feature android:name values
    val metaData: Map<String, String> // meta-data name → value
)
```

`apk-parser` provides `ApkFile.apkMeta` (packageName, versionName) and `ApkFile.transBinaryXml("AndroidManifest.xml")` which returns a plain XML string. Standard Java XML APIs parse the XML to extract `<uses-feature>` and `<meta-data>` elements.

Replaces both stubs in current `ExtensionLoader`:
- `hasFeature()` → `manifest.features.contains(extensionFeature)`
- `extractMainClassName()` → `manifest.metaData[METADATA_CLASS]`

## Component: DexConverter

Converts an APK's DEX files to a JVM-loadable JAR.

**Conversion:** Uses `dex2jar-core` programmatic API. Handles multi-dex (`classes.dex`, `classes2.dex`, etc.) by feeding the APK file directly — dex2jar handles extraction internally.

**Caching:**
```
<appDirs.cacheDir>/otaku-plugin-cache/<sha256-of-apk>.jar
```
- Cache hit → return cached JAR path directly (no conversion)
- Cache miss → convert, write to cache path, return path
- SHA-256 change = new APK version = new cache entry; old entries accumulate but are small
- Conversion failure → log exception, return `null` → caller skips this plugin

## Component: PluginClassLoader

`URLClassLoader` constructed with:
1. Converted plugin JAR (from DexConverter)
2. Parent = `ExtensionLoader::class.java.classLoader` (the app's classloader)

The parent already has `com.programmersbox.models.*` JVM stubs and `android.*` mock classes on its classpath because they're compiled into `kmpextensionloader/jvmMain`. No special parent wiring needed — standard classloader delegation handles it.

## Component: Models JVM Stubs (`com.programmersbox.models.*`)

Plugin class files reference `com.programmersbox.models.*` by exact package and class name. These stubs live in `kmpextensionloader/jvmMain` under the same package structure, compiled only for JVM.

**Classes needed:**
- `ApiService` — interface matching Android version; `initialize` signatures use mock `Application`
- `ApiServicesCatalog` — interface
- `ExternalApiServicesCatalog` — `initialize(app: Application)` uses mock `Application`; `shouldReload(packageName: String, packageInfo: PackageInfo)` uses mock `PackageInfo`
- `ExternalCustomApiServicesCatalog` — same pattern
- `ItemModel`, `InfoModel`, `ChapterModel`, `Storage` — pure data classes, no Android types
- `SourceInformation` — `icon: Drawable?` present but always `null`
- `RemoteSources`, `Sources` — data classes

## Component: Android Mock Layer (`android.*`)

Functional JVM implementations of Android APIs plugins call at runtime. All classes live in `kmpextensionloader/jvmMain` under the `android.*` package hierarchy.

| Class | JVM Implementation |
|---|---|
| `android.app.Application` | Extends `Context`; constructed once per `SourceLoader` |
| `android.content.Context` | Abstract base; provides `getSharedPreferences`, `getFilesDir`, `getCacheDir`, `getPackageManager` |
| `android.content.SharedPreferences` | `.properties` file per `(pluginPackageName, prefsName)` in `appDirs.dataDir/prefs/` |
| `android.content.SharedPreferences.Editor` | Buffered; flushes on `apply()` and `commit()` |
| `android.content.pm.PackageManager` | Returns `PackageInfo` from parsed APK manifest; `getApplicationIcon` returns `null` |
| `android.content.pm.PackageInfo` | Data holder: `packageName`, `versionName`, `versionCode`, `reqFeatures` |
| `android.content.pm.ApplicationInfo` | Data holder: `sourceDir`, `metaData` as `Bundle` |
| `android.os.Bundle` | `HashMap<String, Any?>` wrapper with typed getters |
| `android.os.Build` | Constants: `SDK_INT = 30`, `MANUFACTURER = "Desktop"` |
| `android.os.Build.VERSION` | `SDK_INT = 30`, `RELEASE = "11"` |
| `android.graphics.drawable.Drawable` | Empty stub class — never instantiated, present so class loading resolves |
| `android.util.Log` | Bridges to `println` (or slf4j if available) |

`SharedPreferences` key: files stored as `<dataDir>/prefs/<packageName>/<prefsName>.properties`.

## Component: Updated `ExtensionLoader` (jvmMain)

Replace stub methods with real pipeline:

```
findExtensionApks() → List<File>
  for each apk:
    manifest = ApkManifestParser.parse(apk)           // binary XML
    if extensionFeature !in manifest.features → skip
    jar = DexConverter.convert(apk) ?: continue       // cached DEX→JAR
    classLoader = PluginClassLoader(jar, parentLoader)
    classNames = manifest.metaData[METADATA_CLASS]
                   ?.split(";")
                   ?.map { cls ->
                       val trimmed = cls.trim()
                       if (trimmed.startsWith(".")) manifest.packageName + trimmed else trimmed
                   }
                   ?: emptyList()
    for each className:
      runCatching { Class.forName(className, false, classLoader).newInstance() as? T }
        .onFailure { log }
        .getOrNull()
  → flat list of T instances + their manifest data
```

`MockPackageInfo` and `MockApplicationInfo` are replaced by the real mock classes from the Android mock layer.

## Component: Updated `SourceLoader` (jvmMain)

Mirrors Android `SourceLoader` type-switch, using JVM stubs and mock `Application`:

```kotlin
when (t) {
    is ApiService -> listOf(
        SourceInformation(apiService = t, name = metaName, icon = null, packageName = pkgName)
    )

    is ExternalCustomApiServicesCatalog -> {
        runBlocking { t.initialize(mockApplication) }
        t.getSources().map { it.copy(catalog = t) }
    }

    is ExternalApiServicesCatalog -> {
        runBlocking { t.initialize(mockApplication) }
        t.getSources().map { it.copy(catalog = t) }
    }

    is ApiServicesCatalog -> t.createSources().map {
        SourceInformation(apiService = it, name = metaName, icon = null, packageName = pkgName, catalog = t)
    }

    else -> emptyList()
}
.map { JvmModelMapper.mapSourceInformation(it) }
```

`JvmModelMapper` maps `com.programmersbox.models.*` → `com.programmersbox.kmpmodels.*`. Equivalent of Android's `ModelMapper` but constructed without `Application` dependency. Lives in `kmpextensionloader/jvmMain`.

`mockApplication` is a `MockApplication` instance (concrete subclass of `android.app.Application`) created once in `SourceLoader`, using `AppDirs.dataDir` as its data root for `SharedPreferences` and file paths.

## Error Handling

- APK manifest parse failure → skip plugin, log warning
- DEX conversion failure → skip plugin, log warning  
- Class not found / instantiation failure → skip class, log warning (existing behavior)
- `initialize()` exception → skip plugin, log warning

All failures are per-plugin and non-fatal to the loader as a whole.

## Files Changed / Created

```
sharedutils/kmpextensionloader/
  build.gradle.kts                          (add jvmMain deps)
  src/jvmMain/kotlin/com/programmersbox/
    kmpextensionloader/
      ApkManifestParser.kt                  (new)
      DexConverter.kt                       (new)
      PluginClassLoader.kt                  (new)
      ExtensionLoader.kt                    (replace stubs)
      SourceLoader.kt                       (add all 4 types + jvmModelMapper)
    models/                                 (new — JVM stubs)
      ApiService.kt
      ApiServicesCatalog.kt               (contains ApiServicesCatalog, ExternalApiServicesCatalog, ExternalCustomApiServicesCatalog)
      SourceInformation.kt
      ItemModel.kt
      InfoModel.kt
      ChapterModel.kt
      Storage.kt
      RemoteSources.kt
    android/                                (new — mock layer)
      app/Application.kt
      content/Context.kt
      content/SharedPreferences.kt
      content/pm/PackageManager.kt
      content/pm/PackageInfo.kt
      content/pm/ApplicationInfo.kt
      os/Bundle.kt
      os/Build.kt
      graphics/drawable/Drawable.kt
      util/Log.kt
```
