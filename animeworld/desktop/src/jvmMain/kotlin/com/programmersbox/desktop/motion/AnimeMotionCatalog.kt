package com.programmersbox.desktop.motion

import com.programmersbox.kmpuiviews.motion.DesktopMotionCatalog

fun animeMotionCatalog(): DesktopMotionCatalog = DesktopMotionCatalog(
    appName = "AnimeWorld",
    launch = listOf(CrtPowerOn, PlayButton, Eyecatch),
    quit = listOf(CrtPowerOff, NextEpisode, IrisOut),
)
