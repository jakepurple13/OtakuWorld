package com.programmersbox.anime.shared.videochoice

import com.programmersbox.kmpmodels.KmpStorage
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerGroupingTest {

    private fun s(link: String, server: String?) = KmpStorage(link = link, server = server)

    @Test
    fun groupsInFirstAppearanceOrder() {
        val a1 = s("1", "A")
        val b = s("2", "B")
        val a2 = s("3", "A")

        val result = listOf(a1, b, a2).groupedByServer()

        assertEquals(listOf("A" to listOf(a1, a2), "B" to listOf(b)), result)
    }

    @Test
    fun nullServerIsOneGroup() {
        val items = listOf(s("1", null), s("2", null))

        val result = items.groupedByServer()

        assertEquals(listOf<Pair<String?, List<KmpStorage>>>(null to items), result)
    }

    @Test
    fun serverLabels_markFirstOfEachGroupAndCallNullServerOther() {
        val a1 = s("1", "A")
        val n = s("2", null)
        val a2 = s("3", "A")

        val result = listOf(a1, n, a2).groupedByServer().serverLabels()

        assertEquals(mapOf(a1 to "A", n to "Other"), result)
    }

    @Test
    fun serverLabels_singleGroupIsUnlabeled() {
        assertEquals(emptyMap(), listOf(s("1", "A"), s("2", "A")).groupedByServer().serverLabels())
        assertEquals(emptyMap(), listOf(s("1", null)).groupedByServer().serverLabels())
    }
}
