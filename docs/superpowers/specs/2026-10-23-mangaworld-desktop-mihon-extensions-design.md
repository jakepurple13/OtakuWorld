# MangaWorld Desktop: Mihon Extensions — Design

Date: 2026-10-23
Status: Draft, pending approval

## Intent

MangaWorld Desktop (`mangaworld:desktop`, JVM) should use the same Mihon extensions the Android
app uses, managed the same way: add a repo, browse its extensions, install, trust, update,
uninstall, and read manga from the resulting sources. Today desktop only loads OtakuWorld-format
`*.jar` plugins and has no Mihon support at all.

Mihon extensions are Android APKs (Dalvik bytecode) that call a small set of Android APIs. Desktop
therefore needs (1) an APK → JVM jar conversion step, (2) a class-loading path, (3) the vendored
Mihon runtime compiled for JVM, and (4) JVM stand-ins for the Android APIs extensions touch.

## Goals

- From the existing Extensions screens on desktop, the user can add a Mihon repo URL, see its
  extensions, install one, and its sources show up alongside other sources and work (browse,
  search, details, chapter list, reader pages).
- Trust behaves as on Android: an extension signed with a repo's signing key is trusted
  automatically; otherwise the existing "trust this version" prompt is shown.
- Update and uninstall work from the same screens.
- Works on macOS (arm64 + x64) and Windows (x64) when run via `./gradlew :mangaworld:desktop:run`.
  Linux should work by construction but is not a target.
- Android behavior is unchanged.

## Non-goals (v1)

- **Cloudflare / WebView-dependent sources.** `CloudflareInterceptor` and anything needing a
  WebView will not solve challenges on desktop; such requests fail with a clear error. A headless
  browser (KCEF/JCEF etc.) is a later phase.
- **Source settings (`ConfigurableSource`).** Configurable sources still load and work with
  default preferences, but desktop does not show a settings button/screen for them. (User
  decision.)
- **Background update checks.** Android doesn't run them for Mihon either; updates are shown when
  the Extensions screen is open.
- Aniyomi extensions / AnimeWorld desktop.
- **Packaged installers (DMG/MSI).** They must work eventually (user requirement), but v1 is
  verified through `./gradlew :mangaworld:desktop:run` only. A follow-up phase covers packaging:
  jlink modules for TLS and signature verification (today only `jdk.unsupported`), ProGuard keep
  rules for the vendored `eu.kanade.tachiyomi.*` and `android.*` stub ABI that extensions link
  against by name at runtime, and the macOS entitlements (library validation / JIT, currently
  commented out in `mangaworld/desktop/build.gradle.kts`) so zipline's QuickJS native library
  loads in a signed app. v1 should not make choices that block that phase.

## Anti-goals

- Any regression to Android Mihon support or to desktop's existing jar-plugin loading.
- Loading an extension whose signature was not verified as trusted (repo key or explicit
  per-version trust). Desktop must not be a weaker trust path than Android.
- Mihon extension classes leaking into or clashing with app classes beyond what Android's
  child-first loader allows.

## Current state (from recon)

Shared already (works on JVM as-is):

- Extensions UI + ViewModel in `kmpuiviews` commonMain: `ExtensionListScreen`,
  `ExtensionCatalogScreen`, `InstalledPane`, `ExtensionDetailSheet`, `ExtensionStates`,
  `ExtensionListViewModel`. It already queries `NativeCatalogs` for remote sources, offers
  installs to `catalog.install()` before falling back to `DownloadStateRepository`, and handles
  untrusted sources via `KmpUntrustedApiService`.
- `mihonsupport` commonMain: `MihonRepoClient` (repo.json, legacy + v2 index, signing keys),
  `MihonRepoModels`, `MihonIndexV2`, `MihonLib` (supported lib versions 1.4–1.6), `MihonTrust`,
  `Gzip` (has a `jvmMain` actual).
- Repo URLs (`customUrls`) and trusted IDs (`trustedMihonExtensions`) live in shared datastore
  settings.

Android-only today:

- `mihonsupport/src/androidMain`: `MihonCatalog`, `MihonModule`, `MihonRuntime`,
  `MihonExtensionLoader` (PackageManager scan + `DelegateLastClassLoader`), `MihonApiService` /
  `ConfigurableMihonApiService`, `MihonSourceMapper`, `UntrustedMihonApiService`,
  `MihonPreferenceFragment`, and the vendored runtime (`eu.kanade.tachiyomi.*`, `tachiyomi.*`,
  `mihon.*`, `app.cash.quickjs` shim over zipline).
- Install/uninstall go through the system package installer.

Desktop today:

- `mangaworld/desktop` does not depend on `mihonsupport` and registers no catalogs.
- JVM `SourceLoader` (`sharedutils/kmpextensionloader/src/jvmMain`) scans
  `MangaDesktopSettings.extensionDirectory` for `*.jar`, parent-first `URLClassLoader`, and does
  not accept `NativeCatalogs`.
