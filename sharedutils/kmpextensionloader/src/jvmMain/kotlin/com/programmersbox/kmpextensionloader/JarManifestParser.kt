package com.programmersbox.kmpextensionloader

import java.io.File
import java.util.jar.JarFile

data class JarManifest(
    val packageName: String,
    val versionName: String?,
    val features: Set<String>,
    val metaData: Map<String, String>,
)

object JarManifestParser {
    fun parse(jarFile: File): JarManifest {
        JarFile(jarFile).use { jar ->
            val attrs = jar.manifest?.mainAttributes
                ?: return JarManifest("", null, emptySet(), emptyMap())

            val metaData = mutableMapOf<String, String>()
            for (key in attrs.keys) {
                val keyStr = key.toString()
                val value = attrs.getValue(keyStr) ?: continue
                // Store under both the original hyphenated key and the dot-converted key
                // so Bundle lookups using "programmersbox.otaku.class" find the value
                metaData[keyStr] = value
                metaData[keyStr.replace('-', '.')] = value
            }

            val packageName = attrs.getValue("programmersbox-otaku-package") ?: ""
            val versionName = attrs.getValue("programmersbox-otaku-version")
                ?: attrs.getValue("Implementation-Version")

            val features = buildSet<String> {
                // Single feature per JAR: "programmersbox.otaku.extension.manga" etc.
                attrs.getValue("programmersbox-otaku-extension")?.let { add(it) }
            }

            return JarManifest(
                packageName = packageName,
                versionName = versionName,
                features = features,
                metaData = metaData,
            )
        }
    }
}
