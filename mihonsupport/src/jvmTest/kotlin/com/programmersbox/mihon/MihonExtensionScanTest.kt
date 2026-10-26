package com.programmersbox.mihon

import android.app.Application
import com.programmersbox.kmpextensionloader.ChildFirstClassLoader
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
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
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
        val shapes = ShapeIndex(shapeJars())
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
            val found = mutableSetOf<String>()
            reader.accept(RefCollector(found), ClassReader.SKIP_FRAMES)
            found.forEach { refs.getOrPut(it) { mutableSetOf() } += reader.className }
        }
        fun label(ref: String) = if (refs.getValue(ref).all(::isActivity)) "$ref (Activity only)" else ref
        val unresolved = refs.keys.filter { ref -> ref.substringBefore(' ') !in supers && !resolves(ref) }
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

    private fun shapeJars(): List<File> {
        val explicit = System.getenv("MIHON_SCAN_SHAPE_JARS")?.split(',')?.map { File(it.trim()) }?.filter { it.isFile }
        if (!explicit.isNullOrEmpty()) return explicit
        val sdk = System.getenv("ANDROID_HOME") ?: return emptyList()
        return listOf(File(sdk, "platforms/android-36/android.jar")).filter { it.isFile }
    }

    /** Reads class shapes from jars (e.g. the SDK's android.jar) without loading them. */
    private class ShapeIndex(private val jars: List<File>) {
        fun describe(internalName: String): String {
            jars.forEach { jar ->
                ZipFile(jar).use { zip ->
                    val entry = zip.getEntry("$internalName.class") ?: return@use
                    val reader = ClassReader(zip.getInputStream(entry).use { it.readBytes() })
                    val kind = if (reader.access and Opcodes.ACC_INTERFACE != 0) "interface" else if (reader.access and Opcodes.ACC_ABSTRACT != 0) "abstract class" else "class"
                    val extends = reader.superName?.takeIf { it != "java/lang/Object" }?.let { " extends $it" }.orEmpty()
                    val implements = reader.interfaces.takeIf { it.isNotEmpty() }?.joinToString(prefix = " implements ").orEmpty()
                    return "$kind$extends$implements"
                }
            }
            return "unknown (not in shape jars)"
        }
    }

    /** Collects every referenced class type and every referenced member ("owner name desc"). */
    private class RefCollector(private val refs: MutableSet<String>) : ClassVisitor(Opcodes.ASM9) {
        private fun type(internalName: String?) {
            if (internalName == null) return
            val t = if (internalName.startsWith("[")) Type.getType(internalName).elementType else Type.getObjectType(internalName)
            if (t.sort == Type.OBJECT) refs += t.internalName
        }

        private fun desc(descriptor: String) {
            val t = Type.getType(descriptor)
            if (t.sort == Type.METHOD) {
                t.argumentTypes.forEach { desc(it.descriptor) }
                desc(t.returnType.descriptor)
            } else {
                val element = if (t.sort == Type.ARRAY) t.elementType else t
                if (element.sort == Type.OBJECT) refs += element.internalName
            }
        }

        private fun constant(value: Any?) {
            when (value) {
                is Type -> desc(value.descriptor)
                is Handle -> {
                    type(value.owner)
                    desc(value.desc)
                }
            }
        }

        override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<String>?) {
            type(superName)
            interfaces?.forEach(::type)
        }

        override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor? {
            desc(descriptor)
            return null
        }

        override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<String>?): MethodVisitor {
            desc(descriptor)
            exceptions?.forEach(::type)
            return object : MethodVisitor(Opcodes.ASM9) {
                override fun visitTypeInsn(opcode: Int, type: String) = type(type)

                override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
                    type(owner)
                    desc(descriptor)
                    if (!owner.startsWith("[")) refs += "$owner $name $descriptor"
                }

                override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
                    type(owner)
                    desc(descriptor)
                    if (!owner.startsWith("[")) refs += "$owner $name $descriptor"
                }

                override fun visitInvokeDynamicInsn(name: String, descriptor: String, bootstrapMethodHandle: Handle, vararg bootstrapMethodArguments: Any?) {
                    desc(descriptor)
                    constant(bootstrapMethodHandle)
                    bootstrapMethodArguments.forEach(::constant)
                }

                override fun visitLdcInsn(value: Any?) = constant(value)

                override fun visitMultiANewArrayInsn(descriptor: String, numDimensions: Int) = desc(descriptor)

                override fun visitTryCatchBlock(start: Label, end: Label, handler: Label, type: String?) = type(type)

                override fun visitLocalVariable(name: String, descriptor: String, signature: String?, start: Label, end: Label, index: Int) = desc(descriptor)
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
