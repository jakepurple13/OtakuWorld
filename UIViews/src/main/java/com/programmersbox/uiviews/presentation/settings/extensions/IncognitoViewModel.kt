package com.programmersbox.uiviews.presentation.settings.extensions

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.models.SourceInformation
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
                        .find { it.source == source.apiService.serviceName }
                        ?: IncognitoSource(
                            source = source.apiService.serviceName,
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

    fun toggleIncognito(sourceInformation: SourceInformation, value: Boolean) {
        viewModelScope.launch {
            if (itemDao.doesIncognitoSourceExistSync(sourceInformation.packageName)) {
                sourceRepository
                    .list
                    .filter { it.packageName == sourceInformation.packageName }
                    .forEach {
                        itemDao.updateIncognitoSource(
                            source = it.apiService.serviceName,
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
                                source = it.apiService.serviceName,
                                name = it.name,
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
    val sourceInformation: SourceInformation,
)