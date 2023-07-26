package com.programmersbox.extensionloader

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import com.programmersbox.models.ApiService
import com.programmersbox.models.ApiServicesCatalog
import com.programmersbox.models.ExternalApiServicesCatalog
import com.programmersbox.models.SourceInformation
import kotlinx.coroutines.runBlocking


private const val METADATA_NAME = "programmersbox.otaku.name"
private const val METADATA_CLASS = "programmersbox.otaku.class"
private const val EXTENSION_FEATURE = "programmersbox.otaku.extension"

class SourceLoader(
    application: Application,
    private val context: Context,
    sourceType: String,
    private val sourceRepository: SourceRepository
) {
    private val extensionLoader = ExtensionLoader<Any, List<SourceInformation>>(
        context,
        "$EXTENSION_FEATURE.$sourceType",
        METADATA_CLASS,
    ) { t, a, p ->
        when (t) {
            is ApiService -> listOf(
                SourceInformation(
                    apiService = t,
                    name = a.metaData.getString(METADATA_NAME) ?: "Nothing",
                    icon = context.packageManager.getApplicationIcon(p.packageName),
                    packageName = p.packageName,
                )
            )

            is ExternalApiServicesCatalog -> {
                runBlocking { t.initialize(application) }
                t.getSources().map { it.copy(catalog = t) }
            }

            is ApiServicesCatalog -> t.createSources().map {
                SourceInformation(
                    apiService = it,
                    name = a.metaData.getString(METADATA_NAME) ?: "Nothing",
                    icon = context.packageManager.getApplicationIcon(p.packageName),
                    packageName = p.packageName,
                    catalog = t,
                )
            }

            else -> emptyList()
        }
    }

    private val PACKAGE_FLAGS =
        PackageManager.GET_CONFIGURATIONS or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    private val extensionType = "$EXTENSION_FEATURE.$sourceType"

    init {
        val uninstallApplication: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.dataString == null) return

                val packageString = intent.dataString.orEmpty().removePrefix("package:")
                val isNotOtakuExtension = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context?.packageManager?.getPackageInfo(packageString, PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
                    } else {
                        context?.packageManager?.getPackageInfo(packageString, PACKAGE_FLAGS)
                    }?.reqFeatures?.any { it.name == extensionType } == false
                }.getOrDefault(false)

                if (isNotOtakuExtension) return

                when (intent.action) {
                    Intent.ACTION_PACKAGE_REPLACED -> load()
                    Intent.ACTION_PACKAGE_ADDED -> load()
                    Intent.ACTION_PACKAGE_REMOVED -> load()
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        intentFilter.addDataScheme("package")
        context.registerReceiver(uninstallApplication, intentFilter)
    }

    fun load() {
        sourceRepository.setSources(extensionLoader.loadExtensions().flatten().sortedBy { it.apiService.serviceName })
    }

    suspend fun blockingLoad() {
        sourceRepository.setSources(extensionLoader.loadExtensionsBlocking().flatten().sortedBy { it.apiService.serviceName })
    }
}
