package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.domain.customserver.ListHandler
import com.programmersbox.kmpuiviews.domain.customserver.ServerRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ListRepository(
    private val listDao: ListDao,
    private val serverRepository: ServerRepository,
) {

    private val listHandler: ListHandler?
        get() = serverRepository
            .customServerHandle
            .value

    @OptIn(ExperimentalUuidApi::class)
    suspend fun addList(name: String) {
        val item = CustomListItem(
            uuid = Uuid.random().toString(),
            name = name,
        )
        runCatching { listHandler!!.addList(item) }
        listDao.createList(item)
    }

    suspend fun removeList(item: CustomList) {
        runCatching { listHandler!!.removeList(item) }
        listDao.removeList(item)
    }

    suspend fun addItem(customListItem: CustomListInfo) {
        runCatching { listHandler!!.addItem(customListItem) }
        listDao.addItem(customListItem)
    }

    suspend fun removeItem(customListItem: CustomListInfo) {
        runCatching { listHandler!!.removeItem(customListItem) }
        listDao.removeItem(customListItem)
    }

    suspend fun updateList(item: CustomListItem) {
        runCatching { listHandler!!.updateList(item) }
        listDao.updateList(item)
    }

    suspend fun updateBiometric(uuid: String, useBiometric: Boolean) {
        runCatching { listHandler!!.updateBiometric(uuid, useBiometric) }
        listDao.updateBiometric(uuid, useBiometric)
    }
}