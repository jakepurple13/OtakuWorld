# Desktop CI & Packaging (MangaWorld / AnimeWorld / NovelWorld) — Design

## Goal

Configure the 3 Compose Multiplatform desktop apps for proper cross-platform packaging
(icons, executable naming, Windows/macOS/Linux installers), and give AnimeWorld and
NovelWorld their own desktop CI workflows, mirroring MangaWorld's existing one exactly.

## Current state (as of investigation)

- All 3 desktop modules already exist and build (`:mangaworld:desktop`,
  `:animeworld:desktop`, `:novelworld:desktop`).
- All 3 `build.gradle.kts` files have `targetFormats(Dmg, Msi, Deb)` already set, but share
  the **same** `packageName = "com.programmersbox.desktop"` and have **no icons configured**.
- Icon source: each app has a 512×512 `<app>/src/main/ic_launcher-playstore.png` — the best
  available source for conversion (higher-res than any mipmap density).
- The real reference workflow is `.github/workflows/mangaworld_desktop_build.yaml`
  (not the guessed `mangaworld-desktop.yml`). It is `workflow_call`/`workflow_dispatch` only,
  matrix over `[ubuntu-latest, macos-latest, windows-latest]`, JDK 18, runs
  `:mangaworld:desktop:packageDistributionForCurrentOS`, and uploads **workflow artifacts
  only** (`actions/upload-artifact`) — no GitHub Release asset upload.
- It's wired into `build_check.yaml` and `push_check.yaml` (CI verification on push/PR) as a
  `mangaworld_desktop` job. It's commented-out (unused) in `nightly_release.yaml`, and **not**
  referenced at all in `main_release.yml` (the actual release workflow).
- Confirmed with user: keep this behavior as-is. No GitHub Release asset upload for any of
  the 3 desktop apps — `main_release.yml` / `nightly_release.yaml` are untouched.

## Scope decisions (confirmed with user)

- **Release wiring**: mirror mangaworld exactly — artifact-only upload via
  `actions/upload-artifact`, wired into `build_check.yaml` / `push_check.yaml` only. No
  changes to `main_release.yml` or `nightly_release.yaml`.
- **packageName**: change on all 3 modules (including mangaworld's `build.gradle.kts` — this
  is a Gradle file change, not a change to mangaworld's *workflow*, so it's in scope) from the
  shared `"com.programmersbox.desktop"` to `"MangaWorld"` / `"AnimeWorld"` / `"NovelWorld"`.
- **Icon conversion tooling**: one-time local generation (macOS `sips` + `iconutil` for
  `.icns`; a throwaway Python venv with Pillow for `.ico`), binaries committed to the repo. No
  conversion script or Gradle task added — nothing left to invoke it after this task, so a
  script would be dead weight.

## Icons

Source: `<app>/src/main/ic_launcher-playstore.png` (512×512, per app — mangaworld,
animeworld, novelworld all have one).

Generate into `<app>/desktop/icons/`:

- `icon.icns` — macOS. Built via `sips` (resize to 16/32/128/256/512 + @2x variants into an
  `.iconset` folder) then `iconutil -c icns`.
- `icon.ico` — Windows. Built via Pillow (`Image.save(..., format="ICO", sizes=[(16,16),
  (32,32), (48,48), (64,64), (128,128), (256,256)])`) in a scratch venv, not added as a
  project/Gradle dependency.
- `icon.png` — Linux. Plain 512×512 copy of the source PNG.

## Gradle changes (all 3 `desktop/build.gradle.kts`)

In `compose.desktop.application.nativeDistributions`:

```kotlin
nativeDistributions {
    targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb) // unchanged
    packageName = "MangaWorld" // / "AnimeWorld" / "NovelWorld"
    packageVersion = "1.0.0" // unchanged

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

No other block in these files changes (mangaworld keeps its commented-out entitlements
block, koog dependency, etc. — untouched).

## Workflows

### New files

- `.github/workflows/animeworld_desktop_build.yaml`
- `.github/workflows/novelworld_desktop_build.yaml`

Both are exact structural copies of `mangaworld_desktop_build.yaml` (same `name`, `env`,
triggers, `defaults`, job name/timeout/matrix/JDK version, cache step, gradle-build-action
step), with only these swapped:

- Gradle task: `:animeworld:desktop:packageDistributionForCurrentOS` /
  `:novelworld:desktop:packageDistributionForCurrentOS`
- Artifact paths: `animeworld/desktop/build/compose/binaries/main/{dmg,deb,msi}/*` /
  `novelworld/desktop/build/compose/binaries/main/{dmg,deb,msi}/*`

### CI wiring

Add matching jobs to `build_check.yaml` and `push_check.yaml`, next to the existing
`mangaworld_desktop` job:

```yaml
animeworld_desktop:
  uses: ./.github/workflows/animeworld_desktop_build.yaml

novelworld_desktop:
  uses: ./.github/workflows/novelworld_desktop_build.yaml
```

`mangaworld_desktop_build.yaml` itself is not modified. `main_release.yml` and
`nightly_release.yaml` are not modified.

## Out of scope

- GitHub Release asset upload for any of the 3 desktop apps.
- Mobile (Android/iOS) CI/CD.
- Code signing / notarization.
- Auto-update mechanisms.
- Publishing beyond GitHub Actions artifacts.
- Changes to shared/common module code, desktop app source (`main.kt`, `App.kt`, etc.), or
  `main_release.yml` / `nightly_release.yaml`.
- Unit tests.
- README updates.
