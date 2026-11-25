package com.programmersbox.desktop.motion

import androidx.compose.animation.core.CubicBezierEasing
import com.programmersbox.kmpuiviews.motion.Keyframe
import com.programmersbox.kmpuiviews.motion.MockupEasing
import com.programmersbox.kmpuiviews.motion.track

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
        insetLeft = track(time, Keyframe(2800, 16f, MockupEasing.Out), Keyframe(3200, 0f)),
        insetRight = track(time, Keyframe(1500, 84f), Keyframe(2592, 0f)),
        sheetAlpha = track(time, Keyframe(2650, 1f, MockupEasing.InOut), Keyframe(3200, 0f)),
        boardAlpha = 1f,
        rollDrop = track(time, Keyframe(0, -60f, Drop), Keyframe(850, 0f)),
        rollTravel = track(time, Keyframe(1500, 0f), Keyframe(2800, 100f)),
        rollSpin = track(time, Keyframe(1500, 0f), Keyframe(2800, 540f)),
        stickSpread = track(time, Keyframe(950, 0f, MockupEasing.Out), Keyframe(1100, 1.6f)),
        stickTilt = track(time, Keyframe(950, 10f, MockupEasing.Out), Keyframe(1100, 14f)),
        stickLift = track(time, Keyframe(1100, 0f, MockupEasing.In), Keyframe(1550, -70f)),
    )
}

/** The app turns into a rice sheet, a roll sweeps in and rolls it up, then the chopsticks carry it off. */
internal object SushiRollUp : SushiScene() {
    override val key = "sushi-roll-up"
    override val title = "Sushi roll-up"
    override val durationMillis = 3100

    override fun frame(time: Int) = SushiFrame(
        insetLeft = track(time, Keyframe(1400, 0f), Keyframe(1600, 16f)),
        insetRight = track(time, Keyframe(592, 0f), Keyframe(1400, 67.3f), Keyframe(1600, 84f)),
        sheetAlpha = track(time, Keyframe(0, 0f, MockupEasing.InOut), Keyframe(450, 1f)),
        boardAlpha = track(time, Keyframe(2650, 1f), Keyframe(3100, 0f)),
        rollDrop = track(time, Keyframe(2350, 0f, MockupEasing.In), Keyframe(2850, -62f)),
        rollTravel = track(time, Keyframe(400, 100f), Keyframe(1600, 0f)),
        rollSpin = track(time, Keyframe(400, 540f), Keyframe(1600, 0f)),
        stickSpread = track(time, Keyframe(2100, 1.6f, MockupEasing.InOut), Keyframe(2250, 0f)),
        stickTilt = track(time, Keyframe(2100, 14f, MockupEasing.InOut), Keyframe(2250, 10f)),
        stickLift = track(time, Keyframe(1650, -70f, MockupEasing.Out), Keyframe(2100, 0f)),
    )
}