- Unused but present in the same module: `DexConverter` (dex2jar v76, SHA-256-keyed jar cache),
  `ApkManifestParser` (apk-parser), and JVM stubs in real `android.*` packages (`Context`,
  `SharedPreferences`, `pm.*`, `Bundle`, `Build`, `Application`, `Log`, `Drawable`).
- JVM `DownloadStateRepository` is a no-op; JVM `SourceInfoRepository.versionName` returns the
  name and `uninstall` is a no-op.
- zipline 1.27.0's JVM jar bundles QuickJS natives for macOS arm64/x64, Windows x64/arm64, and
  Linux, so JS-using extensions are feasible on both target OSes.

## Design

### 1. Share the Mihon runtime between Android and JVM

Move the platform-neutral parts of `mihonsupport/src/androidMain` into a source set compiled for
both Android and JVM (e.g. a `jvmAndroidMain`/`jvmCommon` intermediate under the existing
hierarchy). This includes the vendored `eu.kanade.tachiyomi.source.*`, `network.*` (minus the
WebView pieces), `util.*` that are plain JVM, `MihonApiService`, `MihonSourceMapper`,
`UntrustedMihonApiService`, and the Injekt runtime setup. Their dependencies (okhttp, jsoup,
injekt, rxjava1, zipline, kotlinx-serialization) move with them; all have JVM artifacts.

Android-specific pieces become small expect/actual or interface seams with a JVM implementation:

| Piece | Android (unchanged) | JVM |
|---|---|---|
| Cookie store (`AndroidCookieJar`) | `webkit.CookieManager` | in-memory/persisted cookie jar |
| Default user agent (`WebViewUtil`, `WebViewInterceptor`) | from WebView | fixed desktop browser UA |
| `CloudflareInterceptor` | WebView solver | interceptor that fails CF-challenged responses with a clear error |
| `DeviceUtil`, `AppInfo` | Android APIs | constants / system properties |
| Logging (`squareLogcat`) | logcat | stdout/JVM logging |
| `RateLimitInterceptor` `SystemClock` | `SystemClock` | `System.nanoTime`-based |

The **extension ABI must stay byte-compatible**: class and method signatures extensions are
compiled against (`Source`, `HttpSource`, `ParsedHttpSource`, `ConfigurableSource`, `SManga`,
`SChapter`, `Page` incl. `Page.uri: android.net.Uri`, `NetworkHelper`, etc.) keep their exact
packages and signatures. Where those signatures reference Android types, JVM provides stub types
in the real packages (`android.net.Uri`, `android.content.Context`, `SharedPreferences`,
`androidx.preference.PreferenceScreen` and the preference classes extensions construct, etc.).
Exactly which stub classes are needed is for the builder to establish by converting and loading
real extensions.

### 2. JVM Android stubs: one home

The existing stubs in `sharedutils/kmpextensionloader/src/jvmMain/kotlin/com/programmersbox/android/`
already use real `android.*` packages. The Mihon JVM runtime and extensions must see a single copy
of each stub class on the app classpath. Builder's call where they live (keep in
`kmpextensionloader` and have `mihonsupport` jvm depend on it, or move them to a dedicated module),
as long as there is one copy and the desktop app gets it.

The existing stubs do not all match Android's ABI (e.g. `SharedPreferences` is a concrete class;
on Android it is an interface, so extensions compiled against android.jar would hit
`IncompatibleClassChangeError`). No desktop jar plugins are built against these stubs (user
confirmed), so rewrite them freely to match Android's real shapes (class vs interface, method
signatures). `SharedPreferences` on JVM must persist
per `preferenceKey()` (file-backed) so sources keep defaults/state across runs even without a
settings UI.

### 3. Install, uninstall, update on desktop

Desktop has no package installer. Install follows the same route Android uses — the shared
ViewModel falls back to `DownloadStateRepository.downloadAndInstall(link)` — so the existing
install-state UI (progress, error, retry, keyed by download link) works unchanged. `MihonCatalog`
does **not** override `install()`: `KmpExternalCustomApiServicesCatalog.install()` only returns a
`Boolean`, and `installViaCatalogs` swallows exceptions, so that path cannot report progress or
errors. (My call.)

