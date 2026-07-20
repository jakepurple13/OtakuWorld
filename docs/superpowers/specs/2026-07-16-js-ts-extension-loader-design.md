# JS/TS Extension Loader — Design

## Goal

Add a JavaScript/TypeScript extension loading system that coexists alongside the existing
JAR/APK loader (`sharedutils/kmpextensionloader`). Extensions written in JS/TS can be loaded,
validated, executed, hot-reloaded, and auto-updated at runtime on Android, Desktop/JVM, and iOS.
No changes to the existing JAR/APK loader, `KmpApiService`, `SourceRepository`, or any UI.

## Out of scope

- Refactoring the existing JAR/APK loader
- Extension management UI (browse/install/enable/disable)
- Web-based extension authoring IDE
- Update rollback
- Server-side hosting for the centralized registry (client integration only)
- Custom native iOS JS bridging beyond what Zipline/QuickJs provides
- Extension signing/certificates/DRM

## Module layout

```
kmpmodels/extensioninterfaces      (otaku-multiplatform; commonMain only, no JS engine dep)
  Extension                        — contract: getPopular/getLatest/search/getDetail/getContent
  ExtensionItem / ExtensionDetail / ExtensionChapter / ExtensionContent  — new lightweight models
  ExtensionManifest                — id/name/version/author/description/iconUrl/updateUrl/sourceType
  ExtensionUpdateInfo              — {id, latestVersion, downloadUrl, changelog?}

sharedutils/jsextensionloader      (otaku-multiplatform: android/jvm/ios)
  JSExtensionLoader                — discovers, loads, validates, hot-swaps JS/TS extensions
  JsExtension                      — Extension impl backed by a QuickJs instance
  HostBridge                       — the ONLY functions exposed into the JS sandbox (e.g. httpGet)
  ExtensionDiscovery (expect/actual) — local dir scan, remote URL fetch, bundled resource scan
  ManifestParser                   — header-comment or companion manifest.json extraction
  TsTranspiler                     — strip-types JS transpiler bundle run inside the engine at load
  JsExtensionRepository            — in-memory registry, MutableStateFlow<List<JsExtension>>
  ExtensionUpdateChecker           — merges centralized-registry + per-extension updateUrl checks
  ExtensionUpdateScheduler (expect/actual) — Android: WorkManager periodic; JVM/iOS: coroutine ticker
```

Neither module touches `kmpmodels` core, `kmpextensionloader`, `SourceRepository`, or app UI.

## Engine choice: Zipline's `QuickJs` (low-level API)

`app.cash.zipline:zipline` ships a low-level `QuickJs` class (raw JS eval + host-function binding)
in addition to its higher-level Kotlin/JS-bundle `Zipline` loader. We use only the low-level class,
since our input is plain JS/TS text (possibly third-party, untrusted), not Kotlin/JS compiler output.

Reasons over alternatives:
- **GraalJS**: no real Android support, no iOS — fails the cross-platform requirement outright.
- **Raw QuickJS via hand-rolled JNI/cinterop**: works but means owning bridging/sandboxing/memory
  management ourselves across three platforms — much larger surface for bugs, duplicates work
  Zipline already does.
- **Zipline**: already ships `android`, `jvm`, `iosArm64`, `iosSimulatorArm64` targets — the exact
  set the project's `otaku-multiplatform` plugin builds for. Sandboxing is structural: a fresh
  `QuickJs` instance has zero ambient FS/network access unless the host explicitly binds a
  function into it.

## Sandboxing

**Amendment:** the real `app.cash.zipline:zipline` `QuickJs` class has no `.set()`/`.get()`
host-function-binding API (confirmed via `javap` against the resolved artifact) — only
`evaluate()`, `compile()`, `execute()`, `close()`. Zipline's real live-bridge mechanism
(`Zipline.bind`/`take`) requires the `zipline-kotlin-plugin` compiler plugin and
`ZiplineService`-adapter codegen on both sides, which assumes Kotlin/JS-compiled code on the JS
side — it does not fit arbitrary hand-written third-party JS/TS extension text.

Instead, each extension operation splits into a pure **request** function (returns `{url,
headers}` as JSON — no networking) and a pure **parse** function (turns an already-fetched
response body into the result). `JsExtension` calls the request function via `quickJs.evaluate()`,
then calls `HostBridge.httpGet` as a **plain Kotlin method** (no sandbox binding — `JsExtension`
already runs in Kotlin, so this needs no bridge at all), then calls the parse function via another
`quickJs.evaluate()` with the fetched body as an argument. No `fetch`, no filesystem, no other
globals are ever exposed to the sandbox, and the sandbox never calls back into Kotlin
mid-execution — a *stronger* sandbox guarantee than a live binding would have been, and one that
"extension API is the only bridge" holds by construction, not policy. The public Kotlin-facing
`Extension` interface below is unaffected by this — only the JS-facing authoring contract changes
(a `Request`/`Parse` function pair per operation instead of one function).

## Extension contract & models (`kmpmodels:extensioninterfaces`)

```kotlin
interface Extension {
    val manifest: ExtensionManifest
    suspend fun getPopular(page: Int): List<ExtensionItem>
    suspend fun getLatest(page: Int): List<ExtensionItem>
    suspend fun search(query: String, page: Int): List<ExtensionItem>
    suspend fun getDetail(url: String): ExtensionDetail
    suspend fun getContent(url: String): ExtensionContent
}

data class ExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String?,
    val description: String?,
    val iconUrl: String?,
    val updateUrl: String?,
    val sourceType: String,
)

data class ExtensionItem(val title: String, val url: String, val imageUrl: String?)

data class ExtensionDetail(
    val title: String,
    val url: String,
    val imageUrl: String?,
    val description: String?,
    val genres: List<String>,
    val chapters: List<ExtensionChapter>,
)

data class ExtensionChapter(val name: String, val url: String, val uploaded: String?)

data class ExtensionContent(val urls: List<String>, val headers: Map<String, String> = emptyMap())
```

