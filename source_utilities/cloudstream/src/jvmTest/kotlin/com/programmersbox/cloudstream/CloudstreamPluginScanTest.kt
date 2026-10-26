package com.programmersbox.cloudstream

import android.app.Activity
import androidx.fragment.app.Fragment
import com.programmersbox.cloudstream.plugins.CLOUDSTREAM_PARENT_FIRST
import com.programmersbox.cloudstream.repo.CloudstreamRepoClient
import com.programmersbox.kmpextensionloader.ChildFirstClassLoader
import com.programmersbox.kmpextensionloader.DexConverter
import com.programmersbox.kmpextensionloader.ExtensionRefScanner
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.objectweb.asm.ClassReader
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.Test

/**
 * Developer tool, skipped unless CS_SCAN=1. Downloads CloudStream plugins, converts each with
 * [DexConverter], and reports for each converted jar:
 * - every class/interface type it mentions anywhere (supertypes, descriptors, instructions, catch types)
 *   that the host classpath lacks, with its real shape (class vs interface, superclass, interfaces) from
 *   the shape jars. The JVM verifier loads such types when it checks a method's assignments, so one in
 *   an unused settings method can still fail the whole class with NoClassDefFoundError;
 * - every member it calls that the host lacks (the NoSuchMethodError/NoSuchFieldError a call would hit);
 * - the result of loading and initializing every class except Activity/Fragment subclasses (desktop
 *   never creates screens) through [ChildFirstClassLoader] with [CLOUDSTREAM_PARENT_FIRST]: any
 *   NoClassDefFoundError/VerifyError/IncompatibleClassChangeError/... is listed.
 * Types and members referenced only from Activity/Fragment subclasses are marked "(screen only)".
 *
 * Defaults: the four phisher98 plugins `shims.md` was derived from, plus [CloudstreamFixture].
 * CS_SCAN_REPOS overrides the comma-separated repo.json URLs, CS_SCAN_PLUGINS the comma-separated
 * internalNames, CS_SCAN_EXTRA adds comma-separated local `.cs3` paths. CS_SCAN_SHAPE_JARS lists jars to
 * read type shapes from (default: $ANDROID_HOME/platforms/android-36/android.jar; add the classes.jar of
 * the appcompat/fragment/material AARs for their types). Shape jars are only read, never put on a
 * classpath. Downloads, converted jars and scan.md land in CS_SCAN_DIR (default: build/cloudstream-scan).
 *
 * Downloaded plugins are untrusted code: they are only converted, scanned and loaded in this test JVM.
 */
class CloudstreamPluginScanTest {

    @Test fun scanCloudstreamPlugins() = runBlocking {
        assumeTrue("set CS_SCAN=1 to run", System.getenv("CS_SCAN") == "1")
        CloudstreamFixture.assumeAvailable()
        val dir = File(System.getenv("CS_SCAN_DIR") ?: "build/cloudstream-scan").apply { mkdirs() }
        val shapes = ExtensionRefScanner.ShapeIndex(ExtensionRefScanner.shapeJars("CS_SCAN_SHAPE_JARS"))
        val http = HttpClient(OkHttp)
        try {
            val repos = env("CS_SCAN_REPOS") ?: listOf(CloudstreamFixture.REPO_URL)
            val client = CloudstreamRepoClient(http)
            val remotes = client.fetch(repos)
            File(dir, "index.txt").writeText(remotes.joinToString("\n") { "${it.name}\t${it.version}\t${it.downloadLink}" })
            val wanted = env("CS_SCAN_PLUGINS") ?: DEFAULT_PLUGINS

            val plugins = mutableListOf<Pair<String, File?>>()
            wanted.forEach { name ->
                val remote = remotes.firstOrNull { it.packageName == name }
                if (remote == null) {
                    plugins += "$name (not in repo)" to null
                    return@forEach
                }
                val cs3 = File(dir, "plugins/$name-v${remote.version}.cs3")
                if (!cs3.exists()) {
                    val response = http.get(remote.downloadLink)
                    check(response.status.isSuccess()) { "GET ${remote.downloadLink} -> ${response.status}" }
                    cs3.parentFile.mkdirs()
                    cs3.writeBytes(response.readRawBytes())
                }
                plugins += "$name v${remote.version} (${manifest(cs3)})" to cs3
            }
            plugins += "${CloudstreamFixture.INTERNAL_NAME} v${CloudstreamFixture.VERSION} fixture (${manifest(CloudstreamFixture.cs3)})" to CloudstreamFixture.cs3
            env("CS_SCAN_EXTRA")?.forEach { plugins += "local ${File(it).name} (${manifest(File(it))})" to File(it) }

            val report = StringBuilder("| Plugin | Missing types | Missing members | Load failures |\n|---|---|---|---|\n")
            val typeTotals = sortedMapOf<String, MutableSet<String>>()
            val memberTotals = sortedMapOf<String, MutableSet<String>>()
            plugins.forEach { (label, cs3) ->
                if (cs3 == null) {
                    report.append("| $label | | | |\n")
                    return@forEach
                }
                val jar = DexConverter.convert(cs3, File(dir, "jars"))
                if (jar == null) {
                    report.append("| $label | conversion failed | | |\n")
                    return@forEach
                }
                val result = scan(jar)
                val short = label.substringBefore(" (")
                result.types.forEach { typeTotals.getOrPut(it) { sortedSetOf() } += short }
                result.members.forEach { memberTotals.getOrPut(it) { sortedSetOf() } += short }
                fun cell(items: Collection<String>) = items.joinToString("<br>").ifEmpty { "none" }
                report.append("| $label | ${cell(result.types)} | ${cell(result.members)} | ${result.loaded} loaded; ${cell(result.loadFailures)} |\n")
            }
            report.append("\n| Missing type | Real shape | Plugins |\n|---|---|---|\n")
            typeTotals.forEach { (ref, names) ->
                report.append("| $ref | ${shapes.describe(ref.substringBefore(' '))} | ${names.size}: ${names.joinToString()} |\n")
            }
            report.append("\n| Missing member | Plugins |\n|---|---|\n")
            memberTotals.forEach { (ref, names) -> report.append("| $ref | ${names.size}: ${names.joinToString()} |\n") }
            File(dir, "scan.md").writeText(report.toString())
            println(report)
            println("Scan written to ${File(dir, "scan.md").absolutePath}")
        } finally {
            http.close()
        }
    }

