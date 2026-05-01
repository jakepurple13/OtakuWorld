package com.programmersbox.kmpextensionloader

import com.googlecode.dex2jar.tools.Dex2jarCmd
import java.io.File
import java.security.MessageDigest

object DexConverter {

    fun convert(apkFile: File, cacheDir: File): File? {
        val hash = sha256(apkFile)
        val cachedJar = File(cacheDir, "$hash.jar")
        if (cachedJar.exists()) return cachedJar

        return runCatching {
            cacheDir.mkdirs()
            val tmp = File(cacheDir, "$hash.tmp.jar")
            Dex2jarCmd().doMain(
                *arrayOf(apkFile.absolutePath, "-o", tmp.absolutePath, "--force")
            )
            if (tmp.exists() && tmp.length() > 0) {
                tmp.renameTo(cachedJar)
                cachedJar
            } else {
                tmp.delete()
                null
            }
        }.onFailure {
            println("DexConverter: failed to convert ${apkFile.name}: ${it.message}")
            it.printStackTrace()
        }.getOrNull()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
