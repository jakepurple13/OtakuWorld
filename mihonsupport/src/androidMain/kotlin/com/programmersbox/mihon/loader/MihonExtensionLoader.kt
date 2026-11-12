package com.programmersbox.mihon.loader

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.mihon.MihonCatalog
import com.programmersbox.mihon.MihonLib
import com.programmersbox.mihon.MihonTrust
import com.programmersbox.mihon.UntrustedMihonApiService
import com.programmersbox.mihon.mihonApiService
import dalvik.system.DelegateLastClassLoader
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

private const val EXTENSION_FEATURE = "tachiyomi.extension"
private const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"
private const val METADATA_EXTENSION_LIB = "tachiyomix.extensionLib"
private const val METADATA_NAME = "tachiyomix.name"

private const val PACKAGE_FLAGS =
    PackageManager.GET_CONFIGURATIONS or PackageManager.GET_META_DATA or PackageManager.GET_SIGNING_CERTIFICATES

class MihonExtensionLoader(private val context: Context) {

    @SuppressLint("QueryPermissionsNeeded")
    fun load(
        catalog: MihonCatalog,
        repoSigningKeys: Set<String>,
        trustedIds: Set<String>,
        onTrust: suspend (String) -> Unit,
    ): List<KmpSourceInformation> {
        val packages = installedPackages().filter { it.isMihonExtension() }
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
        info.isMihonExtension()
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

    private fun PackageInfo.isMihonExtension() = reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }

    private fun loadPackage(
        pkgInfo: PackageInfo,
        catalog: MihonCatalog,
        repoSigningKeys: Set<String>,
        trustedIds: Set<String>,
        onTrust: suspend (String) -> Unit,
    ): List<KmpSourceInformation> {
        val pkg = pkgInfo.packageName
        val appInfo = pkgInfo.applicationInfo ?: return emptyList()
        val metaData = appInfo.metaData
        val versionCode = pkgInfo.longVersionCode
        val fingerprints = signatures(pkgInfo)
        val label = metaData?.getString(METADATA_NAME)
            ?: context.packageManager.getApplicationLabel(appInfo).toString().removePrefix("Tachiyomi: ")

        fun notLoaded(reason: String, canTrust: Boolean = false) = listOf(
            KmpSourceInformation(
                apiService = UntrustedMihonApiService(pkg, reason, canTrust, label) {
                    onTrust(MihonTrust.trustId(pkg, versionCode, fingerprints.last()))
                },
                name = "$label ($reason)",
                icon = null,
                packageName = pkg,
                catalog = catalog,
            )
        )

        if (metaData == null) return notLoaded("Malformed")

        val lib = MihonLib.libVersion(metaData.getFloat(METADATA_EXTENSION_LIB), pkgInfo.versionName)
        if (!MihonLib.isSupported(lib)) return notLoaded("Unsupported lib $lib")

        if (fingerprints.isEmpty()) return notLoaded("Unsigned")
        if (!MihonTrust.isTrusted(pkg, versionCode, fingerprints.toSet(), repoSigningKeys, trustedIds)) {
            return notLoaded("Untrusted", canTrust = true)
        }

        val classNames = MihonSourceInstantiator.classNames(pkg, metaData.getString(METADATA_SOURCE_CLASS))
        if (classNames.isEmpty()) return notLoaded("Malformed")

        // Trust was checked above, so an unchanged trusted package reuses its sources.
        loaded[pkgInfo.loadedKey()]?.let { return it }
        // Building the service reads extension-supplied id/baseUrl/name, so it is guarded too.
        return try {
            val classLoader = DelegateLastClassLoader(appInfo.sourceDir, null, context.classLoader)
            MihonSourceInstantiator.instantiate(classNames, classLoader).getOrThrow().map { source ->
                val service = mihonApiService(source)
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
