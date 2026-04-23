# ProGuard Rules + releaseMinified Build Type — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add module-specific ProGuard rules to all 18 existing `proguard-rules.pro` files, wire library consumer rules, and add a non-debuggable `releaseMinified` build type with full R8 minification.

**Architecture:** Build type changes are centralized in `buildSrc` plugins — touching 5 plugin files covers all modules. Consumer ProGuard wiring in `AndroidLibraryPlugin` and `MultiplatformLibraryPlugin` ensures library rules propagate automatically to consuming app modules. ProGuard file content is module-specific based on each module's dependencies.

**Tech Stack:** Android Gradle Plugin, R8/ProGuard, Kotlin, buildSrc convention plugins

---

## Files Modified

### buildSrc
- `buildSrc/src/main/kotlin/plugins/ApplicationBuildTypes.kt` — add `ReleaseMinified` entry, fix `Beta` fallbacks
- `buildSrc/src/main/kotlin/plugins/AndroidApplicationPlugin.kt` — register `ReleaseMinified` build type
- `buildSrc/src/main/kotlin/plugins/OtakuManagerPlugin.kt` — register `ReleaseMinified` build type
- `buildSrc/src/main/kotlin/plugins/AndroidLibraryPlugin.kt` — add `consumerProguardFiles`
- `buildSrc/src/main/kotlin/plugins/MultiplatformLibraryPlugin.kt` — add `consumerProguardFiles`

### ProGuard files (18 total)
- `app/proguard-rules.pro` — full ruleset (Kotlin, Serialization, Room, Firebase, Koin, Ktor, Compose)
- `animeworld/proguard-rules.pro` — full ruleset (same as app)
- `animeworldtv/proguard-rules.pro` — full ruleset (same as app)
- `mangaworld/proguard-rules.pro` — full ruleset (same as app)
- `novelworld/proguard-rules.pro` — full ruleset (same as app)
- `UIViews/proguard-rules.pro` — Compose, Parcelable, Kotlin
- `Models/proguard-rules.pro` — Serialization, Kotlin, Enums, Parcelable
- `favoritesdatabase/proguard-rules.pro` — Room, Kotlin
- `sharedutils/proguard-rules.pro` — Kotlin, Enums
- `source_utilities/proguard-rules.pro` — Ktor, JSoup, Kotlin
- `anime_sources/proguard-rules.pro` — Kotlin
- `manga_sources/proguard-rules.pro` — Kotlin
- `novel_sources/proguard-rules.pro` — Kotlin
- `datastore/mangasettings/proguard-rules.pro` — DataStore/Protobuf, Kotlin
- `anime_sources/defaultanimesources/proguard-rules.pro` — Ktor, JSoup, Serialization, Kotlin
- `manga_sources/defaultmangasources/proguard-rules.pro` — Ktor, JSoup, Serialization, Kotlin
- `novel_sources/novelupdates/proguard-rules.pro` — Ktor, JSoup, Serialization, Kotlin
- `novel_sources/bestlightnovel/proguard-rules.pro` — Ktor, JSoup, Serialization, Kotlin

---

## Task 1: Add `ReleaseMinified` to `ApplicationBuildTypes.kt`

**Files:**
- Modify: `buildSrc/src/main/kotlin/plugins/ApplicationBuildTypes.kt`

- [ ] **Step 1: Replace file content**

Replace the entire file with:

```kotlin
package plugins

import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.dsl.BuildType
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.kotlin.dsl.extra

enum class ApplicationBuildTypes(
    val buildTypeName: String
) {

    Release("release") {
        override fun <T : BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit) {
            getByName(buildTypeName) {
                isMinifyEnabled = false
                isShrinkResources = false
                block()
            }
        }
    },
    Debug("debug") {
        override fun <T : BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit) {
            getByName(buildTypeName) {
                extra["enableCrashlytics"] = false
                block()
            }
        }
    },
    Beta("beta") {
        override fun <T : BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit) {
            create(buildTypeName) {
                initWith(getByName(Debug.buildTypeName))
                matchingFallbacks.addAll(listOf(Release.buildTypeName, Debug.buildTypeName))
                if (this is ApplicationBuildType) {
                    isDebuggable = false
                    isShrinkResources = false
                    isMinifyEnabled = false
                }
                block()
            }
        }
    },
    ReleaseMinified("releaseMinified") {
        override fun <T : BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit) {
            create(buildTypeName) {
                initWith(getByName(Release.buildTypeName))
                matchingFallbacks.add(Release.buildTypeName)
                if (this is ApplicationBuildType) {
                    isDebuggable = false
                    isMinifyEnabled = true
                    isShrinkResources = true
                }
                block()
            }
        }
    };

    protected abstract fun <T : BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit)
    fun <T : BuildType> setup(container: NamedDomainObjectContainer<T>, block: T.() -> Unit = {}) = container.setupBuildType(block)
}
```

