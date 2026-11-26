package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.motion.MotionSettingsRoute
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Level
import java.util.logging.Logger

/** Every window and tab, saved on change and on quit, restored on the next launch. */
@Serializable
data class TabSession(val windows: List<WindowSession>)

/** [bounds] is null when the window never reported a floating position (the default size is used then). */
@Serializable
data class WindowSession(
    val bounds: WindowBounds? = null,
    val isMain: Boolean,
    val activeIndex: Int,
    val tabs: List<TabSessionEntry>,
)

/** Each key is its own [JsonElement], so one key that no longer decodes can't fail the whole file. */
@Serializable
data class TabSessionEntry(val pinned: Boolean, val backStack: List<JsonElement>)

/**
 * The NavKeys a module can show, registered under `polymorphic(NavKey::class)`. kmpuiviews registers [Screen] and its
 * desktop pages ([CoreNavKeySerializers]); each app registers its own as a Koin single.
 */
class NavKeySerializers(val module: SerializersModule)

@OptIn(ExperimentalSerializationApi::class)
val CoreNavKeySerializers = NavKeySerializers(
    SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<Screen>()
            // A NavKey nested in Screen.DetailsScreen, not a Screen itself.
            subclass(Screen.DetailsScreen.Details::class)
            subclass(NewTabRoute::class)
            subclass(TabsSettingsRoute::class)
            subclass(MotionSettingsRoute::class)
        }
    }
)

fun navKeyJson(serializers: List<NavKeySerializers>): Json = Json {
    ignoreUnknownKeys = true
    serializersModule = SerializersModule { serializers.forEach { include(it.module) } }
}

/** The main window's size before it was ever saved; Compose's own default window size. */
val DefaultMainWindowSize = DpSize(800.dp, 600.dp)

fun initialWindowSize(bounds: WindowBounds?): DpSize =
    bounds?.let { DpSize(it.width, it.height) } ?: DefaultMainWindowSize

/** Reads and writes [file]. A write goes to a temp file that is then renamed over it, so a crash can't corrupt it. */
class TabSessionStore(private val file: File, private val json: Json) {

    private val navKey = PolymorphicSerializer(NavKey::class)
    private val temp = File(file.parentFile, "${file.name}.tmp")

    /** Null for a key no module registered. */
    fun encode(key: NavKey): JsonElement? = try {
        json.encodeToJsonElement(navKey, key)
    } catch (e: Exception) {
        log.fine("Not saving ${key::class.qualifiedName}: ${e.message}")
        null
    }

    /** Null for a key that no longer decodes (its page was removed or renamed, or its module isn't loaded). */
    fun decode(element: JsonElement): NavKey? = try {
        json.decodeFromJsonElement(navKey, element)
    } catch (e: Exception) {
        log.info("Skipping a saved page that no longer decodes: ${e.message}")
        null
    }

    /** Null for a missing, empty or corrupt file. */
    fun load(): TabSession? = try {
        file.takeIf { it.isFile }
            ?.readText()
            ?.takeIf { it.isNotBlank() }
            ?.let { json.decodeFromString(TabSession.serializer(), it) }
    } catch (e: Exception) {
        log.log(Level.WARNING, "Tab session unreadable, starting fresh", e)
        null
    }

    fun save(controller: TabsController): Boolean = write(controller.toSession(::encode))

    /** False when the write failed; the previous session is then left as it was. */
    fun write(session: TabSession): Boolean = try {
        file.parentFile?.mkdirs()
        temp.writeText(json.encodeToString(TabSession.serializer(), session))
        try {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        true
    } catch (e: Exception) {
        log.log(Level.WARNING, "Tab session not saved", e)
        false
    }

    private companion object {
        val log: Logger = Logger.getLogger(TabSessionStore::class.java.name)
    }
}

/** [Dp] as its value in dp. */
object DpSerializer : KSerializer<Dp> {
    override val descriptor = PrimitiveSerialDescriptor("Dp", PrimitiveKind.FLOAT)
    override fun serialize(encoder: Encoder, value: Dp) = encoder.encodeFloat(value.value)
    override fun deserialize(decoder: Decoder): Dp = decoder.decodeFloat().dp
}
