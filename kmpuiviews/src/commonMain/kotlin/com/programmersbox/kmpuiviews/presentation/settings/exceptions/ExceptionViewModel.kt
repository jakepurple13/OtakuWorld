package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.ExceptionItem
import kotlinx.coroutines.launch

class ExceptionViewModel(
    private val exceptionDao: ExceptionDao,
) : ViewModel() {
    val exceptions = exceptionDao.getAllExceptions()

    fun deleteAll() {
        viewModelScope.launch {
            exceptionDao.deleteAll()
        }
    }

    fun deleteItem(exceptionItem: ExceptionItem) {
        viewModelScope.launch {
            exceptionDao.deleteException(exceptionItem)
        }
    }
}