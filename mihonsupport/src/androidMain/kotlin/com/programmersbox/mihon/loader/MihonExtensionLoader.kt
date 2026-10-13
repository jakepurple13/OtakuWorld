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
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import java.security.MessageDigest

private const val EXTENSION_FEATURE = "tachiyomi.extension"
private const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"
private const val METADATA_EXTENSION_LIB = "tachiyomix.extensionLib"
private const val METADATA_NAME = "tachiyomix.name"

private const val PACKAGE_FLAGS =
    PackageManager.GET_CONFIGURATIONS or PackageManager.GET_META_DATA or PackageManager.GET_SIGNING_CERTIFICATES

object MihonSourceInstantiator {
    fun classNames(pkg: String, metadataValue: String?): List<String> = metadataValue.orEmpty()
        .split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { if (it.startsWith(".")) pkg + it else it }

    /** Instantiates every class; any failure (including LinkageError) fails the whole package, like Mihon. */
    fun instantiate(classNames: List<String>, classLoader: ClassLoader): Result<List<CatalogueSource>> = try {
        Result.success(
            classNames.flatMap { name ->
                when (val obj = Class.forName(name, false, classLoader).getDeclaredConstructor().newInstance()) {
                    is SourceFactory -> obj.createSources()
                    is Source -> listOf(obj)
                    else -> error("Unknown source class type: ${obj.javaClass}")
                }
            }.filterIsInstance<CatalogueSource>()
        )
    } catch (t: Throwable) {
        Result.failure(t)
    }
}

class MihonExtensionLoader(private val context: Context) {

    @SuppressLint("QueryPermissionsNeeded")
    fun load(
        catalog: MihonCatalog,
        repoSigningKeys: Set<String>,
        trustedIds: Set<String>,
        onTrust: suspend (String) -> Unit,
    ): List<KmpSourceInformation> = installedPackages()
        .filter { it.isMihonExtension() }
        .flatMap {
            // One broken package must not abort loading the rest.
            try {
                loadPackage(it, catalog, repoSigningKeys, trustedIds, onTrust)
            } catch (t: Throwable) {
                t.printStackTrace()
                emptyList()
            }
        }

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
    private fun installedPackages(): List<PackageInfo> {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pm.getInstalledPackages(PACKAGE_FLAGS)
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
            }
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
