package com.programmersbox.kmpextensionloader

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object ApkFiles {
    private val PACKAGE_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    /** True when [name] is a dotted Java package name whose `<name>.apk` resolves directly inside [dir]. */
    fun isSafePackageName(dir: File, name: String): Boolean =
        PACKAGE_REGEX.matches(name) && File(dir, "$name.apk").canonicalFile.parentFile == dir.canonicalFile

    fun moveReplacing(from: File, to: File) {
        try {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