    private fun env(name: String): List<String>? =
        System.getenv(name)?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() }

    /** The manifest flags that matter for desktop: requiresResources. */
    private fun manifest(cs3: File): String = ZipFile(cs3).use { zip ->
        val text = zip.getEntry("manifest.json")?.let { e -> zip.getInputStream(e).use { it.readBytes().decodeToString() } }
            ?: return "no manifest.json"
        "requiresResources=" + (Regex("\"requiresResources\"\\s*:\\s*(true|false)").find(text)?.groupValues?.get(1) ?: "?")
    }

    private class ScanResult(val types: List<String>, val members: List<String>, val loaded: Int, val loadFailures: List<String>)

    private val host: ClassLoader = CloudstreamPluginScanTest::class.java.classLoader

    private fun scan(jar: File): ScanResult {
        val classes = ZipFile(jar).use { zip ->
            zip.entries().toList().filter { it.name.endsWith(".class") }.map { e -> zip.getInputStream(e).use { it.readBytes() } }
        }
        val readers = classes.map(::ClassReader)
        val supers = readers.associate { it.className to it.superName }
        fun isScreen(name: String): Boolean {
            var current: String? = name
            while (current != null && current in supers) current = supers[current]
            val hostType = current?.let { runCatching { Class.forName(it.replace('/', '.'), false, host) }.getOrNull() }
                ?: return current in SCREEN_TYPES
            return Activity::class.java.isAssignableFrom(hostType) || Fragment::class.java.isAssignableFrom(hostType)
        }

        // ref -> referencing classes
        val refs = sortedMapOf<String, MutableSet<String>>()
        readers.forEach { reader ->
            ExtensionRefScanner.references(reader).forEach { refs.getOrPut(it) { mutableSetOf() } += reader.className }
        }
        fun label(ref: String) = if (refs.getValue(ref).all(::isScreen)) "$ref (screen only)" else ref
        val unresolved = refs.keys.filter { ref -> ref.substringBefore(' ') !in supers && !ExtensionRefScanner.resolves(ref, host) }
        val types = unresolved.filter { ' ' !in it }
        // A member of a missing type is implied by the type entry.
        val members = unresolved.filter { ' ' in it && it.substringBefore(' ') !in types }

        var loaded = 0
        val failures = mutableListOf<String>()
        ChildFirstClassLoader(arrayOf(jar.toURI().toURL()), host, CLOUDSTREAM_PARENT_FIRST).use { loader ->
            readers.map { it.className }.filterNot(::isScreen).sorted().forEach { name ->
                runCatching { Class.forName(name.replace('/', '.'), true, loader) }
                    .onSuccess { loaded++ }
                    .onFailure { failures += "$name: ${it.javaClass.simpleName}: ${it.message?.lineSequence()?.first()}" }
            }
        }
        return ScanResult(types.map(::label), members.map(::label), loaded, failures)
    }

    private companion object {
        /** The phisher98 plugins `shims.md` was derived from. */
        val DEFAULT_PLUGINS = listOf("Anichi", "AnimePahe", "Anizone", "HiAnime")

        /** Screen supertypes recognized even while the host lacks a stub for them. */
        val SCREEN_TYPES = setOf(
            "androidx/appcompat/app/AppCompatActivity", "androidx/fragment/app/FragmentActivity",
            "androidx/fragment/app/DialogFragment", "androidx/appcompat/app/AppCompatDialogFragment",
            "com/google/android/material/bottomsheet/BottomSheetDialogFragment",
        )
    }
}
