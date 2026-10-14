# Native Mihon Extension Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** MangaWorld loads Mihon extension APKs in-process through a new KMP module `:mihonsupport`, maps them to OtakuWorld's `Kmp*` models, installs/updates them from Mihon repos, and shows their preference screens.

**Architecture:** `:mihonsupport` vendors Mihon's extension-facing runtime (`eu.kanade.tachiyomi.*`, unchanged package names) into `androidMain`, adds a loader (`DelegateLastClassLoader`, lib-version gate, signature trust), a mapper (`MihonApiService : KmpApiService`), and a catalog (`MihonCatalog : KmpExternalCustomApiServicesCatalog`). Platform-free code (repo index client, lib/trust rules) lives in `commonMain`. The app wires the catalog in through Koin (`NativeCatalogs`), so shared modules never depend on `:mihonsupport`.

**Tech Stack:** Kotlin Multiplatform (`otaku-multiplatform` convention plugin, AGP KMP Android library), Koin, Ktor client (commonMain), OkHttp 5, RxJava 1, Injekt, Jsoup, QuickJS, androidx.preference, fragment-compose, Navigation3, Wire protobuf (datastore).

**Spec:** `docs/superpowers/specs/2026-10-13-native-mihon-support-design.md` (read the "Plan refinements" section at the end — it supersedes earlier spec text where they differ).

## Global Constraints

