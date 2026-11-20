package com.programmersbox.desktop.motion

import com.programmersbox.kmpuiviews.motion.DesktopMotionCatalog

fun novelMotionCatalog(): DesktopMotionCatalog = DesktopMotionCatalog(
    appName = "NovelWorld",
    launch = listOf(BookOpening),
    quit = listOf(BookClosing),
)
