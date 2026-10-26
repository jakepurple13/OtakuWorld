# AnimeWorld Desktop — CloudStream Plugins Implementation Plan (Plan 3 of 3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AnimeWorld Desktop adds CloudStream repos, installs/trusts/updates/uninstalls `.cs3` plugins, browses their sources and plays/downloads their episodes with the desktop player from Plan 2 — Android unchanged.

**Architecture:** Mirror what Plan 1 did for Aniyomi. `source_utilities/cloudstream` gains a `jvm()` target and a `src/jvmAndroidShared` source dir (added to androidMain and jvmMain) holding the vendored CloudStream `library` and the platform-neutral glue. The few Android-only files get same-name, same-signature JVM versions in `src/jvmMain` (file names must match: plugins link to Kotlin file-facade classes such as `Coroutines_androidKt`). JVM plugin loading converts the `.cs3`'s `classes.dex` with the existing `DexConverter` and loads it through `JarLoaderCache`/`ChildFirstClassLoader` with CloudStream's host packages parent-first. Missing `android.*`/`androidx.*`/material types that real plugins link against are found with an opt-in scanner (pattern: `mihonsupport/src/jvmTest/.../MihonExtensionScanTest.kt`) and added as stubs in `sharedutils/kmpextensionloader`.

**Tech Stack:** Kotlin Multiplatform, Koin, OkHttp, NiceHttp, Jackson, Rhino, dex2jar (`DexConverter`), ASM (scanner), kotlin.test.

**Spec:** `docs/superpowers/specs/2026-10-23-animeworld-desktop-extensions-design.md` (section 3 "CloudStream on JVM", section 4, non-goals). **Reference:** Plan 1 (`docs/superpowers/plans/2026-10-23-animeworld-desktop-aniyomi.md`) and its commits (`git log --oneline -- source_utilities/aniyomi`), `source_utilities/cloudstream/VENDORED.md`, `source_utilities/cloudstream/shims.md`.

## Global Constraints

- Android behavior unchanged: `ANDROID_HOME=/Users/jacobrein/Library/Android/sdk ./gradlew :source_utilities:cloudstream:testAndroidHostTest :animeworld:assembleNoFirebaseDebug` keep passing; Android keeps `PathClassLoader`, real `Resources`, the settings activity.
- Plugin ABI byte-compatible: every vendored `com.lagradost.*` class keeps package, shape, signatures **and its Kotlin file name** (facade classes `XxxKt` / `Xxx_androidKt` / `Xxx_jvmCommonKt` must exist with the same members on JVM). Every edit marked `OTAKU:` and listed in `VENDORED.md`; update `scripts/vendor-cloudstream.sh` for the new layout.
- One copy of each `android.*`/`androidx.*`/`com.google.android.material.*` stub, in `sharedutils/kmpextensionloader/src/jvmMain`, matching the real SDK shape (class vs interface, supertypes, signatures — `AndroidStubShapeTest` pattern). Real JVM jars are preferred over stubs where they exist and are small (e.g. `androidx.annotation:annotation-jvm`, `com.google.code.gson:gson`).
- Trust identical to Android: plugins load only from repos in `trustedCloudstreamRepos`; otherwise `UntrustedCloudstreamApiService` with the existing trust action.
- Service names unchanged (`cloudstream:<api.name>`, `cloudstream:<internalName>:<api.name>`, `cloudstream-untrusted:<internalName>`).
- Desktop never exposes `ConfigurableCloudstreamApiService` (no settings UI on desktop, spec non-goal).
- WebView/Cloudflare-dependent calls fail with a clear `IOException` (`"…requires a WebView; not supported on desktop yet"`), never crash. `requiresResources` plugins show as a failed placeholder `"Needs Android resources (not supported on desktop)"` unless the loader succeeds without them.
- `animeworld:desktop` must not depend on `mihonsupport`. CloudStream is GPL-3.0 (already in the Android app); no license change.
- Verified via `./gradlew :animeworld:desktop:run` on macOS.

## Review Focus

