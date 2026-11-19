package com.programmersbox.desktop.motion

import androidx.compose.animation.core.CubicBezierEasing

/*
 * Ported from the `Sushi unroll` / `Sushi roll-up` entries in docs/mockups/desktop-launch-quit-animations.html.
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** Chopsticks set a maki roll on the board, let go, and the roll unrolls to the right into the app. */
internal object SushiUnroll : SushiScene() {
    override val key = "sushi-unroll"
    override val title = "Sushi unroll"
    override val durationMillis = 3200

    private val Drop = CubicBezierEasing(.25f, .9f, .3f, 1f)

    override fun frame(time: Int) = SushiFrame(
        insetLeft = track(time, key(2800, 16f, EaseOut), key(3200, 0f)),
        insetRight = track(time, key(1500, 84f), key(2592, 0f)),
        sheetAlpha = track(time, key(2650, 1f, EaseInOut), key(3200, 0f)),
        boardAlpha = 1f,
        rollDrop = track(time, key(0, -60f, Drop), key(850, 0f)),
        rollTravel = track(time, key(1500, 0f), key(2800, 100f)),
        rollSpin = track(time, key(1500, 0f), key(2800, 540f)),
        stickSpread = track(time, key(950, 0f, EaseOut), key(1100, 1.6f)),
        stickTilt = track(time, key(950, 10f, EaseOut), key(1100, 14f)),
        stickLift = track(time, key(1100, 0f, EaseIn), key(1550, -70f)),
    )
}

/** The app turns into a rice sheet, a roll sweeps in and rolls it up, then the chopsticks carry it off. */
internal object SushiRollUp : SushiScene() {
    override val key = "sushi-roll-up"
    override val title = "Sushi roll-up"
    override val durationMillis = 3100

    override fun frame(time: Int) = SushiFrame(
        insetLeft = track(time, key(1400, 0f), key(1600, 16f)),
        insetRight = track(time, key(592, 0f), key(1400, 67.3f), key(1600, 84f)),
        sheetAlpha = track(time, key(0, 0f, EaseInOut), key(450, 1f)),
        boardAlpha = track(time, key(2650, 1f), key(3100, 0f)),
        rollDrop = track(time, key(2350, 0f, EaseIn), key(2850, -62f)),
        rollTravel = track(time, key(400, 100f), key(1600, 0f)),
        rollSpin = track(time, key(400, 540f), key(1600, 0f)),
        stickSpread = track(time, key(2100, 1.6f, EaseInOut), key(2250, 0f)),
        stickTilt = track(time, key(2100, 14f, EaseInOut), key(2250, 10f)),
        stickLift = track(time, key(1650, -70f, EaseOut), key(2100, 0f)),
    )
}
