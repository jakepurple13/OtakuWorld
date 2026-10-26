# CloudStream app-module shims

CloudStream plugins (`.cs3`) are compiled against CloudStream's `app` module as well as its
`library`. Only the `library` is vendored (see `VENDORED.md`). Every `app` class a plugin links
against must exist here under the same FQCN with the same JVM signatures, or that plugin fails to
load (it becomes a failed placeholder; the app does not crash).

The list below comes from `scripts/dex-refs.sh` run over the four reference plugins from
`https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/builds/repo.json`:
Anichi (v28), AnimePahe (v43), Anizone (v10), HiAnime (v2). Types the plugins define themselves and
types already in the vendored `library` are left out.

`ShimPresenceTest` loads every type in the tables below whose status is not `missing`.

## `com.lagradost.*` types from CloudStream's `app` module

| Type | Needed by | Status |
|------|-----------|--------|
| `com.lagradost.cloudstream3.plugins.Plugin` | Anichi, AnimePahe, Anizone, HiAnime | shim |
| `com.lagradost.cloudstream3.CloudStreamApp` | Anichi, AnimePahe, Anizone, HiAnime | shim |
| `com.lagradost.cloudstream3.CloudStreamApp$Companion` | Anichi, AnimePahe, Anizone, HiAnime | shim |
| `com.lagradost.cloudstream3.CommonActivity` | Anichi, AnimePahe, Anizone, HiAnime | shim |
| `com.lagradost.cloudstream3.utils.DataStore` | AnimePahe | shim |
| `com.lagradost.cloudstream3.syncproviders.providers.AniListApi$Title` | Anichi | shim |
| `com.lagradost.cloudstream3.syncproviders.providers.AniListApi$CoverImage` | Anichi | shim |
| `com.lagradost.cloudstream3.syncproviders.providers.AniListApi$LikePageInfo` | Anichi | shim |
| `com.lagradost.cloudstream3.syncproviders.providers.AniListApi$SeasonNextAiringEpisode` | Anichi | shim |
| `com.lagradost.cloudstream3.syncproviders.providers.AniListApi$RecommendationConnection` | Anichi | shim |
| `com.lagradost.cloudstream3.syncproviders.providers.AniListApi$RecommendationEdge` | Anichi (via Jackson) | shim |
| `com.lagradost.cloudstream3.syncproviders.providers.AniListApi$Recommendation` | Anichi (via Jackson) | shim |
| `com.lagradost.cloudstream3.syncproviders.providers.AniListApi$RecommendedMedia` | Anichi (via Jackson) | shim |
| `com.lagradost.cloudstream3.syncproviders.providers.AniListApi$MediaTitle` | Anichi (via Jackson) | shim |
| `com.lagradost.cloudstream3.syncproviders.providers.AniListApi$MediaCoverImage` | Anichi (via Jackson) | shim |
| `com.lagradost.cloudstream3.AcraApplication` | older plugins (deprecated alias) | shim |
| `com.lagradost.cloudstream3.AcraApplication$Companion` | older plugins (deprecated alias) | shim |
| `com.lagradost.cloudstream3.network.CloudflareKiller` | common in other repos | shim |
| `com.lagradost.cloudstream3.network.DdosGuardKiller` | common in other repos | shim |
| `com.lagradost.cloudstream3.network.RequestsHelperKt` | common in other repos (`getHeaders`) | shim |
| `com.lagradost.cloudstream3.utils.AppContextUtils` | common in other repos | shim (subset) |
| `com.lagradost.cloudstream3.actions.VideoClickAction` | `Plugin.registerVideoClickAction` | shim (never invoked) |
| `com.lagradost.cloudstream3.utils.UiText` | `CommonActivity.showToast(UiText)`, `VideoClickAction.name` | missing |
| `com.lagradost.cloudstream3.ui.result.ResultEpisode` | `VideoClickAction` subclasses | missing |
| `com.lagradost.cloudstream3.ui.result.LinkLoadingResult` | `VideoClickAction` subclasses | missing |

The three `missing` types are UI-layer classes with large dependency graphs. None of the four
reference plugins uses them. A plugin that calls the `UiText` toast overloads, or that registers
a `VideoClickAction` subclass, loads; the class that touches the missing type fails with
`NoClassDefFoundError` only when that code runs.