- Vendored Mihon code is pinned to commit `446f6df82c390ab760802c2fce3b677bce3f31c1` and keeps its original package names (`eu.kanade.tachiyomi.*`, `tachiyomi.core.common.*`, `mihon.core.common.*`). Every vendored file starts with `// Vendored from mihonapp/mihon@446f6df8 (Apache-2.0). Local edits marked with "OTAKU:".`
- Supported extension lib versions: exactly `{1.4, 1.6}` (not a range).
- Extension feature `tachiyomi.extension`; metadata keys `tachiyomi.extension.class`, `tachiyomi.extension.factory`, `tachiyomi.extension.nsfw`, `tachiyomix.extensionLib`, `tachiyomix.name`.
- `serviceName` of a loaded Mihon source = `"mihon:${source.id}"`; of an untrusted/failed extension = `"mihon-untrusted:<pkg>"`.
- User-trust id format: `"<pkg>:<versionCode>:<sha256-lowercase-hex>"`.
- Source preferences SharedPreferences name: `"source_<id>"` (Mihon's `ConfigurableSource.preferenceKey()`).
- Dependency versions match Mihon's catalog where extensions bind to them: injekt `1.16.1`, rxjava `1.3.8`, quickjs `com.github.zhanghai.quickjs-java:quickjs-android:547f5b1597`, logcat `0.4`, jsoup/okhttp/kotlinx-serialization = versions already in this repo.
- Android only for runtime behavior; `jvm` and `ios` targets must still compile (`:mihonsupport:compileKotlinJvm`).
- `:kmpuiviews`, `:kmpmodels`, `:sharedutils:kmpextensionloader` must NOT depend on `:mihonsupport`.
- Local builds use the `noFirebase` flavor.

## Review Focus

1. A favorite/history item reopened from the database has no `extras` (no `mihonUrl`, no memo) → `itemInfo` must still work by stripping `baseUrl` from the stored absolute URL. Test owned by Task 4.
2. Repo URLs typed by users vary (`.../index.min.json`, `.../repo.json`, trailing `/`, surrounding whitespace) → all resolve to the same repo base. Test owned by Task 5.
3. An extension class that fails to load (missing class, `LinkageError`, constructor throws) → that package shows as a "Failed to load" entry; the app and other extensions keep working. Test owned by Task 7.
4. The installed-extensions screen shows a group-level uninstall button per catalog; for the "Mihon" group it would uninstall a random extension → hide it when the group spans more than one package. Test owned by Task 8.
5. Chapters with `date_upload == 0` and pages whose `imageUrl` is already set → `uploaded == ""`, `uploadedTime == null`, and no `getImageUrl` network call. Test owned by Task 4.

---

## File Structure

```
mihonsupport/
  build.gradle.kts
  VENDORED.md                                 pinned commit + file list + local edits
  scripts/vendor-mihon.sh                     re-fetches vendored files at the pinned commit
  src/commonMain/kotlin/com/programmersbox/mihon/
    MihonLib.kt                               lib-version parse/gate
    MihonTrust.kt                             trust decision + trust id
    repo/MihonRepoModels.kt                   @Serializable repo.json / index.min.json
    repo/MihonRepoClient.kt                   fetch + normalize + map to KmpRemoteSources
  src/commonTest/kotlin/com/programmersbox/mihon/
    MihonLibTest.kt, MihonTrustTest.kt, repo/MihonRepoClientTest.kt
  src/androidMain/kotlin/
    eu/kanade/tachiyomi/source/**             vendored (source-api)
    eu/kanade/tachiyomi/util/JsoupExtensions.kt   vendored
    eu/kanade/tachiyomi/network/**            vendored + hand-written NetworkHelper, JavaScriptEngine
    eu/kanade/tachiyomi/AppInfo.kt            hand-written
    eu/kanade/tachiyomi/util/system/{DeviceUtil,WebViewUtil}.kt  vendored (Task 3)
    tachiyomi/core/common/util/lang/RxCoroutineBridge.kt     vendored shim
    tachiyomi/core/common/util/system/LogcatExtensions.kt    vendored shim
    mihon/core/common/extensions/JsonObject.kt               vendored shim
    com/programmersbox/mihon/
      MihonRuntime.kt                         Injekt bootstrap
      MihonApiService.kt                      CatalogueSource -> KmpApiService
      MihonSourceMapper.kt                    SManga/SChapter/Page <-> Kmp models
      UntrustedMihonApiService.kt
      loader/MihonExtensionLoader.kt
      MihonCatalog.kt
      MihonModule.kt                          Koin module
      preferences/MihonPreferenceFragment.kt
  src/androidHostTest/kotlin/com/programmersbox/mihon/
    MihonSourceMapperTest.kt, FakeSources.kt, loader/SourceInstantiationTest.kt, VendoredRuntimeTest.kt

kmpmodels/src/commonMain/.../ApiService.kt    + KmpConfigurableApiService, KmpUntrustedApiService, NativeCatalogs
datastore/src/commonMain/proto/settings.proto + trustedMihonExtensions = 37
datastore/.../SettingsHandling.kt             + trusted id helpers
sharedutils/kmpextensionloader/.../androidMain/SourceLoader.kt   + nativeCatalogs
kmpuiviews/.../di/AppModule.kt                + single { NativeCatalogs(getAll()) }
kmpuiviews/.../di/AppModule.android.kt        pass nativeCatalogs to SourceLoader
kmpuiviews/.../presentation/Screen.kt         + SourcePreferences
kmpuiviews/.../settings/extensions/ExtensionListViewModel.kt  native catalogs, trust()
kmpuiviews/.../settings/extensions/ExtensionListScreen.kt     trust + gear + group uninstall guard
mangaworld/build.gradle.kts                   depend on :mihonsupport, fragment-compose
mangaworld/.../GenericManga.kt                include mihonModule, nav entry
mangaworld/.../MihonPreferencesScreen.kt      Compose host for the fragment
settings.gradle.kts                           include(":mihonsupport")
gradle/android.versions.toml                  new aliases
```

---

### Task 1: Module scaffold

**Files:**
- Create: `mihonsupport/build.gradle.kts`, `mihonsupport/VENDORED.md`, `mihonsupport/src/commonMain/kotlin/com/programmersbox/mihon/MihonLib.kt`, `mihonsupport/src/commonTest/kotlin/com/programmersbox/mihon/MihonLibTest.kt`
- Modify: `settings.gradle.kts` (after `include(":kmpmodels:extensioninterfaces")`), `gradle/android.versions.toml`

**Interfaces:**
- Produces: Gradle project `:mihonsupport` (accessor `projects.mihonsupport`); `object MihonLib { val SUPPORTED: Set<Double>; fun libVersion(metadataLib: Float, versionName: String?): Double?; fun libVersion(versionName: String): Double?; fun isSupported(lib: Double?): Boolean }`.

- [ ] **Step 1: Add catalog aliases** to `gradle/android.versions.toml` under `[libraries]` (keep alphabetical-ish placement near related entries; `okhttpLib`, `okhttpDns`, `brotli`, `jsoup`, `preference`, `androidxWebkit` already exist):

```toml
injekt = "uy.kohesive.injekt:injekt-core:1.16.1"
rxjava1 = "io.reactivex:rxjava:1.3.8"
quickjsAndroid = "com.github.zhanghai.quickjs-java:quickjs-android:547f5b1597"
squareLogcat = "com.squareup.logcat:logcat:0.4"
fragmentCompose = "androidx.fragment:fragment-compose:1.9.0"
okhttpMockWebServer = { module = "com.squareup.okhttp3:mockwebserver3", version.ref = "okhttpVersion" }
okhttpLogging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttpVersion" }
```

- [ ] **Step 2: Register the module** in `settings.gradle.kts`:

```kotlin
include(":mihonsupport")
```

- [ ] **Step 3: Create `mihonsupport/build.gradle.kts`**

```kotlin
plugins {
    `otaku-multiplatform`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.mihon"
}

kotlin {
    android {
        namespace = "com.programmersbox.mihon"
        withHostTestBuilder {}.configure {
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(commonLibs.coroutinesCore)
                implementation(commonLibs.kotlinxSerialization)
                implementation(commonLibs.ktorCore)
                implementation(projects.kmpmodels)
            }
        }

        commonTest {
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
                implementation(commonLibs.ktorMock)
            }
        }

        androidMain {
            dependencies {
                implementation(commonLibs.ktorOkHttp)
                implementation(projects.datastore)
                implementation(project.dependencies.platform(commonLibs.koin.bom))
                implementation(commonLibs.koinCores)
                implementation(androidLibs.okhttpLib)
                implementation(androidLibs.okhttpDns)
                implementation(androidLibs.okhttpLogging)
                implementation(androidLibs.brotli)
                implementation(androidLibs.jsoup)
                implementation(androidLibs.injekt)
                implementation(androidLibs.rxjava1)
                implementation(androidLibs.quickjsAndroid)
                implementation(androidLibs.squareLogcat)
                implementation(androidLibs.preference)
                implementation(androidLibs.androidxWebkit)
            }
        }

        getByName("androidHostTest") {
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
                implementation(androidLibs.okhttpMockWebServer)
            }
        }
    }
}
```

If `withHostTestBuilder` is rejected by the AGP version in use, run `./gradlew :mihonsupport:tasks --all | grep -i host` to find the exact DSL; the host test source set must be named `androidHostTest` and its task `testAndroidHostTest`.

- [ ] **Step 4: Write the failing test** `mihonsupport/src/commonTest/kotlin/com/programmersbox/mihon/MihonLibTest.kt`

```kotlin
package com.programmersbox.mihon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MihonLibTest {
    @Test
    fun metadataLibWins() {
        assertEquals(1.6, MihonLib.libVersion(1.6f, "1.4.100"))
    }

    @Test
    fun fallsBackToVersionName() {
        assertEquals(1.4, MihonLib.libVersion(0f, "1.4.190"))
        assertEquals(1.4, MihonLib.libVersion("1.4.190"))
    }

    @Test
    fun unparseable() {
        assertNull(MihonLib.libVersion(0f, null))
        assertNull(MihonLib.libVersion("abc"))
    }

    @Test
    fun onlyExactVersionsSupported() {
        assertTrue(MihonLib.isSupported(1.4))
        assertTrue(MihonLib.isSupported(1.6))
        assertFalse(MihonLib.isSupported(1.3))
        assertFalse(MihonLib.isSupported(1.5))
        assertFalse(MihonLib.isSupported(1.7))
        assertFalse(MihonLib.isSupported(null))
    }
}
```

- [ ] **Step 5: Run it to verify it fails**

Run: `./gradlew :mihonsupport:jvmTest --tests "com.programmersbox.mihon.MihonLibTest"`
Expected: compilation FAIL, `Unresolved reference 'MihonLib'`.

- [ ] **Step 6: Implement** `mihonsupport/src/commonMain/kotlin/com/programmersbox/mihon/MihonLib.kt`

```kotlin
package com.programmersbox.mihon

/** Mihon extension lib version rules. Mirrors Mihon's ExtensionLoader.SUPPORTED_LIB_VERSIONS. */
object MihonLib {
    val SUPPORTED: Set<Double> = setOf(1.4, 1.6)

    /** Lib version from `tachiyomix.extensionLib` metadata, falling back to the versionName prefix. */
    fun libVersion(metadataLib: Float, versionName: String?): Double? =
        metadataLib.takeUnless { it == 0f }?.toString()?.toDouble()
            ?: versionName?.let(::libVersion)

    /** "1.4.190" -> 1.4 */
    fun libVersion(versionName: String): Double? = versionName.substringBeforeLast('.').toDoubleOrNull()

    fun isSupported(lib: Double?): Boolean = lib != null && lib in SUPPORTED
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew :mihonsupport:jvmTest --tests "com.programmersbox.mihon.MihonLibTest"`
Expected: PASS (4 tests).

- [ ] **Step 8: Create `mihonsupport/VENDORED.md`**

```markdown
# Vendored Mihon code

Source: https://github.com/mihonapp/mihon at `446f6df82c390ab760802c2fce3b677bce3f31c1` (Apache-2.0).

Extension APKs are compiled against `extensions-lib` stubs, so the host app must provide these
classes with their original package names. Re-sync with `scripts/vendor-mihon.sh <sha>`, then
re-apply every edit marked `OTAKU:` (listed below).

## Files (copied verbatim unless noted)

(filled in by Tasks 2 and 3)

## Hand-written replacements

(filled in by Tasks 2 and 3)
```

- [ ] **Step 9: Verify all targets compile**

Run: `./gradlew :mihonsupport:compileKotlinJvm :mihonsupport:compileAndroidMain`
Expected: BUILD SUCCESSFUL. (If `compileAndroidMain` is not the task name, use `./gradlew :mihonsupport:assemble`.)

- [ ] **Step 10: Commit**

```bash
git add settings.gradle.kts gradle/android.versions.toml mihonsupport
git commit -m "feat(mihon): scaffold mihonsupport KMP module with lib version rules"
```

---

### Task 2: Vendored extension runtime (source API + network surface)

**Files:**
- Create: `mihonsupport/scripts/vendor-mihon.sh`
- Create (vendored by script): see Step 1 list, under `mihonsupport/src/androidMain/kotlin/`
- Create (hand-written): `mihonsupport/src/androidMain/kotlin/eu/kanade/tachiyomi/network/NetworkHelper.kt`, `.../network/JavaScriptEngine.kt`, `.../eu/kanade/tachiyomi/AppInfo.kt`, `.../com/programmersbox/mihon/MihonRuntime.kt`
- Test: `mihonsupport/src/androidHostTest/kotlin/com/programmersbox/mihon/VendoredRuntimeTest.kt`
- Modify: `mihonsupport/VENDORED.md`

**Interfaces:**
- Consumes: Task 1 module.
- Produces: the full `eu.kanade.tachiyomi.source.*` API (`Source`, `CatalogueSource`, `SourceFactory`, `ConfigurableSource`, `HttpSource`, `ParsedHttpSource`, `ResolvableSource`, `SManga`, `SChapter`, `Page`, `MangasPage`, `FilterList`, `SMangaUpdate`); `eu.kanade.tachiyomi.network.NetworkHelper(context: Context)` with `val client: OkHttpClient`, `val cloudflareClient: OkHttpClient`, `val cookieJar: AndroidCookieJar`, `fun defaultUserAgentProvider(): String`; `object MihonRuntime { fun init(application: Application) }`.

- [ ] **Step 1: Write `mihonsupport/scripts/vendor-mihon.sh`**

```bash
#!/usr/bin/env bash
# Re-fetches vendored Mihon files at a pinned commit. Usage: scripts/vendor-mihon.sh [sha]
set -euo pipefail
SHA="${1:-446f6df82c390ab760802c2fce3b677bce3f31c1}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/src/androidMain/kotlin"
BASE="https://raw.githubusercontent.com/mihonapp/mihon/$SHA"
HEADER="// Vendored from mihonapp/mihon@${SHA:0:8} (Apache-2.0). Local edits marked with \"OTAKU:\"."

fetch() { # $1 = repo path, $2 = path under DEST
  mkdir -p "$DEST/$(dirname "$2")"
  { echo "$HEADER"; curl -fsSL "$BASE/$1"; } > "$DEST/$2"
}

SA=source-api/src/main/kotlin
for f in \
  eu/kanade/tachiyomi/source/CatalogueSource.kt \
  eu/kanade/tachiyomi/source/ConfigurableSource.kt \
  eu/kanade/tachiyomi/source/Source.kt \
  eu/kanade/tachiyomi/source/SourceFactory.kt \
  eu/kanade/tachiyomi/source/UnmeteredSource.kt \
  eu/kanade/tachiyomi/source/model/Filter.kt \
  eu/kanade/tachiyomi/source/model/FilterList.kt \
  eu/kanade/tachiyomi/source/model/MangasPage.kt \
  eu/kanade/tachiyomi/source/model/Page.kt \
  eu/kanade/tachiyomi/source/model/SChapter.kt \
  eu/kanade/tachiyomi/source/model/SChapterImpl.kt \
  eu/kanade/tachiyomi/source/model/SManga.kt \
  eu/kanade/tachiyomi/source/model/SMangaImpl.kt \
  eu/kanade/tachiyomi/source/model/SMangaUpdate.kt \
  eu/kanade/tachiyomi/source/model/UpdateStrategy.kt \
  eu/kanade/tachiyomi/source/online/HttpSource.kt \
  eu/kanade/tachiyomi/source/online/ParsedHttpSource.kt \
  eu/kanade/tachiyomi/source/online/ResolvableSource.kt \
  eu/kanade/tachiyomi/util/JsoupExtensions.kt; do
  fetch "$SA/$f" "$f"
done

CC=core/common/src/main/kotlin
for f in \
  eu/kanade/tachiyomi/network/AndroidCookieJar.kt \
  eu/kanade/tachiyomi/network/DohProviders.kt \
  eu/kanade/tachiyomi/network/HttpException.kt \
  eu/kanade/tachiyomi/network/OkHttpExtensions.kt \
  eu/kanade/tachiyomi/network/ProgressListener.kt \
  eu/kanade/tachiyomi/network/ProgressResponseBody.kt \
  eu/kanade/tachiyomi/network/Requests.kt \
  eu/kanade/tachiyomi/network/interceptor/RateLimitInterceptor.kt \
  eu/kanade/tachiyomi/network/interceptor/SpecificHostRateLimitInterceptor.kt \
  eu/kanade/tachiyomi/network/interceptor/UncaughtExceptionInterceptor.kt \
  eu/kanade/tachiyomi/network/interceptor/UserAgentInterceptor.kt \
  tachiyomi/core/common/util/lang/RxCoroutineBridge.kt \
  tachiyomi/core/common/util/system/LogcatExtensions.kt \
  mihon/core/common/extensions/JsonObject.kt; do
  fetch "$CC/$f" "$f"
done

# Task 3 (Cloudflare / WebView) files:
for f in \
  eu/kanade/tachiyomi/network/interceptor/CloudflareInterceptor.kt \
  eu/kanade/tachiyomi/network/interceptor/WebViewInterceptor.kt \
  eu/kanade/tachiyomi/util/system/DeviceUtil.kt \
  eu/kanade/tachiyomi/util/system/WebViewUtil.kt; do
  if [[ "${WITH_WEBVIEW:-0}" == "1" ]]; then fetch "$CC/$f" "$f"; fi
done
```

Run: `chmod +x mihonsupport/scripts/vendor-mihon.sh && mihonsupport/scripts/vendor-mihon.sh`
Expected: files appear under `mihonsupport/src/androidMain/kotlin/eu/...`, `tachiyomi/...`, `mihon/...`.

- [ ] **Step 2: Write hand-written `NetworkHelper.kt`** (replaces Mihon's Metro-injected one; keeps the public surface extensions use). `mihonsupport/src/androidMain/kotlin/eu/kanade/tachiyomi/network/NetworkHelper.kt`:

```kotlin
// OTAKU: hand-written replacement for mihonapp/mihon core/common NetworkHelper (Metro DI and
// NetworkPreferences removed). Public API matches extensions-lib.
package eu.kanade.tachiyomi.network

import android.content.Context
import eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor
import eu.kanade.tachiyomi.network.interceptor.UserAgentInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import java.io.File
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class NetworkHelper(private val context: Context) {

    val cookieJar = AndroidCookieJar()

    private fun clientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30.seconds)
        .readTimeout(30.seconds)
        .callTimeout(2.minutes)
        .cache(Cache(directory = File(context.cacheDir, "mihon_network_cache"), maxSize = 5L * 1024 * 1024))
        .addInterceptor(UncaughtExceptionInterceptor())
        .addInterceptor(UserAgentInterceptor(::defaultUserAgentProvider))
        .addNetworkInterceptor(BrotliInterceptor)

    val client: OkHttpClient by lazy { clientBuilder().build() }

    @Deprecated("The regular client handles Cloudflare by default")
    @Suppress("UNUSED")
    val cloudflareClient: OkHttpClient get() = client

    fun defaultUserAgentProvider(): String = DEFAULT_USER_AGENT

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Mobile Safari/537.36"
    }
}
```

- [ ] **Step 3: Write `JavaScriptEngine.kt` and `AppInfo.kt`**

`mihonsupport/src/androidMain/kotlin/eu/kanade/tachiyomi/network/JavaScriptEngine.kt`:

```kotlin
// OTAKU: copy of mihonapp/mihon JavaScriptEngine with Metro annotations removed.
package eu.kanade.tachiyomi.network

import android.content.Context
import app.cash.quickjs.QuickJs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("UNUSED", "UNCHECKED_CAST")
class JavaScriptEngine(context: Context) {
    /** @since extensions-lib 1.4 */
    suspend fun <T> evaluate(script: String): T = withContext(Dispatchers.Default) {
        QuickJs.create().use { it.evaluate(script) as T }
    }
}
```

`mihonsupport/src/androidMain/kotlin/eu/kanade/tachiyomi/AppInfo.kt`:

```kotlin
// OTAKU: hand-written implementation of the extensions-lib AppInfo surface.
package eu.kanade.tachiyomi

import android.app.Application
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Suppress("UNUSED")
object AppInfo {
    fun getVersionCode(): Int = packageInfo().longVersionCode.toInt()

    fun getVersionName(): String = packageInfo().versionName.orEmpty()

    /** @since extensions-lib 1.5 */
    fun getSupportedImageMimeTypes(): List<String> =
        listOf("image/jpeg", "image/png", "image/gif", "image/webp", "image/avif", "image/heif", "image/jxl")

    private fun packageInfo() = Injekt.get<Application>().let { it.packageManager.getPackageInfo(it.packageName, 0) }
}
```

- [ ] **Step 4: Write `MihonRuntime.kt`** `mihonsupport/src/androidMain/kotlin/com/programmersbox/mihon/MihonRuntime.kt`:

```kotlin
package com.programmersbox.mihon

import android.app.Application
import android.content.Context
import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.fullType

/** Registers what Mihon extensions expect to find in Injekt. Must run before any extension class loads. */
object MihonRuntime {
    @Volatile
    private var initialized = false

    fun init(application: Application) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            Injekt.addSingleton(fullType<Application>(), application)
            Injekt.addSingleton(fullType<Context>(), application)
            Injekt.addSingletonFactory(fullType<NetworkHelper>()) { NetworkHelper(application) }
            Injekt.addSingletonFactory(fullType<JavaScriptEngine>()) { JavaScriptEngine(application) }
            Injekt.addSingletonFactory(fullType<Json>()) {
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            }
            initialized = true
        }
    }
}
```

- [ ] **Step 5: Compile and fix vendored imports**

Run: `./gradlew :mihonsupport:compileAndroidMain`
Expected: BUILD SUCCESSFUL. If a vendored file references something not vendored (e.g. an import from `mihon.core.metro`), do not pull in more Mihon modules: remove the annotation/import and add an `// OTAKU: removed <thing>` comment on that line. Record each such edit in `VENDORED.md`.

- [ ] **Step 6: Write the failing test** `mihonsupport/src/androidHostTest/kotlin/com/programmersbox/mihon/VendoredRuntimeTest.kt` — proves the vendored Rx→suspend bridge and `HttpSource` request path work against a real HTTP server:

```kotlin
package com.programmersbox.mihon

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VendoredRuntimeTest {
    private val server = MockWebServer()

    @BeforeTest fun start() = server.start()
    @AfterTest fun stop() = server.close()

    private inner class RxOnlySource : HttpSource() {
        override val name = "RxOnly"
        override val lang = "en"
        override val supportsLatest = false
        override val baseUrl: String get() = server.url("/").toString().removeSuffix("/")
        override val client: OkHttpClient = OkHttpClient()
        override fun headersBuilder(): Headers.Builder = Headers.Builder().add("Referer", "$baseUrl/")

        override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/popular?page=$page", headers)
        override fun popularMangaParse(response: Response): MangasPage {
            val titles = response.body.string().lines().filter { it.isNotBlank() }
            return MangasPage(titles.map { t -> SManga.create().apply { title = t; url = "/m/$t" } }, hasNextPage = true)
        }

        override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException()
        override fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()
        override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()
        override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException()
        override fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException()
        override fun chapterListParse(response: Response): List<SChapter> = throw UnsupportedOperationException()
        override fun pageListParse(response: Response): List<Page> = throw UnsupportedOperationException()
        override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()
    }

    @Test
    fun suspendApiBridgesToRxRequestPath() = runTest {
        server.enqueue(MockResponse.Builder().body("One\nTwo\n").build())

        val page = RxOnlySource().getPopularManga(1)

        assertEquals(listOf("One", "Two"), page.mangas.map { it.title })
        assertTrue(page.hasNextPage)
        val recorded = server.takeRequest()
        assertEquals("/popular?page=1", recorded.target)
        assertEquals("${server.url("/").toString().removeSuffix("/")}/", recorded.headers["Referer"])
    }
}
```

If the compiler reports that one of the `override fun ...Parse/Request` members does not exist in the vendored `HttpSource`, delete that override; if it reports a missing abstract member, add it with `= throw UnsupportedOperationException()`. If `MockResponse.Builder`/`recorded.target` differ in this OkHttp version, use the equivalents from `mockwebserver3` (`MockResponse(body = ...)`, `recorded.requestUrl?.encodedPath + "?" + query`).

- [ ] **Step 7: Run it**

Run: `./gradlew :mihonsupport:testAndroidHostTest --tests "com.programmersbox.mihon.VendoredRuntimeTest"`
Expected: PASS. (This is a characterization test of copied code, so there is no red phase; if it fails, the vendored copy or an `OTAKU:` edit is wrong — fix that, not the test.)

- [ ] **Step 8: Update `VENDORED.md`** — replace the two placeholder lines with the exact file lists from Step 1 and the hand-written files from Steps 2–4, plus every `OTAKU:` edit made in Step 5.

- [ ] **Step 9: Commit**

```bash
git add mihonsupport
git commit -m "feat(mihon): vendor Mihon extension runtime and Injekt bootstrap"
```

---

### Task 3: Cloudflare / WebView interceptors

**Files:**
- Create (vendored): `eu/kanade/tachiyomi/network/interceptor/CloudflareInterceptor.kt`, `.../interceptor/WebViewInterceptor.kt`, `eu/kanade/tachiyomi/util/system/DeviceUtil.kt`, `.../util/system/WebViewUtil.kt` under `mihonsupport/src/androidMain/kotlin/`
- Modify: `mihonsupport/src/androidMain/kotlin/eu/kanade/tachiyomi/network/NetworkHelper.kt`, `mihonsupport/VENDORED.md`

**Interfaces:**
- Consumes: Task 2 `NetworkHelper`, `AndroidCookieJar`.
- Produces: `NetworkHelper.client` now solves Cloudflare challenges via WebView (no API change).

- [ ] **Step 1: Fetch the files**

Run: `WITH_WEBVIEW=1 mihonsupport/scripts/vendor-mihon.sh`
Expected: the four files above exist (the script also re-fetches Task 2 files; `git diff` must then show only the four new files plus any Task 2 `OTAKU:` edits that need re-applying — re-apply them).

- [ ] **Step 2: Replace Mihon i18n/toast calls** (moko-resources is not vendored). Edit by hand:

In `CloudflareInterceptor.kt`:
- delete imports `eu.kanade.tachiyomi.util.system.toast`, `tachiyomi.core.common.i18n.stringResource`, `tachiyomi.i18n.MR`
- replace `throw IOException(context.stringResource(MR.strings.information_cloudflare_bypass_failure), e)` with
  `throw IOException("Failed to bypass Cloudflare", e) // OTAKU: literal instead of MR string`
- replace `context.toast(MR.strings.information_webview_outdated, Toast.LENGTH_LONG)` with
  `Toast.makeText(context, "Please update the WebView app for better compatibility", Toast.LENGTH_LONG).show() // OTAKU`

In `WebViewInterceptor.kt`:
- delete imports `eu.kanade.tachiyomi.util.system.toast`, `tachiyomi.i18n.MR`
- replace `context.toast(MR.strings.information_webview_required, Toast.LENGTH_LONG)` with
  `Toast.makeText(context, "WebView is required for this source", Toast.LENGTH_LONG).show() // OTAKU`

- [ ] **Step 3: Wire the interceptor into `NetworkHelper`** — add a scope and replace the `client` property:

```kotlin
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val client: OkHttpClient by lazy {
        clientBuilder()
            .addInterceptor(CloudflareInterceptor(context, cookieJar, scope, ::defaultUserAgentProvider))
            .build()
    }
```

If the vendored `CloudflareInterceptor` constructor differs, match its parameter list exactly (it is the source of truth).

- [ ] **Step 4: Compile and re-run Task 2 tests**

Run: `./gradlew :mihonsupport:compileAndroidMain :mihonsupport:testAndroidHostTest`
Expected: BUILD SUCCESSFUL, `VendoredRuntimeTest` PASS. Remaining unresolved references must be fixed with the same rule as Task 2 Step 5 (remove app-only references, mark `OTAKU:`).

- [ ] **Step 5: Update `VENDORED.md`** with the four files and the three `OTAKU:` replacements.

- [ ] **Step 6: Commit**

```bash
git add mihonsupport
git commit -m "feat(mihon): vendor Cloudflare and WebView interceptors"
```

---

### Task 4: Mapper and `MihonApiService`

**Files:**
- Modify: `kmpmodels/src/commonMain/kotlin/com/programmersbox/kmpmodels/ApiService.kt` (append interface)
- Create: `mihonsupport/src/androidMain/kotlin/com/programmersbox/mihon/MihonApiService.kt`, `.../MihonSourceMapper.kt`
- Test: `mihonsupport/src/androidHostTest/kotlin/com/programmersbox/mihon/FakeSources.kt`, `.../MihonSourceMapperTest.kt`

**Interfaces:**
- Consumes: Task 2 vendored API.
- Produces:
  - `interface KmpConfigurableApiService : KmpApiService` (kmpmodels, no members)
  - `open class MihonApiService(val source: CatalogueSource) : KmpApiService` with `val displayName: String`
  - `class ConfigurableMihonApiService(source: CatalogueSource) : MihonApiService(source), KmpConfigurableApiService`
  - `fun mihonApiService(source: CatalogueSource): MihonApiService`
  - `object MihonSourceMapper` with `const val URL = "mihonUrl"`, `const val MEMO = "mihonMemo"`, `const val HAS_NEXT = "hasNextPage"`, and functions used below.

- [ ] **Step 1: Add the marker** to the end of `kmpmodels/src/commonMain/kotlin/com/programmersbox/kmpmodels/ApiService.kt`:

```kotlin
/** A source that exposes a settings screen (e.g. Mihon ConfigurableSource). */
interface KmpConfigurableApiService : KmpApiService
```

- [ ] **Step 2: Write test fakes** `mihonsupport/src/androidHostTest/kotlin/com/programmersbox/mihon/FakeSources.kt`:

```kotlin
package com.programmersbox.mihon

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response

fun manga(title: String, url: String) = SManga.create().apply {
    this.title = title
    this.url = url
    thumbnail_url = "https://img.example.org$url.jpg"
}

class FakeCatalogueSource(
    override val supportsLatest: Boolean = true,
) : CatalogueSource {
    override val id = 42L
    override val name = "Fake"
    override val lang = "en"

    var lastDetailsUrl: String? = null
    var lastMemo: String? = null

    override suspend fun getPopularManga(page: Int) = MangasPage(listOf(manga("Popular", "/p/1")), hasNextPage = true)
    override suspend fun getLatestUpdates(page: Int) = MangasPage(listOf(manga("Latest", "/l/1")), hasNextPage = false)
    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList) =
        MangasPage(listOf(manga("Search $query", "/s/1")), hasNextPage = false)

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        lastDetailsUrl = manga.url
        lastMemo = manga.memo.toString()
        val details = manga.copy().apply {
            description = "Desc"
            genre = "Action, Comedy"
            author = "Author"
            status = SManga.ONGOING
        }
        val chs = listOf(
            SChapter.create().apply { name = "Ch 2"; url = "/c/2"; date_upload = 1_700_000_000_000; chapter_number = 2f },
            SChapter.create().apply { name = "Ch 1"; url = "/c/1"; date_upload = 0; chapter_number = 1f },
        )
        return SMangaUpdate(details, chs)
    }

    override suspend fun getPageList(chapter: SChapter) = listOf(
        Page(0, url = "/page/0", imageUrl = "https://img.example.org/0.jpg"),
        Page(1, url = "/page/1", imageUrl = null),
    )
}

/** HttpSource that never touches Injekt/network: overrides headersBuilder and getImageUrl. */
class FakeHttpSource : HttpSource() {
    override val name = "FakeHttp"
    override val lang = "en"
    override val supportsLatest = false
    override val baseUrl = "https://example.org"
    var imageUrlCalls = 0

    override fun headersBuilder(): Headers.Builder = Headers.Builder().add("Referer", "https://example.org/")

    override suspend fun getPopularManga(page: Int) = MangasPage(listOf(manga("A", "/a")), hasNextPage = false)
    override suspend fun getPageList(chapter: SChapter) = listOf(
        Page(0, url = "/page/0", imageUrl = "https://img.example.org/0.jpg"),
        Page(1, url = "/page/1", imageUrl = null),
    )
    override suspend fun getImageUrl(page: Page): String {
        imageUrlCalls++
        return "https://img.example.org/resolved-${page.index}.jpg"
    }

    override fun popularMangaRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun popularMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException()
    override fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()
    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException()
    override fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException()
    override fun chapterListParse(response: Response): List<SChapter> = throw UnsupportedOperationException()
    override fun pageListParse(response: Response): List<Page> = throw UnsupportedOperationException()
    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()
}

fun memoOf(key: String, value: String) = buildJsonObject { put(key, JsonPrimitive(value)) }
```

(Same rule as Task 2 Step 6 for overrides the compiler rejects or requires.)

- [ ] **Step 3: Write the failing tests** `mihonsupport/src/androidHostTest/kotlin/com/programmersbox/mihon/MihonSourceMapperTest.kt`:

```kotlin
package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpConfigurableApiService
import com.programmersbox.kmpmodels.KmpItemModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MihonSourceMapperTest {

    @Test
    fun serviceIdentity() {
        val service = mihonApiService(FakeCatalogueSource())
        assertEquals("mihon:42", service.serviceName)
        assertEquals("Fake (en)", service.displayName)
        assertFalse(service is KmpConfigurableApiService)
    }

    @Test
    fun recentUsesLatestWhenSupported() = runTest {
        assertEquals("Latest", mihonApiService(FakeCatalogueSource(supportsLatest = true)).recent(1).single().title)
        assertEquals("Popular", mihonApiService(FakeCatalogueSource(supportsLatest = false)).recent(1).single().title)
    }

    @Test
    fun itemsCarryRelativeUrlAndPaging() = runTest {
        val service = mihonApiService(FakeHttpSource())
        val item = service.allList(1).single()
        assertEquals("https://example.org/a", item.url)
        assertEquals("/a", item.extras[MihonSourceMapper.URL])
        assertEquals(false, item.extras[MihonSourceMapper.HAS_NEXT])
    }

    @Test
    fun searchPassesQuery() = runTest {
        val service = mihonApiService(FakeCatalogueSource())
        assertEquals("Search naruto", service.search("naruto", 1, emptyList()).single().title)
    }

    @Test
    fun itemInfoRoundTripsUrlAndMemo() = runTest {
        val source = FakeCatalogueSource()
        val service = mihonApiService(source)
        val item = service.allList(1).single().apply {
            extras[MihonSourceMapper.MEMO] = memoOf("k", "v").toString()
        }

        val info = service.itemInfo(item)

        assertEquals("/p/1", source.lastDetailsUrl)
        assertEquals("""{"k":"v"}""", source.lastMemo)
        assertEquals(listOf("Action", "Comedy"), info.genres)
        assertEquals("Desc", info.description)
        assertEquals("Author", info.extras["author"])
        assertEquals("Ongoing", info.extras["status"])
        assertEquals(listOf("Ch 2", "Ch 1"), info.chapters.map { it.name })
        assertEquals("/c/2", info.chapters.first().extras[MihonSourceMapper.URL])
        assertEquals(info.url, info.chapters.first().sourceUrl)
    }

    // Review Focus 1: item reloaded from DB has no extras
    @Test
    fun itemInfoWithoutExtrasStripsBaseUrl() = runTest {
        val source = FakeCatalogueSource()
        val service = mihonApiService(source)
        val fromDb = KmpItemModel("Popular", "", "/p/1", "", service)
        service.itemInfo(fromDb)
        assertEquals("/p/1", source.lastDetailsUrl)
    }

    // Review Focus 5a: date_upload == 0
    @Test
    fun chapterUploadTime() = runTest {
        val service = mihonApiService(FakeCatalogueSource())
        val chapters = service.itemInfo(service.allList(1).single()).chapters
        assertEquals(1_700_000_000_000, chapters[0].uploadedTime)
        assertTrue(chapters[0].uploaded.isNotEmpty())
        assertNull(chapters[1].uploadedTime)
        assertEquals("", chapters[1].uploaded)
    }

    // Review Focus 5b: imageUrl already set -> no getImageUrl call
    @Test
    fun pagesResolveImageUrlOnlyWhenMissingAndCarryHeaders() = runTest {
        val source = FakeHttpSource()
        val service = mihonApiService(source)
        val info = KmpItemModel("A", "", "https://example.org/a", "", service)
        val chapter = com.programmersbox.kmpmodels.KmpChapterModel("c", "https://example.org/c/1", "", info.url, service)

        val storages = service.chapterInfo(chapter)

        assertEquals(
            listOf("https://img.example.org/0.jpg", "https://img.example.org/resolved-1.jpg"),
            storages.map { it.link },
        )
        assertEquals(1, source.imageUrlCalls)
        assertEquals("https://example.org/", storages.first().headers["Referer"])
    }

    @Test
    fun configurableSourceGetsMarker() {
        val configurable = object : eu.kanade.tachiyomi.source.CatalogueSource by FakeCatalogueSource(),
            eu.kanade.tachiyomi.source.ConfigurableSource {
            override fun setupPreferenceScreen(screen: androidx.preference.PreferenceScreen) = Unit
        }
        assertIs<KmpConfigurableApiService>(mihonApiService(configurable))
    }
}
```

- [ ] **Step 4: Run to verify failure**

Run: `./gradlew :mihonsupport:testAndroidHostTest --tests "com.programmersbox.mihon.MihonSourceMapperTest"`
Expected: compilation FAIL, `Unresolved reference 'mihonApiService'`.

- [ ] **Step 5: Implement `MihonSourceMapper.kt`**

```kotlin
package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Headers
import okhttp3.Request
import java.text.DateFormat
import java.util.Date

object MihonSourceMapper {
    const val URL = "mihonUrl"
    const val MEMO = "mihonMemo"
    const val HAS_NEXT = "hasNextPage"

    private val json = Json { ignoreUnknownKeys = true }

    fun absolute(baseUrl: String, path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) path else baseUrl + path

    fun relative(baseUrl: String, url: String): String =
        if (baseUrl.isNotEmpty() && url.startsWith(baseUrl)) url.removePrefix(baseUrl) else url

    fun toItems(page: MangasPage, service: MihonApiService): List<KmpItemModel> =
        page.mangas.map { toItem(it, service).apply { extras[HAS_NEXT] = page.hasNextPage } }

    fun toItem(manga: SManga, service: MihonApiService): KmpItemModel = KmpItemModel(
        title = manga.title,
        description = manga.description.orEmpty(),
        url = absolute(service.baseUrl, manga.url),
        imageUrl = manga.thumbnail_url.orEmpty(),
        source = service,
    ).apply {
        extras[URL] = manga.url
        if (manga.memo.isNotEmpty()) extras[MEMO] = manga.memo.toString()
    }

    fun toSManga(model: KmpItemModel, service: MihonApiService): SManga = SManga.create().apply {
        url = model.extras[URL] as? String ?: relative(service.baseUrl, model.url)
        title = model.title
        thumbnail_url = model.imageUrl.ifEmpty { null }
        (model.extras[MEMO] as? String)?.let { memo = json.decodeFromString<JsonObject>(it) }
    }

    fun toInfo(update: SMangaUpdate, requested: SManga, model: KmpItemModel, service: MihonApiService): KmpInfoModel {
        val manga = update.manga.also { if (it.url.isBlank()) it.url = requested.url }
        val infoUrl = absolute(service.baseUrl, manga.url)
        return KmpInfoModel(
            title = manga.title.ifBlank { model.title },
            description = manga.description.orEmpty(),
            url = infoUrl,
            imageUrl = manga.thumbnail_url ?: model.imageUrl,
            chapters = update.chapters.map { toChapter(it, infoUrl, service) },
            genres = manga.getGenres().orEmpty(),
            alternativeNames = emptyList(),
            source = service,
        ).apply {
            extras[URL] = manga.url
            manga.author?.let { extras["author"] = it }
            manga.artist?.let { extras["artist"] = it }
            extras["status"] = statusName(manga.status)
        }
    }

    fun toChapter(chapter: SChapter, infoUrl: String, service: MihonApiService): KmpChapterModel {
        val time = chapter.date_upload.takeIf { it > 0 }
        return KmpChapterModel(
            name = chapter.name,
            url = absolute(service.baseUrl, chapter.url),
            uploaded = time?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) }.orEmpty(),
            sourceUrl = infoUrl,
            source = service,
        ).apply {
            uploadedTime = time
            extras[URL] = chapter.url
            extras["chapterNumber"] = chapter.chapter_number
            if (chapter.memo.isNotEmpty()) extras[MEMO] = chapter.memo.toString()
        }
    }

    fun toSChapter(model: KmpChapterModel, service: MihonApiService): SChapter = SChapter.create().apply {
        url = model.extras[URL] as? String ?: relative(service.baseUrl, model.url)
        name = model.name
        (model.extras[MEMO] as? String)?.let { memo = json.decodeFromString<JsonObject>(it) }
    }

    fun toStorage(page: Page, imageUrl: String, source: Any): KmpStorage =
        KmpStorage(link = imageUrl).apply {
            if (source is HttpSource) headers.putAll(imageHeaders(source, page).toMap())
        }

    private val imageRequestMethod by lazy {
        HttpSource::class.java.getDeclaredMethod("imageRequest", Page::class.java).apply { isAccessible = true }
    }

    /** `imageRequest` is protected; extensions override it to add per-image headers. */
    private fun imageHeaders(source: HttpSource, page: Page): Headers =
        runCatching { (imageRequestMethod.invoke(source, page) as Request).headers }.getOrElse { source.headers }

    private fun Headers.toMap(): Map<String, String> = names().associateWith { get(it).orEmpty() }

    private fun statusName(status: Int) = when (status) {
        SManga.ONGOING -> "Ongoing"
        SManga.COMPLETED -> "Completed"
        SManga.LICENSED -> "Licensed"
        SManga.PUBLISHING_FINISHED -> "Publishing finished"
        SManga.CANCELLED -> "Cancelled"
        SManga.ON_HIATUS -> "On hiatus"
        else -> "Unknown"
    }
}
```

- [ ] **Step 6: Implement `MihonApiService.kt`**

```kotlin
package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.ResolvableSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun mihonApiService(source: CatalogueSource): MihonApiService =
    if (source is ConfigurableSource) ConfigurableMihonApiService(source) else MihonApiService(source)

open class MihonApiService(val source: CatalogueSource) : KmpApiService {
    override val baseUrl: String = (source as? HttpSource)?.baseUrl.orEmpty()
    override val serviceName: String = "mihon:${source.id}"
    override val canScroll: Boolean = true
    val displayName: String = "${source.name} (${source.lang})"

    override suspend fun recent(page: Int): List<KmpItemModel> = withContext(Dispatchers.IO) {
        val result = if (source.supportsLatest) source.getLatestUpdates(page) else source.getPopularManga(page)
        MihonSourceMapper.toItems(result, this@MihonApiService)
    }

    override suspend fun allList(page: Int): List<KmpItemModel> = withContext(Dispatchers.IO) {
        MihonSourceMapper.toItems(source.getPopularManga(page), this@MihonApiService)
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<KmpItemModel>): List<KmpItemModel> =
        withContext(Dispatchers.IO) {
            MihonSourceMapper.toItems(
                source.getSearchManga(page, searchText.toString(), source.getFilterList()),
                this@MihonApiService,
            )
        }

    override suspend fun itemInfo(model: KmpItemModel): KmpInfoModel = withContext(Dispatchers.IO) {
        val manga = MihonSourceMapper.toSManga(model, this@MihonApiService)
        val update = source.getMangaUpdate(manga, emptyList(), fetchDetails = true, fetchChapters = true)
        MihonSourceMapper.toInfo(update, manga, model, this@MihonApiService)
    }

    override suspend fun chapterInfo(chapterModel: KmpChapterModel): List<KmpStorage> = withContext(Dispatchers.IO) {
        val pages = source.getPageList(MihonSourceMapper.toSChapter(chapterModel, this@MihonApiService))
        pages.map { page ->
            val imageUrl = page.imageUrl
                ?: (source as? HttpSource)?.getImageUrl(page)?.also { page.imageUrl = it }
                ?: page.url
            MihonSourceMapper.toStorage(page, imageUrl, source)
        }
    }

    override suspend fun sourceByUrl(url: String): KmpItemModel = withContext(Dispatchers.IO) {
        val resolvable = source as? ResolvableSource ?: error("${source.name} cannot open URLs")
        val manga = resolvable.getManga(url) ?: error("${source.name} could not resolve $url")
        MihonSourceMapper.toItem(manga, this@MihonApiService)
    }
}

class ConfigurableMihonApiService(source: CatalogueSource) : MihonApiService(source), KmpConfigurableApiService
```

If the vendored `ResolvableSource.getManga` signature differs (e.g. takes `Uri` or is named differently), adapt this one call to it and keep `sourceByUrl` semantics.

- [ ] **Step 7: Run tests**

Run: `./gradlew :mihonsupport:testAndroidHostTest --tests "com.programmersbox.mihon.MihonSourceMapperTest"`
Expected: PASS (9 tests).

- [ ] **Step 8: Run kmpmodels tests** (interface added)

Run: `./gradlew :kmpmodels:jvmTest`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add kmpmodels mihonsupport
git commit -m "feat(mihon): map Mihon sources to KmpApiService"
```

---

### Task 5: Repo client (commonMain)

**Files:**
- Create: `mihonsupport/src/commonMain/kotlin/com/programmersbox/mihon/repo/MihonRepoModels.kt`, `.../repo/MihonRepoClient.kt`
- Test: `mihonsupport/src/commonTest/kotlin/com/programmersbox/mihon/repo/MihonRepoClientTest.kt`

**Interfaces:**
- Consumes: Task 1 `MihonLib`; `KmpRemoteSources`, `KmpSources` from kmpmodels.
- Produces: `class MihonRepoClient(httpClient: HttpClient)` with `suspend fun fetch(urls: List<String>): List<KmpRemoteSources>`, `val signingKeys: Set<String>` (from last successful fetch, accumulates per repo base), `companion object { fun normalize(url: String): String }`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programmersbox.mihon.repo

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val REPO = """{"meta":{"name":"Keiyoushi","shortName":"Keiyoushi","website":"https://keiyoushi.github.io","signingKeyFingerprint":"abc123"}}"""

private const val INDEX = """[
 {"name":"Tachiyomi: MangaDex","pkg":"eu.kanade.tachiyomi.extension.all.mangadex","apk":"tachiyomi-all.mangadex-v1.4.190.apk","lang":"all","code":190,"version":"1.4.190","nsfw":1,
  "sources":[{"name":"MangaDex","lang":"en","id":"2499283573021220255","baseUrl":"https://mangadex.org"}]},
 {"name":"Tachiyomi: Old","pkg":"eu.kanade.tachiyomi.extension.en.old","apk":"old-v1.3.5.apk","lang":"en","code":5,"version":"1.3.5","nsfw":0,
  "sources":[{"name":"Old","lang":"en","id":1,"baseUrl":"https://old.example"}]},
 {"name":"Tachiyomi: Mid","pkg":"eu.kanade.tachiyomi.extension.en.mid","apk":"mid-v1.5.1.apk","lang":"en","code":1,"version":"1.5.1","nsfw":0,
  "sources":[{"name":"Mid","lang":"en","id":2,"baseUrl":"https://mid.example"}]}
]"""

class MihonRepoClientTest {
    private fun client(failHost: String? = null) = HttpClient(MockEngine { request ->
        val url = request.url.toString()
        when {
            failHost != null && url.contains(failHost) -> respondError(HttpStatusCode.NotFound)
            url.endsWith("/repo.json") -> respond(REPO)
            url.endsWith("/index.min.json") -> respond(INDEX)
            else -> respondError(HttpStatusCode.NotFound)
        }
    })

    // Review Focus 2
    @Test
    fun normalizesUserUrls() {
        val base = "https://raw.githubusercontent.com/keiyoushi/extensions/repo"
        listOf(
            base, "$base/", "  $base  ", "$base/index.min.json", "$base/repo.json",
        ).forEach { assertEquals(base, MihonRepoClient.normalize(it), it) }
    }

    @Test
    fun mapsSupportedEntries() = runTest {
        val repo = MihonRepoClient(client())
        val result = repo.fetch(listOf("https://repo.example/index.min.json"))

        val dex = result.single()
        assertEquals("MangaDex (18+)", dex.name)
        assertEquals("eu.kanade.tachiyomi.extension.all.mangadex", dex.packageName)
        assertEquals("1.4.190", dex.version)
        assertEquals("https://repo.example/icon/eu.kanade.tachiyomi.extension.all.mangadex.png", dex.iconUrl)
        assertEquals("https://repo.example/apk/tachiyomi-all.mangadex-v1.4.190.apk", dex.downloadLink)
        assertEquals("https://mangadex.org", dex.sources.single().baseUrl)
        assertEquals(setOf("abc123"), repo.signingKeys)
    }

    @Test
    fun failingRepoDoesNotDropOthers() = runTest {
        val repo = MihonRepoClient(client(failHost = "bad.example"))
        val result = repo.fetch(listOf("https://bad.example", "https://good.example"))
        assertEquals(listOf("eu.kanade.tachiyomi.extension.all.mangadex"), result.map { it.packageName })
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :mihonsupport:jvmTest --tests "com.programmersbox.mihon.repo.MihonRepoClientTest"`
Expected: compilation FAIL, `Unresolved reference 'MihonRepoClient'`.

- [ ] **Step 3: Implement `MihonRepoModels.kt`**

```kotlin
package com.programmersbox.mihon.repo

import kotlinx.serialization.Serializable

@Serializable
data class MihonRepo(val meta: Meta) {
    @Serializable
    data class Meta(
        val name: String,
        val shortName: String? = null,
        val website: String = "",
        val signingKeyFingerprint: String,
    )
}

@Serializable
data class MihonIndexEntry(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String = "",
    val code: Long = 0,
    val version: String,
    val nsfw: Int = 0,
    val sources: List<Source> = emptyList(),
) {
    @Serializable
    data class Source(val name: String, val lang: String = "", val baseUrl: String = "")
}
```

- [ ] **Step 4: Implement `MihonRepoClient.kt`**

```kotlin
package com.programmersbox.mihon.repo

import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSources
import com.programmersbox.mihon.MihonLib
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

class MihonRepoClient(private val httpClient: HttpClient) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val keysByRepo = mutableMapOf<String, String>()

    /** Signing key fingerprints of every repo fetched successfully at least once this session. */
    val signingKeys: Set<String> get() = keysByRepo.values.toSet()

    suspend fun fetch(urls: List<String>): List<KmpRemoteSources> = coroutineScope {
        urls.map(::normalize)
            .filter { it.isNotEmpty() }
            .distinct()
            .map { base -> async { runCatching { fetchRepo(base) }.onFailure { it.printStackTrace() }.getOrDefault(emptyList()) } }
            .awaitAll()
            .flatten()
    }

    private suspend fun fetchRepo(base: String): List<KmpRemoteSources> {
        val repo = json.decodeFromString<MihonRepo>(getText("$base/repo.json"))
        val index = json.decodeFromString<List<MihonIndexEntry>>(getText("$base/index.min.json"))
        keysByRepo[base] = repo.meta.signingKeyFingerprint
        return index
            .filter { MihonLib.isSupported(MihonLib.libVersion(it.version)) }
            .map { it.toRemote(base) }
    }

    private suspend fun getText(url: String): String {
        val response = httpClient.get(url)
        check(response.status.isSuccess()) { "GET $url -> ${response.status}" }
        return response.bodyAsText()
    }

    private fun MihonIndexEntry.toRemote(base: String) = KmpRemoteSources(
        name = name.removePrefix("Tachiyomi: ") + if (nsfw == 1) " (18+)" else "",
        packageName = pkg,
        version = version,
        iconUrl = "$base/icon/$pkg.png",
        downloadLink = "$base/apk/$apk",
        sources = sources.map { KmpSources(name = it.name, baseUrl = it.baseUrl, version = version) },
    )

    companion object {
        fun normalize(url: String): String = url.trim()
            .removeSuffix("/index.min.json")
            .removeSuffix("/repo.json")
            .trimEnd('/')
    }
}
```

- [ ] **Step 5: Run tests**

Run: `./gradlew :mihonsupport:jvmTest --tests "com.programmersbox.mihon.repo.MihonRepoClientTest"`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add mihonsupport
git commit -m "feat(mihon): fetch Mihon extension repos"
```

---

### Task 6: Trust rules and persisted trusted extensions

**Files:**
- Create: `mihonsupport/src/commonMain/kotlin/com/programmersbox/mihon/MihonTrust.kt`
- Test: `mihonsupport/src/commonTest/kotlin/com/programmersbox/mihon/MihonTrustTest.kt`
- Modify: `datastore/src/commonMain/proto/settings.proto` (Settings message, after `screensaverType = 36;`), `datastore/src/commonMain/kotlin/com/programmersbox/datastore/SettingsHandling.kt` (after `removeCustomUrl`)

**Interfaces:**
- Produces: `object MihonTrust { fun trustId(pkg: String, versionCode: Long, fingerprint: String): String; fun isTrusted(pkg: String, versionCode: Long, fingerprints: Set<String>, repoSigningKeys: Set<String>, trustedIds: Set<String>): Boolean }`; `NewSettingsHandling.trustedMihonExtensions: Flow<List<String>>`, `suspend fun addTrustedMihonExtension(id: String)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programmersbox.mihon

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MihonTrustTest {
    private val pkg = "eu.kanade.tachiyomi.extension.en.test"

    @Test
    fun repoKeyMatchTrusts() =
        assertTrue(MihonTrust.isTrusted(pkg, 5, setOf("aa", "bb"), repoSigningKeys = setOf("bb"), trustedIds = emptySet()))

    @Test
    fun userTrustedIdTrusts() =
        assertTrue(MihonTrust.isTrusted(pkg, 5, setOf("aa"), emptySet(), setOf(MihonTrust.trustId(pkg, 5, "aa"))))

    @Test
    fun newVersionNeedsRetrust() =
        assertFalse(MihonTrust.isTrusted(pkg, 6, setOf("aa"), emptySet(), setOf(MihonTrust.trustId(pkg, 5, "aa"))))

    @Test
    fun unsignedNeverTrusted() =
        assertFalse(MihonTrust.isTrusted(pkg, 5, emptySet(), setOf(""), setOf(MihonTrust.trustId(pkg, 5, ""))))

    @Test
    fun unknownKeyUntrusted() =
        assertFalse(MihonTrust.isTrusted(pkg, 5, setOf("aa"), setOf("zz"), emptySet()))
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :mihonsupport:jvmTest --tests "com.programmersbox.mihon.MihonTrustTest"`
Expected: compilation FAIL, `Unresolved reference 'MihonTrust'`.

- [ ] **Step 3: Implement `MihonTrust.kt`**

```kotlin
package com.programmersbox.mihon

/** Mirrors Mihon's TrustExtension: repo-signed, or explicitly trusted by the user for this exact version. */
object MihonTrust {
    fun trustId(pkg: String, versionCode: Long, fingerprint: String): String = "$pkg:$versionCode:$fingerprint"

    fun isTrusted(
        pkg: String,
        versionCode: Long,
        fingerprints: Set<String>,
        repoSigningKeys: Set<String>,
        trustedIds: Set<String>,
    ): Boolean {
        if (fingerprints.isEmpty()) return false
        return fingerprints.any { it in repoSigningKeys } ||
            fingerprints.any { trustId(pkg, versionCode, it) in trustedIds }
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :mihonsupport:jvmTest --tests "com.programmersbox.mihon.MihonTrustTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Add the proto field** in `datastore/src/commonMain/proto/settings.proto`, inside `message Settings` after `ScreensaverType screensaverType = 36;`:

```proto
  repeated string trustedMihonExtensions = 37;
```

- [ ] **Step 6: Add helpers** in `SettingsHandling.kt` right after `removeCustomUrl`:

```kotlin
    val trustedMihonExtensions = all.map { it.trustedMihonExtensions }

    suspend fun addTrustedMihonExtension(id: String) = preferences.updateData {
        if (id in it.trustedMihonExtensions) it else it.copy(trustedMihonExtensions = it.trustedMihonExtensions + id)
    }
```

- [ ] **Step 7: Build datastore**

Run: `./gradlew :datastore:jvmTest :datastore:compileAndroidMain`
Expected: BUILD SUCCESSFUL (proto regenerated with the new field).

- [ ] **Step 8: Commit**

```bash
git add mihonsupport datastore
git commit -m "feat(mihon): extension trust rules and persisted trusted ids"
```

---

### Task 7: Extension loader and `MihonCatalog`

**Files:**
- Modify: `kmpmodels/src/commonMain/kotlin/com/programmersbox/kmpmodels/ApiService.kt` (append interfaces)
- Create: `mihonsupport/src/androidMain/kotlin/com/programmersbox/mihon/UntrustedMihonApiService.kt`, `.../loader/MihonExtensionLoader.kt`, `.../MihonCatalog.kt`, `.../MihonModule.kt`
- Test: `mihonsupport/src/androidHostTest/kotlin/com/programmersbox/mihon/loader/SourceInstantiationTest.kt`

**Interfaces:**
- Consumes: Tasks 2, 4, 5, 6.
- Produces:
  - kmpmodels: `interface KmpUntrustedApiService : KmpApiService { val reason: String; val canTrust: Boolean; suspend fun trust() }`; `class NativeCatalogs(val catalogs: List<KmpExternalCustomApiServicesCatalog>)`
  - `class UntrustedMihonApiService(val packageName: String, override val reason: String, override val canTrust: Boolean, private val onTrust: suspend () -> Unit) : KmpUntrustedApiService`
  - `object MihonSourceInstantiator { fun classNames(pkg: String, metadataValue: String?): List<String>; fun instantiate(classNames: List<String>, classLoader: ClassLoader): Result<List<CatalogueSource>> }`
  - `class MihonExtensionLoader(context: Context)` with `fun load(catalog: MihonCatalog, repoSigningKeys: Set<String>, trustedIds: Set<String>, onTrust: suspend (String) -> Unit): List<KmpSourceInformation>`, `fun isExtension(packageName: String): Boolean`
  - `class MihonCatalog(application: Application, settings: NewSettingsHandling, repoClient: MihonRepoClient) : KmpExternalCustomApiServicesCatalog` with `name = "Mihon"`
  - `val mihonModule: Module` (Koin)

- [ ] **Step 1: Add kmpmodels interfaces** at the end of `ApiService.kt`:

```kotlin
/** Placeholder for an installed extension that was not loaded (untrusted, unsupported, or failed). */
interface KmpUntrustedApiService : KmpApiService {
    val reason: String
    val canTrust: Boolean
    suspend fun trust()
}

/** Catalogs that an app registers natively (in-process) rather than through an extension APK. */
class NativeCatalogs(val catalogs: List<KmpExternalCustomApiServicesCatalog>)
```

- [ ] **Step 2: Write the failing test** `SourceInstantiationTest.kt` (Review Focus 3):

```kotlin
package com.programmersbox.mihon.loader

import com.programmersbox.mihon.FakeCatalogueSource
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TwoSourceFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(FakeCatalogueSource(), FakeCatalogueSource(false))
}

class ThrowingSource : CatalogueSource by FakeCatalogueSource() {
    init { throw NoClassDefFoundError("eu/kanade/tachiyomi/Missing") }
}

class SourceInstantiationTest {
    private val loader = javaClass.classLoader!!

    @Test
    fun resolvesRelativeClassNames() {
        assertEquals(
            listOf("com.ext.A", "com.other.B"),
            MihonSourceInstantiator.classNames("com.ext", " .A ; com.other.B"),
        )
        assertEquals(emptyList(), MihonSourceInstantiator.classNames("com.ext", null))
    }

    @Test
    fun sourceAndFactoryInstantiate() {
        val result = MihonSourceInstantiator.instantiate(
            listOf(FakeCatalogueSource::class.java.name, TwoSourceFactory::class.java.name),
            loader,
        )
        assertEquals(3, result.getOrThrow().size)
    }

    @Test
    fun missingClassFailsWithoutThrowing() {
        val result = MihonSourceInstantiator.instantiate(listOf("does.not.Exist"), loader)
        assertTrue(result.isFailure)
    }

    @Test
    fun linkageErrorFailsWithoutThrowing() {
        val result = MihonSourceInstantiator.instantiate(listOf(ThrowingSource::class.java.name), loader)
        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 3: Run to verify failure**

Run: `./gradlew :mihonsupport:testAndroidHostTest --tests "com.programmersbox.mihon.loader.SourceInstantiationTest"`
Expected: compilation FAIL, `Unresolved reference 'MihonSourceInstantiator'`.

- [ ] **Step 4: Implement `UntrustedMihonApiService.kt`**

```kotlin
package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpUntrustedApiService

class UntrustedMihonApiService(
    val packageName: String,
    override val reason: String,
    override val canTrust: Boolean,
    private val onTrust: suspend () -> Unit,
) : KmpUntrustedApiService {
    override val baseUrl: String = ""
    override val serviceName: String = "mihon-untrusted:$packageName"
    override val notWorking: Boolean = true
    override suspend fun trust() = onTrust()
}
```

- [ ] **Step 5: Implement `loader/MihonExtensionLoader.kt`**

```kotlin
package com.programmersbox.mihon.loader

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.mihon.MihonCatalog
import com.programmersbox.mihon.MihonLib
import com.programmersbox.mihon.MihonTrust
import com.programmersbox.mihon.UntrustedMihonApiService
import com.programmersbox.mihon.mihonApiService
import dalvik.system.DelegateLastClassLoader
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import java.security.MessageDigest

private const val EXTENSION_FEATURE = "tachiyomi.extension"
private const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"
private const val METADATA_EXTENSION_LIB = "tachiyomix.extensionLib"
private const val METADATA_NAME = "tachiyomix.name"

private const val PACKAGE_FLAGS =
    PackageManager.GET_CONFIGURATIONS or PackageManager.GET_META_DATA or PackageManager.GET_SIGNING_CERTIFICATES

object MihonSourceInstantiator {
    fun classNames(pkg: String, metadataValue: String?): List<String> = metadataValue.orEmpty()
        .split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { if (it.startsWith(".")) pkg + it else it }

    /** Instantiates every class; any failure (including LinkageError) fails the whole package, like Mihon. */
    fun instantiate(classNames: List<String>, classLoader: ClassLoader): Result<List<CatalogueSource>> = try {
        Result.success(
            classNames.flatMap { name ->
                when (val obj = Class.forName(name, false, classLoader).getDeclaredConstructor().newInstance()) {
                    is SourceFactory -> obj.createSources()
                    is Source -> listOf(obj)
                    else -> error("Unknown source class type: ${obj.javaClass}")
                }
            }.filterIsInstance<CatalogueSource>()
        )
    } catch (t: Throwable) {
        Result.failure(t)
    }
}

class MihonExtensionLoader(private val context: Context) {

    @SuppressLint("QueryPermissionsNeeded")
    fun load(
        catalog: MihonCatalog,
        repoSigningKeys: Set<String>,
        trustedIds: Set<String>,
        onTrust: suspend (String) -> Unit,
    ): List<KmpSourceInformation> = installedPackages()
        .filter { it.isMihonExtension() }
        .flatMap { loadPackage(it, catalog, repoSigningKeys, trustedIds, onTrust) }

    fun isExtension(packageName: String): Boolean = runCatching {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_CONFIGURATIONS.toLong()))
        } else {
            pm.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS)
        }
        info.isMihonExtension()
    }.getOrDefault(false)

    // minSdk is 28; PackageInfoFlags is API 33+.
    private fun installedPackages(): List<PackageInfo> {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pm.getInstalledPackages(PACKAGE_FLAGS)
        }
    }

    private fun PackageInfo.isMihonExtension() = reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }

    private fun loadPackage(
        pkgInfo: PackageInfo,
        catalog: MihonCatalog,
        repoSigningKeys: Set<String>,
        trustedIds: Set<String>,
        onTrust: suspend (String) -> Unit,
    ): List<KmpSourceInformation> {
        val pkg = pkgInfo.packageName
        val appInfo = pkgInfo.applicationInfo ?: return emptyList()
        val metaData = appInfo.metaData
        val versionCode = pkgInfo.longVersionCode
        val fingerprints = signatures(pkgInfo)
        val label = metaData?.getString(METADATA_NAME)
            ?: context.packageManager.getApplicationLabel(appInfo).toString().removePrefix("Tachiyomi: ")

        fun notLoaded(reason: String, canTrust: Boolean = false) = listOf(
            KmpSourceInformation(
                apiService = UntrustedMihonApiService(pkg, reason, canTrust) {
                    onTrust(MihonTrust.trustId(pkg, versionCode, fingerprints.last()))
                },
                name = "$label ($reason)",
                icon = null,
                packageName = pkg,
                catalog = catalog,
            )
        )

        if (metaData == null) return notLoaded("Malformed")

        val lib = MihonLib.libVersion(metaData.getFloat(METADATA_EXTENSION_LIB), pkgInfo.versionName)
        if (!MihonLib.isSupported(lib)) return notLoaded("Unsupported lib $lib")

        if (fingerprints.isEmpty()) return notLoaded("Unsigned")
        if (!MihonTrust.isTrusted(pkg, versionCode, fingerprints.toSet(), repoSigningKeys, trustedIds)) {
            return notLoaded("Untrusted", canTrust = true)
        }

        val classNames = MihonSourceInstantiator.classNames(pkg, metaData.getString(METADATA_SOURCE_CLASS))
        if (classNames.isEmpty()) return notLoaded("Malformed")

        val sources = runCatching { DelegateLastClassLoader(appInfo.sourceDir, null, context.classLoader) }
            .mapCatching { MihonSourceInstantiator.instantiate(classNames, it).getOrThrow() }
            .getOrElse {
                it.printStackTrace()
                return notLoaded("Failed to load: ${it.message}")
            }

        return sources.map { source ->
            val service = mihonApiService(source)
            KmpSourceInformation(
                apiService = service,
                name = service.displayName,
                icon = null,
                packageName = pkg,
                catalog = catalog,
            )
        }
    }

    private fun signatures(pkgInfo: PackageInfo): List<String> {
        val signingInfo = pkgInfo.signingInfo ?: return emptyList()
        val certs = if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
        return certs.orEmpty().map { sig ->
            MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
```

- [ ] **Step 6: Implement `MihonCatalog.kt`**

```kotlin
package com.programmersbox.mihon

import android.app.Application
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.mihon.loader.MihonExtensionLoader
import com.programmersbox.mihon.repo.MihonRepoClient
import kotlinx.coroutines.flow.first

class MihonCatalog(
    private val application: Application,
    private val settings: NewSettingsHandling,
    private val repoClient: MihonRepoClient,
) : KmpExternalCustomApiServicesCatalog {
    override val name: String = "Mihon"
    override val hasRemoteSources: Boolean = true

    private val loader = MihonExtensionLoader(application)

    @Volatile
    private var sources: List<KmpSourceInformation> = emptyList()

    override suspend fun initialize() {
        MihonRuntime.init(application)
        // Refresh repo signing keys; on failure keep whatever was fetched earlier this session.
        runCatching { repoClient.fetch(settings.customUrls.first()) }
        sources = loader.load(
            catalog = this,
            repoSigningKeys = repoClient.signingKeys,
            trustedIds = settings.trustedMihonExtensions.first().toSet(),
            onTrust = { settings.addTrustedMihonExtension(it) },
        )
    }

    override fun getSources(): List<KmpSourceInformation> = sources

    override fun shouldReload(packageName: String): Boolean = loader.isExtension(packageName)

    override suspend fun getRemoteSources(customUrls: List<String>): List<KmpRemoteSources> = repoClient.fetch(customUrls)
}
```

- [ ] **Step 7: Implement `MihonModule.kt`**

```kotlin
package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.mihon.repo.MihonRepoClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.bind
import org.koin.dsl.module

val mihonModule = module {
    single { MihonRepoClient(HttpClient(OkHttp)) }
    single { MihonCatalog(get(), get(), get()) } bind KmpExternalCustomApiServicesCatalog::class
}
```

- [ ] **Step 8: Run tests**

Run: `./gradlew :mihonsupport:testAndroidHostTest :mihonsupport:jvmTest :kmpmodels:jvmTest`
Expected: PASS (all mihonsupport tests so far + `SourceInstantiationTest` 4 tests).

- [ ] **Step 9: Commit**

```bash
git add kmpmodels mihonsupport
git commit -m "feat(mihon): load installed Mihon extensions with trust gate"
```

---

### Task 8: App integration (loader, Koin, extension list)

**Files:**
- Modify: `sharedutils/kmpextensionloader/src/androidMain/kotlin/com/programmersbox/kmpextensionloader/SourceLoader.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.kt` (inside the module, near `single { JsExtensionSourceBridge(get(), get()) }`)
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.android.kt:45-52`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionListViewModel.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/ExtensionListScreen.kt:312-426`
- Modify: `mangaworld/build.gradle.kts`, `mangaworld/src/main/java/com/programmersbox/mangaworld/GenericManga.kt` (the `appModule` definition)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions/InstalledViewStateTest.kt`

**Interfaces:**
- Consumes: `NativeCatalogs`, `KmpUntrustedApiService` (Task 7), `mihonModule` (Task 7).
- Produces: `SourceLoader(application, context, sourceType, sourceRepository, nativeCatalogs: NativeCatalogs = NativeCatalogs(emptyList()), modelMapper)`; `InstalledViewState.canUninstallAll: Boolean`; `ExtensionListViewModel.trust(source: KmpSourceInformation)`.

- [ ] **Step 1: Write the failing test** `InstalledViewStateTest.kt` (Review Focus 4):

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.extensions

import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpSourceInformation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstalledViewStateTest {
    private val service = object : KmpApiService { override val baseUrl = "" }
    private fun info(pkg: String) = KmpSourceInformation(service, "n", null, pkg)

    @Test
    fun singlePackageGroupCanUninstallAll() =
        assertTrue(InstalledViewState(listOf(info("a"), info("a"))).canUninstallAll)

    @Test
    fun multiPackageGroupCannotUninstallAll() =
        assertFalse(InstalledViewState(listOf(info("a"), info("b"))).canUninstallAll)
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :kmpuiviews:jvmTest --tests "*InstalledViewStateTest"`
Expected: compilation FAIL, `Unresolved reference 'canUninstallAll'`.

- [ ] **Step 3: Add `canUninstallAll`** to `InstalledViewState` in `ExtensionListViewModel.kt`:

```kotlin
class InstalledViewState(
    val sourceInformation: List<KmpSourceInformation>,
) {
    var showItems by mutableStateOf(false)

    /** A catalog group can only be uninstalled as a unit when it is one APK (e.g. a bridge). */
    val canUninstallAll: Boolean get() = sourceInformation.map { it.packageName }.distinct().size == 1
}
```

- [ ] **Step 4: Run test**

Run: `./gradlew :kmpuiviews:jvmTest --tests "*InstalledViewStateTest"`
Expected: PASS.

- [ ] **Step 5: Update `ExtensionListViewModel`** — add constructor param `private val nativeCatalogs: NativeCatalogs` (import `com.programmersbox.kmpmodels.NativeCatalogs`, `com.programmersbox.kmpmodels.KmpUntrustedApiService`), then:

Replace `hasCustomBridge`:

```kotlin
    val hasCustomBridge by derivedStateOf {
        nativeCatalogs.catalogs.isNotEmpty() ||
            installedSources.any { it.catalog is KmpExternalCustomApiServicesCatalog && it.name == "Custom Tachiyomi Bridge" }
    }
```

In `init`, in the `.combine(settingsHandling.customUrls) { sources, urls -> ... }` block, replace the catalog list expression:

```kotlin
                (sources.asSequence().map { it.catalog }.filterIsInstance<KmpExternalCustomApiServicesCatalog>() +
                    nativeCatalogs.catalogs.asSequence())
                    .filter { it.hasRemoteSources }
                    .distinct()
                    .toList()
                    .forEach { c ->
```

(keep the body unchanged). Add:

```kotlin
    fun trust(source: KmpSourceInformation) {
        viewModelScope.launch {
            (source.apiService as? KmpUntrustedApiService)?.trust()
            sourceLoader.blockingLoad()
        }
    }
```

- [ ] **Step 6: Register `NativeCatalogs`** in `kmpuiviews/.../di/AppModule.kt` (common module), next to `single { JsExtensionSourceBridge(get(), get()) }`:

```kotlin
    single { NativeCatalogs(getAll()) }
```

(import `com.programmersbox.kmpmodels.NativeCatalogs`). Koin's `getAll()` returns an empty list in apps that register no native catalog.

- [ ] **Step 7: Update `SourceLoader` (Android)** — add constructor parameter after `sourceRepository`:

```kotlin
    private val nativeCatalogs: NativeCatalogs = NativeCatalogs(emptyList()),
```

Add a helper and use it in both `load()` and `blockingLoad()`:

```kotlin
    private suspend fun nativeSources(): List<KmpSourceInformation> = nativeCatalogs.catalogs.flatMap { catalog ->
        runCatching { catalog.initialize(); catalog.getSources() }
            .onFailure { it.printStackTrace() }
            .getOrDefault(emptyList())
    }

    actual fun load() {
        sourceRepository.setSources(
            (extensionLoader.loadExtensions().flatten() + runBlocking { nativeSources() })
                .sortedBy { it.apiService.serviceName }
        )
    }

    actual suspend fun blockingLoad() {
        sourceRepository.setSources(
            (extensionLoader.loadExtensionsBlocking().flatten() + nativeSources())
                .sortedBy { it.apiService.serviceName }
        )
    }
```

In the broadcast receiver, extend the `isOtakuExtension` result:

```kotlin
                    p.reqFeatures?.any { it.name == extensionType } == true || s ||
                        nativeCatalogs.catalogs.any { it.shouldReload(packageString) }
```

Note: on `ACTION_PACKAGE_REMOVED` the package no longer exists, so `getPackageInfo` throws and `isOtakuExtension` is false. Add, before the `runCatching`:

```kotlin
                if (intent.action == Intent.ACTION_PACKAGE_REMOVED &&
                    sourceRepository.list.any { it.packageName == packageString }
                ) {
                    scope.launch { blockingLoad() }
                    return
                }
```

- [ ] **Step 8: Pass it in Koin** (`AppModule.android.kt`):

```kotlin
    single {
        SourceLoader(
            application = get(),
            context = get(),
            sourceType = get<KmpGenericInfo>().sourceType,
            sourceRepository = get(),
            nativeCatalogs = get(),
        )
    }
```

- [ ] **Step 9: Update `InstalledExtensionItems`** in `ExtensionListScreen.kt`:
  - Add parameter `onTrust: (KmpSourceInformation) -> Unit` and pass `onTrust = { viewModel.trust(it) }` from `ExtensionList`.
  - Group header `trailingContent`: change `t?.let {` to `t?.takeIf { u.canUninstallAll }?.let {`.
  - In each item's `onClick`, skip untrusted: `onClick = { if (source.apiService !is KmpUntrustedApiService) currentSourceRepository.tryEmit(source.apiService) }`.
  - In the trailing `Row`, before the delete `IconButton`, add:

```kotlin
                                        (source.apiService as? KmpUntrustedApiService)
                                            ?.takeIf { it.canTrust }
                                            ?.let {
                                                TextButton(onClick = { onTrust(source) }) { Text("Trust") }
                                            }
```

  (imports: `com.programmersbox.kmpmodels.KmpUntrustedApiService`, `androidx.compose.material3.TextButton` if not already imported.)

- [ ] **Step 10: Wire the app** — `mangaworld/build.gradle.kts` dependencies, next to `implementation(projects.kmpmodels)`:

```kotlin
    implementation(projects.mihonsupport)
    implementation(androidLibs.fragmentCompose)
```

In `GenericManga.kt`'s `appModule` (the module ending with `includes(mangaSharedModule())`), change to:

```kotlin
    includes(mangaSharedModule(), mihonModule)
```

(import `com.programmersbox.mihon.mihonModule`).

- [ ] **Step 11: Build and run the shared tests**

Run: `./gradlew :kmpuiviews:jvmTest :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug`
Expected: BUILD SUCCESSFUL, tests PASS. AnimeWorld builds unchanged (no native catalogs registered there).

- [ ] **Step 12: Commit**

```bash
git add sharedutils kmpuiviews mangaworld
git commit -m "feat(mihon): wire native Mihon catalog into MangaWorld extension loading"
```

---

### Task 9: Source preferences screen

**Files:**
- Create: `mihonsupport/src/androidMain/kotlin/com/programmersbox/mihon/preferences/MihonPreferenceFragment.kt`, `mangaworld/src/main/java/com/programmersbox/mangaworld/MihonPreferencesScreen.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt` (inside `sealed class Screen`), `ExtensionListScreen.kt` (trailing `Row` of installed items), `mangaworld/src/main/java/com/programmersbox/mangaworld/GenericManga.kt` (class body)

**Interfaces:**
- Consumes: `KmpConfigurableApiService` (Task 4), `MihonApiService` (Task 4), `SourceRepository.toSourceByApiServiceName`.
- Produces: `Screen.SourcePreferences(serviceName: String)`; `class MihonPreferenceFragment : PreferenceFragmentCompat` with `companion object { const val ARG_SERVICE_NAME = "serviceName" }`.

- [ ] **Step 1: Add the NavKey** inside `sealed class Screen` in `Screen.kt`:

```kotlin
    @Serializable
    data class SourcePreferences(val serviceName: String) : Screen("source_preferences")
```

- [ ] **Step 2: Add the gear** in `ExtensionListScreen.kt` `InstalledExtensionItems`: get `val navActions = LocalNavActions.current` at the top of the composable, then in the trailing `Row` before the Trust button:

```kotlin
                                        if (source.apiService is KmpConfigurableApiService) {
                                            IconButton(
                                                onClick = { navActions.navigate(Screen.SourcePreferences(source.apiService.serviceName)) }
                                            ) { Icon(Icons.Default.Settings, null) }
                                        }
```

(imports: `com.programmersbox.kmpmodels.KmpConfigurableApiService`, `com.programmersbox.kmpuiviews.presentation.Screen`, `androidx.compose.material.icons.filled.Settings`.)

- [ ] **Step 3: Implement `MihonPreferenceFragment.kt`**

```kotlin
package com.programmersbox.mihon.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.mihon.MihonApiService
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.preferenceKey
import org.koin.java.KoinJavaComponent.get

/** Renders a Mihon ConfigurableSource's preferences, stored where the extension reads them. */
class MihonPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val serviceName = requireArguments().getString(ARG_SERVICE_NAME).orEmpty()
        val source = (get<SourceRepository>(SourceRepository::class.java)
            .toSourceByApiServiceName(serviceName)
            ?.apiService as? MihonApiService)
            ?.source as? ConfigurableSource
            ?: return

        preferenceManager.sharedPreferencesName = source.preferenceKey()
        val screen = preferenceManager.createPreferenceScreen(requireContext())
        source.setupPreferenceScreen(screen)
        preferenceScreen = screen
    }

    companion object {
        const val ARG_SERVICE_NAME = "serviceName"
    }
}
```

If the vendored `ConfigurableSource.kt` defines `preferenceKey()` as a member rather than a top-level extension, drop the `preferenceKey` import and call it as a member. Add `implementation(commonLibs.koinCores)` is already present from Task 1; `KoinJavaComponent` is in koin-core.

- [ ] **Step 4: Implement `MihonPreferencesScreen.kt`** in mangaworld:

```kotlin
package com.programmersbox.mangaworld

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.fragment.compose.AndroidFragment
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.mihon.MihonApiService
import com.programmersbox.mihon.preferences.MihonPreferenceFragment
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MihonPreferencesScreen(serviceName: String) {
    val sourceRepository = koinInject<SourceRepository>()
    val title = (sourceRepository.toSourceByApiServiceName(serviceName)?.apiService as? MihonApiService)
        ?.displayName
        ?: serviceName

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { BackButton() },
            )
        }
    ) { padding ->
        AndroidFragment<MihonPreferenceFragment>(
            arguments = bundleOf(MihonPreferenceFragment.ARG_SERVICE_NAME to serviceName),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        )
    }
}
```

Find `BackButton`'s real package with `grep -rn "fun BackButton" kmpuiviews/src/commonMain` and fix the import.

- [ ] **Step 5: Register the nav entry** in `GenericManga` (class body):

```kotlin
    context(navGraph: EntryProviderScope<NavKey>)
    override fun globalNav3Setup() {
        super<GenericSharedManga>.globalNav3Setup()
        navGraph.entry<Screen.SourcePreferences> { MihonPreferencesScreen(it.serviceName) }
    }
```

(imports: `androidx.navigation3.runtime.EntryProviderScope`, `androidx.navigation3.runtime.NavKey`, `com.programmersbox.kmpuiviews.presentation.Screen`.) If the compiler reports ambiguity between `GenericInfo`/`GenericSharedManga` defaults, this explicit override resolves it.

- [ ] **Step 6: Build**

Run: `./gradlew :kmpuiviews:jvmTest :mangaworld:assembleNoFirebaseDebug :mangaworld:desktop:compileKotlinJvm`
Expected: BUILD SUCCESSFUL (desktop unaffected; it has no entry and no configurable sources).

- [ ] **Step 7: Commit**

```bash
git add kmpuiviews mihonsupport mangaworld
git commit -m "feat(mihon): source preferences screen for configurable Mihon sources"
```

---

### Task 10: Release build and on-device verification

**Files:**
- Modify (only if R8 fails): `mangaworld/proguard-rules.pro`
- Modify: `CLAUDE.md` (module table row), `README.md:30` (Mihon sentence)

**Interfaces:**
- Consumes: everything above.

- [ ] **Step 1: R8 build**

Run: `./gradlew :mangaworld:assembleNoFirebaseBeta`
Expected: BUILD SUCCESSFUL. Existing keep rules at `mangaworld/proguard-rules.pro:238-262` already keep `eu.kanade.**`, `tachiyomi.**`, `mihon.**`, `rx.**`, `uy.kohesive.injekt.**`, `app.cash.quickjs.**`. If R8 reports missing classes, add only the `-dontwarn` lines R8 prints in `build/outputs/mapping/.../missing_rules.txt` to `mangaworld/proguard-rules.pro`.

- [ ] **Step 2: Full module test pass**

Run: `./gradlew :mihonsupport:allTests :mihonsupport:testAndroidHostTest :mihonsupport:compileKotlinJvm :kmpmodels:jvmTest :kmpuiviews:jvmTest`
Expected: all PASS.

- [ ] **Step 3: Device check** — `adb devices -l`; if more than one device, ask the user which serial to use. Install: `./gradlew :mangaworld:installNoFirebaseDebug`. Then drive (ARTEMIS `mobile_run_task`, Flash model is enough) this path and confirm each result:
  1. Settings → Extensions → overflow → "Add Custom Tachiyomi Bridge" → add `https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json`.
  2. Remote extensions pane lists Keiyoushi extensions under "Mihon".
  3. Install "MangaDex (18+)" (accept system installer prompt).
  4. Installed list shows MangaDex under "Mihon" as a loaded source (repo-signed, so trusted without prompt). Set it as current source.
  5. Recent/All load covers; search "one piece" returns results; open a title → details + chapters.
  6. Open a chapter → images render.
  7. Back to Extensions → gear on MangaDex → preference screen renders; change a setting (e.g. a language/quality option), go back, reload list → setting took effect.
  8. Sideload an unsigned-by-repo build of any extension (or temporarily remove the repo URL and refresh) → entry shows "(Untrusted)" with a Trust button; Trust → it loads.
  9. Uninstall MangaDex → entry disappears from the installed list.

- [ ] **Step 4: Docs** — `CLAUDE.md` module table, add row after `source_utilities`:

```markdown
| `mihonsupport`            | Native Mihon extension runtime (vendored `eu.kanade.tachiyomi.*`), loader, repo client, mapper to `Kmp*` models (KMP; Android implementation only) |
```

`README.md:30`: replace the sentence about Mihon sources working "after the bridge is installed" with: "For MangaWorld: Mihon extensions work natively — add a Mihon repo URL in Extensions and install from there."

- [ ] **Step 5: Commit**

```bash
git add CLAUDE.md README.md mangaworld/proguard-rules.pro
git commit -m "docs(mihon): document native Mihon support"
```

---

## Addendum tasks (after on-device testing; see spec "Addendum")

### Task 11: Source display name

**Files:**
- Modify: `kmpmodels/src/commonMain/kotlin/com/programmersbox/kmpmodels/ApiService.kt` (`KmpApiService`)
- Modify: `mihonsupport/src/androidMain/kotlin/com/programmersbox/mihon/MihonApiService.kt`, `.../UntrustedMihonApiService.kt`
- Modify: shared UI call sites in `kmpuiviews` that *display* `serviceName` (find with `grep -rn "serviceName" kmpuiviews/src/commonMain | grep -i "text\|title\|stringResource"`): at minimum the Recent/All top bar title, `Res.string.currentSource` in `ExtensionListScreen.kt`, the source chooser rows, and the details screen source label.
- Test: `kmpmodels/src/commonTest/.../DisplayNameTest.kt`, extend `mihonsupport/src/androidHostTest/.../MihonSourceMapperTest.kt`

**Interfaces:**
- Produces: `KmpApiService.displayName: String` (default `serviceName`).

- [ ] **Step 1: Failing tests** — kmpmodels: an object implementing `KmpApiService` without overriding has `displayName == serviceName`. mihonsupport: `mihonApiService(FakeCatalogueSource()).displayName == "Fake (en)"` and `serviceName == "mihon:42"` (unchanged).
- [ ] **Step 2: Run** `./gradlew :kmpmodels:jvmTest :mihonsupport:testAndroidHostTest` → FAIL (unresolved/does not override).
- [ ] **Step 3: Implement** — in `KmpApiService`: `val displayName: String get() = serviceName`. In `MihonApiService` change `val displayName` to `override val displayName`. In `UntrustedMihonApiService` add a `label` constructor param and `override val displayName = label` (pass the label from `MihonExtensionLoader.notLoaded`).
- [ ] **Step 4: Swap display call sites** to `sourceDisplayName`; never change lookups
  (`toSourceByApiServiceName`, DB writes, `currentService` persistence) — those keep `serviceName`.
- [ ] **Step 5: Verify** `./gradlew :kmpmodels:jvmTest :mihonsupport:testAndroidHostTest :kmpuiviews:jvmTest :mangaworld:assembleNoFirebaseDebug :animeworld:assembleNoFirebaseDebug`.
- [ ] **Step 6: Commit** `feat(mihon): show source display names instead of service ids`.

### Task 12: index_v2 repos

**Files:**
- Modify: `gradle/common.versions.toml` (add `kotlinxSerializationProtobuf = { module = "org.jetbrains.kotlinx:kotlinx-serialization-protobuf", version = "1.11.0" }` next to `kotlinxSerialization`), `mihonsupport/build.gradle.kts` (commonMain dep)
- Create: `mihonsupport/src/commonMain/kotlin/com/programmersbox/mihon/repo/MihonIndexV2.kt` (protobuf + JSON models), `.../repo/Gzip.kt` (`expect fun gunzip(bytes: ByteArray): ByteArray` + `fun isGzip(bytes: ByteArray)`), `mihonsupport/src/jvmMain/.../repo/Gzip.jvm.kt`, `mihonsupport/src/androidMain/.../repo/Gzip.android.kt`, `mihonsupport/src/iosMain/.../repo/Gzip.ios.kt`
- Modify: `mihonsupport/src/commonMain/kotlin/com/programmersbox/mihon/repo/MihonRepoModels.kt` (`indexV2`), `.../repo/MihonRepoClient.kt`
- Test: `mihonsupport/src/commonTest/.../repo/MihonIndexV2Test.kt` (plain protobuf + JSON), `mihonsupport/src/jvmTest/.../repo/MihonIndexV2GzipTest.kt` (gzip via `java.util.zip.GZIPOutputStream`)

**Interfaces:**
- Consumes: `MihonLib`, `MihonRepoClient` (fetch/signingKeys contract unchanged: keys only for repos in latest fetch, https-only, CancellationException rethrown).

Protobuf models (field numbers from `mihonapp/extensions-lib/index/index.proto`, verbatim):

```kotlin
@Serializable data class V2Index(
    @ProtoNumber(1) val name: String = "",
    @ProtoNumber(2) val badgeLabel: String = "",
    @ProtoNumber(3) val signingKey: String = "",
    @ProtoNumber(4) val contact: V2Contact? = null,
    @ProtoOneOf val extensions: V2Extensions? = null,
)
@Serializable sealed interface V2Extensions
@Serializable data class V2InlineList(@ProtoNumber(101) val extensionList: V2ExtensionList) : V2Extensions
@Serializable data class V2ListUrl(@ProtoNumber(102) val extensionListUrl: String) : V2Extensions
@Serializable data class V2Contact(@ProtoNumber(1) val website: String = "", @ProtoNumber(2) val discord: String? = null)
@Serializable data class V2ExtensionList(@ProtoNumber(1) val extensions: List<V2Extension> = emptyList())
@Serializable data class V2Extension(
    @ProtoNumber(1) val name: String = "",
    @ProtoNumber(2) val packageName: String = "",
    @ProtoNumber(3) val resources: V2Resources? = null,
    @ProtoNumber(4) val extensionLib: String = "",
    @ProtoNumber(5) val versionCode: Long = 0,
    @ProtoNumber(6) val versionName: String = "",
    @ProtoNumber(7) val contentWarning: V2ContentWarning = V2ContentWarning.CONTENT_WARNING_UNSPECIFIED,
    @ProtoNumber(8) val sources: List<V2Source> = emptyList(),
)
@Serializable data class V2Resources(@ProtoNumber(1) val apkUrl: String = "", @ProtoNumber(2) val iconUrl: String = "")
@Serializable data class V2Source(
    @ProtoNumber(1) val id: Long = 0, @ProtoNumber(2) val name: String = "", @ProtoNumber(3) val language: String = "",
    @ProtoNumber(4) val homeUrl: String = "", @ProtoNumber(5) val mirrorUrls: List<String> = emptyList(),
    @ProtoNumber(7) val message: String? = null,
)
@Serializable enum class V2ContentWarning {
    @ProtoNumber(0) CONTENT_WARNING_UNSPECIFIED, @ProtoNumber(1) CONTENT_WARNING_SAFE,
    @ProtoNumber(2) CONTENT_WARNING_MIXED, @ProtoNumber(3) CONTENT_WARNING_NSFW,
}
```
(If `@ProtoOneOf` with these exact class shapes doesn't compile/decode in kotlinx-serialization 1.11.0, follow its documented oneof pattern; keep field numbers.) For proto3-JSON, decode the same JSON keys (`extensionList` / `extensionListUrl` objects, enum names as strings, int64 possibly quoted) with separate JSON-tolerant data classes or a lenient `Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }` against JSON-shaped mirror classes.

- [ ] **Step 1: Failing tests** (commonTest, MockEngine):
  - repo.json with `index_v2` → client fetches that URL, decodes a protobuf `V2Index` built in-test via `ProtoBuf.encodeToByteArray`, maps entries: name, `packageName`, `versionName`, `iconUrl`, `apkUrl`, sources (`homeUrl` as baseUrl); NSFW gets " (18+)"; `extensionLib` "1.5"/"1.3" filtered out; "1.4"/"1.6" kept.
  - `extensionListUrl` variant: index points at a second URL serving an `V2ExtensionList` → same mapping.
  - JSON variant of the index decodes to the same result.
  - signing key from `repo.json` meta; when meta lacks it, from `Index.signingKey`.
  - http `index_v2` URL is rejected (no entries, no key).
  - repo.json without `index_v2` still uses legacy `index.min.json` (existing tests stay green).
  jvmTest: gzip-compressed protobuf index decodes.
- [ ] **Step 2: Run** `./gradlew :mihonsupport:jvmTest` → FAIL.
- [ ] **Step 3: Implement** models, gunzip expect/actuals, and the v2 branch in `MihonRepoClient.fetchRepo` (keeping the Task-5/fix-wave guarantees: mutex-guarded keys, scoped to latest fetch, CancellationException rethrown, https-only).
- [ ] **Step 4: Run** `./gradlew :mihonsupport:jvmTest :mihonsupport:testAndroidHostTest :mihonsupport:compileKotlinIosSimulatorArm64 :mangaworld:assembleNoFirebaseDebug` → PASS.
- [ ] **Step 5: Commit** `feat(mihon): support Mihon index_v2 extension repos`.
- [ ] **Step 6: Device** — controller verifies Keiyoushi remote list populates and an extension installs.
