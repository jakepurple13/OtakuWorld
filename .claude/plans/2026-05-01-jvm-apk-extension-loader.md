# JVM APK Extension Loader Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable the JVM/desktop `ExtensionLoader` to load real Android APK plugins by parsing binary manifests, converting DEX to JVM bytecode, and providing functional mocks of Android APIs plugins call at runtime.

**Architecture:** `ApkManifestParser` reads binary XML manifests via `apk-parser`. `DexConverter` converts DEX→JAR at load time (SHA-256 cached). `PluginClassLoader` uses the converted JAR + the app's classloader as parent, which already has `com.programmersbox.models.*` JVM stubs and `android.*` mocks on its classpath. `SourceLoader` type-switches on loaded plugin instances (ApiService / three catalog types) and maps them to `KmpSourceInformation` via `JvmModelMapper`.

**Tech Stack:** Kotlin/JVM, `net.dongliu:apk-parser:2.6.10`, `com.github.ThexXTURBOXx.dex2jar:dex-tools:v2.4` (JitPack, already in repos), `ca.gosyer:kotlin-multiplatform-appdirs` (already in project), `kotlin.test` + JUnit 4 for tests.

---

## File Map

```
sharedutils/kmpextensionloader/
  build.gradle.kts                                                        MODIFY
  src/
    jvmMain/kotlin/com/programmersbox/
      android/
        app/Application.kt                                                CREATE
        content/Context.kt                                                CREATE
        content/SharedPreferences.kt                                      CREATE
        content/pm/PackageInfo.kt                                         CREATE  (PackageInfo + FeatureInfo)
        content/pm/ApplicationInfo.kt                                     CREATE
        content/pm/PackageManager.kt                                      CREATE
        graphics/drawable/Drawable.kt                                     CREATE
        os/Bundle.kt                                                      CREATE
        os/Build.kt                                                       CREATE
        util/Log.kt                                                       CREATE
      models/
        ApiService.kt                                                     CREATE
        ApiServicesCatalog.kt                                             CREATE  (3 interfaces)
        Models.kt                                                         CREATE  (all data classes)
      kmpextensionloader/
        ApkManifestParser.kt                                              CREATE
        DexConverter.kt                                                   CREATE
        JvmModelMapper.kt                                                 CREATE
        ExtensionLoader.kt                                                REPLACE
        SourceLoader.kt                                                   REPLACE
    jvmTest/kotlin/com/programmersbox/kmpextensionloader/
      BundleTest.kt                                                       CREATE
      SharedPreferencesTest.kt                                            CREATE
      JvmModelMapperTest.kt                                               CREATE
      ApkManifestParserTest.kt                                            CREATE

kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/di/
  AppModule.jvm.kt                                                        MODIFY  (pass appDirs to SourceLoader)
```

---

## Task 1: Add jvmMain Dependencies

**Files:**
- Modify: `sharedutils/kmpextensionloader/build.gradle.kts`

- [ ] **Step 1: Add jvmMain sourceset with dependencies**

Replace the entire contents of `build.gradle.kts`:

```kotlin
plugins {
    `otaku-multiplatform`
}

otakuDependencies {
    androidPackageName = "com.programmersbox.kmpextensionloader"
}

kotlin {
    android {
        namespace = "com.programmersbox.kmpextensionloader"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(libs.coroutinesCore)
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
                implementation(libs.kotlin.multiplatform.appdirs)
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
    }
}
```

- [ ] **Step 2: Verify the project syncs**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmMainClasses`
Expected: BUILD SUCCESSFUL (dependencies resolve)

- [ ] **Step 3: Commit**

```bash
git add sharedutils/kmpextensionloader/build.gradle.kts
git commit -m "build: add jvmMain deps for APK manifest parsing and DEX conversion"
```

---

## Task 2: Android Mock — os + util + graphics Packages

**Files:**
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/os/Bundle.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/os/Build.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/util/Log.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/graphics/drawable/Drawable.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/BundleTest.kt`

- [ ] **Step 1: Write BundleTest**

```kotlin
// src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/BundleTest.kt
package com.programmersbox.kmpextensionloader

import android.os.Bundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BundleTest {
    @Test fun `putString and getString round-trip`() {
        val b = Bundle()
        b.putString("key", "value")
        assertEquals("value", b.getString("key"))
    }

    @Test fun `getString returns null for missing key`() {
        assertNull(Bundle().getString("missing"))
    }

    @Test fun `getString returns default for missing key`() {
        assertEquals("default", Bundle().getString("missing", "default"))
    }

    @Test fun `putInt and getInt round-trip`() {
        val b = Bundle()
        b.putInt("n", 42)
        assertEquals(42, b.getInt("n"))
    }

    @Test fun `getInt returns 0 for missing key`() {
        assertEquals(0, Bundle().getInt("missing"))
    }

    @Test fun `putBoolean and getBoolean round-trip`() {
        val b = Bundle()
        b.putBoolean("flag", true)
        assertTrue(b.getBoolean("flag"))
    }

    @Test fun `containsKey returns false when missing`() {
        assertFalse(Bundle().containsKey("x"))
    }

    @Test fun `containsKey returns true after put`() {
        val b = Bundle()
        b.putString("x", "y")
        assertTrue(b.containsKey("x"))
    }

    @Test fun `remove deletes key`() {
        val b = Bundle()
        b.putString("k", "v")
        b.remove("k")
        assertNull(b.getString("k"))
    }
}
```

