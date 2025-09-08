package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.SystemAlerter
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ListRepository(
    private val listDao: ListDao,
    private val systemAlerter: SystemAlerter,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun addList(name: String) {
        val item = CustomListItem(
            uuid = Uuid.random().toString(),
            name = name,
        )
        listDao.createList(item)
    }

    suspend fun addToList(uuid: String, title: String, description: String, url: String, imageUrl: String, source: String): Boolean {
        return listDao.addToList(uuid, title, description, url, imageUrl, source)
            .also { systemAlerter.alertListChange() }
    }

    suspend fun create(name: String) {
        listDao.create(name)
        systemAlerter.alertListChange()
    }

    suspend fun updateFullList(item: CustomListItem) {
        listDao.updateFullList(item)
        systemAlerter.alertListChange()
    }

    suspend fun removeList(item: CustomList) {
        listDao.removeList(item)
        systemAlerter.alertListChange()
    }

    suspend fun addItem(customListItem: CustomListInfo) {
        listDao.addItem(customListItem)
        systemAlerter.alertListItemChange()
    }

    suspend fun createList(listItem: CustomListItem): Long {
        return listDao.createList(listItem)
            .also { systemAlerter.alertListChange() }
    }

    suspend fun removeItem(customListItem: CustomListInfo) {
        listDao.removeItem(customListItem)
        systemAlerter.alertListItemChange()
    }

    suspend fun updateBiometric(uuid: String, useBiometric: Boolean) {
        listDao.updateBiometric(uuid, useBiometric)
        systemAlerter.alertListChange()
    }
}