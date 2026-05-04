package com.programmersbox.kmpextensionloader

import net.dongliu.apk.parser.ApkFile
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

data class ApkManifest(
    val packageName: String,
    val versionName: String?,
    val features: Set<String>,
    val metaData: Map<String, String>,
)

object ApkManifestParser {

    fun parse(apkFile: File): ApkManifest {
        ApkFile(apkFile).use { apk ->
            val meta = apk.apkMeta
            val xml = apk.transBinaryXml("AndroidManifest.xml")
            val (features, metaData) = parseManifestXml(xml)
            return ApkManifest(
                packageName = meta.packageName,
                versionName = meta.versionName,
                features = features,
                metaData = metaData,
            )
        }
    }

    private val ANDROID_NS = "http://schemas.android.com/apk/res/android"

    private fun parseManifestXml(xml: String): Pair<Set<String>, Map<String, String>> {
        val features = mutableSetOf<String>()
        val metaData = mutableMapOf<String, String>()

        runCatching {
            val factory = DocumentBuilderFactory.newInstance().also { it.isNamespaceAware = true }
            val doc = factory.newDocumentBuilder()
                .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

            val usesFeature = doc.getElementsByTagName("uses-feature")
            for (i in 0 until usesFeature.length) {
                val el = usesFeature.item(i) as? Element ?: continue
                val name = (el.getAttributeNS(ANDROID_NS, "name").takeIf { it.isNotBlank() }
                    ?: el.getAttribute("android:name").takeIf { it.isNotBlank() }) ?: continue
                features.add(name)
            }

            val metaNodes = doc.getElementsByTagName("meta-data")
            for (i in 0 until metaNodes.length) {
                val el = metaNodes.item(i) as? Element ?: continue
                val name = (el.getAttributeNS(ANDROID_NS, "name").takeIf { it.isNotBlank() }
                    ?: el.getAttribute("android:name").takeIf { it.isNotBlank() }) ?: continue
                val value = el.getAttributeNS(ANDROID_NS, "value").ifBlank {
                    el.getAttribute("android:value")
                }
                metaData[name] = value
            }
        }.onFailure {
            println("ApkManifestParser: failed to parse manifest XML: ${it.message}")
            it.printStackTrace()
        }

        return features to metaData
    }
}
