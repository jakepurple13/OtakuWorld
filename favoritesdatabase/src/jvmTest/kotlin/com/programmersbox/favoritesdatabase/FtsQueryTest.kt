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
}
