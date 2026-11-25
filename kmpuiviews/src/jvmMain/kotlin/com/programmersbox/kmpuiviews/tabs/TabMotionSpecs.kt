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

    /** Tabs gliding between the top strip and the side column when the tab position changes. */
    Layout,
}

/** How much tab motion plays, from [TabMotionMode] and the OS setting; see [tabMotionLevel]. */
enum class TabMotionLevel { Full, Reduced, Off }

/** [TabMotionMode.System] follows the OS reduce-motion setting; the other modes ignore it. */
fun tabMotionLevel(mode: TabMotionMode, systemReduces: Boolean): TabMotionLevel = when (mode) {
    TabMotionMode.System -> if (systemReduces) TabMotionLevel.Reduced else TabMotionLevel.Full
    TabMotionMode.Full -> TabMotionLevel.Full
    TabMotionMode.Reduced -> TabMotionLevel.Reduced
    TabMotionMode.Off -> TabMotionLevel.Off
}

val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

const val ReducedMotionMs = 150

/**
 * [scale] is the debug `TAB_MOTION_SCALE`. Reduced turns every motion into a 150 ms crossfade, and Off makes
 * every motion instant.
 */
fun tabMotionMs(motion: TabMotion, scale: Float, level: TabMotionLevel): Int {
    val base = when (level) {
        TabMotionLevel.Off -> return 0
        TabMotionLevel.Reduced -> ReducedMotionMs
        TabMotionLevel.Full -> when (motion) {
            TabMotion.Background -> 520
            TabMotion.Grow -> 450
            TabMotion.Switch -> 120
            TabMotion.Close -> 180
            TabMotion.Flash -> 300
            TabMotion.SlotOpen -> 220
            TabMotion.Layout -> 450
        }
    }
    return (base * scale).roundToInt()
}

fun tabMotionEasing(motion: TabMotion, level: TabMotionLevel): Easing =
    if (level != TabMotionLevel.Full) LinearEasing else when (motion) {
        TabMotion.Background, TabMotion.Grow, TabMotion.Layout -> EmphasizedDecelerate
        TabMotion.Switch -> LinearEasing
        TabMotion.Close -> EmphasizedAccelerate
        TabMotion.Flash, TabMotion.SlotOpen -> StandardEasing
    }

/** The scale applies only when the JVM property `otaku.tabMotionDebug` is "true"; otherwise it is 1. */
fun tabMotionScale(debugFlag: String?, raw: String?): Float =
    if (debugFlag == "true") parseMotionScale(raw) else 1f

/** Reads `TAB_MOTION_SCALE`: a positive number, else 1. */
fun parseMotionScale(raw: String?): Float =
    raw?.trim()?.toFloatOrNull()?.takeIf { it > 0f } ?: 1f
