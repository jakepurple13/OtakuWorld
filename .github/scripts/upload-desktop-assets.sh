#!/usr/bin/env bash
# Usage: upload-desktop-assets.sh <tag> <artifacts-dir>
# Uploads desktop installers/blockmaps one at a time (3 tries each), then the update ymls last so a
# yml never points at an installer that is not uploaded yet. If any installer of an app fails, that
# app's ymls are skipped (app = file name up to the first '-'). Always exits 0: desktop problems must
# not block the Android release. Needs `gh` and GH_TOKEN.
set -uo pipefail

tag="${1:?tag required}"
dir="${2:?artifacts dir required}"

# find (sorted) instead of globstar so this also runs on bash 3.2
list() { find "$dir" -type f \( "$@" \) | sort; }

upload() {
  local f="$1" i
  for i in 1 2 3; do
    gh release upload "$tag" "$f" --clobber && return 0
    echo "upload of $f failed (try $i/3)" >&2
    sleep "${UPLOAD_RETRY_SLEEP:-5}"
  done
  return 1
}

failed_apps=" "
while IFS= read -r f; do
  if ! upload "$f"; then
    name="$(basename "$f")"
    echo "::warning::could not upload $name after 3 tries"
    failed_apps="$failed_apps${name%%-*} "
  fi
done < <(list -name '*.dmg' -o -name '*.zip' -o -name '*.exe' -o -name '*.deb' -o -name '*.AppImage' -o -name '*.blockmap')

while IFS= read -r f; do
  name="$(basename "$f")"
  app="${name%%-*}"
  app="${app%.yml}"
  case "$failed_apps" in
    *" $app "*)
      echo "::warning::skipping $name because an installer for $app failed to upload"
      continue
      ;;
  esac
  upload "$f" || echo "::warning::could not upload $name after 3 tries"
done < <(list -name '*.yml')
exit 0
