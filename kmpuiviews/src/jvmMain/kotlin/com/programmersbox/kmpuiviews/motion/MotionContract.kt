package com.programmersbox.kmpuiviews.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap

/** Renders [motion] at [timeMillis] with a solid [appColor] app via ImageComposeScene (transparent background). */
fun renderMotionFrame(
    motion: DesktopMotion,
    timeMillis: Int,
    width: Int = 320,
    height: Int = 200,
    appColor: Color = Color.Red,
): ImageBitmap {
    val scene = ImageComposeScene(width, height)
    try {
        scene.setContent {
            MotionHost(motion, { timeMillis }) {
                Box(Modifier.fillMaxSize().background(appColor))
            }
        }
        return scene.render().toComposeImageBitmap()
    } finally {
        scene.close()
    }
}

object MotionContract {
    private val appColor = Color.Red

    /** Problems with [motion] as a [phase] animation in a [width]×[height] host; empty when it obeys the contract. */
    fun violations(motion: DesktopMotion, phase: MotionPhase, width: Int = 320, height: Int = 200): List<String> {
        if (motion === OffMotion) return emptyList()
        val end = motion.durationMillis
        val problems = mutableListOf<String>()
        fun frame(time: Int) = points(renderMotionFrame(motion, time, width, height, appColor))
        when (phase) {
            MotionPhase.Launch -> {
                frame(end).forEach { (name, c) ->
                    if (c != appColor) problems += "launch at $end ms: $name is $c, expected $appColor"
                }
                val centre = frame(0).getValue("centre")
                if (centre == appColor) problems += "launch at 0 ms: centre is already $appColor"
            }

            MotionPhase.Quit -> {
                frame(0).forEach { (name, c) ->
                    if (c != appColor) problems += "quit at 0 ms: $name is $c, expected $appColor"
                }
                frame(end).forEach { (name, c) ->
                    if (c.alpha != 0f) problems += "quit at $end ms: $name is $c, expected alpha 0"
                }
            }
        }
        return problems
    }

    private fun points(image: ImageBitmap): Map<String, Color> {
        val w = image.width
        val h = image.height
        val buffer = IntArray(w * h)
        image.readPixels(buffer)
        fun at(x: Int, y: Int) = Color(buffer[y * w + x])
        return linkedMapOf(
            "centre" to at(w / 2, h / 2),
            "top-left" to at(w / 4, h / 4),
            "top-right" to at(w * 3 / 4, h / 4),
            "bottom-left" to at(w / 4, h * 3 / 4),
            "bottom-right" to at(w * 3 / 4, h * 3 / 4),
        )
    }
}
