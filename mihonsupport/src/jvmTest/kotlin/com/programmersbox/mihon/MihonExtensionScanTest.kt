package com.programmersbox.mihon

import com.programmersbox.kmpextensionloader.DexConverter
import com.programmersbox.mihon.repo.MihonRepoClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.io.File
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.test.Test

/**
 * Developer tool, skipped unless MIHON_SCAN=1. Downloads Keiyoushi extensions, converts each with
 * [DexConverter], and lists every class/member the converted jar references that the host classpath
 * cannot resolve (the NoClassDefFoundError/NoSuchMethodError a real call would hit).
 *
 * MIHON_SCAN_DIR keeps downloads between runs (default: a temp dir). MIHON_SCAN_PACKAGES overrides the
 * comma-separated package list. index.txt (all repo entries) and scan.md (results) land in that dir.
 */
class MihonExtensionScanTest {

    @Test fun scanKeiyoushiExtensions() = runBlocking {
        assumeTrue("set MIHON_SCAN=1 to run", System.getenv("MIHON_SCAN") == "1")
        val dir = File(System.getenv("MIHON_SCAN_DIR") ?: Files.createTempDirectory("mihonscan").toString()).apply { mkdirs() }
        val http = HttpClient(OkHttp)
        try {
            val remotes = MihonRepoClient(http).fetch(listOf(REPO_URL))
            File(dir, "index.txt").writeText(remotes.joinToString("\n") { "${it.lang}\t${it.name}\t${it.packageName}" })
            val wanted = System.getenv("MIHON_SCAN_PACKAGES")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: DEFAULT_PACKAGES

            val report = StringBuilder("| Extension | Unresolved references |\n|---|---|\n")
            val totals = sortedMapOf<String, MutableSet<String>>()
            wanted.forEach { pkg ->
                val remote = remotes.firstOrNull { it.packageName == pkg }
                if (remote == null) {
                    report.append("| $pkg | not in repo |\n")
                    return@forEach
                }
                val apk = File(dir, "apks/$pkg.apk")
                if (!apk.exists()) {
                    val response = http.get(remote.downloadLink)
                    check(response.status.isSuccess()) { "GET ${remote.downloadLink} -> ${response.status}" }
                    apk.parentFile.mkdirs()
                    apk.writeBytes(response.readRawBytes())
                }
                val jar = DexConverter.convert(apk, File(dir, "jars"))
                val missing = if (jar == null) listOf("conversion failed") else unresolved(jar)
                missing.forEach { totals.getOrPut(it) { sortedSetOf() } += remote.name }
                report.append("| ${remote.name} (${remote.lang}, $pkg) | ${missing.joinToString("<br>").ifEmpty { "none" }} |\n")
            }
            report.append("\n| Unresolved reference | Extensions |\n|---|---|\n")
            totals.forEach { (ref, names) -> report.append("| $ref | ${names.size}: ${names.joinToString()} |\n") }
            File(dir, "scan.md").writeText(report.toString())
            println(report)
            println("Scan written to ${File(dir, "scan.md")}")
        } finally {
            http.close()
        }
    }

    private val host: ClassLoader = MihonCatalog::class.java.classLoader

    private fun unresolved(jar: File): List<String> {
        val classes = ZipFile(jar).use { zip ->
            zip.entries().toList().filter { it.name.endsWith(".class") }.map { e -> zip.getInputStream(e).use { it.readBytes() } }
        }
        val own = classes.map { ClassReader(it).className }.toSet()
        val refs = sortedSetOf<String>()
        classes.forEach { bytes -> ClassReader(bytes).accept(RefCollector(refs), ClassReader.SKIP_FRAMES) }
        return refs.filter { ref ->
            val owner = ref.substringBefore(' ')
            owner !in own && !resolves(ref)
        }
    }

    /** "owner" for a class, "owner name desc" for a method/field (field descs never start with '('). */
    private fun resolves(ref: String): Boolean {
        val parts = ref.split(' ')
        val cls = runCatching { Class.forName(parts[0].replace('/', '.'), false, host) }.getOrNull() ?: return false
        if (parts.size == 1) return true
        val (name, desc) = parts[1] to parts[2]
        return hierarchy(cls).any { c ->
            if (desc.startsWith("(")) {
                if (name == "<init>") c == cls && c.declaredConstructors.any { Type.getConstructorDescriptor(it) == desc }
                else c.declaredMethods.any { it.name == name && Type.getMethodDescriptor(it) == desc && (c == cls || !Modifier.isPrivate(it.modifiers)) }
            } else {
                c.declaredFields.any { it.name == name && Type.getDescriptor(it.type) == desc }
            }
        }
    }

    private fun hierarchy(cls: Class<*>): Sequence<Class<*>> = sequence {
        val seen = mutableSetOf<Class<*>>()
        val queue = ArrayDeque(listOf(cls))
        while (queue.isNotEmpty()) {
            val c = queue.removeFirst()
            if (!seen.add(c)) continue
            yield(c)
            c.superclass?.let(queue::add)
            queue.addAll(c.interfaces)
        }
        yield(Any::class.java)
    }

    private class RefCollector(private val refs: MutableSet<String>) : ClassVisitor(Opcodes.ASM9) {
        private fun type(internalName: String?) {
            if (internalName == null) return
            val t = if (internalName.startsWith("[")) Type.getType(internalName).elementType else Type.getObjectType(internalName)
            if (t.sort == Type.OBJECT) refs += t.internalName
        }

        override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<String>?) {
            type(superName)
            interfaces?.forEach(::type)
        }

        override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<String>?): MethodVisitor =
            object : MethodVisitor(Opcodes.ASM9) {
                override fun visitTypeInsn(opcode: Int, type: String) = type(type)

                override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
                    type(owner)
                    if (!owner.startsWith("[")) refs += "$owner $name $descriptor"
                }

                override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
                    type(owner)
                    if (!owner.startsWith("[")) refs += "$owner $name $descriptor"
                }

                override fun visitLdcInsn(value: Any?) {
                    if (value is Type && value.sort == Type.OBJECT) refs += value.internalName
                }
            }
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
