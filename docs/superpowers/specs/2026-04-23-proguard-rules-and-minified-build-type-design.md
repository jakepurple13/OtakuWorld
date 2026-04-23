# ProGuard Rules + releaseMinified Build Type

**Date:** 2026-04-23  
**Status:** Approved

---

## Goals

1. Add module-specific ProGuard rules to all 18 existing `proguard-rules.pro` files.
2. Wire library modules to propagate their rules to consuming app modules via `consumerProguardFiles`.
3. Add a new `releaseMinified` build type that runs R8 with full minification and resource shrinking, non-debuggable, alongside the existing `release` build type.

---

## Build Type: `releaseMinified`

### `ApplicationBuildTypes.kt`

Add new `ReleaseMinified` enum entry:

```kotlin
ReleaseMinified("releaseMinified") {
    override fun <T : BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit) {
        create(buildTypeName) {
            initWith(getByName(Release.buildTypeName))
            matchingFallbacks.add("release")
            if (this is ApplicationBuildType) {
                isDebuggable = false
                isMinifyEnabled = true
                isShrinkResources = true
            }
            block()
        }
    }
}
```

- `initWith(release)` — inherits proguard file refs, signing config, and other release settings.
- `matchingFallbacks.add("release")` — library modules only publish `release`/`debug` variants; this prevents variant resolution failures.
- `isShrinkResources` guarded by `ApplicationBuildType` cast — only valid for app modules, not libraries.

### `AndroidApplicationPlugin.kt` and `OtakuManagerPlugin.kt`

Add to `buildTypes` block:

```kotlin
ApplicationBuildTypes.ReleaseMinified.setup(this) {
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
    )
}
```

---

## Consumer ProGuard Wiring

### `AndroidLibraryPlugin.kt`

Add to `androidConfig`:

```kotlin
defaultConfig {
    consumerProguardFiles("proguard-rules.pro")
}
```

### `MultiplatformLibraryPlugin.kt`

Inside the `KotlinMultiplatformAndroidLibraryExtension` configure block, add:

```kotlin
defaultConfig {
    consumerProguardFiles("proguard-rules.pro")
}
```

This covers `favoritesdatabase` and `datastore/mangasettings` — the two KMP modules with existing `.pro` files.

---

## ProGuard Rules — Per Module

### App modules (shared base)
Applies to: `app`, `animeworld`, `animeworldtv`, `mangaworld`, `novelworld`

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# KotlinX Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep,includedescriptorclasses class com.programmersbox.**$$serializer { *; }
-keepclassmembers class com.programmersbox.** { *** Companion; }
-keepclasseswithmembers class com.programmersbox.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Firebase / Crashlytics
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.** { *; }
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

# Koin
-keep class org.koin.** { *; }
-keepclassmembers class * { @org.koin.core.annotation.* *; }

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembernames class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
```

### `UIViews/proguard-rules.pro`

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
```

### `Models/proguard-rules.pro`

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# KotlinX Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep,includedescriptorclasses class com.programmersbox.**$$serializer { *; }
-keepclassmembers class com.programmersbox.** { *** Companion; }
-keepclasseswithmembers class com.programmersbox.** {
    kotlinx.serialization.KSerializer serializer(...);
}
```

### `favoritesdatabase/proguard-rules.pro`

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
```

### `sharedutils/proguard-rules.pro`

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
```

### `source_utilities/proguard-rules.pro`

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembernames class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# JSoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
```

### `anime_sources/proguard-rules.pro`, `manga_sources/proguard-rules.pro`, `novel_sources/proguard-rules.pro`

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
```

### `datastore/mangasettings/proguard-rules.pro`

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }

# DataStore / Protobuf
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
-dontwarn com.google.protobuf.**
```

### Source app modules
Applies to: `defaultanimesources`, `defaultmangasources`, `novelupdates`, `bestlightnovel`

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }

# KotlinX Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembernames class io.ktor.** { *; }
-dontwarn io.ktor.**

# JSoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
```

---

## Files Changed

### buildSrc
- `buildSrc/src/main/kotlin/plugins/ApplicationBuildTypes.kt` — add `ReleaseMinified` enum entry
- `buildSrc/src/main/kotlin/plugins/AndroidApplicationPlugin.kt` — register `ReleaseMinified` build type
- `buildSrc/src/main/kotlin/plugins/OtakuManagerPlugin.kt` — register `ReleaseMinified` build type
- `buildSrc/src/main/kotlin/plugins/AndroidLibraryPlugin.kt` — add `consumerProguardFiles`
- `buildSrc/src/main/kotlin/plugins/MultiplatformLibraryPlugin.kt` — add `consumerProguardFiles`

### ProGuard files (18 total)
- `app/proguard-rules.pro`
- `animeworld/proguard-rules.pro`
- `animeworldtv/proguard-rules.pro`
- `mangaworld/proguard-rules.pro`
- `novelworld/proguard-rules.pro`
- `UIViews/proguard-rules.pro`
- `Models/proguard-rules.pro`
- `favoritesdatabase/proguard-rules.pro`
- `sharedutils/proguard-rules.pro`
- `source_utilities/proguard-rules.pro`
- `anime_sources/proguard-rules.pro`
- `manga_sources/proguard-rules.pro`
- `novel_sources/proguard-rules.pro`
- `anime_sources/defaultanimesources/proguard-rules.pro`
- `manga_sources/defaultmangasources/proguard-rules.pro`
- `novel_sources/novelupdates/proguard-rules.pro`
- `novel_sources/bestlightnovel/proguard-rules.pro`
- `datastore/mangasettings/proguard-rules.pro`

---

## Out of Scope

- Signing config for `releaseMinified` — user manages keystores separately.
- `AndroidSourcePlugin` source app modules have no `buildTypes` block; their `.pro` files get rules but ProGuard is not wired into their build (not changed here).
- KMP modules without existing `.pro` files (`kmpmodels`, `kmpuiviews`, `imageloader`, etc.) — no new files created; `consumerProguardFiles` wiring in the plugin will be a no-op until files are added later.
