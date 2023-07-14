package com.programmersbox.extensionloader

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import dalvik.system.PathClassLoader
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

private val PACKAGE_FLAGS =
    PackageManager.GET_CONFIGURATIONS or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
    }

/**
 * Use this to load code from other apks!
 *
 * Make sure you are creating those apks and they are installed as applications.
 *
 * @param extensionFeature this should match what you set in the extension module at the manifest level:
 * ```xml
 * <uses-feature android:name="{extensionFeature}" />
 * ```
 *
 * @param metadataClass this should match what you set in the extension module at the application level:
 * ```xml
 * <application ...>
 *      <meta-data android:name="{metadataClass}" android:value="${extClass}" />
 * </application>
 * ```
 * This should match the class name of what you want to access and load.
 * If you want to include other metadata types, add more metadata attributes!
 */
class ExtensionLoader<T, R>(
    private val context: Context,
    private val extensionFeature: String,
    private val metadataClass: String,
    private val mapping: (T, ApplicationInfo, PackageInfo) -> R
) {
    @SuppressLint("QueryPermissionsNeeded")
    fun loadExtensions(mapped: (T, ApplicationInfo, PackageInfo) -> R = mapping): List<R> {
        val packageManager = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            packageManager.getInstalledPackages(PACKAGE_FLAGS)
        }
            .filter { it.reqFeatures.orEmpty().any { f -> f.name == extensionFeature } }

        return runBlocking {
            packages
                .map { async { loadExtension(it, mapped) } }
                .flatMap { it.await() }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    suspend fun loadExtensionsBlocking(mapped: (T, ApplicationInfo, PackageInfo) -> R = mapping): List<R> {
        val packageManager = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            packageManager.getInstalledPackages(PACKAGE_FLAGS)
        }
            .filter { it.reqFeatures.orEmpty().any { f -> f.name == extensionFeature } }

        return runBlocking {
            packages
                .map { async { loadExtension(it, mapped) } }
                .flatMap { it.await() }
        }
    }

    private fun loadExtension(packageInfo: PackageInfo, mapped: (T, ApplicationInfo, PackageInfo) -> R): List<R> {
        val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getApplicationInfo(
                packageInfo.packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            context.packageManager.getApplicationInfo(
                packageInfo.packageName,
                PackageManager.GET_META_DATA
            )
        }

        val classLoader = PathClassLoader(appInfo.sourceDir, null, context.classLoader)

        return appInfo.metaData.getString(metadataClass)
            .orEmpty()
            .split(";")
            .map {
                val sourceClass = it.trim()
                if (sourceClass.startsWith(".")) {
                    packageInfo.packageName + sourceClass
                } else {
                    sourceClass
                }
            }
            .mapNotNull {
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    Class.forName(it, false, classLoader)
                        .getDeclaredConstructor()
                        .newInstance() as? T
                }
                    .onFailure { it.printStackTrace() }
                    .getOrNull()
            }
            .map { mapped(it, appInfo, packageInfo) }
    }
}