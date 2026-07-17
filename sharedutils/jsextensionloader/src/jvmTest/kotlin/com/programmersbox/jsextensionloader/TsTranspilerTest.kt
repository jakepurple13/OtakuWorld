package com.programmersbox.jsextensionloader

import kotlin.test.Test
import kotlin.test.assertEquals

class TsTranspilerTest {

    @Test
    fun stripsFunctionParameterAndReturnTypeAnnotations() {
        val ts = "function getPopular(page: number): Item[] {\n    return [];\n}"
        val expected = "function getPopular(page) {\n    return [];\n}"
        assertEquals(expected, TsTranspiler.transpile(ts))
    }

    @Test
    fun stripsMultipleParameterTypeAnnotations() {
        val ts = "function search(query: string, page: number): Item[] {\n    return [];\n}"
        val expected = "function search(query, page) {\n    return [];\n}"
        assertEquals(expected, TsTranspiler.transpile(ts))
    }

    @Test
    fun stripsInterfaceBlocksEntirely() {
        val ts = """
            interface Item {
                title: string;
                url: string;
            }
            function getPopular(page: number): Item[] {
                return [];
            }
        """.trimIndent()
        val transpiled = TsTranspiler.transpile(ts)
        assertEquals(false, transpiled.contains("interface"))
        assertEquals(true, transpiled.contains("function getPopular(page) {"))
    }

    @Test
    fun stripsTypeAliasLines() {
        val ts = """
            type Genre = string;
            function getPopular(page: number): Genre[] {
                return [];
            }
        """.trimIndent()
        val transpiled = TsTranspiler.transpile(ts)
        assertEquals(false, transpiled.contains("type Genre"))
    }

    @Test
    fun stripsExportKeyword() {
        val ts = "export function getPopular(page: number): Item[] {\n    return [];\n}"
        val expected = "function getPopular(page) {\n    return [];\n}"
        assertEquals(expected, TsTranspiler.transpile(ts))
    }

    @Test
    fun stripsAsCasts() {
        val ts = "function getPopular(page: number) {\n    return (raw as Item[]);\n}"
        val expected = "function getPopular(page) {\n    return (raw);\n}"
        assertEquals(expected, TsTranspiler.transpile(ts))
    }

    @Test
    fun leavesPlainJavaScriptUnchanged() {
        val js = "function getPopular(page) {\n    return [];\n}"
        assertEquals(js, TsTranspiler.transpile(js))
    }
}
