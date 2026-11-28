package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.motion.MotionSettingsRoute
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlinx.serialization.EncodeDefault
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

/**
 * Every window and tab, saved on change and on quit, restored on the next launch. [version] is the file format; a file
 * without one is version 1, and one newer than [CURRENT_VERSION] (written by a later app) is not loaded.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TabSession(
    val windows: List<WindowSession>,
    @EncodeDefault val version: Int = CURRENT_VERSION,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/** [bounds] is null when the window never reported a floating position (the default size is used then). */
@Serializable
data class WindowSession(
    val bounds: WindowBounds? = null,
    val isMain: Boolean,
    val activeIndex: Int,
    val tabs: List<TabSessionEntry>,
)

/**
 * Each key is its own [JsonElement], so one key that no longer decodes can't fail the whole file. [backStack] is the
 * tab's whole history (the name predates that); [index] is the entry shown, -1 (an older file) meaning the last.
 */
@Serializable
data class TabSessionEntry(val pinned: Boolean, val backStack: List<JsonElement>, val index: Int = -1)

/**
 * The NavKeys a module can show, registered under `polymorphic(NavKey::class)`. kmpuiviews registers [Screen] and its
 * desktop pages ([CoreNavKeySerializers]); each app registers its own as a Koin single.
 *
 * [restorable] is false for a key that would come back wrong after a relaunch (a streamed reader or player needs the
 * chapter its click handed over). It is left out of the saved history, so a reader opened from Details returns as Details.
 */
class NavKeySerializers(val module: SerializersModule, val restorable: (NavKey) -> Boolean = { true })

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

/** A key is restorable when every module allows it. */
fun List<NavKeySerializers>.restorable(): (NavKey) -> Boolean = { key -> all { it.restorable(key) } }

/** The main window's size before it was ever saved; Compose's own default window size. */
val DefaultMainWindowSize = DpSize(800.dp, 600.dp)

fun initialWindowSize(bounds: WindowBounds?): DpSize =
    bounds?.let { DpSize(it.width, it.height) } ?: DefaultMainWindowSize

/**
 * Reads and writes [file]. A write goes to a temp file that is then renamed over it, so a crash can't corrupt it.
 * Writes are serialized, so two can never share the temp file.
 */
class TabSessionStore(
    private val file: File,
    private val json: Json,
    private val restorable: (NavKey) -> Boolean = { true },
) {

    private val navKey = PolymorphicSerializer(NavKey::class)
    private val temp = File(file.parentFile, "${file.name}.tmp")
    private val warned = mutableSetOf<String>()

    /** Null for a key no module registered, or one that isn't [restorable]. */
    fun encode(key: NavKey): JsonElement? = try {
        if (restorable(key)) json.encodeToJsonElement(navKey, key) else null
    } catch (e: Exception) {
        val name = key::class.qualifiedName ?: key::class.toString()
        // Once per class: the session is rebuilt on every change.
        if (synchronized(warned) { warned.add(name) }) {
            log.warning("Not saving $name, no module registered it for tab restore: ${e.message}")
        }
        null
    }

    /**
     * Null for a key that no longer decodes (its page was removed or renamed, or its module isn't loaded), or one that
     * isn't [restorable] (a file saved before it was marked so).
     */
    fun decode(element: JsonElement): NavKey? = try {
        json.decodeFromJsonElement(navKey, element).takeIf(restorable)
    } catch (e: Exception) {
        log.info("Skipping a saved page that no longer decodes: ${e.message}")
        null
    }

    /** Null for a missing, empty or corrupt file, or one from a newer format version. */
    fun load(): TabSession? = try {
        file.takeIf { it.isFile }
            ?.readText()
            ?.takeIf { it.isNotBlank() }
            ?.let { json.decodeFromString(TabSession.serializer(), it) }
            ?.takeIf { session ->
                (session.version <= TabSession.CURRENT_VERSION).also { known ->
                    if (!known) log.warning("Tab session is version ${session.version}, newer than ${TabSession.CURRENT_VERSION}; starting fresh")
                }
            }
    } catch (e: Exception) {
        log.log(Level.WARNING, "Tab session unreadable, starting fresh", e)
        null
    }

    fun save(controller: TabsController): Boolean = write(controller.toSession(::encode))

    /** False when the write failed; the previous session is then left as it was. */
    @Synchronized
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
        if (temp.isFile) temp.delete()
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