- **Install:** replace the no-op JVM `DownloadStateRepository` with a real one. For Mihon APK
  links it downloads into a private Mihon extensions directory under the app data dir (separate
  from the user-facing jar `extensionDirectory`), converts once with `DexConverter`, then triggers
  a source reload. Download progress, conversion, and failures (download, conversion, unsupported
  lib version) are reported through `downloadList` so the existing UI shows them. Replacing an
  existing install = update. On Windows, a jar held open by a live class loader may not be
  replaceable; close the old loader before replacing, or write the new version under a new file
  name (builder's call).
- **Uninstall:** `MihonCatalog` overrides `uninstall()` (or the JVM `SourceInfoRepository.uninstall`
  handles Mihon sources): delete the APK and its converted jar, reload.
- **Installed version:** read `versionName` / `versionCode` / package name from the APK manifest
  via `ApkManifestParser`, so the existing UI version comparison drives update badges. This needs
  the JVM `SourceInfoRepository.versionName` (or the Mihon `KmpSourceInformation`) to return the
  real version for Mihon sources.
- Failures (download, conversion, unsupported lib version) surface through the existing
  install-state UI, not silently.

### 4. Loading on desktop

A JVM `MihonExtensionLoader` mirrors the Android one, reading from the private directory instead of
`PackageManager`:

1. For each APK: parse manifest (feature `tachiyomi.extension`, `tachiyomi.extension.class`,
   nsfw, lib version from versionName prefix). Reject unsupported lib versions (`MihonLib`).
2. Verify the APK signature and get signer SHA-256 fingerprints with Google's `apksig`
   (pure-Java). Unsigned or invalid → not loaded.
3. Apply `MihonTrust` exactly as Android: repo signing key match → trusted; stored
   `"pkg:versionCode:fp"` match → trusted; otherwise produce `UntrustedMihonApiService` so the
   existing prompt appears.
4. Trusted → load the converted jar with a **child-first** class loader (parent = app class
   loader, same semantics as Android's `DelegateLastClassLoader`), instantiate the listed
   `Source`/`SourceFactory` classes, wrap in `MihonApiService`.

`MihonRuntime.init` on JVM registers the JVM `Application`/`Context` stub, `NetworkHelper`,
`JavaScriptEngine`, `Json`, `ProtoBuf` in Injekt, as Android does.

### 5. Registration into the desktop app

- JVM `SourceLoader` accepts `NativeCatalogs` (as Android's does) and merges their sources into
  `SourceRepository` with the jar-plugin sources.
- `mangaworld:desktop` depends on `mihonsupport` and includes a JVM `mihonModule` binding
  `MihonCatalog` as `KmpExternalCustomApiServicesCatalog`.
- Mihon sources keep `serviceName = "mihon:${source.id}"` so favorites/history keys match Android.

### 6. Unsupported features on desktop

- Configurable sources: the `KmpConfigurableApiService` entry point (settings button) is hidden on
  desktop. Either don't expose `ConfigurableMihonApiService` on JVM or gate the button by
  platform — builder's call.
- Cloudflare-blocked sources fail with an understandable error in the UI rather than an opaque
  exception.

## Constraints

- Kotlin Multiplatform; follow existing source-set and convention-plugin patterns (CLAUDE.md).
- Android build and Mihon behavior unchanged; existing `mihonsupport` tests keep passing.
- Desktop jar-plugin loading unchanged.
- Lib versions supported: same `MihonLib.SUPPORTED` as Android.
- macOS is the only OS the user can test on; Windows must be built-for but is verified later.

## Decisions log

User's decisions:

- Desktop manages Mihon extensions the same way the Android app does (repos → browse → install).
- v1 excludes Cloudflare/WebView sources.
- v1 excludes source settings on desktop.
- macOS and Windows are priority; only macOS can be tested now.
- No desktop jar plugins depend on the current `android.*` stubs; they may be rewritten to match
  Android.
- Installers must work eventually, but v1 targets `./gradlew :mangaworld:desktop:run`; packaging
  is a follow-up phase.

My calls (flagged for review):

- Trust identical to Android, using `apksig` for signature verification on desktop.
- Configurable sources still load in v1, just without a settings screen.
- Installed APKs + converted jars live in a private app-data directory, not the user's jar
  `extensionDirectory`.
- Install goes through a real JVM `DownloadStateRepository` (same route as Android), not
  `catalog.install()`, so progress and errors show in the existing UI.
- Child-first class loading, matching Android.
- No background update check on desktop (matches Android Mihon behavior).

## Left to the builder

- Exact source-set layout for the shared JVM/Android runtime.
- Where the single copy of the `android.*` stubs lives.
- Exact set of stub classes needed (driven by converting and loading real extensions).
- JVM cookie persistence approach and the fixed UA string.
- How the settings button is hidden for Mihon sources on desktop.

## Verification

- Unit tests: JVM loader (manifest parse, lib-version rejection, trust decisions with a signed test
  APK), install/uninstall file handling, `SourceLoader` merging native catalogs.
- Manual on macOS via `./gradlew :mangaworld:desktop:run`: add the standard Keiyoushi repo,
  install at least two non-Cloudflare extensions (one using `ParsedHttpSource`/Jsoup, one JSON
  API such as MangaDex), browse → details → chapters → read pages; update one; uninstall one;
  confirm an untrusted-signature APK shows the trust prompt.
- Android: `./gradlew :mangaworld:assembleNoFirebaseDebug` + existing `mihonsupport` tests pass.