> **Note on Beta change:** `Beta` previously used `values().filter { it != Beta }` for fallbacks which would have included `releaseMinified`. Changed to explicit list `listOf(Release.buildTypeName, Debug.buildTypeName)` to prevent that.

- [ ] **Step 2: Commit**

```bash
git add buildSrc/src/main/kotlin/plugins/ApplicationBuildTypes.kt
git commit -m "feat(build): add ReleaseMinified build type enum entry"
```

---

## Task 2: Register `ReleaseMinified` in app plugins

**Files:**
- Modify: `buildSrc/src/main/kotlin/plugins/AndroidApplicationPlugin.kt`
- Modify: `buildSrc/src/main/kotlin/plugins/OtakuManagerPlugin.kt`

- [ ] **Step 1: Update `AndroidApplicationPlugin.kt`**

Replace the `buildTypes` block (lines 33–43) in `AndroidApplicationPlugin.kt` with:

```kotlin
        buildTypes {
            ApplicationBuildTypes.Release.setup(this) {
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
            }
            ApplicationBuildTypes.Debug.setup(this)
            ApplicationBuildTypes.Beta.setup(this)
            ApplicationBuildTypes.ReleaseMinified.setup(this) {
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
            }
        }
```

- [ ] **Step 2: Update `OtakuManagerPlugin.kt`**

Replace the `buildTypes` block (lines 29–39) in `OtakuManagerPlugin.kt` with:

```kotlin
        buildTypes {
            ApplicationBuildTypes.Release.setup(this) {
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
            }
            ApplicationBuildTypes.Debug.setup(this)
            ApplicationBuildTypes.Beta.setup(this)
            ApplicationBuildTypes.ReleaseMinified.setup(this) {
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
            }
        }
```

- [ ] **Step 3: Verify Gradle config resolves**

```bash
./gradlew :app:tasks --all 2>/dev/null | grep -i releaseMinified
```

Expected output includes lines like:
```
assembleNoFirebaseReleaseMinified
assembleFullReleaseMinified
assembleNoCloudFirebaseReleaseMinified
```

- [ ] **Step 4: Commit**

```bash
git add buildSrc/src/main/kotlin/plugins/AndroidApplicationPlugin.kt \
        buildSrc/src/main/kotlin/plugins/OtakuManagerPlugin.kt
git commit -m "feat(build): register releaseMinified build type in app plugins"
```

---

## Task 3: Wire `consumerProguardFiles` in library plugins

**Files:**
- Modify: `buildSrc/src/main/kotlin/plugins/AndroidLibraryPlugin.kt`
- Modify: `buildSrc/src/main/kotlin/plugins/MultiplatformLibraryPlugin.kt`

- [ ] **Step 1: Update `AndroidLibraryPlugin.kt`**

Replace the entire file with:

```kotlin
package plugins

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project

class AndroidLibraryPlugin : AndroidPluginBase<LibraryExtension>(LibraryExtension::class) {

    override fun Project.projectSetup() {
        pluginManager.apply("com.android.library")
    }

    override fun LibraryExtension.androidConfig(project: Project) {
        defaultConfig {
            consumerProguardFiles("proguard-rules.pro")
        }
        lint {
            checkReleaseBuilds = false
        }
    }
}
```

- [ ] **Step 2: Update `MultiplatformLibraryPlugin.kt`**

Inside the `KotlinMultiplatformAndroidLibraryExtension` configure block (after `minSdk = AppInfo.minimumSdk`), add `defaultConfig { consumerProguardFiles("proguard-rules.pro") }`:

```kotlin
        (this as org.gradle.api.plugins.ExtensionAware)
            .extensions
            .configure(com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension::class.java) {
                namespace = dependencyHandling.androidPackageName
                compileSdk = AppInfo.compileVersion
                minSdk = AppInfo.minimumSdk

                defaultConfig {
                    consumerProguardFiles("proguard-rules.pro")
                }

                lint {
                    checkReleaseBuilds = false
                }
            }
```

> **If `defaultConfig` doesn't compile** on `KotlinMultiplatformAndroidLibraryExtension`, use this alternative directly in the configure block body instead:
> ```kotlin
> consumerProguardFiles("proguard-rules.pro")
> ```

- [ ] **Step 3: Verify Gradle config resolves**

```bash
./gradlew :favoritesdatabase:tasks 2>/dev/null | head -20
```

Expected: task list prints without error.

- [ ] **Step 4: Commit**

```bash
git add buildSrc/src/main/kotlin/plugins/AndroidLibraryPlugin.kt \
        buildSrc/src/main/kotlin/plugins/MultiplatformLibraryPlugin.kt
git commit -m "feat(build): wire consumerProguardFiles in library plugins"
```

