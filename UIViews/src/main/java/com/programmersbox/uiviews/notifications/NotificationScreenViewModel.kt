package com.programmersbox.uiviews.notifications

import androidx.lifecycle.ViewModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers

class NotificationScreenViewModel : ViewModel() {

    val disposable = CompositeDisposable()

    fun deleteNotification(db: ItemDao, item: NotificationItem, block: () -> Unit = {}) {
        db.deleteNotification(item)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { block() }
            .addTo(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }
}