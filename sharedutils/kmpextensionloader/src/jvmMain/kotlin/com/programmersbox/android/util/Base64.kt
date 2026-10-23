package android.util

/** android.util.Base64 on java.util.Base64, with Android's flags and line wrapping. */
object Base64 {
    const val DEFAULT = 0
    const val NO_PADDING = 1
    const val NO_WRAP = 2
    const val CRLF = 4
    const val URL_SAFE = 8
    const val NO_CLOSE = 16

    /** Android wraps encoded output every 76 characters and ends every line, including the last. */
    private const val LINE_LENGTH = 76

    @JvmStatic fun decode(str: String, flags: Int): ByteArray = decode(str.toByteArray(Charsets.US_ASCII), flags)

    @JvmStatic fun decode(input: ByteArray, flags: Int): ByteArray = decode(input, 0, input.size, flags)

    @JvmStatic
    fun decode(input: ByteArray, offset: Int, len: Int, flags: Int): ByteArray {
        // Android skips whitespace and accepts missing padding; any other bad character is an error.
        val cleaned = String(input, offset, len, Charsets.US_ASCII).filterNot { it == '\n' || it == '\r' || it == ' ' || it == '\t' }
        val decoder = if (flags and URL_SAFE != 0) java.util.Base64.getUrlDecoder() else java.util.Base64.getDecoder()
        return try {
            decoder.decode(cleaned)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("bad base-64", e)
        }
    }

    @JvmStatic fun encodeToString(input: ByteArray, flags: Int): String = String(encode(input, flags), Charsets.US_ASCII)

    @JvmStatic
    fun encodeToString(input: ByteArray, offset: Int, len: Int, flags: Int): String =
        String(encode(input, offset, len, flags), Charsets.US_ASCII)

    @JvmStatic fun encode(input: ByteArray, flags: Int): ByteArray = encode(input, 0, input.size, flags)

    @JvmStatic
    fun encode(input: ByteArray, offset: Int, len: Int, flags: Int): ByteArray {
        var encoder = if (flags and URL_SAFE != 0) java.util.Base64.getUrlEncoder() else java.util.Base64.getEncoder()
        if (flags and NO_PADDING != 0) encoder = encoder.withoutPadding()
        val encoded = encoder.encodeToString(input.copyOfRange(offset, offset + len))
        if (flags and NO_WRAP != 0 || encoded.isEmpty()) return encoded.toByteArray(Charsets.US_ASCII)
        val newline = if (flags and CRLF != 0) "\r\n" else "\n"
        return encoded.chunked(LINE_LENGTH).joinToString("") { it + newline }.toByteArray(Charsets.US_ASCII)
    }
}
