# Desktop CI & Packaging (MangaWorld / AnimeWorld / NovelWorld) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give all 3 desktop apps (MangaWorld, AnimeWorld, NovelWorld) correct packaging (icons, distinct executable names, Dmg/Msi/Deb) and give AnimeWorld/NovelWorld their own CI desktop-build workflows mirroring MangaWorld's existing one.

**Architecture:** Pure config/asset change — no application source code changes. Icon binaries generated once and committed; 3 `build.gradle.kts` files get a `packageName` + icon-path edit; 2 new GitHub Actions workflow files are structural copies of the existing `mangaworld_desktop_build.yaml`; 2 CI dispatcher files (`build_check.yaml`, `push_check.yaml`) get one new job each.

**Tech Stack:** Kotlin/Gradle (Compose Multiplatform desktop packaging DSL), GitHub Actions YAML, macOS `sips`/`iconutil`, Python/Pillow (one-time local tool, not a project dependency).

## Global Constraints

- No unit tests (explicitly out of scope per spec).
- Do not modify `mangaworld_desktop_build.yaml`, `main_release.yml`, or `nightly_release.yaml`.
- Do not modify any desktop app source (`App.kt`, `main.kt`, `PlatformSettings.kt`, `GenericMangaDesktop.kt`, etc.) or shared/common modules.
- `packageName` values: `"MangaWorld"`, `"AnimeWorld"`, `"NovelWorld"` (exact, PascalCase).
- Icon destination convention: `<app>/desktop/icons/icon.icns`, `icon.ico`, `icon.png`.
- Icon source: `<app>/src/main/ic_launcher-playstore.png` (512×512, already exists for all 3 apps).

---

### Task 1: Generate icon assets for all 3 apps

**Files:**
- Create: `mangaworld/desktop/icons/icon.icns`, `mangaworld/desktop/icons/icon.ico`, `mangaworld/desktop/icons/icon.png`
- Create: `animeworld/desktop/icons/icon.icns`, `animeworld/desktop/icons/icon.ico`, `animeworld/desktop/icons/icon.png`
- Create: `novelworld/desktop/icons/icon.icns`, `novelworld/desktop/icons/icon.ico`, `novelworld/desktop/icons/icon.png`

**Interfaces:**
- Produces: the 9 icon files above, at the fixed paths Task 2/3/4 reference via `project.file("icons/icon.<ext>")`.

- [ ] **Step 1: Build `.icns` for all 3 apps using `sips` + `iconutil` (macOS-native, no install needed)**

Run from repo root:

```bash
for app in mangaworld animeworld novelworld; do
  src="$app/src/main/ic_launcher-playstore.png"
  iconset="/tmp/${app}-icon.iconset"
  mkdir -p "$iconset"
  for size in 16 32 64 128 256 512; do
    sips -z "$size" "$size" "$src" --out "$iconset/icon_${size}x${size}.png" >/dev/null
    dbl=$((size * 2))
    if [ "$dbl" -le 1024 ]; then
      sips -z "$dbl" "$dbl" "$src" --out "$iconset/icon_${size}x${size}@2x.png" >/dev/null
    fi
  done
  mkdir -p "$app/desktop/icons"
  iconutil -c icns "$iconset" -o "$app/desktop/icons/icon.icns"
  rm -rf "$iconset"
done
```

- [ ] **Step 2: Verify all 3 `.icns` files are valid**

Run: `file mangaworld/desktop/icons/icon.icns animeworld/desktop/icons/icon.icns novelworld/desktop/icons/icon.icns`

Expected: all 3 lines report `Mac OS X icon, ... "icns"` (non-zero size, no "empty" or error).

- [ ] **Step 3: Create a throwaway venv with Pillow for `.ico` generation**

```bash
python3 -m venv /tmp/icon-venv
/tmp/icon-venv/bin/pip install --quiet Pillow
```

- [ ] **Step 4: Generate `.ico` for all 3 apps**

