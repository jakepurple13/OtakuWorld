package com.programmersbox.aniyomi

/** Aniyomi extension lib versions this runtime is verified against. */
object AniyomiLib {
    val SUPPORTED: Set<Double> = setOf(14.0, 16.0, 17.0)

    /** Lib version from the manifest `animeextension.lib` metadata int (0 = absent), falling back to the versionName prefix. */
    fun libVersion(metadataLib: Int, versionName: String?): Double? =
        metadataLib.takeUnless { it == 0 }?.toDouble()
            ?: versionName?.let(::libVersion)

    /** Key real Aniyomi APKs declare. */
    const val METADATA_LIB_KEY = "tachiyomix.extensionLib"

    /** Older key some builds used; read only when [METADATA_LIB_KEY] is absent. */
    const val METADATA_LIB_KEY_LEGACY = "aniyomix.extensionLib"

    /**
     * Resolves the lib version: [METADATA_LIB_KEY], then [METADATA_LIB_KEY_LEGACY], then the versionName prefix.
     * [metadataInt] returns the manifest meta-data int for a key, or null/0 when absent.
     */
    fun libVersion(metadataInt: (String) -> Int?, versionName: String?): Double? =
        libVersion(
            metadataInt(METADATA_LIB_KEY)?.takeUnless { it == 0 }
                ?: metadataInt(METADATA_LIB_KEY_LEGACY)
                ?: 0,
            versionName,
        )

    /** "16.55" -> 16.0 */
    fun libVersion(versionName: String): Double? = versionName.substringBefore('.').toIntOrNull()?.toDouble()

    fun isSupported(lib: Double?): Boolean = lib != null && lib in SUPPORTED
}
