package com.programmersbox.aniyomi

/** Aniyomi extension lib versions this runtime is verified against. */
object AniyomiLib {
    val SUPPORTED: Set<Double> = setOf(14.0, 16.0, 17.0)

    /** Lib version from the manifest `animeextension.lib` metadata int (0 = absent), falling back to the versionName prefix. */
    fun libVersion(metadataLib: Int, versionName: String?): Double? =
        metadataLib.takeUnless { it == 0 }?.toDouble()
            ?: versionName?.let(::libVersion)

    /** "16.55" -> 16.0 */
    fun libVersion(versionName: String): Double? = versionName.substringBefore('.').toIntOrNull()?.toDouble()

    fun isSupported(lib: Double?): Boolean = lib != null && lib in SUPPORTED
}
