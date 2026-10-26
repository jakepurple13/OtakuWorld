#!/usr/bin/env bash
# Re-fetches vendored Aniyomi files at a pinned commit. Usage: scripts/vendor-aniyomi.sh [sha]
# Overwrites local edits; re-apply every edit marked "OTAKU:" (listed in VENDORED.md).
set -euo pipefail
SHA="${1:-97414446b8a95994c72dd33c41c971a89d4d25b8}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE="https://raw.githubusercontent.com/aniyomiorg/aniyomi/$SHA"
HEADER="// Copied from aniyomiorg/aniyomi $SHA, Apache-2.0. Local edits marked with \"OTAKU:\"."

# Files that need WebView/Android APIs stay in androidMain; the rest is shared with the JVM target.
ANDROID_ONLY=" \
  eu/kanade/tachiyomi/network/AndroidCookieJar.kt \
  eu/kanade/tachiyomi/network/interceptor/CloudflareInterceptor.kt \
  eu/kanade/tachiyomi/network/interceptor/WebViewInterceptor.kt \
  eu/kanade/tachiyomi/util/system/DeviceUtil.kt \
  eu/kanade/tachiyomi/util/system/WebViewUtil.kt \
  tachiyomi/core/common/util/system/LogcatExtensions.kt "

fetch() { # $1 = repo path, $2 = path under the source dir
  local dest="$ROOT/src/jvmAndroidShared/kotlin"
  [[ "$ANDROID_ONLY" == *" $2 "* ]] && dest="$ROOT/src/androidMain/kotlin"
  mkdir -p "$dest/$(dirname "$2")"
  { echo "$HEADER"; curl -fsSL "$BASE/$1"; } > "$dest/$2"
}

SA=source-api/src/commonMain/kotlin
for f in \
  eu/kanade/tachiyomi/animesource/AnimeCatalogueSource.kt \
  eu/kanade/tachiyomi/animesource/AnimeSource.kt \
  eu/kanade/tachiyomi/animesource/AnimeSourceFactory.kt \
  eu/kanade/tachiyomi/animesource/ConfigurableAnimeSource.kt \
  eu/kanade/tachiyomi/animesource/UnmeteredSource.kt \
  eu/kanade/tachiyomi/animesource/model/AnimeFilter.kt \
  eu/kanade/tachiyomi/animesource/model/AnimeFilterList.kt \
  eu/kanade/tachiyomi/animesource/model/AnimeRelation.kt \
  eu/kanade/tachiyomi/animesource/model/AnimeUpdateStrategy.kt \
  eu/kanade/tachiyomi/animesource/model/AnimesPage.kt \
  eu/kanade/tachiyomi/animesource/model/FetchType.kt \
  eu/kanade/tachiyomi/animesource/model/Hoster.kt \
  eu/kanade/tachiyomi/animesource/model/HttpServer.kt \
  eu/kanade/tachiyomi/animesource/model/SAnime.kt \
  eu/kanade/tachiyomi/animesource/model/SAnimeEpisodeUpdate.kt \
  eu/kanade/tachiyomi/animesource/model/SAnimeImpl.kt \
  eu/kanade/tachiyomi/animesource/model/SAnimeSeasonUpdate.kt \
  eu/kanade/tachiyomi/animesource/model/SEpisode.kt \
  eu/kanade/tachiyomi/animesource/model/SEpisodeImpl.kt \
  eu/kanade/tachiyomi/animesource/model/ThumbnailInfo.kt \
  eu/kanade/tachiyomi/animesource/model/Video.kt \
  eu/kanade/tachiyomi/animesource/online/AnimeHttpSource.kt \
  eu/kanade/tachiyomi/animesource/online/ParsedAnimeHttpSource.kt \
  eu/kanade/tachiyomi/animesource/online/ResolvableAnimeSource.kt \
  eu/kanade/tachiyomi/animesource/utils/Preferences.kt \
  eu/kanade/tachiyomi/util/JsonExtensions.kt \
  eu/kanade/tachiyomi/util/JsoupExtensions.kt; do
  fetch "$SA/$f" "$f"
done

# Android actual of the commonMain `expect class PreferenceScreen` (the expect is not vendored).
fetch source-api/src/androidMain/kotlin/eu/kanade/tachiyomi/animesource/PreferenceScreen.kt \
  eu/kanade/tachiyomi/animesource/PreferenceScreen.kt

CC=core/common/src/main/java
for f in \
  eu/kanade/tachiyomi/network/AndroidCookieJar.kt \
  eu/kanade/tachiyomi/network/DohProviders.kt \
  eu/kanade/tachiyomi/network/HttpException.kt \
  eu/kanade/tachiyomi/network/JavaScriptEngine.kt \
  eu/kanade/tachiyomi/network/OkHttpExtensions.kt \
  eu/kanade/tachiyomi/network/ProgressListener.kt \
  eu/kanade/tachiyomi/network/ProgressResponseBody.kt \
  eu/kanade/tachiyomi/network/Requests.kt \
  eu/kanade/tachiyomi/network/interceptor/CloudflareInterceptor.kt \
  eu/kanade/tachiyomi/network/interceptor/RateLimitInterceptor.kt \
  eu/kanade/tachiyomi/network/interceptor/SpecificHostRateLimitInterceptor.kt \
  eu/kanade/tachiyomi/network/interceptor/UncaughtExceptionInterceptor.kt \
  eu/kanade/tachiyomi/network/interceptor/UserAgentInterceptor.kt \
  eu/kanade/tachiyomi/network/interceptor/WebViewInterceptor.kt \
  eu/kanade/tachiyomi/util/system/DeviceUtil.kt \
  eu/kanade/tachiyomi/util/system/WebViewUtil.kt \
  tachiyomi/core/common/util/lang/CoroutinesExtensions.kt \
  tachiyomi/core/common/util/lang/RxCoroutineBridge.kt \
  tachiyomi/core/common/util/system/LogcatExtensions.kt \
  mihon/core/common/extensions/JsonObject.kt; do
  fetch "$CC/$f" "$f"
done
