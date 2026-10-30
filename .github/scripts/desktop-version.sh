#!/usr/bin/env bash
# Usage:
#   desktop-version.sh nightly <appinfo-path> <now-UTC-YYYYMMDDHHMM>   (run inside the git repo)
#   desktop-version.sh stable <tag>
set -euo pipefail

mode="${1:-}"
case "$mode" in
  nightly)
    appinfo="${2:?appinfo path required}"
    now="${3:?timestamp YYYYMMDDHHMM required}"
    [[ "$now" =~ ^[0-9]{12}$ ]] || { echo "bad timestamp: $now" >&2; exit 1; }

    base="$(sed -nE 's/.*otakuVersionName *= *"([0-9]+\.[0-9]+\.[0-9]+)".*/\1/p' "$appinfo" | head -1)"
    [ -n "$base" ] || { echo "otakuVersionName not found in $appinfo" >&2; exit 1; }

    latest="$(git tag -l 'v*' | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -1 || true)"
    latest="${latest#v}"

    # If the base version is already released (tag >= base), move to the next minor.
    if [ -n "$latest" ] && [ "$(printf '%s\n%s\n' "$base" "$latest" | sort -V | tail -1)" = "$latest" ]; then
      IFS=. read -r major minor _ <<< "$latest"
      base="$major.$((minor + 1)).0"
    fi

    echo "${base}-alpha.${now:0:8}.${now:8:4}"
    ;;
  stable)
    tag="${2:?tag required}"
    if [[ ! "$tag" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
      echo "not a stable tag: $tag" >&2
      exit 1
    fi
    echo "${tag#v}"
    ;;
  *)
    echo "usage: $0 nightly <appinfo> <YYYYMMDDHHMM> | stable <tag>" >&2
    exit 2
    ;;
esac