Member-level references seen in the dex (all present in the shims):
`CloudStreamApp.Companion.getContext()`, `CloudStreamApp.Companion.setKey(String, Object)`,
`CommonActivity.INSTANCE`, `CommonActivity.getActivity()`,
`CommonActivity.showToast$default(CommonActivity, String, Integer, int, Object)`,
`DataStore.INSTANCE`, `DataStore.getSharedPrefs(Context)`, `Plugin.<init>()`.

### Shim simplifications (`// OTAKU:` in the sources)

- `Plugin.registerVideoClickAction` is a no-op: AnimeWorld has no external-player action menu.
- `CloudStreamApp` is never instantiated. Its `context` setter is `internal` so
  `CloudstreamShims.install` can set it. `ExceptionHandler`, Coil and the TV-layout check in
  `openBrowser(url, activity)` are dropped.
- Key/value storage uses SharedPreferences `"cloudstream_data"` (upstream: `"rebuild_preference"`),
  keeping CloudStream data apart from the host app's preferences. Values are encoded with the
  vendored `AppUtils.toJsonLiteral`/`parseJson` (kotlinx.serialization, falling back to Jackson),
  as upstream does.
- `DataStore.getDefaultSharedPrefs` reads `<packageName>_preferences` directly instead of going
  through `androidx.preference`.
- `CommonActivity` keeps `activity`, `setActivityInstance` and the `showToast` overloads that do not
  need `UiText`; toasts are plain `Toast.makeText` instead of CloudStream's custom layout.
- `AppContextUtils` keeps the plugin-facing helpers that do not need CloudStream's UI:
  `html`, `openBrowser`, `openWebView` (no-op), `isNetworkAvailable`, `splitQuery`,
  `isUsingMobileData`, `isAppInstalled`.
- `VideoClickAction` keeps the members that do not need `UiText`/`ResultEpisode`/`LinkLoadingResult`.
- `AniListApi` is only a holder for the nested data classes plugins use; it does not extend
  `SyncAPI`.

## Host libraries plugins link against

| Type | Needed by | Status |
|------|-----------|--------|
| `com.lagradost.nicehttp.Requests` | Anichi, AnimePahe, Anizone, HiAnime | module dep (NiceHttp) |
| `com.lagradost.nicehttp.NiceResponse` | Anichi, AnimePahe, Anizone, HiAnime | module dep (NiceHttp) |
| `com.lagradost.nicehttp.ResponseParser` | Anichi, AnimePahe, Anizone, HiAnime | module dep (NiceHttp) |
| `com.fasterxml.jackson.databind.ObjectMapper` | Anichi, AnimePahe, HiAnime | module dep (jackson) |
| `com.fleeksoft.ksoup.Ksoup` | AnimePahe | module dep (ksoup) |
| `io.ktor.http.Url` | Anichi, AnimePahe | module dep (ktor) |
| `kotlinx.serialization.json.Json` | Anichi, AnimePahe, HiAnime | module dep |
| `kotlinx.coroutines.sync.Mutex` | AnimePahe | module dep |
| `okhttp3.OkHttpClient` | Anichi, AnimePahe, Anizone, HiAnime | module dep |
| `org.jsoup.Jsoup` | Anichi, AnimePahe, Anizone, HiAnime | module dep |
| `androidx.appcompat.app.AppCompatActivity` | Anichi, AnimePahe, Anizone, HiAnime | module dep (appcompat, added) |
| `androidx.appcompat.app.AlertDialog$Builder` | AnimePahe | module dep (appcompat, added) |
| `androidx.fragment.app.DialogFragment` | Anichi, AnimePahe, Anizone, HiAnime | module dep (fragment, added) |
| `androidx.fragment.app.FragmentActivity` | Anichi, AnimePahe | module dep (fragment, added) |
| `androidx.fragment.app.FragmentManager` | Anichi, AnimePahe, Anizone, HiAnime | module dep (fragment, added) |
| `com.google.android.material.bottomsheet.BottomSheetDialogFragment` | Anichi, AnimePahe | module dep (material, added) |
| `com.google.android.material.bottomsheet.BottomSheetBehavior` | Anichi, AnimePahe | module dep (material, added) |
| `com.google.android.material.R$id` | Anichi, AnimePahe | app (material resources; kept by consumer rules) |
| `com.google.gson.Gson` | HiAnime | module dep (gson, added) |
| `com.google.gson.JsonParser` | HiAnime | module dep (gson, added) |

