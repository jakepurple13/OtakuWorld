package com.programmersbox.kmpextensionloader

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
import java.util.zip.ZipFile

/**
 * Developer-tool core shared by the opt-in extension scan tests (`MihonExtensionScanTest`,
 * `CloudstreamPluginScanTest`): finds what a converted extension jar references that the host lacks.
 * Not used at runtime.
 *
 * A ref is "owner" for a class/interface type, or "owner name desc" for a method/field (internal names;
 * field descs never start with '(').
 */
object ExtensionRefScanner {

    /** Every class type and member [reader]'s class references (supertypes, descriptors, instructions, catch types). */
    fun references(reader: ClassReader): Set<String> {
        val found = mutableSetOf<String>()
        reader.accept(RefCollector(found), ClassReader.SKIP_FRAMES)
        return found
    }

    /** True if [ref] resolves through [host]: the class exists and, for a member, it or a supertype declares it. */
    fun resolves(ref: String, host: ClassLoader): Boolean {
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

    /**
     * Jars to read type shapes from: the comma-separated paths in env var [envVar], else
     * $ANDROID_HOME/platforms/android-36/android.jar. Missing files are dropped.
     */
    fun shapeJars(envVar: String): List<File> {
        val explicit = System.getenv(envVar)?.split(',')?.map { File(it.trim()) }?.filter { it.isFile }
        if (!explicit.isNullOrEmpty()) return explicit
        val sdk = System.getenv("ANDROID_HOME") ?: return emptyList()
        return listOf(File(sdk, "platforms/android-36/android.jar")).filter { it.isFile }
    }

    /** Reads class shapes from jars (e.g. the SDK's android.jar) without loading them or putting them on a classpath. */
    class ShapeIndex(private val jars: List<File>) {
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
}
