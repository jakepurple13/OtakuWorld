# Releasing OtakuWorld

How to cut a stable release (`vX.Y.Z`) of MangaWorld, AnimeWorld, and NovelWorld for Android and
Desktop.

## Overview

| Piece                                      | Where                                   | What it does                                                                                     |
|--------------------------------------------|-----------------------------------------|--------------------------------------------------------------------------------------------------|
| `otakuVersionName`                         | `buildSrc/src/main/kotlin/AppInfo.kt`   | App version name. The version code comes from `GITHUB_RUN_NUMBER + 5000` on CI.                  |
| `CHANGELOG.md`                             | repo root                               | Release notes, one `## [X.Y.Z] - YYYY-MM-DD` section per release.                                |
| `vX.Y.Z` tag                               | `develop`                               | Pushing it triggers `.github/workflows/main_release.yml`.                                        |
| `main_release.yml`                         | `.github/workflows/`                    | Builds the Android APKs and the Desktop installers, then creates the GitHub release.            |
| `update.json`                              | `master` branch                         | Drives the in-app update prompt on Android. Only `update_real_version` is used for the check.   |

The Desktop apps update themselves from the per-app update `.yml` files that the release workflow
uploads to the GitHub release. They need no manual step.

## Steps

### 1. Prepare the release on `develop`

1. Decide which merged PRs are in the release. Anything merged after the tag commit goes into the
   next release.
2. Set `otakuVersionName` in `buildSrc/src/main/kotlin/AppInfo.kt` to `X.Y.Z`.
3. In `CHANGELOG.md`, rename `[Unreleased]` to `[X.Y.Z] - YYYY-MM-DD`. Set the compare link to
   `vPREVIOUS...vX.Y.Z`. Check that every PR in the release is listed, and that no excluded PR is.
4. Open a PR with these changes (for example `chore(release): X.Y.Z`) and merge it into `develop`.

### 2. Tag and build

1. Pull `develop` and check that `HEAD` is the commit you want to release.
2. Create and push the tag. The tag must match `vX.Y.Z` exactly. Other tag names skip the Desktop
   builds (`.github/scripts/desktop-version.sh stable`).

   ```bash
   git checkout develop
   git pull
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```

3. Watch the **Release Workflow** run:

   ```bash
   gh run list --workflow main_release.yml --limit 1
   gh run watch <run-id>
   ```

   The Android jobs (`mangaworld`, `animeworld`, `novelworld`) must succeed or no release is
   created. Desktop jobs are `continue-on-error`, so a failed Desktop leg does not block the
   release. Check them anyway.

4. If a job fails for a flaky reason, rerun only the failed jobs:

   ```bash
   gh run rerun <run-id> --failed
   ```

### 3. Check the GitHub release

Open `https://github.com/jakepurple13/OtakuWorld/releases/tag/vX.Y.Z` and check that it has:

- The APKs for each app (`*-release.apk`, `*-no-firebase-release.apk`, and the no-cloud variants).
- Desktop installers for each app: ZIP/DMG (macOS arm64 and x64), NSIS `.exe` (Windows), AppImage
  and `.deb` (Linux).
- The per-app Desktop update `.yml` files.

Paste the `CHANGELOG.md` section for `X.Y.Z` into the release body if it is empty.

### 4. Turn on the in-app update prompt

`update.json` lives on `master`, not `develop`. Do this only after the release has its APKs. The
prompt sends users to `update_url`, so it must not point at a missing release.

1. Check out `master` and edit `update.json`:

   ```json
   "update_real_version": "X.Y.Z",
   "update_url": "https://github.com/jakepurple13/OtakuWorld/releases/download/vX.Y.Z/",
   ```

2. Commit (`Update update.json to vX.Y.Z`) and push to `master`.
3. Check the live file:

   ```bash
   curl -s https://raw.githubusercontent.com/jakepurple13/OtakuWorld/master/update.json
   ```

### 5. After the release

1. On `develop`, add a new `## [Unreleased]` section to the top of `CHANGELOG.md` with
   `vX.Y.Z` as the compare base.
2. Watch Crashlytics for new issues in `X.Y.Z` over the next few days.

## Checklist

Copy this into the release PR or an issue.

```markdown
### Release vX.Y.Z

**Prepare (`develop`)**
- [ ] Decide which PRs are in the release (and which go to the next one)
- [ ] `otakuVersionName` in `buildSrc/src/main/kotlin/AppInfo.kt` set to `X.Y.Z`
- [ ] `CHANGELOG.md`: `[Unreleased]` renamed to `[X.Y.Z] - YYYY-MM-DD`, compare link updated
- [ ] Every included PR is in the changelog; no excluded PR is
- [ ] Release PR merged into `develop`
- [ ] CI green on `develop`

**Tag and build**
- [ ] `develop` pulled; `HEAD` is the release commit
- [ ] Tag `vX.Y.Z` created and pushed
- [ ] Release Workflow: `mangaworld`, `animeworld`, `novelworld` jobs passed
- [ ] Release Workflow: Desktop jobs passed (or failures understood)
- [ ] `Release APK` job passed

**GitHub release**
- [ ] APKs attached for all three apps (full, no-firebase, no-cloud)
- [ ] Desktop installers attached for all three apps (macOS, Windows, Linux)
- [ ] Desktop update `.yml` files attached
- [ ] Release notes filled in from `CHANGELOG.md`

**In-app update (`master`)**
- [ ] `update.json`: `update_real_version` = `X.Y.Z`
- [ ] `update.json`: `update_url` points at `.../releases/download/vX.Y.Z/`
- [ ] Pushed to `master`
- [ ] `curl` of the raw `update.json` shows the new version

**After**
- [ ] New `[Unreleased]` section added to `CHANGELOG.md` on `develop`
- [ ] Crashlytics checked for new issues in `X.Y.Z`
```
