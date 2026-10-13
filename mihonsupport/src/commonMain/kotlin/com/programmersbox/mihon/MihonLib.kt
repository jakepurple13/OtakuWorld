package com.programmersbox.mihon

/** Mihon extension lib version rules. Mirrors Mihon's ExtensionLoader.SUPPORTED_LIB_VERSIONS. */
object MihonLib {
    val SUPPORTED: Set<Double> = setOf(1.4, 1.6)

    /** Lib version from `tachiyomix.extensionLib` metadata, falling back to the versionName prefix. */
    fun libVersion(metadataLib: Float, versionName: String?): Double? =
        metadataLib.takeUnless { it == 0f }?.toString()?.toDouble()
            ?: versionName?.let(::libVersion)

    /** "1.4.190" -> 1.4 */
    fun libVersion(versionName: String): Double? = versionName.substringBeforeLast('.').toDoubleOrNull()

    fun isSupported(lib: Double?): Boolean = lib != null && lib in SUPPORTED
}
