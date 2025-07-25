package com.programmersbox.kmpuiviews.presentation.settings.incognito

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class IncognitoViewModel(
    private val itemDao: ItemDao,
    private val sourceRepository: SourceRepository,
) : ViewModel() {

    val incognitoModels = mutableStateListOf<IncognitoModel>()

    init {
        combine(
            itemDao.getAllIncognitoSources(),
            sourceRepository
                .sources
                .map { it.groupBy { it.packageName }.values.map { it.first() } }
                .map { it.filterNot { it.apiService.notWorking } }
        ) { incognitoSources, sources ->
            sources.map { source ->
                IncognitoModel(
                    incognitoSource = incognitoSources
                        .find { it.source == source.packageName }
                        ?: IncognitoSource(
                            source = source.packageName,
                            name = source.name,
                            isIncognito = false
                        ),
                    sourceInformation = source
                )
            }.sortedBy { it.sourceInformation.name }
        }
            .onEach {
                incognitoModels.clear()
                incognitoModels.addAll(it)
            }
            .launchIn(viewModelScope)
    }

    fun toggleIncognito(sourceInformation: KmpSourceInformation, value: Boolean) {
        viewModelScope.launch {
            if (itemDao.doesIncognitoSourceExistSync(sourceInformation.packageName)) {
                sourceRepository
                    .list
                    .filter { it.packageName == sourceInformation.packageName }
                    .forEach {
                        itemDao.updateIncognitoSource(
                            source = it.packageName,
                            isIncognito = value
                        )
                    }
            } else {
                sourceRepository
                    .list
                    .filter { it.packageName == sourceInformation.packageName }
                    .forEach {
                        itemDao.insertIncognitoSource(
                            IncognitoSource(
                                source = it.packageName,
                                name = it.apiService.serviceName,
                                isIncognito = value
                            )
                        )
                    }
            }
        }
    }
}

data class IncognitoModel(
    val incognitoSource: IncognitoSource,
    val sourceInformation: KmpSourceInformation,
)