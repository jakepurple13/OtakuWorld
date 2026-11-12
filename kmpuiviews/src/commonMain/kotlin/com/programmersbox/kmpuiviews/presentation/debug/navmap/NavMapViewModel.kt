package com.programmersbox.kmpuiviews.presentation.debug.navmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NavMapMode { Map, Focus }

data class NavMapState(
    val loading: Boolean = true,
    val graph: NavGraph? = null,
    val layout: GraphLayout? = null,
    val mode: NavMapMode = NavMapMode.Map,
    val selected: String? = null,
    val trail: List<String> = emptyList(),
    val query: String = "",
    val showEntryPoints: Boolean = true,
    val showArgs: Boolean = true,
    val overridesOnly: Boolean = false,
    val orphansOnly: Boolean = false,
)

private const val MAX_TRAIL = 6

class NavMapViewModel(source: NavGraphSource) : ViewModel() {

    private val _state = MutableStateFlow(NavMapState())
    val state: StateFlow<NavMapState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val graph = runCatching { source.read()?.let(::parseNavGraph) }
                .onFailure { it.printStackTrace() }
                .getOrNull()
            _state.update { it.copy(loading = false, graph = graph, layout = graph?.let(::layoutGraph)) }
        }
    }

    fun select(id: String?) = _state.update { it.copy(selected = id) }

    /** Opens Focus mode on [id] with a fresh trail. */
    fun focus(id: String) = _state.update { it.copy(mode = NavMapMode.Focus, trail = listOf(id), selected = id) }

    /** Steps from the focused screen to [id], keeping the last [MAX_TRAIL] screens. */
    fun walk(id: String) = _state.update { it.copy(trail = (it.trail + id).takeLast(MAX_TRAIL), selected = id) }

    fun jumpTrail(index: Int) = _state.update { s ->
        val trail = s.trail.take(index + 1)
        s.copy(trail = trail, selected = trail.lastOrNull())
    }

    fun setMode(mode: NavMapMode) = _state.update { s ->
        if (mode == NavMapMode.Focus && s.trail.isEmpty()) {
            val start = s.selected ?: s.graph?.nodes?.firstOrNull { !it.pseudo }?.id
            s.copy(mode = mode, trail = listOfNotNull(start))
        } else {
            s.copy(mode = mode)
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }
    fun toggleEntryPoints() = _state.update { it.copy(showEntryPoints = !it.showEntryPoints) }
    fun toggleArgs() = _state.update { it.copy(showArgs = !it.showArgs) }
    fun toggleOverridesOnly() = _state.update { it.copy(overridesOnly = !it.overridesOnly) }
    fun toggleOrphansOnly() = _state.update { it.copy(orphansOnly = !it.orphansOnly) }

    /** First node whose id or label contains the query, ignoring case. */
    fun firstMatch(): String? {
        val s = _state.value
        val q = s.query.trim()
        if (q.isEmpty()) return null
        return s.graph?.nodes?.firstOrNull { it.id.contains(q, ignoreCase = true) || it.label.contains(q, ignoreCase = true) }?.id
    }
}
