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
