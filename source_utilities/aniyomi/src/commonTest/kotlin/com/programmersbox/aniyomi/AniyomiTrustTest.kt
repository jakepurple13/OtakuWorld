package com.programmersbox.aniyomi

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AniyomiTrustTest {
    private val pkg = "eu.kanade.tachiyomi.animeextension.en.test"

    @Test
    fun repoKeyMatchTrusts() =
        assertTrue(AniyomiTrust.isTrusted(pkg, 5, setOf("aa", "bb"), repoSigningKeys = setOf("bb"), trustedIds = emptySet()))

    @Test
    fun userTrustedIdTrusts() =
        assertTrue(AniyomiTrust.isTrusted(pkg, 5, setOf("aa"), emptySet(), setOf(AniyomiTrust.trustId(pkg, 5, "aa"))))

    @Test
    fun newVersionNeedsRetrust() =
        assertFalse(AniyomiTrust.isTrusted(pkg, 6, setOf("aa"), emptySet(), setOf(AniyomiTrust.trustId(pkg, 5, "aa"))))

    @Test
    fun unsignedNeverTrusted() =
        assertFalse(AniyomiTrust.isTrusted(pkg, 5, emptySet(), setOf(""), setOf(AniyomiTrust.trustId(pkg, 5, ""))))

    @Test
    fun unknownKeyUntrusted() =
        assertFalse(AniyomiTrust.isTrusted(pkg, 5, setOf("aa"), setOf("zz"), emptySet()))
}
