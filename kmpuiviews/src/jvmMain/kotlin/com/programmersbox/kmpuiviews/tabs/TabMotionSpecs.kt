package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import kotlin.math.roundToInt

/** The tab shell's motions. Each has one duration and easing; see [tabMotionMs] and [tabMotionEasing]. */
enum class TabMotion {
    /** A cover flying from where it was clicked into a background tab's icon. */
    Background,

    /** A cover growing from where it was clicked to fill the content of a foreground tab. */
    Grow,

    /** Crossfade between tabs. */
    Switch,

    /** A closing tab's collapse. */
    Close,

    /** The accent ring on an already-open tab. */
    Flash,

    /** A new tab's slot opening in the strip. */
    SlotOpen,
}

val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

const val ReducedMotionMs = 150

/** [scale] is the debug `TAB_MOTION_SCALE`. Reduced motion turns every motion into a 150 ms crossfade. */
fun tabMotionMs(motion: TabMotion, scale: Float, reduced: Boolean): Int {
    val base = if (reduced) ReducedMotionMs else when (motion) {
        TabMotion.Background -> 520
        TabMotion.Grow -> 450
        TabMotion.Switch -> 120
        TabMotion.Close -> 180
        TabMotion.Flash -> 300
        TabMotion.SlotOpen -> 220
    }
    return (base * scale).roundToInt()
}

fun tabMotionEasing(motion: TabMotion, reduced: Boolean): Easing =
    if (reduced) LinearEasing else when (motion) {
        TabMotion.Background, TabMotion.Grow -> EmphasizedDecelerate
        TabMotion.Switch -> LinearEasing
        TabMotion.Close -> EmphasizedAccelerate
        TabMotion.Flash, TabMotion.SlotOpen -> StandardEasing
    }

/** Reads `TAB_MOTION_SCALE`: a positive number, else 1. */
fun parseMotionScale(raw: String?): Float =
    raw?.trim()?.toFloatOrNull()?.takeIf { it > 0f } ?: 1f

/** The OS command that reports the reduce-motion setting, or null where there is none to read. */
private fun reduceMotionCommand(osName: String): List<String>? = when {
    osName.contains("mac", ignoreCase = true) ->
        listOf("defaults", "read", "-g", "com.apple.universalaccess", "reduceMotion")

    osName.contains("linux", ignoreCase = true) ->
        listOf("gsettings", "get", "org.gnome.desktop.interface", "enable-animations")

    else -> null
}

/** macOS prints `1` when reduce motion is on; GNOME prints `false` when animations are off. Windows is not read. */
fun parseReduceMotion(osName: String, commandOutput: String): Boolean {
    val out = commandOutput.trim()
    return when {
        osName.contains("mac", ignoreCase = true) -> out == "1"
        osName.contains("linux", ignoreCase = true) -> out == "false"
        else -> false
    }
}

/** Blocks on a short OS command: call it off the main thread. */
fun reducedMotion(): Boolean {
    val os = System.getProperty("os.name").orEmpty()
    val command = reduceMotionCommand(os) ?: return false
    return runCatching {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return@runCatching false
        }
        parseReduceMotion(os, process.inputStream.bufferedReader().readText())
    }.getOrDefault(false)
}
