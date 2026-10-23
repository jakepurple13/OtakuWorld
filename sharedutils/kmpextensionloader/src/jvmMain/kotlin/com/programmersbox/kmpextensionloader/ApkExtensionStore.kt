package com.programmersbox.kmpextensionloader

import java.io.File

enum class ApkInstallFailure { INVALID, INCOMPATIBLE, STORAGE }

class ApkInstallException(
    val failure: ApkInstallFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

interface ApkExtensionStore {
    /** Installs [apk] (a downloaded temp file, consumed). Throws [ApkInstallException]. Returns the package name. */
    suspend fun install(apk: File): String

    /** Installed versionName for [packageName], or null if this store doesn't own it. */
    fun versionName(packageName: String): String?
}
