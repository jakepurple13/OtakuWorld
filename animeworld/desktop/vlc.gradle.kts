import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import javax.inject.Inject

// Bundles libVLC for the desktop video player as Compose Desktop app resources:
//   build/appResources/<os>-<arch>/vlc/{lib,plugins}
// Compose merges build/appResources/<os>-<arch> into compose.application.resources.dir for `run`
// and the packaged app, and VlcRuntime loads libVLC from <that dir>/vlc only.
//
// Sources are the official VideoLAN builds; checksums are the .sha256 files VideoLAN publishes
// next to each archive (and were verified against fresh downloads).

val vlcVersion = "3.0.24"

data class VlcArchive(val url: String, val sha256: String)

val vlcArchives = mapOf(
    "macos-arm64" to VlcArchive(
        "https://get.videolan.org/vlc/$vlcVersion/macosx/vlc-$vlcVersion-arm64.dmg",
        "64a89d93cdd30b0e97131743e246373db82d6826ea882d265d46d74b136da2b7",
    ),
    "macos-x64" to VlcArchive(
        "https://get.videolan.org/vlc/$vlcVersion/macosx/vlc-$vlcVersion-intel64.dmg",
        "1ef6c903e2dc026d4e58ddf7b2a9ed0eb6f8caf80a9a9f46f96490340b962707",
    ),
    "windows-x64" to VlcArchive(
        "https://get.videolan.org/vlc/$vlcVersion/win64/vlc-$vlcVersion-win64.zip",
        "fcf30850371ad10c9373cc4f0f4501e7dee49e3e9ae9f20c72fb2661a1ca6323",
    ),
)

// Plugin category dirs (Windows layout) that network/local playback never uses.
val vlcPrunedPluginDirs = listOf(
    "gui", "lua", "control", "services_discovery", "visualization", "video_splitter",
)

// Plugins (lib<name>_plugin.dylib/.dll) that network/local playback never uses. The macOS
// plugins dir is flat, so this list is what prunes it; on Windows it trims the kept dirs.
// stream_out/mux/access_output_file are kept on purpose: downloads may remux through libVLC.
val vlcPrunedPlugins = listOf(
    // Discs and Blu-ray (the Blu-ray .jar files are pruned too)
    "libbluray", "dvdnav", "dvdread", "cdda", "vcd", "dcp",
    // GUIs and control interfaces
    "macosx", "qt", "skins2", "ncurses", "oldrc", "netsync", "gestures", "motion", "lua",
    // Service discovery
    "upnp", "bonjour", "sap", "mediadirs", "podcast", "microdns",
    // Capture devices
    "avcapture", "avaudiocapture", "screen", "dshow", "dtv",
    // Visualizations, notifications, scrobbling, addons, fingerprinting, casting
    "goom", "visual", "glspectrum", "projectm", "osx_notifications", "nsspeechsynthesizer", "sapi",
    "audioscrobbler", "addonsfsstorage", "addonsvorepository", "fingerprinter",
    "stream_out_chromaprint", "stream_out_chromecast", "demux_chromecast", "remoteosd",
    // Encoders (playback only decodes; dav1d decodes AV1)
    "x264", "x26410b", "x265", "twolame", "aom",
    // Streaming protocols and network shares the app does not use
    "access_srt", "access_output_srt", "rist", "access_output_rist", "access_output_shout",
    "access_output_livehttp", "access_output_udp", "stream_out_rtp", "rtp", "live555", "vod_rtsp",
    "sftp", "nfs", "smb", "ftp", "mms", "satip",
    // Game music emulators
    "gme", "sid",
)

val hostOs = System.getProperty("os.name").lowercase()
val hostArch = System.getProperty("os.arch").lowercase()

// Same ids as Compose's currentTarget, so prepareAppResources picks up the right subdir.
val vlcTarget: String? = when {
    hostOs.contains("mac") && hostArch in listOf("aarch64", "arm64") -> "macos-arm64"
    hostOs.contains("mac") && hostArch in listOf("x86_64", "amd64") -> "macos-x64"
    hostOs.contains("windows") && hostArch in listOf("x86_64", "amd64") -> "windows-x64"
    else -> null
}

