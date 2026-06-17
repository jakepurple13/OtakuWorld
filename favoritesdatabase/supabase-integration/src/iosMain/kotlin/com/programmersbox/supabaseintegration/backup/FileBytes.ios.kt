package com.programmersbox.supabaseintegration.backup

import kotlinx.cinterop.refTo
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

actual fun readFileBytes(filePath: String): ByteArray {
    val data = NSData.dataWithContentsOfFile(filePath) ?: error("Cannot read $filePath")
    return ByteArray(data.length.toInt()).also { data.getBytes(it.refTo(0), data.length) }
}

actual fun writeFileBytes(path: String, bytes: ByteArray) {
    NSData.create(bytes = bytes.refTo(0), length = bytes.size.toULong()).writeToFile(path, true)
}
