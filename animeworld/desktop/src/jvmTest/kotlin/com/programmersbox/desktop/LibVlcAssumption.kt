package com.programmersbox.desktop

import org.junit.Assume.assumeTrue
import java.io.File

/**
 * Skips a libVLC test unless `VLC_TESTS=1` is set and this host has a bundled libVLC (it is only
 * prepared for the host OS). Opt-in because the first libVLC load in a test JVM scans its plugins (~30 s).
 */
internal fun assumeLibVlcBundled() {
    assumeTrue("set VLC_TESTS=1 to run", System.getenv("VLC_TESTS") == "1")
    assumeTrue(
        "no libVLC bundled: compose.application.resources.dir/vlc missing",
        System.getProperty("compose.application.resources.dir")?.let { File(it, "vlc").isDirectory } == true,
    )
}
