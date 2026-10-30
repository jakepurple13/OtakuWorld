#!/usr/bin/env bash
# Plain-bash tests for desktop-version.sh and rename-update-yml.sh.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
FAILS=0

pass() { echo "PASS: $1"; }
fail() { echo "FAIL: $1"; FAILS=$((FAILS + 1)); }
assert_eq() { if [ "$2" = "$3" ]; then pass "$1"; else fail "$1 (expected '$2', got '$3')"; fi; }
assert_file() { if [ -f "$2" ]; then pass "$1"; else fail "$1 (missing $2)"; fi; }
assert_no_file() { if [ ! -e "$2" ]; then pass "$1"; else fail "$1 (unexpected $2)"; fi; }

# --- desktop-version.sh ---
APPINFO="$TMP/AppInfo.kt"
echo 'const val otakuVersionName = "33.0.0"' > "$APPINFO"

REPO="$TMP/repo"
mkdir -p "$REPO"
(
  cd "$REPO" || exit 1
  git init -q
  git -c user.name=t -c user.email=t@t commit -q --allow-empty -m init
  for t in v31.0.0 v32.1.2 v32.0.0_test nightly_2026_10_01; do git tag "$t"; done
)

out="$(cd "$REPO" && bash "$HERE/desktop-version.sh" nightly "$APPINFO" 202610301415)"
assert_eq "nightly base ahead of latest tag" "33.0.0-alpha.20261030.1415" "$out"

(cd "$REPO" && git tag v33.0.0)
out="$(cd "$REPO" && bash "$HERE/desktop-version.sh" nightly "$APPINFO" 202610301415)"
assert_eq "nightly bumps minor when base is released" "33.1.0-alpha.20261030.1415" "$out"

out="$(bash "$HERE/desktop-version.sh" stable v33.0.0)"
assert_eq "stable strips v" "33.0.0" "$out"

bash "$HERE/desktop-version.sh" stable v32.0.0_test >/dev/null 2>&1
assert_eq "stable rejects suffixed tag" "1" "$?"

# --- rename-update-yml.sh ---
make_assets() { # dir app n
  local d="$1" app="$2" n="$3" i=0 l
  local labels=(linux-amd64 windows-amd64 macos-arm64 macos-x64)
  mkdir -p "$d/update-yml"
  for l in "${labels[@]}"; do
    i=$((i + 1))
    [ "$i" -le "$n" ] || break
    mkdir -p "$d/release-assets-$app-$l"
    echo x > "$d/release-assets-$app-$l/$app-$l.bin"
  done
  for f in latest.yml latest-mac.yml latest-linux.yml; do echo "version: 1" > "$d/update-yml/$f"; done
}

D4="$TMP/four"; make_assets "$D4" mangaworld 4
bash "$HERE/rename-update-yml.sh" mangaworld "$D4/update-yml"
assert_file "rename: mangaworld.yml" "$D4/update-yml/mangaworld.yml"
assert_file "rename: mangaworld-mac.yml" "$D4/update-yml/mangaworld-mac.yml"
assert_file "rename: mangaworld-linux.yml" "$D4/update-yml/mangaworld-linux.yml"
assert_no_file "rename: no latest.yml" "$D4/update-yml/latest.yml"
assert_no_file "rename: no latest-mac.yml" "$D4/update-yml/latest-mac.yml"
assert_no_file "rename: no latest-linux.yml" "$D4/update-yml/latest-linux.yml"

D3="$TMP/three"; make_assets "$D3" mangaworld 3
bash "$HERE/rename-update-yml.sh" mangaworld "$D3/update-yml"
assert_eq "3 dirs: exit 0 and no ymls" "0" "$(find "$D3/update-yml" -name '*.yml' | wc -l | tr -d ' ')"

DD="$TMP/dup"; make_assets "$DD" mangaworld 4
echo y > "$DD/release-assets-mangaworld-macos-arm64/same.dmg"
echo y > "$DD/release-assets-mangaworld-macos-x64/same.dmg"
bash "$HERE/rename-update-yml.sh" mangaworld "$DD/update-yml"
assert_eq "duplicate basename: exit 0 and no ymls" "0" "$(find "$DD/update-yml" -name '*.yml' | wc -l | tr -d ' ')"

