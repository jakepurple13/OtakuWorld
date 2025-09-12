package com.programmersbox.otakuworld

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.programmersbox.otakuworld.info.InfoViewModel
import com.programmersbox.otakuworld.repository.OtakuRepository
import com.programmersbox.otakuworld.repository.ServerHandler
import com.programmersbox.otakuworld.repository.ServerHandling
import com.programmersbox.otakuworld.settings.SettingsViewModel
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

class PlaygroundApp : Application() {
    override fun onCreate() {
        super.onCreate()
        //TODO: This acts funky if user enabled force dark mode from developer options
        DynamicColors.applyToActivitiesIfAvailable(this)

        startKoin {
            androidContext(this@PlaygroundApp)
            loadKoinModules(
                module {
                    viewModelOf(::InfoViewModel)
                    viewModelOf(::SettingsViewModel)
                    singleOf(::OtakuProvider)
                    singleOf(::OtakuRepository)
                    singleOf(::AppInfo)
                    singleOf(::QrCodeRepository)
                    singleOf(::ServerHandling) bind ServerHandler::class
                }
            )
        }

        val otakuProvider = get<OtakuProvider>()
        val providerType = get<AppInfo>().provider

        /*AccountManager
            .get(this)
            .addOnAccountsUpdatedListener(
                object : OnAccountsUpdateListener {
                    override fun onAccountsUpdated(accounts: Array<out Account?>?) {
                        runCatching {
                            accounts?.forEach { account ->
                                account?.let {
                                    ContentResolver.setSyncAutomatically(
                                        it,
                                        otakuProvider.favoritesUri {
                                            appType = App.MangaWorld
                                            provider = providerType
                                        },
                                        true
                                    )
                                    ContentResolver.requestSync(
                                        SyncRequest.Builder()
                                            .setDisallowMetered(true)
                                            //TODO: Set extras so we can use the same sync adapter!
                                            .setSyncAdapter(
                                                it,
                                                otakuProvider.favoritesUri {
                                                    appType = App.MangaWorld
                                                    provider = providerType
                                                }
                                            )
                                            .setExtras(
                                                bundleOf(
                                                    "type" to "manga"
                                                )
                                            )
                                            .syncPeriodic(
                                                1.days.inWholeSeconds,
                                                1.hours.inWholeSeconds
                                            )
                                            .build()
                                    )
                                }
                            }
                        }
                    }
                },
                Handler(Looper.getMainLooper()),
                true,
                arrayOf(BuildConfig.ACCOUNT_TYPE)
            )*/
    }
}