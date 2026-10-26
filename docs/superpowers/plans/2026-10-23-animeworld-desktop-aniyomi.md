# AnimeWorld Desktop — Aniyomi Extensions Implementation Plan (Plan 1 of 3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AnimeWorld Desktop installs, trusts, updates, uninstalls and browses Aniyomi extensions from the existing Extensions screens (browse → details → episode list → video list), with Android unchanged.

**Architecture:** Mirror what `feat/mangaworld-desktop-mihon-extensions` did for Mihon: compile the vendored Aniyomi runtime for Android and JVM from a shared `src/jvmAndroidShared` source dir, add a JVM APK store/inspector/loader/catalog in `source_utilities/aniyomi/src/jvmMain`, reuse `kmpextensionloader` (dex conversion, Android stubs, manifest parsing), and register the catalog in `animeworld:desktop`. The desktop install path picks the right store per downloaded file.

**Tech Stack:** Kotlin Multiplatform, Koin, OkHttp, Injekt, dex2jar (`DexConverter`), `apksig`, `apk-parser`, kotlin.test.

**Spec:** `docs/superpowers/specs/2026-10-23-animeworld-desktop-extensions-design.md` (sections 1, 2, 4). Plans 2 (player spike → player → downloads) and 3 (CloudStream) follow separately.

**Reference implementation:** the Mihon desktop commits on this branch. Before each task, read the matching Mihon commit; the Aniyomi version should look like it. Key ones: `dab907219` (runtime compiled for JVM, cookie jar, Cloudflare stub), `212024c01` (APK inspector + `apksig`), `b5ce6ace0` (APK store), `7124c6623` (child-first loader), `cf6d94950` (desktop wiring).

## Global Constraints

- Android behavior unchanged; `./gradlew :source_utilities:aniyomi:testAndroidHostTest` and `:animeworld:assembleNoFirebaseDebug` keep passing.
- MangaWorld desktop Mihon support unchanged; `./gradlew :mihonsupport:jvmTest :kmpuiviews:jvmTest` keep passing.
- Extension ABI byte-compatible: vendored `eu.kanade.tachiyomi.*`, `tachiyomi.*`, `mihon.*`, `app.cash.quickjs.*` keep package, class shape and signatures. Keep `source_utilities/aniyomi/VENDORED.md` accurate (new file locations, every `OTAKU:` edit).
- `animeworld:desktop` must never depend on `mihonsupport` (duplicate vendored classes).
- Same `AniyomiLib.SUPPORTED` (14.0, 16.0, 17.0) and `AniyomiTrust` rules as Android.
- Service names unchanged (`AniyomiApiService.serviceName`, `aniyomi-untrusted:<pkg>`), so favorites/history match Android.
- No source settings UI on desktop: sources load with default preferences; desktop never returns `ConfigurableAniyomiApiService`.
- Cloudflare/WebView-dependent requests fail with a clear error, never a crash.
- Verified via `./gradlew :animeworld:desktop:run` on macOS.

## Review Focus

- A Mihon APK downloaded while only the Aniyomi store exists (or vice versa) must give an "invalid extension" install error, not be silently stored by the wrong store. → Task 1 test `noStoreAcceptsFileReportsInvalid`.
- An Aniyomi APK whose lib version is unsupported (e.g. 13) must be rejected at install with INCOMPATIBLE. → Task 3 test `rejectsUnsupportedLib`.
- Updating an installed extension (same package, new APK) while its class loader is live must replace it and the reload must show the new version. → Task 4 test `reloadAfterUpdateUsesNewJar`.
- A Cloudflare-challenged response must surface as an `IOException` with a readable message. → Task 2 test `cloudflareChallengeFailsClearly`.
- Extensions that start a local `HttpServer` (NanoHTTPD) must run on JVM. → Task 2 test `httpServerStartsOnJvm`.

---

### Task 1: Store selection per downloaded file

