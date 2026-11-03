package com.programmersbox.mihon

import android.app.Application
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.ExceptionItem
import com.programmersbox.kmpextensionloader.ApkExtensionStore
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.mihon.repo.MihonRepoClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File
import java.util.concurrent.atomic.AtomicLong

val mihonModule = module {
    single { MihonRepoClient(HttpClient(OkHttp) { engine { config { followSslRedirects(false) } } }) }
    single {
        MihonApkStore(
            File(get<AppDirs>().getUserDataDir(), "mihon-extensions"),
            File(get<AppDirs>().getUserCacheDir(), "mihon-jars"),
            onError = exceptionDbReporter(get()),
        )
    } bind ApkExtensionStore::class
    single {
        MihonCatalog(
            Application("mihon", File(get<AppDirs>().getUserDataDir(), "mihon-data")),
            get<NewSettingsHandling>(),
            get(),
            get(),
        )
    } bind KmpExternalCustomApiServicesCatalog::class
}

/**
 * Writes failures to the exception database (Settings > Exceptions) so they are visible in packaged builds,
 * which have no console. ExceptionItem is keyed by millisecond time, so times are kept unique to stop
 * failures from back-to-back packages replacing each other.
 */
private fun exceptionDbReporter(dao: ExceptionDao): (Throwable) -> Unit {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val lastTime = AtomicLong(0)
    return { error ->
        val time = lastTime.updateAndGet { maxOf(it + 1, System.currentTimeMillis()) }
        scope.launch {
            runCatching { dao.insertException(ExceptionItem(time = time, message = error.stackTraceToString())) }
                .onFailure { it.printStackTrace() }
        }
    }
}
