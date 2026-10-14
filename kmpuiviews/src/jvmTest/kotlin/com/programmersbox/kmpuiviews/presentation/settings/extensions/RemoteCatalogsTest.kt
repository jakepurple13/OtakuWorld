package com.programmersbox.kmpuiviews.presentation.settings.extensions

import com.programmersbox.kmpmodels.KmpRemoteSources
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class RemoteCatalogsTest {
    private fun remote(pkg: String) = KmpRemoteSources(pkg, pkg, "1.0.0", "", "", emptyList())

    @Test
    fun loadStoresResultAndError() = runTest {
        val catalogs = RemoteCatalogs()
        catalogs.load("ok") { listOf(remote("a")) }
        catalogs.load("bad") { error("index.min.json returned 404") }
        assertEquals(listOf("a"), assertIs<RemoteViewState>(catalogs.states["ok"]).sources.map { it.packageName })
        assertEquals("index.min.json returned 404", assertIs<RemoteErrorState>(catalogs.states["bad"]).message)
    }

    @Test
    fun retryReplacesOnlyTarget() = runTest {
        val catalogs = RemoteCatalogs()
        var fail = true
        catalogs.load("ok") { listOf(remote("a")) }
        catalogs.load("flaky") { if (fail) error("down") else listOf(remote("b")) }
        val okBefore = catalogs.states["ok"]
        fail = false
        catalogs.retry("flaky")
        assertIs<RemoteViewState>(catalogs.states["flaky"])
        assertSame(okBefore, catalogs.states["ok"])
    }

    @Test
    fun retryUnknownIsNoOp() = runTest {
        val catalogs = RemoteCatalogs()
        catalogs.retry("missing")
        assertEquals(emptyMap(), catalogs.states.toMap())
    }

    @Test
    fun firstLoadShowsLoading() = runTest {
        val catalogs = RemoteCatalogs()
        val gate = CompletableDeferred<Unit>()
        val job = launch { catalogs.load("slow") { gate.await(); listOf(remote("a")) } }
        testScheduler.runCurrent()
        assertSame(RemoteLoadingState, catalogs.states["slow"])
        gate.complete(Unit)
        job.join()
        assertIs<RemoteViewState>(catalogs.states["slow"])
    }

    @Test
    fun reloadKeepsExistingStateUntilResult() = runTest {
        val catalogs = RemoteCatalogs()
        catalogs.load("c") { listOf(remote("a")) }
        val loaded = catalogs.states["c"]
        val gate = CompletableDeferred<Unit>()
        val job = launch { catalogs.load("c") { gate.await(); listOf(remote("b")) } }
        testScheduler.runCurrent()
        assertSame(loaded, catalogs.states["c"])
        gate.complete(Unit)
        job.join()
        assertEquals(listOf("b"), assertIs<RemoteViewState>(catalogs.states["c"]).sources.map { it.packageName })
    }
}
