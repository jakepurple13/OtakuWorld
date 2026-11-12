package com.programmersbox.aniyomi

import com.programmersbox.kmpextensionloader.ApkExtensionStore
import com.programmersbox.kmpextensionloader.ApkFiles
import com.programmersbox.kmpextensionloader.ApkInstallException
import com.programmersbox.kmpextensionloader.ApkInstallFailure
import com.programmersbox.kmpextensionloader.ApkManifestParser
import com.programmersbox.kmpextensionloader.DexConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** [jar] is null when dex conversion failed; the package is still listed so the failure is visible. */
data class InstalledAniyomiApk(val apk: File, val jar: File?, val info: AniyomiApkInfo)

class AniyomiApkStore(
    private val apkDir: File,
    private val jarCacheDir: File,
    private val inspect: (File) -> AniyomiApkInfo = AniyomiApkInspector::inspect,
    private val convert: (File, File) -> File? = DexConverter::convert,
) : ApkExtensionStore {

    override fun accepts(file: File): Boolean =
        runCatching { "tachiyomi.animeextension" in ApkManifestParser.parse(file).features }.getOrDefault(false)

    override suspend fun install(apk: File): String = withContext(Dispatchers.IO) {
        try {
            val info = try {
                inspect(apk)
            } catch (e: Exception) {
                throw ApkInstallException(ApkInstallFailure.INVALID, "Not a readable APK: ${e.message}", e)
            }
            if (!info.isExtension) {
                throw ApkInstallException(ApkInstallFailure.INVALID, "${info.packageName} is not an Aniyomi extension")
            }
            if (!AniyomiLib.isSupported(info.libVersion)) {
                throw ApkInstallException(
                    ApkInstallFailure.INCOMPATIBLE,
                    "Unsupported extension lib version ${info.libVersion}"
                )
            }
            if (!isSafePackageName(info.packageName)) {
                throw ApkInstallException(ApkInstallFailure.INVALID, "Invalid package name: ${info.packageName}")
            }
            requireSameSigner(info.packageName, info.fingerprints)
            // Convert before moving so a failure leaves apkDir untouched. Jar name is content-hash based.
            convert(apk, jarCacheDir)
                ?: throw ApkInstallException(ApkInstallFailure.INVALID, "Could not convert ${info.packageName} dex")
            try {
                apkDir.mkdirs()
                val target = File(apkDir, "${info.packageName}.apk")
                installedCache.remove(target.absolutePath)
                ApkFiles.moveReplacing(apk, target)
            } catch (e: IOException) {
                throw ApkInstallException(ApkInstallFailure.STORAGE, "Could not store ${info.packageName}: ${e.message}", e)
            }
            info.packageName
        } finally {
            apk.delete()
        }
    }

    suspend fun uninstall(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (!isSafePackageName(packageName)) return@withContext false
        val apk = File(apkDir, "$packageName.apk")
        if (!apk.exists()) return@withContext false
        val jar = runCatching { DexConverter.jarFor(apk, jarCacheDir) }.getOrNull()
        installedCache.remove(apk.absolutePath)
        val deleted = apk.delete()
        if (deleted) jar?.delete()
        deleted
    }

    private data class ApkStamp(val length: Long, val lastModified: Long) {
        constructor(apk: File) : this(apk.length(), apk.lastModified())
    }

    /** [installed] results by apk path: an unchanged apk is not re-verified, re-hashed or re-converted on every load. */
    private val installedCache = ConcurrentHashMap<String, Pair<ApkStamp, InstalledAniyomiApk>>()

    private fun cached(apk: File): InstalledAniyomiApk? = installedCache[apk.absolutePath]
        ?.takeIf { (stamp, entry) -> stamp == ApkStamp(apk) && entry.jar?.isFile == true }
        ?.second

    fun installed(): List<InstalledAniyomiApk> {
        val apks = apkFiles()
        installedCache.keys.retainAll(apks.map { it.absolutePath }.toSet())
        return apks.mapNotNull { apk ->
            cached(apk)?.let { return@mapNotNull it }
            runCatching {
                val info = inspect(apk)
                val jar = runCatching { convert(apk, jarCacheDir) }.getOrNull()
                if (jar == null) println("AniyomiApkStore: could not convert ${apk.name}")
                InstalledAniyomiApk(apk, jar, info)
            }.onFailure { println("AniyomiApkStore: skipping ${apk.name}: ${it.message}") }.getOrNull()
                ?.also { if (it.jar != null) installedCache[apk.absolutePath] = ApkStamp(apk) to it }
        }
    }

    /** Like Android, an update must share a signer with the installed package, so another repo cannot take it over. */
    private fun requireSameSigner(packageName: String, fingerprints: List<String>) {
        val existing = File(apkDir, "$packageName.apk").takeIf { it.isFile } ?: return
        val installed = runCatching { inspect(existing).fingerprints }.getOrDefault(emptyList())
        if (installed.isNotEmpty() && installed.none { it in fingerprints }) {
            throw ApkInstallException(
                ApkInstallFailure.INVALID,
                "$packageName is already installed with a different signature; uninstall it first",
            )
        }
    }

    override fun versionName(packageName: String): String? {
        if (!isSafePackageName(packageName)) return null
        val apk = File(apkDir, "$packageName.apk").takeIf { it.isFile } ?: return null
        // Manifest only: full inspect verifies signatures and is too slow for UI callers.
        return runCatching { ApkManifestParser.parse(apk).versionName }.getOrNull()
    }

    fun cleanupStaleJars() {
        val keep = apkFiles()
            .mapNotNull { cached(it)?.jar?.name ?: runCatching { DexConverter.jarFor(it, jarCacheDir).name }.getOrNull() }
            .toSet()
        jarCacheDir.listFiles { f -> DexConverter.isCachedJar(f) }
            ?.filter { it.name !in keep }
            ?.forEach { runCatching { it.delete() } }
    }

    private fun isSafePackageName(name: String): Boolean = ApkFiles.isSafePackageName(apkDir, name)

    private fun apkFiles(): List<File> =
        apkDir.listFiles { f -> f.isFile && f.extension == "apk" }?.sortedBy { it.name }.orEmpty()
}
