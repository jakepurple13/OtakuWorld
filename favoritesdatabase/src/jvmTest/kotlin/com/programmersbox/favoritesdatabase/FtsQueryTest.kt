package com.programmersbox.favoritesdatabase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FtsQueryTest {

    @Test fun `single word becomes quoted prefix`() = assertEquals("\"blue\"*", ftsMatch("blue"))

    @Test fun `words AND together`() = assertEquals("\"blu\"* \"lo\"*", ftsMatch("  blu   lo "))

    @Test fun `fts syntax is stripped`() =
        assertEquals("\"a\"* \"b\"* \"NEAR\"* \"c\"*", ftsMatch("a\"* -b: NEAR(c)"))

    @Test fun `blank or symbols only gives null`() {
        assertNull(ftsMatch(""))
        assertNull(ftsMatch(" *\"- "))
    }

    @Test fun `column filter per phrase`() =
        assertEquals("content : \"tour\"* content : \"arc\"*", ftsMatch("tour arc", column = "content"))

    @Test fun `unicode letters kept`() = assertEquals("\"進撃\"* \"café\"*", ftsMatch("進撃 café"))

    // unicode61 keeps combining marks inside tokens (Devanagari, Thai)
    @Test fun `combining marks stay in the word`() = assertEquals("\"हिन्दी\"*", ftsMatch("हिन्दी"))

    @Test fun `ftsMatchAny joins keywords with OR`() =
        assertEquals("(\"slime\"*) OR (\"found\"* \"family\"*)", ftsMatchAny(listOf("slime", "found family")))

    // A short AI keyword as a prefix would match far too much ("man" -> "manga", "many")
    @Test fun `ftsMatchAny keeps short words exact`() =
        assertEquals("(\"man\") OR (\"the\" \"slime\"*)", ftsMatchAny(listOf("man", "the slime")))

    @Test fun `ftsMatchAny sanitizes keywords`() =
        assertEquals("(\"a\" \"b\") OR (\"OR\")", ftsMatchAny(listOf("a\"* -b:", "OR", " *\"- ")))

    @Test fun `ftsMatchAny scopes every token to the column`() =
        assertEquals("(content : \"x\") OR (content : \"y\" content : \"z\")", ftsMatchAny(listOf("x", "y z"), column = "content"))

    @Test fun `ftsMatchAny returns null without tokens`() {
        assertNull(ftsMatchAny(emptyList()))
        assertNull(ftsMatchAny(listOf(" ", "*-")))
    }
}