`DownloadStateRepository` uses `stores.firstOrNull()`. Desktop apps will register more than one store (Aniyomi now, CloudStream in Plan 3), so each store must say whether it accepts a file.

**Files:**
- Modify: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/ApkExtensionStore.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/DownloadStateRepository.kt` (`val store = stores.firstOrNull() ...`)
- Modify: `mihonsupport/src/jvmMain/kotlin/com/programmersbox/mihon/MihonApkStore.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/DownloadStateRepositoryTest.kt`, `mihonsupport/src/jvmTest/kotlin/com/programmersbox/mihon/MihonApkStoreTest.kt`

**Interfaces:**
- Produces: `ApkExtensionStore.accepts(file: File): Boolean` — cheap, no side effects, never throws (false on unreadable). Used by Tasks 3 and Plan 3.

- [ ] **Step 1: Write failing tests**

In `DownloadStateRepositoryTest`, give the `store(...)` helper an `accepts: Boolean = true` parameter and add:

```kotlin
@Test
fun picksFirstStoreThatAcceptsFile() = runTest {
    val rejecting = store(accepts = false)
    val accepting = store()
    val repo = repository(stores = listOf(rejecting, accepting))
    repo.downloadAndInstall(url)
    // await Installed as other tests do
    assertEquals(0, rejecting.installCount)
    assertEquals(1, accepting.installCount)
}