---

## Task 4: Add ProGuard rules to app modules

**Files:**
- Modify: `app/proguard-rules.pro`
- Modify: `animeworld/proguard-rules.pro`
- Modify: `animeworldtv/proguard-rules.pro`
- Modify: `mangaworld/proguard-rules.pro`
- Modify: `novelworld/proguard-rules.pro`

All five files get the same full ruleset. Replace each file's content with:

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

# Stack traces — preserve line numbers in crash reports
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

- [ ] **Step 1: Write rules to `app/proguard-rules.pro`** (content above)

- [ ] **Step 2: Write rules to `animeworld/proguard-rules.pro`** (same content)

- [ ] **Step 3: Write rules to `animeworldtv/proguard-rules.pro`** (same content)

- [ ] **Step 4: Write rules to `mangaworld/proguard-rules.pro`** (same content)

- [ ] **Step 5: Write rules to `novelworld/proguard-rules.pro`** (same content)

- [ ] **Step 6: Commit**

```bash
git add app/proguard-rules.pro \
        animeworld/proguard-rules.pro \
        animeworldtv/proguard-rules.pro \
        mangaworld/proguard-rules.pro \
        novelworld/proguard-rules.pro
git commit -m "feat(proguard): add rules to app modules"
```

---

## Task 5: Add ProGuard rules to library modules

**Files:**
- Modify: `UIViews/proguard-rules.pro`
- Modify: `Models/proguard-rules.pro`
- Modify: `favoritesdatabase/proguard-rules.pro`
- Modify: `sharedutils/proguard-rules.pro`
- Modify: `source_utilities/proguard-rules.pro`
- Modify: `anime_sources/proguard-rules.pro`
- Modify: `manga_sources/proguard-rules.pro`
- Modify: `novel_sources/proguard-rules.pro`
- Modify: `datastore/mangasettings/proguard-rules.pro`

- [ ] **Step 1: Write rules to `UIViews/proguard-rules.pro`**

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

- [ ] **Step 2: Write rules to `Models/proguard-rules.pro`**

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

- [ ] **Step 3: Write rules to `favoritesdatabase/proguard-rules.pro`**

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
```

- [ ] **Step 4: Write rules to `sharedutils/proguard-rules.pro`**

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

- [ ] **Step 5: Write rules to `source_utilities/proguard-rules.pro`**

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

- [ ] **Step 6: Write rules to `anime_sources/proguard-rules.pro`**

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
```

- [ ] **Step 7: Write rules to `manga_sources/proguard-rules.pro`**

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
```

- [ ] **Step 8: Write rules to `novel_sources/proguard-rules.pro`**

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
```

- [ ] **Step 9: Write rules to `datastore/mangasettings/proguard-rules.pro`**

```proguard
# Kotlin
-keep class kotlin.Metadata { *; }

# DataStore / Protobuf
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
-dontwarn com.google.protobuf.**
```

- [ ] **Step 10: Commit**

```bash
git add UIViews/proguard-rules.pro \
        Models/proguard-rules.pro \
        favoritesdatabase/proguard-rules.pro \
        sharedutils/proguard-rules.pro \
        source_utilities/proguard-rules.pro \
        anime_sources/proguard-rules.pro \
        manga_sources/proguard-rules.pro \
        novel_sources/proguard-rules.pro \
        datastore/mangasettings/proguard-rules.pro
git commit -m "feat(proguard): add rules to library modules"
```

---

## Task 6: Add ProGuard rules to source app modules

**Files:**
- Modify: `anime_sources/defaultanimesources/proguard-rules.pro`
- Modify: `manga_sources/defaultmangasources/proguard-rules.pro`
- Modify: `novel_sources/novelupdates/proguard-rules.pro`
- Modify: `novel_sources/bestlightnovel/proguard-rules.pro`

All four files get the same ruleset. Replace each file's content with:

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

- [ ] **Step 1: Write rules to `anime_sources/defaultanimesources/proguard-rules.pro`** (content above)

- [ ] **Step 2: Write rules to `manga_sources/defaultmangasources/proguard-rules.pro`** (same content)

- [ ] **Step 3: Write rules to `novel_sources/novelupdates/proguard-rules.pro`** (same content)

- [ ] **Step 4: Write rules to `novel_sources/bestlightnovel/proguard-rules.pro`** (same content)

- [ ] **Step 5: Commit**

```bash
git add anime_sources/defaultanimesources/proguard-rules.pro \
        manga_sources/defaultmangasources/proguard-rules.pro \
        novel_sources/novelupdates/proguard-rules.pro \
        novel_sources/bestlightnovel/proguard-rules.pro
git commit -m "feat(proguard): add rules to source app modules"
```

---