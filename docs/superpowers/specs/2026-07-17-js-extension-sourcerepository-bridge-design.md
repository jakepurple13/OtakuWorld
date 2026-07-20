# JS Extension → SourceRepository Bridge — Design

## Goal

Make JS/TS extensions loaded via `JsExtensionRepository` (see
`2026-07-16-js-ts-extension-loader-design.md`) show up seamlessly in the existing
`SourceRepository` — the same repository real JAR/APK sources and `ExampleService` use, which
feeds the "Installed Extensions" settings screen. A JS extension should be indistinguishable from
a normal source once loaded: same list, same detail/read/download flow, no special-casing in the
UI layer.

## Out of scope

- Any change to `kmpmodels:extensioninterfaces` or `sharedutils/jsextensionloader` themselves —
  both stay exactly as built; this bridge is purely additive, living downstream of both.
- Any change to the "Installed Extensions" screen or other UI — the bridge only affects what data
  reaches `SourceRepository`; the UI already knows how to render whatever's in there.
- Bridging in the other direction (making legacy JAR/APK sources loadable through the JS pipeline)
  — not requested, not needed.

## Architecture

Two new commonMain classes in `kmpuiviews` (the natural home: it already depends on both
`kmpmodels` and `jsextensionloader`, so this keeps those two lower modules mutually decoupled as
originally designed):

```
kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/
  JsApiServiceAdapter.kt   — KmpApiService implementation wrapping one JsExtension
  JsExtensionSourceBridge.kt — reactive mirror: JsExtensionRepository -> SourceRepository
```

## `JsApiServiceAdapter`

```kotlin
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
            chapters = detail.chapters.map { it.toKmpChapterModel(sourceUrl = detail.url, source = this) },
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
        title = title, description = "", url = url, imageUrl = imageUrl.orEmpty(), source = this@JsApiServiceAdapter,
    )

    private fun ExtensionChapter.toKmpChapterModel(sourceUrl: String, source: KmpApiService) = KmpChapterModel(
        name = name, url = url, uploaded = uploaded.orEmpty(), sourceUrl = sourceUrl, source = source,
    )
}
```

Notes:
- `recent → getLatest`, `allList → getPopular` per the chosen mapping.
- `search` overrides the interface's default (which just filters an already-fetched `list`
  locally) with a real host-mediated search call, since extensions can do that.
- `baseUrl` is synthetic (`https://<manifest.id>.jsextension/`) — JS extensions have no
  compile-time-known domain. `icon` is always `null` in the `KmpSourceInformation` this adapter
  gets wrapped in (it's a drawable resource `Int?`, which a dynamic extension can't supply).
- Flow-returning `KmpApiService` members (`getRecentFlow`, `getItemInfoFlow`, etc.) are NOT
  overridden — the interface's default implementations already wrap the suspend functions above
  in `flowOn(Dispatchers.IO)` plus error catching, so they come for free.
- Exceptions from `jsExtension.*` calls propagate uncaught, matching how the interface's own
  default `itemInfo`/`chapterInfo` bodies behave (no try/catch) — callers already go through the
  Flow wrappers for exception handling.

## `JsExtensionSourceBridge`

```kotlin
class JsExtensionSourceBridge(
    private val jsExtensionRepository: JsExtensionRepository,
    private val sourceRepository: SourceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mirrored = mutableMapOf<String, Pair<JsExtension, KmpSourceInformation>>()

    init {
        jsExtensionRepository.extensions
            .onEach { current -> sync(current) }
            .launchIn(scope)
    }

    private fun sync(current: List<JsExtension>) {
        val currentById = current.associateBy { it.manifest.id }

        // Remove entries whose extension disappeared entirely.
        (mirrored.keys - currentById.keys).forEach { id ->
            mirrored.remove(id)?.let { (_, info) ->
                runCatching { sourceRepository.removeSource(info) }
            }
        }

        // Add new extensions, and swap any whose underlying instance changed
        // (e.g. an auto-update reload replaced a same-id extension).
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

Diffing is keyed by `manifest.id` but also checks instance identity (`!==`), not just id
membership — `JsExtensionRepository.register()` replaces a same-id extension in place during
auto-update reloads, and the bridge must swap to the new instance rather than leave
`SourceRepository` pointing at a closed `JsExtension` (whose `QuickJs` handle has already been
closed by the reload). Each add/remove is wrapped in `runCatching` so one bad extension can't stop
the rest of the sync from proceeding.

## Wiring

Registered as a normal (lazy) Koin `single` in `kmpuiviews`'s common module — not
`createdAtStart`, per explicit decision. Since Koin only constructs a lazy single on first access,
one injection line is added at each app's entry point to trigger construction:

- `BaseMainActivity.kt` (Android): `private val jsExtensionSourceBridge by inject<JsExtensionSourceBridge>()`
- `DesktopUi.kt` (JVM): `val jsExtensionSourceBridge = koinInject<JsExtensionSourceBridge>()`

No other change to either file — both already call `jsExtensionRepository.register(...)` for the
bundled example extension (see `2026-07-17`'s prior wiring commit), and the bridge picks that up
automatically from here on, on both Android (debug-only, since the example is only loaded under
`BuildConfig.DEBUG`) and Desktop (always, since the example is always loaded there).

## Testing

Unit tests in `kmpuiviews`'s `jvmTest`:
- `JsApiServiceAdapter` — each mapping function (`recent`/`allList`/`itemInfo`/`chapterInfo`/
  `search`/`sourceByUrl`), against a fake `Extension`/`JsExtension`-shaped stub returning known
  values, asserting the exact field mapping (including nullable → empty-string defaults).
- `JsExtensionSourceBridge` — diff logic against fake `JsExtensionRepository`/`SourceRepository`
  doubles: add-on-new-id, remove-on-disappearance, swap-on-same-id-different-instance, and that a
  `runCatching` failure on one extension doesn't prevent others from syncing.
