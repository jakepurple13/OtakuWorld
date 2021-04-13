package com.programmersbox.uiviews

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.utils.currentService
import com.programmersbox.uiviews.utils.setupWithNavController
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

abstract class BaseMainActivity : AppCompatActivity(), GenericInfo {

    protected val disposable = CompositeDisposable()

    private var currentNavController: LiveData<NavController>? = null

    protected abstract fun onCreate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        genericInfo = this
        setContentView(R.layout.base_main_activity)

        toSource(currentService.orEmpty())?.let { sourcePublish.onNext(it) }

        if (savedInstanceState == null) {
            setupBottomNavBar()
        }

        onCreate()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setupBottomNavBar()
    }

    private fun setupBottomNavBar() {
        val navGraphIds = listOf(R.navigation.recent_nav, R.navigation.all_nav, R.navigation.setting_nav)

        val controller = findViewById<BottomNavigationView>(R.id.navLayout2)
            .also {
                GlobalScope.launch {
                    try {
                        val request = Request.Builder()
                            .url("https://github.com/jakepurple13/OtakuWorld/releases/latest")
                            .get()
                            .build()
                        @Suppress("BlockingMethodInNonBlockingContext") val response = OkHttpClient().newCall(request).execute()
                        val f = response.request().url().path.split("/").lastOrNull()?.toDoubleOrNull()
                        runOnUIThread {
                            if (packageManager?.getPackageInfo(packageName, 0)?.versionName?.toDoubleOrNull() ?: 0.0 < f ?: 0.0) {
                                it.getOrCreateBadge(R.id.setting_nav).number = 1
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
            }
            .setupWithNavController(
                navGraphIds = navGraphIds,
                fragmentManager = supportFragmentManager,
                containerId = R.id.mainShows,
                intent = intent
            )

        currentNavController = controller

        /*sourcePublish.onNext(currentSource)

        sourcePublish
            .subscribe { currentSource = it }
            .addTo(disposable)

        downloadOrStreamPublish
            .subscribe { downloadOrStream = it }
            .addTo(disposable)

        updateCheckPublish
            .subscribe { lastUpdateCheck = it }
            .addTo(disposable)*/
    }

    override fun onSupportNavigateUp(): Boolean = currentNavController?.value?.navigateUp() ?: false

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    companion object {
        var genericInfo by Delegates.notNull<GenericInfo>()
    }

}