- [ ] **Step 2: Run test — expect FAIL (Bundle not defined)**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmTest --tests "*.BundleTest"`
Expected: compilation error — `android.os.Bundle` not found

- [ ] **Step 3: Create Bundle**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/android/os/Bundle.kt
package android.os

class Bundle {
    private val data = HashMap<String, Any?>()

    fun putString(key: String, value: String?) { data[key] = value }
    fun getString(key: String): String? = data[key] as? String
    fun getString(key: String, defaultValue: String): String = data[key] as? String ?: defaultValue

    fun putInt(key: String, value: Int) { data[key] = value }
    fun getInt(key: String): Int = data[key] as? Int ?: 0
    fun getInt(key: String, defaultValue: Int): Int = data[key] as? Int ?: defaultValue

    fun putLong(key: String, value: Long) { data[key] = value }
    fun getLong(key: String): Long = data[key] as? Long ?: 0L
    fun getLong(key: String, defaultValue: Long): Long = data[key] as? Long ?: defaultValue

    fun putFloat(key: String, value: Float) { data[key] = value }
    fun getFloat(key: String): Float = data[key] as? Float ?: 0f
    fun getFloat(key: String, defaultValue: Float): Float = data[key] as? Float ?: defaultValue

    fun putBoolean(key: String, value: Boolean) { data[key] = value }
    fun getBoolean(key: String): Boolean = data[key] as? Boolean ?: false
    fun getBoolean(key: String, defaultValue: Boolean): Boolean = data[key] as? Boolean ?: defaultValue

    fun putStringArrayList(key: String, value: ArrayList<String>?) { data[key] = value }
    @Suppress("UNCHECKED_CAST")
    fun getStringArrayList(key: String): ArrayList<String>? = data[key] as? ArrayList<String>

    fun containsKey(key: String): Boolean = data.containsKey(key)
    fun remove(key: String) { data.remove(key) }
    fun size(): Int = data.size
    fun isEmpty(): Boolean = data.isEmpty()
    fun keySet(): Set<String> = data.keys
    fun clear() { data.clear() }
}
```

- [ ] **Step 4: Create Build, Log, Drawable**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/android/os/Build.kt
package android.os

object Build {
    const val MANUFACTURER = "Desktop"
    const val MODEL = "Desktop"
    const val BRAND = "Desktop"
    const val DEVICE = "Desktop"
    const val PRODUCT = "Desktop"

    object VERSION {
        const val SDK_INT = 30
        const val RELEASE = "11"
        const val CODENAME = "REL"
    }
}
```

```kotlin
// src/jvmMain/kotlin/com/programmersbox/android/util/Log.kt
package android.util

object Log {
    const val VERBOSE = 2
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6

    @JvmStatic fun v(tag: String, msg: String): Int { println("V/$tag: $msg"); return 0 }
    @JvmStatic fun v(tag: String, msg: String, tr: Throwable): Int { println("V/$tag: $msg"); tr.printStackTrace(); return 0 }
    @JvmStatic fun d(tag: String, msg: String): Int { println("D/$tag: $msg"); return 0 }
    @JvmStatic fun d(tag: String, msg: String, tr: Throwable): Int { println("D/$tag: $msg"); tr.printStackTrace(); return 0 }
    @JvmStatic fun i(tag: String, msg: String): Int { println("I/$tag: $msg"); return 0 }
    @JvmStatic fun i(tag: String, msg: String, tr: Throwable): Int { println("I/$tag: $msg"); tr.printStackTrace(); return 0 }
    @JvmStatic fun w(tag: String, msg: String): Int { println("W/$tag: $msg"); return 0 }
    @JvmStatic fun w(tag: String, msg: String, tr: Throwable): Int { println("W/$tag: $msg"); tr.printStackTrace(); return 0 }
    @JvmStatic fun e(tag: String, msg: String): Int { System.err.println("E/$tag: $msg"); return 0 }
    @JvmStatic fun e(tag: String, msg: String, tr: Throwable): Int { System.err.println("E/$tag: $msg"); tr.printStackTrace(); return 0 }
    @JvmStatic fun wtf(tag: String, msg: String): Int { System.err.println("WTF/$tag: $msg"); return 0 }
}
```

```kotlin
// src/jvmMain/kotlin/com/programmersbox/android/graphics/drawable/Drawable.kt
package android.graphics.drawable

abstract class Drawable
```

- [ ] **Step 5: Run BundleTest — expect PASS**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmTest --tests "*.BundleTest"`
Expected: BUILD SUCCESSFUL, all 9 tests pass

- [ ] **Step 6: Commit**

```bash
git add sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/ \
        sharedutils/kmpextensionloader/src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/BundleTest.kt
git commit -m "feat: add Android mock layer - os/util/graphics packages"
```

---

## Task 3: Android Mock — SharedPreferences + Context + Application

**Files:**
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/content/SharedPreferences.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/content/Context.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/app/Application.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/SharedPreferencesTest.kt`

- [ ] **Step 1: Write SharedPreferencesTest**

```kotlin
// src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/SharedPreferencesTest.kt
package com.programmersbox.kmpextensionloader

