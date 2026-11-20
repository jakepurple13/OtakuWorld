package com.programmersbox.desktop.motion

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import com.programmersbox.kmpuiviews.motion.Keyframe
import com.programmersbox.kmpuiviews.motion.MockupEasing
import com.programmersbox.kmpuiviews.motion.track

/*
 * Ported from the `Book opening` / `Book closing` entries in docs/mockups/desktop-launch-quit-animations.html.
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 * Layers, back to front: desk (Behind), app, book (InFront).
 */

/** `.bk-zoom` scale when the camera has dived into the pages. */
private const val BOOK_DIVE = 3.2f

/** `.bk-slide` translateX once the cover is open: centres the open spread, cqw. */
private const val BOOK_OPEN_SLIDE = 15f

/** `.book` translateY while it is below the desk line, cqw. */
private const val BOOK_SUNK = 3f

/** App `scale()` while it is hidden behind the book. */
private const val APP_BEHIND_SCALE = 1.04f

/** A closed book rises onto a desk, the cover swings open to the left, and the camera dives into the pages. */
internal object BookOpening : DesktopMotion {
    override val key = "book-opening"
    override val title = "Book opening"
    override val durationMillis = 2400

    fun frame(time: Int) = BookFrame(
        deskAlpha = track(time, Keyframe(1500, 1f), Keyframe(2100, 0f)),
        appAlpha = track(time, Keyframe(1600, 0f, MockupEasing.Out), Keyframe(2300, 1f)),
        appScale = track(time, Keyframe(1600, APP_BEHIND_SCALE, MockupEasing.Out), Keyframe(2300, 1f)),
        zoom = track(time, Keyframe(1500, 1f, MockupEasing.In), Keyframe(2300, BOOK_DIVE)),
        zoomAlpha = track(time, Keyframe(1500, 1f, MockupEasing.In), Keyframe(2300, 0f)),
        slide = track(time, Keyframe(500, 0f, MockupEasing.InOut), Keyframe(1400, BOOK_OPEN_SLIDE)),
        bookAlpha = track(time, Keyframe(0, 0f, MockupEasing.Out), Keyframe(450, 1f)),
        bookDrop = track(time, Keyframe(0, BOOK_SUNK, MockupEasing.Out), Keyframe(450, 0f)),
        coverAngle = track(time, Keyframe(500, 0f, MockupEasing.InOut), Keyframe(1400, -180f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier = bookApp { frame(time()) }

    @Composable
    override fun BoxScope.Behind(time: () -> Int) = BookDesk { frame(time()) }

    @Composable
    override fun BoxScope.InFront(time: () -> Int) = BookOnDesk { frame(time()) }
}

/** The app pulls back into the pages of an open book, the cover swings shut, and the book sinks away. */
internal object BookClosing : DesktopMotion {
    override val key = "book-closing"
    override val title = "Book closing"
    override val durationMillis = 2300

    fun frame(time: Int) = BookFrame(
        deskAlpha = track(time, Keyframe(1800, 1f), Keyframe(2300, 0f)),
        appAlpha = track(time, Keyframe(0, 1f, MockupEasing.In), Keyframe(600, 0f)),
        appScale = track(time, Keyframe(0, 1f, MockupEasing.In), Keyframe(600, APP_BEHIND_SCALE)),
        zoom = track(time, Keyframe(0, BOOK_DIVE, MockupEasing.Out), Keyframe(700, 1f)),
        zoomAlpha = track(time, Keyframe(0, 0f, MockupEasing.Out), Keyframe(700, 1f)),
        slide = track(time, Keyframe(800, BOOK_OPEN_SLIDE, MockupEasing.InOut), Keyframe(1600, 0f)),
        bookAlpha = track(time, Keyframe(1700, 1f, MockupEasing.In), Keyframe(2100, 0f)),
        bookDrop = track(time, Keyframe(1700, 0f, MockupEasing.In), Keyframe(2100, BOOK_SUNK)),
        coverAngle = track(time, Keyframe(800, -180f, MockupEasing.InOut), Keyframe(1600, 0f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier = bookApp { frame(time()) }

    @Composable
    override fun BoxScope.Behind(time: () -> Int) = BookDesk { frame(time()) }

    @Composable
    override fun BoxScope.InFront(time: () -> Int) = BookOnDesk { frame(time()) }
}
