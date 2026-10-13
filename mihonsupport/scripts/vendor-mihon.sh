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
