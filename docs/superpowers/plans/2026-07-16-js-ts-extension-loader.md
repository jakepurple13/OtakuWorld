# JS/TS Extension Loader Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a JS/TS extension loading, execution, and auto-update system in two new Gradle modules (`kmpmodels/extensioninterfaces`, `sharedutils/jsextensionloader`) that coexists with the existing JAR/APK loader without modifying it.

**Architecture:** `kmpmodels/extensioninterfaces` is a pure-Kotlin commonMain-only contract module (`Extension` interface + lightweight models). `sharedutils/jsextensionloader` implements the loader using `app.cash.zipline:zipline`'s low-level `QuickJs` class (already multiplatform: android/jvm/iosArm64/iosSimulatorArm64) for sandboxed execution, a Kotlin-side regex/brace-based TS-stripping transpiler (no bundled JS library needed), Ktor for remote discovery and update checks, and a plain `DataStoreHandling` key for the update-mode setting.

**Tech Stack:** Kotlin Multiplatform, `app.cash.zipline:zipline` (QuickJs), Ktor Client, kotlinx.serialization, kotlinx.coroutines, AndroidX WorkManager (Android update scheduling), Koin (own module, not wired into any app).

## Global Constraints

- Do not modify `kmpmodels` core, `sharedutils/kmpextensionloader`, `SourceRepository`, `KmpApiService`, or any existing UI. The one approved exception is a single new key added to `datastore/src/commonMain/kotlin/com/programmersbox/datastore/DataStoreHandling.kt`.
- New modules use the `otaku-multiplatform` convention plugin, targeting android/jvm/iosArm64/iosSimulatorArm64 — the same matrix `sharedutils/kmpextensionloader` already builds for.
- Zipline version pinned to `1.27.0` (matches the existing `ziplineVersion` pin already used elsewhere in the repo for `zipline-loader`/`zipline-profiler`, avoiding dependency resolution conflicts).
- Extension functions (`getPopular`, `getLatest`, `search`, `getDetail`, `getContent`) must be **synchronous** — they return plain values, not Promises. Async/Promise support is out of scope for this plan (would require a JS microtask-pump loop inside QuickJs); the `.d.ts` and sample extension only declare synchronous signatures.
- The only bridge exposed into the QuickJs sandbox is `HostBridge` (an `httpGet` function). No `fetch`, no filesystem, no other globals are bound.
- Unit tests run in `jvmTest` only, mirroring `sharedutils/kmpextensionloader`'s existing convention (that module also only has a `jvmTest` source set, no `commonTest`/instrumented Android/iOS tests) — Android and iOS actuals are thin platform glue, compiled but not unit-tested directly.
- This plan ships infrastructure only. No app (`mangaworld`/`animeworld`/`novelworld`) `Application` class, Koin `startKoin` block, `WorkerModule.kt`, `BackgroundWorkHandlerImpl.kt`, or `NotificationEnums.kt` is modified — those are existing app-integration files and wiring an app to actually call `loadKoinModules(jsExtensionLoaderModule)` / `JsExtensionUpdateScheduler.schedule(...)` is a follow-up outside this plan's scope ("only the loading, execution, and update infrastructure is in scope").
- DRY: the update-check-and-act logic is written once (`JsExtensionUpdateRunner`) and reused by both the Android `CoroutineWorker` and the JVM/iOS coroutine ticker.

---

### Task 1: `kmpmodels:extensioninterfaces` — Extension contract & models

**Files:**
- Create: `kmpmodels/extensioninterfaces/build.gradle.kts`
- Create: `kmpmodels/extensioninterfaces/src/commonMain/kotlin/com/programmersbox/extensioninterfaces/Extension.kt`
- Create: `kmpmodels/extensioninterfaces/src/commonMain/kotlin/com/programmersbox/extensioninterfaces/ExtensionModels.kt`
- Create: `kmpmodels/extensioninterfaces/src/commonMain/kotlin/com/programmersbox/extensioninterfaces/ExtensionManifest.kt`
- Create: `kmpmodels/extensioninterfaces/src/commonMain/kotlin/com/programmersbox/extensioninterfaces/ExtensionUpdateInfo.kt`
- Test: `kmpmodels/extensioninterfaces/src/commonTest/kotlin/com/programmersbox/extensioninterfaces/ExtensionModelsTest.kt`
- Modify: `settings.gradle.kts`

**Interfaces:**
- Produces: `Extension` interface (`manifest`, `getPopular`, `getLatest`, `search`, `getDetail`, `getContent`), `ExtensionItem`, `ExtensionDetail`, `ExtensionChapter`, `ExtensionContent`, `ExtensionManifest(id, name, version, author, description, iconUrl, updateUrl)`, `ExtensionUpdateInfo(id, latestVersion, downloadUrl, changelog)` — all consumed by every later task.

- [ ] **Step 1: Register the module in `settings.gradle.kts`**

Add this line right after `include(":kmpmodels")` (currently line 96):

```kotlin
include(":kmpmodels:extensioninterfaces")
```

- [ ] **Step 2: Create the module's `build.gradle.kts`**

```kotlin
plugins {
    `otaku-multiplatform`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.extensioninterfaces"
}

kotlin {
    android {
        namespace = "com.programmersbox.extensioninterfaces"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(commonLibs.kotlinxSerialization)
            }
        }

        commonTest {
            dependencies {
                implementation(commonLibs.kotlin.test)
            }
        }
    }
}
```

- [ ] **Step 3: Write the failing test for the models**

Create `kmpmodels/extensioninterfaces/src/commonTest/kotlin/com/programmersbox/extensioninterfaces/ExtensionModelsTest.kt`:

```kotlin
package com.programmersbox.extensioninterfaces

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun extensionItem_roundTripsThroughJson() {
        val item = ExtensionItem(title = "Chapter 1", url = "https://example.com/1", imageUrl = null)
        val encoded = json.encodeToString(ExtensionItem.serializer(), item)
        val decoded = json.decodeFromString(ExtensionItem.serializer(), encoded)
        assertEquals(item, decoded)
    }

    @Test
    fun extensionDetail_roundTripsThroughJson() {
        val detail = ExtensionDetail(
            title = "My Manga",
            url = "https://example.com/manga",
            imageUrl = "https://example.com/manga.png",
            description = "A manga",
            genres = listOf("Action", "Adventure"),
            chapters = listOf(ExtensionChapter(name = "Ch. 1", url = "https://example.com/1", uploaded = "2026-01-01")),
        )
        val encoded = json.encodeToString(ExtensionDetail.serializer(), detail)
        val decoded = json.decodeFromString(ExtensionDetail.serializer(), encoded)
        assertEquals(detail, decoded)
    }

    @Test
    fun extensionUpdateInfo_roundTripsThroughJson() {
        val update = ExtensionUpdateInfo(
            id = "my-extension",
            latestVersion = "1.2.0",
            downloadUrl = "https://example.com/my-extension.js",
            changelog = "Fixed a bug",
        )
        val encoded = json.encodeToString(ExtensionUpdateInfo.serializer(), update)
        val decoded = json.decodeFromString(ExtensionUpdateInfo.serializer(), encoded)
        assertEquals(update, decoded)
    }
}
```

