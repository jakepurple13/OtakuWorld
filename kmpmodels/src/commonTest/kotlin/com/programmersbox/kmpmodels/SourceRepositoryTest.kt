package com.programmersbox.kmpmodels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceRepositoryTest {

    private class FakeApiService(override val baseUrl: String) : KmpApiService

    private fun source(packageName: String, name: String = packageName) = KmpSourceInformation(
        apiService = FakeApiService("https://$packageName/"),
        name = name,
        icon = null,
        packageName = packageName,
    )

    @Test
    fun setSourcesLeavesExternallyAddedEntriesAlone() {
        val repository = SourceRepository()
        repository.addSource(source("js.example"))

        repository.setSources(listOf(source("com.legacy.one")))

        val packageNames = repository.list.map { it.packageName }.toSet()
        assertEquals(setOf("js.example", "com.legacy.one"), packageNames)
    }

    @Test
    fun setSourcesRemovesEntriesNoLongerInTheNewManagedList() {
        val repository = SourceRepository()
        repository.setSources(listOf(source("com.legacy.one"), source("com.legacy.two")))

        repository.setSources(listOf(source("com.legacy.one")))

        assertEquals(listOf("com.legacy.one"), repository.list.map { it.packageName })
    }

    @Test
    fun setSourcesReplacesAnEntryWithTheSamePackageNameRatherThanDuplicatingIt() {
        val repository = SourceRepository()
        repository.setSources(listOf(source("com.legacy.one", name = "Old Name")))

        repository.setSources(listOf(source("com.legacy.one", name = "New Name")))

        assertEquals(1, repository.list.size)
        assertEquals("New Name", repository.list.first().name)
    }

    @Test
    fun setSourcesNeverTouchesAnEntryItNeverManaged() {
        val repository = SourceRepository()
        repository.addSource(source("js.example"))
        repository.setSources(listOf(source("com.legacy.one")))

        // A later rescan that no longer mentions com.legacy.one at all (e.g. that source was
        // uninstalled) should drop it - but js.example, never part of any setSources call,
        // must never be touched no matter how many rescans happen.
        repository.setSources(emptyList())

        assertEquals(listOf("js.example"), repository.list.map { it.packageName })
    }

    @Test
    fun addSourceAndRemoveSourceStillWorkAsBefore() {
        val repository = SourceRepository()
        val info = source("com.example")

        repository.addSource(info)
        assertTrue(repository.list.contains(info))

        repository.removeSource(info)
        assertTrue(repository.list.isEmpty())
    }
}