```bash
for app in mangaworld animeworld novelworld; do
  /tmp/icon-venv/bin/python3 -c "
from PIL import Image
img = Image.open('$app/src/main/ic_launcher-playstore.png').convert('RGBA')
img.save('$app/desktop/icons/icon.ico', sizes=[(16,16),(32,32),(48,48),(64,64),(128,128),(256,256)])
"
done
```

- [ ] **Step 5: Verify all 3 `.ico` files are valid**

Run: `file mangaworld/desktop/icons/icon.ico animeworld/desktop/icons/icon.ico novelworld/desktop/icons/icon.ico`

Expected: all 3 lines report `MS Windows icon resource - ...`.

- [ ] **Step 6: Copy the playstore PNG as the Linux icon for all 3 apps**

```bash
for app in mangaworld animeworld novelworld; do
  cp "$app/src/main/ic_launcher-playstore.png" "$app/desktop/icons/icon.png"
done
```

- [ ] **Step 7: Verify all 3 `.png` files are 512×512**

Run: `file mangaworld/desktop/icons/icon.png animeworld/desktop/icons/icon.png novelworld/desktop/icons/icon.png`

Expected: all 3 lines report `PNG image data, 512 x 512, ...`.

- [ ] **Step 8: Clean up the throwaway venv**

```bash
rm -rf /tmp/icon-venv
```

- [ ] **Step 9: Commit**

```bash
git add mangaworld/desktop/icons animeworld/desktop/icons novelworld/desktop/icons
git commit -m "$(cat <<'EOF'
feat(desktop): add per-app icon assets for Windows/macOS/Linux packaging

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Wire icons + rename packageName in MangaWorld desktop build

**Files:**
- Modify: `mangaworld/desktop/build.gradle.kts:85-94`

**Interfaces:**
- Consumes: `mangaworld/desktop/icons/icon.{icns,ico,png}` from Task 1.

- [ ] **Step 1: Edit the `nativeDistributions` block**

Change:

```kotlin
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.programmersbox.desktop"
            packageVersion = "1.0.0"

            //com.apple.security.local-authentication
            /*macOS {
                entitlementsFile.set(project.file("entitlements.plist"))
            }*/
        }
```

To:

```kotlin
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MangaWorld"
            packageVersion = "1.0.0"

            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }
            macOS {
                iconFile.set(project.file("icons/icon.icns"))
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
            }

            //com.apple.security.local-authentication
            /*macOS {
                entitlementsFile.set(project.file("entitlements.plist"))
            }*/
        }
```

- [ ] **Step 2: Verify it builds and picks up the icon**

Run: `./gradlew :mangaworld:desktop:packageDistributionForCurrentOS`

Expected: `BUILD SUCCESSFUL`, and `mangaworld/desktop/build/compose/binaries/main/dmg/MangaWorld-1.0.0.dmg` exists (filename now uses `MangaWorld`, not `desktop`).

Run: `ls mangaworld/desktop/build/compose/binaries/main/dmg/`

- [ ] **Step 3: Commit**

```bash
git add mangaworld/desktop/build.gradle.kts
git commit -m "$(cat <<'EOF'
fix(mangaworld): set distinct packageName and wire packaging icons

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Wire icons + rename packageName in AnimeWorld desktop build

**Files:**
- Modify: `animeworld/desktop/build.gradle.kts:76-80`

**Interfaces:**
- Consumes: `animeworld/desktop/icons/icon.{icns,ico,png}` from Task 1.

- [ ] **Step 1: Edit the `nativeDistributions` block**

Change:

```kotlin
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.programmersbox.desktop"
            packageVersion = "1.0.0"
        }
```

To:

```kotlin
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "AnimeWorld"
            packageVersion = "1.0.0"

            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }
            macOS {
                iconFile.set(project.file("icons/icon.icns"))
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
```

- [ ] **Step 2: Verify it builds and picks up the icon**

Run: `./gradlew :animeworld:desktop:packageDistributionForCurrentOS`

Expected: `BUILD SUCCESSFUL`, and `animeworld/desktop/build/compose/binaries/main/dmg/AnimeWorld-1.0.0.dmg` exists.

