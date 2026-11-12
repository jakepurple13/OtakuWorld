package com.programmersbox.navgraph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class KotlinSourceTest {

    @Test
    fun stripsCommentsAndStrings() {
        val text = "val a = \"navigate(Screen.X) {\" // navigate(Y) {\n/* { */ fun f() {}"
        val src = KotlinSource("A.kt", text)

        assertFalse(src.code.contains("navigate"))
        assertEquals(text.length, src.code.length)
        val open = text.indexOf("fun f() {") + "fun f() ".length
        assertEquals(text.lastIndexOf('}'), src.matchingBrace(open))
    }

    @Test
    fun keepsNewlinesAndRawStrings() {
        val text = "val a = \"\"\"\n{ navigate(Q)\n\"\"\"\nval b = 'x'\nfun g() { }"
        val src = KotlinSource("A.kt", text)

        assertFalse(src.code.contains("navigate"))
        assertEquals(text.count { it == '\n' }, src.code.count { it == '\n' })
        val open = text.indexOf("{ }")
        assertEquals(open + 2, src.matchingBrace(open))
    }

    @Test
    fun lineOf() {
        val text = "a\nb\nccc"
        val src = KotlinSource("A.kt", text)
        assertEquals(3, src.lineOf(text.indexOf("ccc")))
        assertEquals(1, src.lineOf(0))
    }

    @Test
    fun packageName() {
        assertEquals("p.q", KotlinSource("A.kt", "// hi\npackage p.q\n\nclass A").packageName)
    }
}