@Test
fun noStoreAcceptsFileReportsInvalid() = runTest {
    val repo = repository(stores = listOf(store(accepts = false)))
    repo.downloadAndInstall(url)
    val status = /* await terminal status */
    assertEquals(InstallErrorReason.INVALID, (status as DownloadAndInstallStatus.Error).reason)
}
```

In `MihonApkStoreTest`: `acceptsMihonApk` → `assertTrue(store.accepts(MihonFixture.apk))`; `rejectsNonApk` → `assertFalse(store.accepts(tempFile with "not a zip"))`.

- [ ] **Step 2: Run, verify they fail**

Run: `./gradlew :kmpuiviews:jvmTest --tests '*DownloadStateRepositoryTest*' :mihonsupport:jvmTest --tests '*MihonApkStoreTest*'`
Expected: compile failure (`accepts` undefined).

- [ ] **Step 3: Implement**

- Add `fun accepts(file: File): Boolean` to `ApkExtensionStore` (abstract; no default).
- `DownloadStateRepository`: `stores.firstOrNull { it.accepts(file) }`; none → `ApkInstallException(ApkInstallFailure.INVALID, "Not a supported extension")` handled by the existing INVALID branch. Keep `NoStoreException` for an empty list.
- `MihonApkStore.accepts`: `runCatching { "tachiyomi.extension" in ApkManifestParser.parse(file).features }.getOrDefault(false)`.

- [ ] **Step 4: Run, verify pass**

Same command. Expected: PASS, and the whole `:kmpuiviews:jvmTest :mihonsupport:jvmTest` still passes.

- [ ] **Step 5: Commit** — `feat(desktop-extensions): pick install store by file, not list order`

---

### Task 2: Compile the vendored Aniyomi runtime for JVM

**Files:**
- Modify: `source_utilities/aniyomi/build.gradle.kts` — mirror `mihonsupport/build.gradle.kts`: `androidMain { kotlin.srcDir("src/jvmAndroidShared/kotlin") }`, `jvmMain { kotlin.srcDir(...) ; dependencies { … } }` with the same JVM deps Mihon's `jvmMain` has (plus `androidLibs.nanohttpd`, `androidLibs.preference` is Android-only → the stub in `kmpextensionloader` replaces it), `androidHostTest`/`jvmTest` both get `src/jvmAndroidSharedTest/kotlin`.
- Move (`git mv`, contents unchanged unless noted) from `src/androidMain/kotlin` to `src/jvmAndroidShared/kotlin`: everything except the Android-only list below.
- Android-only, stays in `androidMain`: `AniyomiCatalog.kt`, `AniyomiModule.kt`, `loader/AniyomiExtensionLoader.kt`, `preferences/AniyomiPreferenceFragment.kt`, `repo/Gzip.android.kt`, `eu/kanade/tachiyomi/network/AndroidCookieJar.kt`, `network/interceptor/CloudflareInterceptor.kt`, `network/interceptor/WebViewInterceptor.kt`, `util/system/WebViewUtil.kt`, `util/system/DeviceUtil.kt`, `tachiyomi/core/common/util/system/LogcatExtensions.kt`.
- Create: `src/androidMain/.../AniyomiApiServiceFactory.android.kt` (current `aniyomiApiService` body) and `src/jvmMain/.../AniyomiApiServiceFactory.jvm.kt` (always `AniyomiApiService(source, lib)`), as Mihon did. Remove `aniyomiApiService` from the moved `AniyomiApiService.kt`; `ConfigurableAniyomiApiService` stays in the shared file.
- Create in `src/jvmMain/kotlin/eu/kanade/tachiyomi/network/`: `AndroidCookieJar.kt`, `interceptor/CloudflareInterceptor.kt` — copy of the Mihon JVM files (same behavior, Aniyomi's package names are identical).
- Shared files that call `logcat`, `WebViewUtil`, `DeviceUtil` or WebView types: replace with what Mihon's shared files did (`git show dab907219` for `NetworkHelper.kt`, `AppInfo.kt`, `MihonApiService.kt`). Mark each change `OTAKU:` and list it in `VENDORED.md`.
- Move tests: `src/androidHostTest/kotlin/**` → `src/jvmAndroidSharedTest/kotlin/**` where they don't use Android APIs.
- Create tests: `src/jvmTest/kotlin/com/programmersbox/aniyomi/AniyomiRuntimeJvmTest.kt`

**Interfaces:**
- Produces (JVM): `AniyomiRuntime.init(application: android.app.Application)`, `aniyomiApiService(source: AnimeCatalogueSource, lib: Double): AniyomiApiService` (never configurable on JVM), `AniyomiSourceInstantiator.instantiate(classNames: String, pkg: String, loader: ClassLoader): List<AnimeCatalogueSource>`, `UntrustedAniyomiApiService(...)` (unchanged constructor), `AniyomiHttpServers.portFor(source: AnimeHttpSource): Int`.

- [ ] **Step 1: Write failing JVM tests** in `AniyomiRuntimeJvmTest`:

```kotlin
@Test fun runtimeRegistersNetworkHelper() {
    AniyomiRuntime.init(Application("aniyomi-test", tempDir))
    assertNotNull(Injekt.get<NetworkHelper>().client)
}
@Test fun factoryNeverConfigurableOnJvm() {
    assertFalse(aniyomiApiService(FakeConfigurableAnimeSource(), 16.0) is KmpConfigurableApiService)
}
@Test fun cloudflareChallengeFailsClearly() {
    // MockWebServer returns 403 with "Server: cloudflare" + cf challenge body, as MihonCloudflareInterceptorJvmTest
    val e = assertFailsWith<IOException> { client.newCall(request).execute() }
    assertContains(e.message!!, "Cloudflare")
}
@Test fun httpServerStartsOnJvm() {
    val port = AniyomiHttpServers.portFor(FakeHttpServerSource()) // createHttpServer() returns a NanoHTTPD-backed HttpServer
    assertTrue(port > 0)
}
```

`FakeConfigurableAnimeSource` / `FakeHttpServerSource` go next to the existing `FakeSources.kt` (moved to `jvmAndroidSharedTest`).

- [ ] **Step 2: Run, verify fail**

Run: `./gradlew :source_utilities:aniyomi:jvmTest`
Expected: compilation errors (classes not on JVM).

- [ ] **Step 3: Do the moves and JVM actuals listed under Files**

Fix compile errors by mirroring Mihon, not by changing extension-facing signatures. Missing `android.*` types → add them to `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/...` (one copy), matching Android's real shape (class vs interface, signatures).

- [ ] **Step 4: Run, verify pass**

Run: `./gradlew :source_utilities:aniyomi:jvmTest :source_utilities:aniyomi:testAndroidHostTest :sharedutils:kmpextensionloader:jvmTest :animeworld:assembleNoFirebaseDebug`
Expected: all PASS / BUILD SUCCESSFUL.

- [ ] **Step 5: Commit** — `feat(aniyomi): compile vendored Aniyomi runtime for JVM with desktop cookie jar and Cloudflare stub`

---

### Task 3: Aniyomi APK inspector and store on JVM

**Files:**
- Create: `source_utilities/aniyomi/src/jvmMain/kotlin/com/programmersbox/aniyomi/AniyomiApkInspector.kt`, `AniyomiApkStore.kt`
- Create fixture: `source_utilities/aniyomi/src/jvmTest/resources/fixtures/<real-aniyomi-extension>.apk` and `src/jvmTest/kotlin/com/programmersbox/aniyomi/AniyomiFixture.kt`
- Test: `src/jvmTest/kotlin/com/programmersbox/aniyomi/AniyomiApkInspectorTest.kt`, `AniyomiApkStoreTest.kt`
- Modify: `source_utilities/aniyomi/build.gradle.kts` jvmMain deps: `projects.sharedutils.kmpextensionloader`, `androidLibs.apksig`, `"net.dongliu:apk-parser:2.6.10"`, `desktopLibs.kotlin.multiplatform.appdirs` (as Mihon).

**Fixture:** download one real, signed Aniyomi extension APK (lib 14/16/17, not Cloudflare-protected, preferably a JSON-API source) from a public Aniyomi repo's `index.min.json`. Document in `AniyomiFixture` (KDoc like `MihonFixture`): source URL, `PACKAGE`, `VERSION_NAME`, `VERSION_CODE`, `SOURCE_CLASS` (manifest `tachiyomi.animeextension.class`), `REPO_FINGERPRINT` (repo's signing key), APK sha256. Also copy `mihonsupport/src/jvmTest/resources/fixtures/tachiyomi-en.mangapill-v1.6.0.apk` as the "manga APK, not anime" fixture.

**Interfaces:**
- Produces:
  ```kotlin
  data class AniyomiApkInfo(
      val packageName: String, val versionName: String?, val versionCode: Long, val label: String,
      val isExtension: Boolean, val hasMetadata: Boolean, val libVersion: Double?,
      val sourceClasses: String?,          // raw ';'-separated value for AniyomiSourceInstantiator
      val nsfw: Boolean, val fingerprints: List<String>,  // lowercase hex SHA-256, oldest → newest
  )
  object AniyomiApkInspector { fun inspect(apk: File): AniyomiApkInfo }
  data class InstalledAniyomiApk(val apk: File, val jar: File?, val info: AniyomiApkInfo)
  class AniyomiApkStore(apkDir: File, jarCacheDir: File,
      inspect: (File) -> AniyomiApkInfo = AniyomiApkInspector::inspect,
      convert: (File, File) -> File? = DexConverter::convert) : ApkExtensionStore {
      override suspend fun install(apk: File): String
      override fun accepts(file: File): Boolean
      override fun versionName(packageName: String): String?
      suspend fun uninstall(packageName: String): Boolean
      fun installed(): List<InstalledAniyomiApk>
      fun cleanupStaleJars()
  }
  ```
- Manifest keys (from Android loader): feature `tachiyomi.animeextension`, `tachiyomi.animeextension.class`, `aniyomix.extensionLib` (int; `AniyomiLib.libVersion(metaLib?.toIntOrNull() ?: 0, versionName)`), nsfw `tachiyomi.animeextension.nsfw` == "1", label = application label with `"Aniyomi: "` prefix removed (fall back to last package segment).

- [ ] **Step 1: Write failing tests**

`AniyomiApkInspectorTest`: `inspectsFixture` asserts every `AniyomiFixture` constant plus `isExtension`, `hasMetadata`, `AniyomiLib.isSupported(libVersion)`, `fingerprints.last() == REPO_FINGERPRINT`; `mangaApkIsNotAnimeExtension` asserts `!isExtension` for the MangaPill fixture.

`AniyomiApkStoreTest` (mirror `MihonApkStoreTest`): `installStoresApkAndJar`, `rejectsMangaApk` (INVALID), `rejectsUnsupportedLib` (inspect lambda returning `libVersion = 13.0` → INCOMPATIBLE), `rejectsPathTraversalPackage`, `uninstallDeletesApkAndJar`, `versionNameFromManifest`, `acceptsAnimeApkOnly` (`accepts(fixture)` true, `accepts(mangapill)` false).

- [ ] **Step 2: Run, verify fail** — `./gradlew :source_utilities:aniyomi:jvmTest --tests '*AniyomiApk*'` → unresolved references.

- [ ] **Step 3: Implement** — same structure and error messages as `MihonApkStore`/`MihonApkInspector`, with Aniyomi keys and `AniyomiLib`. If the package-name regex and `moveReplacing` would be copied verbatim, move them into `kmpextensionloader` (e.g. `ApkFiles.kt`) and use them from both stores.

- [ ] **Step 4: Run, verify pass** — same command, plus `:mihonsupport:jvmTest` if shared helpers moved.

- [ ] **Step 5: Commit** — `feat(aniyomi): desktop store for installed Aniyomi APKs and converted jars`

---

### Task 4: JVM loader and catalog

**Files:**
- Move: `mihonsupport/src/jvmMain/kotlin/com/programmersbox/mihon/ChildFirstClassLoader.kt` → `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/ChildFirstClassLoader.kt` with the parent-first prefixes as a constructor parameter; Mihon passes its current list unchanged.
- Create: `source_utilities/aniyomi/src/jvmMain/kotlin/com/programmersbox/aniyomi/JvmAniyomiExtensionLoader.kt`, `AniyomiCatalog.kt` (JVM), `AniyomiModule.kt` (JVM)
- Test: `src/jvmTest/kotlin/com/programmersbox/aniyomi/JvmAniyomiExtensionLoaderTest.kt`, `AniyomiCatalogJvmTest.kt`, `AniyomiDexConversionTest.kt`

**Interfaces:**
- Consumes: Task 2 runtime, Task 3 store.
- Produces:
  ```kotlin
  class ChildFirstClassLoader(urls: Array<URL>, parent: ClassLoader, parentFirst: List<String>) : URLClassLoader
  class JvmAniyomiExtensionLoader(store: AniyomiApkStore, parentClassLoader: ClassLoader = AniyomiCatalog::class.java.classLoader) {
      fun load(catalog: AniyomiCatalog, repoSigningKeys: Set<String>, trustedIds: Set<String>, onTrust: suspend (String) -> Unit): List<KmpSourceInformation>
      fun isExtension(packageName: String): Boolean
      fun close()
  }
  class AniyomiCatalog(application: Application, settings: NewSettingsHandling, repoClient: AniyomiRepoClient, store: AniyomiApkStore)
      : KmpExternalCustomApiServicesCatalog   // name "Aniyomi", hasRemoteSources true, uninstall → store.uninstall
  val aniyomiModule: Module  // JVM: AniyomiRepoClient, AniyomiApkStore(userData/"aniyomi-extensions", userCache/"aniyomi-jars") bind ApkExtensionStore, AniyomiCatalog(Application("aniyomi", userData/"aniyomi-data"), …) bind KmpExternalCustomApiServicesCatalog
  ```
- Aniyomi parent-first prefixes: Mihon's list plus `"fi.iki.elonen."` (NanoHTTPD) and anything else the vendored ABI exposes (`kotlinx.serialization` is already under `kotlinx.`).

- [ ] **Step 1: Write failing tests** (mirror `JvmMihonExtensionLoaderTest`, `MihonCatalogJvmTest`, `MihonDexConversionTest`):
  - `trustedFixtureLoadsSources` — repo key = `REPO_FINGERPRINT` → services are `AniyomiApiService`, `displayName` non-blank, `packageName == PACKAGE`.
  - `untrustedFixtureShowsTrustPrompt` — no keys/trust → single `UntrustedAniyomiApiService` with `canTrust == true`, name ends `(Untrusted)`; calling `trust()` passes `AniyomiTrust.trustId(PACKAGE, VERSION_CODE, REPO_FINGERPRINT)` to `onTrust`.
  - `unsupportedLibShowsReason` — `Unsupported lib`.
  - `reloadAfterUpdateUsesNewJar` — install fixture, load, install a re-inspected copy with a different `versionCode` (inspect lambda), load again → old loader closed, new services returned, no exception.
  - `brokenPackageDoesNotAbortOthers`.
  - `catalogUninstallRemovesSource`.
  - `fixtureConvertsAndLinks` — converted jar loads every class in the fixture without `LinkageError` (as `MihonDexConversionTest`).

- [ ] **Step 2: Run, verify fail** — `./gradlew :source_utilities:aniyomi:jvmTest --tests '*JvmAniyomi*' --tests '*AniyomiCatalogJvm*' --tests '*AniyomiDexConversion*'`.

- [ ] **Step 3: Implement** — mirror `JvmMihonExtensionLoader` / JVM `MihonCatalog` / JVM `mihonModule`; call `AniyomiRuntime.init(application)` in `initialize()`; use `aniyomiApiService(source, lib)`. Add any `android.*` stubs the fixture's link check needs to `kmpextensionloader`.

- [ ] **Step 4: Run, verify pass** — `./gradlew :source_utilities:aniyomi:jvmTest :mihonsupport:jvmTest :sharedutils:kmpextensionloader:jvmTest`.

- [ ] **Step 5: Commit** — `feat(aniyomi): load trusted Aniyomi extensions on desktop via child-first class loader`

---

### Task 5: Enable Aniyomi in AnimeWorld desktop

**Files:**
- Modify: `animeworld/desktop/build.gradle.kts` (`implementation(projects.sourceUtilities.aniyomi)`)
- Modify: `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt` (`includes(aniyomiModule)` in `moduleBlock`)

- [ ] **Step 1: Build** — `./gradlew :animeworld:desktop:compileKotlinJvm` → BUILD SUCCESSFUL. Confirm `mihonsupport` is not on the classpath: `./gradlew :animeworld:desktop:dependencies --configuration jvmRuntimeClasspath | grep -c mihonsupport` → `0`.

- [ ] **Step 2: Manual check on macOS** — `./gradlew :animeworld:desktop:run`:
  1. Extensions → add an Aniyomi repo URL → its extensions list.
  2. Install two non-Cloudflare extensions (one HTML/Jsoup, one JSON API) → progress, then Installed.
  3. Each source: browse/recent → details → episode list → open an episode reaches the video-choice step (playback itself is Plan 2; `VideoNotSupportedScreen` is expected).
  4. Install an older version or a non-repo-signed APK → trust prompt; trust → loads.
  5. Update one (if an update exists) → new version shown; uninstall one → gone.
  6. A Cloudflare-protected source → readable error, app keeps running.
  7. Restart app → installed sources still load.
  Record results (and the extensions used) in the commit body.

- [ ] **Step 3: Regression** — `./gradlew :mangaworld:desktop:compileKotlinJvm :animeworld:assembleNoFirebaseDebug :mangaworld:assembleNoFirebaseDebug`.

- [ ] **Step 4: Commit** — `feat(animeworld-desktop): enable Aniyomi extensions`, plus a `CHANGELOG.md` entry matching existing style.
