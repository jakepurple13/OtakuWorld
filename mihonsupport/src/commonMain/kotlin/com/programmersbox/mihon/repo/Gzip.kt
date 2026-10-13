package com.programmersbox.mihon.repo

/** Decompresses a gzip stream. */
expect fun gunzip(bytes: ByteArray): ByteArray

/** True when [bytes] start with the gzip magic number `1f 8b`. */
fun isGzip(bytes: ByteArray): Boolean =
    bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
