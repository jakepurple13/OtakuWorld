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
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import com.programmersbox.kmpuiviews.utils.fireListener
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class AccountInfoViewModel(
    itemDao: ItemDao,
    listDao: ListDao,
    historyDao: HistoryDao,
    blurHashDao: BlurHashDao,
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
            historyDao.getAllHistoryCount(),
            listDao.getAllListsCount(),
            listDao.getAllListItemsCount(),
            itemDao.getAllChaptersCount(),
            blurHashDao.getAllHashesCount()
        ) {
            AccountInfoCount(
                cloudFavorites = it[0],
                localFavorites = it[1],
                notifications = it[2],
                incognitoSources = it[3],
                history = it[4],
                lists = it[5] - 1,
                itemsInLists = it[6],
                chapters = it[7],
                blurHashes = it[8]
            )
        }
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
) {

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
            blurHashes = 0
        )
    }
}