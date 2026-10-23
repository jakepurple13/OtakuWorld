# MangaWorld Desktop Mihon Extensions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** MangaWorld Desktop installs, trusts, loads, updates and uninstalls Mihon extensions from Mihon repos through the existing Extensions screens, the same way the Android app does.

**Architecture:** The vendored Mihon runtime in `mihonsupport` is compiled for both Android and JVM by sharing one source directory between `androidMain` and `jvmMain`; WebView-bound pieces get JVM replacements with identical public signatures. Desktop installs downloaded APKs into a private store, converts them to jars with the existing `DexConverter`, verifies signatures with `apksig`, applies the existing `MihonTrust` rules and loads trusted extensions with a child-first class loader. Real `android.*` / `androidx.preference.*` stub classes on the JVM classpath, rewritten to Android's exact shapes, satisfy extension bytecode.

**Tech Stack:** Kotlin Multiplatform, Koin, Ktor, OkHttp, Injekt, zipline QuickJS, dex2jar v76, apk-parser 2.6.10, `com.android.tools.build:apksig`.

**Spec:** `docs/superpowers/specs/2026-10-23-mangaworld-desktop-mihon-extensions-design.md`

## Global Constraints

- Android Mihon behavior unchanged; `./gradlew :mihonsupport:testAndroidHostTest` and `./gradlew :mangaworld:assembleNoFirebaseDebug` keep passing after every task.
- Desktop OtakuWorld jar-plugin loading unchanged (except stub shapes, which no plugin depends on — user confirmed).
- Supported lib versions: `MihonLib.SUPPORTED` (1.4, 1.6), unchanged and shared.
- Mihon `serviceName` stays `"mihon:${source.id}"`; untrusted stays `"mihon-untrusted:$packageName"`.
- Trust rules are `MihonTrust` exactly; nothing loads unless signature-verified and trusted.
- Extension ABI (packages, class names, signatures of `eu.kanade.tachiyomi.*`, `android.*`, `androidx.preference.*` types extensions link against) must match extensions-lib / Android exactly.
- v1 verified via `./gradlew :mangaworld:desktop:run` on macOS; Windows must build but is not verified. Packaging (DMG/MSI) is out of scope; don't block it.
- Out of scope: Cloudflare/WebView solving, source settings UI on desktop, background update checks.
- Match surrounding code style; mark edits to vendored files with `// OTAKU:` comments as the existing vendored files do.

## Review Focus

1. **Configurable extension fails JVM verification** — an extension implementing `ConfigurableSource` references `ListPreference`, `SwitchPreferenceCompat`, etc. inside `setupPreferenceScreen`; HotSpot's verifier loads those classes to check assignability when the class links, so missing or wrongly-parented preference stubs make the whole extension fail to load even though desktop never shows settings. Expected: it loads. Test added in Task 1 (hierarchy) and Task 5 (fixture load).
2. **Update on Windows while the old jar is open** — `URLClassLoader` keeps the old converted jar locked. Expected: update succeeds; stale jars are cleaned on a later load. Test in Task 4 (`install replaces apk and keeps old jar until cleanup`).
3. **Cloudflare-challenged response** — expected: a readable error ("Cloudflare-protected; not supported on desktop yet") reaches the UI, not a hang or opaque stack. Test in Task 2.
4. **Downloaded file is not a Mihon extension or has unsupported lib** — expected: install shows an error state (`INVALID` / `INCOMPATIBLE`), nothing written to the store. Test in Task 4 and Task 7.
5. **Repo offline at startup** — expected: catalog still loads with the signing keys fetched earlier this session; with none, repo-signed extensions show as untrusted (trust prompt), not crash. Test in Task 5.

---

## File Structure

