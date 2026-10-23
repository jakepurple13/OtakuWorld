package com.programmersbox.aniyomi

import com.android.apksig.ApkVerifier
import com.programmersbox.kmpextensionloader.ApkManifestParser
import net.dongliu.apk.parser.ApkFile
import java.io.File
import java.security.MessageDigest
import java.security.cert.X509Certificate

data class AniyomiApkInfo(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    val label: String,
    val isExtension: Boolean,
    /** False when the manifest has no <meta-data>; Android reports such packages as "Malformed". */
    val hasMetadata: Boolean,
    val libVersion: Double?,
    /** Raw `;`-separated value of `tachiyomi.animeextension.class`, for AniyomiSourceInstantiator. */
    val sourceClasses: String?,
    val nsfw: Boolean,
    /** Lowercase hex SHA-256 of signer certificates, oldest to newest; empty if unsigned or invalid. */
    val fingerprints: List<String>,
)

object AniyomiApkInspector {
    private const val FEATURE_EXTENSION = "tachiyomi.animeextension"
    private const val METADATA_SOURCE_CLASS = "tachiyomi.animeextension.class"
    private const val METADATA_NSFW = "tachiyomi.animeextension.nsfw"
    private const val LABEL_PREFIX = "Aniyomi: "

    fun inspect(apk: File): AniyomiApkInfo {
        val manifest = ApkManifestParser.parse(apk)
        val meta = manifest.metaData
        return AniyomiApkInfo(
            packageName = manifest.packageName,
            versionName = manifest.versionName,
            versionCode = manifest.versionCode,
            label = label(apk)?.removePrefix(LABEL_PREFIX)?.takeIf { it.isNotBlank() }
                ?: manifest.packageName.substringAfterLast('.'),
            isExtension = FEATURE_EXTENSION in manifest.features,
            hasMetadata = meta.isNotEmpty(),
            libVersion = AniyomiLib.libVersion({ meta[it]?.toIntOrNull() }, manifest.versionName),
            sourceClasses = meta[METADATA_SOURCE_CLASS],
            nsfw = meta[METADATA_NSFW] == "1",
            fingerprints = fingerprints(apk),
        )
    }

    private fun label(apk: File): String? = runCatching { ApkFile(apk).use { it.apkMeta.label } }.getOrNull()

    private fun fingerprints(apk: File): List<String> = runCatching {
        val result = ApkVerifier.Builder(apk).build().verify()
        if (!result.isVerified) return emptyList()
        val lineage = result.v3SchemeSigners.singleOrNull()?.signingCertificateLineage?.certificatesInLineage
        val certs: List<X509Certificate> = if (!lineage.isNullOrEmpty()) lineage else result.signerCertificates
        certs.map { cert ->
            MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString("") { "%02x".format(it) }
        }
    }.getOrDefault(emptyList())
}
