package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.SystemAlerter

class IncognitoRepository(
    private val dao: ItemDao,
    private val systemAlerter: SystemAlerter,
) {
    suspend fun addIncognito(
        url: String,
        title: String,
        isIncognito: Boolean = true,
    ) {
        dao.insertIncognitoSource(
            IncognitoSource(
                source = url,
                name = title,
                isIncognito = isIncognito
            )
        )
        systemAlerter.alertIncognitoChange()
    }

    suspend fun removeIncognito(
        url: String,
    ) {
        dao.deleteIncognitoSource(url)
        systemAlerter.alertIncognitoChange()
    }

    suspend fun updateIncognito(
        url: String,
        isIncognito: Boolean,
    ) {
        dao.updateIncognitoSource(url, isIncognito)
        systemAlerter.alertIncognitoChange()
    }
}