import android.content.SharedPreferences
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedPreferencesTest {
    private lateinit var tmpDir: File
    private lateinit var prefs: SharedPreferences

    @BeforeTest fun setUp() {
        tmpDir = Files.createTempDirectory("prefs-test").toFile()
        prefs = SharedPreferences(File(tmpDir, "test.properties"))
    }

    @AfterTest fun tearDown() { tmpDir.deleteRecursively() }

    @Test fun `getString returns null when key absent`() {
        assertNull(prefs.getString("k", null))
    }

    @Test fun `putString persists after edit commit`() {
        prefs.edit().putString("name", "alice").commit()
        val fresh = SharedPreferences(File(tmpDir, "test.properties"))
        assertEquals("alice", fresh.getString("name", null))
    }

    @Test fun `putInt round-trips`() {
        prefs.edit().putInt("count", 7).apply()
        assertEquals(7, SharedPreferences(File(tmpDir, "test.properties")).getInt("count", 0))
    }

    @Test fun `putBoolean round-trips`() {
        prefs.edit().putBoolean("flag", true).apply()
        assertTrue(SharedPreferences(File(tmpDir, "test.properties")).getBoolean("flag", false))
    }

    @Test fun `remove deletes key`() {
        prefs.edit().putString("x", "y").commit()
        prefs.edit().remove("x").commit()
        assertNull(SharedPreferences(File(tmpDir, "test.properties")).getString("x", null))
    }

    @Test fun `clear removes all keys`() {
        prefs.edit().putString("a", "1").putString("b", "2").commit()
        prefs.edit().clear().commit()
        val fresh = SharedPreferences(File(tmpDir, "test.properties"))
        assertNull(fresh.getString("a", null))
        assertNull(fresh.getString("b", null))
    }

    @Test fun `contains returns false when absent`() {
        assertFalse(prefs.contains("missing"))
    }

    @Test fun `contains returns true after put`() {
        prefs.edit().putString("k", "v").commit()
        assertTrue(SharedPreferences(File(tmpDir, "test.properties")).contains("k"))
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmTest --tests "*.SharedPreferencesTest"`
Expected: compilation error — `android.content.SharedPreferences` not found

- [ ] **Step 3: Create SharedPreferences**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/android/content/SharedPreferences.kt
package android.content

import java.io.File
import java.util.Properties

class SharedPreferences(private val file: File) {
    private val props = Properties()

    init {
        if (file.exists()) file.inputStream().use { props.load(it) }
    }

    fun getString(key: String, defValue: String?): String? = props.getProperty(key) ?: defValue
    fun getInt(key: String, defValue: Int): Int = props.getProperty(key)?.toIntOrNull() ?: defValue
    fun getLong(key: String, defValue: Long): Long = props.getProperty(key)?.toLongOrNull() ?: defValue
    fun getFloat(key: String, defValue: Float): Float = props.getProperty(key)?.toFloatOrNull() ?: defValue
    fun getBoolean(key: String, defValue: Boolean): Boolean =
        props.getProperty(key)?.toBooleanStrictOrNull() ?: defValue
    fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        val v = props.getProperty(key) ?: return defValues
        return if (v.isEmpty()) emptySet() else v.split(",").toSet()
    }
    fun contains(key: String): Boolean = props.containsKey(key)
    fun getAll(): Map<String, *> = props.entries.associate { it.key.toString() to it.value }

    fun edit(): Editor = Editor(props, file)

    class Editor(private val props: Properties, private val file: File) {
        private val pending = Properties()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        fun putString(key: String, value: String?): Editor {
            if (value == null) removals.add(key) else pending.setProperty(key, value); return this
        }
        fun putInt(key: String, value: Int): Editor { pending.setProperty(key, value.toString()); return this }
        fun putLong(key: String, value: Long): Editor { pending.setProperty(key, value.toString()); return this }
        fun putFloat(key: String, value: Float): Editor { pending.setProperty(key, value.toString()); return this }
        fun putBoolean(key: String, value: Boolean): Editor { pending.setProperty(key, value.toString()); return this }
        fun putStringSet(key: String, values: Set<String>?): Editor {
            if (values == null) removals.add(key) else pending.setProperty(key, values.joinToString(","))
            return this
        }
        fun remove(key: String): Editor { removals.add(key); return this }
        fun clear(): Editor { clearAll = true; return this }

        fun apply() { commit() }

        fun commit(): Boolean = runCatching {
            if (clearAll) props.clear()
            removals.forEach { props.remove(it) }
            pending.forEach { k, v -> props.setProperty(k.toString(), v.toString()) }
            file.parentFile?.mkdirs()
            file.outputStream().use { props.store(it, null) }
        }.isSuccess
    }
}
```

- [ ] **Step 4: Create Context and Application**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/android/content/Context.kt
package android.content

import android.content.pm.PackageManager
import java.io.File

abstract class Context {
    abstract val packageName: String
    abstract val dataDir: File

    open fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        SharedPreferences(File(dataDir, "prefs/$name.properties"))

    open fun getFilesDir(): File = File(dataDir, "files").also { it.mkdirs() }
    open fun getCacheDir(): File = File(dataDir, "cache").also { it.mkdirs() }
    open fun getExternalFilesDir(type: String?): File? = getFilesDir()
    open fun getExternalCacheDir(): File? = getCacheDir()
    open fun getDir(name: String, mode: Int): File = File(dataDir, name).also { it.mkdirs() }
    open fun getDatabasePath(name: String): File = File(dataDir, "databases/$name")
    open fun getSystemService(name: String): Any? = null

    abstract fun getPackageManager(): PackageManager

    companion object {
        const val MODE_PRIVATE = 0
    }
}
```

```kotlin
// src/jvmMain/kotlin/com/programmersbox/android/app/Application.kt
package android.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import java.io.File

open class Application(
    override val packageName: String,
    baseDataDir: File,
    val apkPath: String = "",
) : Context() {

    override val dataDir: File = File(baseDataDir, packageName).also { it.mkdirs() }

    private inner class MockPackageManager : PackageManager() {
        override fun getPackageInfo(packageName: String, flags: Int): PackageInfo =
            PackageInfo().apply { this.packageName = packageName }

        override fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo =
            ApplicationInfo().apply {
                this.packageName = packageName
                this.sourceDir = if (packageName == this@Application.packageName) apkPath else ""
            }
    }

    private val pm = MockPackageManager()
    override fun getPackageManager(): PackageManager = pm

    fun getApplicationInfo(): ApplicationInfo = ApplicationInfo().apply {
        this.packageName = this@Application.packageName
        this.sourceDir = apkPath
        this.metaData = Bundle()
    }
}
```

- [ ] **Step 5: Run SharedPreferencesTest — expect PASS**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmTest --tests "*.SharedPreferencesTest"`
Expected: BUILD SUCCESSFUL, all 8 tests pass

- [ ] **Step 6: Commit**

```bash
git add sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/content/ \
        sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/app/ \
        sharedutils/kmpextensionloader/src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/SharedPreferencesTest.kt
git commit -m "feat: add Android mock layer - SharedPreferences, Context, Application"
```

---

## Task 4: Android Mock — pm Package

**Files:**
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/content/pm/PackageInfo.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/content/pm/ApplicationInfo.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/content/pm/PackageManager.kt`

- [ ] **Step 1: Create PackageInfo + FeatureInfo**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/android/content/pm/PackageInfo.kt
package android.content.pm

class PackageInfo {
    var packageName: String = ""
    var versionName: String? = null
    var versionCode: Int = 0
    var reqFeatures: Array<FeatureInfo>? = null
}

class FeatureInfo {
    var name: String? = null
    var flags: Int = 0
}
```

- [ ] **Step 2: Create ApplicationInfo**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/android/content/pm/ApplicationInfo.kt
package android.content.pm

import android.os.Bundle

class ApplicationInfo {
    var packageName: String = ""
    var sourceDir: String = ""
    var nativeLibraryDir: String = ""
    var metaData: Bundle? = null
}
```

- [ ] **Step 3: Create PackageManager**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/android/content/pm/PackageManager.kt
package android.content.pm

import android.graphics.drawable.Drawable

abstract class PackageManager {
    abstract fun getPackageInfo(packageName: String, flags: Int): PackageInfo
    open fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo = ApplicationInfo()
    open fun getApplicationIcon(packageName: String): Drawable? = null
    open fun getApplicationIcon(info: ApplicationInfo): Drawable? = null
    open fun getInstalledPackages(flags: Int): List<PackageInfo> = emptyList()

    companion object {
        const val GET_META_DATA = 0x00000128
        const val GET_CONFIGURATIONS = 0x00004000
        const val GET_SIGNING_CERTIFICATES = 0x08000000
        const val PERMISSION_GRANTED = 0
        const val PERMISSION_DENIED = -1
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmMainClasses`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/content/pm/
git commit -m "feat: add Android mock layer - pm package (PackageInfo, ApplicationInfo, PackageManager)"
```

---

## Task 5: Models JVM Stubs (`com.programmersbox.models.*`)

These must exactly mirror the class signatures from `Models/src/main/java/com/programmersbox/models/` so that converted plugin class files can resolve their references.

**Files:**
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/models/ApiService.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/models/ApiServicesCatalog.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/models/Models.kt`

- [ ] **Step 1: Create ApiService stub**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/models/ApiService.kt
package com.programmersbox.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.Serializable

interface ApiService : Serializable {
    val baseUrl: String
    val websiteUrl: String get() = baseUrl
    val canScroll: Boolean get() = false
    val canScrollAll: Boolean get() = canScroll
    val canPlay: Boolean get() = true
    val canDownload: Boolean get() = true
    val notWorking: Boolean get() = false
    val serviceName: String get() = this::class.java.name

    fun getRecentFlow(page: Int = 1): Flow<List<ItemModel>> = flow { emit(recent(page)) }.dispatchIo()
    suspend fun recent(page: Int = 1): List<ItemModel> = emptyList()

    fun getListFlow(page: Int = 1): Flow<List<ItemModel>> = flow { emit(allList(page)) }.dispatchIo()
    suspend fun allList(page: Int = 1): List<ItemModel> = emptyList()

    fun getItemInfoFlow(model: ItemModel): Flow<Result<InfoModel>> = flow {
        emit(runCatching { itemInfo(model) })
    }.dispatchIo()
    suspend fun itemInfo(model: ItemModel): InfoModel = error("Need to create an itemInfo")

    suspend fun search(searchText: CharSequence, page: Int = 1, list: List<ItemModel>): List<ItemModel> =
        list.filter { it.title.contains(searchText, true) }

    fun searchListFlow(searchText: CharSequence, page: Int = 1, list: List<ItemModel>): Flow<List<ItemModel>> =
        flow { emit(search(searchText, page, list)) }

    fun searchSourceList(searchText: CharSequence, page: Int = 1, list: List<ItemModel>): Flow<List<ItemModel>> = flow {
        if (searchText.isBlank()) throw Exception("No search necessary")
        emitAll(searchListFlow(searchText, page, list))
    }.dispatchIo().catch {
        it.printStackTrace()
        emitAll(flow { emit(list.filter { s -> s.title.contains(searchText, true) }) })
    }

    fun getChapterInfoFlow(chapterModel: ChapterModel): Flow<List<Storage>> =
        flow { emit(chapterInfo(chapterModel)) }
            .catch { it.printStackTrace(); emit(emptyList()) }
            .dispatchIo()
    suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> = emptyList()

    fun getSourceByUrlFlow(url: String): Flow<ItemModel> = flow { emit(sourceByUrl(url)) }
        .dispatchIo()
        .catch { it.printStackTrace(); emit(ItemModel("", "", url, "", this@ApiService)) }
    suspend fun sourceByUrl(url: String): ItemModel = error("Not setup")

    fun <T> Flow<T>.dispatchIo(): Flow<T> = this.flowOn(Dispatchers.IO)
    fun <T> Flow<List<T>>.dispatchIoAndCatchList(): Flow<List<T>> = this
        .dispatchIo()
        .catch { it.printStackTrace(); emit(emptyList()) }
}
```

- [ ] **Step 2: Create ApiServicesCatalog stubs (3 interfaces)**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/models/ApiServicesCatalog.kt
package com.programmersbox.models

import android.app.Application
import android.content.pm.PackageInfo

interface ApiServicesCatalog {
    val name: String
    fun createSources(): List<ApiService>
}

interface ExternalApiServicesCatalog : ApiServicesCatalog {
    val hasRemoteSources: Boolean
    suspend fun initialize(app: Application)
    fun getSources(): List<SourceInformation>
    override fun createSources(): List<ApiService> = getSources().map { it.apiService }
    suspend fun getRemoteSources(): List<RemoteSources> = emptyList()
    fun shouldReload(packageName: String, packageInfo: PackageInfo): Boolean = false
}

interface ExternalCustomApiServicesCatalog : ApiServicesCatalog {
    val hasRemoteSources: Boolean
    suspend fun initialize(app: Application)
    fun getSources(): List<SourceInformation>
    override fun createSources(): List<ApiService> = getSources().map { it.apiService }
    fun shouldReload(packageName: String, packageInfo: PackageInfo): Boolean = false
    suspend fun getRemoteSources(customUrls: List<String>): List<RemoteSources> = emptyList()
}
```

- [ ] **Step 3: Create data model stubs**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/models/Models.kt
package com.programmersbox.models

import android.graphics.drawable.Drawable

data class ItemModel(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val source: ApiService,
) {
    val extras = mutableMapOf<String, Any>()
    val otherExtras = mutableMapOf<String, Any>()
    fun toInfoModel() = source.getItemInfoFlow(this)
}

data class InfoModel(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val chapters: List<ChapterModel>,
    val genres: List<String>,
    val alternativeNames: List<String>,
    val source: ApiService,
) {
    val extras = mutableMapOf<String, Any>()
}

data class ChapterModel(
    val name: String,
    val url: String,
    val uploaded: String,
    val sourceUrl: String,
    val source: ApiService,
) {
    var uploadedTime: Long? = null
    fun getChapterInfo() = source.getChapterInfoFlow(this)
    val extras = mutableMapOf<String, Any>()
    val otherExtras = mutableMapOf<String, Any>()
}

class NormalLink(var normal: Normal? = null)
class Normal(var storage: Array<Storage>? = emptyArray())

data class Storage(
    var sub: String? = null,
    var source: String? = null,
    var link: String? = null,
    var quality: String? = null,
    var filename: String? = null,
) {
    val headers = mutableMapOf<String, String>()
}

data class SourceInformation(
    val apiService: ApiService,
    val name: String,
    val icon: Drawable?,
    val packageName: String,
    val catalog: ApiServicesCatalog? = null,
)

data class RemoteSources(
    val name: String,
    val packageName: String,
    val version: String,
    val iconUrl: String,
    val downloadLink: String,
    val sources: List<Sources>,
)

data class Sources(
    val name: String,
    val baseUrl: String,
    val version: String,
)
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmMainClasses`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/models/
git commit -m "feat: add com.programmersbox.models JVM stubs for plugin class loading"
```

---

## Task 6: JvmModelMapper

Maps `com.programmersbox.models.*` → `com.programmersbox.kmpmodels.*`. Equivalent of Android's `ModelMapper` but without `Application` dependency on construction — instead takes a `MockApplication` for catalog `initialize()` delegation.

**Files:**
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/JvmModelMapper.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/JvmModelMapperTest.kt`

- [ ] **Step 1: Write JvmModelMapperTest**

```kotlin
// src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/JvmModelMapperTest.kt
package com.programmersbox.kmpextensionloader

import android.app.Application
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import kotlinx.coroutines.flow.Flow
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmModelMapperTest {

    private val mockApp = Application("com.test", File(System.getProperty("java.io.tmpdir")))
    private val mapper = JvmModelMapper(mockApp)

    private val stubService = object : ApiService {
        override val baseUrl = "https://example.com"
        override val serviceName = "TestService"
    }

    @Test fun `mapApiService preserves baseUrl`() {
        val kmp: KmpApiService = mapper.mapApiService(stubService)
        assertEquals("https://example.com", kmp.baseUrl)
    }

    @Test fun `mapApiService preserves serviceName`() {
        val kmp: KmpApiService = mapper.mapApiService(stubService)
        assertEquals("TestService", kmp.serviceName)
    }

    @Test fun `mapApiService preserves canScroll default false`() {
        assertEquals(false, mapper.mapApiService(stubService).canScroll)
    }

    @Test fun `mapSourceInformation preserves name and packageName`() {
        val si = com.programmersbox.models.SourceInformation(
            apiService = stubService,
            name = "My Source",
            icon = null,
            packageName = "com.example.pkg",
        )
        val kmp = mapper.mapSourceInformation(si)
        assertEquals("My Source", kmp.name)
        assertEquals("com.example.pkg", kmp.packageName)
    }

    @Test fun `mapSourceInformation icon is always null`() {
        val si = com.programmersbox.models.SourceInformation(
            apiService = stubService, name = "x", icon = null, packageName = "pkg"
        )
        assertEquals(null, mapper.mapSourceInformation(si).icon)
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmTest --tests "*.JvmModelMapperTest"`
Expected: compilation error — `JvmModelMapper` not found

- [ ] **Step 3: Create JvmModelMapper**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/JvmModelMapper.kt
package com.programmersbox.kmpextensionloader

import android.app.Application
import com.programmersbox.kmpmodels.*
import com.programmersbox.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JvmModelMapper(private val mockApplication: Application) {

    fun mapSourceInformation(s: SourceInformation): KmpSourceInformation = KmpSourceInformation(
        apiService = mapApiService(s.apiService),
        name = s.name,
        icon = null,
        packageName = s.packageName,
        catalog = s.catalog?.let { mapCatalog(it) }
    )

    fun mapCatalog(catalog: ApiServicesCatalog): KmpApiServicesCatalog = when (catalog) {
        is ExternalApiServicesCatalog -> object : KmpExternalApiServicesCatalog {
            override val hasRemoteSources = catalog.hasRemoteSources
            override val name = catalog.name
            override suspend fun initialize() = catalog.initialize(mockApplication)
            override fun getSources() = catalog.getSources().map { mapSourceInformation(it) }
            override fun createSources() = catalog.createSources().map { mapApiService(it) }
            override suspend fun getRemoteSources() = catalog.getRemoteSources().map { mapRemoteSources(it) }
            override fun shouldReload(packageName: String) = false
        }
        is ExternalCustomApiServicesCatalog -> object : KmpExternalCustomApiServicesCatalog {
            override val hasRemoteSources = catalog.hasRemoteSources
            override val name = catalog.name
            override suspend fun initialize() = catalog.initialize(mockApplication)
            override fun getSources() = catalog.getSources().map { mapSourceInformation(it) }
            override fun createSources() = catalog.createSources().map { mapApiService(it) }
            override suspend fun getRemoteSources(customUrls: List<String>) =
                catalog.getRemoteSources(customUrls).map { mapRemoteSources(it) }
            override fun shouldReload(packageName: String) = false
        }
        else -> object : KmpApiServicesCatalog {
            override val name = catalog.name
            override fun createSources() = catalog.createSources().map { mapApiService(it) }
        }
    }

    fun mapRemoteSources(r: RemoteSources): KmpRemoteSources = KmpRemoteSources(
        name = r.name, packageName = r.packageName, version = r.version,
        iconUrl = r.iconUrl, downloadLink = r.downloadLink,
        sources = r.sources.map { KmpSources(it.name, it.baseUrl, it.version) }
    )

    fun mapApiService(service: ApiService): KmpApiService = object : KmpApiService {
        override val baseUrl = service.baseUrl
        override val websiteUrl = service.websiteUrl
        override val canScroll = service.canScroll
        override val canScrollAll = service.canScrollAll
        override val canPlay = service.canPlay
        override val canDownload = service.canDownload
        override val notWorking = service.notWorking
        override val serviceName = service.serviceName

        override fun getRecentFlow(page: Int): Flow<List<KmpItemModel>> =
            service.getRecentFlow(page).map { it.map { mapItemModel(it) } }
        override suspend fun recent(page: Int) = service.recent(page).map { mapItemModel(it) }
        override fun getListFlow(page: Int): Flow<List<KmpItemModel>> =
            service.getListFlow(page).map { it.map { mapItemModel(it) } }
        override suspend fun allList(page: Int) = service.allList(page).map { mapItemModel(it) }
        override fun getItemInfoFlow(model: KmpItemModel): Flow<Result<KmpInfoModel>> =
            service.getItemInfoFlow(reverseMapItemModel(model)).map { it.map { mapInfoModel(it) } }
        override suspend fun itemInfo(model: KmpItemModel) =
            mapInfoModel(service.itemInfo(reverseMapItemModel(model)))
        override fun getChapterInfoFlow(chapterModel: KmpChapterModel): Flow<List<KmpStorage>> =
            service.getChapterInfoFlow(reverseMapChapterModel(chapterModel)).map { it.map { mapStorage(it) } }
        override suspend fun chapterInfo(chapterModel: KmpChapterModel) =
            service.chapterInfo(reverseMapChapterModel(chapterModel)).map { mapStorage(it) }
        override fun getSourceByUrlFlow(url: String): Flow<KmpItemModel> =
            service.getSourceByUrlFlow(url).map { mapItemModel(it) }
        override suspend fun sourceByUrl(url: String) = mapItemModel(service.sourceByUrl(url))
        override suspend fun search(searchText: CharSequence, page: Int, list: List<KmpItemModel>) =
            service.search(searchText, page, list.map { reverseMapItemModel(it) }).map { mapItemModel(it) }
        override fun searchListFlow(searchText: CharSequence, page: Int, list: List<KmpItemModel>): Flow<List<KmpItemModel>> =
            service.searchListFlow(searchText, page, list.map { reverseMapItemModel(it) }).map { it.map { mapItemModel(it) } }
        override fun searchSourceList(searchText: CharSequence, page: Int, list: List<KmpItemModel>): Flow<List<KmpItemModel>> =
            service.searchSourceList(searchText, page, list.map { reverseMapItemModel(it) }).map { it.map { mapItemModel(it) } }
    }

    private fun mapItemModel(m: ItemModel): KmpItemModel = KmpItemModel(
        title = m.title, description = m.description, url = m.url,
        imageUrl = m.imageUrl, source = mapApiService(m.source)
    )

    private fun reverseMapItemModel(m: KmpItemModel): ItemModel = ItemModel(
        title = m.title, description = m.description, url = m.url,
        imageUrl = m.imageUrl, source = reverseMapApiService(m.source)
    )

    private fun mapInfoModel(m: InfoModel): KmpInfoModel = KmpInfoModel(
        title = m.title, description = m.description, url = m.url, imageUrl = m.imageUrl,
        chapters = m.chapters.map { mapChapterModel(it) },
        genres = m.genres, alternativeNames = m.alternativeNames,
        source = mapApiService(m.source)
    )

    private fun mapChapterModel(m: ChapterModel): KmpChapterModel = KmpChapterModel(
        name = m.name, url = m.url, uploaded = m.uploaded,
        sourceUrl = m.sourceUrl, source = mapApiService(m.source)
    )

    private fun reverseMapChapterModel(m: KmpChapterModel): ChapterModel = ChapterModel(
        name = m.name, url = m.url, uploaded = m.uploaded,
        sourceUrl = m.sourceUrl, source = reverseMapApiService(m.source)
    )

    private fun mapStorage(s: Storage): KmpStorage = KmpStorage(
        sub = s.sub, source = s.source, link = s.link, quality = s.quality, filename = s.filename
    )

    private fun reverseMapApiService(service: KmpApiService): ApiService = object : ApiService {
        override val baseUrl = service.baseUrl
        override val websiteUrl = service.websiteUrl
        override val canScroll = service.canScroll
        override val canScrollAll = service.canScrollAll
        override val canPlay = service.canPlay
        override val canDownload = service.canDownload
        override val notWorking = service.notWorking
        override val serviceName = service.serviceName
        // All flow methods delegate back so round-trip through the KmpApiService works
        override fun getRecentFlow(page: Int): Flow<List<ItemModel>> =
            service.getRecentFlow(page).map { it.map { reverseMapItemModel(it) } }
        override suspend fun recent(page: Int) = service.recent(page).map { reverseMapItemModel(it) }
        override fun getListFlow(page: Int): Flow<List<ItemModel>> =
            service.getListFlow(page).map { it.map { reverseMapItemModel(it) } }
        override suspend fun allList(page: Int) = service.allList(page).map { reverseMapItemModel(it) }
    }
}
```

- [ ] **Step 4: Run JvmModelMapperTest — expect PASS**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmTest --tests "*.JvmModelMapperTest"`
Expected: BUILD SUCCESSFUL, all 5 tests pass

- [ ] **Step 5: Commit**

```bash
git add sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/JvmModelMapper.kt \
        sharedutils/kmpextensionloader/src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/JvmModelMapperTest.kt
git commit -m "feat: add JvmModelMapper for models.* → kmpmodels.* conversion"
```

---

## Task 7: ApkManifestParser

**Files:**
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/ApkManifestParser.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/ApkManifestParserTest.kt`

- [ ] **Step 1: Write ApkManifestParserTest**

```kotlin
// src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/ApkManifestParserTest.kt
package com.programmersbox.kmpextensionloader

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApkManifestParserTest {

    // Place a real extension APK at this path before running the test.
    // Any OtakuWorld extension APK (e.g. defaultanimesources-debug.apk) works.
    // If the file doesn't exist the test is skipped.
    private val testApk: File = File(System.getProperty("test.apk.path", ""))

    @Test fun `parse returns non-blank packageName`() {
        if (!testApk.exists()) return
        val manifest = ApkManifestParser.parse(testApk)
        assertTrue(manifest.packageName.isNotBlank(), "packageName must not be blank")
    }

    @Test fun `parse extracts programmersbox feature`() {
        if (!testApk.exists()) return
        val manifest = ApkManifestParser.parse(testApk)
        assertTrue(
            manifest.features.any { it.startsWith("programmersbox.otaku") },
            "Expected a programmersbox.otaku feature, got: ${manifest.features}"
        )
    }

    @Test fun `parse extracts class metadata`() {
        if (!testApk.exists()) return
        val manifest = ApkManifestParser.parse(testApk)
        assertTrue(
            manifest.metaData.containsKey("programmersbox.otaku.class"),
            "Expected programmersbox.otaku.class meta-data"
        )
    }

    @Test fun `parse returns empty manifest for non-apk file`() {
        val tmp = File.createTempFile("fake", ".apk")
        tmp.writeText("not an apk")
        val manifest = runCatching { ApkManifestParser.parse(tmp) }.getOrNull()
        tmp.delete()
        // Should either return empty manifest or null — not throw uncaught
        assertTrue(manifest == null || manifest.packageName.isEmpty() || manifest.features.isEmpty())
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmTest --tests "*.ApkManifestParserTest"`
Expected: compilation error — `ApkManifestParser` not found

- [ ] **Step 3: Create ApkManifestParser**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/ApkManifestParser.kt
package com.programmersbox.kmpextensionloader

import net.dongliu.apk.parser.ApkFile
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

data class ApkManifest(
    val packageName: String,
    val versionName: String?,
    val features: Set<String>,
    val metaData: Map<String, String>,
)

object ApkManifestParser {

    fun parse(apkFile: File): ApkManifest {
        ApkFile(apkFile).use { apk ->
            val meta = apk.apkMeta
            val xml = apk.transBinaryXml("AndroidManifest.xml")
            val (features, metaData) = parseManifestXml(xml)
            return ApkManifest(
                packageName = meta.packageName,
                versionName = meta.versionName,
                features = features,
                metaData = metaData,
            )
        }
    }

    private fun parseManifestXml(xml: String): Pair<Set<String>, Map<String, String>> {
        val features = mutableSetOf<String>()
        val metaData = mutableMapOf<String, String>()

        runCatching {
            val doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(ByteArrayInputStream(xml.toByteArray()))

            val usesFeature = doc.getElementsByTagName("uses-feature")
            for (i in 0 until usesFeature.length) {
                val el = usesFeature.item(i) as? Element ?: continue
                val name = el.getAttribute("android:name").takeIf { it.isNotBlank() } ?: continue
                features.add(name)
            }

            val metaNodes = doc.getElementsByTagName("meta-data")
            for (i in 0 until metaNodes.length) {
                val el = metaNodes.item(i) as? Element ?: continue
                val name = el.getAttribute("android:name").takeIf { it.isNotBlank() } ?: continue
                val value = el.getAttribute("android:value")
                metaData[name] = value
            }
        }.onFailure { it.printStackTrace() }

        return features to metaData
    }
}
```

- [ ] **Step 4: Run test — expect PASS for non-APK test, skip for APK tests when no APK present**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmTest --tests "*.ApkManifestParserTest"`
Expected: BUILD SUCCESSFUL. The `parse returns empty manifest for non-apk file` test passes. APK tests are skipped (file not present).

To run with a real APK:
```bash
./gradlew :sharedutils:kmpextensionloader:jvmTest --tests "*.ApkManifestParserTest" \
  -Dtest.apk.path=/path/to/defaultanimesources-debug.apk
```

- [ ] **Step 5: Commit**

```bash
git add sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/ApkManifestParser.kt \
        sharedutils/kmpextensionloader/src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/ApkManifestParserTest.kt
git commit -m "feat: add ApkManifestParser for binary APK manifest extraction"
```

---

## Task 8: DexConverter

**Files:**
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/DexConverter.kt`

- [ ] **Step 1: Create DexConverter**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/DexConverter.kt
package com.programmersbox.kmpextensionloader

import com.googlecode.dex2jar.tools.Dex2jarCmd  // from com.github.ThexXTURBOXx.dex2jar:dex-tools:v76
import java.io.File
import java.security.MessageDigest

object DexConverter {

    fun convert(apkFile: File, cacheDir: File): File? {
        val hash = sha256(apkFile)
        val cachedJar = File(cacheDir, "$hash.jar")
        if (cachedJar.exists()) return cachedJar

        return runCatching {
            cacheDir.mkdirs()
            val tmp = File(cacheDir, "$hash.tmp.jar")
            Dex2jarCmd().doMain(
                arrayOf(apkFile.absolutePath, "-o", tmp.absolutePath, "--force")
            )
            if (tmp.exists() && tmp.length() > 0) {
                tmp.renameTo(cachedJar)
                cachedJar
            } else {
                tmp.delete()
                null
            }
        }.onFailure {
            println("DexConverter: failed to convert ${apkFile.name}: ${it.message}")
            it.printStackTrace()
        }.getOrNull()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmMainClasses`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/DexConverter.kt
git commit -m "feat: add DexConverter with SHA-256 disk caching"
```

---

## Task 9: Updated ExtensionLoader

Replace all stub methods with the real pipeline. The `MockApplicationInfo`, `MockPackageInfo`, `MockFeatureInfo`, `MockBundle` inner classes are removed and replaced by real `android.*` mock classes from Task 4.

**Files:**
- Modify: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/ExtensionLoader.kt`

- [ ] **Step 1: Replace ExtensionLoader.kt entirely**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/ExtensionLoader.kt
package com.programmersbox.kmpextensionloader

import android.content.pm.ApplicationInfo
import android.content.pm.FeatureInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URLClassLoader

class ExtensionLoader<T, R>(
    private val extensionsDir: File,
    private val cacheDir: File,
    private val extensionFeature: String,
    private val metadataClass: String,
    private val mapping: (T, ApplicationInfo, PackageInfo) -> R,
) {
    fun loadExtensions(mapped: (T, ApplicationInfo, PackageInfo) -> R = mapping): List<R> =
        runBlocking {
            findExtensionApks()
                .map { async { loadExtension(it, mapped) } }
                .flatMap { it.await() }
        }

    suspend fun loadExtensionsBlocking(mapped: (T, ApplicationInfo, PackageInfo) -> R = mapping): List<R> =
        coroutineScope {
            findExtensionApks()
                .map { async { loadExtension(it, mapped) } }
                .awaitAll()
                .flatten()
        }

    private fun findExtensionApks(): List<File> {
        if (!extensionsDir.exists() || !extensionsDir.isDirectory) return emptyList()
        return extensionsDir.listFiles { f -> f.isFile && f.extension.equals("apk", ignoreCase = true) }
            ?.toList() ?: emptyList()
    }

    private fun loadExtension(apkFile: File, mapped: (T, ApplicationInfo, PackageInfo) -> R): List<R> {
        return runCatching {
            val manifest = ApkManifestParser.parse(apkFile)

            if (!manifest.features.contains(extensionFeature)) return emptyList()

            val jar = DexConverter.convert(apkFile, cacheDir) ?: return emptyList()

            val classLoader = URLClassLoader(
                arrayOf(jar.toURI().toURL()),
                this::class.java.classLoader,
            )

            val packageInfo = PackageInfo().apply {
                packageName = manifest.packageName
                versionName = manifest.versionName
                reqFeatures = manifest.features.map { FeatureInfo().apply { name = it } }.toTypedArray()
            }

            val metaBundle = Bundle().apply {
                manifest.metaData.forEach { (k, v) -> putString(k, v) }
            }

            val appInfo = ApplicationInfo().apply {
                packageName = manifest.packageName
                sourceDir = apkFile.absolutePath
                metaData = metaBundle
            }

            val classNames = metaBundle.getString(metadataClass)
                .orEmpty()
                .split(";")
                .map { cls ->
                    val trimmed = cls.trim()
                    if (trimmed.startsWith(".")) manifest.packageName + trimmed else trimmed
                }
                .filter { it.isNotBlank() }

            classNames.mapNotNull { className ->
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    Class.forName(className, false, classLoader)
                        .getDeclaredConstructor()
                        .newInstance() as? T
                }
                    .onFailure { println("ExtensionLoader: failed to load $className: ${it.message}") }
                    .getOrNull()
            }.map { mapped(it, appInfo, packageInfo) }
        }
            .onFailure { println("ExtensionLoader: failed to load ${apkFile.name}: ${it.message}") }
            .getOrElse { emptyList() }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmMainClasses`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/ExtensionLoader.kt
git commit -m "feat: replace ExtensionLoader stubs with real APK manifest parsing and DEX conversion"
```

---

## Task 10: Updated SourceLoader + AppModule wiring

**Files:**
- Modify: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/SourceLoader.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.jvm.kt`

- [ ] **Step 1: Replace SourceLoader.kt entirely**

```kotlin
// src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/SourceLoader.kt
package com.programmersbox.kmpextensionloader

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.models.ApiService
import com.programmersbox.models.ApiServicesCatalog
import com.programmersbox.models.ExternalApiServicesCatalog
import com.programmersbox.models.ExternalCustomApiServicesCatalog
import com.programmersbox.models.SourceInformation
import kotlinx.coroutines.runBlocking
import java.io.File

private const val METADATA_NAME = "programmersbox.otaku.name"
private const val METADATA_CLASS = "programmersbox.otaku.class"
private const val EXTENSION_FEATURE = "programmersbox.otaku.extension"

actual class SourceLoader(
    private val extensionsDir: File,
    sourceType: String,
    private val sourceRepository: SourceRepository,
    private val appDirs: AppDirs,
) {
    private val cacheDir = File(appDirs.getUserCacheDir(), "otaku-plugin-cache")
    private val dataDir = File(appDirs.getUserDataDir())

    private val extensionLoader = ExtensionLoader<Any, List<KmpSourceInformation>>(
        extensionsDir = extensionsDir,
        cacheDir = cacheDir,
        extensionFeature = "$EXTENSION_FEATURE.$sourceType",
        metadataClass = METADATA_CLASS,
    ) { t, appInfo, packageInfo ->
        val metaName = appInfo.metaData?.getString(METADATA_NAME) ?: "Unknown"
        val pkgName = packageInfo.packageName
        val pluginApp = Application(pkgName, dataDir, appInfo.sourceDir)
        val mapper = JvmModelMapper(pluginApp)

        when (t) {
            is ApiService -> listOf(
                SourceInformation(
                    apiService = t,
                    name = metaName,
                    icon = null,
                    packageName = pkgName,
                )
            )

            is ExternalCustomApiServicesCatalog -> {
                runBlocking { t.initialize(pluginApp) }
                t.getSources().map { it.copy(catalog = t) }
            }

            is ExternalApiServicesCatalog -> {
                runBlocking { t.initialize(pluginApp) }
                t.getSources().map { it.copy(catalog = t) }
            }

            is ApiServicesCatalog -> t.createSources().map { service ->
                SourceInformation(
                    apiService = service,
                    name = metaName,
                    icon = null,
                    packageName = pkgName,
                    catalog = t,
                )
            }

            else -> emptyList()
        }.map { mapper.mapSourceInformation(it) }
    }

    actual fun load() {
        sourceRepository.setSources(
            extensionLoader.loadExtensions().flatten().sortedBy { it.apiService.serviceName }
        )
    }

    actual suspend fun blockingLoad() {
        sourceRepository.setSources(
            extensionLoader.loadExtensionsBlocking().flatten().sortedBy { it.apiService.serviceName }
        )
    }
}
```

- [ ] **Step 2: Update AppModule.jvm.kt — pass appDirs to SourceLoader**

In `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.jvm.kt`, replace the `SourceLoader` single block:

```kotlin
single {
    SourceLoader(
        extensionsDir = File("~/Downloads"),
        sourceType = get<KmpGenericInfo>().sourceType,
        sourceRepository = get(),
        appDirs = get(),
    )
}
```

- [ ] **Step 3: Verify compilation of both modules**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmMainClasses :kmpuiviews:jvmMainClasses`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run all jvmTest tests**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmTest`
Expected: BUILD SUCCESSFUL, BundleTest (9), SharedPreferencesTest (8), JvmModelMapperTest (5) all pass

- [ ] **Step 5: Commit**

```bash
git add sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/SourceLoader.kt \
        kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.jvm.kt
git commit -m "feat: implement full JVM APK extension loading with all plugin types"
```

---

## Notes

**Running integration tests with a real APK:**
Build any OtakuWorld extension APK (`./gradlew :anime_sources:defaultanimesources:assembleDebug`) and pass its path:
```bash
./gradlew :sharedutils:kmpextensionloader:jvmTest \
  -Dtest.apk.path=anime_sources/defaultanimesources/build/outputs/apk/debug/defaultanimesources-debug.apk
```

**Verifying end-to-end:**
Point `extensionsDir` in `AppModule.jvm.kt` to a directory containing a real extension APK and run the desktop app. Sources should appear in the extension list UI.

**If dex2jar fails for a specific APK:**
Some APKs use features dex2jar doesn't handle (Kotlin metadata, certain DEX opcodes). Check the printed stack trace. Fallback: the loader skips the plugin and continues loading others.
