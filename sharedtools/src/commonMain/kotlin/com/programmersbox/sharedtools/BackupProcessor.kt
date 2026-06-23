package com.programmersbox.sharedtools

import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import org.koin.core.module.Module
import org.koin.core.module.dsl.new
import org.koin.core.qualifier.named
import org.koin.dsl.bind

/**
 * Abstract class representing a backup processor that handles the backup and restoration of data.
 * This class provides methods for converting data to and from JSON format and defines abstract
 * operations for performing the backup and restore tasks.
 */
abstract class BackupProcessor {
    /**
     * Represents the name of a file associated with this entity.
     * This value is expected to store only the file name, not the complete file path.
     * It is typically used to identify or reference the corresponding resource.
     */
    abstract val fileName: String

    /**
     * Backs up data to the specified sink.
     *
     * This method performs a suspend operation to transfer data into the provided `sink`.
     *
     * @param sink The `BufferedSink` where the data will be written during the backup operation.
     */
    abstract suspend fun backup(sink: BufferedSink)

    /**
     * Restores the state or configuration of an object using the provided JSON string and buffered source.
     *
     * @param json A JSON-formatted string used for restoration.
     * @param bufferedSource A buffered source containing additional data necessary for the restore operation.
     */
    abstract suspend fun restore(json: String, bufferedSource: BufferedSource)

    /**
     * Converts an object of type [T] into its JSON string representation.
     *
     * This method leverages Kotlin's serialization library to serialize the object.
     * The type [T] must be annotated with `@Serializable` for the serialization to succeed.
     *
     * The function is inlined and uses reified type parameters, making it easier to
     * work with generic types at runtime without explicitly passing the class type.
     *
     * @receiver The object of type [T] to be serialized into a JSON string.
     * @return A JSON-formatted string representing the serialized object.
     * @throws SerializationException If the object cannot be serialized.
     */
    protected inline fun <reified T> T.toJson() = Json.encodeToString(this)

    /**
     * Extension function to deserialize a JSON string into an object of the specified type.
     *
     * This function uses the `kotlinx.serialization` library to decode the JSON content
     * into an instance of the provided type [T]. The type is determined at runtime using
     * reified type parameters.
     *
     * @param T The type into which the JSON string will be deserialized.
     * @receiver The JSON string to be deserialized.
     * @return The deserialized object of type [T].
     * @throws SerializationException If the JSON string cannot be deserialized into the specified type.
     */
    protected inline fun <reified T> String.fromJson() = Json.decodeFromString<T>(this)
}

inline fun <reified T : BackupProcessor> Module.backupProcessor(
    named: String,
    crossinline factoryBlock: () -> T,
) = factory(named(named)) { new(factoryBlock) } bind BackupProcessor::class

inline fun <reified T : BackupProcessor, reified T1> Module.backupProcessor(
    named: String,
    crossinline factoryBlock: (T1) -> T,
) = factory(named(named)) { new(factoryBlock) } bind BackupProcessor::class

inline fun <reified T : BackupProcessor, reified T1, reified T2> Module.backupProcessor(
    named: String,
    crossinline factoryBlock: (T1, T2) -> T,
) = factory(named(named)) { new(factoryBlock) } bind BackupProcessor::class

