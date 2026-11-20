package com.programmersbox.desktop.motion

import com.programmersbox.kmpuiviews.motion.DesktopMotionCatalog

fun novelMotionCatalog(): DesktopMotionCatalog = DesktopMotionCatalog(
    appName = "NovelWorld",
    launch = listOf(BookOpening, Typewriter, InkDrop),
    quit = listOf(BookClosing, TheEnd, LampOut),
)
