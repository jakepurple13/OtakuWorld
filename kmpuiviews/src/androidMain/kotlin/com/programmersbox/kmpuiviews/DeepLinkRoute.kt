package com.programmersbox.kmpuiviews

import androidx.core.net.toUri
import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.ByteString.Companion.encodeUtf8

class DeepLinkRoute<T : Any>(
    private val serializer: KSerializer<T>,
    schemeAndHost: String,
) {
    // Automatically generate the route path (e.g., "details/{id}")
    val routePattern: String = generateRoutePattern()

    // The complete pattern for the matcher (e.g., "myapp://details/{id}")
    val fullPattern: String = "$schemeAndHost$routePattern"

    fun toMatcher(): UriDeepLinkMatcher<T> = UriDeepLinkMatcher(fullPattern.toUri(), serializer)

    fun createUri(route: T): String {
        val jsonMap = Json.encodeToJsonElement(serializer, route).jsonObject

        var uri = fullPattern
        jsonMap.forEach { (key, element) ->
            val rawValue = element.jsonPrimitive.content
            // URL Encode the value before injecting it into the URI
            val safeValue = rawValue.encodeUtf8().string(Charsets.UTF_8)
            uri = uri.replace("{$key}", safeValue)
        }
        return uri
    }

    private fun generateRoutePattern(): String {
        val descriptor = serializer.descriptor

        // Extract "details" from com.example.DetailsScreen.Details
        val className = descriptor.serialName.substringAfterLast(".").lowercase()
        val properties = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }

        if (properties.isEmpty()) return className

        // Build as query parameters: ?title={title}&description={description}&url={url}...
        val queryParams = properties.joinToString(separator = "&") { "$it={$it}" }
        return "$className?$queryParams"
    }
}