`org.xmlpull.v1` (AnimePahe, Anizone) comes from the Android framework. `R$id` classes are only
generated when an app is built, so `ShimPresenceTest` skips the `app` row.

## Desktop (JVM)

On desktop the `android.*` / `androidx.*` / material types above resolve to the link-time stubs in
`:sharedutils:kmpextensionloader` (`src/jvmMain`); appcompat, fragment and material are not on the
JVM classpath. Converted plugins load through `ChildFirstClassLoader` with `CLOUDSTREAM_PARENT_FIRST`
(`src/jvmMain/.../plugins/CloudstreamParentFirst.kt`). `org.xmlpull` is not parent-first on desktop:
the JVM host does not ship it.

`CloudstreamPluginScanTest` (opt-in, `CS_SCAN=1`) converts the four reference plugins plus the
`HiAnime` fixture and reports every type and member they reference that the JVM host lacks, and every
class that fails to load. Stubs were added for everything that broke class loading, plus the
supertypes of the plugins' screens, so every non-screen class of the four plugins loads and links:

- `android.content.DialogInterface` with `OnClickListener`, `OnKeyListener`, `OnCancelListener`,
  `OnDismissListener`; `android.view.View.OnClickListener` / `OnFocusChangeListener`;
  `android.widget.CompoundButton` with `OnCheckedChangeListener`; `android.view.KeyEvent` /
  `InputEvent`. Settings and donation dialogs implement these with synthetic lambda classes, and the
  JVM loads a class's interfaces together with the class.
- `androidx.core.app.ComponentActivity` -> `androidx.activity.ComponentActivity` ->
  `androidx.fragment.app.FragmentActivity` -> `androidx.appcompat.app.AppCompatActivity`,
  `FragmentManager`, `FragmentTransaction`, `DialogFragment`, `AppCompatDialogFragment`,
  `com.google.android.material.bottomsheet.BottomSheetDialogFragment`. Plugins only reach these
  after `CommonActivity.activity is AppCompatActivity`, which is never true on desktop.
- `Context.getApplicationContext()` (returns the context itself). Anichi calls it in `load`.

## JVM: missing at runtime only

These types are still missing on desktop. Plugins only touch them inside method bodies desktop never
runs: settings, donation and WebView dialogs (`Fragment`/`Activity` subclasses, which desktop never
creates, or lambdas those dialogs create). A plugin class that mentions one still loads; the code fails
with `NoClassDefFoundError` only if it runs. `CloudstreamJvmRuntimeTest` skips these names when it
checks the tables above on the JVM (two columns, so `ShimPresenceTest` does not read this table).

