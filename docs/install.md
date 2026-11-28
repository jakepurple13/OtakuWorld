# Download & install

Go to the [latest release](https://github.com/jakepurple13/OtakuWorld/releases/latest) and open
**Assets** at the bottom. There are a lot of files. You only need **one** per app.

In the names below, `<app>` is `mangaworld`, `animeworld`, or `novelworld`, and `<version>` is
the version number, such as `33.1.0`.

## Android phones and tablets (APK files)

| File                               | Pick this if…                                                                                                                 |
|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `<app>-release.apk`                | You're not sure. This is the normal version with every feature. It can send anonymous crash reports and usage stats, which you can turn off in Settings › Privacy & Security. |
| `<app>-no-firebase-release.apk`    | You want an app that never sends anything to Google. It's missing a few features (see below). |

**What the no-firebase version can't do**

| Feature                                   | `-release` | `-no-firebase-release` |
|-------------------------------------------|------------|------------------------|
| Translate descriptions and titles         | ✅          | ❌                      |
| Download offline translation languages    | ✅          | ❌                      |
| AI TL;DR of descriptions in other languages | ✅ (translated first) | Summarized as-is |
| Anonymous crash reports and usage stats   | ✅ (can turn off) | Never sent        |

Everything else works the same in both, including extensions, downloads, cloud sync, casting, and
the on-device AI features.

The two versions install as **separate apps** with separate data. Pick one and stick with it, or
you'll end up with two copies that don't share favorites.

**How to install an APK**

1. Download the `.apk` file on your phone.
2. Open it from your notifications or your Downloads folder.
3. If Android says it blocked the install, tap **Settings** and turn on **Allow from this
   source** for your browser or file manager. Go back and tap **Install**.
4. To update later, install the newer APK over the old one. Your data stays. The app also tells
   you when a new version is out.

## Desktop computers

| Your computer                                     | Download                                     |
|---------------------------------------------------|----------------------------------------------|
| **Mac with Apple Silicon** (M1, M2, M3, M4 or newer) | `<app>-<version>-mac-arm64.dmg`              |
| **Mac with an Intel processor** (older Macs)      | `<app>-<version>-mac-x64.dmg`                |
| **Windows**                                       | `<app>-<version>-win-x64-nsis.exe`           |
| **Linux** (any distribution)                      | `<app>-<version>-linux-x86_64.AppImage`      |
| **Linux** (Debian, Ubuntu, Mint, Pop!_OS)         | `<app>-<version>-linux-amd64.deb`            |

Not sure which Mac you have? Click the Apple menu › **About This Mac**. If it says **Chip: Apple
M…**, pick `arm64`. If it says **Processor: Intel**, pick `x64`.

**What each file type is**

| Ending        | What it is                                                                                     | Download it? |
|---------------|------------------------------------------------------------------------------------------------|--------------|
| `.dmg`        | Mac disk image. Open it and drag the app into **Applications**.                               | Yes (Mac)    |
| `.zip` (mac)  | The same Mac app in a zip. Same as the `.dmg`, just packed differently.                       | Optional     |
| `.exe`        | Windows installer. Double-click and follow the steps.                                         | Yes (Windows)|
| `.AppImage`   | A Linux app in one file. No install needed; make it executable and run it.                    | Yes (Linux)  |
| `.deb`        | Linux installer package for Debian-based systems.                                             | Yes (Linux)  |
| `.blockmap`   | Used by the app's built-in updater to download only what changed.                            | **No**       |
| `.yml`        | Version info read by the app's built-in updater.                                              | **No**       |

**First launch: "unidentified developer" warnings**

The desktop apps aren't signed yet, so your computer warns you the first time. This is expected.

- **macOS:** if it says the app *can't be opened* or *is damaged*, open **System Settings ›
  Privacy & Security**, scroll down, and click **Open Anyway** next to the app's name. If that
  button doesn't appear, run this in Terminal (replace the name with your app):
  `xattr -dr com.apple.quarantine /Applications/MangaWorld.app`
- **Windows:** if SmartScreen says *Windows protected your PC*, click **More info**, then
  **Run anyway**.
- **Linux (AppImage):** right-click the file › Properties › Permissions › **Allow executing as
  program**, or run `chmod +x <file>.AppImage`.

## Stable or Nightly?

- **Stable** ([latest release](https://github.com/jakepurple13/OtakuWorld/releases/latest)):
  tested versions. Pick this.
- **Nightly** ([nightly builds](https://github.com/jakepurple13/OtakuWorld/releases)): built
  every day from the newest code. You get features first, and bugs first too.

On desktop you can switch between them in **Settings › Updates**.
