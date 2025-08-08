package com.programmersbox.otakuworld

/*
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.models.SourceInformation
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun OtherStuffTrying() {
    val context = LocalContext.current
    val vm = viewModel { OtherViewModel(context) }

    Column {
        vm.list.forEach {
            ListItem(headlineContent = { Text(it.name) })
        }
    }
}

class OtherViewModel(context: Context) : ViewModel() {
    val sourceRepository = SourceRepository()

    val list = mutableStateListOf<SourceInformation>()

    init {
        sourceRepository.sources
            .onEach {
                list.clear()
                list.addAll(it)
            }
            .launchIn(viewModelScope)
    }
}
*/
