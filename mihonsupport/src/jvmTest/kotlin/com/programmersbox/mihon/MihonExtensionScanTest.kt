package com.programmersbox.mihon

import android.app.Application
import com.programmersbox.kmpextensionloader.ChildFirstClassLoader
import com.programmersbox.kmpextensionloader.DexConverter
import com.programmersbox.kmpextensionloader.ExtensionRefScanner
import com.programmersbox.mihon.repo.MihonRepoClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.objectweb.asm.ClassReader
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.test.Test

/**
 * Developer tool, skipped unless MIHON_SCAN=1. Downloads Keiyoushi extensions, converts each with
 * [DexConverter], and reports for each converted jar:
 * - every class/interface type it mentions anywhere (supertypes, descriptors, instructions, catch types)
 *   that the host classpath lacks, with its real shape (class vs interface, superclass, interfaces) from
 *   the shape jars. The JVM verifier loads such types when it checks a method's assignments, so one in
 *   an unused settings method still fails the whole class with NoClassDefFoundError;
 * - every member it calls that the host lacks (the NoSuchMethodError/NoSuchFieldError a call would hit);
 * - the result of loading and initializing every class except Activity subclasses (desktop never loads
 *   those): any NoClassDefFoundError/VerifyError/IncompatibleClassChangeError/... is listed.
 * Types and members referenced only from Activity subclasses are marked "(Activity only)".
 *
 * MIHON_SCAN_DIR keeps downloads between runs (default: a temp dir). MIHON_SCAN_PACKAGES overrides the
 * comma-separated package list. MIHON_SCAN_EXTRA_APKS adds comma-separated local APK paths.
 * MIHON_SCAN_SHAPE_JARS lists jars to read type shapes from (default: $ANDROID_HOME/platforms/android-36/android.jar;
 * add androidx.preference's classes.jar for its types). Shape jars are only read, never put on a classpath.
 * index.txt (all repo entries) and scan.md (results) land in MIHON_SCAN_DIR.
 */
class MihonExtensionScanTest {

    @Test fun scanKeiyoushiExtensions() = runBlocking {
        assumeTrue("set MIHON_SCAN=1 to run", System.getenv("MIHON_SCAN") == "1")
        val dir = File(System.getenv("MIHON_SCAN_DIR") ?: Files.createTempDirectory("mihonscan").toString()).apply { mkdirs() }
        MihonRuntime.init(Application("mihon-scan", File(dir, "data")))
        val shapes = ExtensionRefScanner.ShapeIndex(ExtensionRefScanner.shapeJars("MIHON_SCAN_SHAPE_JARS"))
        val http = HttpClient(OkHttp)
        try {
            val remotes = MihonRepoClient(http).fetch(listOf(REPO_URL))
            File(dir, "index.txt").writeText(remotes.joinToString("\n") { "${it.lang}\t${it.name}\t${it.packageName}" })
            val wanted = System.getenv("MIHON_SCAN_PACKAGES")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: DEFAULT_PACKAGES

            val apks = mutableListOf<Pair<String, File?>>()
            wanted.forEach { pkg ->
                val remote = remotes.firstOrNull { it.packageName == pkg }
                if (remote == null) {
                    apks += "$pkg (not in repo)" to null
                    return@forEach
                }
                val apk = File(dir, "apks/$pkg.apk")
                if (!apk.exists()) {
                    val response = http.get(remote.downloadLink)
                    check(response.status.isSuccess()) { "GET ${remote.downloadLink} -> ${response.status}" }
                    apk.parentFile.mkdirs()
                    apk.writeBytes(response.readRawBytes())
                }
                apks += "${remote.name} ${remote.version} (${remote.lang}, $pkg)" to apk
            }
            System.getenv("MIHON_SCAN_EXTRA_APKS")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach {
                apks += "local ${File(it).name}" to File(it)
            }

            val report = StringBuilder("| Extension | Missing types | Missing members | Verify failures |\n|---|---|---|---|\n")
            val typeTotals = sortedMapOf<String, MutableSet<String>>()
            val memberTotals = sortedMapOf<String, MutableSet<String>>()
            apks.forEach { (label, apk) ->
                if (apk == null) {
                    report.append("| $label | | | |\n")
                    return@forEach
                }
                val jar = DexConverter.convert(apk, File(dir, "jars"))
                if (jar == null) {
                    report.append("| $label | conversion failed | | |\n")
                    return@forEach
                }
                val result = scan(jar)
                result.types.forEach { typeTotals.getOrPut(it) { sortedSetOf() } += label.substringBefore(" (") }
                result.members.forEach { memberTotals.getOrPut(it) { sortedSetOf() } += label.substringBefore(" (") }
                fun cell(items: Collection<String>) = items.joinToString("<br>").ifEmpty { "none" }
                report.append("| $label | ${cell(result.types)} | ${cell(result.members)} | ${result.verified} verified; ${cell(result.verifyFailures)} |\n")
            }
            report.append("\n| Missing type | Real shape | Extensions |\n|---|---|---|\n")
            typeTotals.forEach { (ref, names) ->
                report.append("| $ref | ${shapes.describe(ref.substringBefore(' '))} | ${names.size}: ${names.joinToString()} |\n")
            }
            report.append("\n| Missing member | Extensions |\n|---|---|\n")
            memberTotals.forEach { (ref, names) -> report.append("| $ref | ${names.size}: ${names.joinToString()} |\n") }
            File(dir, "scan.md").writeText(report.toString())
            println(report)
            println("Scan written to ${File(dir, "scan.md")}")
        } finally {
            http.close()
        }
    }

