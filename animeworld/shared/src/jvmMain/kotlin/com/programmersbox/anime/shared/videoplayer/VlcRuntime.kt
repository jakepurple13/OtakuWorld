package com.programmersbox.anime.shared.videoplayer

import com.sun.jna.NativeLibrary
import uk.co.caprica.vlcj.binding.lib.LibC
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.factory.discovery.strategy.BaseNativeDiscoveryStrategy
import java.io.File

/**
 * Loads the libVLC that the build bundles as Compose Desktop app resources
 * (`<compose.application.resources.dir>/vlc/{lib,plugins}`, produced by `prepareVlc`).
 *
 * A system VLC is never used: discovery only searches the bundled `lib` dir, and loading fails
 * if JNA ends up with a libvlc from anywhere else.
 *
 * There is no plugin cache: libVLC 3 can only write `plugins.dat` into the plugins dir itself,
 * which is read-only in a packaged app, so the build prunes the plugins instead to keep the
 * first-load scan short.
 */
object VlcRuntime {
    private const val RESOURCES_DIR_PROPERTY = "compose.application.resources.dir"

    private val options = arrayOf(
        "--adaptive-maxheight=1080",
        "--no-video-title-show",
        "--quiet",
    )

    private val result: Result<MediaPlayerFactory> by lazy { runCatching { load() } }

    /** Absolute path of the lib dir libVLC was loaded from, or null if it has not loaded. */
    @Volatile
    var loadedFrom: String? = null
        private set

    /** libVLC version string (e.g. `3.0.24 Vetinari`), or null if it has not loaded. */
    @Volatile
    var version: String? = null
        private set

    /** Loads libVLC from `<resourcesDir>/vlc` only. Idempotent: later calls return the same result. */
    fun factory(): Result<MediaPlayerFactory> = result

    private fun load(): MediaPlayerFactory {
        val resourcesDir = System.getProperty(RESOURCES_DIR_PROPERTY)
            ?: error("libVLC is not bundled: system property $RESOURCES_DIR_PROPERTY is not set")
        val libDir = File(resourcesDir, "vlc/lib").absoluteFile
        check(libDir.isDirectory) { "libVLC is not bundled: $libDir does not exist" }

        val discovery = NativeDiscovery(BundledDiscovery(libDir.path))
        check(discovery.discover()) { "libVLC not found in $libDir" }

        val factory = MediaPlayerFactory(discovery, *options)
        val loadedFile = NativeLibrary.getInstance(RuntimeUtil.getLibVlcLibraryName()).file?.absoluteFile
        if (loadedFile?.parentFile != libDir) {
            factory.release()
            error("libVLC was loaded from $loadedFile instead of the bundled $libDir")
        }

        loadedFrom = loadedFile.parentFile.path
        version = factory.application().version()
        println("[VlcRuntime] loaded libVLC $version from $loadedFrom")
        return factory
    }

    /** Native discovery restricted to the bundled lib dir (see the desktop player spike). */
    private class BundledDiscovery(private val libDir: String) : BaseNativeDiscoveryStrategy(
        if (RuntimeUtil.isWindows()) {
            arrayOf("libvlc\\.dll", "libvlccore\\.dll")
        } else {
            arrayOf("libvlc\\.dylib", "libvlccore\\.dylib")
        },
        arrayOf("%s/../plugins"),
    ) {
        override fun supported() = RuntimeUtil.isMac() || RuntimeUtil.isWindows()

        override fun discoveryDirectories() = listOf(libDir)

        override fun onFound(path: String): Boolean {
            // libvlc links libvlccore via @rpath (macOS) or the DLL search path (Windows):
            // preload it by absolute path so the bundled copy is the one that resolves.
            val core = RuntimeUtil.getLibVlcCoreLibraryName()
            NativeLibrary.addSearchPath(core, path)
            NativeLibrary.getInstance(core)
            return true
        }

        // Must be set natively before libvlc_new; Java's System.getenv will not show it.
        override fun setPluginPath(pluginPath: String): Boolean =
            if (RuntimeUtil.isWindows()) {
                LibC.INSTANCE._putenv("$PLUGIN_ENV_NAME=$pluginPath") == 0
            } else {
                LibC.INSTANCE.setenv(PLUGIN_ENV_NAME, pluginPath, 1) == 0
            }
    }
}