Run: `ls animeworld/desktop/build/compose/binaries/main/dmg/`

- [ ] **Step 3: Commit**

```bash
git add animeworld/desktop/build.gradle.kts
git commit -m "$(cat <<'EOF'
fix(animeworld): set distinct packageName and wire packaging icons

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Wire icons + rename packageName in NovelWorld desktop build

**Files:**
- Modify: `novelworld/desktop/build.gradle.kts:76-80`

**Interfaces:**
- Consumes: `novelworld/desktop/icons/icon.{icns,ico,png}` from Task 1.

- [ ] **Step 1: Edit the `nativeDistributions` block**

Change:

```kotlin
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.programmersbox.desktop"
            packageVersion = "1.0.0"
        }
```

To:

```kotlin
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "NovelWorld"
            packageVersion = "1.0.0"

            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }
            macOS {
                iconFile.set(project.file("icons/icon.icns"))
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
```

- [ ] **Step 2: Verify it builds and picks up the icon**

Run: `./gradlew :novelworld:desktop:packageDistributionForCurrentOS`

Expected: `BUILD SUCCESSFUL`, and `novelworld/desktop/build/compose/binaries/main/dmg/NovelWorld-1.0.0.dmg` exists.

Run: `ls novelworld/desktop/build/compose/binaries/main/dmg/`

- [ ] **Step 3: Commit**

```bash
git add novelworld/desktop/build.gradle.kts
git commit -m "$(cat <<'EOF'
fix(novelworld): set distinct packageName and wire packaging icons

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: Create AnimeWorld desktop build workflow

**Files:**
- Create: `.github/workflows/animeworld_desktop_build.yaml`
- Reference (do not modify): `.github/workflows/mangaworld_desktop_build.yaml`

**Interfaces:**
- Produces: a reusable workflow `./.github/workflows/animeworld_desktop_build.yaml`, consumed by Task 7.

- [ ] **Step 1: Create the file**

```yaml
name: Compose Desktop Build

env:
  GITHUB_DEPLOY: 'false'

on:
  workflow_dispatch:
  workflow_call:
    inputs:
      IS_PRERELEASE:
        required: false
        type: boolean
        default: false

defaults:
  run:
    shell: bash

jobs:
  build:
    name: Build Package
    timeout-minutes: 15
    continue-on-error: true
    # if: github.event_name  == 'pull_request'

    runs-on: ${{ matrix.os }}
    strategy:
      fail-fast: false
      matrix:
        os: [ ubuntu-latest, macos-latest, windows-latest ]
        jdk: [ 18 ]

    steps:
      - name: Check out the source code
        uses: actions/checkout@v6

      - name: Setup JDK
        uses: actions/setup-java@v5
        with:
          distribution: adopt
          java-version: 18

      - name: Cache Gradle dependencies
        uses: actions/cache@v5
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-

      - name: Build with Gradle
        uses: gradle/gradle-build-action@v3
        with:
          gradle-version: current
          arguments: ":animeworld:desktop:packageDistributionForCurrentOS"

      - name: Upload ${{ matrix.os }} Build
        uses: actions/upload-artifact@v7
        with:
          name: ${{ matrix.os }}-build
          path: |
            animeworld/desktop/build/compose/binaries/main/dmg/*.dmg
            animeworld/desktop/build/compose/binaries/main/deb/*.deb
            animeworld/desktop/build/compose/binaries/main/msi/*.msi
```

- [ ] **Step 2: Verify the YAML is valid and matches mangaworld's structure**

```bash
python3 -c "
import yaml
a = yaml.safe_load(open('.github/workflows/mangaworld_desktop_build.yaml'))
b = yaml.safe_load(open('.github/workflows/animeworld_desktop_build.yaml'))
assert a.keys() == b.keys(), 'top-level keys differ'
assert a[True].keys() == b[True].keys(), 'on: keys differ'  # yaml parses bare 'on' as True
assert list(a['jobs']['build'].keys()) == list(b['jobs']['build'].keys()), 'job keys differ'
print('OK: structurally matches mangaworld_desktop_build.yaml')
"
```

Expected: `OK: structurally matches mangaworld_desktop_build.yaml` (no `AssertionError`).

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/animeworld_desktop_build.yaml
git commit -m "$(cat <<'EOF'
ci: add AnimeWorld desktop build workflow

Mirrors mangaworld_desktop_build.yaml exactly, swapping only the Gradle
module path and artifact output paths.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Create NovelWorld desktop build workflow

**Files:**
- Create: `.github/workflows/novelworld_desktop_build.yaml`
- Reference (do not modify): `.github/workflows/mangaworld_desktop_build.yaml`

**Interfaces:**
- Produces: a reusable workflow `./.github/workflows/novelworld_desktop_build.yaml`, consumed by Task 7.

- [ ] **Step 1: Create the file**

```yaml
name: Compose Desktop Build

env:
  GITHUB_DEPLOY: 'false'

on:
  workflow_dispatch:
  workflow_call:
    inputs:
      IS_PRERELEASE:
        required: false
        type: boolean
        default: false

defaults:
  run:
    shell: bash

jobs:
  build:
    name: Build Package
    timeout-minutes: 15
    continue-on-error: true
    # if: github.event_name  == 'pull_request'

    runs-on: ${{ matrix.os }}
    strategy:
      fail-fast: false
      matrix:
        os: [ ubuntu-latest, macos-latest, windows-latest ]
        jdk: [ 18 ]

    steps:
      - name: Check out the source code
        uses: actions/checkout@v6

      - name: Setup JDK
        uses: actions/setup-java@v5
        with:
          distribution: adopt
          java-version: 18

      - name: Cache Gradle dependencies
        uses: actions/cache@v5
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-

      - name: Build with Gradle
        uses: gradle/gradle-build-action@v3
        with:
          gradle-version: current
          arguments: ":novelworld:desktop:packageDistributionForCurrentOS"

      - name: Upload ${{ matrix.os }} Build
        uses: actions/upload-artifact@v7
        with:
          name: ${{ matrix.os }}-build
          path: |
            novelworld/desktop/build/compose/binaries/main/dmg/*.dmg
            novelworld/desktop/build/compose/binaries/main/deb/*.deb
            novelworld/desktop/build/compose/binaries/main/msi/*.msi
```

- [ ] **Step 2: Verify the YAML is valid and matches mangaworld's structure**

```bash
python3 -c "
import yaml
a = yaml.safe_load(open('.github/workflows/mangaworld_desktop_build.yaml'))
b = yaml.safe_load(open('.github/workflows/novelworld_desktop_build.yaml'))
assert a.keys() == b.keys(), 'top-level keys differ'
assert a[True].keys() == b[True].keys(), 'on: keys differ'
assert list(a['jobs']['build'].keys()) == list(b['jobs']['build'].keys()), 'job keys differ'
print('OK: structurally matches mangaworld_desktop_build.yaml')
"
```

Expected: `OK: structurally matches mangaworld_desktop_build.yaml` (no `AssertionError`).

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/novelworld_desktop_build.yaml
git commit -m "$(cat <<'EOF'
ci: add NovelWorld desktop build workflow

Mirrors mangaworld_desktop_build.yaml exactly, swapping only the Gradle
module path and artifact output paths.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: Wire the two new desktop jobs into the CI dispatcher workflows

**Files:**
- Modify: `.github/workflows/build_check.yaml:49-62`
- Modify: `.github/workflows/push_check.yaml:45-58`

**Interfaces:**
- Consumes: `.github/workflows/animeworld_desktop_build.yaml` (Task 5), `.github/workflows/novelworld_desktop_build.yaml` (Task 6).

- [ ] **Step 1: Edit `build_check.yaml` — add `animeworld_desktop` job after `animeworld`, and `novelworld_desktop` job after `novelworld`**

Change:

```yaml
  animeworld:
    uses: ./.github/workflows/animeworld_build.yaml
    if: github.event.inputs.build_animeworld == 'true' || github.event_name == 'pull_request'
    secrets: inherit # pass all secrets

  #  animeworldtv:
```

To:

```yaml
  animeworld:
    uses: ./.github/workflows/animeworld_build.yaml
    if: github.event.inputs.build_animeworld == 'true' || github.event_name == 'pull_request'
    secrets: inherit # pass all secrets

  animeworld_desktop:
    uses: ./.github/workflows/animeworld_desktop_build.yaml
    if: github.event.inputs.build_animeworld == 'true' || github.event_name == 'pull_request'
    secrets: inherit # pass all secrets

  #  animeworldtv:
```

And change:

```yaml
  novelworld:
    uses: ./.github/workflows/novelworld_build.yaml
    if: github.event.inputs.build_novelworld == 'true' || github.event_name == 'pull_request'
    secrets: inherit # pass all secrets

#  otakuworld:
```

To:

```yaml
  novelworld:
    uses: ./.github/workflows/novelworld_build.yaml
    if: github.event.inputs.build_novelworld == 'true' || github.event_name == 'pull_request'
    secrets: inherit # pass all secrets

  novelworld_desktop:
    uses: ./.github/workflows/novelworld_desktop_build.yaml
    if: github.event.inputs.build_novelworld == 'true' || github.event_name == 'pull_request'
    secrets: inherit # pass all secrets

#  otakuworld:
```

- [ ] **Step 2: Edit `push_check.yaml` — same additions, with `push` instead of `pull_request`**

Change:

```yaml
  animeworld:
    uses: ./.github/workflows/animeworld_build.yaml
    if: github.event.inputs.build_animeworld == 'true' || github.event_name == 'push'
    secrets: inherit # pass all secrets

  #  animeworldtv:
```

To:

```yaml
  animeworld:
    uses: ./.github/workflows/animeworld_build.yaml
    if: github.event.inputs.build_animeworld == 'true' || github.event_name == 'push'
    secrets: inherit # pass all secrets

  animeworld_desktop:
    uses: ./.github/workflows/animeworld_desktop_build.yaml
    if: github.event.inputs.build_animeworld == 'true' || github.event_name == 'push'
    secrets: inherit # pass all secrets

  #  animeworldtv:
```

And change:

```yaml
  novelworld:
    uses: ./.github/workflows/novelworld_build.yaml
    if: github.event.inputs.build_novelworld == 'true' || github.event_name == 'push'
    secrets: inherit # pass all secrets

#  otakuworld:
```

To:

```yaml
  novelworld:
    uses: ./.github/workflows/novelworld_build.yaml
    if: github.event.inputs.build_novelworld == 'true' || github.event_name == 'push'
    secrets: inherit # pass all secrets

  novelworld_desktop:
    uses: ./.github/workflows/novelworld_desktop_build.yaml
    if: github.event.inputs.build_novelworld == 'true' || github.event_name == 'push'
    secrets: inherit # pass all secrets

#  otakuworld:
```

- [ ] **Step 3: Verify both files are valid YAML and have the expected job keys**

```bash
python3 -c "
import yaml
for f in ['.github/workflows/build_check.yaml', '.github/workflows/push_check.yaml']:
    d = yaml.safe_load(open(f))
    jobs = set(d['jobs'].keys())
    required = {'mangaworld', 'mangaworld_desktop', 'animeworld', 'animeworld_desktop', 'novelworld', 'novelworld_desktop'}
    missing = required - jobs
    assert not missing, f'{f} missing jobs: {missing}'
    print(f, 'OK, jobs:', sorted(jobs))
"
```

Expected: prints `OK, jobs: [...]` for both files, listing all 6 required job names, no `AssertionError`.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/build_check.yaml .github/workflows/push_check.yaml
git commit -m "$(cat <<'EOF'
ci: wire animeworld/novelworld desktop builds into CI dispatchers

Adds animeworld_desktop and novelworld_desktop jobs to build_check.yaml
and push_check.yaml, matching the existing mangaworld_desktop job's
gating pattern.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```