- A plugin calling `runOnMainThread`/`main {}` from a background thread on desktop must not deadlock or throw (no Android `Looper`; Swing EDT may be absent in tests). → Task 1 test `mainThreadHelpersRunWithoutLooper`.
- Facade classes plugins link against exist on JVM with the same names (e.g. `com.lagradost.cloudstream3.utils.Coroutines_androidKt`, `AppUtilsKt`, `ExtractorApiKt`). → Task 1 test `pluginFacadeClassesExistOnJvm` (list derived from `shims.md` + `scripts/dex-refs.sh` output on the fixtures).
- Reloading the catalog after uninstall/update must unregister the old plugin's `MainAPI`s/extractors from the global `APIHolder` and close its class loader. → Task 3 test `uninstallUnregistersApisAndClosesLoader`.
- A plugin whose `load()` throws must leave no partial registrations and show a failed placeholder. → Task 3 test `throwingPluginLeavesNoRegistrations`.
- A `.cs3` download must never be routed to the APK installer (`DownloadStateRepository`) on desktop. → Task 4 test `cs3LinkClaimedByCatalog`.

---

### Task 1: Compile CloudStream for JVM

**Files:**
- Modify: `source_utilities/cloudstream/build.gradle.kts` — add `jvm()`; `kotlin.srcDir("src/jvmAndroidShared/kotlin")` in androidMain and jvmMain; `src/jvmAndroidSharedTest/kotlin` in androidHostTest and jvmTest; jvmMain deps = the androidMain `api(...)` libs that have JVM artifacts (NiceHttp, Jackson, Rhino, ksoup, cryptography core+provider (JVM provider variant), atomicfu, kotlinx-io, jsoup, okhttp, ktor-core, datetime, serialization, coroutines, kotlin-reflect, gson, `androidx.annotation:annotation-jvm`) + `projects.sharedutils.kmpextensionloader` + `desktopLibs.kotlin.multiplatform.appdirs` + `projects.datastore` + Koin; jvmTest = kotlin.test, coroutines-test, ktorMock, mockwebserver3. Do **not** add appcompat/fragment/material to jvm (Android AARs).
- Move (`git mv`, pure-move commit first) everything under `src/androidMain/kotlin` to `src/jvmAndroidShared/kotlin` **except** the Android-only set: vendored `network/WebViewResolver.android.kt`, `network/CloudflareKiller.kt`, `utils/AppContextUtils.kt`, `CloudStreamApp.kt`, `CommonActivity.kt`, `utils/DataStore.kt`, `plugins/Plugin.kt`, `utils/Coroutines.android.kt`; glue `plugins/PluginLoader.kt`, `CloudstreamShims.kt`, `CloudstreamModule.kt`, `CloudstreamCatalog.kt` (until Task 3 decides), `CloudstreamApiService.kt` (only `ConfigurableCloudstreamApiService` is Android-typed — split it out so the rest moves), `preferences/CloudstreamSettingsActivity.kt`. Move tests that don't need Android to `src/jvmAndroidSharedTest`.
- Create JVM versions in `src/jvmMain/kotlin` **with identical file names, packages and public signatures** for each vendored Android-only file: `WebViewResolver.android.kt` (same classes/overloads incl. `$default`; resolve throws the WebView `IOException`), `CloudflareKiller.kt` (interceptor that throws the same clear `IOException` on a Cloudflare challenge, like Aniyomi's JVM stub), `AppContextUtils.kt` (`html` via jsoup text or `Html` stub, `openBrowser`→`java.awt.Desktop.browse` when supported else no-op, `openWebView` no-op, `isNetworkAvailable`=true, `splitQuery`, `isUsingMobileData`=false, `isAppInstalled`=false — keep every member shims.md lists), `CloudStreamApp.kt` (companion API over the JVM `Context`/`Application` stubs + `FileSharedPreferences`), `CommonActivity.kt` (`showToast*` log to stdout; `activity` null), `DataStore.kt` (on `FileSharedPreferences`), `Plugin.kt` (same shape; `load(context: Context)`), `Coroutines.android.kt` (`runOnMainThreadNative`/`main` dispatch to `Dispatchers.Main` when available, else run inline on a dedicated single-thread executor — never block a caller waiting on itself).
- Stubs in `sharedutils/kmpextensionloader/src/jvmMain`: add what the moved/JVM files need to compile (expected: `android.app.Activity`, `android.content.res.AssetManager`, `android.net.{ConnectivityManager, Network, NetworkCapabilities}`, `android.net.http.SslError`, `androidx.core.content.edit` helpers if referenced, …). One copy each, real SDK shapes.
- Update `VENDORED.md` (layout, JVM replacements list) and `scripts/vendor-cloudstream.sh` (destination split; never overwrites `src/jvmMain`).
- Test: `source_utilities/cloudstream/src/jvmTest/kotlin/com/programmersbox/cloudstream/CloudstreamJvmRuntimeTest.kt`

**Interfaces:**
- Produces (JVM): all vendored `com.lagradost.*` types; `CloudstreamApiService(api, internalName, serviceName)`, `serviceNamesFor(...)`, `CloudstreamMapper`, `PluginStore`, `InstalledPlugin`, `PluginRegistrations` (shared), `UntrustedCloudstreamApiService`.

- [ ] **Step 1: Failing tests** — `CloudstreamJvmRuntimeTest`: `mainThreadHelpersRunWithoutLooper` (call `runOnMainThread { … }` and `main { }` from `Dispatchers.IO` in a plain JVM test; completes within 2 s), `pluginFacadeClassesExistOnJvm` (`Class.forName` each facade/type listed in `shims.md` that is not marked "missing", plus `Coroutines_androidKt`, `AppUtilsKt`, `ExtractorApiKt`), `cloudflareChallengeFailsClearly` (MockWebServer CF challenge → `IOException` containing "Cloudflare"), `webViewResolverFailsClearly`, `dataStoreRoundTrip` (`CloudStreamApp.setKey/getKey` persist via a temp-dir `Application`). Run existing moved shared tests on JVM too.
- [ ] **Step 2: Run** `./gradlew :source_utilities:cloudstream:jvmTest` → fails (no JVM target).
- [ ] **Step 3: Implement** (pure-move commit, then edits).
- [ ] **Step 4: Run** `./gradlew :source_utilities:cloudstream:jvmTest :sharedutils:kmpextensionloader:jvmTest` and `ANDROID_HOME=… ./gradlew :source_utilities:cloudstream:testAndroidHostTest :animeworld:assembleNoFirebaseDebug` → PASS.
- [ ] **Step 5: Commit** — `feat(cloudstream): compile vendored CloudStream library for JVM`

---

### Task 2: Real plugin fixtures and the stub scanner

**Files:**
- Create: `source_utilities/cloudstream/src/jvmTest/resources/fixtures/*.cs3` (1–2 small real plugins from a public CloudStream repo with anime sources — prefer the phisher98 repo `https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/builds/repo.json`, e.g. AnimePahe or Anizone; one that does **not** need a WebView or `requiresResources`), `.../CloudstreamFixture.kt` (KDoc: source URL, repo URL, internalName, version, sha256, pluginClassName from `manifest.json`).
- Create: `source_utilities/cloudstream/src/jvmTest/kotlin/com/programmersbox/cloudstream/CloudstreamPluginScanTest.kt` — opt-in (`CS_SCAN=1`) developer tool ported from `MihonExtensionScanTest`: fetch the repo's plugins (default: the 4 plugins named in `shims.md` + the fixtures; env overrides), `DexConverter.convert`, ASM-collect referenced types/members not defined in the plugin, resolve against the JVM host classpath, report missing types (with real shape from `$ANDROID_HOME/platforms/android-36/android.jar`) and members, then load+init each class via `ChildFirstClassLoader` with the CloudStream parent-first list and report linkage errors. Writes `build/cloudstream-scan/scan.md`.
- Modify: `sharedutils/kmpextensionloader/src/jvmMain/...` — add stubs for every missing type/member the scan reports for the default plugin set that affects **class loading/linking** (supertypes, field/param types the verifier loads; `androidx.appcompat.app.AppCompatActivity`, `androidx.fragment.app.{Fragment, DialogFragment, FragmentActivity, FragmentManager}`, `com.google.android.material.bottomsheet.*` are expected). Types only touched inside method bodies that the desktop never runs (settings dialogs) may stay missing; list them in `shims.md` under a new "JVM: missing at runtime only" section.
- Test: `source_utilities/cloudstream/src/jvmTest/kotlin/com/programmersbox/cloudstream/PluginFixtureLinkTest.kt`

**Interfaces:**
- Produces: `object CloudstreamFixture { val cs3: File; const val INTERNAL_NAME; const val VERSION; const val PLUGIN_CLASS; … }`; `CLOUDSTREAM_PARENT_FIRST: List<String>` (in `source_utilities/cloudstream/src/jvmMain/.../plugins/`) = `ChildFirstClassLoader.BASE_PARENT_FIRST` + `"com.lagradost."`, `"com.fasterxml.jackson."`, `"org.mozilla.javascript."`, `"com.fleeksoft."`, `"io.ktor."`, `"com.google.gson."`, `"org.xmlpull."`, `"dev.whyoleg."` (adjust to what the host actually provides; never parent-first a package the host doesn't ship).

- [ ] **Step 1: Failing test** — `PluginFixtureLinkTest.fixtureClassesLink`: convert each fixture, load every class through `ChildFirstClassLoader(…, CLOUDSTREAM_PARENT_FIRST)` and `Class.forName(name, true, loader)` except classes extending `android.app.Activity`/`androidx.fragment.app.Fragment`; assert no `LinkageError`/`ClassNotFoundException`; and the `pluginClassName` instantiates as `com.lagradost.cloudstream3.plugins.BasePlugin`.
- [ ] **Step 2: Run** `./gradlew :source_utilities:cloudstream:jvmTest --tests '*PluginFixtureLinkTest*'` → fails (missing stubs).
- [ ] **Step 3: Implement** scanner, run it once with `CS_SCAN=1` (record summary in the report), add stubs.
- [ ] **Step 4: Run** → PASS; `:sharedutils:kmpextensionloader:jvmTest` (shape test) PASS.
- [ ] **Step 5: Commit** — `feat(cloudstream): plugin fixtures, JVM stub scanner and the stubs real plugins link against`

---

### Task 3: JVM plugin loader and catalog

**Files:**
- Create: `source_utilities/cloudstream/src/jvmMain/kotlin/com/programmersbox/cloudstream/plugins/PluginLoader.kt` (same public API as Android: `class PluginLoader(…) { fun load(plugin: InstalledPlugin): LoadedPlugin; fun unload(loaded: LoadedPlugin) }`), JVM `CloudstreamModule.kt`, and either a shared `CloudstreamCatalog` (preferred if the only Android type is `Application` — then move it to `jvmAndroidShared`) or a JVM one.
- Test: `source_utilities/cloudstream/src/jvmTest/kotlin/com/programmersbox/cloudstream/JvmPluginLoaderTest.kt`, `CloudstreamCatalogJvmTest.kt`

**Interfaces:**
- JVM `PluginLoader(jarCacheDir: File, context: Context, parentClassLoader: ClassLoader = …)`: `DexConverter.convert(cs3, jarCacheDir)` → `JarLoaderCache<…>(parent, CLOUDSTREAM_PARENT_FIRST).getOrLoad(jar) { cl -> read "manifest.json" resource → BasePlugin.Manifest (vendored jackson mapper) → instantiate pluginClassName }`; `requiresResources` → throw the clear "Needs Android resources" error (catalog turns it into a placeholder); `PluginRegistrations.capture { if (instance is Plugin) instance.load(context) else instance.load() }`. `unload` calls `beforeUnload`, `PluginRegistrations.remove`, and lets the cache close the loader on the next `retain`.
- JVM `cloudstreamModule`: `CloudstreamRepoClient(HttpClient(OkHttp){ followSslRedirects(false) })`; `PluginStore(File(AppDirs.userDataDir, "cloudstream-plugins"), OkHttpClient.Builder().followSslRedirects(false).build())`; `PluginLoader(File(AppDirs.userCacheDir, "cloudstream-jars"), Application("cloudstream", File(dataDir, "cloudstream-data")))`; catalog `bind KmpExternalCustomApiServicesCatalog::class`. JVM never builds `ConfigurableCloudstreamApiService`.
- Shims install on JVM: set `CloudStreamApp.context` to the stub `Application`; no lifecycle callbacks.

- [ ] **Step 1: Failing tests** — `JvmPluginLoaderTest`: `loadsFixtureAndRegistersApis` (≥1 `MainAPI`, service names via `serviceNamesFor`), `throwingPluginLeavesNoRegistrations` (fixture-independent: a tiny test plugin jar compiled in test sources or a fake `BasePlugin` whose `load` throws after registering), `requiresResourcesGivesClearError`, `reloadReusesLoaderForSameJar`. `CloudstreamCatalogJvmTest`: `untrustedRepoShowsTrustPrompt` (placeholder `canTrust == true`, trust adds repo URL), `trustedRepoLoadsSources`, `uninstallUnregistersApisAndClosesLoader`, `failedLoadShowsPlaceholder`, `noConfigurableServicesOnDesktop`.
- [ ] **Step 2: Run** `./gradlew :source_utilities:cloudstream:jvmTest --tests '*JvmPluginLoaderTest*' --tests '*CloudstreamCatalogJvmTest*'` → fail.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS; Android host tests + assemble still PASS if shared code moved.
- [ ] **Step 5: Commit** — `feat(cloudstream): load trusted CloudStream plugins on desktop`

---

### Task 4: Enable CloudStream in AnimeWorld desktop

**Files:**
- Modify: `animeworld/desktop/build.gradle.kts` (`implementation(projects.sourceUtilities.cloudstream)`), `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt` (`includes(cloudstreamModule)`), `CHANGELOG.md` (AnimeWorld → Desktop: CloudStream plugins).
- Test: `animeworld/desktop/src/jvmTest/kotlin/com/programmersbox/desktop/CloudstreamDesktopWiringTest.kt`; opt-in live test `animeworld/desktop/src/jvmTest/kotlin/com/programmersbox/desktop/CloudstreamEndToEndNetworkTest.kt` (`CLOUDSTREAM_E2E=1`).

- [ ] **Step 1: Failing tests** — `cs3LinkClaimedByCatalog` (Koin-wired desktop catalogs: `installViaCatalogs` with a remote whose link ends `.cs3` is claimed by the CloudStream catalog, `DownloadStateRepository` not invoked), `aniyomiAndCloudstreamCoexist` (both catalogs registered; no duplicate-class conflict: load the Aniyomi fixture and the CloudStream fixture in one JVM).
- [ ] **Step 2: Run** `./gradlew :animeworld:desktop:jvmTest --tests '*CloudstreamDesktopWiringTest*'` → fail.
- [ ] **Step 3: Implement** wiring; `./gradlew :animeworld:desktop:dependencies --configuration jvmRuntimeClasspath | grep -c mihonsupport` → `0`.
- [ ] **Step 4: Run** → PASS. Then opt-in live test, run once and report: add the fixture's repo, trust, install the fixture plugin, `recent(1)` → `itemInfo` → `chapterInfo` → first `KmpStorage` plays a frame in `DesktopPlayer` (and, if quick, downloads via `AnimeDownloadManager`). If the site fails for reasons outside our code, report precisely.
- [ ] **Step 5: Regression** — `./gradlew :source_utilities:cloudstream:jvmTest :source_utilities:aniyomi:jvmTest :animeworld:shared:jvmTest :animeworld:desktop:jvmTest :mangaworld:desktop:compileKotlinJvm` and `ANDROID_HOME=… ./gradlew :source_utilities:cloudstream:testAndroidHostTest :animeworld:assembleNoFirebaseDebug`; headless `:animeworld:desktop:run` smoke (start, no exceptions, stop).
- [ ] **Step 6: Commit** — `feat(animeworld-desktop): enable CloudStream plugins`

Manual checklist for the user (macOS, `./gradlew :animeworld:desktop:run`): add a CloudStream repo, trust it, install two plugins, browse → details → play with subtitles, download one, uninstall one, confirm an untrusted repo shows the trust prompt.
