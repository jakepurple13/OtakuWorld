#!/usr/bin/env bash
# Lists the types a CloudStream plugin (.cs3 = zip with classes.dex + manifest.json) references.
# Usage: dex-refs.sh <plugin.cs3> [type-prefix]
#   type-prefix defaults to "com/lagradost/"; pass "" to list every referenced type.
# Requires dexdump from the Android build-tools ($ANDROID_HOME/build-tools/*/dexdump).
set -euo pipefail

cs3="${1:?usage: dex-refs.sh <plugin.cs3> [type-prefix]}"
prefix="${2-com/lagradost/}"
sdk="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
dexdump="$(ls -d "$sdk"/build-tools/*/dexdump 2>/dev/null | sort -V | tail -n 1)"
[ -x "$dexdump" ] || { echo "dexdump not found under $sdk/build-tools" >&2; exit 1; }

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
unzip -q -o "$cs3" classes.dex -d "$work"

# Disassembly mentions every type the code touches (supertypes, fields, invokes, casts).
"$dexdump" -d "$work/classes.dex" \
  | grep -oE "L${prefix}[A-Za-z0-9_/\$]+;" \
  | sed -E 's/^L//; s/;$//; s#/#.#g' \
  | sort -u
