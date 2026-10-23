package com.programmersbox.mihon

import com.programmersbox.kmpextensionloader.ApkExtensionStore
import com.programmersbox.kmpextensionloader.ApkManifestParser
import com.programmersbox.kmpextensionloader.ApkInstallException
import com.programmersbox.kmpextensionloader.ApkInstallFailure
import com.programmersbox.kmpextensionloader.DexConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** [jar] is null when dex conversion failed; the package is still listed so the failure is visible. */
data class InstalledMihonApk(val apk: File, val jar: File?, val info: MihonApkInfo)

class MihonApkStore(
    private val apkDir: File,
    private val jarCacheDir: File,
    private val inspect: (File) -> MihonApkInfo = MihonApkInspector::inspect,
    private val convert: (File, File) -> File? = DexConverter::convert,
) : ApkExtensionStore {

    override fun accepts(file: File): Boolean =
        runCatching { "tachiyomi.extension" in ApkManifestParser.parse(file).features }.getOrDefault(false)

    override suspend fun install(apk: File): String = withContext(Dispatchers.IO) {
        try {
            val info = try {
                inspect(apk)
            } catch (e: Exception) {
                throw ApkInstallException(ApkInstallFailure.INVALID, "Not a readable APK: ${e.message}", e)
            }
            if (!info.isExtension) {
                throw ApkInstallException(ApkInstallFailure.INVALID, "${info.packageName} is not a Mihon extension")
            }
            if (!MihonLib.isSupported(info.libVersion)) {
                throw ApkInstallException(
                    ApkInstallFailure.INCOMPATIBLE,
                    "Unsupported extension lib version ${info.libVersion}"
                )
            }
            if (!isSafePackageName(info.packageName)) {
                throw ApkInstallException(ApkInstallFailure.INVALID, "Invalid package name: ${info.packageName}")
            }
            // Convert before moving so a failure leaves apkDir untouched. Jar name is content-hash based.
            convert(apk, jarCacheDir)
                ?: throw ApkInstallException(ApkInstallFailure.INVALID, "Could not convert ${info.packageName} dex")
            try {
                apkDir.mkdirs()
                moveReplacing(apk, File(apkDir, "${info.packageName}.apk"))
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
        val deleted = apk.delete()
        if (deleted) jar?.delete()
        deleted
    }

    fun installed(): List<InstalledMihonApk> = apkFiles().mapNotNull { apk ->
        runCatching {
            val info = inspect(apk)
            val jar = runCatching { convert(apk, jarCacheDir) }.getOrNull()
            if (jar == null) println("MihonApkStore: could not convert ${apk.name}")
            InstalledMihonApk(apk, jar, info)
        }.onFailure { println("MihonApkStore: skipping ${apk.name}: ${it.message}") }.getOrNull()
    }

    override fun versionName(packageName: String): String? {
        if (!isSafePackageName(packageName)) return null
        val apk = File(apkDir, "$packageName.apk").takeIf { it.isFile } ?: return null
        // Manifest only: full inspect verifies signatures and is too slow for UI callers.
        return runCatching { ApkManifestParser.parse(apk).versionName }.getOrNull()
    }

    fun cleanupStaleJars() {
        val keep = apkFiles()
            .mapNotNull { runCatching { DexConverter.jarFor(it, jarCacheDir).name }.getOrNull() }
            .toSet()
        jarCacheDir.listFiles { f -> DexConverter.isCachedJar(f) }
            ?.filter { it.name !in keep }
            ?.forEach { runCatching { it.delete() } }
    }

    private fun isSafePackageName(name: String): Boolean =
        PACKAGE_REGEX.matches(name) &&
            File(apkDir, "$name.apk").canonicalFile.parentFile == apkDir.canonicalFile

    private fun apkFiles(): List<File> =
        apkDir.listFiles { f -> f.isFile && f.extension == "apk" }?.sortedBy { it.name }.orEmpty()

    private fun moveReplacing(from: File, to: File) {
        try {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object {
        val PACKAGE_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
    }
}