| Type | Used by (where) |
|------|-----------------|
| `android.app.Dialog` | Anichi, AnimePahe, Anizone, HiAnime (only inside Fragment/Activity subclasses) |
| `android.content.res.ColorStateList` | AnimePahe, Anizone (only inside Fragment/Activity subclasses) |
| `android.content.res.Configuration` | Anichi, AnimePahe, Anizone, HiAnime (only inside Fragment/Activity subclasses) |
| `android.content.res.Resources$Theme` | AnimePahe, Anizone (only inside Fragment/Activity subclasses) |
| `android.content.res.XmlResourceParser` | AnimePahe, Anizone (only inside Fragment/Activity subclasses) |
| `android.graphics.Color` | Anichi, AnimePahe, Anizone, HiAnime (only inside Fragment/Activity subclasses) |
| `android.graphics.ColorFilter` | Anichi, AnimePahe (cursor drawable of the WebView dialogs (Anichi turnstile, AnimePahe Cloudflare)) |
| `android.graphics.Matrix` | Anichi, AnimePahe (only inside Fragment/Activity subclasses) |
| `android.graphics.Paint$Join` | Anichi, AnimePahe (cursor drawable of the WebView dialogs) |
| `android.graphics.Paint$Style` | Anichi, AnimePahe (cursor drawable of the WebView dialogs) |
| `android.graphics.Path` | Anichi, AnimePahe (cursor drawable of the WebView dialogs) |
| `android.graphics.drawable.ColorDrawable` | Anichi, AnimePahe, Anizone, HiAnime (only inside Fragment/Activity subclasses) |
| `android.graphics.drawable.GradientDrawable` | Anichi, AnimePahe, Anizone, HiAnime (fields of the donation dialog's listener lambdas) |
| `android.graphics.drawable.GradientDrawable$Orientation` | Anichi, AnimePahe, Anizone, HiAnime (only inside Fragment/Activity subclasses) |
| `android.text.SpannableStringBuilder` | Anichi, AnimePahe, Anizone, HiAnime (only inside Fragment/Activity subclasses) |
| `android.text.style.ForegroundColorSpan` | Anichi, AnimePahe, Anizone, HiAnime (only inside Fragment/Activity subclasses) |
| `android.text.style.StyleSpan` | Anichi, AnimePahe, Anizone, HiAnime (only inside Fragment/Activity subclasses) |
| `android.util.AttributeSet` | Anichi, AnimePahe (only inside Fragment/Activity subclasses) |
| `android.view.LayoutInflater` | Anichi, AnimePahe, Anizone, HiAnime (only inside Fragment/Activity subclasses) |
| `android.view.MotionEvent` | Anichi, AnimePahe (Anichi turnstile solver's WebView click simulation) |
| `android.view.Window` | Anichi, AnimePahe, Anizone, HiAnime (Anichi turnstile solver, only when `CommonActivity.activity` is an AppCompatActivity) |
| `android.webkit.WebStorage` | AnimePahe (only inside Fragment/Activity subclasses) |
| `android.widget.ArrayAdapter` | Anizone (only inside Fragment/Activity subclasses) |
| `android.widget.FrameLayout` | Anichi, AnimePahe, Anizone, HiAnime (lambdas of the WebView dialogs) |
| `android.widget.FrameLayout$LayoutParams` | Anichi, AnimePahe (lambdas of the WebView dialogs) |
| `android.widget.ImageView` | Anichi, AnimePahe, Anizone (lambdas of the WebView dialogs) |
| `android.widget.LinearLayout` | Anichi, AnimePahe, Anizone, HiAnime (only inside Fragment/Activity subclasses) |
| `android.widget.LinearLayout$LayoutParams` | Anichi, AnimePahe, Anizone, HiAnime (only inside Fragment/Activity subclasses) |
| `android.widget.ProgressBar` | Anichi, AnimePahe (only inside Fragment/Activity subclasses) |
| `android.widget.RadioButton` | AnimePahe (listener lambda of AnimePahe's domain sheet) |
| `android.widget.Spinner` | Anizone (listener lambda of Anizone's settings fragment) |
| `android.widget.SpinnerAdapter` | Anizone (only inside Fragment/Activity subclasses) |
| `android.widget.Switch` | AnimePahe (only inside Fragment/Activity subclasses) |
| `androidx.appcompat.app.AlertDialog` | AnimePahe (only inside Fragment/Activity subclasses) |
| `androidx.appcompat.app.AlertDialog$Builder` | AnimePahe (only inside Fragment/Activity subclasses) |
| `com.google.android.material.R$id` | Anichi, AnimePahe (only inside Fragment/Activity subclasses) |
| `com.google.android.material.bottomsheet.BottomSheetBehavior` | Anichi, AnimePahe (only inside Fragment/Activity subclasses) |
| `com.google.android.material.bottomsheet.BottomSheetDialog` | Anichi, AnimePahe (only inside Fragment/Activity subclasses) |
| `org.xmlpull.v1.XmlPullParser` | AnimePahe, Anizone (only inside Fragment/Activity subclasses) |

Members still missing on types the desktop does have are in the same places: `WebView` /
`WebSettings` / `WebViewClient` / `WebChromeClient` / `CookieManager` members (WebView dialogs and
Anichi's turnstile solver; desktop has no WebView), `Paint` / `Canvas` drawing members (cursor
drawables), `View` / `TextView` / `Button` setters and listener setters (dialogs), `Activity`
members such as `isFinishing`, `isDestroyed`, `runOnUiThread` and `getWindow` (only after
`activity is AppCompatActivity`), `ContextWrapper.getBaseContext` (settings callbacks looking for the
activity), `Handler.removeCallbacks*`, `Intent.makeRestartActivityTask` and
`PackageManager.getLaunchIntentForPackage` (restart-the-app buttons). The full list is in
`build/cloudstream-scan/scan.md` after a scan.
