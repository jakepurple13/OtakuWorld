package com.programmersbox.kmpuiviews.presentation.settings.accountinfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.BlurHashDao
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import com.programmersbox.kmpuiviews.utils.fireListener
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class AccountInfoViewModel(
    itemDao: ItemDao,
    listDao: ListDao,
    historyDao: HistoryDao,
    blurHashDao: BlurHashDao,
    translationModelHandler: TranslationModelHandler,
    sourceRepository: SourceRepository,
    firebaseConnection: KmpFirebaseConnection.KmpFirebaseListener,
) : ViewModel() {

    private val favoriteListener = fireListener(itemListener = firebaseConnection)

    var accountInfo by mutableStateOf(AccountInfoCount.Empty)
        private set

    init {
        combine(
            favoriteListener
                .getAllShowsFlow()
                .map { it.size },
            itemDao.getAllFavoritesCount(),
            itemDao.getAllNotificationCount(),
            itemDao.getAllIncognitoSourcesCount(),
            historyDao.getAllRecentHistoryCount(),
            listDao.getAllListsCount(),
            listDao.getAllListItemsCount(),
            itemDao.getAllChaptersCount(),
            blurHashDao.getAllHashesCount(),
            flow { emit(translationModelHandler.modelList().size) },
            sourceRepository
                .sources
                .map { list ->
                    list
                        .filterNot { it.apiService.notWorking }
                        .groupBy { it.packageName }
                        .size
                },
            historyDao.getAllHistoryCount(),
        ) { AccountInfoCount(it) }
            .onEach { accountInfo = it }
            .launchIn(viewModelScope)
    }
}

data class AccountInfoCount(
    val cloudFavorites: Int,
    val localFavorites: Int,
    val notifications: Int,
    val incognitoSources: Int,
    val history: Int,
    val lists: Int,
    val itemsInLists: Int,
    val chapters: Int,
    val blurHashes: Int,
    val translationModels: Int,
    val sourceCount: Int,
    val globalSearchHistory: Int,
) {
    constructor(array: Array<Int>) : this(
        cloudFavorites = array[0],
        localFavorites = array[1],
        notifications = array[2],
        incognitoSources = array[3],
        history = array[4],
        lists = array[5],
        itemsInLists = array[6],
        chapters = array[7],
        blurHashes = array[8],
        translationModels = array[9],
        sourceCount = array[10],
        globalSearchHistory = array[11],
    )

    val totalFavorites: Int
        get() = cloudFavorites + localFavorites

    companion object {
        val Empty = AccountInfoCount(
            cloudFavorites = 0,
            localFavorites = 0,
            notifications = 0,
            incognitoSources = 0,
            history = 0,
            lists = 0,
            itemsInLists = 0,
            chapters = 0,
            blurHashes = 0,
            translationModels = 0,
            sourceCount = 0,
            globalSearchHistory = 0,
        )
    }
}