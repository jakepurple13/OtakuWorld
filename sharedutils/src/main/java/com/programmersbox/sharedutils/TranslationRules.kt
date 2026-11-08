package com.programmersbox.sharedutils

/** False when the language is unidentified (`und`) or already the target language. */
fun shouldTranslate(detected: String, target: String): Boolean =
    detected != "und" && detected != target
