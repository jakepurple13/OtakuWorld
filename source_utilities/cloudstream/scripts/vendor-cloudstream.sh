#!/usr/bin/env bash
# Re-fetches the vendored CloudStream `library` module at a pinned commit.
# Usage: scripts/vendor-cloudstream.sh [sha]
# Overwrites src/androidMain/kotlin/com/lagradost; re-apply every edit marked "OTAKU:"
# (listed in VENDORED.md) afterwards.
set -euo pipefail
SHA="${1:-d9c55e2e5b020dde8e28c12a3e7802df962265ac}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/src/androidMain/kotlin"
HEADER="// Copied from recloudstream/cloudstream $SHA, GPL-3.0"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
curl -fsSL "https://codeload.github.com/recloudstream/cloudstream/tar.gz/$SHA" | tar xz -C "$TMP"
LIB="$TMP/cloudstream-$SHA/library/src"

# Not copied (see VENDORED.md "Dropped"): commonMain files that only hold `expect` declarations
# (the androidMain actuals replace them) and the NewPipe-backed YouTube extractor.
SKIP=" \
commonMain/kotlin/com/lagradost/api/ContextHelper.kt \
commonMain/kotlin/com/lagradost/api/Log.kt \
commonMain/kotlin/com/lagradost/cloudstream3/network/WebViewResolver.kt \
commonMain/kotlin/com/lagradost/cloudstream3/utils/SubtitleHelperPlatform.kt \
commonMain/kotlin/com/lagradost/cloudstream3/extractors/YoutubeExtractor.kt \
jvmCommonMain/kotlin/com/lagradost/cloudstream3/extractors/YoutubeExtractor.jvmCommon.kt \
"

rm -rf "$DEST/com/lagradost"
for set in commonMain jvmCommonMain androidMain; do
  for f in $(cd "$LIB" && find "$set/kotlin" -name '*.kt' -type f | sort); do
    case "$SKIP" in *" $f "*) continue ;; esac
    out="$DEST/${f#"$set"/kotlin/}"
    mkdir -p "$(dirname "$out")"
    { echo "$HEADER"; cat "$LIB/$f"; } > "$out"
  done
done
