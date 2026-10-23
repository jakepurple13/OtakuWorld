// Vendored from mihonapp/mihon@446f6df8 (Apache-2.0). Local edits marked with "OTAKU:".
package eu.kanade.tachiyomi.source.model

// OTAKU: removed androidx.compose.runtime.Stable import and @Stable annotation (no Compose dependency in this module)

data class FilterList(val list: List<Filter<*>>) : List<Filter<*>> by list {

    constructor(vararg fs: Filter<*>) : this(if (fs.isNotEmpty()) fs.asList() else emptyList())

    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return list.hashCode()
    }
}
