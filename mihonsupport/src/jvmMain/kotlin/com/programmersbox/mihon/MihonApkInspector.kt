package com.programmersbox.mihon

import com.android.apksig.ApkVerifier
import com.programmersbox.kmpextensionloader.ApkManifestParser
import com.programmersbox.mihon.loader.MihonSourceInstantiator
import java.io.File
import java.security.MessageDigest
import java.security.cert.X509Certificate

data class MihonApkInfo(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    val label: String,
    val isExtension: Boolean,
    /** False when the manifest has no <meta-data>; Android reports such packages as "Malformed". */
    val hasMetadata: Boolean,
    val libVersion: Double?,
    val sourceClasses: List<String>,
    val nsfw: Boolean,
    /** Lowercase hex SHA-256 of signer certificates, oldest to newest; empty if unsigned or invalid. */
    val fingerprints: List<String>,
)

object MihonApkInspector {
    private const val FEATURE_EXTENSION = "tachiyomi.extension"
    private const val METADATA_NAME = "tachiyomix.name"
    private const val METADATA_EXTENSION_LIB = "tachiyomix.extensionLib"
    private const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"
    private const val METADATA_NSFW = "tachiyomi.extension.nsfw"

    fun inspect(apk: File): MihonApkInfo {
        val manifest = ApkManifestParser.parse(apk)
        val meta = manifest.metaData
        return MihonApkInfo(
            packageName = manifest.packageName,
            versionName = manifest.versionName,
            versionCode = manifest.versionCode,
            label = meta[METADATA_NAME]?.takeIf { it.isNotBlank() }
                ?: manifest.packageName.substringAfterLast('.'),
            isExtension = FEATURE_EXTENSION in manifest.features,
            hasMetadata = meta.isNotEmpty(),
            libVersion = MihonLib.libVersion(meta[METADATA_EXTENSION_LIB]?.toFloatOrNull() ?: 0f, manifest.versionName),
            sourceClasses = MihonSourceInstantiator.classNames(manifest.packageName, meta[METADATA_SOURCE_CLASS]),
            nsfw = meta[METADATA_NSFW] == "1",
            fingerprints = fingerprints(apk),
        )
    }

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