Models intentionally carry no `source: Extension` back-reference (unlike legacy `KmpItemModel`,
whose `source: KmpApiService` field ties every model to the old interface). This keeps the new
contract generic enough that a future JAR/APK-side implementation could adopt it too, without
requiring any change to `kmpmodels` core or the legacy loader today.

## JSExtensionLoader mechanics

**Discovery** (`expect class ExtensionDiscovery`) — three producers of raw
`(scriptText, manifestJson?, sourceId)` tuples:
- local filesystem dir scan (Android: app-specific dir; JVM: configurable dir, mirroring
  `MangaDesktopSettings.extensionDirectory`; iOS: `NSFileManager` documents dir)
- remote URL download (Ktor `HttpClient`, same `Json { ignoreUnknownKeys = true; isLenient = true }`
  idiom used by `OtakuWorldCatalog`)
- bundled app resources (Android assets / JVM classpath resource / iOS bundle resource)

**Metadata parsing** (`ManifestParser`) — a companion `manifest.json` next to the `.js`/`.ts` file
takes precedence; otherwise parse a leading comment block (`// name: ...`, `// version: ...`,
`// author: ...`, `// description: ...`, `// iconUrl: ...`, `// updateUrl: ...`, one key per line,
case-insensitive key match) up to the first non-comment line.

**TypeScript handling** (`TsTranspiler`) — if the source file is `.ts`, run it through a bundled
strip-types-only transpiler (Sucrase-style: removes type annotations/interfaces, no type-checking)
loaded once into the `QuickJs` instance and cached across loads, before eval. Type safety for
extension authors comes from the `.d.ts` declaration file used in their own editor, not from
on-device checking.

**Load + validate** — eval the transpiled JS in a fresh `QuickJs` instance, then probe
(`typeof getPopular === 'function'`, etc.) for all five required functions
(`getPopular`, `getLatest`, `search`, `getDetail`, `getContent`). Any missing function →
`ExtensionValidationException(missing: List<String>)`, extension rejected, nothing registered.

**JsExtension** — wraps the validated `QuickJs` instance + `ExtensionManifest`, implements
`Extension` by calling into the JS functions and marshalling their JSON return values into
`ExtensionItem`/`ExtensionDetail`/`ExtensionChapter`/`ExtensionContent` via kotlinx.serialization.

**JsExtensionRepository** — `MutableStateFlow<List<JsExtension>>`, `load(source)`, `unload(id)`
(disposes the `QuickJs` instance, releases native resources, drops from the flow). Standalone
registry, not wired into the existing `SourceRepository` or any UI (out of scope).

## Auto-update system

- `ExtensionUpdateSource` sealed interface:
  - `CentralizedRegistry(endpoint: String)` — one Ktor GET returns `List<ExtensionUpdateInfo>` for
    the whole registry, mirroring `OtakuWorldCatalog`'s `index.min.json` shape.
  - `PerExtensionUrl(url: String)` — one Ktor GET per extension whose manifest declares
    `updateUrl`, returns a single `ExtensionUpdateInfo`.
- `ExtensionUpdateChecker` — queries the centralized registry once, and for every loaded extension
  whose `updateUrl` isn't covered by the registry response, queries individually. Version
  comparison reuses the existing `AppUpdate.checkForUpdate` (not reimplemented).
- Settings — flat `DataStoreHandling` key `jsExtensionUpdateMode`
  (enum `AUTOMATIC` / `NOTIFY` / `DISABLED`, default `NOTIFY`). Check interval is a hardcoded daily
  constant — only the mode is user-configurable per spec.
- Behavior:
  - `AUTOMATIC` — silently re-download and reload the extension via `JSExtensionLoader`.
  - `NOTIFY` — fire a local notification per outdated extension (same pattern as
    `SourceUpdateChecker`), no auto-install.
  - `DISABLED` — scheduler doesn't run checks at all.
- Scheduling (`expect class ExtensionUpdateScheduler`):
  - Android — WorkManager daily periodic unique job, alongside the existing `"sourceChecks"` job,
    following the `BackgroundWorkHandlerImpl` convention.
  - JVM/iOS — in-process coroutine ticker (`while (true) { delay(24h); check() }` from app scope).
    No OS-level background scheduling on these platforms, consistent with the codebase's existing
    lack of a background-job primitive there.

## Testing

- Manifest/header parsing — both companion-JSON and comment-header modes, including precedence
  when both are present.
- Function-signature validation — missing-function rejection produces the right
  `ExtensionValidationException`.
- TS strip-transpile output — sample `.ts` input transpiles to expected `.js` shape.
- `Extension` marshalling — fixture JS extension's five functions produce correctly-parsed
  `ExtensionItem`/`ExtensionDetail`/etc.
- `ExtensionUpdateChecker` — fake `CentralizedRegistry` and `PerExtensionUrl` responses, verifying
  `AppUpdate.checkForUpdate` reuse and correct merge behavior across both sources.

## Sample extension

A fixture JS extension ships in `sharedutils/jsextensionloader` test resources with a full metadata
comment header and stubbed implementations of all five required functions. Serves as both the test
fixture and the reference example for extension authors.

## `.d.ts` declaration file

A hand-written `.d.ts` file defines the full extension API contract (`getPopular`, `getLatest`,
`search`, `getDetail`, `getContent` signatures, parameter and return types matching
`ExtensionItem`/`ExtensionDetail`/etc.) shipped alongside the sample extension, for extension
authors' editor type-checking and autocomplete. Not consumed on-device.
