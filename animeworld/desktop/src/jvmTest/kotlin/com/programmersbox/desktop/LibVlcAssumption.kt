package com.programmersbox.desktop

import org.junit.Assume.assumeTrue
import java.io.File

/** Skips a libVLC test when this host has no bundled libVLC (it is only prepared for the host OS). */
internal fun assumeLibVlcBundled() = assumeTrue(
    "no libVLC bundled: compose.application.resources.dir/vlc missing",
    System.getProperty("compose.application.resources.dir")?.let { File(it, "vlc").isDirectory } == true,
)
