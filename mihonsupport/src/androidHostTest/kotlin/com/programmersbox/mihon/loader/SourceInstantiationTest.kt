package com.programmersbox.mihon.loader

import com.programmersbox.mihon.FakeCatalogueSource
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TwoSourceFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(FakeCatalogueSource(), FakeCatalogueSource(false))
}

class ThrowingSource : CatalogueSource by FakeCatalogueSource() {
    init { throw NoClassDefFoundError("eu/kanade/tachiyomi/Missing") }
}

class SourceInstantiationTest {
    private val loader = javaClass.classLoader!!

    @Test
    fun resolvesRelativeClassNames() {
        assertEquals(
            listOf("com.ext.A", "com.other.B"),
            MihonSourceInstantiator.classNames("com.ext", " .A ; com.other.B"),
        )
        assertEquals(emptyList(), MihonSourceInstantiator.classNames("com.ext", null))
    }

    @Test
    fun sourceAndFactoryInstantiate() {
        val result = MihonSourceInstantiator.instantiate(
            listOf(FakeCatalogueSource::class.java.name, TwoSourceFactory::class.java.name),
            loader,
        )
        assertEquals(3, result.getOrThrow().size)
    }

    @Test
    fun missingClassFailsWithoutThrowing() {
        val result = MihonSourceInstantiator.instantiate(listOf("does.not.Exist"), loader)
        assertTrue(result.isFailure)
    }

    @Test
    fun linkageErrorFailsWithoutThrowing() {
        val result = MihonSourceInstantiator.instantiate(listOf(ThrowingSource::class.java.name), loader)
        assertTrue(result.isFailure)
    }
}
