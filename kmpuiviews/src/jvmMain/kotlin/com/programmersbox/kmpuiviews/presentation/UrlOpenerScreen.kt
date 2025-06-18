package com.programmersbox.kmpuiviews.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.settings.ListSetting
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun UrlOpenerScreen(
    viewModel: UrlOpenerViewModel = koinViewModel(),
) {
    var url by remember { mutableStateOf("") }

    OtakuScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Url Opener") },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            ListSetting(
                value = viewModel.currentChosenSource,
                updateValue = { source, dismiss ->
                    viewModel.currentChosenSource = source
                    dismiss.value = false
                },
                options = viewModel.sourceList,
                dialogTitle = { Text("Source") },
                settingTitle = { Text("Source") },
                confirmText = {
                    TextButton(
                        onClick = { it.value = false }
                    ) { Text("Close") }
                }
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Url") }
            )

            Button(
                onClick = {

                }
            ) { Text("Open") }
        }
    }
}

class UrlOpenerViewModel(
    sourceRepository: SourceRepository,
) : ViewModel() {

    val sourceList = mutableStateListOf<KmpSourceInformation>()

    var currentChosenSource by mutableStateOf<KmpSourceInformation?>(null)

    init {
        sourceRepository
            .sources
            .onEach {
                sourceList.addAll(it.filter { it.apiService.notWorking })
                currentChosenSource = sourceList.firstOrNull()
            }
            .launchIn(viewModelScope)
    }

    fun open(url: String) {
        viewModelScope.launch {
            currentChosenSource?.let {
                it.apiService.itemInfo(
                    KmpItemModel(
                        url = url,
                        title = url,
                        description = url,
                        imageUrl = url,
                        source = it.apiService
                    )
                )
            }
        }
    }
}