abstract class PrepareVlcTask @Inject constructor(
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : DefaultTask() {
    @get:Input abstract val version: Property<String>
    @get:Input abstract val url: Property<String>
    @get:Input abstract val sha256: Property<String>
    @get:Input abstract val prunedPluginDirs: ListProperty<String>
    @get:Input abstract val prunedPlugins: ListProperty<String>

    /** Holds the downloaded archive between runs, so a clean output does not re-download. */
    @get:Internal abstract val downloadDir: DirectoryProperty

    /** `.../appResources/<os>-<arch>/vlc` */
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun prepare() {
        val archive = download()
        val out = outputDir.get().asFile
        fileSystemOperations.delete { delete(out) }
        val extractDir = temporaryDir.resolve("extract")
        fileSystemOperations.delete { delete(extractDir) }

        if (archive.name.endsWith(".dmg")) {
            val mountPoint = temporaryDir.resolve("mnt").apply { mkdirs() }
            execOperations.exec {
                commandLine("hdiutil", "attach", "-nobrowse", "-readonly", "-noverify", "-quiet", "-mountpoint", mountPoint.path, archive.path)
            }
            try {
                val macOs = mountPoint.resolve("VLC.app/Contents/MacOS")
                install(macOs.resolve("lib"), macOs.resolve("plugins"), out, ".dylib")
            } finally {
                execOperations.exec { commandLine("hdiutil", "detach", "-quiet", "-force", mountPoint.path) }
            }
            // Downloads made by a browser carry quarantine; strip it so dyld loads the plugins.
            execOperations.exec {
                commandLine("xattr", "-dr", "com.apple.quarantine", out.path)
                isIgnoreExitValue = true
            }
        } else {
            val root = "vlc-${version.get()}"
            fileSystemOperations.copy {
                from(archiveOperations.zipTree(archive))
                include("$root/libvlc.dll", "$root/libvlccore.dll", "$root/plugins/**")
                into(extractDir)
            }
            install(extractDir.resolve(root), extractDir.resolve("$root/plugins"), out, ".dll")
            fileSystemOperations.delete { delete(extractDir) }
        }

        logger.lifecycle("libVLC ${version.get()} prepared in $out (${out.walkTopDown().filter { it.isFile }.sumOf { it.length() } / (1024 * 1024)} MB)")
    }

    /** Copies libvlc + libvlccore into `out/lib` and the pruned plugins into `out/plugins`. */
    private fun install(libSource: File, pluginsSource: File, out: File, libExtension: String) {
        val lib = out.resolve("lib").apply { mkdirs() }
        for (name in listOf("libvlc", "libvlccore")) {
            // Files.copy follows symlinks (macOS ships libvlc.dylib -> libvlc.5.dylib); the
            // unversioned names are the install names libvlc links against via @rpath.
            Files.copy(libSource.resolve("$name$libExtension").toPath(), lib.resolve("$name$libExtension").toPath())
        }

        val prunedDirs = prunedPluginDirs.get().toSet()
        val pruned = prunedPlugins.get().toSet()
        val pluginName = Regex("""^lib(.+)_plugin\.(dylib|dll)$""")
        val plugins = out.resolve("plugins")
        pluginsSource.walkTopDown()
            .onEnter { it == pluginsSource || it.relativeTo(pluginsSource).invariantSeparatorsPath !in prunedDirs }
            .filter { it.isFile }
            .filterNot { it.name.endsWith(".jar") }
            .filterNot { pluginName.find(it.name)?.groupValues?.get(1) in pruned }
            .forEach { file ->
                val target = plugins.resolve(file.relativeTo(pluginsSource))
                target.parentFile.mkdirs()
                Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
    }

    /** Downloads the archive once into [downloadDir], verifying its SHA-256. */
    private fun download(): File {
        val expected = sha256.get()
        val archive = downloadDir.get().asFile.resolve(url.get().substringAfterLast('/'))
        if (archive.isFile && sha256Of(archive) == expected) return archive

        archive.parentFile.mkdirs()
        val part = File(archive.path + ".part")
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
        var lastError: Exception? = null
        // get.videolan.org redirects to a random mirror; retry so one bad mirror does not fail the build.
        repeat(3) { attempt ->
            try {
                logger.lifecycle("Downloading ${url.get()} (attempt ${attempt + 1})")
                val response = client.send(
                    HttpRequest.newBuilder(URI(url.get())).GET().build(),
                    HttpResponse.BodyHandlers.ofFile(part.toPath()),
                )
                check(response.statusCode() == 200) { "HTTP ${response.statusCode()} from ${response.uri()}" }
                val actual = sha256Of(part)
                check(actual == expected) { "SHA-256 mismatch for ${response.uri()}: expected $expected, got $actual" }
                Files.move(part.toPath(), archive.toPath(), StandardCopyOption.REPLACE_EXISTING)
                return archive
            } catch (e: Exception) {
                lastError = e
                part.delete()
            }
        }
        throw GradleException("Could not download libVLC from ${url.get()}", lastError)
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

val vlcArchive = vlcTarget?.let { vlcArchives.getValue(it) }

if (vlcTarget != null && vlcArchive != null) {
    val prepareVlc = tasks.register<PrepareVlcTask>("prepareVlc") {
        group = "build"
        description = "Downloads libVLC $vlcVersion for $vlcTarget into build/appResources/$vlcTarget/vlc"
        version.set(vlcVersion)
        url.set(vlcArchive.url)
        sha256.set(vlcArchive.sha256)
        prunedPluginDirs.set(vlcPrunedPluginDirs)
        prunedPlugins.set(vlcPrunedPlugins)
        downloadDir.set(layout.buildDirectory.dir("vlc-download"))
        outputDir.set(layout.buildDirectory.dir("appResources/$vlcTarget/vlc"))
    }

    // Tests run without Compose's prepareAppResources, so point them at the per-target dir directly.
    tasks.withType<Test>().named { it == "jvmTest" }.configureEach {
        dependsOn(prepareVlc)
        systemProperty(
            "compose.application.resources.dir",
            layout.buildDirectory.dir("appResources/$vlcTarget").get().asFile.absolutePath,
        )
    }
} else {
    // No official VideoLAN libVLC build for this host (e.g. Linux): the app builds without one,
    // and the player reports that libVLC is unavailable.
    tasks.register("prepareVlc") {
        group = "build"
        description = "No bundled libVLC for $hostOs/$hostArch"
        doLast { logger.warn("libVLC is not bundled for $hostOs/$hostArch; the desktop player will be unavailable.") }
    }
}
