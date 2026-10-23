package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.mihon.loader.MihonSourceInstantiator

/** Desktop counterpart of Android's MihonExtensionLoader: loads the APKs in [store] through their converted jars. */
class JvmMihonExtensionLoader(
    private val store: MihonApkStore,
    private val parentClassLoader: ClassLoader = MihonCatalog::class.java.classLoader,
) {
    private val classLoaders = mutableListOf<ChildFirstClassLoader>()

    @Synchronized
    fun load(
        catalog: MihonCatalog,
        repoSigningKeys: Set<String>,
        trustedIds: Set<String>,
        onTrust: suspend (String) -> Unit,
    ): List<KmpSourceInformation> {
        close()
        val sources = store.installed()
            .filter { it.info.isExtension }
            .flatMap {
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

    // The store only accepts Mihon extensions, so any installed, readable APK is one.
    fun isExtension(packageName: String): Boolean = store.versionName(packageName) != null

    /** Closes the class loaders from the previous [load]. */
    @Synchronized
    fun close() {
        classLoaders.forEach { runCatching { it.close() } }
        classLoaders.clear()
    }

    private fun loadPackage(
        installed: InstalledMihonApk,
        catalog: MihonCatalog,
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
                apiService = UntrustedMihonApiService(pkg, reason, canTrust, info.label) {
                    onTrust(MihonTrust.trustId(pkg, versionCode, fingerprints.last()))
                },
                name = "${info.label} ($reason)",
                icon = null,
                packageName = pkg,
                catalog = catalog,
            )
        )

        val lib = info.libVersion
        if (!MihonLib.isSupported(lib)) return notLoaded("Unsupported lib $lib")

        if (fingerprints.isEmpty()) return notLoaded("Unsigned")
        if (!MihonTrust.isTrusted(pkg, versionCode, fingerprints.toSet(), repoSigningKeys, trustedIds)) {
            return notLoaded("Untrusted", canTrust = true)
        }

        val classNames = info.sourceClasses
        if (classNames.isEmpty()) return notLoaded("Malformed")

        // Building the service reads extension-supplied id/baseUrl/name, so it is guarded too.
        val classLoader = ChildFirstClassLoader(arrayOf(installed.jar.toURI().toURL()), parentClassLoader)
        return try {
            MihonSourceInstantiator.instantiate(classNames, classLoader).getOrThrow().map { source ->
                val service = mihonApiService(source)
                KmpSourceInformation(
                    apiService = service,
                    name = service.displayName,
                    icon = null,
                    packageName = pkg,
                    catalog = catalog,
                )
            }.also { classLoaders += classLoader }
        } catch (t: Throwable) {
            t.printStackTrace()
            runCatching { classLoader.close() }
            notLoaded("Failed to load: ${t.message}")
        }
    }
}
