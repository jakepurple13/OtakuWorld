package com.programmersbox.koogintegration.integrator

import com.programmersbox.favoritesdatabase.ListDao

class ListIntegrator(
    private val listDao: ListDao,
) : KoogIntegrator<String>() {
    override suspend fun map(input: String): String {
        return buildString {
            appendLine("Lists")
            listDao
                .getAllListsSync()
                .forEach { list ->
                    appendLine(list.item.name)
                    appendLine(" - ${list.item.description}")
                    appendLine(" - last time updated: ${list.item.time}")

                    appendLine(" - items:")
                    list
                        .list
                        .forEach { item ->
                            appendLine(item.title)
                            appendLine("  - ${item.description}")
                            appendLine("  - ${item.source}")
                        }
                }
        }
    }
}