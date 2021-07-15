package com.programmersbox.animeworldtv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.programmersbox.anime_sources.Sources
import com.programmersbox.models.sourcePublish
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers

/**
 * Loads [MainFragment].
 */
class MainActivity : FragmentActivity() {

    private val disposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (currentService == null) {
            val s = Sources.values().random()
            sourcePublish.onNext(s)
            currentService = s.serviceName
        } else if (currentService != null) {
            try {
                Sources.valueOf(currentService!!)
            } catch (e: IllegalArgumentException) {
                null
            }?.let(sourcePublish::onNext)
        }

        Single.create<AppUpdate.AppUpdates> { AppUpdate.getUpdate()?.let(it::onSuccess) ?: it.onError(Throwable("Something went wrong")) }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .doOnError {}
            .subscribe(appUpdateCheck::onNext)
            .addTo(disposable)

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}