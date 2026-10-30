#!/usr/bin/env bash
# Usage: rename-update-yml.sh <app> <yml-dir>
# <yml-dir> is the update-yml dir written by generate-update-yml; the release-assets-<app>-*
# dirs are its siblings. Renames latest*.yml to <app>*.yml. If a leg is missing or installer
# names collide across legs, deletes all ymls so no partial metadata is published.
set -euo pipefail

app="${1:?app required}"
dir="${2:?yml dir required}"
assets="$(dirname "$dir")"

drop() {
  echo "::warning::$1; not publishing update ymls for $app"
  rm -f "$dir"/*.yml
  exit 0
}

shopt -s nullglob
dirs=("$assets"/release-assets-"$app"-*/)
[ "${#dirs[@]}" -ge 4 ] || drop "expected 4 release-assets-$app-* dirs, found ${#dirs[@]}"

dups="$(for d in "${dirs[@]}"; do
  find "$d" -type f \( -name '*.dmg' -o -name '*.zip' -o -name '*.exe' -o -name '*.deb' \
    -o -name '*.AppImage' -o -name '*.blockmap' \) -exec basename {} \;
done | sort | uniq -d)"
[ -z "$dups" ] || drop "duplicate installer names across legs: $(echo "$dups" | tr '\n' ' ')"

mv "$dir/latest.yml" "$dir/$app.yml"
mv "$dir/latest-mac.yml" "$dir/$app-mac.yml"
mv "$dir/latest-linux.yml" "$dir/$app-linux.yml"
