package com.programmersbox.kmpuiviews.screensaver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ListDao
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class CustomListRotationViewModel(
    listDao: ListDao,
) : ViewModel() {

    val lists: StateFlow<List<CustomList>>
        field = MutableStateFlow<List<CustomList>>(emptyList())

    var currentIndex by mutableIntStateOf(0)

    init {
        listDao
            .getAllLists()
            .onEach { list ->
                lists.update { list }
                if (currentIndex >= list.size) currentIndex = 0
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            while (isActive) {
                delay(ROTATION_INTERVAL)
                val size = lists.value.size
                if (size > 0) currentIndex = (currentIndex + 1) % size
            }
        }
    }

    companion object {
        private val ROTATION_INTERVAL = 20.seconds
    }
}
