package com.programmersbox.supabaseintegration.backup

import java.io.File

actual fun readFileBytes(filePath: String): ByteArray = File(filePath).readBytes()
actual fun writeFileBytes(path: String, bytes: ByteArray) { File(path).writeBytes(bytes) }
