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
        insetLeft = track(time, Key(2800, 16f, EaseOut), Key(3200, 0f)),
        insetRight = track(time, Key(1500, 84f), Key(2592, 0f)),
        sheetAlpha = track(time, Key(2650, 1f, EaseInOut), Key(3200, 0f)),
        boardAlpha = 1f,
        rollDrop = track(time, Key(0, -60f, Drop), Key(850, 0f)),
        rollTravel = track(time, Key(1500, 0f), Key(2800, 100f)),
        rollSpin = track(time, Key(1500, 0f), Key(2800, 540f)),
        stickSpread = track(time, Key(950, 0f, EaseOut), Key(1100, 1.6f)),
        stickTilt = track(time, Key(950, 10f, EaseOut), Key(1100, 14f)),
        stickLift = track(time, Key(1100, 0f, EaseIn), Key(1550, -70f)),
    )
}

/** The app turns into a rice sheet, a roll sweeps in and rolls it up, then the chopsticks carry it off. */
internal object SushiRollUp : SushiScene() {
    override val key = "sushi-roll-up"
    override val title = "Sushi roll-up"
    override val durationMillis = 3100

    override fun frame(time: Int) = SushiFrame(
        insetLeft = track(time, Key(1400, 0f), Key(1600, 16f)),
        insetRight = track(time, Key(592, 0f), Key(1400, 67.3f), Key(1600, 84f)),
        sheetAlpha = track(time, Key(0, 0f, EaseInOut), Key(450, 1f)),
        boardAlpha = track(time, Key(2650, 1f), Key(3100, 0f)),
        rollDrop = track(time, Key(2350, 0f, EaseIn), Key(2850, -62f)),
        rollTravel = track(time, Key(400, 100f), Key(1600, 0f)),
        rollSpin = track(time, Key(400, 540f), Key(1600, 0f)),
        stickSpread = track(time, Key(2100, 1.6f, EaseInOut), Key(2250, 0f)),
        stickTilt = track(time, Key(2100, 14f, EaseInOut), Key(2250, 10f)),
        stickLift = track(time, Key(1650, -70f, EaseOut), Key(2100, 0f)),
    )
}
