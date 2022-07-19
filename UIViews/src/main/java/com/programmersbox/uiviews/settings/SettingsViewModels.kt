package com.programmersbox.uiviews.settings

import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedutils.*
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.shouldCheckFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class AccountViewModel : ViewModel() {

    var accountInfo by mutableStateOf<CustomFirebaseUser?>(null)

    init {
        FirebaseAuthentication.addAuthStateListener { p0 -> accountInfo = p0 }
    }

    fun signInOrOut(context: Context, activity: ComponentActivity) {
        FirebaseAuthentication.signInOrOut(context, activity, R.string.logOut, R.string.areYouSureLogOut, R.string.yes, R.string.no)
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuthentication.clear()
    }
}

class AboutViewModel : ViewModel() {

    var canCheck by mutableStateOf(false)

    fun init(context: Context) {
        viewModelScope.launch { context.shouldCheckFlow.collect { canCheck = it } }
    }

    private val checker = AtomicBoolean(false)

    suspend fun updateChecker(context: Context) {
        try {
            if (!checker.get()) {
                checker.set(true)
                AppUpdate.getUpdate()?.let(appUpdateCheck::onNext)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            checker.set(false)
            withContext(Dispatchers.Main) { context.let { c -> Toast.makeText(c, "Done Checking", Toast.LENGTH_SHORT).show() } }
        }
    }
}

class NotificationViewModel(dao: ItemDao) : ViewModel() {

    var savedNotifications by mutableStateOf(0)
        private set

    init {
        viewModelScope.launch {
            dao.getAllNotificationCountFlow()
                .dispatchIo()
                .onEach { savedNotifications = it }
                .collect()
        }
    }

}

class GeneralViewModel : ViewModel() {

    var translationModels: List<CustomRemoteModel> by mutableStateOf(emptyList())
        private set

    fun getModels(onSuccess: () -> Unit) {
        TranslatorUtils.getModels {
            translationModels = it
            onSuccess()
        }
    }

    fun deleteModel(model: CustomRemoteModel) {
        TranslatorUtils.deleteModel(model)
    }

}