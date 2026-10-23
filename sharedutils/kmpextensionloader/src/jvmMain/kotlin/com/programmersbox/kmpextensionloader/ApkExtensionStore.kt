package com.programmersbox.kmpextensionloader

import java.io.File

enum class ApkInstallFailure { INVALID, INCOMPATIBLE, STORAGE }

class ApkInstallException(
    val failure: ApkInstallFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

interface ApkExtensionStore {
    /** Whether this store can install [file]. Cheap, side-effect free, never throws (false when unreadable). */
    fun accepts(file: File): Boolean

    /** Installs [apk] (a downloaded temp file, consumed). Throws [ApkInstallException]. Returns the package name. */
    suspend fun install(apk: File): String

    /** Installed versionName for [packageName], or null if this store doesn't own it. */
    fun versionName(packageName: String): String?
}
