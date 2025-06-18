package com.programmersbox.kmpuiviews.repository

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.SourceOrder
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import com.programmersbox.kmpuiviews.utils.dispatchIo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

class SetupRepository(
    private val sourceRepository: SourceRepository,
    private val itemDao: ItemDao,
    private val dataStoreHandling: DataStoreHandling,
    private val appUpdateCheck: AppUpdateCheck,
    private val currentSourceRepository: CurrentSourceRepository,
) {
    fun setup(scope: CoroutineScope) {
        sourceRepository
            .sources
            .onEach {
                it.forEachIndexed { index, sourceInformation ->
                    itemDao.insertSourceOrder(
                        SourceOrder(
                            source = sourceInformation.packageName,
                            name = sourceInformation.apiService.serviceName,
                            order = index
                        )
                    )
                }
            }
            .launchIn(scope)

        dataStoreHandling
            .currentService
            .asFlow()
            .mapNotNull {
                if (it == null) {
                    sourceRepository
                        .list
                        .filter { c -> c.catalog == null }
                        .randomOrNull()
                } else {
                    sourceRepository.toSourceByApiServiceName(it)
                }
            }
            .onEach { currentSourceRepository.emit(it.apiService) }
            .launchIn(scope)

        flow { emit(AppUpdate.getUpdate()) }
            .catch { emit(null) }
            .dispatchIo()
            .onEach(appUpdateCheck.updateAppCheck::emit)
            .launchIn(scope)
    }
}