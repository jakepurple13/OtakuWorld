package com.programmersbox.uiviews.presentation.recent

import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.SourceInformation
import com.programmersbox.uiviews.repository.CurrentSourceRepository
import com.programmersbox.uiviews.repository.FavoritesRepository
import com.programmersbox.uiviews.utils.combineSources
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.fireListener
import com.programmersbox.uiviews.utils.recordFirebaseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.beryukhov.reactivenetwork.ReactiveNetwork

class RecentViewModel(
    dao: ItemDao,
    sourceRepository: SourceRepository,
    currentSourceRepository: CurrentSourceRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    var isRefreshing by mutableStateOf(false)
    private val sourceList = mutableStateListOf<ItemModel>()
    val favoriteList = mutableStateListOf<DbModel>()

    val filteredSourceList by derivedStateOf { sourceList.distinctBy { it.url } }

    @SuppressLint("MissingPermission")
    val observeNetwork = ReactiveNetwork()
        .observeInternetConnectivity()
        .flowOn(Dispatchers.IO)

    var count = 1

    private val itemListener = fireListener()

    var currentSource by mutableStateOf<ApiService?>(null)

    val gridState = LazyGridState(0, 0)

    val sources = mutableStateListOf<SourceInformation>()

    val snackbarHostState = SnackbarHostState()

    var isIncognitoSource by mutableStateOf(false)

    init {
        combineSources(sourceRepository, dao)
            .onEach {
                sources.clear()
                sources.addAll(it)
            }
            .launchIn(viewModelScope)

        favoritesRepository
            .getAllFavorites(itemListener)
            .onEach {
                favoriteList.clear()
                favoriteList.addAll(it)
            }
            .launchIn(viewModelScope)

        currentSourceRepository.asFlow()
            .filterNotNull()
            .onEach {
                currentSource = it
                count = 1
                sourceList.clear()
                sourceLoadCompose()
            }
            .launchIn(viewModelScope)

        snapshotFlow { currentSource }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { gridState.scrollToItem(0) }
            .launchIn(viewModelScope)

        snapshotFlow { currentSource }
            .filterNotNull()
            .flatMapMerge { dao.getIncognitoSourceByName(it.serviceName) }
            .onEach { isIncognitoSource = it?.isIncognito ?: false }
            .launchIn(viewModelScope)

        observeNetwork
            .onEach { if (sourceList.isEmpty() && currentSource != null && it && count != 1) reset() }
            .launchIn(viewModelScope)
    }

    fun reset() {
        count = 1
        sourceList.clear()
        sourceLoadCompose()
    }

    fun loadMore() {
        count++
        sourceLoadCompose()
    }

    private fun sourceLoadCompose() {
        currentSource
            ?.getRecentFlow(count)
            ?.onStart { isRefreshing = true }
            ?.dispatchIo()
            ?.catch {
                it.printStackTrace()
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar(
                        "Something went wrong",
                        withDismissAction = true
                    )
                }
                emit(emptyList())
                recordFirebaseException(it)
            }
            ?.onCompletion { isRefreshing = false }
            ?.onEach { sourceList.addAll(it) }
            ?.launchIn(viewModelScope)
    }

    fun favoriteAction(action: FavoriteAction) {
        when (action) {
            is FavoriteAction.Add -> {
                viewModelScope.launch {
                    favoritesRepository.addFavorite(action.info.toDbModel())
                }
            }

            is FavoriteAction.Remove -> {
                viewModelScope.launch {
                    favoritesRepository.removeFavorite(action.info.toDbModel())
                }
            }
        }
    }

    sealed class FavoriteAction {
        data class Add(val info: ItemModel) : FavoriteAction()
        data class Remove(val info: ItemModel) : FavoriteAction()
    }
}