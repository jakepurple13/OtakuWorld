package com.programmersbox.aniyomi.repo

import java.util.zip.GZIPInputStream

actual fun gunzip(bytes: ByteArray): ByteArray = GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
