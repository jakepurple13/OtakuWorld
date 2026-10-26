package com.programmersbox.cloudstream.plugins

import com.programmersbox.kmpextensionloader.ChildFirstClassLoader

/**
 * Packages a converted `.cs3` must take from the host, never from a copy bundled in the plugin: the
 * CloudStream ABI (vendored library, app shims, NiceHttp) and the libraries plugins compile against that
 * this module ships on the JVM classpath. `org.xmlpull.` is left out: Android ships it in the framework,
 * the desktop host does not, so a plugin's own copy (if any) must still be usable.
 */
val CLOUDSTREAM_PARENT_FIRST: List<String> = ChildFirstClassLoader.BASE_PARENT_FIRST + listOf(
    "com.lagradost.",
    "com.fasterxml.jackson.",
    "org.mozilla.javascript.",
    "com.fleeksoft.",
    "io.ktor.",
    "com.google.gson.",
    "dev.whyoleg.",
)
