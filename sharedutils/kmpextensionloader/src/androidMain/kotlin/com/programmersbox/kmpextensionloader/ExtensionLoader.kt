package com.programmersbox.kmpextensionloader

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import dalvik.system.PathClassLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

private val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or PackageManager.GET_SIGNING_CERTIFICATES

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
    private val mapping: (T, ApplicationInfo, PackageInfo) -> R,
) {
    private val limitedDispatcher = Dispatchers.Default.limitedParallelism(4)

    @SuppressLint("QueryPermissionsNeeded")
    fun loadExtensions(mapped: (T, ApplicationInfo, PackageInfo) -> R = mapping): List<R> {
        val packages = extensionPackages()

        return runBlocking {
            packages
                .map { async(limitedDispatcher) { loadExtension(it, mapped) } }
                .flatMap { it.await() }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    suspend fun loadExtensionsBlocking(mapped: (T, ApplicationInfo, PackageInfo) -> R = mapping): List<R> = coroutineScope {
        val packages = extensionPackages()

        packages
            .map { async(limitedDispatcher) { loadExtension(it, mapped) } }
            .flatMap { it.await() }
    }

    /** Instances per installed package version, so a reload reuses them instead of building a new class loader. */
    private data class InstanceKey(val packageName: String, val versionCode: Long, val lastUpdateTime: Long)

    private val instances = ConcurrentHashMap<InstanceKey, List<T>>()

    /**
     * Installed packages that declare [extensionFeature]. Signing data is only requested for those,
     * not for every app on the device. Drops cached instances of packages that are gone or changed.
     */
    private fun extensionPackages(): List<PackageInfo> {
        val packageManager = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_CONFIGURATIONS.toLong()))
        } else {
            packageManager.getInstalledPackages(PackageManager.GET_CONFIGURATIONS)
        }
            .filter { it.reqFeatures.orEmpty().any { f -> f.name == extensionFeature } }
            .mapNotNull {
                // The package can be removed between the two calls.
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.getPackageInfo(it.packageName, PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
                    } else {
                        packageManager.getPackageInfo(it.packageName, PACKAGE_FLAGS)
                    }
                }.getOrNull()
            }
        instances.keys.retainAll(packages.map { it.instanceKey() }.toSet())
        return packages
    }

    private fun PackageInfo.instanceKey() = InstanceKey(packageName, longVersionCode, lastUpdateTime)

    /** One broken package (removed mid-load, missing meta-data, throwing mapping) must not abort loading the rest. */
    private fun loadExtension(packageInfo: PackageInfo, mapped: (T, ApplicationInfo, PackageInfo) -> R): List<R> = try {
        loadExtensionOrThrow(packageInfo, mapped)
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        t.printStackTrace()
        emptyList()
    }

    private fun loadExtensionOrThrow(packageInfo: PackageInfo, mapped: (T, ApplicationInfo, PackageInfo) -> R): List<R> {
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

        return instances
            .getOrPut(packageInfo.instanceKey()) { createInstances(packageInfo, appInfo) }
            .map { mapped(it, appInfo, packageInfo) }
    }

    private fun createInstances(packageInfo: PackageInfo, appInfo: ApplicationInfo): List<T> {
        val classLoader = PathClassLoader(appInfo.sourceDir, null, this::class.java.classLoader)

        return appInfo.metaData?.getString(metadataClass)
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
    }
}