# realistic Nucleus layout: app/ distributable and package.json in each format dir, across two legs
DR="$TMP/real"; make_assets "$DR" mangaworld 4
for leg in macos-arm64 macos-x64; do
  b="$DR/release-assets-mangaworld-$leg"
  mkdir -p "$b/dmg" "$b/zip" "$b/app/MangaWorld.app/Contents/MacOS" "$b/app/MangaWorld.app/Contents/Resources"
  echo '{}' > "$b/dmg/package.json"; echo '{}' > "$b/zip/package.json"
  echo '{}' > "$b/dmg/packaging-metadata.json"; echo '{}' > "$b/zip/packaging-metadata.json"
  echo p > "$b/app/MangaWorld.app/Contents/Info.plist"
  echo l > "$b/app/MangaWorld.app/Contents/MacOS/libjli.dylib"
  echo m > "$b/dmg/mangaworld-33.0.0-mac-$leg.dmg"
  echo b > "$b/dmg/mangaworld-33.0.0-mac-$leg.dmg.blockmap"
done
bash "$HERE/rename-update-yml.sh" mangaworld "$DR/update-yml"
assert_file "realistic layout: mangaworld-mac.yml survives" "$DR/update-yml/mangaworld-mac.yml"
assert_file "realistic layout: mangaworld.yml survives" "$DR/update-yml/mangaworld.yml"

# --- upload-desktop-assets.sh ---
FAKEBIN="$TMP/fakebin"; mkdir -p "$FAKEBIN"
cat > "$FAKEBIN/gh" <<'GH'
#!/usr/bin/env bash
# fake gh: logs "<file basename>" per attempt; fails when the basename is in $FAIL_FILE
f="$(basename "$4")"
echo "$f" >> "$UPLOAD_LOG"
[ "$f" = "${FAIL_FILE:-}" ] && exit 1
exit 0
GH
chmod +x "$FAKEBIN/gh"
UP="$TMP/up"; mkdir -p "$UP/a" "$UP/b"
for f in mangaworld-1-mac.dmg mangaworld-1-mac.dmg.blockmap mangaworld-mac.yml animeworld-1.zip animeworld.yml; do echo x > "$UP/a/$f"; done
echo x > "$UP/b/mangaworld-1-linux.deb"; echo x > "$UP/b/mangaworld-linux.yml"
export UPLOAD_RETRY_SLEEP=0

export UPLOAD_LOG="$TMP/up1.log"; : > "$UPLOAD_LOG"
PATH="$FAKEBIN:$PATH" bash "$HERE/upload-desktop-assets.sh" tag "$UP" >/dev/null
assert_eq "upload: all 7 files uploaded once" "7" "$(wc -l < "$UPLOAD_LOG" | tr -d ' ')"
assert_eq "upload: ymls last" "1" "$(awk '/\.yml$/ {y=1; next} y {bad=1} END {print bad ? 0 : 1}' "$UPLOAD_LOG")"

export UPLOAD_LOG="$TMP/up2.log"; : > "$UPLOAD_LOG"
FAIL_FILE=mangaworld-1-mac.dmg PATH="$FAKEBIN:$PATH" bash "$HERE/upload-desktop-assets.sh" tag "$UP" >/dev/null
assert_eq "upload: failing installer retried 3 times" "3" "$(grep -c '^mangaworld-1-mac.dmg$' "$UPLOAD_LOG")"
assert_eq "upload: failed app ymls skipped" "0" "$(grep -c '^mangaworld.*\.yml$' "$UPLOAD_LOG")"
assert_eq "upload: other app yml still uploaded" "1" "$(grep -c '^animeworld.yml$' "$UPLOAD_LOG")"
FAIL_FILE=mangaworld-1-mac.dmg PATH="$FAKEBIN:$PATH" bash "$HERE/upload-desktop-assets.sh" tag "$UP" >/dev/null
assert_eq "upload: exits 0 even on failure" "0" "$?"

if [ "$FAILS" -eq 0 ]; then echo "ALL PASSED"; else echo "$FAILS FAILED"; exit 1; fi