    private class ScanResult(val types: List<String>, val members: List<String>, val verified: Int, val verifyFailures: List<String>)

    private val host: ClassLoader = MihonCatalog::class.java.classLoader

    private fun scan(jar: File): ScanResult {
        val classes = ZipFile(jar).use { zip ->
            zip.entries().toList().filter { it.name.endsWith(".class") }.map { e -> zip.getInputStream(e).use { it.readBytes() } }
        }
        val readers = classes.map(::ClassReader)
        val supers = readers.associate { it.className to it.superName }
        fun isActivity(name: String): Boolean {
            var current: String? = name
            repeat(supers.size + 1) {
                if (current == null) return false
                if (current == "android/app/Activity") return true
                current = supers[current]
            }
            return false
        }

        // ref -> referencing classes
        val refs = sortedMapOf<String, MutableSet<String>>()
        readers.forEach { reader ->
            ExtensionRefScanner.references(reader).forEach { refs.getOrPut(it) { mutableSetOf() } += reader.className }
        }
        fun label(ref: String) = if (refs.getValue(ref).all(::isActivity)) "$ref (Activity only)" else ref
        val unresolved = refs.keys.filter { ref -> ref.substringBefore(' ') !in supers && !ExtensionRefScanner.resolves(ref, host) }
        val types = unresolved.filter { ' ' !in it }
        // A member of a missing type is implied by the type entry.
        val members = unresolved.filter { ' ' in it && it.substringBefore(' ') !in types }

        var verified = 0
        val failures = mutableListOf<String>()
        ChildFirstClassLoader(arrayOf(jar.toURI().toURL()), host, ChildFirstClassLoader.BASE_PARENT_FIRST).use { loader ->
            readers.map { it.className }.filterNot(::isActivity).forEach { name ->
                runCatching { Class.forName(name.replace('/', '.'), true, loader) }
                    .onSuccess { verified++ }
                    .onFailure { failures += "$name: ${it.javaClass.simpleName}: ${it.message?.lineSequence()?.first()}" }
            }
        }
        return ScanResult(types.map(::label), members.map(::label), verified, failures)
    }

    private companion object {
        const val REPO_URL = "https://raw.githubusercontent.com/keiyoushi/extensions/repo"
        // Mix of API, ParsedHttpSource/Madara, multi-language, and image-descrambling extensions.
        val DEFAULT_PACKAGES = listOf(
            "all.mangadex", "all.mangaplus", "all.mangafire", "all.pixiv", "all.webtoons", "all.toomics",
            "all.comikey", "all.komga", "all.cubari", "all.kagane",
            "en.mangapill", "en.asurascans", "en.flamecomics", "en.mangakatana", "en.mangago", "en.manganelo",
            "en.mangatown", "en.tapastic", "en.weebcentral", "en.hiperdex", "en.toonily",
            "fr.japscan", "en.cmanhua", "es.lectormonline",
        ).map { "eu.kanade.tachiyomi.extension.$it" }
    }
}
