package com.programmersbox.desktop.motion

import com.programmersbox.kmpuiviews.motion.DesktopMotionCatalog

fun mangaMotionCatalog(): DesktopMotionCatalog = DesktopMotionCatalog(
    appName = "MangaWorld",
    launch = listOf(SushiUnroll),
    quit = listOf(SushiRollUp),
)
