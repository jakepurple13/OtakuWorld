package com.programmersbox.aniyomi.loader

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.programmersbox.aniyomi.AniyomiLib
import com.programmersbox.aniyomi.AniyomiTrust
import com.programmersbox.aniyomi.UntrustedAniyomiApiService
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpSourceInformation
import dalvik.system.DelegateLastClassLoader
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

private const val EXTENSION_FEATURE = "tachiyomi.animeextension"
private const val METADATA_SOURCE_CLASS = "tachiyomi.animeextension.class"

private const val PACKAGE_FLAGS =
    PackageManager.GET_CONFIGURATIONS or PackageManager.GET_META_DATA or PackageManager.GET_SIGNING_CERTIFICATES

/**
 * @param apiServiceFactory maps a loaded source and its lib version to the service the app uses.
 */
class AniyomiExtensionLoader(
    private val context: Context,
    private val apiServiceFactory: (AnimeCatalogueSource, Double) -> KmpApiService,
) {

    @SuppressLint("QueryPermissionsNeeded")
    suspend fun load(
        catalog: KmpExternalCustomApiServicesCatalog,
        repoSigningKeys: Set<String>,
        trustedIds: Set<String>,
        onTrust: suspend (String) -> Unit,
    ): List<KmpSourceInformation> {
        val packages = installedPackages().filter { it.isAniyomiExtension() }
        loaded.keys.retainAll(packages.map { it.loadedKey() }.toSet())
        return packages.flatMap {
            // One broken package must not abort loading the rest.
            try {
                loadPackage(fullPackageInfo(it.packageName), catalog, repoSigningKeys, trustedIds, onTrust)
            } catch (t: Throwable) {
                t.printStackTrace()
                emptyList()
            }
        }
    }

    /** Sources per installed package version, so a reload reuses them instead of building a new class loader. */
    private data class LoadedKey(val packageName: String, val versionCode: Long, val lastUpdateTime: Long)

    private val loaded = ConcurrentHashMap<LoadedKey, List<KmpSourceInformation>>()

    private fun PackageInfo.loadedKey() = LoadedKey(packageName, longVersionCode, lastUpdateTime)

    fun isExtension(packageName: String): Boolean = runCatching {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_CONFIGURATIONS.toLong()))
        } else {
            pm.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS)
        }
        info.isAniyomiExtension()
    }.getOrDefault(false)

    // minSdk is 28; PackageInfoFlags is API 33+.
    // Features only: signing data and meta-data are fetched just for extensions, not for every app on the device.
    private fun installedPackages(): List<PackageInfo> {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_CONFIGURATIONS.toLong()))
        } else {
            pm.getInstalledPackages(PackageManager.GET_CONFIGURATIONS)
        }
    }

    private fun fullPackageInfo(packageName: String): PackageInfo {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pm.getPackageInfo(packageName, PACKAGE_FLAGS)
        }
    }

    private fun PackageInfo.isAniyomiExtension() = reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }

    private fun loadPackage(
        pkgInfo: PackageInfo,
        catalog: KmpExternalCustomApiServicesCatalog,
        repoSigningKeys: Set<String>,
        trustedIds: Set<String>,
        onTrust: suspend (String) -> Unit,
    ): List<KmpSourceInformation> {
        val pkg = pkgInfo.packageName
        val appInfo = pkgInfo.applicationInfo ?: return emptyList()
        val metaData = appInfo.metaData
        val versionCode = pkgInfo.longVersionCode
        val fingerprints = signatures(pkgInfo)
        val label = context.packageManager.getApplicationLabel(appInfo).toString().removePrefix("Aniyomi: ")

        fun notLoaded(reason: String, canTrust: Boolean = false) = listOf(
            KmpSourceInformation(
                apiService = UntrustedAniyomiApiService(
                    packageName = pkg,
                    versionCode = versionCode,
                    fingerprint = fingerprints.lastOrNull().orEmpty(),
                    reason = reason,
                    canTrust = canTrust,
                    onTrust = onTrust,
                ),
                name = "$label ($reason)",
                icon = null,
                packageName = pkg,
                catalog = catalog,
            )
        )

        if (metaData == null) return notLoaded("Malformed")

        val lib = AniyomiLib.libVersion({ key -> metaData.getInt(key).takeIf { metaData.containsKey(key) } }, pkgInfo.versionName)
        if (lib == null || !AniyomiLib.isSupported(lib)) return notLoaded("Unsupported lib $lib")

        if (fingerprints.isEmpty()) return notLoaded("Unsigned")
        if (!AniyomiTrust.isTrusted(pkg, versionCode, fingerprints.toSet(), repoSigningKeys, trustedIds)) {
            return notLoaded("Untrusted", canTrust = true)
        }

        val classNames = metaData.getString(METADATA_SOURCE_CLASS)
        if (classNames.isNullOrBlank()) return notLoaded("Malformed")

        // Trust was checked above, so an unchanged trusted package reuses its sources.
        loaded[pkgInfo.loadedKey()]?.let { return it }
        // Building the service reads extension-supplied id/baseUrl/name, so it is guarded too.
        return try {
            val classLoader = DelegateLastClassLoader(appInfo.sourceDir, null, context.classLoader)
            AniyomiSourceInstantiator.instantiate(classNames, pkg, classLoader).map { source ->
                val service = apiServiceFactory(source, lib)
                KmpSourceInformation(
                    apiService = service,
                    name = service.displayName,
                    icon = null,
                    packageName = pkg,
                    catalog = catalog,
                )
            }.also { loaded[pkgInfo.loadedKey()] = it }
        } catch (t: Throwable) {
            t.printStackTrace()
            notLoaded("Failed to load: ${t.message}")
        }
    }

    private fun signatures(pkgInfo: PackageInfo): List<String> {
        val signingInfo = pkgInfo.signingInfo ?: return emptyList()
        val certs = if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
        return certs.orEmpty().map { sig ->
            MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
