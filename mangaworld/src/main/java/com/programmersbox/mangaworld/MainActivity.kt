package com.programmersbox.mangaworld

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.programmersbox.manga_sources.Sources
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.currentService
import io.reactivex.rxkotlin.addTo

class MainActivity : BaseMainActivity() {

    companion object {
        lateinit var activity: MainActivity
    }

    override fun onCreate() {

        activity = this

        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))

        if (currentService == null) {
            sourcePublish.onNext(Sources.NINE_ANIME)
            currentService = Sources.NINE_ANIME.serviceName
        }

        MobileAds.initialize(this)

        val bottomNav = findViewById<BottomNavigationView>(R.id.navLayout2)

        showOrHideNav
            .distinctUntilChanged()
            .subscribe {
                if (it) {
                    bottomNav?.show()
                    showSystemBars()
                } else {
                    bottomNav?.hide()
                    hideSystemBars()
                }
            }
            .addTo(disposable)
    }

    private fun hideSystemBars() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemBars() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

}