// Copied from aniyomiorg/aniyomi 97414446b8a95994c72dd33c41c971a89d4d25b8, Apache-2.0. Local edits marked with "OTAKU:".
package eu.kanade.tachiyomi.util

import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * App provided default [Json] instance. Configured as
 * ```
 * Json {
 *     ignoreUnknownKeys = true
 *     explicitNulls = false
 * }
 * ```
 *
 * @since extensions-lib 16
 */
val defaultJson: Json = Injekt.get<Json>()
