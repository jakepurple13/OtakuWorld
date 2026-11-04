package com.programmersbox.kmpuiviews

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.SplashScreen
import java.awt.geom.RoundRectangle2D

/**
 * Animates an indeterminate progress bar on the native JVM splash (`-splash:`, a card rendered
 * by `RenderDesktopSplash` in buildSrc) from a daemon thread.
 *
 * The bar is drawn off the AWT event thread on purpose: Koin start-up and the main window's first
 * composition block that thread for seconds, which froze the old Compose splash. AWT closes the
 * native splash itself once the first window becomes visible, which also ends this thread.
 */
internal object NativeSplash {
    // Must match SplashCard in buildSrc/src/main/kotlin/DesktopSplash.kt.
    private const val BAR_X = 32f
    private const val BAR_Y = 236f
    private const val BAR_WIDTH = 296f
    private const val BAR_HEIGHT = 4f

    // Material 3 baseline dark scheme, as in the card.
    private val track = Color(0x4A4458)
    private val indicator = Color(0xD0BCFF)

    private const val CYCLE_MS = 1750L
    private const val FRAME_MS = 1000L / 60

    fun start() {
        val splash = runCatching { SplashScreen.getSplashScreen() }.getOrNull() ?: return
        Thread(
            {
                runCatching { animate(splash) } // IllegalStateException once AWT closes the splash
            },
            "native-splash"
        ).apply {
            isDaemon = true
            start()
        }
    }

    private fun animate(splash: SplashScreen) {
        val g = splash.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val bar = RoundRectangle2D.Float()
        val start = System.nanoTime()
        while (splash.isVisible) {
            val frameStart = System.nanoTime()
            val t = ((frameStart - start) / 1_000_000 % CYCLE_MS).toFloat()

            g.composite = AlphaComposite.Clear
            g.fillRect(BAR_X.toInt() - 1, BAR_Y.toInt() - 1, BAR_WIDTH.toInt() + 2, BAR_HEIGHT.toInt() + 2)
            g.composite = AlphaComposite.SrcOver

            g.color = track
            bar.setRoundRect(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, BAR_HEIGHT, BAR_HEIGHT)
            g.fill(bar)

            g.color = indicator
            segment(g, bar, head = FirstHead.at(t), tail = FirstTail.at(t))
            segment(g, bar, head = SecondHead.at(t), tail = SecondTail.at(t))

            splash.update()
            val sleep = FRAME_MS - (System.nanoTime() - frameStart) / 1_000_000
            if (sleep > 0) Thread.sleep(sleep)
        }
    }

    private fun segment(g: Graphics2D, bar: RoundRectangle2D.Float, head: Float, tail: Float) {
        val width = (head - tail) * BAR_WIDTH
        if (width <= 0f) return
        bar.setRoundRect(BAR_X + tail * BAR_WIDTH, BAR_Y, width, BAR_HEIGHT, BAR_HEIGHT, BAR_HEIGHT)
        g.fill(bar)
    }

    // Same keyframes as the Material indeterminate LinearProgressIndicator.
    private val FirstHead = Keyframe(delay = 0f, duration = 750f, CubicBezier(0.2f, 0f, 0.8f, 1f))
    private val FirstTail = Keyframe(delay = 333f, duration = 850f, CubicBezier(0.4f, 0f, 1f, 1f))
    private val SecondHead = Keyframe(delay = 1000f, duration = 567f, CubicBezier(0f, 0f, 0.65f, 1f))
    private val SecondTail = Keyframe(delay = 1267f, duration = 533f, CubicBezier(0.1f, 0f, 0.45f, 1f))

    private class Keyframe(val delay: Float, val duration: Float, val easing: CubicBezier) {
        fun at(t: Float): Float = easing.transform(((t - delay) / duration).coerceIn(0f, 1f))
    }

    private class CubicBezier(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {
        private fun bezier(a: Float, b: Float, s: Float): Float {
            val u = 1 - s
            return 3 * u * u * s * a + 3 * u * s * s * b + s * s * s
        }

        fun transform(x: Float): Float {
            if (x <= 0f || x >= 1f) return x
            var lo = 0f
            var hi = 1f
            repeat(20) {
                val mid = (lo + hi) / 2
                if (bezier(x1, x2, mid) < x) lo = mid else hi = mid
            }
            return bezier(y1, y2, (lo + hi) / 2)
        }
    }
}