`sharedutils/kmpextensionloader/src/jvmMain/kotlin/`
- `com/programmersbox/android/content/SharedPreferences.kt` — rewrite to Android `interface` shape.
- `com/programmersbox/kmpextensionloader/FileSharedPreferences.kt` — new, file-backed implementation (content of today's stub class).
- `com/programmersbox/android/content/Context.kt` — returns `FileSharedPreferences`.
- `com/programmersbox/android/net/Uri.kt`, `android/os/SystemClock.kt`, `android/annotation/SuppressLint.kt` — new stubs.
- `com/programmersbox/androidx/preference/*.kt` — new preference stubs (one file per class family).
- `com/programmersbox/kmpextensionloader/ApkExtensionStore.kt` — new interface desktop installers implement.
- `com/programmersbox/kmpextensionloader/ApkManifestParser.kt` — add `versionCode`.
- `com/programmersbox/kmpextensionloader/SourceLoader.kt` — accept `NativeCatalogs`.

(Note: existing stub files live under a `com/programmersbox/android/...` folder but declare `package android.*`. Keep that convention.)

`mihonsupport/`
- `build.gradle.kts` — shared source dir, JVM deps, apksig.
- `src/jvmAndroidShared/kotlin/` — new dir; vendored runtime + `MihonApiService.kt`, `MihonSourceMapper.kt`, `UntrustedMihonApiService.kt`, `MihonRuntime.kt`, `MihonSourceInstantiator.kt` moved here.
- `src/jvmAndroidSharedTest/kotlin/` — new dir; platform-neutral tests moved here.
- `src/androidMain/kotlin/` — keeps `MihonCatalog`, `MihonModule`, `MihonExtensionLoader`, `MihonPreferenceFragment`, `mihonApiService` factory, WebView-bound vendored files, `AndroidCookieJar`, `LogcatExtensions`.
- `src/jvmMain/kotlin/eu/kanade/tachiyomi/network/AndroidCookieJar.kt`, `.../interceptor/CloudflareInterceptor.kt` — JVM replacements.
- `src/jvmMain/kotlin/com/programmersbox/mihon/` — `MihonApiServiceFactory.jvm.kt`, `MihonApkInspector.kt`, `MihonApkStore.kt`, `JvmMihonExtensionLoader.kt`, `ChildFirstClassLoader.kt`, `MihonCatalog.kt`, `MihonModule.kt`.
- `src/jvmTest/resources/fixtures/` — one real Keiyoushi extension APK.

`kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/`
- `repository/DownloadStateRepository.kt` — real implementation.
- `repository/SourceInfoRepository.jvm.kt` — version from stores.
- `di/AppModule.jvm.kt`, `di/RepositoryModule.jvm.kt` — wiring.

`mangaworld/desktop/` — `build.gradle.kts` dependency, `main.kt` includes `mihonModule`.

---

### Task 1: Android-shaped JVM stubs

**Files:**
- Modify: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/content/SharedPreferences.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/FileSharedPreferences.kt`
- Modify: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/content/Context.kt`
- Create: `.../android/net/Uri.kt`, `.../android/os/SystemClock.kt`, `.../android/annotation/SuppressLint.kt`
- Create: `.../androidx/preference/Preference.kt` (Preference, PreferenceGroup, PreferenceScreen), `.../androidx/preference/DialogPreferences.kt` (DialogPreference, ListPreference, MultiSelectListPreference, EditTextPreference), `.../androidx/preference/TwoStatePreferences.kt` (TwoStatePreference, CheckBoxPreference, SwitchPreferenceCompat)
- Modify: `sharedutils/kmpextensionloader/src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/SharedPreferencesTest.kt`
- Create: `sharedutils/kmpextensionloader/src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/AndroidStubShapeTest.kt`

**Interfaces:**
- Produces: `interface android.content.SharedPreferences` with nested `interface Editor` and `interface OnSharedPreferenceChangeListener`, method set identical to Android's (`getAll`, `getString`, `getStringSet`, `getInt`, `getLong`, `getFloat`, `getBoolean`, `contains`, `edit`, `register/unregisterOnSharedPreferenceChangeListener`; Editor `put*`, `remove`, `clear`, `commit(): Boolean`, `apply()`).
- Produces: `class FileSharedPreferences(file: File) : SharedPreferences` (package `com.programmersbox.kmpextensionloader`), behavior of today's stub, plus change listeners fired on commit.
- Produces: `abstract class android.net.Uri` with `companion object { @JvmStatic fun parse(uriString: String): Uri }`, `getScheme()`, `getHost()`, `getPath()`, `getLastPathSegment()`, `getQueryParameter(key)`, `toString()` (backed by `java.net.URI`).
- Produces: `object android.os.SystemClock` with `@JvmStatic elapsedRealtime(): Long`, `uptimeMillis(): Long`.
- Produces: `androidx.preference` classes, each `open`, constructor `(context: Context)`. Hierarchy exactly: `Preference` ← `PreferenceGroup` ← `PreferenceScreen`; `Preference` ← `DialogPreference` ← `ListPreference`, `MultiSelectListPreference`, `EditTextPreference`; `Preference` ← `TwoStatePreference` ← `CheckBoxPreference`, `SwitchPreferenceCompat`. Members are no-op/storing Kotlin `var`s for what extensions set: `key`, `title`, `summary`, `isVisible`, `isEnabled`, `setDefaultValue(Any?)`, `setOnPreferenceChangeListener(OnPreferenceChangeListener?)` with nested `fun interface OnPreferenceChangeListener { fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean }`, `dialogTitle`/`dialogMessage` on DialogPreference, `entries`/`entryValues: Array<CharSequence>` + `value`/`values`, `text` on EditTextPreference, `isChecked`, `summaryOn`/`summaryOff`. `PreferenceGroup.addPreference(Preference): Boolean`, `PreferenceScreen.getContext(): Context`. Further members are added in Task 8 only if a real extension needs them.

- [ ] **Step 1: Write failing tests**

```kotlin
class AndroidStubShapeTest {
    @Test fun sharedPreferencesIsInterface() {
        assertTrue(android.content.SharedPreferences::class.java.isInterface)
        assertTrue(android.content.SharedPreferences.Editor::class.java.isInterface)
    }
    @Test fun preferenceHierarchyMatchesAndroidx() {
        assertEquals(androidx.preference.DialogPreference::class.java, androidx.preference.ListPreference::class.java.superclass)
        assertEquals(androidx.preference.TwoStatePreference::class.java, androidx.preference.SwitchPreferenceCompat::class.java.superclass)
        assertEquals(androidx.preference.PreferenceGroup::class.java, androidx.preference.PreferenceScreen::class.java.superclass)
        assertEquals(androidx.preference.Preference::class.java, androidx.preference.PreferenceGroup::class.java.superclass)
    }
    @Test fun uriParses() {
        val uri = android.net.Uri.parse("https://example.com/a/b.jpg?x=1")
        assertEquals("example.com", uri.host); assertEquals("b.jpg", uri.lastPathSegment); assertEquals("1", uri.getQueryParameter("x"))
    }
}
```

Update `SharedPreferencesTest` to construct `FileSharedPreferences(file)` and add `listenerFiresOnCommit` (register listener, `edit().putString("k","v").commit()`, assert listener got key `"k"`).

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmTest`
Expected: compile failure (unresolved `FileSharedPreferences`, `androidx.preference`, `android.net.Uri`).

- [ ] **Step 3: Implement the stubs listed in Interfaces**

Move today's `SharedPreferences` class body into `FileSharedPreferences`; `Context.getSharedPreferences` returns the cached `FileSharedPreferences`. Fix any compile errors in `JvmModelMapper`/`SourceLoader` from the shape change.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmTest`
Expected: PASS (all, including existing `BundleTest`, `JvmModelMapperTest`, `ApkManifestParserTest`).

- [ ] **Step 5: Commit**

```bash
git add sharedutils/kmpextensionloader
git commit -m "feat(desktop): reshape JVM android stubs to Android ABI, add Uri/SystemClock/preference stubs"
```

---

### Task 2: Compile the Mihon runtime for JVM

**Files:**
- Modify: `mihonsupport/build.gradle.kts`
- Move (git mv) into `mihonsupport/src/jvmAndroidShared/kotlin/`: everything under `src/androidMain/kotlin/{app,eu,mihon,tachiyomi}` **except** `eu/kanade/tachiyomi/network/AndroidCookieJar.kt`, `network/interceptor/CloudflareInterceptor.kt`, `network/interceptor/WebViewInterceptor.kt`, `util/system/WebViewUtil.kt`, `util/system/DeviceUtil.kt`, `tachiyomi/core/common/util/system/LogcatExtensions.kt`; plus `com/programmersbox/mihon/{MihonApiService.kt,MihonSourceMapper.kt,UntrustedMihonApiService.kt,MihonRuntime.kt}`.
- Create: `mihonsupport/src/jvmAndroidShared/kotlin/com/programmersbox/mihon/MihonSourceInstantiator.kt` (extract `object MihonSourceInstantiator` out of `MihonExtensionLoader.kt`)
- Create: `mihonsupport/src/androidMain/kotlin/com/programmersbox/mihon/MihonApiServiceFactory.android.kt` (the existing `fun mihonApiService(...)`, moved out of `MihonApiService.kt`)
- Create: `mihonsupport/src/jvmMain/kotlin/com/programmersbox/mihon/MihonApiServiceFactory.jvm.kt`
- Create: `mihonsupport/src/jvmMain/kotlin/eu/kanade/tachiyomi/network/AndroidCookieJar.kt`
- Create: `mihonsupport/src/jvmMain/kotlin/eu/kanade/tachiyomi/network/interceptor/CloudflareInterceptor.kt`
- Move into `mihonsupport/src/jvmAndroidSharedTest/kotlin/`: `FakeSources.kt`, `MihonSourceMapperTest.kt`, `QuickJsShimTest.kt`, `VendoredRuntimeTest.kt`, `loader/SourceInstantiationTest.kt`, `NetworkHelperTest.kt` — each only if it compiles without Android/Robolectric APIs; leave the rest in `androidHostTest`.
- Create: `mihonsupport/src/jvmTest/kotlin/com/programmersbox/mihon/CloudflareInterceptorJvmTest.kt`

**Interfaces:**
- Produces (both platforms, unchanged signatures): `MihonApiService`, `ConfigurableMihonApiService`, `UntrustedMihonApiService`, `MihonSourceMapper`, `MihonRuntime.init(application: android.app.Application)`, `MihonSourceInstantiator.classNames(pkg, metadataValue)`, `MihonSourceInstantiator.instantiate(classNames, classLoader): Result<List<CatalogueSource>>`.
- Produces: `fun mihonApiService(source: CatalogueSource): MihonApiService` per platform. JVM always returns plain `MihonApiService(source)` (no `KmpConfigurableApiService`, so the settings button stays hidden on desktop).
- Produces (JVM): `class AndroidCookieJar : CookieJar` with the same public members as the Android file (`saveFromResponse`, `loadForRequest`, `get(url)`, `remove(url, cookieNames, maxAge)`, `removeAll()`), in-memory, thread-safe.
- Produces (JVM): `class CloudflareInterceptor(context: Context, cookieManager: AndroidCookieJar, scope: CoroutineScope, defaultUserAgentProvider: () -> String) : Interceptor` — same detection rule as Android's `shouldIntercept` (status 403/503 with `Server: cloudflare*` header); on a hit closes the response and throws `IOException("Cloudflare-protected; not supported on desktop yet")`.

- [ ] **Step 1: Restructure source sets**

In `mihonsupport/build.gradle.kts`: add `kotlin.srcDir("src/jvmAndroidShared/kotlin")` to `androidMain` and `jvmMain`, and `kotlin.srcDir("src/jvmAndroidSharedTest/kotlin")` to `androidHostTest` and `jvmTest`. Add a `jvmMain` dependency block with: `commonLibs.ktorOkHttp`, `projects.datastore`, Koin BOM + `commonLibs.koinCores`, `androidLibs.okhttpLib`, `commonLibs.kotlinxSerializationOkio`, `androidLibs.okhttpDns`, `androidLibs.okhttpLogging`, `androidLibs.brotli`, `androidLibs.zstd`, `androidLibs.jsoup`, `androidLibs.injekt`, `androidLibs.rxjava1`, `commonLibs.zipline`, `projects.sharedutils.kmpextensionloader`. `jvmTest`: `commonLibs.kotlin.test`, `commonLibs.coroutinesTest`, `androidLibs.okhttpMockWebServer`. Do the git moves listed above.

- [ ] **Step 2: Write the failing Cloudflare test**

```kotlin
@Test fun cloudflareChallengeFailsWithReadableError() {
    // MockWebServer returns 403 with header "Server: cloudflare"
    val ex = assertFailsWith<IOException> { client.newCall(Request(server.url("/"))).execute() }
    assertEquals("Cloudflare-protected; not supported on desktop yet", ex.message)
}
@Test fun normalResponsePassesThrough() { /* 200 -> body returned */ }
```

- [ ] **Step 3: Run JVM compile/tests to verify failure**

Run: `./gradlew :mihonsupport:jvmTest`
Expected: compile errors (missing JVM `AndroidCookieJar`/`CloudflareInterceptor`/`mihonApiService`, possibly missing stubs).

- [ ] **Step 4: Implement JVM replacements**

Create the JVM files in Interfaces. If shared files still fail to compile on JVM because of an `android.*` type, add the minimal stub to `kmpextensionloader` jvmMain (Task 1 location) rather than forking the shared file; fork only if the use is WebView-bound.

- [ ] **Step 5: Run both platforms**

Run: `./gradlew :mihonsupport:jvmTest :mihonsupport:testAndroidHostTest`
Expected: PASS on both. `QuickJsShimTest` passing on `jvmTest` proves zipline's QuickJS native loads on the host JVM.

- [ ] **Step 6: Commit**

```bash
git add mihonsupport
git commit -m "feat(mihon): compile vendored Mihon runtime for JVM with desktop cookie jar and Cloudflare stub"
```

---

### Task 3: APK inspection (manifest + signature) on JVM

**Files:**
- Modify: `gradle/android.versions.toml` (add `apksig = "com.android.tools.build:apksig:9.4.1"` — same version as AGP in `libs.versions.toml`; version refs can't cross catalogs)
- Modify: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/ApkManifestParser.kt` (add `val versionCode: Long` to `ApkManifest`, from `apk.apkMeta.versionCode`)
- Modify: `mihonsupport/build.gradle.kts` (jvmMain: apksig, `"net.dongliu:apk-parser:2.6.10"`)
- Create: `mihonsupport/src/jvmMain/kotlin/com/programmersbox/mihon/MihonApkInspector.kt`
- Create: `mihonsupport/src/jvmTest/resources/fixtures/<extension>.apk`
- Create: `mihonsupport/src/jvmTest/kotlin/com/programmersbox/mihon/MihonApkInspectorTest.kt`

**Interfaces:**
- Produces:

```kotlin
data class MihonApkInfo(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    val label: String,              // tachiyomix.name meta-data, else app label minus "Tachiyomi: "
    val isExtension: Boolean,       // uses-feature "tachiyomi.extension"
    val libVersion: Double?,        // MihonLib.libVersion(meta "tachiyomix.extensionLib" as Float or 0f, versionName)
    val sourceClasses: List<String>,// MihonSourceInstantiator.classNames(pkg, meta "tachiyomi.extension.class")
    val nsfw: Boolean,              // meta "tachiyomi.extension.nsfw" == "1"
    val fingerprints: List<String>, // lowercase hex SHA-256 of signer cert encodings; empty if unsigned/invalid
)
object MihonApkInspector { fun inspect(apk: File): MihonApkInfo }
```

Fingerprint order must match Android's: if the APK has a v3 signing-certificate lineage, use its certificates oldest→newest (`signingInfo.signingCertificateHistory`); otherwise the signer certificates. `ApkVerifier.Builder(apk).build().verify()`; `!result.isVerified` → empty list.

- [ ] **Step 1: Add the fixture**

Download one small `ParsedHttpSource`-based English extension APK from the Keiyoushi repo (`https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json`, APK under `/apk/`). Record in a comment at the top of the test: package name, versionName, versionCode, source class value, and the repo's `signingKeyFingerprint` from `repo.json`.

- [ ] **Step 2: Write failing tests**

```kotlin
@Test fun readsFixtureManifest() {
    val info = MihonApkInspector.inspect(fixture)
    assertTrue(info.isExtension)
    assertEquals(FIXTURE_PACKAGE, info.packageName)
    assertEquals(FIXTURE_VERSION_CODE, info.versionCode)
    assertTrue(MihonLib.isSupported(info.libVersion))
    assertTrue(info.sourceClasses.isNotEmpty() && info.sourceClasses.all { it.startsWith("eu.kanade.tachiyomi.extension") })
}
@Test fun fixtureSignedByRepoKey() {
    assertEquals(KEIYOUSHI_FINGERPRINT, MihonApkInspector.inspect(fixture).fingerprints.last())
}
@Test fun tamperedApkHasNoFingerprints() {
    // copy fixture, flip a byte inside classes.dex entry data, inspect -> fingerprints empty
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :mihonsupport:jvmTest --tests "com.programmersbox.mihon.MihonApkInspectorTest"`
Expected: FAIL (unresolved `MihonApkInspector`).

- [ ] **Step 4: Implement `MihonApkInspector.inspect`** per Interfaces.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :mihonsupport:jvmTest --tests "com.programmersbox.mihon.MihonApkInspectorTest" :sharedutils:kmpextensionloader:jvmTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add gradle/android.versions.toml sharedutils/kmpextensionloader mihonsupport
git commit -m "feat(mihon): inspect Mihon APK manifest and verify signatures on JVM"
```

---

### Task 4: Desktop APK store (install, uninstall, versions)

**Files:**
- Create: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/ApkExtensionStore.kt`
- Create: `mihonsupport/src/jvmMain/kotlin/com/programmersbox/mihon/MihonApkStore.kt`
- Create: `mihonsupport/src/jvmTest/kotlin/com/programmersbox/mihon/MihonApkStoreTest.kt`

**Interfaces:**
- Produces (kmpextensionloader):

```kotlin
enum class ApkInstallFailure { INVALID, INCOMPATIBLE, STORAGE }
class ApkInstallException(val failure: ApkInstallFailure, message: String, cause: Throwable? = null) : Exception(message, cause)

interface ApkExtensionStore {
    /** Installs [apk] (a downloaded temp file, consumed). Throws [ApkInstallException]. Returns the package name. */
    suspend fun install(apk: File): String
    /** Installed versionName for [packageName], or null if this store doesn't own it. */
    fun versionName(packageName: String): String?
}
```

- Produces (mihonsupport):

```kotlin
data class InstalledMihonApk(val apk: File, val jar: File, val info: MihonApkInfo)

class MihonApkStore(
    private val apkDir: File,
    private val jarCacheDir: File,
    private val inspect: (File) -> MihonApkInfo = MihonApkInspector::inspect,   // test seam
    private val convert: (File, File) -> File? = DexConverter::convert,         // test seam
) : ApkExtensionStore {
    override suspend fun install(apk: File): String
    suspend fun uninstall(packageName: String): Boolean
    /** Every installed APK with its converted jar (converting if the jar is missing). Bad entries are skipped and logged. */
    fun installed(): List<InstalledMihonApk>
    override fun versionName(packageName: String): String?
    /** Deletes converted jars that belong to no installed APK; ignores files the OS refuses to delete (Windows locks). */
    fun cleanupStaleJars()
}
```

Rules: `install` inspects first; `!isExtension` → `INVALID`; `!MihonLib.isSupported(libVersion)` → `INCOMPATIBLE`; `DexConverter.convert(apk, jarCacheDir) == null` → `INVALID`; IO failure writing → `STORAGE`. Nothing is written to `apkDir` unless all checks pass. Stored as `apkDir/<packageName>.apk` (atomic move, replacing). Jar = `DexConverter.convert` output (SHA-256-named, so a new version gets a new jar file and the old one is left for `cleanupStaleJars`). Trust is **not** checked at install (Android doesn't either).

- [ ] **Step 1: Write failing tests** (temp dirs, fixture from Task 3)

```kotlin
@Test fun installStoresApkAndJar()            // install returns FIXTURE_PACKAGE; installed() has 1 entry, jar exists; versionName == FIXTURE_VERSION_NAME
@Test fun installRejectsNonExtension()        // a plain zip renamed .apk -> ApkInstallException(INVALID); apkDir empty
@Test fun installRejectsUnsupportedLib()      // test seam: MihonApkStore(..., inspect = { fixtureInfo.copy(libVersion = 1.2) }) -> INCOMPATIBLE; apkDir empty
@Test fun uninstallDeletesApkAndJar()         // returns true; installed() empty; versionName null
@Test fun installReplacesApkAndKeepsOldJarUntilCleanup() // install fixture, then install same pkg with different bytes (re-signed not needed: use inspect seam) -> one apk, old jar still present until cleanupStaleJars() removes it
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :mihonsupport:jvmTest --tests "com.programmersbox.mihon.MihonApkStoreTest"`
Expected: FAIL (unresolved).

- [ ] **Step 3: Implement `ApkExtensionStore` and `MihonApkStore`** per Interfaces.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :mihonsupport:jvmTest --tests "com.programmersbox.mihon.MihonApkStoreTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add sharedutils/kmpextensionloader mihonsupport
git commit -m "feat(mihon): desktop store for installed Mihon APKs and converted jars"
```

---

### Task 5: JVM loader, catalog, runtime and Koin module

**Files:**
- Create: `mihonsupport/src/jvmMain/kotlin/com/programmersbox/mihon/ChildFirstClassLoader.kt`
- Create: `mihonsupport/src/jvmMain/kotlin/com/programmersbox/mihon/JvmMihonExtensionLoader.kt`
- Create: `mihonsupport/src/jvmMain/kotlin/com/programmersbox/mihon/MihonCatalog.kt`
- Create: `mihonsupport/src/jvmMain/kotlin/com/programmersbox/mihon/MihonModule.kt`
- Create: `mihonsupport/src/jvmTest/kotlin/com/programmersbox/mihon/JvmMihonExtensionLoaderTest.kt`
- Create: `mihonsupport/src/jvmTest/kotlin/com/programmersbox/mihon/MihonCatalogJvmTest.kt`

**Interfaces:**
- Consumes: `MihonApkStore.installed()/uninstall()/cleanupStaleJars()` (Task 4), `MihonApkInfo` (Task 3), `MihonSourceInstantiator`, `mihonApiService`, `UntrustedMihonApiService`, `MihonRuntime.init` (Task 2), `MihonTrust`, `MihonLib`, `MihonRepoClient`.
- Produces:

```kotlin
/** Child-first like Android's DelegateLastClassLoader; JDK (java.*, javax.*, jdk.*, sun.*) and kotlin.* always parent-first. */
class ChildFirstClassLoader(urls: Array<URL>, parent: ClassLoader) : URLClassLoader(urls, parent)

class JvmMihonExtensionLoader(private val store: MihonApkStore, private val parentClassLoader: ClassLoader = MihonCatalog::class.java.classLoader) {
    fun load(catalog: MihonCatalog, repoSigningKeys: Set<String>, trustedIds: Set<String>, onTrust: suspend (String) -> Unit): List<KmpSourceInformation>
    fun isExtension(packageName: String): Boolean
    fun close()   // closes class loaders from the previous load
}

class MihonCatalog(
    private val application: android.app.Application,
    private val settings: NewSettingsHandling,
    private val repoClient: MihonRepoClient,
    private val store: MihonApkStore,
) : KmpExternalCustomApiServicesCatalog {
    // name = "Mihon", hasRemoteSources = true; initialize/getSources/shouldReload/getRemoteSources as Android's MihonCatalog
    override suspend fun uninstall(source: KmpSourceInformation): Boolean // store.uninstall(source.packageName)
}

val mihonModule: Module  // JVM
```

Loader decision order and reasons mirror Android `MihonExtensionLoader.loadPackage` exactly (`"Malformed"`, `"Unsupported lib $lib"`, `"Unsigned"`, `"Untrusted"` with `canTrust = true`, `"Failed to load: ${t.message}"`); trust id = `MihonTrust.trustId(pkg, versionCode, fingerprints.last())`. One bad package never aborts the rest. Each `load` closes the previous loaders (`close()`) and calls `store.cleanupStaleJars()` afterwards.

JVM `mihonModule`:
- `single { MihonRepoClient(HttpClient(OkHttp) { engine { config { followSslRedirects(false) } } }) }`
- `single { MihonApkStore(File(get<AppDirs>().getUserDataDir(), "mihon-extensions"), File(get<AppDirs>().getUserCacheDir(), "mihon-jars")) } bind ApkExtensionStore::class`
- `single { MihonCatalog(Application("mihon", File(get<AppDirs>().getUserDataDir(), "mihon-data")), get(), get(), get()) } bind KmpExternalCustomApiServicesCatalog::class`

(`AppDirs` is `ca.gosyer.appdirs.AppDirs`, already bound in `kmpuiviews` `AppModule.jvm.kt`; add `desktopLibs.kotlin.multiplatform.appdirs` to mihonsupport jvmMain.)

- [ ] **Step 1: Write failing tests** (fixture APK installed into a temp `MihonApkStore`)

```kotlin
@Test fun repoSignedFixtureLoadsAsMihonApiService() {
    val sources = loader.load(catalog, setOf(KEIYOUSHI_FINGERPRINT), emptySet()) {}
    assertTrue(sources.isNotEmpty())
    assertTrue(sources.all { it.apiService is MihonApiService && it.apiService.serviceName.startsWith("mihon:") })
    assertTrue(sources.none { it.apiService is KmpConfigurableApiService })
}
@Test fun unknownSignerIsUntrustedAndTrustable() {
    val s = loader.load(catalog, emptySet(), emptySet()) {}.single()
    val api = assertIs<UntrustedMihonApiService>(s.apiService)
    assertTrue(api.canTrust); assertEquals("Untrusted", api.reason)
}
@Test fun trustIdLoadsWithoutRepoKey() {
    val id = MihonTrust.trustId(FIXTURE_PACKAGE, FIXTURE_VERSION_CODE, KEIYOUSHI_FINGERPRINT)
    assertIs<MihonApiService>(loader.load(catalog, emptySet(), setOf(id)) {}.first().apiService)
}
@Test fun onTrustReceivesExactTrustId()  // call trust() on the untrusted service; captured id == trustId(...)
@Test fun brokenPackageDoesNotAbortOthers() // store with fixture + a seam entry whose jar is garbage -> fixture still loads; broken one reports "Failed to load: ..."
```

`MihonCatalogJvmTest`:
```kotlin
@Test fun initializeSurvivesOfflineRepo()   // MihonRepoClient with Ktor MockEngine that throws; settings customUrls = one https url; initialize() does not throw; fixture appears as Untrusted
@Test fun uninstallRemovesFromStore()       // uninstall(source) == true; store.installed() empty
```

If a configurable extension is easy to pick as a second fixture, add `configurableFixtureLoads` (Review Focus 1).

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :mihonsupport:jvmTest --tests "com.programmersbox.mihon.JvmMihonExtensionLoaderTest" --tests "com.programmersbox.mihon.MihonCatalogJvmTest"`
Expected: FAIL (unresolved).

- [ ] **Step 3: Implement** `ChildFirstClassLoader`, `JvmMihonExtensionLoader`, JVM `MihonCatalog`, JVM `mihonModule` per Interfaces. `MihonCatalog.initialize()` calls `MihonRuntime.init(application)` first.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :mihonsupport:jvmTest :mihonsupport:testAndroidHostTest`
Expected: PASS. If the fixture fails with `NoClassDefFoundError`/`NoSuchMethodError` for an `android.*`/`androidx.*` type, add that stub/member in Task 1's location and note it in the commit message.

- [ ] **Step 5: Commit**

```bash
git add mihonsupport sharedutils/kmpextensionloader
git commit -m "feat(mihon): load trusted Mihon extensions on desktop via child-first class loader"
```

---

### Task 6: Desktop source registration and installed versions

**Files:**
- Modify: `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/kmpextensionloader/SourceLoader.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.jvm.kt:63-70`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/SourceInfoRepository.jvm.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/di/RepositoryModule.jvm.kt:22`
- Create: `sharedutils/kmpextensionloader/src/jvmTest/kotlin/com/programmersbox/kmpextensionloader/SourceLoaderNativeCatalogTest.kt`
- Modify: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/SourceInfoRepositoryTest.kt`

**Interfaces:**
- Consumes: `NativeCatalogs` (`kmpmodels`, already bound by common `AppModule.kt`), `ApkExtensionStore` (Task 4).
- Produces: JVM `SourceLoader(extensionsDir, sourceType, sourceRepository, appDirs, nativeCatalogs: NativeCatalogs = NativeCatalogs(emptyList()))` — `load()`/`blockingLoad()` set `jar sources + native sources` sorted by `serviceName`, with native sources gathered exactly like Android's `nativeSources()` (per-catalog `initialize()` + `getSources()`, failures logged and skipped, `CancellationException` rethrown).
- Produces: JVM `SourceInfoRepository(stores: List<ApkExtensionStore> = emptyList())`; `versionName(info)` = first non-null `store.versionName(info.packageName)`, else `info.name` (today's behavior). Koin: `single { SourceInfoRepository(getAll()) }`.

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun nativeCatalogSourcesAreMerged()    // empty extensionsDir + fake KmpExternalCustomApiServicesCatalog returning 2 sources -> FakeSourceRepository gets both, sorted by serviceName
@Test fun failingCatalogIsSkipped()          // one catalog throws in initialize(), another succeeds -> only the good one's sources
```
`SourceInfoRepositoryTest`:
```kotlin
@Test fun versionFromStore()                 // store returns "1.4.12" for pkg -> versionName == "1.4.12"
@Test fun fallsBackToName()                  // no store owns pkg -> name (existing behavior)
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :sharedutils:kmpextensionloader:jvmTest :kmpuiviews:jvmTest --tests "*SourceLoaderNativeCatalogTest" --tests "*SourceInfoRepositoryTest"`
Expected: FAIL.

- [ ] **Step 3: Implement** per Interfaces; in `AppModule.jvm.kt` pass `nativeCatalogs = get()`.

- [ ] **Step 4: Run tests to verify they pass**

Run: same as Step 2. Expected: PASS. Also `./gradlew :animeworld:desktop:compileKotlinJvm` (shares `AppModule.jvm.kt`) compiles.

- [ ] **Step 5: Commit**

```bash
git add sharedutils/kmpextensionloader kmpuiviews
git commit -m "feat(desktop): merge native catalogs into desktop sources, report installed versions from stores"
```

---

### Task 7: Real desktop download-and-install

**Files:**
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/DownloadStateRepository.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/di/RepositoryModule.jvm.kt:20`
- Create: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/repository/DownloadStateRepositoryTest.kt`

**Interfaces:**
- Consumes: `ApkExtensionStore`, `ApkInstallException`, `ApkInstallFailure` (Task 4); `SourceLoader.blockingLoad()` (Task 6); `DownloadStateInterface`, `DownloadAndInstallState`, `DownloadAndInstallStatus`, `InstallErrorReason` (existing, commonMain).
- Produces:

```kotlin
class DownloadStateRepository(
    private val client: HttpClient,
    private val stores: List<ApkExtensionStore>,
    private val tempDir: File,
    private val reload: suspend () -> Unit,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : DownloadStateInterface
```

Behavior:
- `downloadAndInstall(url)` and `downloadThenInstall(url)` both: add a `DownloadAndInstallState(url, name = last path segment, id = UUID, status)` to `downloadList` (a `MutableStateFlow`), stream the body to a temp file emitting `Downloading(progress)` (`progress` in 0f..1f from `Content-Length`; `0f` if unknown), then `Installing`, then `stores.first().install(file)`, then `reload()`, then `Installed`.
- Error mapping: no store bound → `Error(UNKNOWN, "No extension installer available")`; `ApkInstallException` `INVALID`→`INVALID`, `INCOMPATIBLE`→`INCOMPATIBLE`, `STORAGE`→`STORAGE`; non-2xx or network exception → `GENERIC` with the message. Temp file always deleted.
- `cancelDownload(id)` cancels that job and sets `Cancelled`.
- `install(url)` returns `emptyFlow()` (the desktop has no separate "install downloaded file" step).
- Koin: `single { DownloadStateRepository(HttpClient(), getAll(), File(get<AppDirs>().getUserCacheDir(), "extension-downloads"), reload = { get<SourceLoader>().blockingLoad() }) } bind DownloadStateInterface::class` (use whatever Ktor engine `kmpuiviews` jvmMain already uses).

- [ ] **Step 1: Write failing tests** (Ktor `MockEngine`, fake `ApkExtensionStore`, recorded `reload`)

```kotlin
@Test fun successGoesDownloadingInstallingInstalledAndReloads()  // statuses observed in order end with Installed; store got file; reload called once
@Test fun invalidApkReportsInvalid()             // store throws ApkInstallException(INVALID) -> final Error(INVALID, msg); reload not called
@Test fun incompatibleReportsIncompatible()
@Test fun http404ReportsGeneric()
@Test fun noStoreReportsUnknown()
@Test fun cancelSetsCancelled()                  // MockEngine suspends; cancelDownload(id) -> Cancelled
@Test fun tempFileDeletedAfterwards()            // tempDir empty after success and after failure
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :kmpuiviews:jvmTest --tests "*DownloadStateRepositoryTest"`
Expected: FAIL.

- [ ] **Step 3: Implement** per Interfaces.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :kmpuiviews:jvmTest --tests "*DownloadStateRepositoryTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews
git commit -m "feat(desktop): download and install extensions with progress and error states"
```

---

### Task 8: Wire into MangaWorld Desktop and verify end to end

**Files:**
- Modify: `mangaworld/desktop/build.gradle.kts` (add `implementation(projects.mihonsupport)` to `commonMain`/`jvmMain` deps, matching how other projects are listed)
- Modify: `mangaworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt` (include `mihonModule` in `moduleBlock`)
- Modify: stub files from Task 1 as the smoke test demands
- Modify: `CHANGELOG.md` (entry under the unreleased section, matching existing format)

- [ ] **Step 1: Wire**

Add the dependency and `includes(mihonModule)` (or the module-list equivalent used by `desktopSetup`'s `moduleBlock`).

- [ ] **Step 2: Build everything touched**

Run: `./gradlew :mangaworld:desktop:compileKotlinJvm :animeworld:desktop:compileKotlinJvm :mangaworld:assembleNoFirebaseDebug :mihonsupport:allTests :sharedutils:kmpextensionloader:jvmTest :kmpuiviews:jvmTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual smoke on macOS**

Run: `./gradlew :mangaworld:desktop:run`
1. Settings → Extensions → add `https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json` (or the repo URL format the Android app accepts). Expected: Mihon catalog lists extensions with language/18+ chips.
2. Install a Jsoup/`ParsedHttpSource` English extension and MangaDex. Expected: progress shown, then both appear under Installed and in the source list.
3. For each: browse recent → open details → chapter list → open reader; pages load.
4. Install an extension whose `versionCode` is older than the repo's (copy an older APK into the store dir, restart). Expected: update badge; Update installs the new version.
5. Uninstall one. Expected: disappears from Installed and the source list.
6. Install a configurable extension. Expected: it loads and works; no settings button.
7. Install a known Cloudflare-protected extension. Expected: browsing shows the "Cloudflare-protected; not supported on desktop yet" error.
For any `NoClassDefFoundError`/`NoSuchMethodError` on an `android.*`/`androidx.*` type, add the stub/member (Task 1 location) with a test in `AndroidStubShapeTest`, rerun.

- [ ] **Step 4: Add CHANGELOG entry**

One line under unreleased: "MangaWorld Desktop: install and use Mihon extensions from Mihon repos (non-Cloudflare sources; source settings not yet available on desktop)."

- [ ] **Step 5: Commit**

```bash
git add mangaworld/desktop sharedutils/kmpextensionloader CHANGELOG.md
git commit -m "feat(mangaworld-desktop): enable Mihon extensions"
```
