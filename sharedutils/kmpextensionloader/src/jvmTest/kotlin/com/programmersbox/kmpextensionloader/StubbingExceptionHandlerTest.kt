package com.programmersbox.kmpextensionloader

import com.googlecode.d2j.Method
import com.googlecode.d2j.node.DexMethodNode
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StubbingExceptionHandlerTest {

    private class ByteLoader : ClassLoader(StubbingExceptionHandlerTest::class.java.classLoader) {
        fun define(name: String, bytes: ByteArray): Class<*> = defineClass(name, bytes, 0, bytes.size)
    }

    @Test fun failedMethodBecomesThrowingStubAndIsRecorded() {
        val handler = StubbingExceptionHandler("ext.apk")
        val method = Method("Lstub/Broken;", "run", arrayOf(), "V")
        val body = MethodNode(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "run", "()V", null, null)

        handler.handleMethodTranslateException(method, DexMethodNode(body.access, method), body, IllegalStateException("bad dex"))

        assertEquals(listOf("ext.apk: Lstub/Broken;.run()V: bad dex"), handler.failures)

        // The stub must be a valid body that throws when called.
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "stub/Broken", null, "java/lang/Object", null)
        body.accept(cw)
        cw.visitEnd()
        val cls = ByteLoader().define("stub.Broken", cw.toByteArray())
        val thrown = assertFailsWith<InvocationTargetException> { cls.getMethod("run").invoke(null) }
        val cause = assertIs<RuntimeException>(thrown.cause)
        assertTrue(cause.message.orEmpty().startsWith("d2j fail translate: "))
    }

    @Test fun fileFailureIsRecordedWithoutThrowing() {
        val handler = StubbingExceptionHandler("ext.apk")
        handler.handleFileException(RuntimeException("Method code too large!"))
        assertEquals(listOf("ext.apk: Method code too large!"), handler.failures)
    }
}
