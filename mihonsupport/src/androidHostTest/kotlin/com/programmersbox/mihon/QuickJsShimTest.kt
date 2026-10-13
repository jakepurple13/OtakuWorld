package com.programmersbox.mihon

import app.cash.quickjs.QuickJs
import java.io.Closeable
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Zipline's native library does not load on the host JVM, so this checks the binary surface
 * extensions link against instead of calling create().
 */
class QuickJsShimTest {
    private val cls = QuickJs::class.java

    @Test
    fun createIsStaticAndReturnsQuickJs() {
        val create = cls.getMethod("create")
        assertTrue(Modifier.isStatic(create.modifiers))
        assertEquals(cls, create.returnType)
    }

    @Test
    fun evaluateOverloadsReturnObject() {
        assertEquals(Any::class.java, cls.getMethod("evaluate", String::class.java).returnType)
        assertEquals(Any::class.java, cls.getMethod("evaluate", String::class.java, String::class.java).returnType)
    }

    @Test
    fun compileAndExecuteMatchBytecodeApi() {
        assertEquals(
            ByteArray::class.java,
            cls.getMethod("compile", String::class.java, String::class.java).returnType,
        )
        assertEquals(Any::class.java, cls.getMethod("execute", ByteArray::class.java).returnType)
    }

    @Test
    fun isCloseable() {
        assertTrue(Closeable::class.java.isAssignableFrom(cls))
        cls.getMethod("close")
    }
}
