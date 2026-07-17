# JS Extension → SourceRepository Bridge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make JS/TS extensions loaded via `JsExtensionRepository` appear seamlessly in `SourceRepository` — the same repository real JAR/APK sources and `ExampleService` use — so a JS extension is indistinguishable from a normal source in the existing "Installed Extensions" screen and everywhere else `SourceRepository` is consumed.

**Architecture:** `JsApiServiceAdapter` wraps one `JsExtension` and implements `KmpApiService` by mapping its two-phase request/parse calls onto the legacy suspend-function contract. `JsExtensionSourceBridge` reactively mirrors `JsExtensionRepository.extensions` into `SourceRepository`, diffing by extension id *and* instance identity so an auto-update reload (same id, new instance) correctly swaps rather than leaving a stale closed `JsExtension` visible.

**Tech Stack:** Kotlin Multiplatform (`kmpuiviews` commonMain), Koin, kotlinx.coroutines (`StateFlow`/`onEach`/`launchIn`), `app.cash.zipline:zipline` (`QuickJs`, via the already-built `jsextensionloader` module).

## Global Constraints

- No changes to `kmpmodels:extensioninterfaces` or `sharedutils/jsextensionloader` — this bridge is purely additive, built entirely in `kmpuiviews`, which already depends on both.
- No changes to any UI screen (`ExtensionListScreen.kt` etc.) — only `SourceRepository`'s contents change; the UI already renders whatever's in there.
- `recent()` maps to `getLatest()`, `allList()` maps to `getPopular()` (not the reverse).
- `KmpSourceInformation.icon` is a drawable resource `Int?` — always `null` for JS extensions (they can't supply a compile-time resource id).
- `JsExtensionSourceBridge`'s internal `CoroutineScope` must be a constructor parameter with a real-world default (`CoroutineScope(SupervisorJob() + Dispatchers.Default)`), overridable in tests — do not hardcode an internal scope that can't be swapped for a `TestScope`.
- `JsExtensionSourceBridge` must NOT be registered `createdAtStart` — it's a normal lazy Koin `single`, constructed on first injection (per explicit decision).
- Every add/remove against `SourceRepository` inside the bridge's sync logic must be wrapped in `runCatching` so one failing extension doesn't stop others from syncing.

---

### Task 1: `JsApiServiceAdapter`

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/JsApiServiceAdapter.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/JsApiServiceAdapterTest.kt`

**Interfaces:**
- Consumes: `JsExtension` (`sharedutils/jsextensionloader`, constructor `JsExtension(manifest: ExtensionManifest, quickJs: QuickJs, hostBridge: HostBridge)`, methods `getPopular(page)`, `getLatest(page)`, `search(query, page)`, `getDetail(url)`, `getContent(url)`), `KmpApiService`/`KmpItemModel`/`KmpInfoModel`/`KmpChapterModel`/`KmpStorage` (`kmpmodels`, already in this repo)
- Produces: `JsApiServiceAdapter(jsExtension: JsExtension) : KmpApiService` — consumed by Task 2 (`JsExtensionSourceBridge`)

- [ ] **Step 1: Write the failing tests**

Create `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/JsApiServiceAdapterTest.kt`:

```kotlin
package com.programmersbox.kmpuiviews.repository

import app.cash.zipline.QuickJs
import com.programmersbox.extensioninterfaces.ExtensionManifest
import com.programmersbox.jsextensionloader.HostBridge
import com.programmersbox.jsextensionloader.JsExtension
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JsApiServiceAdapterTest {

    private class StubHostBridge(private val response: String = "") : HostBridge {
        override fun httpGet(url: String, headersJson: String): String = response
    }

    private val manifest = ExtensionManifest(
        id = "adapter-test",
        name = "Adapter Test Extension",
        version = "1.0.0",
        author = null,
        description = null,
        iconUrl = null,
        updateUrl = null,
    )

    private var quickJs: QuickJs? = null

    @AfterTest
    fun tearDown() {
        quickJs?.close()
    }

    private fun buildAdapter(): JsApiServiceAdapter {
        val js = QuickJs.create()
        quickJs = js
        js.evaluate(FIXTURE_SCRIPT, "adapter-fixture.js")
        return JsApiServiceAdapter(JsExtension(manifest, js, StubHostBridge()))
    }

    @Test
    fun recentMapsGetLatestIntoKmpItemModel() = runTest {
        val adapter = buildAdapter()
        val result = adapter.recent(page = 1)
        assertEquals(1, result.size)
        assertEquals("Latest Item", result.first().title)
        assertEquals("", result.first().description)
        assertEquals(adapter, result.first().source)
    }

    @Test
    fun allListMapsGetPopularIntoKmpItemModel() = runTest {
        val adapter = buildAdapter()
        val result = adapter.allList(page = 1)
        assertEquals("Popular Item", result.first().title)
    }

    @Test
    fun searchCallsExtensionSearchDirectly() = runTest {
        val adapter = buildAdapter()
        val result = adapter.search("dragon", page = 1, list = emptyList())
        assertEquals("Search Result for dragon", result.first().title)
    }

    @Test
    fun itemInfoMapsGetDetailIntoKmpInfoModelWithChapters() = runTest {
        val adapter = buildAdapter()
        val item = adapter.recent(page = 1).first()
        val info = adapter.itemInfo(item)
        assertEquals("Detail Title", info.title)
        assertEquals(1, info.chapters.size)
        assertEquals("Chapter 1", info.chapters.first().name)
        assertEquals(item.url, info.chapters.first().sourceUrl)
        assertEquals(adapter, info.chapters.first().source)
    }

    @Test
    fun chapterInfoMapsGetContentIntoKmpStorageWithHeaders() = runTest {
        val adapter = buildAdapter()
        val chapter = adapter.itemInfo(adapter.recent(page = 1).first()).chapters.first()
        val storages = adapter.chapterInfo(chapter)
        assertEquals(1, storages.size)
        assertEquals("https://example.com/content/1.png", storages.first().link)
        assertEquals("1.png", storages.first().filename)
        assertEquals("bar", storages.first().headers["foo"])
    }

    @Test
    fun sourceByUrlMapsGetDetailIntoKmpItemModel() = runTest {
        val adapter = buildAdapter()
        val result = adapter.sourceByUrl("https://example.com/detail")
        assertEquals("Detail Title", result.title)
        assertEquals("https://example.com/detail", result.url)
    }

    @Test
    fun baseUrlIsSyntheticAndDerivedFromManifestId() {
        val adapter = buildAdapter()
        assertEquals("https://adapter-test.jsextension/", adapter.baseUrl)
    }

    @Test
    fun serviceNameUsesManifestName() {
        val adapter = buildAdapter()
        assertEquals("Adapter Test Extension", adapter.serviceName)
    }

    companion object {
        private const val FIXTURE_SCRIPT = """
            function getPopularRequest(page) { return { url: "https://example.com/popular", headers: {} }; }
            function getPopularParse(page, responseBody) {
                return [{ title: "Popular Item", url: "https://example.com/popular/1", imageUrl: null }];
            }
            function getLatestRequest(page) { return { url: "https://example.com/latest", headers: {} }; }
            function getLatestParse(page, responseBody) {
                return [{ title: "Latest Item", url: "https://example.com/latest/1", imageUrl: null }];
            }
            function searchRequest(query, page) { return { url: "https://example.com/search", headers: {} }; }
            function searchParse(query, page, responseBody) {
                return [{ title: "Search Result for " + query, url: "https://example.com/search/1", imageUrl: null }];
            }
            function getDetailRequest(url) { return { url: url, headers: {} }; }
            function getDetailParse(url, responseBody) {
                return {
                    title: "Detail Title",
                    url: url,
                    imageUrl: null,
                    description: null,
                    genres: [],
                    chapters: [ { name: "Chapter 1", url: "https://example.com/chapter/1", uploaded: null } ]
                };
            }
            function getContentRequest(url) { return { url: url, headers: {} }; }
            function getContentParse(url, responseBody) {
                return { urls: ["https://example.com/content/1.png"], headers: { foo: "bar" } };
            }
        """
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.repository.JsApiServiceAdapterTest"`
Expected: FAIL — `JsApiServiceAdapter` unresolved.

- [ ] **Step 3: Implement `JsApiServiceAdapter`**

Create `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/JsApiServiceAdapter.kt`:

```kotlin
package com.programmersbox.kmpuiviews.repository

import com.programmersbox.extensioninterfaces.ExtensionChapter
import com.programmersbox.extensioninterfaces.ExtensionItem
import com.programmersbox.jsextensionloader.JsExtension
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage

/**
 * Wraps a [JsExtension] so it can be registered into the legacy [SourceRepository]
 * alongside real JAR/APK sources. Each JS extension operation is two-phase
 * (request/parse) internally — that's fully hidden here; this class only maps
 * shapes, no networking of its own.
 */
class JsApiServiceAdapter(private val jsExtension: JsExtension) : KmpApiService {

    override val baseUrl: String = "https://${jsExtension.manifest.id}.jsextension/"
    override val canScroll: Boolean = true
    override val serviceName: String get() = jsExtension.manifest.name

    override suspend fun recent(page: Int): List<KmpItemModel> =
        jsExtension.getLatest(page).map { it.toKmpItemModel() }

    override suspend fun allList(page: Int): List<KmpItemModel> =
        jsExtension.getPopular(page).map { it.toKmpItemModel() }

    override suspend fun itemInfo(model: KmpItemModel): KmpInfoModel {
        val detail = jsExtension.getDetail(model.url)
        return KmpInfoModel(
            title = detail.title,
            description = detail.description.orEmpty(),
            url = detail.url,
            imageUrl = detail.imageUrl.orEmpty(),
            chapters = detail.chapters.map { it.toKmpChapterModel(sourceUrl = detail.url) },
            genres = detail.genres,
            alternativeNames = emptyList(),
            source = this,
        )
    }

    override suspend fun chapterInfo(chapterModel: KmpChapterModel): List<KmpStorage> {
        val content = jsExtension.getContent(chapterModel.url)
        return content.urls.map { url ->
            KmpStorage(
                source = serviceName,
                link = url,
                quality = "Default",
                filename = url.substringAfterLast("/"),
            ).apply { headers.putAll(content.headers) }
        }
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<KmpItemModel>): List<KmpItemModel> =
        jsExtension.search(searchText.toString(), page).map { it.toKmpItemModel() }

    override suspend fun sourceByUrl(url: String): KmpItemModel {
        val detail = jsExtension.getDetail(url)
        return KmpItemModel(
            title = detail.title,
            description = detail.description.orEmpty(),
            url = detail.url,
            imageUrl = detail.imageUrl.orEmpty(),
            source = this,
        )
    }

    private fun ExtensionItem.toKmpItemModel() = KmpItemModel(
        title = title,
        description = "",
        url = url,
        imageUrl = imageUrl.orEmpty(),
        source = this@JsApiServiceAdapter,
    )

    private fun ExtensionChapter.toKmpChapterModel(sourceUrl: String) = KmpChapterModel(
        name = name,
        url = url,
        uploaded = uploaded.orEmpty(),
        sourceUrl = sourceUrl,
        source = this@JsApiServiceAdapter,
    )
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.repository.JsApiServiceAdapterTest"`
Expected: PASS (9 tests)

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/JsApiServiceAdapter.kt kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/JsApiServiceAdapterTest.kt
git commit -m "feat: add JsApiServiceAdapter bridging JsExtension into KmpApiService"
```

---

### Task 2: `JsExtensionSourceBridge`

> **Amendment (post-authoring discovery):** the bridge's `init{}` launches a collector on `jsExtensionRepository.extensions` that never completes by design (a `StateFlow` never finishes). Passing `runTest`'s own scope (`this`) as `scope` in tests makes `runTest` wait for that never-ending coroutine to finish, timing out with `UncompletedCoroutinesError`. `TestScope.backgroundScope` (the usual fix for "launch and don't wait") turned out to desync from the outer `advanceUntilIdle()` calls in this exact shape, so the working pattern here is `scope = TestScope(testScheduler)` — a second `TestScope` sharing the same `TestCoroutineScheduler` as the enclosing `runTest`, which `advanceUntilIdle()` drives correctly without being awaited for completion. This matches the pattern already used successfully in `CoroutineExtensionUpdateSchedulerTest` from the earlier JS/TS extension loader plan. The test code below already reflects this fix.
>
> Also: `kmpuiviews`'s jvmTest source set has an unrelated, pre-existing compile break (three `dictionary` package test files reference classes that don't exist) that predates this whole branch. To get a genuine test run, temporarily move those three files aside (`mv X X.bak`), run the test, then restore them immediately and confirm via `git status` that no diff remains — do this every time you need to run `:kmpuiviews:jvmTest` until that unrelated issue is fixed separately.

**Files:**
- Create: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/JsExtensionSourceBridge.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/JsExtensionSourceBridgeTest.kt`

**Interfaces:**
- Consumes: `JsApiServiceAdapter` (Task 1), `JsExtensionRepository` (`sharedutils/jsextensionloader`: `extensions: StateFlow<List<JsExtension>>`, `register(extension)`, `unload(id)`), `SourceRepository`/`KmpSourceInformation` (`kmpmodels`)
- Produces: `JsExtensionSourceBridge(jsExtensionRepository, sourceRepository, scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default))` — consumed by Task 3 (Koin wiring)

- [ ] **Step 1: Write the failing tests**

Create `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/JsExtensionSourceBridgeTest.kt`:

```kotlin
package com.programmersbox.kmpuiviews.repository

import app.cash.zipline.QuickJs
import com.programmersbox.extensioninterfaces.ExtensionManifest
import com.programmersbox.jsextensionloader.HostBridge
import com.programmersbox.jsextensionloader.JsExtension
import com.programmersbox.jsextensionloader.JsExtensionRepository
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsExtensionSourceBridgeTest {

    private class StubHostBridge : HostBridge {
        override fun httpGet(url: String, headersJson: String): String = ""
    }

    private val createdQuickJs = mutableListOf<QuickJs>()

    @AfterTest
    fun tearDown() {
        createdQuickJs.forEach { it.close() }
    }

    private fun extensionWithId(id: String, version: String = "1.0.0"): JsExtension {
        val manifest = ExtensionManifest(
            id = id, name = "Extension $id", version = version, author = null,
            description = null, iconUrl = null, updateUrl = null,
        )
        val js = QuickJs.create()
        createdQuickJs.add(js)
        js.evaluate(BRIDGE_FIXTURE_SCRIPT, "$id.js")
        return JsExtension(manifest, js, StubHostBridge())
    }

    @Test
    fun mirrorsNewlyRegisteredExtensionIntoSourceRepository() = runTest {
        val jsExtensionRepository = JsExtensionRepository()
        val sourceRepository = SourceRepository()
        JsExtensionSourceBridge(jsExtensionRepository, sourceRepository, scope = TestScope(testScheduler))

        jsExtensionRepository.register(extensionWithId("a"))
        advanceUntilIdle()

        assertEquals(1, sourceRepository.list.size)
        assertEquals("js.a", sourceRepository.list.first().packageName)
        assertEquals("Extension a", sourceRepository.list.first().apiService.serviceName)
    }

    @Test
    fun removesFromSourceRepositoryWhenUnloaded() = runTest {
        val jsExtensionRepository = JsExtensionRepository()
        val sourceRepository = SourceRepository()
        JsExtensionSourceBridge(jsExtensionRepository, sourceRepository, scope = TestScope(testScheduler))

        jsExtensionRepository.register(extensionWithId("a"))
        advanceUntilIdle()
        jsExtensionRepository.unload("a")
        advanceUntilIdle()

        assertTrue(sourceRepository.list.isEmpty())
    }

    @Test
    fun swapsSourceRepositoryEntryWhenSameIdExtensionIsReplaced() = runTest {
        val jsExtensionRepository = JsExtensionRepository()
        val sourceRepository = SourceRepository()
        JsExtensionSourceBridge(jsExtensionRepository, sourceRepository, scope = TestScope(testScheduler))

        jsExtensionRepository.register(extensionWithId("a", version = "1.0.0"))
        advanceUntilIdle()
        val firstInfo = sourceRepository.list.first()

        jsExtensionRepository.register(extensionWithId("a", version = "2.0.0"))
        advanceUntilIdle()

        assertEquals(1, sourceRepository.list.size)
        val secondInfo = sourceRepository.list.first()
        assertTrue(firstInfo !== secondInfo)
        assertEquals("js.a", secondInfo.packageName)
    }

    @Test
    fun mirrorsMultipleIndependentExtensions() = runTest {
        val jsExtensionRepository = JsExtensionRepository()
        val sourceRepository = SourceRepository()
        JsExtensionSourceBridge(jsExtensionRepository, sourceRepository, scope = TestScope(testScheduler))

        jsExtensionRepository.register(extensionWithId("a"))
        jsExtensionRepository.register(extensionWithId("b"))
        advanceUntilIdle()

        assertEquals(setOf("js.a", "js.b"), sourceRepository.list.map { it.packageName }.toSet())
    }

    companion object {
        private const val BRIDGE_FIXTURE_SCRIPT = """
            function getPopularRequest(page) { return { url: "https://example.com/popular", headers: {} }; }
            function getPopularParse(page, responseBody) { return []; }
            function getLatestRequest(page) { return { url: "https://example.com/latest", headers: {} }; }
            function getLatestParse(page, responseBody) { return []; }
            function searchRequest(query, page) { return { url: "https://example.com/search", headers: {} }; }
            function searchParse(query, page, responseBody) { return []; }
            function getDetailRequest(url) { return { url: url, headers: {} }; }
            function getDetailParse(url, responseBody) {
                return { title: "t", url: url, imageUrl: null, description: null, genres: [], chapters: [] };
            }
            function getContentRequest(url) { return { url: url, headers: {} }; }
            function getContentParse(url, responseBody) { return { urls: [], headers: {} }; }
        """
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.repository.JsExtensionSourceBridgeTest"`
Expected: FAIL — `JsExtensionSourceBridge` unresolved.

- [ ] **Step 3: Implement `JsExtensionSourceBridge`**

Create `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/JsExtensionSourceBridge.kt`:

```kotlin
package com.programmersbox.kmpuiviews.repository

import com.programmersbox.jsextensionloader.JsExtension
import com.programmersbox.jsextensionloader.JsExtensionRepository
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Reactively mirrors [JsExtensionRepository] into [SourceRepository] so JS
 * extensions appear alongside real sources with no other wiring required.
 * Diffs by extension id AND instance identity — an auto-update reload
 * replaces a same-id extension in place, and the mirrored entry must swap to
 * the new instance rather than keep pointing at a closed [JsExtension].
 */
class JsExtensionSourceBridge(
    private val jsExtensionRepository: JsExtensionRepository,
    private val sourceRepository: SourceRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val mirrored = mutableMapOf<String, Pair<JsExtension, KmpSourceInformation>>()

    init {
        jsExtensionRepository.extensions
            .onEach { current -> sync(current) }
            .launchIn(scope)
    }

    private fun sync(current: List<JsExtension>) {
        val currentById = current.associateBy { it.manifest.id }

        (mirrored.keys - currentById.keys).forEach { id ->
            mirrored.remove(id)?.let { (_, info) ->
                runCatching { sourceRepository.removeSource(info) }
            }
        }

        currentById.forEach { (id, extension) ->
            val existing = mirrored[id]
            if (existing == null || existing.first !== extension) {
                runCatching {
                    existing?.let { (_, oldInfo) -> sourceRepository.removeSource(oldInfo) }
                    val info = KmpSourceInformation(
                        apiService = JsApiServiceAdapter(extension),
                        name = extension.manifest.name,
                        icon = null,
                        packageName = "js.${extension.manifest.id}",
                    )
                    sourceRepository.addSource(info)
                    mirrored[id] = extension to info
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.repository.JsExtensionSourceBridgeTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/JsExtensionSourceBridge.kt kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/JsExtensionSourceBridgeTest.kt
git commit -m "feat: add JsExtensionSourceBridge reactively mirroring JsExtensionRepository into SourceRepository"
```

---

### Task 3: Wire the bridge into Koin and both app entry points

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.kt`
- Modify: `UIViews/src/main/java/com/programmersbox/uiviews/BaseMainActivity.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/DesktopUi.kt`

**Interfaces:**
- Consumes: `JsExtensionSourceBridge` (Task 2)
- Produces: nothing further consumes this — it's the final integration point.

- [ ] **Step 1: Register `JsExtensionSourceBridge` as a lazy Koin single**

In `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.kt`, add this import alongside the existing ones:

```kotlin
import com.programmersbox.kmpuiviews.repository.JsExtensionSourceBridge
```

Then add this line inside the `val appModule = module { ... }` block (anywhere among the other `single { }` entries, e.g. right after `singleOf(::AppUpdateCheck)`):

```kotlin
    single { JsExtensionSourceBridge(get(), get()) }
```

This is a normal lazy single (NOT `createdAtStart`) — it's only constructed the first time something injects it, which Step 2/3 below do.

- [ ] **Step 2: Inject it in `BaseMainActivity.kt` (Android)**

In `UIViews/src/main/java/com/programmersbox/uiviews/BaseMainActivity.kt`, add this import:

```kotlin
import com.programmersbox.kmpuiviews.repository.JsExtensionSourceBridge
```

Add this property alongside the other `by inject<...>()` properties (e.g. right after `private val jsExtensionDiscovery by inject<ExtensionDiscovery>()`):

```kotlin
    private val jsExtensionSourceBridge by inject<JsExtensionSourceBridge>()
```

> **Amendment (post-review correction):** `org.koin.android.ext.android.inject`'s `by inject<T>()` returns a `Lazy<T>` — Koin's `single { }` factory only runs the first time the property's *value* is actually read, not merely because the property is declared. Since nothing else in this class references `jsExtensionSourceBridge`, declaring it alone is NOT sufficient — the bridge would silently never be constructed and JS extensions would never appear on Android. Add an explicit forcing read in `onCreate` (unconditional — the mirroring mechanism itself isn't debug-gated, only the bundled example extension's loading is), right after `enableEdgeToEdge()`:
> ```kotlin
>         // Forces Koin to construct this lazy single now, starting its reactive
>         // JsExtensionRepository -> SourceRepository mirroring for the process lifetime.
>         // by inject<T>() only resolves on first access - it is never referenced elsewhere.
>         jsExtensionSourceBridge.let { }
> ```

- [ ] **Step 3: Inject it in `DesktopUi.kt` (JVM)**

In `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/DesktopUi.kt`, add this import:

```kotlin
import com.programmersbox.kmpuiviews.repository.JsExtensionSourceBridge
```

Add this line right after the existing `val jsExtensionDiscovery = koinInject<ExtensionDiscovery>()` line (inside the same `content = { ... }` composable block):

```kotlin
            val jsExtensionSourceBridge = koinInject<JsExtensionSourceBridge>()
```

- [ ] **Step 4: Compile-verify the JVM side**

Run: `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:desktop:compileKotlinJvm`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Run the full `kmpuiviews` jvmTest suite (regression check)**

Run: `./gradlew :kmpuiviews:jvmTest`
Expected: BUILD SUCCESSFUL, all tests pass including the 13 new ones from Tasks 1–2.

- [ ] **Step 6: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.kt UIViews/src/main/java/com/programmersbox/uiviews/BaseMainActivity.kt kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/DesktopUi.kt
git commit -m "feat: wire JsExtensionSourceBridge into Koin and both app entry points"
```

## Final verification

- [ ] `./gradlew :kmpuiviews:jvmTest` — all tests pass (Tasks 1–2's 13 new tests plus all pre-existing ones).
- [ ] `./gradlew :kmpuiviews:compileKotlinJvm :mangaworld:desktop:compileKotlinJvm` — BUILD SUCCESSFUL.
- [ ] Android compile (`BaseMainActivity.kt`) cannot be verified in this environment (no Android SDK configured — a pre-existing, already-documented limitation from the parent plan). The edit mirrors the exact `by inject<T>()` pattern already used for every other property in that class, so risk is low; note this limitation rather than treating it as a blocker.
