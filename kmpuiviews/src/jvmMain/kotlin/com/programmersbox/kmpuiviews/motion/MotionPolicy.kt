package com.programmersbox.kmpuiviews.motion

enum class OverlayPlatform {
    MacOS, Windows, LinuxX11, LinuxWayland;

    companion object {
        fun current(
            env: (String) -> String? = System::getenv,
            os: String = System.getProperty("os.name").orEmpty(),
        ): OverlayPlatform = when {
            os.startsWith("Mac", ignoreCase = true) -> MacOS
            os.startsWith("Windows", ignoreCase = true) -> Windows
            env("XDG_SESSION_TYPE").equals("wayland", ignoreCase = true) || !env("WAYLAND_DISPLAY").isNullOrEmpty() -> LinuxWayland
            else -> LinuxX11
        }
    }
}

/** Platforms where the quit overlay has been verified; the others play quit in the window. */
val OverlayEnabled: Set<OverlayPlatform> = setOf(OverlayPlatform.MacOS, OverlayPlatform.Windows)

enum class QuitHost { Overlay, InWindow }

data class QuitPlan(val motion: DesktopMotion, val host: QuitHost)

data class MotionConditions(
    val reduceMotion: Boolean,
    val fullscreen: Boolean = false,
    val minimized: Boolean = false,
    val platform: OverlayPlatform,
    val enabled: Set<OverlayPlatform> = OverlayEnabled,
)

fun launchMotion(chosen: DesktopMotion, c: MotionConditions): DesktopMotion =
    if (c.reduceMotion && chosen !== OffMotion) FadeInMotion else chosen

fun quitPlan(chosen: DesktopMotion, c: MotionConditions): QuitPlan {
    if (c.minimized) return QuitPlan(OffMotion, QuitHost.InWindow)
    val motion = if (c.reduceMotion && chosen !== OffMotion) FadeOutMotion else chosen
    val host = if (c.fullscreen || c.platform !in c.enabled) QuitHost.InWindow else QuitHost.Overlay
    return QuitPlan(motion, host)
}
