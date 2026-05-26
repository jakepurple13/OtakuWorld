package com.programmersbox.manga.shared.reader

actual fun sanitizePath(path: String): String {
    return "file://" + path.replace(" ", "%20")
}