- [ ] **Step 4: Run the test to verify it fails (types don't exist yet)**

Run: `./gradlew :kmpmodels:extensioninterfaces:jvmTest`
Expected: FAIL — compilation error, `ExtensionItem`/`ExtensionDetail`/`ExtensionChapter`/`ExtensionUpdateInfo` unresolved.

- [ ] **Step 5: Create the models**

Create `kmpmodels/extensioninterfaces/src/commonMain/kotlin/com/programmersbox/extensioninterfaces/ExtensionModels.kt`:

```kotlin
package com.programmersbox.extensioninterfaces

import kotlinx.serialization.Serializable

@Serializable
data class ExtensionItem(
    val title: String,
    val url: String,
    val imageUrl: String?,
)

@Serializable
data class ExtensionChapter(
    val name: String,
    val url: String,
    val uploaded: String?,
)

@Serializable
data class ExtensionDetail(
    val title: String,
    val url: String,
    val imageUrl: String?,
    val description: String?,
    val genres: List<String>,
    val chapters: List<ExtensionChapter>,
)

@Serializable
data class ExtensionContent(
    val urls: List<String>,
    val headers: Map<String, String> = emptyMap(),
)
```

Create `kmpmodels/extensioninterfaces/src/commonMain/kotlin/com/programmersbox/extensioninterfaces/ExtensionManifest.kt`:

```kotlin
package com.programmersbox.extensioninterfaces

data class ExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String?,
    val description: String?,
    val iconUrl: String?,
    val updateUrl: String?,
)
```

Create `kmpmodels/extensioninterfaces/src/commonMain/kotlin/com/programmersbox/extensioninterfaces/ExtensionUpdateInfo.kt`:

```kotlin
package com.programmersbox.extensioninterfaces

import kotlinx.serialization.Serializable

@Serializable
data class ExtensionUpdateInfo(
    val id: String,
    val latestVersion: String,
    val downloadUrl: String,
    val changelog: String? = null,
)
```

Create `kmpmodels/extensioninterfaces/src/commonMain/kotlin/com/programmersbox/extensioninterfaces/Extension.kt`:

```kotlin
package com.programmersbox.extensioninterfaces

interface Extension {
    val manifest: ExtensionManifest

    suspend fun getPopular(page: Int): List<ExtensionItem>
    suspend fun getLatest(page: Int): List<ExtensionItem>
    suspend fun search(query: String, page: Int): List<ExtensionItem>
    suspend fun getDetail(url: String): ExtensionDetail
    suspend fun getContent(url: String): ExtensionContent
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :kmpmodels:extensioninterfaces:jvmTest`
Expected: PASS (3 tests)

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts kmpmodels/extensioninterfaces
git commit -m "feat: add kmpmodels:extensioninterfaces module with Extension contract"
```

---

### Task 2: `sharedutils:jsextensionloader` scaffold + `ExtensionManifestParser`

**Files:**
- Create: `sharedutils/jsextensionloader/build.gradle.kts`
- Create: `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/ExtensionManifestParser.kt`
- Test: `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/ExtensionManifestParserTest.kt`
- Modify: `settings.gradle.kts`
- Modify: `gradle/common.versions.toml`

**Interfaces:**
- Consumes: `ExtensionManifest` (Task 1, `com.programmersbox.extensioninterfaces`)
- Produces: `ExtensionManifestParser.parse(scriptText: String, companionManifestJson: String?, sourceId: String): ExtensionManifest` — consumed by `JSExtensionLoader` (Task 6).

- [ ] **Step 1: Register the module in `settings.gradle.kts`**

Add right after `include(":sharedutils:kmpextensionloader")` (currently line 97):

```kotlin
include(":sharedutils:jsextensionloader")
```

- [ ] **Step 2: Add the Zipline core dependency to the version catalog**

In `gradle/common.versions.toml`, add a new section right after the `# Ktor` section (after line 157, before `# Lifecycle KMP`):

```toml
# Zipline
ziplineCoreVersion = "1.27.0"
zipline = { module = "app.cash.zipline:zipline", version.ref = "ziplineCoreVersion" }
```

- [ ] **Step 3: Create the module's `build.gradle.kts`**

```kotlin
plugins {
    `otaku-multiplatform`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.jsextensionloader"
}

kotlin {
    android {
        namespace = "com.programmersbox.jsextensionloader"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(commonLibs.coroutinesCore)
                implementation(commonLibs.kotlinxSerialization)
                implementation(commonLibs.zipline)
                implementation(commonLibs.ktorCore)
                implementation(commonLibs.ktorContentNegotiation)
                implementation(commonLibs.ktorJson)
                implementation(projects.kmpmodels.extensioninterfaces)
                implementation(projects.datastore)
            }
        }

        androidMain {
            dependencies {
                implementation(commonLibs.ktorAndroid)
                implementation(androidLibs.workRuntimeKtx)
            }
        }

        jvmMain {
            dependencies {
                implementation(commonLibs.ktorOkHttp)
            }
        }

        iosMain {
            dependencies {
                implementation(iosLibs.ktorDarwin)
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation(commonLibs.coroutinesTest)
                implementation(commonLibs.ktorMock)
            }
        }
    }
}
```

- [ ] **Step 4: Write the failing test for header-comment and companion-JSON manifest parsing**

Create `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/ExtensionManifestParserTest.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExtensionManifestParserTest {

    @Test
    fun parsesHeaderCommentMetadata() {
        val script = """
            // name: My Extension
            // version: 1.0.0
            // author: Jane Doe
            // description: A test extension
            // iconUrl: https://example.com/icon.png
            // updateUrl: https://example.com/update.json
            function getPopular(page) { return []; }
        """.trimIndent()

        val manifest = ExtensionManifestParser.parse(script, companionManifestJson = null, sourceId = "my-extension")

        assertEquals("my-extension", manifest.id)
        assertEquals("My Extension", manifest.name)
        assertEquals("1.0.0", manifest.version)
        assertEquals("Jane Doe", manifest.author)
        assertEquals("A test extension", manifest.description)
        assertEquals("https://example.com/icon.png", manifest.iconUrl)
        assertEquals("https://example.com/update.json", manifest.updateUrl)
    }

    @Test
    fun headerCommentStopsAtFirstNonCommentLine() {
        val script = """
            // name: My Extension
            // version: 1.0.0
            function getPopular(page) { return []; }
            // author: this should be ignored, it's after code
        """.trimIndent()

        val manifest = ExtensionManifestParser.parse(script, companionManifestJson = null, sourceId = "my-extension")

        assertEquals("My Extension", manifest.name)
        assertNull(manifest.author)
    }

    @Test
    fun companionManifestJsonTakesPrecedenceOverHeaderComment() {
        val script = """
            // name: Header Name
            // version: 0.0.1
            function getPopular(page) { return []; }
        """.trimIndent()
        val companionJson = """
            {"name": "JSON Name", "version": "2.0.0", "sourceType": "manga"}
        """.trimIndent()

        val manifest = ExtensionManifestParser.parse(script, companionManifestJson = companionJson, sourceId = "my-extension")

        assertEquals("JSON Name", manifest.name)
        assertEquals("2.0.0", manifest.version)
        assertEquals("my-extension", manifest.id)
    }

    @Test
    fun companionManifestJsonExplicitIdOverridesSourceId() {
        val companionJson = """
            {"id": "explicit-id", "name": "JSON Name", "version": "2.0.0"}
        """.trimIndent()

        val manifest = ExtensionManifestParser.parse("", companionManifestJson = companionJson, sourceId = "my-extension")

        assertEquals("explicit-id", manifest.id)
    }
}
```

- [ ] **Step 5: Run the test to verify it fails**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: FAIL — `ExtensionManifestParser` unresolved.

- [ ] **Step 6: Implement `ExtensionManifestParser`**

Create `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/ExtensionManifestParser.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import com.programmersbox.extensioninterfaces.ExtensionManifest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val manifestJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

@Serializable
private data class ManifestJsonDto(
    val id: String? = null,
    val name: String,
    val version: String,
    val author: String? = null,
    val description: String? = null,
    val iconUrl: String? = null,
    val updateUrl: String? = null,
)

object ExtensionManifestParser {

    private val headerLine = Regex("""^//\s*(\w+)\s*:\s*(.+)$""")

    fun parse(scriptText: String, companionManifestJson: String?, sourceId: String): ExtensionManifest {
        if (companionManifestJson != null) {
            val dto = manifestJson.decodeFromString(ManifestJsonDto.serializer(), companionManifestJson)
            return ExtensionManifest(
                id = dto.id ?: sourceId,
                name = dto.name,
                version = dto.version,
                author = dto.author,
                description = dto.description,
                iconUrl = dto.iconUrl,
                updateUrl = dto.updateUrl,
            )
        }
        return parseHeaderComment(scriptText, sourceId)
    }

    private fun parseHeaderComment(scriptText: String, sourceId: String): ExtensionManifest {
        val fields = mutableMapOf<String, String>()
        for (line in scriptText.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val match = headerLine.find(trimmed) ?: break
            fields[match.groupValues[1].lowercase()] = match.groupValues[2].trim()
        }
        return ExtensionManifest(
            id = sourceId,
            name = fields.getValue("name"),
            version = fields.getValue("version"),
            author = fields["author"],
            description = fields["description"],
            iconUrl = fields["iconurl"],
            updateUrl = fields["updateurl"],
        )
    }
}
```

- [ ] **Step 7: Run the test to verify it passes**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: PASS (4 tests)

- [ ] **Step 8: Commit**

```bash
git add settings.gradle.kts gradle/common.versions.toml sharedutils/jsextensionloader
git commit -m "feat: scaffold sharedutils:jsextensionloader with manifest parser"
```

---

### Task 3: `TsTranspiler` — on-device TypeScript stripping

**Files:**
- Create: `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/TsTranspiler.kt`
- Test: `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/TsTranspilerTest.kt`

**Interfaces:**
- Produces: `TsTranspiler.transpile(source: String): String` — consumed by `JSExtensionLoader` (Task 6).

- [ ] **Step 1: Write the failing tests**

Create `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/TsTranspilerTest.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import kotlin.test.Test
import kotlin.test.assertEquals

class TsTranspilerTest {

    @Test
    fun stripsFunctionParameterAndReturnTypeAnnotations() {
        val ts = "function getPopular(page: number): Item[] {\n    return [];\n}"
        val expected = "function getPopular(page) {\n    return [];\n}"
        assertEquals(expected, TsTranspiler.transpile(ts))
    }

    @Test
    fun stripsMultipleParameterTypeAnnotations() {
        val ts = "function search(query: string, page: number): Item[] {\n    return [];\n}"
        val expected = "function search(query, page) {\n    return [];\n}"
        assertEquals(expected, TsTranspiler.transpile(ts))
    }

    @Test
    fun stripsInterfaceBlocksEntirely() {
        val ts = """
            interface Item {
                title: string;
                url: string;
            }
            function getPopular(page: number): Item[] {
                return [];
            }
        """.trimIndent()
        val transpiled = TsTranspiler.transpile(ts)
        assertEquals(false, transpiled.contains("interface"))
        assertEquals(true, transpiled.contains("function getPopular(page) {"))
    }

    @Test
    fun stripsTypeAliasLines() {
        val ts = """
            type Genre = string;
            function getPopular(page: number): Genre[] {
                return [];
            }
        """.trimIndent()
        val transpiled = TsTranspiler.transpile(ts)
        assertEquals(false, transpiled.contains("type Genre"))
    }

    @Test
    fun stripsExportKeyword() {
        val ts = "export function getPopular(page: number): Item[] {\n    return [];\n}"
        val expected = "function getPopular(page) {\n    return [];\n}"
        assertEquals(expected, TsTranspiler.transpile(ts))
    }

    @Test
    fun stripsAsCasts() {
        val ts = "function getPopular(page: number) {\n    return (raw as Item[]);\n}"
        val expected = "function getPopular(page) {\n    return (raw);\n}"
        assertEquals(expected, TsTranspiler.transpile(ts))
    }

    @Test
    fun leavesPlainJavaScriptUnchanged() {
        val js = "function getPopular(page) {\n    return [];\n}"
        assertEquals(js, TsTranspiler.transpile(js))
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: FAIL — `TsTranspiler` unresolved.

- [ ] **Step 3: Implement `TsTranspiler`**

Create `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/TsTranspiler.kt`:

```kotlin
package com.programmersbox.jsextensionloader

object TsTranspiler {

    private val interfaceStart = Regex("""interface\s+\w+[^{]*\{""")
    private val functionSignature = Regex(
        """(export\s+)?function\s+(\w+)\s*\(([^)]*)\)\s*(:\s*[^{]+)?\s*\{"""
    )
    private val asCast = Regex("""\s+as\s+[\w.]+(\[\])?""")
    private val leadingExport = Regex("""^export\s+(default\s+)?""")

    fun transpile(source: String): String {
        var result = stripInterfaceBlocks(source)
        result = stripTypeAliasLines(result)
        result = functionSignature.replace(result) { match ->
            val name = match.groupValues[2]
            val params = stripParamTypes(match.groupValues[3])
            "function $name($params) {"
        }
        result = asCast.replace(result, "")
        result = leadingExport.replace(result, "")
        return result
    }

    private fun stripParamTypes(params: String): String =
        params.split(",")
            .map { it.substringBefore(":").trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")

    private fun stripInterfaceBlocks(source: String): String {
        val builder = StringBuilder()
        var index = 0
        while (index < source.length) {
            val match = interfaceStart.find(source, index)
            if (match == null) {
                builder.append(source, index, source.length)
                break
            }
            builder.append(source, index, match.range.first)
            var depth = 1
            var cursor = match.range.last + 1
            while (cursor < source.length && depth > 0) {
                when (source[cursor]) {
                    '{' -> depth++
                    '}' -> depth--
                }
                cursor++
            }
            index = cursor
        }
        return builder.toString()
    }

    private fun stripTypeAliasLines(source: String): String =
        source.lineSequence()
            .filterNot { it.trimStart().startsWith("type ") || it.trimStart().startsWith("export type ") }
            .joinToString("\n")
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: PASS (7 new tests, 11 total)

- [ ] **Step 5: Commit**

```bash
git add sharedutils/jsextensionloader
git commit -m "feat: add on-device TypeScript-stripping transpiler"
```

---

### Task 4: `SemVerCompare` — version comparison

**Files:**
- Create: `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/SemVerCompare.kt`
- Test: `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/SemVerCompareTest.kt`

**Interfaces:**
- Produces: `SemVerCompare.isNewer(currentVersion: String, candidateVersion: String): Boolean` — consumed by `ExtensionUpdateChecker` (Task 9).

- [ ] **Step 1: Write the failing tests**

Create `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/SemVerCompareTest.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SemVerCompareTest {

    @Test
    fun newerPatchVersionIsNewer() {
        assertTrue(SemVerCompare.isNewer("1.0.0", "1.0.1"))
    }

    @Test
    fun newerMinorVersionIsNewer() {
        assertTrue(SemVerCompare.isNewer("1.0.9", "1.1.0"))
    }

    @Test
    fun newerMajorVersionIsNewer() {
        assertTrue(SemVerCompare.isNewer("1.9.9", "2.0.0"))
    }

    @Test
    fun sameVersionIsNotNewer() {
        assertFalse(SemVerCompare.isNewer("1.0.0", "1.0.0"))
    }

    @Test
    fun olderVersionIsNotNewer() {
        assertFalse(SemVerCompare.isNewer("1.2.0", "1.1.0"))
    }

    @Test
    fun malformedVersionReturnsFalse() {
        assertFalse(SemVerCompare.isNewer("not-a-version", "1.0.0"))
    }

    @Test
    fun differentPartCountsCompareCorrectly() {
        assertTrue(SemVerCompare.isNewer("1.0", "1.0.1"))
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: FAIL — `SemVerCompare` unresolved.

- [ ] **Step 3: Implement `SemVerCompare`**

Create `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/SemVerCompare.kt`:

```kotlin
package com.programmersbox.jsextensionloader

object SemVerCompare {

    fun isNewer(currentVersion: String, candidateVersion: String): Boolean = try {
        val current = currentVersion.split(".").map { it.trim().toInt() }
        val candidate = candidateVersion.split(".").map { it.trim().toInt() }
        val length = maxOf(current.size, candidate.size)
        var result = false
        for (i in 0 until length) {
            val c = current.getOrElse(i) { 0 }
            val n = candidate.getOrElse(i) { 0 }
            if (n > c) {
                result = true
                break
            }
            if (n < c) {
                result = false
                break
            }
        }
        result
    } catch (e: NumberFormatException) {
        false
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: PASS (7 new tests, 18 total)

- [ ] **Step 5: Commit**

```bash
git add sharedutils/jsextensionloader
git commit -m "feat: add SemVerCompare for extension update-version comparisons"
```

---

### Task 5: Sample extension fixtures, `.d.ts`, `HostBridge`, and `JsExtension`

**Files:**
- Create: `sharedutils/jsextensionloader/samples/sample-extension.js`
- Create: `sharedutils/jsextensionloader/samples/sample-extension.ts`
- Create: `sharedutils/jsextensionloader/samples/otaku-extension.d.ts`
- Create: `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/HostBridge.kt`
- Create: `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/ExtensionValidator.kt`
- Create: `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/JsExtension.kt`
- Create: `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/SampleExtensionFixture.kt`
- Test: `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/JsExtensionTest.kt`

**Interfaces:**
- Consumes: `ExtensionManifest`, `ExtensionItem`, `ExtensionDetail`, `ExtensionContent` (Task 1)
- Produces: `HostBridge` interface (`httpGet`), `ExtensionValidator.validate(quickJs: QuickJs): List<String>`, `ExtensionValidationException(missing: List<String>)`, `JsExtension(manifest, quickJs): Extension` — all consumed by `JSExtensionLoader` (Task 6).

- [ ] **Step 1: Create the sample JS extension file**

Create `sharedutils/jsextensionloader/samples/sample-extension.js`:

```javascript
// name: Sample Extension
// version: 1.0.0
// author: OtakuWorld
// description: Reference/fixture extension with stubbed implementations of all required functions.
// iconUrl: https://example.com/sample-extension-icon.png
// updateUrl: https://example.com/sample-extension/update.json

function getPopular(page) {
    return [
        { title: "Sample Item", url: "https://example.com/item/1", imageUrl: null }
    ];
}

function getLatest(page) {
    return [
        { title: "Latest Sample Item", url: "https://example.com/item/2", imageUrl: null }
    ];
}

function search(query, page) {
    return [
        { title: "Search Result for " + query, url: "https://example.com/item/3", imageUrl: null }
    ];
}

function getDetail(url) {
    return {
        title: "Sample Item",
        url: url,
        imageUrl: null,
        description: "A sample item detail.",
        genres: ["Action"],
        chapters: [
            { name: "Chapter 1", url: "https://example.com/chapter/1", uploaded: null }
        ]
    };
}

function getContent(url) {
    return {
        urls: ["https://example.com/content/1.png"],
        headers: {}
    };
}
```

- [ ] **Step 2: Create the sample TypeScript extension file**

Create `sharedutils/jsextensionloader/samples/sample-extension.ts`:

```typescript
// name: Sample TypeScript Extension
// version: 1.0.0
// author: OtakuWorld
// description: TypeScript reference/fixture extension with stubbed implementations of all required functions.
// iconUrl: https://example.com/sample-extension-icon.png
// updateUrl: https://example.com/sample-extension/update.json

interface Item {
    title: string;
    url: string;
    imageUrl: string;
}

function getPopular(page: number): Item[] {
    return [
        { title: "Sample Item", url: "https://example.com/item/1", imageUrl: null }
    ];
}

function getLatest(page: number): Item[] {
    return [
        { title: "Latest Sample Item", url: "https://example.com/item/2", imageUrl: null }
    ];
}

function search(query: string, page: number): Item[] {
    return [
        { title: "Search Result for " + query, url: "https://example.com/item/3", imageUrl: null }
    ];
}

function getDetail(url: string) {
    return {
        title: "Sample Item",
        url: url,
        imageUrl: null,
        description: "A sample item detail.",
        genres: ["Action"],
        chapters: [
            { name: "Chapter 1", url: "https://example.com/chapter/1", uploaded: null }
        ]
    };
}

function getContent(url: string) {
    return {
        urls: ["https://example.com/content/1.png"],
        headers: {}
    };
}
```

- [ ] **Step 3: Create the `.d.ts` declaration file for extension authors**

Create `sharedutils/jsextensionloader/samples/otaku-extension.d.ts`:

```typescript
// Type declarations for OtakuWorld JS/TS extensions.
// Reference these in your editor for type-checking and autocomplete.
// Not consumed on-device — extension functions must be synchronous.

interface ExtensionItem {
    title: string;
    url: string;
    imageUrl: string | null;
}

interface ExtensionChapter {
    name: string;
    url: string;
    uploaded: string | null;
}

interface ExtensionDetail {
    title: string;
    url: string;
    imageUrl: string | null;
    description: string | null;
    genres: string[];
    chapters: ExtensionChapter[];
}

interface ExtensionContent {
    urls: string[];
    headers?: Record<string, string>;
}

declare function getPopular(page: number): ExtensionItem[];
declare function getLatest(page: number): ExtensionItem[];
declare function search(query: string, page: number): ExtensionItem[];
declare function getDetail(url: string): ExtensionDetail;
declare function getContent(url: string): ExtensionContent;
```

- [ ] **Step 4: Copy the sample JS extension into a Kotlin test fixture**

Create `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/SampleExtensionFixture.kt`:

```kotlin
package com.programmersbox.jsextensionloader

object SampleExtensionFixture {

    // Kept in sync with samples/sample-extension.js — duplicated here so tests
    // don't need cross-platform classpath/resource loading to reach the samples/ dir.
    val SCRIPT_TEXT = """
        // name: Sample Extension
        // version: 1.0.0
        // author: OtakuWorld
        // description: Reference/fixture extension with stubbed implementations of all required functions.
        // iconUrl: https://example.com/sample-extension-icon.png
        // updateUrl: https://example.com/sample-extension/update.json

        function getPopular(page) {
            return [
                { title: "Sample Item", url: "https://example.com/item/1", imageUrl: null }
            ];
        }

        function getLatest(page) {
            return [
                { title: "Latest Sample Item", url: "https://example.com/item/2", imageUrl: null }
            ];
        }

        function search(query, page) {
            return [
                { title: "Search Result for " + query, url: "https://example.com/item/3", imageUrl: null }
            ];
        }

        function getDetail(url) {
            return {
                title: "Sample Item",
                url: url,
                imageUrl: null,
                description: "A sample item detail.",
                genres: ["Action"],
                chapters: [
                    { name: "Chapter 1", url: "https://example.com/chapter/1", uploaded: null }
                ]
            };
        }

        function getContent(url) {
            return {
                urls: ["https://example.com/content/1.png"],
                headers: {}
            };
        }
    """.trimIndent()

    const val MISSING_FUNCTIONS_SCRIPT = """
        function getPopular(page) { return []; }
        function getLatest(page) { return []; }
    """
}
```

- [ ] **Step 5: Write the failing test for `JsExtension`**

Create `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/JsExtensionTest.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs
import com.programmersbox.extensioninterfaces.ExtensionManifest
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JsExtensionTest {

    private val manifest = ExtensionManifest(
        id = "sample-extension",
        name = "Sample Extension",
        version = "1.0.0",
        author = "OtakuWorld",
        description = null,
        iconUrl = null,
        updateUrl = null,
    )

    private var quickJs: QuickJs? = null

    @AfterTest
    fun tearDown() {
        quickJs?.close()
    }

    private fun loadSampleExtension(): JsExtension {
        val js = QuickJs.create()
        quickJs = js
        js.evaluate(SampleExtensionFixture.SCRIPT_TEXT, "sample-extension.js")
        return JsExtension(manifest, js)
    }

    @Test
    fun getPopularReturnsParsedItems() = runTest {
        val extension = loadSampleExtension()
        val items = extension.getPopular(page = 1)
        assertEquals(1, items.size)
        assertEquals("Sample Item", items.first().title)
    }

    @Test
    fun getDetailReturnsParsedDetail() = runTest {
        val extension = loadSampleExtension()
        val detail = extension.getDetail("https://example.com/item/1")
        assertEquals("Sample Item", detail.title)
        assertEquals(1, detail.chapters.size)
        assertEquals("Chapter 1", detail.chapters.first().name)
    }

    @Test
    fun getContentReturnsParsedContent() = runTest {
        val extension = loadSampleExtension()
        val content = extension.getContent("https://example.com/item/1")
        assertEquals(listOf("https://example.com/content/1.png"), content.urls)
    }

    @Test
    fun searchIncludesQueryInResult() = runTest {
        val extension = loadSampleExtension()
        val items = extension.search("dragon", page = 1)
        assertEquals("Search Result for dragon", items.first().title)
    }

    @Test
    fun validatorReportsNoMissingFunctionsForSampleExtension() {
        val js = QuickJs.create()
        quickJs = js
        js.evaluate(SampleExtensionFixture.SCRIPT_TEXT, "sample-extension.js")
        assertEquals(emptyList(), ExtensionValidator.validate(js))
    }

    @Test
    fun validatorReportsMissingFunctions() {
        val js = QuickJs.create()
        quickJs = js
        js.evaluate(SampleExtensionFixture.MISSING_FUNCTIONS_SCRIPT, "incomplete-extension.js")
        val missing = ExtensionValidator.validate(js)
        assertEquals(listOf("search", "getDetail", "getContent"), missing)
    }
}
```

- [ ] **Step 6: Run the tests to verify they fail**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: FAIL — `JsExtension`, `ExtensionValidator` unresolved.

- [ ] **Step 7: Implement `HostBridge`**

Create `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/HostBridge.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

private val hostBridgeJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * The ONLY bridge exposed into the QuickJs sandbox. Extension code can reach
 * the network exclusively through [httpGet] — there is no ambient fetch/fs.
 */
interface HostBridge {
    fun httpGet(url: String, headersJson: String): String
}

class KtorHostBridge(private val client: HttpClient) : HostBridge {
    override fun httpGet(url: String, headersJson: String): String = runBlocking {
        val headers: Map<String, String> = hostBridgeJson.decodeFromString(headersJson)
        client.get(url) {
            headers.forEach { (key, value) -> header(key, value) }
        }.bodyAsText()
    }
}
```

- [ ] **Step 8: Implement `ExtensionValidator` and `ExtensionValidationException`**

Create `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/ExtensionValidator.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs
import kotlinx.serialization.json.Json

private val validatorJson = Json { ignoreUnknownKeys = true }

class ExtensionValidationException(val missing: List<String>) :
    Exception("Extension is missing required function(s): ${missing.joinToString()}")

object ExtensionValidator {

    private val requiredFunctions = listOf("getPopular", "getLatest", "search", "getDetail", "getContent")

    fun validate(quickJs: QuickJs): List<String> {
        val probe = requiredFunctions.joinToString(
            separator = ",",
            prefix = "JSON.stringify({",
            postfix = "})",
        ) { "\"$it\": typeof $it" }
        val resultJson = quickJs.evaluate(probe, "extension-validate.js") as String
        val types: Map<String, String> = validatorJson.decodeFromString(resultJson)
        return requiredFunctions.filter { types[it] != "function" }
    }
}
```

- [ ] **Step 9: Implement `JsExtension`**

Create `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/JsExtension.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs
import com.programmersbox.extensioninterfaces.Extension
import com.programmersbox.extensioninterfaces.ExtensionContent
import com.programmersbox.extensioninterfaces.ExtensionDetail
import com.programmersbox.extensioninterfaces.ExtensionItem
import com.programmersbox.extensioninterfaces.ExtensionManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private val jsExtensionJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

class JsExtension(
    override val manifest: ExtensionManifest,
    private val quickJs: QuickJs,
) : Extension {

    override suspend fun getPopular(page: Int): List<ExtensionItem> =
        call("getPopular($page)")

    override suspend fun getLatest(page: Int): List<ExtensionItem> =
        call("getLatest($page)")

    override suspend fun search(query: String, page: Int): List<ExtensionItem> =
        call("search(${jsExtensionJson.encodeToString(String.serializer(), query)}, $page)")

    override suspend fun getDetail(url: String): ExtensionDetail =
        call("getDetail(${jsExtensionJson.encodeToString(String.serializer(), url)})")

    override suspend fun getContent(url: String): ExtensionContent =
        call("getContent(${jsExtensionJson.encodeToString(String.serializer(), url)})")

    private suspend inline fun <reified T> call(callExpression: String): T = withContext(Dispatchers.Default) {
        val resultJson = quickJs.evaluate("JSON.stringify($callExpression)", "extension-call.js") as String
        jsExtensionJson.decodeFromString(serializer(), resultJson)
    }

    fun close() {
        quickJs.close()
    }
}
```

- [ ] **Step 10: Run the tests to verify they pass**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: PASS (6 new tests, 24 total)

- [ ] **Step 11: Commit**

```bash
git add sharedutils/jsextensionloader
git commit -m "feat: add sandboxed JsExtension (QuickJs), validator, sample extension, and .d.ts"
```

---

### Task 6: `JSExtensionLoader` orchestrator

**Files:**
- Create: `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/JSExtensionLoader.kt`
- Test: `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/JSExtensionLoaderTest.kt`

**Interfaces:**
- Consumes: `ExtensionManifestParser.parse` (Task 2), `TsTranspiler.transpile` (Task 3), `HostBridge`, `ExtensionValidator`, `ExtensionValidationException`, `JsExtension` (Task 5)
- Produces: `JSExtensionLoader(hostBridge: HostBridge).load(scriptText: String, fileName: String, companionManifestJson: String?): JsExtension` — consumed by `JsExtensionUpdateRunner` (Task 11) and by app-level discovery flows.

- [ ] **Step 1: Write the failing tests**

Create `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/JSExtensionLoaderTest.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JSExtensionLoaderTest {

    private class NoOpHostBridge : HostBridge {
        override fun httpGet(url: String, headersJson: String): String = ""
    }

    private var loaded: JsExtension? = null

    @AfterTest
    fun tearDown() {
        loaded?.close()
    }

    @Test
    fun loadsAndValidatesAPlainJavaScriptExtension() = runTest {
        val loader = JSExtensionLoader(NoOpHostBridge())
        val extension = loader.load(
            scriptText = SampleExtensionFixture.SCRIPT_TEXT,
            fileName = "sample-extension.js",
            companionManifestJson = null,
        )
        loaded = extension
        assertEquals("Sample Extension", extension.manifest.name)
        assertEquals(1, extension.getPopular(1).size)
    }

    @Test
    fun transpilesAndLoadsATypeScriptExtension() = runTest {
        val ts = """
            // name: TS Sample
            // version: 1.0.0
            interface Item { title: string; url: string; }
            function getPopular(page: number): Item[] {
                return [{ title: "TS Item", url: "https://example.com/1" }];
            }
            function getLatest(page: number): Item[] { return []; }
            function search(query: string, page: number): Item[] { return []; }
            function getDetail(url: string) {
                return { title: "TS Item", url: url, imageUrl: null, description: null, genres: [], chapters: [] };
            }
            function getContent(url: string) {
                return { urls: [], headers: {} };
            }
        """.trimIndent()

        val loader = JSExtensionLoader(NoOpHostBridge())
        val extension = loader.load(scriptText = ts, fileName = "ts-sample.ts", companionManifestJson = null)
        loaded = extension

        assertEquals("TS Sample", extension.manifest.name)
        assertEquals("TS Item", extension.getPopular(1).first().title)
    }

    @Test
    fun rejectsExtensionsMissingRequiredFunctions() = runTest {
        val loader = JSExtensionLoader(NoOpHostBridge())
        val exception = assertFailsWith<ExtensionValidationException> {
            loader.load(
                scriptText = "// name: Incomplete\n// version: 1.0.0\n" + SampleExtensionFixture.MISSING_FUNCTIONS_SCRIPT,
                fileName = "incomplete.js",
                companionManifestJson = null,
            )
        }
        assertEquals(listOf("search", "getDetail", "getContent"), exception.missing)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: FAIL — `JSExtensionLoader` unresolved.

- [ ] **Step 3: Implement `JSExtensionLoader`**

Create `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/JSExtensionLoader.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs

class JSExtensionLoader(private val hostBridge: HostBridge) {

    fun load(scriptText: String, fileName: String, companionManifestJson: String?): JsExtension {
        val sourceId = fileName.substringBeforeLast(".")
        val manifest = ExtensionManifestParser.parse(scriptText, companionManifestJson, sourceId)
        val isTypeScript = fileName.endsWith(".ts")
        val transpiled = if (isTypeScript) TsTranspiler.transpile(scriptText) else scriptText

        val quickJs = QuickJs.create()
        quickJs.set("HostBridge", HostBridge::class.java, hostBridge)
        quickJs.evaluate(transpiled, fileName)

        val missing = ExtensionValidator.validate(quickJs)
        if (missing.isNotEmpty()) {
            quickJs.close()
            throw ExtensionValidationException(missing)
        }

        return JsExtension(manifest, quickJs)
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: PASS (3 new tests, 27 total)

- [ ] **Step 5: Commit**

```bash
git add sharedutils/jsextensionloader
git commit -m "feat: add JSExtensionLoader orchestrating parse, transpile, eval, and validation"
```

---

### Task 7: `JsExtensionRepository` — hot-reload registry

**Files:**
- Create: `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/JsExtensionRepository.kt`
- Test: `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/JsExtensionRepositoryTest.kt`

**Interfaces:**
- Consumes: `JsExtension` (Task 5)
- Produces: `JsExtensionRepository` — `extensions: StateFlow<List<JsExtension>>`, `register(extension: JsExtension)`, `unload(id: String)` — consumed by `JsExtensionUpdateRunner` (Task 11).

- [ ] **Step 1: Write the failing tests**

Create `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/JsExtensionRepositoryTest.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs
import com.programmersbox.extensioninterfaces.ExtensionManifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsExtensionRepositoryTest {

    private fun extensionWithId(id: String): JsExtension {
        val quickJs = QuickJs.create()
        quickJs.evaluate(SampleExtensionFixture.SCRIPT_TEXT, "$id.js")
        val manifest = ExtensionManifest(
            id = id, name = id, version = "1.0.0", author = null,
            description = null, iconUrl = null, updateUrl = null,
        )
        return JsExtension(manifest, quickJs)
    }

    @Test
    fun registerAddsExtension() {
        val repository = JsExtensionRepository()
        repository.register(extensionWithId("one"))
        assertEquals(1, repository.extensions.value.size)
        assertEquals("one", repository.extensions.value.first().manifest.id)
    }

    @Test
    fun registeringSameIdReplacesThePrevious() {
        val repository = JsExtensionRepository()
        repository.register(extensionWithId("one"))
        repository.register(extensionWithId("one"))
        assertEquals(1, repository.extensions.value.size)
    }

    @Test
    fun unloadRemovesExtensionAndClosesIt() {
        val repository = JsExtensionRepository()
        repository.register(extensionWithId("one"))
        repository.unload("one")
        assertTrue(repository.extensions.value.isEmpty())
    }

    @Test
    fun unloadingUnknownIdIsANoOp() {
        val repository = JsExtensionRepository()
        repository.register(extensionWithId("one"))
        repository.unload("does-not-exist")
        assertEquals(1, repository.extensions.value.size)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: FAIL — `JsExtensionRepository` unresolved.

- [ ] **Step 3: Implement `JsExtensionRepository`**

Create `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/JsExtensionRepository.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class JsExtensionRepository {

    private val _extensions = MutableStateFlow<List<JsExtension>>(emptyList())
    val extensions: StateFlow<List<JsExtension>> = _extensions

    fun register(extension: JsExtension) {
        _extensions.update { current ->
            current.filterNot { it.manifest.id == extension.manifest.id } + extension
        }
    }

    fun unload(id: String) {
        _extensions.update { current ->
            current.find { it.manifest.id == id }?.close()
            current.filterNot { it.manifest.id == id }
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: PASS (4 new tests, 31 total)

- [ ] **Step 5: Commit**

```bash
git add sharedutils/jsextensionloader
git commit -m "feat: add JsExtensionRepository for hot load/unload"
```

---

### Task 8: `ExtensionDiscovery` — local/remote/bundled scanning (expect/actual)

**Files:**
- Create: `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/ExtensionDiscovery.kt`
- Create: `sharedutils/jsextensionloader/src/androidMain/kotlin/com/programmersbox/jsextensionloader/ExtensionDiscovery.android.kt`
- Create: `sharedutils/jsextensionloader/src/jvmMain/kotlin/com/programmersbox/jsextensionloader/ExtensionDiscovery.jvm.kt`
- Create: `sharedutils/jsextensionloader/src/iosMain/kotlin/com/programmersbox/jsextensionloader/ExtensionDiscovery.ios.kt`
- Test: `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/ExtensionDiscoveryTest.kt`

**Interfaces:**
- Produces: `DiscoveredExtensionSource(sourceId, fileName, scriptText, companionManifestJson)`, `expect class ExtensionDiscovery { scanLocalDirectory(); fetchRemote(url); scanBundledResources() }` — consumed by `JsExtensionUpdateRunner` (Task 11) and future app wiring.

- [ ] **Step 1: Write the failing tests (JVM actual only, per Global Constraints)**

Create `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/ExtensionDiscoveryTest.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExtensionDiscoveryTest {

    @Test
    fun scanLocalDirectoryFindsJsAndTsFilesWithCompanionManifests() = runTest {
        val tempDir = kotlin.io.path.createTempDirectory().toFile()
        try {
            File(tempDir, "one.js").writeText("// name: One\n// version: 1.0.0\n")
            File(tempDir, "one.manifest.json").writeText("""{"name":"One","version":"1.0.0"}""")
            File(tempDir, "two.ts").writeText("// name: Two\n// version: 1.0.0\n")
            File(tempDir, "ignored.txt").writeText("not an extension")

            val discovery = ExtensionDiscovery(
                extensionsDir = { tempDir },
                bundledResourcesDir = "js_extensions",
                client = HttpClient(MockEngine { respond("") }),
            )

            val sources = discovery.scanLocalDirectory().sortedBy { it.sourceId }

            assertEquals(2, sources.size)
            assertEquals("one", sources[0].sourceId)
            assertEquals("""{"name":"One","version":"1.0.0"}""", sources[0].companionManifestJson)
            assertEquals("two", sources[1].sourceId)
            assertNull(sources[1].companionManifestJson)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun fetchRemoteDownloadsScriptText() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = SampleExtensionFixture.SCRIPT_TEXT,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/javascript"),
            )
        }
        val discovery = ExtensionDiscovery(
            extensionsDir = { kotlin.io.path.createTempDirectory().toFile() },
            bundledResourcesDir = "js_extensions",
            client = HttpClient(mockEngine),
        )

        val source = discovery.fetchRemote("https://example.com/sample-extension.js")

        assertEquals("sample-extension", source.sourceId)
        assertEquals(SampleExtensionFixture.SCRIPT_TEXT, source.scriptText)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: FAIL — `ExtensionDiscovery` unresolved.

- [ ] **Step 3: Declare the common `expect class` and shared data class**

Create `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/ExtensionDiscovery.kt`:

```kotlin
package com.programmersbox.jsextensionloader

data class DiscoveredExtensionSource(
    val sourceId: String,
    val fileName: String,
    val scriptText: String,
    val companionManifestJson: String?,
)

expect class ExtensionDiscovery {
    suspend fun scanLocalDirectory(): List<DiscoveredExtensionSource>
    suspend fun fetchRemote(url: String): DiscoveredExtensionSource
    suspend fun scanBundledResources(): List<DiscoveredExtensionSource>
}
```

- [ ] **Step 4: Implement the JVM actual**

Create `sharedutils/jsextensionloader/src/jvmMain/kotlin/com/programmersbox/jsextensionloader/ExtensionDiscovery.jvm.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.File

actual class ExtensionDiscovery(
    private val extensionsDir: () -> File,
    private val bundledResourcesDir: String,
    private val client: HttpClient,
) {
    actual suspend fun scanLocalDirectory(): List<DiscoveredExtensionSource> {
        val dir = extensionsDir()
        val files = dir.listFiles { file -> file.extension == "js" || file.extension == "ts" }.orEmpty()
        return files.map { file ->
            val manifestFile = File(dir, "${file.nameWithoutExtension}.manifest.json")
            DiscoveredExtensionSource(
                sourceId = file.nameWithoutExtension,
                fileName = file.name,
                scriptText = file.readText(),
                companionManifestJson = manifestFile.takeIf { it.exists() }?.readText(),
            )
        }
    }

    actual suspend fun fetchRemote(url: String): DiscoveredExtensionSource {
        val scriptText = client.get(url).bodyAsText()
        val fileName = url.substringAfterLast("/")
        return DiscoveredExtensionSource(
            sourceId = fileName.substringBeforeLast("."),
            fileName = fileName,
            scriptText = scriptText,
            companionManifestJson = null,
        )
    }

    actual suspend fun scanBundledResources(): List<DiscoveredExtensionSource> {
        val resourceUrl = ExtensionDiscovery::class.java.classLoader?.getResource(bundledResourcesDir)
            ?: return emptyList()
        val dir = File(resourceUrl.toURI())
        val files = dir.listFiles { file -> file.extension == "js" || file.extension == "ts" }.orEmpty()
        return files.map { file ->
            DiscoveredExtensionSource(
                sourceId = file.nameWithoutExtension,
                fileName = file.name,
                scriptText = file.readText(),
                companionManifestJson = null,
            )
        }
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: PASS (2 new tests, 33 total)

- [ ] **Step 6: Implement the Android actual (compile-checked only, not unit-tested per Global Constraints)**

Create `sharedutils/jsextensionloader/src/androidMain/kotlin/com/programmersbox/jsextensionloader/ExtensionDiscovery.android.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.File

actual class ExtensionDiscovery(
    private val context: Context,
    private val extensionsSubDir: String,
    private val bundledAssetsDir: String,
    private val client: HttpClient,
) {
    actual suspend fun scanLocalDirectory(): List<DiscoveredExtensionSource> {
        val dir = File(context.filesDir, extensionsSubDir)
        val files = dir.listFiles { file -> file.extension == "js" || file.extension == "ts" }.orEmpty()
        return files.map { file ->
            val manifestFile = File(dir, "${file.nameWithoutExtension}.manifest.json")
            DiscoveredExtensionSource(
                sourceId = file.nameWithoutExtension,
                fileName = file.name,
                scriptText = file.readText(),
                companionManifestJson = manifestFile.takeIf { it.exists() }?.readText(),
            )
        }
    }

    actual suspend fun fetchRemote(url: String): DiscoveredExtensionSource {
        val scriptText = client.get(url).bodyAsText()
        val fileName = url.substringAfterLast("/")
        return DiscoveredExtensionSource(
            sourceId = fileName.substringBeforeLast("."),
            fileName = fileName,
            scriptText = scriptText,
            companionManifestJson = null,
        )
    }

    actual suspend fun scanBundledResources(): List<DiscoveredExtensionSource> {
        val assetManager = context.assets
        val fileNames = assetManager.list(bundledAssetsDir).orEmpty()
            .filter { it.endsWith(".js") || it.endsWith(".ts") }
        return fileNames.map { fileName ->
            val scriptText = assetManager.open("$bundledAssetsDir/$fileName").bufferedReader().use { it.readText() }
            DiscoveredExtensionSource(
                sourceId = fileName.substringBeforeLast("."),
                fileName = fileName,
                scriptText = scriptText,
                companionManifestJson = null,
            )
        }
    }
}
```

Run: `./gradlew :sharedutils:jsextensionloader:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Implement the iOS actual (compile-checked only, not unit-tested per Global Constraints)**

Create `sharedutils/jsextensionloader/src/iosMain/kotlin/com/programmersbox/jsextensionloader/ExtensionDiscovery.ios.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

@OptIn(ExperimentalForeignApi::class)
actual class ExtensionDiscovery(
    private val extensionsDirectoryPath: String,
    private val bundledResourcesSubdirectory: String,
    private val client: HttpClient,
) {
    actual suspend fun scanLocalDirectory(): List<DiscoveredExtensionSource> {
        val fileManager = NSFileManager.defaultManager
        val fileNames = (fileManager.contentsOfDirectoryAtPath(extensionsDirectoryPath, null) as? List<String>)
            .orEmpty()
            .filter { it.endsWith(".js") || it.endsWith(".ts") }
        return fileNames.map { fileName ->
            val fullPath = "$extensionsDirectoryPath/$fileName"
            val scriptText = NSString.stringWithContentsOfFile(fullPath, NSUTF8StringEncoding, null) as String
            val manifestPath = "$extensionsDirectoryPath/${fileName.substringBeforeLast(".")}.manifest.json"
            val manifestText = if (fileManager.fileExistsAtPath(manifestPath)) {
                NSString.stringWithContentsOfFile(manifestPath, NSUTF8StringEncoding, null) as String?
            } else {
                null
            }
            DiscoveredExtensionSource(
                sourceId = fileName.substringBeforeLast("."),
                fileName = fileName,
                scriptText = scriptText,
                companionManifestJson = manifestText,
            )
        }
    }

    actual suspend fun fetchRemote(url: String): DiscoveredExtensionSource {
        val scriptText = client.get(url).bodyAsText()
        val fileName = url.substringAfterLast("/")
        return DiscoveredExtensionSource(
            sourceId = fileName.substringBeforeLast("."),
            fileName = fileName,
            scriptText = scriptText,
            companionManifestJson = null,
        )
    }

    actual suspend fun scanBundledResources(): List<DiscoveredExtensionSource> {
        val bundlePath = NSBundle.mainBundle.pathForResource(bundledResourcesSubdirectory, null) ?: return emptyList()
        val fileManager = NSFileManager.defaultManager
        val fileNames = (fileManager.contentsOfDirectoryAtPath(bundlePath, null) as? List<String>)
            .orEmpty()
            .filter { it.endsWith(".js") || it.endsWith(".ts") }
        return fileNames.map { fileName ->
            val fullPath = "$bundlePath/$fileName"
            val scriptText = NSString.stringWithContentsOfFile(fullPath, NSUTF8StringEncoding, null) as String
            DiscoveredExtensionSource(
                sourceId = fileName.substringBeforeLast("."),
                fileName = fileName,
                scriptText = scriptText,
                companionManifestJson = null,
            )
        }
    }
}
```

Run: `./gradlew :sharedutils:jsextensionloader:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL. If the `NSFileManager`/`NSString` interop signatures generated on this machine differ slightly (e.g. an `error` parameter requiring `memScoped`), adjust the call sites to match the generated Kotlin/Native signatures — this is normal cinterop reconciliation, not a design change.

- [ ] **Step 8: Commit**

```bash
git add sharedutils/jsextensionloader
git commit -m "feat: add ExtensionDiscovery for local/remote/bundled scanning (android/jvm/ios)"
```

---

### Task 9: `ExtensionUpdateSource` and `ExtensionUpdateChecker`

**Files:**
- Create: `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/ExtensionUpdateChecker.kt`
- Test: `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/ExtensionUpdateCheckerTest.kt`

**Interfaces:**
- Consumes: `ExtensionUpdateInfo` (Task 1), `SemVerCompare` (Task 4)
- Produces: `InstalledExtension(id, currentVersion, updateUrl)`, `ExtensionUpdateSource` sealed interface, `ExtensionUpdateChecker(client).findAvailableUpdates(installed, registryEndpoint): List<ExtensionUpdateInfo>` — consumed by `JsExtensionUpdateRunner` (Task 11).

- [ ] **Step 1: Write the failing tests**

Create `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/ExtensionUpdateCheckerTest.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtensionUpdateCheckerTest {

    private fun clientReturning(responsesByUrl: Map<String, String>): HttpClient {
        val mockEngine = MockEngine { request ->
            val body = responsesByUrl.getValue(request.url.toString())
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun findsUpdateFromCentralizedRegistryWhenNewer() = runTest {
        val client = clientReturning(
            mapOf(
                "https://example.com/registry.json" to
                    """[{"id":"ext-a","latestVersion":"2.0.0","downloadUrl":"https://example.com/ext-a.js"}]""",
            )
        )
        val checker = ExtensionUpdateChecker(client)
        val installed = listOf(InstalledExtension(id = "ext-a", currentVersion = "1.0.0", updateUrl = null))

        val updates = checker.findAvailableUpdates(installed, registryEndpoint = "https://example.com/registry.json")

        assertEquals(1, updates.size)
        assertEquals("ext-a", updates.first().id)
    }

    @Test
    fun skipsRegistryEntryWhenNotNewer() = runTest {
        val client = clientReturning(
            mapOf(
                "https://example.com/registry.json" to
                    """[{"id":"ext-a","latestVersion":"1.0.0","downloadUrl":"https://example.com/ext-a.js"}]""",
            )
        )
        val checker = ExtensionUpdateChecker(client)
        val installed = listOf(InstalledExtension(id = "ext-a", currentVersion = "1.0.0", updateUrl = null))

        val updates = checker.findAvailableUpdates(installed, registryEndpoint = "https://example.com/registry.json")

        assertTrue(updates.isEmpty())
    }

    @Test
    fun fallsBackToPerExtensionUpdateUrlWhenNotInRegistry() = runTest {
        val client = clientReturning(
            mapOf(
                "https://example.com/registry.json" to "[]",
                "https://example.com/ext-b/update.json" to
                    """{"id":"ext-b","latestVersion":"3.0.0","downloadUrl":"https://example.com/ext-b.js"}""",
            )
        )
        val checker = ExtensionUpdateChecker(client)
        val installed = listOf(
            InstalledExtension(id = "ext-b", currentVersion = "2.0.0", updateUrl = "https://example.com/ext-b/update.json"),
        )

        val updates = checker.findAvailableUpdates(installed, registryEndpoint = "https://example.com/registry.json")

        assertEquals(1, updates.size)
        assertEquals("ext-b", updates.first().id)
    }

    @Test
    fun checksBothSourcesWhenNoRegistryEndpointGiven() = runTest {
        val client = clientReturning(
            mapOf(
                "https://example.com/ext-c/update.json" to
                    """{"id":"ext-c","latestVersion":"1.1.0","downloadUrl":"https://example.com/ext-c.js"}""",
            )
        )
        val checker = ExtensionUpdateChecker(client)
        val installed = listOf(
            InstalledExtension(id = "ext-c", currentVersion = "1.0.0", updateUrl = "https://example.com/ext-c/update.json"),
        )

        val updates = checker.findAvailableUpdates(installed, registryEndpoint = null)

        assertEquals(1, updates.size)
        assertEquals("ext-c", updates.first().id)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: FAIL — `ExtensionUpdateChecker`, `InstalledExtension` unresolved.

- [ ] **Step 3: Implement `ExtensionUpdateSource` and `ExtensionUpdateChecker`**

Create `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/ExtensionUpdateChecker.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import com.programmersbox.extensioninterfaces.ExtensionUpdateInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

data class InstalledExtension(
    val id: String,
    val currentVersion: String,
    val updateUrl: String?,
)

sealed interface ExtensionUpdateSource {
    data class CentralizedRegistry(val endpoint: String) : ExtensionUpdateSource
    data class PerExtensionUrl(val url: String) : ExtensionUpdateSource
}

class ExtensionUpdateChecker(private val client: HttpClient) {

    suspend fun checkCentralizedRegistry(source: ExtensionUpdateSource.CentralizedRegistry): List<ExtensionUpdateInfo> =
        client.get(source.endpoint).body()

    suspend fun checkPerExtensionUrl(source: ExtensionUpdateSource.PerExtensionUrl): ExtensionUpdateInfo =
        client.get(source.url).body()

    suspend fun findAvailableUpdates(
        installed: List<InstalledExtension>,
        registryEndpoint: String?,
    ): List<ExtensionUpdateInfo> {
        val registryUpdates = registryEndpoint
            ?.let { checkCentralizedRegistry(ExtensionUpdateSource.CentralizedRegistry(it)) }
            .orEmpty()

        val coveredIds = registryUpdates.map { it.id }.toSet()
        val perExtensionUpdates = installed
            .filter { it.id !in coveredIds && it.updateUrl != null }
            .map { checkPerExtensionUrl(ExtensionUpdateSource.PerExtensionUrl(it.updateUrl!!)) }

        return (registryUpdates + perExtensionUpdates).filter { update ->
            val current = installed.find { it.id == update.id }?.currentVersion
            current != null && SemVerCompare.isNewer(current, update.latestVersion)
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: PASS (4 new tests, 37 total)

- [ ] **Step 5: Commit**

```bash
git add sharedutils/jsextensionloader
git commit -m "feat: add ExtensionUpdateChecker merging centralized-registry and per-extension update checks"
```

---

### Task 10: `ExtensionUpdateMode`, `JsExtensionUpdateSettings`, and the `DataStoreHandling` key

**Files:**
- Create: `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/JsExtensionUpdateSettings.kt`
- Modify: `datastore/src/commonMain/kotlin/com/programmersbox/datastore/DataStoreHandling.kt`
- Test: `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/JsExtensionUpdateSettingsTest.kt`

**Interfaces:**
- Produces: `ExtensionUpdateMode` enum (`AUTOMATIC`, `NOTIFY`, `DISABLED`), `JsExtensionUpdateSettings.getMode(): ExtensionUpdateMode`, `.setMode(mode: ExtensionUpdateMode)` — consumed by `JsExtensionUpdateRunner` (Task 11).

- [ ] **Step 1: Add the `jsExtensionUpdateMode` key to `DataStoreHandling`**

In `datastore/src/commonMain/kotlin/com/programmersbox/datastore/DataStoreHandling.kt`, add this field right before the closing brace of the class (after `timeSpentDoing`, currently ending at line 67):

```kotlin

    val jsExtensionUpdateMode = DataStoreHandler(
        key = intPreferencesKey("jsExtensionUpdateMode"),
        defaultValue = 1 // ExtensionUpdateMode.NOTIFY.ordinal
    )
```

- [ ] **Step 2: Write the failing test**

Create `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/JsExtensionUpdateSettingsTest.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import com.programmersbox.datastore.DataStoreHandling
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JsExtensionUpdateSettingsTest {

    @Test
    fun defaultsToNotify() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling())
        assertEquals(ExtensionUpdateMode.NOTIFY, settings.getMode())
    }

    @Test
    fun setModePersistsAndGetModeReadsItBack() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling())
        settings.setMode(ExtensionUpdateMode.AUTOMATIC)
        assertEquals(ExtensionUpdateMode.AUTOMATIC, settings.getMode())
        settings.setMode(ExtensionUpdateMode.DISABLED)
        assertEquals(ExtensionUpdateMode.DISABLED, settings.getMode())
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: FAIL — `ExtensionUpdateMode`, `JsExtensionUpdateSettings` unresolved.

- [ ] **Step 4: Implement `ExtensionUpdateMode` and `JsExtensionUpdateSettings`**

Create `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/JsExtensionUpdateSettings.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import com.programmersbox.datastore.DataStoreHandling

enum class ExtensionUpdateMode { AUTOMATIC, NOTIFY, DISABLED }

class JsExtensionUpdateSettings(
    private val dataStoreHandling: DataStoreHandling = DataStoreHandling(),
) {
    suspend fun getMode(): ExtensionUpdateMode {
        val ordinal = dataStoreHandling.jsExtensionUpdateMode.get()
        return ExtensionUpdateMode.entries.getOrElse(ordinal) { ExtensionUpdateMode.NOTIFY }
    }

    suspend fun setMode(mode: ExtensionUpdateMode) {
        dataStoreHandling.jsExtensionUpdateMode.set(mode.ordinal)
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: PASS (2 new tests, 39 total)

- [ ] **Step 6: Commit**

```bash
git add datastore/src/commonMain/kotlin/com/programmersbox/datastore/DataStoreHandling.kt sharedutils/jsextensionloader
git commit -m "feat: add extension update mode setting (automatic/notify/disabled)"
```

---

### Task 11: `JsExtensionUpdateRunner` + Android `JsExtensionUpdateWorker` + scheduling + Koin module

**Files:**
- Create: `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/JsExtensionUpdateRunner.kt`
- Create: `sharedutils/jsextensionloader/src/androidMain/kotlin/com/programmersbox/jsextensionloader/JsExtensionUpdateWorker.kt`
- Create: `sharedutils/jsextensionloader/src/androidMain/kotlin/com/programmersbox/jsextensionloader/JsExtensionUpdateScheduler.kt`
- Create: `sharedutils/jsextensionloader/src/androidMain/kotlin/com/programmersbox/jsextensionloader/JsExtensionLoaderModule.kt`
- Test: `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/JsExtensionUpdateRunnerTest.kt`
- Modify: `sharedutils/jsextensionloader/build.gradle.kts`

**Interfaces:**
- Consumes: `JsExtensionRepository` (Task 7), `ExtensionDiscovery`, `JSExtensionLoader` (Tasks 6/8), `ExtensionUpdateChecker`, `InstalledExtension` (Task 9), `JsExtensionUpdateSettings`, `ExtensionUpdateMode` (Task 10)
- Produces: `JsExtensionUpdateRunner.run()` — consumed by Android's `JsExtensionUpdateWorker` and, in Task 12, by the JVM/iOS coroutine ticker. `JsExtensionUpdateScheduler.schedule(workManager)` / `.cancel(workManager)` and `jsExtensionLoaderModule` (Koin) are available for a future app to wire in — not invoked by this plan.

- [ ] **Step 1: Add Koin dependency to `commonMain` (needed by `KoinComponent` in the worker)**

In `sharedutils/jsextensionloader/build.gradle.kts`, add these two lines to the `commonMain` `dependencies { }` block (alongside the existing `implementation(projects.datastore)` line):

```kotlin
                implementation(project.dependencies.platform(commonLibs.koin.bom))
                implementation(commonLibs.koinCores)
```

- [ ] **Step 2: Write the failing test for `JsExtensionUpdateRunner`**

Create `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/JsExtensionUpdateRunnerTest.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.extensioninterfaces.ExtensionUpdateInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsExtensionUpdateRunnerTest {

    private class NoOpHostBridge : HostBridge {
        override fun httpGet(url: String, headersJson: String): String = ""
    }

    private val repository = JsExtensionRepository()

    @AfterTest
    fun tearDown() {
        repository.extensions.value.forEach { repository.unload(it.manifest.id) }
    }

    private fun clientReturning(responsesByUrl: Map<String, String>): HttpClient {
        val mockEngine = MockEngine { request ->
            val body = responsesByUrl[request.url.toString()]
                ?: SampleExtensionFixture.SCRIPT_TEXT
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun disabledModeDoesNothing() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling()).apply { setMode(ExtensionUpdateMode.DISABLED) }
        val notified = mutableListOf<ExtensionUpdateInfo>()
        val client = clientReturning(emptyMap())
        val runner = JsExtensionUpdateRunner(
            repository = repository,
            discovery = ExtensionDiscovery(
                extensionsDir = { kotlin.io.path.createTempDirectory().toFile() },
                bundledResourcesDir = "js_extensions",
                client = client,
            ),
            loader = JSExtensionLoader(NoOpHostBridge()),
            updateChecker = ExtensionUpdateChecker(client),
            settings = settings,
            registryEndpoint = null,
            onUpdateAvailable = { notified.add(it) },
        )

        runner.run()

        assertTrue(notified.isEmpty())
    }

    @Test
    fun notifyModeReportsAvailableUpdatesWithoutReloading() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling()).apply { setMode(ExtensionUpdateMode.NOTIFY) }
        val notified = mutableListOf<ExtensionUpdateInfo>()
        val client = clientReturning(
            mapOf(
                "https://example.com/registry.json" to
                    """[{"id":"sample-extension","latestVersion":"2.0.0","downloadUrl":"https://example.com/sample-extension.js"}]""",
            )
        )
        val runner = JsExtensionUpdateRunner(
            repository = repository,
            discovery = ExtensionDiscovery(
                extensionsDir = { kotlin.io.path.createTempDirectory().toFile() },
                bundledResourcesDir = "js_extensions",
                client = client,
            ),
            loader = JSExtensionLoader(NoOpHostBridge()),
            updateChecker = ExtensionUpdateChecker(client),
            settings = settings,
            registryEndpoint = "https://example.com/registry.json",
            onUpdateAvailable = { notified.add(it) },
        )
        repository.register(
            JSExtensionLoader(NoOpHostBridge()).load(
                scriptText = SampleExtensionFixture.SCRIPT_TEXT,
                fileName = "sample-extension.js",
                companionManifestJson = null,
            )
        )

        runner.run()

        assertEquals(1, notified.size)
        assertEquals("sample-extension", notified.first().id)
        assertEquals("1.0.0", repository.extensions.value.first().manifest.version)
    }

    @Test
    fun automaticModeReloadsUpdatedExtension() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling()).apply { setMode(ExtensionUpdateMode.AUTOMATIC) }
        val client = clientReturning(
            mapOf(
                "https://example.com/registry.json" to
                    """[{"id":"sample-extension","latestVersion":"2.0.0","downloadUrl":"https://example.com/sample-extension.js"}]""",
                "https://example.com/sample-extension.js" to SampleExtensionFixture.SCRIPT_TEXT,
            )
        )
        val runner = JsExtensionUpdateRunner(
            repository = repository,
            discovery = ExtensionDiscovery(
                extensionsDir = { kotlin.io.path.createTempDirectory().toFile() },
                bundledResourcesDir = "js_extensions",
                client = client,
            ),
            loader = JSExtensionLoader(NoOpHostBridge()),
            updateChecker = ExtensionUpdateChecker(client),
            settings = settings,
            registryEndpoint = "https://example.com/registry.json",
            onUpdateAvailable = { },
        )
        repository.register(
            JSExtensionLoader(NoOpHostBridge()).load(
                scriptText = SampleExtensionFixture.SCRIPT_TEXT,
                fileName = "sample-extension.js",
                companionManifestJson = null,
            )
        )

        runner.run()

        assertEquals(1, repository.extensions.value.size)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: FAIL — `JsExtensionUpdateRunner` unresolved.

- [ ] **Step 4: Implement `JsExtensionUpdateRunner`**

Create `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/JsExtensionUpdateRunner.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import com.programmersbox.extensioninterfaces.ExtensionUpdateInfo

class JsExtensionUpdateRunner(
    private val repository: JsExtensionRepository,
    private val discovery: ExtensionDiscovery,
    private val loader: JSExtensionLoader,
    private val updateChecker: ExtensionUpdateChecker,
    private val settings: JsExtensionUpdateSettings,
    private val registryEndpoint: String?,
    private val onUpdateAvailable: suspend (ExtensionUpdateInfo) -> Unit,
) {
    suspend fun run() {
        val mode = settings.getMode()
        if (mode == ExtensionUpdateMode.DISABLED) return

        val installed = repository.extensions.value.map {
            InstalledExtension(
                id = it.manifest.id,
                currentVersion = it.manifest.version,
                updateUrl = it.manifest.updateUrl,
            )
        }
        val updates = updateChecker.findAvailableUpdates(installed, registryEndpoint)

        when (mode) {
            ExtensionUpdateMode.AUTOMATIC -> updates.forEach { update ->
                val source = discovery.fetchRemote(update.downloadUrl)
                val extension = loader.load(source.scriptText, source.fileName, source.companionManifestJson)
                repository.register(extension)
            }
            ExtensionUpdateMode.NOTIFY -> updates.forEach { onUpdateAvailable(it) }
            ExtensionUpdateMode.DISABLED -> Unit
        }
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: PASS (3 new tests, 42 total)

- [ ] **Step 6: Implement the Android `JsExtensionUpdateWorker` (compile-checked, not unit-tested per Global Constraints)**

Create `sharedutils/jsextensionloader/src/androidMain/kotlin/com/programmersbox/jsextensionloader/JsExtensionUpdateWorker.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named

class JsExtensionUpdateWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val notificationManager by lazy { applicationContext.getSystemService<NotificationManager>() }

    override suspend fun doWork(): Result {
        return try {
            val repository = get<JsExtensionRepository>()
            val discovery = get<ExtensionDiscovery>()
            val loader = get<JSExtensionLoader>()
            val updateChecker = get<ExtensionUpdateChecker>()
            val settings = get<JsExtensionUpdateSettings>()
            val registryEndpoint = getKoin().getOrNull<String>(named("jsExtensionRegistryEndpoint"))

            val runner = JsExtensionUpdateRunner(
                repository = repository,
                discovery = discovery,
                loader = loader,
                updateChecker = updateChecker,
                settings = settings,
                registryEndpoint = registryEndpoint,
                onUpdateAvailable = { update ->
                    val notification = NotificationCompat.Builder(applicationContext, JS_EXTENSION_UPDATE_CHANNEL_ID)
                        .setContentTitle("${update.id} has an update!")
                        .setContentText("${update.latestVersion} is available.")
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .build()
                    notificationManager?.notify(update.id.hashCode(), notification)
                },
            )
            runner.run()
            Result.success()
        } catch (e: Exception) {
            Result.success()
        }
    }

    companion object {
        const val JS_EXTENSION_UPDATE_CHANNEL_ID = "jsExtensionUpdateChannel"
    }
}
```

- [ ] **Step 7: Implement the WorkManager scheduling helper**

Create `sharedutils/jsextensionloader/src/androidMain/kotlin/com/programmersbox/jsextensionloader/JsExtensionUpdateScheduler.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object JsExtensionUpdateScheduler {

    private const val UNIQUE_WORK_NAME = "jsExtensionChecks"

    fun schedule(workManager: WorkManager) {
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<JsExtensionUpdateWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        )
    }

    fun cancel(workManager: WorkManager) {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
```

- [ ] **Step 8: Implement the Koin module (provided for a future app to load — not invoked by this plan)**

Create `sharedutils/jsextensionloader/src/androidMain/kotlin/com/programmersbox/jsextensionloader/JsExtensionLoaderModule.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import org.koin.dsl.module

/**
 * Not loaded by this plan. A consuming app wires this in with
 * `loadKoinModules(jsExtensionLoaderModule)` plus `JsExtensionUpdateScheduler.schedule(...)`
 * once it decides to integrate JS/TS extensions — that integration is out of scope here.
 */
val jsExtensionLoaderModule = module {
    single { HttpClient(Android) }
    single { KtorHostBridge(get()) }
    single<HostBridge> { get<KtorHostBridge>() }
    single { JSExtensionLoader(get()) }
    single { JsExtensionRepository() }
    single { ExtensionUpdateChecker(get()) }
    single { JsExtensionUpdateSettings() }
    single {
        ExtensionDiscovery(
            context = get(),
            extensionsSubDir = "js_extensions",
            bundledAssetsDir = "js_extensions",
            client = get(),
        )
    }
}
```

- [ ] **Step 9: Compile-check the Android source set**

Run: `./gradlew :sharedutils:jsextensionloader:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: Commit**

```bash
git add sharedutils/jsextensionloader
git commit -m "feat: add JsExtensionUpdateRunner, Android update worker, WorkManager scheduler, and Koin module"
```

---

### Task 12: JVM/iOS coroutine-ticker scheduler

**Files:**
- Create: `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/CoroutineExtensionUpdateScheduler.kt`
- Test: `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/CoroutineExtensionUpdateSchedulerTest.kt`

**Interfaces:**
- Consumes: `ExtensionUpdateMode`, `JsExtensionUpdateSettings` (Task 10)
- Produces: `CoroutineExtensionUpdateScheduler(scope, checkInterval, settings, onCheck).start()/.stop()` — for a future JVM/iOS app entry point (e.g. `DesktopUi.kt`'s startup routine) to call alongside a `JsExtensionUpdateRunner`; not wired into any app by this plan, matching Task 11's Android scheduler.

- [ ] **Step 1: Write the failing test**

Create `sharedutils/jsextensionloader/src/jvmTest/kotlin/com/programmersbox/jsextensionloader/CoroutineExtensionUpdateSchedulerTest.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import com.programmersbox.datastore.DataStoreHandling
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class CoroutineExtensionUpdateSchedulerTest {

    @Test
    fun ticksOncePerIntervalWhileEnabled() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling()).apply { setMode(ExtensionUpdateMode.NOTIFY) }
        var checkCount = 0
        val scheduler = CoroutineExtensionUpdateScheduler(
            scope = TestScope(testScheduler),
            checkInterval = 24.hours,
            settings = settings,
            onCheck = { checkCount++ },
        )

        scheduler.start()
        advanceTimeBy(24.hours.inWholeMilliseconds + 1_000)
        advanceTimeBy(24.hours.inWholeMilliseconds + 1_000)
        scheduler.stop()

        assertEquals(2, checkCount)
    }

    @Test
    fun doesNotCheckWhenDisabled() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling()).apply { setMode(ExtensionUpdateMode.DISABLED) }
        var checkCount = 0
        val scheduler = CoroutineExtensionUpdateScheduler(
            scope = TestScope(testScheduler),
            checkInterval = 24.hours,
            settings = settings,
            onCheck = { checkCount++ },
        )

        scheduler.start()
        advanceTimeBy(24.hours.inWholeMilliseconds + 1_000)
        scheduler.stop()

        assertEquals(0, checkCount)
    }

    @Test
    fun stopCancelsFurtherChecks() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling()).apply { setMode(ExtensionUpdateMode.NOTIFY) }
        var checkCount = 0
        val scheduler = CoroutineExtensionUpdateScheduler(
            scope = TestScope(testScheduler),
            checkInterval = 24.hours,
            settings = settings,
            onCheck = { checkCount++ },
        )

        scheduler.start()
        scheduler.stop()
        advanceTimeBy(48.hours.inWholeMilliseconds)

        assertEquals(0, checkCount)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: FAIL — `CoroutineExtensionUpdateScheduler` unresolved.

- [ ] **Step 3: Implement `CoroutineExtensionUpdateScheduler`**

Create `sharedutils/jsextensionloader/src/commonMain/kotlin/com/programmersbox/jsextensionloader/CoroutineExtensionUpdateScheduler.kt`:

```kotlin
package com.programmersbox.jsextensionloader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class CoroutineExtensionUpdateScheduler(
    private val scope: CoroutineScope,
    private val checkInterval: Duration = 24.hours,
    private val settings: JsExtensionUpdateSettings,
    private val onCheck: suspend () -> Unit,
) {
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch {
            while (true) {
                delay(checkInterval)
                if (settings.getMode() != ExtensionUpdateMode.DISABLED) {
                    onCheck()
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :sharedutils:jsextensionloader:jvmTest`
Expected: PASS (3 new tests, 45 total)

- [ ] **Step 5: Compile-check iOS**

Run: `./gradlew :sharedutils:jsextensionloader:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add sharedutils/jsextensionloader
git commit -m "feat: add JVM/iOS coroutine-ticker scheduler for extension update checks"
```

---

## Final verification

- [ ] Run the full new-module test suite: `./gradlew :kmpmodels:extensioninterfaces:jvmTest :sharedutils:jsextensionloader:jvmTest` — expect all ~45 tests passing.
- [ ] Run a full Android compile of the new modules: `./gradlew :sharedutils:jsextensionloader:compileDebugKotlinAndroid :kmpmodels:extensioninterfaces:compileDebugKotlinAndroid`
- [ ] Run a full iOS compile of the new modules: `./gradlew :sharedutils:jsextensionloader:compileKotlinIosSimulatorArm64 :kmpmodels:extensioninterfaces:compileKotlinIosSimulatorArm64`
- [ ] Confirm no existing module was modified other than `settings.gradle.kts`, `gradle/common.versions.toml`, and `datastore/.../DataStoreHandling.kt`: `git diff --stat develop...HEAD` should show only new files under `kmpmodels/extensioninterfaces/`, `sharedutils/jsextensionloader/`, plus those three modified files.
