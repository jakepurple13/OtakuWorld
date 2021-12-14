package com.programmersbox.uiviews

import android.app.assist.AssistContent
import android.net.Uri
import android.os.Bundle
import android.webkit.URLUtil
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.programmersbox.models.sourcePublish
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.appUpdateCheck
import com.programmersbox.uiviews.utils.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

abstract class BaseMainActivity : AppCompatActivity() {

    protected val disposable = CompositeDisposable()

    protected var currentNavController: LiveData<NavController>? = null

    protected val genericInfo: GenericInfo by inject()

    protected abstract fun onCreate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_main_activity)

        genericInfo.toSource(currentService.orEmpty())?.let { sourcePublish.onNext(it) }

        if (savedInstanceState == null) {
            setupBottomNavBar()
        }

        when (runBlocking { themeSetting.first() }) {
            "System" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            "Light" -> AppCompatDelegate.MODE_NIGHT_NO
            "Dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> null
        }?.let(AppCompatDelegate::setDefaultNightMode)

        onCreate()

        intent.data?.let {
            if (URLUtil.isValidUrl(it.toString())) {
                currentService?.let { it1 ->
                    genericInfo.toSource(it1)?.getSourceByUrl(it.toString())
                        ?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribeBy { it2 ->
                            currentNavController?.value?.navigate(RecentFragmentDirections.actionRecentFragment2ToDetailsFragment2(it2))
                        }
                        ?.addTo(disposable)
                }
            }
        }

    }

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        outContent?.webUri = Uri.parse(currentDetailsUrl)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setupBottomNavBar()
    }

    enum class Screen(val id: Int) { RECENT(R.id.recent_nav), ALL(R.id.all_nav), SETTINGS(R.id.setting_nav) }

    fun goToScreen(screen: Screen) {
        findViewById<BottomNavigationView>(R.id.navLayout2)?.selectedItemId = screen.id
    }

    private fun setupBottomNavBar() {
        val navGraphIds = listOf(R.navigation.recent_nav, R.navigation.all_nav, R.navigation.setting_nav)
        currentScreen.value = R.id.recent_nav
        val controller = findViewById<BottomNavigationView>(R.id.navLayout2)
            .also { b ->
                appUpdateCheck
                    .filter {
                        AppUpdate.checkForUpdate(
                            packageManager?.getPackageInfo(packageName, 0)?.versionName.orEmpty(),
                            it.update_real_version.orEmpty()
                        )
                    }
                    .subscribe { b.getOrCreateBadge(R.id.setting_nav).number = 1 }
                    .addTo(disposable)
            }
            .setupWithNavController(
                navGraphIds = navGraphIds,
                fragmentManager = supportFragmentManager,
                containerId = R.id.mainShows,
                intent = intent
            )

        currentNavController = controller

        Single.create<AppUpdate.AppUpdates> {
            AppUpdate.getUpdate()?.let { d -> it.onSuccess(d) } ?: it.onError(Exception("Something went wrong"))
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { }
            .subscribeBy { appUpdateCheck.onNext(it) }
            .addTo(disposable)
    }

    override fun onSupportNavigateUp(): Boolean = currentNavController?.value?.navigateUp() ?: false

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

}