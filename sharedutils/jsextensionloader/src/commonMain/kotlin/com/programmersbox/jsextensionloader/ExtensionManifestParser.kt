package com.programmersbox.jsextensionloader

import com.programmersbox.extensioninterfaces.ExtensionManifest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val manifestJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

@Serializable
private data class ManifestJsonDto(
    val id: String? = null,
    val name: String,
    val version: String,
    val author: String? = null,
    val description: String? = null,
    val iconUrl: String? = null,
    val updateUrl: String? = null,
)

object ExtensionManifestParser {

    private val headerLine = Regex("""^//\s*(\w+)\s*:\s*(.+)$""")

    fun parse(scriptText: String, companionManifestJson: String?, sourceId: String): ExtensionManifest {
        if (companionManifestJson != null) {
            val dto = manifestJson.decodeFromString(ManifestJsonDto.serializer(), companionManifestJson)
            return ExtensionManifest(
                id = dto.id ?: sourceId,
                name = dto.name,
                version = dto.version,
                author = dto.author,
                description = dto.description,
                iconUrl = dto.iconUrl,
                updateUrl = dto.updateUrl,
            )
        }
        return parseHeaderComment(scriptText, sourceId)
    }

    private fun parseHeaderComment(scriptText: String, sourceId: String): ExtensionManifest {
        val fields = mutableMapOf<String, String>()
        for (line in scriptText.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val match = headerLine.find(trimmed) ?: break
            fields[match.groupValues[1].lowercase()] = match.groupValues[2].trim()
        }
        return ExtensionManifest(
            id = sourceId,
            name = fields.getValue("name"),
            version = fields.getValue("version"),
            author = fields["author"],
            description = fields["description"],
            iconUrl = fields["iconurl"],
            updateUrl = fields["updateurl"],
        )
    }
}
