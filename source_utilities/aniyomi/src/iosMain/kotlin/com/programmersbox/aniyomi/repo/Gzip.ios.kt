package com.programmersbox.aniyomi.repo

// iOS has no Aniyomi runtime, so gzip-compressed index_v2 repos are never decoded there.
actual fun gunzip(bytes: ByteArray): ByteArray =
    throw UnsupportedOperationException("gunzip is not supported on iOS")
