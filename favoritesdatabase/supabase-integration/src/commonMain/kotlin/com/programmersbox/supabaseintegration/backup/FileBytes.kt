package com.programmersbox.supabaseintegration.backup

expect fun readFileBytes(filePath: String): ByteArray
expect fun writeFileBytes(path: String, bytes: ByteArray)
