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

    private fun parseManifestXml(xml: String): Pair<Set<String>, Map<String, String>> {
        val features = mutableSetOf<String>()
        val metaData = mutableMapOf<String, String>()

        runCatching {
            val doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(ByteArrayInputStream(xml.toByteArray()))

            val usesFeature = doc.getElementsByTagName("uses-feature")
            for (i in 0 until usesFeature.length) {
                val el = usesFeature.item(i) as? Element ?: continue
                val name = el.getAttribute("android:name").takeIf { it.isNotBlank() } ?: continue
                features.add(name)
            }

            val metaNodes = doc.getElementsByTagName("meta-data")
            for (i in 0 until metaNodes.length) {
                val el = metaNodes.item(i) as? Element ?: continue
                val name = el.getAttribute("android:name").takeIf { it.isNotBlank() } ?: continue
                val value = el.getAttribute("android:value")
                metaData[name] = value
            }
        }.onFailure { it.printStackTrace() }

        return features to metaData
    }
}
