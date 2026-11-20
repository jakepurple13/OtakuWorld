# Desktop launch & quit animations

Date: 2026-11-19. Approved mockups: `docs/mockups/desktop-launch-quit-animations.html`
(https://claude.ai/artifact/6mWrjGDYA8dfjD5zun9iwV). The mockup's `seq()` keyframes are the source of truth for each
animation's timings, easings and shapes.

## What ships

Each desktop app (MangaWorld, AnimeWorld, NovelWorld) plays a launch animation when the main window opens and a quit
animation when it closes. The user picks one of each in a new settings page.

| App | Launch | Quit |
|---|---|---|
| MangaWorld | Sushi unroll (default), Ink panels, Impact | Sushi roll-up (default), Screentone fade, The End panel |
| AnimeWorld | CRT power-on (default), Play button, Eyecatch | CRT power-off (default), Next episode, Iris out |
| NovelWorld | Book opening (default), Typewriter, Ink drop | Book closing (default), The End, Lamp out |

Every app also offers **Off** and **Fade** for both. An app lists only its own three animations; animations never cross
between apps. Each app's code lives in its own `:xworld:desktop` module, so an app ships only its own animations.

## Behavior

- **Launch** plays inside the main window, over the live app content, on the first frames after the native `-splash:`
  card closes. Any mouse press or key press during it jumps to the end.
- **Quit** ends on the desktop wallpaper. On a close request (window close button, macOS ⌘Q / Quit menu) the app takes
  a picture of the window content, opens a short-lived transparent, undecorated, non-focusable, always-on-top window at
  the main window's bounds, hides the main window on the overlay's first frame, plays the animation over the picture,
  then calls `exitApplication()`.
- The picture covers only the content; the overlay draws the title bar as a band in the title bar's color above it.
- **Fade**: launch 450 ms fade-in with scale 0.985 → 1; quit 350 ms fade-out with scale 1 → 0.985 (mockup values).
- **Off**: the window appears and closes at once.
- Fallbacks:
  - Window minimized at quit → Off.
  - OS asks for reduced motion → Fade (Off stays Off).
  - macOS full screen, or a platform where the overlay is not verified (Linux) → the quit animation plays inside the
    main window over the live content, ending on the window background, then the app exits.
- A second close request during a quit exits at once. A close request during a launch ends the launch first.

## Settings

- Each app's Desktop Settings (`PlatformSettings.kt`) gets a **Motion** group with one row, **Launch & quit
  animations**, whose supporting text is `Launch: <name> · Quit: <name>`. It opens a new page.
- The page: Launch / Quit segmented button; a radio list grouped **Basic** (Off, Fade) then the app's name (its three);
  a preview pane that plays the hovered or selected option on a stand-in app layout, with a **Play again** button.
  One column with the preview on top below 640 dp wide.
- Stored per app (each app has its own data directory) in `MangaDesktopSettings` as two string keys,
  `launchAnimation` and `quitAnimation`. Empty or unknown values resolve to the app's default.

## Reference

Scrcpy Vwr (`/Users/jacobrein/Desktop/scrcpy_viewer`, `composeApp/.../ui/window/launch/`) ships the same overlay design on
Nucleus 2.5.18's Tao backend. OtakuWorld uses AWT `application {}` with the JNI `DecoratedWindow`, which has no
`transparent` option, so the overlay here is a plain Compose `Window(undecorated = true, transparent = true)`.
Port: `WindowPicture` / `recordInto`, the overlay frame loop, the launch/quit policy shape, `systemReducesMotion()`.
