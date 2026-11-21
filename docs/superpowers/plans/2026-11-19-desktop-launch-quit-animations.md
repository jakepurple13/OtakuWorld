# Desktop Launch & Quit Animations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Per-app, user-selectable launch and quit animations for the three desktop apps, with Off and Fade, chosen from a new Desktop Settings page.

**Architecture:** Shared plumbing lives in `kmpuiviews/src/jvmMain/.../kmpuiviews/motion/`: the `DesktopMotion` contract, Off/Fade, the run clock, the host that layers a motion around app content, the launch/quit policy, the quit controller and transparent overlay window, the settings page, and a frame renderer used by contract tests. Each app's `:xworld:desktop` module holds only its own six animations and registers a `DesktopMotionCatalog` in Koin. Launch plays in the main window over live content; quit plays in a transparent overlay window over a picture of the content, so it ends on the wallpaper.

**Tech Stack:** Kotlin 2, Compose Multiplatform Desktop (AWT `application {}`), Nucleus 2.5.18 JNI `DecoratedWindow`, Koin + navigation3 DSL, DataStore (`DataStoreHandler`), `kotlin.test`, `androidx.compose.ui.ImageComposeScene`, `runComposeUiTest` (`commonLibs.cmp.ui.test`).

**Spec:** `docs/superpowers/specs/2026-11-19-desktop-launch-quit-animations-design.md` (timings source of truth: `docs/mockups/desktop-launch-quit-animations.html`, each concept's `run(s)` `seq()` frames).

## Global Constraints

- Package for shared code: `com.programmersbox.kmpuiviews.motion` (in `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/motion/`).
- Package for app code: `com.programmersbox.desktop.motion` in each `:xworld:desktop` `jvmMain`. No app's animations may be referenced from another app's module or from `kmpuiviews`.
- Settings keys (in `MangaDesktopSettings`): `launchAnimation`, `quitAnimation`; `stringPreferencesKey`, default `""`.
- Built-in keys: `"off"`, `"fade"`. App keys are kebab-case of the name (e.g. `"sushi-unroll"`).
- Fade: launch 450 ms, alpha 0→1, scale 0.985→1, `CubicBezierEasing(.2f,.8f,.2f,1f)`; quit 350 ms, alpha 1→0, scale 1→0.985, `CubicBezierEasing(.55f,0f,.9f,.35f)`.
- Overlay verified platforms: `setOf(OverlayPlatform.MacOS, OverlayPlatform.Windows)`.
- Mockup easings map to Compose: `E.o` = `CubicBezierEasing(.2f,.8f,.2f,1f)`, `E.io` = `(.65f,0f,.35f,1f)`, `E.i` = `(.55f,0f,.9f,.35f)`, `E.back` = `(.3f,1.55f,.5f,1f)`, `'linear'` = `LinearEasing`. `cqw` = percent of the host width; the mockup stage is 16:10.
- Copy: page title `Launch & quit animations`; group header `Motion`; row supporting text `Launch: <title> · Quit: <title>`; segmented labels `Launch`, `Quit`; list group headers `Basic` and the app name; preview button `Play again`.
- Every lazy item gets `Modifier.animateItem()`.
- Commits end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

- Close clicked again while a quit animation plays → app exits at once, no second overlay (Task 5 test `secondRequestExitsNow`).
- Close requested while the launch animation is still running → launch jumps to its end, then the quit runs (Task 5 test `quitDuringLaunchSkipsLaunch`).
- Quit from the Dock / ⌘Q while the window is minimized → no animation, immediate exit (Task 2 test `minimizedQuitIsOff`).
- Stored key that no longer exists or belongs to another app (`"crt-power-on"` in MangaWorld) → the app's default, no crash (Task 1 test `unknownKeyResolvesToDefault`).
- OS reduced motion on → Fade instead of the chosen animation, but Off stays Off (Task 2 tests `reducedMotionFades`, `reducedMotionKeepsOff`).

---

### Task 1: Motion contract, catalog, Off and Fade

**Files:**
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/motion/DesktopMotion.kt`
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/motion/BuiltInMotions.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/motion/DesktopMotionCatalogTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class MotionPhase { Launch, Quit }
  interface DesktopMotion {
      val key: String
      val title: String
      val durationMillis: Int
      /** Transform/clip/alpha applied to the app content for the frame at [time] ms. */
      fun Modifier.app(time: () -> Int): Modifier = this
      @Composable fun BoxScope.Behind(time: () -> Int) {}
      @Composable fun BoxScope.InFront(time: () -> Int) {}
  }
  object OffMotion : DesktopMotion            // key "off", title "Off", 0 ms
  object FadeInMotion : DesktopMotion         // key "fade", title "Fade", 450 ms
  object FadeOutMotion : DesktopMotion        // key "fade", title "Fade", 350 ms
  data class DesktopMotionCatalog(val appName: String, val launch: List<DesktopMotion>, val quit: List<DesktopMotion>)
  fun DesktopMotionCatalog.options(phase: MotionPhase): List<DesktopMotion>   // [Off, Fade(phase)] + own
  fun DesktopMotionCatalog.resolve(phase: MotionPhase, key: String): DesktopMotion  // ""/unknown → own first
  fun DesktopMotion.progress(time: Int, start: Int, end: Int, easing: Easing): Float // 0..1, clamped
  ```
  `progress` is the shared helper every animation uses to port a mockup segment `[start, end]`.

- [ ] **Step 1: Write the failing tests**

```kotlin
class DesktopMotionCatalogTest {
    private val a = fake("a-one"); private val b = fake("a-two")
    private val catalog = DesktopMotionCatalog("AWorld", launch = listOf(a, b), quit = listOf(b))

    @Test fun optionsAreOffFadeThenOwn() =
        assertEquals(listOf("off", "fade", "a-one", "a-two"), catalog.options(MotionPhase.Launch).map { it.key })
    @Test fun quitFadeIsFadeOut() = assertSame(FadeOutMotion, catalog.options(MotionPhase.Quit)[1])
    @Test fun emptyKeyResolvesToDefault() = assertSame(a, catalog.resolve(MotionPhase.Launch, ""))
    @Test fun unknownKeyResolvesToDefault() = assertSame(a, catalog.resolve(MotionPhase.Launch, "crt-power-on"))
    @Test fun offResolves() = assertSame(OffMotion, catalog.resolve(MotionPhase.Quit, "off"))
    @Test fun progressClampsAndEases() {
        assertEquals(0f, a.progress(50, 100, 200, LinearEasing))
        assertEquals(0.5f, a.progress(150, 100, 200, LinearEasing))
        assertEquals(1f, a.progress(900, 100, 200, LinearEasing))
    }
    private fun fake(k: String) = object : DesktopMotion { override val key = k; override val title = k; override val durationMillis = 1000 }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.motion.DesktopMotionCatalogTest"`
Expected: FAIL, unresolved `DesktopMotionCatalog`.

- [ ] **Step 3: Implement the files above.** Fade objects implement only `Modifier.app` with `graphicsLayer { alpha; scaleX; scaleY }` read from `time()` inside the layer block (no recomposition per frame).

- [ ] **Step 4: Run the test command; expected PASS.**

- [ ] **Step 5: Commit** `feat(desktop-motion): add DesktopMotion contract, catalog, Off and Fade`

---

### Task 2: Settings keys and launch/quit policy

**Files:**
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/Platform.jvm.kt` (`MangaDesktopSettings`, after `skippedUpdateVersion`)
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/motion/MotionPolicy.kt`
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/motion/ReducedMotion.kt` (port `systemReducesMotion()` from `/Users/jacobrein/Desktop/scrcpy_viewer/composeApp/src/jvmMain/kotlin/com/programmersbox/scrcpyviewer/ui/common/ReducedMotion.kt`)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/motion/MotionPolicyTest.kt`

**Interfaces:**
- Consumes: Task 1 types.
- Produces:
  ```kotlin
  // MangaDesktopSettings
  val launchAnimation: DataStoreHandler<String>  // key "launchAnimation", default ""
  val quitAnimation: DataStoreHandler<String>    // key "quitAnimation", default ""

  enum class OverlayPlatform { MacOS, Windows, LinuxX11, LinuxWayland; companion object { fun current(): OverlayPlatform } }
  val OverlayEnabled: Set<OverlayPlatform>
  enum class QuitHost { Overlay, InWindow }
  data class QuitPlan(val motion: DesktopMotion, val host: QuitHost)
  data class MotionConditions(val reduceMotion: Boolean, val fullscreen: Boolean = false, val minimized: Boolean = false,
                              val platform: OverlayPlatform, val enabled: Set<OverlayPlatform> = OverlayEnabled)
  fun launchMotion(chosen: DesktopMotion, c: MotionConditions): DesktopMotion
  fun quitPlan(chosen: DesktopMotion, c: MotionConditions): QuitPlan
  fun systemReducesMotion(): Boolean
  ```

- [ ] **Step 1: Write the failing tests**

```kotlin
class MotionPolicyTest {
    private val sushi = object : DesktopMotion { override val key = "sushi-unroll"; override val title = "Sushi"; override val durationMillis = 3200 }
    private val mac = MotionConditions(reduceMotion = false, platform = OverlayPlatform.MacOS)

    @Test fun launchPlaysChosen() = assertSame(sushi, launchMotion(sushi, mac))
    @Test fun reducedMotionFades() = assertSame(FadeInMotion, launchMotion(sushi, mac.copy(reduceMotion = true)))
    @Test fun reducedMotionKeepsOff() = assertSame(OffMotion, launchMotion(OffMotion, mac.copy(reduceMotion = true)))
    @Test fun quitUsesOverlayOnMac() = assertEquals(QuitPlan(sushi, QuitHost.Overlay), quitPlan(sushi, mac))
    @Test fun minimizedQuitIsOff() = assertEquals(OffMotion, quitPlan(sushi, mac.copy(minimized = true)).motion)
    @Test fun quitReducedMotionFades() = assertSame(FadeOutMotion, quitPlan(sushi, mac.copy(reduceMotion = true)).motion)
    @Test fun fullscreenQuitPlaysInWindow() = assertEquals(QuitHost.InWindow, quitPlan(sushi, mac.copy(fullscreen = true)).host)
    @Test fun linuxQuitPlaysInWindow() =
        assertEquals(QuitHost.InWindow, quitPlan(sushi, mac.copy(platform = OverlayPlatform.LinuxX11)).host)
}
```

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.motion.MotionPolicyTest"`; expected FAIL (unresolved).
- [ ] **Step 3: Implement.** Order in `quitPlan`: minimized → Off; reduced motion and chosen ≠ Off → FadeOut; fullscreen or platform ∉ enabled → InWindow; else Overlay. `OverlayPlatform.current()` copies Scrcpy's `XDG_SESSION_TYPE`/`WAYLAND_DISPLAY` check.
- [ ] **Step 4: Run; expected PASS.**
- [ ] **Step 5: Commit** `feat(desktop-motion): add settings keys and launch/quit policy`

---

### Task 3: Run clock, host and frame renderer

**Files:**
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/motion/MotionRun.kt`
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/motion/MotionHost.kt`
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/motion/MotionContract.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/motion/MotionRunTest.kt`, `.../MotionContractTest.kt`

**Interfaces:**
- Consumes: Task 1.
- Produces:
  ```kotlin
  class MotionRun(val motion: DesktopMotion) {
      val timeMillis: Int            // snapshot state
      val finished: Boolean          // snapshot state; true at once when durationMillis == 0
      fun skip()                     // timeMillis = durationMillis, finished = true
  }
  /** Advances [timeMillis] with withFrameNanos; after the last frame plus one, sets finished and calls onEnd once. */
  @Composable fun MotionRun.Play(onEnd: () -> Unit = {})

  /** Behind, then content wrapped in Modifier.app, then InFront. Content keeps the same slot whatever [motion] is. */
  @Composable fun MotionHost(motion: DesktopMotion, time: () -> Int, modifier: Modifier = Modifier, content: @Composable () -> Unit)

  /** Renders [motion] at [timeMillis] with a solid [appColor] app via ImageComposeScene (transparent background). */
  fun renderMotionFrame(motion: DesktopMotion, timeMillis: Int, width: Int = 320, height: Int = 200, appColor: Color = Color.Red): ImageBitmap

  object MotionContract {
      /** Problems with [motion] as a [phase] animation; empty when it obeys the contract. */
      fun violations(motion: DesktopMotion, phase: MotionPhase): List<String>
  }
  ```
  Contract, sampled at the centre and the four quarter points (x, y ∈ {¼, ¾}): Launch: at `durationMillis` every point is exactly `appColor`; at 0 the centre is not `appColor`. Quit: at 0 every point is `appColor`; at `durationMillis` every point has alpha 0. `OffMotion` is exempt from both (it has no frames).

- [ ] **Step 1: Write the failing tests**

```kotlin
class MotionRunTest {
    @Test fun skipFinishes() { val r = MotionRun(FadeInMotion); r.skip(); assertEquals(450, r.timeMillis); assertTrue(r.finished) }
    @Test fun offIsFinishedAtOnce() = assertTrue(MotionRun(OffMotion).finished)
    @OptIn(ExperimentalTestApi::class)
    @Test fun playEndsOnce() = runComposeUiTest {
        var ends = 0; val r = MotionRun(FadeInMotion)
        mainClock.autoAdvance = false
        setContent { r.Play { ends++ } }
        mainClock.advanceTimeBy(1_000)
        assertTrue(r.finished); assertEquals(1, ends)
    }
}
class MotionContractTest {
    @Test fun fadeObeysLaunch() = assertEquals(emptyList(), MotionContract.violations(FadeInMotion, MotionPhase.Launch))
    @Test fun fadeObeysQuit() = assertEquals(emptyList(), MotionContract.violations(FadeOutMotion, MotionPhase.Quit))
    @Test fun offObeysLaunch() = assertEquals(emptyList(), MotionContract.violations(OffMotion, MotionPhase.Launch))
    @Test fun fadeInIsNotAQuit() = assertTrue(MotionContract.violations(FadeInMotion, MotionPhase.Quit).isNotEmpty())
}
```
(Quit with Off at duration 0 cannot end transparent; `violations(OffMotion, Quit)` returns empty by rule: Off is exempt.)

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.motion.Motion*"`; expected FAIL.
- [ ] **Step 3: Implement.** `renderMotionFrame` uses `ImageComposeScene(width, height).apply { setContent { MotionHost(motion, { timeMillis }) { Box(Modifier.fillMaxSize().background(appColor)) } } }.render()` then `toComposeImageBitmap()`, and closes the scene.
- [ ] **Step 4: Run; expected PASS.**
- [ ] **Step 5: Commit** `feat(desktop-motion): add run clock, host and contract renderer`

---

### Task 4: Launch in the main window

**Files:**
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/DesktopUi.kt` (`InternalDesktopUi`, the `Box` around `HomeNav`; `OtakuDecoratedWindow` gets `state: WindowState` and `visible: Boolean = true` params passed to `DecoratedWindow`)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/motion/LaunchSkipTest.kt`

**Interfaces:**
- Consumes: `MotionRun`, `MotionHost`, `launchMotion`, `systemReducesMotion`, `MangaDesktopSettings.launchAnimation`, Koin `DesktopMotionCatalog` (use `getKoin().getOrNull<DesktopMotionCatalog>()`; null → Off, so an app without a catalog behaves as today).
- Produces:
  ```kotlin
  /** Ends [run] on the first pointer press or key press inside it. */
  fun Modifier.skipMotionOnInput(run: MotionRun): Modifier
  ```
  and a `windowState = rememberWindowState()` hoisted in `InternalDesktopUi`, plus `val launchRun: MotionRun` there, both used by Task 5.

- [ ] **Step 1: Write the failing test** `pressSkipsLaunch`: `runComposeUiTest`, `Box(Modifier.size(100.dp).skipMotionOnInput(run).testTag("host"))` with `run = MotionRun(FadeInMotion)`; `onNodeWithTag("host").performMouseInput { press() }`; assert `run.finished`. Second test `keySkipsLaunch` with `performKeyInput { pressKey(Key.A) }` on a focused host.
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.motion.LaunchSkipTest"`; expected FAIL.
- [ ] **Step 3: Implement** `skipMotionOnInput` (`pointerInput` observing `PointerEventPass.Initial` without consuming, and `onPreviewKeyEvent` returning false). In `InternalDesktopUi`: read the stored key once with `remember { runBlocking { settings.launchAnimation.get() } }` (a local file read before first frame), build `launchRun = remember { MotionRun(launchMotion(catalog.resolve(Launch, key), MotionConditions(systemReducesMotion(), platform = OverlayPlatform.current()))) }`, wrap the `HomeNav` box content in `MotionHost(if (launchRun.finished) OffMotion else launchRun.motion, { launchRun.timeMillis }, Modifier.skipMotionOnInput(launchRun))` and call `launchRun.Play()`.
- [ ] **Step 4: Run tests; expected PASS. Then** `./gradlew :mangaworld:desktop:run` and confirm the app opens normally (catalog not registered yet → Off).
- [ ] **Step 5: Commit** `feat(desktop-motion): play launch motion in the main window`

---

### Task 5: Quit controller

**Files:**
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/motion/QuitController.kt`
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/motion/QuitControllerTest.kt`

**Interfaces:**
- Consumes: `QuitPlan`, `MotionRun`.
- Produces:
  ```kotlin
  class QuitController(
      private val plan: () -> QuitPlan,           // evaluated at request time
      private val launch: () -> MotionRun?,
      private val exit: () -> Unit,
  ) {
      sealed interface State { data object Idle : State; data class Playing(val plan: QuitPlan, val run: MotionRun) : State; data object Exited : State }
      val state: State                            // snapshot state
      val hideMainWindow: Boolean                 // true from overlayFirstFrame() on
      fun requestQuit()
      fun overlayFirstFrame()
      fun finished()                              // run ended → exit()
  }
  ```

- [ ] **Step 1: Write the failing tests**

```kotlin
class QuitControllerTest {
    private var exits = 0
    private fun controller(motion: DesktopMotion, launch: MotionRun? = null) =
        QuitController({ QuitPlan(motion, QuitHost.Overlay) }, { launch }, { exits++ })

    @Test fun offExitsAtOnce() { controller(OffMotion).requestQuit(); assertEquals(1, exits) }
    @Test fun requestStartsPlaying() { val c = controller(FadeOutMotion); c.requestQuit(); assertIs<QuitController.State.Playing>(c.state); assertEquals(0, exits) }
    @Test fun secondRequestExitsNow() { val c = controller(FadeOutMotion); c.requestQuit(); c.requestQuit(); assertEquals(1, exits) }
    @Test fun finishedExitsOnce() { val c = controller(FadeOutMotion); c.requestQuit(); c.finished(); c.finished(); assertEquals(1, exits) }
    @Test fun quitDuringLaunchSkipsLaunch() { val l = MotionRun(FadeInMotion); controller(FadeOutMotion, l).requestQuit(); assertTrue(l.finished) }
    @Test fun hidesOnFirstFrame() { val c = controller(FadeOutMotion); c.requestQuit(); assertFalse(c.hideMainWindow); c.overlayFirstFrame(); assertTrue(c.hideMainWindow) }
}
```

- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.motion.QuitControllerTest"`; expected FAIL.
- [ ] **Step 3: Implement** (`exit` guarded so it runs at most once; `State.Exited` after it).
- [ ] **Step 4: Run; expected PASS.**
- [ ] **Step 5: Commit** `feat(desktop-motion): add quit controller`

---

### Task 6: Quit overlay window, picture and wiring

**Files:**
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/motion/WindowPicture.kt` (port `WindowPicture` + `Modifier.recordInto` from Scrcpy `LaunchOverlay.kt`)
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/motion/QuitOverlayWindow.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/DesktopUi.kt` (`InternalDesktopUi`)

**Interfaces:**
- Consumes: Tasks 2–5; `windowState`, `launchRun` from Task 4.
- Produces:
  ```kotlin
  class WindowPicture { suspend fun capture(): ImageBitmap? }
  fun Modifier.recordInto(picture: WindowPicture): Modifier
  /** Transparent, undecorated, non-focusable, always-on-top Window at [position]/[size]; draws a [titleBarHeight] band of
   *  [titleBarColor] above [picture], wrapped in MotionHost(run.motion). Calls onFirstFrame after its first frame, onEnd after run ends. */
  @Composable fun ApplicationScope.QuitOverlayWindow(position: WindowPosition, size: DpSize, picture: ImageBitmap?,
      titleBarHeight: Dp, titleBarColor: Color, run: MotionRun, onFirstFrame: () -> Unit, onEnd: () -> Unit)
  ```
  `ApplicationScope` comes from AWT `application {}`; thread it into `InternalDesktopUi` as a receiver (both `BaseDesktopUi` overloads already have a scope).

- [ ] **Step 1: Wire it.** In `InternalDesktopUi`: `val picture = remember { WindowPicture() }`; apply `Modifier.recordInto(picture)` to the `HomeNav` box; create `QuitController(plan = { quitPlan(catalog.resolve(Quit, runBlocking { settings.quitAnimation.get() }), MotionConditions(systemReducesMotion(), fullscreen = windowState.placement == WindowPlacement.Fullscreen, minimized = windowState.isMinimized, platform = OverlayPlatform.current())) }, launch = { launchRun }, exit = exitApplication)`; pass `onCloseRequest = quitController::requestQuit` and `visible = !quitController.hideMainWindow` to `OtakuDecoratedWindow`. On macOS register `Desktop.getDesktop().setQuitHandler { _, response -> response.cancelQuit(); quitController.requestQuit() }` once in a `DisposableEffect`. When `state` is `Playing` with `QuitHost.Overlay`: capture the picture (`LaunchedEffect`), then show `QuitOverlayWindow` (title bar band = window height − content height, color = the title bar's background from `rememberMaterialTitleBarStyle(chromeColorScheme)`); with `QuitHost.InWindow`: swap the host's motion to the quit run over the live content and call `finished()` from `Play`.
- [ ] **Step 2: Manual check on macOS** with Fade (temporarily set the stored key: `quitAnimation = "fade"` via the settings page in Task 7 or a debug edit): run `./gradlew :mangaworld:desktop:run`, close the window. Expected: the window fades over the wallpaper (not black/white), no second window flash, app exits; ⌘Q does the same; close twice quickly exits at once. Record result in the PR description. If `transparent = true` renders black on this Nucleus/AWT setup, stop and report before continuing.
- [ ] **Step 3: Run all motion tests** `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.motion.*"`; expected PASS.
- [ ] **Step 4: Commit** `feat(desktop-motion): play quit motion in a transparent overlay window`

---

### Task 7: Settings page and Motion row

**Files:**
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/motion/MotionSettingsScreen.kt`
- Create: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/motion/MotionSettingsGroup.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/DesktopUi.kt` (`desktopSetup` module: `navigation<MotionSettingsRoute> { MotionSettingsScreen() }`)
- Test: `kmpuiviews/src/jvmTest/kotlin/com/programmersbox/kmpuiviews/motion/MotionSettingsScreenTest.kt`

**Interfaces:**
- Consumes: catalog, settings keys, `MotionRun`, `MotionHost`.
- Produces:
  ```kotlin
  @Serializable data object MotionSettingsRoute : NavKey
  @Composable fun MotionSettingsScreen()
  /** Testable body: no Koin, no DataStore. */
  @Composable fun MotionSettingsContent(catalog: DesktopMotionCatalog, launchKey: String, quitKey: String,
      onLaunchKey: (String) -> Unit, onQuitKey: (String) -> Unit)
  /** "Motion" header + one SegmentedListItem row that navigates to MotionSettingsRoute. Returns nothing if no catalog in Koin. */
  fun LazyListScope.motionSettingsGroup()
  @Composable fun MotionPreview(motion: DesktopMotion, modifier: Modifier = Modifier)  // 16:10, stand-in app, Play again
  ```
  Layout follows the approved mockup: `LargeTopAppBar` + `BackButton()`, `SingleChoiceSegmentedButtonRow` (Launch/Quit), radio `SegmentedListItem`s grouped `Basic` / `catalog.appName`, supporting text `"%.1f s"` of duration (Off: `Opens straight to the app` / `Closes right away`; Fade: `0.45 s` / `0.35 s`), preview pane right (sticky) or on top below 640 dp. Hover (`Modifier.onPointerEvent(PointerEventType.Enter)`) previews a row after 180 ms; click selects and saves. Stand-in app: `surface` background, a `surfaceContainer` top bar, a rail and a grid of `primaryContainer` tiles.

- [ ] **Step 1: Write the failing tests** (`runComposeUiTest`, catalog of two fakes named "One"/"Two", appName "AWorld"):
  - `listsOnlyBasicAndOwn`: nodes with text `Off`, `Fade`, `One`, `Two`, `AWorld`, `Basic` exist; text `CRT power-on` does not.
  - `clickSavesLaunchKey`: click `Two` → `onLaunchKey` received `"two"`.
  - `quitTabSavesQuitKey`: click `Quit`, click `Off` → `onQuitKey` received `"off"`.
- [ ] **Step 2: Run** `./gradlew :kmpuiviews:jvmTest --tests "com.programmersbox.kmpuiviews.motion.MotionSettingsScreenTest"`; expected FAIL.
- [ ] **Step 3: Implement.** `motionSettingsGroup` row supporting text: `"Launch: ${launch.title} · Quit: ${quit.title}"`, icon `Icons.Default.Animation`, trailing `Icons.AutoMirrored.Filled.KeyboardArrowRight`, `onClick = { LocalNavActions.current.navigate(MotionSettingsRoute) }` (capture `LocalNavActions.current` in composition).
- [ ] **Step 4: Run; expected PASS.**
- [ ] **Step 5: Commit** `feat(desktop-motion): add Launch & quit animations settings page`

---

### Task 8: MangaWorld catalog + Sushi unroll / Sushi roll-up

**Files:**
- Create: `mangaworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/motion/SushiRoll.kt` (shared roll + chopsticks + rice sheet drawing)
- Create: `mangaworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/motion/SushiMotions.kt` (`SushiUnroll`, `SushiRollUp`)
- Create: `mangaworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/motion/MangaMotionCatalog.kt`
- Modify: `mangaworld/desktop/build.gradle.kts` (add `jvmTest.dependencies { implementation(commonLibs.kotlin.test) }`)
- Modify: `mangaworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt` (`single { mangaMotionCatalog() }` in `moduleBlock`)
- Modify: `mangaworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/PlatformSettings.kt` (`motionSettingsGroup()` before `updatesSettingsGroup()`)
- Test: `mangaworld/desktop/src/jvmTest/kotlin/com/programmersbox/desktop/motion/MangaMotionsTest.kt`

**Interfaces:**
- Consumes: Task 1 contract, `progress`, Task 3 `MotionContract`, Task 7 `motionSettingsGroup`.
- Produces: `fun mangaMotionCatalog(): DesktopMotionCatalog` with `appName = "MangaWorld"`, `launch = listOf(SushiUnroll, InkPanels, Impact)`, `quit = listOf(SushiRollUp, ScreentoneFade, TheEndPanel)` (Task 9 adds the last four; until then the lists hold only the sushi objects). Keys: `sushi-unroll`, `ink-panels`, `impact`, `sushi-roll-up`, `screentone-fade`, `the-end-panel`.

- [ ] **Step 1: Write the failing test**

```kotlin
class MangaMotionsTest {
    private val catalog = mangaMotionCatalog()
    @Test fun launchesObeyContract() = catalog.launch.forEach { assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Launch), it.key) }
    @Test fun quitsObeyContract() = catalog.quit.forEach { assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Quit), it.key) }
    @Test fun sushiIsDefault() { assertEquals("sushi-unroll", catalog.launch.first().key); assertEquals("sushi-roll-up", catalog.quit.first().key) }
    @Test fun durationsMatchMockup() { assertEquals(3200, SushiUnroll.durationMillis); assertEquals(3100, SushiRollUp.durationMillis) }
}
```

- [ ] **Step 2: Run** `./gradlew :mangaworld:desktop:jvmTest --tests "com.programmersbox.desktop.motion.MangaMotionsTest"`; expected FAIL.
- [ ] **Step 3: Implement** by porting the mockup's `Sushi unroll` and `Sushi roll-up` `run(s)` frames: board/rice sheet in `Behind`/`InFront` via `Canvas`, app reveal via `Modifier.app { drawWithContent { clipRect(...) } }` with the inset keyframes, roll drop/travel/rotation and chopstick open/lift as `graphicsLayer` transforms. Sizes in fractions of host width (`20cqw` roll = `0.2f * width`).
- [ ] **Step 4: Run; expected PASS. Then** `./gradlew :mangaworld:desktop:run`; Settings → Desktop Settings → Launch & quit animations; confirm the list shows Off, Fade, Sushi unroll / Sushi roll-up only, the preview plays, and relaunch/quit play the sushi.
- [ ] **Step 5: Commit** `feat(mangaworld-desktop): add sushi launch and quit motions`

### Task 9: MangaWorld Ink panels, Impact, Screentone fade, The End panel

**Files:**
- Create in `mangaworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/motion/`: `InkPanels.kt`, `Impact.kt`, `ScreentoneFade.kt`, `TheEndPanel.kt`
- Modify: `MangaMotionCatalog.kt` (full lists from Task 8)
- Test: extend `MangaMotionsTest` with `allSixPresent` (keys in order from Task 8) and durations `2600, 2200, 2400, 2300`.

Steps as Task 8: failing test → run (FAIL) → port each mockup `run(s)` (panel strokes via `PathMeasure.getSegment`, SFX text `ドン!` with `MaterialTheme` font fallback, halftone `Canvas` dots with radius keyframes, caption spring `E.back`) → run (PASS) → `:mangaworld:desktop:run` preview check → commit `feat(mangaworld-desktop): add remaining manga motions`.

### Task 10: AnimeWorld catalog + CRT power-on / CRT power-off

Same shape as Task 8 in `animeworld/desktop` (jvmTest deps already exist). Files `motion/CrtTv.kt`, `motion/CrtMotions.kt`, `motion/AnimeMotionCatalog.kt`; register `single { animeMotionCatalog() }` in `animeworld/desktop/.../main.kt`; `motionSettingsGroup()` in its `PlatformSettings.kt`. Catalog: `appName = "AnimeWorld"`, launch `CrtPowerOn, PlayButton, Eyecatch`, quit `CrtPowerOff, NextEpisode, IrisOut`; keys `crt-power-on`, `play-button`, `eyecatch`, `crt-power-off`, `next-episode`, `iris-out`. Test `AnimeMotionsTest` mirrors `MangaMotionsTest`; durations `2300`, `1500`. Static noise: one 96×96 `ImageBitmap` generated once, offset by a step function of time. Commit `feat(animeworld-desktop): add CRT launch and quit motions`.

### Task 11: AnimeWorld Play button, Eyecatch, Next episode, Iris out

As Task 9. Durations `1900, 1900, 2300, 1700`. Eyecatch title text uses `"ANIME"`/`"WORLD"`; Next episode copy `NEXT EPISODE` / `SEE YOU SOON`. Commit `feat(animeworld-desktop): add remaining anime motions`.

### Task 12: NovelWorld catalog + Book opening / Book closing

As Task 8 in `novelworld/desktop` (add `jvmTest` deps). Files `motion/Book.kt`, `motion/BookMotions.kt`, `motion/NovelMotionCatalog.kt`. Cover uses `graphicsLayer { rotationY; cameraDistance = 12f * density; transformOrigin = TransformOrigin(0f, .5f) }`, drawing the inside face once `|rotationY| > 90`. Catalog `appName = "NovelWorld"`, launch `BookOpening, Typewriter, InkDrop`, quit `BookClosing, TheEnd, LampOut`; keys `book-opening`, `typewriter`, `ink-drop`, `book-closing`, `the-end`, `lamp-out`. Durations `2400`, `2300`. Commit `feat(novelworld-desktop): add book launch and quit motions`.

### Task 13: NovelWorld Typewriter, Ink drop, The End, Lamp out

As Task 9. Durations `2600, 1900, 2400, 2200`. Typed text via substring length stepped per mockup `steps(n)`; Lamp out vignette via `Brush.radialGradient` with an animated stop. Commit `feat(novelworld-desktop): add remaining novel motions`.

### Task 14: Hand test and PR

- [ ] **Step 1:** `./gradlew :kmpuiviews:jvmTest :mangaworld:desktop:jvmTest :animeworld:desktop:jvmTest :novelworld:desktop:jvmTest`; expected all PASS.
- [ ] **Step 2:** On macOS run each app (`./gradlew :<app>:desktop:run`), pick every launch and quit option once, relaunch/quit. Check: quit ends on wallpaper, ⌘Q animates, minimized ⌘Q exits at once, click during launch skips it, full screen quit plays in-window, MangaWorld lists no anime/novel animations.
- [ ] **Step 3:** Push and open the PR against `develop`, listing what was hand-tested and that Windows and Linux are untested.
