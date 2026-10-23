package com.programmersbox.kmpextensionloader

import com.googlecode.d2j.DexConstants
import com.googlecode.d2j.Method
import com.googlecode.d2j.reader.BaseDexFileReader
import com.googlecode.d2j.reader.Op
import com.googlecode.d2j.visitors.DexCodeVisitor
import com.googlecode.d2j.visitors.DexFileVisitor
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ElidedConstructorFixTest {
    private val root = Files.createTempDirectory("elidedctor").toFile()

    @AfterTest fun cleanup() {
        root.deleteRecursively()
    }

    @Test fun ancestorPredicate() {
        val supers = mapOf(
            "Lt/Foo;" to OBJECT,
            "Lt/Sub;" to "Lt/Foo;",
            "Lt/Lib;" to "Lokhttp3/Base;",
            "Lt/LoopA;" to "Lt/LoopB;",
            "Lt/LoopB;" to "Lt/LoopA;",
        )
        assertTrue(ElidedConstructorFix.isAncestor(supers, "Lt/Foo;", OBJECT))
        assertTrue(ElidedConstructorFix.isAncestor(supers, "Lt/Sub;", "Lt/Foo;"))
        assertTrue(ElidedConstructorFix.isAncestor(supers, "Lt/Sub;", OBJECT))
        assertTrue(ElidedConstructorFix.isAncestor(supers, "Lt/Lib;", "Lokhttp3/Base;"))
        assertTrue(ElidedConstructorFix.isAncestor(supers, "Lt/Lib;", OBJECT))
        assertFalse(ElidedConstructorFix.isAncestor(supers, "Lt/Foo;", "Lt/Sub;"))
        assertFalse(ElidedConstructorFix.isAncestor(supers, "Lt/Foo;", "Lt/Lib;"))
        assertFalse(ElidedConstructorFix.isAncestor(supers, "Lt/Lib;", "Lokhttp3/Other;"))
        assertFalse(ElidedConstructorFix.isAncestor(supers, "Lt/Missing;", OBJECT))
        assertFalse(ElidedConstructorFix.isAncestor(supers, "Lt/LoopA;", OBJECT))
    }

    @Test fun convertedInstancesHaveTheRightType() {
        val jar = File(root, "out.jar")
        DexConverter.translate(FakeDex, jar, "fake")
        URLClassLoader(arrayOf(jar.toURI().toURL()), javaClass.classLoader).use { loader ->
            val factory = Class.forName("t.Factory", true, loader)
            fun make(name: String): String = factory.getMethod(name).invoke(null).javaClass.name

            assertEquals("t.Foo", make("makeFoo"), "elided constructor is retargeted")
            assertEquals("t.Sub", make("makeSub"), "elided constructors up the chain are added")
            assertEquals("java.lang.Object", make("makeObject"), "a genuine new Object() is untouched")
            assertEquals("t.Bar", make("makeBar"), "a class with its own constructor is untouched")
            assertEquals("java.lang.Object", make("makeAfterStaleAlias"), "an overwritten register is forgotten")
            assertEquals("t.Child", make("makeChild"), "a super call that skips an elided constructor verifies")
        }
    }

    /** A hand-built dex in the shape R8 emits. */
    private object FakeDex : BaseDexFileReader {
        override fun getDexVersion() = DexConstants.DEX_035
        override fun getClassNames() = listOf("t.Foo", "t.Sub", "t.Bar", "t.Base", "t.Child", "t.Factory")
        override fun accept(dv: DexFileVisitor) = accept(dv, 0)
        override fun accept(dv: DexFileVisitor, classIdx: Int, config: Int) = accept(dv, config)

        override fun accept(dv: DexFileVisitor, config: Int) {
            emptyClass(dv, "Lt/Foo;", OBJECT) // R8 dropped its trivial constructor
            emptyClass(dv, "Lt/Sub;", "Lt/Foo;") // same, one level down

            dv.visit(DexConstants.ACC_PUBLIC, "Lt/Bar;", OBJECT, null)?.run {
                method(DexConstants.ACC_PUBLIC or DexConstants.ACC_CONSTRUCTOR, Method("Lt/Bar;", "<init>", arrayOf(), "V"), 1) {
                    visitMethodStmt(Op.INVOKE_DIRECT, intArrayOf(0), OBJECT_INIT)
                    visitStmt0R(Op.RETURN_VOID)
                }
                visitEnd()
            }

            // R8 dropped Base's trivial constructor, so Child's constructor calls Object.<init> on `this` directly
            // (kotlinx.serialization's synthetic constructors on a sealed base look like this).
            dv.visit(DexConstants.ACC_PUBLIC or DexConstants.ACC_ABSTRACT, "Lt/Base;", OBJECT, null)?.visitEnd()
            dv.visit(DexConstants.ACC_PUBLIC, "Lt/Child;", "Lt/Base;", null)?.run {
                // 3 registers, 2 ins (this, String): `this` is v1.
                method(DexConstants.ACC_PUBLIC or DexConstants.ACC_CONSTRUCTOR, CHILD_INIT, 3) {
                    visitMethodStmt(Op.INVOKE_DIRECT, intArrayOf(1), OBJECT_INIT)
                    visitStmt0R(Op.RETURN_VOID)
                }
                visitEnd()
            }

            dv.visit(DexConstants.ACC_PUBLIC, "Lt/Factory;", OBJECT, null)?.run {
                fun factory(name: String, registers: Int, body: DexCodeVisitor.() -> Unit) =
                    method(DexConstants.ACC_PUBLIC or DexConstants.ACC_STATIC, Method("Lt/Factory;", name, arrayOf(), OBJECT), registers) {
                        body()
                        visitStmt1R(Op.RETURN_OBJECT, 0)
                    }
                factory("makeFoo", 1) {
                    visitTypeStmt(Op.NEW_INSTANCE, 0, -1, "Lt/Foo;")
                    visitMethodStmt(Op.INVOKE_DIRECT, intArrayOf(0), OBJECT_INIT)
                }
                factory("makeSub", 1) {
                    visitTypeStmt(Op.NEW_INSTANCE, 0, -1, "Lt/Sub;")
                    visitMethodStmt(Op.INVOKE_DIRECT, intArrayOf(0), OBJECT_INIT)
                }
                factory("makeObject", 1) {
                    visitTypeStmt(Op.NEW_INSTANCE, 0, -1, OBJECT)
                    visitMethodStmt(Op.INVOKE_DIRECT, intArrayOf(0), OBJECT_INIT)
                }
                factory("makeBar", 1) {
                    visitTypeStmt(Op.NEW_INSTANCE, 0, -1, "Lt/Bar;")
                    visitMethodStmt(Op.INVOKE_DIRECT, intArrayOf(0), Method("Lt/Bar;", "<init>", arrayOf(), "V"))
                }
                factory("makeChild", 2) {
                    visitTypeStmt(Op.NEW_INSTANCE, 0, -1, "Lt/Child;")
                    visitConstStmt(Op.CONST_STRING, 1, "x")
                    visitMethodStmt(Op.INVOKE_DIRECT, intArrayOf(0, 1), CHILD_INIT)
                }
                // v0 is tracked as a new Foo, then overwritten by a move before the Object.<init> call.
                factory("makeAfterStaleAlias", 3) {
                    visitTypeStmt(Op.NEW_INSTANCE, 0, -1, "Lt/Foo;")
                    visitStmt2R(Op.MOVE_OBJECT, 2, 0)
                    visitTypeStmt(Op.NEW_INSTANCE, 1, -1, OBJECT)
                    visitStmt2R(Op.MOVE_OBJECT, 0, 1)
                    visitMethodStmt(Op.INVOKE_DIRECT, intArrayOf(0), OBJECT_INIT)
                }
                visitEnd()
            }
            dv.visitEnd()
        }

        private fun emptyClass(dv: DexFileVisitor, name: String, superName: String) {
            dv.visit(DexConstants.ACC_PUBLIC, name, superName, null)?.visitEnd()
        }

        private fun com.googlecode.d2j.visitors.DexClassVisitor.method(
            access: Int,
            method: Method,
            registers: Int,
            body: DexCodeVisitor.() -> Unit,
        ) {
            val mv = visitMethod(access, method) ?: return
            mv.visitCode()?.run {
                visitRegister(registers)
                body()
                visitEnd()
            }
            mv.visitEnd()
        }
    }

    private companion object {
        const val OBJECT = "Ljava/lang/Object;"
        val OBJECT_INIT = Method(OBJECT, "<init>", arrayOf(), "V")
        val CHILD_INIT = Method("Lt/Child;", "<init>", arrayOf("Ljava/lang/String;"), "V")
    }
}
