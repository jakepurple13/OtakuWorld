package com.programmersbox.supabaseintegration.backup

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.getBytes
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
actual fun readFileBytes(filePath: String): ByteArray {
    val data = NSData.dataWithContentsOfFile(filePath) ?: error("Cannot read $filePath")
    return ByteArray(data.length.toInt()).also { data.getBytes(it.refTo(0) as COpaquePointer?, data.length) }
}

@OptIn(ExperimentalForeignApi::class)
actual fun writeFileBytes(path: String, bytes: ByteArray) {
    NSData.create(bytes = bytes.refTo(0) as COpaquePointer?, length = bytes.size.toULong()).writeToFile(path, true)
}
