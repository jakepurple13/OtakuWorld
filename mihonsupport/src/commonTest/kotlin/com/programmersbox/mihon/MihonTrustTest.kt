package com.programmersbox.mihon

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MihonTrustTest {
    private val pkg = "eu.kanade.tachiyomi.extension.en.test"

    @Test
    fun repoKeyMatchTrusts() =
        assertTrue(MihonTrust.isTrusted(pkg, 5, setOf("aa", "bb"), repoSigningKeys = setOf("bb"), trustedIds = emptySet()))

    @Test
    fun userTrustedIdTrusts() =
        assertTrue(MihonTrust.isTrusted(pkg, 5, setOf("aa"), emptySet(), setOf(MihonTrust.trustId(pkg, 5, "aa"))))

    @Test
    fun newVersionNeedsRetrust() =
        assertFalse(MihonTrust.isTrusted(pkg, 6, setOf("aa"), emptySet(), setOf(MihonTrust.trustId(pkg, 5, "aa"))))

    @Test
    fun unsignedNeverTrusted() =
        assertFalse(MihonTrust.isTrusted(pkg, 5, emptySet(), setOf(""), setOf(MihonTrust.trustId(pkg, 5, ""))))

    @Test
    fun unknownKeyUntrusted() =
        assertFalse(MihonTrust.isTrusted(pkg, 5, setOf("aa"), setOf("zz"), emptySet()))
}
