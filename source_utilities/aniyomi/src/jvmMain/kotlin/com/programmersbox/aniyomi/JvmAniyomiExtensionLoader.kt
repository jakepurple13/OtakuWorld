package com.programmersbox.aniyomi

import com.programmersbox.aniyomi.loader.AniyomiSourceInstantiator
import com.programmersbox.kmpextensionloader.ChildFirstClassLoader
import com.programmersbox.kmpmodels.KmpSourceInformation

/** Desktop counterpart of Android's AniyomiExtensionLoader: loads the APKs in [store] through their converted jars. */
class JvmAniyomiExtensionLoader(
    private val store: AniyomiApkStore,
    private val parentClassLoader: ClassLoader = AniyomiCatalog::class.java.classLoader,
) {
    private class LoadedJar(val classLoader: ChildFirstClassLoader, val services: List<AniyomiApiService>)

    /**
     * Keyed by jar path. Jar names are content hashes, so an unchanged jar keeps its loader and services
     * across loads: services handed out earlier stay usable, and their unloaded classes can still load.
     */
    private val loadedJars = mutableMapOf<String, LoadedJar>()

    @Synchronized
    fun load(
        catalog: AniyomiCatalog,
        repoSigningKeys: Set<String>,
        trustedIds: Set<String>,
        onTrust: suspend (String) -> Unit,
    ): List<KmpSourceInformation> {
        val installed = store.installed().filter { it.info.isExtension }
        // Close loaders only for jars that are gone (uninstalled or updated), before their files are deleted.
        val liveJars = installed.mapNotNull { it.jar?.absolutePath }.toSet()
        (loadedJars.keys - liveJars).forEach { path ->
            loadedJars.remove(path)?.let { runCatching { it.classLoader.close() } }
        }
        val sources = installed.flatMap {
            // One broken package must not abort loading the rest.
            try {
                loadPackage(it, catalog, repoSigningKeys, trustedIds, onTrust)
            } catch (t: Throwable) {
                t.printStackTrace()
                emptyList()
            }
        }
        store.cleanupStaleJars()
        return sources
    }

    // The store only accepts Aniyomi extensions, so any installed, readable APK is one.
    fun isExtension(packageName: String): Boolean = store.versionName(packageName) != null

    /** Closes every class loader; sources from earlier loads stop working. */
    @Synchronized
    fun close() {
        loadedJars.values.forEach { runCatching { it.classLoader.close() } }
        loadedJars.clear()
    }

    private fun loadPackage(
        installed: InstalledAniyomiApk,
        catalog: AniyomiCatalog,
        repoSigningKeys: Set<String>,
        trustedIds: Set<String>,
        onTrust: suspend (String) -> Unit,
    ): List<KmpSourceInformation> {
        val info = installed.info
        val pkg = info.packageName
        val versionCode = info.versionCode
        val fingerprints = info.fingerprints

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
                name = "${info.label} ($reason)",
                icon = null,
                packageName = pkg,
                catalog = catalog,
            )
        )

        if (!info.hasMetadata) return notLoaded("Malformed")

        val lib = info.libVersion
        if (lib == null || !AniyomiLib.isSupported(lib)) return notLoaded("Unsupported lib $lib")

        if (fingerprints.isEmpty()) return notLoaded("Unsigned")
        if (!AniyomiTrust.isTrusted(pkg, versionCode, fingerprints.toSet(), repoSigningKeys, trustedIds)) {
            return notLoaded("Untrusted", canTrust = true)
        }

        val classNames = info.sourceClasses
        if (classNames.isNullOrBlank()) return notLoaded("Malformed")

        val jar = installed.jar ?: return notLoaded("Failed to convert")

        // Building the service reads extension-supplied id/baseUrl/name, so it is guarded too.
        return try {
            val services = loadedJars.getOrPut(jar.absolutePath) {
                val classLoader = ChildFirstClassLoader(arrayOf(jar.toURI().toURL()), parentClassLoader, ANIYOMI_PARENT_FIRST)
                try {
                    LoadedJar(
                        classLoader,
                        AniyomiSourceInstantiator.instantiate(classNames, pkg, classLoader).map { aniyomiApiService(it, lib) },
                    )
                } catch (t: Throwable) {
                    runCatching { classLoader.close() }
                    throw t
                }
            }.services
            services.map { service ->
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

    internal companion object {
        /** The extension ABI the host provides; see [ChildFirstClassLoader]. */
        val ANIYOMI_PARENT_FIRST = listOf(
            "java.", "javax.", "jdk.", "sun.", "kotlin.", "kotlinx.",
            "android.", "androidx.", "eu.kanade.tachiyomi.", "tachiyomi.", "mihon.",
            "okhttp3.", "okio.", "org.jsoup.", "uy.kohesive.injekt.", "rx.", "app.cash.quickjs.",
            "fi.iki.elonen.", "app.cash.zipline.",
        )
    }
}
